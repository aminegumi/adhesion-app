// Psychological Profile Models

class PsychologicalProfile {
  final int id;
  final int userId;
  final String? userName;
  final String profileType;
  final double? anxietyScore;
  final double? depressionScore;
  final double? motivationScore;
  final double? selfEfficacyScore;
  final double? socialSupportScore;
  final double? healthLocusScore;
  final double? adherenceRiskScore;
  final String? summary;
  final String? detailedInterpretation;
  final String? status;
  final String? createdAt;

  PsychologicalProfile({
    required this.id,
    required this.userId,
    this.userName,
    required this.profileType,
    this.anxietyScore,
    this.depressionScore,
    this.motivationScore,
    this.selfEfficacyScore,
    this.socialSupportScore,
    this.healthLocusScore,
    this.adherenceRiskScore,
    this.summary,
    this.detailedInterpretation,
    this.status,
    this.createdAt,
  });

  factory PsychologicalProfile.fromJson(Map<String, dynamic> json) {
    return PsychologicalProfile(
      id: json['id'] as int,
      userId: json['userId'] as int,
      userName: json['userName'] as String?,
      profileType: json['profileType'] as String,
      anxietyScore: (json['anxietyScore'] as num?)?.toDouble(),
      depressionScore: (json['depressionScore'] as num?)?.toDouble(),
      motivationScore: (json['motivationScore'] as num?)?.toDouble(),
      selfEfficacyScore: (json['selfEfficacyScore'] as num?)?.toDouble(),
      socialSupportScore: (json['socialSupportScore'] as num?)?.toDouble(),
      healthLocusScore: (json['healthLocusScore'] as num?)?.toDouble(),
      adherenceRiskScore: (json['adherenceRiskScore'] as num?)?.toDouble(),
      summary: json['summary'] as String?,
      detailedInterpretation: json['detailedInterpretation'] as String?,
      status: json['status'] as String?,
      createdAt: json['createdAt'] as String?,
    );
  }
}

class ProfileCreateRequest {
  final int userId;
  final List<int>? sessionIds;
  final double? anxietyScore;
  final double? depressionScore;
  final double? motivationScore;
  final double? selfEfficacyScore;
  final double? socialSupportScore;
  final double? healthLocusScore;

  ProfileCreateRequest({
    required this.userId,
    this.sessionIds,
    this.anxietyScore,
    this.depressionScore,
    this.motivationScore,
    this.selfEfficacyScore,
    this.socialSupportScore,
    this.healthLocusScore,
  });

  Map<String, dynamic> toJson() => {
    'userId': userId,
    if (sessionIds != null) 'sessionIds': sessionIds,
    if (anxietyScore != null) 'anxietyScore': anxietyScore,
    if (depressionScore != null) 'depressionScore': depressionScore,
    if (motivationScore != null) 'motivationScore': motivationScore,
    if (selfEfficacyScore != null) 'selfEfficacyScore': selfEfficacyScore,
    if (socialSupportScore != null) 'socialSupportScore': socialSupportScore,
    if (healthLocusScore != null) 'healthLocusScore': healthLocusScore,
  };
}
