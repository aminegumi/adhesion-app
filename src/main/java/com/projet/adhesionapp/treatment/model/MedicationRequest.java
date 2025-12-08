package com.projet.adhesionapp.treatment.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating/updating a medication.
 */
public record MedicationRequest(
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
        Integer reminderMinutesBefore) {
}
