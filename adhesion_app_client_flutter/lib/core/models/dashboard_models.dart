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
