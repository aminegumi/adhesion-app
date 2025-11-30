package com.projet.adhesionapp.assessment.model;

import jakarta.validation.constraints.NotBlank;

public record UpdateTestRequest(
        @NotBlank String title,
        @NotBlank String version,
        boolean active) {
}