package com.projet.adhesionapp.treatment.service;

import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.habit.repo.DoseLogRepository;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.service.PsychologicalProfileService;
import com.projet.adhesionapp.treatment.domain.DailyTask;
import com.projet.adhesionapp.treatment.domain.Medication;
import com.projet.adhesionapp.treatment.domain.TreatmentPlan;
import com.projet.adhesionapp.treatment.model.*;
import com.projet.adhesionapp.treatment.repo.DailyTaskRepository;
import com.projet.adhesionapp.treatment.repo.MedicationRepository;
import com.projet.adhesionapp.treatment.repo.TreatmentPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TreatmentPlanService {

    private final TreatmentPlanRepository planRepository;
    private final DailyTaskRepository taskRepository;
    private final MedicationRepository medicationRepository;
    private final DoseLogRepository doseLogRepository;
    private final UserService userService;
    private final PsychologicalProfileService profileService;
    private final OpenAIService openAIService;

    /**
     * Generate a treatment plan for a user. Uses AI when available, falls back to
     * template.
     */
    @Transactional
    public TreatmentPlan generateTreatmentPlan(CreatePlanRequest request) {
        User user = userService.findById(request.userId());
        PsychologicalProfile profile = profileService.getLatestProfile(request.userId());

        String profileType = profile != null ? profile.getProfileType() : "Unknown";
        List<String> issues = request.identifiedIssues() != null ? request.identifiedIssues()
                : (profile != null ? extractIssuesFromProfile(profile) : List.of("General wellness"));

        String medications = request.medications() != null ? request.medications() : "Not specified";

        // Try AI generation, fall back to template if unavailable
        String planContent;
        try {
            planContent = openAIService.generateTreatmentPlan(
                    user.getDisplayName(),
                    profileType,
                    issues,
                    medications);
        } catch (Exception e) {
            log.warn("AI treatment plan generation failed, using template: {}", e.getMessage());
            planContent = generateTemplatePlan(user.getDisplayName(), issues, medications);
        }

        int durationWeeks = request.durationWeeks() != null ? request.durationWeeks() : 4;
        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();

        TreatmentPlan plan = TreatmentPlan.builder()
                .user(user)
                .profile(profile)
                .title(request.title() != null ? request.title() : "Personalized Treatment Plan")
                .description(request.description())
                .planContent(planContent)
                .durationWeeks(durationWeeks)
                .startDate(startDate)
                .endDate(startDate.plusWeeks(durationWeeks))
                .medications(medications)
                .identifiedIssues(String.join(", ", issues))
                .status(TreatmentPlan.PlanStatus.ACTIVE)
                .build();

        plan = planRepository.save(plan);

        // Create medications from the list
        if (request.medicationsList() != null && !request.medicationsList().isEmpty()) {
            List<Medication> meds = createMedicationsFromRequest(plan, request.medicationsList());
            medicationRepository.saveAll(meds);
            plan.setMedicationList(new HashSet<>(meds));
        }

        // Generate daily tasks based on the plan
        List<DailyTask> tasks = generateDailyTasks(plan, issues, durationWeeks);
        for (DailyTask task : tasks) {
            task.setTreatmentPlan(plan);
        }
        taskRepository.saveAll(tasks);
        plan.setDailyTasks(new HashSet<>(tasks));

        return plan;
    }

    /**
     * Get all treatment plans for a user.
     */
    public List<TreatmentPlan> getUserPlans(Long userId) {
        return planRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get active treatment plans for a user.
     */
    public List<TreatmentPlan> getActivePlans(Long userId) {
        return planRepository.findByUserIdAndStatus(userId, TreatmentPlan.PlanStatus.ACTIVE);
    }

    /**
     * Get a treatment plan by ID.
     */
    public TreatmentPlan getById(Long id) {
        return planRepository.findByIdWithMedications(id)
                .orElseThrow(() -> new NotFoundException("Treatment plan not found"));
    }

    /**
     * Update an existing treatment plan.
     */
    @Transactional
    public TreatmentPlan updatePlan(Long planId, CreatePlanRequest request) {
        TreatmentPlan plan = getById(planId);
        
        if (request.title() != null) {
            plan.setTitle(request.title());
        }
        if (request.description() != null) {
            plan.setDescription(request.description());
        }
        if (request.durationWeeks() != null) {
            plan.setDurationWeeks(request.durationWeeks());
            if (plan.getStartDate() != null) {
                plan.setEndDate(plan.getStartDate().plusWeeks(request.durationWeeks()));
            }
        }
        if (request.medications() != null) {
            plan.setMedications(request.medications());
        }
        if (request.identifiedIssues() != null) {
            plan.setIdentifiedIssues(String.join(", ", request.identifiedIssues()));
        }
        
        // Update medications list if provided - use orphanRemoval
        if (request.medicationsList() != null) {
            // First delete dose logs that reference the medications (foreign key constraint)
            for (Medication med : plan.getMedicationList()) {
                doseLogRepository.deleteByMedicationId(med.getId());
            }
            // Clear existing medications (orphanRemoval will delete them)
            plan.getMedicationList().clear();
            // Create new ones
            if (!request.medicationsList().isEmpty()) {
                List<Medication> meds = createMedicationsFromRequest(plan, request.medicationsList());
                plan.getMedicationList().addAll(meds);
            }
        }
        
        return planRepository.save(plan);
    }

    /**
     * Delete a treatment plan and all its associated data.
     */
    @Transactional
    public void deletePlan(Long planId) {
        TreatmentPlan plan = getById(planId);
        
        // First delete dose logs that reference the medications (foreign key constraint)
        for (Medication med : plan.getMedicationList()) {
            doseLogRepository.deleteByMedicationId(med.getId());
        }
        
        // Clear the collections - orphanRemoval will delete the entities
        plan.getDailyTasks().clear();
        plan.getMedicationList().clear();
        
        // Delete the plan
        planRepository.delete(plan);
    }

    /**
     * Get today's tasks for a user.
     */
    public List<DailyTask> getTodaysTasks(Long userId) {
        return taskRepository.findByTreatmentPlanUserIdAndScheduledDate(userId, LocalDate.now());
    }

    /**
     * Get pending tasks for a user.
     */
    public List<DailyTask> getPendingTasks(Long userId) {
        return taskRepository.findByTreatmentPlanUserIdAndCompletedFalse(userId);
    }

    /**
     * Complete a daily task.
     */
    @Transactional
    public DailyTask completeTask(Long taskId, String notes) {
        DailyTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        task.setCompleted(true);
        task.setCompletedAt(Instant.now());
        task.setNotes(notes);

        // Update plan progress
        updatePlanProgress(task.getTreatmentPlan());

        return taskRepository.save(task);
    }

    private List<String> extractIssuesFromProfile(PsychologicalProfile profile) {
        List<String> issues = new ArrayList<>();

        if (profile.getAnxietyScore() != null && profile.getAnxietyScore() > 60) {
            issues.add("Anxiety management");
        }
        if (profile.getDepressionScore() != null && profile.getDepressionScore() > 60) {
            issues.add("Mood improvement");
        }
        if (profile.getMotivationScore() != null && profile.getMotivationScore() < 40) {
            issues.add("Motivation enhancement");
        }
        if (profile.getSelfEfficacyScore() != null && profile.getSelfEfficacyScore() < 40) {
            issues.add("Building self-confidence");
        }
        if (profile.getSocialSupportScore() != null && profile.getSocialSupportScore() < 40) {
            issues.add("Social connection");
        }

        if (issues.isEmpty()) {
            issues.add("General wellness maintenance");
        }

        return issues;
    }

    private List<DailyTask> generateDailyTasks(TreatmentPlan plan, List<String> issues, int weeks) {
        List<DailyTask> tasks = new ArrayList<>();
        LocalDate startDate = plan.getStartDate();

        // Create recurring daily tasks based on issues
        String[] morningTasks = { "Take morning medication", "10-minute mindfulness meditation", "Review daily goals" };
        String[] afternoonTasks = { "15-minute walk", "Healthy snack break", "Check in with feelings" };
        String[] eveningTasks = { "Take evening medication", "Gratitude journaling", "Relaxation exercise" };

        // Create tasks for the first week as a sample (in production, generate for all
        // weeks)
        for (int day = 0; day < 7; day++) {
            LocalDate date = startDate.plusDays(day);

            // Morning tasks
            for (String taskTitle : morningTasks) {
                tasks.add(DailyTask.builder()
                        .title(taskTitle)
                        .category(taskTitle.contains("medication") ? "Medication" : "Mindfulness")
                        .scheduledDate(date)
                        .timeOfDay("Morning")
                        .durationMinutes(10)
                        .isRecurring(true)
                        .completed(false)
                        .build());
            }

            // Afternoon task
            tasks.add(DailyTask.builder()
                    .title(afternoonTasks[day % afternoonTasks.length])
                    .category("Exercise")
                    .scheduledDate(date)
                    .timeOfDay("Afternoon")
                    .durationMinutes(15)
                    .isRecurring(true)
                    .completed(false)
                    .build());

            // Evening tasks
            for (String taskTitle : eveningTasks) {
                tasks.add(DailyTask.builder()
                        .title(taskTitle)
                        .category(taskTitle.contains("medication") ? "Medication" : "Wellness")
                        .scheduledDate(date)
                        .timeOfDay("Evening")
                        .durationMinutes(10)
                        .isRecurring(true)
                        .completed(false)
                        .build());
            }
        }

        return tasks;
    }

    private void updatePlanProgress(TreatmentPlan plan) {
        List<DailyTask> allTasks = taskRepository.findByTreatmentPlanId(plan.getId());
        if (allTasks.isEmpty())
            return;

        long completed = allTasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        int progress = (int) ((completed * 100) / allTasks.size());

        plan.setProgressPercentage(progress);
        if (progress >= 100) {
            plan.setStatus(TreatmentPlan.PlanStatus.COMPLETED);
        }
        planRepository.save(plan);
    }

    public TreatmentPlanDto toDto(TreatmentPlan plan) {
        List<MedicationDto> medicationDtos = new ArrayList<>();
        try {
            if (plan.getMedicationList() != null && !plan.getMedicationList().isEmpty()) {
                medicationDtos = plan.getMedicationList().stream()
                        .map(this::toMedicationDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Could not load medications for plan {}: {}", plan.getId(), e.getMessage());
        }

        return new TreatmentPlanDto(
                plan.getId(),
                plan.getUser().getId(),
                plan.getUser().getDisplayName(),
                plan.getProfile() != null ? plan.getProfile().getId() : null,
                plan.getTitle(),
                plan.getDescription(),
                plan.getPlanContent(),
                plan.getDurationWeeks(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getMedications(),
                medicationDtos,
                plan.getIdentifiedIssues(),
                plan.getProgressPercentage(),
                plan.getStatus() != null ? plan.getStatus().name() : null,
                plan.getCreatedAt());
    }

    public DailyTaskDto toTaskDto(DailyTask task) {
        return new DailyTaskDto(
                task.getId(),
                task.getTreatmentPlan().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCategory(),
                task.getScheduledDate(),
                task.getTimeOfDay(),
                task.getDurationMinutes(),
                task.getIsRecurring(),
                task.getCompleted(),
                task.getCompletedAt(),
                task.getNotes());
    }

    /**
     * Generate a template treatment plan when AI is unavailable.
     */
    private String generateTemplatePlan(String patientName, List<String> issues, String medications) {
        StringBuilder plan = new StringBuilder();
        plan.append("# Personalized Treatment Plan for ").append(patientName).append("\n\n");

        plan.append("## Overview\n");
        plan.append("This 4-week treatment plan is designed to help you improve medication adherence ");
        plan.append("and develop healthy habits for better health outcomes.\n\n");

        plan.append("## Identified Focus Areas\n");
        for (String issue : issues) {
            plan.append("- ").append(issue).append("\n");
        }
        plan.append("\n");

        plan.append("## Current Medications\n");
        plan.append(medications).append("\n\n");

        plan.append("## Weekly Goals\n\n");

        plan.append("### Week 1: Building Foundation\n");
        plan.append("- Set up medication reminders\n");
        plan.append("- Establish a daily routine\n");
        plan.append("- Track your adherence daily\n\n");

        plan.append("### Week 2: Developing Habits\n");
        plan.append("- Take medications at the same time each day\n");
        plan.append("- Practice mindfulness for 5 minutes daily\n");
        plan.append("- Log any side effects or concerns\n\n");

        plan.append("### Week 3: Strengthening Commitment\n");
        plan.append("- Identify and address barriers to adherence\n");
        plan.append("- Celebrate small wins\n");
        plan.append("- Engage in light physical activity\n\n");

        plan.append("### Week 4: Maintaining Progress\n");
        plan.append("- Review your progress\n");
        plan.append("- Adjust strategies as needed\n");
        plan.append("- Plan for continued success\n\n");

        plan.append("## Daily Recommendations\n");
        plan.append("1. Take medications as prescribed\n");
        plan.append("2. Stay hydrated\n");
        plan.append("3. Get adequate sleep (7-8 hours)\n");
        plan.append("4. Practice stress management techniques\n");
        plan.append("5. Log your daily progress in this app\n");

        return plan.toString();
    }

    /**
     * Create medications from request list.
     */
    private List<Medication> createMedicationsFromRequest(TreatmentPlan plan, List<MedicationRequest> requests) {
        List<Medication> medications = new ArrayList<>();
        for (MedicationRequest req : requests) {
            Medication med = Medication.builder()
                    .treatmentPlan(plan)
                    .name(req.name())
                    .dosage(req.dosage())
                    .instructions(req.instructions())
                    .timesPerDay(req.timesPerDay() != null ? req.timesPerDay() : 1)
                    .isChronic(req.isChronic() != null ? req.isChronic() : false)
                    .startDate(req.startDate() != null ? req.startDate() : plan.getStartDate())
                    .endDate(req.endDate())
                    .scheduledTimes(req.scheduledTimes() != null ? String.join(",", req.scheduledTimes()) : null)
                    .notes(req.notes())
                    .notificationsEnabled(req.notificationsEnabled() != null ? req.notificationsEnabled() : true)
                    .reminderMinutesBefore(req.reminderMinutesBefore() != null ? req.reminderMinutesBefore() : 15)
                    .build();
            medications.add(med);
        }
        return medications;
    }

    /**
     * Convert Medication entity to DTO.
     */
    public MedicationDto toMedicationDto(Medication med) {
        List<String> times = med.getScheduledTimes() != null
                ? Arrays.asList(med.getScheduledTimes().split(","))
                : new ArrayList<>();

        return new MedicationDto(
                med.getId(),
                med.getTreatmentPlan().getId(),
                med.getName(),
                med.getDosage(),
                med.getInstructions(),
                med.getTimesPerDay(),
                med.getIsChronic(),
                med.getStartDate(),
                med.getEndDate(),
                times,
                med.getNotes(),
                med.getNotificationsEnabled(),
                med.getReminderMinutesBefore(),
                med.getCreatedAt());
    }

    /**
     * Get all medications for a user.
     */
    public List<Medication> getUserMedications(Long userId) {
        return medicationRepository.findByTreatmentPlanUserId(userId);
    }

    /**
     * Get active medications for a user.
     */
    public List<Medication> getActiveMedications(Long userId) {
        return medicationRepository.findActiveMedicationsByUserId(userId);
    }

    /**
     * Get chronic medications for a user.
     */
    public List<Medication> getChronicMedications(Long userId) {
        return medicationRepository.findByTreatmentPlanUserIdAndIsChronicTrue(userId);
    }

    /**
     * Get medications for a treatment plan.
     */
    public List<Medication> getPlanMedications(Long planId) {
        return medicationRepository.findByTreatmentPlanId(planId);
    }

    /**
     * Add a medication to an existing plan.
     */
    @Transactional
    public Medication addMedicationToPlan(Long planId, MedicationRequest request) {
        TreatmentPlan plan = getById(planId);
        Medication med = Medication.builder()
                .treatmentPlan(plan)
                .name(request.name())
                .dosage(request.dosage())
                .instructions(request.instructions())
                .timesPerDay(request.timesPerDay() != null ? request.timesPerDay() : 1)
                .isChronic(request.isChronic() != null ? request.isChronic() : false)
                .startDate(request.startDate() != null ? request.startDate() : plan.getStartDate())
                .endDate(request.endDate())
                .scheduledTimes(request.scheduledTimes() != null ? String.join(",", request.scheduledTimes()) : null)
                .notes(request.notes())
                .notificationsEnabled(request.notificationsEnabled() != null ? request.notificationsEnabled() : true)
                .reminderMinutesBefore(request.reminderMinutesBefore() != null ? request.reminderMinutesBefore() : 15)
                .build();
        return medicationRepository.save(med);
    }

    /**
     * Update a medication.
     */
    @Transactional
    public Medication updateMedication(Long medicationId, MedicationRequest request) {
        Medication med = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new NotFoundException("Medication not found"));

        if (request.name() != null)
            med.setName(request.name());
        if (request.dosage() != null)
            med.setDosage(request.dosage());
        if (request.instructions() != null)
            med.setInstructions(request.instructions());
        if (request.timesPerDay() != null)
            med.setTimesPerDay(request.timesPerDay());
        if (request.isChronic() != null)
            med.setIsChronic(request.isChronic());
        if (request.startDate() != null)
            med.setStartDate(request.startDate());
        if (request.endDate() != null)
            med.setEndDate(request.endDate());
        if (request.scheduledTimes() != null)
            med.setScheduledTimes(String.join(",", request.scheduledTimes()));
        if (request.notes() != null)
            med.setNotes(request.notes());
        if (request.notificationsEnabled() != null)
            med.setNotificationsEnabled(request.notificationsEnabled());
        if (request.reminderMinutesBefore() != null)
            med.setReminderMinutesBefore(request.reminderMinutesBefore());

        return medicationRepository.save(med);
    }

    /**
     * Delete a medication.
     */
    @Transactional
    public void deleteMedication(Long medicationId) {
        medicationRepository.deleteById(medicationId);
    }

    /**
     * Get medications needing reminders (for notification service).
     */
    public List<MedicationDto> getMedicationsForNotification(Long userId) {
        return medicationRepository.findByTreatmentPlanUserIdAndNotificationsEnabledTrue(userId)
                .stream()
                .map(this::toMedicationDto)
                .collect(Collectors.toList());
    }
}
