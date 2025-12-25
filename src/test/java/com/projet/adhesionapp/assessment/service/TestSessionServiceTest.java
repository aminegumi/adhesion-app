package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.*;
import com.projet.adhesionapp.assessment.model.*;
import com.projet.adhesionapp.assessment.repo.*;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test Session Service Tests")
class TestSessionServiceTest {

    @Mock
    private TestSessionRepository sessionRepo;

    @Mock
    private TestDefinitionRepository testRepo;

    @Mock
    private AnswerRepository answerRepo;

    @Mock
    private TestScoringService scoringService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TestSessionService testSessionService;

    private User testUser;
    private TestDefinition testDefinition;
    private TestSession testSession;
    private List<QuestionItem> questions;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordHash("hashed")
                .displayName("Jean Dupont")
                .birthDate(java.time.LocalDate.of(1990, 1, 1))
                .gender("M")
                .active(true)
                .build();

        questions = List.of(
                createQuestion(1L, "Q1", "How often do you feel anxious?", 0, 3, 1),
                createQuestion(2L, "Q2", "How often do you feel sad?", 0, 3, 2),
                createQuestion(3L, "Q3", "How is your sleep quality?", 0, 3, 3)
        );

        testDefinition = TestDefinition.builder()
                .id(1L)
                .code("PHQ-9")
                .title("Patient Health Questionnaire")
                .questions(questions)
                .build();

        testSession = TestSession.builder()
                .id(1L)
                .testDefinition(testDefinition)
                .user(testUser)
                .startedAt(Instant.now())
                .status(TestSessionStatus.IN_PROGRESS)
                .answers(new ArrayList<>())
                .build();
    }

    private QuestionItem createQuestion(Long id, String code, String text, int min, int max, int order) {
        QuestionItem q = new QuestionItem();
        q.setId(id);
        q.setCode(code);
        q.setText(text);
        q.setMinScore(min);
        q.setMaxScore(max);
        q.setOrderIndex(order);
        q.setReverseScored(false);
        return q;
    }

    @Nested
    @DisplayName("Start Session Tests")
    class StartSessionTests {

        @Test
        @DisplayName("Should start new session successfully")
        void shouldStartNewSessionSuccessfully() {
            // Given
            when(testRepo.findWithQuestionsById(1L)).thenReturn(Optional.of(testDefinition));
            when(sessionRepo.save(any(TestSession.class))).thenAnswer(i -> {
                TestSession s = i.getArgument(0);
                s.setId(1L);
                return s;
            });

            // When
            TestSessionDto result = testSessionService.startSession(1L, testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.testId()).isEqualTo(1L);
            assertThat(result.testCode()).isEqualTo("PHQ-9");
            assertThat(result.testTitle()).isEqualTo("Patient Health Questionnaire");
            assertThat(result.questions()).hasSize(3);

            verify(sessionRepo).save(any(TestSession.class));
        }

        @Test
        @DisplayName("Should throw NotFoundException when test not found")
        void shouldThrowNotFoundExceptionWhenTestNotFound() {
            // Given
            when(testRepo.findWithQuestionsById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> testSessionService.startSession(999L, testUser))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Test introuvable");
        }

        @Test
        @DisplayName("Should set correct initial status")
        void shouldSetCorrectInitialStatus() {
            // Given
            when(testRepo.findWithQuestionsById(1L)).thenReturn(Optional.of(testDefinition));
            when(sessionRepo.save(any(TestSession.class))).thenAnswer(i -> {
                TestSession s = i.getArgument(0);
                assertThat(s.getStatus()).isEqualTo(TestSessionStatus.IN_PROGRESS);
                s.setId(1L);
                return s;
            });

            // When
            testSessionService.startSession(1L, testUser);

            // Then
            verify(sessionRepo).save(argThat(session -> 
                    session.getStatus() == TestSessionStatus.IN_PROGRESS));
        }

        @Test
        @DisplayName("Should include all question details in DTO")
        void shouldIncludeAllQuestionDetailsInDto() {
            // Given
            when(testRepo.findWithQuestionsById(1L)).thenReturn(Optional.of(testDefinition));
            when(sessionRepo.save(any(TestSession.class))).thenAnswer(i -> {
                TestSession s = i.getArgument(0);
                s.setId(1L);
                return s;
            });

            // When
            TestSessionDto result = testSessionService.startSession(1L, testUser);

            // Then
            QuestionItemDto firstQuestion = result.questions().get(0);
            assertThat(firstQuestion.id()).isEqualTo(1L);
            assertThat(firstQuestion.code()).isEqualTo("Q1");
            assertThat(firstQuestion.text()).isEqualTo("How often do you feel anxious?");
            assertThat(firstQuestion.minScore()).isEqualTo(0);
            assertThat(firstQuestion.maxScore()).isEqualTo(3);
            assertThat(firstQuestion.orderIndex()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Submit Answers Tests")
    class SubmitAnswersTests {

        @Test
        @DisplayName("Should submit answers and calculate score")
        void shouldSubmitAnswersAndCalculateScore() {
            // Given
            testSession.setAnswers(new ArrayList<>());
            List<AnswerSubmissionDto> answers = List.of(
                    new AnswerSubmissionDto(1L, 2, null),
                    new AnswerSubmissionDto(2L, 1, null),
                    new AnswerSubmissionDto(3L, 2, null)
            );

            TestScoringService.TestInterpretation interpretation = new TestScoringService.TestInterpretation(
                    "Mild",
                    "Score of 5: Mild depression",
                    "Shows mild depressive symptoms",
                    List.of("Consider lifestyle changes", "Monitor symptoms")
            );

            when(sessionRepo.findById(1L)).thenReturn(Optional.of(testSession));
            when(scoringService.calculateTotalScore(testSession)).thenReturn(5);
            when(scoringService.interpretScore("PHQ-9", 5)).thenReturn(interpretation);
            when(sessionRepo.save(any(TestSession.class))).thenReturn(testSession);

            // When
            TestResultDto result = testSessionService.submitAnswers(1L, answers);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.testCode()).isEqualTo("PHQ-9");
            assertThat(result.totalScore()).isEqualTo(5);
            assertThat(result.interpretationLevel()).isEqualTo("Mild");

            verify(userService).incrementTestsCompleted(testUser.getId());
        }

        @Test
        @DisplayName("Should throw NotFoundException when session not found")
        void shouldThrowNotFoundExceptionWhenSessionNotFound() {
            // Given
            when(sessionRepo.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> testSessionService.submitAnswers(999L, List.of()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Session introuvable");
        }

        @Test
        @DisplayName("Should update session status to interpreted")
        void shouldUpdateSessionStatusToInterpreted() {
            // Given
            testSession.setAnswers(new ArrayList<>());
            List<AnswerSubmissionDto> answers = List.of(
                    new AnswerSubmissionDto(1L, 2, null)
            );

            TestScoringService.TestInterpretation interpretation = new TestScoringService.TestInterpretation(
                    "Minimal", "Score of 2", "Minimal symptoms", List.of()
            );

            when(sessionRepo.findById(1L)).thenReturn(Optional.of(testSession));
            when(scoringService.calculateTotalScore(testSession)).thenReturn(2);
            when(scoringService.interpretScore("PHQ-9", 2)).thenReturn(interpretation);
            when(sessionRepo.save(any(TestSession.class))).thenReturn(testSession);

            // When
            testSessionService.submitAnswers(1L, answers);

            // Then
            verify(sessionRepo).save(argThat(session ->
                    session.getStatus() == TestSessionStatus.INTERPRETED));
        }

        @Test
        @DisplayName("Should handle text responses")
        void shouldHandleTextResponses() {
            // Given
            testSession.setAnswers(new ArrayList<>());
            List<AnswerSubmissionDto> answers = List.of(
                    new AnswerSubmissionDto(1L, 2, "I feel anxious in social situations")
            );

            TestScoringService.TestInterpretation interpretation = new TestScoringService.TestInterpretation(
                    "Moderate", "Score description", "Interpretation", List.of()
            );

            when(sessionRepo.findById(1L)).thenReturn(Optional.of(testSession));
            when(scoringService.calculateTotalScore(testSession)).thenReturn(2);
            when(scoringService.interpretScore(anyString(), anyInt())).thenReturn(interpretation);
            when(sessionRepo.save(any(TestSession.class))).thenReturn(testSession);

            // When
            TestResultDto result = testSessionService.submitAnswers(1L, answers);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should include recommendations in result")
        void shouldIncludeRecommendationsInResult() {
            // Given
            testSession.setAnswers(new ArrayList<>());
            List<String> recommendations = List.of(
                    "Practice mindfulness",
                    "Exercise regularly",
                    "Seek professional help"
            );

            TestScoringService.TestInterpretation interpretation = new TestScoringService.TestInterpretation(
                    "Moderate", "Score description", "Interpretation", recommendations
            );

            when(sessionRepo.findById(1L)).thenReturn(Optional.of(testSession));
            when(scoringService.calculateTotalScore(any())).thenReturn(10);
            when(scoringService.interpretScore(anyString(), anyInt())).thenReturn(interpretation);
            when(sessionRepo.save(any(TestSession.class))).thenReturn(testSession);

            // When
            TestResultDto result = testSessionService.submitAnswers(1L, List.of());

            // Then
            assertThat(result.recommendations()).hasSize(3);
            assertThat(result.recommendations()).contains("Practice mindfulness");
        }
    }

    @Nested
    @DisplayName("Get Patient Test History Tests")
    class GetPatientTestHistoryTests {

        @Test
        @DisplayName("Should return test history for patient")
        void shouldReturnTestHistoryForPatient() {
            // Given
            testSession.setTotalScore(8);
            testSession.setInterpretationLevel("Mild");
            testSession.setInterpretationDescription("Mild symptoms");
            testSession.setSubmittedAt(Instant.now());
            testSession.setStatus(TestSessionStatus.INTERPRETED);

            TestScoringService.TestInterpretation interpretation = new TestScoringService.TestInterpretation(
                    "Mild", "Score of 8", "Mild symptoms", List.of()
            );

            when(sessionRepo.findByUserIdAndStatusOrderBySubmittedAtDesc(1L, TestSessionStatus.INTERPRETED))
                    .thenReturn(List.of(testSession));
            when(scoringService.interpretScore("PHQ-9", 8)).thenReturn(interpretation);

            // When
            List<TestResultDto> results = testSessionService.getPatientTestHistory(1L);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).testCode()).isEqualTo("PHQ-9");
            assertThat(results.get(0).totalScore()).isEqualTo(8);
        }

        @Test
        @DisplayName("Should return empty list when no history")
        void shouldReturnEmptyListWhenNoHistory() {
            // Given
            when(sessionRepo.findByUserIdAndStatusOrderBySubmittedAtDesc(1L, TestSessionStatus.INTERPRETED))
                    .thenReturn(Collections.emptyList());

            // When
            List<TestResultDto> results = testSessionService.getPatientTestHistory(1L);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return multiple results ordered by date")
        void shouldReturnMultipleResultsOrderedByDate() {
            // Given
            TestSession session2 = TestSession.builder()
                    .id(2L)
                    .testDefinition(testDefinition)
                    .user(testUser)
                    .totalScore(12)
                    .interpretationLevel("Moderate")
                    .submittedAt(Instant.now().minusSeconds(3600))
                    .status(TestSessionStatus.INTERPRETED)
                    .build();

            testSession.setTotalScore(5);
            testSession.setInterpretationLevel("Mild");
            testSession.setSubmittedAt(Instant.now());
            testSession.setStatus(TestSessionStatus.INTERPRETED);

            TestScoringService.TestInterpretation interpretation = new TestScoringService.TestInterpretation(
                    "Mild", "Score", "Interpretation", List.of()
            );

            when(sessionRepo.findByUserIdAndStatusOrderBySubmittedAtDesc(1L, TestSessionStatus.INTERPRETED))
                    .thenReturn(List.of(testSession, session2));
            when(scoringService.interpretScore(anyString(), anyInt())).thenReturn(interpretation);

            // When
            List<TestResultDto> results = testSessionService.getPatientTestHistory(1L);

            // Then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Get Test Result Tests")
    class GetTestResultTests {

        @Test
        @DisplayName("Should return test result by session id")
        void shouldReturnTestResultBySessionId() {
            // Given
            testSession.setTotalScore(10);
            testSession.setInterpretationLevel("Moderate");
            testSession.setInterpretationDescription("Moderate depressive symptoms");
            testSession.setSubmittedAt(Instant.now());
            testSession.setStatus(TestSessionStatus.INTERPRETED);

            TestScoringService.TestInterpretation interpretation = new TestScoringService.TestInterpretation(
                    "Moderate", "Score of 10", "Moderate symptoms", List.of("Rec 1", "Rec 2")
            );

            when(sessionRepo.findById(1L)).thenReturn(Optional.of(testSession));
            when(scoringService.interpretScore("PHQ-9", 10)).thenReturn(interpretation);

            // When
            TestResultDto result = testSessionService.getTestResult(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.sessionId()).isEqualTo(1L);
            assertThat(result.testCode()).isEqualTo("PHQ-9");
            assertThat(result.totalScore()).isEqualTo(10);
            assertThat(result.interpretationLevel()).isEqualTo("Moderate");
        }

        @Test
        @DisplayName("Should throw NotFoundException when session not found")
        void shouldThrowNotFoundExceptionWhenSessionNotFound() {
            // Given
            when(sessionRepo.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> testSessionService.getTestResult(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Session introuvable");
        }
    }
}
