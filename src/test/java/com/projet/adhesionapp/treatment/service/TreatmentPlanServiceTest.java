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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires complets pour TreatmentPlanService
 * Conforme au PAQ - Couverture minimale 80%
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TreatmentPlanService Unit Tests")
class TreatmentPlanServiceTest {

    @Mock
    private TreatmentPlanRepository planRepository;

    @Mock
    private DailyTaskRepository taskRepository;

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private DoseLogRepository doseLogRepository;

    @Mock
    private UserService userService;

    @Mock
    private PsychologicalProfileService profileService;

    @Mock
    private OpenAIService openAIService;

    @InjectMocks
    private TreatmentPlanService treatmentPlanService;

    private User testUser;
    private TreatmentPlan testPlan;
    private PsychologicalProfile testProfile;
    private DailyTask testTask;
    private Medication testMedication;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .displayName("Test User")
                .active(true)
                .build();

        testProfile = PsychologicalProfile.builder()
                .id(1L)
                .user(testUser)
                .profileType("Balanced")
                .anxietyScore(65.0)  // > 60 triggers "Anxiety management"
                .depressionScore(50.0)
                .motivationScore(35.0)  // < 40 triggers "Motivation enhancement"
                .selfEfficacyScore(35.0)
                .socialSupportScore(30.0)
                .build();

        testPlan = TreatmentPlan.builder()
                .id(1L)
                .user(testUser)
                .profile(testProfile)
                .title("Test Treatment Plan")
                .description("Test description")
                .planContent("Plan content for testing")
                .durationWeeks(4)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(4))
                .status(TreatmentPlan.PlanStatus.ACTIVE)
                .progressPercentage(0)
                .medicationList(new HashSet<>())
                .dailyTasks(new HashSet<>())
                .build();

        testMedication = Medication.builder()
                .id(1L)
                .treatmentPlan(testPlan)
                .name("Aspirin")
                .dosage("100mg")
                .instructions("Take with water")
                .timesPerDay(2)
                .isChronic(false)
                .startDate(LocalDate.now())
                .scheduledTimes("08:00,20:00")
                .notificationsEnabled(true)
                .reminderMinutesBefore(15)
                .createdAt(Instant.now())
                .build();

        testTask = DailyTask.builder()
                .id(1L)
                .treatmentPlan(testPlan)
                .title("Take medication")
                .description("Morning dose")
                .category("Medication")
                .scheduledDate(LocalDate.now())
                .timeOfDay("Morning")
                .durationMinutes(5)
                .isRecurring(true)
                .completed(false)
                .build();
    }

    @Nested
    @DisplayName("Get Treatment Plans Tests")
    class GetTreatmentPlansTests {

        @Test
        @DisplayName("Should get user plans")
        void shouldGetUserPlans() {
            // Given
            when(planRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(testPlan));

            // When
            List<TreatmentPlan> result = treatmentPlanService.getUserPlans(1L);

            // Then
            assertEquals(1, result.size());
            assertEquals("Test Treatment Plan", result.get(0).getTitle());
        }

        @Test
        @DisplayName("Should get active plans")
        void shouldGetActivePlans() {
            // Given
            when(planRepository.findByUserIdAndStatus(1L, TreatmentPlan.PlanStatus.ACTIVE))
                    .thenReturn(List.of(testPlan));

            // When
            List<TreatmentPlan> result = treatmentPlanService.getActivePlans(1L);

            // Then
            assertEquals(1, result.size());
            assertEquals(TreatmentPlan.PlanStatus.ACTIVE, result.get(0).getStatus());
        }

        @Test
        @DisplayName("Should get plan by ID")
        void shouldGetPlanById() {
            // Given
            when(planRepository.findByIdWithMedications(1L))
                    .thenReturn(Optional.of(testPlan));

            // When
            TreatmentPlan result = treatmentPlanService.getById(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Should throw exception when plan not found")
        void shouldThrowExceptionWhenPlanNotFound() {
            // Given
            when(planRepository.findByIdWithMedications(999L))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> treatmentPlanService.getById(999L));
        }
    }

    @Nested
    @DisplayName("Daily Tasks Tests")
    class DailyTasksTests {

        @Test
        @DisplayName("Should get today's tasks")
        void shouldGetTodaysTasks() {
            // Given
            when(taskRepository.findByTreatmentPlanUserIdAndScheduledDate(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of(testTask));

            // When
            List<DailyTask> result = treatmentPlanService.getTodaysTasks(1L);

            // Then
            assertEquals(1, result.size());
            assertFalse(result.get(0).getCompleted());
        }

        @Test
        @DisplayName("Should get pending tasks")
        void shouldGetPendingTasks() {
            // Given
            when(taskRepository.findByTreatmentPlanUserIdAndCompletedFalse(1L))
                    .thenReturn(List.of(testTask));

            // When
            List<DailyTask> result = treatmentPlanService.getPendingTasks(1L);

            // Then
            assertEquals(1, result.size());
            assertFalse(result.get(0).getCompleted());
        }

        @Test
        @DisplayName("Should complete task")
        void shouldCompleteTask() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(DailyTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            DailyTask result = treatmentPlanService.completeTask(1L, "Completed successfully");

            // Then
            assertTrue(result.getCompleted());
            assertNotNull(result.getCompletedAt());
        }

        @Test
        @DisplayName("Should throw exception when completing non-existent task")
        void shouldThrowExceptionWhenCompletingNonExistentTask() {
            // Given
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> treatmentPlanService.completeTask(999L, "notes"));
        }
    }

    @Nested
    @DisplayName("Generate Treatment Plan Tests")
    class GenerateTreatmentPlanTests {

        @Test
        @DisplayName("Should generate treatment plan with AI content")
        void shouldGenerateTreatmentPlanWithAIContent() {
            // Given
            CreatePlanRequest request = new CreatePlanRequest(
                    1L, "Treatment Plan", "Description", 
                    List.of("Anxiety management"), "Medication list", null, 4, LocalDate.now());

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateTreatmentPlan(anyString(), anyString(), anyList(), anyString()))
                    .thenReturn("AI generated plan content");
            when(planRepository.save(any(TreatmentPlan.class)))
                    .thenAnswer(invocation -> {
                        TreatmentPlan p = invocation.getArgument(0);
                        p.setId(1L);
                        p.setMedicationList(new HashSet<>());
                        p.setDailyTasks(new HashSet<>());
                        return p;
                    });
            when(taskRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

            // When
            TreatmentPlan result = treatmentPlanService.generateTreatmentPlan(request);

            // Then
            assertNotNull(result);
            assertEquals("AI generated plan content", result.getPlanContent());
            verify(openAIService).generateTreatmentPlan(anyString(), anyString(), anyList(), anyString());
        }

        @Test
        @DisplayName("Should generate treatment plan with template when AI fails")
        void shouldGenerateTreatmentPlanWithTemplateWhenAIFails() {
            // Given
            CreatePlanRequest request = new CreatePlanRequest(
                    1L, "Treatment Plan", "Description", 
                    List.of("Anxiety management"), "Medication list", null, 4, LocalDate.now());

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateTreatmentPlan(anyString(), anyString(), anyList(), anyString()))
                    .thenThrow(new RuntimeException("AI service unavailable"));
            when(planRepository.save(any(TreatmentPlan.class)))
                    .thenAnswer(invocation -> {
                        TreatmentPlan p = invocation.getArgument(0);
                        p.setId(1L);
                        p.setMedicationList(new HashSet<>());
                        p.setDailyTasks(new HashSet<>());
                        return p;
                    });
            when(taskRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

            // When
            TreatmentPlan result = treatmentPlanService.generateTreatmentPlan(request);

            // Then
            assertNotNull(result);
            assertTrue(result.getPlanContent().contains("Personalized Treatment Plan"));
        }

        @Test
        @DisplayName("Should generate treatment plan with medications list")
        void shouldGenerateTreatmentPlanWithMedicationsList() {
            // Given
            MedicationRequest medRequest = new MedicationRequest(
                    "Aspirin", "100mg", "Take with food", 2, false,
                    LocalDate.now(), null, List.of("08:00", "20:00"), "Notes", true, 15);
            CreatePlanRequest request = new CreatePlanRequest(
                    1L, "Treatment Plan", "Description", 
                    List.of("Anxiety"), "Medications", List.of(medRequest), 4, LocalDate.now());

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateTreatmentPlan(anyString(), anyString(), anyList(), anyString()))
                    .thenReturn("AI plan");
            when(planRepository.save(any(TreatmentPlan.class)))
                    .thenAnswer(invocation -> {
                        TreatmentPlan p = invocation.getArgument(0);
                        p.setId(1L);
                        p.setMedicationList(new HashSet<>());
                        p.setDailyTasks(new HashSet<>());
                        return p;
                    });
            when(medicationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(taskRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

            // When
            TreatmentPlan result = treatmentPlanService.generateTreatmentPlan(request);

            // Then
            assertNotNull(result);
            verify(medicationRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should extract issues from profile when not provided")
        void shouldExtractIssuesFromProfileWhenNotProvided() {
            // Given - Profile with high anxiety and low motivation
            CreatePlanRequest request = new CreatePlanRequest(
                    1L, null, null, null, null, null, null, null);

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateTreatmentPlan(anyString(), anyString(), anyList(), anyString()))
                    .thenReturn("AI plan");
            when(planRepository.save(any(TreatmentPlan.class)))
                    .thenAnswer(invocation -> {
                        TreatmentPlan p = invocation.getArgument(0);
                        p.setId(1L);
                        p.setMedicationList(new HashSet<>());
                        p.setDailyTasks(new HashSet<>());
                        return p;
                    });
            when(taskRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

            // When
            TreatmentPlan result = treatmentPlanService.generateTreatmentPlan(request);

            // Then
            assertNotNull(result);
            assertTrue(result.getIdentifiedIssues().contains("Anxiety management") || 
                       result.getIdentifiedIssues().contains("Motivation enhancement"));
        }
    }

    @Nested
    @DisplayName("Update Plan Tests")
    class UpdatePlanTests {

        @Test
        @DisplayName("Should update plan title")
        void shouldUpdatePlanTitle() {
            // Given
            CreatePlanRequest updateRequest = new CreatePlanRequest(
                    null, "Updated Title", null, null, null, null, null, null);
            when(planRepository.findByIdWithMedications(1L)).thenReturn(Optional.of(testPlan));
            when(planRepository.save(any(TreatmentPlan.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            TreatmentPlan result = treatmentPlanService.updatePlan(1L, updateRequest);

            // Then
            assertEquals("Updated Title", result.getTitle());
        }

        @Test
        @DisplayName("Should update plan duration and recalculate end date")
        void shouldUpdatePlanDurationAndRecalculateEndDate() {
            // Given
            CreatePlanRequest updateRequest = new CreatePlanRequest(
                    null, null, null, null, null, null, 8, null);
            when(planRepository.findByIdWithMedications(1L)).thenReturn(Optional.of(testPlan));
            when(planRepository.save(any(TreatmentPlan.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            TreatmentPlan result = treatmentPlanService.updatePlan(1L, updateRequest);

            // Then
            assertEquals(8, result.getDurationWeeks());
            assertEquals(testPlan.getStartDate().plusWeeks(8), result.getEndDate());
        }

        @Test
        @DisplayName("Should update plan medications")
        void shouldUpdatePlanMedications() {
            // Given
            testPlan.getMedicationList().add(testMedication);
            MedicationRequest newMedRequest = new MedicationRequest(
                    "Ibuprofen", "200mg", "Take after meals", 3, false,
                    LocalDate.now(), null, List.of("08:00", "14:00", "20:00"), null, true, 10);
            CreatePlanRequest updateRequest = new CreatePlanRequest(
                    null, null, null, null, null, List.of(newMedRequest), null, null);
            
            when(planRepository.findByIdWithMedications(1L)).thenReturn(Optional.of(testPlan));
            when(planRepository.save(any(TreatmentPlan.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(doseLogRepository).deleteByMedicationId(anyLong());

            // When
            TreatmentPlan result = treatmentPlanService.updatePlan(1L, updateRequest);

            // Then
            assertNotNull(result);
            verify(doseLogRepository).deleteByMedicationId(1L);
        }
    }

    @Nested
    @DisplayName("Delete Plan Tests")
    class DeletePlanTests {

        @Test
        @DisplayName("Should delete plan and associated data")
        void shouldDeletePlanAndAssociatedData() {
            // Given
            testPlan.getMedicationList().add(testMedication);
            testPlan.getDailyTasks().add(testTask);
            
            when(planRepository.findByIdWithMedications(1L)).thenReturn(Optional.of(testPlan));
            doNothing().when(doseLogRepository).deleteByMedicationId(anyLong());
            doNothing().when(planRepository).delete(any(TreatmentPlan.class));

            // When
            treatmentPlanService.deletePlan(1L);

            // Then
            verify(doseLogRepository).deleteByMedicationId(1L);
            verify(planRepository).delete(testPlan);
        }
    }

    @Nested
    @DisplayName("Medication Management Tests")
    class MedicationManagementTests {

        @Test
        @DisplayName("Should get user medications")
        void shouldGetUserMedications() {
            // Given
            when(medicationRepository.findByTreatmentPlanUserId(1L))
                    .thenReturn(List.of(testMedication));

            // When
            List<Medication> result = treatmentPlanService.getUserMedications(1L);

            // Then
            assertEquals(1, result.size());
            assertEquals("Aspirin", result.get(0).getName());
        }

        @Test
        @DisplayName("Should get active medications")
        void shouldGetActiveMedications() {
            // Given
            when(medicationRepository.findActiveMedicationsByUserId(1L))
                    .thenReturn(List.of(testMedication));

            // When
            List<Medication> result = treatmentPlanService.getActiveMedications(1L);

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should get chronic medications")
        void shouldGetChronicMedications() {
            // Given
            testMedication.setIsChronic(true);
            when(medicationRepository.findByTreatmentPlanUserIdAndIsChronicTrue(1L))
                    .thenReturn(List.of(testMedication));

            // When
            List<Medication> result = treatmentPlanService.getChronicMedications(1L);

            // Then
            assertEquals(1, result.size());
            assertTrue(result.get(0).getIsChronic());
        }

        @Test
        @DisplayName("Should get plan medications")
        void shouldGetPlanMedications() {
            // Given
            when(medicationRepository.findByTreatmentPlanId(1L))
                    .thenReturn(List.of(testMedication));

            // When
            List<Medication> result = treatmentPlanService.getPlanMedications(1L);

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should add medication to plan")
        void shouldAddMedicationToPlan() {
            // Given
            MedicationRequest request = new MedicationRequest(
                    "Paracetamol", "500mg", "As needed", 3, false,
                    null, null, null, null, true, 15);
            
            when(planRepository.findByIdWithMedications(1L)).thenReturn(Optional.of(testPlan));
            when(medicationRepository.save(any(Medication.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Medication result = treatmentPlanService.addMedicationToPlan(1L, request);

            // Then
            assertNotNull(result);
            assertEquals("Paracetamol", result.getName());
            assertEquals("500mg", result.getDosage());
        }

        @Test
        @DisplayName("Should update medication")
        void shouldUpdateMedication() {
            // Given
            MedicationRequest request = new MedicationRequest(
                    "Updated Name", "200mg", null, null, null,
                    null, null, null, null, null, null);
            
            when(medicationRepository.findById(1L)).thenReturn(Optional.of(testMedication));
            when(medicationRepository.save(any(Medication.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Medication result = treatmentPlanService.updateMedication(1L, request);

            // Then
            assertEquals("Updated Name", result.getName());
            assertEquals("200mg", result.getDosage());
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent medication")
        void shouldThrowExceptionWhenUpdatingNonExistentMedication() {
            // Given
            MedicationRequest request = new MedicationRequest(
                    "Name", null, null, null, null, null, null, null, null, null, null);
            when(medicationRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> treatmentPlanService.updateMedication(999L, request));
        }

        @Test
        @DisplayName("Should delete medication")
        void shouldDeleteMedication() {
            // When
            treatmentPlanService.deleteMedication(1L);

            // Then
            verify(medicationRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should get medications for notification")
        void shouldGetMedicationsForNotification() {
            // Given
            when(medicationRepository.findByTreatmentPlanUserIdAndNotificationsEnabledTrue(1L))
                    .thenReturn(List.of(testMedication));

            // When
            List<MedicationDto> result = treatmentPlanService.getMedicationsForNotification(1L);

            // Then
            assertEquals(1, result.size());
            assertTrue(result.get(0).notificationsEnabled());
        }
    }

    @Nested
    @DisplayName("DTO Conversion Tests")
    class DtoConversionTests {

        @Test
        @DisplayName("Should convert treatment plan to DTO")
        void shouldConvertTreatmentPlanToDto() {
            // When
            TreatmentPlanDto dto = treatmentPlanService.toDto(testPlan);

            // Then
            assertNotNull(dto);
            assertEquals(1L, dto.id());
            assertEquals(1L, dto.userId());
            assertEquals("Test User", dto.userName());
            assertEquals("Test Treatment Plan", dto.title());
            assertEquals("Test description", dto.description());
            assertEquals(4, dto.durationWeeks());
            assertEquals("ACTIVE", dto.status());
        }

        @Test
        @DisplayName("Should convert daily task to DTO")
        void shouldConvertDailyTaskToDto() {
            // When
            DailyTaskDto dto = treatmentPlanService.toTaskDto(testTask);

            // Then
            assertNotNull(dto);
            assertEquals(1L, dto.id());
            assertEquals(1L, dto.planId());
            assertEquals("Take medication", dto.title());
            assertEquals("Morning dose", dto.description());
            assertEquals("Medication", dto.category());
            assertEquals("Morning", dto.timeOfDay());
            assertFalse(dto.completed());
        }

        @Test
        @DisplayName("Should convert medication to DTO")
        void shouldConvertMedicationToDto() {
            // When
            MedicationDto dto = treatmentPlanService.toMedicationDto(testMedication);

            // Then
            assertNotNull(dto);
            assertEquals(1L, dto.id());
            assertEquals(1L, dto.planId());
            assertEquals("Aspirin", dto.name());
            assertEquals("100mg", dto.dosage());
            assertEquals("Take with water", dto.instructions());
            assertEquals(2, dto.timesPerDay());
            assertFalse(dto.isChronic());
            assertTrue(dto.notificationsEnabled());
            assertEquals(15, dto.reminderMinutesBefore());
            assertEquals(2, dto.scheduledTimes().size());
        }

        @Test
        @DisplayName("Should handle null scheduled times in medication DTO")
        void shouldHandleNullScheduledTimesInMedicationDto() {
            // Given
            testMedication.setScheduledTimes(null);

            // When
            MedicationDto dto = treatmentPlanService.toMedicationDto(testMedication);

            // Then
            assertNotNull(dto);
            assertTrue(dto.scheduledTimes().isEmpty());
        }
    }

}
