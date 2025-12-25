package com.projet.adhesionapp.analytics.service;

import com.projet.adhesionapp.analytics.domain.Prediction;
import com.projet.adhesionapp.analytics.model.PredictRequest;
import com.projet.adhesionapp.analytics.repo.PredictionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Prediction Service Tests")
class PredictionServiceTest {

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private UserService userService;

    @Mock
    private FeatureBuilderService featureBuilderService;

    @Mock
    private ExplanationService explanationService;

    @InjectMocks
    private PredictionService predictionService;

    private User testUser;
    private PredictRequest testRequest;
    private Prediction testPrediction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordHash("hashed")
                .displayName("Jean Dupont")
                .birthDate(java.time.LocalDate.of(1990, 1, 1))
                .gender("M")
                .active(true)
                .build();

        testRequest = new PredictRequest(
                1L,
                LocalDate.now(),
                75.0,  // profileScore
                0.8,   // recentAdherenceRate
                0.6,   // testCompletionRate
                70.0,  // averageTestScore
                0.5,   // planProgressRate
                2,     // activePlansCount
                14     // daysStreak
        );

        testPrediction = Prediction.builder()
                .id(1L)
                .user(testUser)
                .date(LocalDate.now())
                .probNonAdherence(0.25)
                .modelVersion("v1-baseline")
                .topFeaturesJson("{}")
                .build();
    }

    @Nested
    @DisplayName("Predict And Save Tests")
    class PredictAndSaveTests {

        @Test
        @DisplayName("Should predict and save successfully")
        void shouldPredictAndSaveSuccessfully() {
            // Given
            Map<String, Double> features = Map.of(
                    "profileScore", 75.0,
                    "recentAdherenceRate", 0.8,
                    "testCompletionRate", 0.6,
                    "averageTestScore", 70.0,
                    "planProgressRate", 0.5,
                    "activePlansCount", 2.0,
                    "daysStreak", 14.0
            );

            when(userService.findById(1L)).thenReturn(testUser);
            when(featureBuilderService.buildFeatures(testRequest)).thenReturn(features);
            when(explanationService.explain(any(), anyDouble())).thenReturn("{\"topFactors\":[]}");
            when(predictionRepository.save(any(Prediction.class))).thenAnswer(i -> {
                Prediction p = i.getArgument(0);
                p.setId(1L);
                return p;
            });

            // When
            Prediction result = predictionService.predictAndSave(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getDate()).isEqualTo(LocalDate.now());
            assertThat(result.getModelVersion()).isEqualTo("v1-baseline");
            assertThat(result.getProbNonAdherence()).isBetween(0.0, 1.0);

            verify(predictionRepository).save(any(Prediction.class));
        }

        @Test
        @DisplayName("Should use current date when request date is null")
        void shouldUseCurrentDateWhenRequestDateIsNull() {
            // Given
            PredictRequest requestWithNullDate = new PredictRequest(
                    1L, null, 75.0, 0.8, 0.6, 70.0, 0.5, 2, 14
            );

            Map<String, Double> features = Map.of(
                    "profileScore", 75.0,
                    "recentAdherenceRate", 0.8
            );

            when(userService.findById(1L)).thenReturn(testUser);
            when(featureBuilderService.buildFeatures(requestWithNullDate)).thenReturn(features);
            when(explanationService.explain(any(), anyDouble())).thenReturn("{}");
            when(predictionRepository.save(any(Prediction.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Prediction result = predictionService.predictAndSave(requestWithNullDate);

            // Then
            assertThat(result.getDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Should calculate high risk for poor features")
        void shouldCalculateHighRiskForPoorFeatures() {
            // Given
            Map<String, Double> poorFeatures = Map.of(
                    "profileScore", 20.0,           // Low profile score
                    "recentAdherenceRate", 0.3,     // Low adherence
                    "testCompletionRate", 0.0,      // No tests completed
                    "averageTestScore", 30.0,       // Low test scores
                    "planProgressRate", 0.0,        // No plan progress
                    "activePlansCount", 0.0,        // No active plans
                    "daysStreak", 0.0               // No streak
            );

            PredictRequest poorRequest = new PredictRequest(
                    1L, LocalDate.now(), 20.0, 0.3, 0.0, 30.0, 0.0, 0, 0
            );

            when(userService.findById(1L)).thenReturn(testUser);
            when(featureBuilderService.buildFeatures(poorRequest)).thenReturn(poorFeatures);
            when(explanationService.explain(any(), anyDouble())).thenReturn("{}");
            when(predictionRepository.save(any(Prediction.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Prediction result = predictionService.predictAndSave(poorRequest);

            // Then
            assertThat(result.getProbNonAdherence()).isGreaterThan(0.5);
        }

        @Test
        @DisplayName("Should calculate low risk for excellent features")
        void shouldCalculateLowRiskForExcellentFeatures() {
            // Given
            Map<String, Double> excellentFeatures = Map.of(
                    "profileScore", 95.0,
                    "recentAdherenceRate", 0.95,
                    "testCompletionRate", 1.0,
                    "averageTestScore", 90.0,
                    "planProgressRate", 0.9,
                    "activePlansCount", 3.0,
                    "daysStreak", 30.0
            );

            PredictRequest excellentRequest = new PredictRequest(
                    1L, LocalDate.now(), 95.0, 0.95, 1.0, 90.0, 0.9, 3, 30
            );

            when(userService.findById(1L)).thenReturn(testUser);
            when(featureBuilderService.buildFeatures(excellentRequest)).thenReturn(excellentFeatures);
            when(explanationService.explain(any(), anyDouble())).thenReturn("{}");
            when(predictionRepository.save(any(Prediction.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Prediction result = predictionService.predictAndSave(excellentRequest);

            // Then
            assertThat(result.getProbNonAdherence()).isLessThan(0.4);
        }

        @Test
        @DisplayName("Should handle missing optional features")
        void shouldHandleMissingOptionalFeatures() {
            // Given
            Map<String, Double> minimalFeatures = new HashMap<>();
            minimalFeatures.put("profileScore", 50.0);
            // Other features will use defaults

            when(userService.findById(1L)).thenReturn(testUser);
            when(featureBuilderService.buildFeatures(testRequest)).thenReturn(minimalFeatures);
            when(explanationService.explain(any(), anyDouble())).thenReturn("{}");
            when(predictionRepository.save(any(Prediction.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Prediction result = predictionService.predictAndSave(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getProbNonAdherence()).isBetween(0.0, 1.0);
        }
    }

    @Nested
    @DisplayName("Get Last Predictions Tests")
    class GetLastPredictionsTests {

        @Test
        @DisplayName("Should return last predictions for user")
        void shouldReturnLastPredictionsForUser() {
            // Given
            List<Prediction> predictions = List.of(
                    Prediction.builder().id(1L).user(testUser).probNonAdherence(0.3).build(),
                    Prediction.builder().id(2L).user(testUser).probNonAdherence(0.25).build()
            );

            when(userService.findById(1L)).thenReturn(testUser);
            when(predictionRepository.findTop10ByUserOrderByDateDesc(testUser)).thenReturn(predictions);

            // When
            List<Prediction> result = predictionService.getLastPredictionsForUser(1L);

            // Then
            assertThat(result).hasSize(2);
            verify(predictionRepository).findTop10ByUserOrderByDateDesc(testUser);
        }

        @Test
        @DisplayName("Should return empty list when no predictions")
        void shouldReturnEmptyListWhenNoPredictions() {
            // Given
            when(userService.findById(1L)).thenReturn(testUser);
            when(predictionRepository.findTop10ByUserOrderByDateDesc(testUser)).thenReturn(Collections.emptyList());

            // When
            List<Prediction> result = predictionService.getLastPredictionsForUser(1L);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get By ID Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return prediction by id")
        void shouldReturnPredictionById() {
            // Given
            when(predictionRepository.findById(1L)).thenReturn(Optional.of(testPrediction));

            // When
            Prediction result = predictionService.getById(1L);

            // Then
            assertThat(result).isEqualTo(testPrediction);
        }

        @Test
        @DisplayName("Should throw NotFoundException when prediction not found")
        void shouldThrowNotFoundExceptionWhenPredictionNotFound() {
            // Given
            when(predictionRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> predictionService.getById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Prédiction introuvable");
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete prediction by id")
        void shouldDeletePredictionById() {
            // When
            predictionService.deleteById(1L);

            // Then
            verify(predictionRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should delete all predictions for user")
        void shouldDeleteAllPredictionsForUser() {
            // Given
            List<Prediction> predictions = List.of(testPrediction);
            when(userService.findById(1L)).thenReturn(testUser);
            when(predictionRepository.findTop10ByUserOrderByDateDesc(testUser)).thenReturn(predictions);

            // When
            predictionService.deleteAllForUser(1L);

            // Then
            verify(predictionRepository).deleteAll(predictions);
        }
    }
}


@ExtendWith(MockitoExtension.class)
@DisplayName("Feature Builder Service Tests")
class FeatureBuilderServiceTest {

    private FeatureBuilderService featureBuilderService;

    @BeforeEach
    void setUp() {
        featureBuilderService = new FeatureBuilderService();
    }

    @Nested
    @DisplayName("Build Features Tests")
    class BuildFeaturesTests {

        @Test
        @DisplayName("Should build all features from request")
        void shouldBuildAllFeaturesFromRequest() {
            // Given
            PredictRequest request = new PredictRequest(
                    1L,
                    LocalDate.now(),
                    75.0,   // profileScore
                    0.8,    // recentAdherenceRate
                    0.6,    // testCompletionRate
                    70.0,   // averageTestScore
                    0.5,    // planProgressRate
                    2,      // activePlansCount
                    14      // daysStreak
            );

            // When
            Map<String, Double> features = featureBuilderService.buildFeatures(request);

            // Then
            assertThat(features).containsEntry("profileScore", 75.0);
            assertThat(features).containsEntry("recentAdherenceRate", 0.8);
            assertThat(features).containsEntry("testCompletionRate", 0.6);
            assertThat(features).containsEntry("averageTestScore", 70.0);
            assertThat(features).containsEntry("planProgressRate", 0.5);
            assertThat(features).containsEntry("activePlansCount", 2.0);
            assertThat(features).containsEntry("daysStreak", 14.0);
        }

        @Test
        @DisplayName("Should handle zero values")
        void shouldHandleZeroValues() {
            // Given
            PredictRequest request = new PredictRequest(
                    1L, LocalDate.now(), 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0
            );

            // When
            Map<String, Double> features = featureBuilderService.buildFeatures(request);

            // Then
            assertThat(features).containsEntry("profileScore", 0.0);
            assertThat(features).containsEntry("recentAdherenceRate", 0.0);
            assertThat(features).containsEntry("activePlansCount", 0.0);
            assertThat(features).containsEntry("daysStreak", 0.0);
        }

        @Test
        @DisplayName("Should handle maximum values")
        void shouldHandleMaximumValues() {
            // Given
            PredictRequest request = new PredictRequest(
                    1L, LocalDate.now(), 100.0, 1.0, 1.0, 100.0, 1.0, 10, 365
            );

            // When
            Map<String, Double> features = featureBuilderService.buildFeatures(request);

            // Then
            assertThat(features).containsEntry("profileScore", 100.0);
            assertThat(features).containsEntry("recentAdherenceRate", 1.0);
            assertThat(features).containsEntry("daysStreak", 365.0);
        }

        @Test
        @DisplayName("Should return correct feature count")
        void shouldReturnCorrectFeatureCount() {
            // Given
            PredictRequest request = new PredictRequest(
                    1L, LocalDate.now(), 50.0, 0.5, 0.5, 50.0, 0.5, 1, 7
            );

            // When
            Map<String, Double> features = featureBuilderService.buildFeatures(request);

            // Then
            assertThat(features).hasSize(7);
        }
    }
}
