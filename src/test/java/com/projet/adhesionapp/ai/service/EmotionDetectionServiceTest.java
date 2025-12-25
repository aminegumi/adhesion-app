package com.projet.adhesionapp.ai.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmotionDetectionService - Tests unitaires")
class EmotionDetectionServiceTest {

    private EmotionDetectionService service;

    @BeforeEach
    void setUp() {
        // Service will initialize with modelAvailable = false since model file doesn't exist
        service = new EmotionDetectionService();
    }

    @Nested
    @DisplayName("detectEmotion")
    class DetectEmotionTests {

        @Test
        @DisplayName("Retourne réponse par défaut quand modèle non disponible")
        void shouldReturnDefaultResponseWhenModelNotAvailable() throws Exception {
            // Image factice (n'importe quelles données puisque le modèle n'est pas chargé)
            byte[] fakeImage = new byte[]{1, 2, 3, 4, 5};

            Map<String, Object> result = service.detectEmotion(fakeImage);

            assertThat(result).containsEntry("emotion", "neutral");
            assertThat(result).containsEntry("confidence", 0.5);
        }

        @Test
        @DisplayName("Retourne neutral pour image vide")
        void shouldReturnNeutralForEmptyImage() throws Exception {
            byte[] emptyImage = new byte[0];

            Map<String, Object> result = service.detectEmotion(emptyImage);

            assertThat(result.get("emotion")).isEqualTo("neutral");
        }

        @Test
        @DisplayName("Confidence par défaut est 0.5")
        void shouldHaveDefaultConfidence() throws Exception {
            byte[] image = new byte[]{10, 20, 30};

            Map<String, Object> result = service.detectEmotion(image);

            assertThat((Double) result.get("confidence")).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Résultat contient les clés requises")
        void shouldContainRequiredKeys() throws Exception {
            byte[] image = new byte[]{1, 2, 3};

            Map<String, Object> result = service.detectEmotion(image);

            assertThat(result).containsKeys("emotion", "confidence");
        }
    }

    @Nested
    @DisplayName("Service Initialization")
    class InitializationTests {

        @Test
        @DisplayName("Service s'initialise sans erreur même sans modèle")
        void shouldInitializeWithoutError() {
            EmotionDetectionService newService = new EmotionDetectionService();
            assertThat(newService).isNotNull();
        }

        @Test
        @DisplayName("Multiples instances possibles")
        void shouldAllowMultipleInstances() {
            EmotionDetectionService service1 = new EmotionDetectionService();
            EmotionDetectionService service2 = new EmotionDetectionService();

            assertThat(service1).isNotSameAs(service2);
        }
    }
}
