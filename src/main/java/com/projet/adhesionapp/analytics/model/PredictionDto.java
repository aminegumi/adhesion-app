package com.projet.adhesionapp.analytics.model;

import java.time.Instant;
import java.time.LocalDate;


public record PredictionDto(
        Long id,
        Long userId,
        LocalDate date,
        double probNonAdherence,
        String modelVersion,
        String topFeaturesJson,
        Instant createdAt
) {
}