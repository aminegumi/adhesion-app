import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/dashboard_models.dart';
import '../../core/models/test_models.dart';
import '../../core/models/recommendation_models.dart';

/// Represents a single activity item in the history log
class ActivityItem {
  final String type; // 'test', 'prediction', 'adherence', 'recommendation'
  final String title;
  final String subtitle;
  final DateTime date;
  final IconData icon;
  final Color color;
  final Map<String, dynamic>? metadata;

  ActivityItem({
    required this.type,
    required this.title,
    required this.subtitle,
    required this.date,
    required this.icon,
    required this.color,
    this.metadata,
  });
}

class HistoryPage extends StatefulWidget {
  final String baseUrl;
  const HistoryPage({super.key, required this.baseUrl});

  @override
  State<HistoryPage> createState() => _HistoryPageState();
}

class _HistoryPageState extends State<HistoryPage> {
  late final ApiClient _api;
  List<ActivityItem> _activities = [];
  bool _loading = true;
  String? _error;
  String _selectedFilter = 'all';

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _loadActivities();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  /// Helper to parse date strings
  DateTime _parseDate(String? dateStr) {
    if (dateStr == null) return DateTime.now();
    try {
      return DateTime.parse(dateStr);
    } catch (e) {
      return DateTime.now();
    }
  }

  Future<void> _loadActivities() async {
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

      final activities = <ActivityItem>[];

      // Load test history
      try {
        final tests = await _api.getTestHistory(userId);
        for (final test in tests) {
          activities.add(
            ActivityItem(
              type: 'test',
              title: 'Completed: ${test.testTitle}',
              subtitle: 'Score: ${test.totalScore}',
              date: _parseDate(test.submittedAt),
              icon: Icons.psychology_rounded,
              color: const Color(0xFF8B5CF6),
              metadata: {'testResult': test},
            ),
          );
        }
      } catch (e) {
        print('Failed to load test history: $e');
      }

      // Load predictions history
      try {
        final predictions = await _api.getPredictionsHistory(userId);
        for (final pred in predictions) {
          final riskPercent = (pred.probNonAdherence * 100).toStringAsFixed(0);
          activities.add(
            ActivityItem(
              type: 'prediction',
              title: 'AI Prediction Generated',
              subtitle: 'Non-adherence risk: $riskPercent%',
              date: DateTime.parse(pred.date),
              icon: Icons.insights_rounded,
              color: const Color(0xFF0EA5E9),
              metadata: {'prediction': pred},
            ),
          );
        }
      } catch (e) {
        print('Failed to load predictions: $e');
      }

      // Load adherence history
      try {
        final now = DateTime.now();
        final from = _formatDate(now.subtract(const Duration(days: 90)));
        final to = _formatDate(now);
        final adherenceHistory = await _api.getAdherenceHistory(
          userId: userId,
          from: from,
          to: to,
        );
        for (final entry in adherenceHistory) {
          activities.add(
            ActivityItem(
              type: 'adherence',
              title: 'Adherence Recorded',
              subtitle:
                  'Score: ${entry.adherenceScore.toStringAsFixed(0)}% • Completion: ${entry.completionRate.toStringAsFixed(0)}%',
              date: DateTime.parse(entry.date),
              icon: Icons.check_circle_rounded,
              color: const Color(0xFF10B981),
              metadata: {'adherence': entry},
            ),
          );
        }
      } catch (e) {
        print('Failed to load adherence history: $e');
      }

      // Load recommendations
      try {
        final recommendations = await _api.getAllRecommendations(userId);
        for (final rec in recommendations) {
          DateTime recDate = DateTime.now();
          if (rec.createdAt != null) {
            try {
              recDate = DateTime.parse(rec.createdAt!);
            } catch (_) {}
          }
          activities.add(
            ActivityItem(
              type: 'recommendation',
              title: rec.title,
              subtitle: 'Status: ${rec.status ?? "Active"} • ${rec.category}',
              date: recDate,
              icon: Icons.lightbulb_rounded,
              color: const Color(0xFFF59E0B),
              metadata: {'recommendation': rec},
            ),
          );
        }
      } catch (e) {
        print('Failed to load recommendations: $e');
      }

      // Sort by date (newest first)
      activities.sort((a, b) => b.date.compareTo(a.date));

      setState(() => _activities = activities);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  String _formatDate(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  List<ActivityItem> get _filteredActivities {
    if (_selectedFilter == 'all') return _activities;
    return _activities.where((a) => a.type == _selectedFilter).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      body: CustomScrollView(
        slivers: [
          _buildAppBar(),
          SliverToBoxAdapter(child: _buildFilterChips()),
          if (_loading)
            const SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(color: Color(0xFF6366F1)),
              ),
            )
          else if (_error != null)
            SliverFillRemaining(child: _buildErrorView())
          else if (_filteredActivities.isEmpty)
            SliverFillRemaining(child: _buildEmptyView())
          else
            _buildActivityList(),
        ],
      ),
    );
  }

  Widget _buildAppBar() {
    return SliverAppBar(
      expandedHeight: 120,
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
          child: const Icon(
            Icons.arrow_back_ios_new,
            color: Colors.white,
            size: 18,
          ),
        ),
        onPressed: () => Navigator.pop(context),
      ),
      actions: [
        IconButton(
          icon: Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.2),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Icon(Icons.refresh_rounded, color: Colors.white),
          ),
          onPressed: _loadActivities,
        ),
        const SizedBox(width: 8),
      ],
      flexibleSpace: FlexibleSpaceBar(
        titlePadding: const EdgeInsets.only(left: 20, bottom: 16),
        title: const Text(
          'Activity History',
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
              colors: [Color(0xFF6366F1), Color(0xFF8B5CF6)],
            ),
          ),
          child: Stack(
            children: [
              Positioned(
                right: -30,
                top: -30,
                child: Container(
                  width: 120,
                  height: 120,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: Colors.white.withOpacity(0.1),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFilterChips() {
    final filters = [
      {'key': 'all', 'label': 'All', 'icon': Icons.list_rounded},
      {'key': 'test', 'label': 'Tests', 'icon': Icons.psychology_rounded},
      {
        'key': 'prediction',
        'label': 'Predictions',
        'icon': Icons.insights_rounded,
      },
      {
        'key': 'adherence',
        'label': 'Adherence',
        'icon': Icons.check_circle_rounded,
      },
      {
        'key': 'recommendation',
        'label': 'Tips',
        'icon': Icons.lightbulb_rounded,
      },
    ];

    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: Row(
          children: filters.map((f) {
            final isSelected = _selectedFilter == f['key'];
            return Padding(
              padding: const EdgeInsets.only(right: 8),
              child: FilterChip(
                selected: isSelected,
                label: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      f['icon'] as IconData,
                      size: 16,
                      color: isSelected
                          ? Colors.white
                          : const Color(0xFF6366F1),
                    ),
                    const SizedBox(width: 6),
                    Text(f['label'] as String),
                  ],
                ),
                labelStyle: TextStyle(
                  color: isSelected ? Colors.white : const Color(0xFF374151),
                  fontWeight: FontWeight.w500,
                ),
                backgroundColor: Colors.white,
                selectedColor: const Color(0xFF6366F1),
                checkmarkColor: Colors.white,
                showCheckmark: false,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20),
                  side: BorderSide(
                    color: isSelected
                        ? const Color(0xFF6366F1)
                        : const Color(0xFFE5E7EB),
                  ),
                ),
                onSelected: (selected) {
                  setState(() => _selectedFilter = f['key'] as String);
                },
              ),
            );
          }).toList(),
        ),
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
              child: Icon(
                Icons.error_outline,
                size: 48,
                color: Colors.red.shade400,
              ),
            ),
            const SizedBox(height: 24),
            Text(
              _error!,
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey.shade700, fontSize: 16),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _loadActivities,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF6366F1),
                foregroundColor: Colors.white,
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
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(28),
              decoration: BoxDecoration(
                color: const Color(0xFF6366F1).withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.history_rounded,
                size: 64,
                color: Color(0xFF6366F1),
              ),
            ),
            const SizedBox(height: 32),
            const Text(
              'No Activity Yet',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1F2937),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'Complete tests and track your medication\nadherence to see your activity here',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Colors.grey.shade600,
                fontSize: 15,
                height: 1.5,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActivityList() {
    final activities = _filteredActivities;

    // Group activities by date
    final Map<String, List<ActivityItem>> groupedActivities = {};
    for (final activity in activities) {
      final dateKey = _getDateLabel(activity.date);
      groupedActivities.putIfAbsent(dateKey, () => []).add(activity);
    }

    return SliverPadding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate((context, index) {
          final dateKey = groupedActivities.keys.elementAt(index);
          final items = groupedActivities[dateKey]!;
          return _buildDateSection(dateKey, items);
        }, childCount: groupedActivities.length),
      ),
    );
  }

  String _getDateLabel(DateTime date) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final yesterday = today.subtract(const Duration(days: 1));
    final activityDate = DateTime(date.year, date.month, date.day);

    if (activityDate == today) {
      return 'Today';
    } else if (activityDate == yesterday) {
      return 'Yesterday';
    } else if (now.difference(date).inDays < 7) {
      return _weekdayName(date.weekday);
    } else {
      return '${date.day}/${date.month}/${date.year}';
    }
  }

  String _weekdayName(int weekday) {
    const days = [
      '',
      'Monday',
      'Tuesday',
      'Wednesday',
      'Thursday',
      'Friday',
      'Saturday',
      'Sunday',
    ];
    return days[weekday];
  }

  Widget _buildDateSection(String dateLabel, List<ActivityItem> activities) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 12),
          child: Text(
            dateLabel,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: Color(0xFF374151),
            ),
          ),
        ),
        ...activities.map((activity) => _buildActivityCard(activity)),
      ],
    );
  }

  Widget _buildActivityCard(ActivityItem activity) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () => _showActivityDetails(activity),
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: activity.color.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(activity.icon, color: activity.color, size: 24),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        activity.title,
                        style: const TextStyle(
                          fontWeight: FontWeight.w600,
                          fontSize: 15,
                          color: Color(0xFF1F2937),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        activity.subtitle,
                        style: TextStyle(
                          color: Colors.grey.shade600,
                          fontSize: 13,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        _formatTime(activity.date),
                        style: TextStyle(
                          color: Colors.grey.shade400,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                Icon(Icons.chevron_right_rounded, color: Colors.grey.shade400),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String _formatTime(DateTime date) {
    final hour = date.hour.toString().padLeft(2, '0');
    final minute = date.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  void _showActivityDetails(ActivityItem activity) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        height: MediaQuery.of(context).size.height * 0.5,
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
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
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: activity.color.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Icon(
                            activity.icon,
                            color: activity.color,
                            size: 28,
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _getTypeLabel(activity.type),
                                style: TextStyle(
                                  color: activity.color,
                                  fontWeight: FontWeight.w600,
                                  fontSize: 13,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                activity.title,
                                style: const TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFF1F2937),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),
                    _buildDetailRow(
                      Icons.calendar_today_rounded,
                      'Date',
                      _formatFullDate(activity.date),
                    ),
                    const SizedBox(height: 12),
                    _buildDetailRow(
                      Icons.access_time_rounded,
                      'Time',
                      _formatTime(activity.date),
                    ),
                    const SizedBox(height: 12),
                    _buildDetailRow(
                      Icons.info_outline_rounded,
                      'Details',
                      activity.subtitle,
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _getTypeLabel(String type) {
    switch (type) {
      case 'test':
        return 'Psychological Test';
      case 'prediction':
        return 'AI Prediction';
      case 'adherence':
        return 'Adherence Record';
      case 'recommendation':
        return 'Health Recommendation';
      default:
        return 'Activity';
    }
  }

  String _formatFullDate(DateTime date) {
    const months = [
      '',
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];
    return '${months[date.month]} ${date.day}, ${date.year}';
  }

  Widget _buildDetailRow(IconData icon, String label, String value) {
    return Row(
      children: [
        Icon(icon, size: 20, color: Colors.grey.shade500),
        const SizedBox(width: 12),
        Text(
          '$label: ',
          style: TextStyle(
            color: Colors.grey.shade600,
            fontWeight: FontWeight.w500,
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: const TextStyle(
              color: Color(0xFF1F2937),
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    );
  }
}
