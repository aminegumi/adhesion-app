package com.projet.adhesionapp.assessment.model;

import java.time.Instant;
import java.util.List;

public record TestSessionDto(
        Long id,
        Long testId,
        String testCode,
        String testTitle,
        Instant startedAt,
        List<QuestionItemDto> questions) {
}