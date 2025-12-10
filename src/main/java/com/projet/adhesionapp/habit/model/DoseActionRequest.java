package com.projet.adhesionapp.habit.model;

import com.projet.adhesionapp.habit.domain.DoseLog.SkipReason;

/**
 * Request for marking a dose as taken or skipped
 */
public record DoseActionRequest(
        String action, // "take" or "skip"
        String notes, // Optional notes
        SkipReason skipReason // Required if action is "skip"
) {
}
