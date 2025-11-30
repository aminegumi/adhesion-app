package com.projet.adhesionapp.assessment.model;

import jakarta.validation.constraints.NotBlank;

public record CreateTestRequest(
        @NotBlank String code,
        @NotBlank String title,
        @NotBlank String version,
        boolean active) {
}