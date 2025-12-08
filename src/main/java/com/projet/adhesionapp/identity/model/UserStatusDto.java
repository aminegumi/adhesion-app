package com.projet.adhesionapp.identity.model;

import java.time.Instant;

/**
 * DTO representing the user's current status in the app flow.
 */
public record UserStatusDto(
        Long userId,
        String displayName,
        String email,
        boolean onboardingCompleted,
        boolean needsRetake,
        int requiredTestsCount,
        int completedTestsCount,
        int testsRemaining,
        Instant lastTestCompletedAt,
        String nextAction) {
    /**
     * Determine the next action the user should take.
     */
    public static String determineNextAction(boolean onboardingCompleted, boolean needsRetake, int testsRemaining) {
        if (!onboardingCompleted) {
            if (testsRemaining > 0) {
                return "TAKE_TESTS";
            } else {
                return "CREATE_PROFILE";
            }
        }
        if (needsRetake) {
            return "RETAKE_TESTS";
        }
        return "FULL_ACCESS";
    }
}
