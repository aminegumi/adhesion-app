package com.projet.adhesionapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour OpenApiConfig
 */
@DisplayName("OpenApiConfig - Tests unitaires")
class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
    }

    @Nested
    @DisplayName("OpenAPI Bean")
    class OpenAPIBeanTests {

        @Test
        @DisplayName("Retourne un objet OpenAPI valide")
        void shouldReturnValidOpenAPI() {
            OpenAPI openAPI = openApiConfig.adhesionOpenAPI();

            assertThat(openAPI).isNotNull();
        }

        @Test
        @DisplayName("Info contient le titre correct")
        void shouldHaveCorrectTitle() {
            OpenAPI openAPI = openApiConfig.adhesionOpenAPI();
            Info info = openAPI.getInfo();

            assertThat(info).isNotNull();
            assertThat(info.getTitle()).isEqualTo("Adhesion App API");
        }

        @Test
        @DisplayName("Info contient la version")
        void shouldHaveVersion() {
            OpenAPI openAPI = openApiConfig.adhesionOpenAPI();
            Info info = openAPI.getInfo();

            assertThat(info.getVersion()).isEqualTo("v1");
        }

        @Test
        @DisplayName("Info contient la description")
        void shouldHaveDescription() {
            OpenAPI openAPI = openApiConfig.adhesionOpenAPI();
            Info info = openAPI.getInfo();

            assertThat(info.getDescription()).contains("adhésion thérapeutique");
        }

        @Test
        @DisplayName("Documentation externe est définie")
        void shouldHaveExternalDocs() {
            OpenAPI openAPI = openApiConfig.adhesionOpenAPI();

            assertThat(openAPI.getExternalDocs()).isNotNull();
            assertThat(openAPI.getExternalDocs().getDescription()).isEqualTo("Documentation technique");
            assertThat(openAPI.getExternalDocs().getUrl()).isEqualTo("https://example.com/docs");
        }
    }
}
