package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.Answer;
import com.projet.adhesionapp.assessment.domain.QuestionItem;
import com.projet.adhesionapp.assessment.domain.TestSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestScoringService Unit Tests")
class TestScoringServiceTest {

    @InjectMocks
    private TestScoringService testScoringService;

    private TestSession testSession;

    @BeforeEach
    void setUp() {
        testSession = new TestSession();
        testSession.setAnswers(new ArrayList<>());
    }

    private Answer createAnswer(int score) {
        Answer answer = new Answer();
        answer.setScore(score);
        answer.setValue(score);
        QuestionItem question = new QuestionItem();
        question.setId(1L);
        answer.setQuestion(question);
        return answer;
    }

    @Nested
    @DisplayName("Calculate Total Score Tests")
    class CalculateTotalScoreTests {

        @Test
        @DisplayName("Should calculate total score from answers")
        void shouldCalculateTotalScore() {
            // Given
            testSession.getAnswers().add(createAnswer(3));
            testSession.getAnswers().add(createAnswer(2));
            testSession.getAnswers().add(createAnswer(1));

            // When
            int totalScore = testScoringService.calculateTotalScore(testSession);

            // Then
            assertEquals(6, totalScore);
        }

        @Test
        @DisplayName("Should return zero for empty answers")
        void shouldReturnZeroForEmptyAnswers() {
            // When
            int totalScore = testScoringService.calculateTotalScore(testSession);

            // Then
            assertEquals(0, totalScore);
        }

        @Test
        @DisplayName("Should handle single answer")
        void shouldHandleSingleAnswer() {
            // Given
            testSession.getAnswers().add(createAnswer(5));

            // When
            int totalScore = testScoringService.calculateTotalScore(testSession);

            // Then
            assertEquals(5, totalScore);
        }
    }

    @Nested
    @DisplayName("MMAS-8 Interpretation Tests")
    class MMAS8InterpretationTests {

        @Test
        @DisplayName("Should interpret high adherence score (8)")
        void shouldInterpretHighAdherence() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("MMAS-8", 8);

            // Then
            assertEquals("HIGH_ADHERENCE", result.level());
            assertTrue(result.clinicalInterpretation().contains("High medication adherence"));
            assertFalse(result.recommendations().isEmpty());
        }

        @Test
        @DisplayName("Should interpret medium adherence score (6-7)")
        void shouldInterpretMediumAdherence() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("MMAS-8", 6);

            // Then
            assertEquals("MEDIUM_ADHERENCE", result.level());
            assertTrue(result.clinicalInterpretation().contains("Medium medication adherence"));
        }

        @ParameterizedTest
        @CsvSource({"0", "1", "2", "3", "4", "5"})
        @DisplayName("Should interpret low adherence score (0-5)")
        void shouldInterpretLowAdherence(int score) {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("MMAS-8", score);

            // Then
            assertEquals("LOW_ADHERENCE", result.level());
            assertTrue(result.clinicalInterpretation().contains("Low medication adherence"));
        }
    }

    @Nested
    @DisplayName("PHQ-9 Interpretation Tests")
    class PHQ9InterpretationTests {

        @Test
        @DisplayName("Should interpret minimal depression (0-4)")
        void shouldInterpretMinimalDepression() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("PHQ-9", 3);

            // Then - Using actual service value
            assertEquals("MINIMAL_DEPRESSION", result.level());
            assertTrue(result.clinicalInterpretation().toLowerCase().contains("minimal"));
        }

        @Test
        @DisplayName("Should interpret mild depression (5-9)")
        void shouldInterpretMildDepression() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("PHQ-9", 7);

            // Then - Using actual service value
            assertEquals("MILD_DEPRESSION", result.level());
        }

        @Test
        @DisplayName("Should interpret moderate depression (10-14)")
        void shouldInterpretModerateDepression() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("PHQ-9", 12);

            // Then - Using actual service value
            assertEquals("MODERATE_DEPRESSION", result.level());
        }

        @Test
        @DisplayName("Should interpret moderately severe depression (15-19)")
        void shouldInterpretModeratelySevereDepression() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("PHQ-9", 17);

            // Then - Using actual service value
            assertEquals("MODERATELY_SEVERE_DEPRESSION", result.level());
        }

        @Test
        @DisplayName("Should interpret severe depression (20+)")
        void shouldInterpretSevereDepression() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("PHQ-9", 22);

            // Then - Using actual service value
            assertEquals("SEVERE_DEPRESSION", result.level());
        }
    }

    @Nested
    @DisplayName("GAD-7 Interpretation Tests")
    class GAD7InterpretationTests {

        @Test
        @DisplayName("Should interpret minimal anxiety (0-4)")
        void shouldInterpretMinimalAnxiety() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("GAD-7", 2);

            // Then - Using actual service value
            assertEquals("MINIMAL_ANXIETY", result.level());
        }

        @Test
        @DisplayName("Should interpret mild anxiety (5-9)")
        void shouldInterpretMildAnxiety() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("GAD-7", 7);

            // Then - Using actual service value
            assertEquals("MILD_ANXIETY", result.level());
        }

        @Test
        @DisplayName("Should interpret moderate anxiety (10-14)")
        void shouldInterpretModerateAnxiety() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("GAD-7", 12);

            // Then - Using actual service value
            assertEquals("MODERATE_ANXIETY", result.level());
        }

        @Test
        @DisplayName("Should interpret severe anxiety (15+)")
        void shouldInterpretSevereAnxiety() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("GAD-7", 18);

            // Then - Using actual service value
            assertEquals("SEVERE_ANXIETY", result.level());
        }
    }

    @Nested
    @DisplayName("BMQ Interpretation Tests")
    class BMQInterpretationTests {

        @Test
        @DisplayName("Should interpret high necessity beliefs (>35)")
        void shouldInterpretHighNecessityBeliefs() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("BMQ", 40);

            // Then
            assertEquals("HIGH_NECESSITY_BELIEFS", result.level());
        }

        @Test
        @DisplayName("Should interpret balanced beliefs (25-35)")
        void shouldInterpretBalancedBeliefs() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("BMQ", 30);

            // Then
            assertEquals("BALANCED_BELIEFS", result.level());
        }

        @Test
        @DisplayName("Should interpret high concerns (<25)")
        void shouldInterpretHighConcerns() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("BMQ", 20);

            // Then
            assertEquals("HIGH_CONCERNS", result.level());
        }
    }

    @Nested
    @DisplayName("MARS-5 Interpretation Tests")
    class MARS5InterpretationTests {

        @Test
        @DisplayName("Should interpret excellent adherence (23+)")
        void shouldInterpretExcellentMARS5Adherence() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("MARS-5", 24);

            // Then - Using actual service value
            assertEquals("EXCELLENT_ADHERENCE", result.level());
        }

        @Test
        @DisplayName("Should interpret good adherence (20-22)")
        void shouldInterpretGoodMARS5Adherence() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("MARS-5", 21);

            // Then - Using actual service value
            assertEquals("GOOD_ADHERENCE", result.level());
        }

        @Test
        @DisplayName("Should interpret moderate adherence (15-19)")
        void shouldInterpretModerateMARS5Adherence() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("MARS-5", 17);

            // Then - Using actual service value
            assertEquals("MODERATE_ADHERENCE", result.level());
        }

        @Test
        @DisplayName("Should interpret poor adherence (<15)")
        void shouldInterpretPoorMARS5Adherence() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("MARS-5", 10);

            // Then - Using actual service value
            assertEquals("POOR_ADHERENCE", result.level());
        }
    }

    @Nested
    @DisplayName("Brief IPQ Interpretation Tests")
    class BriefIPQInterpretationTests {

        @Test
        @DisplayName("Should interpret positive illness perception (<=30)")
        void shouldInterpretPositivePerception() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("BRIEF-IPQ", 25);

            // Then - Using actual service value
            assertEquals("POSITIVE_ILLNESS_PERCEPTION", result.level());
        }

        @Test
        @DisplayName("Should interpret moderate illness perception (31-50)")
        void shouldInterpretModeratePerception() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("BRIEF-IPQ", 40);

            // Then - Using actual service value
            assertEquals("MODERATE_ILLNESS_PERCEPTION", result.level());
        }

        @Test
        @DisplayName("Should interpret negative illness perception (>50)")
        void shouldInterpretNegativePerception() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("BRIEF-IPQ", 65);

            // Then - Using actual service value
            assertEquals("NEGATIVE_ILLNESS_PERCEPTION", result.level());
        }
    }

    @Nested
    @DisplayName("Self-Efficacy Interpretation Tests")
    class SelfEfficacyInterpretationTests {

        @Test
        @DisplayName("Should interpret high self-efficacy (>=48)")
        void shouldInterpretHighSelfEfficacy() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("SE-CHRONIC", 50);

            // Then - Using actual service value
            assertEquals("HIGH_SELF_EFFICACY", result.level());
        }

        @Test
        @DisplayName("Should interpret moderate self-efficacy (30-47)")
        void shouldInterpretModerateSelfEfficacy() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("SE-CHRONIC", 38);

            // Then - Using actual service value
            assertEquals("MODERATE_SELF_EFFICACY", result.level());
        }

        @Test
        @DisplayName("Should interpret low self-efficacy (<30)")
        void shouldInterpretLowSelfEfficacy() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("SE-CHRONIC", 20);

            // Then - Using actual service value
            assertEquals("LOW_SELF_EFFICACY", result.level());
        }
    }

    @Nested
    @DisplayName("TSS Interpretation Tests")
    class TSSInterpretationTests {

        @Test
        @DisplayName("Should interpret high treatment satisfaction (>60)")
        void shouldInterpretHighSatisfaction() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("TSS", 70);

            // Then
            assertEquals("HIGH_SATISFACTION", result.level());
        }

        @Test
        @DisplayName("Should interpret moderate treatment satisfaction (40-60)")
        void shouldInterpretModerateSatisfaction() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("TSS", 50);

            // Then
            assertEquals("MODERATE_SATISFACTION", result.level());
        }

        @Test
        @DisplayName("Should interpret low treatment satisfaction (<40)")
        void shouldInterpretLowSatisfaction() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("TSS", 30);

            // Then
            assertEquals("LOW_SATISFACTION", result.level());
        }
    }

    @Nested
    @DisplayName("Unknown Test Interpretation Tests")
    class UnknownTestInterpretationTests {

        @Test
        @DisplayName("Should return unknown interpretation for unrecognized test")
        void shouldReturnUnknownForUnrecognizedTest() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("UNKNOWN-TEST", 50);

            // Then
            assertEquals("UNKNOWN", result.level());
            assertTrue(result.clinicalInterpretation().contains("No interpretation available"));
        }
    }

    @Nested
    @DisplayName("Interpretation Record Tests")
    class InterpretationRecordTests {

        @Test
        @DisplayName("Should return interpretation with all components")
        void shouldReturnInterpretationWithAllComponents() {
            // When
            TestScoringService.TestInterpretation result = 
                    testScoringService.interpretScore("MMAS-8", 8);

            // Then
            assertNotNull(result.level());
            assertNotNull(result.scoreDescription());
            assertNotNull(result.clinicalInterpretation());
            assertNotNull(result.recommendations());
            assertFalse(result.recommendations().isEmpty());
        }
    }
}
