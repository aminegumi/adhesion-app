package com.projet.adhesionapp.habit.service;

import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.habit.domain.DoseLog;
import com.projet.adhesionapp.habit.domain.DoseLog.DoseStatus;
import com.projet.adhesionapp.habit.domain.DoseLog.SkipReason;
import com.projet.adhesionapp.habit.model.AdherenceStatsDto;
import com.projet.adhesionapp.habit.model.AdherenceStatsDto.MedicationAdherenceDto;
import com.projet.adhesionapp.habit.model.DoseActionRequest;
import com.projet.adhesionapp.habit.model.DoseLogDto;
import com.projet.adhesionapp.habit.repo.DoseLogRepository;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import com.projet.adhesionapp.treatment.domain.Medication;
import com.projet.adhesionapp.treatment.repo.MedicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoseLogService {

    private final DoseLogRepository doseLogRepository;
    private final MedicationRepository medicationRepository;
    private final UserRepository userRepository;

    /**
     * Get today's doses for a user
     */
    public List<DoseLogDto> getTodaysDoses(Long userId) {
        return getDosesForDate(userId, LocalDate.now());
    }

    /**
     * Get doses for a specific date
     */
    public List<DoseLogDto> getDosesForDate(Long userId, LocalDate date) {
        // First, ensure doses are generated for this date
        generateDosesForDate(userId, date);

        return doseLogRepository.findByUserIdAndScheduledDateOrderByScheduledTime(userId, date)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get doses for a date range
     */
    public List<DoseLogDto> getDosesForRange(Long userId, LocalDate from, LocalDate to) {
        return doseLogRepository
                .findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(userId, from, to)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Generate dose logs for a user's medications on a specific date
     */
    @Transactional
    public void generateDosesForDate(Long userId, LocalDate date) {
        List<Medication> activeMedications = medicationRepository.findActiveMedicationsByUserId(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        for (Medication med : activeMedications) {
            List<LocalTime> times = med.getScheduledTimesList();

            if (times.isEmpty()) {
                // Default times based on timesPerDay
                times = generateDefaultTimes(med.getTimesPerDay());
            }

            for (LocalTime time : times) {
                // Check if dose log already exists
                Optional<DoseLog> existing = doseLogRepository
                        .findByMedicationIdAndScheduledDateAndScheduledTime(med.getId(), date, time);

                if (existing.isEmpty()) {
                    DoseLog dose = DoseLog.builder()
                            .user(user)
                            .medication(med)
                            .scheduledDate(date)
                            .scheduledTime(time)
                            .status(DoseStatus.PENDING)
                            .build();
                    doseLogRepository.save(dose);
                    log.debug("Created dose log for medication {} at {} on {}", med.getName(), time, date);
                }
            }
        }
    }

    /**
     * Mark a dose as taken
     */
    @Transactional
    public DoseLogDto takeDose(Long doseId, String notes) {
        DoseLog dose = doseLogRepository.findById(doseId)
                .orElseThrow(() -> new NotFoundException("Dose not found: " + doseId));

        dose.markTaken(notes);
        doseLogRepository.save(dose);

        log.info("User {} took dose {} ({}) - delay: {} minutes",
                dose.getUser().getId(), doseId, dose.getMedication().getName(), dose.getDelayMinutes());

        return toDto(dose);
    }

    /**
     * Mark a dose as skipped
     */
    @Transactional
    public DoseLogDto skipDose(Long doseId, SkipReason reason, String notes) {
        DoseLog dose = doseLogRepository.findById(doseId)
                .orElseThrow(() -> new NotFoundException("Dose not found: " + doseId));

        dose.markSkipped(reason, notes);
        doseLogRepository.save(dose);

        log.info("User {} skipped dose {} ({}) - reason: {}",
                dose.getUser().getId(), doseId, dose.getMedication().getName(), reason);

        return toDto(dose);
    }

    /**
     * Handle dose action (take or skip)
     */
    @Transactional
    public DoseLogDto handleDoseAction(Long doseId, DoseActionRequest request) {
        if ("take".equalsIgnoreCase(request.action())) {
            return takeDose(doseId, request.notes());
        } else if ("skip".equalsIgnoreCase(request.action())) {
            return skipDose(doseId, request.skipReason(), request.notes());
        } else {
            throw new IllegalArgumentException("Invalid action: " + request.action());
        }
    }

    /**
     * Get comprehensive adherence statistics
     */
    public AdherenceStatsDto getAdherenceStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        LocalDate sevenDaysAgo = today.minusDays(7);

        // Overall stats
        Double overallRate = doseLogRepository.calculateAdherenceRate(userId, thirtyDaysAgo, today);
        Double last7DaysRate = doseLogRepository.calculateAdherenceRate(userId, sevenDaysAgo, today);
        Double last30DaysRate = overallRate;

        // Streak
        Integer currentStreak = doseLogRepository.calculateCurrentStreak(userId);

        // Total counts
        long totalTaken = doseLogRepository.countByUserIdAndStatus(userId, DoseStatus.TAKEN);
        long totalMissed = doseLogRepository.countByUserIdAndStatus(userId, DoseStatus.MISSED);
        long totalSkipped = doseLogRepository.countByUserIdAndStatus(userId, DoseStatus.SKIPPED);

        // By medication
        List<MedicationAdherenceDto> byMedication = calculateByMedication(userId, thirtyDaysAgo, today);

        // By time of day
        Map<String, Double> byTimeOfDay = calculateByTimeOfDay(userId, thirtyDaysAgo, today);

        // Skip reasons
        Map<String, Long> skipReasonCounts = calculateSkipReasons(userId, thirtyDaysAgo, today);

        // Trend (compare last 7 days to previous 7 days)
        LocalDate fourteenDaysAgo = today.minusDays(14);
        Double previousWeekRate = doseLogRepository.calculateAdherenceRate(userId, fourteenDaysAgo, sevenDaysAgo);
        Double trend = (last7DaysRate != null && previousWeekRate != null)
                ? last7DaysRate - previousWeekRate
                : 0.0;

        return new AdherenceStatsDto(
                userId,
                user.getDisplayName(),
                overallRate != null ? overallRate : 0.0,
                currentStreak != null ? currentStreak : 0,
                0, // TODO: Calculate longest streak
                totalTaken,
                totalMissed,
                totalSkipped,
                last7DaysRate != null ? last7DaysRate : 0.0,
                last30DaysRate != null ? last30DaysRate : 0.0,
                byMedication,
                byTimeOfDay,
                skipReasonCounts,
                trend);
    }

    /**
     * Run every 5 minutes to mark overdue doses as missed
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void markOverdueDosesAsMissed() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Allow 2 hour grace period
        LocalTime cutoffTime = now.minusHours(2);

        List<DoseLog> overdueDoses = doseLogRepository.findOverduePendingDoses(today, cutoffTime);

        for (DoseLog dose : overdueDoses) {
            dose.setStatus(DoseStatus.MISSED);
            doseLogRepository.save(dose);
            log.info("Marked dose {} as missed (scheduled: {} {})",
                    dose.getId(), dose.getScheduledDate(), dose.getScheduledTime());
        }

        if (!overdueDoses.isEmpty()) {
            log.info("Marked {} overdue doses as missed", overdueDoses.size());
        }
    }

    /**
     * Generate default medication times based on frequency
     */
    private List<LocalTime> generateDefaultTimes(int timesPerDay) {
        return switch (timesPerDay) {
            case 1 -> List.of(LocalTime.of(8, 0));
            case 2 -> List.of(LocalTime.of(8, 0), LocalTime.of(20, 0));
            case 3 -> List.of(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0));
            case 4 -> List.of(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(17, 0), LocalTime.of(21, 0));
            default -> List.of(LocalTime.of(8, 0));
        };
    }

    private List<MedicationAdherenceDto> calculateByMedication(Long userId, LocalDate from, LocalDate to) {
        List<Medication> medications = medicationRepository.findByTreatmentPlanUserId(userId);
        List<MedicationAdherenceDto> result = new ArrayList<>();

        for (Medication med : medications) {
            List<DoseLog> doses = doseLogRepository
                    .findByMedicationIdOrderByScheduledDateDescScheduledTimeDesc(med.getId());
            long total = doses.stream().filter(d -> d.getStatus() != DoseStatus.PENDING).count();
            long taken = doses.stream().filter(d -> d.getStatus() == DoseStatus.TAKEN).count();
            double rate = total > 0 ? (taken * 100.0 / total) : 0;

            result.add(new MedicationAdherenceDto(med.getId(), med.getName(), rate, taken, total));
        }

        return result;
    }

    private Map<String, Double> calculateByTimeOfDay(Long userId, LocalDate from, LocalDate to) {
        List<DoseLog> doses = doseLogRepository
                .findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(userId, from, to);

        Map<String, List<DoseLog>> byPeriod = doses.stream()
                .filter(d -> d.getStatus() != DoseStatus.PENDING)
                .collect(Collectors.groupingBy(d -> {
                    int hour = d.getScheduledTime().getHour();
                    if (hour < 12)
                        return "Morning";
                    if (hour < 17)
                        return "Afternoon";
                    return "Evening";
                }));

        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, List<DoseLog>> entry : byPeriod.entrySet()) {
            long total = entry.getValue().size();
            long taken = entry.getValue().stream().filter(d -> d.getStatus() == DoseStatus.TAKEN).count();
            result.put(entry.getKey(), total > 0 ? (taken * 100.0 / total) : 0.0);
        }

        return result;
    }

    private Map<String, Long> calculateSkipReasons(Long userId, LocalDate from, LocalDate to) {
        List<DoseLog> doses = doseLogRepository
                .findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(userId, from, to);

        return doses.stream()
                .filter(d -> d.getStatus() == DoseStatus.SKIPPED && d.getSkipReason() != null)
                .collect(Collectors.groupingBy(d -> d.getSkipReason().name(), Collectors.counting()));
    }

    private DoseLogDto toDto(DoseLog dose) {
        return new DoseLogDto(
                dose.getId(),
                dose.getMedication().getId(),
                dose.getMedication().getName(),
                dose.getMedication().getDosage(),
                dose.getScheduledDate(),
                dose.getScheduledTime(),
                dose.getStatus(),
                dose.getTakenAt(),
                dose.getDelayMinutes(),
                dose.getNotes(),
                dose.getSkipReason(),
                dose.getReminderSent());
    }
}
