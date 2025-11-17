package com.projet.adhesionapp.habit.web;

import com.projet.adhesionapp.habit.domain.ActionPlan;
import com.projet.adhesionapp.habit.model.PlanDto;
import com.projet.adhesionapp.habit.service.ActionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class ActionPlanController {

    private final ActionPlanService actionPlanService;

    @PostMapping
    public PlanDto create(@RequestBody PlanDto dto) {
        ActionPlan saved = actionPlanService.createPlan(dto);
        return new PlanDto(
                saved.getId(),
                saved.getUser().getId(),
                saved.getDate(),
                saved.getPrimaryActionId(),
                saved.getAltActionId()
        );
    }

    @GetMapping("/user/{userId}")
    public List<PlanDto> getForUser(@PathVariable Long userId) {
        return actionPlanService.getPlansForUser(userId)
                .stream()
                .map(p -> new PlanDto(
                        p.getId(),
                        p.getUser().getId(),
                        p.getDate(),
                        p.getPrimaryActionId(),
                        p.getAltActionId()
                ))
                .toList();
    }
}
