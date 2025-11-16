package com.projet.adhesionapp.assessment.domain;

import  jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private TestSession session;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private QuestionItem item;

    private int value;
}