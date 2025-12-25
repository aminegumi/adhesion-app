package com.projet.adhesionapp.common.scheduler;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.recommendation.service.RecommendationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de ScheduledNotificationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledNotificationService - Tests unitaires")
class ScheduledNotificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private RecommendationService recommendationService;
    @InjectMocks private ScheduledNotificationService service;

    private User activeUser;
    private User userNeedingRetake;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .email("active@example.com")
                .active(true)
                .onboardingCompleted(true)
                .build();

        userNeedingRetake = User.builder()
                .id(2L)
                .email("retake@example.com")
                .active(true)
                .onboardingCompleted(true)
                .lastTestCompletedAt(Instant.now().minusSeconds(20L * 24 * 60 * 60)) // 20 days ago
                .build();
    }

    @Nested
    @DisplayName("generatePeriodicMotivations")
    class GeneratePeriodicMotivationsTests {
        @Test
        @DisplayName("Génère des recommandations pour utilisateurs actifs avec onboarding")
        void shouldGenerateRecommendationsForActiveUsers() {
            when(userRepository.findByActiveTrue()).thenReturn(List.of(activeUser));

            service.generatePeriodicMotivations();

            verify(recommendationService).generateRecommendations(1L);
        }

        @Test
        @DisplayName("Ignore utilisateurs sans onboarding complété")
        void shouldSkipUsersWithoutOnboarding() {
            User notOnboarded = User.builder()
                    .id(3L)
                    .active(true)
                    .onboardingCompleted(false)
                    .build();
            when(userRepository.findByActiveTrue()).thenReturn(List.of(notOnboarded));

            service.generatePeriodicMotivations();

            verify(recommendationService, never()).generateRecommendations(anyLong());
        }

        @Test
        @DisplayName("Continue malgré erreur pour un utilisateur")
        void shouldContinueOnError() {
            User user2 = User.builder()
                    .id(4L)
                    .active(true)
                    .onboardingCompleted(true)
                    .build();
            when(userRepository.findByActiveTrue()).thenReturn(List.of(activeUser, user2));
            doThrow(new RuntimeException("Error")).when(recommendationService).generateRecommendations(1L);

            service.generatePeriodicMotivations();

            verify(recommendationService).generateRecommendations(1L);
            verify(recommendationService).generateRecommendations(4L);
        }
    }

    @Nested
    @DisplayName("checkRetakeReminders")
    class CheckRetakeRemindersTests {
        @Test
        @DisplayName("Marque utilisateurs pour retake après 15 jours")
        void shouldMarkUsersForRetake() {
            when(userRepository.findByOnboardingCompletedTrueAndLastTestCompletedAtBefore(any()))
                    .thenReturn(List.of(userNeedingRetake));

            service.checkRetakeReminders();

            verify(userRepository).save(argThat(user -> 
                !user.getOnboardingCompleted() && user.getCompletedTestsCount() == 0
            ));
        }

        @Test
        @DisplayName("Aucune action si aucun utilisateur à retake")
        void shouldDoNothingIfNoUsersNeedRetake() {
            when(userRepository.findByOnboardingCompletedTrueAndLastTestCompletedAtBefore(any()))
                    .thenReturn(List.of());

            service.checkRetakeReminders();

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("healthCheck")
    class HealthCheckTests {
        @Test
        @DisplayName("Health check s'exécute sans erreur")
        void shouldExecuteHealthCheck() {
            // Simply verify it doesn't throw
            service.healthCheck();
        }
    }
}
