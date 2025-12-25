package com.projet.adhesionapp.treatment.domain;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyTask - Tests unitaires")
class DailyTaskTest {

    private TreatmentPlan testPlan;
    private DailyTask task;

    @BeforeEach
    void setUp() {
        testPlan = TreatmentPlan.builder().id(1L).title("Plan Test").build();
        task = DailyTask.builder()
                .id(1L)
                .treatmentPlan(testPlan)
                .title("Prendre médicament")
                .description("Prendre le médicament avec le repas")
                .category("Medication")
                .scheduledDate(LocalDate.now())
                .timeOfDay("Morning")
                .durationMinutes(5)
                .isRecurring(true)
                .completed(false)
                .build();
    }

    @Nested
    @DisplayName("Lifecycle Callbacks")
    class LifecycleTests {

        @Test
        @DisplayName("PrePersist initialise createdAt")
        void shouldInitializeCreatedAt() {
            DailyTask newTask = DailyTask.builder()
                    .title("Test")
                    .treatmentPlan(testPlan)
                    .build();
            newTask.prePersist();

            assertThat(newTask.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("PrePersist initialise completed à false")
        void shouldInitializeCompletedToFalse() {
            DailyTask newTask = DailyTask.builder()
                    .title("Test")
                    .treatmentPlan(testPlan)
                    .build();
            newTask.setCompleted(null);
            newTask.prePersist();

            assertThat(newTask.getCompleted()).isFalse();
        }

        @Test
        @DisplayName("PrePersist initialise isRecurring à false")
        void shouldInitializeIsRecurringToFalse() {
            DailyTask newTask = DailyTask.builder()
                    .title("Test")
                    .treatmentPlan(testPlan)
                    .build();
            newTask.setIsRecurring(null);
            newTask.prePersist();

            assertThat(newTask.getIsRecurring()).isFalse();
        }

        @Test
        @DisplayName("PrePersist ne modifie pas valeurs existantes")
        void shouldNotModifyExistingValues() {
            Instant createdAt = Instant.now().minusSeconds(3600);
            task.setCreatedAt(createdAt);
            task.setCompleted(true);
            task.setIsRecurring(true);

            task.prePersist();

            assertThat(task.getCreatedAt()).isEqualTo(createdAt);
            assertThat(task.getCompleted()).isTrue();
            assertThat(task.getIsRecurring()).isTrue();
        }
    }

    @Nested
    @DisplayName("Task Completion")
    class CompletionTests {

        @Test
        @DisplayName("Marque la tâche comme complétée")
        void shouldMarkAsCompleted() {
            task.setCompleted(true);
            task.setCompletedAt(Instant.now());

            assertThat(task.getCompleted()).isTrue();
            assertThat(task.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Tâche non complétée par défaut")
        void shouldNotBeCompletedByDefault() {
            DailyTask newTask = DailyTask.builder()
                    .title("Test")
                    .treatmentPlan(testPlan)
                    .build();

            assertThat(newTask.getCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder and Fields")
    class BuilderTests {

        @Test
        @DisplayName("Construit avec tous les champs")
        void shouldBuildWithAllFields() {
            DailyTask fullTask = DailyTask.builder()
                    .id(1L)
                    .treatmentPlan(testPlan)
                    .title("Exercice de relaxation")
                    .description("10 minutes de méditation")
                    .category("Mindfulness")
                    .scheduledDate(LocalDate.now())
                    .timeOfDay("Evening")
                    .durationMinutes(10)
                    .isRecurring(true)
                    .completed(false)
                    .notes("Notes personnelles")
                    .build();

            assertThat(fullTask.getTitle()).isEqualTo("Exercice de relaxation");
            assertThat(fullTask.getCategory()).isEqualTo("Mindfulness");
            assertThat(fullTask.getDurationMinutes()).isEqualTo(10);
            assertThat(fullTask.getTimeOfDay()).isEqualTo("Evening");
        }

        @Test
        @DisplayName("Valeurs par défaut correctes")
        void shouldHaveCorrectDefaults() {
            DailyTask newTask = DailyTask.builder()
                    .title("Test")
                    .treatmentPlan(testPlan)
                    .build();

            assertThat(newTask.getIsRecurring()).isFalse();
            assertThat(newTask.getCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Time of Day")
    class TimeOfDayTests {

        @Test
        @DisplayName("Peut être Morning")
        void canBeMorning() {
            task.setTimeOfDay("Morning");
            assertThat(task.getTimeOfDay()).isEqualTo("Morning");
        }

        @Test
        @DisplayName("Peut être Afternoon")
        void canBeAfternoon() {
            task.setTimeOfDay("Afternoon");
            assertThat(task.getTimeOfDay()).isEqualTo("Afternoon");
        }

        @Test
        @DisplayName("Peut être Evening")
        void canBeEvening() {
            task.setTimeOfDay("Evening");
            assertThat(task.getTimeOfDay()).isEqualTo("Evening");
        }

        @Test
        @DisplayName("Peut être Any")
        void canBeAny() {
            task.setTimeOfDay("Any");
            assertThat(task.getTimeOfDay()).isEqualTo("Any");
        }
    }

    @Nested
    @DisplayName("Categories")
    class CategoryTests {

        @Test
        @DisplayName("Supporte catégorie Medication")
        void supportsMedicationCategory() {
            task.setCategory("Medication");
            assertThat(task.getCategory()).isEqualTo("Medication");
        }

        @Test
        @DisplayName("Supporte catégorie Exercise")
        void supportsExerciseCategory() {
            task.setCategory("Exercise");
            assertThat(task.getCategory()).isEqualTo("Exercise");
        }

        @Test
        @DisplayName("Supporte catégorie Mindfulness")
        void supportsMindfulnessCategory() {
            task.setCategory("Mindfulness");
            assertThat(task.getCategory()).isEqualTo("Mindfulness");
        }

        @Test
        @DisplayName("Supporte catégorie Social")
        void supportsSocialCategory() {
            task.setCategory("Social");
            assertThat(task.getCategory()).isEqualTo("Social");
        }
    }
}
