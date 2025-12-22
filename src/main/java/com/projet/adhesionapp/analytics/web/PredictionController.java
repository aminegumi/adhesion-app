package com.projet.adhesionapp.analytics.web;

import com.projet.adhesionapp.analytics.domain.Prediction;
import com.projet.adhesionapp.analytics.model.AdherencePredictionDto;
import com.projet.adhesionapp.analytics.model.PredictRequest;
import com.projet.adhesionapp.analytics.model.PredictionDto;
import com.projet.adhesionapp.analytics.service.AdherencePredictionService;
import com.projet.adhesionapp.analytics.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;
    private final AdherencePredictionService adherencePredictionService;

    /**
     * Enhanced adherence prediction using multi-factor evidence-based model.
     * Combines behavioral data, psychological assessments, and treatment
     * complexity.
     */
    @GetMapping("/adherence/{userId}")
    public AdherencePredictionDto predictAdherence(@PathVariable Long userId) {
        return adherencePredictionService.predictAdherence(userId);
    }

    @PostMapping
    public PredictionDto predict(@RequestBody PredictRequest request) {
        Prediction saved = predictionService.predictAndSave(request);
        return new PredictionDto(
                saved.getId(),
                saved.getUser().getId(),
                saved.getDate(),
                saved.getProbNonAdherence(),
                saved.getModelVersion(),
                saved.getTopFeaturesJson(),
                saved.getCreatedAt());
    }

    @GetMapping("/user/{userId}")
    public List<PredictionDto> history(@PathVariable Long userId) {
        return predictionService.getLastPredictionsForUser(userId)
                .stream()
                .map(p -> new PredictionDto(
                        p.getId(),
                        p.getUser().getId(),
                        p.getDate(),
                        p.getProbNonAdherence(),
                        p.getModelVersion(),
                        p.getTopFeaturesJson(),
                        p.getCreatedAt()))
                .toList();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        predictionService.deleteById(id);
    }

    @DeleteMapping("/user/{userId}")
    public void deleteAllForUser(@PathVariable Long userId) {
        predictionService.deleteAllForUser(userId);
    }
}