package com.projet.adhesionapp.identity.web;

import com.projet.adhesionapp.identity.domain.Consent;
import com.projet.adhesionapp.identity.service.ConsentService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API de gestion des consentements utilisateur.
 */
@RestController
@RequestMapping("/api/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @GetMapping("/user/{userId}")
    public List<Consent> getConsents(@PathVariable Long userId) {
        return consentService.getConsentsForUser(userId);
    }

    @PostMapping("/user/{userId}/grant")
    public Consent grant(@PathVariable Long userId,
                         @RequestBody GrantConsentRequest request) {
        return consentService.grantConsent(userId, request.getScope());
    }

    @PostMapping("/user/{userId}/revoke")
    public Consent revoke(@PathVariable Long userId,
                          @RequestBody GrantConsentRequest request) {
        return consentService.revokeConsent(userId, request.getScope());
    }

    @Data
    public static class GrantConsentRequest {
        @NotBlank
        private String scope;
    }
}

