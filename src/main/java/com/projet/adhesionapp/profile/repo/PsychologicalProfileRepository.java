package com.projet.adhesionapp.profile.repo;

import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PsychologicalProfileRepository extends JpaRepository<PsychologicalProfile, Long> {

    List<PsychologicalProfile> findByUserOrderByCreatedAtDesc(User user);

    Optional<PsychologicalProfile> findFirstByUserOrderByCreatedAtDesc(User user);

    List<PsychologicalProfile> findByUserId(Long userId);

    List<PsychologicalProfile> findByProfileType(String profileType);

    List<PsychologicalProfile> findByStatus(PsychologicalProfile.ProfileStatus status);
}
