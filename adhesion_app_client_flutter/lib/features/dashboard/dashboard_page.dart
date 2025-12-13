import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../core/api_client.dart';
import '../../core/models/dashboard_models.dart';
import '../tests/tests_page.dart';
import '../profile/profile_page.dart';
import '../recommendations/recommendations_page.dart';
import '../motivation/motivation_page.dart';
import '../motivation/get_motivation_page.dart';
import '../treatment/treatment_plan_page.dart';
import '../history/history_page.dart';
import '../predictions/predictions_page.dart';
import '../insights/adherence_insights_page.dart';
import '../auth/login_page.dart';
import '../doses/todays_doses_page.dart';
import '../doses/adherence_stats_page.dart';
import '../medications/pages/my_medications_page.dart';
import '../emotions/combined_emotion_page.dart';

class DashboardPage extends StatefulWidget {
  final String baseUrl;
  final String? userName;
  const DashboardPage({super.key, required this.baseUrl, this.userName});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> with SingleTickerProviderStateMixin {
  late final ApiClient _api;
  late AnimationController _animationController;
  Future<_DashboardData>? _future;
  String _userName = 'User';
  int _currentIndex = 0;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _userName = widget.userName ?? 'User';
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    )..forward();
    _future = _load();
  }

  @override
  void dispose() {
    _animationController.dispose();
    _api.close();
    super.dispose();
  }

  Future<_DashboardData> _load() async {
    final userId = await ApiClient.getStoredUserId();

    if (widget.userName == null) {
      final storedName = await ApiClient.getStoredUserName();
      if (storedName != null && mounted) {
        setState(() => _userName = storedName);
      }
    }

    if (userId == null) {
      return _DashboardData.empty();
    }

    final now = DateTime.now();
    final to = _fmtDate(now);
    final from = _fmtDate(now.subtract(const Duration(days: 30)));

    List<AdherenceSummary> history = [];
    List<PredictionItem> preds = [];
    int plansCount = 0;
    int activePlansCount = 0;
    int completedTests = 0;
    double? adherenceRate;

    try {
      history = await _api.getAdherenceHistory(userId: userId, from: from, to: to);
    } catch (e) {
      print('Failed to load adherence history: $e');
    }

    try {
      preds = await _api.getPredictionsHistory(userId);
    } catch (e) {
      print('Failed to load predictions: $e');
    }

    try {
      final plans = await _api.getUserTreatmentPlans(userId);
      plansCount = plans.length;
      activePlansCount = plans.where((p) => p.status == 'ACTIVE').length;
    } catch (e) {
      print('Failed to load plans: $e');
    }

    try {
      final testHistory = await _api.getTestHistory(userId);
      completedTests = testHistory.length;
    } catch (e) {
      print('Failed to load test history: $e');
    }

    try {
      adherenceRate = await _api.getAdherenceScore(userId);
    } catch (e) {
      print('Failed to load adherence rate: $e');
    }

    final latestScore = history.isNotEmpty ? history.last.adherenceScore : null;
    final latestPred = preds.isNotEmpty ? preds.last.probNonAdherence : null;

    // Calculate wellness score based on multiple factors
    final wellnessScore = _calculateWellnessScore(
      adherenceScore: latestScore ?? adherenceRate,
      predictionRisk: latestPred,
      testsCompleted: completedTests,
      activePlans: activePlansCount,
    );

    return _DashboardData(
      displayName: _userName,
      adherenceScore: latestScore ?? adherenceRate,
      lastPrediction: latestPred,
      completedTests: completedTests,
      plansCount: plansCount,
      activePlansCount: activePlansCount,
      wellnessScore: wellnessScore,
      history: history.reversed.toList(),
      predictions: preds.reversed.toList(),
    );
  }

  double _calculateWellnessScore({
    double? adherenceScore,
    double? predictionRisk,
    int testsCompleted = 0,
    int activePlans = 0,
  }) {
    // Start from 0, not a base score
    double score = 0.0;
    int factors = 0;

    // Adherence score contributes (if available)
    if (adherenceScore != null && adherenceScore > 0) {
      score += adherenceScore; // Already 0-100
      factors++;
    }

    // Prediction risk (inverse) - lower risk = higher wellness
    if (predictionRisk != null) {
      score += (1 - predictionRisk) * 100;
      factors++;
    }

    // Tests completed factor (each test adds value, max at 5)
    if (testsCompleted > 0) {
      final testScore = (testsCompleted.clamp(0, 5) / 5) * 100;
      score += testScore;
      factors++;
    }

    // Active plans factor
    if (activePlans > 0) {
      score += 80; // Having active plans is positive
      factors++;
    }

    // Calculate average, default to 50 if no data
    if (factors == 0) {
      return 50.0; // Default when no data available
    }

    return (score / factors).clamp(0, 100);
  }

  String _fmtDate(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  void _logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => LoginPage(baseUrl: widget.baseUrl)),
      (route) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      body: SafeArea(
        child: FutureBuilder<_DashboardData>(
          future: _future,
          builder: (context, snap) {
            if (snap.connectionState != ConnectionState.done) {
              return const Center(
                child: CircularProgressIndicator(color: Color(0xFF6366F1)),
              );
            }
            if (snap.hasError) {
              return Center(child: Text('Failed to load: ${snap.error}'));
            }
            final data = snap.data ?? _DashboardData.empty();
            return RefreshIndicator(
              onRefresh: () async {
                setState(() => _future = _load());
                await _future;
              },
              color: const Color(0xFF6366F1),
              child: CustomScrollView(
                slivers: [
                  SliverToBoxAdapter(child: _buildHeader(data)),
                  SliverToBoxAdapter(child: _buildQuickStats(data)),
                  SliverToBoxAdapter(child: _buildQuickActions()),
                  SliverToBoxAdapter(child: _buildFeatureCards()),
                  SliverToBoxAdapter(child: _buildRecentActivity(data)),
                  const SliverToBoxAdapter(child: SizedBox(height: 100)),
                ],
              ),
            );
          },
        ),
      ),
      drawer: _buildDrawer(),
      bottomNavigationBar: _buildBottomNav(),
    );
  }

  Widget _buildHeader(_DashboardData data) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFA855F7)],
        ),
        borderRadius: BorderRadius.only(
          bottomLeft: Radius.circular(32),
          bottomRight: Radius.circular(32),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Builder(
                builder: (context) => GestureDetector(
                  onTap: () => Scaffold.of(context).openDrawer(),
                  child: Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: const Icon(Icons.menu_rounded, color: Colors.white),
                  ),
                ),
              ),
              const Spacer(),
              GestureDetector(
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => ProfilePage(baseUrl: widget.baseUrl)),
                ),
                child: Container(
                  padding: const EdgeInsets.all(3),
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white, width: 2),
                  ),
                  child: CircleAvatar(
                    radius: 20,
                    backgroundColor: Colors.white.withOpacity(0.3),
                    child: Text(
                      _userName.isNotEmpty ? _userName[0].toUpperCase() : 'U',
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          Text(
            _getGreeting(),
            style: TextStyle(
              color: Colors.white.withOpacity(0.8),
              fontSize: 16,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            _userName,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 28,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 20),
          // Wellness Score Card
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.15),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: Colors.white.withOpacity(0.2)),
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Icon(Icons.favorite_rounded, color: Colors.white, size: 28),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Wellness Score',
                        style: TextStyle(color: Colors.white70, fontSize: 13),
                      ),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          Text(
                            '${data.wellnessScore.toStringAsFixed(0)}',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 32,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          Text(
                            '/100',
                            style: TextStyle(
                              color: Colors.white.withOpacity(0.6),
                              fontSize: 16,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                _buildWellnessIndicator(data.wellnessScore),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildWellnessIndicator(double score) {
    Color color;
    IconData icon;
    String label;

    if (score >= 75) {
      color = const Color(0xFF10B981);
      icon = Icons.sentiment_very_satisfied_rounded;
      label = 'Great';
    } else if (score >= 50) {
      color = const Color(0xFFF59E0B);
      icon = Icons.sentiment_satisfied_rounded;
      label = 'Good';
    } else if (score >= 25) {
      color = const Color(0xFFF97316);
      icon = Icons.sentiment_neutral_rounded;
      label = 'Fair';
    } else {
      color = const Color(0xFFEF4444);
      icon = Icons.sentiment_dissatisfied_rounded;
      label = 'Needs Work';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: color.withOpacity(0.2),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        children: [
          Icon(icon, color: Colors.white, size: 24),
          const SizedBox(height: 4),
          Text(
            label,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 11,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }

  String _getGreeting() {
    final hour = DateTime.now().hour;
    if (hour < 12) return 'Good Morning 🌅';
    if (hour < 17) return 'Good Afternoon ☀️';
    return 'Good Evening 🌙';
  }

  Widget _buildQuickStats(_DashboardData data) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Overview',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: Color(0xFF1F2937),
            ),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _buildStatCard(
                  icon: Icons.psychology_rounded,
                  title: 'Tests',
                  value: '${data.completedTests}',
                  color: const Color(0xFF8B5CF6),
                  subtitle: 'Completed',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildStatCard(
                  icon: Icons.assignment_rounded,
                  title: 'Plans',
                  value: '${data.activePlansCount}',
                  color: const Color(0xFF10B981),
                  subtitle: 'Active',
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard({
    required IconData icon,
    required String title,
    required String value,
    required Color color,
    required String subtitle,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: color, size: 24),
          ),
          const SizedBox(height: 14),
          Text(
            value,
            style: TextStyle(
              fontSize: 28,
              fontWeight: FontWeight.w800,
              color: color,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '$title $subtitle',
            style: const TextStyle(
              color: Color(0xFF6B7280),
              fontSize: 13,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuickActions() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Quick Actions',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: Color(0xFF1F2937),
            ),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _buildActionButton(
                  icon: Icons.auto_awesome_rounded,
                  label: 'Get Motivated',
                  gradient: [const Color(0xFFF59E0B), const Color(0xFFFBBF24)],
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => GetMotivationPage(baseUrl: widget.baseUrl)),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildActionButton(
                  icon: Icons.science_rounded,
                  label: 'Take Test',
                  gradient: [const Color(0xFF6366F1), const Color(0xFF8B5CF6)],
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => TestsPage(baseUrl: widget.baseUrl)),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _buildActionButton(
                  icon: Icons.spa_rounded,
                  label: 'AI Assistant',
                  gradient: [const Color(0xFF4FD1C5), const Color(0xFF38B2AC)],
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => MotivationPage(baseUrl: widget.baseUrl)),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildActionButton(
                  icon: Icons.assignment_rounded,
                  label: 'Action Plan',
                  gradient: [const Color(0xFF8B5CF6), const Color(0xFFA855F7)],
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => TreatmentPlanPage(baseUrl: widget.baseUrl)),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildActionButton({
    required IconData icon,
    required String label,
    required List<Color> gradient,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          gradient: LinearGradient(colors: gradient),
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: gradient[0].withOpacity(0.3),
              blurRadius: 12,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: Colors.white, size: 22),
            const SizedBox(width: 10),
            Text(
              label,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 15,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFeatureCards() {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Health Features',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: Color(0xFF1F2937),
            ),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _buildFeatureCard(
                  icon: Icons.face_rounded,
                  title: 'Emotions',
                  subtitle: 'AI Detection',
                  color: const Color(0xFFA855F7),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => CombinedEmotionPage(baseUrl: widget.baseUrl)),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildFeatureCard(
                  icon: Icons.insights_rounded,
                  title: 'Predictions',
                  subtitle: 'AI Analysis',
                  color: const Color(0xFF0EA5E9),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => PredictionsPage(baseUrl: widget.baseUrl)),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _buildFeatureCard(
                  icon: Icons.medication_rounded,
                  title: 'Medications',
                  subtitle: 'My Meds',
                  color: const Color(0xFF10B981),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const MyMedicationsPage()),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildFeatureCard(
                  icon: Icons.lightbulb_rounded,
                  title: 'Tips',
                  subtitle: 'Recommendations',
                  color: const Color(0xFFF59E0B),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => RecommendationsPage(baseUrl: widget.baseUrl)),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildFeatureCard({
    required IconData icon,
    required String title,
    required String subtitle,
    required Color color,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: color.withOpacity(0.2)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.04),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: color, size: 24),
            ),
            const SizedBox(height: 14),
            Text(
              title,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: Color(0xFF1F2937),
              ),
            ),
            const SizedBox(height: 2),
            Text(
              subtitle,
              style: const TextStyle(
                color: Color(0xFF9CA3AF),
                fontSize: 12,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRecentActivity(_DashboardData data) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Recent Activity',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF1F2937),
                ),
              ),
              TextButton(
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => HistoryPage(baseUrl: widget.baseUrl)),
                ),
                child: const Text('See All'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (data.history.isEmpty && data.predictions.isEmpty)
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: const Color(0xFFE5E7EB)),
              ),
              child: const Column(
                children: [
                  Icon(Icons.history, size: 48, color: Color(0xFFD1D5DB)),
                  SizedBox(height: 12),
                  Text(
                    'No activity yet',
                    style: TextStyle(
                      color: Color(0xFF6B7280),
                      fontSize: 15,
                    ),
                  ),
                  Text(
                    'Take a test to get started',
                    style: TextStyle(
                      color: Color(0xFF9CA3AF),
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            )
          else
            ...data.predictions.take(3).map((p) => _buildActivityItem(
              icon: Icons.insights_rounded,
              title: 'AI Prediction',
              subtitle: '${(p.probNonAdherence * 100).toStringAsFixed(0)}% risk',
              date: p.date,
              color: const Color(0xFF0EA5E9),
            )),
        ],
      ),
    );
  }

  Widget _buildActivityItem({
    required IconData icon,
    required String title,
    required String subtitle,
    required String date,
    required Color color,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFE5E7EB)),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: color, size: 20),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
                Text(subtitle, style: const TextStyle(color: Color(0xFF6B7280), fontSize: 13)),
              ],
            ),
          ),
          Text(date, style: const TextStyle(color: Color(0xFF9CA3AF), fontSize: 12)),
        ],
      ),
    );
  }

  Widget _buildDrawer() {
    return Drawer(
      child: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF6366F1), Color(0xFF4F46E5)],
            stops: [0.0, 0.3],
          ),
        ),
        child: Column(
          children: [
            // Header
            Container(
              padding: const EdgeInsets.only(top: 60, bottom: 30, left: 24, right: 24),
              child: Column(
                children: [
                  Container(
                    padding: const EdgeInsets.all(4),
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      border: Border.all(color: Colors.white, width: 3),
                    ),
                    child: CircleAvatar(
                      radius: 40,
                      backgroundColor: Colors.white.withOpacity(0.2),
                      child: Text(
                        _userName.isNotEmpty ? _userName[0].toUpperCase() : 'U',
                        style: const TextStyle(color: Colors.white, fontSize: 32, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    _userName,
                    style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 4),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: const Text(
                      '💚 Health Member',
                      style: TextStyle(color: Colors.white, fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
            // Menu Items
            Expanded(
              child: Container(
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.only(
                    topLeft: Radius.circular(32),
                    topRight: Radius.circular(32),
                  ),
                ),
                child: ListView(
                  padding: const EdgeInsets.only(top: 24),
                  children: [
                    _drawerSection('Main'),
                    _drawerItem(Icons.home_rounded, 'Dashboard', () => Navigator.pop(context)),
                    _drawerItem(Icons.person_rounded, 'My Profile', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => ProfilePage(baseUrl: widget.baseUrl)));
                    }),
                    const SizedBox(height: 16),
                    _drawerSection('Health Tools'),
                    _drawerItem(Icons.science_rounded, 'Psychological Tests', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => TestsPage(baseUrl: widget.baseUrl)));
                    }),
                    _drawerItem(Icons.face_rounded, 'Emotion Detection', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => CombinedEmotionPage(baseUrl: widget.baseUrl)));
                    }),
                    _drawerItem(Icons.insights_rounded, 'AI Predictions', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => PredictionsPage(baseUrl: widget.baseUrl)));
                    }),
                    const SizedBox(height: 16),
                    _drawerSection('AI Assistants'),
                    _drawerItem(Icons.auto_awesome_rounded, 'Get Motivated', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => GetMotivationPage(baseUrl: widget.baseUrl)));
                    }),
                    _drawerItem(Icons.spa_rounded, 'Serenity AI Chat', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => MotivationPage(baseUrl: widget.baseUrl)));
                    }),
                    _drawerItem(Icons.lightbulb_rounded, 'Recommendations', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => RecommendationsPage(baseUrl: widget.baseUrl)));
                    }),
                    const SizedBox(height: 16),
                    _drawerSection('Treatment'),
                    _drawerItem(Icons.assignment_rounded, 'Treatment Plans', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => TreatmentPlanPage(baseUrl: widget.baseUrl)));
                    }),
                    _drawerItem(Icons.medication_rounded, 'My Medications', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => const MyMedicationsPage()));
                    }),
                    _drawerItem(Icons.calendar_today_rounded, "Today's Doses", () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => TodaysDosesPage(baseUrl: widget.baseUrl)));
                    }),
                    const SizedBox(height: 16),
                    _drawerSection('History'),
                    _drawerItem(Icons.history_rounded, 'Activity History', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => HistoryPage(baseUrl: widget.baseUrl)));
                    }),
                    _drawerItem(Icons.analytics_rounded, 'Adherence Stats', () {
                      Navigator.pop(context);
                      Navigator.push(context, MaterialPageRoute(builder: (_) => AdherenceStatsPage(baseUrl: widget.baseUrl)));
                    }),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ),
            // Logout Button
            Container(
              color: Colors.white,
              padding: const EdgeInsets.all(20),
              child: ElevatedButton.icon(
                onPressed: () {
                  Navigator.pop(context);
                  _logout();
                },
                icon: const Icon(Icons.logout_rounded),
                label: const Text('Sign Out'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFFEE2E2),
                  foregroundColor: const Color(0xFFDC2626),
                  minimumSize: const Size(double.infinity, 52),
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _drawerSection(String title) {
    return Padding(
      padding: const EdgeInsets.only(left: 24, bottom: 8, top: 8),
      child: Text(
        title.toUpperCase(),
        style: const TextStyle(
          color: Color(0xFF9CA3AF),
          fontSize: 11,
          fontWeight: FontWeight.w700,
          letterSpacing: 1.2,
        ),
      ),
    );
  }

  Widget _drawerItem(IconData icon, String title, VoidCallback onTap) {
    return ListTile(
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: const Color(0xFF6366F1).withOpacity(0.1),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Icon(icon, color: const Color(0xFF6366F1), size: 20),
      ),
      title: Text(
        title,
        style: const TextStyle(
          fontWeight: FontWeight.w500,
          color: Color(0xFF374151),
        ),
      ),
      trailing: const Icon(Icons.chevron_right, color: Color(0xFFD1D5DB), size: 20),
      onTap: onTap,
      contentPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
    );
  }

  Widget _buildBottomNav() {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 20,
            offset: const Offset(0, -5),
          ),
        ],
      ),
      child: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) {
          setState(() => _currentIndex = index);
          switch (index) {
            case 0:
              break;
            case 1:
              Navigator.push(context, MaterialPageRoute(builder: (_) => TestsPage(baseUrl: widget.baseUrl)));
              break;
            case 2:
              Navigator.push(context, MaterialPageRoute(builder: (_) => MotivationPage(baseUrl: widget.baseUrl)));
              break;
            case 3:
              Navigator.push(context, MaterialPageRoute(builder: (_) => HistoryPage(baseUrl: widget.baseUrl)));
              break;
            case 4:
              Navigator.push(context, MaterialPageRoute(builder: (_) => ProfilePage(baseUrl: widget.baseUrl)));
              break;
          }
        },
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_rounded), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.science_rounded), label: 'Tests'),
          NavigationDestination(icon: Icon(Icons.spa_rounded), label: 'Serenity'),
          NavigationDestination(icon: Icon(Icons.history_rounded), label: 'History'),
          NavigationDestination(icon: Icon(Icons.person_rounded), label: 'Profile'),
        ],
      ),
    );
  }
}

class _DashboardData {
  final String displayName;
  final double? adherenceScore;
  final double? lastPrediction;
  final int completedTests;
  final int plansCount;
  final int activePlansCount;
  final double wellnessScore;
  final List<AdherenceSummary> history;
  final List<PredictionItem> predictions;

  _DashboardData({
    required this.displayName,
    required this.adherenceScore,
    required this.lastPrediction,
    required this.completedTests,
    required this.plansCount,
    required this.activePlansCount,
    required this.wellnessScore,
    required this.history,
    required this.predictions,
  });

  factory _DashboardData.empty() => _DashboardData(
    displayName: '',
    adherenceScore: null,
    lastPrediction: null,
    completedTests: 0,
    plansCount: 0,
    activePlansCount: 0,
    wellnessScore: 50.0,
    history: const [],
    predictions: const [],
  );
}
