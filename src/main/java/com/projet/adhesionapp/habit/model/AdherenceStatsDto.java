package com.projet.adhesionapp.habit.model;

import java.util.List;
import java.util.Map;

/**
 * Detailed adherence statistics for a user
 */
public record AdherenceStatsDto(
        Long userId,
        String userName,

        // Overall stats
        Double overallAdherenceRate,
        Integer currentStreak,
        Integer longestStreak,
        Long totalDosesTaken,
        Long totalDosesMissed,
        Long totalDosesSkipped,

        // Period breakdown
        Double last7DaysRate,
        Double last30DaysRate,

        // By medication
        List<MedicationAdherenceDto> byMedication,

        // By time of day
        Map<String, Double> byTimeOfDay,

        // Skip reasons analysis
        Map<String, Long> skipReasonCounts,

        // Trend (positive = improving, negative = declining)
        Double trendPercentage) {
    public record MedicationAdherenceDto(
            Long medicationId,
            String medicationName,
            Double adherenceRate,
            Long dosesTaken,
            Long dosesTotal) {
    }
}
