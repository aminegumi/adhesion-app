package com.projet.adhesionapp.ai.web;

import com.projet.adhesionapp.ai.model.*;
import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.service.PsychologicalProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final OpenAIService openAIService;
    private final UserService userService;
    private final PsychologicalProfileService profileService;

    @PostMapping("/motivation")
    public ResponseEntity<MotivationResponse> getMotivationalMessage(
            @Valid @RequestBody MotivationRequest request) {

        User user = userService.findById(request.userId());
        PsychologicalProfile profile = profileService.getLatestProfile(user.getId());

        String profileSummary = profile != null ? profile.getProfileType() + ": " + profile.getSummary()
                : "No profile available yet";

        double adherenceScore = request.adherenceScore() != null ? request.adherenceScore() : 0.5;

        String message = openAIService.generateMotivationalMessage(
                user.getDisplayName(),
                profileSummary,
                adherenceScore);

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
}
