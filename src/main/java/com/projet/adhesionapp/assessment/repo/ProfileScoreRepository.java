package com.projet.adhesionapp.assessment.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.projet.adhesionapp.assessment.domain.ProfileScore;

import java.util.List;

public interface ProfileScoreRepository extends JpaRepository<ProfileScore, Long> {

    List<ProfileScore> findBySessionId(Long sessionId);
}
