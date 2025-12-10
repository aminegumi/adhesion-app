package com.projet.adhesionapp.common.notification;

import com.projet.adhesionapp.habit.domain.DoseLog;
import com.projet.adhesionapp.habit.repo.DoseLogRepository;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.treatment.domain.Medication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Service for sending medication reminders.
 * 
 * In a real production environment, this would integrate with:
 * - Firebase Cloud Messaging (FCM) for mobile push notifications
 * - Apple Push Notification Service (APNS) for iOS
 * - Web Push API for browser notifications
 * - Twilio for SMS reminders
 * - SendGrid/Mailgun for email reminders
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationReminderService {

    private final DoseLogRepository doseLogRepository;
    private final NotificationSender notificationSender;

    /**
     * Check every minute for doses needing reminders
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void checkAndSendReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<DoseLog> dosesNeedingReminders = doseLogRepository.findDosesNeedingReminders(today);

        for (DoseLog dose : dosesNeedingReminders) {
            Medication medication = dose.getMedication();
            int reminderMinutes = medication.getReminderMinutesBefore() != null
                    ? medication.getReminderMinutesBefore()
                    : 15;

            // Calculate when reminder should be sent
            LocalTime reminderTime = dose.getScheduledTime().minusMinutes(reminderMinutes);

            // Send if it's time (or past time but within grace period)
            if (now.isAfter(reminderTime) && now.isBefore(dose.getScheduledTime().plusMinutes(5))) {
                sendDoseReminder(dose);
            }
        }
    }

    /**
     * Send a dose reminder notification
     */
    @Transactional
    public void sendDoseReminder(DoseLog dose) {
        User user = dose.getUser();
        Medication medication = dose.getMedication();

        String title = "💊 Medication Reminder";
        String body = String.format("Time to take %s (%s)",
                medication.getName(),
                medication.getDosage() != null ? medication.getDosage() : "as prescribed");

        // Build notification data
        NotificationPayload payload = NotificationPayload.builder()
                .userId(user.getId())
                .title(title)
                .body(body)
                .type(NotificationType.MEDICATION_REMINDER)
                .actionUrl("/doses/" + dose.getId())
                .dataEntry("doseId", dose.getId().toString())
                .dataEntry("medicationId", medication.getId().toString())
                .dataEntry("medicationName", medication.getName())
                .dataEntry("scheduledTime", dose.getScheduledTime().toString())
                .build();

        // Send notification
        boolean sent = notificationSender.send(payload);

        if (sent) {
            dose.setReminderSent(true);
            dose.setReminderSentAt(Instant.now());
            doseLogRepository.save(dose);

            log.info("Sent medication reminder to user {} for {} at {}",
                    user.getId(), medication.getName(), dose.getScheduledTime());
        }
    }

    /**
     * Send a missed dose alert
     */
    public void sendMissedDoseAlert(DoseLog dose) {
        User user = dose.getUser();
        Medication medication = dose.getMedication();

        String title = "⚠️ Missed Medication";
        String body = String.format("You missed your %s dose scheduled for %s. " +
                "Don't take a double dose - consult your pharmacist if unsure.",
                medication.getName(), dose.getScheduledTime());

        NotificationPayload payload = NotificationPayload.builder()
                .userId(user.getId())
                .title(title)
                .body(body)
                .type(NotificationType.MISSED_DOSE_ALERT)
                .priority(NotificationPriority.HIGH)
                .build();

        notificationSender.send(payload);
    }

    /**
     * Send adherence summary (daily or weekly)
     */
    public void sendAdherenceSummary(User user, double adherenceRate, int streak) {
        String emoji = adherenceRate >= 80 ? "🌟" : adherenceRate >= 50 ? "💪" : "📈";
        String title = emoji + " Your Adherence Summary";

        String body;
        if (adherenceRate >= 90) {
            body = String.format("Excellent work! You're at %.0f%% adherence with a %d-day streak! Keep it up!",
                    adherenceRate, streak);
        } else if (adherenceRate >= 70) {
            body = String.format(
                    "Good progress! You're at %.0f%% adherence. A few small improvements can get you to 100%%!",
                    adherenceRate);
        } else {
            body = String.format("Your adherence is at %.0f%%. Let's work together to improve - every dose counts!",
                    adherenceRate);
        }

        NotificationPayload payload = NotificationPayload.builder()
                .userId(user.getId())
                .title(title)
                .body(body)
                .type(NotificationType.ADHERENCE_SUMMARY)
                .build();

        notificationSender.send(payload);
    }

    /**
     * Send refill reminder
     */
    public void sendRefillReminder(User user, Medication medication, int daysLeft) {
        String title = "🔄 Refill Reminder";
        String body = String.format("You have approximately %d days of %s left. " +
                "Consider ordering a refill soon.", daysLeft, medication.getName());

        NotificationPayload payload = NotificationPayload.builder()
                .userId(user.getId())
                .title(title)
                .body(body)
                .type(NotificationType.REFILL_REMINDER)
                .data("medicationId", medication.getId().toString())
                .build();

        notificationSender.send(payload);
    }
}
