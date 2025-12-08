package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.QuestionItem;
import com.projet.adhesionapp.assessment.domain.TestDefinition;
import com.projet.adhesionapp.assessment.model.CreateQuestionRequest;
import com.projet.adhesionapp.assessment.model.UpdateQuestionRequest;
import com.projet.adhesionapp.assessment.model.QuestionItemDto;
import com.projet.adhesionapp.assessment.repo.QuestionItemRepository;
import com.projet.adhesionapp.assessment.repo.TestDefinitionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionItemRepository repo;
    private final TestDefinitionRepository testRepo;

    public List<QuestionItemDto> getByTestId(Long testId) {
        return repo.findByTestDefinitionIdOrderByOrderIndexAsc(testId)
                .stream().map(this::toDto).toList();
    }

    public QuestionItemDto getById(Long id) {
        return toDto(repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Question id " + id + " introuvable")));
    }

    @Transactional
    public QuestionItemDto create(CreateQuestionRequest req) {
        TestDefinition test = testRepo.findById(req.testId())
                .orElseThrow(() -> new NotFoundException("Test id " + req.testId() + " introuvable"));

        QuestionItem q = QuestionItem.builder()
                .testDefinition(test)
                .code(req.code())
                .text(req.text())
                .orderIndex(req.orderIndex())
                .reverseScored(req.reverseScored())
                .minScore(req.minScore())
                .maxScore(req.maxScore())
                .build();

        return toDto(repo.save(q));
    }

    @Transactional
    public QuestionItemDto update(Long id, UpdateQuestionRequest req) {
        QuestionItem q = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Question id " + id + " introuvable"));

        q.setText(req.text());
        q.setOrderIndex(req.orderIndex());
        q.setReverseScored(req.reverseScored());
        q.setMinScore(req.minScore());
        q.setMaxScore(req.maxScore());

        return toDto(q); // dirty checking
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Question id " + id + " introuvable");
        }
        repo.deleteById(id);
    }

    private QuestionItemDto toDto(QuestionItem q) {
        return new QuestionItemDto(q.getId(), q.getCode(), q.getText(),
                q.getMinScore(), q.getMaxScore(), q.getOrderIndex(), q.isReverseScored());
    }
}