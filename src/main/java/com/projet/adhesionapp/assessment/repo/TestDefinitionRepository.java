package com.projet.adhesionapp.assessment.repo;

import com.projet.adhesionapp.assessment.domain.TestDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface TestDefinitionRepository extends JpaRepository<TestDefinition, Long> {
    Optional<TestDefinition> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "questions")
    Optional<TestDefinition> findWithQuestionsById(Long id);

    @EntityGraph(attributePaths = "questions")
    Optional<TestDefinition> findWithQuestionsByCode(String code);
}
