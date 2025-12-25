package com.projet.adhesionapp.analytics.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Explanation Service Tests")
class ExplanationServiceTest {

    private ExplanationService explanationService;

    @BeforeEach
    void setUp() {
        explanationService = new ExplanationService();
    }

    @Nested
    @DisplayName("Explain Tests")
    class ExplainTests {

        @Test
        @DisplayName("Should generate high risk explanation")
        void shouldGenerateHighRiskExplanation() {
            // Given
            Map<String, Double> features = Map.of(
                    "profileScore", 30.0,
                    "recentAdherenceRate", 0.3
            );
            double probNonAdherence = 0.8;

            // When
            String result = explanationService.explain(features, probNonAdherence);

            // Then
            assertThat(result).contains("profileScore");
            assertThat(result).contains("recentAdherenceRate");
            assertThat(result).contains("Risque élevé");
        }

        @Test
        @DisplayName("Should generate moderate risk explanation")
        void shouldGenerateModerateRiskExplanation() {
            // Given
            Map<String, Double> features = Map.of(
                    "profileScore", 60.0,
                    "recentAdherenceRate", 0.6
            );
            double probNonAdherence = 0.5;

            // When
            String result = explanationService.explain(features, probNonAdherence);

            // Then
            assertThat(result).contains("Risque modéré");
        }

        @Test
        @DisplayName("Should generate low risk explanation")
        void shouldGenerateLowRiskExplanation() {
            // Given
            Map<String, Double> features = Map.of(
                    "profileScore", 85.0,
                    "recentAdherenceRate", 0.9
            );
            double probNonAdherence = 0.2;

            // When
            String result = explanationService.explain(features, probNonAdherence);

            // Then
            assertThat(result).contains("Risque faible");
        }

        @Test
        @DisplayName("Should handle missing features with defaults")
        void shouldHandleMissingFeaturesWithDefaults() {
            // Given
            Map<String, Double> emptyFeatures = new HashMap<>();
            double probNonAdherence = 0.5;

            // When
            String result = explanationService.explain(emptyFeatures, probNonAdherence);

            // Then
            assertThat(result).contains("profileScore");
            assertThat(result).contains("0.0"); // Default value
        }

        @Test
        @DisplayName("Should return valid JSON format")
        void shouldReturnValidJsonFormat() {
            // Given
            Map<String, Double> features = Map.of(
                    "profileScore", 70.0,
                    "recentAdherenceRate", 0.75
            );
            double probNonAdherence = 0.35;

            // When
            String result = explanationService.explain(features, probNonAdherence);

            // Then
            assertThat(result).startsWith("{");
            assertThat(result).endsWith("}");
            assertThat(result).contains("\"profileScore\":");
            assertThat(result).contains("\"recentAdherenceRate\":");
            assertThat(result).contains("\"comment\":");
        }

        @Test
        @DisplayName("Should handle boundary probability 0.7")
        void shouldHandleBoundaryProbability07() {
            // Given
            Map<String, Double> features = Map.of("profileScore", 50.0, "recentAdherenceRate", 0.5);

            // When
            String result = explanationService.explain(features, 0.7);

            // Then
            assertThat(result).contains("Risque élevé");
        }

        @Test
        @DisplayName("Should handle boundary probability 0.4")
        void shouldHandleBoundaryProbability04() {
            // Given
            Map<String, Double> features = Map.of("profileScore", 50.0, "recentAdherenceRate", 0.5);

            // When
            String result = explanationService.explain(features, 0.4);

            // Then
            assertThat(result).contains("Risque modéré");
        }

        @Test
        @DisplayName("Should handle probability just below 0.4")
        void shouldHandleProbabilityJustBelow04() {
            // Given
            Map<String, Double> features = Map.of("profileScore", 80.0, "recentAdherenceRate", 0.85);

            // When
            String result = explanationService.explain(features, 0.39);

            // Then
            assertThat(result).contains("Risque faible");
        }

        @Test
        @DisplayName("Should handle zero probability")
        void shouldHandleZeroProbability() {
            // Given
            Map<String, Double> features = Map.of("profileScore", 100.0, "recentAdherenceRate", 1.0);

            // When
            String result = explanationService.explain(features, 0.0);

            // Then
            assertThat(result).contains("Risque faible");
        }

        @Test
        @DisplayName("Should handle maximum probability")
        void shouldHandleMaximumProbability() {
            // Given
            Map<String, Double> features = Map.of("profileScore", 10.0, "recentAdherenceRate", 0.1);

            // When
            String result = explanationService.explain(features, 1.0);

            // Then
            assertThat(result).contains("Risque élevé");
        }

        @Test
        @DisplayName("Should include correct profile score in output")
        void shouldIncludeCorrectProfileScoreInOutput() {
            // Given
            Map<String, Double> features = Map.of("profileScore", 75.5, "recentAdherenceRate", 0.8);

            // When
            String result = explanationService.explain(features, 0.3);

            // Then
            assertThat(result).contains("75.5");
        }

        @Test
        @DisplayName("Should include correct adherence rate in output")
        void shouldIncludeCorrectAdherenceRateInOutput() {
            // Given
            Map<String, Double> features = Map.of("profileScore", 60.0, "recentAdherenceRate", 0.85);

            // When
            String result = explanationService.explain(features, 0.3);

            // Then
            assertThat(result).contains("0.85");
        }
    }
}
