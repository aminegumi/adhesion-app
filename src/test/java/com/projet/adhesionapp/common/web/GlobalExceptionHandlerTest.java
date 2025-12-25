package com.projet.adhesionapp.common.web;

import com.projet.adhesionapp.common.exception.ApiException;
import com.projet.adhesionapp.common.exception.BadRequestException;
import com.projet.adhesionapp.common.exception.NotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Tests unitaires")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleNotFound")
    class HandleNotFoundTests {
        @Test
        @DisplayName("Retourne 404 pour NotFoundException")
        void shouldReturn404ForNotFoundException() {
            NotFoundException ex = new NotFoundException("Utilisateur non trouvé");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleNotFound(ex);

            assertThat(response.getStatusCodeValue()).isEqualTo(404);
            assertThat(response.getBody().message()).isEqualTo("Utilisateur non trouvé");
            assertThat(response.getBody().error()).isEqualTo("Not Found");
        }
    }

    @Nested
    @DisplayName("handleBadRequest")
    class HandleBadRequestTests {
        @Test
        @DisplayName("Retourne 400 pour BadRequestException")
        void shouldReturn400ForBadRequestException() {
            BadRequestException ex = new BadRequestException("Paramètre invalide");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBadRequest(ex);

            assertThat(response.getStatusCodeValue()).isEqualTo(400);
            assertThat(response.getBody().message()).isEqualTo("Paramètre invalide");
        }
    }

    @Nested
    @DisplayName("handleApiException")
    class HandleApiExceptionTests {
        @Test
        @DisplayName("Retourne 422 pour ApiException")
        void shouldReturn422ForApiException() {
            ApiException ex = new ApiException("Erreur de traitement");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleApiException(ex);

            assertThat(response.getStatusCodeValue()).isEqualTo(422);
            assertThat(response.getBody().message()).isEqualTo("Erreur de traitement");
        }
    }

    @Nested
    @DisplayName("handleValidation")
    class HandleValidationTests {
        @Test
        @DisplayName("Retourne 400 avec détails pour erreurs de validation")
        void shouldReturn400WithDetailsForValidationErrors() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("object", "email", "Email invalide");
            
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidation(ex);

            assertThat(response.getStatusCodeValue()).isEqualTo(400);
            assertThat(response.getBody().message()).isEqualTo("Erreur de validation");
            assertThat(response.getBody().details()).containsEntry("email", "Email invalide");
        }
    }

    @Nested
    @DisplayName("handleOther")
    class HandleOtherTests {
        @Test
        @DisplayName("Retourne 500 pour exception générique")
        void shouldReturn500ForGenericException() {
            Exception ex = new RuntimeException("Erreur inattendue");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleOther(ex);

            assertThat(response.getStatusCodeValue()).isEqualTo(500);
            assertThat(response.getBody().message()).contains("Erreur interne du serveur");
        }
    }

    @Nested
    @DisplayName("ErrorResponse")
    class ErrorResponseTests {
        @Test
        @DisplayName("ErrorResponse contient tous les champs")
        void shouldContainAllFields() {
            var response = handler.handleNotFound(new NotFoundException("Test"));

            assertThat(response.getBody().timestamp()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().error()).isNotNull();
            assertThat(response.getBody().message()).isNotNull();
            assertThat(response.getBody().details()).isNotNull();
        }
    }
}
