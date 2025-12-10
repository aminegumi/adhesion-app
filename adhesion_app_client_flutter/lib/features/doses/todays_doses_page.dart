import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/dose_models.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Today's Doses Page - Shows all scheduled doses for today with take/skip actions
class TodaysDosesPage extends StatefulWidget {
  final String baseUrl;
  const TodaysDosesPage({super.key, required this.baseUrl});

  @override
  State<TodaysDosesPage> createState() => _TodaysDosesPageState();
}

class _TodaysDosesPageState extends State<TodaysDosesPage> {
  late final ApiClient _api;
  List<DoseLog> _doses = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _loadDoses();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _loadDoses() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      if (userId == null) {
        setState(() => _error = 'Please log in first');
        return;
      }

      final doses = await _api.getTodaysDoses(userId);
      setState(() {
        _doses = doses;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  Future<void> _takeDose(DoseLog dose) async {
    try {
      final updated = await _api.takeDose(dose.id);
      setState(() {
        final index = _doses.indexWhere((d) => d.id == dose.id);
        if (index >= 0) _doses[index] = updated;
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('✓ ${dose.medicationName} taken!'),
            backgroundColor: Colors.green,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _showSkipDialog(DoseLog dose) async {
    SkipReason? selectedReason;
    final notesController = TextEditingController();

    final result = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text('Skip ${dose.medicationName}?'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Please select a reason:'),
              const SizedBox(height: 12),
              ...SkipReason.values.map(
                (reason) => RadioListTile<SkipReason>(
                  title: Text(reason.displayName),
                  value: reason,
                  groupValue: selectedReason,
                  onChanged: (v) => setDialogState(() => selectedReason = v),
                  dense: true,
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: notesController,
                decoration: const InputDecoration(
                  labelText: 'Additional notes (optional)',
                  border: OutlineInputBorder(),
                ),
                maxLines: 2,
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: selectedReason != null
                  ? () => Navigator.pop(context, true)
                  : null,
              style: ElevatedButton.styleFrom(backgroundColor: Colors.orange),
              child: const Text('Skip Dose'),
            ),
          ],
        ),
      ),
    );

    if (result == true && selectedReason != null) {
      try {
        final updated = await _api.skipDose(
          dose.id,
          selectedReason!,
          notes: notesController.text.isNotEmpty ? notesController.text : null,
        );
        setState(() {
          final index = _doses.indexWhere((d) => d.id == dose.id);
          if (index >= 0) _doses[index] = updated;
        });
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      appBar: AppBar(
        title: const Text("Today's Medications"),
        backgroundColor: const Color(0xFF6366F1),
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadDoses),
        ],
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
            ElevatedButton(onPressed: _loadDoses, child: const Text('Retry')),
          ],
        ),
      );
    }

    if (_doses.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.medication_outlined, size: 80, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              'No medications scheduled for today',
              style: TextStyle(fontSize: 18, color: Colors.grey[600]),
            ),
            const SizedBox(height: 8),
            Text(
              'Add medications in your treatment plan',
              style: TextStyle(color: Colors.grey[500]),
            ),
          ],
        ),
      );
    }

    // Separate pending, completed, and missed doses
    final pendingDoses = _doses.where((d) => d.isPending).toList();
    final completedDoses = _doses
        .where((d) => d.isTaken || d.isSkipped)
        .toList();
    final missedDoses = _doses.where((d) => d.isMissed).toList();

    return RefreshIndicator(
      onRefresh: _loadDoses,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Progress summary
          _buildProgressCard(
            pendingDoses.length,
            completedDoses.length,
            missedDoses.length,
          ),
          const SizedBox(height: 20),

          // Pending doses (action required)
          if (pendingDoses.isNotEmpty) ...[
            _buildSectionHeader('⏰ Upcoming', pendingDoses.length),
            const SizedBox(height: 8),
            ...pendingDoses.map((dose) => _buildDoseCard(dose)),
            const SizedBox(height: 20),
          ],

          // Missed doses
          if (missedDoses.isNotEmpty) ...[
            _buildSectionHeader('⚠️ Missed', missedDoses.length, Colors.red),
            const SizedBox(height: 8),
            ...missedDoses.map((dose) => _buildDoseCard(dose)),
            const SizedBox(height: 20),
          ],

          // Completed doses
          if (completedDoses.isNotEmpty) ...[
            _buildSectionHeader(
              '✓ Completed',
              completedDoses.length,
              Colors.green,
            ),
            const SizedBox(height: 8),
            ...completedDoses.map((dose) => _buildDoseCard(dose)),
          ],
        ],
      ),
    );
  }

  Widget _buildProgressCard(int pending, int completed, int missed) {
    final total = pending + completed + missed;
    final progress = total > 0 ? completed / total : 0.0;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF6366F1), Color(0xFF8B5CF6)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF6366F1).withOpacity(0.3),
            blurRadius: 12,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                "Today's Progress",
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Text(
                '${(progress * 100).toInt()}%',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 10,
              backgroundColor: Colors.white.withOpacity(0.3),
              valueColor: const AlwaysStoppedAnimation<Color>(Colors.white),
            ),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildProgressStat('Pending', pending, Colors.white70),
              _buildProgressStat('Done', completed, Colors.greenAccent),
              _buildProgressStat('Missed', missed, Colors.redAccent),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildProgressStat(String label, int count, Color color) {
    return Column(
      children: [
        Text(
          '$count',
          style: TextStyle(
            color: color,
            fontSize: 24,
            fontWeight: FontWeight.bold,
          ),
        ),
        Text(
          label,
          style: const TextStyle(color: Colors.white70, fontSize: 12),
        ),
      ],
    );
  }

  Widget _buildSectionHeader(String title, int count, [Color? color]) {
    return Row(
      children: [
        Text(
          title,
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: color ?? Colors.grey[800],
          ),
        ),
        const SizedBox(width: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
          decoration: BoxDecoration(
            color: (color ?? const Color(0xFF6366F1)).withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Text(
            '$count',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.bold,
              color: color ?? const Color(0xFF6366F1),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildDoseCard(DoseLog dose) {
    Color cardColor;
    Color borderColor;
    IconData statusIcon;

    switch (dose.status) {
      case DoseStatus.taken:
        cardColor = Colors.green.withOpacity(0.05);
        borderColor = Colors.green;
        statusIcon = Icons.check_circle;
        break;
      case DoseStatus.skipped:
        cardColor = Colors.orange.withOpacity(0.05);
        borderColor = Colors.orange;
        statusIcon = Icons.skip_next;
        break;
      case DoseStatus.missed:
        cardColor = Colors.red.withOpacity(0.05);
        borderColor = Colors.red;
        statusIcon = Icons.warning;
        break;
      default:
        cardColor = Colors.white;
        borderColor = dose.isOverdue
            ? Colors.orange
            : Colors.grey.withOpacity(0.2);
        statusIcon = dose.isOverdue ? Icons.schedule : Icons.medication;
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: cardColor,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: borderColor, width: 1.5),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            // Time and icon
            Container(
              width: 60,
              height: 60,
              decoration: BoxDecoration(
                color: borderColor.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(statusIcon, color: borderColor, size: 24),
                  const SizedBox(height: 4),
                  Text(
                    dose.formattedTime,
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: borderColor,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 16),

            // Medication info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    dose.medicationName,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  if (dose.dosage != null)
                    Text(
                      dose.dosage!,
                      style: TextStyle(color: Colors.grey[600], fontSize: 14),
                    ),
                  if (dose.isOverdue && dose.isPending)
                    Container(
                      margin: const EdgeInsets.only(top: 4),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.orange.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text(
                        'Overdue',
                        style: TextStyle(
                          color: Colors.orange,
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  if (dose.isTaken && dose.delayMinutes != null)
                    Text(
                      dose.delayMinutes! > 0
                          ? 'Taken ${dose.delayMinutes} min late'
                          : 'Taken on time',
                      style: TextStyle(color: Colors.grey[500], fontSize: 12),
                    ),
                ],
              ),
            ),

            // Action buttons
            if (dose.isPending) ...[
              IconButton(
                icon: const Icon(Icons.close, color: Colors.orange),
                onPressed: () => _showSkipDialog(dose),
                tooltip: 'Skip',
              ),
              const SizedBox(width: 4),
              ElevatedButton(
                onPressed: () => _takeDose(dose),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF10B981),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                ),
                child: const Text(
                  'Take',
                  style: TextStyle(color: Colors.white),
                ),
              ),
            ] else
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 6,
                ),
                decoration: BoxDecoration(
                  color: borderColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  dose.status.displayName,
                  style: TextStyle(
                    color: borderColor,
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
