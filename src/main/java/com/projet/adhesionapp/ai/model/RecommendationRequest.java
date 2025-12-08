package com.projet.adhesionapp.ai.model;

import jakarta.validation.constraints.NotNull;

public record RecommendationRequest(
        @NotNull Long userId,
        Double nonAdherenceRisk) {
}
