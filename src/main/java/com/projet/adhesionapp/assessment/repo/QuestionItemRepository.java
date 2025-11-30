package com.projet.adhesionapp.assessment.repo;

import com.projet.adhesionapp.assessment.domain.QuestionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionItemRepository extends JpaRepository<QuestionItem, Long> {
    List<QuestionItem> findByTestDefinitionIdOrderByOrderIndexAsc(Long testId);
}
