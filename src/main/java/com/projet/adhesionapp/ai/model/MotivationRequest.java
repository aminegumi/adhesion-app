package com.projet.adhesionapp.ai.model;

import jakarta.validation.constraints.NotNull;

public record MotivationRequest(
        @NotNull Long userId,
        Double adherenceScore) {
}
