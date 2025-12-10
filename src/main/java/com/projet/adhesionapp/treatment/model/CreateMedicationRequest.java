package com.projet.adhesionapp.treatment.model;

import com.projet.adhesionapp.treatment.domain.UserMedication.MedicationForm;

import java.time.LocalDate;
import java.util.List;

/**
 * Request to create a new user medication
 */
public record CreateMedicationRequest(
        Long userId,
        String name,
        String dosage,
        MedicationForm form,
        Integer frequencyPerDay,
        List<String> scheduledTimes, // e.g., ["08:00", "20:00"]
        String prescribedBy,
        String instructions,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isChronic,
        Integer currentStock,
        Integer lowStockThreshold,
        Boolean remindersEnabled,
        Integer reminderMinutesBefore,
        String notes,
        String reason,
        String color) {
    public CreateMedicationRequest {
        // Default values
        if (frequencyPerDay == null)
            frequencyPerDay = 1;
        if (form == null)
            form = MedicationForm.TABLET;
        if (isChronic == null)
            isChronic = false;
        if (remindersEnabled == null)
            remindersEnabled = true;
        if (reminderMinutesBefore == null)
            reminderMinutesBefore = 15;
    }
}
