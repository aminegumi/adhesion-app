package com.projet.adhesionapp.assessment.domain;

import com.projet.adhesionapp.identity.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private TestDefinition testDefinition;

    private Instant startedAt;
    private Instant submittedAt;
}
