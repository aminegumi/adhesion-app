package com.projet.adhesionapp.treatment.application;

import com.projet.adhesionapp.treatment.model.UserMedicationDto;
import com.projet.adhesionapp.treatment.model.CreateMedicationRequest;
import com.projet.adhesionapp.treatment.service.UserMedicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing user medications.
 * Provides endpoints for CRUD operations on medications
 * and generating daily dose schedules.
 */
@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class UserMedicationController {

    private final UserMedicationService userMedicationService;

    /**
     * Get all medications for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserMedicationDto>> getUserMedications(@PathVariable Long userId) {
        List<UserMedicationDto> medications = userMedicationService.getUserMedications(userId);
        return ResponseEntity.ok(medications);
    }

    /**
     * Get only active medications for a user
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<UserMedicationDto>> getActiveMedications(@PathVariable Long userId) {
        List<UserMedicationDto> medications = userMedicationService.getActiveMedications(userId);
        return ResponseEntity.ok(medications);
    }

    /**
     * Add a new medication for a user
     */
    @PostMapping
    public ResponseEntity<UserMedicationDto> addMedication(@RequestBody CreateMedicationRequest request) {
        UserMedicationDto medication = userMedicationService.addMedication(request);

        // Generate doses for today in a separate transaction
        try {
            userMedicationService.generateDailySchedule(request.userId());
        } catch (Exception e) {
            // Log but don't fail the response - doses can be generated later
            log.warn("Failed to generate doses for user {}: {}", request.userId(), e.getMessage());
        }

        return ResponseEntity.ok(medication);
    }

    /**
     * Update an existing medication
     */
    @PutMapping("/{medicationId}")
    public ResponseEntity<UserMedicationDto> updateMedication(
            @PathVariable Long medicationId,
            @RequestBody CreateMedicationRequest request) {
        UserMedicationDto medication = userMedicationService.updateMedication(medicationId, request);
        return ResponseEntity.ok(medication);
    }

    /**
     * Delete a medication
     */
    @DeleteMapping("/{medicationId}")
    public ResponseEntity<Void> deleteMedication(@PathVariable Long medicationId) {
        userMedicationService.deleteMedication(medicationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle medication active status
     */
    @PatchMapping("/{medicationId}/toggle-active")
    public ResponseEntity<UserMedicationDto> toggleActive(@PathVariable Long medicationId) {
        UserMedicationDto medication = userMedicationService.toggleActive(medicationId);
        return ResponseEntity.ok(medication);
    }

    /**
     * Update scheduled times for a medication
     */
    @PutMapping("/{medicationId}/times")
    public ResponseEntity<UserMedicationDto> updateScheduledTimes(
            @PathVariable Long medicationId,
            @RequestBody List<String> times) {
        // Convert string times to LocalTime
        List<LocalTime> scheduledTimes = times.stream()
                .map(LocalTime::parse)
                .toList();
        UserMedicationDto medication = userMedicationService.updateScheduledTimes(medicationId, scheduledTimes);
        return ResponseEntity.ok(medication);
    }

    /**
     * Toggle reminders for a medication
     */
    @PatchMapping("/{medicationId}/toggle-reminders")
    public ResponseEntity<UserMedicationDto> toggleReminders(@PathVariable Long medicationId) {
        UserMedicationDto medication = userMedicationService.toggleReminders(medicationId);
        return ResponseEntity.ok(medication);
    }

    /**
     * Generate daily schedule for a user (creates DoseLog entries for today)
     */
    @PostMapping("/user/{userId}/generate-schedule")
    public ResponseEntity<Map<String, Object>> generateDailySchedule(@PathVariable Long userId) {
        int dosesCreated = userMedicationService.generateDailySchedule(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "dosesCreated", dosesCreated,
                "message", dosesCreated + " doses scheduled for today"));
    }

    /**
     * Get a single medication by ID
     */
    @GetMapping("/{medicationId}")
    public ResponseEntity<UserMedicationDto> getMedication(@PathVariable Long medicationId) {
        UserMedicationDto medication = userMedicationService.getMedication(medicationId);
        return ResponseEntity.ok(medication);
    }
}
