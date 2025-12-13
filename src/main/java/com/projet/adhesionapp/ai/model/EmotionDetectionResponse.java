package com.projet.adhesionapp.ai.model;

import java.util.List;
import java.util.Map;

public record EmotionDetectionResponse(
    String primaryEmotion,
    double confidence,
    List<EmotionScore> allEmotions,
    String analysis,
    String recommendation,
    Long userId
) {
    public record EmotionScore(String emotion, double score) {}
}
