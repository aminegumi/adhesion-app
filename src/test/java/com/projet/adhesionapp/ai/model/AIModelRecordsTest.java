package com.projet.adhesionapp.ai.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour les records du package ai.model
 */
class AIModelRecordsTest {

    // ==================== EmotionDetectionRequest Tests ====================

    @Test
    void emotionDetectionRequest_ShouldCreateWithValues() {
        // When
        EmotionDetectionRequest request = new EmotionDetectionRequest(1L, "I feel happy");

        // Then
        assertEquals(1L, request.userId());
        assertEquals("I feel happy", request.text());
    }

    @Test
    void emotionDetectionRequest_ShouldSupportEquality() {
        // Given
        EmotionDetectionRequest request1 = new EmotionDetectionRequest(1L, "Test");
        EmotionDetectionRequest request2 = new EmotionDetectionRequest(1L, "Test");
        EmotionDetectionRequest request3 = new EmotionDetectionRequest(2L, "Test");

        // Then
        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
    }

    @Test
    void emotionDetectionRequest_ShouldHaveConsistentHashCode() {
        // Given
        EmotionDetectionRequest request1 = new EmotionDetectionRequest(1L, "Test");
        EmotionDetectionRequest request2 = new EmotionDetectionRequest(1L, "Test");

        // Then
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    // ==================== EmotionDetectionResponse Tests ====================

    @Test
    void emotionDetectionResponse_ShouldCreateWithAllFields() {
        // Given
        List<EmotionDetectionResponse.EmotionScore> emotions = List.of(
                new EmotionDetectionResponse.EmotionScore("joy", 0.8),
                new EmotionDetectionResponse.EmotionScore("neutral", 0.2)
        );

        // When
        EmotionDetectionResponse response = new EmotionDetectionResponse(
                "joy", 0.8, emotions, "User is happy", "Keep up the good mood", 1L
        );

        // Then
        assertEquals("joy", response.primaryEmotion());
        assertEquals(0.8, response.confidence(), 0.01);
        assertEquals(2, response.allEmotions().size());
        assertEquals("User is happy", response.analysis());
        assertEquals("Keep up the good mood", response.recommendation());
        assertEquals(1L, response.userId());
    }

    @Test
    void emotionScore_ShouldCreateCorrectly() {
        // When
        EmotionDetectionResponse.EmotionScore score = new EmotionDetectionResponse.EmotionScore("sadness", 0.75);

        // Then
        assertEquals("sadness", score.emotion());
        assertEquals(0.75, score.score(), 0.01);
    }

    @Test
    void emotionScore_ShouldSupportEquality() {
        // Given
        EmotionDetectionResponse.EmotionScore score1 = new EmotionDetectionResponse.EmotionScore("joy", 0.9);
        EmotionDetectionResponse.EmotionScore score2 = new EmotionDetectionResponse.EmotionScore("joy", 0.9);
        EmotionDetectionResponse.EmotionScore score3 = new EmotionDetectionResponse.EmotionScore("joy", 0.5);

        // Then
        assertEquals(score1, score2);
        assertNotEquals(score1, score3);
    }

    // ==================== TestAnalysisRequest Tests ====================

    @Test
    void testAnalysisRequest_ShouldCreateWithAllFields() {
        // When
        TestAnalysisRequest request = new TestAnalysisRequest(
                "PHQ-9",
                List.of("depression", "anxiety"),
                List.of(15.0, 10.0),
                List.of("Answer1", "Answer2")
        );

        // Then
        assertEquals("PHQ-9", request.testName());
        assertEquals(2, request.dimensions().size());
        assertEquals(2, request.scores().size());
        assertEquals(2, request.answers().size());
    }

    @Test
    void testAnalysisRequest_ShouldAllowNullAnswers() {
        // When
        TestAnalysisRequest request = new TestAnalysisRequest(
                "GAD-7",
                List.of("anxiety"),
                List.of(12.0),
                null
        );

        // Then
        assertNull(request.answers());
        assertEquals("GAD-7", request.testName());
    }

    @Test
    void testAnalysisRequest_ShouldSupportEquality() {
        // Given
        TestAnalysisRequest request1 = new TestAnalysisRequest("Test", List.of("dim"), List.of(1.0), null);
        TestAnalysisRequest request2 = new TestAnalysisRequest("Test", List.of("dim"), List.of(1.0), null);

        // Then
        assertEquals(request1, request2);
    }

    // ==================== TestAnalysisResponse Tests ====================

    @Test
    void testAnalysisResponse_ShouldCreateWithValues() {
        // When
        TestAnalysisResponse response = new TestAnalysisResponse("Analysis result", "PHQ-9");

        // Then
        assertEquals("Analysis result", response.analysis());
        assertEquals("PHQ-9", response.testName());
    }

    @Test
    void testAnalysisResponse_ShouldSupportEquality() {
        // Given
        TestAnalysisResponse response1 = new TestAnalysisResponse("Result", "Test");
        TestAnalysisResponse response2 = new TestAnalysisResponse("Result", "Test");

        // Then
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    // ==================== TreatmentPlanRequest Tests ====================

    @Test
    void treatmentPlanRequest_ShouldCreateWithAllFields() {
        // When
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                1L,
                List.of("insomnia", "anxiety"),
                "Aspirin 100mg"
        );

        // Then
        assertEquals(1L, request.userId());
        assertEquals(2, request.identifiedIssues().size());
        assertEquals("Aspirin 100mg", request.currentMedications());
    }

    @Test
    void treatmentPlanRequest_ShouldAllowNullOptionalFields() {
        // When
        TreatmentPlanRequest request = new TreatmentPlanRequest(1L, null, null);

        // Then
        assertEquals(1L, request.userId());
        assertNull(request.identifiedIssues());
        assertNull(request.currentMedications());
    }

    // ==================== TreatmentPlanResponse Tests ====================

    @Test
    void treatmentPlanResponse_ShouldCreateWithValues() {
        // When
        TreatmentPlanResponse response = new TreatmentPlanResponse(
                "Treatment plan: Take medication at 8am and 8pm",
                1L
        );

        // Then
        assertEquals("Treatment plan: Take medication at 8am and 8pm", response.plan());
        assertEquals(1L, response.userId());
    }

    @Test
    void treatmentPlanResponse_ShouldSupportEquality() {
        // Given
        TreatmentPlanResponse response1 = new TreatmentPlanResponse("Plan", 1L);
        TreatmentPlanResponse response2 = new TreatmentPlanResponse("Plan", 1L);
        TreatmentPlanResponse response3 = new TreatmentPlanResponse("Different", 1L);

        // Then
        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
    }

    // ==================== FacialEmotionDetectionRequest Tests ====================

    @Test
    void facialEmotionDetectionRequest_ShouldCreateWithValues() {
        // Given
        byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};

        // When
        FacialEmotionDetectionRequest request = new FacialEmotionDetectionRequest(1L, imageBytes);

        // Then
        assertEquals(1L, request.userId());
        assertArrayEquals(imageBytes, request.imageBytes());
    }

    @Test
    void facialEmotionDetectionRequest_ShouldHandleEmptyImage() {
        // Given
        byte[] emptyImage = new byte[0];

        // When
        FacialEmotionDetectionRequest request = new FacialEmotionDetectionRequest(1L, emptyImage);

        // Then
        assertEquals(0, request.imageBytes().length);
    }

    // ==================== Record ToString Tests ====================

    @Test
    void allRecords_ShouldHaveToString() {
        // Given
        EmotionDetectionRequest emotionReq = new EmotionDetectionRequest(1L, "text");
        TestAnalysisRequest testReq = new TestAnalysisRequest("Test", List.of(), List.of(), null);
        TreatmentPlanRequest planReq = new TreatmentPlanRequest(1L, null, null);

        // Then
        assertNotNull(emotionReq.toString());
        assertNotNull(testReq.toString());
        assertNotNull(planReq.toString());
        assertTrue(emotionReq.toString().contains("EmotionDetectionRequest"));
        assertTrue(testReq.toString().contains("TestAnalysisRequest"));
        assertTrue(planReq.toString().contains("TreatmentPlanRequest"));
    }
}
