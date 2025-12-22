import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/treatment_models.dart';
import '../../main.dart';

class TreatmentPlanPage extends StatefulWidget {
  final String baseUrl;
  const TreatmentPlanPage({super.key, required this.baseUrl});

  @override
  State<TreatmentPlanPage> createState() => _TreatmentPlanPageState();
}

class _TreatmentPlanPageState extends State<TreatmentPlanPage>
    with SingleTickerProviderStateMixin {
  late final ApiClient _api;
  late TabController _tabController;
  List<TreatmentPlan> _plans = [];
  List<DailyTask> _todaysTasks = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _tabController = TabController(length: 2, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _api.close();
    super.dispose();
  }

  Future<void> _loadData() async {
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

      final plans = await _api.getUserTreatmentPlans(userId);
      final tasks = await _api.getTodaysTasks(userId);

      setState(() {
        _plans = plans;
        _todaysTasks = tasks;
      });
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _completeTask(DailyTask task) async {
    // Show confirmation dialog
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Complete Task'),
        content: Text('Mark "${task.title}" as completed?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
            child: const Text('Complete'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      await _api.completeTask(task.id);
      await _loadData();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: const [
                Icon(Icons.check_circle, color: Colors.white),
                SizedBox(width: 8),
                Text('Task completed! Great job! 🎉'),
              ],
            ),
            backgroundColor: Colors.green,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _createPlan() async {
    // Show dialog to customize plan
    final result = await showDialog<CreatePlanRequest>(
      context: context,
      builder: (ctx) => _CreatePlanDialog(),
    );

    if (result == null) return;

    setState(() => _loading = true);
    try {
      final userId = await ApiClient.getStoredUserId();
      if (userId == null) return;

      await _api.createTreatmentPlan(
        CreatePlanRequest(
          userId: userId,
          title: result.title ?? 'My Treatment Plan',
          description: result.description,
          durationWeeks: result.durationWeeks ?? 4,
          medications: result.medications,
          medicationsList: result.medicationsList,
        ),
      );
      await _loadData();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Treatment plan created successfully! 🎯'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      setState(() => _loading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final pendingCount = _todaysTasks.where((t) => !t.completed).length;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Treatment Plans'),
        backgroundColor: Colors.green.shade700,
        foregroundColor: Colors.white,
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white70,
          tabs: [
            Tab(
              icon: const Icon(Icons.today),
              text: "Today's Tasks ($pendingCount)",
            ),
            Tab(
              icon: const Icon(Icons.assignment),
              text: 'My Plans (${_plans.length})',
            ),
          ],
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? _buildErrorView()
          : TabBarView(
              controller: _tabController,
              children: [_buildTodaysTasksTab(), _buildPlansTab()],
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: _createPlan,
        backgroundColor: Colors.green,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline, size: 64, color: Colors.red[300]),
          const SizedBox(height: 16),
          Text(_error!, style: TextStyle(color: Colors.red[700])),
          const SizedBox(height: 16),
          ElevatedButton.icon(
            onPressed: _loadData,
            icon: const Icon(Icons.refresh),
            label: const Text('Retry'),
          ),
        ],
      ),
    );
  }

  Widget _buildTodaysTasksTab() {
    if (_todaysTasks.isEmpty) {
      return _buildEmptyTasksView();
    }

    final pendingTasks = _todaysTasks.where((t) => !t.completed).toList();
    final completedTasks = _todaysTasks.where((t) => t.completed).toList();

    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Summary card
          _buildDailySummaryCard(pendingTasks.length, completedTasks.length),
          const SizedBox(height: 20),

          // Pending tasks
          if (pendingTasks.isNotEmpty) ...[
            _buildSectionHeader(
              'Pending Tasks',
              Icons.pending_actions,
              Colors.orange,
            ),
            const SizedBox(height: 8),
            ...pendingTasks.map((task) => _buildTaskCard(task, false)),
            const SizedBox(height: 20),
          ],

          // Completed tasks
          if (completedTasks.isNotEmpty) ...[
            _buildSectionHeader('Completed', Icons.check_circle, Colors.green),
            const SizedBox(height: 8),
            ...completedTasks.map((task) => _buildTaskCard(task, true)),
          ],
        ],
      ),
    );
  }

  Widget _buildDailySummaryCard(int pending, int completed) {
    final total = pending + completed;
    final progress = total > 0 ? completed / total : 0.0;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Colors.green.shade600, Colors.green.shade400],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.green.withOpacity(0.3),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    "Today's Progress",
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '$completed of $total tasks completed',
                    style: const TextStyle(color: Colors.white70),
                  ),
                ],
              ),
              Container(
                width: 60,
                height: 60,
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  shape: BoxShape.circle,
                ),
                child: Center(
                  child: Text(
                    '${(progress * 100).toInt()}%',
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: LinearProgressIndicator(
              value: progress,
              backgroundColor: Colors.white.withOpacity(0.3),
              valueColor: const AlwaysStoppedAnimation<Color>(Colors.white),
              minHeight: 10,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title, IconData icon, Color color) {
    return Row(
      children: [
        Icon(icon, color: color, size: 20),
        const SizedBox(width: 8),
        Text(
          title,
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
      ],
    );
  }

  Widget _buildTaskCard(DailyTask task, bool isCompleted) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      elevation: isCompleted ? 0 : 2,
      color: isCompleted ? Colors.grey[100] : null,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: isCompleted
            ? BorderSide.none
            : BorderSide(color: _getTaskColor(task.category).withOpacity(0.3)),
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        leading: Container(
          width: 44,
          height: 44,
          decoration: BoxDecoration(
            color: isCompleted
                ? Colors.grey[300]
                : _getTaskColor(task.category).withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(
            _getTaskIcon(task.category),
            color: isCompleted ? Colors.grey : _getTaskColor(task.category),
          ),
        ),
        title: Text(
          task.title,
          style: TextStyle(
            fontWeight: FontWeight.w600,
            decoration: isCompleted ? TextDecoration.lineThrough : null,
            color: isCompleted ? Colors.grey : null,
          ),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Row(
            children: [
              _buildTaskChip(task.timeOfDay ?? 'Any time', Icons.access_time),
              const SizedBox(width: 8),
              if (task.durationMinutes != null)
                _buildTaskChip('${task.durationMinutes} min', Icons.timer),
            ],
          ),
        ),
        trailing: isCompleted
            ? Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: Colors.green[100],
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.check, color: Colors.green),
              )
            : InkWell(
                onTap: () => _completeTask(task),
                borderRadius: BorderRadius.circular(20),
                child: Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.green, width: 2),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.check, color: Colors.green),
                ),
              ),
      ),
    );
  }

  Widget _buildTaskChip(String text, IconData icon) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.grey[200],
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: Colors.grey[600]),
          const SizedBox(width: 4),
          Text(text, style: TextStyle(fontSize: 11, color: Colors.grey[600])),
        ],
      ),
    );
  }

  Widget _buildEmptyTasksView() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.celebration, size: 80, color: Colors.green[300]),
          const SizedBox(height: 16),
          const Text(
            'No tasks for today!',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          Text(
            _plans.isEmpty
                ? 'Create a treatment plan to get started'
                : 'Great job! You\'re all caught up! 🎉',
            style: TextStyle(color: Colors.grey[600]),
          ),
        ],
      ),
    );
  }

  Widget _buildPlansTab() {
    if (_plans.isEmpty) {
      return _buildEmptyPlansView();
    }

    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _plans.length,
        itemBuilder: (context, index) => _buildPlanCard(_plans[index]),
      ),
    );
  }

  Widget _buildEmptyPlansView() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.assignment_outlined, size: 80, color: Colors.grey[400]),
          const SizedBox(height: 16),
          const Text(
            'No Treatment Plans Yet',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          Text(
            'Create a personalized plan to improve your health',
            style: TextStyle(color: Colors.grey[600]),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: _createPlan,
            icon: const Icon(Icons.add),
            label: const Text('Create Your First Plan'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPlanCard(TreatmentPlan plan) {
    final isActive = plan.status?.toUpperCase() == 'ACTIVE';

    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      elevation: 3,
      child: InkWell(
        onTap: () => _showPlanDetails(plan),
        borderRadius: BorderRadius.circular(16),
        child: Column(
          children: [
            // Header
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: isActive
                      ? [Colors.green.shade600, Colors.green.shade400]
                      : [Colors.grey.shade600, Colors.grey.shade400],
                ),
                borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(16),
                ),
              ),
              child: Row(
                children: [
                  const Icon(Icons.healing, color: Colors.white, size: 28),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          plan.title,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          '${plan.durationWeeks ?? 4} weeks • ${plan.status ?? 'Active'}',
                          style: const TextStyle(color: Colors.white70),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      '${plan.progressPercentage}%',
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            // Body
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  // Progress bar
                  Row(
                    children: [
                      Expanded(
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: plan.progressPercentage / 100,
                            backgroundColor: Colors.grey[200],
                            valueColor: AlwaysStoppedAnimation<Color>(
                              isActive ? Colors.green : Colors.grey,
                            ),
                            minHeight: 8,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Dates
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      _buildDateInfo('Start', plan.startDate ?? 'N/A'),
                      _buildDateInfo('End', plan.endDate ?? 'N/A'),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDateInfo(String label, String date) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: TextStyle(color: Colors.grey[600], fontSize: 12)),
        const SizedBox(height: 2),
        Text(date, style: const TextStyle(fontWeight: FontWeight.w600)),
      ],
    );
  }

  void _showPlanDetails(TreatmentPlan plan) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => _PlanDetailsSheet(
        plan: plan,
        onEdit: () {
          Navigator.pop(context);
          _editPlan(plan);
        },
        onDelete: () {
          Navigator.pop(context);
          _deletePlan(plan);
        },
      ),
    );
  }

  Future<void> _editPlan(TreatmentPlan plan) async {
    final result = await showDialog<CreatePlanRequest>(
      context: context,
      builder: (ctx) => _CreatePlanDialog(existingPlan: plan),
    );

    if (result == null) return;

    setState(() => _loading = true);
    try {
      await _api.updateTreatmentPlan(
        plan.id,
        CreatePlanRequest(
          userId: plan.userId,
          title: result.title ?? plan.title,
          description: result.description ?? plan.description,
          durationWeeks: result.durationWeeks ?? plan.durationWeeks ?? 4,
          medications: result.medications,
          medicationsList: result.medicationsList,
        ),
      );
      await _loadData();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Treatment plan updated successfully! ✏️'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      setState(() => _loading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to update: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _deletePlan(TreatmentPlan plan) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete Plan'),
        content: Text('Are you sure you want to delete "${plan.title}"?\n\nThis action cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    setState(() => _loading = true);
    try {
      await _api.deleteTreatmentPlan(plan.id);
      await _loadData();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Treatment plan deleted 🗑️'),
            backgroundColor: Colors.orange,
          ),
        );
      }
    } catch (e) {
      setState(() => _loading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to delete: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  IconData _getTaskIcon(String? category) {
    switch (category?.toLowerCase()) {
      case 'medication':
        return Icons.medication;
      case 'exercise':
        return Icons.fitness_center;
      case 'mindfulness':
        return Icons.self_improvement;
      case 'wellness':
        return Icons.spa;
      default:
        return Icons.task_alt;
    }
  }

  Color _getTaskColor(String? category) {
    switch (category?.toLowerCase()) {
      case 'medication':
        return Colors.blue;
      case 'exercise':
        return Colors.orange;
      case 'mindfulness':
        return Colors.purple;
      case 'wellness':
        return Colors.teal;
      default:
        return Colors.green;
    }
  }
}

// Dialog for creating or editing a plan
class _CreatePlanDialog extends StatefulWidget {
  final TreatmentPlan? existingPlan;
  
  const _CreatePlanDialog({this.existingPlan});
  
  @override
  State<_CreatePlanDialog> createState() => _CreatePlanDialogState();
}

class _CreatePlanDialogState extends State<_CreatePlanDialog> {
  late final TextEditingController _titleController;
  late final TextEditingController _descController;
  late int _weeks;
  late final List<Medication> _medications;

  @override
  void initState() {
    super.initState();
    // Initialize with existing plan data if editing
    _titleController = TextEditingController(
      text: widget.existingPlan?.title ?? 'My Treatment Plan',
    );
    _descController = TextEditingController(
      text: widget.existingPlan?.description ?? '',
    );
    _weeks = widget.existingPlan?.durationWeeks ?? 4;
    _medications = widget.existingPlan?.medicationsList.toList() ?? [];
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descController.dispose();
    super.dispose();
  }

  void _addMedication() async {
    final medication = await showDialog<Medication>(
      context: context,
      builder: (ctx) => _AddMedicationDialog(),
    );
    if (medication != null) {
      setState(() => _medications.add(medication));
    }
  }

  void _editMedication(int index) async {
    final medication = await showDialog<Medication>(
      context: context,
      builder: (ctx) => _AddMedicationDialog(medication: _medications[index]),
    );
    if (medication != null) {
      setState(() => _medications[index] = medication);
    }
  }

  void _removeMedication(int index) {
    setState(() => _medications.removeAt(index));
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.existingPlan != null;
    
    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Container(
        width: MediaQuery.of(context).size.width * 0.9,
        constraints: const BoxConstraints(maxWidth: 500, maxHeight: 600),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.green.shade600,
                borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(16),
                ),
              ),
              child: Row(
                children: [
                  Icon(isEditing ? Icons.edit : Icons.healing, color: Colors.white),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      isEditing ? 'Edit Treatment Plan' : 'Create Treatment Plan',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.white),
                    onPressed: () => Navigator.pop(context),
                  ),
                ],
              ),
            ),
            // Content
            Flexible(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    TextField(
                      controller: _titleController,
                      decoration: const InputDecoration(
                        labelText: 'Plan Title',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.title),
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _descController,
                      decoration: const InputDecoration(
                        labelText: 'Description (optional)',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.description),
                      ),
                      maxLines: 2,
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        const Icon(Icons.calendar_today, color: Colors.grey),
                        const SizedBox(width: 8),
                        const Text('Duration: '),
                        const SizedBox(width: 8),
                        DropdownButton<int>(
                          value: _weeks,
                          items: [2, 4, 6, 8, 12]
                              .map(
                                (w) => DropdownMenuItem(
                                  value: w,
                                  child: Text('$w weeks'),
                                ),
                              )
                              .toList(),
                          onChanged: (v) => setState(() => _weeks = v ?? 4),
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),
                    // Medications section
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            Icon(Icons.medication, color: Colors.blue.shade600),
                            const SizedBox(width: 8),
                            const Text(
                              'Medications',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        TextButton.icon(
                          onPressed: _addMedication,
                          icon: const Icon(Icons.add),
                          label: const Text('Add'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    if (_medications.isEmpty)
                      Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Colors.grey[100],
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: Colors.grey[300]!),
                        ),
                        child: Row(
                          children: [
                            Icon(Icons.info_outline, color: Colors.grey[600]),
                            const SizedBox(width: 8),
                            const Text('No medications added yet'),
                          ],
                        ),
                      )
                    else
                      ..._medications.asMap().entries.map((entry) {
                        final idx = entry.key;
                        final med = entry.value;
                        return _buildMedicationCard(med, idx);
                      }),
                  ],
                ),
              ),
            ),
            // Actions
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: const Text('Cancel'),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton.icon(
                    onPressed: () {
                      Navigator.pop(
                        context,
                        CreatePlanRequest(
                          userId: 0, // Will be set later
                          title: _titleController.text,
                          description: _descController.text.isNotEmpty
                              ? _descController.text
                              : null,
                          medicationsList: _medications.isNotEmpty
                              ? _medications
                              : null,
                          durationWeeks: _weeks,
                        ),
                      );
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.green,
                      foregroundColor: Colors.white,
                    ),
                    icon: const Icon(Icons.check),
                    label: const Text('Create Plan'),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMedicationCard(Medication med, int index) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: med.isChronic ? Colors.purple[100] : Colors.blue[100],
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(
            Icons.medication,
            color: med.isChronic ? Colors.purple : Colors.blue,
          ),
        ),
        title: Text(
          med.name,
          style: const TextStyle(fontWeight: FontWeight.w600),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (med.dosage != null) Text(med.dosage!),
            Row(
              children: [
                Icon(Icons.schedule, size: 14, color: Colors.grey[600]),
                const SizedBox(width: 4),
                Text(
                  '${med.timesPerDay}x/day',
                  style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                ),
                if (med.isChronic) ...[
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 6,
                      vertical: 2,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.purple[50],
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: const Text(
                      'Chronic',
                      style: TextStyle(fontSize: 10, color: Colors.purple),
                    ),
                  ),
                ],
              ],
            ),
            if (med.scheduledTimes.isNotEmpty)
              Text(
                'Times: ${med.scheduledTimes.join(", ")}',
                style: TextStyle(fontSize: 11, color: Colors.grey[600]),
              ),
          ],
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(
              icon: const Icon(Icons.edit, size: 20),
              onPressed: () => _editMedication(index),
            ),
            IconButton(
              icon: const Icon(Icons.delete, size: 20, color: Colors.red),
              onPressed: () => _removeMedication(index),
            ),
          ],
        ),
        isThreeLine: true,
      ),
    );
  }
}

// Dialog for adding/editing a medication
class _AddMedicationDialog extends StatefulWidget {
  final Medication? medication;

  const _AddMedicationDialog({this.medication});

  @override
  State<_AddMedicationDialog> createState() => _AddMedicationDialogState();
}

class _AddMedicationDialogState extends State<_AddMedicationDialog> {
  final _nameController = TextEditingController();
  final _dosageController = TextEditingController();
  final _instructionsController = TextEditingController();
  final _notesController = TextEditingController();
  int _timesPerDay = 1;
  bool _isChronic = false;
  bool _notificationsEnabled = true;
  int _reminderMinutes = 15;
  List<TimeOfDay> _scheduledTimes = [];

  @override
  void initState() {
    super.initState();
    _nameController.addListener(_onTextChanged);
    if (widget.medication != null) {
      final med = widget.medication!;
      _nameController.text = med.name;
      _dosageController.text = med.dosage ?? '';
      _instructionsController.text = med.instructions ?? '';
      _notesController.text = med.notes ?? '';
      _timesPerDay = med.timesPerDay;
      _isChronic = med.isChronic;
      _notificationsEnabled = med.notificationsEnabled;
      _reminderMinutes = med.reminderMinutesBefore;
      _scheduledTimes = med.scheduledTimes.map((t) {
        final parts = t.split(':');
        return TimeOfDay(
          hour: int.tryParse(parts[0]) ?? 8,
          minute: int.tryParse(parts.length > 1 ? parts[1] : '0') ?? 0,
        );
      }).toList();
    }
  }

  @override
  void dispose() {
    _nameController.removeListener(_onTextChanged);
    _nameController.dispose();
    _dosageController.dispose();
    _instructionsController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  void _onTextChanged() {
    setState(() {});
  }

  void _addTime() async {
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.now(),
    );
    if (time != null) {
      setState(() {
        _scheduledTimes.add(time);
        _scheduledTimes.sort((a, b) {
          final aMinutes = a.hour * 60 + a.minute;
          final bMinutes = b.hour * 60 + b.minute;
          return aMinutes.compareTo(bMinutes);
        });
      });
    }
  }

  void _removeTime(int index) {
    setState(() => _scheduledTimes.removeAt(index));
  }

  String _formatTime(TimeOfDay time) {
    final hour = time.hour.toString().padLeft(2, '0');
    final minute = time.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Container(
        width: MediaQuery.of(context).size.width * 0.9,
        constraints: const BoxConstraints(maxWidth: 450, maxHeight: 550),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.blue.shade600,
                borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(16),
                ),
              ),
              child: Row(
                children: [
                  const Icon(Icons.medication, color: Colors.white),
                  const SizedBox(width: 8),
                  Text(
                    widget.medication != null
                        ? 'Edit Medication'
                        : 'Add Medication',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),
            // Content
            Flexible(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    TextField(
                      controller: _nameController,
                      decoration: const InputDecoration(
                        labelText: 'Medication Name *',
                        border: OutlineInputBorder(),
                        hintText: 'e.g., Metformin',
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _dosageController,
                      decoration: const InputDecoration(
                        labelText: 'Dosage',
                        border: OutlineInputBorder(),
                        hintText: 'e.g., 500mg',
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _instructionsController,
                      decoration: const InputDecoration(
                        labelText: 'Instructions',
                        border: OutlineInputBorder(),
                        hintText: 'e.g., Take with food',
                      ),
                    ),
                    const SizedBox(height: 16),
                    // Times per day
                    Row(
                      children: [
                        const Text('Times per day: '),
                        const SizedBox(width: 8),
                        DropdownButton<int>(
                          value: _timesPerDay,
                          items: [1, 2, 3, 4, 5, 6]
                              .map(
                                (n) => DropdownMenuItem(
                                  value: n,
                                  child: Text('$n'),
                                ),
                              )
                              .toList(),
                          onChanged: (v) {
                            setState(() => _timesPerDay = v ?? 1);
                          },
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    // Chronic switch
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('Chronic Medication'),
                      subtitle: const Text('Long-term treatment'),
                      value: _isChronic,
                      onChanged: (v) => setState(() => _isChronic = v),
                      activeColor: Colors.purple,
                    ),
                    // Notifications switch
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('Enable Notifications'),
                      subtitle: const Text('Get reminded for each dose'),
                      value: _notificationsEnabled,
                      onChanged: (v) =>
                          setState(() => _notificationsEnabled = v),
                      activeColor: Colors.blue,
                    ),
                    if (_notificationsEnabled) ...[
                      Row(
                        children: [
                          const Text('Remind me: '),
                          DropdownButton<int>(
                            value: _reminderMinutes,
                            items: [5, 10, 15, 30, 60]
                                .map(
                                  (m) => DropdownMenuItem(
                                    value: m,
                                    child: Text('$m min before'),
                                  ),
                                )
                                .toList(),
                            onChanged: (v) {
                              setState(() => _reminderMinutes = v ?? 15);
                            },
                          ),
                        ],
                      ),
                    ],
                    const SizedBox(height: 12),
                    // Scheduled times
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'Scheduled Times:',
                          style: TextStyle(fontWeight: FontWeight.w600),
                        ),
                        TextButton.icon(
                          onPressed: _addTime,
                          icon: const Icon(Icons.add, size: 18),
                          label: const Text('Add Time'),
                        ),
                      ],
                    ),
                    if (_scheduledTimes.isEmpty)
                      Text(
                        'No times set - tap "Add Time" to schedule',
                        style: TextStyle(color: Colors.grey[600], fontSize: 12),
                      )
                    else
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: _scheduledTimes.asMap().entries.map((entry) {
                          final idx = entry.key;
                          final time = entry.value;
                          return Chip(
                            label: Text(_formatTime(time)),
                            deleteIcon: const Icon(Icons.close, size: 16),
                            onDeleted: () => _removeTime(idx),
                            backgroundColor: Colors.blue[50],
                          );
                        }).toList(),
                      ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _notesController,
                      decoration: const InputDecoration(
                        labelText: 'Notes (optional)',
                        border: OutlineInputBorder(),
                      ),
                      maxLines: 2,
                    ),
                  ],
                ),
              ),
            ),
            // Actions
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: const Text('Cancel'),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton(
                    onPressed: _nameController.text.trim().isEmpty
                        ? null
                        : () {
                            Navigator.pop(
                              context,
                              Medication(
                                name: _nameController.text.trim(),
                                dosage: _dosageController.text.isNotEmpty
                                    ? _dosageController.text
                                    : null,
                                instructions:
                                    _instructionsController.text.isNotEmpty
                                    ? _instructionsController.text
                                    : null,
                                timesPerDay: _timesPerDay,
                                isChronic: _isChronic,
                                scheduledTimes: _scheduledTimes
                                    .map((t) => _formatTime(t))
                                    .toList(),
                                notes: _notesController.text.isNotEmpty
                                    ? _notesController.text
                                    : null,
                                notificationsEnabled: _notificationsEnabled,
                                reminderMinutesBefore: _reminderMinutes,
                              ),
                            );
                          },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.blue,
                      foregroundColor: Colors.white,
                    ),
                    child: Text(widget.medication != null ? 'Update' : 'Add'),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// Plan details bottom sheet
class _PlanDetailsSheet extends StatefulWidget {
  final TreatmentPlan plan;
  final VoidCallback? onEdit;
  final VoidCallback? onDelete;

  const _PlanDetailsSheet({
    required this.plan,
    this.onEdit,
    this.onDelete,
  });

  @override
  State<_PlanDetailsSheet> createState() => _PlanDetailsSheetState();
}

class _PlanDetailsSheetState extends State<_PlanDetailsSheet> {
  bool _isSyncing = false;

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.7,
      maxChildSize: 0.95,
      minChildSize: 0.5,
      builder: (context, scrollController) => Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: Column(
          children: [
            // Handle
            Container(
              margin: const EdgeInsets.only(top: 12),
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey[300],
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            // Header
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [Colors.green.shade600, Colors.green.shade400],
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    widget.plan.title,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          const Icon(
                            Icons.calendar_today,
                            size: 16,
                            color: Colors.white70,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            '${widget.plan.startDate ?? 'N/A'} - ${widget.plan.endDate ?? 'N/A'}',
                            style: const TextStyle(color: Colors.white70),
                          ),
                        ],
                      ),
                      // Action buttons row
                      Row(
                        children: [
                          // Edit button
                          if (widget.onEdit != null)
                            IconButton(
                              onPressed: widget.onEdit,
                              icon: const Icon(Icons.edit, color: Colors.white, size: 20),
                              tooltip: 'Edit Plan',
                              style: IconButton.styleFrom(
                                backgroundColor: Colors.white.withOpacity(0.2),
                              ),
                            ),
                          const SizedBox(width: 8),
                          // Delete button
                          if (widget.onDelete != null)
                            IconButton(
                              onPressed: widget.onDelete,
                              icon: const Icon(Icons.delete, color: Colors.white, size: 20),
                              tooltip: 'Delete Plan',
                              style: IconButton.styleFrom(
                                backgroundColor: Colors.red.withOpacity(0.5),
                              ),
                            ),
                          const SizedBox(width: 8),
                          // Sync medications button
                          if (widget.plan.medicationsList.isNotEmpty)
                            ElevatedButton.icon(
                              onPressed: _isSyncing ? null : _syncMedications,
                              icon: _isSyncing
                                  ? const SizedBox(
                                      width: 16,
                                      height: 16,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                        valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                                      ),
                                    )
                                  : const Icon(Icons.sync, size: 16),
                              label: Text(_isSyncing ? 'Syncing...' : 'Sync Meds'),
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.white.withOpacity(0.2),
                                foregroundColor: Colors.white,
                                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                textStyle: const TextStyle(fontSize: 12),
                              ),
                            ),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),
            // Content
            Expanded(
              child: ListView(
                controller: scrollController,
                padding: const EdgeInsets.all(20),
                children: [
                  // Progress
                  _buildSection(
                    'Progress',
                    Icons.trending_up,
                    child: Column(
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            const Text('Completion'),
                            Text(
                              '${widget.plan.progressPercentage}%',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: widget.plan.progressPercentage / 100,
                            backgroundColor: Colors.grey[200],
                            valueColor: const AlwaysStoppedAnimation<Color>(
                              Colors.green,
                            ),
                            minHeight: 10,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Focus Areas
                  if (widget.plan.identifiedIssues != null &&
                      widget.plan.identifiedIssues!.isNotEmpty)
                    _buildSection(
                      'Focus Areas',
                      Icons.center_focus_strong,
                      child: Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: widget.plan.identifiedIssues!
                            .split(',')
                            .map(
                              (issue) => Chip(
                                label: Text(issue.trim()),
                                backgroundColor: Colors.green[50],
                              ),
                            )
                            .toList(),
                      ),
                    ),
                  const SizedBox(height: 20),

                  // Medications List (detailed)
                  if (widget.plan.medicationsList.isNotEmpty)
                    _buildSection(
                      'Medications (${widget.plan.medicationsList.length})',
                      Icons.medication,
                      child: Column(
                        children: widget.plan.medicationsList
                            .map((med) => _buildMedicationCard(med))
                            .toList(),
                      ),
                    )
                  else if (widget.plan.medications != null &&
                      widget.plan.medications!.isNotEmpty)
                    _buildSection(
                      'Medications',
                      Icons.medication,
                      child: Text(widget.plan.medications!),
                    ),
                  const SizedBox(height: 20),

                  // Plan Content (formatted)
                  if (widget.plan.planContent != null)
                    _buildSection(
                      'Plan Details',
                      Icons.description,
                      child: _buildFormattedContent(widget.plan.planContent!),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _syncMedications() async {
    setState(() => _isSyncing = true);
    try {
      final userId = await ApiClient.getStoredUserId();
      if (userId == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('User not found')),
        );
        return;
      }

      final apiClient = ApiClient(baseUrl: AdhesionApp.effectiveBaseUrl());
      final syncedMeds = await apiClient.syncMedicationsFromTreatmentPlan(
        widget.plan.id,
        userId,
      );

      if (syncedMeds.isNotEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Synced ${syncedMeds.length} medication(s) to your list')),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('All medications already synced')),
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to sync medications: $e')),
      );
    } finally {
      setState(() => _isSyncing = false);
    }
  }

  Widget _buildSection(String title, IconData icon, {required Widget child}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(icon, color: Colors.green, size: 20),
            const SizedBox(width: 8),
            Text(
              title,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        const SizedBox(height: 12),
        child,
      ],
    );
  }

  Widget _buildMedicationCard(Medication med) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: med.isChronic
                        ? Colors.purple[100]
                        : Colors.blue[100],
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(
                    Icons.medication,
                    color: med.isChronic ? Colors.purple : Colors.blue,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        med.name,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      if (med.dosage != null)
                        Text(
                          med.dosage!,
                          style: TextStyle(
                            color: Colors.grey[600],
                            fontSize: 14,
                          ),
                        ),
                    ],
                  ),
                ),
                if (med.isChronic)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.purple[50],
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.purple[200]!),
                    ),
                    child: const Text(
                      'Chronic',
                      style: TextStyle(
                        fontSize: 11,
                        color: Colors.purple,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            // Schedule info
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.grey[50],
                borderRadius: BorderRadius.circular(8),
              ),
              child: Column(
                children: [
                  Row(
                    children: [
                      Icon(Icons.repeat, size: 16, color: Colors.grey[600]),
                      const SizedBox(width: 8),
                      Text(
                        '${med.timesPerDay} time${med.timesPerDay > 1 ? 's' : ''} per day',
                        style: TextStyle(color: Colors.grey[700]),
                      ),
                    ],
                  ),
                  if (med.scheduledTimes.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Icon(Icons.schedule, size: 16, color: Colors.grey[600]),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Wrap(
                            spacing: 6,
                            runSpacing: 4,
                            children: med.scheduledTimes.map((time) {
                              return Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 8,
                                  vertical: 4,
                                ),
                                decoration: BoxDecoration(
                                  color: Colors.blue[50],
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: Text(
                                  time,
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.blue[700],
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              );
                            }).toList(),
                          ),
                        ),
                      ],
                    ),
                  ],
                  if (med.notificationsEnabled) ...[
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Icon(
                          Icons.notifications_active,
                          size: 16,
                          color: Colors.orange[600],
                        ),
                        const SizedBox(width: 8),
                        Text(
                          'Reminder ${med.reminderMinutesBefore} min before',
                          style: TextStyle(
                            color: Colors.orange[700],
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
            if (med.instructions != null && med.instructions!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.info_outline, size: 16, color: Colors.grey[600]),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      med.instructions!,
                      style: TextStyle(color: Colors.grey[700], fontSize: 13),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildFormattedContent(String content) {
    // Parse markdown-like content
    final lines = content.split('\n');
    final widgets = <Widget>[];

    for (final line in lines) {
      if (line.startsWith('# ')) {
        widgets.add(
          Padding(
            padding: const EdgeInsets.only(top: 12, bottom: 8),
            child: Text(
              line.substring(2),
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
          ),
        );
      } else if (line.startsWith('## ')) {
        widgets.add(
          Padding(
            padding: const EdgeInsets.only(top: 10, bottom: 6),
            child: Text(
              line.substring(3),
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Colors.green,
              ),
            ),
          ),
        );
      } else if (line.startsWith('### ')) {
        widgets.add(
          Padding(
            padding: const EdgeInsets.only(top: 8, bottom: 4),
            child: Text(
              line.substring(4),
              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
            ),
          ),
        );
      } else if (line.startsWith('- ')) {
        widgets.add(
          Padding(
            padding: const EdgeInsets.only(left: 8, top: 2, bottom: 2),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('• ', style: TextStyle(fontSize: 14)),
                Expanded(child: Text(line.substring(2))),
              ],
            ),
          ),
        );
      } else if (line.trim().isNotEmpty) {
        widgets.add(
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 2),
            child: Text(line),
          ),
        );
      }
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: widgets,
    );
  }
}
