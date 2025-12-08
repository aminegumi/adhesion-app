package com.projet.adhesionapp.assessment.domain;

/**
 * Status of a psychological test session
 */
public enum TestSessionStatus {
    IN_PROGRESS, // Test started but not completed
    COMPLETED, // All questions answered
    SCORED, // Score calculated
    INTERPRETED, // Clinical interpretation generated
    CANCELLED // Test abandoned
}
