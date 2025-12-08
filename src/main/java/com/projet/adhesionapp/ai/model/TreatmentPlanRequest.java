package com.projet.adhesionapp.ai.model;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TreatmentPlanRequest(
        @NotNull Long userId,
        List<String> identifiedIssues,
        String currentMedications) {
}
