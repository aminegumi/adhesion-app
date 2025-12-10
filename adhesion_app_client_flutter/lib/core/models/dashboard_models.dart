class AdherenceSummary {
  final String date; // YYYY-MM-DD
  final double completionRate;
  final double adherenceScore;

  AdherenceSummary({
    required this.date,
    required this.completionRate,
    required this.adherenceScore,
  });

  factory AdherenceSummary.fromJson(Map<String, dynamic> json) =>
      AdherenceSummary(
        date: json['date'] as String,
        completionRate: (json['completionRate'] as num).toDouble(),
        adherenceScore: (json['adherenceScore'] as num).toDouble(),
      );
}

class PredictionItem {
  final int id;
  final String date; // YYYY-MM-DD
  final double probNonAdherence;
  final String modelVersion;

  PredictionItem({
    required this.id,
    required this.date,
    required this.probNonAdherence,
    required this.modelVersion,
  });

  factory PredictionItem.fromJson(Map<String, dynamic> json) => PredictionItem(
    id: (json['id'] ?? 0) as int,
    date: json['date'] as String,
    probNonAdherence: (json['probNonAdherence'] as num).toDouble(),
    modelVersion: (json['modelVersion'] ?? '') as String,
  );
}

/// Enhanced adherence prediction with multi-factor analysis
class AdherencePrediction {
  final int userId;
  final String userName;
  final double predictedAdherence; // 0.0 - 1.0
  final String riskLevel; // Low, Moderate, High, Very High
  final double confidence;
  final List<RiskFactor> riskFactors;
  final Map<String, double> componentScores;
  final int currentStreak;
  final int bestStreak;
  final Map<String, int> skipReasonBreakdown;
  final String recommendations;
  final String predictionDate;

  AdherencePrediction({
    required this.userId,
    required this.userName,
    required this.predictedAdherence,
    required this.riskLevel,
    required this.confidence,
    required this.riskFactors,
    required this.componentScores,
    required this.currentStreak,
    required this.bestStreak,
    required this.skipReasonBreakdown,
    required this.recommendations,
    required this.predictionDate,
  });

  /// Get adherence as percentage (0-100)
  double get adherencePercentage => predictedAdherence * 100;

  /// Check if high risk
  bool get isHighRisk => riskLevel == 'High' || riskLevel == 'Very High';

  factory AdherencePrediction.fromJson(Map<String, dynamic> json) {
    return AdherencePrediction(
      userId: json['userId'] as int,
      userName: json['userName'] as String? ?? '',
      predictedAdherence: (json['predictedAdherence'] as num).toDouble(),
      riskLevel: json['riskLevel'] as String? ?? 'Moderate',
      confidence: (json['confidence'] as num?)?.toDouble() ?? 0.5,
      riskFactors:
          (json['riskFactors'] as List<dynamic>?)
              ?.map((e) => RiskFactor.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      componentScores:
          (json['componentScores'] as Map<String, dynamic>?)?.map(
            (k, v) => MapEntry(k, (v as num).toDouble()),
          ) ??
          {},
      currentStreak: json['currentStreak'] as int? ?? 0,
      bestStreak: json['bestStreak'] as int? ?? 0,
      skipReasonBreakdown:
          (json['skipReasonBreakdown'] as Map<String, dynamic>?)?.map(
            (k, v) => MapEntry(k, (v as num).toInt()),
          ) ??
          {},
      recommendations: json['recommendations'] as String? ?? '',
      predictionDate: json['predictionDate'] as String? ?? '',
    );
  }
}

/// A single risk factor contributing to adherence risk
class RiskFactor {
  final String factor;
  final String description;
  final String severity; // low, medium, high
  final String recommendation;

  RiskFactor({
    required this.factor,
    required this.description,
    required this.severity,
    required this.recommendation,
  });

  /// Get color for UI display
  int get colorValue {
    switch (severity) {
      case 'high':
        return 0xFFE53935; // Red
      case 'medium':
        return 0xFFFB8C00; // Orange
      case 'low':
        return 0xFFFDD835; // Yellow
      default:
        return 0xFF9E9E9E; // Gray
    }
  }

  factory RiskFactor.fromJson(Map<String, dynamic> json) {
    return RiskFactor(
      factor: json['factor'] as String? ?? '',
      description: json['description'] as String? ?? '',
      severity: json['severity'] as String? ?? 'medium',
      recommendation: json['recommendation'] as String? ?? '',
    );
  }
}
