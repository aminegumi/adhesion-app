package com.projet.adhesionapp.treatment.domain;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Medication - Tests unitaires")
class MedicationTest {

    private TreatmentPlan testPlan;
    private Medication medication;

    @BeforeEach
    void setUp() {
        testPlan = TreatmentPlan.builder().id(1L).title("Plan Test").build();
        medication = Medication.builder()
                .id(1L)
                .treatmentPlan(testPlan)
                .name("Metformin")
                .dosage("500mg")
                .instructions("Prendre avec repas")
                .timesPerDay(2)
                .isChronic(true)
                .notificationsEnabled(true)
                .reminderMinutesBefore(15)
                .build();
    }

    @Nested
    @DisplayName("Scheduled Times")
    class ScheduledTimesTests {

        @Test
        @DisplayName("Convertit les heures en liste")
        void shouldConvertTimesToList() {
            medication.setScheduledTimes("08:00,14:00,20:00");

            List<LocalTime> times = medication.getScheduledTimesList();

            assertThat(times).hasSize(3);
            assertThat(times.get(0)).isEqualTo(LocalTime.of(8, 0));
        }

        @Test
        @DisplayName("Définit les heures depuis une liste")
        void shouldSetTimesFromList() {
            List<LocalTime> times = List.of(LocalTime.of(9, 0), LocalTime.of(21, 0));

            medication.setScheduledTimesList(times);

            assertThat(medication.getScheduledTimes()).isEqualTo("09:00,21:00");
        }

        @Test
        @DisplayName("Retourne liste vide si null")
        void shouldReturnEmptyListIfNull() {
            medication.setScheduledTimes(null);

            List<LocalTime> times = medication.getScheduledTimesList();

            assertThat(times).isEmpty();
        }

        @Test
        @DisplayName("Retourne liste vide si vide")
        void shouldReturnEmptyListIfEmpty() {
            medication.setScheduledTimes("");

            List<LocalTime> times = medication.getScheduledTimesList();

            assertThat(times).isEmpty();
        }

        @Test
        @DisplayName("Gère les heures invalides")
        void shouldHandleInvalidTimes() {
            medication.setScheduledTimes("08:00,invalid,20:00");

            List<LocalTime> times = medication.getScheduledTimesList();

            assertThat(times).hasSize(2);
        }

        @Test
        @DisplayName("Set null si liste vide")
        void shouldSetNullIfEmptyList() {
            medication.setScheduledTimesList(List.of());

            assertThat(medication.getScheduledTimes()).isNull();
        }
    }

    @Nested
    @DisplayName("Lifecycle Callbacks")
    class LifecycleTests {

        @Test
        @DisplayName("PrePersist initialise createdAt")
        void shouldInitializeCreatedAt() {
            Medication newMed = Medication.builder()
                    .name("Test")
                    .treatmentPlan(testPlan)
                    .build();
            newMed.prePersist();

            assertThat(newMed.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("PrePersist initialise timesPerDay par défaut")
        void shouldInitializeTimesPerDay() {
            Medication newMed = Medication.builder()
                    .name("Test")
                    .treatmentPlan(testPlan)
                    .build();
            newMed.setTimesPerDay(null);
            newMed.prePersist();

            assertThat(newMed.getTimesPerDay()).isEqualTo(1);
        }

        @Test
        @DisplayName("PrePersist initialise isChronic par défaut")
        void shouldInitializeIsChronic() {
            Medication newMed = Medication.builder()
                    .name("Test")
                    .treatmentPlan(testPlan)
                    .build();
            newMed.setIsChronic(null);
            newMed.prePersist();

            assertThat(newMed.getIsChronic()).isFalse();
        }

        @Test
        @DisplayName("PrePersist initialise notificationsEnabled par défaut")
        void shouldInitializeNotificationsEnabled() {
            Medication newMed = Medication.builder()
                    .name("Test")
                    .treatmentPlan(testPlan)
                    .build();
            newMed.setNotificationsEnabled(null);
            newMed.prePersist();

            assertThat(newMed.getNotificationsEnabled()).isTrue();
        }

        @Test
        @DisplayName("PreUpdate met à jour updatedAt")
        void shouldUpdateUpdatedAt() {
            medication.preUpdate();

            assertThat(medication.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Builder and Fields")
    class BuilderTests {

        @Test
        @DisplayName("Construit avec tous les champs")
        void shouldBuildWithAllFields() {
            Medication med = Medication.builder()
                    .id(1L)
                    .treatmentPlan(testPlan)
                    .name("Ibuprofène")
                    .dosage("400mg")
                    .instructions("Après manger")
                    .timesPerDay(3)
                    .isChronic(false)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusWeeks(2))
                    .scheduledTimes("08:00,14:00,20:00")
                    .notes("Notes importantes")
                    .notificationsEnabled(true)
                    .reminderMinutesBefore(30)
                    .build();

            assertThat(med.getName()).isEqualTo("Ibuprofène");
            assertThat(med.getDosage()).isEqualTo("400mg");
            assertThat(med.getTimesPerDay()).isEqualTo(3);
        }

        @Test
        @DisplayName("Valeurs par défaut correctes")
        void shouldHaveCorrectDefaults() {
            Medication med = Medication.builder()
                    .name("Test")
                    .treatmentPlan(testPlan)
                    .build();

            assertThat(med.getTimesPerDay()).isEqualTo(1);
            assertThat(med.getIsChronic()).isFalse();
            assertThat(med.getNotificationsEnabled()).isTrue();
            assertThat(med.getReminderMinutesBefore()).isEqualTo(15);
        }
    }
}
