package com.projet.adhesionapp.recommendation.repo;

import com.projet.adhesionapp.recommendation.domain.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUserIdOrderByPriorityAsc(Long userId);

    List<Recommendation> findByUserIdAndStatus(Long userId, Recommendation.RecommendationStatus status);

    List<Recommendation> findByUserIdAndCategory(Long userId, String category);

    List<Recommendation> findByProfileId(Long profileId);

    List<Recommendation> findByUserIdAndCompletedFalseOrderByPriorityAsc(Long userId);
}
