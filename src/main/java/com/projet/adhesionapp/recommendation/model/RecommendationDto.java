package com.projet.adhesionapp.recommendation.model;

import java.time.Instant;

public record RecommendationDto(
        Long id,
        Long userId,
        Long profileId,
        String category,
        Integer priority,
        String title,
        String description,
        String actionableSteps,
        String expectedBenefits,
        String timeFrame,
        String difficulty,
        String status,
        Boolean completed,
        Instant createdAt,
        Instant completedAt) {
}
