import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import '../../core/api_client.dart';
import '../../core/models/dashboard_models.dart';

/// Enhanced Adherence Insights page using multi-factor evidence-based prediction
class AdherenceInsightsPage extends StatefulWidget {
  final String baseUrl;
  const AdherenceInsightsPage({super.key, required this.baseUrl});

  @override
  State<AdherenceInsightsPage> createState() => _AdherenceInsightsPageState();
}

class _AdherenceInsightsPageState extends State<AdherenceInsightsPage> {
  late final ApiClient _api;
  AdherencePrediction? _prediction;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _loadPrediction();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _loadPrediction() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final userId = await ApiClient.getStoredUserId();
      if (userId == null) {
        setState(() => _error = 'Please log in first');
        return;
      }

      final prediction = await _api.getEnhancedPrediction(userId);
      setState(() => _prediction = prediction);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      body: CustomScrollView(
        slivers: [
          _buildAppBar(),
          if (_loading)
            const SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(color: Color(0xFF0EA5E9)),
              ),
            )
          else if (_error != null)
            SliverFillRemaining(child: _buildErrorView())
          else if (_prediction != null)
            SliverToBoxAdapter(child: _buildContent())
          else
            SliverFillRemaining(child: _buildEmptyView()),
        ],
      ),
    );
  }

  Widget _buildAppBar() {
    return SliverAppBar(
      expandedHeight: 140,
      floating: false,
      pinned: true,
      elevation: 0,
      backgroundColor: const Color(0xFF0EA5E9),
      leading: IconButton(
        icon: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.2),
            borderRadius: BorderRadius.circular(12),
          ),
          child: const Icon(
            Icons.arrow_back_ios_new,
            color: Colors.white,
            size: 18,
          ),
        ),
        onPressed: () => Navigator.pop(context),
      ),
      actions: [
        Padding(
          padding: const EdgeInsets.only(right: 8),
          child: IconButton(
            icon: Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(Icons.refresh, color: Colors.white, size: 20),
            ),
            onPressed: _loadPrediction,
            tooltip: 'Refresh',
          ),
        ),
      ],
      flexibleSpace: FlexibleSpaceBar(
        titlePadding: const EdgeInsets.only(left: 20, bottom: 16),
        title: const Text(
          'Adherence Insights',
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.w700,
            fontSize: 20,
          ),
        ),
        background: Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFF0EA5E9), Color(0xFF0284C7)],
            ),
          ),
          child: Stack(
            children: [
              Positioned(
                right: -30,
                top: -30,
                child: Container(
                  width: 150,
                  height: 150,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: Colors.white.withOpacity(0.1),
                  ),
                ),
              ),
              Positioned(
                right: 30,
                bottom: 0,
                child: Icon(
                  Icons.analytics_outlined,
                  size: 80,
                  color: Colors.white.withOpacity(0.1),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildContent() {
    final p = _prediction!;
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Main Prediction Card
          _buildMainPredictionCard(p),
          const SizedBox(height: 20),

          // Streaks Card
          _buildStreaksCard(p),
          const SizedBox(height: 20),

          // Component Scores
          _buildComponentScoresCard(p),
          const SizedBox(height: 20),

          // Risk Factors
          if (p.riskFactors.isNotEmpty) ...[
            _buildRiskFactorsCard(p),
            const SizedBox(height: 20),
          ],

          // Skip Reasons Analysis
          if (p.skipReasonBreakdown.isNotEmpty) ...[
            _buildSkipReasonsCard(p),
            const SizedBox(height: 20),
          ],

          // AI Recommendations
          if (p.recommendations.isNotEmpty) ...[
            _buildRecommendationsCard(p),
            const SizedBox(height: 20),
          ],

          // Confidence Indicator
          _buildConfidenceCard(p),
          const SizedBox(height: 40),
        ],
      ),
    );
  }

  Widget _buildMainPredictionCard(AdherencePrediction p) {
    final color = _getRiskColor(p.riskLevel);
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [color.withOpacity(0.1), color.withOpacity(0.05)],
        ),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(_getRiskIcon(p.riskLevel), color: color, size: 28),
              const SizedBox(width: 12),
              Text(
                '${p.riskLevel} Risk',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: color,
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          SizedBox(
            width: 160,
            height: 160,
            child: Stack(
              alignment: Alignment.center,
              children: [
                SizedBox(
                  width: 160,
                  height: 160,
                  child: CircularProgressIndicator(
                    value: p.predictedAdherence,
                    strokeWidth: 12,
                    backgroundColor: Colors.grey.shade200,
                    valueColor: AlwaysStoppedAnimation(color),
                  ),
                ),
                Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      '${p.adherencePercentage.toStringAsFixed(0)}%',
                      style: TextStyle(
                        fontSize: 36,
                        fontWeight: FontWeight.bold,
                        color: color,
                      ),
                    ),
                    Text(
                      'Predicted\nAdherence',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.grey.shade600,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Text(
            _getRiskDescription(p.riskLevel),
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 14, color: Colors.grey.shade700),
          ),
        ],
      ),
    );
  }

  Widget _buildStreaksCard(AdherencePrediction p) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: _buildStreakItem(
              icon: Icons.local_fire_department,
              iconColor: Colors.orange,
              value: '${p.currentStreak}',
              label: 'Current Streak',
            ),
          ),
          Container(height: 50, width: 1, color: Colors.grey.shade200),
          Expanded(
            child: _buildStreakItem(
              icon: Icons.emoji_events,
              iconColor: Colors.amber,
              value: '${p.bestStreak}',
              label: 'Best Streak',
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStreakItem({
    required IconData icon,
    required Color iconColor,
    required String value,
    required String label,
  }) {
    return Column(
      children: [
        Icon(icon, color: iconColor, size: 32),
        const SizedBox(height: 8),
        Text(
          value,
          style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
        ),
        Text(
          label,
          style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
        ),
      ],
    );
  }

  Widget _buildComponentScoresCard(AdherencePrediction p) {
    final components = p.componentScores.entries.toList();
    if (components.isEmpty) return const SizedBox();

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.pie_chart, color: Color(0xFF0EA5E9)),
              SizedBox(width: 8),
              Text(
                'Factor Analysis',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
            ],
          ),
          const SizedBox(height: 16),
          ...components.map((e) => _buildComponentBar(e.key, e.value)),
        ],
      ),
    );
  }

  Widget _buildComponentBar(String name, double value) {
    final displayName = _formatComponentName(name);
    final color = value >= 0.7
        ? const Color(0xFF10B981)
        : value >= 0.5
        ? const Color(0xFFF59E0B)
        : const Color(0xFFEF4444);

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(displayName, style: const TextStyle(fontSize: 13)),
              Text(
                '${(value * 100).toStringAsFixed(0)}%',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: color,
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: value,
              backgroundColor: Colors.grey.shade200,
              valueColor: AlwaysStoppedAnimation(color),
              minHeight: 8,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRiskFactorsCard(AdherencePrediction p) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.warning_amber_rounded, color: Color(0xFFF59E0B)),
              SizedBox(width: 8),
              Text(
                'Risk Factors',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
            ],
          ),
          const SizedBox(height: 16),
          ...p.riskFactors.map((rf) => _buildRiskFactorItem(rf)),
        ],
      ),
    );
  }

  Widget _buildRiskFactorItem(RiskFactor rf) {
    final color = Color(rf.colorValue);
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: color,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  rf.severity.toUpperCase(),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  rf.factor,
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 14,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            rf.description,
            style: TextStyle(fontSize: 13, color: Colors.grey.shade700),
          ),
          if (rf.recommendation.isNotEmpty) ...[
            const SizedBox(height: 8),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(
                  Icons.lightbulb_outline,
                  size: 16,
                  color: Color(0xFF10B981),
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    rf.recommendation,
                    style: const TextStyle(
                      fontSize: 12,
                      color: Color(0xFF10B981),
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildSkipReasonsCard(AdherencePrediction p) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.skip_next, color: Color(0xFF8B5CF6)),
              SizedBox(width: 8),
              Text(
                'Skip Reasons (Last 30 Days)',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
            ],
          ),
          const SizedBox(height: 16),
          ...p.skipReasonBreakdown.entries.map(
            (e) => _buildSkipReasonItem(
              _formatSkipReason(e.key),
              e.value,
              p.skipReasonBreakdown.values.reduce((a, b) => a + b),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSkipReasonItem(String reason, int count, int total) {
    final percentage = total > 0 ? count / total : 0.0;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          Expanded(
            flex: 2,
            child: Text(reason, style: const TextStyle(fontSize: 13)),
          ),
          Expanded(
            flex: 3,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: percentage,
                backgroundColor: Colors.grey.shade200,
                valueColor: const AlwaysStoppedAnimation(Color(0xFF8B5CF6)),
                minHeight: 8,
              ),
            ),
          ),
          const SizedBox(width: 8),
          SizedBox(
            width: 40,
            child: Text(
              '$count',
              textAlign: TextAlign.right,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecommendationsCard(AdherencePrediction p) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.auto_awesome, color: Color(0xFF10B981)),
              SizedBox(width: 8),
              Text(
                'AI Recommendations',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
            ],
          ),
          const SizedBox(height: 16),
          MarkdownBody(
            data: p.recommendations,
            styleSheet: MarkdownStyleSheet(
              p: TextStyle(
                fontSize: 14,
                color: Colors.grey.shade800,
                height: 1.5,
              ),
              listBullet: TextStyle(fontSize: 14, color: Colors.grey.shade800),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildConfidenceCard(AdherencePrediction p) {
    final confidencePercent = (p.confidence * 100).toStringAsFixed(0);
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Icon(Icons.info_outline, color: Colors.grey.shade600, size: 20),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'Prediction confidence: $confidencePercent% based on ${p.confidence >= 0.7 ? 'comprehensive' : 'limited'} data. '
              'Complete more assessments and track doses regularly to improve accuracy.',
              style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
            ),
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
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.red.shade300),
            const SizedBox(height: 16),
            Text(
              'Could not load insights',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Colors.grey.shade800,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              _error ?? 'Unknown error',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey.shade600),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _loadPrediction,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF0EA5E9),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 12,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyView() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.analytics_outlined,
              size: 64,
              color: Colors.grey.shade400,
            ),
            const SizedBox(height: 16),
            Text(
              'No Data Available',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Colors.grey.shade800,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Complete psychological assessments and track your medications to get personalized insights.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey.shade600),
            ),
          ],
        ),
      ),
    );
  }

  Color _getRiskColor(String riskLevel) {
    switch (riskLevel) {
      case 'Low':
        return const Color(0xFF10B981);
      case 'Moderate':
        return const Color(0xFFF59E0B);
      case 'High':
        return const Color(0xFFF97316);
      case 'Very High':
        return const Color(0xFFEF4444);
      default:
        return Colors.grey;
    }
  }

  IconData _getRiskIcon(String riskLevel) {
    switch (riskLevel) {
      case 'Low':
        return Icons.check_circle;
      case 'Moderate':
        return Icons.info;
      case 'High':
        return Icons.warning;
      case 'Very High':
        return Icons.error;
      default:
        return Icons.help;
    }
  }

  String _getRiskDescription(String riskLevel) {
    switch (riskLevel) {
      case 'Low':
        return 'Great job! You\'re on track with your medication routine.';
      case 'Moderate':
        return 'There\'s room for improvement. Small changes can make a big difference.';
      case 'High':
        return 'Several factors are affecting your adherence. Review the recommendations below.';
      case 'Very High':
        return 'Your adherence needs attention. Consider speaking with your healthcare provider.';
      default:
        return '';
    }
  }

  String _formatComponentName(String name) {
    switch (name) {
      case 'pastBehavior':
        return 'Historical Adherence';
      case 'mmas8':
        return 'Self-Reported Adherence (MMAS-8)';
      case 'depression':
        return 'Mental Health (Depression)';
      case 'anxiety':
        return 'Mental Health (Anxiety)';
      case 'beliefs':
        return 'Medication Beliefs';
      case 'treatmentComplexity':
        return 'Treatment Simplicity';
      case 'temporalPatterns':
        return 'Timing Consistency';
      default:
        return name;
    }
  }

  String _formatSkipReason(String reason) {
    switch (reason) {
      case 'FORGOT':
        return 'Forgot';
      case 'SIDE_EFFECTS':
        return 'Side Effects';
      case 'FEELING_BETTER':
        return 'Feeling Better';
      case 'RAN_OUT':
        return 'Ran Out';
      case 'COST':
        return 'Cost Issues';
      case 'OTHER':
        return 'Other';
      default:
        return reason;
    }
  }
}
