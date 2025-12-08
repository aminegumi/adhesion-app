package com.projet.adhesionapp.assessment.repo;

import com.projet.adhesionapp.assessment.domain.TestSession;
import com.projet.adhesionapp.assessment.domain.TestSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    List<TestSession> findByUserId(Long userId);

    List<TestSession> findByUserIdAndStatusOrderBySubmittedAtDesc(Long userId, TestSessionStatus status);

    @Query("SELECT ts FROM TestSession ts WHERE ts.user.id = :userId AND ts.testDefinition.code = :testCode AND ts.status = :status ORDER BY ts.submittedAt DESC")
    List<TestSession> findByUserIdAndTestCodeAndStatus(Long userId, String testCode, TestSessionStatus status);

    @Query("SELECT ts FROM TestSession ts WHERE ts.user.id = :userId ORDER BY ts.submittedAt DESC")
    List<TestSession> findAllByUserIdOrderBySubmittedAtDesc(Long userId);
}