package com.projet.adhesionapp.assessment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TestSession session;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id")
    private QuestionItem question;

    /**
     * The raw score value selected by the patient
     */
    private int score;

    /**
     * Legacy column - mapped to score for backwards compatibility
     * This column exists in the database with NOT NULL constraint
     */
    @Column(name = "value", nullable = false)
    private Integer value;

    /**
     * Optional text response for open-ended questions
     */
    @Column(length = 1000)
    private String textResponse;

    /**
     * Ensure value is set from score before persisting
     */
    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.value == null) {
            this.value = this.score;
        }
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.value == null) {
            this.value = this.score;
        }
    }

    /**
     * Get the effective score (accounting for reverse scoring)
     */
    public int getEffectiveScore() {
        if (question != null && question.isReverseScored()) {
            return question.getMaxScore() - score + question.getMinScore();
        }
        return score;
    }
}