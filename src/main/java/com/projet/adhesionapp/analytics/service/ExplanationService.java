package com.projet.adhesionapp.analytics.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Génère une explication textuelle simple à partir des features.
 */
@Service
public class ExplanationService {

    public String explain(Map<String, Double> features, double probNonAdherence) {
        double profile = features.getOrDefault("profileScore", 0.0);
        double recentAdh = features.getOrDefault("recentAdherenceRate", 0.0);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"profileScore\": ").append(profile).append(", ");
        sb.append("\"recentAdherenceRate\": ").append(recentAdh).append(", ");
        sb.append("\"comment\": ");

        if (probNonAdherence >= 0.7) {
            sb.append("\"Risque élevé de non-adhésion, lié à un profil psychométrique fragile ou une faible adhésion récente.\"");
        } else if (probNonAdherence >= 0.4) {
            sb.append("\"Risque modéré, surveiller l'évolution des habitudes et du profil.\"");
        } else {
            sb.append("\"Risque faible de non-adhésion, profil et habitudes globalement favorables.\"");
        }

        sb.append("}");
        return sb.toString();
    }
}