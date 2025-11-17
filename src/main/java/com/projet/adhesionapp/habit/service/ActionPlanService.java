package com.projet.adhesionapp.habit.service;

import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.habit.domain.ActionPlan;
import com.projet.adhesionapp.habit.model.PlanDto;
import com.projet.adhesionapp.habit.repo.ActionPlanRepository;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionPlanService {

    private final ActionPlanRepository actionPlanRepository;
    private final UserService userService;

    public ActionPlan createPlan(PlanDto dto) {
        User user = userService.findById(dto.userId());

        ActionPlan plan = ActionPlan.builder()
                .user(user)
                .date(dto.date())
                .primaryActionId(dto.primaryActionId())
                .altActionId(dto.altActionId())
                .build();

        return actionPlanRepository.save(plan);
    }

    public List<ActionPlan> getPlansForUser(Long userId) {
        User user = userService.findById(userId);
        return actionPlanRepository.findByUserOrderByDateDesc(user);
    }

    public ActionPlan getById(Long id) {
        return actionPlanRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan d'action introuvable"));
    }
}
