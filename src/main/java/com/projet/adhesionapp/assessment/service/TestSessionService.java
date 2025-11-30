package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.Answer;
import com.projet.adhesionapp.assessment.domain.TestDefinition;
import com.projet.adhesionapp.assessment.domain.TestSession;
import com.projet.adhesionapp.assessment.repo.AnswerRepository;
import com.projet.adhesionapp.assessment.repo.TestDefinitionRepository;
import com.projet.adhesionapp.assessment.repo.TestSessionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.assessment.model.TestSessionDto;
import com.projet.adhesionapp.assessment.model.QuestionItemDto;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestSessionService {

    private final TestSessionRepository sessionRepo;
    private final TestDefinitionRepository testRepo;
    private final AnswerRepository answerRepo;

    @Transactional
    public TestSessionDto startSession(Long testId, User user) {
        TestDefinition test = testRepo.findWithQuestionsById(testId)
                .orElseThrow(() -> new NotFoundException("Test introuvable"));

        TestSession session = TestSession.builder()
                .testDefinition(test)
                .user(user)
                .startedAt(Instant.now())
                .build();

        TestSession saved = sessionRepo.save(session);
        return toDto(saved); // questions are loaded within the transaction
    }

    public void submitAnswers(Long sessionId, List<Answer> answers) {
        TestSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session introuvable"));

        answers.forEach(a -> a.setSession(session));

        answerRepo.saveAll(answers);

        session.setSubmittedAt(Instant.now());
        sessionRepo.save(session);
    }

    private TestSessionDto toDto(TestSession s) {
        List<QuestionItemDto> q = s.getTestDefinition().getQuestions()
                .stream()
                .map(it -> new QuestionItemDto(it.getId(), it.getCode(), it.getText()))
                .toList();
        TestDefinition td = s.getTestDefinition();
        return new TestSessionDto(
                s.getId(),
                td.getId(),
                td.getCode(),
                td.getTitle(),
                s.getStartedAt(),
                q);
    }
}
