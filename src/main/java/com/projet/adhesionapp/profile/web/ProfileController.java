package com.projet.adhesionapp.profile.web;

import com.projet.adhesionapp.profile.domain.PsychologicalProfile;
import com.projet.adhesionapp.profile.model.ProfileCreateRequest;
import com.projet.adhesionapp.profile.model.ProfileDto;
import com.projet.adhesionapp.profile.service.PsychologicalProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final PsychologicalProfileService profileService;

    @PostMapping
    public ResponseEntity<ProfileDto> createProfile(@Valid @RequestBody ProfileCreateRequest request) {
        PsychologicalProfile profile = profileService.createProfile(request);
        return ResponseEntity.ok(profileService.toDto(profile));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable Long id) {
        PsychologicalProfile profile = profileService.getById(id);
        return ResponseEntity.ok(profileService.toDto(profile));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProfileDto>> getUserProfiles(@PathVariable Long userId) {
        List<ProfileDto> profiles = profileService.getUserProfiles(userId)
                .stream()
                .map(profileService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<ProfileDto> getLatestProfile(@PathVariable Long userId) {
        PsychologicalProfile profile = profileService.getLatestProfile(userId);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profileService.toDto(profile));
    }

    @GetMapping
    public ResponseEntity<List<ProfileDto>> getAllProfiles() {
        List<ProfileDto> profiles = profileService.getAllProfiles()
                .stream()
                .map(profileService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(profiles);
    }
}
