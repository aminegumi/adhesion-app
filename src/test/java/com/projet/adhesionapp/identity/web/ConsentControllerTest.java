package com.projet.adhesionapp.identity.web;

import com.projet.adhesionapp.identity.domain.Consent;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.ConsentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur de consentement.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentController - Tests unitaires")
class ConsentControllerTest {

    @Mock private ConsentService consentService;
    @InjectMocks private ConsentController controller;

    private User testUser;
    private Consent testConsent;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).build();
        testConsent = Consent.builder()
                .id(1L)
                .user(testUser)
                .scope("DATA_SHARING")
                .grantedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("getConsents")
    class GetConsentsTests {
        @Test
        @DisplayName("Retourne les consentements d'un utilisateur")
        void shouldReturnConsentsForUser() {
            when(consentService.getConsentsForUser(1L)).thenReturn(List.of(testConsent));

            List<Consent> result = controller.getConsents(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScope()).isEqualTo("DATA_SHARING");
        }

        @Test
        @DisplayName("Retourne liste vide si aucun consentement")
        void shouldReturnEmptyListIfNoConsents() {
            when(consentService.getConsentsForUser(999L)).thenReturn(List.of());

            List<Consent> result = controller.getConsents(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("grant")
    class GrantTests {
        @Test
        @DisplayName("Accorde un consentement")
        void shouldGrantConsent() {
            ConsentController.GrantConsentRequest request = new ConsentController.GrantConsentRequest();
            request.setScope("DATA_SHARING");
            when(consentService.grantConsent(1L, "DATA_SHARING")).thenReturn(testConsent);

            Consent result = controller.grant(1L, request);

            assertThat(result.getGrantedAt()).isNotNull();
            verify(consentService).grantConsent(1L, "DATA_SHARING");
        }
    }

    @Nested
    @DisplayName("revoke")
    class RevokeTests {
        @Test
        @DisplayName("Révoque un consentement")
        void shouldRevokeConsent() {
            User user = User.builder().id(1L).build();
            Consent revokedConsent = Consent.builder()
                    .id(1L)
                    .user(user)
                    .scope("DATA_SHARING")
                    .grantedAt(Instant.now().minusSeconds(3600))
                    .revokedAt(Instant.now())
                    .build();

            ConsentController.GrantConsentRequest request = new ConsentController.GrantConsentRequest();
            request.setScope("DATA_SHARING");
            when(consentService.revokeConsent(1L, "DATA_SHARING")).thenReturn(revokedConsent);

            Consent result = controller.revoke(1L, request);

            assertThat(result.getRevokedAt()).isNotNull();
            verify(consentService).revokeConsent(1L, "DATA_SHARING");
        }
    }
}
