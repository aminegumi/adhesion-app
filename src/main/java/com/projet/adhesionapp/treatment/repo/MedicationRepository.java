package com.projet.adhesionapp.treatment.repo;

import com.projet.adhesionapp.treatment.domain.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {

    List<Medication> findByTreatmentPlanId(Long planId);

    List<Medication> findByTreatmentPlanUserId(Long userId);

    /**
     * Find all chronic medications for a user.
     */
    List<Medication> findByTreatmentPlanUserIdAndIsChronicTrue(Long userId);

    /**
     * Find all medications with notifications enabled for a user.
     */
    List<Medication> findByTreatmentPlanUserIdAndNotificationsEnabledTrue(Long userId);

    /**
     * Find all active medications for a user (within date range).
     */
    @Query("SELECT m FROM Medication m WHERE m.treatmentPlan.user.id = :userId " +
            "AND (m.startDate IS NULL OR m.startDate <= CURRENT_DATE) " +
            "AND (m.endDate IS NULL OR m.endDate >= CURRENT_DATE) " +
            "AND m.treatmentPlan.status = 'ACTIVE'")
    List<Medication> findActiveMedicationsByUserId(@Param("userId") Long userId);

    /**
     * Find medications needing reminders at a specific time.
     */
    @Query("SELECT m FROM Medication m WHERE m.notificationsEnabled = true " +
            "AND m.scheduledTimes LIKE %:time%")
    List<Medication> findMedicationsWithScheduledTime(@Param("time") String time);
}
