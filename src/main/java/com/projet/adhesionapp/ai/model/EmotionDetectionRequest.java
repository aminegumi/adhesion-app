package com.projet.adhesionapp.ai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmotionDetectionRequest(
    @NotNull Long userId,
    @NotBlank String text
) {}
