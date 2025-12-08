package com.projet.adhesionapp.profile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.assessment.domain.ProfileScore;
import com.projet.adhesionapp.assessment.domain.TestSession;
import com.projet.adhesionapp.assessment.repo.ProfileScoreRepository;
import com.projet.adhesionapp.assessment.repo.TestSessionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.model.ProfileCreateRequest;
import com.projet.adhesionapp.profile.model.ProfileDto;
import com.projet.adhesionapp.profile.repo.PsychologicalProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PsychologicalProfileService {

    private final PsychologicalProfileRepository profileRepository;
    private final UserService userService;
    private final ProfileScoreRepository profileScoreRepository;
    private final TestSessionRepository testSessionRepository;
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper;

    /**
     * Create a new psychological profile based on test results.
     */
    @Transactional
    public PsychologicalProfile createProfile(ProfileCreateRequest request) {
        User user = userService.findById(request.userId());

        // Gather scores from test sessions
        Map<String, Double> dimensionScores = new HashMap<>();

        if (request.sessionIds() != null && !request.sessionIds().isEmpty()) {
            for (Long sessionId : request.sessionIds()) {
                // First try to get profile scores
                List<ProfileScore> scores = profileScoreRepository.findBySessionId(sessionId);
                for (ProfileScore score : scores) {
                    dimensionScores.put(score.getDimension(), score.getScore());
                }

                // Also extract dimension from test session directly
                TestSession session = testSessionRepository.findById(sessionId).orElse(null);
                if (session != null && session.getTotalScore() != null) {
                    String testCode = session.getTestDefinition().getCode();
                    // Map test codes to psychological dimensions
                    double normalizedScore = normalizeTestScore(testCode, session.getTotalScore());
                    String dimension = mapTestCodeToDimension(testCode);
                    if (dimension != null) {
                        dimensionScores.put(dimension, normalizedScore);
                    }
                }
            }
        }

        // If no session IDs, try to get from user's completed sessions
        if (dimensionScores.isEmpty()) {
            List<TestSession> userSessions = testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(
                    user.getId(),
                    com.projet.adhesionapp.assessment.domain.TestSessionStatus.INTERPRETED);
            for (TestSession session : userSessions) {
                if (session.getTotalScore() != null) {
                    String testCode = session.getTestDefinition().getCode();
                    double normalizedScore = normalizeTestScore(testCode, session.getTotalScore());
                    String dimension = mapTestCodeToDimension(testCode);
                    if (dimension != null && !dimensionScores.containsKey(dimension)) {
                        dimensionScores.put(dimension, normalizedScore);
                    }
                }
            }
        }

        // If direct scores are provided, use them
        if (request.anxietyScore() != null)
            dimensionScores.put("anxiety", request.anxietyScore());
        if (request.depressionScore() != null)
            dimensionScores.put("depression", request.depressionScore());
        if (request.motivationScore() != null)
            dimensionScores.put("motivation", request.motivationScore());
        if (request.selfEfficacyScore() != null)
            dimensionScores.put("selfEfficacy", request.selfEfficacyScore());
        if (request.socialSupportScore() != null)
            dimensionScores.put("socialSupport", request.socialSupportScore());
        if (request.healthLocusScore() != null)
            dimensionScores.put("healthLocus", request.healthLocusScore());

        // Determine profile type based on scores
        String profileType = determineProfileType(dimensionScores);

        // Calculate adherence risk
        double adherenceRisk = calculateAdherenceRisk(dimensionScores);

        // Generate AI interpretation
        String summary = generateSummary(user.getDisplayName(), profileType, dimensionScores);
        String interpretation = generateDetailedInterpretation(user.getDisplayName(), profileType, dimensionScores);

        // Serialize dimension scores to JSON
        String dimensionScoresJson = serializeDimensionScores(dimensionScores);

        PsychologicalProfile profile = PsychologicalProfile.builder()
                .user(user)
                .profileType(profileType)
                .anxietyScore(dimensionScores.get("anxiety"))
                .depressionScore(dimensionScores.get("depression"))
                .motivationScore(dimensionScores.get("motivation"))
                .selfEfficacyScore(dimensionScores.get("selfEfficacy"))
                .socialSupportScore(dimensionScores.get("socialSupport"))
                .healthLocusScore(dimensionScores.get("healthLocus"))
                .adherenceRiskScore(adherenceRisk)
                .summary(summary)
                .detailedInterpretation(interpretation)
                .dimensionScoresJson(dimensionScoresJson)
                .status(PsychologicalProfile.ProfileStatus.ACTIVE)
                .build();

        return profileRepository.save(profile);
    }

    /**
     * Get the latest profile for a user.
     */
    public PsychologicalProfile getLatestProfile(Long userId) {
        User user = userService.findById(userId);
        return profileRepository.findFirstByUserOrderByCreatedAtDesc(user).orElse(null);
    }

    /**
     * Get all profiles for a user.
     */
    public List<PsychologicalProfile> getUserProfiles(Long userId) {
        return profileRepository.findByUserId(userId);
    }

    /**
     * Get profile by ID.
     */
    public PsychologicalProfile getById(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
    }

    /**
     * Get all profiles.
     */
    public List<PsychologicalProfile> getAllProfiles() {
        return profileRepository.findAll();
    }

    /**
     * Determine the psychological profile type based on dimension scores.
     */
    private String determineProfileType(Map<String, Double> scores) {
        double anxiety = scores.getOrDefault("anxiety", 50.0);
        double depression = scores.getOrDefault("depression", 50.0);
        double motivation = scores.getOrDefault("motivation", 50.0);
        double selfEfficacy = scores.getOrDefault("selfEfficacy", 50.0);
        double socialSupport = scores.getOrDefault("socialSupport", 50.0);

        // Profile classification logic
        if (anxiety > 70 && depression > 60) {
            return "Anxious-Depressive";
        } else if (anxiety > 70 && selfEfficacy < 40) {
            return "Anxious-Low Confidence";
        } else if (motivation > 70 && selfEfficacy > 70) {
            return "Highly Motivated";
        } else if (motivation < 40 && selfEfficacy < 40) {
            return "Disengaged";
        } else if (socialSupport < 40 && depression > 50) {
            return "Isolated";
        } else if (selfEfficacy > 60 && motivation > 60 && anxiety < 50) {
            return "Resilient";
        } else if (anxiety > 50 || depression > 50) {
            return "Moderately Stressed";
        } else {
            return "Balanced";
        }
    }

    /**
     * Calculate adherence risk based on psychological scores.
     */
    private double calculateAdherenceRisk(Map<String, Double> scores) {
        double anxiety = scores.getOrDefault("anxiety", 50.0) / 100;
        double depression = scores.getOrDefault("depression", 50.0) / 100;
        double motivation = scores.getOrDefault("motivation", 50.0) / 100;
        double selfEfficacy = scores.getOrDefault("selfEfficacy", 50.0) / 100;
        double socialSupport = scores.getOrDefault("socialSupport", 50.0) / 100;
        double healthLocus = scores.getOrDefault("healthLocus", 50.0) / 100;

        // Risk factors increase risk, protective factors decrease it
        double risk = 0.0;
        risk += anxiety * 0.20; // High anxiety increases risk
        risk += depression * 0.25; // Depression is a major risk factor
        risk += (1 - motivation) * 0.20; // Low motivation increases risk
        risk += (1 - selfEfficacy) * 0.15;// Low self-efficacy increases risk
        risk += (1 - socialSupport) * 0.10;// Low social support increases risk
        risk += (1 - healthLocus) * 0.10; // External locus increases risk

        return Math.min(1.0, Math.max(0.0, risk));
    }

    private String generateSummary(String patientName, String profileType, Map<String, Double> scores) {
        try {
            String scoresSummary = scores.entrySet().stream()
                    .map(e -> e.getKey() + ": " + String.format("%.1f", e.getValue()))
                    .collect(Collectors.joining(", "));

            String systemPrompt = "You are a clinical psychologist. Generate a brief 2-3 sentence summary of a patient's psychological profile. Be professional and compassionate.";
            String userMessage = String.format("Patient: %s\nProfile Type: %s\nScores: %s\n\nGenerate a brief summary.",
                    patientName, profileType, scoresSummary);

            return openAIService.generateCompletion(systemPrompt, userMessage);
        } catch (Exception e) {
            log.warn("Failed to generate AI summary, using default", e);
            return String.format("Profile type: %s. Further evaluation recommended.", profileType);
        }
    }

    private String generateDetailedInterpretation(String patientName, String profileType, Map<String, Double> scores) {
        try {
            List<String> dimensions = new ArrayList<>(scores.keySet());
            List<Double> scoreValues = new ArrayList<>(scores.values());

            return openAIService.analyzeTestResults(profileType, dimensions, scoreValues, null);
        } catch (Exception e) {
            log.warn("Failed to generate detailed interpretation", e);
            return "Detailed interpretation pending review.";
        }
    }

    /**
     * Map test code to psychological dimension.
     */
    private String mapTestCodeToDimension(String testCode) {
        if (testCode == null)
            return null;
        return switch (testCode.toUpperCase()) {
            case "MMAS8", "MMAS-8" -> "motivation"; // Medication adherence relates to motivation
            case "PHQ9", "PHQ-9" -> "depression";
            case "GAD7", "GAD-7" -> "anxiety";
            case "GSE", "GSE-10" -> "selfEfficacy";
            case "MSPSS" -> "socialSupport";
            case "MHLC", "MHLC-C" -> "healthLocus";
            default -> testCode.toLowerCase();
        };
    }

    /**
     * Normalize test score to 0-100 scale based on test type.
     */
    private double normalizeTestScore(String testCode, int rawScore) {
        if (testCode == null)
            return rawScore;
        return switch (testCode.toUpperCase()) {
            case "MMAS8", "MMAS-8" -> {
                // MMAS-8: 0-8, where 8 = high adherence = high motivation
                yield Math.min(100, (rawScore / 8.0) * 100);
            }
            case "PHQ9", "PHQ-9" -> {
                // PHQ-9: 0-27, where 27 = severe depression
                yield Math.min(100, (rawScore / 27.0) * 100);
            }
            case "GAD7", "GAD-7" -> {
                // GAD-7: 0-21, where 21 = severe anxiety
                yield Math.min(100, (rawScore / 21.0) * 100);
            }
            case "GSE", "GSE-10" -> {
                // GSE: 10-40, where 40 = high self-efficacy
                yield Math.min(100, ((rawScore - 10) / 30.0) * 100);
            }
            case "MSPSS" -> {
                // MSPSS: 12-84, where 84 = high social support
                yield Math.min(100, ((rawScore - 12) / 72.0) * 100);
            }
            default -> Math.min(100, rawScore);
        };
    }

    private String serializeDimensionScores(Map<String, Double> scores) {
        try {
            return objectMapper.writeValueAsString(scores);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize dimension scores", e);
            return "{}";
        }
    }

    public ProfileDto toDto(PsychologicalProfile profile) {
        return new ProfileDto(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getDisplayName(),
                profile.getProfileType(),
                profile.getAnxietyScore(),
                profile.getDepressionScore(),
                profile.getMotivationScore(),
                profile.getSelfEfficacyScore(),
                profile.getSocialSupportScore(),
                profile.getHealthLocusScore(),
                profile.getAdherenceRiskScore(),
                profile.getSummary(),
                profile.getDetailedInterpretation(),
                profile.getStatus().name(),
                profile.getCreatedAt());
    }
}
