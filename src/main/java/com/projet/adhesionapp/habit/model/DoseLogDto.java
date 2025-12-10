package com.projet.adhesionapp.habit.model;

import com.projet.adhesionapp.habit.domain.DoseLog.DoseStatus;
import com.projet.adhesionapp.habit.domain.DoseLog.SkipReason;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for dose log data
 */
public record DoseLogDto(
        Long id,
        Long medicationId,
        String medicationName,
        String dosage,
        LocalDate scheduledDate,
        LocalTime scheduledTime,
        DoseStatus status,
        Instant takenAt,
        Integer delayMinutes,
        String notes,
        SkipReason skipReason,
        Boolean reminderSent) {
}
