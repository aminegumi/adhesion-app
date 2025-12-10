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

  static Future<String?> getStoredUserName() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('user_display_name');
  }

  static Future<void> saveUserInfo(
    int userId,
    String displayName,
    String email,
  ) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('user_id', userId);
    await prefs.setString('user_display_name', displayName);
    await prefs.setString('user_email', email);
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

  /// Generate a new prediction for the user based on their profile and adherence data
  Future<PredictionItem> generatePrediction(int userId) async {
    // First get the user's profile score
    double profileScore = 50.0; // default
    double recentAdherenceRate = 0.5; // default

    try {
      final profile = await getLatestProfile(userId);
      if (profile != null) {
        // Calculate average score from profile using available fields
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
      }
    } catch (e) {
      print('Could not get adherence history for prediction: $e');
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
      }),
    );
    if (res.statusCode != 200) {
      throw ApiException(res.statusCode, res.body);
    }
    final map = jsonDecode(res.body) as Map<String, dynamic>;
    return PredictionItem.fromJson(map);
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

  static Future<int?> getStoredUserId() async {
    final prefs = await SharedPreferences.getInstance();
    final id = prefs.getInt('user_id');
    return id;
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
