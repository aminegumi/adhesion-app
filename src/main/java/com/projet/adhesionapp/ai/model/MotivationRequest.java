package com.projet.adhesionapp.ai.model;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MotivationRequest(
        @NotNull Long userId,
        Double adherenceScore,
        String userMessage,
        List<ChatMessage> conversationHistory) {

    public record ChatMessage(
            String role, // "user" or "assistant"
            String content,
            Long timestamp) {
    }
}
