package com.projet.adhesionapp.config;

import org.junit.jupiter.api.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour CorsConfig
 */
@DisplayName("CorsConfig - Tests unitaires")
class CorsConfigTest {

    private CorsConfig corsConfig;

    @BeforeEach
    void setUp() {
        corsConfig = new CorsConfig();
    }

    @Nested
    @DisplayName("CorsConfigurationSource Bean")
    class CorsConfigurationSourceTests {

        @Test
        @DisplayName("Retourne un CorsConfigurationSource valide")
        void shouldReturnValidCorsConfigurationSource() {
            CorsConfigurationSource source = corsConfig.corsConfigurationSource();

            assertThat(source).isNotNull();
            assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
        }

        @Test
        @DisplayName("Configuration CORS pour le chemin racine")
        void shouldHaveConfigurationForRootPath() {
            CorsConfigurationSource source = corsConfig.corsConfigurationSource();
            
            // Vérifier que la source retourne une configuration pour /**
            CorsConfiguration config = source.getCorsConfiguration(
                    new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));
            
            assertThat(config).isNotNull();
        }

        @Test
        @DisplayName("Autorise les méthodes HTTP standard")
        void shouldAllowStandardHttpMethods() {
            CorsConfigurationSource source = corsConfig.corsConfigurationSource();
            CorsConfiguration config = source.getCorsConfiguration(
                    new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

            assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        }

        @Test
        @DisplayName("Autorise les credentials")
        void shouldAllowCredentials() {
            CorsConfigurationSource source = corsConfig.corsConfigurationSource();
            CorsConfiguration config = source.getCorsConfiguration(
                    new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

            assertThat(config.getAllowCredentials()).isTrue();
        }

        @Test
        @DisplayName("Autorise tous les headers")
        void shouldAllowAllHeaders() {
            CorsConfigurationSource source = corsConfig.corsConfigurationSource();
            CorsConfiguration config = source.getCorsConfiguration(
                    new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

            assertThat(config.getAllowedHeaders()).contains("*");
        }

        @Test
        @DisplayName("Expose tous les headers")
        void shouldExposeAllHeaders() {
            CorsConfigurationSource source = corsConfig.corsConfigurationSource();
            CorsConfiguration config = source.getCorsConfiguration(
                    new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

            assertThat(config.getExposedHeaders()).contains("*");
        }

        @Test
        @DisplayName("Autorise les origines localhost")
        void shouldAllowLocalhostOrigins() {
            CorsConfigurationSource source = corsConfig.corsConfigurationSource();
            CorsConfiguration config = source.getCorsConfiguration(
                    new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

            assertThat(config.getAllowedOriginPatterns()).contains(
                    "http://localhost:*",
                    "http://127.0.0.1:*"
            );
        }

        @Test
        @DisplayName("Autorise l'émulateur Android")
        void shouldAllowAndroidEmulator() {
            CorsConfigurationSource source = corsConfig.corsConfigurationSource();
            CorsConfiguration config = source.getCorsConfiguration(
                    new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

            assertThat(config.getAllowedOriginPatterns()).contains("http://10.0.2.2:*");
        }
    }
}
