package com.projet.adhesionapp.treatment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * UserMedication - A medication prescribed to a user.
 * This is the primary entity for tracking what medications a user takes.
 * 
 * The daily schedule (DoseLog) is auto-generated from active medications.
 */
@Entity
@Table(name = "user_medications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private com.projet.adhesionapp.identity.domain.User user;

    /**
     * Name of the medication (e.g., "Metformin", "Doliprane")
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Dosage per intake (e.g., "500mg", "10ml", "1 tablet")
     */
    @Column(length = 100)
    private String dosage;

    /**
     * Form of the medication
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MedicationForm form = MedicationForm.TABLET;

    /**
     * Number of times per day the medication should be taken
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer frequencyPerDay = 1;

    /**
     * Scheduled times for taking medication, stored as comma-separated values
     * e.g., "08:00,14:00,20:00"
     * If null/empty, times are auto-generated based on frequencyPerDay
     */
    @Column(length = 200)
    private String scheduledTimes;

    /**
     * Prescribing doctor's name
     */
    @Column(length = 200)
    private String prescribedBy;

    /**
     * Instructions for taking the medication (e.g., "Take with food", "Before
     * sleep")
     */
    @Column(length = 500)
    private String instructions;

    /**
     * Start date for this medication
     */
    private LocalDate startDate;

    /**
     * End date for this medication (null means ongoing/chronic)
     */
    private LocalDate endDate;

    /**
     * Whether this is a chronic/long-term medication
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isChronic = false;

    /**
     * Current stock count (number of pills/doses remaining)
     */
    private Integer currentStock;

    /**
     * Alert when stock falls below this number
     */
    private Integer lowStockThreshold;

    /**
     * Whether this medication is currently active
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Whether notifications/reminders are enabled
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean remindersEnabled = true;

    /**
     * Minutes before scheduled time to send reminder
     */
    @Builder.Default
    private Integer reminderMinutesBefore = 15;

    /**
     * Optional notes
     */
    @Column(length = 1000)
    private String notes;

    /**
     * Reason for taking this medication (optional)
     */
    @Column(length = 500)
    private String reason;

    /**
     * Color for UI display (hex code)
     */
    @Column(length = 10)
    private String color;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (frequencyPerDay == null) {
            frequencyPerDay = 1;
        }
        if (active == null) {
            active = true;
        }
        if (remindersEnabled == null) {
            remindersEnabled = true;
        }
        if (isChronic == null) {
            isChronic = false;
        }

        // Validate scheduled times match frequency
        validateScheduledTimes();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        validateScheduledTimes();
    }

    /**
     * Ensure scheduled times count matches frequency per day.
     * If user enters more times than frequency, truncate.
     * If fewer, the dose service will auto-generate remaining times.
     */
    private void validateScheduledTimes() {
        if (scheduledTimes != null && !scheduledTimes.isBlank() && frequencyPerDay != null) {
            List<LocalTime> times = getScheduledTimesList();
            if (times.size() > frequencyPerDay) {
                // Truncate to match frequency
                setScheduledTimesList(times.subList(0, frequencyPerDay));
            }
        }
    }

    /**
     * Check if this medication is currently active (within date range and not
     * deactivated)
     */
    @Transient
    public boolean isCurrentlyActive() {
        if (!Boolean.TRUE.equals(active))
            return false;

        LocalDate today = LocalDate.now();
        if (startDate != null && today.isBefore(startDate))
            return false;
        if (endDate != null && today.isAfter(endDate))
            return false;

        return true;
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

    /**
     * Get default times based on frequency if none specified.
     */
    @Transient
    public List<LocalTime> getEffectiveScheduledTimes() {
        List<LocalTime> specified = getScheduledTimesList();
        if (!specified.isEmpty()) {
            return specified;
        }

        // Generate default times based on frequency
        return generateDefaultTimes(frequencyPerDay);
    }

    private List<LocalTime> generateDefaultTimes(int frequency) {
        return switch (frequency) {
            case 1 -> List.of(LocalTime.of(8, 0));
            case 2 -> List.of(LocalTime.of(8, 0), LocalTime.of(20, 0));
            case 3 -> List.of(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0));
            case 4 -> List.of(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(17, 0), LocalTime.of(21, 0));
            default -> {
                List<LocalTime> times = new ArrayList<>();
                int interval = 24 / Math.max(1, frequency);
                for (int i = 0; i < frequency; i++) {
                    times.add(LocalTime.of((8 + i * interval) % 24, 0));
                }
                yield times;
            }
        };
    }

    /**
     * Medication form types
     */
    public enum MedicationForm {
        TABLET,
        CAPSULE,
        LIQUID,
        INJECTION,
        CREAM,
        DROPS,
        INHALER,
        PATCH,
        SUPPOSITORY,
        OTHER
    }
}
