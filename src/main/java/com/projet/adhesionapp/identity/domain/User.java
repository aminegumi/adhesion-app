package com.projet.adhesionapp.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Utilisateur (patient) de l'application.
 * D'après le diagramme : id, email, passwordHash, displayName, birthDate,
 * gender, active, createdAt.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 255)
    private String displayName;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 50)
    private String gender;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Whether the user has completed initial psychological tests and has a profile.
     * Users must complete onboarding before accessing full app features.
     */
    @Column(nullable = true)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    /**
     * When the user last completed psychological tests.
     * Used to prompt retake after 15 days.
     */
    private Instant lastTestCompletedAt;

    /**
     * Number of tests required for profile creation (default 2: MMAS-8 + one other)
     */
    @Column(nullable = true)
    @Builder.Default
    private Integer requiredTestsCount = 2;

    /**
     * Number of tests completed during current onboarding
     */
    @Column(nullable = true)
    @Builder.Default
    private Integer completedTestsCount = 0;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (active == null) {
            active = Boolean.TRUE;
        }
        if (onboardingCompleted == null) {
            onboardingCompleted = false;
        }
        if (requiredTestsCount == null) {
            requiredTestsCount = 2;
        }
        if (completedTestsCount == null) {
            completedTestsCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Check if user needs to retake tests (15 days since last test)
     */
    public boolean needsRetake() {
        if (lastTestCompletedAt == null)
            return false;
        Instant fifteenDaysAgo = Instant.now().minusSeconds(15 * 24 * 60 * 60);
        return lastTestCompletedAt.isBefore(fifteenDaysAgo);
    }
}
