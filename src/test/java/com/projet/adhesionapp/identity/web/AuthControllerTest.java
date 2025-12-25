package com.projet.adhesionapp.identity.web;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur d'authentification.
 * Cas de test selon le plan d'assurance qualité:
 * - CT_AUTH_01: Inscription réussie
 * - CT_AUTH_02: Inscription avec consentement null
 * - CT_AUTH_03: Connexion réussie
 * - CT_AUTH_04: Connexion avec mauvais identifiants
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Controller - Tests unitaires")
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Nested
    @DisplayName("CT_AUTH_01-02: Inscription")
    class RegisterTests {

        @Test
        @DisplayName("CT_AUTH_01: Inscription réussie avec toutes les données")
        void shouldRegisterUserSuccessfully() {
            // Arrange
            AuthController.RegisterRequest request = new AuthController.RegisterRequest();
            request.setEmail("test@emsi.ma");
            request.setPassword("SecurePass123!");
            request.setDisplayName("Mohamed Test");
            request.setBirthDate(LocalDate.of(1995, 5, 15));
            request.setGender("M");
            request.setConsentGiven(true);

            User expectedUser = User.builder()
                    .id(1L)
                    .email("test@emsi.ma")
                    .displayName("Mohamed Test")
                    .build();

            when(userService.register(
                    eq("test@emsi.ma"),
                    eq("SecurePass123!"),
                    eq("Mohamed Test"),
                    eq(LocalDate.of(1995, 5, 15)),
                    eq("M"),
                    eq(true)
            )).thenReturn(expectedUser);

            // Act
            User result = authController.register(request);

            // Assert
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@emsi.ma");
            assertThat(result.getDisplayName()).isEqualTo("Mohamed Test");
            verify(userService).register(anyString(), anyString(), anyString(), any(), anyString(), eq(true));
        }

        @Test
        @DisplayName("CT_AUTH_02: Inscription avec consentement null (défaut false)")
        void shouldRegisterWithDefaultConsent() {
            // Arrange
            AuthController.RegisterRequest request = new AuthController.RegisterRequest();
            request.setEmail("patient@emsi.ma");
            request.setPassword("Password123!");
            request.setDisplayName("Patient Test");
            request.setBirthDate(LocalDate.of(1990, 1, 1));
            request.setGender("F");
            request.setConsentGiven(null);

            User expectedUser = User.builder().id(2L).build();
            when(userService.register(anyString(), anyString(), anyString(), any(), anyString(), eq(false)))
                    .thenReturn(expectedUser);

            // Act
            authController.register(request);

            // Assert
            verify(userService).register(
                    eq("patient@emsi.ma"),
                    anyString(),
                    anyString(),
                    any(),
                    anyString(),
                    eq(false)
            );
        }
    }

    @Nested
    @DisplayName("CT_AUTH_03-04: Connexion")
    class LoginTests {

        @Test
        @DisplayName("CT_AUTH_03: Connexion réussie retourne token et user")
        void shouldLoginSuccessfully() {
            // Arrange
            AuthController.LoginRequest request = new AuthController.LoginRequest();
            request.setEmail("user@emsi.ma");
            request.setPassword("ValidPassword123!");

            User authenticatedUser = User.builder()
                    .id(1L)
                    .email("user@emsi.ma")
                    .displayName("User Test")
                    .build();

            when(userService.authenticate("user@emsi.ma", "ValidPassword123!"))
                    .thenReturn(authenticatedUser);

            // Act
            AuthController.LoginResponse response = authController.login(request);

            // Assert
            assertThat(response.token()).isNotNull();
            assertThat(response.token()).isNotEmpty();
            assertThat(response.user().getId()).isEqualTo(1L);
            assertThat(response.user().getEmail()).isEqualTo("user@emsi.ma");
        }

        @Test
        @DisplayName("CT_AUTH_04: Connexion avec mauvais identifiants propage l'exception")
        void shouldPropagateAuthenticationException() {
            // Arrange
            AuthController.LoginRequest request = new AuthController.LoginRequest();
            request.setEmail("wrong@emsi.ma");
            request.setPassword("WrongPassword");

            when(userService.authenticate("wrong@emsi.ma", "WrongPassword"))
                    .thenThrow(new RuntimeException("Invalid credentials"));

            // Act & Assert
            assertThatThrownBy(() -> authController.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid credentials");
        }
    }
}
