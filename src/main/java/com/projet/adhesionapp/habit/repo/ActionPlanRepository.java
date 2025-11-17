package com.projet.adhesionapp.habit.repo;


import com.projet.adhesionapp.habit.domain.ActionPlan;
import com.projet.adhesionapp.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ActionPlanRepository extends JpaRepository<ActionPlan, Long> {

    List<ActionPlan> findByUserOrderByDateDesc(User user);

    List<ActionPlan> findByUserAndDate(User user, LocalDate date);
}
