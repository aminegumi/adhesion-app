package com.projet.adhesionapp.habit.web;

import com.projet.adhesionapp.habit.domain.ActionPlan;
import com.projet.adhesionapp.habit.model.PlanDto;
import com.projet.adhesionapp.habit.service.ActionPlanService;
import com.projet.adhesionapp.identity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionPlanControllerTest {

    @Mock
    private ActionPlanService actionPlanService;

    @InjectMocks
    private ActionPlanController actionPlanController;

    private User testUser;
    private ActionPlan testPlan;
    private PlanDto testPlanDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        testPlan = new ActionPlan();
        testPlan.setId(10L);
        testPlan.setUser(testUser);
        testPlan.setDate(LocalDate.of(2025, 1, 15));
        testPlan.setPrimaryActionId(100L);
        testPlan.setAltActionId(200L);

        testPlanDto = new PlanDto(
                null,
                1L,
                LocalDate.of(2025, 1, 15),
                100L,
                200L
        );
    }

    // ==================== Create Plan Tests ====================

    @Test
    void create_ShouldCreateAndReturnPlanDto() {
        // Given
        when(actionPlanService.createPlan(any(PlanDto.class))).thenReturn(testPlan);

        // When
        PlanDto result = actionPlanController.create(testPlanDto);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals(1L, result.userId());
        assertEquals(LocalDate.of(2025, 1, 15), result.date());
        assertEquals(100L, result.primaryActionId());
        assertEquals(200L, result.altActionId());
        verify(actionPlanService).createPlan(testPlanDto);
    }

    @Test
    void create_ShouldPassDtoToService() {
        // Given
        PlanDto inputDto = new PlanDto(null, 2L, LocalDate.now(), 50L, 75L);
        ActionPlan savedPlan = new ActionPlan();
        savedPlan.setId(20L);
        User user = new User();
        user.setId(2L);
        savedPlan.setUser(user);
        savedPlan.setDate(LocalDate.now());
        savedPlan.setPrimaryActionId(50L);
        savedPlan.setAltActionId(75L);
        when(actionPlanService.createPlan(inputDto)).thenReturn(savedPlan);

        // When
        PlanDto result = actionPlanController.create(inputDto);

        // Then
        assertEquals(20L, result.id());
        assertEquals(2L, result.userId());
    }

    @Test
    void create_WithNullAltAction_ShouldHandleCorrectly() {
        // Given
        PlanDto dtoWithoutAlt = new PlanDto(null, 1L, LocalDate.now(), 100L, null);
        ActionPlan planWithoutAlt = new ActionPlan();
        planWithoutAlt.setId(30L);
        planWithoutAlt.setUser(testUser);
        planWithoutAlt.setDate(LocalDate.now());
        planWithoutAlt.setPrimaryActionId(100L);
        planWithoutAlt.setAltActionId(null);
        when(actionPlanService.createPlan(dtoWithoutAlt)).thenReturn(planWithoutAlt);

        // When
        PlanDto result = actionPlanController.create(dtoWithoutAlt);

        // Then
        assertNull(result.altActionId());
        assertEquals(100L, result.primaryActionId());
    }

    // ==================== Get Plans For User Tests ====================

    @Test
    void getForUser_ShouldReturnListOfPlans() {
        // Given
        ActionPlan plan1 = new ActionPlan();
        plan1.setId(1L);
        plan1.setUser(testUser);
        plan1.setDate(LocalDate.of(2025, 1, 10));
        plan1.setPrimaryActionId(10L);
        plan1.setAltActionId(20L);

        ActionPlan plan2 = new ActionPlan();
        plan2.setId(2L);
        plan2.setUser(testUser);
        plan2.setDate(LocalDate.of(2025, 1, 11));
        plan2.setPrimaryActionId(30L);
        plan2.setAltActionId(40L);

        when(actionPlanService.getPlansForUser(1L)).thenReturn(List.of(plan1, plan2));

        // When
        List<PlanDto> result = actionPlanController.getForUser(1L);

        // Then
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(2L, result.get(1).id());
        verify(actionPlanService).getPlansForUser(1L);
    }

    @Test
    void getForUser_WithNoPlans_ShouldReturnEmptyList() {
        // Given
        when(actionPlanService.getPlansForUser(99L)).thenReturn(List.of());

        // When
        List<PlanDto> result = actionPlanController.getForUser(99L);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getForUser_ShouldMapAllFieldsCorrectly() {
        // Given
        LocalDate planDate = LocalDate.of(2025, 6, 15);
        ActionPlan plan = new ActionPlan();
        plan.setId(100L);
        User user = new User();
        user.setId(5L);
        plan.setUser(user);
        plan.setDate(planDate);
        plan.setPrimaryActionId(500L);
        plan.setAltActionId(600L);

        when(actionPlanService.getPlansForUser(5L)).thenReturn(List.of(plan));

        // When
        List<PlanDto> result = actionPlanController.getForUser(5L);

        // Then
        assertEquals(1, result.size());
        PlanDto dto = result.get(0);
        assertEquals(100L, dto.id());
        assertEquals(5L, dto.userId());
        assertEquals(planDate, dto.date());
        assertEquals(500L, dto.primaryActionId());
        assertEquals(600L, dto.altActionId());
    }

    @Test
    void getForUser_MultipleUsers_ShouldOnlyReturnRequestedUserPlans() {
        // Given
        ActionPlan userOnePlan = new ActionPlan();
        userOnePlan.setId(1L);
        userOnePlan.setUser(testUser);
        userOnePlan.setDate(LocalDate.now());
        userOnePlan.setPrimaryActionId(10L);

        when(actionPlanService.getPlansForUser(1L)).thenReturn(List.of(userOnePlan));
        when(actionPlanService.getPlansForUser(2L)).thenReturn(List.of());

        // When
        List<PlanDto> userOneResult = actionPlanController.getForUser(1L);
        List<PlanDto> userTwoResult = actionPlanController.getForUser(2L);

        // Then
        assertEquals(1, userOneResult.size());
        assertTrue(userTwoResult.isEmpty());
    }
}
