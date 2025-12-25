package com.projet.adhesionapp.habit.service;

import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.habit.domain.DoseLog;
import com.projet.adhesionapp.habit.domain.DoseLog.DoseStatus;
import com.projet.adhesionapp.habit.domain.DoseLog.SkipReason;
import com.projet.adhesionapp.habit.model.AdherenceStatsDto;
import com.projet.adhesionapp.habit.model.DoseActionRequest;
import com.projet.adhesionapp.habit.model.DoseLogDto;
import com.projet.adhesionapp.habit.repo.DoseLogRepository;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import com.projet.adhesionapp.treatment.domain.Medication;
import com.projet.adhesionapp.treatment.domain.TreatmentPlan;
import com.projet.adhesionapp.treatment.repo.MedicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires complets pour DoseLogService
 * Conforme au PAQ - Couverture minimale 80%
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DoseLogService Unit Tests")
class DoseLogServiceTest {

    @Mock
    private DoseLogRepository doseLogRepository;

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DoseLogService doseLogService;

    private User testUser;
    private Medication testMedication;
    private TreatmentPlan testPlan;
    private DoseLog testDoseLog;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("patient@example.com")
                .displayName("Test Patient")
                .active(true)
                .build();

        testPlan = TreatmentPlan.builder()
                .id(1L)
                .user(testUser)
                .title("Treatment Plan")
                .build();

        testMedication = Medication.builder()
                .id(1L)
                .treatmentPlan(testPlan)
                .name("Aspirin")
                .dosage("100mg")
                .timesPerDay(2)
                .scheduledTimes("08:00,20:00")
                .notificationsEnabled(true)
                .build();

        testDoseLog = DoseLog.builder()
                .id(1L)
                .user(testUser)
                .medication(testMedication)
                .scheduledDate(LocalDate.now())
                .scheduledTime(LocalTime.of(8, 0))
                .status(DoseStatus.PENDING)
                .reminderSent(false)
                .build();
    }

    @Nested
    @DisplayName("Get Doses Tests")
    class GetDosesTests {

        @Test
        @DisplayName("Should get today's doses")
        void shouldGetTodaysDoses() {
            // Given
            when(medicationRepository.findActiveMedicationsByUserId(1L))
                    .thenReturn(List.of(testMedication));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByMedication_IdAndScheduledDateAndScheduledTime(
                    anyLong(), any(LocalDate.class), any(LocalTime.class)))
                    .thenReturn(Optional.of(testDoseLog));
            when(doseLogRepository.findByUserIdAndScheduledDateOrderByScheduledTime(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of(testDoseLog));

            // When
            List<DoseLogDto> result = doseLogService.getTodaysDoses(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should get doses for specific date")
        void shouldGetDosesForSpecificDate() {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 15);
            when(medicationRepository.findActiveMedicationsByUserId(1L))
                    .thenReturn(List.of(testMedication));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByMedication_IdAndScheduledDateAndScheduledTime(
                    anyLong(), eq(date), any(LocalTime.class)))
                    .thenReturn(Optional.of(testDoseLog));
            when(doseLogRepository.findByUserIdAndScheduledDateOrderByScheduledTime(1L, date))
                    .thenReturn(List.of(testDoseLog));

            // When
            List<DoseLogDto> result = doseLogService.getDosesForDate(1L, date);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should get doses for date range")
        void shouldGetDosesForDateRange() {
            // Given
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 1, 31);
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    1L, from, to))
                    .thenReturn(List.of(testDoseLog));

            // When
            List<DoseLogDto> result = doseLogService.getDosesForRange(1L, from, to);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Generate Doses Tests")
    class GenerateDosesTests {

        @Test
        @DisplayName("Should generate doses for date with scheduled times")
        void shouldGenerateDosesWithScheduledTimes() {
            // Given
            LocalDate date = LocalDate.now();
            when(medicationRepository.findActiveMedicationsByUserId(1L))
                    .thenReturn(List.of(testMedication));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByMedication_IdAndScheduledDateAndScheduledTime(
                    anyLong(), eq(date), any(LocalTime.class)))
                    .thenReturn(Optional.empty());
            when(doseLogRepository.save(any(DoseLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            doseLogService.generateDosesForDate(1L, date);

            // Then
            verify(doseLogRepository, atLeastOnce()).save(any(DoseLog.class));
        }

        @Test
        @DisplayName("Should not duplicate existing dose logs")
        void shouldNotDuplicateExistingDoseLogs() {
            // Given
            LocalDate date = LocalDate.now();
            when(medicationRepository.findActiveMedicationsByUserId(1L))
                    .thenReturn(List.of(testMedication));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByMedication_IdAndScheduledDateAndScheduledTime(
                    anyLong(), eq(date), any(LocalTime.class)))
                    .thenReturn(Optional.of(testDoseLog));

            // When
            doseLogService.generateDosesForDate(1L, date);

            // Then
            verify(doseLogRepository, never()).save(any(DoseLog.class));
        }

        @Test
        @DisplayName("Should generate default times when no scheduled times")
        void shouldGenerateDefaultTimesWhenNoScheduledTimes() {
            // Given
            LocalDate date = LocalDate.now();
            testMedication.setScheduledTimes(null);
            testMedication.setTimesPerDay(3);
            
            when(medicationRepository.findActiveMedicationsByUserId(1L))
                    .thenReturn(List.of(testMedication));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByMedication_IdAndScheduledDateAndScheduledTime(
                    anyLong(), eq(date), any(LocalTime.class)))
                    .thenReturn(Optional.empty());
            when(doseLogRepository.save(any(DoseLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            doseLogService.generateDosesForDate(1L, date);

            // Then
            // Should create 3 doses for timesPerDay = 3
            verify(doseLogRepository, times(3)).save(any(DoseLog.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            LocalDate date = LocalDate.now();
            when(medicationRepository.findActiveMedicationsByUserId(1L))
                    .thenReturn(List.of(testMedication));
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> doseLogService.generateDosesForDate(1L, date));
        }
    }

    @Nested
    @DisplayName("Take Dose Tests")
    class TakeDoseTests {

        @Test
        @DisplayName("Should mark dose as taken")
        void shouldMarkDoseAsTaken() {
            // Given
            when(doseLogRepository.findById(1L)).thenReturn(Optional.of(testDoseLog));
            when(doseLogRepository.save(any(DoseLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            DoseLogDto result = doseLogService.takeDose(1L, "Took with food");

            // Then
            assertNotNull(result);
            assertEquals(DoseStatus.TAKEN, result.status());
            verify(doseLogRepository).save(any(DoseLog.class));
        }

        @Test
        @DisplayName("Should throw exception when dose not found for take")
        void shouldThrowExceptionWhenDoseNotFoundForTake() {
            // Given
            when(doseLogRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> doseLogService.takeDose(999L, "notes"));
        }
    }

    @Nested
    @DisplayName("Skip Dose Tests")
    class SkipDoseTests {

        @Test
        @DisplayName("Should mark dose as skipped")
        void shouldMarkDoseAsSkipped() {
            // Given
            when(doseLogRepository.findById(1L)).thenReturn(Optional.of(testDoseLog));
            when(doseLogRepository.save(any(DoseLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            DoseLogDto result = doseLogService.skipDose(1L, SkipReason.SIDE_EFFECTS, "Felt nauseous");

            // Then
            assertNotNull(result);
            assertEquals(DoseStatus.SKIPPED, result.status());
            assertEquals(SkipReason.SIDE_EFFECTS, result.skipReason());
        }

        @Test
        @DisplayName("Should throw exception when dose not found for skip")
        void shouldThrowExceptionWhenDoseNotFoundForSkip() {
            // Given
            when(doseLogRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> doseLogService.skipDose(999L, SkipReason.FORGOT, "notes"));
        }
    }

    @Nested
    @DisplayName("Handle Dose Action Tests")
    class HandleDoseActionTests {

        @Test
        @DisplayName("Should handle take action")
        void shouldHandleTakeAction() {
            // Given
            DoseActionRequest request = new DoseActionRequest("take", "Took with breakfast", SkipReason.OTHER);
            when(doseLogRepository.findById(1L)).thenReturn(Optional.of(testDoseLog));
            when(doseLogRepository.save(any(DoseLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            DoseLogDto result = doseLogService.handleDoseAction(1L, request);

            // Then
            assertEquals(DoseStatus.TAKEN, result.status());
        }

        @Test
        @DisplayName("Should handle skip action")
        void shouldHandleSkipAction() {
            // Given
            DoseActionRequest request = new DoseActionRequest("skip", "Need refill", SkipReason.RAN_OUT);
            when(doseLogRepository.findById(1L)).thenReturn(Optional.of(testDoseLog));
            when(doseLogRepository.save(any(DoseLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            DoseLogDto result = doseLogService.handleDoseAction(1L, request);

            // Then
            assertEquals(DoseStatus.SKIPPED, result.status());
        }

        @Test
        @DisplayName("Should throw exception for invalid action")
        void shouldThrowExceptionForInvalidAction() {
            // Given
            DoseActionRequest request = new DoseActionRequest("invalid", null, null);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> doseLogService.handleDoseAction(1L, request));
        }
    }

    @Nested
    @DisplayName("Adherence Stats Tests")
    class AdherenceStatsTests {

        @Test
        @DisplayName("Should get adherence stats")
        void shouldGetAdherenceStats() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.calculateAdherenceRate(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(85.0);
            when(doseLogRepository.calculateCurrentStreak(1L)).thenReturn(5);
            when(doseLogRepository.countByUserIdAndStatus(1L, DoseStatus.TAKEN)).thenReturn(50L);
            when(doseLogRepository.countByUserIdAndStatus(1L, DoseStatus.MISSED)).thenReturn(5L);
            when(doseLogRepository.countByUserIdAndStatus(1L, DoseStatus.SKIPPED)).thenReturn(3L);
            when(medicationRepository.findByTreatmentPlanUserId(1L)).thenReturn(List.of(testMedication));
            when(doseLogRepository.findByMedication_IdOrderByScheduledDateDescScheduledTimeDesc(1L))
                    .thenReturn(List.of(testDoseLog));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(testDoseLog));

            // When
            AdherenceStatsDto result = doseLogService.getAdherenceStats(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.userId());
            assertEquals("Test Patient", result.userName());
            assertEquals(85.0, result.overallAdherenceRate());
            assertEquals(5, result.currentStreak());
            assertEquals(50L, result.totalDosesTaken());
            assertEquals(5L, result.totalDosesMissed());
            assertEquals(3L, result.totalDosesSkipped());
        }

        @Test
        @DisplayName("Should handle null adherence rate")
        void shouldHandleNullAdherenceRate() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.calculateAdherenceRate(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(null);
            when(doseLogRepository.calculateCurrentStreak(1L)).thenReturn(null);
            when(doseLogRepository.countByUserIdAndStatus(anyLong(), any(DoseStatus.class))).thenReturn(0L);
            when(medicationRepository.findByTreatmentPlanUserId(1L)).thenReturn(Collections.emptyList());
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // When
            AdherenceStatsDto result = doseLogService.getAdherenceStats(1L);

            // Then
            assertNotNull(result);
            assertEquals(0.0, result.overallAdherenceRate());
            assertEquals(0, result.currentStreak());
        }

        @Test
        @DisplayName("Should throw exception when user not found for stats")
        void shouldThrowExceptionWhenUserNotFoundForStats() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> doseLogService.getAdherenceStats(999L));
        }
    }

    @Nested
    @DisplayName("Mark Overdue Doses Tests")
    class MarkOverdueDosesTests {

        @Test
        @DisplayName("Should mark overdue doses as missed")
        void shouldMarkOverdueDosesAsMissed() {
            // Given
            DoseLog overdueDose = DoseLog.builder()
                    .id(2L)
                    .user(testUser)
                    .medication(testMedication)
                    .scheduledDate(LocalDate.now())
                    .scheduledTime(LocalTime.of(6, 0))
                    .status(DoseStatus.PENDING)
                    .build();

            when(doseLogRepository.findOverduePendingDoses(any(LocalDate.class), any(LocalTime.class)))
                    .thenReturn(List.of(overdueDose));
            when(doseLogRepository.save(any(DoseLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            doseLogService.markOverdueDosesAsMissed();

            // Then
            verify(doseLogRepository).save(any(DoseLog.class));
            assertEquals(DoseStatus.MISSED, overdueDose.getStatus());
        }

        @Test
        @DisplayName("Should do nothing when no overdue doses")
        void shouldDoNothingWhenNoOverdueDoses() {
            // Given
            when(doseLogRepository.findOverduePendingDoses(any(LocalDate.class), any(LocalTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            doseLogService.markOverdueDosesAsMissed();

            // Then
            verify(doseLogRepository, never()).save(any(DoseLog.class));
        }
    }

    @Nested
    @DisplayName("Default Times Generation Tests")
    class DefaultTimesGenerationTests {

        @Test
        @DisplayName("Should generate 1 time for once daily")
        void shouldGenerateOneTimeForOnceDaily() {
            // Given
            LocalDate date = LocalDate.now();
            testMedication.setScheduledTimes(null);
            testMedication.setTimesPerDay(1);
            
            when(medicationRepository.findActiveMedicationsByUserId(1L))
                    .thenReturn(List.of(testMedication));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByMedication_IdAndScheduledDateAndScheduledTime(
                    anyLong(), eq(date), any(LocalTime.class)))
                    .thenReturn(Optional.empty());
            when(doseLogRepository.save(any(DoseLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            doseLogService.generateDosesForDate(1L, date);

            // Then
            verify(doseLogRepository, times(1)).save(any(DoseLog.class));
        }

        @Test
        @DisplayName("Should generate 2 times for twice daily")
        void shouldGenerateTwoTimesForTwiceDaily() {
            // Given
            LocalDate date = LocalDate.now();
            testMedication.setScheduledTimes(null);
            testMedication.setTimesPerDay(2);
            
            when(medicationRepository.findActiveMedicationsByUserId(1L))
                    .thenReturn(List.of(testMedication));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByMedication_IdAndScheduledDateAndScheduledTime(
                    anyLong(), eq(date), any(LocalTime.class)))
                    .thenReturn(Optional.empty());
            when(doseLogRepository.save(any(DoseLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            doseLogService.generateDosesForDate(1L, date);

            // Then
            verify(doseLogRepository, times(2)).save(any(DoseLog.class));
        }

        @Test
        @DisplayName("Should generate 4 times for four times daily")
        void shouldGenerateFourTimesForFourTimesDaily() {
            // Given
            LocalDate date = LocalDate.now();
            testMedication.setScheduledTimes(null);
            testMedication.setTimesPerDay(4);
            
            when(medicationRepository.findActiveMedicationsByUserId(1L))
                    .thenReturn(List.of(testMedication));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByMedication_IdAndScheduledDateAndScheduledTime(
                    anyLong(), eq(date), any(LocalTime.class)))
                    .thenReturn(Optional.empty());
            when(doseLogRepository.save(any(DoseLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            doseLogService.generateDosesForDate(1L, date);

            // Then
            verify(doseLogRepository, times(4)).save(any(DoseLog.class));
        }
    }

    @Nested
    @DisplayName("Time of Day Adherence Tests")
    class TimeOfDayAdherenceTests {

        @Test
        @DisplayName("Should calculate by time of day - morning")
        void shouldCalculateByTimeOfDayMorning() {
            // Given
            DoseLog morningDose = DoseLog.builder()
                    .id(1L)
                    .user(testUser)
                    .medication(testMedication)
                    .scheduledDate(LocalDate.now().minusDays(1))
                    .scheduledTime(LocalTime.of(9, 0)) // Morning
                    .status(DoseStatus.TAKEN)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.calculateAdherenceRate(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(85.0);
            when(doseLogRepository.calculateCurrentStreak(1L)).thenReturn(5);
            when(doseLogRepository.countByUserIdAndStatus(anyLong(), any(DoseStatus.class))).thenReturn(10L);
            when(medicationRepository.findByTreatmentPlanUserId(1L)).thenReturn(List.of(testMedication));
            when(doseLogRepository.findByMedication_IdOrderByScheduledDateDescScheduledTimeDesc(1L))
                    .thenReturn(List.of(morningDose));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(morningDose));

            // When
            AdherenceStatsDto result = doseLogService.getAdherenceStats(1L);

            // Then
            assertNotNull(result);
            assertNotNull(result.byTimeOfDay());
            assertTrue(result.byTimeOfDay().containsKey("Morning"));
        }

        @Test
        @DisplayName("Should calculate by time of day - afternoon")
        void shouldCalculateByTimeOfDayAfternoon() {
            // Given
            DoseLog afternoonDose = DoseLog.builder()
                    .id(1L)
                    .user(testUser)
                    .medication(testMedication)
                    .scheduledDate(LocalDate.now().minusDays(1))
                    .scheduledTime(LocalTime.of(14, 0)) // Afternoon
                    .status(DoseStatus.TAKEN)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.calculateAdherenceRate(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(85.0);
            when(doseLogRepository.calculateCurrentStreak(1L)).thenReturn(5);
            when(doseLogRepository.countByUserIdAndStatus(anyLong(), any(DoseStatus.class))).thenReturn(10L);
            when(medicationRepository.findByTreatmentPlanUserId(1L)).thenReturn(List.of(testMedication));
            when(doseLogRepository.findByMedication_IdOrderByScheduledDateDescScheduledTimeDesc(1L))
                    .thenReturn(List.of(afternoonDose));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(afternoonDose));

            // When
            AdherenceStatsDto result = doseLogService.getAdherenceStats(1L);

            // Then
            assertNotNull(result);
            assertTrue(result.byTimeOfDay().containsKey("Afternoon"));
        }

        @Test
        @DisplayName("Should calculate by time of day - evening")
        void shouldCalculateByTimeOfDayEvening() {
            // Given
            DoseLog eveningDose = DoseLog.builder()
                    .id(1L)
                    .user(testUser)
                    .medication(testMedication)
                    .scheduledDate(LocalDate.now().minusDays(1))
                    .scheduledTime(LocalTime.of(20, 0)) // Evening
                    .status(DoseStatus.TAKEN)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.calculateAdherenceRate(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(85.0);
            when(doseLogRepository.calculateCurrentStreak(1L)).thenReturn(5);
            when(doseLogRepository.countByUserIdAndStatus(anyLong(), any(DoseStatus.class))).thenReturn(10L);
            when(medicationRepository.findByTreatmentPlanUserId(1L)).thenReturn(List.of(testMedication));
            when(doseLogRepository.findByMedication_IdOrderByScheduledDateDescScheduledTimeDesc(1L))
                    .thenReturn(List.of(eveningDose));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(eveningDose));

            // When
            AdherenceStatsDto result = doseLogService.getAdherenceStats(1L);

            // Then
            assertNotNull(result);
            assertTrue(result.byTimeOfDay().containsKey("Evening"));
        }
    }

    @Nested
    @DisplayName("Skip Reason Stats Tests")
    class SkipReasonStatsTests {

        @Test
        @DisplayName("Should count skip reasons")
        void shouldCountSkipReasons() {
            // Given
            DoseLog skippedDose = DoseLog.builder()
                    .id(1L)
                    .user(testUser)
                    .medication(testMedication)
                    .scheduledDate(LocalDate.now().minusDays(1))
                    .scheduledTime(LocalTime.of(8, 0))
                    .status(DoseStatus.SKIPPED)
                    .skipReason(SkipReason.SIDE_EFFECTS)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.calculateAdherenceRate(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(85.0);
            when(doseLogRepository.calculateCurrentStreak(1L)).thenReturn(5);
            when(doseLogRepository.countByUserIdAndStatus(anyLong(), any(DoseStatus.class))).thenReturn(10L);
            when(medicationRepository.findByTreatmentPlanUserId(1L)).thenReturn(List.of(testMedication));
            when(doseLogRepository.findByMedication_IdOrderByScheduledDateDescScheduledTimeDesc(1L))
                    .thenReturn(List.of(skippedDose));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(skippedDose));

            // When
            AdherenceStatsDto result = doseLogService.getAdherenceStats(1L);

            // Then
            assertNotNull(result);
            assertNotNull(result.skipReasonCounts());
            assertTrue(result.skipReasonCounts().containsKey("SIDE_EFFECTS"));
        }
    }
}
