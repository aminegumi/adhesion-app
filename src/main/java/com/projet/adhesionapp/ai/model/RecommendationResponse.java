package com.projet.adhesionapp.ai.model;

public record RecommendationResponse(
        String recommendations,
        Long userId) {
}
