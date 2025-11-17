package com.projet.adhesionapp.analytics.model;

import java.time.LocalDate;

/**
 * Requête simplifiée pour la V1 :
 * - profileScore : score psychométrique global (0-100)
 * - recentAdherenceRate : taux d'adhésion récent (0-1)
 */
public record PredictRequest(
        Long userId,
        LocalDate date,
        double profileScore,
        double recentAdherenceRate
) {
}
