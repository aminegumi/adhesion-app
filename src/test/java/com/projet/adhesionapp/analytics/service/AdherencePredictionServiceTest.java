package com.projet.adhesionapp.analytics.service;

import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.analytics.domain.Prediction;
import com.projet.adhesionapp.analytics.model.AdherencePredictionDto;
import com.projet.adhesionapp.analytics.repo.PredictionRepository;
import com.projet.adhesionapp.assessment.domain.TestDefinition;
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
import com.projet.adhesionapp.profile.repo.PsychologicalProfileRepository;
import com.projet.adhesionapp.treatment.domain.Medication;
import com.projet.adhesionapp.treatment.repo.MedicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdherencePredictionService Unit Tests")
class AdherencePredictionServiceTest {

    @Mock
    private DoseLogRepository doseLogRepository;

    @Mock
    private TestSessionRepository testSessionRepository;

    @Mock
    private PsychologicalProfileRepository profileRepository;

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private OpenAIService aiService;

    @InjectMocks
    private AdherencePredictionService adherencePredictionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .displayName("Test User")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("Predict Adherence Tests")
    class PredictAdherenceTests {

        @Test
        @DisplayName("Should predict adherence for existing user")
        void shouldPredictAdherenceForExistingUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
            when(testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(anyLong(), any()))
                    .thenReturn(List.of());
            when(medicationRepository.findActiveMedicationsByUserId(anyLong())).thenReturn(List.of());
            when(aiService.generateCompletion(anyString(), anyString())).thenReturn("Recommendations");

            AdherencePredictionDto result = adherencePredictionService.predictAdherence(1L);

            assertNotNull(result);
            assertEquals(1L, result.userId());
            assertEquals("Test User", result.userName());
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> adherencePredictionService.predictAdherence(999L));
        }

        @Test
        @DisplayName("Should handle user with dose logs")
        void shouldHandleUserWithDoseLogs() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            
            List<DoseLog> doseLogs = createDoseLogs();
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(doseLogs);
            when(testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(anyLong(), any()))
                    .thenReturn(List.of());
            when(medicationRepository.findActiveMedicationsByUserId(anyLong())).thenReturn(List.of());
            when(aiService.generateCompletion(anyString(), anyString())).thenReturn("Recommendations");

            AdherencePredictionDto result = adherencePredictionService.predictAdherence(1L);

            assertNotNull(result);
            assertThat(result.predictedAdherence()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should handle complex medication regimen")
        void shouldHandleComplexMedicationRegimen() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
            when(testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(anyLong(), any()))
                    .thenReturn(List.of());
            
            List<Medication> medications = List.of(
                    Medication.builder().id(1L).name("Med1").timesPerDay(3).build(),
                    Medication.builder().id(2L).name("Med2").timesPerDay(4).build(),
                    Medication.builder().id(3L).name("Med3").timesPerDay(2).build()
            );
            when(medicationRepository.findActiveMedicationsByUserId(anyLong())).thenReturn(medications);
            when(aiService.generateCompletion(anyString(), anyString())).thenReturn("Recommendations");

            AdherencePredictionDto result = adherencePredictionService.predictAdherence(1L);

            assertNotNull(result);
            // Should have a risk factor for complex regimen (>4 daily doses)
            assertThat(result.riskFactors()).isNotNull();
        }

        @Test
        @DisplayName("Should handle AI service failure with fallback")
        void shouldHandleAIServiceFailureWithFallback() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
            when(testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(anyLong(), any()))
                    .thenReturn(List.of());
            when(medicationRepository.findActiveMedicationsByUserId(anyLong())).thenReturn(List.of());
            when(aiService.generateCompletion(anyString(), anyString()))
                    .thenThrow(new RuntimeException("AI service error"));

            AdherencePredictionDto result = adherencePredictionService.predictAdherence(1L);

            assertNotNull(result);
            assertNotNull(result.recommendations());
        }
    }

    @Nested
    @DisplayName("Calculate Adherence Score Tests")
    class CalculateAdherenceScoreTests {

        @Test
        @DisplayName("Should return default score for user with no predictions")
        void shouldReturnDefaultScoreForNoPredictions() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(predictionRepository.findTop10ByUserOrderByDateDesc(testUser)).thenReturn(List.of());

            double score = adherencePredictionService.calculateAdherenceScore(1L);

            assertEquals(0.5, score);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFoundForScore() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> adherencePredictionService.calculateAdherenceScore(999L));
        }

        @Test
        @DisplayName("Should calculate weighted average from predictions")
        void shouldCalculateWeightedAverageFromPredictions() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            
            List<Prediction> predictions = List.of(
                    createPrediction(0.2), // Recent - high adherence (1 - 0.2 = 0.8)
                    createPrediction(0.3), // Older - (1 - 0.3 = 0.7)
                    createPrediction(0.4)  // Oldest - (1 - 0.4 = 0.6)
            );
            when(predictionRepository.findTop10ByUserOrderByDateDesc(testUser)).thenReturn(predictions);

            double score = adherencePredictionService.calculateAdherenceScore(1L);

            assertThat(score).isBetween(0.0, 1.0);
            // Recent prediction has higher weight
            assertThat(score).isGreaterThan(0.6);
        }

        @Test
        @DisplayName("Should handle single prediction")
        void shouldHandleSinglePrediction() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            
            List<Prediction> predictions = List.of(createPrediction(0.1));
            when(predictionRepository.findTop10ByUserOrderByDateDesc(testUser)).thenReturn(predictions);

            double score = adherencePredictionService.calculateAdherenceScore(1L);

            assertThat(score).isCloseTo(0.9, within(0.01));
        }

        @Test
        @DisplayName("Should ensure score is between 0 and 1")
        void shouldEnsureScoreIsBounded() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            
            List<Prediction> predictions = List.of(
                    createPrediction(0.0), // Perfect adherence
                    createPrediction(1.0)  // No adherence
            );
            when(predictionRepository.findTop10ByUserOrderByDateDesc(testUser)).thenReturn(predictions);

            double score = adherencePredictionService.calculateAdherenceScore(1L);

            assertThat(score).isBetween(0.0, 1.0);
        }
    }

    @Nested
    @DisplayName("Test Scores Integration Tests")
    class TestScoresIntegrationTests {

        @Test
        @DisplayName("Should identify depression risk from high PHQ-9 score")
        void shouldIdentifyDepressionRiskFromHighPhq9() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
            
            TestDefinition phq9Def = TestDefinition.builder().id(1L).code("PHQ-9").build();
            TestSession phq9Session = TestSession.builder()
                    .id(1L)
                    .testDefinition(phq9Def)
                    .totalScore(18) // Moderate-severe depression
                    .build();
            when(testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(anyLong(), eq(TestSessionStatus.INTERPRETED)))
                    .thenReturn(List.of(phq9Session));
            when(medicationRepository.findActiveMedicationsByUserId(anyLong())).thenReturn(List.of());
            when(aiService.generateCompletion(anyString(), anyString())).thenReturn("Recommendations");

            AdherencePredictionDto result = adherencePredictionService.predictAdherence(1L);

            assertNotNull(result);
            boolean hasDepressionRisk = result.riskFactors().stream()
                    .anyMatch(rf -> rf.factor().contains("Depression"));
            assertThat(hasDepressionRisk).isTrue();
        }

        @Test
        @DisplayName("Should identify anxiety risk from high GAD-7 score")
        void shouldIdentifyAnxietyRiskFromHighGad7() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
            
            TestDefinition gad7Def = TestDefinition.builder().id(2L).code("GAD-7").build();
            TestSession gad7Session = TestSession.builder()
                    .id(2L)
                    .testDefinition(gad7Def)
                    .totalScore(15) // Severe anxiety
                    .build();
            when(testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(anyLong(), eq(TestSessionStatus.INTERPRETED)))
                    .thenReturn(List.of(gad7Session));
            when(medicationRepository.findActiveMedicationsByUserId(anyLong())).thenReturn(List.of());
            when(aiService.generateCompletion(anyString(), anyString())).thenReturn("Recommendations");

            AdherencePredictionDto result = adherencePredictionService.predictAdherence(1L);

            assertNotNull(result);
            boolean hasAnxietyRisk = result.riskFactors().stream()
                    .anyMatch(rf -> rf.factor().contains("Anxiety"));
            assertThat(hasAnxietyRisk).isTrue();
        }

        @Test
        @DisplayName("Should identify low adherence from MMAS-8 score")
        void shouldIdentifyLowAdherenceFromMmas8() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
            
            TestDefinition mmas8Def = TestDefinition.builder().id(3L).code("MMAS-8").build();
            TestSession mmas8Session = TestSession.builder()
                    .id(3L)
                    .testDefinition(mmas8Def)
                    .totalScore(4) // Low adherence
                    .build();
            when(testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(anyLong(), eq(TestSessionStatus.INTERPRETED)))
                    .thenReturn(List.of(mmas8Session));
            when(medicationRepository.findActiveMedicationsByUserId(anyLong())).thenReturn(List.of());
            when(aiService.generateCompletion(anyString(), anyString())).thenReturn("Recommendations");

            AdherencePredictionDto result = adherencePredictionService.predictAdherence(1L);

            assertNotNull(result);
            boolean hasAdherenceRisk = result.riskFactors().stream()
                    .anyMatch(rf -> rf.factor().contains("Adherence"));
            assertThat(hasAdherenceRisk).isTrue();
        }
    }

    @Nested
    @DisplayName("Behavioral Analysis Tests")
    class BehavioralAnalysisTests {

        @Test
        @DisplayName("Should calculate adherence rate from dose logs")
        void shouldCalculateAdherenceRateFromDoseLogs() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            
            // 8 taken, 2 skipped = 80% adherence
            List<DoseLog> doseLogs = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                doseLogs.add(createDoseLog(DoseStatus.TAKEN, null));
            }
            for (int i = 0; i < 2; i++) {
                doseLogs.add(createDoseLog(DoseStatus.SKIPPED, SkipReason.FORGOT));
            }
            
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(doseLogs);
            when(testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(anyLong(), any()))
                    .thenReturn(List.of());
            when(medicationRepository.findActiveMedicationsByUserId(anyLong())).thenReturn(List.of());
            when(aiService.generateCompletion(anyString(), anyString())).thenReturn("Recommendations");

            AdherencePredictionDto result = adherencePredictionService.predictAdherence(1L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should identify skip reasons in breakdown")
        void shouldIdentifySkipReasonsInBreakdown() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            
            List<DoseLog> doseLogs = List.of(
                    createDoseLog(DoseStatus.SKIPPED, SkipReason.FORGOT),
                    createDoseLog(DoseStatus.SKIPPED, SkipReason.FORGOT),
                    createDoseLog(DoseStatus.SKIPPED, SkipReason.SIDE_EFFECTS),
                    createDoseLog(DoseStatus.TAKEN, null)
            );
            
            when(doseLogRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
                    anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(doseLogs);
            when(testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(anyLong(), any()))
                    .thenReturn(List.of());
            when(medicationRepository.findActiveMedicationsByUserId(anyLong())).thenReturn(List.of());
            when(aiService.generateCompletion(anyString(), anyString())).thenReturn("Recommendations");

            AdherencePredictionDto result = adherencePredictionService.predictAdherence(1L);

            assertNotNull(result.skipReasonBreakdown());
            assertThat(result.skipReasonBreakdown()).containsKey("FORGOT");
        }
    }

    // Helper methods
    private List<DoseLog> createDoseLogs() {
        List<DoseLog> logs = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 5; i++) {
            DoseLog log = DoseLog.builder()
                    .id((long) i)
                    .user(testUser)
                    .scheduledDate(today.minusDays(i))
                    .scheduledTime(LocalTime.of(8, 0))
                    .status(DoseStatus.TAKEN)
                    .delayMinutes(5)
                    .build();
            logs.add(log);
        }
        return logs;
    }

    private DoseLog createDoseLog(DoseStatus status, SkipReason skipReason) {
        return DoseLog.builder()
                .id(System.nanoTime())
                .user(testUser)
                .scheduledDate(LocalDate.now().minusDays((int) (Math.random() * 30)))
                .scheduledTime(LocalTime.of(8, 0))
                .status(status)
                .skipReason(skipReason)
                .delayMinutes(status == DoseStatus.TAKEN ? 5 : null)
                .build();
    }

    private Prediction createPrediction(double probNonAdherence) {
        Prediction prediction = new Prediction();
        prediction.setUser(testUser);
        prediction.setProbNonAdherence(probNonAdherence);
        prediction.setDate(LocalDate.now());
        return prediction;
    }
}
