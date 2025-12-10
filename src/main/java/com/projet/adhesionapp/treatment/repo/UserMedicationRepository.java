package com.projet.adhesionapp.treatment.repo;

import com.projet.adhesionapp.treatment.domain.UserMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserMedicationRepository extends JpaRepository<UserMedication, Long> {

    /**
     * Find all medications for a user
     */
    List<UserMedication> findByUserId(Long userId);

    /**
     * Find all active medications for a user
     */
    List<UserMedication> findByUserIdAndActiveTrue(Long userId);

    /**
     * Find all currently active medications (within date range and active=true)
     */
    @Query("SELECT m FROM UserMedication m WHERE m.user.id = :userId " +
            "AND m.active = true " +
            "AND (m.startDate IS NULL OR m.startDate <= :date) " +
            "AND (m.endDate IS NULL OR m.endDate >= :date)")
    List<UserMedication> findCurrentlyActiveMedications(
            @Param("userId") Long userId,
            @Param("date") LocalDate date);

    /**
     * Find chronic medications for a user
     */
    List<UserMedication> findByUserIdAndIsChronicTrue(Long userId);

    /**
     * Find medications with reminders enabled
     */
    List<UserMedication> findByUserIdAndRemindersEnabledTrue(Long userId);

    /**
     * Find medications with low stock
     */
    @Query("SELECT m FROM UserMedication m WHERE m.user.id = :userId " +
            "AND m.active = true " +
            "AND m.currentStock IS NOT NULL " +
            "AND m.lowStockThreshold IS NOT NULL " +
            "AND m.currentStock <= m.lowStockThreshold")
    List<UserMedication> findLowStockMedications(@Param("userId") Long userId);

    /**
     * Find medications by name (for suggestions/autocomplete)
     */
    @Query("SELECT DISTINCT m.name FROM UserMedication m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findMedicationNames(@Param("query") String query);

    /**
     * Count active medications for a user
     */
    long countByUserIdAndActiveTrue(Long userId);
}
