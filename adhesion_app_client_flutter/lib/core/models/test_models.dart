class TestDto {
  final int? id;
  final String? code;
  final String? title;
  final String? version;
  final bool? active;
  final String? description;
  final List<QuestionDto>? questions;

  TestDto({
    this.id,
    this.code,
    this.title,
    this.version,
    this.active,
    this.description,
    this.questions,
  });

  factory TestDto.fromJson(Map<String, dynamic> json) => TestDto(
    id: json['id'] as int?,
    code: json['code'] as String?,
    title: json['title'] as String?,
    version: json['version'] as String?,
    active: json['active'] as bool?,
    description: json['description'] as String?,
    questions: (json['questions'] as List?)
        ?.map((e) => QuestionDto.fromJson(e as Map<String, dynamic>))
        .toList(),
  );
}

class QuestionDto {
  final int id;
  final String code;
  final String text;
  final int orderIndex;
  final bool reverseScored;
  final int minScore;
  final int maxScore;

  QuestionDto({
    required this.id,
    required this.code,
    required this.text,
    required this.orderIndex,
    required this.reverseScored,
    required this.minScore,
    required this.maxScore,
  });

  factory QuestionDto.fromJson(Map<String, dynamic> json) => QuestionDto(
    id: (json['id'] as num?)?.toInt() ?? 0,
    code: json['code'] as String? ?? '',
    text: json['text'] as String? ?? '',
    orderIndex: (json['orderIndex'] as num?)?.toInt() ?? 0,
    reverseScored: json['reverseScored'] as bool? ?? false,
    minScore: (json['minScore'] as num?)?.toInt() ?? 0,
    maxScore: (json['maxScore'] as num?)?.toInt() ?? 5,
  );
}

class TestSessionDto {
  final int id;
  final int testId;
  final String testCode;
  final String testTitle;
  final String status;
  final List<QuestionDto> questions;

  TestSessionDto({
    required this.id,
    required this.testId,
    required this.testCode,
    required this.testTitle,
    required this.status,
    required this.questions,
  });

  factory TestSessionDto.fromJson(Map<String, dynamic> json) => TestSessionDto(
    id: (json['id'] as num?)?.toInt() ?? 0,
    testId: (json['testId'] as num?)?.toInt() ?? 0,
    testCode: json['testCode'] as String? ?? '',
    testTitle: json['testTitle'] as String? ?? '',
    status: json['status'] as String? ?? 'IN_PROGRESS',
    questions:
        (json['questions'] as List?)
            ?.map((e) => QuestionDto.fromJson(e as Map<String, dynamic>))
            .toList() ??
        [],
  );
}

class AnswerSubmission {
  final int questionId;
  final int score;
  final String? textResponse;

  AnswerSubmission({
    required this.questionId,
    required this.score,
    this.textResponse,
  });

  Map<String, dynamic> toJson() => {
    'questionId': questionId,
    'score': score,
    if (textResponse != null) 'textResponse': textResponse,
  };
}

class TestResultDto {
  final int sessionId;
  final String testCode;
  final String testTitle;
  final int totalScore;
  final String interpretationLevel;
  final String scoreDescription;
  final String clinicalInterpretation;
  final List<String> recommendations;
  final String? submittedAt;

  TestResultDto({
    required this.sessionId,
    required this.testCode,
    required this.testTitle,
    required this.totalScore,
    required this.interpretationLevel,
    required this.scoreDescription,
    required this.clinicalInterpretation,
    required this.recommendations,
    this.submittedAt,
  });

  factory TestResultDto.fromJson(Map<String, dynamic> json) => TestResultDto(
    sessionId: (json['sessionId'] as num?)?.toInt() ?? 0,
    testCode: json['testCode'] as String? ?? '',
    testTitle: json['testTitle'] as String? ?? '',
    totalScore: (json['totalScore'] as num?)?.toInt() ?? 0,
    interpretationLevel: json['interpretationLevel'] as String? ?? '',
    scoreDescription: json['scoreDescription'] as String? ?? '',
    clinicalInterpretation: json['clinicalInterpretation'] as String? ?? '',
    recommendations:
        (json['recommendations'] as List?)?.map((e) => e.toString()).toList() ??
        [],
    submittedAt: json['submittedAt']?.toString(),
  );
}
