package com.projet.adhesionapp.ai.web;

import com.projet.adhesionapp.ai.model.*;
import com.projet.adhesionapp.ai.service.EmotionDetectionService;
import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.service.PsychologicalProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur IA.
 * CT_AI_01 - CT_AI_06: Génération de contenu IA
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI Controller - Tests unitaires")
class AIControllerTest {

    @Mock private OpenAIService openAIService;
    @Mock private UserService userService;
    @Mock private PsychologicalProfileService profileService;
    @Mock private EmotionDetectionService emotionDetectionService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private AIController controller;

    private User testUser;
    private PsychologicalProfile testProfile;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("patient@test.com")
                .displayName("Patient Test")
                .build();

        testProfile = PsychologicalProfile.builder()
                .id(1L)
                .user(testUser)
                .profileType("Anxious")
                .summary("Profil anxieux avec bonne motivation")
                .anxietyScore(70.0)
                .motivationScore(80.0)
                .build();
    }

    @Nested
    @DisplayName("CT_AI_01: Messages motivationnels")
    class MotivationTests {

        @Test
        @DisplayName("CT_AI_01a: Génère un message motivationnel simple")
        void shouldGenerateMotivationalMessage() {
            // MotivationRequest(Long userId, Double adherenceScore, String userMessage, List<ChatMessage> conversationHistory)
            MotivationRequest request = new MotivationRequest(1L, 0.75, null, null);

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateMotivationalMessage(anyString(), anyString(), anyDouble()))
                    .thenReturn("Félicitations Patient Test! Votre adhésion est excellente!");

            ResponseEntity<MotivationResponse> response = controller.getMotivationalMessage(request);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().message()).contains("Félicitations");
            assertThat(response.getBody().userId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("CT_AI_01b: Génère une réponse conversationnelle")
        void shouldGenerateConversationalResponse() {
            MotivationRequest.ChatMessage userMsg = new MotivationRequest.ChatMessage("user", "Je me sens stressé", System.currentTimeMillis());
            MotivationRequest.ChatMessage assistantMsg = new MotivationRequest.ChatMessage("assistant", "Je comprends", System.currentTimeMillis());
            List<MotivationRequest.ChatMessage> history = List.of(userMsg, assistantMsg);

            MotivationRequest request = new MotivationRequest(1L, 0.5, "Comment gérer mon stress?", history);

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateConversationalResponse(anyString(), anyString(), anyDouble(), anyString(), anyList()))
                    .thenReturn("Voici quelques techniques de gestion du stress...");

            ResponseEntity<MotivationResponse> response = controller.getMotivationalMessage(request);

            assertThat(response.getBody().message()).contains("techniques");
            verify(openAIService).generateConversationalResponse(anyString(), anyString(), anyDouble(), eq("Comment gérer mon stress?"), anyList());
        }
    }

    @Nested
    @DisplayName("CT_AI_02: Recommandations IA")
    class RecommendationsTests {

        @Test
        @DisplayName("CT_AI_02a: Génère des recommandations personnalisées")
        void shouldGenerateRecommendations() {
            // RecommendationRequest(Long userId, Double nonAdherenceRisk)
            RecommendationRequest request = new RecommendationRequest(1L, 0.3);

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(openAIService.generateRecommendations(anyString(), anyString(), anyString(), anyDouble()))
                    .thenReturn("1. Pratiquer la méditation\n2. Établir une routine");

            ResponseEntity<RecommendationResponse> response = controller.getRecommendations(request);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().recommendations()).contains("méditation");
        }
    }

    @Nested
    @DisplayName("CT_AI_03: Détection d'émotions faciales")
    class FacialEmotionTests {

        @Test
        @DisplayName("CT_AI_03a: Détecte l'émotion faciale avec succès")
        void shouldDetectFacialEmotion() throws Exception {
            MockMultipartFile image = new MockMultipartFile(
                    "image", "face.jpg", "image/jpeg", "fake-image-data".getBytes()
            );

            when(userService.findById(1L)).thenReturn(testUser);
            when(emotionDetectionService.detectEmotion(any(byte[].class)))
                    .thenReturn(Map.of("emotion", "happy", "confidence", 0.95));

            ResponseEntity<FacialEmotionDetectionResponse> response = 
                    controller.analyzeFacialEmotion(1L, image);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            // FacialEmotionDetectionResponse(String dominantEmotion, double confidence,
            //   Map<String, Double> emotionScores, String insight, Long userId)
            assertThat(response.getBody().dominantEmotion()).isEqualTo("happy");
            assertThat(response.getBody().confidence()).isEqualTo(0.95);
        }

        @Test
        @DisplayName("CT_AI_03b: Gère les erreurs de détection gracieusement")
        void shouldHandleDetectionError() throws Exception {
            MockMultipartFile image = new MockMultipartFile(
                    "image", "corrupted.jpg", "image/jpeg", "corrupted".getBytes()
            );

            when(userService.findById(1L)).thenReturn(testUser);
            when(emotionDetectionService.detectEmotion(any(byte[].class)))
                    .thenThrow(new RuntimeException("Detection failed"));

            ResponseEntity<FacialEmotionDetectionResponse> response = 
                    controller.analyzeFacialEmotion(1L, image);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().dominantEmotion()).isEqualTo("neutral");
            assertThat(response.getBody().confidence()).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("CT_AI_04: Sans profil utilisateur")
    class NoProfileTests {

        @Test
        @DisplayName("CT_AI_04a: Génère un message même sans profil")
        void shouldGenerateMessageWithoutProfile() {
            MotivationRequest request = new MotivationRequest(1L, 0.5, null, null);

            when(userService.findById(1L)).thenReturn(testUser);
            when(profileService.getLatestProfile(1L)).thenReturn(null);
            when(openAIService.generateMotivationalMessage(anyString(), contains("No profile"), anyDouble()))
                    .thenReturn("Continuez vos efforts!");

            ResponseEntity<MotivationResponse> response = controller.getMotivationalMessage(request);

            assertThat(response.getBody().message()).isNotNull();
            verify(openAIService).generateMotivationalMessage(eq("Patient Test"), contains("No profile"), eq(0.5));
        }
    }
}
