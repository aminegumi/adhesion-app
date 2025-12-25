package com.projet.adhesionapp.assessment.web;

import com.projet.adhesionapp.assessment.model.*;
import com.projet.adhesionapp.assessment.service.TestSessionService;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur de session.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionController - Tests unitaires")
class SessionControllerTest {

    @Mock private TestSessionService sessionService;
    @Mock private UserService userService;
    @InjectMocks private SessionController controller;

    private User testUser;
    private TestSessionDto testSessionDto;
    private TestResultDto testResultDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@example.com").build();
        testSessionDto = new TestSessionDto(1L, 1L, "PHQ9", "PHQ-9", Instant.now(), List.of());
        testResultDto = new TestResultDto(
                1L, "PHQ9", "PHQ-9", 15,
                "MODERATE", "Score modéré", "Dépression modérée",
                List.of("Consulter un professionnel"), Instant.now()
        );
    }

    @Nested
    @DisplayName("start")
    class StartTests {
        @Test
        void shouldStartSession() {
            StartSessionRequest request = new StartSessionRequest(1L, 1L);
            when(userService.findById(1L)).thenReturn(testUser);
            when(sessionService.startSession(1L, testUser)).thenReturn(testSessionDto);

            TestSessionDto result = controller.start(request);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.testCode()).isEqualTo("PHQ9");
        }
    }

    @Nested
    @DisplayName("submit")
    class SubmitTests {
        @Test
        void shouldSubmitAnswers() {
            List<AnswerSubmissionDto> answers = List.of(
                    new AnswerSubmissionDto(1L, 2, null),
                    new AnswerSubmissionDto(2L, 3, null)
            );
            when(sessionService.submitAnswers(1L, answers)).thenReturn(testResultDto);

            TestResultDto result = controller.submit(1L, answers);

            assertThat(result.totalScore()).isEqualTo(15);
            assertThat(result.interpretationLevel()).isEqualTo("MODERATE");
        }
    }

    @Nested
    @DisplayName("getPatientHistory")
    class GetPatientHistoryTests {
        @Test
        void shouldReturnHistory() {
            when(sessionService.getPatientTestHistory(1L)).thenReturn(List.of(testResultDto));

            List<TestResultDto> result = controller.getPatientHistory(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).testCode()).isEqualTo("PHQ9");
        }

        @Test
        void shouldReturnEmptyHistory() {
            when(sessionService.getPatientTestHistory(999L)).thenReturn(List.of());

            List<TestResultDto> result = controller.getPatientHistory(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getResult")
    class GetResultTests {
        @Test
        void shouldReturnResult() {
            when(sessionService.getTestResult(1L)).thenReturn(testResultDto);

            TestResultDto result = controller.getResult(1L);

            assertThat(result.sessionId()).isEqualTo(1L);
            assertThat(result.recommendations()).contains("Consulter un professionnel");
        }
    }
}
