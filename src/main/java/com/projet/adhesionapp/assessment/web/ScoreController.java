package com.projet.adhesionapp.assessment.web;


import com.projet.adhesionapp.assessment.domain.ProfileScore;
import com.projet.adhesionapp.assessment.service.ScoringService;
import lombok.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoringService scoringService;

    @PostMapping("/{sessionId}/compute/{dimension}")
    public ProfileScore compute(
            @PathVariable Long sessionId,
            @PathVariable String dimension) {
        return scoringService.computeScore(sessionId, dimension);
    }
}
