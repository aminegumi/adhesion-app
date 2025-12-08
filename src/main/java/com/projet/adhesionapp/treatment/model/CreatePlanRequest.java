package com.projet.adhesionapp.treatment.model;

import java.time.LocalDate;
import java.util.List;

public record CreatePlanRequest(
                Long userId,
                String title,
                String description,
                List<String> identifiedIssues,
                String medications,
                List<MedicationRequest> medicationsList,
                Integer durationWeeks,
                LocalDate startDate) {
}
