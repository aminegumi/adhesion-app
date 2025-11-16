package com.projet.adhesionapp.assessment.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String title;
    private String version;
    private boolean active;

    @OneToMany(mappedBy = "testDefinition", cascade = CascadeType.ALL)
    private List<QuestionItem> questions = new ArrayList<>();
}
