package com.projet.adhesionapp.config;

import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour SecurityConfig
 */
@DisplayName("SecurityConfig - Tests unitaires")
class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
    }

    @Nested
    @DisplayName("PasswordEncoder Bean")
    class PasswordEncoderTests {

        @Test
        @DisplayName("Retourne un BCryptPasswordEncoder")
        void shouldReturnBCryptPasswordEncoder() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();

            assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        }

        @Test
        @DisplayName("PasswordEncoder peut encoder un mot de passe")
        void shouldEncodePassword() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();
            String rawPassword = "testPassword123";

            String encoded = encoder.encode(rawPassword);

            assertThat(encoded).isNotEqualTo(rawPassword);
            assertThat(encoded).isNotEmpty();
        }

        @Test
        @DisplayName("PasswordEncoder peut vérifier un mot de passe")
        void shouldMatchPassword() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();
            String rawPassword = "testPassword123";
            String encoded = encoder.encode(rawPassword);

            boolean matches = encoder.matches(rawPassword, encoded);

            assertThat(matches).isTrue();
        }

        @Test
        @DisplayName("PasswordEncoder refuse un mauvais mot de passe")
        void shouldNotMatchWrongPassword() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();
            String rawPassword = "testPassword123";
            String encoded = encoder.encode(rawPassword);

            boolean matches = encoder.matches("wrongPassword", encoded);

            assertThat(matches).isFalse();
        }

        @Test
        @DisplayName("Chaque encodage produit un résultat différent (salt)")
        void shouldProduceDifferentEncodings() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();
            String rawPassword = "testPassword123";

            String encoded1 = encoder.encode(rawPassword);
            String encoded2 = encoder.encode(rawPassword);

            assertThat(encoded1).isNotEqualTo(encoded2);
        }
    }
}
