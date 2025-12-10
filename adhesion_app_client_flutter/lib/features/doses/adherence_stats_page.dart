import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/dose_models.dart';

/// Adherence Statistics Page - Shows detailed adherence analytics
class AdherenceStatsPage extends StatefulWidget {
  final String baseUrl;
  const AdherenceStatsPage({super.key, required this.baseUrl});

  @override
  State<AdherenceStatsPage> createState() => _AdherenceStatsPageState();
}

class _AdherenceStatsPageState extends State<AdherenceStatsPage> {
  late final ApiClient _api;
  AdherenceStats? _stats;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _loadStats();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _loadStats() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final userId = await ApiClient.getStoredUserId();
      if (userId == null) {
        setState(() {
          _error = 'Please log in first';
          _loading = false;
        });
        return;
      }

      final stats = await _api.getAdherenceStats(userId);
      setState(() {
        _stats = stats;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      appBar: AppBar(
        title: const Text('Adherence Statistics'),
        backgroundColor: const Color(0xFF6366F1),
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.red[300]),
            const SizedBox(height: 16),
            Text(_error!, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: _loadStats, child: const Text('Retry')),
          ],
        ),
      );
    }

    final stats = _stats!;

    return RefreshIndicator(
      onRefresh: _loadStats,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Overall adherence card
          _buildOverallCard(stats),
          const SizedBox(height: 16),

          // Streak and trend
          Row(
            children: [
              Expanded(child: _buildStreakCard(stats)),
              const SizedBox(width: 12),
              Expanded(child: _buildTrendCard(stats)),
            ],
          ),
          const SizedBox(height: 16),

          // Dose breakdown
          _buildDoseBreakdownCard(stats),
          const SizedBox(height: 16),

          // By medication
          if (stats.byMedication.isNotEmpty) ...[
            _buildMedicationBreakdown(stats),
            const SizedBox(height: 16),
          ],

          // By time of day
          if (stats.byTimeOfDay.isNotEmpty) ...[
            _buildTimeOfDayBreakdown(stats),
            const SizedBox(height: 16),
          ],

          // Skip reasons
          if (stats.skipReasonCounts.isNotEmpty) ...[
            _buildSkipReasonsCard(stats),
          ],
        ],
      ),
    );
  }

  Widget _buildOverallCard(AdherenceStats stats) {
    final rate = stats.overallAdherenceRate;
    Color progressColor;
    String emoji;
    String message;

    if (rate >= 90) {
      progressColor = const Color(0xFF10B981);
      emoji = '🌟';
      message = 'Excellent adherence!';
    } else if (rate >= 70) {
      progressColor = const Color(0xFF3B82F6);
      emoji = '👍';
      message = 'Good progress, keep it up!';
    } else if (rate >= 50) {
      progressColor = Colors.orange;
      emoji = '💪';
      message = 'Room for improvement';
    } else {
      progressColor = Colors.red;
      emoji = '📈';
      message = "Let's work on this together";
    }

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [progressColor, progressColor.withOpacity(0.8)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: progressColor.withOpacity(0.4),
            blurRadius: 15,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        children: [
          Text(emoji, style: const TextStyle(fontSize: 48)),
          const SizedBox(height: 8),
          Text(
            '${rate.toStringAsFixed(0)}%',
            style: const TextStyle(
              fontSize: 56,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          Text(
            'Overall Adherence (30 days)',
            style: TextStyle(
              color: Colors.white.withOpacity(0.9),
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.2),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(
              message,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStreakCard(AdherenceStats stats) {
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
        children: [
          const Icon(
            Icons.local_fire_department,
            color: Colors.orange,
            size: 36,
          ),
          const SizedBox(height: 8),
          Text(
            '${stats.currentStreak}',
            style: const TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.bold,
              color: Colors.orange,
            ),
          ),
          Text(
            'Day Streak',
            style: TextStyle(color: Colors.grey[600], fontSize: 14),
          ),
        ],
      ),
    );
  }

  Widget _buildTrendCard(AdherenceStats stats) {
    final isPositive = stats.trendPercentage >= 0;
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
        children: [
          Icon(
            isPositive ? Icons.trending_up : Icons.trending_down,
            color: isPositive ? Colors.green : Colors.red,
            size: 36,
          ),
          const SizedBox(height: 8),
          Text(
            '${isPositive ? '+' : ''}${stats.trendPercentage.toStringAsFixed(1)}%',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: isPositive ? Colors.green : Colors.red,
            ),
          ),
          Text(
            'Week Trend',
            style: TextStyle(color: Colors.grey[600], fontSize: 14),
          ),
        ],
      ),
    );
  }

  Widget _buildDoseBreakdownCard(AdherenceStats stats) {
    final total =
        stats.totalDosesTaken +
        stats.totalDosesMissed +
        stats.totalDosesSkipped;

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
          const Text(
            'Dose Breakdown',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildDoseStatItem(
                'Taken',
                stats.totalDosesTaken,
                Colors.green,
                Icons.check_circle,
              ),
              _buildDoseStatItem(
                'Missed',
                stats.totalDosesMissed,
                Colors.red,
                Icons.cancel,
              ),
              _buildDoseStatItem(
                'Skipped',
                stats.totalDosesSkipped,
                Colors.orange,
                Icons.skip_next,
              ),
            ],
          ),
          const SizedBox(height: 16),
          // Progress bar
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: Row(
              children: [
                if (stats.totalDosesTaken > 0)
                  Expanded(
                    flex: stats.totalDosesTaken,
                    child: Container(height: 8, color: Colors.green),
                  ),
                if (stats.totalDosesSkipped > 0)
                  Expanded(
                    flex: stats.totalDosesSkipped,
                    child: Container(height: 8, color: Colors.orange),
                  ),
                if (stats.totalDosesMissed > 0)
                  Expanded(
                    flex: stats.totalDosesMissed,
                    child: Container(height: 8, color: Colors.red),
                  ),
                if (total == 0)
                  Expanded(
                    child: Container(height: 8, color: Colors.grey[300]),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDoseStatItem(
    String label,
    int count,
    Color color,
    IconData icon,
  ) {
    return Column(
      children: [
        Icon(icon, color: color, size: 28),
        const SizedBox(height: 4),
        Text(
          '$count',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
        Text(label, style: TextStyle(color: Colors.grey[600], fontSize: 12)),
      ],
    );
  }

  Widget _buildMedicationBreakdown(AdherenceStats stats) {
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
          const Text(
            'By Medication',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),
          ...stats.byMedication.map(
            (med) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        med.medicationName,
                        style: const TextStyle(fontWeight: FontWeight.w500),
                      ),
                      Text(
                        '${med.adherenceRate.toStringAsFixed(0)}%',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: med.adherenceRate >= 80
                              ? Colors.green
                              : Colors.orange,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: med.adherenceRate / 100,
                      minHeight: 6,
                      backgroundColor: Colors.grey[200],
                      valueColor: AlwaysStoppedAnimation<Color>(
                        med.adherenceRate >= 80 ? Colors.green : Colors.orange,
                      ),
                    ),
                  ),
                  Text(
                    '${med.dosesTaken}/${med.dosesTotal} doses',
                    style: TextStyle(fontSize: 11, color: Colors.grey[500]),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTimeOfDayBreakdown(AdherenceStats stats) {
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
          const Text(
            'By Time of Day',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildTimeCard(
                'Morning',
                stats.byTimeOfDay['Morning'] ?? 0,
                Icons.wb_sunny,
                Colors.amber,
              ),
              _buildTimeCard(
                'Afternoon',
                stats.byTimeOfDay['Afternoon'] ?? 0,
                Icons.light_mode,
                Colors.orange,
              ),
              _buildTimeCard(
                'Evening',
                stats.byTimeOfDay['Evening'] ?? 0,
                Icons.nights_stay,
                Colors.indigo,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTimeCard(String label, double rate, IconData icon, Color color) {
    return Column(
      children: [
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(icon, color: color, size: 28),
        ),
        const SizedBox(height: 8),
        Text(
          '${rate.toStringAsFixed(0)}%',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: rate >= 80
                ? Colors.green
                : (rate >= 50 ? Colors.orange : Colors.red),
          ),
        ),
        Text(label, style: TextStyle(color: Colors.grey[600], fontSize: 12)),
      ],
    );
  }

  Widget _buildSkipReasonsCard(AdherenceStats stats) {
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
          const Text(
            'Skip Reasons',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),
          ...stats.skipReasonCounts.entries.map((entry) {
            final reason = SkipReason.values.firstWhere(
              (r) => r.name.toUpperCase() == entry.key.toUpperCase(),
              orElse: () => SkipReason.other,
            );
            return Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(reason.displayName),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 2,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.orange.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      '${entry.value}',
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        color: Colors.orange,
                      ),
                    ),
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }
}
