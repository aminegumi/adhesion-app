package com.projet.adhesionapp.habit.web;

import com.projet.adhesionapp.habit.domain.AdherenceDay;
import com.projet.adhesionapp.habit.model.AdherenceSummaryDto;
import com.projet.adhesionapp.habit.service.AdherenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/adherence")
@RequiredArgsConstructor
public class AdherenceController {

    private final AdherenceService adherenceService;

    @PostMapping("/user/{userId}")
    public AdherenceSummaryDto saveForUser(
            @PathVariable Long userId,
            @RequestBody AdherenceSummaryDto dto
    ) {
        AdherenceDay saved = adherenceService.saveDailyAdherence(userId, dto);
        return new AdherenceSummaryDto(
                saved.getDate(),
                saved.getCompletionRate(),
                saved.getAdherenceScore()
        );
    }

    @GetMapping("/user/{userId}")
    public List<AdherenceSummaryDto> getHistory(
            @PathVariable Long userId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return adherenceService.getHistory(userId, from, to)
                .stream()
                .map(d -> new AdherenceSummaryDto(
                        d.getDate(),
                        d.getCompletionRate(),
                        d.getAdherenceScore()
                ))
                .toList();
    }
}
