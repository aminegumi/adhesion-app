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
     * Modèle très simplifié :
     * - plus le score de profil est élevé, plus le risque baisse
     * - plus l'adhésion récente est élevée, plus le risque baisse
     */
    private double computeProbability(Map<String, Double> features) {
        double profile = features.getOrDefault("profileScore", 50.0); // 0-100
        double recentAdh = features.getOrDefault("recentAdherenceRate", 0.5); // 0-1

        // Normalisation simple
        double profileRisk = 1.0 - (profile / 100.0);
        double adherenceRisk = 1.0 - recentAdh;

        double raw = 0.6 * profileRisk + 0.4 * adherenceRisk;
        // borne entre 0 et 1
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
}
