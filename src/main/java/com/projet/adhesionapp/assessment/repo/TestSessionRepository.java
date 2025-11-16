package com.projet.adhesionapp.assessment.repo;

import com.projet.adhesionapp.assessment.domain.TestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    List<TestSession> findByUserId(Long userId);
}