package com.projet.adhesionapp.assessment.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

 @Entity
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public class QuestionItem {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String code;
        private String text;
        private int orderIndex;
        private boolean reverseScored;
        private int minScore;
        private int maxScore;

        @ManyToOne
        @JoinColumn(name = "test_id")
        private TestDefinition testDefinition;
    }

