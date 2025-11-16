package com.projet.adhesionapp.identity.repo;


import com.projet.adhesionapp.identity.domain.Consent;
import com.projet.adhesionapp.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    List<Consent> findByUser(User user);

    Optional<Consent> findByUserAndScope(User user, String scope);
}

