package com.projet.adhesionapp.treatment.application;

import com.projet.adhesionapp.treatment.domain.UserMedication.MedicationForm;
import com.projet.adhesionapp.treatment.model.CreateMedicationRequest;
import com.projet.adhesionapp.treatment.model.UserMedicationDto;
import com.projet.adhesionapp.treatment.service.UserMedicationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur de médicaments utilisateur.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserMedicationController - Tests unitaires")
class UserMedicationControllerTest {

    @Mock private UserMedicationService medicationService;
    @InjectMocks private UserMedicationController controller;

    private UserMedicationDto testMedication;
    private CreateMedicationRequest createRequest;

    @BeforeEach
    void setUp() {
        testMedication = new UserMedicationDto(
                1L, 1L, "Amlodipine", "5mg", MedicationForm.TABLET, 2,
                List.of("08:00", "20:00"), "Dr. Martin", "Prendre avec de l'eau",
                LocalDate.now(), null, false, 30, 5, true, true, 15, null, null, null, true
        );
        createRequest = new CreateMedicationRequest(
                1L, "Amlodipine", "5mg", MedicationForm.TABLET, 2,
                List.of("08:00", "20:00"), "Dr. Martin", "Prendre avec de l'eau",
                LocalDate.now(), null, false, 30, 5, true, 15, null, null, null
        );
    }

    @Nested
    @DisplayName("getUserMedications")
    class GetUserMedicationsTests {
        @Test
        @DisplayName("Retourne les médicaments d'un utilisateur")
        void shouldReturnUserMedications() {
            when(medicationService.getUserMedications(1L)).thenReturn(List.of(testMedication));

            ResponseEntity<List<UserMedicationDto>> response = controller.getUserMedications(1L);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getActiveMedications")
    class GetActiveMedicationsTests {
        @Test
        @DisplayName("Retourne uniquement médicaments actifs")
        void shouldReturnActiveMedications() {
            when(medicationService.getActiveMedications(1L)).thenReturn(List.of(testMedication));

            ResponseEntity<List<UserMedicationDto>> response = controller.getActiveMedications(1L);

            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).active()).isTrue();
        }
    }

    @Nested
    @DisplayName("addMedication")
    class AddMedicationTests {
        @Test
        @DisplayName("Ajoute un médicament et génère le planning")
        void shouldAddMedicationAndGenerateSchedule() {
            when(medicationService.addMedication(createRequest)).thenReturn(testMedication);
            when(medicationService.generateDailySchedule(1L)).thenReturn(2);

            ResponseEntity<UserMedicationDto> response = controller.addMedication(createRequest);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().name()).isEqualTo("Amlodipine");
            verify(medicationService).generateDailySchedule(1L);
        }

        @Test
        @DisplayName("Continue même si génération du planning échoue")
        void shouldContinueIfScheduleGenerationFails() {
            when(medicationService.addMedication(createRequest)).thenReturn(testMedication);
            doThrow(new RuntimeException("Schedule error")).when(medicationService).generateDailySchedule(1L);

            ResponseEntity<UserMedicationDto> response = controller.addMedication(createRequest);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("updateMedication")
    class UpdateMedicationTests {
        @Test
        @DisplayName("Met à jour un médicament")
        void shouldUpdateMedication() {
            when(medicationService.updateMedication(1L, createRequest)).thenReturn(testMedication);

            ResponseEntity<UserMedicationDto> response = controller.updateMedication(1L, createRequest);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            verify(medicationService).updateMedication(1L, createRequest);
        }
    }

    @Nested
    @DisplayName("deleteMedication")
    class DeleteMedicationTests {
        @Test
        @DisplayName("Supprime un médicament")
        void shouldDeleteMedication() {
            doNothing().when(medicationService).deleteMedication(1L);

            ResponseEntity<Void> response = controller.deleteMedication(1L);

            assertThat(response.getStatusCodeValue()).isEqualTo(204);
            verify(medicationService).deleteMedication(1L);
        }
    }

    @Nested
    @DisplayName("toggleActive")
    class ToggleActiveTests {
        @Test
        @DisplayName("Toggle le statut actif")
        void shouldToggleActive() {
            UserMedicationDto inactiveMed = new UserMedicationDto(
                    1L, 1L, "Amlodipine", "5mg", MedicationForm.TABLET, 2,
                    List.of("08:00"), "Dr. Martin", null,
                    LocalDate.now(), null, false, 30, 5, false, true, 15, null, null, null, false
            );
            when(medicationService.toggleActive(1L)).thenReturn(inactiveMed);

            ResponseEntity<UserMedicationDto> response = controller.toggleActive(1L);

            assertThat(response.getBody().active()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateScheduledTimes")
    class UpdateScheduledTimesTests {
        @Test
        @DisplayName("Met à jour les heures programmées")
        void shouldUpdateScheduledTimes() {
            List<String> times = List.of("09:00", "21:00");
            UserMedicationDto updated = new UserMedicationDto(
                    1L, 1L, "Amlodipine", "5mg", MedicationForm.TABLET, 2,
                    List.of("09:00", "21:00"), "Dr. Martin", null,
                    LocalDate.now(), null, false, 30, 5, true, true, 15, null, null, null, true
            );
            when(medicationService.updateScheduledTimes(eq(1L), any())).thenReturn(updated);

            ResponseEntity<UserMedicationDto> response = controller.updateScheduledTimes(1L, times);

            assertThat(response.getBody().scheduledTimes()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("toggleReminders")
    class ToggleRemindersTests {
        @Test
        @DisplayName("Toggle les rappels")
        void shouldToggleReminders() {
            UserMedicationDto noReminders = new UserMedicationDto(
                    1L, 1L, "Amlodipine", "5mg", MedicationForm.TABLET, 2,
                    List.of("08:00"), "Dr. Martin", null,
                    LocalDate.now(), null, false, 30, 5, true, false, 15, null, null, null, true
            );
            when(medicationService.toggleReminders(1L)).thenReturn(noReminders);

            ResponseEntity<UserMedicationDto> response = controller.toggleReminders(1L);

            assertThat(response.getBody().remindersEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("generateDailySchedule")
    class GenerateDailyScheduleTests {
        @Test
        @DisplayName("Génère le planning quotidien")
        void shouldGenerateDailySchedule() {
            when(medicationService.generateDailySchedule(1L)).thenReturn(4);

            ResponseEntity<Map<String, Object>> response = controller.generateDailySchedule(1L);

            assertThat(response.getBody())
                    .containsEntry("success", true)
                    .containsEntry("dosesCreated", 4);
        }
    }

    @Nested
    @DisplayName("getMedication")
    class GetMedicationTests {
        @Test
        @DisplayName("Retourne un médicament par ID")
        void shouldReturnMedicationById() {
            when(medicationService.getMedication(1L)).thenReturn(testMedication);

            ResponseEntity<UserMedicationDto> response = controller.getMedication(1L);

            assertThat(response.getBody().id()).isEqualTo(1L);
        }
    }
}
