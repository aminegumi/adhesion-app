package com.projet.adhesionapp.ai.model;

import java.util.Map;

public record FacialEmotionDetectionResponse(
    String dominantEmotion,
    double confidence,
    Map<String, Double> emotionScores,
    String insight,
    Long userId
) {}
