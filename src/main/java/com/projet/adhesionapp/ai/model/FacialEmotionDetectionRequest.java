package com.projet.adhesionapp.ai.model;

import jakarta.validation.constraints.NotNull;

public record FacialEmotionDetectionRequest(
    @NotNull Long userId,
    @NotNull byte[] imageBytes
) {}
