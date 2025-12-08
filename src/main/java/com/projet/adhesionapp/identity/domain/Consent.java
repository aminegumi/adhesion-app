package com.projet.adhesionapp.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Consentement de l'utilisateur pour un scope donné.
 * Diagramme : id, userId, scope, grantedAt, revokedAt.
 */
@Entity
@Table(name = "consents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Scope du consentement (ex: DATA, NOTIFICATION, ANALYTICS...).
     */
    @Column(nullable = false, length = 100)
    private String scope;

    @Column(nullable = false)
    private Instant grantedAt;

    @Column
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (grantedAt == null) {
            grantedAt = now;
        }
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
