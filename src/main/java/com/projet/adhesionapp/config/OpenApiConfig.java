package com.projet.adhesionapp.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger (springdoc-openapi).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI adhesionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Adhesion App API")
                        .version("v1")
                        .description("API pour l’optimisation de l’adhésion thérapeutique"))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentation technique")
                        .url("https://example.com/docs"));
    }
}
