package com.projet.adhesionapp.treatment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Daily task as part of a treatment plan.
 */
@Entity
@Table(name = "daily_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "treatment_plan_id")
    private TreatmentPlan treatmentPlan;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    /**
     * Category of the task (e.g., "Medication", "Exercise", "Mindfulness",
     * "Social")
     */
    private String category;

    /**
     * Scheduled date for the task
     */
    private LocalDate scheduledDate;

    /**
     * Time of day (e.g., "Morning", "Afternoon", "Evening", "Any")
     */
    private String timeOfDay;

    /**
     * Estimated duration in minutes
     */
    private Integer durationMinutes;

    /**
     * Whether this task repeats daily
     */
    @Builder.Default
    private Boolean isRecurring = false;

    /**
     * Whether the task was completed
     */
    @Builder.Default
    private Boolean completed = false;

    /**
     * When the task was completed
     */
    private Instant completedAt;

    /**
     * User notes or feedback
     */
    @Column(length = 500)
    private String notes;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (completed == null) {
            completed = false;
        }
        if (isRecurring == null) {
            isRecurring = false;
        }
    }
}
