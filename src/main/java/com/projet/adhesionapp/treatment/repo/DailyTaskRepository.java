package com.projet.adhesionapp.treatment.repo;

import com.projet.adhesionapp.treatment.domain.DailyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyTaskRepository extends JpaRepository<DailyTask, Long> {

    List<DailyTask> findByTreatmentPlanId(Long planId);

    List<DailyTask> findByTreatmentPlanIdAndScheduledDate(Long planId, LocalDate date);

    List<DailyTask> findByTreatmentPlanUserIdAndScheduledDate(Long userId, LocalDate date);

    List<DailyTask> findByTreatmentPlanUserIdAndCompletedFalse(Long userId);

    @Modifying
    @Query("DELETE FROM DailyTask d WHERE d.treatmentPlan.id = :planId")
    void deleteByTreatmentPlanId(@Param("planId") Long planId);
}
