package com.projet.adhesionapp.treatment.service;

import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.service.PsychologicalProfileService;
import com.projet.adhesionapp.treatment.domain.DailyTask;
import com.projet.adhesionapp.treatment.domain.TreatmentPlan;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
                .build();

        testTask = DailyTask.builder()
                .id(1L)
                .treatmentPlan(testPlan)
                .title("Take medication")
                .description("Morning dose")
                .scheduledDate(LocalDate.now())
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

}
