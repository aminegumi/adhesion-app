package com.projet.adhesionapp.analytics.service;

import com.projet.adhesionapp.analytics.model.PredictRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * V1 très simple : on transforme la requête en features numériques.
 * Plus tard, on pourra récupérer des données réelles (tests, habitudes, etc.).
 */
@Service
public class FeatureBuilderService {

    public Map<String, Double> buildFeatures(PredictRequest req) {
        Map<String, Double> features = new HashMap<>();
        features.put("profileScore", req.profileScore());
        features.put("recentAdherenceRate", req.recentAdherenceRate());
        return features;
    }
}
