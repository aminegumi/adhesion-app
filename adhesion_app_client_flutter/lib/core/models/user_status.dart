/// User status DTO - tells the app what the user should do next
class UserStatus {
  final int userId;
  final String displayName;
  final String email;
  final bool onboardingCompleted;
  final bool needsRetake;
  final int requiredTestsCount;
  final int completedTestsCount;
  final int testsRemaining;
  final String? lastTestCompletedAt;
  final String
  nextAction; // TAKE_TESTS, CREATE_PROFILE, RETAKE_TESTS, FULL_ACCESS

  UserStatus({
    required this.userId,
    required this.displayName,
    required this.email,
    required this.onboardingCompleted,
    required this.needsRetake,
    required this.requiredTestsCount,
    required this.completedTestsCount,
    required this.testsRemaining,
    this.lastTestCompletedAt,
    required this.nextAction,
  });

  factory UserStatus.fromJson(Map<String, dynamic> json) => UserStatus(
    userId: json['userId'] as int,
    displayName: json['displayName'] as String? ?? 'User',
    email: json['email'] as String? ?? '',
    onboardingCompleted: json['onboardingCompleted'] as bool? ?? false,
    needsRetake: json['needsRetake'] as bool? ?? false,
    requiredTestsCount: json['requiredTestsCount'] as int? ?? 2,
    completedTestsCount: json['completedTestsCount'] as int? ?? 0,
    testsRemaining: json['testsRemaining'] as int? ?? 2,
    lastTestCompletedAt: json['lastTestCompletedAt'] as String?,
    nextAction: json['nextAction'] as String? ?? 'TAKE_TESTS',
  );

  bool get canAccessFullApp => nextAction == 'FULL_ACCESS';
  bool get needsTests =>
      nextAction == 'TAKE_TESTS' || nextAction == 'RETAKE_TESTS';
  bool get needsProfile => nextAction == 'CREATE_PROFILE';
}
