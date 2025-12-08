package com.projet.adhesionapp.assessment.model;

import java.time.Instant;
import java.util.List;

/**
 * DTO for returning test results with interpretation
 */
public record TestResultDto(
        Long sessionId,
        String testCode,
        String testTitle,
        Integer totalScore,
        String interpretationLevel,
        String scoreDescription,
        String clinicalInterpretation,
        List<String> recommendations,
        Instant completedAt) {
}
