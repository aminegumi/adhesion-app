package com.projet.adhesionapp.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projet.adhesionapp.ai.model.MotivationRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAI Service Tests")
class OpenAIServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ObjectMapper objectMapper;
    private OpenAIService openAIService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        openAIService = new OpenAIService(webClient, "test-api-key", objectMapper);
        ReflectionTestUtils.setField(openAIService, "model", "test-model");
        ReflectionTestUtils.setField(openAIService, "maxTokens", 1000);
    }

    @SuppressWarnings("unchecked")
    private void setupWebClientMock(String response) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/chat/completions")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenAnswer(inv -> requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(response));
    }

    @SuppressWarnings("unchecked")
    private void setupWebClientMockWithException(RuntimeException exception) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/chat/completions")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenAnswer(inv -> requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenThrow(exception);
    }

    private String createValidApiResponse(String content) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode choices = root.putArray("choices");
        ObjectNode choice = choices.addObject();
        ObjectNode message = choice.putObject("message");
        message.put("content", content);
        return root.toString();
    }

    @Nested
    @DisplayName("Generate Completion Tests")
    class GenerateCompletionTests {

        @Test
        @DisplayName("Should generate completion successfully")
        void shouldGenerateCompletionSuccessfully() {
            // Given
            String expectedResponse = "Hello! How can I help you?";
            setupWebClientMock(createValidApiResponse(expectedResponse));

            // When
            String result = openAIService.generateCompletion("System prompt", "Hello");

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(webClient).post();
        }

        @Test
        @DisplayName("Should throw RuntimeException when response format is invalid")
        void shouldThrowRuntimeExceptionWhenResponseFormatInvalid() {
            // Given
            setupWebClientMock("{}");

            // When & Then
            assertThatThrownBy(() -> openAIService.generateCompletion("Prompt", "Message"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to parse OpenRouter API response");
        }

        @Test
        @DisplayName("Should handle rate limit error (429)")
        void shouldHandleRateLimitError() {
            // Given
            WebClientResponseException rateLimitException = WebClientResponseException.create(
                    429, "Too Many Requests", null, null, null);
            setupWebClientMockWithException(rateLimitException);

            // When & Then
            assertThatThrownBy(() -> openAIService.generateCompletion("Prompt", "Message"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("rate limit exceeded");
        }

        @Test
        @DisplayName("Should handle generic WebClient error")
        void shouldHandleGenericWebClientError() {
            // Given
            WebClientResponseException serverError = WebClientResponseException.create(
                    500, "Internal Server Error", null, null, null);
            setupWebClientMockWithException(serverError);

            // When & Then
            assertThatThrownBy(() -> openAIService.generateCompletion("Prompt", "Message"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI service error");
        }

        @Test
        @DisplayName("Should handle unexpected exception")
        void shouldHandleUnexpectedException() {
            // Given
            when(webClient.post()).thenThrow(new RuntimeException("Unexpected error"));

            // When & Then
            assertThatThrownBy(() -> openAIService.generateCompletion("Prompt", "Message"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate AI response");
        }
    }

    @Nested
    @DisplayName("Generate Motivational Message Tests")
    class GenerateMotivationalMessageTests {

        @Test
        @DisplayName("Should generate motivational message for patient")
        void shouldGenerateMotivationalMessage() {
            // Given
            String expectedMessage = "Bonjour Jean! You're doing great!";
            setupWebClientMock(createValidApiResponse(expectedMessage));

            // When
            String result = openAIService.generateMotivationalMessage("Jean", "Balanced profile", 0.75);

            // Then
            assertThat(result).isEqualTo(expectedMessage);
        }

        @Test
        @DisplayName("Should handle low adherence score")
        void shouldHandleLowAdherenceScore() {
            // Given
            String expectedMessage = "Marie, let's work together!";
            setupWebClientMock(createValidApiResponse(expectedMessage));

            // When
            String result = openAIService.generateMotivationalMessage("Marie", "Needs support", 0.3);

            // Then
            assertThat(result).isNotBlank();
        }

        @Test
        @DisplayName("Should handle high adherence score")
        void shouldHandleHighAdherenceScore() {
            // Given
            String expectedMessage = "Excellent work!";
            setupWebClientMock(createValidApiResponse(expectedMessage));

            // When
            String result = openAIService.generateMotivationalMessage("Pierre", "Excellent profile", 0.95);

            // Then
            assertThat(result).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Generate Conversational Response Tests")
    class GenerateConversationalResponseTests {

        @Test
        @DisplayName("Should generate conversational response with history")
        void shouldGenerateConversationalResponseWithHistory() {
            // Given
            String expectedResponse = "I understand, Pierre.";
            setupWebClientMock(createValidApiResponse(expectedResponse));
            List<MotivationRequest.ChatMessage> history = List.of(
                    new MotivationRequest.ChatMessage("user", "Hello", System.currentTimeMillis()),
                    new MotivationRequest.ChatMessage("assistant", "Hi Pierre!", System.currentTimeMillis())
            );

            // When
            String result = openAIService.generateConversationalResponse(
                    "Pierre", "Anxious profile", 0.8, "I'm stressed", history);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("Should handle empty conversation history")
        void shouldHandleEmptyConversationHistory() {
            // Given
            String expectedResponse = "Hello Sophie!";
            setupWebClientMock(createValidApiResponse(expectedResponse));

            // When
            String result = openAIService.generateConversationalResponse(
                    "Sophie", "Stable", 0.9, "Hi", List.of());

            // Then
            assertThat(result).isNotBlank();
        }

        @Test
        @DisplayName("Should handle null conversation history")
        void shouldHandleNullConversationHistory() {
            // Given
            String expectedResponse = "Hello!";
            setupWebClientMock(createValidApiResponse(expectedResponse));

            // When
            String result = openAIService.generateConversationalResponse(
                    "Luc", "Profile", 0.7, "Hi", null);

            // Then
            assertThat(result).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Generate Recommendations Tests")
    class GenerateRecommendationsTests {

        @Test
        @DisplayName("Should generate recommendations for high risk patient")
        void shouldGenerateRecommendationsForHighRiskPatient() {
            // Given
            String expectedRecs = "1. Set daily reminders\n2. Practice relaxation";
            setupWebClientMock(createValidApiResponse(expectedRecs));

            // When
            String result = openAIService.generateRecommendations(
                    "Emma", "Anxious", "High anxiety", 0.8);

            // Then
            assertThat(result).isEqualTo(expectedRecs);
        }

        @Test
        @DisplayName("Should generate recommendations for low risk patient")
        void shouldGenerateRecommendationsForLowRiskPatient() {
            // Given
            String expectedRecs = "Keep up the good work!";
            setupWebClientMock(createValidApiResponse(expectedRecs));

            // When
            String result = openAIService.generateRecommendations(
                    "Louis", "Balanced", "Good scores", 0.2);

            // Then
            assertThat(result).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Generate Treatment Plan Tests")
    class GenerateTreatmentPlanTests {

        @Test
        @DisplayName("Should generate treatment plan with issues")
        void shouldGenerateTreatmentPlanWithIssues() {
            // Given
            String expectedPlan = "Week 1: Establish routine...";
            setupWebClientMock(createValidApiResponse(expectedPlan));
            List<String> issues = List.of("Depression management", "Anxiety reduction");

            // When
            String result = openAIService.generateTreatmentPlan(
                    "Claire", "Depressive", issues, "Sertraline 50mg");

            // Then
            assertThat(result).isEqualTo(expectedPlan);
        }

        @Test
        @DisplayName("Should handle empty issues list")
        void shouldHandleEmptyIssuesList() {
            // Given
            String expectedPlan = "Maintenance plan...";
            setupWebClientMock(createValidApiResponse(expectedPlan));

            // When
            String result = openAIService.generateTreatmentPlan(
                    "Thomas", "Balanced", List.of(), "None");

            // Then
            assertThat(result).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Analyze Test Results Tests")
    class AnalyzeTestResultsTests {

        @Test
        @DisplayName("Should analyze test results with multiple dimensions")
        void shouldAnalyzeTestResultsWithMultipleDimensions() {
            // Given
            String expectedAnalysis = "Overall mild depression symptoms...";
            setupWebClientMock(createValidApiResponse(expectedAnalysis));

            // When
            String result = openAIService.analyzeTestResults(
                    "PHQ-9",
                    List.of("Mood", "Sleep", "Energy"),
                    List.of(2.5, 3.0, 2.0),
                    List.of("Sometimes", "Often", "Rarely"));

            // Then
            assertThat(result).isEqualTo(expectedAnalysis);
        }

        @Test
        @DisplayName("Should handle empty scores")
        void shouldHandleEmptyScores() {
            // Given
            String expectedAnalysis = "Insufficient data";
            setupWebClientMock(createValidApiResponse(expectedAnalysis));

            // When
            String result = openAIService.analyzeTestResults(
                    "GAD-7", List.of(), List.of(), List.of());

            // Then
            assertThat(result).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Detect Emotions Tests")
    class DetectEmotionsTests {

        @Test
        @DisplayName("Should detect emotions from text")
        void shouldDetectEmotionsFromText() {
            // Given
            String expectedResponse = "{\"primaryEmotion\": \"anxious\", \"confidence\": 0.85}";
            setupWebClientMock(createValidApiResponse(expectedResponse));

            // When
            String result = openAIService.detectEmotions(
                    "Alice", "I'm feeling very anxious about my appointment");

            // Then
            assertThat(result).contains("anxious");
        }

        @Test
        @DisplayName("Should detect positive emotions")
        void shouldDetectPositiveEmotions() {
            // Given
            String expectedResponse = "{\"primaryEmotion\": \"happy\", \"confidence\": 0.9}";
            setupWebClientMock(createValidApiResponse(expectedResponse));

            // When
            String result = openAIService.detectEmotions(
                    "Bob", "I'm so happy today!");

            // Then
            assertThat(result).contains("happy");
        }

        @Test
        @DisplayName("Should detect neutral emotions")
        void shouldDetectNeutralEmotions() {
            // Given
            String expectedResponse = "{\"primaryEmotion\": \"neutral\", \"confidence\": 0.7}";
            setupWebClientMock(createValidApiResponse(expectedResponse));

            // When
            String result = openAIService.detectEmotions(
                    "Charlie", "Today is Wednesday.");

            // Then
            assertThat(result).contains("neutral");
        }
    }
}
