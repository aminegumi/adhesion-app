package com.projet.adhesionapp.assessment.model;

import jakarta.validation.constraints.NotNull;

public record StartSessionRequest(
        @NotNull Long testId,
        @NotNull Long userId) {
}
