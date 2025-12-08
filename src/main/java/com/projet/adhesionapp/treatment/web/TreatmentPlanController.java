package com.projet.adhesionapp.treatment.web;

import com.projet.adhesionapp.treatment.domain.DailyTask;
import com.projet.adhesionapp.treatment.domain.Medication;
import com.projet.adhesionapp.treatment.domain.TreatmentPlan;
import com.projet.adhesionapp.treatment.model.*;
import com.projet.adhesionapp.treatment.service.TreatmentPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/treatment-plans")
@RequiredArgsConstructor
public class TreatmentPlanController {

    private final TreatmentPlanService treatmentPlanService;

    @PostMapping
    public ResponseEntity<TreatmentPlanDto> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        TreatmentPlan plan = treatmentPlanService.generateTreatmentPlan(request);
        return ResponseEntity.ok(treatmentPlanService.toDto(plan));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TreatmentPlanDto> getPlan(@PathVariable Long id) {
        TreatmentPlan plan = treatmentPlanService.getById(id);
        return ResponseEntity.ok(treatmentPlanService.toDto(plan));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TreatmentPlanDto>> getUserPlans(@PathVariable Long userId) {
        return ResponseEntity.ok(treatmentPlanService.getUserPlans(userId).stream()
                .map(treatmentPlanService::toDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<TreatmentPlanDto>> getActivePlans(@PathVariable Long userId) {
        return ResponseEntity.ok(treatmentPlanService.getActivePlans(userId).stream()
                .map(treatmentPlanService::toDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/tasks/user/{userId}/today")
    public ResponseEntity<List<DailyTaskDto>> getTodaysTasks(@PathVariable Long userId) {
        return ResponseEntity.ok(treatmentPlanService.getTodaysTasks(userId).stream()
                .map(treatmentPlanService::toTaskDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/tasks/user/{userId}/pending")
    public ResponseEntity<List<DailyTaskDto>> getPendingTasks(@PathVariable Long userId) {
        return ResponseEntity.ok(treatmentPlanService.getPendingTasks(userId).stream()
                .map(treatmentPlanService::toTaskDto)
                .collect(Collectors.toList()));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<DailyTaskDto> completeTask(
            @PathVariable Long taskId,
            @RequestParam(required = false) String notes) {
        DailyTask task = treatmentPlanService.completeTask(taskId, notes);
        return ResponseEntity.ok(treatmentPlanService.toTaskDto(task));
    }

    // ==================== Medication Endpoints ====================

    @GetMapping("/medications/user/{userId}")
    public ResponseEntity<List<MedicationDto>> getUserMedications(@PathVariable Long userId) {
        return ResponseEntity.ok(treatmentPlanService.getUserMedications(userId).stream()
                .map(treatmentPlanService::toMedicationDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/medications/user/{userId}/active")
    public ResponseEntity<List<MedicationDto>> getActiveMedications(@PathVariable Long userId) {
        return ResponseEntity.ok(treatmentPlanService.getActiveMedications(userId).stream()
                .map(treatmentPlanService::toMedicationDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/medications/user/{userId}/chronic")
    public ResponseEntity<List<MedicationDto>> getChronicMedications(@PathVariable Long userId) {
        return ResponseEntity.ok(treatmentPlanService.getChronicMedications(userId).stream()
                .map(treatmentPlanService::toMedicationDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{planId}/medications")
    public ResponseEntity<List<MedicationDto>> getPlanMedications(@PathVariable Long planId) {
        return ResponseEntity.ok(treatmentPlanService.getPlanMedications(planId).stream()
                .map(treatmentPlanService::toMedicationDto)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{planId}/medications")
    public ResponseEntity<MedicationDto> addMedicationToPlan(
            @PathVariable Long planId,
            @Valid @RequestBody MedicationRequest request) {
        Medication med = treatmentPlanService.addMedicationToPlan(planId, request);
        return ResponseEntity.ok(treatmentPlanService.toMedicationDto(med));
    }

    @PutMapping("/medications/{medicationId}")
    public ResponseEntity<MedicationDto> updateMedication(
            @PathVariable Long medicationId,
            @Valid @RequestBody MedicationRequest request) {
        Medication med = treatmentPlanService.updateMedication(medicationId, request);
        return ResponseEntity.ok(treatmentPlanService.toMedicationDto(med));
    }

    @DeleteMapping("/medications/{medicationId}")
    public ResponseEntity<Void> deleteMedication(@PathVariable Long medicationId) {
        treatmentPlanService.deleteMedication(medicationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/medications/user/{userId}/notifications")
    public ResponseEntity<List<MedicationDto>> getMedicationsForNotification(@PathVariable Long userId) {
        return ResponseEntity.ok(treatmentPlanService.getMedicationsForNotification(userId));
    }
}
