import 'dart:convert';
import 'package:http/http.dart' as http;
import 'models/login_models.dart';
import 'models/register_models.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'models/dashboard_models.dart';
import 'models/test_models.dart';
import 'models/profile_models.dart';
import 'models/recommendation_models.dart';
import 'models/treatment_models.dart';
import 'models/ai_models.dart';
import 'models/user_status.dart';
import 'models/dose_models.dart';
import '../features/medications/models/user_medication.dart';

class ApiException implements Exception {
  final int status;
  final String message;
  ApiException(this.status, this.message);
  @override
  String toString() => 'ApiException($status): $message';
}

class ApiClient {
  final String baseUrl;
  final http.Client _client;

  ApiClient({required this.baseUrl, http.Client? client})
    : _client = client ?? http.Client() {
    // debug
    // print('ApiClient baseUrl: $baseUrl');
  }

  Future<LoginResponse> login(String email, String password) async {
    final uri = Uri.parse('$baseUrl/api/auth/login');
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'email': email, 'password': password}),
    );
    // debug print
    print('Login ${res.statusCode}: ${res.body}');
    if (res.statusCode != 200) {
      final body = res.body.isNotEmpty ? res.body : 'Login failed';
      throw ApiException(res.statusCode, body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return LoginResponse.fromJson(map);
  }

  Future<User> register(RegisterRequest req) async {
    final uri = Uri.parse('$baseUrl/api/auth/register');
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(req.toJson()),
    );
    if (res.statusCode != 200) {
      throw ApiException(
        res.statusCode,
        res.body.isNotEmpty ? res.body : 'Register failed',
      );
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return User.fromJson(map);
  }

  // ==================== USER STATUS API ====================

  Future<UserStatus> getUserStatus(int userId) async {
    final uri = Uri.parse('$baseUrl/api/users/$userId/status');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return UserStatus.fromJson(map);
  }

  /// Get user profile by ID
  Future<User> getUserProfile(int userId) async {
    final uri = Uri.parse('$baseUrl/api/users/$userId');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return User.fromJson(map);
  }

  /// Update user profile
  Future<User> updateUserProfile(
    int userId, {
    String? displayName,
    String? email,
    String? gender,
    String? birthDate,
    bool? consentGiven,
  }) async {
    final uri = Uri.parse('$baseUrl/api/users/$userId');
    final body = <String, dynamic>{};
    if (displayName != null) body['displayName'] = displayName;
    if (email != null) body['email'] = email;
    if (gender != null) body['gender'] = gender;
    if (birthDate != null) body['birthDate'] = birthDate;
    if (consentGiven != null) body['consentGiven'] = consentGiven;
    
    final res = await _client.put(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return User.fromJson(map);
  }

  static Future<String?> getStoredUserName() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('user_display_name');
  }

  static Future<int?> getStoredUserId() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getInt('user_id');
  }

  static Future<void> saveUserInfo(
    int userId,
    String displayName,
    String email, {
    String? role,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('user_id', userId);
    await prefs.setString('user_display_name', displayName);
    await prefs.setString('user_email', email);
    if (role != null) {
      await prefs.setString('user_role', role);
    }
  }

  Future<List<AdherenceSummary>> getAdherenceHistory({
    required int userId,
    required String from,
    required String to,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/api/adherence/user/$userId?from=$from&to=$to',
    );
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => AdherenceSummary.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  /// Get enhanced adherence prediction using multi-factor evidence-based model
  Future<AdherencePrediction> getEnhancedPrediction(int userId) async {
    final uri = Uri.parse('$baseUrl/api/predictions/adherence/$userId');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return AdherencePrediction.fromJson(map);
  }

  Future<List<PredictionItem>> getPredictionsHistory(int userId) async {
    final uri = Uri.parse('$baseUrl/api/predictions/user/$userId');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => PredictionItem.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  /// Generate a new prediction for the user based on multiple factors:
  /// profile scores, adherence history, test results, and treatment plan progress
  Future<PredictionItem> generatePrediction(int userId) async {
    // Initialize default values
    double profileScore = 50.0;
    double recentAdherenceRate = 0.5;
    double testCompletionRate = 0.0;
    double averageTestScore = 50.0;
    double planProgressRate = 0.0;
    int activePlansCount = 0;
    int daysStreak = 0;

    // Get profile score
    try {
      final profile = await getLatestProfile(userId);
      if (profile != null) {
        profileScore =
            ((profile.motivationScore ?? 50) +
                (profile.selfEfficacyScore ?? 50) +
                (100 - (profile.anxietyScore ?? 50)) +
                (100 - (profile.depressionScore ?? 50))) /
            4;
      }
    } catch (e) {
      print('Could not get profile for prediction: $e');
    }

    // Get adherence history
    try {
      final now = DateTime.now();
      final from =
          '${now.subtract(const Duration(days: 30)).year}-${now.subtract(const Duration(days: 30)).month.toString().padLeft(2, '0')}-${now.subtract(const Duration(days: 30)).day.toString().padLeft(2, '0')}';
      final to =
          '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';
      final history = await getAdherenceHistory(
        userId: userId,
        from: from,
        to: to,
      );
      if (history.isNotEmpty) {
        recentAdherenceRate =
            history.map((h) => h.adherenceScore).reduce((a, b) => a + b) /
            history.length /
            100;
        
        // Calculate streak from consecutive days with good adherence
        int streak = 0;
        for (final h in history.reversed) {
          if (h.adherenceScore >= 70) {
            streak++;
          } else {
            break;
          }
        }
        daysStreak = streak;
      }
    } catch (e) {
      print('Could not get adherence history for prediction: $e');
    }

    // Get test history for test completion rate and average score
    try {
      final testHistory = await getTestHistory(userId);
      if (testHistory.isNotEmpty) {
        // Assuming user should complete at least 4 core tests (PHQ, GAD, BMQ, MMAS)
        testCompletionRate = (testHistory.length / 4.0).clamp(0.0, 1.0);
        
        // Calculate average score from test results
        double totalScore = 0;
        int scoreCount = 0;
        for (final result in testHistory) {
          if (result.totalScore != null) {
            // Normalize score to 0-100 (higher is better for adherence)
            // Most tests: lower score = less severe symptoms = better
            double normalizedScore = 100.0 - (result.totalScore! * 5).clamp(0, 100).toDouble();
            totalScore += normalizedScore;
            scoreCount++;
          }
        }
        if (scoreCount > 0) {
          averageTestScore = totalScore / scoreCount;
        }
      }
    } catch (e) {
      print('Could not get test history for prediction: $e');
    }

    // Get treatment plan progress
    try {
      final plans = await getUserTreatmentPlans(userId);
      if (plans.isNotEmpty) {
        final activePlans = plans.where((p) => p.status == 'ACTIVE').toList();
        activePlansCount = activePlans.length;
        
        // Calculate average progress of active plans
        if (activePlans.isNotEmpty) {
          double totalProgress = 0;
          for (final plan in activePlans) {
            // Use the progressPercentage field from the plan
            totalProgress += plan.progressPercentage / 100.0;
          }
          planProgressRate = totalProgress / activePlans.length;
        }
      }
    } catch (e) {
      print('Could not get treatment plans for prediction: $e');
    }

    final uri = Uri.parse('$baseUrl/api/predictions');
    final now = DateTime.now();
    final dateStr =
        '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';

    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'userId': userId,
        'date': dateStr,
        'profileScore': profileScore,
        'recentAdherenceRate': recentAdherenceRate,
        'testCompletionRate': testCompletionRate,
        'averageTestScore': averageTestScore,
        'planProgressRate': planProgressRate,
        'activePlansCount': activePlansCount,
        'daysStreak': daysStreak,
      }),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return PredictionItem.fromJson(map);
  }

  /// Delete a prediction by ID
  Future<void> deletePrediction(int predictionId) async {
    final uri = Uri.parse('$baseUrl/api/predictions/$predictionId');
    final res = await _client.delete(uri);
    if (res.statusCode != 200 && res.statusCode != 204) {
      throw ApiException(res.statusCode, res.body);
    }
  }

  Future<int> getPlansCount(int userId) async {
    final uri = Uri.parse('$baseUrl/api/plans/user/$userId');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = jsonDecode(res.body) as List;
    return list.length;
  }

  Future<List<TestDto>> getTests() async {
    final uri = Uri.parse('$baseUrl/api/tests');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(
        res.statusCode,
        res.body.isNotEmpty ? res.body : 'Failed to load tests',
      );
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => TestDto.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  // ==================== ADMIN APIs ====================

  /// Get all users (admin only)
  Future<List<User>> getAllUsers() async {
    final uri = Uri.parse('$baseUrl/api/admin/users');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => User.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  /// Get users who have given data sharing consent
  Future<List<User>> getConsentedUsers() async {
    final uri = Uri.parse('$baseUrl/api/admin/users/consented');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => User.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  /// Get test results for a specific user (admin endpoint)
  Future<List<TestResultDto>> getUserTestResults(int userId) async {
    final uri = Uri.parse('$baseUrl/api/admin/users/$userId/test-results');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => TestResultDto.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  /// Get treatment plans for a specific user (admin endpoint)
  Future<List<TreatmentPlan>> getUserTreatmentPlansAdmin(int userId) async {
    final uri = Uri.parse('$baseUrl/api/admin/users/$userId/treatment-plans');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => TreatmentPlan.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  /// Create a new admin user
  Future<User> createAdmin({
    required String email,
    required String password,
    required String displayName,
    String? birthDate,
    String? gender,
  }) async {
    final uri = Uri.parse('$baseUrl/api/admin/create');
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'email': email,
        'password': password,
        'displayName': displayName,
        if (birthDate != null) 'birthDate': birthDate,
        if (gender != null) 'gender': gender,
      }),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return User.fromJson(map);
  }

  /// Create a new test
  Future<TestDto> createTest(Map<String, dynamic> testData) async {
    final uri = Uri.parse('$baseUrl/api/tests');
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(testData),
    );
    if (res.statusCode != 200 && res.statusCode != 201) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return TestDto.fromJson(map);
  }

  /// Update an existing test
  Future<TestDto> updateTest(int testId, Map<String, dynamic> testData) async {
    final uri = Uri.parse('$baseUrl/api/tests/$testId');
    final res = await _client.put(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(testData),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return TestDto.fromJson(map);
  }

  /// Delete a test
  Future<void> deleteTest(int testId) async {
    final uri = Uri.parse('$baseUrl/api/tests/$testId');
    final res = await _client.delete(uri);
    if (res.statusCode != 200 && res.statusCode != 204) {
      throw ApiException(res.statusCode, res.body);
    }
  }

  Future<TestDto> getTestWithQuestions(int testId) async {
    final uri = Uri.parse('$baseUrl/api/tests/$testId');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return TestDto.fromJson(map);
  }

  Future<TestSessionDto> startTestSession(int testId, int userId) async {
    final uri = Uri.parse('$baseUrl/api/sessions/start');
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'testId': testId, 'userId': userId}),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return TestSessionDto.fromJson(map);
  }

  Future<TestResultDto> submitTestAnswers(
    int sessionId,
    List<AnswerSubmission> answers,
  ) async {
    final uri = Uri.parse('$baseUrl/api/sessions/$sessionId/submit');
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(answers.map((a) => a.toJson()).toList()),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return TestResultDto.fromJson(map);
  }

  Future<List<TestResultDto>> getTestHistory(int userId) async {
    final uri = Uri.parse('$baseUrl/api/sessions/patient/$userId/history');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => TestResultDto.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  // ==================== PSYCHOLOGICAL PROFILE APIs ====================

  Future<PsychologicalProfile?> getLatestProfile(int userId) async {
    final uri = Uri.parse('$baseUrl/api/profiles/user/$userId/latest');
    final res = await _client.get(uri);
    if (res.statusCode == 404) {
      return null;
    }
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return PsychologicalProfile.fromJson(map);
  }

  Future<List<PsychologicalProfile>> getUserProfiles(int userId) async {
    final uri = Uri.parse('$baseUrl/api/profiles/user/$userId');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => PsychologicalProfile.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  Future<PsychologicalProfile> createProfile(
    ProfileCreateRequest request,
  ) async {
    final uri = Uri.parse('$baseUrl/api/profiles');
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(request.toJson()),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return PsychologicalProfile.fromJson(map);
  }

  // ==================== RECOMMENDATION APIs ====================

  Future<List<Recommendation>> getUserRecommendations(int userId) async {
    final uri = Uri.parse('$baseUrl/api/recommendations/user/$userId');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => Recommendation.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  /// Alias for getUserRecommendations - gets all recommendations for a user
  Future<List<Recommendation>> getAllRecommendations(int userId) async {
    return getUserRecommendations(userId);
  }

  Future<List<Recommendation>> getActiveRecommendations(int userId) async {
    final uri = Uri.parse('$baseUrl/api/recommendations/user/$userId/active');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => Recommendation.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  Future<List<Recommendation>> generateRecommendations(int userId) async {
    final uri = Uri.parse('$baseUrl/api/recommendations/generate/$userId');
    final res = await _client.post(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => Recommendation.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  Future<Recommendation> completeRecommendation(
    int id, {
    String? feedback,
  }) async {
    var uriStr = '$baseUrl/api/recommendations/$id/complete';
    if (feedback != null) {
      uriStr += '?feedback=${Uri.encodeComponent(feedback)}';
    }
    final uri = Uri.parse(uriStr);
    final res = await _client.post(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return Recommendation.fromJson(map);
  }

  // ==================== TREATMENT PLAN APIs ====================

  Future<List<TreatmentPlan>> getUserTreatmentPlans(int userId) async {
    final uri = Uri.parse('$baseUrl/api/treatment-plans/user/$userId');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => TreatmentPlan.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  Future<TreatmentPlan> createTreatmentPlan(CreatePlanRequest request) async {
    final uri = Uri.parse('$baseUrl/api/treatment-plans');
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(request.toJson()),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return TreatmentPlan.fromJson(map);
  }

  Future<TreatmentPlan> updateTreatmentPlan(int planId, CreatePlanRequest request) async {
    final uri = Uri.parse('$baseUrl/api/treatment-plans/$planId');
    final res = await _client.put(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(request.toJson()),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return TreatmentPlan.fromJson(map);
  }

  Future<void> deleteTreatmentPlan(int planId) async {
    final uri = Uri.parse('$baseUrl/api/treatment-plans/$planId');
    final res = await _client.delete(uri);
    if (res.statusCode != 200 && res.statusCode != 204) {
      throw ApiException(res.statusCode, res.body);
    }
  }

  Future<List<DailyTask>> getTodaysTasks(int userId) async {
    final uri = Uri.parse(
      '$baseUrl/api/treatment-plans/tasks/user/$userId/today',
    );
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => DailyTask.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  Future<DailyTask> completeTask(int taskId, {String? notes}) async {
    var uriStr = '$baseUrl/api/treatment-plans/tasks/$taskId/complete';
    if (notes != null) {
      uriStr += '?notes=${Uri.encodeComponent(notes)}';
    }
    final uri = Uri.parse(uriStr);
    final res = await _client.post(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return DailyTask.fromJson(map);
  }

  Future<List<UserMedication>> syncMedicationsFromTreatmentPlan(int planId, int userId) async {
    final uri = Uri.parse('$baseUrl/api/treatment-plans/$planId/sync-medications?userId=$userId');
    final res = await _client.post(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = (jsonDecode(res.body) as List)
        .map((e) => UserMedication.fromJson(e as Map<String, dynamic>))
        .toList();
    return list;
  }

  // ==================== AI APIs ====================

  Future<MotivationResponse> getMotivation(
    int userId, {
    double? adherenceScore,
  }) async {
    final uri = Uri.parse('$baseUrl/api/ai/motivation');
    final body = {
      'userId': userId,
      if (adherenceScore != null) 'adherenceScore': adherenceScore,
    };
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return MotivationResponse.fromJson(map);
  }

  Future<MotivationResponse> askAI(
    int userId,
    String userMessage,
    List<ChatMessage> conversationHistory, {
    double? adherenceScore,
  }) async {
    final uri = Uri.parse('$baseUrl/api/ai/motivation');
    final body = {
      'userId': userId,
      'userMessage': userMessage,
      'conversationHistory': conversationHistory.map((msg) => {
        'role': msg.role,
        'content': msg.content,
        'timestamp': msg.timestamp.millisecondsSinceEpoch,
      }).toList(),
      if (adherenceScore != null) 'adherenceScore': adherenceScore,
    };
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return MotivationResponse.fromJson(map);
  }

  Future<RecommendationResponse> getAIRecommendations(
    int userId, {
    double? riskScore,
  }) async {
    final uri = Uri.parse('$baseUrl/api/ai/recommendations');
    final body = {
      'userId': userId,
      if (riskScore != null) 'nonAdherenceRisk': riskScore,
    };
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return RecommendationResponse.fromJson(map);
  }

  /// Detect emotions from user's text input
  Future<EmotionResult> detectEmotions(int userId, String text) async {
    final uri = Uri.parse('$baseUrl/api/ai/detect-emotions');
    final body = {
      'userId': userId,
      'text': text,
    };
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return EmotionResult.fromJson(map);
  }

  /// Analyze facial emotion from image
  Future<FacialEmotionResult> analyzeFacialEmotion(int userId, List<int> imageBytes) async {
    final uri = Uri.parse('$baseUrl/api/ai/analyze-facial-emotion');
    final request = http.MultipartRequest('POST', uri)
      ..fields['userId'] = userId.toString()
      ..files.add(http.MultipartFile.fromBytes('image', imageBytes, filename: 'image.jpg'));
    final streamedResponse = await request.send();
    final res = await http.Response.fromStream(streamedResponse);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return FacialEmotionResult.fromJson(map);
  }

  /// Send a conversational message to the health assistant
  Future<MotivationResponse> sendMotivationMessage(
    int userId,
    String userMessage,
    double? adherenceScore,
    List<Map<String, dynamic>> conversationHistory,
  ) async {
    final uri = Uri.parse('$baseUrl/api/ai/motivation');
    final body = {
      'userId': userId,
      'userMessage': userMessage,
      'adherenceScore': adherenceScore,
      'conversationHistory': conversationHistory,
    };
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return MotivationResponse.fromJson(map);
  }

  /// Get adherence score for a user
  Future<double> getAdherenceScore(int userId) async {
    final uri = Uri.parse('$baseUrl/api/doses/user/$userId/stats');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    // Extract overallAdherenceRate from the AdherenceStatsDto
    return (map['overallAdherenceRate'] as num?)?.toDouble() ?? 0.0;
  }

  // ==================== Dose Tracking APIs ====================

  /// Get today's doses for a user
  Future<List<DoseLog>> getTodaysDoses(int userId) async {
    final uri = Uri.parse('$baseUrl/api/doses/user/$userId/today');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = jsonDecode(res.body) as List;
    return list
        .map((e) => DoseLog.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Get doses for a specific date
  Future<List<DoseLog>> getDosesForDate(int userId, DateTime date) async {
    final dateStr =
        '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
    final uri = Uri.parse('$baseUrl/api/doses/user/$userId/date/$dateStr');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = jsonDecode(res.body) as List;
    return list
        .map((e) => DoseLog.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Get doses for a date range
  Future<List<DoseLog>> getDosesForRange(
    int userId,
    DateTime from,
    DateTime to,
  ) async {
    final fromStr =
        '${from.year}-${from.month.toString().padLeft(2, '0')}-${from.day.toString().padLeft(2, '0')}';
    final toStr =
        '${to.year}-${to.month.toString().padLeft(2, '0')}-${to.day.toString().padLeft(2, '0')}';
    final uri = Uri.parse(
      '$baseUrl/api/doses/user/$userId/range?from=$fromStr&to=$toStr',
    );
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final list = jsonDecode(res.body) as List;
    return list
        .map((e) => DoseLog.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Mark a dose as taken
  Future<DoseLog> takeDose(int doseId, {String? notes}) async {
    final uri = Uri.parse(
      '$baseUrl/api/doses/$doseId/take${notes != null ? '?notes=${Uri.encodeComponent(notes)}' : ''}',
    );
    final res = await _client.post(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    return DoseLog.fromJson(jsonDecode(res.body) as Map<String, dynamic>);
  }

  /// Mark a dose as skipped
  Future<DoseLog> skipDose(
    int doseId,
    SkipReason reason, {
    String? notes,
  }) async {
    final uri = Uri.parse('$baseUrl/api/doses/$doseId/action');
    final res = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'action': 'skip',
        'skipReason': reason.name.toUpperCase(),
        'notes': notes,
      }),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    return DoseLog.fromJson(jsonDecode(res.body) as Map<String, dynamic>);
  }

  /// Get adherence statistics
  Future<AdherenceStats> getAdherenceStats(int userId) async {
    final uri = Uri.parse('$baseUrl/api/doses/user/$userId/stats');
    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    return AdherenceStats.fromJson(
      jsonDecode(res.body) as Map<String, dynamic>,
    );
  }

  // ==================== STATIC HTTP METHODS ====================

  static const String _defaultBaseUrl = 'http://localhost:8082';

  /// Static GET request
  static Future<http.Response> get(String endpoint) async {
    final uri = Uri.parse('$_defaultBaseUrl$endpoint');
    final res = await http.get(uri);
    if (res.statusCode >= 400) {
      throw ApiException(res.statusCode, res.body);
    }
    return res;
  }

  /// Static POST request
  static Future<http.Response> post(String endpoint, dynamic body) async {
    final uri = Uri.parse('$_defaultBaseUrl$endpoint');
    final res = await http.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    if (res.statusCode >= 400) {
      throw ApiException(res.statusCode, res.body);
    }
    return res;
  }

  /// Static PUT request
  static Future<http.Response> put(String endpoint, dynamic body) async {
    final uri = Uri.parse('$_defaultBaseUrl$endpoint');
    final res = await http.put(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    if (res.statusCode >= 400) {
      throw ApiException(res.statusCode, res.body);
    }
    return res;
  }

  /// Static PATCH request
  static Future<http.Response> patch(String endpoint, dynamic body) async {
    final uri = Uri.parse('$_defaultBaseUrl$endpoint');
    final res = await http.patch(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    if (res.statusCode >= 400) {
      throw ApiException(res.statusCode, res.body);
    }
    return res;
  }

  /// Static DELETE request
  static Future<http.Response> delete(String endpoint) async {
    final uri = Uri.parse('$_defaultBaseUrl$endpoint');
    final res = await http.delete(uri);
    if (res.statusCode >= 400) {
      throw ApiException(res.statusCode, res.body);
    }
    return res;
  }

  void close() => _client.close();
}
