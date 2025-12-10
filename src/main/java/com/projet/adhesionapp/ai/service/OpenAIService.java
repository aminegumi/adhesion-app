package com.projet.adhesionapp.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenAIService {

        private final WebClient geminiWebClient;
        private final String apiKey;
        private final ObjectMapper objectMapper;

        @Value("${gemini.model:gemini-flash-latest}")
        private String model;

        @Value("${gemini.max-tokens:1000}")
        private int maxTokens;

        public OpenAIService(WebClient geminiWebClient,
                        @Qualifier("geminiApiKey") String apiKey,
                        ObjectMapper objectMapper) {
                this.geminiWebClient = geminiWebClient;
                this.apiKey = apiKey;
                this.objectMapper = objectMapper;
        }

        /**
         * Generate a chat completion based on a system prompt and user message using
         * Gemini API.
         */
        public String generateCompletion(String systemPrompt, String userMessage) {
                try {
                        // Combine system prompt and user message for Gemini
                        String combinedPrompt = systemPrompt + "\n\n" + userMessage;

                        log.info("Calling Gemini API with model: {}", model);

                        Map<String, Object> requestBody = Map.of(
                                        "contents", List.of(
                                                        Map.of(
                                                                        "parts", List.of(
                                                                                        Map.of("text", combinedPrompt)))),
                                        "generationConfig", Map.of(
                                                        "temperature", 0.7,
                                                        "maxOutputTokens", maxTokens,
                                                        "topP", 0.95,
                                                        "topK", 40));

                        String response = geminiWebClient.post()
                                        .uri("/models/{model}:generateContent?key={apiKey}", model, apiKey)
                                        .bodyValue(requestBody)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .block();

                        log.debug("Gemini API response: {}", response);

                        // Parse the response to extract the text
                        JsonNode jsonNode = objectMapper.readTree(response);
                        JsonNode candidates = jsonNode.get("candidates");
                        if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                                JsonNode content = candidates.get(0).get("content");
                                if (content != null) {
                                        JsonNode parts = content.get("parts");
                                        if (parts != null && parts.isArray() && parts.size() > 0) {
                                                String text = parts.get(0).get("text").asText();
                                                log.info("Gemini API call successful, response length: {}",
                                                                text.length());
                                                return text;
                                        }
                                }
                        }

                        log.error("Unexpected Gemini API response format: {}", response);
                        throw new RuntimeException("Failed to parse Gemini API response");

                } catch (WebClientResponseException e) {
                        log.error("Gemini API HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                        if (e.getStatusCode().value() == 429) {
                                throw new RuntimeException(
                                                "AI service rate limit exceeded. Please try again in a few seconds.",
                                                e);
                        }
                        throw new RuntimeException("AI service error: " + e.getMessage(), e);
                } catch (Exception e) {
                        log.error("Error calling Gemini API: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to generate AI response: " + e.getMessage(), e);
                }
        }

        /**
         * Generate a motivational message for a patient.
         */
        public String generateMotivationalMessage(String patientName, String profileSummary, double adherenceScore) {
                String systemPrompt = """
                                You are a supportive and empathetic health psychology assistant. Your role is to motivate patients
                                to adhere to their medication and treatment plans. Be warm, encouraging, and personalized.
                                Keep messages concise (2-3 paragraphs max) and actionable.
                                Use the patient's name and reference their specific situation when possible.
                                """;

                String userMessage = String.format("""
                                Generate a personalized motivational message for patient %s.

                                Their psychological profile summary: %s

                                Their current medication adherence score: %.0f%%

                                Please encourage them and provide 2-3 practical tips to improve their adherence.
                                """, patientName, profileSummary, adherenceScore * 100);

                return generateCompletion(systemPrompt, userMessage);
        }

        /**
         * Generate personalized recommendations based on profile.
         */
        public String generateRecommendations(String patientName, String profileType,
                        String profileDetails, double nonAdherenceRisk) {
                String systemPrompt = """
                                You are a clinical psychology expert specializing in medication adherence and behavioral health.
                                Your role is to provide evidence-based, personalized recommendations to help patients
                                improve their psychological well-being and medication adherence.
                                Structure your response as a numbered list of specific, actionable recommendations.
                                Include both psychological strategies and practical behavioral tips.
                                """;

                String userMessage = String.format(
                                """
                                                Generate personalized recommendations for patient %s.

                                                Psychological Profile Type: %s
                                                Profile Details: %s
                                                Non-adherence Risk Score: %.0f%%

                                                Please provide 5-7 specific recommendations tailored to this patient's profile and risk level.
                                                Consider their psychological profile when suggesting coping strategies and behavioral changes.
                                                """,
                                patientName, profileType, profileDetails, nonAdherenceRisk * 100);

                return generateCompletion(systemPrompt, userMessage);
        }

        /**
         * Generate a treatment plan based on psychological profile.
         */
        public String generateTreatmentPlan(String patientName, String profileType,
                        List<String> identifiedIssues, String currentMedications) {
                String systemPrompt = """
                                You are a mental health treatment planning specialist. Create comprehensive,
                                evidence-based treatment plans that address psychological barriers to medication adherence.
                                Structure plans with clear phases, goals, and timeline.
                                Include both therapeutic interventions and practical daily habits.
                                """;

                String issuesList = String.join(", ", identifiedIssues);
                String userMessage = String.format("""
                                Create a personalized treatment plan for patient %s.

                                Psychological Profile: %s
                                Identified Issues: %s
                                Current Medications: %s

                                Please create a structured 4-week treatment plan with:
                                1. Weekly goals and milestones
                                2. Daily habits to develop
                                3. Coping strategies for identified issues
                                4. Methods to track progress
                                """, patientName, profileType, issuesList, currentMedications);

                return generateCompletion(systemPrompt, userMessage);
        }

        /**
         * Analyze test results and generate psychological profile interpretation.
         */
        public String analyzeTestResults(String testName, List<String> dimensions,
                        List<Double> scores, List<String> answers) {
                String systemPrompt = """
                                You are a clinical psychologist expert in psychometric testing and interpretation.
                                Analyze test results and provide clear, professional interpretations.
                                Be accurate but also compassionate - these results will be shared with patients.
                                Identify strengths as well as areas for improvement.
                                """;

                StringBuilder dimensionScores = new StringBuilder();
                for (int i = 0; i < dimensions.size() && i < scores.size(); i++) {
                        dimensionScores.append(String.format("- %s: %.1f\n", dimensions.get(i), scores.get(i)));
                }

                String userMessage = String.format("""
                                Analyze the following psychological test results:

                                Test: %s

                                Dimension Scores:
                                %s

                                Please provide:
                                1. An overall interpretation of the results
                                2. Key strengths identified
                                3. Areas that may need attention
                                4. Recommended follow-up actions
                                """, testName, dimensionScores.toString());

                return generateCompletion(systemPrompt, userMessage);
        }
}
