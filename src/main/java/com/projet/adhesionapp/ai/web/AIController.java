package com.projet.adhesionapp.ai.web;
import com.projet.adhesionapp.ai.model.FacialEmotionDetectionRequest;
import com.projet.adhesionapp.ai.model.FacialEmotionDetectionResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.adhesionapp.ai.model.*;
import com.projet.adhesionapp.ai.service.EmotionDetectionService;
import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.service.PsychologicalProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

        @PostMapping(value = "/analyze-facial-emotion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<FacialEmotionDetectionResponse> analyzeFacialEmotion(
                        @RequestParam("userId") Long userId,
                        @RequestParam("image") MultipartFile image) {
                User user = userService.findById(userId);
                try {
                        byte[] imageBytes = image.getBytes();
                        Map<String, Object> result = emotionDetectionService.detectEmotion(imageBytes);
                        String emotion = (String) result.get("emotion");
                        double confidence = (Double) result.get("confidence");

                        return ResponseEntity.ok(new FacialEmotionDetectionResponse(
                                        emotion,
                                        confidence,
                                        Map.of(emotion, confidence),
                                        "Emotion detected successfully",
                                        user.getId()
                        ));
                } catch (Exception e) {
                        log.error("Failed to detect emotion: {}", e.getMessage());
                        return ResponseEntity.ok(new FacialEmotionDetectionResponse(
                                        "neutral",
                                        0.5,
                                        Map.of("neutral", 0.5),
                                        "Could not analyze facial emotion. Please try again.",
                                        user.getId()
                        ));
                }
        }

    private final OpenAIService openAIService;
    private final UserService userService;
    private final PsychologicalProfileService profileService;
    private final ObjectMapper objectMapper;
    private final EmotionDetectionService emotionDetectionService;

    @PostMapping("/motivation")
    public ResponseEntity<MotivationResponse> getMotivationalMessage(
            @Valid @RequestBody MotivationRequest request) {

        User user = userService.findById(request.userId());
        PsychologicalProfile profile = profileService.getLatestProfile(user.getId());

        String profileSummary = profile != null ? profile.getProfileType() + ": " + profile.getSummary()
                : "No profile available yet";

        double adherenceScore = request.adherenceScore() != null ? request.adherenceScore() : 0.5;

        String message;

        if (request.userMessage() != null && !request.userMessage().trim().isEmpty()) {
            // Conversational mode - use the user's message and conversation history
            message = openAIService.generateConversationalResponse(
                    user.getDisplayName(),
                    profileSummary,
                    adherenceScore,
                    request.userMessage(),
                    request.conversationHistory());
        } else {
            // Legacy mode - generate motivational message
            message = openAIService.generateMotivationalMessage(
                    user.getDisplayName(),
                    profileSummary,
                    adherenceScore);
        }

        return ResponseEntity.ok(new MotivationResponse(message, user.getId()));
    }

    @PostMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @Valid @RequestBody RecommendationRequest request) {

        User user = userService.findById(request.userId());
        PsychologicalProfile profile = profileService.getLatestProfile(user.getId());

        String profileType = profile != null ? profile.getProfileType() : "Unknown";
        String profileDetails = profile != null ? profile.getSummary() : "No profile available";
        double riskScore = request.nonAdherenceRisk() != null ? request.nonAdherenceRisk() : 0.5;

        String recommendations = openAIService.generateRecommendations(
                user.getDisplayName(),
                profileType,
                profileDetails,
                riskScore);

        return ResponseEntity.ok(new RecommendationResponse(recommendations, user.getId()));
    }

    @PostMapping("/treatment-plan")
    public ResponseEntity<TreatmentPlanResponse> generateTreatmentPlan(
            @Valid @RequestBody TreatmentPlanRequest request) {

        User user = userService.findById(request.userId());
        PsychologicalProfile profile = profileService.getLatestProfile(user.getId());

        String profileType = profile != null ? profile.getProfileType() : "Unknown";

        String plan = openAIService.generateTreatmentPlan(
                user.getDisplayName(),
                profileType,
                request.identifiedIssues(),
                request.currentMedications());

        return ResponseEntity.ok(new TreatmentPlanResponse(plan, user.getId()));
    }

    @PostMapping("/analyze-test")
    public ResponseEntity<TestAnalysisResponse> analyzeTestResults(
            @Valid @RequestBody TestAnalysisRequest request) {

        String analysis = openAIService.analyzeTestResults(
                request.testName(),
                request.dimensions(),
                request.scores(),
                request.answers());

        return ResponseEntity.ok(new TestAnalysisResponse(analysis, request.testName()));
    }

    @PostMapping("/detect-emotions")
    public ResponseEntity<EmotionDetectionResponse> detectEmotions(
            @Valid @RequestBody EmotionDetectionRequest request) {

        User user = userService.findById(request.userId());
        String rawResponse = openAIService.detectEmotions(user.getDisplayName(), request.text());

        try {
            // Parse the JSON response from AI
            // Clean potential markdown code blocks
            String cleanedResponse = rawResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
            
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

            String primaryEmotion = jsonNode.get("primaryEmotion").asText();
            double confidence = jsonNode.get("confidence").asDouble();
            String analysis = jsonNode.get("analysis").asText();
            String recommendation = jsonNode.get("recommendation").asText();

            List<EmotionDetectionResponse.EmotionScore> allEmotions = new ArrayList<>();
            JsonNode emotionsArray = jsonNode.get("allEmotions");
            if (emotionsArray != null && emotionsArray.isArray()) {
                for (JsonNode emotionNode : emotionsArray) {
                    allEmotions.add(new EmotionDetectionResponse.EmotionScore(
                            emotionNode.get("emotion").asText(),
                            emotionNode.get("score").asDouble()));
                }
            }

            return ResponseEntity.ok(new EmotionDetectionResponse(
                    primaryEmotion,
                    confidence,
                    allEmotions,
                    analysis,
                    recommendation,
                    user.getId()));

        } catch (Exception e) {
            log.error("Failed to parse emotion detection response: {}", e.getMessage());
            // Return a default response if parsing fails
            return ResponseEntity.ok(new EmotionDetectionResponse(
                    "neutral",
                    0.5,
                    List.of(new EmotionDetectionResponse.EmotionScore("neutral", 0.5)),
                    "I understand you're sharing something with me. " + user.getDisplayName() + ", thank you for opening up.",
                    "Take a moment to breathe and reflect on how you're feeling. Remember, it's okay to experience emotions.",
                    user.getId()));
        }
    }
}
