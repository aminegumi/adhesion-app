package com.projet.adhesionapp.ai.service;

import ai.onnxruntime.*;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmotionDetectionService {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final boolean modelAvailable;

    public EmotionDetectionService() {
        OrtEnvironment tempEnv = null;
        OrtSession tempSession = null;
        boolean available = false;

        try {
            // Try to initialize ONNX Runtime environment
            tempEnv = OrtEnvironment.getEnvironment();

            // Try to load the ONNX model - if it doesn't exist, we'll handle it gracefully
            tempSession = tempEnv.createSession("src/main/resources/models/emotion_model.onnx",
                    new OrtSession.SessionOptions());
            available = true;
        } catch (Exception e) {
            System.out.println("Warning: Emotion detection model not found or ONNX Runtime not available. Emotion detection will return default values.");
            System.out.println("To enable emotion detection, place the model file at: src/main/resources/models/emotion_model.onnx");
            System.out.println("Exception: " + e.getMessage());
        }

        this.env = tempEnv != null ? tempEnv : OrtEnvironment.getEnvironment();
        this.session = tempSession;
        this.modelAvailable = available;
    }

    public Map<String, Object> detectEmotion(byte[] imageBytes) throws OrtException, IOException {
        // If model is not available, return a default response
        if (!modelAvailable || session == null) {
            Map<String, Object> defaultResponse = new HashMap<>();
            defaultResponse.put("emotion", "neutral");
            defaultResponse.put("confidence", 0.5);
            return defaultResponse;
        }

        // Preprocess image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        // Resize to 224x224 (assuming model input size)
        BufferedImage resized = resizeImage(image, 224, 224);

        // Convert to float array (normalize to 0-1)
        float[] inputData = imageToFloatArray(resized);

        // Create input tensor
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData),
                new long[]{1, 3, 224, 224});

        // Run inference
        OrtSession.Result result = session.run(Map.of("input", inputTensor));

        // Get output (assuming emotions are: anger, disgust, fear, happy, sad, surprise, neutral)
        float[] outputs = ((float[][][][]) result.get(0).getValue())[0][0][0];

        // Find max probability emotion
        String[] emotions = {"anger", "disgust", "fear", "happy", "sad", "surprise", "neutral"};
        int maxIndex = 0;
        float maxProb = outputs[0];
        for (int i = 1; i < outputs.length; i++) {
            if (outputs[i] > maxProb) {
                maxProb = outputs[i];
                maxIndex = i;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("emotion", emotions[maxIndex]);
        response.put("confidence", maxProb);
        return response;
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        resized.getGraphics().drawImage(original.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        return resized;
    }

    private float[] imageToFloatArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[] data = new float[3 * width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                data[y * width + x] = r / 255.0f;
                data[width * height + y * width + x] = g / 255.0f;
                data[2 * width * height + y * width + x] = b / 255.0f;
            }
        }
        return data;
    }
}