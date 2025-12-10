package com.projet.adhesionapp.treatment.model;

import com.projet.adhesionapp.treatment.domain.UserMedication;
import com.projet.adhesionapp.treatment.domain.UserMedication.MedicationForm;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for user medication
 */
public record UserMedicationDto(
        Long id,
        Long userId,
        String name,
        String dosage,
        MedicationForm form,
        Integer frequencyPerDay,
        List<String> scheduledTimes,
        String prescribedBy,
        String instructions,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isChronic,
        Integer currentStock,
        Integer lowStockThreshold,
        Boolean active,
        Boolean remindersEnabled,
        Integer reminderMinutesBefore,
        String notes,
        String reason,
        String color,
        Boolean isCurrentlyActive) {
    public static UserMedicationDto fromEntity(UserMedication med) {
        return new UserMedicationDto(
                med.getId(),
                med.getUser().getId(),
                med.getName(),
                med.getDosage(),
                med.getForm(),
                med.getFrequencyPerDay(),
                med.getScheduledTimesList().stream().map(Object::toString).toList(),
                med.getPrescribedBy(),
                med.getInstructions(),
                med.getStartDate(),
                med.getEndDate(),
                med.getIsChronic(),
                med.getCurrentStock(),
                med.getLowStockThreshold(),
                med.getActive(),
                med.getRemindersEnabled(),
                med.getReminderMinutesBefore(),
                med.getNotes(),
                med.getReason(),
                med.getColor(),
                med.isCurrentlyActive());
    }
}
