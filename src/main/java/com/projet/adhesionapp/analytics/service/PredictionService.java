package com.projet.adhesionapp.analytics.service;

import com.projet.adhesionapp.analytics.domain.Prediction;
import com.projet.adhesionapp.analytics.model.PredictRequest;
import com.projet.adhesionapp.analytics.repo.PredictionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final UserService userService;
    private final FeatureBuilderService featureBuilderService;
    private final ExplanationService explanationService;

    public Prediction predictAndSave(PredictRequest req) {
        User user = userService.findById(req.userId());

        Map<String, Double> features = featureBuilderService.buildFeatures(req);
        double prob = computeProbability(features);
        String explanationJson = explanationService.explain(features, prob);

        Prediction prediction = Prediction.builder()
                .user(user)
                .date(req.date() != null ? req.date() : LocalDate.now())
                .probNonAdherence(prob)
                .modelVersion("v1-baseline")
                .topFeaturesJson(explanationJson)
                .build();

        return predictionRepository.save(prediction);
    }

    /**
     * Enhanced prediction model with multiple factors:
     * - Profile score (psychological wellness): 25% weight
     * - Recent adherence rate: 25% weight
     * - Test completion and scores: 20% weight
     * - Plan progress: 15% weight
     * - Consistency (streak): 15% weight
     */
    private double computeProbability(Map<String, Double> features) {
        // Extract features with defaults
        double profile = features.getOrDefault("profileScore", 50.0); // 0-100
        double recentAdh = features.getOrDefault("recentAdherenceRate", 0.5); // 0-1
        double testCompletion = features.getOrDefault("testCompletionRate", 0.0); // 0-1
        double avgTestScore = features.getOrDefault("averageTestScore", 50.0); // 0-100
        double planProgress = features.getOrDefault("planProgressRate", 0.0); // 0-1
        double activePlans = features.getOrDefault("activePlansCount", 0.0);
        double streak = features.getOrDefault("daysStreak", 0.0);

        // Calculate risk components (lower is better for adherence)
        double profileRisk = 1.0 - (profile / 100.0);
        double adherenceRisk = 1.0 - recentAdh;
        
        // Test factor: completion rate and average score
        double testFactor = 1.0;
        if (testCompletion > 0) {
            double testScoreNorm = avgTestScore / 100.0;
            testFactor = 1.0 - ((testCompletion * 0.4) + (testScoreNorm * 0.6));
        }
        
        // Plan progress factor: having and completing plans reduces risk
        double planFactor = 1.0;
        if (activePlans > 0) {
            planFactor = 1.0 - (planProgress * 0.8 + 0.2); // Having a plan is already positive
        }
        
        // Streak factor: consistency reduces risk (max benefit at 30 days)
        double streakNorm = Math.min(streak / 30.0, 1.0);
        double streakFactor = 1.0 - (streakNorm * 0.5);

        // Weighted combination (weights sum to 1.0)
        double raw = 0.25 * profileRisk 
                   + 0.25 * adherenceRisk 
                   + 0.20 * testFactor 
                   + 0.15 * planFactor 
                   + 0.15 * streakFactor;
        
        // Bound between 0 and 1
        if (raw < 0) raw = 0;
        if (raw > 1) raw = 1;
        return raw;
    }

    public List<Prediction> getLastPredictionsForUser(Long userId) {
        User user = userService.findById(userId);
        return predictionRepository.findTop10ByUserOrderByDateDesc(user);
    }

    public Prediction getById(Long id) {
        return predictionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Prédiction introuvable"));
    }

    public void deleteById(Long id) {
        predictionRepository.deleteById(id);
    }

    public void deleteAllForUser(Long userId) {
        User user = userService.findById(userId);
        List<Prediction> predictions = predictionRepository.findTop10ByUserOrderByDateDesc(user);
        predictionRepository.deleteAll(predictions);
    }
}
