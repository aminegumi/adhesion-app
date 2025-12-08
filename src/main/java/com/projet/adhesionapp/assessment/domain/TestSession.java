package com.projet.adhesionapp.assessment.domain;

import com.projet.adhesionapp.identity.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private TestDefinition testDefinition;

    private Instant startedAt;
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    private TestSessionStatus status;

    private Integer totalScore;
    private String interpretationLevel;
    private String interpretationDescription;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();

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

    public void addAnswer(Answer answer) {
        answers.add(answer);
        answer.setSession(this);
    }
}
