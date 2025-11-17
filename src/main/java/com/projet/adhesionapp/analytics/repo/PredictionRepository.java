package com.projet.adhesionapp.analytics.repo;

import com.projet.adhesionapp.analytics.domain.Prediction;
import com.projet.adhesionapp.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findTop10ByUserOrderByDateDesc(User user);
}
