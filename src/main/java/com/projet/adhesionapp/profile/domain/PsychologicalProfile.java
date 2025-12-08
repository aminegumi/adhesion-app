package com.projet.adhesionapp.profile.domain;

import com.projet.adhesionapp.identity.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Psychological Profile entity that stores the analyzed profile of a patient
 * based on their psychological test results.
 */
@Entity
@Table(name = "psychological_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PsychologicalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * The type of psychological profile (e.g., "Anxious", "Motivated", "Avoidant",
     * "Resilient")
     */
    @Column(nullable = false, length = 100)
    private String profileType;

    /**
     * Anxiety score (0-100)
     */
    private Double anxietyScore;

    /**
     * Depression score (0-100)
     */
    private Double depressionScore;

    /**
     * Motivation score (0-100)
     */
    private Double motivationScore;

    /**
     * Self-efficacy score (0-100) - belief in one's ability to succeed
     */
    private Double selfEfficacyScore;

    /**
     * Social support score (0-100)
     */
    private Double socialSupportScore;

    /**
     * Health locus of control score (0-100) - internal vs external
     */
    private Double healthLocusScore;

    /**
     * Overall adherence risk score (0-1) - probability of non-adherence
     */
    private Double adherenceRiskScore;

    /**
     * AI-generated summary of the profile
     */
    @Column(length = 2000)
    private String summary;

    /**
     * AI-generated detailed interpretation
     */
    @Column(length = 5000)
    private String detailedInterpretation;

    /**
     * JSON containing dimension scores from all tests
     */
    @Column(length = 3000)
    private String dimensionScoresJson;

    /**
     * Profile status
     */
    @Enumerated(EnumType.STRING)
    private ProfileStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = ProfileStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum ProfileStatus {
        ACTIVE,
        ARCHIVED,
        PENDING_REVIEW
    }
}
