package com.projet.adhesionapp.recommendation.web;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.recommendation.domain.Recommendation;
import com.projet.adhesionapp.recommendation.domain.Recommendation.RecommendationStatus;
import com.projet.adhesionapp.recommendation.domain.Recommendation.Difficulty;
import com.projet.adhesionapp.recommendation.model.*;
import com.projet.adhesionapp.recommendation.service.RecommendationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur de recommandations.
 * CT_REC_01 - CT_REC_04: Génération et gestion des recommandations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Recommendation Controller - Tests unitaires")
class RecommendationControllerTest {

    @Mock private RecommendationService recommendationService;
    @InjectMocks private RecommendationController controller;

    private Recommendation testRecommendation;
    private RecommendationDto testRecommendationDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("patient@test.com").build();

        testRecommendation = Recommendation.builder()
                .id(1L)
                .user(testUser)
                .category("Lifestyle")
                .priority(1)
                .title("Méditation quotidienne")
                .description("Pratiquer la méditation 10 minutes par jour")
                .actionableSteps("1. Trouver un endroit calme 2. S'asseoir confortablement")
                .expectedBenefits("Réduction du stress et de l'anxiété")
                .timeFrame("2 semaines")
                .difficulty(Difficulty.EASY)
                .status(RecommendationStatus.ACTIVE)
                .completed(false)
                .build();

        testRecommendationDto = new RecommendationDto(
                1L, 1L, 1L, "Lifestyle", 1,
                "Méditation quotidienne", "Pratiquer la méditation 10 minutes par jour",
                "1. Trouver un endroit calme 2. S'asseoir confortablement",
                "Réduction du stress et de l'anxiété", "2 semaines", "EASY",
                "ACTIVE", false, Instant.now(), null
        );
    }

    @Nested
    @DisplayName("CT_REC_01: Génération de recommandations")
    class GenerateRecommendationsTests {

        @Test
        @DisplayName("CT_REC_01a: Génère des recommandations pour un utilisateur")
        void shouldGenerateRecommendations() {
            when(recommendationService.generateRecommendations(1L)).thenReturn(List.of(testRecommendation));
            when(recommendationService.toDto(testRecommendation)).thenReturn(testRecommendationDto);

            ResponseEntity<List<RecommendationDto>> response = controller.generateRecommendations(1L);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).title()).isEqualTo("Méditation quotidienne");
        }
    }

    @Nested
    @DisplayName("CT_REC_02: Récupération des recommandations")
    class GetRecommendationsTests {

        @Test
        @DisplayName("CT_REC_02a: Récupère les recommandations d'un utilisateur")
        void shouldGetUserRecommendations() {
            when(recommendationService.getUserRecommendations(1L)).thenReturn(List.of(testRecommendation));
            when(recommendationService.toDto(testRecommendation)).thenReturn(testRecommendationDto);

            ResponseEntity<List<RecommendationDto>> response = controller.getUserRecommendations(1L);

            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).category()).isEqualTo("Lifestyle");
        }

        @Test
        @DisplayName("CT_REC_02b: Récupère les recommandations actives")
        void shouldGetActiveRecommendations() {
            when(recommendationService.getActiveRecommendations(1L)).thenReturn(List.of(testRecommendation));
            when(recommendationService.toDto(testRecommendation)).thenReturn(testRecommendationDto);

            ResponseEntity<List<RecommendationDto>> response = controller.getActiveRecommendations(1L);

            assertThat(response.getBody()).isNotEmpty();
            assertThat(response.getBody().get(0).status()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("CT_REC_03: Complétion des recommandations")
    class CompleteRecommendationTests {

        @Test
        @DisplayName("CT_REC_03a: Marque une recommandation comme complétée")
        void shouldCompleteRecommendation() {
            Recommendation completed = Recommendation.builder()
                    .id(1L)
                    .user(testUser)
                    .title("Méditation quotidienne")
                    .status(RecommendationStatus.COMPLETED)
                    .completed(true)
                    .build();
            RecommendationDto completedDto = new RecommendationDto(
                    1L, 1L, 1L, "Lifestyle", 1,
                    "Méditation quotidienne", "Pratiquer la méditation",
                    "Steps", "Benefits", "2 semaines", "EASY",
                    "COMPLETED", true, Instant.now(), Instant.now()
            );

            when(recommendationService.completeRecommendation(1L, "Très utile")).thenReturn(completed);
            when(recommendationService.toDto(completed)).thenReturn(completedDto);

            ResponseEntity<RecommendationDto> response = controller.completeRecommendation(1L, "Très utile");

            assertThat(response.getBody().completed()).isTrue();
            assertThat(response.getBody().status()).isEqualTo("COMPLETED");
        }
    }

    @Nested
    @DisplayName("CT_REC_04: Rejet des recommandations")
    class DismissRecommendationTests {

        @Test
        @DisplayName("CT_REC_04a: Rejette une recommandation")
        void shouldDismissRecommendation() {
            Recommendation dismissed = Recommendation.builder()
                    .id(1L)
                    .user(testUser)
                    .status(RecommendationStatus.DISMISSED)
                    .build();
            RecommendationDto dismissedDto = new RecommendationDto(
                    1L, 1L, 1L, "Lifestyle", 1,
                    "Méditation", "Description", "Steps", "Benefits",
                    "2 semaines", "EASY", "DISMISSED", false,
                    Instant.now(), null
            );

            when(recommendationService.dismissRecommendation(1L, "Pas applicable")).thenReturn(dismissed);
            when(recommendationService.toDto(dismissed)).thenReturn(dismissedDto);

            ResponseEntity<RecommendationDto> response = controller.dismissRecommendation(1L, "Pas applicable");

            assertThat(response.getBody().status()).isEqualTo("DISMISSED");
        }
    }
}
