package com.projet.adhesionapp.analytics.model;

import java.time.LocalDate;

/**
 * Enhanced prediction request with multiple factors:
 * - profileScore: psychological profile score (0-100)
 * - recentAdherenceRate: recent medication adherence rate (0-1)
 * - testCompletionRate: percentage of recommended tests completed (0-1)
 * - averageTestScore: average score from psychological tests (0-100)
 * - planProgressRate: treatment plan completion progress (0-1)
 * - activePlansCount: number of active treatment plans
 * - daysStreak: current adherence streak in days
 */
public record PredictRequest(
        Long userId,
        LocalDate date,
        double profileScore,
        double recentAdherenceRate,
        double testCompletionRate,
        double averageTestScore,
        double planProgressRate,
        int activePlansCount,
        int daysStreak
) {
    // Default constructor for backward compatibility
    public PredictRequest(Long userId, LocalDate date, double profileScore, double recentAdherenceRate) {
        this(userId, date, profileScore, recentAdherenceRate, 0.0, 50.0, 0.0, 0, 0);
    }
}
