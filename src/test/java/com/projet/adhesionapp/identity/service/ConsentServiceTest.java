package com.projet.adhesionapp.identity.service;

import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.Consent;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.ConsentRepository;
import com.projet.adhesionapp.identity.repo.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de ConsentService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentService - Tests unitaires")
class ConsentServiceTest {

    @Mock private ConsentRepository consentRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private ConsentService service;

    private User testUser;
    private Consent testConsent;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        testConsent = Consent.builder()
                .id(1L)
                .user(testUser)
                .scope("DATA_SHARING")
                .grantedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("getConsentsForUser")
    class GetConsentsForUserTests {
        @Test
        @DisplayName("Retourne les consentements d'un utilisateur")
        void shouldReturnConsentsForUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(consentRepository.findByUser(testUser)).thenReturn(List.of(testConsent));

            List<Consent> result = service.getConsentsForUser(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScope()).isEqualTo("DATA_SHARING");
        }

        @Test
        @DisplayName("Lève exception si utilisateur non trouvé")
        void shouldThrowIfUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getConsentsForUser(999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Retourne liste vide si aucun consentement")
        void shouldReturnEmptyListIfNoConsents() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(consentRepository.findByUser(testUser)).thenReturn(List.of());

            List<Consent> result = service.getConsentsForUser(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("grantConsent")
    class GrantConsentTests {
        @Test
        @DisplayName("Crée un nouveau consentement")
        void shouldCreateNewConsent() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(consentRepository.findByUserAndScope(testUser, "ANALYTICS")).thenReturn(Optional.empty());
            when(consentRepository.save(any(Consent.class))).thenAnswer(inv -> inv.getArgument(0));

            Consent result = service.grantConsent(1L, "ANALYTICS");

            assertThat(result.getScope()).isEqualTo("ANALYTICS");
            assertThat(result.getGrantedAt()).isNotNull();
            assertThat(result.getRevokedAt()).isNull();
        }

        @Test
        @DisplayName("Met à jour un consentement existant")
        void shouldUpdateExistingConsent() {
            Consent existingConsent = Consent.builder()
                    .id(2L)
                    .user(testUser)
                    .scope("DATA_SHARING")
                    .revokedAt(Instant.now())
                    .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(consentRepository.findByUserAndScope(testUser, "DATA_SHARING")).thenReturn(Optional.of(existingConsent));
            when(consentRepository.save(any(Consent.class))).thenAnswer(inv -> inv.getArgument(0));

            Consent result = service.grantConsent(1L, "DATA_SHARING");

            assertThat(result.getGrantedAt()).isNotNull();
            assertThat(result.getRevokedAt()).isNull(); // Should be cleared
        }

        @Test
        @DisplayName("Lève exception si utilisateur non trouvé")
        void shouldThrowIfUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.grantConsent(999L, "DATA_SHARING"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("revokeConsent")
    class RevokeConsentTests {
        @Test
        @DisplayName("Révoque un consentement existant")
        void shouldRevokeExistingConsent() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(consentRepository.findByUserAndScope(testUser, "DATA_SHARING")).thenReturn(Optional.of(testConsent));
            when(consentRepository.save(any(Consent.class))).thenAnswer(inv -> inv.getArgument(0));

            Consent result = service.revokeConsent(1L, "DATA_SHARING");

            assertThat(result.getRevokedAt()).isNotNull();
        }

        @Test
        @DisplayName("Lève exception si consentement non trouvé")
        void shouldThrowIfConsentNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(consentRepository.findByUserAndScope(testUser, "UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revokeConsent(1L, "UNKNOWN"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Consentement");
        }

        @Test
        @DisplayName("Lève exception si utilisateur non trouvé")
        void shouldThrowIfUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revokeConsent(999L, "DATA_SHARING"))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
