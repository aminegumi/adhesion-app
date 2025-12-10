package com.projet.adhesionapp.analytics.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive adherence prediction result with all contributing factors.
 */
public record AdherencePredictionDto(
        Long userId,
        String userName,

        /**
         * Predicted adherence probability (0.0 - 1.0)
         * Based on weighted multi-factor model
         */
        double predictedAdherence,

        /**
         * Risk classification: Low, Moderate, High, Very High
         */
        String riskLevel,

        /**
         * Confidence in the prediction (0.0 - 1.0)
         * Higher when more data is available
         */
        double confidence,

        /**
         * List of identified risk factors with severity and recommendations
         */
        List<RiskFactorDto> riskFactors,

        /**
         * Breakdown of component scores used in prediction
         */
        Map<String, Double> componentScores,

        /**
         * Current streak of consecutive taken doses
         */
        int currentStreak,

        /**
         * Best ever streak of consecutive taken doses
         */
        int bestStreak,

        /**
         * Breakdown of skip reasons from behavioral data
         */
        Map<String, Long> skipReasonBreakdown,

        /**
         * AI-generated personalized recommendations
         */
        String recommendations,

        /**
         * Date of prediction
         */
        LocalDate predictionDate) {
    /**
     * Get adherence percentage (0-100)
     */
    public double getAdherencePercentage() {
        return predictedAdherence * 100;
    }

    /**
     * Check if user is at high risk for non-adherence
     */
    public boolean isHighRisk() {
        return "High".equals(riskLevel) || "Very High".equals(riskLevel);
    }

    /**
     * Get number of high severity risk factors
     */
    public long getHighSeverityFactorCount() {
        return riskFactors.stream()
                .filter(r -> "high".equals(r.severity()))
                .count();
    }
}
