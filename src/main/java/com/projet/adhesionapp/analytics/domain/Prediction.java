package com.projet.adhesionapp.analytics.domain;


import com.projet.adhesionapp.identity.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate date;

    /**
     * Probabilité de NON-adhésion (0.0 -> 1.0).
     */
    private double probNonAdherence;

    private String modelVersion;

    /**
     * Features importantes sérialisées (JSON simplifié).
     */
    @Column(length = 2000)
    private String topFeaturesJson;
}

