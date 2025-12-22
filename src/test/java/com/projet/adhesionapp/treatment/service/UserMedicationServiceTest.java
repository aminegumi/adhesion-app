package com.projet.adhesionapp.treatment.service;

import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.habit.repo.DoseLogRepository;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import com.projet.adhesionapp.treatment.domain.UserMedication;
import com.projet.adhesionapp.treatment.model.CreateMedicationRequest;
import com.projet.adhesionapp.treatment.model.UserMedicationDto;
import com.projet.adhesionapp.treatment.repo.MedicationRepository;
import com.projet.adhesionapp.treatment.repo.UserMedicationRepository;
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
@DisplayName("UserMedicationService Unit Tests")
class UserMedicationServiceTest {

    @Mock
    private UserMedicationRepository medicationRepository;

    @Mock
    private DoseLogRepository doseLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MedicationRepository planMedicationRepository;

    @InjectMocks
    private UserMedicationService userMedicationService;

    private User testUser;
    private UserMedication testMedication;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .displayName("Test User")
                .active(true)
                .build();

        testMedication = UserMedication.builder()
                .id(1L)
                .user(testUser)
                .name("Aspirin")
                .dosage("100mg")
                .form(UserMedication.MedicationForm.TABLET)
                .frequencyPerDay(2)
                .startDate(LocalDate.now())
                .isChronic(false)
                .active(true)
                .remindersEnabled(true)
                .build();
    }

    @Nested
    @DisplayName("Get Medications Tests")
    class GetMedicationsTests {

        @Test
        @DisplayName("Should get all user medications")
        void shouldGetAllUserMedications() {
            // Given
            when(medicationRepository.findByUserId(1L)).thenReturn(List.of(testMedication));

            // When
            List<UserMedicationDto> result = userMedicationService.getUserMedications(1L);

            // Then
            assertEquals(1, result.size());
            assertEquals("Aspirin", result.get(0).name());
            verify(medicationRepository).findByUserId(1L);
        }

        @Test
        @DisplayName("Should get active medications")
        void shouldGetActiveMedications() {
            // Given
            when(medicationRepository.findCurrentlyActiveMedications(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of(testMedication));

            // When
            List<UserMedicationDto> result = userMedicationService.getActiveMedications(1L);

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return empty list when no medications")
        void shouldReturnEmptyListWhenNoMedications() {
            // Given
            when(medicationRepository.findByUserId(1L)).thenReturn(List.of());

            // When
            List<UserMedicationDto> result = userMedicationService.getUserMedications(1L);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should get medication by ID")
        void shouldGetMedicationById() {
            // Given
            when(medicationRepository.findById(1L)).thenReturn(Optional.of(testMedication));

            // When
            UserMedicationDto result = userMedicationService.getMedication(1L);

            // Then
            assertNotNull(result);
            assertEquals("Aspirin", result.name());
        }

        @Test
        @DisplayName("Should throw exception when medication not found")
        void shouldThrowExceptionWhenMedicationNotFound() {
            // Given
            when(medicationRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> userMedicationService.getMedication(999L));
        }
    }

    @Nested
    @DisplayName("Create Medication Tests")
    class CreateMedicationTests {

        @Test
        @DisplayName("Should create medication successfully")
        void shouldCreateMedicationSuccessfully() {
            // Given
            CreateMedicationRequest request = new CreateMedicationRequest(
                    1L,
                    "Ibuprofen",
                    "200mg",
                    UserMedication.MedicationForm.TABLET,
                    3,
                    List.of("08:00", "14:00", "20:00"),
                    "Dr. Smith",
                    "Take with food",
                    LocalDate.now(),
                    null,
                    false,
                    30,
                    5,
                    true,
                    15,
                    "Pain relief",
                    "Back pain",
                    "#FF5733"
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(medicationRepository.save(any(UserMedication.class))).thenAnswer(invocation -> {
                UserMedication med = invocation.getArgument(0);
                med.setId(2L);
                return med;
            });

            // When
            UserMedicationDto result = userMedicationService.createMedication(1L, request);

            // Then
            assertNotNull(result);
            assertEquals("Ibuprofen", result.name());
            assertEquals("200mg", result.dosage());
            verify(medicationRepository).save(any(UserMedication.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            CreateMedicationRequest request = new CreateMedicationRequest(
                    999L, "TestMed", "10mg", null, 1, null, null, null,
                    null, null, false, null, null, false, null, null, null, null
            );

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> userMedicationService.createMedication(999L, request));
        }
    }

    @Nested
    @DisplayName("Update Medication Tests")
    class UpdateMedicationTests {

        @Test
        @DisplayName("Should update medication successfully")
        void shouldUpdateMedicationSuccessfully() {
            // Given
            CreateMedicationRequest updateRequest = new CreateMedicationRequest(
                    1L,
                    "Updated Aspirin",
                    "200mg",
                    UserMedication.MedicationForm.CAPSULE,
                    3,
                    List.of("09:00", "15:00", "21:00"),
                    "Dr. New",
                    "Updated instructions",
                    LocalDate.now(),
                    LocalDate.now().plusMonths(1),
                    true,
                    50,
                    10,
                    true,
                    20,
                    "Updated notes",
                    "Updated reason",
                    "#00FF00"
            );

            when(medicationRepository.findById(1L)).thenReturn(Optional.of(testMedication));
            when(medicationRepository.save(any(UserMedication.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserMedicationDto result = userMedicationService.updateMedication(1L, updateRequest);

            // Then
            assertNotNull(result);
            verify(medicationRepository).save(any(UserMedication.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent medication")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            // Given
            CreateMedicationRequest request = new CreateMedicationRequest(
                    999L, "TestMed", "10mg", null, 1, null, null, null,
                    null, null, false, null, null, false, null, null, null, null
            );

            when(medicationRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> userMedicationService.updateMedication(999L, request));
        }
    }

    @Nested
    @DisplayName("Delete Medication Tests")
    class DeleteMedicationTests {

        @Test
        @DisplayName("Should delete medication successfully")
        void shouldDeleteMedicationSuccessfully() {
            // Given - Use existsById as in actual service
            when(medicationRepository.existsById(1L)).thenReturn(true);
            doNothing().when(doseLogRepository).deleteByUserMedicationId(1L);
            doNothing().when(medicationRepository).deleteById(1L);

            // When
            userMedicationService.deleteMedication(1L);

            // Then
            verify(medicationRepository).existsById(1L);
            verify(doseLogRepository).deleteByUserMedicationId(1L);
            verify(medicationRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent medication")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            // Given
            when(medicationRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> userMedicationService.deleteMedication(999L));
        }
    }
}
