// AI Response Models

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
