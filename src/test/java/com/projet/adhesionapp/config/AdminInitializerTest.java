package com.projet.adhesionapp.config;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AdminInitializer
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInitializer - Tests unitaires")
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @Nested
    @DisplayName("Création du compte admin")
    class CreateAdminTests {

        @Test
        @DisplayName("Crée un compte admin si n'existe pas")
        void shouldCreateAdminIfNotExists() throws Exception {
            // Given
            when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("admin123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            adminInitializer.run();

            // Then
            verify(userRepository).save(argThat(user -> 
                user.getEmail().equals("admin@gmail.com") &&
                user.getRole().equals("ADMIN") &&
                user.getActive() &&
                user.getOnboardingCompleted()
            ));
        }

        @Test
        @DisplayName("Met à jour le compte admin si existe déjà")
        void shouldUpdateAdminIfExists() throws Exception {
            // Given
            User existingUser = User.builder()
                    .id(1L)
                    .email("admin@gmail.com")
                    .role("USER")
                    .build();
            
            when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.encode("admin123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            adminInitializer.run();

            // Then
            verify(userRepository).save(argThat(user -> 
                user.getRole().equals("ADMIN")
            ));
        }

        @Test
        @DisplayName("Utilise le bon email admin")
        void shouldUseCorrectAdminEmail() throws Exception {
            // Given
            when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            adminInitializer.run();

            // Then
            verify(userRepository).findByEmail("admin@gmail.com");
        }

        @Test
        @DisplayName("Hash le mot de passe admin")
        void shouldHashAdminPassword() throws Exception {
            // Given
            when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("admin123")).thenReturn("hashedPassword123");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            adminInitializer.run();

            // Then
            verify(passwordEncoder).encode("admin123");
            verify(userRepository).save(argThat(user -> 
                user.getPasswordHash().equals("hashedPassword123")
            ));
        }
    }
}
