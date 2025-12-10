package com.projet.adhesionapp.habit.domain;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.treatment.domain.Medication;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Tracks individual medication dose events.
 * Each time a user takes (or skips) a scheduled dose, a record is created.
 */
@Entity
@Table(name = "dose_logs", indexes = {
        @Index(name = "idx_dose_log_user_date", columnList = "user_id, scheduled_date"),
        @Index(name = "idx_dose_log_medication", columnList = "medication_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medication_id")
    private Medication medication;

    /**
     * The date this dose was scheduled for
     */
    @Column(nullable = false)
    private LocalDate scheduledDate;

    /**
     * The time this dose was scheduled for (e.g., 08:00)
     */
    @Column(nullable = false)
    private LocalTime scheduledTime;

    /**
     * Status of the dose
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DoseStatus status = DoseStatus.PENDING;

    /**
     * When the user actually took the dose (null if not taken)
     */
    private Instant takenAt;

    /**
     * Delay in minutes from scheduled time (positive = late, negative = early)
     */
    private Integer delayMinutes;

    /**
     * Optional notes from user (e.g., "felt nauseous", "took with food")
     */
    @Column(length = 500)
    private String notes;

    /**
     * If skipped, reason for skipping
     */
    @Enumerated(EnumType.STRING)
    private SkipReason skipReason;

    /**
     * Was a reminder sent for this dose?
     */
    @Builder.Default
    private Boolean reminderSent = false;

    /**
     * When the reminder was sent
     */
    private Instant reminderSentAt;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Mark dose as taken
     */
    public void markTaken(String notes) {
        this.status = DoseStatus.TAKEN;
        this.takenAt = Instant.now();
        this.notes = notes;

        // Calculate delay
        Instant scheduledInstant = scheduledDate.atTime(scheduledTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant();
        this.delayMinutes = (int) java.time.Duration.between(scheduledInstant, this.takenAt).toMinutes();
    }

    /**
     * Mark dose as skipped
     */
    public void markSkipped(SkipReason reason, String notes) {
        this.status = DoseStatus.SKIPPED;
        this.skipReason = reason;
        this.notes = notes;
    }

    public enum DoseStatus {
        PENDING, // Not yet due
        TAKEN, // User confirmed taking
        SKIPPED, // User explicitly skipped
        MISSED // Time passed without confirmation
    }

    public enum SkipReason {
        FORGOT,
        SIDE_EFFECTS,
        RAN_OUT,
        FEELING_BETTER,
        DOCTOR_ADVISED,
        OTHER
    }
}
