package com.projet.adhesionapp.common.notification;

import com.projet.adhesionapp.habit.domain.DoseLog;
import com.projet.adhesionapp.habit.domain.DoseLog.DoseStatus;
import com.projet.adhesionapp.habit.repo.DoseLogRepository;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.treatment.domain.Medication;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Medication Reminder Service Tests")
class MedicationReminderServiceTest {

    @Mock
    private DoseLogRepository doseLogRepository;

    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private MedicationReminderService reminderService;

    private User testUser;
    private Medication testMedication;
    private DoseLog testDoseLog;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordHash("hashed")
                .displayName("Jean Dupont")
                .birthDate(java.time.LocalDate.of(1990, 1, 1))
                .gender("M")
                .active(true)
                .build();

        testMedication = Medication.builder()
                .id(1L)
                .name("Paracetamol")
                .dosage("500mg")
                .reminderMinutesBefore(15)
                .build();

        testDoseLog = DoseLog.builder()
                .id(1L)
                .user(testUser)
                .medication(testMedication)
                .scheduledDate(LocalDate.now())
                .scheduledTime(LocalTime.now().plusMinutes(10))
                .status(DoseStatus.PENDING)
                .reminderSent(false)
                .build();
    }

    @Nested
    @DisplayName("Check And Send Reminders Tests")
    class CheckAndSendRemindersTests {

        @Test
        @DisplayName("Should send reminder when dose is within reminder window")
        void shouldSendReminderWhenDoseWithinWindow() {
            // Given
            LocalTime scheduledTime = LocalTime.now().plusMinutes(10);
            testDoseLog.setScheduledTime(scheduledTime);

            when(doseLogRepository.findDosesNeedingReminders(any(LocalDate.class)))
                    .thenReturn(List.of(testDoseLog));
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);
            when(doseLogRepository.save(any(DoseLog.class))).thenReturn(testDoseLog);

            // When
            reminderService.checkAndSendReminders();

            // Then
            verify(notificationSender).send(any(NotificationPayload.class));
            verify(doseLogRepository).save(testDoseLog);
        }

        @Test
        @DisplayName("Should use default reminder minutes when not set")
        void shouldUseDefaultReminderMinutesWhenNotSet() {
            // Given
            testMedication.setReminderMinutesBefore(null);
            LocalTime scheduledTime = LocalTime.now().plusMinutes(10);
            testDoseLog.setScheduledTime(scheduledTime);

            when(doseLogRepository.findDosesNeedingReminders(any(LocalDate.class)))
                    .thenReturn(List.of(testDoseLog));
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);
            when(doseLogRepository.save(any(DoseLog.class))).thenReturn(testDoseLog);

            // When
            reminderService.checkAndSendReminders();

            // Then
            verify(notificationSender).send(any(NotificationPayload.class));
        }

        @Test
        @DisplayName("Should not send reminder when outside window")
        void shouldNotSendReminderWhenOutsideWindow() {
            // Given - dose is 2 hours in the future, beyond 15 min reminder
            LocalTime scheduledTime = LocalTime.now().plusHours(2);
            testDoseLog.setScheduledTime(scheduledTime);

            when(doseLogRepository.findDosesNeedingReminders(any(LocalDate.class)))
                    .thenReturn(List.of(testDoseLog));

            // When
            reminderService.checkAndSendReminders();

            // Then
            verify(notificationSender, never()).send(any(NotificationPayload.class));
        }

        @Test
        @DisplayName("Should handle empty dose list")
        void shouldHandleEmptyDoseList() {
            // Given
            when(doseLogRepository.findDosesNeedingReminders(any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // When
            reminderService.checkAndSendReminders();

            // Then
            verify(notificationSender, never()).send(any(NotificationPayload.class));
        }

        @Test
        @DisplayName("Should handle multiple doses")
        void shouldHandleMultipleDoses() {
            // Given
            DoseLog dose2 = DoseLog.builder()
                    .id(2L)
                    .user(testUser)
                    .medication(testMedication)
                    .scheduledDate(LocalDate.now())
                    .scheduledTime(LocalTime.now().plusMinutes(5))
                    .status(DoseStatus.PENDING)
                    .reminderSent(false)
                    .build();

            testDoseLog.setScheduledTime(LocalTime.now().plusMinutes(10));

            when(doseLogRepository.findDosesNeedingReminders(any(LocalDate.class)))
                    .thenReturn(List.of(testDoseLog, dose2));
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);
            when(doseLogRepository.save(any(DoseLog.class))).thenAnswer(i -> i.getArgument(0));

            // When
            reminderService.checkAndSendReminders();

            // Then
            verify(notificationSender, times(2)).send(any(NotificationPayload.class));
        }
    }

    @Nested
    @DisplayName("Send Dose Reminder Tests")
    class SendDoseReminderTests {

        @Test
        @DisplayName("Should send dose reminder successfully")
        void shouldSendDoseReminderSuccessfully() {
            // Given
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);
            when(doseLogRepository.save(any(DoseLog.class))).thenReturn(testDoseLog);

            // When
            reminderService.sendDoseReminder(testDoseLog);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());

            NotificationPayload payload = payloadCaptor.getValue();
            assertThat(payload.getUserId()).isEqualTo(testUser.getId());
            assertThat(payload.getTitle()).contains("Medication Reminder");
            assertThat(payload.getBody()).contains("Paracetamol");
            assertThat(payload.getType()).isEqualTo(NotificationType.MEDICATION_REMINDER);

            verify(doseLogRepository).save(testDoseLog);
            assertThat(testDoseLog.getReminderSent()).isTrue();
            assertThat(testDoseLog.getReminderSentAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not mark reminder sent when notification fails")
        void shouldNotMarkReminderSentWhenNotificationFails() {
            // Given
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(false);

            // When
            reminderService.sendDoseReminder(testDoseLog);

            // Then
            verify(doseLogRepository, never()).save(any(DoseLog.class));
            assertThat(testDoseLog.getReminderSent()).isFalse();
        }

        @Test
        @DisplayName("Should handle medication without dosage")
        void shouldHandleMedicationWithoutDosage() {
            // Given
            testMedication.setDosage(null);
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);
            when(doseLogRepository.save(any(DoseLog.class))).thenReturn(testDoseLog);

            // When
            reminderService.sendDoseReminder(testDoseLog);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());

            NotificationPayload payload = payloadCaptor.getValue();
            assertThat(payload.getBody()).contains("as prescribed");
        }

        @Test
        @DisplayName("Should include correct data entries in payload")
        void shouldIncludeCorrectDataEntriesInPayload() {
            // Given
            testDoseLog.setScheduledTime(LocalTime.of(10, 30));
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);
            when(doseLogRepository.save(any(DoseLog.class))).thenReturn(testDoseLog);

            // When
            reminderService.sendDoseReminder(testDoseLog);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());

            NotificationPayload payload = payloadCaptor.getValue();
            assertThat(payload.getData()).containsKey("doseId");
            assertThat(payload.getData()).containsKey("medicationId");
            assertThat(payload.getData()).containsKey("medicationName");
            assertThat(payload.getData()).containsKey("scheduledTime");
        }
    }

    @Nested
    @DisplayName("Send Missed Dose Alert Tests")
    class SendMissedDoseAlertTests {

        @Test
        @DisplayName("Should send missed dose alert")
        void shouldSendMissedDoseAlert() {
            // Given
            testDoseLog.setScheduledTime(LocalTime.of(8, 0));
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);

            // When
            reminderService.sendMissedDoseAlert(testDoseLog);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());

            NotificationPayload payload = payloadCaptor.getValue();
            assertThat(payload.getUserId()).isEqualTo(testUser.getId());
            assertThat(payload.getTitle()).contains("Missed Medication");
            assertThat(payload.getBody()).contains("Paracetamol");
            assertThat(payload.getBody()).contains("08:00");
            assertThat(payload.getType()).isEqualTo(NotificationType.MISSED_DOSE_ALERT);
            assertThat(payload.getPriority()).isEqualTo(NotificationPriority.HIGH);
        }

        @Test
        @DisplayName("Should include warning about double dose")
        void shouldIncludeWarningAboutDoubleDose() {
            // When
            reminderService.sendMissedDoseAlert(testDoseLog);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());

            NotificationPayload payload = payloadCaptor.getValue();
            assertThat(payload.getBody()).contains("double dose");
        }
    }

    @Nested
    @DisplayName("Send Adherence Summary Tests")
    class SendAdherenceSummaryTests {

        @Test
        @DisplayName("Should send excellent adherence summary")
        void shouldSendExcellentAdherenceSummary() {
            // Given
            double adherenceRate = 95.0;
            int streak = 14;
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);

            // When
            reminderService.sendAdherenceSummary(testUser, adherenceRate, streak);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());

            NotificationPayload payload = payloadCaptor.getValue();
            assertThat(payload.getTitle()).contains("🌟");
            assertThat(payload.getBody()).contains("Excellent");
            assertThat(payload.getBody()).contains("95%");
            assertThat(payload.getBody()).contains("14-day streak");
            assertThat(payload.getType()).isEqualTo(NotificationType.ADHERENCE_SUMMARY);
        }

        @Test
        @DisplayName("Should send good adherence summary")
        void shouldSendGoodAdherenceSummary() {
            // Given
            double adherenceRate = 75.0;
            int streak = 7;
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);

            // When
            reminderService.sendAdherenceSummary(testUser, adherenceRate, streak);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());

            NotificationPayload payload = payloadCaptor.getValue();
            assertThat(payload.getTitle()).contains("💪");
            assertThat(payload.getBody()).contains("Good progress");
            assertThat(payload.getBody()).contains("75%");
        }

        @Test
        @DisplayName("Should send low adherence summary with encouragement")
        void shouldSendLowAdherenceSummaryWithEncouragement() {
            // Given
            double adherenceRate = 45.0;
            int streak = 2;
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);

            // When
            reminderService.sendAdherenceSummary(testUser, adherenceRate, streak);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());

            NotificationPayload payload = payloadCaptor.getValue();
            assertThat(payload.getTitle()).contains("📈");
            assertThat(payload.getBody()).contains("45%");
            assertThat(payload.getBody()).contains("every dose counts");
        }

        @Test
        @DisplayName("Should handle boundary adherence rates")
        void shouldHandleBoundaryAdherenceRates() {
            // Test exactly 80% (good range)
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);
            reminderService.sendAdherenceSummary(testUser, 80.0, 5);

            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());
            assertThat(payloadCaptor.getValue().getTitle()).contains("Adherence Summary");

            // Test exactly 50% (boundary between low and medium)
            reminderService.sendAdherenceSummary(testUser, 50.0, 3);
            verify(notificationSender, times(2)).send(any(NotificationPayload.class));
        }
    }

    @Nested
    @DisplayName("Send Refill Reminder Tests")
    class SendRefillReminderTests {

        @Test
        @DisplayName("Should send refill reminder")
        void shouldSendRefillReminder() {
            // Given
            int daysLeft = 5;
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);

            // When
            reminderService.sendRefillReminder(testUser, testMedication, daysLeft);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());

            NotificationPayload payload = payloadCaptor.getValue();
            assertThat(payload.getUserId()).isEqualTo(testUser.getId());
            assertThat(payload.getTitle()).contains("Refill Reminder");
            assertThat(payload.getBody()).contains("5 days");
            assertThat(payload.getBody()).contains("Paracetamol");
            assertThat(payload.getType()).isEqualTo(NotificationType.REFILL_REMINDER);
            assertThat(payload.getData()).containsKey("medicationId");
        }

        @Test
        @DisplayName("Should handle single day left")
        void shouldHandleSingleDayLeft() {
            // Given
            int daysLeft = 1;
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);

            // When
            reminderService.sendRefillReminder(testUser, testMedication, daysLeft);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());
            assertThat(payloadCaptor.getValue().getBody()).contains("1 days");
        }

        @Test
        @DisplayName("Should handle different medications")
        void shouldHandleDifferentMedications() {
            // Given
            Medication anotherMed = Medication.builder()
                    .id(2L)
                    .name("Ibuprofen")
                    .dosage("400mg")
                    .build();
            when(notificationSender.send(any(NotificationPayload.class))).thenReturn(true);

            // When
            reminderService.sendRefillReminder(testUser, anotherMed, 3);

            // Then
            ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
            verify(notificationSender).send(payloadCaptor.capture());
            assertThat(payloadCaptor.getValue().getBody()).contains("Ibuprofen");
        }
    }
}
