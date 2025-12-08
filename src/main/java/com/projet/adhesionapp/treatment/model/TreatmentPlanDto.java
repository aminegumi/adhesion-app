package com.projet.adhesionapp.treatment.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TreatmentPlanDto(
                Long id,
                Long userId,
                String userName,
                Long profileId,
                String title,
                String description,
                String planContent,
                Integer durationWeeks,
                LocalDate startDate,
                LocalDate endDate,
                String medications,
                List<MedicationDto> medicationsList,
                String identifiedIssues,
                Integer progressPercentage,
                String status,
                Instant createdAt) {
}
