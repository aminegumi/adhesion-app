package com.projet.adhesionapp.identity.web;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.model.UserStatusDto;
import com.projet.adhesionapp.identity.service.UserService;
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
 * Tests unitaires du contrôleur utilisateur.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController - Tests unitaires")
class UserControllerTest {

    @Mock private UserService userService;
    @InjectMocks private UserController controller;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .displayName("Test User")
                .onboardingCompleted(true)
                .requiredTestsCount(2)
                .completedTestsCount(1)
                .consentGiven(false)
                .build();
    }

    @Nested
    @DisplayName("getAll")
    class GetAllTests {
        @Test
        @DisplayName("Retourne tous les utilisateurs")
        void shouldReturnAllUsers() {
            when(userService.findAll()).thenReturn(List.of(testUser));
            List<User> result = controller.getAll();
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {
        @Test
        @DisplayName("Retourne utilisateur par ID")
        void shouldReturnUserById() {
            when(userService.findById(1L)).thenReturn(testUser);
            User result = controller.getById(1L);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("getUserStatus")
    class GetUserStatusTests {
        @Test
        @DisplayName("Retourne le statut utilisateur")
        void shouldReturnUserStatus() {
            when(userService.findById(1L)).thenReturn(testUser);

            ResponseEntity<UserStatusDto> response = controller.getUserStatus(1L);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().userId()).isEqualTo(1L);
            assertThat(response.getBody().testsRemaining()).isEqualTo(1);
        }

        @Test
        @DisplayName("Calcule correctement tests restants")
        void shouldCalculateTestsRemaining() {
            testUser.setRequiredTestsCount(3);
            testUser.setCompletedTestsCount(2);
            when(userService.findById(1L)).thenReturn(testUser);

            ResponseEntity<UserStatusDto> response = controller.getUserStatus(1L);

            assertThat(response.getBody().testsRemaining()).isEqualTo(1);
        }

        @Test
        @DisplayName("Gère valeurs null")
        void shouldHandleNullValues() {
            testUser.setRequiredTestsCount(null);
            testUser.setCompletedTestsCount(null);
            when(userService.findById(1L)).thenReturn(testUser);

            ResponseEntity<UserStatusDto> response = controller.getUserStatus(1L);

            assertThat(response.getBody().requiredTestsCount()).isEqualTo(2);
            assertThat(response.getBody().completedTestsCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("deactivate/activate")
    class DeactivateActivateTests {
        @Test
        @DisplayName("Désactive un utilisateur")
        void shouldDeactivateUser() {
            controller.deactivate(1L);
            verify(userService).deactivate(1L);
        }

        @Test
        @DisplayName("Active un utilisateur")
        void shouldActivateUser() {
            controller.activate(1L);
            verify(userService).activate(1L);
        }
    }

    @Nested
    @DisplayName("updateConsent")
    class UpdateConsentTests {
        @Test
        @DisplayName("Met à jour le consentement")
        void shouldUpdateConsent() {
            User updatedUser = User.builder()
                    .id(1L)
                    .consentGiven(true)
                    .build();
            when(userService.updateConsent(1L, true)).thenReturn(updatedUser);

            ResponseEntity<User> response = controller.updateConsent(1L, true);

            assertThat(response.getBody().getConsentGiven()).isTrue();
        }
    }
}
