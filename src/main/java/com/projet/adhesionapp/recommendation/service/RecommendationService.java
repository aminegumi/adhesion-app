package com.projet.adhesionapp.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.service.PsychologicalProfileService;
import com.projet.adhesionapp.recommendation.domain.Recommendation;
import com.projet.adhesionapp.recommendation.model.RecommendationDto;
import com.projet.adhesionapp.recommendation.repo.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final UserService userService;
    private final PsychologicalProfileService profileService;
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper;

    /**
     * Generate AI-powered recommendations for a user based on their psychological
     * profile.
     */
    @Transactional
    public List<Recommendation> generateRecommendations(Long userId) {
        User user = userService.findById(userId);
        PsychologicalProfile profile = profileService.getLatestProfile(userId);

        if (profile == null) {
            throw new NotFoundException(
                    "No psychological profile found for user. Please complete a psychological assessment first.");
        }

        // Generate AI recommendations
        String aiRecommendations = openAIService.generateRecommendations(
                user.getDisplayName(),
                profile.getProfileType(),
                profile.getSummary(),
                profile.getAdherenceRiskScore());

        // Parse and create recommendations
        List<Recommendation> recommendations = parseAndCreateRecommendations(user, profile, aiRecommendations);

        return recommendationRepository.saveAll(recommendations);
    }

    /**
     * Get all active recommendations for a user.
     */
    public List<Recommendation> getActiveRecommendations(Long userId) {
        return recommendationRepository.findByUserIdAndCompletedFalseOrderByPriorityAsc(userId);
    }

    /**
     * Get all recommendations for a user.
     */
    public List<Recommendation> getUserRecommendations(Long userId) {
        return recommendationRepository.findByUserIdOrderByPriorityAsc(userId);
    }

    /**
     * Mark a recommendation as completed.
     */
    @Transactional
    public Recommendation completeRecommendation(Long id, String feedback) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recommendation not found"));

        rec.setCompleted(true);
        rec.setCompletedAt(Instant.now());
        rec.setStatus(Recommendation.RecommendationStatus.COMPLETED);
        rec.setUserFeedback(feedback);

        return recommendationRepository.save(rec);
    }

    /**
     * Dismiss a recommendation.
     */
    @Transactional
    public Recommendation dismissRecommendation(Long id, String reason) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recommendation not found"));

        rec.setStatus(Recommendation.RecommendationStatus.DISMISSED);
        rec.setUserFeedback(reason);

        return recommendationRepository.save(rec);
    }

    private List<Recommendation> parseAndCreateRecommendations(User user, PsychologicalProfile profile,
            String aiResponse) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Parse the AI response and create structured recommendations
        String[] lines = aiResponse.split("\n");
        int priority = 1;
        StringBuilder currentRec = new StringBuilder();
        String currentTitle = "";

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            // Check if this is a new numbered recommendation
            if (line.matches("^\\d+\\..*")) {
                // Save previous recommendation if exists
                if (!currentTitle.isEmpty()) {
                    recommendations.add(createRecommendation(user, profile, currentTitle,
                            currentRec.toString().trim(), priority++));
                }
                // Start new recommendation
                currentTitle = line.replaceFirst("^\\d+\\.\\s*", "");
                currentRec = new StringBuilder();
            } else {
                currentRec.append(line).append(" ");
            }
        }

        // Don't forget the last recommendation
        if (!currentTitle.isEmpty()) {
            recommendations.add(createRecommendation(user, profile, currentTitle,
                    currentRec.toString().trim(), priority));
        }

        // If no structured recommendations found, create a general one
        if (recommendations.isEmpty()) {
            recommendations.add(Recommendation.builder()
                    .user(user)
                    .profile(profile)
                    .category("General")
                    .priority(1)
                    .title("Personalized Health Recommendations")
                    .description(aiResponse)
                    .difficulty(Recommendation.Difficulty.MEDIUM)
                    .timeFrame("Ongoing")
                    .build());
        }

        return recommendations;
    }

    private Recommendation createRecommendation(User user, PsychologicalProfile profile,
            String title, String description, int priority) {
        // Determine category based on content
        String category = determineCategory(title + " " + description);
        Recommendation.Difficulty difficulty = determineDifficulty(description);

        return Recommendation.builder()
                .user(user)
                .profile(profile)
                .category(category)
                .priority(priority)
                .title(truncate(title, 200))
                .description(truncate(description, 2000))
                .difficulty(difficulty)
                .timeFrame("Daily")
                .build();
    }

    private String determineCategory(String content) {
        content = content.toLowerCase();
        if (content.contains("medication") || content.contains("medicine") || content.contains("pill")) {
            return "Medication";
        } else if (content.contains("exercise") || content.contains("physical") || content.contains("walk")
                || content.contains("sport")) {
            return "Exercise";
        } else if (content.contains("sleep") || content.contains("rest") || content.contains("relax")) {
            return "Lifestyle";
        } else if (content.contains("friend") || content.contains("family") || content.contains("social")
                || content.contains("support")) {
            return "Social";
        } else if (content.contains("anxiety") || content.contains("stress") || content.contains("therapy")
                || content.contains("mindfulness")) {
            return "Mental Health";
        } else if (content.contains("diet") || content.contains("eat") || content.contains("nutrition")) {
            return "Nutrition";
        }
        return "General";
    }

    private Recommendation.Difficulty determineDifficulty(String content) {
        content = content.toLowerCase();
        if (content.contains("simple") || content.contains("easy") || content.contains("just")) {
            return Recommendation.Difficulty.EASY;
        } else if (content.contains("challenge") || content.contains("difficult") || content.contains("significant")) {
            return Recommendation.Difficulty.HARD;
        }
        return Recommendation.Difficulty.MEDIUM;
    }

    private String truncate(String str, int maxLength) {
        if (str == null)
            return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    public RecommendationDto toDto(Recommendation rec) {
        return new RecommendationDto(
                rec.getId(),
                rec.getUser().getId(),
                rec.getProfile() != null ? rec.getProfile().getId() : null,
                rec.getCategory(),
                rec.getPriority(),
                rec.getTitle(),
                rec.getDescription(),
                rec.getActionableSteps(),
                rec.getExpectedBenefits(),
                rec.getTimeFrame(),
                rec.getDifficulty() != null ? rec.getDifficulty().name() : null,
                rec.getStatus() != null ? rec.getStatus().name() : null,
                rec.getCompleted(),
                rec.getCreatedAt(),
                rec.getCompletedAt());
    }
}
