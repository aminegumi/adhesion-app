package com.projet.adhesionapp.assessment.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.projet.adhesionapp.assessment.domain.ProfileScore;

public interface ProfileScoreRepository extends JpaRepository<ProfileScore, Long> {}

