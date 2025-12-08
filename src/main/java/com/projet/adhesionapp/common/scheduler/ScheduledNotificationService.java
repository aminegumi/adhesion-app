package com.projet.adhesionapp.common.scheduler;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled service for automated tasks:
 * - Generate motivations and recommendations every 2 hours
 * - Check for users needing to retake tests (15+ days)
 * - Send notification reminders
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledNotificationService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final RecommendationService recommendationService;

    /**
     * Every 2 hours, generate fresh motivations and recommendations for active
     * users.
     * Cron: second minute hour day month weekday
     * "0 0 *​/2 * * *" = at minute 0 of every 2nd hour
     */
    @Scheduled(cron = "0 0 */2 * * *")
    public void generatePeriodicMotivations() {
        log.info("Running scheduled motivation generation...");

        List<User> activeUsers = userRepository.findByActiveTrue();
        int successCount = 0;

        for (User user : activeUsers) {
            // Only generate for users who have completed onboarding
            if (Boolean.TRUE.equals(user.getOnboardingCompleted())) {
                try {
                    recommendationService.generateRecommendations(user.getId());
                    successCount++;
                    log.debug("Generated recommendations for user {}", user.getId());
                } catch (Exception e) {
                    log.warn("Failed to generate recommendations for user {}: {}", user.getId(), e.getMessage());
                }
            }
        }

        log.info("Completed motivation generation for {} users", successCount);
    }

    /**
     * Daily at 9 AM, check for users needing to retake tests (15+ days since last
     * test).
     * Cron: "0 0 9 * * *" = at 09:00:00 every day
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void checkRetakeReminders() {
        log.info("Checking for users needing test retake...");

        Instant fifteenDaysAgo = Instant.now().minusSeconds(15L * 24 * 60 * 60);
        List<User> usersNeedingRetake = userRepository
                .findByOnboardingCompletedTrueAndLastTestCompletedAtBefore(fifteenDaysAgo);

        for (User user : usersNeedingRetake) {
            // Mark user as needing retake - they'll be prompted on next login
            user.setOnboardingCompleted(false);
            user.setCompletedTestsCount(0);
            userRepository.save(user);

            log.info("Marked user {} for test retake (last test: {})",
                    user.getId(), user.getLastTestCompletedAt());

            // TODO: Send push notification or email to user
            // notificationService.sendRetakeReminder(user);
        }

        log.info("Found {} users needing test retake", usersNeedingRetake.size());
    }

    /**
     * Every 30 minutes, log system health status.
     * Useful for monitoring the scheduler is running.
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    public void healthCheck() {
        log.debug("Notification scheduler health check - OK at {}", Instant.now());
    }
}
