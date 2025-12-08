package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.*;
import com.projet.adhesionapp.assessment.repo.AnswerRepository;
import com.projet.adhesionapp.assessment.repo.TestDefinitionRepository;
import com.projet.adhesionapp.assessment.repo.TestSessionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import com.projet.adhesionapp.assessment.model.*;
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
        private final TestScoringService scoringService;
        private final UserService userService;

        @Transactional
        public TestSessionDto startSession(Long testId, User user) {
                TestDefinition test = testRepo.findWithQuestionsById(testId)
                                .orElseThrow(() -> new NotFoundException("Test introuvable"));

                TestSession session = TestSession.builder()
                                .testDefinition(test)
                                .user(user)
                                .startedAt(Instant.now())
                                .status(TestSessionStatus.IN_PROGRESS)
                                .build();

                TestSession saved = sessionRepo.save(session);
                return toDto(saved);
        }

        @Transactional
        public TestResultDto submitAnswers(Long sessionId, List<AnswerSubmissionDto> answers) {
                TestSession session = sessionRepo.findById(sessionId)
                                .orElseThrow(() -> new NotFoundException("Session introuvable"));

                // Save all answers
                answers.forEach(dto -> {
                        Answer answer = Answer.builder()
                                        .session(session)
                                        .question(new QuestionItem() {
                                                {
                                                        setId(dto.questionId());
                                                }
                                        })
                                        .score(dto.score())
                                        .value(dto.score()) // Set value for legacy column
                                        .textResponse(dto.textResponse())
                                        .build();
                        session.addAnswer(answer);
                });

                session.setSubmittedAt(Instant.now());
                session.setStatus(TestSessionStatus.COMPLETED);

                // Calculate score and interpretation
                int totalScore = scoringService.calculateTotalScore(session);
                String testCode = session.getTestDefinition().getCode();
                TestScoringService.TestInterpretation interpretation = scoringService.interpretScore(testCode,
                                totalScore);

                session.setTotalScore(totalScore);
                session.setInterpretationLevel(interpretation.level());
                session.setInterpretationDescription(interpretation.clinicalInterpretation());
                session.setStatus(TestSessionStatus.INTERPRETED);

                sessionRepo.save(session);

                // Increment user's completed tests count for onboarding
                userService.incrementTestsCompleted(session.getUser().getId());

                return new TestResultDto(
                                session.getId(),
                                testCode,
                                session.getTestDefinition().getTitle(),
                                totalScore,
                                interpretation.level(),
                                interpretation.scoreDescription(),
                                interpretation.clinicalInterpretation(),
                                interpretation.recommendations(),
                                session.getSubmittedAt());
        }

        @Transactional(readOnly = true)
        public List<TestResultDto> getPatientTestHistory(Long userId) {
                return sessionRepo.findByUserIdAndStatusOrderBySubmittedAtDesc(userId, TestSessionStatus.INTERPRETED)
                                .stream()
                                .map(this::toResultDto)
                                .toList();
        }

        @Transactional(readOnly = true)
        public TestResultDto getTestResult(Long sessionId) {
                TestSession session = sessionRepo.findById(sessionId)
                                .orElseThrow(() -> new NotFoundException("Session introuvable"));
                return toResultDto(session);
        }

        private TestResultDto toResultDto(TestSession session) {
                String testCode = session.getTestDefinition().getCode();
                TestScoringService.TestInterpretation interpretation = scoringService.interpretScore(testCode,
                                session.getTotalScore());

                return new TestResultDto(
                                session.getId(),
                                testCode,
                                session.getTestDefinition().getTitle(),
                                session.getTotalScore(),
                                session.getInterpretationLevel(),
                                interpretation.scoreDescription(),
                                session.getInterpretationDescription(),
                                interpretation.recommendations(),
                                session.getSubmittedAt());
        }

        private TestSessionDto toDto(TestSession s) {
                List<QuestionItemDto> q = s.getTestDefinition().getQuestions()
                                .stream()
                                .map(it -> new QuestionItemDto(it.getId(), it.getCode(), it.getText(),
                                                it.getMinScore(), it.getMaxScore(), it.getOrderIndex(),
                                                it.isReverseScored()))
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
