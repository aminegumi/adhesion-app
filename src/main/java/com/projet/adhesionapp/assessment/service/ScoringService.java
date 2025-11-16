package com.projet.adhesionapp.assessment.service;


import com.projet.adhesionapp.assessment.domain.Answer;
import com.projet.adhesionapp.assessment.domain.ProfileScore;
import com.projet.adhesionapp.assessment.repo.AnswerRepository;
import com.projet.adhesionapp.assessment.repo.ProfileScoreRepository;
import lombok.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoringService {

    private final ProfileScoreRepository scoreRepo;
    private final AnswerRepository answerRepo;

    public ProfileScore computeScore(Long sessionId, String dimension) {

        List<Answer> answers = answerRepo.findAll()
                .stream()
                .filter(a -> a.getSession().getId().equals(sessionId))
                .toList();

        double avg = answers.stream()
                .mapToInt(Answer::getValue)
                .average()
                .orElse(0);

        ProfileScore score = ProfileScore.builder()
                .dimension(dimension)
                .score(avg)
                .computedAt(Instant.now())
                .build();

        return scoreRepo.save(score);
    }
}
