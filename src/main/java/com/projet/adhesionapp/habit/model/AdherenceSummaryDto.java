package com.projet.adhesionapp.habit.model;

import java.time.LocalDate;

public record AdherenceSummaryDto(
        LocalDate date,
        double completionRate,
        double adherenceScore
) {
}
