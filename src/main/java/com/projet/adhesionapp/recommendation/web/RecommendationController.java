package com.projet.adhesionapp.recommendation.web;

import com.projet.adhesionapp.recommendation.domain.Recommendation;
import com.projet.adhesionapp.recommendation.model.RecommendationDto;
import com.projet.adhesionapp.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping("/generate/{userId}")
    public ResponseEntity<List<RecommendationDto>> generateRecommendations(@PathVariable Long userId) {
        List<Recommendation> recommendations = recommendationService.generateRecommendations(userId);
        return ResponseEntity.ok(recommendations.stream()
                .map(recommendationService::toDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecommendationDto>> getUserRecommendations(@PathVariable Long userId) {
        return ResponseEntity.ok(recommendationService.getUserRecommendations(userId).stream()
                .map(recommendationService::toDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<RecommendationDto>> getActiveRecommendations(@PathVariable Long userId) {
        return ResponseEntity.ok(recommendationService.getActiveRecommendations(userId).stream()
                .map(recommendationService::toDto)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<RecommendationDto> completeRecommendation(
            @PathVariable Long id,
            @RequestParam(required = false) String feedback) {
        Recommendation rec = recommendationService.completeRecommendation(id, feedback);
        return ResponseEntity.ok(recommendationService.toDto(rec));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<RecommendationDto> dismissRecommendation(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Recommendation rec = recommendationService.dismissRecommendation(id, reason);
        return ResponseEntity.ok(recommendationService.toDto(rec));
    }
}
