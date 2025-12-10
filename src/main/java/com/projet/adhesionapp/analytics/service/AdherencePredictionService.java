package com.projet.adhesionapp.analytics.service;

import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.analytics.model.AdherencePredictionDto;
import com.projet.adhesionapp.analytics.model.RiskFactorDto;
import com.projet.adhesionapp.assessment.domain.TestSession;
import com.projet.adhesionapp.assessment.domain.TestSessionStatus;
import com.projet.adhesionapp.assessment.repo.TestSessionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.habit.domain.DoseLog;
import com.projet.adhesionapp.habit.domain.DoseLog.DoseStatus;
import com.projet.adhesionapp.habit.domain.DoseLog.SkipReason;
import com.projet.adhesionapp.habit.repo.DoseLogRepository;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.repo.PsychologicalProfileRepository;
import com.projet.adhesionapp.treatment.domain.Medication;
import com.projet.adhesionapp.treatment.repo.MedicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Real-world adherence prediction service using evidence-based factors.
 * 
 * This service combines:
 * 1. Actual behavioral data (dose-taking patterns from DoseLog)
 * 2. Validated psychological assessments (MMAS-8, PHQ-9, GAD-7, BMQ)
 * 3. Treatment complexity factors
 * 4. Temporal patterns (time of day, day of week)
 * 
 * Based on research from:
 * - World Health Organization adherence framework
 * - Morisky et al. (2008) - MMAS-8 validation
 * - Horne et al. (1999) - Beliefs about Medicines
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdherencePredictionService {

    private final DoseLogRepository doseLogRepository;
    private final TestSessionRepository testSessionRepository;
    private final PsychologicalProfileRepository profileRepository;
    private final MedicationRepository medicationRepository;
    private final UserRepository userRepository;
    private final OpenAIService aiService;

    // Weight factors based on research literature
    private static final double WEIGHT_PAST_BEHAVIOR = 0.35; // Historical adherence is best predictor
    private static final double WEIGHT_MMAS8 = 0.20; // Validated adherence scale
    private static final double WEIGHT_DEPRESSION = 0.15; // PHQ-9 - major adherence barrier
    private static final double WEIGHT_BELIEFS = 0.10; // BMQ Necessity-Concerns differential
    private static final double WEIGHT_ANXIETY = 0.08; // GAD-7
    private static final double WEIGHT_TREATMENT_COMPLEXITY = 0.07;
    private static final double WEIGHT_TEMPORAL_PATTERNS = 0.05;

    /**
     * Generate a comprehensive adherence prediction for a user.
     * Uses multi-factor evidence-based model.
     */
    public AdherencePredictionDto predictAdherence(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        List<RiskFactorDto> riskFactors = new ArrayList<>();
        Map<String, Double> componentScores = new HashMap<>();

        // 1. BEHAVIORAL DATA - Most important predictor (35% weight)
        BehavioralAnalysis behavioral = analyzeBehavioralPatterns(userId);
        componentScores.put("pastBehavior", behavioral.adherenceRate);
        if (behavioral.adherenceRate < 0.8) {
            riskFactors.add(new RiskFactorDto(
                    "Past Adherence",
                    String.format("Historical adherence rate of %.0f%% is below optimal (80%%)",
                            behavioral.adherenceRate * 100),
                    calculateSeverity(behavioral.adherenceRate, 0.8, 0.5),
                    "Focus on building consistent medication-taking habits"));
        }

        // 2. MMAS-8 SCORE - Validated adherence measure (20% weight)
        Double mmas8Score = getMmas8Score(userId);
        double normalizedMmas8 = normalizeMmas8(mmas8Score);
        componentScores.put("mmas8", normalizedMmas8);
        if (mmas8Score != null && mmas8Score < 6) {
            String adherenceLevel = mmas8Score < 6 ? "Low" : "Medium";
            riskFactors.add(new RiskFactorDto(
                    "Self-Reported Adherence",
                    String.format("MMAS-8 score of %.1f indicates %s adherence", mmas8Score, adherenceLevel),
                    mmas8Score < 6 ? "high" : "medium",
                    "Consider discussing medication barriers with your healthcare provider"));
        }

        // 3. DEPRESSION (PHQ-9) - Major barrier (15% weight)
        Double phq9Score = getTestScore(userId, "PHQ-9");
        double depressionFactor = normalizePhq9(phq9Score);
        componentScores.put("depression", 1 - depressionFactor); // Invert: lower depression = better adherence
        if (phq9Score != null && phq9Score >= 10) {
            String severity = phq9Score >= 20 ? "severe" : (phq9Score >= 15 ? "moderately severe" : "moderate");
            riskFactors.add(new RiskFactorDto(
                    "Depression",
                    String.format("PHQ-9 score of %.0f suggests %s depression symptoms", phq9Score, severity),
                    phq9Score >= 15 ? "high" : "medium",
                    "Depression can significantly impact medication adherence. Consider speaking with a mental health professional."));
        }

        // 4. MEDICATION BELIEFS (BMQ) - 10% weight
        BeliefsAnalysis beliefs = analyzeMedicationBeliefs(userId);
        componentScores.put("beliefs", beliefs.necessityConcernsDifferential);
        if (beliefs.concernsScore > beliefs.necessityScore) {
            riskFactors.add(new RiskFactorDto(
                    "Medication Concerns",
                    "Your concerns about medications outweigh your perceived necessity",
                    "medium",
                    "Discuss your specific concerns with your healthcare provider to get personalized information"));
        }

        // 5. ANXIETY (GAD-7) - 8% weight
        Double gad7Score = getTestScore(userId, "GAD-7");
        double anxietyFactor = normalizeGad7(gad7Score);
        componentScores.put("anxiety", 1 - anxietyFactor);
        if (gad7Score != null && gad7Score >= 10) {
            riskFactors.add(new RiskFactorDto(
                    "Anxiety",
                    String.format("GAD-7 score of %.0f indicates moderate to severe anxiety", gad7Score),
                    gad7Score >= 15 ? "high" : "medium",
                    "Anxiety can interfere with medication routines. Consider relaxation techniques or professional support."));
        }

        // 6. TREATMENT COMPLEXITY - 7% weight
        TreatmentComplexity complexity = analyzeRegimen(userId);
        componentScores.put("treatmentComplexity", complexity.simplicityScore);
        if (complexity.totalDailyDoses > 4) {
            riskFactors.add(new RiskFactorDto(
                    "Complex Regimen",
                    String.format("You have %d medications with %d daily doses", complexity.medicationCount,
                            complexity.totalDailyDoses),
                    complexity.totalDailyDoses > 6 ? "high" : "medium",
                    "Consider using pill organizers or medication reminder apps"));
        }

        // 7. TEMPORAL PATTERNS - 5% weight
        TemporalPatterns temporal = analyzeTemporalPatterns(userId);
        componentScores.put("temporalPatterns", temporal.consistencyScore);
        if (temporal.problematicTimeSlot != null) {
            riskFactors.add(new RiskFactorDto(
                    "Timing Issue",
                    String.format("Lower adherence during %s doses", temporal.problematicTimeSlot),
                    "low",
                    "Set specific reminders for " + temporal.problematicTimeSlot + " medications"));
        }
        if (temporal.problematicDay != null) {
            riskFactors.add(new RiskFactorDto(
                    "Day Pattern",
                    String.format("Lower adherence on %ss", temporal.problematicDay),
                    "low",
                    "Plan ahead for " + temporal.problematicDay + " routines"));
        }

        // Calculate weighted prediction
        double predictedAdherence = calculateWeightedPrediction(componentScores);

        // Classify risk level
        String riskLevel = classifyRisk(predictedAdherence, riskFactors);

        // Generate AI-powered recommendations
        String aiRecommendations = generateAIRecommendations(user, predictedAdherence, riskFactors, behavioral);

        // Calculate confidence based on data availability
        double confidence = calculateConfidence(componentScores, behavioral.dataPoints);

        return new AdherencePredictionDto(
                userId,
                user.getDisplayName(),
                predictedAdherence,
                riskLevel,
                confidence,
                riskFactors,
                componentScores,
                behavioral.currentStreak,
                behavioral.bestStreak,
                behavioral.skipReasonBreakdown,
                aiRecommendations,
                LocalDate.now());
    }

    /**
     * Analyze behavioral patterns from actual dose-taking data
     */
    private BehavioralAnalysis analyzeBehavioralPatterns(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        List<DoseLog> logs = doseLogRepository
                .findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                        userId, thirtyDaysAgo, today);

        BehavioralAnalysis analysis = new BehavioralAnalysis();
        analysis.dataPoints = logs.size();

        if (logs.isEmpty()) {
            analysis.adherenceRate = 0.5; // No data - assume moderate risk
            analysis.currentStreak = 0;
            analysis.bestStreak = 0;
            analysis.skipReasonBreakdown = new HashMap<>();
            return analysis;
        }

        // Calculate adherence rate
        long taken = logs.stream().filter(d -> d.getStatus() == DoseStatus.TAKEN).count();
        long total = logs.stream().filter(d -> d.getStatus() != DoseStatus.PENDING).count();
        analysis.adherenceRate = total > 0 ? (double) taken / total : 0.5;

        // Calculate current streak
        analysis.currentStreak = calculateCurrentStreak(logs);

        // Calculate best streak
        analysis.bestStreak = calculateBestStreak(logs);

        // Analyze skip reasons
        analysis.skipReasonBreakdown = logs.stream()
                .filter(d -> d.getStatus() == DoseStatus.SKIPPED && d.getSkipReason() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getSkipReason().name(),
                        Collectors.counting()));

        // Calculate average delay when doses are taken
        analysis.averageDelay = logs.stream()
                .filter(d -> d.getStatus() == DoseStatus.TAKEN && d.getDelayMinutes() != null)
                .mapToInt(DoseLog::getDelayMinutes)
                .average()
                .orElse(0);

        return analysis;
    }

    /**
     * Get MMAS-8 score for user
     */
    private Double getMmas8Score(Long userId) {
        return getTestScore(userId, "MMAS-8");
    }

    /**
     * Get most recent score for a specific test
     */
    private Double getTestScore(Long userId, String testCode) {
        return testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(userId, TestSessionStatus.INTERPRETED)
                .stream()
                .filter(s -> s.getTestDefinition().getCode().equals(testCode))
                .findFirst()
                .map(s -> s.getTotalScore() != null ? s.getTotalScore().doubleValue() : null)
                .orElse(null);
    }

    /**
     * Normalize MMAS-8 score (0-8 scale) to 0-1 adherence score
     * 8 = High adherence, 6-7 = Medium, <6 = Low
     */
    private double normalizeMmas8(Double score) {
        if (score == null)
            return 0.5; // No data - neutral
        // Higher score = better adherence
        return Math.min(1.0, score / 8.0);
    }

    /**
     * Normalize PHQ-9 (0-27 scale) - higher score = more depression = lower
     * adherence
     */
    private double normalizePhq9(Double score) {
        if (score == null)
            return 0.0; // No data - assume no depression
        return Math.min(1.0, score / 27.0);
    }

    /**
     * Normalize GAD-7 (0-21 scale) - higher score = more anxiety
     */
    private double normalizeGad7(Double score) {
        if (score == null)
            return 0.0;
        return Math.min(1.0, score / 21.0);
    }

    /**
     * Analyze BMQ beliefs about medications
     */
    private BeliefsAnalysis analyzeMedicationBeliefs(Long userId) {
        BeliefsAnalysis analysis = new BeliefsAnalysis();

        TestSession bmqSession = testSessionRepository
                .findByUserIdAndStatusOrderBySubmittedAtDesc(userId, TestSessionStatus.INTERPRETED)
                .stream()
                .filter(s -> s.getTestDefinition().getCode().equals("BMQ"))
                .findFirst()
                .orElse(null);

        if (bmqSession == null) {
            analysis.necessityScore = 0.5;
            analysis.concernsScore = 0.5;
            analysis.necessityConcernsDifferential = 0.5;
            return analysis;
        }

        // BMQ has 10 questions: 5 necessity (Q1-5), 5 concerns (Q6-10)
        // Each scored 1-5, so max per subscale = 25
        double totalScore = bmqSession.getTotalScore() != null ? bmqSession.getTotalScore() : 25;

        // Approximate split (in real implementation, we'd store subscale scores
        // separately)
        analysis.necessityScore = Math.min(1.0, (totalScore * 0.6) / 25); // Higher = more necessity belief
        analysis.concernsScore = Math.min(1.0, (totalScore * 0.4) / 25); // Higher = more concerns

        // Necessity-Concerns differential: positive = good, negative = risk
        double differential = analysis.necessityScore - analysis.concernsScore;
        analysis.necessityConcernsDifferential = (differential + 1) / 2; // Normalize to 0-1

        return analysis;
    }

    /**
     * Analyze treatment regimen complexity
     */
    private TreatmentComplexity analyzeRegimen(Long userId) {
        TreatmentComplexity complexity = new TreatmentComplexity();

        List<Medication> medications = medicationRepository.findActiveMedicationsByUserId(userId);
        complexity.medicationCount = medications.size();

        complexity.totalDailyDoses = medications.stream()
                .mapToInt(Medication::getTimesPerDay)
                .sum();

        // Simplicity score: fewer medications and doses = higher score
        // Optimal: 1-2 medications, 1-2 times daily
        double medFactor = Math.max(0, 1 - (complexity.medicationCount - 1) * 0.15);
        double doseFactor = Math.max(0, 1 - (complexity.totalDailyDoses - 2) * 0.1);
        complexity.simplicityScore = (medFactor + doseFactor) / 2;

        return complexity;
    }

    /**
     * Analyze temporal patterns in adherence
     */
    private TemporalPatterns analyzeTemporalPatterns(Long userId) {
        TemporalPatterns patterns = new TemporalPatterns();

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<DoseLog> logs = doseLogRepository
                .findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                        userId, thirtyDaysAgo, LocalDate.now());

        if (logs.isEmpty()) {
            patterns.consistencyScore = 0.5;
            return patterns;
        }

        // Analyze by time of day
        Map<String, List<DoseLog>> byTimeSlot = logs.stream()
                .filter(d -> d.getStatus() != DoseStatus.PENDING)
                .collect(Collectors.groupingBy(d -> getTimeSlot(d.getScheduledTime())));

        Map<String, Double> slotAdherence = new HashMap<>();
        for (Map.Entry<String, List<DoseLog>> entry : byTimeSlot.entrySet()) {
            long taken = entry.getValue().stream().filter(d -> d.getStatus() == DoseStatus.TAKEN).count();
            slotAdherence.put(entry.getKey(), (double) taken / entry.getValue().size());
        }

        // Find problematic time slot
        patterns.problematicTimeSlot = slotAdherence.entrySet().stream()
                .filter(e -> e.getValue() < 0.7)
                .min(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

        // Analyze by day of week
        Map<DayOfWeek, List<DoseLog>> byDay = logs.stream()
                .filter(d -> d.getStatus() != DoseStatus.PENDING)
                .collect(Collectors.groupingBy(d -> d.getScheduledDate().getDayOfWeek()));

        Map<DayOfWeek, Double> dayAdherence = new HashMap<>();
        for (Map.Entry<DayOfWeek, List<DoseLog>> entry : byDay.entrySet()) {
            long taken = entry.getValue().stream().filter(d -> d.getStatus() == DoseStatus.TAKEN).count();
            dayAdherence.put(entry.getKey(), (double) taken / entry.getValue().size());
        }

        // Find problematic day
        patterns.problematicDay = dayAdherence.entrySet().stream()
                .filter(e -> e.getValue() < 0.7)
                .min(Comparator.comparingDouble(Map.Entry::getValue))
                .map(e -> e.getKey().name())
                .orElse(null);

        // Overall consistency score
        double variance = slotAdherence.values().stream()
                .mapToDouble(d -> d)
                .map(d -> Math.pow(d - 0.8, 2))
                .average()
                .orElse(0);
        patterns.consistencyScore = Math.max(0, 1 - variance * 2);

        return patterns;
    }

    private String getTimeSlot(LocalTime time) {
        int hour = time.getHour();
        if (hour < 10)
            return "Morning";
        if (hour < 14)
            return "Midday";
        if (hour < 18)
            return "Afternoon";
        return "Evening";
    }

    /**
     * Calculate weighted prediction score
     */
    private double calculateWeightedPrediction(Map<String, Double> scores) {
        double prediction = 0;

        prediction += scores.getOrDefault("pastBehavior", 0.5) * WEIGHT_PAST_BEHAVIOR;
        prediction += scores.getOrDefault("mmas8", 0.5) * WEIGHT_MMAS8;
        prediction += scores.getOrDefault("depression", 0.5) * WEIGHT_DEPRESSION;
        prediction += scores.getOrDefault("beliefs", 0.5) * WEIGHT_BELIEFS;
        prediction += scores.getOrDefault("anxiety", 0.5) * WEIGHT_ANXIETY;
        prediction += scores.getOrDefault("treatmentComplexity", 0.5) * WEIGHT_TREATMENT_COMPLEXITY;
        prediction += scores.getOrDefault("temporalPatterns", 0.5) * WEIGHT_TEMPORAL_PATTERNS;

        return Math.max(0, Math.min(1, prediction));
    }

    /**
     * Classify risk level based on prediction and risk factors
     */
    private String classifyRisk(double prediction, List<RiskFactorDto> riskFactors) {
        long highRiskCount = riskFactors.stream().filter(r -> "high".equals(r.severity())).count();

        if (prediction >= 0.85 && highRiskCount == 0)
            return "Low";
        if (prediction >= 0.7 && highRiskCount <= 1)
            return "Moderate";
        if (prediction >= 0.5 || highRiskCount <= 2)
            return "High";
        return "Very High";
    }

    /**
     * Calculate confidence in prediction based on available data
     */
    private double calculateConfidence(Map<String, Double> scores, int dataPoints) {
        // More data = higher confidence
        double dataConfidence = Math.min(1, dataPoints / 30.0);

        // More assessment scores = higher confidence
        long nonDefaultScores = scores.values().stream().filter(s -> s != 0.5).count();
        double assessmentConfidence = nonDefaultScores / (double) scores.size();

        return (dataConfidence * 0.6 + assessmentConfidence * 0.4);
    }

    private String calculateSeverity(double value, double goodThreshold, double badThreshold) {
        if (value >= goodThreshold)
            return "low";
        if (value >= badThreshold)
            return "medium";
        return "high";
    }

    private int calculateCurrentStreak(List<DoseLog> logs) {
        // Sort by date descending
        List<DoseLog> sorted = logs.stream()
                .filter(d -> d.getStatus() != DoseStatus.PENDING)
                .sorted((a, b) -> {
                    int dateCompare = b.getScheduledDate().compareTo(a.getScheduledDate());
                    return dateCompare != 0 ? dateCompare : b.getScheduledTime().compareTo(a.getScheduledTime());
                })
                .toList();

        int streak = 0;
        for (DoseLog log : sorted) {
            if (log.getStatus() == DoseStatus.TAKEN) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private int calculateBestStreak(List<DoseLog> logs) {
        List<DoseLog> sorted = logs.stream()
                .filter(d -> d.getStatus() != DoseStatus.PENDING)
                .sorted(Comparator.comparing(DoseLog::getScheduledDate)
                        .thenComparing(DoseLog::getScheduledTime))
                .toList();

        int best = 0;
        int current = 0;
        for (DoseLog log : sorted) {
            if (log.getStatus() == DoseStatus.TAKEN) {
                current++;
                best = Math.max(best, current);
            } else {
                current = 0;
            }
        }
        return best;
    }

    /**
     * Generate personalized AI recommendations
     */
    private String generateAIRecommendations(User user, double prediction, List<RiskFactorDto> riskFactors,
            BehavioralAnalysis behavioral) {
        String systemPrompt = """
                You are a medication adherence specialist providing personalized, evidence-based recommendations.
                Be specific, actionable, and encouraging. Focus on practical behavioral strategies.
                Format your response as a numbered list with 4-6 recommendations.
                Keep each recommendation to 1-2 sentences.
                """;

        StringBuilder context = new StringBuilder();
        context.append(String.format("Patient: %s\n", user.getDisplayName()));
        context.append(String.format("Predicted adherence: %.0f%%\n", prediction * 100));
        context.append(String.format("Current streak: %d doses\n", behavioral.currentStreak));
        context.append(String.format("Best streak: %d doses\n", behavioral.bestStreak));
        context.append(String.format("Average delay when taking meds: %.0f minutes\n", behavioral.averageDelay));

        if (!riskFactors.isEmpty()) {
            context.append("\nIdentified risk factors:\n");
            for (RiskFactorDto rf : riskFactors) {
                context.append(String.format("- %s (%s severity): %s\n", rf.factor(), rf.severity(), rf.description()));
            }
        }

        if (!behavioral.skipReasonBreakdown.isEmpty()) {
            context.append("\nSkip reasons:\n");
            for (Map.Entry<String, Long> entry : behavioral.skipReasonBreakdown.entrySet()) {
                context.append(String.format("- %s: %d times\n", entry.getKey(), entry.getValue()));
            }
        }

        String userMessage = "Based on this patient's data, provide specific personalized recommendations to improve their medication adherence:\n\n"
                + context;

        try {
            return aiService.generateCompletion(systemPrompt, userMessage);
        } catch (Exception e) {
            log.error("Failed to generate AI recommendations", e);
            return generateFallbackRecommendations(prediction, riskFactors);
        }
    }

    private String generateFallbackRecommendations(double prediction, List<RiskFactorDto> riskFactors) {
        StringBuilder sb = new StringBuilder();
        sb.append("Based on your profile, here are some recommendations:\n\n");

        if (prediction < 0.7) {
            sb.append("1. Set daily medication reminders on your phone\n");
            sb.append("2. Use a pill organizer to track your doses\n");
            sb.append("3. Link taking medication to an existing daily habit (e.g., brushing teeth)\n");
        }

        for (RiskFactorDto rf : riskFactors) {
            if (rf.recommendation() != null) {
                sb.append("• ").append(rf.recommendation()).append("\n");
            }
        }

        return sb.toString();
    }

    // Inner classes for analysis results
    private static class BehavioralAnalysis {
        double adherenceRate;
        int currentStreak;
        int bestStreak;
        double averageDelay;
        int dataPoints;
        Map<String, Long> skipReasonBreakdown = new HashMap<>();
    }

    private static class BeliefsAnalysis {
        double necessityScore;
        double concernsScore;
        double necessityConcernsDifferential;
    }

    private static class TreatmentComplexity {
        int medicationCount;
        int totalDailyDoses;
        double simplicityScore;
    }

    private static class TemporalPatterns {
        double consistencyScore;
        String problematicTimeSlot;
        String problematicDay;
    }
}
