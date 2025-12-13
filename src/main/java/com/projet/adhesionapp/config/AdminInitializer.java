package com.projet.adhesionapp.config;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Initializes an admin account on startup if it doesn't exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@gmail.com";
        
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .displayName("Admin User")
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .gender("Male")
                    .role("ADMIN")
                    .consentGiven(true)
                    .active(true)
                    .onboardingCompleted(true)
                    .build();
            
            userRepository.save(admin);
            log.info("✅ Admin account created: {}", adminEmail);
        } else {
            // Update existing user to be admin
            userRepository.findByEmail(adminEmail).ifPresent(user -> {
                user.setRole("ADMIN");
                user.setPasswordHash(passwordEncoder.encode("admin123"));
                userRepository.save(user);
                log.info("✅ Admin account updated: {}", adminEmail);
            });
        }
    }
}

