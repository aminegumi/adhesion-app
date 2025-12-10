package com.projet.adhesionapp.habit.web;

import com.projet.adhesionapp.habit.model.AdherenceStatsDto;
import com.projet.adhesionapp.habit.model.DoseActionRequest;
import com.projet.adhesionapp.habit.model.DoseLogDto;
import com.projet.adhesionapp.habit.service.DoseLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doses")
@RequiredArgsConstructor
public class DoseLogController {

    private final DoseLogService doseLogService;

    /**
     * Get today's doses for a user
     */
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<List<DoseLogDto>> getTodaysDoses(@PathVariable Long userId) {
        return ResponseEntity.ok(doseLogService.getTodaysDoses(userId));
    }

    /**
     * Get doses for a specific date
     */
    @GetMapping("/user/{userId}/date/{date}")
    public ResponseEntity<List<DoseLogDto>> getDosesForDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(doseLogService.getDosesForDate(userId, date));
    }

    /**
     * Get doses for a date range
     */
    @GetMapping("/user/{userId}/range")
    public ResponseEntity<List<DoseLogDto>> getDosesForRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(doseLogService.getDosesForRange(userId, from, to));
    }

    /**
     * Mark a dose as taken or skipped
     */
    @PostMapping("/{doseId}/action")
    public ResponseEntity<DoseLogDto> handleDoseAction(
            @PathVariable Long doseId,
            @RequestBody DoseActionRequest request) {
        return ResponseEntity.ok(doseLogService.handleDoseAction(doseId, request));
    }

    /**
     * Quick action: mark dose as taken
     */
    @PostMapping("/{doseId}/take")
    public ResponseEntity<DoseLogDto> takeDose(
            @PathVariable Long doseId,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(doseLogService.takeDose(doseId, notes));
    }

    /**
     * Get adherence statistics for a user
     */
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<AdherenceStatsDto> getAdherenceStats(@PathVariable Long userId) {
        return ResponseEntity.ok(doseLogService.getAdherenceStats(userId));
    }
}
