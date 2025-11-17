package com.projet.adhesionapp.habit.service;


import com.projet.adhesionapp.common.exception.NotFoundException;
import com.projet.adhesionapp.habit.domain.AdherenceDay;
import com.projet.adhesionapp.habit.model.AdherenceSummaryDto;
import com.projet.adhesionapp.habit.repo.AdherenceDayRepository;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdherenceService {

    private final AdherenceDayRepository adherenceDayRepository;
    private final UserService userService;

    public AdherenceDay saveDailyAdherence(Long userId, AdherenceSummaryDto dto) {
        User user = userService.findById(userId);

        AdherenceDay entity = AdherenceDay.builder()
                .user(user)
                .date(dto.date())
                .completionRate(dto.completionRate())
                .adherenceScore(dto.adherenceScore())
                .build();

        return adherenceDayRepository.save(entity);
    }

    public List<AdherenceDay> getHistory(Long userId, LocalDate from, LocalDate to) {
        User user = userService.findById(userId);
        return adherenceDayRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, from, to);
    }

    public AdherenceDay getById(Long id) {
        return adherenceDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Enregistrement d'adhésion introuvable"));
    }
}
