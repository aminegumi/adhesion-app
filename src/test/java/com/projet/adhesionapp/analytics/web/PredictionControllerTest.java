package com.projet.adhesionapp.analytics.web;

import com.projet.adhesionapp.analytics.domain.Prediction;
import com.projet.adhesionapp.analytics.model.*;
import com.projet.adhesionapp.analytics.service.AdherencePredictionService;
import com.projet.adhesionapp.analytics.service.PredictionService;
import com.projet.adhesionapp.identity.domain.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur de prédictions.
 * CT_PRED_01 - CT_PRED_04: Prédictions d'adhésion
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Prediction Controller - Tests unitaires")
class PredictionControllerTest {

    @Mock private PredictionService predictionService;
    @Mock private AdherencePredictionService adherencePredictionService;
    @InjectMocks private PredictionController controller;

    private AdherencePredictionDto testPrediction;
    private Prediction prediction;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("patient@test.com").build();

        testPrediction = new AdherencePredictionDto(
                1L, "Patient Test", 0.85, "Low", 0.9,
                List.of(), Map.of("psychological", 0.75, "behavioral", 0.90),
                7, 14, Map.of("FORGOT", 2L),
                "Continuez vos bonnes habitudes", LocalDate.now()
        );

        prediction = Prediction.builder()
                .id(1L)
                .user(testUser)
                .date(LocalDate.now())
                .probNonAdherence(0.15)
                .modelVersion("1.0")
                .topFeaturesJson("{}")
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("CT_PRED_01: Prédiction améliorée")
    class AdherencePredictionTests {

        @Test
        @DisplayName("CT_PRED_01a: Calcule la prédiction multi-facteur")
        void shouldCalculateAdherencePrediction() {
            when(adherencePredictionService.predictAdherence(1L)).thenReturn(testPrediction);

            AdherencePredictionDto result = controller.predictAdherence(1L);

            assertThat(result.predictedAdherence()).isEqualTo(0.85);
            assertThat(result.riskLevel()).isEqualTo("Low");
            assertThat(result.currentStreak()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("CT_PRED_02: Prédiction simple")
    class SimplePredictionTests {

        @Test
        @DisplayName("CT_PRED_02a: Sauvegarde une prédiction")
        void shouldSavePrediction() {
            PredictRequest request = new PredictRequest(
                    1L, LocalDate.now(), 75.0, 0.85, 0.8, 65.0, 0.6, 2, 7
            );

            when(predictionService.predictAndSave(request)).thenReturn(prediction);

            PredictionDto result = controller.predict(request);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.probNonAdherence()).isEqualTo(0.15);
        }
    }

    @Nested
    @DisplayName("CT_PRED_03: Historique")
    class HistoryTests {

        @Test
        @DisplayName("CT_PRED_03a: Récupère l'historique des prédictions")
        void shouldGetPredictionHistory() {
            when(predictionService.getLastPredictionsForUser(1L)).thenReturn(List.of(prediction));

            List<PredictionDto> result = controller.history(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).userId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("CT_PRED_04: Suppression")
    class DeleteTests {

        @Test
        @DisplayName("CT_PRED_04a: Supprime une prédiction")
        void shouldDeletePrediction() {
            doNothing().when(predictionService).deleteById(1L);

            controller.delete(1L);

            verify(predictionService).deleteById(1L);
        }

        @Test
        @DisplayName("CT_PRED_04b: Supprime toutes les prédictions d'un utilisateur")
        void shouldDeleteAllForUser() {
            doNothing().when(predictionService).deleteAllForUser(1L);

            controller.deleteAllForUser(1L);

            verify(predictionService).deleteAllForUser(1L);
        }
    }
}
