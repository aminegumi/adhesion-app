package com.projet.adhesionapp.treatment.model;

import java.time.Instant;
import java.time.LocalDate;

public record DailyTaskDto(
        Long id,
        Long planId,
        String title,
        String description,
        String category,
        LocalDate scheduledDate,
        String timeOfDay,
        Integer durationMinutes,
        Boolean isRecurring,
        Boolean completed,
        Instant completedAt,
        String notes) {
}
