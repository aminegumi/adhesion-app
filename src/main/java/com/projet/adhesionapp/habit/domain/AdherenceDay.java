package com.projet.adhesionapp.habit.domain;

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
public class AdherenceDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate date;

    /**
     * Taux de complétion des actions du jour (0.0 -> 1.0).
     */
    private double completionRate;

    /**
     * Score d’adhésion du jour (par ex. 0.0 -> 1.0).
     */
    private double adherenceScore;
}
