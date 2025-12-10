import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../core/api_client.dart';
import '../../core/models/dashboard_models.dart';
import '../tests/tests_page.dart';
import '../profile/profile_page.dart';
import '../recommendations/recommendations_page.dart';
import '../motivation/motivation_page.dart';
import '../treatment/treatment_plan_page.dart';
import '../history/history_page.dart';
import '../predictions/predictions_page.dart';
import '../insights/adherence_insights_page.dart';
import '../auth/login_page.dart';
import '../doses/todays_doses_page.dart';
import '../doses/adherence_stats_page.dart';
import '../medications/pages/my_medications_page.dart';

class DashboardPage extends StatefulWidget {
  final String baseUrl;
  final String? userName;
  const DashboardPage({super.key, required this.baseUrl, this.userName});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  late final ApiClient _api;
  Future<_DashboardData>? _future;
  String _userName = 'User';

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _userName = widget.userName ?? 'User';
    _future = _load();
  }

  Future<_DashboardData> _load() async {
    final userId = await ApiClient.getStoredUserId();

    // Get stored user name if not provided
    if (widget.userName == null) {
      final storedName = await ApiClient.getStoredUserName();
      if (storedName != null && mounted) {
        setState(() => _userName = storedName);
      }
    }

    if (userId == null) {
      return _DashboardData.empty();
    }
    // last 30 days window
    final now = DateTime.now();
    final to = _fmtDate(now);
    final from = _fmtDate(now.subtract(const Duration(days: 30)));

    List<AdherenceSummary> history = [];
    List<PredictionItem> preds = [];
    int plansCount = 0;
    int completedTests = 0;

    try {
      history = await _api.getAdherenceHistory(
        userId: userId,
        from: from,
        to: to,
      );
    } catch (e) {
      print('Failed to load adherence history: $e');
    }

    try {
      preds = await _api.getPredictionsHistory(userId);
    } catch (e) {
      print('Failed to load predictions: $e');
    }

    try {
      plansCount = await _api.getPlansCount(userId);
    } catch (e) {
      print('Failed to load plans count: $e');
    }

    try {
      final testHistory = await _api.getTestHistory(userId);
      completedTests = testHistory.length;
    } catch (e) {
      print('Failed to load test history: $e');
    }

    final latestScore = history.isNotEmpty ? history.last.adherenceScore : null;
    final latestPred = preds.isNotEmpty ? preds.last.probNonAdherence : null;

    return _DashboardData(
      displayName: _userName,
      adherenceScore: latestScore,
      lastPrediction: latestPred,
      completedTests: completedTests,
      plansCount: plansCount,
      history: history.reversed.toList(),
      predictions: preds.reversed.toList(),
    );
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

  /// Build a prominent card for Today's Doses - the main CTA
  Widget _buildTodaysDosesCard(BuildContext context) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => TodaysDosesPage(baseUrl: widget.baseUrl),
          ),
        );
      },
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: [Color(0xFF2563EB), Color(0xFF7C3AED)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF2563EB).withOpacity(0.4),
              blurRadius: 15,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(
                Icons.medication_rounded,
                color: Colors.white,
                size: 32,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    "Today's Medications",
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Track your doses and stay on schedule',
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.9),
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Icon(
                Icons.arrow_forward_ios,
                color: Colors.white,
                size: 16,
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            CircleAvatar(
              radius: 16,
              backgroundColor: Colors.blue.shade100,
              child: Text(
                _userName.isNotEmpty ? _userName[0].toUpperCase() : 'U',
                style: TextStyle(
                  color: Colors.blue.shade700,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Text(
              'Hello, $_userName',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Logout',
            onPressed: _logout,
          ),
        ],
      ),
      bottomNavigationBar: _BottomNavBar(
        currentIndex: 0,
        baseUrl: widget.baseUrl,
      ),
      body: SafeArea(
        child: FutureBuilder<_DashboardData>(
          future: _future,
          builder: (context, snap) {
            if (snap.connectionState != ConnectionState.done) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snap.hasError) {
              return Center(child: Text('Failed to load: ${snap.error}'));
            }
            final data = snap.data ?? _DashboardData.empty();
            return SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Summary grid
                  Row(
                    children: [
                      Expanded(
                        child: _StatCard(
                          icon: Icons.verified,
                          title: 'Adherence Score',
                          value: data.adherenceScore != null
                              ? '${data.adherenceScore!.toStringAsFixed(0)}%'
                              : '--',
                          color: Colors.indigo,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _StatCard(
                          icon: Icons.model_training,
                          title: 'Last Prediction',
                          value: data.lastPrediction != null
                              ? '${(data.lastPrediction! * 100).toStringAsFixed(0)}%'
                              : '--',
                          color: Colors.teal,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: _StatCard(
                          icon: Icons.task_alt,
                          title: 'Completed Tests',
                          value: '${data.completedTests}',
                          color: Colors.green,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _StatCard(
                          icon: Icons.assignment,
                          title: 'Action Plans',
                          value: '${data.plansCount}',
                          color: Colors.deepPurple,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  // TODAY'S DOSES - Prominent card
                  _buildTodaysDosesCard(context),
                  const SizedBox(height: 16),
                  const Text(
                    'Quick Actions',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: _QuickAction(
                          label: 'Start a Test',
                          color1: const Color(0xFF7C3AED),
                          color2: const Color(0xFF9333EA),
                          icon: Icons.science,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) =>
                                    TestsPage(baseUrl: widget.baseUrl),
                              ),
                            );
                          },
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _QuickAction(
                          label: 'My Profile',
                          color1: const Color(0xFF7C3AED),
                          color2: const Color(0xFF6366F1),
                          icon: Icons.psychology,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) =>
                                    ProfilePage(baseUrl: widget.baseUrl),
                              ),
                            );
                          },
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _QuickAction(
                          label: 'Motivation',
                          color1: const Color(0xFFE11D48),
                          color2: const Color(0xFFF43F5E),
                          icon: Icons.favorite,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) =>
                                    MotivationPage(baseUrl: widget.baseUrl),
                              ),
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: _QuickAction(
                          label: 'Recommendations',
                          color1: const Color(0xFF0D9488),
                          color2: const Color(0xFF14B8A6),
                          icon: Icons.lightbulb,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => RecommendationsPage(
                                  baseUrl: widget.baseUrl,
                                ),
                              ),
                            );
                          },
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _QuickAction(
                          label: 'Treatment Plan',
                          color1: const Color(0xFF059669),
                          color2: const Color(0xFF10B981),
                          icon: Icons.healing,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) =>
                                    TreatmentPlanPage(baseUrl: widget.baseUrl),
                              ),
                            );
                          },
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _QuickAction(
                          label: 'My Medications',
                          color1: const Color(0xFFDB2777),
                          color2: const Color(0xFFF472B6),
                          icon: Icons.medication_liquid,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => const MyMedicationsPage(),
                              ),
                            );
                          },
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _QuickAction(
                          label: 'AI Insights',
                          color1: const Color(0xFF065F46),
                          color2: const Color(0xFF0E7490),
                          icon: Icons.insights,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => AdherenceInsightsPage(
                                  baseUrl: widget.baseUrl,
                                ),
                              ),
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: _QuickAction(
                          label: "Today's Doses",
                          color1: const Color(0xFF2563EB),
                          color2: const Color(0xFF3B82F6),
                          icon: Icons.medication,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) =>
                                    TodaysDosesPage(baseUrl: widget.baseUrl),
                              ),
                            );
                          },
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _QuickAction(
                          label: 'Adherence Stats',
                          color1: const Color(0xFF7C3AED),
                          color2: const Color(0xFFA855F7),
                          icon: Icons.bar_chart,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) =>
                                    AdherenceStatsPage(baseUrl: widget.baseUrl),
                              ),
                            );
                          },
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _QuickAction(
                          label: 'History',
                          color1: const Color(0xFF475569),
                          color2: const Color(0xFF64748B),
                          icon: Icons.history,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) =>
                                    HistoryPage(baseUrl: widget.baseUrl),
                              ),
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Recent Activity',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      TextButton(
                        onPressed: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) =>
                                  HistoryPage(baseUrl: widget.baseUrl),
                            ),
                          );
                        },
                        child: const Text(
                          'See All',
                          style: TextStyle(
                            color: Color(0xFF6366F1),
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  ..._recentItems(data),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  List<Widget> _recentItems(_DashboardData data) {
    final items = <Widget>[];

    // Show adherence history entries (last 3)
    for (final h in data.history.take(3)) {
      items.add(
        _RecentTile(
          icon: Icons.psychology,
          title: 'Completion ${h.completionRate.toStringAsFixed(0)}%',
          subtitle: h.date,
        ),
      );
    }
    // Show predictions (last 3)
    for (final p in data.predictions.take(3)) {
      items.add(
        _RecentTile(
          icon: Icons.trending_up,
          title:
              'New Prediction: ${(p.probNonAdherence * 100).toStringAsFixed(0)}% risk',
          subtitle: p.date,
        ),
      );
    }

    if (items.isEmpty) {
      items.add(
        const _RecentTile(
          icon: Icons.info_outline,
          title: 'No recent activity',
          subtitle: 'Start a test to see activity here',
        ),
      );
    }
    return items;
  }
}

class _DashboardData {
  final String displayName;
  final double? adherenceScore;
  final double? lastPrediction;
  final int completedTests;
  final int plansCount;
  final List<AdherenceSummary> history;
  final List<PredictionItem> predictions;

  _DashboardData({
    required this.displayName,
    required this.adherenceScore,
    required this.lastPrediction,
    required this.completedTests,
    required this.plansCount,
    required this.history,
    required this.predictions,
  });

  factory _DashboardData.empty() => _DashboardData(
    displayName: '',
    adherenceScore: null,
    lastPrediction: null,
    completedTests: 0,
    plansCount: 0,
    history: const [],
    predictions: const [],
  );
}

class _StatCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String value;
  final Color color;

  const _StatCard({
    required this.icon,
    required this.title,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 110,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: const [
          BoxShadow(
            color: Color(0x11000000),
            blurRadius: 10,
            offset: Offset(0, 4),
          ),
        ],
      ),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: color),
          const Spacer(),
          Text(title, style: const TextStyle(color: Color(0xFF6B7280))),
          const SizedBox(height: 4),
          Text(
            value,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}

class _QuickAction extends StatelessWidget {
  final String label;
  final Color color1;
  final Color color2;
  final IconData icon;
  final VoidCallback onTap;

  const _QuickAction({
    required this.label,
    required this.color1,
    required this.color2,
    required this.icon,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 110,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          gradient: LinearGradient(colors: [color1, color2]),
        ),
        padding: const EdgeInsets.all(12),
        child: Align(
          alignment: Alignment.bottomLeft,
          child: Row(
            children: [
              Icon(icon, color: Colors.white),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  label,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RecentTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;

  const _RecentTile({
    required this.icon,
    required this.title,
    required this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: const Color(0xFFF1F5F9),
        child: Icon(icon, color: const Color(0xFF111827)),
      ),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
      subtitle: Text(
        subtitle,
        style: const TextStyle(color: Color(0xFF6B7280)),
      ),
      trailing: const Icon(Icons.chevron_right),
    );
  }
}

class _BottomNavBar extends StatelessWidget {
  final int currentIndex;
  final String baseUrl;
  const _BottomNavBar({required this.currentIndex, required this.baseUrl});

  @override
  Widget build(BuildContext context) {
    return NavigationBar(
      selectedIndex: currentIndex,
      onDestinationSelected: (i) {
        if (i == currentIndex) return; // Already on this tab

        switch (i) {
          case 0: // Home
            Navigator.of(context).pushAndRemoveUntil(
              MaterialPageRoute(
                builder: (_) => DashboardPage(baseUrl: baseUrl),
              ),
              (route) => false,
            );
            break;
          case 1: // Meds (Today's Doses)
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => TodaysDosesPage(baseUrl: baseUrl),
              ),
            );
            break;
          case 2: // Tests
            Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => TestsPage(baseUrl: baseUrl)),
            );
            break;
          case 3: // Stats
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => AdherenceStatsPage(baseUrl: baseUrl),
              ),
            );
            break;
          case 4: // Profile
            Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => ProfilePage(baseUrl: baseUrl)),
            );
            break;
        }
      },
      destinations: const [
        NavigationDestination(
          icon: Icon(Icons.home_outlined),
          selectedIcon: Icon(Icons.home),
          label: 'Home',
        ),
        NavigationDestination(
          icon: Icon(Icons.medication_outlined),
          selectedIcon: Icon(Icons.medication),
          label: 'Meds',
        ),
        NavigationDestination(
          icon: Icon(Icons.science_outlined),
          selectedIcon: Icon(Icons.science),
          label: 'Tests',
        ),
        NavigationDestination(
          icon: Icon(Icons.bar_chart_outlined),
          selectedIcon: Icon(Icons.bar_chart),
          label: 'Stats',
        ),
        NavigationDestination(
          icon: Icon(Icons.person_outline),
          selectedIcon: Icon(Icons.person),
          label: 'Profile',
        ),
      ],
    );
  }
}
