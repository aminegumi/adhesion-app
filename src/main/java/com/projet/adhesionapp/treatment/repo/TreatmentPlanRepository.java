package com.projet.adhesionapp.treatment.repo;

import com.projet.adhesionapp.treatment.domain.TreatmentPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, Long> {

    @EntityGraph(attributePaths = { "medicationList", "user" })
    List<TreatmentPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = { "medicationList", "user" })
    List<TreatmentPlan> findByUserIdAndStatus(Long userId, TreatmentPlan.PlanStatus status);

    List<TreatmentPlan> findByStatus(TreatmentPlan.PlanStatus status);

    @EntityGraph(attributePaths = { "medicationList", "dailyTasks", "user" })
    @Query("SELECT DISTINCT p FROM TreatmentPlan p WHERE p.id = :id")
    Optional<TreatmentPlan> findByIdWithMedications(@Param("id") Long id);
}
