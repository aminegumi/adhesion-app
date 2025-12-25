package com.projet.adhesionapp.identity.web;

import com.projet.adhesionapp.assessment.model.TestResultDto;
import com.projet.adhesionapp.assessment.service.TestSessionService;
import com.projet.adhesionapp.common.exception.BadRequestException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.treatment.domain.TreatmentPlan;
import com.projet.adhesionapp.treatment.model.TreatmentPlanDto;
import com.projet.adhesionapp.treatment.service.TreatmentPlanService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur d'administration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController - Tests unitaires")
class AdminControllerTest {

    @Mock private UserService userService;
    @Mock private TestSessionService testSessionService;
    @Mock private TreatmentPlanService treatmentPlanService;
    @InjectMocks private AdminController controller;

    private User testUser;
    private User consentedUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("patient@example.com")
                .displayName("Patient Test")
                .consentGiven(false)
                .build();

        consentedUser = User.builder()
                .id(2L)
                .email("consented@example.com")
                .displayName("Consented User")
                .consentGiven(true)
                .build();
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsersTests {
        @Test
        @DisplayName("Retourne tous les utilisateurs")
        void shouldReturnAllUsers() {
            when(userService.findAll()).thenReturn(List.of(testUser, consentedUser));
            ResponseEntity<List<User>> response = controller.getAllUsers();
            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("Retourne liste vide si aucun utilisateur")
        void shouldReturnEmptyListWhenNoUsers() {
            when(userService.findAll()).thenReturn(List.of());
            ResponseEntity<List<User>> response = controller.getAllUsers();
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getConsentedUsers")
    class GetConsentedUsersTests {
        @Test
        @DisplayName("Retourne utilisateurs avec consentement")
        void shouldReturnConsentedUsers() {
            when(userService.findConsentedUsers()).thenReturn(List.of(consentedUser));
            ResponseEntity<List<User>> response = controller.getConsentedUsers();
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getConsentGiven()).isTrue();
        }
    }

    @Nested
    @DisplayName("getUserTestResults")
    class GetUserTestResultsTests {
        @Test
        @DisplayName("Retourne résultats si consentement donné")
        void shouldReturnResultsIfConsentGiven() {
            TestResultDto result = new TestResultDto(
                    1L, "PHQ9", "PHQ-9", 15, "MODERATE",
                    "Score modéré", "Interprétation",
                    List.of("Recommendation"), Instant.now()
            );
            when(userService.findById(2L)).thenReturn(consentedUser);
            when(testSessionService.getPatientTestHistory(2L)).thenReturn(List.of(result));

            ResponseEntity<List<TestResultDto>> response = controller.getUserTestResults(2L);

            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).testCode()).isEqualTo("PHQ9");
        }

        @Test
        @DisplayName("Lève BadRequestException si pas de consentement")
        void shouldThrowIfNoConsent() {
            when(userService.findById(1L)).thenReturn(testUser);

            assertThatThrownBy(() -> controller.getUserTestResults(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("consent");
        }
    }

    @Nested
    @DisplayName("getUserTreatmentPlans")
    class GetUserTreatmentPlansTests {
        @Test
        @DisplayName("Retourne plans si consentement donné")
        void shouldReturnPlansIfConsentGiven() {
            TreatmentPlan plan = TreatmentPlan.builder().id(1L).build();
            TreatmentPlanDto planDto = new TreatmentPlanDto(
                    1L, 2L, "Patient", null, "Plan Test", "Description", 
                    null, 4, null, null, null, List.of(), null, 0, "ACTIVE", null
            );
            when(userService.findById(2L)).thenReturn(consentedUser);
            when(treatmentPlanService.getUserPlans(2L)).thenReturn(List.of(plan));
            when(treatmentPlanService.toDto(plan)).thenReturn(planDto);

            ResponseEntity<List<TreatmentPlanDto>> response = controller.getUserTreatmentPlans(2L);

            assertThat(response.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("Lève BadRequestException si pas de consentement")
        void shouldThrowIfNoConsent() {
            when(userService.findById(1L)).thenReturn(testUser);

            assertThatThrownBy(() -> controller.getUserTreatmentPlans(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("consent");
        }
    }

    @Nested
    @DisplayName("getAdmins")
    class GetAdminsTests {
        @Test
        @DisplayName("Retourne les administrateurs")
        void shouldReturnAdmins() {
            User admin = User.builder().id(10L).email("admin@example.com").role("ADMIN").build();
            when(userService.findByRole("ADMIN")).thenReturn(List.of(admin));

            ResponseEntity<List<User>> response = controller.getAdmins();

            assertThat(response.getBody()).hasSize(1);
        }
    }
}
