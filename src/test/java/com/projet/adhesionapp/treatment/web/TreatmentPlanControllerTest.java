package com.projet.adhesionapp.treatment.web;

import com.projet.adhesionapp.treatment.domain.*;
import com.projet.adhesionapp.treatment.domain.TreatmentPlan.PlanStatus;
import com.projet.adhesionapp.treatment.model.*;
import com.projet.adhesionapp.treatment.service.TreatmentPlanService;
import com.projet.adhesionapp.treatment.service.UserMedicationService;
import com.projet.adhesionapp.identity.domain.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur de plans de traitement.
 * CT_TREAT_01 - CT_TREAT_05: CRUD et tâches
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Treatment Plan Controller - Tests unitaires")
class TreatmentPlanControllerTest {

    @Mock private TreatmentPlanService treatmentPlanService;
    @Mock private UserMedicationService userMedicationService;
    @InjectMocks private TreatmentPlanController controller;

    private TreatmentPlan testPlan;
    private TreatmentPlanDto testPlanDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("patient@test.com").build();

        testPlan = TreatmentPlan.builder()
                .id(1L)
                .user(testUser)
                .title("Plan Hypertension")
                .description("Traitement pour hypertension")
                .durationWeeks(12)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .status(PlanStatus.ACTIVE)
                .progressPercentage(50)
                .build();

        testPlanDto = new TreatmentPlanDto(
                1L, 1L, "Patient Test", 1L, "Plan Hypertension",
                "Traitement hypertension", "Contenu du plan", 12,
                LocalDate.now(), LocalDate.now().plusMonths(3),
                "Amlodipine", List.of(), "Hypertension", 50, "ACTIVE", Instant.now()
        );
    }

    @Nested
    @DisplayName("CT_TREAT_01: Création de plans")
    class CreatePlanTests {

        @Test
        @DisplayName("CT_TREAT_01a: Crée un plan avec succès")
        void shouldCreatePlanSuccessfully() {
            CreatePlanRequest request = new CreatePlanRequest(
                    1L, "Nouveau Plan", "Description",
                    List.of("Issue1"), "Amlodipine", List.of(), 12, LocalDate.now()
            );
            when(treatmentPlanService.generateTreatmentPlan(request)).thenReturn(testPlan);
            when(treatmentPlanService.toDto(testPlan)).thenReturn(testPlanDto);

            ResponseEntity<TreatmentPlanDto> response = controller.createPlan(request);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().title()).isEqualTo("Plan Hypertension");
            assertThat(response.getBody().status()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("CT_TREAT_02: Récupération de plans")
    class GetPlanTests {

        @Test
        @DisplayName("CT_TREAT_02a: Récupère un plan par ID")
        void shouldGetPlanById() {
            when(treatmentPlanService.getById(1L)).thenReturn(testPlan);
            when(treatmentPlanService.toDto(testPlan)).thenReturn(testPlanDto);

            ResponseEntity<TreatmentPlanDto> response = controller.getPlan(1L);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("CT_TREAT_02b: Récupère les plans d'un utilisateur")
        void shouldGetUserPlans() {
            when(treatmentPlanService.getUserPlans(1L)).thenReturn(List.of(testPlan));
            when(treatmentPlanService.toDto(testPlan)).thenReturn(testPlanDto);

            ResponseEntity<List<TreatmentPlanDto>> response = controller.getUserPlans(1L);

            assertThat(response.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("CT_TREAT_02c: Récupère les plans actifs")
        void shouldGetActivePlansForUser() {
            when(treatmentPlanService.getActivePlans(1L)).thenReturn(List.of(testPlan));
            when(treatmentPlanService.toDto(testPlan)).thenReturn(testPlanDto);

            ResponseEntity<List<TreatmentPlanDto>> response = controller.getActivePlans(1L);

            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).status()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("CT_TREAT_03: Modification et suppression")
    class ModifyPlanTests {

        @Test
        @DisplayName("CT_TREAT_03a: Met à jour un plan existant")
        void shouldUpdatePlan() {
            CreatePlanRequest updateRequest = new CreatePlanRequest(
                    1L, "Plan Modifié", "Nouvelle description",
                    List.of("Issue2"), "Metformin", List.of(), 8, LocalDate.now()
            );
            TreatmentPlan updatedPlan = TreatmentPlan.builder()
                    .id(1L)
                    .user(testUser)
                    .title("Plan Modifié")
                    .status(PlanStatus.ACTIVE)
                    .build();
            TreatmentPlanDto updatedDto = new TreatmentPlanDto(
                    1L, 1L, "User", 1L, "Plan Modifié", "Nouvelle description",
                    "Contenu", 8, LocalDate.now(), LocalDate.now().plusMonths(2),
                    "Metformin", List.of(), null, 0, "ACTIVE", Instant.now()
            );

            when(treatmentPlanService.updatePlan(1L, updateRequest)).thenReturn(updatedPlan);
            when(treatmentPlanService.toDto(updatedPlan)).thenReturn(updatedDto);

            ResponseEntity<TreatmentPlanDto> response = controller.updatePlan(1L, updateRequest);

            assertThat(response.getBody().title()).isEqualTo("Plan Modifié");
        }

        @Test
        @DisplayName("CT_TREAT_03b: Supprime un plan")
        void shouldDeletePlan() {
            doNothing().when(treatmentPlanService).deletePlan(1L);

            ResponseEntity<Void> response = controller.deletePlan(1L);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(treatmentPlanService).deletePlan(1L);
        }
    }

    @Nested
    @DisplayName("CT_TREAT_04: Tâches quotidiennes")
    class DailyTasksTests {

        @Test
        @DisplayName("CT_TREAT_04a: Récupère les tâches du jour")
        void shouldGetTodaysTasks() {
            DailyTask task = DailyTask.builder()
                    .id(1L)
                    .title("Prendre Amlodipine")
                    .description("Prendre 5mg")
                    .category("Medication")
                    .scheduledDate(LocalDate.now())
                    .timeOfDay("Morning")
                    .completed(false)
                    .build();
            DailyTaskDto taskDto = new DailyTaskDto(
                    1L, 1L, "Prendre Amlodipine", "Prendre 5mg", "Medication",
                    LocalDate.now(), "Morning", 5, false, false, null, null
            );

            when(treatmentPlanService.getTodaysTasks(1L)).thenReturn(List.of(task));
            when(treatmentPlanService.toTaskDto(task)).thenReturn(taskDto);

            ResponseEntity<List<DailyTaskDto>> response = controller.getTodaysTasks(1L);

            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).title()).isEqualTo("Prendre Amlodipine");
        }

        @Test
        @DisplayName("CT_TREAT_04b: Récupère les tâches en attente")
        void shouldGetPendingTasks() {
            DailyTask pendingTask = DailyTask.builder()
                    .id(2L)
                    .title("Tâche en attente")
                    .completed(false)
                    .build();
            DailyTaskDto pendingDto = new DailyTaskDto(
                    2L, 1L, "Tâche en attente", null, "Exercise",
                    LocalDate.now(), "Afternoon", 30, false, false, null, null
            );

            when(treatmentPlanService.getPendingTasks(1L)).thenReturn(List.of(pendingTask));
            when(treatmentPlanService.toTaskDto(pendingTask)).thenReturn(pendingDto);

            ResponseEntity<List<DailyTaskDto>> response = controller.getPendingTasks(1L);

            assertThat(response.getBody()).isNotEmpty();
            assertThat(response.getBody().get(0).completed()).isFalse();
        }

        @Test
        @DisplayName("CT_TREAT_04c: Complète une tâche")
        void shouldCompleteTask() {
            DailyTask completedTask = DailyTask.builder()
                    .id(1L)
                    .title("Tâche complétée")
                    .completed(true)
                    .build();
            DailyTaskDto completedDto = new DailyTaskDto(
                    1L, 1L, "Tâche complétée", null, "Exercise",
                    LocalDate.now(), "Morning", 30, false, true, Instant.now(), "Fait!"
            );

            when(treatmentPlanService.completeTask(1L, "Fait!")).thenReturn(completedTask);
            when(treatmentPlanService.toTaskDto(completedTask)).thenReturn(completedDto);

            ResponseEntity<DailyTaskDto> response = controller.completeTask(1L, "Fait!");

            assertThat(response.getBody().completed()).isTrue();
        }
    }
}
