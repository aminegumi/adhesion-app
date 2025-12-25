package com.projet.adhesionapp.habit.web;

import com.projet.adhesionapp.habit.domain.DoseLog.DoseStatus;
import com.projet.adhesionapp.habit.model.*;
import com.projet.adhesionapp.habit.service.DoseLogService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur de suivi des doses.
 * CT_DOSE_01 - CT_DOSE_04: Gestion des prises médicamenteuses
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DoseLog Controller - Tests unitaires")
class DoseLogControllerTest {

    @Mock private DoseLogService doseLogService;
    @InjectMocks private DoseLogController controller;

    private DoseLogDto testDoseLogDto;

    @BeforeEach
    void setUp() {
        testDoseLogDto = new DoseLogDto(
                1L, 1L, "Amlodipine", "5mg",
                LocalDate.now(), LocalTime.of(8, 0), DoseStatus.TAKEN,
                Instant.now(), 0, null, null, true
        );
    }

    @Nested
    @DisplayName("CT_DOSE_01: Doses du jour")
    class TodayDosesTests {

        @Test
        @DisplayName("CT_DOSE_01a: Récupère les doses programmées du jour")
        void shouldGetTodaysDoses() {
            when(doseLogService.getTodaysDoses(1L)).thenReturn(List.of(testDoseLogDto));

            ResponseEntity<List<DoseLogDto>> response = controller.getTodaysDoses(1L);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).medicationName()).isEqualTo("Amlodipine");
        }
    }

    @Nested
    @DisplayName("CT_DOSE_02: Doses par date")
    class DosesByDateTests {

        @Test
        @DisplayName("CT_DOSE_02a: Récupère les doses d'une date spécifique")
        void shouldGetDosesForDate() {
            LocalDate date = LocalDate.now();
            when(doseLogService.getDosesForDate(1L, date)).thenReturn(List.of(testDoseLogDto));

            ResponseEntity<List<DoseLogDto>> response = controller.getDosesForDate(1L, date);

            assertThat(response.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("CT_DOSE_02b: Récupère les doses par plage de dates")
        void shouldGetDosesForRange() {
            LocalDate from = LocalDate.now().minusDays(7);
            LocalDate to = LocalDate.now();

            when(doseLogService.getDosesForRange(1L, from, to)).thenReturn(List.of(testDoseLogDto));

            ResponseEntity<List<DoseLogDto>> response = controller.getDosesForRange(1L, from, to);

            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("CT_DOSE_03: Actions sur les doses")
    class DoseActionsTests {

        @Test
        @DisplayName("CT_DOSE_03a: Action sur une dose (prise/sautée)")
        void shouldHandleDoseAction() {
            DoseActionRequest request = new DoseActionRequest("take", null, null);
            DoseLogDto takenDto = new DoseLogDto(
                    1L, 1L, "Amlodipine", "5mg",
                    LocalDate.now(), LocalTime.of(8, 0), DoseStatus.TAKEN,
                    Instant.now(), 5, "Pris avec repas", null, true
            );

            when(doseLogService.handleDoseAction(1L, request)).thenReturn(takenDto);

            ResponseEntity<DoseLogDto> response = controller.handleDoseAction(1L, request);

            assertThat(response.getBody().status()).isEqualTo(DoseStatus.TAKEN);
        }

        @Test
        @DisplayName("CT_DOSE_03b: Action rapide - marquer comme prise")
        void shouldTakeDose() {
            DoseLogDto takenDto = new DoseLogDto(
                    1L, 1L, "Amlodipine", "5mg",
                    LocalDate.now(), LocalTime.of(8, 0), DoseStatus.TAKEN,
                    Instant.now(), 0, null, null, true
            );

            when(doseLogService.takeDose(1L, null)).thenReturn(takenDto);

            ResponseEntity<DoseLogDto> response = controller.takeDose(1L, null);

            assertThat(response.getBody().status()).isEqualTo(DoseStatus.TAKEN);
        }
    }

    @Nested
    @DisplayName("CT_DOSE_04: Statistiques d'adhésion")
    class AdherenceStatsTests {

        @Test
        @DisplayName("CT_DOSE_04a: Récupère les statistiques d'adhésion")
        void shouldGetAdherenceStats() {
            AdherenceStatsDto stats = new AdherenceStatsDto(
                    1L, "Patient Test", 85.5, 7, 14,
                    100L, 15L, 5L, 90.0, 85.0,
                    List.of(), Map.of("Morning", 95.0, "Evening", 80.0),
                    Map.of("FORGOT", 10L, "SIDE_EFFECTS", 5L), 2.5
            );

            when(doseLogService.getAdherenceStats(1L)).thenReturn(stats);

            ResponseEntity<AdherenceStatsDto> response = controller.getAdherenceStats(1L);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().overallAdherenceRate()).isEqualTo(85.5);
            assertThat(response.getBody().currentStreak()).isEqualTo(7);
        }
    }
}
