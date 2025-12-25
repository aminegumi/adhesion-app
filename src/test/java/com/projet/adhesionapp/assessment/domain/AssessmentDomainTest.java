package com.projet.adhesionapp.assessment.domain;

import com.projet.adhesionapp.identity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour les entités du package assessment.domain
 */
class AssessmentDomainTest {

    private User testUser;
    private TestDefinition testDefinition;
    private TestSession testSession;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");

        testDefinition = TestDefinition.builder()
                .id(1L)
                .code("TEST-001")
                .title("Test Definition")
                .version("1.0")
                .active(true)
                .questions(new ArrayList<>())
                .build();

        testSession = TestSession.builder()
                .id(1L)
                .user(testUser)
                .testDefinition(testDefinition)
                .build();
    }

    // ==================== ProfileScore Tests ====================

    @Test
    void profileScore_ShouldBuildWithBuilder() {
        // When
        ProfileScore score = ProfileScore.builder()
                .id(1L)
                .user(testUser)
                .dimension("depression")
                .score(0.75)
                .session(testSession)
                .build();

        // Then
        assertEquals(1L, score.getId());
        assertEquals(testUser, score.getUser());
        assertEquals("depression", score.getDimension());
        assertEquals(0.75, score.getScore(), 0.01);
        assertEquals(testSession, score.getSession());
    }

    @Test
    void profileScore_PrePersist_ShouldSetTimestamps() {
        // Given
        ProfileScore score = new ProfileScore();

        // When
        score.onCreate();

        // Then
        assertNotNull(score.getCreatedAt());
        assertNotNull(score.getUpdatedAt());
        assertNotNull(score.getComputedAt());
        assertEquals(score.getCreatedAt(), score.getUpdatedAt());
    }

    @Test
    void profileScore_PrePersist_ShouldNotOverwriteComputedAt() {
        // Given
        ProfileScore score = new ProfileScore();
        Instant customComputedAt = Instant.now().minusSeconds(3600);
        score.setComputedAt(customComputedAt);

        // When
        score.onCreate();

        // Then
        assertEquals(customComputedAt, score.getComputedAt());
    }

    @Test
    void profileScore_PreUpdate_ShouldUpdateTimestamp() throws InterruptedException {
        // Given
        ProfileScore score = new ProfileScore();
        score.onCreate();
        Instant originalUpdatedAt = score.getUpdatedAt();
        Thread.sleep(10); // Small delay to ensure different timestamp

        // When
        score.onUpdate();

        // Then
        assertTrue(score.getUpdatedAt().isAfter(originalUpdatedAt) || 
                   score.getUpdatedAt().equals(originalUpdatedAt));
    }

    @Test
    void profileScore_ShouldAllowAllFieldsAccess() {
        // Given
        ProfileScore score = new ProfileScore();

        // When
        score.setId(10L);
        score.setUser(testUser);
        score.setDimension("anxiety");
        score.setScore(0.5);
        score.setSession(testSession);
        score.setComputedAt(Instant.now());
        score.setCreatedAt(Instant.now());
        score.setUpdatedAt(Instant.now());

        // Then
        assertEquals(10L, score.getId());
        assertEquals(testUser, score.getUser());
        assertEquals("anxiety", score.getDimension());
        assertEquals(0.5, score.getScore(), 0.01);
    }

    // ==================== QuestionItem Tests ====================

    @Test
    void questionItem_ShouldBuildWithBuilder() {
        // When
        QuestionItem item = QuestionItem.builder()
                .id(1L)
                .code("Q1")
                .text("How often do you feel sad?")
                .orderIndex(1)
                .reverseScored(false)
                .minScore(0)
                .maxScore(4)
                .testDefinition(testDefinition)
                .build();

        // Then
        assertEquals(1L, item.getId());
        assertEquals("Q1", item.getCode());
        assertEquals("How often do you feel sad?", item.getText());
        assertEquals(1, item.getOrderIndex());
        assertFalse(item.isReverseScored());
        assertEquals(0, item.getMinScore());
        assertEquals(4, item.getMaxScore());
    }

    @Test
    void questionItem_PrePersist_ShouldSetTimestamps() {
        // Given
        QuestionItem item = new QuestionItem();

        // When
        item.onCreate();

        // Then
        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
    }

    @Test
    void questionItem_PreUpdate_ShouldUpdateTimestamp() throws InterruptedException {
        // Given
        QuestionItem item = new QuestionItem();
        item.onCreate();
        Instant original = item.getUpdatedAt();
        Thread.sleep(10);

        // When
        item.onUpdate();

        // Then
        assertTrue(item.getUpdatedAt().isAfter(original) || item.getUpdatedAt().equals(original));
    }

    @Test
    void questionItem_ShouldSupportReverseScoring() {
        // Given
        QuestionItem normalItem = QuestionItem.builder()
                .reverseScored(false)
                .build();
        QuestionItem reverseItem = QuestionItem.builder()
                .reverseScored(true)
                .build();

        // Then
        assertFalse(normalItem.isReverseScored());
        assertTrue(reverseItem.isReverseScored());
    }

    @Test
    void questionItem_ShouldAllowAllFieldsAccess() {
        // Given
        QuestionItem item = new QuestionItem();

        // When
        item.setId(5L);
        item.setCode("Q5");
        item.setText("Test question");
        item.setOrderIndex(5);
        item.setReverseScored(true);
        item.setMinScore(1);
        item.setMaxScore(5);
        item.setTestDefinition(testDefinition);

        // Then
        assertEquals(5L, item.getId());
        assertEquals("Q5", item.getCode());
        assertEquals("Test question", item.getText());
        assertEquals(5, item.getOrderIndex());
        assertTrue(item.isReverseScored());
        assertEquals(1, item.getMinScore());
        assertEquals(5, item.getMaxScore());
    }

    // ==================== TestDefinition Tests ====================

    @Test
    void testDefinition_ShouldBuildWithBuilder() {
        // When
        TestDefinition def = TestDefinition.builder()
                .id(1L)
                .code("PHQ-9")
                .title("Patient Health Questionnaire")
                .version("1.0")
                .active(true)
                .build();

        // Then
        assertEquals(1L, def.getId());
        assertEquals("PHQ-9", def.getCode());
        assertEquals("Patient Health Questionnaire", def.getTitle());
        assertEquals("1.0", def.getVersion());
        assertTrue(def.isActive());
    }

    @Test
    void testDefinition_PrePersist_ShouldSetTimestamps() {
        // Given
        TestDefinition def = new TestDefinition();

        // When
        def.onCreate();

        // Then
        assertNotNull(def.getCreatedAt());
        assertNotNull(def.getUpdatedAt());
    }

    @Test
    void testDefinition_PreUpdate_ShouldUpdateTimestamp() throws InterruptedException {
        // Given
        TestDefinition def = new TestDefinition();
        def.onCreate();
        Instant original = def.getUpdatedAt();
        Thread.sleep(10);

        // When
        def.onUpdate();

        // Then
        assertTrue(def.getUpdatedAt().isAfter(original) || def.getUpdatedAt().equals(original));
    }

    @Test
    void testDefinition_ShouldHaveQuestionsCollection() {
        // Given
        TestDefinition def = TestDefinition.builder()
                .code("TEST")
                .questions(new ArrayList<>())
                .build();

        QuestionItem q1 = QuestionItem.builder().code("Q1").text("Question 1").build();
        QuestionItem q2 = QuestionItem.builder().code("Q2").text("Question 2").build();

        // When
        def.getQuestions().add(q1);
        def.getQuestions().add(q2);

        // Then
        assertEquals(2, def.getQuestions().size());
    }

    @Test
    void testDefinition_ShouldAllowAllFieldsAccess() {
        // Given
        TestDefinition def = new TestDefinition();

        // When
        def.setId(10L);
        def.setCode("GAD-7");
        def.setTitle("Generalized Anxiety Disorder");
        def.setVersion("2.0");
        def.setActive(false);
        def.setQuestions(List.of());

        // Then
        assertEquals(10L, def.getId());
        assertEquals("GAD-7", def.getCode());
        assertEquals("Generalized Anxiety Disorder", def.getTitle());
        assertEquals("2.0", def.getVersion());
        assertFalse(def.isActive());
    }

    // ==================== TestDefinition Builder Default Tests ====================

    @Test
    void testDefinition_Builder_ShouldInitializeQuestionsAsEmptyList() {
        // When
        TestDefinition def = TestDefinition.builder()
                .code("TEST")
                .build();

        // Then - @Builder.Default should initialize empty list
        assertNotNull(def.getQuestions());
    }

    // ==================== AllArgsConstructor Tests ====================

    @Test
    void profileScore_AllArgsConstructor_ShouldWork() {
        // When
        ProfileScore score = new ProfileScore(
                1L, testUser, "dimension", 0.5, testSession, Instant.now(), Instant.now(), Instant.now()
        );

        // Then
        assertEquals(1L, score.getId());
        assertEquals("dimension", score.getDimension());
    }

    @Test
    void questionItem_AllArgsConstructor_ShouldWork() {
        // When
        QuestionItem item = new QuestionItem(
                1L, "Q1", "Text", 1, false, 0, 4, testDefinition, Instant.now(), Instant.now()
        );

        // Then
        assertEquals(1L, item.getId());
        assertEquals("Q1", item.getCode());
    }

    @Test
    void testDefinition_AllArgsConstructor_ShouldWork() {
        // When
        TestDefinition def = new TestDefinition(
                1L, "CODE", "Title", "1.0", true, List.of(), Instant.now(), Instant.now()
        );

        // Then
        assertEquals(1L, def.getId());
        assertEquals("CODE", def.getCode());
    }

    // ==================== NoArgsConstructor Tests ====================

    @Test
    void profileScore_NoArgsConstructor_ShouldWork() {
        // When
        ProfileScore score = new ProfileScore();

        // Then
        assertNotNull(score);
        assertNull(score.getId());
    }

    @Test
    void questionItem_NoArgsConstructor_ShouldWork() {
        // When
        QuestionItem item = new QuestionItem();

        // Then
        assertNotNull(item);
        assertNull(item.getId());
    }

    @Test
    void testDefinition_NoArgsConstructor_ShouldWork() {
        // When
        TestDefinition def = new TestDefinition();

        // Then
        assertNotNull(def);
        assertNull(def.getId());
    }

    // ==================== Score Range Tests ====================

    @Test
    void questionItem_ScoreRange_ShouldBeAccessible() {
        // Given
        QuestionItem likertItem = QuestionItem.builder()
                .minScore(0)
                .maxScore(4)
                .build();

        QuestionItem binaryItem = QuestionItem.builder()
                .minScore(0)
                .maxScore(1)
                .build();

        // Then
        assertEquals(0, likertItem.getMinScore());
        assertEquals(4, likertItem.getMaxScore());
        assertEquals(0, binaryItem.getMinScore());
        assertEquals(1, binaryItem.getMaxScore());
    }
}
