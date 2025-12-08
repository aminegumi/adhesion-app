package com.projet.adhesionapp.ai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TestAnalysisRequest(
        @NotBlank String testName,
        @NotNull List<String> dimensions,
        @NotNull List<Double> scores,
        List<String> answers) {
}
