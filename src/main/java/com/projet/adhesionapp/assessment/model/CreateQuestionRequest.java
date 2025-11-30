package com.projet.adhesionapp.assessment.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionRequest(
        @NotNull Long testId,
        @NotBlank String code,
        @NotBlank String text,
        @NotNull Integer orderIndex,
        @NotNull Boolean reverseScored,
        @NotNull Integer minScore,
        @NotNull Integer maxScore) {
}