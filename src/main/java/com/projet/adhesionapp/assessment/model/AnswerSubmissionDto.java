package com.projet.adhesionapp.assessment.model;

/**
 * DTO for submitting an answer to a question
 */
public record AnswerSubmissionDto(
        Long questionId,
        int score,
        String textResponse) {
}
