package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.QuestionItem;
import com.projet.adhesionapp.assessment.domain.TestDefinition;
import com.projet.adhesionapp.assessment.repo.TestDefinitionRepository;
import com.projet.adhesionapp.assessment.repo.TestSessionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Initializes validated psychological tests for medication adherence
 * assessment.
 * 
 * Tests included:
 * 1. MMAS-8 (Morisky Medication Adherence Scale) - Most widely used
 * 2. BMQ (Beliefs about Medicines Questionnaire) - Medication beliefs
 * 3. PHQ-9 (Patient Health Questionnaire) - Depression screening
 * 4. GAD-7 (Generalized Anxiety Disorder) - Anxiety screening
 * 5. MARS-5 (Medication Adherence Report Scale) - Self-reported adherence
 * 6. Brief IPQ (Illness Perception Questionnaire) - Illness beliefs
 * 7. SEE (Self-Efficacy for Managing Chronic Disease) - Self-efficacy
 * 8. TSQM (Treatment Satisfaction Questionnaire) - Treatment satisfaction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PsychologicalTestDataInitializer {

        private final TestDefinitionRepository testDefinitionRepository;
        private final TestSessionRepository testSessionRepository;

        private static final List<String> REQUIRED_TEST_CODES = List.of("MMAS-8", "PHQ-9", "GAD-7", "BMQ");

        @PostConstruct
        @Transactional
        public void initializeTests() {
                // Check if all required tests exist and have questions
                boolean needsReinitialize = false;

                for (String code : REQUIRED_TEST_CODES) {
                        // Use eager fetch to avoid LazyInitializationException
                        var testOpt = testDefinitionRepository.findWithQuestionsByCode(code);
                        if (testOpt.isEmpty()) {
                                log.info("Missing test: {}", code);
                                needsReinitialize = true;
                                break;
                        }
                        TestDefinition test = testOpt.get();
                        if (test.getQuestions() == null || test.getQuestions().isEmpty()) {
                                log.info("Test {} has no questions", code);
                                needsReinitialize = true;
                                break;
                        }
                }

                if (needsReinitialize) {
                        log.info("Reinitializing psychological tests database...");

                        // Delete test sessions first (they have FK to test_definition)
                        testSessionRepository.deleteAll();
                        testSessionRepository.flush();

                        // Then delete all existing tests (will cascade to questions)
                        testDefinitionRepository.deleteAll();
                        testDefinitionRepository.flush();

                        // Create 4 validated tests for medication adherence
                        createMMAS8(); // Primary adherence measure
                        createPHQ9(); // Depression (major factor in non-adherence)
                        createGAD7(); // Anxiety (affects medication behavior)
                        createBMQ(); // Medication beliefs

                        log.info("Psychological tests initialization complete. Created 4 tests.");
                } else {
                        log.info("All {} required tests exist with questions. Skipping initialization.",
                                        REQUIRED_TEST_CODES.size());
                }
        }

        /**
         * MMAS-8: Morisky Medication Adherence Scale (8-item)
         * Most widely validated medication adherence measure
         * Score interpretation: 8 = High adherence, 6-7 = Medium, <6 = Low
         */
        private void createMMAS8() {
                TestDefinition test = TestDefinition.builder()
                                .code("MMAS-8")
                                .title("Morisky Medication Adherence Scale")
                                .version("1.0")
                                .active(true)
                                .questions(new ArrayList<>())
                                .build();

                List<QuestionItem> questions = new ArrayList<>();

                questions.add(createQuestion(test, "MMAS8_Q1",
                                "Do you sometimes forget to take your medicine?", 1, false, 0, 1));
                questions.add(createQuestion(test, "MMAS8_Q2",
                                "People sometimes miss taking their medicines for reasons other than forgetting. Over the past 2 weeks, were there any days when you did not take your medicine?",
                                2, false, 0, 1));
                questions.add(createQuestion(test, "MMAS8_Q3",
                                "Have you ever cut back or stopped taking your medicine without telling your doctor because you felt worse when you took it?",
                                3, false, 0, 1));
                questions.add(createQuestion(test, "MMAS8_Q4",
                                "When you travel or leave home, do you sometimes forget to bring along your medicine?",
                                4, false, 0,
                                1));
                questions.add(createQuestion(test, "MMAS8_Q5",
                                "Did you take all your medicine yesterday?", 5, true, 0, 1));
                questions.add(createQuestion(test, "MMAS8_Q6",
                                "When you feel like your symptoms are under control, do you sometimes stop taking your medicine?",
                                6,
                                false, 0, 1));
                questions.add(createQuestion(test, "MMAS8_Q7",
                                "Taking medicine every day is a real inconvenience for some people. Do you ever feel hassled about sticking to your treatment plan?",
                                7, false, 0, 1));
                questions.add(createQuestion(test, "MMAS8_Q8",
                                "How often do you have difficulty remembering to take all your medicine? (0=Never/Rarely, 1=Once in a while, 2=Sometimes, 3=Usually, 4=All the time)",
                                8, false, 0, 4));

                test.setQuestions(questions);
                testDefinitionRepository.save(test);
                log.info("Created MMAS-8 test with {} questions", questions.size());
        }

        /**
         * BMQ: Beliefs about Medicines Questionnaire
         * Assesses cognitive representations of medication
         * Subscales: Necessity beliefs and Concerns
         */
        private void createBMQ() {
                TestDefinition test = TestDefinition.builder()
                                .code("BMQ")
                                .title("Beliefs about Medicines Questionnaire")
                                .version("1.0")
                                .active(true)
                                .questions(new ArrayList<>())
                                .build();

                List<QuestionItem> questions = new ArrayList<>();

                // Necessity subscale (higher = stronger necessity beliefs)
                questions.add(createQuestion(test, "BMQ_N1",
                                "My health, at present, depends on my medicines", 1, false, 1, 5));
                questions.add(createQuestion(test, "BMQ_N2",
                                "My life would be impossible without my medicines", 2, false, 1, 5));
                questions.add(createQuestion(test, "BMQ_N3",
                                "Without my medicines I would be very ill", 3, false, 1, 5));
                questions.add(createQuestion(test, "BMQ_N4",
                                "My health in the future will depend on my medicines", 4, false, 1, 5));
                questions.add(createQuestion(test, "BMQ_N5",
                                "My medicines protect me from becoming worse", 5, false, 1, 5));

                // Concerns subscale (higher = more concerns)
                questions.add(createQuestion(test, "BMQ_C1",
                                "Having to take medicines worries me", 6, false, 1, 5));
                questions.add(createQuestion(test, "BMQ_C2",
                                "I sometimes worry about the long-term effects of my medicines", 7, false, 1, 5));
                questions.add(createQuestion(test, "BMQ_C3",
                                "My medicines are a mystery to me", 8, false, 1, 5));
                questions.add(createQuestion(test, "BMQ_C4",
                                "My medicines disrupt my life", 9, false, 1, 5));
                questions.add(createQuestion(test, "BMQ_C5",
                                "I sometimes worry about becoming too dependent on my medicines", 10, false, 1, 5));

                test.setQuestions(questions);
                testDefinitionRepository.save(test);
                log.info("Created BMQ test with {} questions", questions.size());
        }

        /**
         * PHQ-9: Patient Health Questionnaire
         * Standard depression screening tool
         * Score: 0-4 Minimal, 5-9 Mild, 10-14 Moderate, 15-19 Moderately Severe, 20-27
         * Severe
         */
        private void createPHQ9() {
                TestDefinition test = TestDefinition.builder()
                                .code("PHQ-9")
                                .title("Patient Health Questionnaire - Depression")
                                .version("1.0")
                                .active(true)
                                .questions(new ArrayList<>())
                                .build();

                List<QuestionItem> questions = new ArrayList<>();
                String prefix = "Over the last 2 weeks, how often have you been bothered by: ";

                questions.add(createQuestion(test, "PHQ9_Q1",
                                prefix + "Little interest or pleasure in doing things", 1, false, 0, 3));
                questions.add(createQuestion(test, "PHQ9_Q2",
                                prefix + "Feeling down, depressed, or hopeless", 2, false, 0, 3));
                questions.add(createQuestion(test, "PHQ9_Q3",
                                prefix + "Trouble falling or staying asleep, or sleeping too much", 3, false, 0, 3));
                questions.add(createQuestion(test, "PHQ9_Q4",
                                prefix + "Feeling tired or having little energy", 4, false, 0, 3));
                questions.add(createQuestion(test, "PHQ9_Q5",
                                prefix + "Poor appetite or overeating", 5, false, 0, 3));
                questions.add(createQuestion(test, "PHQ9_Q6",
                                prefix + "Feeling bad about yourself - or that you are a failure or have let yourself or your family down",
                                6, false, 0, 3));
                questions.add(createQuestion(test, "PHQ9_Q7",
                                prefix + "Trouble concentrating on things, such as reading the newspaper or watching television",
                                7,
                                false, 0, 3));
                questions.add(createQuestion(test, "PHQ9_Q8",
                                prefix + "Moving or speaking so slowly that other people could have noticed. Or the opposite - being so fidgety or restless that you have been moving around a lot more than usual",
                                8, false, 0, 3));
                questions.add(createQuestion(test, "PHQ9_Q9",
                                prefix + "Thoughts that you would be better off dead, or of hurting yourself in some way",
                                9, false, 0,
                                3));

                test.setQuestions(questions);
                testDefinitionRepository.save(test);
                log.info("Created PHQ-9 test with {} questions", questions.size());
        }

        /**
         * GAD-7: Generalized Anxiety Disorder Scale
         * Standard anxiety screening tool
         * Score: 0-4 Minimal, 5-9 Mild, 10-14 Moderate, 15-21 Severe
         */
        private void createGAD7() {
                TestDefinition test = TestDefinition.builder()
                                .code("GAD-7")
                                .title("Generalized Anxiety Disorder Scale")
                                .version("1.0")
                                .active(true)
                                .questions(new ArrayList<>())
                                .build();

                List<QuestionItem> questions = new ArrayList<>();
                String prefix = "Over the last 2 weeks, how often have you been bothered by: ";

                questions.add(createQuestion(test, "GAD7_Q1",
                                prefix + "Feeling nervous, anxious, or on edge", 1, false, 0, 3));
                questions.add(createQuestion(test, "GAD7_Q2",
                                prefix + "Not being able to stop or control worrying", 2, false, 0, 3));
                questions.add(createQuestion(test, "GAD7_Q3",
                                prefix + "Worrying too much about different things", 3, false, 0, 3));
                questions.add(createQuestion(test, "GAD7_Q4",
                                prefix + "Trouble relaxing", 4, false, 0, 3));
                questions.add(createQuestion(test, "GAD7_Q5",
                                prefix + "Being so restless that it is hard to sit still", 5, false, 0, 3));
                questions.add(createQuestion(test, "GAD7_Q6",
                                prefix + "Becoming easily annoyed or irritable", 6, false, 0, 3));
                questions.add(createQuestion(test, "GAD7_Q7",
                                prefix + "Feeling afraid as if something awful might happen", 7, false, 0, 3));

                test.setQuestions(questions);
                testDefinitionRepository.save(test);
                log.info("Created GAD-7 test with {} questions", questions.size());
        }

        /**
         * MARS-5: Medication Adherence Report Scale
         * Self-reported medication-taking behavior
         * Higher scores = better adherence (range 5-25)
         */
        private void createMARS5() {
                TestDefinition test = TestDefinition.builder()
                                .code("MARS-5")
                                .title("Medication Adherence Report Scale")
                                .version("1.0")
                                .active(true)
                                .questions(new ArrayList<>())
                                .build();

                List<QuestionItem> questions = new ArrayList<>();
                String prefix = "Here are some ways in which people have said they use their medicines. For each statement, please tell us how often you do this: ";

                questions.add(createQuestion(test, "MARS5_Q1",
                                prefix + "I forget to take my medicines", 1, true, 1, 5));
                questions.add(createQuestion(test, "MARS5_Q2",
                                prefix + "I alter the dose of my medicines", 2, true, 1, 5));
                questions.add(createQuestion(test, "MARS5_Q3",
                                prefix + "I stop taking my medicines for a while", 3, true, 1, 5));
                questions.add(createQuestion(test, "MARS5_Q4",
                                prefix + "I decide to miss out a dose", 4, true, 1, 5));
                questions.add(createQuestion(test, "MARS5_Q5",
                                prefix + "I take less than instructed", 5, true, 1, 5));

                test.setQuestions(questions);
                testDefinitionRepository.save(test);
                log.info("Created MARS-5 test with {} questions", questions.size());
        }

        /**
         * Brief IPQ: Brief Illness Perception Questionnaire
         * Assesses cognitive and emotional illness representations
         * Each item 0-10 scale
         */
        private void createBriefIPQ() {
                TestDefinition test = TestDefinition.builder()
                                .code("BRIEF-IPQ")
                                .title("Brief Illness Perception Questionnaire")
                                .version("1.0")
                                .active(true)
                                .questions(new ArrayList<>())
                                .build();

                List<QuestionItem> questions = new ArrayList<>();

                questions.add(createQuestion(test, "IPQ_Q1",
                                "How much does your illness affect your life? (0=No affect at all, 10=Severely affects my life)",
                                1,
                                false, 0, 10));
                questions.add(createQuestion(test, "IPQ_Q2",
                                "How long do you think your illness will continue? (0=A very short time, 10=Forever)",
                                2, false, 0,
                                10));
                questions.add(createQuestion(test, "IPQ_Q3",
                                "How much control do you feel you have over your illness? (0=Absolutely no control, 10=Extreme amount of control)",
                                3, true, 0, 10));
                questions.add(createQuestion(test, "IPQ_Q4",
                                "How much do you think your treatment can help your illness? (0=Not at all, 10=Extremely helpful)",
                                4,
                                true, 0, 10));
                questions.add(createQuestion(test, "IPQ_Q5",
                                "How much do you experience symptoms from your illness? (0=No symptoms at all, 10=Many severe symptoms)",
                                5, false, 0, 10));
                questions.add(createQuestion(test, "IPQ_Q6",
                                "How concerned are you about your illness? (0=Not at all concerned, 10=Extremely concerned)",
                                6, false,
                                0, 10));
                questions.add(createQuestion(test, "IPQ_Q7",
                                "How well do you feel you understand your illness? (0=Don't understand at all, 10=Understand very clearly)",
                                7, true, 0, 10));
                questions.add(createQuestion(test, "IPQ_Q8",
                                "How much does your illness affect you emotionally? (0=Not at all affected, 10=Extremely affected)",
                                8,
                                false, 0, 10));

                test.setQuestions(questions);
                testDefinitionRepository.save(test);
                log.info("Created Brief-IPQ test with {} questions", questions.size());
        }

        /**
         * Self-Efficacy Scale for Managing Chronic Disease
         * Measures confidence in managing health conditions
         * Higher scores = greater self-efficacy (1-10 scale per item)
         */
        private void createSelfEfficacyScale() {
                TestDefinition test = TestDefinition.builder()
                                .code("SE-CHRONIC")
                                .title("Self-Efficacy for Managing Chronic Disease")
                                .version("1.0")
                                .active(true)
                                .questions(new ArrayList<>())
                                .build();

                List<QuestionItem> questions = new ArrayList<>();
                String prefix = "How confident are you that you can: ";

                questions.add(createQuestion(test, "SE_Q1",
                                prefix + "Keep the fatigue caused by your disease from interfering with the things you want to do?",
                                1,
                                false, 1, 10));
                questions.add(createQuestion(test, "SE_Q2",
                                prefix + "Keep the physical discomfort or pain of your disease from interfering with the things you want to do?",
                                2, false, 1, 10));
                questions.add(createQuestion(test, "SE_Q3",
                                prefix + "Keep the emotional distress caused by your disease from interfering with the things you want to do?",
                                3, false, 1, 10));
                questions.add(createQuestion(test, "SE_Q4",
                                prefix + "Keep any other symptoms or health problems you have from interfering with the things you want to do?",
                                4, false, 1, 10));
                questions.add(createQuestion(test, "SE_Q5",
                                prefix + "Do the different tasks and activities needed to manage your health condition so as to reduce your need to see a doctor?",
                                5, false, 1, 10));
                questions.add(createQuestion(test, "SE_Q6",
                                prefix + "Do things other than just taking medication to reduce how much your illness affects your everyday life?",
                                6, false, 1, 10));

                test.setQuestions(questions);
                testDefinitionRepository.save(test);
                log.info("Created Self-Efficacy scale with {} questions", questions.size());
        }

        /**
         * Treatment Satisfaction Scale (simplified TSQM)
         * Measures satisfaction with current treatment
         * Higher scores = greater satisfaction
         */
        private void createTreatmentSatisfactionScale() {
                TestDefinition test = TestDefinition.builder()
                                .code("TSS")
                                .title("Treatment Satisfaction Scale")
                                .version("1.0")
                                .active(true)
                                .questions(new ArrayList<>())
                                .build();

                List<QuestionItem> questions = new ArrayList<>();

                // Effectiveness subscale
                questions.add(createQuestion(test, "TSS_E1",
                                "How satisfied or dissatisfied are you with the ability of your current medicine to prevent or treat your condition? (1=Extremely Dissatisfied, 7=Extremely Satisfied)",
                                1, false, 1, 7));
                questions.add(createQuestion(test, "TSS_E2",
                                "How satisfied or dissatisfied are you with the way your current medicine relieves your symptoms? (1=Extremely Dissatisfied, 7=Extremely Satisfied)",
                                2, false, 1, 7));
                questions.add(createQuestion(test, "TSS_E3",
                                "How satisfied or dissatisfied are you with the amount of time it takes your current medicine to start working? (1=Extremely Dissatisfied, 7=Extremely Satisfied)",
                                3, false, 1, 7));

                // Side effects subscale
                questions.add(createQuestion(test, "TSS_S1",
                                "How bothered are you by side effects of your current medicine? (1=Extremely Bothered, 7=Not at all Bothered)",
                                4, false, 1, 7));
                questions.add(createQuestion(test, "TSS_S2",
                                "To what extent do side effects interfere with your physical health and ability to function? (1=A Great Deal, 7=Not at all)",
                                5, false, 1, 7));

                // Convenience subscale
                questions.add(createQuestion(test, "TSS_C1",
                                "How easy or difficult is it to take your current medicine as instructed? (1=Extremely Difficult, 7=Extremely Easy)",
                                6, false, 1, 7));
                questions.add(createQuestion(test, "TSS_C2",
                                "How easy or difficult is it to plan when you will take your medicine each time? (1=Extremely Difficult, 7=Extremely Easy)",
                                7, false, 1, 7));
                questions.add(createQuestion(test, "TSS_C3",
                                "How convenient or inconvenient is it to take your medicine as instructed? (1=Extremely Inconvenient, 7=Extremely Convenient)",
                                8, false, 1, 7));

                // Global satisfaction
                questions.add(createQuestion(test, "TSS_G1",
                                "How certain are you that the good things about your current medicine outweigh the bad things? (1=Not at all Certain, 7=Extremely Certain)",
                                9, false, 1, 7));
                questions.add(createQuestion(test, "TSS_G2",
                                "Taking all things into account, how satisfied or dissatisfied are you with your current medicine? (1=Extremely Dissatisfied, 7=Extremely Satisfied)",
                                10, false, 1, 7));

                test.setQuestions(questions);
                testDefinitionRepository.save(test);
                log.info("Created Treatment Satisfaction Scale with {} questions", questions.size());
        }

        private QuestionItem createQuestion(TestDefinition test, String code, String text,
                        int order, boolean reverseScored, int min, int max) {
                return QuestionItem.builder()
                                .testDefinition(test)
                                .code(code)
                                .text(text)
                                .orderIndex(order)
                                .reverseScored(reverseScored)
                                .minScore(min)
                                .maxScore(max)
                                .build();
        }
}
