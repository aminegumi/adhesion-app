// AI Response Models

class MotivationRequest {
  final int userId;
  final double? adherenceScore;
  final String? userMessage;
  final List<ChatMessage>? conversationHistory;

  MotivationRequest({
    required this.userId,
    this.adherenceScore,
    this.userMessage,
    this.conversationHistory,
  });

  Map<String, dynamic> toJson() {
    return {
      'userId': userId,
      'adherenceScore': adherenceScore,
      'userMessage': userMessage,
      'conversationHistory': conversationHistory?.map((msg) => {
        'role': msg.role,
        'content': msg.content,
        'timestamp': msg.timestamp.millisecondsSinceEpoch,
      }).toList(),
    };
  }
}

class ChatMessage {
  final String role;
  final String content;
  final DateTime timestamp;

  ChatMessage({
    required this.role,
    required this.content,
    required this.timestamp,
  });

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    return ChatMessage(
      role: json['role'] as String,
      content: json['content'] as String,
      timestamp: DateTime.fromMillisecondsSinceEpoch(json['timestamp'] as int),
    );
  }
}

class MotivationResponse {
  final String message;
  final int userId;

  MotivationResponse({required this.message, required this.userId});

  factory MotivationResponse.fromJson(Map<String, dynamic> json) {
    return MotivationResponse(
      message: json['message'] as String,
      userId: json['userId'] as int,
    );
  }
}

class RecommendationResponse {
  final String recommendations;
  final int userId;

  RecommendationResponse({required this.recommendations, required this.userId});

  factory RecommendationResponse.fromJson(Map<String, dynamic> json) {
    return RecommendationResponse(
      recommendations: json['recommendations'] as String,
      userId: json['userId'] as int,
    );
  }
}

class TestAnalysisResponse {
  final String analysis;
  final String testName;

  TestAnalysisResponse({required this.analysis, required this.testName});

  factory TestAnalysisResponse.fromJson(Map<String, dynamic> json) {
    return TestAnalysisResponse(
      analysis: json['analysis'] as String,
      testName: json['testName'] as String,
    );
  }
}

class EmotionScore {
  final String emotion;
  final double score;

  EmotionScore({required this.emotion, required this.score});

  factory EmotionScore.fromJson(Map<String, dynamic> json) {
    return EmotionScore(
      emotion: json['emotion'] ?? '',
      score: (json['score'] ?? 0).toDouble(),
    );
  }
}

class EmotionResult {
  final String primaryEmotion;
  final double confidence;
  final List<EmotionScore> allEmotions;
  final String analysis;
  final String recommendation;

  EmotionResult({
    required this.primaryEmotion,
    required this.confidence,
    required this.allEmotions,
    required this.analysis,
    required this.recommendation,
  });

  factory EmotionResult.fromJson(Map<String, dynamic> json) {
    return EmotionResult(
      primaryEmotion: json['primaryEmotion'] ?? 'neutral',
      confidence: (json['confidence'] ?? 0.5).toDouble(),
      allEmotions: (json['allEmotions'] as List<dynamic>?)
              ?.map((e) => EmotionScore.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      analysis: json['analysis'] ?? '',
      recommendation: json['recommendation'] ?? '',
    );
  }
}

class FacialEmotionResult {
  final String dominantEmotion;
  final double confidence;
  final Map<String, double> emotionScores;
  final String? insight;

  FacialEmotionResult({
    required this.dominantEmotion,
    required this.confidence,
    required this.emotionScores,
    this.insight,
  });

  factory FacialEmotionResult.fromJson(Map<String, dynamic> json) {
    final scores = <String, double>{};
    if (json['emotionScores'] != null) {
      (json['emotionScores'] as Map<String, dynamic>).forEach((key, value) {
        scores[key] = (value as num).toDouble();
      });
    }
    
    return FacialEmotionResult(
      dominantEmotion: json['dominantEmotion'] ?? 'neutral',
      confidence: (json['confidence'] as num?)?.toDouble() ?? 0.0,
      emotionScores: scores,
      insight: json['insight'],
    );
  }
}
