package com.projet.adhesionapp.identity.service;

import com.projet.adhesionapp.common.exception.BadRequestException;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String email,
            String rawPassword,
            String displayName,
            LocalDate birthDate,
            String gender) {

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Un utilisateur existe déjà avec cet email.");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .displayName(displayName)
                .birthDate(birthDate)
                .gender(gender)
                .active(true)
                .onboardingCompleted(false)
                .requiredTestsCount(2)
                .completedTestsCount(0)
                .build();

        return userRepository.save(user);
    }

    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadRequestException("Email ou mot de passe incorrect.");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
    }

    public void deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
        user.setActive(false);
    }

    public void activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
        user.setActive(true);
    }

    /**
     * Increment the completed tests count for a user.
     * If they've completed the required number, mark onboarding as complete.
     */
    public void incrementTestsCompleted(Long userId) {
        User user = findById(userId);
        int newCount = (user.getCompletedTestsCount() != null ? user.getCompletedTestsCount() : 0) + 1;
        user.setCompletedTestsCount(newCount);
        user.setLastTestCompletedAt(Instant.now());

        int requiredTests = user.getRequiredTestsCount() != null ? user.getRequiredTestsCount() : 2;
        if (newCount >= requiredTests) {
            user.setOnboardingCompleted(true);
        }
        userRepository.save(user);
    }

    /**
     * Mark user as needing to retake tests (after 15 days).
     */
    public void markNeedsRetake(Long userId) {
        User user = findById(userId);
        user.setOnboardingCompleted(false);
        user.setCompletedTestsCount(0);
        userRepository.save(user);
    }

    /**
     * Complete user onboarding (called when profile is created).
     */
    public void completeOnboarding(Long userId) {
        User user = findById(userId);
        user.setOnboardingCompleted(true);
        user.setLastTestCompletedAt(Instant.now());
        userRepository.save(user);
    }

    /**
     * Get users who need to retake tests (15+ days since last test).
     */
    @Transactional(readOnly = true)
    public List<User> getUsersNeedingRetake() {
        Instant fifteenDaysAgo = Instant.now().minusSeconds(15L * 24 * 60 * 60);
        return userRepository.findByOnboardingCompletedTrueAndLastTestCompletedAtBefore(fifteenDaysAgo);
    }
}
