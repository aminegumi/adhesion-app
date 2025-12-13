package com.projet.adhesionapp.identity.repo;

import com.projet.adhesionapp.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Find users who completed onboarding but haven't taken tests recently
     */
    List<User> findByOnboardingCompletedTrueAndLastTestCompletedAtBefore(Instant cutoffDate);

    /**
     * Find users who need onboarding (haven't completed initial tests)
     */
    List<User> findByOnboardingCompletedFalse();

    /**
     * Find active users for notifications
     */
    List<User> findByActiveTrue();

    /**
     * Find users who have given data sharing consent
     */
    List<User> findByConsentGivenTrueAndActiveTrue();

    /**
     * Find users by role
     */
    List<User> findByRole(String role);
}
