package com.projet.adhesionapp.identity.web;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.model.UserStatusDto;
import com.projet.adhesionapp.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de gestion basique des utilisateurs (patients).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.findById(id);
    }

    /**
     * Get user status - tells the frontend what the user should do next.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<UserStatusDto> getUserStatus(@PathVariable Long id) {
        User user = userService.findById(id);

        int testsRemaining = Math.max(0,
                (user.getRequiredTestsCount() != null ? user.getRequiredTestsCount() : 2) -
                        (user.getCompletedTestsCount() != null ? user.getCompletedTestsCount() : 0));

        boolean needsRetake = user.needsRetake();
        boolean onboardingCompleted = Boolean.TRUE.equals(user.getOnboardingCompleted());

        String nextAction = UserStatusDto.determineNextAction(onboardingCompleted, needsRetake, testsRemaining);

        UserStatusDto status = new UserStatusDto(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                onboardingCompleted,
                needsRetake,
                user.getRequiredTestsCount() != null ? user.getRequiredTestsCount() : 2,
                user.getCompletedTestsCount() != null ? user.getCompletedTestsCount() : 0,
                testsRemaining,
                user.getLastTestCompletedAt(),
                nextAction);

        return ResponseEntity.ok(status);
    }

    @PostMapping("/{id}/deactivate")
    public void deactivate(@PathVariable Long id) {
        userService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public void activate(@PathVariable Long id) {
        userService.activate(id);
    }

}
