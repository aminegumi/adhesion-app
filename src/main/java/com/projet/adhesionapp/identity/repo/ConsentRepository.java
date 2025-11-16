package com.projet.adhesionapp.identity.repo;


import com.projet.adhesionapp.identity.domain.Consent;
import com.projet.adhesionapp.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentRepository extends JpaRepository<Consent, Long> {

    List<Consent> findByUser(User user);

    Optional<Consent> findByUserAndScope(User user, String scope);
}

