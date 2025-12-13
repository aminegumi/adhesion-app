import 'dart:convert';
import 'package:http/http.dart' as http;

/// Service for interacting with OpenRouter AI API
class OpenRouterService {
  // OpenRouter API configuration
  static const String _apiKey = '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';
  static const String _apiUrl = 'https://openrouter.ai/api/v1/chat/completions';
  
  // Using Alibaba Tongyi DeepResearch model (free tier)
  static const String defaultModel = 'alibaba/tongyi-deepresearch-30b-a3b:free';
  static const int maxTokens = 1000;
  
  final http.Client _client;
  
  OpenRouterService({http.Client? client}) : _client = client ?? http.Client();
  
  /// Send a chat message and get AI response
  Future<String> chat({
    required String userMessage,
    required List<ChatMessageData> conversationHistory,
    String? systemPrompt,
    String? currentEmotion,
    double? adherenceScore,
  }) async {
    final effectiveSystemPrompt = systemPrompt ?? _buildSystemPrompt(
      currentEmotion: currentEmotion,
      adherenceScore: adherenceScore,
    );
    
    final messages = <Map<String, String>>[
      {'role': 'system', 'content': effectiveSystemPrompt},
    ];
    
    // Add conversation history (last 10 messages for context)
    final recentHistory = conversationHistory.length > 10 
        ? conversationHistory.sublist(conversationHistory.length - 10)
        : conversationHistory;
    
    for (final msg in recentHistory) {
      messages.add({
        'role': msg.role,
        'content': msg.content,
      });
    }
    
    // Add current message
    messages.add({'role': 'user', 'content': userMessage});
    
    try {
      final response = await _client.post(
        Uri.parse(_apiUrl),
        headers: {
          'Authorization': 'Bearer $_apiKey',
          'Content-Type': 'application/json',
          'HTTP-Referer': 'https://adhesion-app.com',
          'X-Title': 'Adhesion Mental Health App',
        },
        body: jsonEncode({
          'model': defaultModel,
          'messages': messages,
          'max_tokens': maxTokens,
          'temperature': 0.7,
        }),
      );
      
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final content = data['choices'][0]['message']['content'] as String;
        return _cleanResponse(content);
      } else {
        throw AIServiceException(
          'Failed to get AI response: ${response.statusCode}',
          response.statusCode,
        );
      }
    } catch (e) {
      if (e is AIServiceException) rethrow;
      throw AIServiceException('Connection error: $e', 0);
    }
  }
  
  /// Generate a motivational message
  Future<String> getMotivation({
    String? emotion,
    double? adherenceScore,
    String? context,
  }) async {
    final prompt = _buildMotivationPrompt(
      emotion: emotion,
      adherenceScore: adherenceScore,
      context: context,
    );
    
    return chat(
      userMessage: prompt,
      conversationHistory: [],
      systemPrompt: '''You are a supportive mental health companion. 
Generate warm, encouraging motivational messages focused on mental wellness 
and medication adherence. Keep responses under 100 words, be genuine and caring.
Do not use asterisks or special formatting.''',
    );
  }
  
  /// Analyze emotional content in text
  Future<EmotionAnalysis> analyzeTextEmotion(String text) async {
    final prompt = '''Analyze the emotional tone of this text and provide:
1. Primary emotion (one of: happy, sad, angry, anxious, fearful, neutral, hopeful)
2. Confidence level (0.0 to 1.0)
3. A brief supportive observation
4. A wellness recommendation

Text: "$text"

Respond in this exact format:
EMOTION: [emotion]
CONFIDENCE: [number]
OBSERVATION: [text]
RECOMMENDATION: [text]''';

    try {
      final response = await chat(
        userMessage: prompt,
        conversationHistory: [],
        systemPrompt: 'You are an empathetic emotion analyzer. Respond only in the exact format requested.',
      );
      
      return _parseEmotionAnalysis(response);
    } catch (e) {
      return EmotionAnalysis(
        primaryEmotion: 'neutral',
        confidence: 0.5,
        observation: 'I\'m here to support you.',
        recommendation: 'Take a moment to breathe and reflect.',
      );
    }
  }
  
  String _buildSystemPrompt({String? currentEmotion, double? adherenceScore}) {
    final emotionContext = currentEmotion != null 
        ? '\n\nThe user\'s current emotional state appears to be: $currentEmotion. Tailor your response accordingly.'
        : '';
    
    final adherenceContext = adherenceScore != null
        ? '\n\nThe user\'s medication adherence score is ${(adherenceScore * 100).toStringAsFixed(0)}%. ${adherenceScore < 0.7 ? 'Gently encourage better adherence.' : 'Acknowledge their good adherence.'}'
        : '';
    
    return '''You are Serenity, a compassionate Mental Health Assistant for the Adhesion app.

Your role:
- Provide emotional support and therapeutic conversations
- Support medication adherence and healthy habits
- Listen with empathy and validate feelings
- Offer practical coping strategies
- Never diagnose or replace professional help

Communication style:
- Warm, caring, and conversational
- Keep responses concise (60-100 words)
- Ask thoughtful follow-up questions
- No asterisks or special formatting
- Be genuine and human-like$emotionContext$adherenceContext''';
  }
  
  String _buildMotivationPrompt({
    String? emotion,
    double? adherenceScore,
    String? context,
  }) {
    final parts = <String>[];
    
    if (emotion != null) {
      parts.add('Current emotional state: $emotion');
    }
    
    if (adherenceScore != null) {
      final percentage = (adherenceScore * 100).toStringAsFixed(0);
      parts.add('Medication adherence: $percentage%');
    }
    
    if (context != null) {
      parts.add('Context: $context');
    }
    
    if (parts.isEmpty) {
      return 'Please provide an encouraging message about mental wellness and self-care.';
    }
    
    return 'Based on: ${parts.join(", ")}. Please provide a personalized motivational message.';
  }
  
  String _cleanResponse(String text) {
    return text
        .replaceAll('*', '')
        .replaceAll(RegExp(r'\n{3,}'), '\n\n')
        .replaceAll(RegExp(r' +'), ' ')
        .replaceAll(RegExp(r'\[Your Name\]', caseSensitive: false), '')
        .replaceAll(RegExp(r'\[Name\]', caseSensitive: false), '')
        .trim();
  }
  
  EmotionAnalysis _parseEmotionAnalysis(String response) {
    String emotion = 'neutral';
    double confidence = 0.5;
    String observation = 'I\'m here to support you.';
    String recommendation = 'Take a moment to breathe.';
    
    final lines = response.split('\n');
    for (final line in lines) {
      final lowerLine = line.toLowerCase().trim();
      if (lowerLine.startsWith('emotion:')) {
        emotion = line.substring(8).trim().toLowerCase();
      } else if (lowerLine.startsWith('confidence:')) {
        final value = double.tryParse(line.substring(11).trim());
        if (value != null) confidence = value.clamp(0.0, 1.0);
      } else if (lowerLine.startsWith('observation:')) {
        observation = line.substring(12).trim();
      } else if (lowerLine.startsWith('recommendation:')) {
        recommendation = line.substring(15).trim();
      }
    }
    
    return EmotionAnalysis(
      primaryEmotion: emotion,
      confidence: confidence,
      observation: observation,
      recommendation: recommendation,
    );
  }
  
  void close() => _client.close();
}

/// Chat message data for conversation history
class ChatMessageData {
  final String role;
  final String content;
  final DateTime timestamp;
  
  ChatMessageData({
    required this.role,
    required this.content,
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();
  
  Map<String, dynamic> toJson() => {
    'role': role,
    'content': content,
    'timestamp': timestamp.millisecondsSinceEpoch,
  };
}

/// Result of emotion analysis
class EmotionAnalysis {
  final String primaryEmotion;
  final double confidence;
  final String observation;
  final String recommendation;
  
  EmotionAnalysis({
    required this.primaryEmotion,
    required this.confidence,
    required this.observation,
    required this.recommendation,
  });
}

/// Exception for AI service errors
class AIServiceException implements Exception {
  final String message;
  final int statusCode;
  
  AIServiceException(this.message, this.statusCode);
  
  @override
  String toString() => 'AIServiceException: $message (status: $statusCode)';
}

