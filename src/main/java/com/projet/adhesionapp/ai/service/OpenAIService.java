package com.projet.adhesionapp.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.adhesionapp.ai.model.MotivationRequest;
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

        private final WebClient openRouterWebClient;
        private final String apiKey;
        private final ObjectMapper objectMapper;

        @Value("${openrouter.model:alibaba/tongyi-deepresearch-30b-a3b:free}")
        private String model;

        @Value("${openrouter.max-tokens:1000}")
        private int maxTokens;

        public OpenAIService(WebClient openRouterWebClient,
                        @Qualifier("openRouterApiKey") String apiKey,
                        ObjectMapper objectMapper) {
                this.openRouterWebClient = openRouterWebClient;
                this.apiKey = apiKey;
                this.objectMapper = objectMapper;
        }

        /**
         * Generate a chat completion based on a system prompt and user message using
         * OpenRouter API.
         */
        public String generateCompletion(String systemPrompt, String userMessage) {
                try {
                        log.info("Calling OpenRouter API with model: {}", model);

                        // Build messages array in OpenAI-compatible format
                        List<Map<String, String>> messages = List.of(
                                        Map.of("role", "system", "content", systemPrompt),
                                        Map.of("role", "user", "content", userMessage)
                        );

                        Map<String, Object> requestBody = Map.of(
                                        "model", model,
                                        "messages", messages,
                                        "max_tokens", maxTokens,
                                        "temperature", 0.7,
                                        "top_p", 0.95
                        );

                        String response = openRouterWebClient.post()
                                        .uri("/chat/completions")
                                        .bodyValue(requestBody)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .block();

                        log.debug("OpenRouter API response: {}", response);

                        // Parse the response to extract the text (OpenAI-compatible format)
                        JsonNode jsonNode = objectMapper.readTree(response);
                        JsonNode choices = jsonNode.get("choices");
                        if (choices != null && choices.isArray() && choices.size() > 0) {
                                JsonNode message = choices.get(0).get("message");
                                if (message != null) {
                                        JsonNode content = message.get("content");
                                        if (content != null) {
                                                String text = content.asText();
                                                log.info("OpenRouter API call successful, response length: {}",
                                                                text.length());
                                                return text;
                                        }
                                }
                        }

                        log.error("Unexpected OpenRouter API response format: {}", response);
                        throw new RuntimeException("Failed to parse OpenRouter API response");

                } catch (WebClientResponseException e) {
                        log.error("OpenRouter API HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                        if (e.getStatusCode().value() == 429) {
                                throw new RuntimeException(
                                                "AI service rate limit exceeded. Please try again in a few seconds.",
                                                e);
                        }
                        throw new RuntimeException("AI service error: " + e.getMessage(), e);
                } catch (Exception e) {
                        log.error("Error calling OpenRouter API: {}", e.getMessage(), e);
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
                                
                                IMPORTANT RULES:
                                - Address the patient directly by their first name (provided below)
                                - NEVER use placeholders like [Your Name], [Name], or similar
                                - Do NOT use markdown formatting (no **, *, ##, etc.)
                                - Write in plain, warm, conversational text
                                - End with an encouraging sign-off using their name
                                """;

                String userMessage = String.format("""
                                Generate a personalized motivational message for %s.

                                Their psychological profile summary: %s

                                Their current medication adherence score: %.0f%%

                                Write a warm, encouraging message that:
                                1. Greets them by their name (%s)
                                2. Acknowledges their current progress
                                3. Provides 2-3 practical tips to improve their adherence
                                4. Ends with an encouraging note using their name
                                
                                Remember: Use plain text only, no markdown or placeholders.
                                """, patientName, profileSummary, adherenceScore * 100, patientName);

                return generateCompletion(systemPrompt, userMessage);
        }

        /**
         * Generate a conversational response for mental health support.
         */
        public String generateConversationalResponse(String patientName, String profileSummary,
                        double adherenceScore, String userMessage, List<MotivationRequest.ChatMessage> conversationHistory) {

                String systemPrompt = String.format("""
                                You are a compassionate and professional mental health assistant specializing in medication adherence
                                and psychological support. You help patients with:
                                - Medication adherence challenges
                                - Mental health concerns (anxiety, depression, stress)
                                - Treatment motivation and compliance
                                - Wellness and lifestyle improvements
                                - Emotional support and coping strategies

                                PATIENT CONTEXT:
                                Name: %s
                                Psychological Profile: %s
                                Current Adherence Score: %.0f%%

                                CONVERSATION GUIDELINES:
                                - Be empathetic, supportive, and non-judgmental
                                - Address the patient by their first name (%s)
                                - Keep responses conversational and natural (2-4 sentences max)
                                - Focus on health, medication, and mental wellness topics
                                - Provide practical, actionable advice when appropriate
                                - If they express serious mental health concerns, gently suggest professional help
                                - Maintain conversation context and reference previous messages when relevant
                                - End responses in a way that invites further conversation

                                IMPORTANT: Stay focused on health and medication topics. If the conversation drifts to non-health topics,
                                gently redirect back to health and wellness.
                                """, patientName, profileSummary, adherenceScore * 100, patientName);

                // Build conversation context
                StringBuilder conversationContext = new StringBuilder();
                if (conversationHistory != null && !conversationHistory.isEmpty()) {
                        conversationContext.append("\nRECENT CONVERSATION:\n");
                        for (MotivationRequest.ChatMessage msg : conversationHistory) {
                                String role = msg.role().equals("user") ? "Patient" : "Assistant";
                                conversationContext.append(String.format("%s: %s\n", role, msg.content()));
                        }
                }

                String fullUserMessage = String.format("""
                                %s

                                Current patient message: %s

                                Respond as a mental health assistant focusing on their health, medication adherence,
                                and psychological well-being. Keep your response conversational and supportive.
                                """, conversationContext.toString(), userMessage);

                return generateCompletion(systemPrompt, fullUserMessage);
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
                                
                                IMPORTANT RULES:
                                - Address the patient directly by their first name
                                - NEVER use placeholders like [Your Name], [Name], or similar
                                - Do NOT use markdown formatting (no **, *, ##, etc.)
                                - Use simple numbered lists (1. 2. 3.) for recommendations
                                - Write in plain, professional but warm text
                                """;

                String userMessage = String.format(
                                """
                                                Generate personalized recommendations for %s.

                                                Psychological Profile Type: %s
                                                Profile Details: %s
                                                Non-adherence Risk Score: %.0f%%

                                                Please provide 5-7 specific recommendations tailored to %s's profile and risk level.
                                                Consider their psychological profile when suggesting coping strategies and behavioral changes.
                                                
                                                Remember: Use plain text only, no markdown. Address them as %s.
                                                """,
                                patientName, profileType, profileDetails, nonAdherenceRisk * 100, patientName, patientName);

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
                                
                                IMPORTANT RULES:
                                - Address the patient directly by their first name
                                - NEVER use placeholders like [Your Name], [Name], or similar
                                - Do NOT use markdown formatting (no **, *, ##, etc.)
                                - Use simple numbered lists and clear sections
                                - Write in plain, professional but supportive text
                                """;

                String issuesList = String.join(", ", identifiedIssues);
                String userMessage = String.format("""
                                Create a personalized treatment plan for %s.

                                Psychological Profile: %s
                                Identified Issues: %s
                                Current Medications: %s

                                Please create a structured 4-week treatment plan with:
                                1. Weekly goals and milestones
                                2. Daily habits to develop
                                3. Coping strategies for identified issues
                                4. Methods to track progress
                                
                                Address %s directly throughout the plan. Use plain text, no markdown.
                                """, patientName, profileType, issuesList, currentMedications, patientName);

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
                                
                                IMPORTANT RULES:
                                - Do NOT use markdown formatting (no **, *, ##, etc.)
                                - Use simple numbered lists and clear sections
                                - Write in plain, professional but supportive text
                                - Be encouraging while being honest about areas to improve
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

                                Please provide in plain text (no markdown):
                                1. An overall interpretation of the results
                                2. Key strengths identified
                                3. Areas that may need attention
                                4. Recommended follow-up actions
                                """, testName, dimensionScores.toString());

                return generateCompletion(systemPrompt, userMessage);
        }

        /**
         * Detect emotions from user's text input and provide supportive feedback.
         */
        public String detectEmotions(String userName, String text) {
                String systemPrompt = """
                                You are an empathetic emotional support AI specialized in mental health and well-being.
                                Your role is to analyze text for emotional content and provide supportive, understanding responses.
                                
                                IMPORTANT RULES:
                                - Address the user by their first name
                                - NEVER use placeholders like [Your Name], [Name], or similar
                                - Do NOT use markdown formatting (no **, *, ##, etc.)
                                - Be warm, supportive, and non-judgmental
                                - Provide actionable coping strategies when appropriate
                                
                                Respond in the following JSON format (and ONLY this format):
                                {
                                  "primaryEmotion": "the main emotion detected (e.g., anxious, sad, happy, stressed, frustrated, hopeful, calm, angry, overwhelmed, neutral)",
                                  "confidence": 0.85,
                                  "allEmotions": [
                                    {"emotion": "anxious", "score": 0.85},
                                    {"emotion": "stressed", "score": 0.60},
                                    {"emotion": "hopeful", "score": 0.30}
                                  ],
                                  "analysis": "A brief, empathetic analysis of what you sense from their message (2-3 sentences, address them by name)",
                                  "recommendation": "Supportive advice and coping strategies tailored to their emotional state (2-3 sentences, address them by name)"
                                }
                                """;

                String userMessage = String.format("""
                                Analyze the following text from %s and detect their emotional state:

                                "%s"

                                Provide your analysis in the JSON format specified. Address them as %s in your response.
                                """, userName, text, userName);

                return generateCompletion(systemPrompt, userMessage);
        }

        /**
         * Analyze facial emotion from a base64-encoded image using OpenRouter API.
         */
        public String analyzeFacialEmotion(String userName, String imageBase64) {
                String systemPrompt = """
            You are an expert in facial emotion recognition and psychological support. Your job is to analyze a patient's facial image (provided as a base64 string) and detect their dominant emotion, confidence, and provide a brief insight.
            IMPORTANT RULES:
            - Only use the image for emotion detection, do not ask for text input
            - Output JSON ONLY in the following format:
            {
              \"dominantEmotion\": \"happy|sad|angry|fearful|surprised|disgusted|neutral\",
              \"confidence\": 0.92,
              \"emotionScores\": {\"happy\":0.92,\"sad\":0.03,\"neutral\":0.05},
              \"insight\": "A brief, empathetic analysis of the detected facial emotion."
            }
            - Do NOT use markdown or code blocks
            - Do NOT use placeholders or ask for more info
            - Be concise and supportive
                """;
        
                        String userMessage = String.format("""
        Analyze the following base64-encoded facial image for %s and detect their dominant emotion.
        
        Base64 image:
        %s
        
        Respond ONLY in the JSON format specified above.
        """, userName, imageBase64);

                return generateCompletion(systemPrompt, userMessage);
        }
}
