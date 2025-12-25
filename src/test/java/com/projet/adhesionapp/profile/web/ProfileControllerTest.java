package com.projet.adhesionapp.profile.web;

import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.model.*;
import com.projet.adhesionapp.profile.service.PsychologicalProfileService;
import com.projet.adhesionapp.identity.domain.User;
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
 * Tests unitaires du contrôleur de profils psychologiques.
 * CT_PROF_01 - CT_PROF_05: Gestion des profils
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Profile Controller - Tests unitaires")
class ProfileControllerTest {

    @Mock private PsychologicalProfileService profileService;
    @InjectMocks private ProfileController controller;

    private PsychologicalProfile testProfile;
    private ProfileDto testProfileDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@example.com").build();

        testProfile = PsychologicalProfile.builder()
                .id(1L)
                .user(testUser)
                .profileType("Anxious")
                .anxietyScore(75.0)
                .depressionScore(40.0)
                .motivationScore(60.0)
                .selfEfficacyScore(55.0)
                .socialSupportScore(50.0)
                .healthLocusScore(65.0)
                .build();

        // ProfileDto(Long id, Long userId, String userName, String profileType,
        //   Double anxietyScore, Double depressionScore, Double motivationScore,
        //   Double selfEfficacyScore, Double socialSupportScore, Double healthLocusScore,
        //   Double adherenceRiskScore, String summary, String detailedInterpretation, 
        //   String status, Instant createdAt)
        testProfileDto = new ProfileDto(
                1L, 1L, "Patient Test", "Anxious",
                75.0, 40.0, 60.0, 55.0, 50.0, 65.0, 30.0,
                "Profil anxieux modéré", "Interprétation détaillée",
                "ACTIVE", Instant.now()
        );
    }

    @Nested
    @DisplayName("CT_PROF_01: Création de profils")
    class CreateProfileTests {

        @Test
        @DisplayName("CT_PROF_01a: Crée un profil psychologique")
        void shouldCreateProfile() {
            // ProfileCreateRequest(Long userId, List<Long> sessionIds, 
            //   Double anxietyScore, Double depressionScore, Double motivationScore,
            //   Double selfEfficacyScore, Double socialSupportScore, Double healthLocusScore)
            ProfileCreateRequest request = new ProfileCreateRequest(
                    1L, List.of(1L, 2L), 75.0, 40.0, 60.0, 55.0, 50.0, 65.0
            );

            when(profileService.createProfile(request)).thenReturn(testProfile);
            when(profileService.toDto(testProfile)).thenReturn(testProfileDto);

            ResponseEntity<ProfileDto> response = controller.createProfile(request);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().profileType()).isEqualTo("Anxious");
            assertThat(response.getBody().anxietyScore()).isEqualTo(75.0);
        }
    }

    @Nested
    @DisplayName("CT_PROF_02: Récupération de profils")
    class GetProfileTests {

        @Test
        @DisplayName("CT_PROF_02a: Récupère un profil par ID")
        void shouldGetProfileById() {
            when(profileService.getById(1L)).thenReturn(testProfile);
            when(profileService.toDto(testProfile)).thenReturn(testProfileDto);

            ResponseEntity<ProfileDto> response = controller.getProfile(1L);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("CT_PROF_03: Récupère les profils d'un utilisateur")
        void shouldGetProfilesForUser() {
            when(profileService.getUserProfiles(1L)).thenReturn(List.of(testProfile));
            when(profileService.toDto(testProfile)).thenReturn(testProfileDto);

            ResponseEntity<List<ProfileDto>> response = controller.getUserProfiles(1L);

            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).userId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("CT_PROF_04: Dernier profil")
    class LatestProfileTests {

        @Test
        @DisplayName("CT_PROF_04a: Récupère le dernier profil actif")
        void shouldGetLatestProfile() {
            when(profileService.getLatestProfile(1L)).thenReturn(testProfile);
            when(profileService.toDto(testProfile)).thenReturn(testProfileDto);

            ResponseEntity<ProfileDto> response = controller.getLatestProfile(1L);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody().profileType()).isEqualTo("Anxious");
        }

        @Test
        @DisplayName("CT_PROF_04b: Retourne 404 si aucun profil")
        void shouldReturn404WhenNoProfile() {
            when(profileService.getLatestProfile(999L)).thenReturn(null);

            ResponseEntity<ProfileDto> response = controller.getLatestProfile(999L);

            assertThat(response.getStatusCodeValue()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("CT_PROF_05: Liste complète")
    class AllProfilesTests {

        @Test
        @DisplayName("CT_PROF_05a: Récupère tous les profils")
        void shouldGetAllProfiles() {
            when(profileService.getAllProfiles()).thenReturn(List.of(testProfile));
            when(profileService.toDto(testProfile)).thenReturn(testProfileDto);

            ResponseEntity<List<ProfileDto>> response = controller.getAllProfiles();

            assertThat(response.getBody()).hasSize(1);
        }
    }
}
