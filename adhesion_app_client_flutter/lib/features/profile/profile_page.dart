import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/profile_models.dart';
import 'edit_profile_page.dart';

class ProfilePage extends StatefulWidget {
  final String baseUrl;
  const ProfilePage({super.key, required this.baseUrl});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  late final ApiClient _api;
  PsychologicalProfile? _profile;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _loadProfile();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _loadProfile() async {
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

      final profile = await _api.getLatestProfile(userId);
      setState(() => _profile = profile);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _navigateToEditProfile() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => EditProfilePage(baseUrl: widget.baseUrl),
      ),
    );
    // Reload profile if changes were made
    if (result == true) {
      _loadProfile();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Psychological Profile'),
        backgroundColor: Colors.deepPurple,
        foregroundColor: Colors.white,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.error_outline, size: 64, color: Colors.grey[400]),
                  const SizedBox(height: 16),
                  Text(_error!, style: TextStyle(color: Colors.grey[600])),
                  const SizedBox(height: 16),
                  ElevatedButton(
                    onPressed: _loadProfile,
                    child: const Text('Retry'),
                  ),
                ],
              ),
            )
          : _profile == null
          ? _buildNoProfile()
          : _buildProfile(),
    );
  }

  Widget _buildNoProfile() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.psychology_outlined, size: 80, color: Colors.grey[400]),
            const SizedBox(height: 24),
            const Text(
              'No Psychological Profile Yet',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Text(
              'Complete psychological tests to generate your personalized profile.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: () => Navigator.pop(context),
              icon: const Icon(Icons.science),
              label: const Text('Take a Test'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.deepPurple,
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

  Widget _buildProfile() {
    final profile = _profile!;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Profile Type Card
          Card(
            elevation: 4,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [Colors.deepPurple, Colors.deepPurple.shade300],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                children: [
                  const Icon(Icons.psychology, size: 48, color: Colors.white),
                  const SizedBox(height: 12),
                  Text(
                    profile.profileType,
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Adherence Risk: ${((profile.adherenceRiskScore ?? 0) * 100).toStringAsFixed(0)}%',
                    style: TextStyle(color: Colors.white.withOpacity(0.9)),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 20),

          // Summary
          if (profile.summary != null) ...[
            const Text(
              'Summary',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(profile.summary!),
              ),
            ),
            const SizedBox(height: 20),
          ],

          // Dimension Scores
          const Text(
            'Psychological Dimensions',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 12),
          _buildScoreCard(
            'Anxiety',
            profile.anxietyScore,
            Colors.red,
            reversed: true,
          ),
          _buildScoreCard(
            'Depression',
            profile.depressionScore,
            Colors.indigo,
            reversed: true,
          ),
          _buildScoreCard('Motivation', profile.motivationScore, Colors.green),
          _buildScoreCard(
            'Self-Efficacy',
            profile.selfEfficacyScore,
            Colors.blue,
          ),
          _buildScoreCard(
            'Social Support',
            profile.socialSupportScore,
            Colors.orange,
          ),
          _buildScoreCard(
            'Health Locus',
            profile.healthLocusScore,
            Colors.teal,
          ),

          const SizedBox(height: 20),

          // Detailed Interpretation
          if (profile.detailedInterpretation != null) ...[
            const Text(
              'Detailed Interpretation',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(profile.detailedInterpretation!),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildScoreCard(
    String label,
    double? score,
    Color color, {
    bool reversed = false,
  }) {
    final displayScore = score ?? 50.0;
    final isGood = reversed ? displayScore < 50 : displayScore > 50;

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    label,
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                  Row(
                    children: [
                      Icon(
                        isGood ? Icons.thumb_up : Icons.thumb_down,
                        size: 16,
                        color: isGood ? Colors.green : Colors.orange,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '${displayScore.toStringAsFixed(0)}%',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: color,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 8),
              LinearProgressIndicator(
                value: displayScore / 100,
                backgroundColor: color.withOpacity(0.2),
                valueColor: AlwaysStoppedAnimation<Color>(color),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
