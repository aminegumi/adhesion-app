import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/recommendation_models.dart';

class RecommendationsPage extends StatefulWidget {
  final String baseUrl;
  const RecommendationsPage({super.key, required this.baseUrl});

  @override
  State<RecommendationsPage> createState() => _RecommendationsPageState();
}

class _RecommendationsPageState extends State<RecommendationsPage> {
  late final ApiClient _api;
  List<Recommendation> _recommendations = [];
  bool _loading = true;
  bool _generating = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _loadRecommendations();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _loadRecommendations() async {
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

      final recommendations = await _api.getActiveRecommendations(userId);
      setState(() => _recommendations = recommendations);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _generateRecommendations() async {
    setState(() => _generating = true);

    try {
      final userId = await ApiClient.getStoredUserId();
      if (userId == null) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Please log in first')));
        return;
      }

      final recommendations = await _api.generateRecommendations(userId);
      setState(() => _recommendations = recommendations);

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Generated ${recommendations.length} recommendations!'),
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Failed to generate: $e')));
    } finally {
      setState(() => _generating = false);
    }
  }

  Future<void> _completeRecommendation(Recommendation rec) async {
    try {
      await _api.completeRecommendation(rec.id);
      _loadRecommendations();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Recommendation completed! 🎉')),
      );
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Failed: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Recommendations'),
        backgroundColor: Colors.teal,
        foregroundColor: Colors.white,
        actions: [
          if (!_generating)
            IconButton(
              icon: const Icon(Icons.auto_awesome),
              onPressed: _generateRecommendations,
              tooltip: 'Generate AI Recommendations',
            )
          else
            const Padding(
              padding: EdgeInsets.all(12),
              child: SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Colors.white,
                ),
              ),
            ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? Center(child: Text(_error!))
          : _recommendations.isEmpty
          ? _buildEmpty()
          : _buildList(),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.lightbulb_outline, size: 80, color: Colors.grey[400]),
            const SizedBox(height: 24),
            const Text(
              'No Recommendations Yet',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Text(
              'Generate personalized AI recommendations based on your psychological profile.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _generating ? null : _generateRecommendations,
              icon: _generating
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.auto_awesome),
              label: Text(
                _generating ? 'Generating...' : 'Generate Recommendations',
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.teal,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 12,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildList() {
    // Group by category
    final Map<String, List<Recommendation>> grouped = {};
    for (final rec in _recommendations) {
      grouped.putIfAbsent(rec.category, () => []).add(rec);
    }

    return RefreshIndicator(
      onRefresh: _loadRecommendations,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: grouped.length,
        itemBuilder: (context, index) {
          final category = grouped.keys.elementAt(index);
          final recs = grouped[category]!;
          return _buildCategorySection(category, recs);
        },
      ),
    );
  }

  Widget _buildCategorySection(String category, List<Recommendation> recs) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Row(
            children: [
              Icon(
                _getCategoryIcon(category),
                color: _getCategoryColor(category),
              ),
              const SizedBox(width: 8),
              Text(
                category,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
        ...recs.map((rec) => _buildRecommendationCard(rec)),
        const SizedBox(height: 16),
      ],
    );
  }

  Widget _buildRecommendationCard(Recommendation rec) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        onTap: () => _showDetails(rec),
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                width: 4,
                height: 60,
                decoration: BoxDecoration(
                  color: _getCategoryColor(rec.category),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      rec.title,
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    if (rec.description != null) ...[
                      const SizedBox(height: 4),
                      Text(
                        rec.description!,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(color: Colors.grey[600], fontSize: 13),
                      ),
                    ],
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        _buildChip(
                          rec.difficulty ?? 'Medium',
                          _getDifficultyColor(rec.difficulty),
                        ),
                        const SizedBox(width: 8),
                        _buildChip(rec.timeFrame ?? 'Daily', Colors.blue),
                      ],
                    ),
                  ],
                ),
              ),
              IconButton(
                icon: const Icon(Icons.check_circle_outline),
                color: Colors.green,
                onPressed: () => _completeRecommendation(rec),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildChip(String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 11,
          color: color,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }

  void _showDetails(Recommendation rec) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => DraggableScrollableSheet(
        initialChildSize: 0.6,
        maxChildSize: 0.9,
        minChildSize: 0.4,
        expand: false,
        builder: (context, scrollController) => SingleChildScrollView(
          controller: scrollController,
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.grey[300],
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Icon(
                    _getCategoryIcon(rec.category),
                    color: _getCategoryColor(rec.category),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    rec.category,
                    style: TextStyle(color: _getCategoryColor(rec.category)),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                rec.title,
                style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 16),
              if (rec.description != null) ...[
                Text(rec.description!, style: const TextStyle(fontSize: 16)),
                const SizedBox(height: 16),
              ],
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: () {
                    Navigator.pop(context);
                    _completeRecommendation(rec);
                  },
                  icon: const Icon(Icons.check),
                  label: const Text('Mark as Completed'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.green,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  IconData _getCategoryIcon(String category) {
    switch (category.toLowerCase()) {
      case 'medication':
        return Icons.medication;
      case 'exercise':
        return Icons.fitness_center;
      case 'mental health':
        return Icons.psychology;
      case 'social':
        return Icons.people;
      case 'nutrition':
        return Icons.restaurant;
      case 'lifestyle':
        return Icons.self_improvement;
      default:
        return Icons.lightbulb;
    }
  }

  Color _getCategoryColor(String category) {
    switch (category.toLowerCase()) {
      case 'medication':
        return Colors.blue;
      case 'exercise':
        return Colors.orange;
      case 'mental health':
        return Colors.purple;
      case 'social':
        return Colors.pink;
      case 'nutrition':
        return Colors.green;
      case 'lifestyle':
        return Colors.teal;
      default:
        return Colors.grey;
    }
  }

  Color _getDifficultyColor(String? difficulty) {
    switch (difficulty?.toLowerCase()) {
      case 'easy':
        return Colors.green;
      case 'hard':
        return Colors.red;
      default:
        return Colors.orange;
    }
  }
}
