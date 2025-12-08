package com.projet.adhesionapp.profile.model;

import java.time.Instant;

public record ProfileDto(
        Long id,
        Long userId,
        String userName,
        String profileType,
        Double anxietyScore,
        Double depressionScore,
        Double motivationScore,
        Double selfEfficacyScore,
        Double socialSupportScore,
        Double healthLocusScore,
        Double adherenceRiskScore,
        String summary,
        String detailedInterpretation,
        String status,
        Instant createdAt) {
}
