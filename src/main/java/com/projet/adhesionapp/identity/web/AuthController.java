package com.projet.adhesionapp.identity.web;


import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Endpoints d'inscription et de connexion.
 * (Le "token" renvoyé est pour l’instant un UUID fictif, en attendant le JWT.)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public User register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName(),
                request.getBirthDate(),
                request.getGender()
        );
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.getEmail(), request.getPassword());
        // Token fake pour le moment
        String token = UUID.randomUUID().toString();
        return new LoginResponse(token, user);
    }

    // DTOs internes pour les requêtes / réponses

    @Data
    public static class RegisterRequest {
        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String password;

        @NotBlank
        private String displayName;

        @NotNull
        private LocalDate birthDate;

        @NotBlank
        private String gender;
    }

    @Data
    public static class LoginRequest {
        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String password;
    }

    public record LoginResponse(String token, User user) { }
}
