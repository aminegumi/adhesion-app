package com.projet.adhesionapp.analytics.service;

import com.projet.adhesionapp.analytics.model.PredictRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced feature builder that transforms prediction requests into numerical features.
 * Uses multiple factors: profile scores, adherence history, test results, and plan progress.
 */
@Service
public class FeatureBuilderService {

    public Map<String, Double> buildFeatures(PredictRequest req) {
        Map<String, Double> features = new HashMap<>();
        
        // Core features
        features.put("profileScore", req.profileScore());
        features.put("recentAdherenceRate", req.recentAdherenceRate());
        
        // Test-related features
        features.put("testCompletionRate", req.testCompletionRate());
        features.put("averageTestScore", req.averageTestScore());
        
        // Treatment plan features
        features.put("planProgressRate", req.planProgressRate());
        features.put("activePlansCount", (double) req.activePlansCount());
        
        // Streak feature (consistency indicator)
        features.put("daysStreak", (double) req.daysStreak());
        
        return features;
    }
}
