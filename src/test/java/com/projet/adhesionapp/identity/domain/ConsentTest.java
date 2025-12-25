package com.projet.adhesionapp.identity.domain;

import org.junit.jupiter.api.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour l'entité Consent
 */
@DisplayName("Consent - Tests unitaires")
class ConsentTest {

    private User testUser;
    private Consent consent;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();
        
        consent = Consent.builder()
                .id(1L)
                .user(testUser)
                .scope("DATA")
                .build();
    }

    @Nested
    @DisplayName("Lifecycle Callbacks")
    class LifecycleTests {

        @Test
        @DisplayName("PrePersist initialise grantedAt si null")
        void shouldInitializeGrantedAtIfNull() {
            Consent newConsent = Consent.builder()
                    .user(testUser)
                    .scope("NOTIFICATION")
                    .build();
            
            newConsent.prePersist();

            assertThat(newConsent.getGrantedAt()).isNotNull();
        }

        @Test
        @DisplayName("PrePersist initialise createdAt")
        void shouldInitializeCreatedAt() {
            Consent newConsent = Consent.builder()
                    .user(testUser)
                    .scope("ANALYTICS")
                    .build();
            
            newConsent.prePersist();

            assertThat(newConsent.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("PrePersist initialise updatedAt")
        void shouldInitializeUpdatedAt() {
            Consent newConsent = Consent.builder()
                    .user(testUser)
                    .scope("DATA")
                    .build();
            
            newConsent.prePersist();

            assertThat(newConsent.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("PrePersist ne modifie pas grantedAt existant")
        void shouldNotModifyExistingGrantedAt() {
            Instant existingGrantedAt = Instant.now().minusSeconds(3600);
            Consent newConsent = Consent.builder()
                    .user(testUser)
                    .scope("DATA")
                    .grantedAt(existingGrantedAt)
                    .build();
            
            newConsent.prePersist();

            assertThat(newConsent.getGrantedAt()).isEqualTo(existingGrantedAt);
        }

        @Test
        @DisplayName("PreUpdate met à jour updatedAt")
        void shouldUpdateUpdatedAt() {
            consent.prePersist();
            Instant initialUpdatedAt = consent.getUpdatedAt();
            
            // Attendre un peu pour avoir une différence de temps
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            consent.preUpdate();

            assertThat(consent.getUpdatedAt()).isAfterOrEqualTo(initialUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Builder and Fields")
    class BuilderTests {

        @Test
        @DisplayName("Construit avec tous les champs")
        void shouldBuildWithAllFields() {
            Instant now = Instant.now();
            Consent fullConsent = Consent.builder()
                    .id(1L)
                    .user(testUser)
                    .scope("DATA")
                    .grantedAt(now)
                    .revokedAt(null)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            assertThat(fullConsent.getId()).isEqualTo(1L);
            assertThat(fullConsent.getUser()).isEqualTo(testUser);
            assertThat(fullConsent.getScope()).isEqualTo("DATA");
            assertThat(fullConsent.getGrantedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Scope peut être DATA")
        void scopeCanBeData() {
            consent.setScope("DATA");
            assertThat(consent.getScope()).isEqualTo("DATA");
        }

        @Test
        @DisplayName("Scope peut être NOTIFICATION")
        void scopeCanBeNotification() {
            consent.setScope("NOTIFICATION");
            assertThat(consent.getScope()).isEqualTo("NOTIFICATION");
        }

        @Test
        @DisplayName("Scope peut être ANALYTICS")
        void scopeCanBeAnalytics() {
            consent.setScope("ANALYTICS");
            assertThat(consent.getScope()).isEqualTo("ANALYTICS");
        }
    }

    @Nested
    @DisplayName("Revocation")
    class RevocationTests {

        @Test
        @DisplayName("Peut révoquer un consentement")
        void canRevokeConsent() {
            Instant revokedAt = Instant.now();
            consent.setRevokedAt(revokedAt);

            assertThat(consent.getRevokedAt()).isEqualTo(revokedAt);
        }

        @Test
        @DisplayName("RevokedAt est null par défaut")
        void revokedAtIsNullByDefault() {
            Consent newConsent = Consent.builder()
                    .user(testUser)
                    .scope("DATA")
                    .build();

            assertThat(newConsent.getRevokedAt()).isNull();
        }
    }
}
