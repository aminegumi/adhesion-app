package com.projet.adhesionapp.identity.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Utilisateur (patient) de l'application.
 * D'après le diagramme : id, email, passwordHash, displayName, birthDate, gender, active, createdAt.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 255)
    private String displayName;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 50)
    private String gender;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (active == null) {
            active = Boolean.TRUE;
        }
    }
}
