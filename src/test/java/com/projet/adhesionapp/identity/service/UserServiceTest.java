package com.projet.adhesionapp.identity.service;

import com.projet.adhesionapp.common.exception.BadRequestException;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .displayName("Test User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .gender("Male")
                .active(true)
                .onboardingCompleted(false)
                .requiredTestsCount(2)
                .completedTestsCount(0)
                .role("USER")
                .consentGiven(false)
                .build();

        adminUser = User.builder()
                .id(2L)
                .email("admin@example.com")
                .passwordHash("hashedPassword")
                .displayName("Admin User")
                .birthDate(LocalDate.of(1985, 3, 20))
                .gender("Female")
                .active(true)
                .onboardingCompleted(true)
                .requiredTestsCount(0)
                .completedTestsCount(0)
                .role("ADMIN")
                .consentGiven(true)
                .build();
    }

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUser() {
            // Given
            String email = "newuser@example.com";
            String password = "password123";
            String displayName = "New User";
            LocalDate birthDate = LocalDate.of(1995, 8, 10);
            String gender = "Male";

            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(3L);
                return user;
            });

            // When
            User result = userService.register(email, password, displayName, birthDate, gender);

            // Then
            assertNotNull(result);
            assertEquals(email, result.getEmail());
            assertEquals(displayName, result.getDisplayName());
            assertEquals("USER", result.getRole());
            assertTrue(result.getActive());
            assertFalse(result.getOnboardingCompleted());
            
            verify(userRepository).existsByEmail(email);
            verify(passwordEncoder).encode(password);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should register new user with consent")
        void shouldRegisterNewUserWithConsent() {
            // Given
            String email = "consent@example.com";
            String password = "password123";
            boolean consentGiven = true;

            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.register(email, password, "Consent User", 
                    LocalDate.of(1990, 1, 1), "Other", consentGiven);

            // Then
            assertNotNull(result);
            assertTrue(result.getConsentGiven());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            String existingEmail = "test@example.com";
            when(userRepository.existsByEmail(existingEmail)).thenReturn(true);

            // When & Then
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> userService.register(existingEmail, "pass", "Name", 
                            LocalDate.now(), "Male"));
            
            assertEquals("Un utilisateur existe déjà avec cet email.", exception.getMessage());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("User Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should authenticate user with valid credentials")
        void shouldAuthenticateWithValidCredentials() {
            // Given
            String email = "test@example.com";
            String rawPassword = "correctPassword";
            
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(rawPassword, testUser.getPasswordHash())).thenReturn(true);

            // When
            User result = userService.authenticate(email, rawPassword);

            // Then
            assertNotNull(result);
            assertEquals(testUser.getId(), result.getId());
            assertEquals(email, result.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            String email = "nonexistent@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> userService.authenticate(email, "anyPassword"));
        }

        @Test
        @DisplayName("Should throw exception when password is incorrect")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Given
            String email = "test@example.com";
            String wrongPassword = "wrongPassword";
            
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(wrongPassword, testUser.getPasswordHash())).thenReturn(false);

            // When & Then
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> userService.authenticate(email, wrongPassword));
            
            assertEquals("Email ou mot de passe incorrect.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {

        @Test
        @DisplayName("Should find all users")
        void shouldFindAllUsers() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(testUser, adminUser));

            // When
            List<User> result = userService.findAll();

            // Then
            assertEquals(2, result.size());
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should find user by ID")
        void shouldFindUserById() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            User result = userService.findById(1L);

            // Then
            assertNotNull(result);
            assertEquals(testUser.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw exception when user not found by ID")
        void shouldThrowExceptionWhenUserNotFoundById() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () -> userService.findById(999L));
        }

        @Test
        @DisplayName("Should deactivate user")
        void shouldDeactivateUser() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            userService.deactivate(1L);

            // Then
            assertFalse(testUser.getActive());
        }

        @Test
        @DisplayName("Should activate user")
        void shouldActivateUser() {
            // Given
            testUser.setActive(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            userService.activate(1L);

            // Then
            assertTrue(testUser.getActive());
        }
    }

    @Nested
    @DisplayName("Profile Update Tests")
    class ProfileUpdateTests {

        @Test
        @DisplayName("Should update user profile")
        void shouldUpdateUserProfile() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.updateProfile(1L, "Updated Name", 
                    "newemail@example.com", "Female", LocalDate.of(1991, 6, 20), true);

            // Then
            assertEquals("Updated Name", result.getDisplayName());
            assertEquals("newemail@example.com", result.getEmail());
            assertEquals("Female", result.getGender());
            assertTrue(result.getConsentGiven());
        }

        @Test
        @DisplayName("Should throw exception when updating to existing email")
        void shouldThrowExceptionWhenUpdatingToExistingEmail() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

            // When & Then
            assertThrows(BadRequestException.class,
                    () -> userService.updateProfile(1L, null, "admin@example.com", null, null, null));
        }

        @Test
        @DisplayName("Should update consent status")
        void shouldUpdateConsentStatus() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.updateConsent(1L, true);

            // Then
            assertTrue(result.getConsentGiven());
        }
    }

    @Nested
    @DisplayName("Admin User Tests")
    class AdminUserTests {

        @Test
        @DisplayName("Should create admin user")
        void shouldCreateAdminUser() {
            // Given
            String email = "newadmin@example.com";
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(10L);
                return user;
            });

            // When
            User result = userService.createAdmin(email, "adminPass", "Admin Name", 
                    "1985-01-15", "Male");

            // Then
            assertNotNull(result);
            assertEquals("ADMIN", result.getRole());
            assertTrue(result.getOnboardingCompleted());
            assertTrue(result.getConsentGiven());
        }

        @Test
        @DisplayName("Should find users by role")
        void shouldFindUsersByRole() {
            // Given
            when(userRepository.findByRole("ADMIN")).thenReturn(List.of(adminUser));

            // When
            List<User> result = userService.findByRole("ADMIN");

            // Then
            assertEquals(1, result.size());
            assertEquals("ADMIN", result.get(0).getRole());
        }
    }

    @Nested
    @DisplayName("Onboarding Tests")
    class OnboardingTests {

        @Test
        @DisplayName("Should increment completed tests count")
        void shouldIncrementCompletedTestsCount() {
            // Given
            testUser.setCompletedTestsCount(1);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.incrementTestsCompleted(1L);

            // Then
            assertEquals(2, testUser.getCompletedTestsCount());
            assertTrue(testUser.getOnboardingCompleted());
            assertNotNull(testUser.getLastTestCompletedAt());
        }

        @Test
        @DisplayName("Should not complete onboarding when less than required tests")
        void shouldNotCompleteOnboardingWhenLessThanRequiredTests() {
            // Given
            testUser.setCompletedTestsCount(0);
            testUser.setRequiredTestsCount(2);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.incrementTestsCompleted(1L);

            // Then
            assertEquals(1, testUser.getCompletedTestsCount());
            assertFalse(testUser.getOnboardingCompleted());
        }

        @Test
        @DisplayName("Should mark user as needing retake")
        void shouldMarkUserAsNeedingRetake() {
            // Given
            testUser.setOnboardingCompleted(true);
            testUser.setCompletedTestsCount(2);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.markNeedsRetake(1L);

            // Then
            assertFalse(testUser.getOnboardingCompleted());
            assertEquals(0, testUser.getCompletedTestsCount());
        }

        @Test
        @DisplayName("Should complete onboarding")
        void shouldCompleteOnboarding() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.completeOnboarding(1L);

            // Then
            assertTrue(testUser.getOnboardingCompleted());
            assertNotNull(testUser.getLastTestCompletedAt());
        }

        @Test
        @DisplayName("Should find users needing retake")
        void shouldFindUsersNeedingRetake() {
            // Given
            testUser.setOnboardingCompleted(true);
            testUser.setLastTestCompletedAt(Instant.now().minusSeconds(20L * 24 * 60 * 60)); // 20 days ago
            when(userRepository.findByOnboardingCompletedTrueAndLastTestCompletedAtBefore(any(Instant.class)))
                    .thenReturn(List.of(testUser));

            // When
            List<User> result = userService.getUsersNeedingRetake();

            // Then
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Consent Tests")
    class ConsentTests {

        @Test
        @DisplayName("Should find consented users")
        void shouldFindConsentedUsers() {
            // Given
            adminUser.setConsentGiven(true);
            when(userRepository.findByConsentGivenTrueAndActiveTrue()).thenReturn(List.of(adminUser));

            // When
            List<User> result = userService.findConsentedUsers();

            // Then
            assertEquals(1, result.size());
            assertTrue(result.get(0).getConsentGiven());
        }
    }
}
