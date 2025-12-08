package com.projet.adhesionapp.recommendation.domain;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Personalized recommendation for a patient based on their psychological
 * profile.
 */
@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private PsychologicalProfile profile;

    /**
     * Category of recommendation (e.g., "Medication", "Lifestyle", "Mental Health",
     * "Social", "Exercise")
     */
    @Column(nullable = false, length = 50)
    private String category;

    /**
     * Priority level (1-5, where 1 is highest priority)
     */
    private Integer priority;

    /**
     * Short title of the recommendation
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Detailed description of the recommendation
     */
    @Column(length = 2000)
    private String description;

    /**
     * AI-generated actionable steps
     */
    @Column(length = 3000)
    private String actionableSteps;

    /**
     * Expected benefits of following this recommendation
     */
    @Column(length = 1000)
    private String expectedBenefits;

    /**
     * Time frame for implementing (e.g., "Daily", "Weekly", "One-time")
     */
    private String timeFrame;

    /**
     * Difficulty level (Easy, Medium, Hard)
     */
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    /**
     * Status of the recommendation
     */
    @Enumerated(EnumType.STRING)
    private RecommendationStatus status;

    /**
     * Whether the user has marked this as completed
     */
    private Boolean completed;

    /**
     * User feedback or notes
     */
    @Column(length = 500)
    private String userFeedback;

    private Instant createdAt;
    private Instant completedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = RecommendationStatus.ACTIVE;
        }
        if (completed == null) {
            completed = false;
        }
    }

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    public enum RecommendationStatus {
        ACTIVE, COMPLETED, DISMISSED, EXPIRED
    }
}
