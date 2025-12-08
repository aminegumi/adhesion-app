package com.projet.adhesionapp.profile.model;

import java.util.List;

public record ProfileCreateRequest(
        Long userId,
        List<Long> sessionIds,
        Double anxietyScore,
        Double depressionScore,
        Double motivationScore,
        Double selfEfficacyScore,
        Double socialSupportScore,
        Double healthLocusScore) {
}
