package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.QuestionItem;
import com.projet.adhesionapp.assessment.domain.TestDefinition;
import com.projet.adhesionapp.assessment.repo.TestDefinitionRepository;
import com.projet.adhesionapp.assessment.repo.TestSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PsychologicalTestDataInitializerTest {

    @Mock
    private TestDefinitionRepository testDefinitionRepository;

    @Mock
    private TestSessionRepository testSessionRepository;

    @InjectMocks
    private PsychologicalTestDataInitializer initializer;

    private TestDefinition mmasTest;
    private TestDefinition phqTest;

    @BeforeEach
    void setUp() {
        // Create mock test with questions
        mmasTest = TestDefinition.builder()
                .id(1L)
                .code("MMAS-8")
                .title("Morisky Medication Adherence Scale")
                .version("1.0")
                .active(true)
                .questions(createMockQuestions())
                .build();

        phqTest = TestDefinition.builder()
                .id(2L)
                .code("PHQ-9")
                .title("Patient Health Questionnaire")
                .version("1.0")
                .active(true)
                .questions(createMockQuestions())
                .build();
    }

    private List<QuestionItem> createMockQuestions() {
        List<QuestionItem> questions = new ArrayList<>();
        QuestionItem q1 = new QuestionItem();
        q1.setId(1L);
        q1.setCode("Q1");
        q1.setText("Question 1");
        questions.add(q1);
        return questions;
    }

    // ==================== Initialization Tests ====================

    @Test
    void initializeTests_WhenAllTestsExist_ShouldNotReinitialize() {
        // Given - All required tests exist with questions
        when(testDefinitionRepository.findWithQuestionsByCode("MMAS-8")).thenReturn(Optional.of(mmasTest));
        when(testDefinitionRepository.findWithQuestionsByCode("PHQ-9")).thenReturn(Optional.of(phqTest));
        
        TestDefinition gadTest = TestDefinition.builder()
                .code("GAD-7")
                .questions(createMockQuestions())
                .build();
        when(testDefinitionRepository.findWithQuestionsByCode("GAD-7")).thenReturn(Optional.of(gadTest));
        
        TestDefinition bmqTest = TestDefinition.builder()
                .code("BMQ")
                .questions(createMockQuestions())
                .build();
        when(testDefinitionRepository.findWithQuestionsByCode("BMQ")).thenReturn(Optional.of(bmqTest));

        // When
        initializer.initializeTests();

        // Then - Should not delete or save anything
        verify(testDefinitionRepository, never()).deleteAll();
        verify(testDefinitionRepository, never()).save(any(TestDefinition.class));
    }

    @Test
    void initializeTests_WhenTestMissing_ShouldReinitialize() {
        // Given - MMAS-8 is missing
        when(testDefinitionRepository.findWithQuestionsByCode("MMAS-8")).thenReturn(Optional.empty());

        // When
        initializer.initializeTests();

        // Then - Should delete all and recreate
        verify(testSessionRepository).deleteAll();
        verify(testDefinitionRepository).deleteAll();
        verify(testDefinitionRepository, times(4)).save(any(TestDefinition.class));
    }

    @Test
    void initializeTests_WhenTestHasNoQuestions_ShouldReinitialize() {
        // Given - MMAS-8 exists but has no questions
        TestDefinition emptyTest = TestDefinition.builder()
                .code("MMAS-8")
                .questions(new ArrayList<>())
                .build();
        when(testDefinitionRepository.findWithQuestionsByCode("MMAS-8")).thenReturn(Optional.of(emptyTest));

        // When
        initializer.initializeTests();

        // Then - Should reinitialize
        verify(testSessionRepository).deleteAll();
        verify(testDefinitionRepository).deleteAll();
    }

    @Test
    void initializeTests_WhenTestHasNullQuestions_ShouldReinitialize() {
        // Given - Test with null questions
        TestDefinition nullQuestionsTest = TestDefinition.builder()
                .code("MMAS-8")
                .questions(null)
                .build();
        when(testDefinitionRepository.findWithQuestionsByCode("MMAS-8")).thenReturn(Optional.of(nullQuestionsTest));

        // When
        initializer.initializeTests();

        // Then - Should reinitialize
        verify(testSessionRepository).deleteAll();
        verify(testDefinitionRepository).deleteAll();
    }

    @Test
    void initializeTests_ShouldCreateFourTests() {
        // Given - No tests exist
        when(testDefinitionRepository.findWithQuestionsByCode(anyString())).thenReturn(Optional.empty());

        // When
        initializer.initializeTests();

        // Then - Should create exactly 4 tests
        ArgumentCaptor<TestDefinition> testCaptor = ArgumentCaptor.forClass(TestDefinition.class);
        verify(testDefinitionRepository, times(4)).save(testCaptor.capture());

        List<TestDefinition> savedTests = testCaptor.getAllValues();
        assertEquals(4, savedTests.size());

        // Verify test codes
        List<String> codes = savedTests.stream().map(TestDefinition::getCode).toList();
        assertTrue(codes.contains("MMAS-8"));
        assertTrue(codes.contains("PHQ-9"));
        assertTrue(codes.contains("GAD-7"));
        assertTrue(codes.contains("BMQ"));
    }

    @Test
    void initializeTests_MMAS8_ShouldHave8Questions() {
        // Given
        when(testDefinitionRepository.findWithQuestionsByCode(anyString())).thenReturn(Optional.empty());

        // When
        initializer.initializeTests();

        // Then - Verify MMAS-8 has correct structure
        ArgumentCaptor<TestDefinition> testCaptor = ArgumentCaptor.forClass(TestDefinition.class);
        verify(testDefinitionRepository, times(4)).save(testCaptor.capture());

        TestDefinition mmas = testCaptor.getAllValues().stream()
                .filter(t -> "MMAS-8".equals(t.getCode()))
                .findFirst()
                .orElseThrow();

        assertEquals(8, mmas.getQuestions().size());
        assertEquals("Morisky Medication Adherence Scale", mmas.getTitle());
        assertTrue(mmas.isActive());
    }

    @Test
    void initializeTests_PHQ9_ShouldHave9Questions() {
        // Given
        when(testDefinitionRepository.findWithQuestionsByCode(anyString())).thenReturn(Optional.empty());

        // When
        initializer.initializeTests();

        // Then
        ArgumentCaptor<TestDefinition> testCaptor = ArgumentCaptor.forClass(TestDefinition.class);
        verify(testDefinitionRepository, times(4)).save(testCaptor.capture());

        TestDefinition phq = testCaptor.getAllValues().stream()
                .filter(t -> "PHQ-9".equals(t.getCode()))
                .findFirst()
                .orElseThrow();

        assertEquals(9, phq.getQuestions().size());
        assertEquals("Patient Health Questionnaire - Depression", phq.getTitle());
    }

    @Test
    void initializeTests_GAD7_ShouldHave7Questions() {
        // Given
        when(testDefinitionRepository.findWithQuestionsByCode(anyString())).thenReturn(Optional.empty());

        // When
        initializer.initializeTests();

        // Then
        ArgumentCaptor<TestDefinition> testCaptor = ArgumentCaptor.forClass(TestDefinition.class);
        verify(testDefinitionRepository, times(4)).save(testCaptor.capture());

        TestDefinition gad = testCaptor.getAllValues().stream()
                .filter(t -> "GAD-7".equals(t.getCode()))
                .findFirst()
                .orElseThrow();

        assertEquals(7, gad.getQuestions().size());
        assertEquals("Generalized Anxiety Disorder Scale", gad.getTitle());
    }

    @Test
    void initializeTests_BMQ_ShouldHaveQuestions() {
        // Given
        when(testDefinitionRepository.findWithQuestionsByCode(anyString())).thenReturn(Optional.empty());

        // When
        initializer.initializeTests();

        // Then
        ArgumentCaptor<TestDefinition> testCaptor = ArgumentCaptor.forClass(TestDefinition.class);
        verify(testDefinitionRepository, times(4)).save(testCaptor.capture());

        TestDefinition bmq = testCaptor.getAllValues().stream()
                .filter(t -> "BMQ".equals(t.getCode()))
                .findFirst()
                .orElseThrow();

        assertFalse(bmq.getQuestions().isEmpty());
        assertEquals("Beliefs about Medicines Questionnaire", bmq.getTitle());
    }

    @Test
    void initializeTests_ShouldDeleteSessionsBeforeTests() {
        // Given
        when(testDefinitionRepository.findWithQuestionsByCode(anyString())).thenReturn(Optional.empty());

        // When
        initializer.initializeTests();

        // Then - Sessions should be deleted first (due to FK constraint)
        var inOrder = inOrder(testSessionRepository, testDefinitionRepository);
        inOrder.verify(testSessionRepository).deleteAll();
        inOrder.verify(testSessionRepository).flush();
        inOrder.verify(testDefinitionRepository).deleteAll();
        inOrder.verify(testDefinitionRepository).flush();
    }

    @Test
    void initializeTests_QuestionsHaveCorrectScoreRange() {
        // Given
        when(testDefinitionRepository.findWithQuestionsByCode(anyString())).thenReturn(Optional.empty());

        // When
        initializer.initializeTests();

        // Then
        ArgumentCaptor<TestDefinition> testCaptor = ArgumentCaptor.forClass(TestDefinition.class);
        verify(testDefinitionRepository, times(4)).save(testCaptor.capture());

        for (TestDefinition test : testCaptor.getAllValues()) {
            for (QuestionItem question : test.getQuestions()) {
                assertNotNull(question.getCode(), "Question should have a code");
                assertNotNull(question.getText(), "Question should have text");
                assertTrue(question.getMinScore() >= 0, "Min score should be >= 0");
                assertTrue(question.getMaxScore() > question.getMinScore(), 
                        "Max score should be > min score");
            }
        }
    }

    @Test
    void initializeTests_QuestionsAreProperlyOrdered() {
        // Given
        when(testDefinitionRepository.findWithQuestionsByCode(anyString())).thenReturn(Optional.empty());

        // When
        initializer.initializeTests();

        // Then
        ArgumentCaptor<TestDefinition> testCaptor = ArgumentCaptor.forClass(TestDefinition.class);
        verify(testDefinitionRepository, times(4)).save(testCaptor.capture());

        for (TestDefinition test : testCaptor.getAllValues()) {
            List<QuestionItem> questions = test.getQuestions();
            for (int i = 0; i < questions.size(); i++) {
                assertEquals(i + 1, questions.get(i).getOrderIndex(),
                        "Question order index should be sequential starting from 1");
            }
        }
    }

    @Test
    void initializeTests_SecondTestMissing_ShouldReinitialize() {
        // Given - First test exists, second is missing
        when(testDefinitionRepository.findWithQuestionsByCode("MMAS-8")).thenReturn(Optional.of(mmasTest));
        when(testDefinitionRepository.findWithQuestionsByCode("PHQ-9")).thenReturn(Optional.empty());

        // When
        initializer.initializeTests();

        // Then
        verify(testSessionRepository).deleteAll();
        verify(testDefinitionRepository).deleteAll();
        verify(testDefinitionRepository, times(4)).save(any(TestDefinition.class));
    }
}
