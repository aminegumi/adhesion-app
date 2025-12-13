package com.projet.adhesionapp.identity.web;

import com.projet.adhesionapp.assessment.model.TestResultDto;
import com.projet.adhesionapp.assessment.service.TestSessionService;
import com.projet.adhesionapp.common.exception.BadRequestException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.treatment.domain.TreatmentPlan;
import com.projet.adhesionapp.treatment.model.TreatmentPlanDto;
import com.projet.adhesionapp.treatment.service.TreatmentPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-only endpoints for managing users and viewing consented data.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final TestSessionService testSessionService;
    private final TreatmentPlanService treatmentPlanService;

    /**
     * Get all users (admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    /**
     * Get users who have given data sharing consent
     */
    @GetMapping("/users/consented")
    public ResponseEntity<List<User>> getConsentedUsers() {
        return ResponseEntity.ok(userService.findConsentedUsers());
    }

    /**
     * Get test results for a specific user (requires user consent)
     */
    @GetMapping("/users/{userId}/test-results")
    public ResponseEntity<List<TestResultDto>> getUserTestResults(@PathVariable Long userId) {
        User user = userService.findById(userId);
        
        // Check if user has given consent
        if (!Boolean.TRUE.equals(user.getConsentGiven())) {
            throw new BadRequestException("User has not given data sharing consent");
        }
        
        List<TestResultDto> results = testSessionService.getPatientTestHistory(userId);
        return ResponseEntity.ok(results);
    }

    /**
     * Get treatment plans for a specific user (requires user consent)
     */
    @GetMapping("/users/{userId}/treatment-plans")
    public ResponseEntity<List<TreatmentPlanDto>> getUserTreatmentPlans(@PathVariable Long userId) {
        User user = userService.findById(userId);
        
        // Check if user has given consent
        if (!Boolean.TRUE.equals(user.getConsentGiven())) {
            throw new BadRequestException("User has not given data sharing consent");
        }
        
        List<TreatmentPlan> plans = treatmentPlanService.getUserPlans(userId);
        List<TreatmentPlanDto> planDtos = plans.stream()
            .map(treatmentPlanService::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(planDtos);
    }

    /**
     * Get admin users
     */
    @GetMapping("/admins")
    public ResponseEntity<List<User>> getAdmins() {
        return ResponseEntity.ok(userService.findByRole("ADMIN"));
    }

    /**
     * Create a new admin user
     */
    @PostMapping("/create")
    public ResponseEntity<User> createAdmin(@RequestBody CreateAdminRequest request) {
        User admin = userService.createAdmin(
            request.getEmail(),
            request.getPassword(),
            request.getDisplayName(),
            request.getBirthDate(),
            request.getGender()
        );
        return ResponseEntity.ok(admin);
    }

    @lombok.Data
    public static class CreateAdminRequest {
        private String email;
        private String password;
        private String displayName;
        private String birthDate;
        private String gender;
    }
}

