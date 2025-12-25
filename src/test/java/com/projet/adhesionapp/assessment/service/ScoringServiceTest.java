package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.Answer;
import com.projet.adhesionapp.assessment.domain.ProfileScore;
import com.projet.adhesionapp.assessment.domain.TestSession;
import com.projet.adhesionapp.assessment.repo.AnswerRepository;
import com.projet.adhesionapp.assessment.repo.ProfileScoreRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringService - Tests unitaires")
class ScoringServiceTest {

    @Mock private ProfileScoreRepository scoreRepo;
    @Mock private AnswerRepository answerRepo;
    @InjectMocks private ScoringService service;

    private TestSession testSession;
    private Answer answer1;
    private Answer answer2;

    @BeforeEach
    void setUp() {
        testSession = TestSession.builder().id(1L).build();
        
        answer1 = Answer.builder()
                .id(1L)
                .session(testSession)
                .score(3)
                .build();
        
        answer2 = Answer.builder()
                .id(2L)
                .session(testSession)
                .score(5)
                .build();
    }

    @Nested
    @DisplayName("computeScore")
    class ComputeScoreTests {
        
        @Test
        @DisplayName("Calcule le score moyen des réponses")
        void shouldComputeAverageScore() {
            when(answerRepo.findAll()).thenReturn(List.of(answer1, answer2));
            when(scoreRepo.save(any(ProfileScore.class))).thenAnswer(inv -> {
                ProfileScore ps = inv.getArgument(0);
                ps.setId(1L);
                return ps;
            });

            ProfileScore result = service.computeScore(1L, "DEPRESSION");

            assertThat(result.getDimension()).isEqualTo("DEPRESSION");
            assertThat(result.getScore()).isEqualTo(4.0); // (3+5)/2 = 4
            assertThat(result.getComputedAt()).isNotNull();
            verify(scoreRepo).save(any(ProfileScore.class));
        }

        @Test
        @DisplayName("Retourne 0 si aucune réponse")
        void shouldReturnZeroIfNoAnswers() {
            when(answerRepo.findAll()).thenReturn(List.of());
            when(scoreRepo.save(any(ProfileScore.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileScore result = service.computeScore(1L, "ANXIETY");

            assertThat(result.getScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Filtre les réponses par session")
        void shouldFilterAnswersBySession() {
            TestSession otherSession = TestSession.builder().id(2L).build();
            Answer otherAnswer = Answer.builder()
                    .id(3L)
                    .session(otherSession)
                    .score(10)
                    .build();
            
            when(answerRepo.findAll()).thenReturn(List.of(answer1, answer2, otherAnswer));
            when(scoreRepo.save(any(ProfileScore.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileScore result = service.computeScore(1L, "TEST");

            // Should only average answers from session 1 (3+5)/2 = 4
            assertThat(result.getScore()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Gère une seule réponse")
        void shouldHandleSingleAnswer() {
            when(answerRepo.findAll()).thenReturn(List.of(answer1));
            when(scoreRepo.save(any(ProfileScore.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileScore result = service.computeScore(1L, "STRESS");

            assertThat(result.getScore()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Calcule avec plusieurs dimensions")
        void shouldCalculateForMultipleDimensions() {
            when(answerRepo.findAll()).thenReturn(List.of(answer1, answer2));
            when(scoreRepo.save(any(ProfileScore.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileScore result1 = service.computeScore(1L, "PHQ9");
            ProfileScore result2 = service.computeScore(1L, "GAD7");

            assertThat(result1.getDimension()).isEqualTo("PHQ9");
            assertThat(result2.getDimension()).isEqualTo("GAD7");
        }
    }
}
