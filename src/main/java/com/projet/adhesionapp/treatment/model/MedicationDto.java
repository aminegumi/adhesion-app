package com.projet.adhesionapp.treatment.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for Medication entity.
 */
public record MedicationDto(
        Long id,
        Long planId,
        String name,
        String dosage,
        String instructions,
        Integer timesPerDay,
        Boolean isChronic,
        LocalDate startDate,
        LocalDate endDate,
        List<String> scheduledTimes,
        String notes,
        Boolean notificationsEnabled,
        Integer reminderMinutesBefore,
        Instant createdAt) {
}
