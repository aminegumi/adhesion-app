package com.projet.adhesionapp.habit.domain;

import com.projet.adhesionapp.identity.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate date;

    /**
     * Identifiant (technique) de l'action principale.
     * Pour la V1 on le garde comme simple id numérique,
     * même si plus tard il pourra pointer vers une entité DailyAction.
     */
    private Long primaryActionId;

    /**
     * Identifiant (technique) de l'action alternative.
     */
    private Long altActionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
