package com.projet.adhesionapp.identity.service;

import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.identity.domain.Consent;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.repo.ConsentRepository;
import com.projet.adhesionapp.identity.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Consent> getConsentsForUser(Long userId) {
        User user = loadUser(userId);
        return consentRepository.findByUser(user);
    }

    public Consent grantConsent(Long userId, String scope) {
        User user = loadUser(userId);

        Consent consent = consentRepository.findByUserAndScope(user, scope)
                .orElse(Consent.builder()
                        .user(user)
                        .scope(scope)
                        .build());

        consent.setGrantedAt(Instant.now());
        consent.setRevokedAt(null);

        return consentRepository.save(consent);
    }

    public Consent revokeConsent(Long userId, String scope) {
        User user = loadUser(userId);

        Consent consent = consentRepository.findByUserAndScope(user, scope)
                .orElseThrow(() -> new NotFoundException("Consentement non trouvé pour ce scope."));

        consent.setRevokedAt(Instant.now());
        return consentRepository.save(consent);
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
    }
}
