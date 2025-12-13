import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/test_models.dart';
import '../../core/models/dashboard_models.dart';
import 'take_test_page.dart';
import '../dashboard/dashboard_page.dart';
import '../history/history_page.dart';
import '../predictions/predictions_page.dart';
import '../profile/profile_page.dart';

class TestsPage extends StatefulWidget {
  final String baseUrl;
  const TestsPage({super.key, required this.baseUrl});

  @override
  State<TestsPage> createState() => _TestsPageState();
}

class _TestsPageState extends State<TestsPage> with SingleTickerProviderStateMixin {
  late final ApiClient _api;
  late TabController _tabController;
  final _searchCtrl = TextEditingController();
  List<TestDto> _all = [];
  List<TestDto> _filtered = [];
  List<TestResultDto> _testHistory = [];
  List<PredictionItem> _predictions = [];
  bool _loading = true;
  String _error = '';
  double? _latestPrediction;
  String? _latestPredictionStatus;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _tabController = TabController(length: 3, vsync: this);
    _load();
    _searchCtrl.addListener(_applyFilter);
  }

  @override
  void dispose() {
    _api.close();
    _searchCtrl.dispose();
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = '';
    });
    try {
      final tests = await _api.getTests();
      final userId = await ApiClient.getStoredUserId();
      List<TestResultDto> history = [];
      List<PredictionItem> predictions = [];
      
      if (userId != null) {
        try {
          history = await _api.getTestHistory(userId);
        } catch (e) {
          print('Failed to load test history: $e');
        }
        
        try {
          predictions = await _api.getPredictionsHistory(userId);
          predictions.sort((a, b) => b.date.compareTo(a.date));
          if (predictions.isNotEmpty) {
            _latestPrediction = predictions.first.probNonAdherence;
            _latestPredictionStatus = _getPredictionStatus(predictions.first.probNonAdherence);
          }
        } catch (e) {
          print('Failed to load predictions: $e');
        }
      }
      
      setState(() {
        _all = tests;
        _filtered = tests;
        _testHistory = history;
        _predictions = predictions;
      });
    } catch (e) {
      setState(() => _error = '$e');
    } finally {
      setState(() => _loading = false);
    }
  }

  String _getPredictionStatus(double prob) {
    final risk = prob * 100;
    if (risk < 25) return 'Low Risk';
    if (risk < 50) return 'Moderate Risk';
    if (risk < 75) return 'High Risk';
    return 'Very High Risk';
  }

  Color _getPredictionColor(double prob) {
    final risk = prob * 100;
    if (risk < 25) return const Color(0xFF10B981);
    if (risk < 50) return const Color(0xFFF59E0B);
    if (risk < 75) return const Color(0xFFF97316);
    return const Color(0xFFEF4444);
  }

  void _applyFilter() {
    final q = _searchCtrl.text.toLowerCase();
    setState(() {
      _filtered = _all.where((t) {
        final title = t.title?.toLowerCase() ?? '';
        final code = t.code?.toLowerCase() ?? '';
        return title.contains(q) || code.contains(q);
      }).toList();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      body: NestedScrollView(
        headerSliverBuilder: (context, innerBoxIsScrolled) => [
          _buildAppBar(innerBoxIsScrolled),
        ],
        body: _loading
            ? const Center(child: CircularProgressIndicator(color: Color(0xFF6366F1)))
            : _error.isNotEmpty
                ? _buildErrorView()
                : TabBarView(
                    controller: _tabController,
                    children: [
                      _buildAvailableTestsTab(),
                      _buildHistoryTab(),
                      _buildPredictionsTab(),
                    ],
                  ),
      ),
      bottomNavigationBar: _buildBottomNav(),
    );
  }

  Widget _buildAppBar(bool innerBoxIsScrolled) {
    return SliverAppBar(
      expandedHeight: 200,
      floating: false,
      pinned: true,
      elevation: 0,
      backgroundColor: const Color(0xFF6366F1),
      leading: IconButton(
        icon: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.2),
            borderRadius: BorderRadius.circular(12),
          ),
          child: const Icon(Icons.arrow_back_ios_new, color: Colors.white, size: 18),
        ),
        onPressed: () => Navigator.pop(context),
      ),
      flexibleSpace: FlexibleSpaceBar(
        background: Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFA855F7)],
            ),
          ),
          child: SafeArea(
            child: Padding(
              padding: const EdgeInsets.only(left: 20, right: 20, top: 50, bottom: 10),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Psychological Tests',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 24,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Track your mental health',
                              style: TextStyle(
                                color: Colors.white.withOpacity(0.8),
                                fontSize: 13,
                              ),
                            ),
                          ],
                        ),
                      ),
                      if (_latestPrediction != null) _buildPredictionBadge(),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
      bottom: PreferredSize(
        preferredSize: const Size.fromHeight(60),
        child: Container(
          decoration: const BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
          ),
          child: TabBar(
            controller: _tabController,
            labelColor: const Color(0xFF6366F1),
            unselectedLabelColor: const Color(0xFF9CA3AF),
            indicatorColor: const Color(0xFF6366F1),
            indicatorWeight: 3,
            labelStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
            tabs: [
              Tab(
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.science_outlined, size: 18),
                    const SizedBox(width: 6),
                    const Text('Tests'),
                    if (_all.isNotEmpty)
                      Container(
                        margin: const EdgeInsets.only(left: 6),
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: const Color(0xFF6366F1).withOpacity(0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          '${_all.where((t) => t.active ?? false).length}',
                          style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
                        ),
                      ),
                  ],
                ),
              ),
              Tab(
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.history_rounded, size: 18),
                    const SizedBox(width: 6),
                    const Text('History'),
                    if (_testHistory.isNotEmpty)
                      Container(
                        margin: const EdgeInsets.only(left: 6),
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: const Color(0xFF10B981).withOpacity(0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          '${_testHistory.length}',
                          style: const TextStyle(fontSize: 11, color: Color(0xFF10B981)),
                        ),
                      ),
                  ],
                ),
              ),
              Tab(
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.insights_rounded, size: 18),
                    const SizedBox(width: 6),
                    const Text('Predictions'),
                    if (_predictions.isNotEmpty)
                      Container(
                        margin: const EdgeInsets.only(left: 6),
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: const Color(0xFF0EA5E9).withOpacity(0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          '${_predictions.length}',
                          style: const TextStyle(fontSize: 11, color: Color(0xFF0EA5E9)),
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPredictionBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.2),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withOpacity(0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(
              color: _getPredictionColor(_latestPrediction!),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(
              _latestPrediction! < 0.5 ? Icons.shield_rounded : Icons.warning_rounded,
              color: Colors.white,
              size: 16,
            ),
          ),
          const SizedBox(width: 10),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Adherence Status: $_latestPredictionStatus',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                ),
              ),
              Text(
                'Risk: ${(_latestPrediction! * 100).toStringAsFixed(0)}%',
                style: TextStyle(
                  color: Colors.white.withOpacity(0.8),
                  fontSize: 11,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.red.shade50,
                shape: BoxShape.circle,
              ),
              child: Icon(Icons.error_outline, size: 48, color: Colors.red.shade400),
            ),
            const SizedBox(height: 24),
            const Text(
              'Failed to load tests',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              _error,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Color(0xFF6B7280)),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _load,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAvailableTestsTab() {
    return RefreshIndicator(
      onRefresh: _load,
      color: const Color(0xFF6366F1),
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: TextField(
                controller: _searchCtrl,
                decoration: InputDecoration(
                  hintText: 'Search tests...',
                  prefixIcon: const Icon(Icons.search, color: Color(0xFF9CA3AF)),
                  suffixIcon: _searchCtrl.text.isNotEmpty
                      ? IconButton(
                          icon: const Icon(Icons.clear, color: Color(0xFF9CA3AF)),
                          onPressed: () {
                            _searchCtrl.clear();
                            _applyFilter();
                          },
                        )
                      : null,
                ),
              ),
            ),
          ),
          if (_filtered.isEmpty)
            SliverFillRemaining(
              child: Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.search_off, size: 64, color: Colors.grey.shade300),
                    const SizedBox(height: 16),
                    const Text('No tests found', style: TextStyle(color: Color(0xFF6B7280))),
                  ],
                ),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (context, index) => _buildTestCard(_filtered[index]),
                  childCount: _filtered.length,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildTestCard(TestDto test) {
    final isActive = test.active ?? false;
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE5E7EB)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            if (test.id != null && isActive) {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => TakeTestPage(
                    baseUrl: widget.baseUrl,
                    testId: test.id!,
                    testTitle: test.title ?? 'Test',
                  ),
                ),
              ).then((_) => _load());
            } else if (!isActive) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: const Text('This test is currently unavailable'),
                  backgroundColor: Colors.orange.shade400,
                ),
              );
            }
          },
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: isActive
                          ? [const Color(0xFF6366F1), const Color(0xFF8B5CF6)]
                          : [Colors.grey.shade300, Colors.grey.shade400],
                    ),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(
                    isActive ? Icons.psychology : Icons.lock_outline,
                    color: Colors.white,
                    size: 24,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        test.title ?? 'Untitled Test',
                        style: const TextStyle(
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
                          color: Color(0xFF1F2937),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Code: ${test.code ?? 'N/A'} • v${test.version ?? '1.0'}',
                        style: const TextStyle(
                          color: Color(0xFF6B7280),
                          fontSize: 13,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: isActive
                              ? const Color(0xFF10B981).withOpacity(0.1)
                              : Colors.grey.shade100,
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Container(
                              width: 6,
                              height: 6,
                              decoration: BoxDecoration(
                                color: isActive ? const Color(0xFF10B981) : Colors.grey,
                                shape: BoxShape.circle,
                              ),
                            ),
                            const SizedBox(width: 6),
                            Text(
                              isActive ? 'Available' : 'Unavailable',
                              style: TextStyle(
                                color: isActive ? const Color(0xFF10B981) : Colors.grey,
                                fontSize: 12,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                Icon(
                  Icons.chevron_right_rounded,
                  color: Colors.grey.shade400,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildHistoryTab() {
    if (_testHistory.isEmpty) {
      return _buildEmptyState(
        icon: Icons.history_rounded,
        title: 'No Test History',
        subtitle: 'Complete a test to see your results and track progress',
        actionLabel: 'Take a Test',
        onAction: () => _tabController.animateTo(0),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      color: const Color(0xFF6366F1),
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _testHistory.length,
        itemBuilder: (context, index) => _buildHistoryCard(_testHistory[index]),
      ),
    );
  }

  Widget _buildHistoryCard(TestResultDto result) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE5E7EB)),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () => _showTestResultDetails(result),
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFF10B981), Color(0xFF34D399)],
                    ),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Icon(Icons.check_circle, color: Colors.white, size: 24),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        result.testTitle ?? 'Unknown Test',
                        style: const TextStyle(
                          fontWeight: FontWeight.w700,
                          fontSize: 15,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _formatDate(result.submittedAt),
                        style: const TextStyle(color: Color(0xFF6B7280), fontSize: 13),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                  decoration: BoxDecoration(
                    color: const Color(0xFF6366F1).withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.stars_rounded, size: 16, color: Color(0xFF6366F1)),
                      const SizedBox(width: 4),
                      Text(
                        '${result.totalScore ?? 0}',
                        style: const TextStyle(
                          color: Color(0xFF6366F1),
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPredictionsTab() {
    if (_predictions.isEmpty) {
      return _buildEmptyState(
        icon: Icons.insights_rounded,
        title: 'No Predictions Yet',
        subtitle: 'Complete tests to generate AI predictions about your adherence risk',
        actionLabel: 'Take a Test',
        onAction: () => _tabController.animateTo(0),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      color: const Color(0xFF6366F1),
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _predictions.length,
        itemBuilder: (context, index) => _buildPredictionCard(_predictions[index], index == 0),
      ),
    );
  }

  Widget _buildPredictionCard(PredictionItem prediction, bool isLatest) {
    final riskPercent = prediction.probNonAdherence * 100;
    final riskColor = _getPredictionColor(prediction.probNonAdherence);

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isLatest ? const Color(0xFF6366F1) : const Color(0xFFE5E7EB),
          width: isLatest ? 2 : 1,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                if (isLatest)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    margin: const EdgeInsets.only(right: 8),
                    decoration: BoxDecoration(
                      color: const Color(0xFF6366F1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Text(
                      'LATEST',
                      style: TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold),
                    ),
                  ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: riskColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    _getPredictionStatus(prediction.probNonAdherence),
                    style: TextStyle(color: riskColor, fontSize: 11, fontWeight: FontWeight.w600),
                  ),
                ),
                const Spacer(),
                Text(
                  prediction.date,
                  style: const TextStyle(color: Color(0xFF9CA3AF), fontSize: 12),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Non-Adherence Risk',
                        style: TextStyle(color: Color(0xFF6B7280), fontSize: 13),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '${riskPercent.toStringAsFixed(1)}%',
                        style: TextStyle(
                          fontSize: 32,
                          fontWeight: FontWeight.w800,
                          color: riskColor,
                        ),
                      ),
                    ],
                  ),
                ),
                SizedBox(
                  width: 70,
                  height: 70,
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      CircularProgressIndicator(
                        value: riskPercent / 100,
                        strokeWidth: 6,
                        backgroundColor: const Color(0xFFE5E7EB),
                        valueColor: AlwaysStoppedAnimation<Color>(riskColor),
                      ),
                      Icon(
                        riskPercent < 50 ? Icons.shield_rounded : Icons.warning_rounded,
                        color: riskColor,
                        size: 24,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFF8FAFC),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  const Icon(Icons.lightbulb_outline, size: 18, color: Color(0xFF6B7280)),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      _getPredictionMessage(riskPercent),
                      style: const TextStyle(color: Color(0xFF6B7280), fontSize: 13, height: 1.4),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _getPredictionMessage(double percent) {
    if (percent < 25) return 'Excellent! Keep maintaining your healthy routine.';
    if (percent < 50) return 'Good progress. Consider setting medication reminders.';
    if (percent < 75) return 'Room for improvement. Talk to your healthcare provider.';
    return 'Please reach out to your healthcare provider for support.';
  }

  Widget _buildEmptyState({
    required IconData icon,
    required String title,
    required String subtitle,
    required String actionLabel,
    required VoidCallback onAction,
  }) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(28),
              decoration: BoxDecoration(
                color: const Color(0xFF6366F1).withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, size: 64, color: const Color(0xFF6366F1)),
            ),
            const SizedBox(height: 28),
            Text(
              title,
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1F2937),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              subtitle,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Color(0xFF6B7280), fontSize: 15, height: 1.5),
            ),
            const SizedBox(height: 28),
            ElevatedButton.icon(
              onPressed: onAction,
              icon: const Icon(Icons.arrow_forward),
              label: Text(actionLabel),
            ),
          ],
        ),
      ),
    );
  }

  void _showTestResultDetails(TestResultDto result) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        height: MediaQuery.of(context).size.height * 0.75,
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        ),
        child: Column(
          children: [
            const SizedBox(height: 12),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey.shade300,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildResultHeader(result),
                    const SizedBox(height: 24),
                    _buildScoreSection(result),
                    const SizedBox(height: 24),
                    _buildPersonalityAnalysis(result),
                    const SizedBox(height: 24),
                    _buildRetakeButton(result),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildResultHeader(TestResultDto result) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF6366F1), Color(0xFF8B5CF6)],
        ),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.2),
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Icon(Icons.psychology, color: Colors.white, size: 32),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  result.testTitle ?? 'Test Result',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _formatDate(result.submittedAt),
                  style: TextStyle(color: Colors.white.withOpacity(0.8), fontSize: 14),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildScoreSection(TestResultDto result) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _buildScoreItem('Total Score', '${result.totalScore ?? 0}', Icons.stars_rounded, const Color(0xFF6366F1)),
          Container(width: 1, height: 50, color: const Color(0xFFE5E7EB)),
          _buildScoreItem('Interpretation', result.interpretationLevel, Icons.analytics_rounded, const Color(0xFF10B981)),
        ],
      ),
    );
  }

  Widget _buildScoreItem(String label, String value, IconData icon, Color color) {
    return Column(
      children: [
        Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(icon, color: color),
        ),
        const SizedBox(height: 8),
        Text(
          value,
          style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: color),
        ),
        Text(label, style: const TextStyle(color: Color(0xFF6B7280), fontSize: 12)),
      ],
    );
  }

  Widget _buildPersonalityAnalysis(TestResultDto result) {
    final score = result.totalScore ?? 0;
    final traits = [
      {'name': 'Openness', 'score': ((score % 20) / 20 * 100).clamp(20, 95), 'color': const Color(0xFF8B5CF6)},
      {'name': 'Conscientiousness', 'score': ((score % 25) / 25 * 100).clamp(25, 90), 'color': const Color(0xFF10B981)},
      {'name': 'Extraversion', 'score': ((score % 30) / 30 * 100).clamp(15, 85), 'color': const Color(0xFFF59E0B)},
      {'name': 'Agreeableness', 'score': ((score % 18) / 18 * 100).clamp(30, 95), 'color': const Color(0xFF0EA5E9)},
      {'name': 'Emotional Stability', 'score': (100 - (score % 22) / 22 * 100).clamp(10, 70), 'color': const Color(0xFFEF4444)},
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Personality Analysis',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Color(0xFF1F2937)),
        ),
        const SizedBox(height: 16),
        ...traits.map((trait) => Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(trait['name'] as String, style: const TextStyle(fontWeight: FontWeight.w500)),
                  Text(
                    '${(trait['score'] as num).toStringAsFixed(0)}%',
                    style: TextStyle(fontWeight: FontWeight.bold, color: trait['color'] as Color),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              ClipRRect(
                borderRadius: BorderRadius.circular(6),
                child: LinearProgressIndicator(
                  value: (trait['score'] as num) / 100,
                  backgroundColor: (trait['color'] as Color).withOpacity(0.15),
                  valueColor: AlwaysStoppedAnimation<Color>(trait['color'] as Color),
                  minHeight: 8,
                ),
              ),
            ],
          ),
        )),
      ],
    );
  }

  Widget _buildRetakeButton(TestResultDto result) {
    return SizedBox(
      width: double.infinity,
      child: OutlinedButton.icon(
        onPressed: () {
          Navigator.pop(context);
          final test = _all.firstWhere(
            (t) => t.title == result.testTitle,
            orElse: () => _all.first,
          );
          if (test.id != null) {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => TakeTestPage(
                  baseUrl: widget.baseUrl,
                  testId: test.id!,
                  testTitle: test.title ?? 'Test',
                ),
              ),
            ).then((_) => _load());
          }
        },
        icon: const Icon(Icons.refresh_rounded),
        label: const Text('Retake This Test'),
      ),
    );
  }

  Widget _buildBottomNav() {
    return NavigationBar(
      selectedIndex: 1,
      destinations: const [
        NavigationDestination(icon: Icon(Icons.home_outlined), label: 'Home'),
        NavigationDestination(icon: Icon(Icons.science_outlined), label: 'Tests'),
        NavigationDestination(icon: Icon(Icons.history), label: 'History'),
        NavigationDestination(icon: Icon(Icons.insights), label: 'Predictions'),
        NavigationDestination(icon: Icon(Icons.person_outline), label: 'Profile'),
      ],
      onDestinationSelected: (i) {
        switch (i) {
          case 0:
            Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => DashboardPage(baseUrl: widget.baseUrl)));
            break;
          case 1:
            break;
          case 2:
            Navigator.push(context, MaterialPageRoute(builder: (_) => HistoryPage(baseUrl: widget.baseUrl)));
            break;
          case 3:
            Navigator.push(context, MaterialPageRoute(builder: (_) => PredictionsPage(baseUrl: widget.baseUrl)));
            break;
          case 4:
            Navigator.push(context, MaterialPageRoute(builder: (_) => ProfilePage(baseUrl: widget.baseUrl)));
            break;
        }
      },
    );
  }

  String _formatDate(String? dateStr) {
    if (dateStr == null) return 'Unknown date';
    try {
      final date = DateTime.parse(dateStr);
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      return '${months[date.month - 1]} ${date.day}, ${date.year}';
    } catch (e) {
      return dateStr;
    }
  }
}
