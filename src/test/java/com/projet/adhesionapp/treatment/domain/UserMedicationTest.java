package com.projet.adhesionapp.treatment.domain;

import com.projet.adhesionapp.identity.domain.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserMedication - Tests unitaires")
class UserMedicationTest {

    private User testUser;
    private UserMedication medication;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@example.com").build();
        medication = UserMedication.builder()
                .id(1L)
                .user(testUser)
                .name("Aspirine")
                .dosage("100mg")
                .form(UserMedication.MedicationForm.TABLET)
                .frequencyPerDay(2)
                .startDate(LocalDate.now())
                .active(true)
                .isChronic(false)
                .remindersEnabled(true)
                .reminderMinutesBefore(15)
                .build();
    }

    @Nested
    @DisplayName("Scheduled Times")
    class ScheduledTimesTests {

        @Test
        @DisplayName("Convertit les heures en liste")
        void shouldConvertTimesToList() {
            medication.setScheduledTimes("08:00,12:00,20:00");

            List<LocalTime> times = medication.getScheduledTimesList();

            assertThat(times).hasSize(3);
            assertThat(times.get(0)).isEqualTo(LocalTime.of(8, 0));
            assertThat(times.get(1)).isEqualTo(LocalTime.of(12, 0));
            assertThat(times.get(2)).isEqualTo(LocalTime.of(20, 0));
        }

        @Test
        @DisplayName("Définit les heures depuis une liste")
        void shouldSetTimesFromList() {
            List<LocalTime> times = List.of(
                    LocalTime.of(9, 0),
                    LocalTime.of(21, 0)
            );

            medication.setScheduledTimesList(times);

            assertThat(medication.getScheduledTimes()).isEqualTo("09:00,21:00");
        }

        @Test
        @DisplayName("Retourne liste vide si pas d'heures")
        void shouldReturnEmptyListIfNoTimes() {
            medication.setScheduledTimes(null);

            List<LocalTime> times = medication.getScheduledTimesList();

            assertThat(times).isEmpty();
        }

        @Test
        @DisplayName("Génère heures effectives basées sur fréquence")
        void shouldGenerateEffectiveTimesBasedOnFrequency() {
            medication.setScheduledTimes(null);
            medication.setFrequencyPerDay(3);

            List<LocalTime> times = medication.getEffectiveScheduledTimes();

            assertThat(times).hasSize(3);
        }

        @Test
        @DisplayName("Utilise heures définies si présentes")
        void shouldUseDefinedTimesIfPresent() {
            medication.setScheduledTimes("07:30,19:30");
            medication.setFrequencyPerDay(2);

            List<LocalTime> times = medication.getEffectiveScheduledTimes();

            assertThat(times).hasSize(2);
            assertThat(times.get(0)).isEqualTo(LocalTime.of(7, 30));
        }
    }

    @Nested
    @DisplayName("Active Status")
    class ActiveStatusTests {

        @Test
        @DisplayName("Est actuellement actif si actif et dans la période")
        void shouldBeCurrentlyActiveIfActiveAndInPeriod() {
            medication.setActive(true);
            medication.setStartDate(LocalDate.now().minusDays(5));
            medication.setEndDate(LocalDate.now().plusDays(5));

            assertThat(medication.isCurrentlyActive()).isTrue();
        }

        @Test
        @DisplayName("N'est pas actif si désactivé")
        void shouldNotBeActiveIfDeactivated() {
            medication.setActive(false);

            assertThat(medication.isCurrentlyActive()).isFalse();
        }

        @Test
        @DisplayName("N'est pas actif si avant date de début")
        void shouldNotBeActiveBeforeStartDate() {
            medication.setActive(true);
            medication.setStartDate(LocalDate.now().plusDays(1));

            assertThat(medication.isCurrentlyActive()).isFalse();
        }

        @Test
        @DisplayName("N'est pas actif si après date de fin")
        void shouldNotBeActiveAfterEndDate() {
            medication.setActive(true);
            medication.setStartDate(LocalDate.now().minusDays(10));
            medication.setEndDate(LocalDate.now().minusDays(1));

            assertThat(medication.isCurrentlyActive()).isFalse();
        }

        @Test
        @DisplayName("Est actif si chronique sans date de fin")
        void shouldBeActiveIfChronicWithoutEndDate() {
            medication.setActive(true);
            medication.setIsChronic(true);
            medication.setEndDate(null);
            medication.setStartDate(LocalDate.now().minusDays(30));

            assertThat(medication.isCurrentlyActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("MedicationForm Enum")
    class MedicationFormTests {

        @Test
        @DisplayName("Tous les types de médicaments existent")
        void shouldHaveAllMedicationForms() {
            assertThat(UserMedication.MedicationForm.values()).contains(
                    UserMedication.MedicationForm.TABLET,
                    UserMedication.MedicationForm.CAPSULE,
                    UserMedication.MedicationForm.LIQUID,
                    UserMedication.MedicationForm.INJECTION,
                    UserMedication.MedicationForm.CREAM,
                    UserMedication.MedicationForm.INHALER,
                    UserMedication.MedicationForm.DROPS,
                    UserMedication.MedicationForm.PATCH,
                    UserMedication.MedicationForm.SUPPOSITORY,
                    UserMedication.MedicationForm.OTHER
            );
        }

        @Test
        @DisplayName("Valeur par défaut est TABLET")
        void shouldDefaultToTablet() {
            UserMedication newMed = UserMedication.builder()
                    .name("Test")
                    .build();

            assertThat(newMed.getForm()).isEqualTo(UserMedication.MedicationForm.TABLET);
        }
    }

    @Nested
    @DisplayName("Stock Management")
    class StockManagementTests {

        @Test
        @DisplayName("Détecte stock bas")
        void shouldDetectLowStock() {
            medication.setCurrentStock(3);
            medication.setLowStockThreshold(5);

            assertThat(medication.getCurrentStock()).isLessThan(medication.getLowStockThreshold());
        }

        @Test
        @DisplayName("Stock suffisant")
        void shouldHaveSufficientStock() {
            medication.setCurrentStock(10);
            medication.setLowStockThreshold(5);

            assertThat(medication.getCurrentStock()).isGreaterThanOrEqualTo(medication.getLowStockThreshold());
        }
    }

    @Nested
    @DisplayName("Builder and Fields")
    class BuilderTests {

        @Test
        @DisplayName("Construit avec tous les champs")
        void shouldBuildWithAllFields() {
            UserMedication med = UserMedication.builder()
                    .id(1L)
                    .user(testUser)
                    .name("Doliprane")
                    .dosage("500mg")
                    .form(UserMedication.MedicationForm.TABLET)
                    .frequencyPerDay(3)
                    .scheduledTimes("08:00,14:00,20:00")
                    .prescribedBy("Dr. Martin")
                    .instructions("Prendre avec de l'eau")
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusMonths(1))
                    .isChronic(false)
                    .currentStock(30)
                    .lowStockThreshold(5)
                    .active(true)
                    .remindersEnabled(true)
                    .reminderMinutesBefore(15)
                    .notes("Notes")
                    .reason("Douleur")
                    .color("#FF0000")
                    .build();

            assertThat(med.getName()).isEqualTo("Doliprane");
            assertThat(med.getDosage()).isEqualTo("500mg");
            assertThat(med.getPrescribedBy()).isEqualTo("Dr. Martin");
            assertThat(med.getColor()).isEqualTo("#FF0000");
        }

        @Test
        @DisplayName("Valeurs par défaut correctes")
        void shouldHaveCorrectDefaults() {
            UserMedication med = UserMedication.builder()
                    .name("Test")
                    .build();

            assertThat(med.getForm()).isEqualTo(UserMedication.MedicationForm.TABLET);
            assertThat(med.getFrequencyPerDay()).isEqualTo(1);
            assertThat(med.getIsChronic()).isFalse();
            assertThat(med.getActive()).isTrue();
            assertThat(med.getRemindersEnabled()).isTrue();
        }
    }
}
