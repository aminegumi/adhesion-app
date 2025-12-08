package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for scoring psychological tests and interpreting results.
 * Provides clinical interpretation based on validated scoring guidelines.
 */
@Service
@Slf4j
public class TestScoringService {

    /**
     * Calculate total score for a completed test session
     */
    public int calculateTotalScore(TestSession session) {
        return session.getAnswers().stream()
                .mapToInt(Answer::getEffectiveScore)
                .sum();
    }

    /**
     * Get detailed interpretation based on test code and score
     */
    public TestInterpretation interpretScore(String testCode, int totalScore) {
        return switch (testCode) {
            case "MMAS-8" -> interpretMMAS8(totalScore);
            case "BMQ" -> interpretBMQ(totalScore);
            case "PHQ-9" -> interpretPHQ9(totalScore);
            case "GAD-7" -> interpretGAD7(totalScore);
            case "MARS-5" -> interpretMARS5(totalScore);
            case "BRIEF-IPQ" -> interpretBriefIPQ(totalScore);
            case "SE-CHRONIC" -> interpretSelfEfficacy(totalScore);
            case "TSS" -> interpretTSS(totalScore);
            default -> new TestInterpretation("UNKNOWN", "Score: " + totalScore,
                    "No interpretation available for this test.", List.of());
        };
    }

    /**
     * MMAS-8 Interpretation
     * Score Range: 0-8 (with Q8 contributing 0-1 based on response)
     */
    private TestInterpretation interpretMMAS8(int score) {
        String level;
        String description;
        List<String> recommendations;

        if (score == 8) {
            level = "HIGH_ADHERENCE";
            description = "High medication adherence. Patient consistently takes medications as prescribed.";
            recommendations = List.of(
                    "Continue current medication routine",
                    "Reinforce positive behaviors",
                    "Schedule regular follow-ups to maintain adherence");
        } else if (score >= 6) {
            level = "MEDIUM_ADHERENCE";
            description = "Medium medication adherence. Some lapses in medication-taking behavior.";
            recommendations = List.of(
                    "Identify specific barriers to adherence",
                    "Consider medication reminders or apps",
                    "Discuss simplification of medication regimen if possible",
                    "Address any concerns about side effects");
        } else {
            level = "LOW_ADHERENCE";
            description = "Low medication adherence. Significant gaps in medication-taking behavior require intervention.";
            recommendations = List.of(
                    "Comprehensive medication review needed",
                    "Assess for depression, cognitive issues, or other barriers",
                    "Consider directly observed therapy or pill organizers",
                    "Evaluate medication beliefs and address misconceptions",
                    "Simplify regimen if possible (once-daily dosing)",
                    "Involve family/caregivers in medication management");
        }

        return new TestInterpretation(level, "MMAS-8 Score: " + score + "/8", description, recommendations);
    }

    /**
     * BMQ Interpretation
     * Necessity subscale: 5-25, Concerns subscale: 5-25
     * Necessity-Concerns Differential determines profile
     */
    private TestInterpretation interpretBMQ(int score) {
        // Note: For full BMQ, you'd need to separate necessity (Q1-5) and concerns
        // (Q6-10) scores
        // This simplified version uses total score
        String level;
        String description;
        List<String> recommendations;

        int midpoint = 30; // Middle of total range 10-50

        if (score > 35) {
            level = "HIGH_NECESSITY_BELIEFS";
            description = "Strong belief in medication necessity. May indicate good understanding of treatment importance.";
            recommendations = List.of(
                    "Validate patient's understanding of medication importance",
                    "Monitor for any emerging concerns about long-term use",
                    "Assess actual adherence behavior");
        } else if (score >= 25) {
            level = "BALANCED_BELIEFS";
            description = "Balanced medication beliefs. Neither strongly positive nor negative about medications.";
            recommendations = List.of(
                    "Explore specific concerns patient may have",
                    "Provide education about medication benefits and risks",
                    "Regular check-ins to monitor belief changes");
        } else {
            level = "HIGH_CONCERNS";
            description = "Significant concerns about medications outweigh perceived necessity. Risk of non-adherence.";
            recommendations = List.of(
                    "Address specific medication concerns directly",
                    "Provide clear information about expected benefits",
                    "Discuss side effect management strategies",
                    "Consider alternative medications if concerns persist",
                    "Motivational interviewing to resolve ambivalence");
        }

        return new TestInterpretation(level, "BMQ Score: " + score + "/50", description, recommendations);
    }

    /**
     * PHQ-9 Interpretation
     * Score Range: 0-27
     */
    private TestInterpretation interpretPHQ9(int score) {
        String level;
        String description;
        List<String> recommendations;

        if (score <= 4) {
            level = "MINIMAL_DEPRESSION";
            description = "Minimal or no depressive symptoms. Depression unlikely to impact medication adherence.";
            recommendations = List.of(
                    "No specific depression intervention needed",
                    "Continue monitoring at regular intervals",
                    "Promote healthy lifestyle behaviors");
        } else if (score <= 9) {
            level = "MILD_DEPRESSION";
            description = "Mild depressive symptoms present. May have minor impact on self-care behaviors.";
            recommendations = List.of(
                    "Watchful waiting with repeat assessment in 2-4 weeks",
                    "Encourage physical activity and social engagement",
                    "Consider supportive counseling",
                    "Monitor impact on medication adherence");
        } else if (score <= 14) {
            level = "MODERATE_DEPRESSION";
            description = "Moderate depressive symptoms. Likely impacting medication adherence and self-care.";
            recommendations = List.of(
                    "Consider antidepressant medication and/or psychotherapy",
                    "Referral to mental health specialist",
                    "Simplify medication regimens to reduce burden",
                    "More frequent monitoring of adherence",
                    "Involve support persons in care");
        } else if (score <= 19) {
            level = "MODERATELY_SEVERE_DEPRESSION";
            description = "Moderately severe depression. Significant impairment in daily functioning expected.";
            recommendations = List.of(
                    "Active treatment with antidepressants and/or psychotherapy recommended",
                    "Psychiatric consultation advised",
                    "Close monitoring for medication adherence",
                    "Consider intensive support services",
                    "Assess for suicidal ideation");
        } else {
            level = "SEVERE_DEPRESSION";
            description = "Severe depressive symptoms. High impairment, immediate intervention needed.";
            recommendations = List.of(
                    "Urgent psychiatric evaluation required",
                    "Assess suicide risk immediately",
                    "Consider hospitalization if indicated",
                    "Maximum support for medication management",
                    "Daily check-ins if outpatient care continues");
        }

        return new TestInterpretation(level, "PHQ-9 Score: " + score + "/27", description, recommendations);
    }

    /**
     * GAD-7 Interpretation
     * Score Range: 0-21
     */
    private TestInterpretation interpretGAD7(int score) {
        String level;
        String description;
        List<String> recommendations;

        if (score <= 4) {
            level = "MINIMAL_ANXIETY";
            description = "Minimal anxiety. Unlikely to significantly impact medication adherence.";
            recommendations = List.of(
                    "No specific anxiety intervention needed",
                    "Promote stress management techniques",
                    "Continue regular monitoring");
        } else if (score <= 9) {
            level = "MILD_ANXIETY";
            description = "Mild anxiety symptoms. May cause some worry about medications or health.";
            recommendations = List.of(
                    "Psychoeducation about anxiety",
                    "Relaxation techniques and mindfulness",
                    "Regular exercise promotion",
                    "Address any specific health-related worries");
        } else if (score <= 14) {
            level = "MODERATE_ANXIETY";
            description = "Moderate anxiety. May significantly affect health behaviors and adherence.";
            recommendations = List.of(
                    "Consider CBT or other psychotherapy",
                    "Evaluate need for anxiolytic medication",
                    "Address medication-related anxiety specifically",
                    "Provide extra reassurance about treatment",
                    "Simplify medication routines to reduce anxiety");
        } else {
            level = "SEVERE_ANXIETY";
            description = "Severe anxiety symptoms. Likely causing significant distress and functional impairment.";
            recommendations = List.of(
                    "Psychiatric evaluation recommended",
                    "Active treatment with medication and/or therapy",
                    "Assess for panic disorder, social anxiety, or specific phobias",
                    "Maximum support for medication adherence",
                    "Consider impact on all health behaviors");
        }

        return new TestInterpretation(level, "GAD-7 Score: " + score + "/21", description, recommendations);
    }

    /**
     * MARS-5 Interpretation
     * Score Range: 5-25 (higher = better adherence)
     */
    private TestInterpretation interpretMARS5(int score) {
        String level;
        String description;
        List<String> recommendations;

        if (score >= 23) {
            level = "EXCELLENT_ADHERENCE";
            description = "Excellent self-reported adherence. Patient rarely deviates from prescribed regimen.";
            recommendations = List.of(
                    "Maintain current approach",
                    "Reinforce positive behaviors",
                    "Regular monitoring to sustain adherence");
        } else if (score >= 20) {
            level = "GOOD_ADHERENCE";
            description = "Good adherence with occasional minor deviations.";
            recommendations = List.of(
                    "Identify any patterns in missed doses",
                    "Address remaining barriers",
                    "Strengthen medication routine");
        } else if (score >= 15) {
            level = "MODERATE_ADHERENCE";
            description = "Moderate adherence. Notable intentional or unintentional non-adherence.";
            recommendations = List.of(
                    "Detailed assessment of non-adherence reasons",
                    "Motivational interviewing",
                    "Medication reminder systems",
                    "Address beliefs and concerns about medication");
        } else {
            level = "POOR_ADHERENCE";
            description = "Poor self-reported adherence. Significant intervention needed.";
            recommendations = List.of(
                    "Comprehensive adherence assessment",
                    "Identify and address all barriers",
                    "Consider DOT or supervised administration",
                    "Simplify regimen if possible",
                    "Engage family/caregivers",
                    "Frequent follow-up appointments");
        }

        return new TestInterpretation(level, "MARS-5 Score: " + score + "/25", description, recommendations);
    }

    /**
     * Brief IPQ Interpretation
     * Score Range: 0-80 (higher = more threatening illness perception)
     */
    private TestInterpretation interpretBriefIPQ(int score) {
        String level;
        String description;
        List<String> recommendations;

        if (score <= 30) {
            level = "POSITIVE_ILLNESS_PERCEPTION";
            description = "Generally positive illness perception. Good understanding and control beliefs.";
            recommendations = List.of(
                    "Reinforce accurate illness understanding",
                    "Maintain therapeutic relationship",
                    "Continue education as condition evolves");
        } else if (score <= 50) {
            level = "MODERATE_ILLNESS_PERCEPTION";
            description = "Mixed illness perceptions. Some areas of concern or misunderstanding.";
            recommendations = List.of(
                    "Identify specific areas of concern",
                    "Targeted education on illness and treatment",
                    "Enhance sense of control through self-management skills",
                    "Address emotional impact of illness");
        } else {
            level = "NEGATIVE_ILLNESS_PERCEPTION";
            description = "Negative illness perception. High perceived threat and low control beliefs.";
            recommendations = List.of(
                    "Intensive illness education needed",
                    "Cognitive restructuring for maladaptive beliefs",
                    "Self-management support program",
                    "Address emotional distress related to illness",
                    "Consider psychological intervention");
        }

        return new TestInterpretation(level, "Brief IPQ Score: " + score + "/80", description, recommendations);
    }

    /**
     * Self-Efficacy for Chronic Disease Interpretation
     * Score Range: 6-60 (higher = greater self-efficacy)
     */
    private TestInterpretation interpretSelfEfficacy(int score) {
        String level;
        String description;
        List<String> recommendations;

        if (score >= 48) {
            level = "HIGH_SELF_EFFICACY";
            description = "High self-efficacy for disease management. Patient confident in ability to manage condition.";
            recommendations = List.of(
                    "Support continued self-management",
                    "Encourage goal-setting for health behaviors",
                    "Patient may be suitable for peer support role");
        } else if (score >= 30) {
            level = "MODERATE_SELF_EFFICACY";
            description = "Moderate self-efficacy. Some confidence but room for improvement.";
            recommendations = List.of(
                    "Skill-building interventions",
                    "Mastery experiences through small achievable goals",
                    "Social modeling (peer support groups)",
                    "Verbal persuasion and encouragement");
        } else {
            level = "LOW_SELF_EFFICACY";
            description = "Low self-efficacy. Patient lacks confidence in ability to manage condition.";
            recommendations = List.of(
                    "Intensive self-management support program",
                    "Break tasks into small, achievable steps",
                    "Provide frequent positive feedback",
                    "Peer support programs",
                    "Consider underlying depression or anxiety",
                    "More frequent healthcare provider contact");
        }

        return new TestInterpretation(level, "Self-Efficacy Score: " + score + "/60", description, recommendations);
    }

    /**
     * Treatment Satisfaction Scale Interpretation
     * Score Range: 10-70 (higher = greater satisfaction)
     */
    private TestInterpretation interpretTSS(int score) {
        String level;
        String description;
        List<String> recommendations;

        if (score >= 56) {
            level = "HIGH_SATISFACTION";
            description = "High treatment satisfaction. Patient is satisfied with medication and its effects.";
            recommendations = List.of(
                    "Maintain current treatment approach",
                    "Monitor for any changes in satisfaction",
                    "Regular adherence monitoring");
        } else if (score >= 40) {
            level = "MODERATE_SATISFACTION";
            description = "Moderate treatment satisfaction. Some aspects may need attention.";
            recommendations = List.of(
                    "Identify specific sources of dissatisfaction",
                    "Address side effect concerns",
                    "Discuss alternative formulations if convenience is an issue",
                    "Set realistic expectations for treatment outcomes");
        } else {
            level = "LOW_SATISFACTION";
            description = "Low treatment satisfaction. High risk of non-adherence or treatment discontinuation.";
            recommendations = List.of(
                    "Comprehensive treatment review needed",
                    "Consider alternative medications",
                    "Address side effects aggressively",
                    "Evaluate treatment goals and expectations",
                    "Shared decision-making about treatment options",
                    "Close monitoring for adherence");
        }

        return new TestInterpretation(level, "TSS Score: " + score + "/70", description, recommendations);
    }

    /**
     * Record class for test interpretation results
     */
    public record TestInterpretation(
            String level,
            String scoreDescription,
            String clinicalInterpretation,
            List<String> recommendations) {
    }
}
