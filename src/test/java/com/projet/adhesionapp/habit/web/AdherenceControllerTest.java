package com.projet.adhesionapp.habit.web;

import com.projet.adhesionapp.habit.domain.AdherenceDay;
import com.projet.adhesionapp.habit.model.AdherenceSummaryDto;
import com.projet.adhesionapp.habit.service.AdherenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdherenceControllerTest {

    @Mock
    private AdherenceService adherenceService;

    @InjectMocks
    private AdherenceController adherenceController;

    private AdherenceDay testAdherenceDay;
    private AdherenceSummaryDto testSummaryDto;

    @BeforeEach
    void setUp() {
        testAdherenceDay = new AdherenceDay();
        testAdherenceDay.setDate(LocalDate.of(2025, 1, 15));
        testAdherenceDay.setCompletionRate(0.85);
        testAdherenceDay.setAdherenceScore(0.90);

        testSummaryDto = new AdherenceSummaryDto(
                LocalDate.of(2025, 1, 15),
                0.85,
                0.90
        );
    }

    // ==================== Save Daily Adherence Tests ====================

    @Test
    void saveForUser_ShouldSaveAndReturnSummary() {
        // Given
        when(adherenceService.saveDailyAdherence(eq(1L), any(AdherenceSummaryDto.class)))
                .thenReturn(testAdherenceDay);

        // When
        AdherenceSummaryDto result = adherenceController.saveForUser(1L, testSummaryDto);

        // Then
        assertNotNull(result);
        assertEquals(LocalDate.of(2025, 1, 15), result.date());
        assertEquals(0.85, result.completionRate(), 0.01);
        assertEquals(0.90, result.adherenceScore(), 0.01);
        verify(adherenceService).saveDailyAdherence(1L, testSummaryDto);
    }

    @Test
    void saveForUser_ShouldPassUserIdAndDtoToService() {
        // Given
        AdherenceSummaryDto inputDto = new AdherenceSummaryDto(LocalDate.now(), 0.75, 0.80);
        AdherenceDay savedDay = new AdherenceDay();
        savedDay.setDate(LocalDate.now());
        savedDay.setCompletionRate(0.75);
        savedDay.setAdherenceScore(0.80);
        when(adherenceService.saveDailyAdherence(eq(5L), any())).thenReturn(savedDay);

        // When
        AdherenceSummaryDto result = adherenceController.saveForUser(5L, inputDto);

        // Then
        assertEquals(0.75, result.completionRate(), 0.01);
        verify(adherenceService).saveDailyAdherence(5L, inputDto);
    }

    @Test
    void saveForUser_WithPerfectScore_ShouldReturnFullValues() {
        // Given
        AdherenceSummaryDto perfectDto = new AdherenceSummaryDto(LocalDate.now(), 1.0, 1.0);
        AdherenceDay perfectDay = new AdherenceDay();
        perfectDay.setDate(LocalDate.now());
        perfectDay.setCompletionRate(1.0);
        perfectDay.setAdherenceScore(1.0);
        when(adherenceService.saveDailyAdherence(anyLong(), any())).thenReturn(perfectDay);

        // When
        AdherenceSummaryDto result = adherenceController.saveForUser(1L, perfectDto);

        // Then
        assertEquals(1.0, result.completionRate(), 0.01);
        assertEquals(1.0, result.adherenceScore(), 0.01);
    }

    @Test
    void saveForUser_WithZeroScore_ShouldReturnZeroValues() {
        // Given
        AdherenceSummaryDto zeroDto = new AdherenceSummaryDto(LocalDate.now(), 0.0, 0.0);
        AdherenceDay zeroDay = new AdherenceDay();
        zeroDay.setDate(LocalDate.now());
        zeroDay.setCompletionRate(0.0);
        zeroDay.setAdherenceScore(0.0);
        when(adherenceService.saveDailyAdherence(anyLong(), any())).thenReturn(zeroDay);

        // When
        AdherenceSummaryDto result = adherenceController.saveForUser(1L, zeroDto);

        // Then
        assertEquals(0.0, result.completionRate(), 0.01);
        assertEquals(0.0, result.adherenceScore(), 0.01);
    }

    // ==================== Get History Tests ====================

    @Test
    void getHistory_ShouldReturnListOfAdherenceSummaries() {
        // Given
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        AdherenceDay day1 = new AdherenceDay();
        day1.setDate(LocalDate.of(2025, 1, 10));
        day1.setCompletionRate(0.8);
        day1.setAdherenceScore(0.85);

        AdherenceDay day2 = new AdherenceDay();
        day2.setDate(LocalDate.of(2025, 1, 11));
        day2.setCompletionRate(0.9);
        day2.setAdherenceScore(0.95);

        when(adherenceService.getHistory(1L, from, to)).thenReturn(List.of(day1, day2));

        // When
        List<AdherenceSummaryDto> result = adherenceController.getHistory(1L, from, to);

        // Then
        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2025, 1, 10), result.get(0).date());
        assertEquals(0.8, result.get(0).completionRate(), 0.01);
        assertEquals(LocalDate.of(2025, 1, 11), result.get(1).date());
        assertEquals(0.9, result.get(1).completionRate(), 0.01);
    }

    @Test
    void getHistory_WithNoData_ShouldReturnEmptyList() {
        // Given
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);
        when(adherenceService.getHistory(99L, from, to)).thenReturn(List.of());

        // When
        List<AdherenceSummaryDto> result = adherenceController.getHistory(99L, from, to);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getHistory_ShouldMapAllFieldsCorrectly() {
        // Given
        LocalDate from = LocalDate.of(2025, 6, 1);
        LocalDate to = LocalDate.of(2025, 6, 30);

        AdherenceDay day = new AdherenceDay();
        day.setDate(LocalDate.of(2025, 6, 15));
        day.setCompletionRate(0.77);
        day.setAdherenceScore(0.82);

        when(adherenceService.getHistory(5L, from, to)).thenReturn(List.of(day));

        // When
        List<AdherenceSummaryDto> result = adherenceController.getHistory(5L, from, to);

        // Then
        assertEquals(1, result.size());
        AdherenceSummaryDto dto = result.get(0);
        assertEquals(LocalDate.of(2025, 6, 15), dto.date());
        assertEquals(0.77, dto.completionRate(), 0.01);
        assertEquals(0.82, dto.adherenceScore(), 0.01);
    }

    @Test
    void getHistory_ShouldPassCorrectDateRangeToService() {
        // Given
        LocalDate from = LocalDate.of(2025, 3, 1);
        LocalDate to = LocalDate.of(2025, 3, 15);
        when(adherenceService.getHistory(1L, from, to)).thenReturn(List.of());

        // When
        adherenceController.getHistory(1L, from, to);

        // Then
        verify(adherenceService).getHistory(1L, from, to);
    }

    @Test
    void getHistory_SameFromAndToDate_ShouldWork() {
        // Given
        LocalDate singleDate = LocalDate.of(2025, 5, 20);
        AdherenceDay singleDay = new AdherenceDay();
        singleDay.setDate(singleDate);
        singleDay.setCompletionRate(0.5);
        singleDay.setAdherenceScore(0.6);

        when(adherenceService.getHistory(1L, singleDate, singleDate)).thenReturn(List.of(singleDay));

        // When
        List<AdherenceSummaryDto> result = adherenceController.getHistory(1L, singleDate, singleDate);

        // Then
        assertEquals(1, result.size());
        assertEquals(singleDate, result.get(0).date());
    }
}
