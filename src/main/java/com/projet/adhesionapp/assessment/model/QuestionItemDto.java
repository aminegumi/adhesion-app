package com.projet.adhesionapp.assessment.model;

/**
 * DTO for question items in a psychological test
 */
public record QuestionItemDto(
        Long id,
        String code,
        String text,
        int minScore,
        int maxScore,
        int orderIndex,
        boolean reverseScored) {
}