package com.projet.adhesionapp.treatment.domain;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TreatmentPlan - Tests unitaires")
class TreatmentPlanTest {

    private User testUser;
    private TreatmentPlan plan;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@example.com").build();
        plan = TreatmentPlan.builder()
                .id(1L)
                .user(testUser)
                .title("Plan de traitement")
                .description("Description du plan")
                .durationWeeks(8)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(8))
                .build();
    }

    @Nested
    @DisplayName("Lifecycle Callbacks")
    class LifecycleTests {

        @Test
        @DisplayName("PrePersist initialise createdAt")
        void shouldInitializeCreatedAt() {
            TreatmentPlan newPlan = TreatmentPlan.builder()
                    .title("Test")
                    .user(testUser)
                    .build();
            newPlan.prePersist();

            assertThat(newPlan.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("PrePersist initialise status à ACTIVE")
        void shouldInitializeStatusToActive() {
            TreatmentPlan newPlan = TreatmentPlan.builder()
                    .title("Test")
                    .user(testUser)
                    .build();
            newPlan.prePersist();

            assertThat(newPlan.getStatus()).isEqualTo(TreatmentPlan.PlanStatus.ACTIVE);
        }

        @Test
        @DisplayName("PrePersist initialise progressPercentage à 0")
        void shouldInitializeProgressToZero() {
            TreatmentPlan newPlan = TreatmentPlan.builder()
                    .title("Test")
                    .user(testUser)
                    .build();
            newPlan.prePersist();

            assertThat(newPlan.getProgressPercentage()).isEqualTo(0);
        }

        @Test
        @DisplayName("PreUpdate met à jour updatedAt")
        void shouldUpdateUpdatedAt() {
            plan.preUpdate();

            assertThat(plan.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("PrePersist ne modifie pas valeurs existantes")
        void shouldNotModifyExistingValues() {
            plan.setStatus(TreatmentPlan.PlanStatus.PAUSED);
            plan.setProgressPercentage(50);
            Instant createdAt = Instant.now().minusSeconds(3600);
            plan.setCreatedAt(createdAt);

            plan.prePersist();

            assertThat(plan.getStatus()).isEqualTo(TreatmentPlan.PlanStatus.PAUSED);
            assertThat(plan.getProgressPercentage()).isEqualTo(50);
            assertThat(plan.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("PlanStatus Enum")
    class PlanStatusTests {

        @Test
        @DisplayName("Tous les statuts existent")
        void shouldHaveAllStatuses() {
            assertThat(TreatmentPlan.PlanStatus.values()).contains(
                    TreatmentPlan.PlanStatus.DRAFT,
                    TreatmentPlan.PlanStatus.ACTIVE,
                    TreatmentPlan.PlanStatus.PAUSED,
                    TreatmentPlan.PlanStatus.COMPLETED,
                    TreatmentPlan.PlanStatus.CANCELLED
            );
        }

        @Test
        @DisplayName("valueOf fonctionne pour chaque statut")
        void shouldParseEachStatus() {
            assertThat(TreatmentPlan.PlanStatus.valueOf("ACTIVE")).isEqualTo(TreatmentPlan.PlanStatus.ACTIVE);
            assertThat(TreatmentPlan.PlanStatus.valueOf("DRAFT")).isEqualTo(TreatmentPlan.PlanStatus.DRAFT);
            assertThat(TreatmentPlan.PlanStatus.valueOf("COMPLETED")).isEqualTo(TreatmentPlan.PlanStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Builder and Fields")
    class BuilderTests {

        @Test
        @DisplayName("Construit avec tous les champs")
        void shouldBuildWithAllFields() {
            PsychologicalProfile profile = PsychologicalProfile.builder().id(1L).build();
            TreatmentPlan fullPlan = TreatmentPlan.builder()
                    .id(1L)
                    .user(testUser)
                    .profile(profile)
                    .title("Plan complet")
                    .description("Description détaillée")
                    .planContent("Contenu du plan")
                    .durationWeeks(12)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusWeeks(12))
                    .medications("Aspirine, Paracétamol")
                    .identifiedIssues("Stress, Anxiété")
                    .weeklyGoalsJson("{\"week1\": \"goal1\"}")
                    .progressPercentage(25)
                    .status(TreatmentPlan.PlanStatus.ACTIVE)
                    .build();

            assertThat(fullPlan.getTitle()).isEqualTo("Plan complet");
            assertThat(fullPlan.getDurationWeeks()).isEqualTo(12);
            assertThat(fullPlan.getMedications()).contains("Aspirine");
            assertThat(fullPlan.getProgressPercentage()).isEqualTo(25);
        }

        @Test
        @DisplayName("Collections initialisées vides")
        void shouldInitializeEmptyCollections() {
            TreatmentPlan newPlan = TreatmentPlan.builder()
                    .title("Test")
                    .user(testUser)
                    .build();

            assertThat(newPlan.getDailyTasks()).isNotNull().isEmpty();
            assertThat(newPlan.getMedicationList()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Relationships")
    class RelationshipTests {

        @Test
        @DisplayName("Associe correctement l'utilisateur")
        void shouldAssociateUser() {
            assertThat(plan.getUser()).isEqualTo(testUser);
            assertThat(plan.getUser().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Peut avoir un profil psychologique")
        void shouldHaveProfile() {
            PsychologicalProfile profile = PsychologicalProfile.builder().id(2L).build();
            plan.setProfile(profile);

            assertThat(plan.getProfile()).isEqualTo(profile);
        }
    }
}
