package com.projet.adhesionapp.habit.repo;

import com.projet.adhesionapp.habit.domain.DoseLog;
import com.projet.adhesionapp.habit.domain.DoseLog.DoseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoseLogRepository extends JpaRepository<DoseLog, Long> {

    /**
     * Find all doses for a user on a specific date
     */
    List<DoseLog> findByUserIdAndScheduledDateOrderByScheduledTime(Long userId, LocalDate date);

    /**
     * Find all doses for a user in a date range
     */
    List<DoseLog> findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscScheduledTimeAsc(
            Long userId, LocalDate from, LocalDate to);

    /**
     * Find a specific dose log
     */
    Optional<DoseLog> findByMedicationIdAndScheduledDateAndScheduledTime(
            Long medicationId, LocalDate date, LocalTime time);

    /**
     * Find pending doses that are now overdue (for marking as missed)
     */
    @Query("SELECT d FROM DoseLog d WHERE d.status = 'PENDING' " +
            "AND (d.scheduledDate < :today OR (d.scheduledDate = :today AND d.scheduledTime < :now))")
    List<DoseLog> findOverduePendingDoses(@Param("today") LocalDate today, @Param("now") LocalTime now);

    /**
     * Find doses needing reminders
     */
    @Query("SELECT d FROM DoseLog d WHERE d.status = 'PENDING' " +
            "AND d.reminderSent = false " +
            "AND d.scheduledDate = :today " +
            "AND d.medication.notificationsEnabled = true")
    List<DoseLog> findDosesNeedingReminders(@Param("today") LocalDate today);

    /**
     * Count doses by status for a user in a date range
     */
    @Query("SELECT d.status, COUNT(d) FROM DoseLog d " +
            "WHERE d.user.id = :userId " +
            "AND d.scheduledDate BETWEEN :from AND :to " +
            "GROUP BY d.status")
    List<Object[]> countByStatusForUserInRange(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Calculate adherence rate for a user (taken / total * 100)
     */
    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN d.status = 'TAKEN' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 0) " +
            "FROM DoseLog d " +
            "WHERE d.user.id = :userId " +
            "AND d.scheduledDate BETWEEN :from AND :to " +
            "AND d.status != 'PENDING'")
    Double calculateAdherenceRate(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Get user's adherence streak (consecutive days with 100% adherence)
     */
    @Query(value = "WITH daily_adherence AS (" +
            "  SELECT scheduled_date, " +
            "         CASE WHEN COUNT(*) = SUM(CASE WHEN status = 'TAKEN' THEN 1 ELSE 0 END) THEN 1 ELSE 0 END as perfect "
            +
            "  FROM dose_logs " +
            "  WHERE user_id = :userId AND status != 'PENDING' " +
            "  GROUP BY scheduled_date " +
            "  ORDER BY scheduled_date DESC " +
            ") " +
            "SELECT COUNT(*) FROM (" +
            "  SELECT scheduled_date, perfect, " +
            "         SUM(CASE WHEN perfect = 0 THEN 1 ELSE 0 END) OVER (ORDER BY scheduled_date DESC) as grp " +
            "  FROM daily_adherence " +
            ") sub WHERE grp = 0", nativeQuery = true)
    Integer calculateCurrentStreak(@Param("userId") Long userId);

    /**
     * Find all doses for a specific medication
     */
    List<DoseLog> findByMedicationIdOrderByScheduledDateDescScheduledTimeDesc(Long medicationId);

    /**
     * Count total doses by user
     */
    long countByUserId(Long userId);

    /**
     * Count taken doses by user
     */
    long countByUserIdAndStatus(Long userId, DoseStatus status);
}
