package com.projet.adhesionapp.assessment.web;

import com.projet.adhesionapp.assessment.model.*;
import com.projet.adhesionapp.assessment.service.TestSessionService;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "Test Sessions", description = "Psychological test session management")
public class SessionController {

    private final TestSessionService service;
    private final UserService userService;

    @PostMapping("/start")
    @Operation(summary = "Start a new test session", description = "Initializes a psychological test session for a patient")
    public TestSessionDto start(@Valid @RequestBody StartSessionRequest req) {
        User user = userService.findById(req.userId());
        return service.startSession(req.testId(), user);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit test answers", description = "Submits answers and returns scored results with clinical interpretation")
    public TestResultDto submit(@PathVariable Long id,
            @RequestBody List<AnswerSubmissionDto> answers) {
        return service.submitAnswers(id, answers);
    }

    @GetMapping("/patient/{userId}/history")
    @Operation(summary = "Get patient test history", description = "Returns all completed tests for a patient with interpretations")
    public List<TestResultDto> getPatientHistory(@PathVariable Long userId) {
        return service.getPatientTestHistory(userId);
    }

    @GetMapping("/{id}/result")
    @Operation(summary = "Get test result", description = "Returns the scored result and interpretation for a completed test")
    public TestResultDto getResult(@PathVariable Long id) {
        return service.getTestResult(id);
    }
}
