package com.projet.adhesionapp.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.service.PsychologicalProfileService;
import com.projet.adhesionapp.recommendation.domain.Recommendation;
import com.projet.adhesionapp.recommendation.model.RecommendationDto;
import com.projet.adhesionapp.recommendation.repo.RecommendationRepository;
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
 * Tests unitaires complets pour RecommendationService
 * Conforme au PAQ - Couverture minimale 80%
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService Unit Tests")
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private UserService userService;

    @Mock
    private PsychologicalProfileService profileService;

    @Mock
    private OpenAIService openAIService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RecommendationService recommendationService;

    private User testUser;
    private PsychologicalProfile testProfile;
    private Recommendation testRecommendation;

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
                .profileType("Anxious-Depressive")
                .adherenceRiskScore(0.6)
                .summary("Patient shows elevated anxiety and depression scores.")
                .status(PsychologicalProfile.ProfileStatus.ACTIVE)
                .build();

        testRecommendation = Recommendation.builder()
                .id(1L)
                .user(testUser)
                .profile(testProfile)
                .category("Mental Health")
                .priority(1)
                .title("Practice daily mindfulness")
                .description("Engage in 10 minutes of mindfulness meditation each morning")
                .difficulty(Recommendation.Difficulty.EASY)
                .timeFrame("Daily")
                .status(Recommendation.RecommendationStatus.ACTIVE)
                .completed(false)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Generate Recommendations Tests")
    class GenerateRecommendationsTests {

        @Test
        @DisplayName("Should generate recommendations with numbered format")
        void shouldGenerateRecommendationsWithNumberedFormat() {
            // Given
            String aiResponse = """
                1. Practice daily mindfulness
                Engage in 10 minutes of mindfulness meditation each morning to reduce anxiety.
                2. Exercise regularly
                Take a 15-minute walk daily to improve mood and overall well-being.
                3. Connect with support network
                Reach out to family or friends at least once a week for social support.
                """;

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("Practice daily mindfulness", result.get(0).getTitle());
            assertEquals("Exercise regularly", result.get(1).getTitle());
            assertEquals("Connect with support network", result.get(2).getTitle());
            verify(recommendationRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw exception when no profile exists")
        void shouldThrowExceptionWhenNoProfileExists() {
            // Given
            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(null);

            // When & Then
            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> recommendationService.generateRecommendations(1L));
            assertTrue(exception.getMessage().contains("No psychological profile found"));
        }

        @Test
        @DisplayName("Should create general recommendation when no structured format")
        void shouldCreateGeneralRecommendationWhenNoStructuredFormat() {
            // Given
            String aiResponse = "Consider improving your daily routine with regular exercise and meditation practices.";

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("General", result.get(0).getCategory());
            assertEquals("Personalized Health Recommendations", result.get(0).getTitle());
        }

        @Test
        @DisplayName("Should categorize medication recommendations")
        void shouldCategorizeMedicationRecommendations() {
            // Given
            String aiResponse = """
                1. Take medication on time
                Set reminders to take your prescribed medication at the same time each day.
                """;

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertEquals("Medication", result.get(0).getCategory());
        }

        @Test
        @DisplayName("Should categorize exercise recommendations")
        void shouldCategorizeExerciseRecommendations() {
            // Given
            String aiResponse = """
                1. Daily walking routine
                Take a 30-minute walk in the park to improve physical health.
                """;

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertEquals("Exercise", result.get(0).getCategory());
        }

        @Test
        @DisplayName("Should categorize lifestyle recommendations")
        void shouldCategorizeLifestyleRecommendations() {
            // Given
            String aiResponse = """
                1. Improve sleep hygiene
                Get 8 hours of rest each night and avoid screens before bed.
                """;

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertEquals("Lifestyle", result.get(0).getCategory());
        }

        @Test
        @DisplayName("Should categorize social recommendations")
        void shouldCategorizeSocialRecommendations() {
            // Given
            String aiResponse = """
                1. Strengthen family connections
                Spend quality time with family members each week for better support.
                """;

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertEquals("Social", result.get(0).getCategory());
        }

        @Test
        @DisplayName("Should categorize mental health recommendations")
        void shouldCategorizeMentalHealthRecommendations() {
            // Given - Use content that contains mental health keywords but NOT exercise keywords
            String aiResponse = """
                1. Practice therapy for anxiety
                Use mindfulness techniques to manage stress and reduce anxiety levels.
                """;

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertEquals("Mental Health", result.get(0).getCategory());
        }

        @Test
        @DisplayName("Should categorize nutrition recommendations")
        void shouldCategorizeNutritionRecommendations() {
            // Given
            String aiResponse = """
                1. Healthy eating habits
                Maintain a balanced diet rich in fruits and vegetables for better nutrition.
                """;

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertEquals("Nutrition", result.get(0).getCategory());
        }

        @Test
        @DisplayName("Should determine easy difficulty")
        void shouldDetermineEasyDifficulty() {
            // Given
            String aiResponse = """
                1. Simple breathing exercise
                Just take 5 deep breaths whenever you feel stressed. This is an easy practice.
                """;

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertEquals(Recommendation.Difficulty.EASY, result.get(0).getDifficulty());
        }

        @Test
        @DisplayName("Should determine hard difficulty")
        void shouldDetermineHardDifficulty() {
            // Given
            String aiResponse = """
                1. Take on a significant challenge
                This will be a difficult undertaking that requires commitment.
                """;

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertEquals(Recommendation.Difficulty.HARD, result.get(0).getDifficulty());
        }
    }

    @Nested
    @DisplayName("Get Recommendations Tests")
    class GetRecommendationsTests {

        @Test
        @DisplayName("Should get active recommendations for user")
        void shouldGetActiveRecommendationsForUser() {
            // Given
            when(recommendationRepository.findByUserIdAndCompletedFalseOrderByPriorityAsc(1L))
                    .thenReturn(List.of(testRecommendation));

            // When
            List<Recommendation> result = recommendationService.getActiveRecommendations(1L);

            // Then
            assertEquals(1, result.size());
            assertFalse(result.get(0).getCompleted());
        }

        @Test
        @DisplayName("Should get all user recommendations")
        void shouldGetAllUserRecommendations() {
            // Given
            when(recommendationRepository.findByUserIdOrderByPriorityAsc(1L))
                    .thenReturn(List.of(testRecommendation));

            // When
            List<Recommendation> result = recommendationService.getUserRecommendations(1L);

            // Then
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Complete Recommendation Tests")
    class CompleteRecommendationTests {

        @Test
        @DisplayName("Should complete recommendation with feedback")
        void shouldCompleteRecommendationWithFeedback() {
            // Given
            when(recommendationRepository.findById(1L)).thenReturn(Optional.of(testRecommendation));
            when(recommendationRepository.save(any(Recommendation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Recommendation result = recommendationService.completeRecommendation(1L, "Very helpful!");

            // Then
            assertTrue(result.getCompleted());
            assertNotNull(result.getCompletedAt());
            assertEquals(Recommendation.RecommendationStatus.COMPLETED, result.getStatus());
            assertEquals("Very helpful!", result.getUserFeedback());
        }

        @Test
        @DisplayName("Should throw exception when recommendation not found for completion")
        void shouldThrowExceptionWhenRecommendationNotFoundForCompletion() {
            // Given
            when(recommendationRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> recommendationService.completeRecommendation(999L, "feedback"));
        }
    }

    @Nested
    @DisplayName("Dismiss Recommendation Tests")
    class DismissRecommendationTests {

        @Test
        @DisplayName("Should dismiss recommendation with reason")
        void shouldDismissRecommendationWithReason() {
            // Given
            when(recommendationRepository.findById(1L)).thenReturn(Optional.of(testRecommendation));
            when(recommendationRepository.save(any(Recommendation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Recommendation result = recommendationService.dismissRecommendation(1L, "Not applicable to my situation");

            // Then
            assertEquals(Recommendation.RecommendationStatus.DISMISSED, result.getStatus());
            assertEquals("Not applicable to my situation", result.getUserFeedback());
        }

        @Test
        @DisplayName("Should throw exception when recommendation not found for dismissal")
        void shouldThrowExceptionWhenRecommendationNotFoundForDismissal() {
            // Given
            when(recommendationRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> recommendationService.dismissRecommendation(999L, "reason"));
        }
    }

    @Nested
    @DisplayName("DTO Conversion Tests")
    class DtoConversionTests {

        @Test
        @DisplayName("Should convert recommendation to DTO")
        void shouldConvertRecommendationToDto() {
            // When
            RecommendationDto dto = recommendationService.toDto(testRecommendation);

            // Then
            assertNotNull(dto);
            assertEquals(1L, dto.id());
            assertEquals(1L, dto.userId());
            assertEquals(1L, dto.profileId());
            assertEquals("Mental Health", dto.category());
            assertEquals(1, dto.priority());
            assertEquals("Practice daily mindfulness", dto.title());
            assertEquals("Engage in 10 minutes of mindfulness meditation each morning", dto.description());
            assertEquals("Daily", dto.timeFrame());
            assertEquals("EASY", dto.difficulty());
            assertEquals("ACTIVE", dto.status());
            assertFalse(dto.completed());
        }

        @Test
        @DisplayName("Should convert recommendation to DTO with null profile")
        void shouldConvertRecommendationToDtoWithNullProfile() {
            // Given
            testRecommendation.setProfile(null);

            // When
            RecommendationDto dto = recommendationService.toDto(testRecommendation);

            // Then
            assertNotNull(dto);
            assertNull(dto.profileId());
        }

        @Test
        @DisplayName("Should convert recommendation to DTO with null difficulty")
        void shouldConvertRecommendationToDtoWithNullDifficulty() {
            // Given
            testRecommendation.setDifficulty(null);

            // When
            RecommendationDto dto = recommendationService.toDto(testRecommendation);

            // Then
            assertNotNull(dto);
            assertNull(dto.difficulty());
        }

        @Test
        @DisplayName("Should convert recommendation to DTO with null status")
        void shouldConvertRecommendationToDtoWithNullStatus() {
            // Given
            testRecommendation.setStatus(null);

            // When
            RecommendationDto dto = recommendationService.toDto(testRecommendation);

            // Then
            assertNotNull(dto);
            assertNull(dto.status());
        }
    }

    @Nested
    @DisplayName("Title Truncation Tests")
    class TitleTruncationTests {

        @Test
        @DisplayName("Should truncate long titles")
        void shouldTruncateLongTitles() {
            // Given
            String longTitle = "1. " + "A".repeat(250);
            String aiResponse = longTitle + "\nDescription text here.";

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn(aiResponse);
            when(recommendationRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Recommendation> result = recommendationService.generateRecommendations(1L);

            // Then
            assertTrue(result.get(0).getTitle().length() <= 200);
            assertTrue(result.get(0).getTitle().endsWith("..."));
        }
    }
}
