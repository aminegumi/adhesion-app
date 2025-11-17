package com.projet.adhesionapp.habit.model;

import java.time.LocalDate;

public record PlanDto(
        Long id,
        Long userId,
        LocalDate date,
        Long primaryActionId,
        Long altActionId
) {
}
