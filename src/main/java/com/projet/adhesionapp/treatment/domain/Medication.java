package com.projet.adhesionapp.treatment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Medication entity representing a medication in a treatment plan.
 * Supports multiple daily doses with specific times for notifications.
 */
@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "treatment_plan_id")
    private TreatmentPlan treatmentPlan;

    /**
     * Name of the medication (e.g., "Metformin")
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Dosage information (e.g., "500mg", "10ml")
     */
    @Column(length = 100)
    private String dosage;

    /**
     * Instructions for taking the medication (e.g., "Take with food")
     */
    @Column(length = 500)
    private String instructions;

    /**
     * Number of times per day the medication should be taken
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer timesPerDay = 1;

    /**
     * Whether this is a chronic/long-term medication
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isChronic = false;

    /**
     * Start date for this medication (null means start immediately with plan)
     */
    private java.time.LocalDate startDate;

    /**
     * End date for this medication (null means same as plan end date)
     */
    private java.time.LocalDate endDate;

    /**
     * Scheduled times for taking medication, stored as comma-separated values
     * e.g., "08:00,14:00,20:00"
     */
    @Column(length = 200)
    private String scheduledTimes;

    /**
     * Notes from healthcare provider
     */
    @Column(length = 500)
    private String notes;

    /**
     * Whether notifications are enabled for this medication
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean notificationsEnabled = true;

    /**
     * Minutes before scheduled time to send reminder
     */
    @Builder.Default
    private Integer reminderMinutesBefore = 15;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (timesPerDay == null) {
            timesPerDay = 1;
        }
        if (isChronic == null) {
            isChronic = false;
        }
        if (notificationsEnabled == null) {
            notificationsEnabled = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Get the scheduled times as a list of LocalTime objects.
     */
    @Transient
    public List<LocalTime> getScheduledTimesList() {
        List<LocalTime> times = new ArrayList<>();
        if (scheduledTimes != null && !scheduledTimes.isBlank()) {
            String[] parts = scheduledTimes.split(",");
            for (String part : parts) {
                try {
                    times.add(LocalTime.parse(part.trim()));
                } catch (Exception e) {
                    // Skip invalid times
                }
            }
        }
        return times;
    }

    /**
     * Set the scheduled times from a list of LocalTime objects.
     */
    public void setScheduledTimesList(List<LocalTime> times) {
        if (times == null || times.isEmpty()) {
            this.scheduledTimes = null;
        } else {
            this.scheduledTimes = times.stream()
                    .map(LocalTime::toString)
                    .reduce((a, b) -> a + "," + b)
                    .orElse(null);
        }
    }
}
