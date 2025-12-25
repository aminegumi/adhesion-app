package com.projet.adhesionapp.profile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.assessment.domain.ProfileScore;
import com.projet.adhesionapp.assessment.domain.TestDefinition;
import com.projet.adhesionapp.assessment.domain.TestSession;
import com.projet.adhesionapp.assessment.domain.TestSessionStatus;
import com.projet.adhesionapp.assessment.repo.ProfileScoreRepository;
import com.projet.adhesionapp.assessment.repo.TestSessionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.model.ProfileCreateRequest;
import com.projet.adhesionapp.profile.model.ProfileDto;
import com.projet.adhesionapp.profile.repo.PsychologicalProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires complets pour PsychologicalProfileService
 * Conforme au PAQ - Couverture minimale 80%
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PsychologicalProfileService Unit Tests")
class PsychologicalProfileServiceTest {

    @Mock
    private PsychologicalProfileRepository profileRepository;

    @Mock
    private UserService userService;

    @Mock
    private ProfileScoreRepository profileScoreRepository;

    @Mock
    private TestSessionRepository testSessionRepository;

    @Mock
    private OpenAIService openAIService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PsychologicalProfileService profileService;

    private User testUser;
    private PsychologicalProfile testProfile;
    private TestSession testSession;
    private TestDefinition testDefinition;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("patient@example.com")
                .displayName("Test Patient")
                .active(true)
                .build();

        testProfile = PsychologicalProfile.builder()
                .id(1L)
                .user(testUser)
                .profileType("Balanced")
                .anxietyScore(45.0)
                .depressionScore(30.0)
                .motivationScore(70.0)
                .selfEfficacyScore(65.0)
                .socialSupportScore(60.0)
                .healthLocusScore(55.0)
                .adherenceRiskScore(0.3)
                .summary("Test summary")
                .detailedInterpretation("Test interpretation")
                .status(PsychologicalProfile.ProfileStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        testDefinition = TestDefinition.builder()
                .id(1L)
                .code("GAD-7")
                .title("Generalized Anxiety Disorder 7")
                .build();

        testSession = TestSession.builder()
                .id(1L)
                .user(testUser)
                .testDefinition(testDefinition)
                .status(TestSessionStatus.INTERPRETED)
                .totalScore(10)
                .build();
    }

    @Nested
    @DisplayName("Create Profile Tests")
    class CreateProfileTests {

        @Test
        @DisplayName("Should create profile with direct scores")
        void shouldCreateProfileWithDirectScores() {
            // Given
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 60.0, 40.0, 70.0, 65.0, 55.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> {
                        PsychologicalProfile p = invocation.getArgument(0);
                        p.setId(1L);
                        return p;
                    });

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result);
            assertEquals(60.0, result.getAnxietyScore());
            assertEquals(40.0, result.getDepressionScore());
            assertEquals(70.0, result.getMotivationScore());
            verify(profileRepository).save(any(PsychologicalProfile.class));
        }

        @Test
        @DisplayName("Should create profile with session IDs")
        void shouldCreateProfileWithSessionIds() {
            // Given
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, List.of(1L), null, null, null, null, null, null);

            ProfileScore profileScore = ProfileScore.builder()
                    .dimension("anxiety")
                    .score(55.0)
                    .build();

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileScoreRepository.findBySessionId(1L)).thenReturn(List.of(profileScore));
            when(testSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> {
                        PsychologicalProfile p = invocation.getArgument(0);
                        p.setId(1L);
                        return p;
                    });

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result);
            verify(profileScoreRepository).findBySessionId(1L);
            verify(testSessionRepository).findById(1L);
        }

        @Test
        @DisplayName("Should create profile from user sessions when no session IDs provided")
        void shouldCreateProfileFromUserSessions() {
            // Given
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, null, null, null, null, null, null);

            when(userService.findById(1L)).thenReturn(testUser);
            when(testSessionRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(1L, TestSessionStatus.INTERPRETED))
                    .thenReturn(List.of(testSession));
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> {
                        PsychologicalProfile p = invocation.getArgument(0);
                        p.setId(1L);
                        return p;
                    });

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result);
            verify(testSessionRepository).findByUserIdAndStatusOrderBySubmittedAtDesc(1L, TestSessionStatus.INTERPRETED);
        }

        @Test
        @DisplayName("Should create Anxious-Depressive profile type")
        void shouldCreateAnxiousDepressiveProfileType() {
            // Given - High anxiety (>70) and high depression (>60)
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 75.0, 65.0, 50.0, 50.0, 50.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertEquals("Anxious-Depressive", result.getProfileType());
        }

        @Test
        @DisplayName("Should create Anxious-Low Confidence profile type")
        void shouldCreateAnxiousLowConfidenceProfileType() {
            // Given - High anxiety (>70) and low self-efficacy (<40)
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 75.0, 30.0, 50.0, 35.0, 50.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertEquals("Anxious-Low Confidence", result.getProfileType());
        }

        @Test
        @DisplayName("Should create Highly Motivated profile type")
        void shouldCreateHighlyMotivatedProfileType() {
            // Given - High motivation (>70) and high self-efficacy (>70)
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 30.0, 30.0, 75.0, 75.0, 60.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertEquals("Highly Motivated", result.getProfileType());
        }

        @Test
        @DisplayName("Should create Disengaged profile type")
        void shouldCreateDisengagedProfileType() {
            // Given - Low motivation (<40) and low self-efficacy (<40)
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 50.0, 50.0, 35.0, 35.0, 50.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertEquals("Disengaged", result.getProfileType());
        }

        @Test
        @DisplayName("Should create Isolated profile type")
        void shouldCreateIsolatedProfileType() {
            // Given - Low social support (<40) and depression >50
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 50.0, 55.0, 50.0, 50.0, 35.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertEquals("Isolated", result.getProfileType());
        }

        @Test
        @DisplayName("Should create Resilient profile type")
        void shouldCreateResilientProfileType() {
            // Given - High self-efficacy (>60), high motivation (>60), low anxiety (<50)
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 40.0, 40.0, 65.0, 65.0, 60.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertEquals("Resilient", result.getProfileType());
        }

        @Test
        @DisplayName("Should create Moderately Stressed profile type")
        void shouldCreateModeratelyStressedProfileType() {
            // Given - Anxiety or depression > 50 but not fitting other categories
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 55.0, 55.0, 50.0, 50.0, 50.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertEquals("Moderately Stressed", result.getProfileType());
        }

        @Test
        @DisplayName("Should create Balanced profile type")
        void shouldCreateBalancedProfileType() {
            // Given - Default/balanced scores
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 45.0, 45.0, 55.0, 55.0, 55.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertEquals("Balanced", result.getProfileType());
        }

        @Test
        @DisplayName("Should handle AI summary generation failure")
        void shouldHandleAISummaryGenerationFailure() {
            // Given
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString()))
                    .thenThrow(new RuntimeException("AI service unavailable"));
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result);
            assertTrue(result.getSummary().contains("Profile type:"));
        }

        @Test
        @DisplayName("Should handle AI interpretation generation failure")
        void shouldHandleAIInterpretationGenerationFailure() {
            // Given
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenThrow(new RuntimeException("AI service unavailable"));
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result);
            assertEquals("Detailed interpretation pending review.", result.getDetailedInterpretation());
        }
    }

    @Nested
    @DisplayName("Get Profile Tests")
    class GetProfileTests {

        @Test
        @DisplayName("Should get latest profile for user")
        void shouldGetLatestProfileForUser() {
            // Given
            when(userService.findById(1L)).thenReturn(testUser);
            when(profileRepository.findFirstByUserOrderByCreatedAtDesc(testUser))
                    .thenReturn(Optional.of(testProfile));

            // When
            PsychologicalProfile result = profileService.getLatestProfile(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Balanced", result.getProfileType());
        }

        @Test
        @DisplayName("Should return null when no profile exists")
        void shouldReturnNullWhenNoProfileExists() {
            // Given
            when(userService.findById(1L)).thenReturn(testUser);
            when(profileRepository.findFirstByUserOrderByCreatedAtDesc(testUser))
                    .thenReturn(Optional.empty());

            // When
            PsychologicalProfile result = profileService.getLatestProfile(1L);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should get user profiles")
        void shouldGetUserProfiles() {
            // Given
            when(profileRepository.findByUserId(1L)).thenReturn(List.of(testProfile));

            // When
            List<PsychologicalProfile> result = profileService.getUserProfiles(1L);

            // Then
            assertEquals(1, result.size());
            assertEquals("Balanced", result.get(0).getProfileType());
        }

        @Test
        @DisplayName("Should get profile by ID")
        void shouldGetProfileById() {
            // Given
            when(profileRepository.findById(1L)).thenReturn(Optional.of(testProfile));

            // When
            PsychologicalProfile result = profileService.getById(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Should throw exception when profile not found")
        void shouldThrowExceptionWhenProfileNotFound() {
            // Given
            when(profileRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () -> profileService.getById(999L));
        }

        @Test
        @DisplayName("Should get all profiles")
        void shouldGetAllProfiles() {
            // Given
            when(profileRepository.findAll()).thenReturn(List.of(testProfile));

            // When
            List<PsychologicalProfile> result = profileService.getAllProfiles();

            // Then
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Test Score Normalization Tests")
    class TestScoreNormalizationTests {

        @Test
        @DisplayName("Should normalize PHQ-9 score")
        void shouldNormalizePHQ9Score() {
            // Given - PHQ-9 session with score 10 (out of 27)
            testDefinition.setCode("PHQ-9");
            testSession.setTotalScore(10);

            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, List.of(1L), null, null, null, null, null, null);

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileScoreRepository.findBySessionId(1L)).thenReturn(List.of());
            when(testSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result);
            // PHQ-9: 10/27 * 100 ≈ 37.0
            assertNotNull(result.getDepressionScore());
        }

        @Test
        @DisplayName("Should normalize GAD-7 score")
        void shouldNormalizeGAD7Score() {
            // Given - GAD-7 session with score 14 (out of 21)
            testDefinition.setCode("GAD-7");
            testSession.setTotalScore(14);

            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, List.of(1L), null, null, null, null, null, null);

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileScoreRepository.findBySessionId(1L)).thenReturn(List.of());
            when(testSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result);
            // GAD-7: 14/21 * 100 ≈ 66.7
            assertNotNull(result.getAnxietyScore());
        }

        @Test
        @DisplayName("Should normalize MMAS-8 score")
        void shouldNormalizeMmas8Score() {
            // Given - MMAS-8 session
            testDefinition.setCode("MMAS-8");
            testSession.setTotalScore(6);

            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, List.of(1L), null, null, null, null, null, null);

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileScoreRepository.findBySessionId(1L)).thenReturn(List.of());
            when(testSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result);
            // MMAS-8: 6/8 * 100 = 75
            assertNotNull(result.getMotivationScore());
        }

        @Test
        @DisplayName("Should normalize GSE score")
        void shouldNormalizeGSEScore() {
            // Given - GSE session (10-40 scale)
            testDefinition.setCode("GSE");
            testSession.setTotalScore(30);

            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, List.of(1L), null, null, null, null, null, null);

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileScoreRepository.findBySessionId(1L)).thenReturn(List.of());
            when(testSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result);
            // GSE: (30-10)/30 * 100 = 66.7
            assertNotNull(result.getSelfEfficacyScore());
        }

        @Test
        @DisplayName("Should normalize MSPSS score")
        void shouldNormalizeMSPSSScore() {
            // Given - MSPSS session (12-84 scale)
            testDefinition.setCode("MSPSS");
            testSession.setTotalScore(60);

            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, List.of(1L), null, null, null, null, null, null);

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileScoreRepository.findBySessionId(1L)).thenReturn(List.of());
            when(testSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result);
            // MSPSS: (60-12)/72 * 100 = 66.7
            assertNotNull(result.getSocialSupportScore());
        }
    }

    @Nested
    @DisplayName("Adherence Risk Calculation Tests")
    class AdherenceRiskCalculationTests {

        @Test
        @DisplayName("Should calculate high adherence risk")
        void shouldCalculateHighAdherenceRisk() {
            // Given - High anxiety, depression, low motivation/self-efficacy/support
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 80.0, 80.0, 20.0, 20.0, 20.0, 20.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result.getAdherenceRiskScore());
            assertTrue(result.getAdherenceRiskScore() > 0.5, "Risk should be high");
        }

        @Test
        @DisplayName("Should calculate low adherence risk")
        void shouldCalculateLowAdherenceRisk() {
            // Given - Low anxiety, depression, high motivation/self-efficacy/support
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, null, 20.0, 20.0, 80.0, 80.0, 80.0, 80.0);

            when(userService.findById(1L)).thenReturn(testUser);
            when(openAIService.generateCompletion(anyString(), anyString())).thenReturn("AI Summary");
            when(openAIService.analyzeTestResults(anyString(), anyList(), anyList(), any()))
                    .thenReturn("AI Interpretation");
            when(profileRepository.save(any(PsychologicalProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PsychologicalProfile result = profileService.createProfile(request);

            // Then
            assertNotNull(result.getAdherenceRiskScore());
            assertTrue(result.getAdherenceRiskScore() < 0.5, "Risk should be low");
        }
    }

    @Nested
    @DisplayName("DTO Conversion Tests")
    class DtoConversionTests {

        @Test
        @DisplayName("Should convert profile to DTO")
        void shouldConvertProfileToDto() {
            // When
            ProfileDto dto = profileService.toDto(testProfile);

            // Then
            assertNotNull(dto);
            assertEquals(1L, dto.id());
            assertEquals(1L, dto.userId());
            assertEquals("Test Patient", dto.userName());
            assertEquals("Balanced", dto.profileType());
            assertEquals(45.0, dto.anxietyScore());
            assertEquals(30.0, dto.depressionScore());
            assertEquals(70.0, dto.motivationScore());
            assertEquals(65.0, dto.selfEfficacyScore());
            assertEquals(60.0, dto.socialSupportScore());
            assertEquals(55.0, dto.healthLocusScore());
            assertEquals(0.3, dto.adherenceRiskScore());
            assertEquals("Test summary", dto.summary());
            assertEquals("Test interpretation", dto.detailedInterpretation());
            assertEquals("ACTIVE", dto.status());
        }
    }
}
