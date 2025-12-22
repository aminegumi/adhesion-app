package com.projet.adhesionapp.analytics.service;

import com.projet.adhesionapp.ai.service.OpenAIService;
import com.projet.adhesionapp.analytics.model.AdherencePredictionDto;
import com.projet.adhesionapp.analytics.repo.PredictionRepository;
import com.projet.adhesionapp.assessment.repo.TestSessionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.habit.repo.DoseLogRepository;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import com.projet.adhesionapp.profile.repo.PsychologicalProfileRepository;
import com.projet.adhesionapp.treatment.repo.MedicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdherencePredictionService Unit Tests")
class AdherencePredictionServiceTest {

    @Mock
    private DoseLogRepository doseLogRepository;

    @Mock
    private TestSessionRepository testSessionRepository;

    @Mock
    private PsychologicalProfileRepository profileRepository;

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private OpenAIService aiService;

    @InjectMocks
    private AdherencePredictionService adherencePredictionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .displayName("Test User")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("Predict Adherence Tests")
    class PredictAdherenceTests {

        @Test
        @DisplayName("Should predict adherence for existing user")
        void shouldPredictAdherenceForExistingUser() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            AdherencePredictionDto result = adherencePredictionService.predictAdherence(1L);

            // Then
            assertNotNull(result);
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> adherencePredictionService.predictAdherence(999L));
        }
    }
}
