package com.projet.adhesionapp.ai.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService {

    private final OpenAiService openAiService;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.max-tokens:1000}")
    private int maxTokens;

    /**
     * Generate a chat completion based on a system prompt and user message.
     */
    public String generateCompletion(String systemPrompt, String userMessage) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), userMessage));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .maxTokens(maxTokens)
                    .temperature(0.7)
                    .build();

            var response = openAiService.createChatCompletion(request);
            return response.getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage());
            throw new RuntimeException("Failed to generate AI response", e);
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

        String userMessage = String.format("""
                Generate personalized recommendations for patient %s.

                Psychological Profile Type: %s
                Profile Details: %s
                Non-adherence Risk Score: %.0f%%

                Please provide 5-7 specific recommendations tailored to this patient's profile and risk level.
                Consider their psychological profile when suggesting coping strategies and behavioral changes.
                """, patientName, profileType, profileDetails, nonAdherenceRisk * 100);

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
