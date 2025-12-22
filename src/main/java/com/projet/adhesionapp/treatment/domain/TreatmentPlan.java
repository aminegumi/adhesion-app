package com.projet.adhesionapp.treatment.domain;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Treatment plan for a patient with goals, milestones, and daily tasks.
 */
@Entity
@Table(name = "treatment_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private PsychologicalProfile profile;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    /**
     * AI-generated detailed plan content
     */
    @Column(length = 5000)
    private String planContent;

    /**
     * Duration in weeks
     */
    private Integer durationWeeks;

    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Current medications the patient is taking
     */
    @Column(length = 1000)
    private String medications;

    /**
     * Identified issues to address
     */
    @Column(length = 1000)
    private String identifiedIssues;

    /**
     * Weekly goals JSON
     */
    @Column(length = 3000)
    private String weeklyGoalsJson;

    /**
     * Progress percentage (0-100)
     */
    private Integer progressPercentage;

    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    @OneToMany(mappedBy = "treatmentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<DailyTask> dailyTasks = new HashSet<>();

    @OneToMany(mappedBy = "treatmentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Medication> medicationList = new HashSet<>();

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = PlanStatus.ACTIVE;
        }
        if (progressPercentage == null) {
            progressPercentage = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum PlanStatus {
        DRAFT, ACTIVE, PAUSED, COMPLETED, CANCELLED
    }
}
