// Recommendation Models

class Recommendation {
  final int id;
  final int userId;
  final int? profileId;
  final String category;
  final int priority;
  final String title;
  final String? description;
  final String? actionableSteps;
  final String? expectedBenefits;
  final String? timeFrame;
  final String? difficulty;
  final String? status;
  final bool completed;
  final String? createdAt;
  final String? completedAt;

  Recommendation({
    required this.id,
    required this.userId,
    this.profileId,
    required this.category,
    required this.priority,
    required this.title,
    this.description,
    this.actionableSteps,
    this.expectedBenefits,
    this.timeFrame,
    this.difficulty,
    this.status,
    required this.completed,
    this.createdAt,
    this.completedAt,
  });

  factory Recommendation.fromJson(Map<String, dynamic> json) {
    return Recommendation(
      id: json['id'] as int,
      userId: json['userId'] as int,
      profileId: json['profileId'] as int?,
      category: json['category'] as String? ?? 'General',
      priority: json['priority'] as int? ?? 1,
      title: json['title'] as String,
      description: json['description'] as String?,
      actionableSteps: json['actionableSteps'] as String?,
      expectedBenefits: json['expectedBenefits'] as String?,
      timeFrame: json['timeFrame'] as String?,
      difficulty: json['difficulty'] as String?,
      status: json['status'] as String?,
      completed: json['completed'] as bool? ?? false,
      createdAt: json['createdAt'] as String?,
      completedAt: json['completedAt'] as String?,
    );
  }
}
