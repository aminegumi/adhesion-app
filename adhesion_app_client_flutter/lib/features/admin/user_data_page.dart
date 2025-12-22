import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/login_models.dart';
import '../../core/models/test_models.dart';
import '../../core/models/treatment_models.dart';
import '../../core/theme/app_theme.dart';

class UserDataPage extends StatefulWidget {
  final String baseUrl;
  final User? specificUser;
  final bool showAllUsers; // New: show all users including non-consented
  
  const UserDataPage({
    super.key,
    required this.baseUrl,
    this.specificUser,
    this.showAllUsers = false,
  });

  @override
  State<UserDataPage> createState() => _UserDataPageState();
}

class _UserDataPageState extends State<UserDataPage> with SingleTickerProviderStateMixin {
  late final ApiClient _api;
  late final TabController _tabController;
  
  List<User> _users = [];
  User? _selectedUser;
  List<TestResultDto> _testResults = [];
  List<TreatmentPlan> _treatmentPlans = [];
  bool _loading = true;
  bool _loadingDetails = false;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _tabController = TabController(length: 2, vsync: this);
    
    if (widget.specificUser != null) {
      _selectedUser = widget.specificUser;
      _loading = false; // No need to load user list when specific user is passed
      _loadUserDetails(widget.specificUser!);
    } else {
      _loadUsers();
    }
  }

  @override
  void dispose() {
    _tabController.dispose();
    _api.close();
    super.dispose();
  }

  Future<void> _loadUsers() async {
    setState(() => _loading = true);
    try {
      // Load all users or only consented based on showAllUsers flag
      if (widget.showAllUsers) {
        _users = await _api.getAllUsers();
        // Filter out admin users
        _users = _users.where((u) => u.role != 'ADMIN').toList();
      } else {
        _users = await _api.getConsentedUsers();
      }
    } catch (e) {
      // Handle error
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loadUserDetails(User user) async {
    setState(() {
      _selectedUser = user;
      _loadingDetails = true;
    });
    
    try {
      final results = await Future.wait<dynamic>([
        _api.getUserTestResults(user.id!).catchError((_) => <TestResultDto>[]),
        _api.getUserTreatmentPlansAdmin(user.id!).catchError((_) => <TreatmentPlan>[]),
      ]);
      
      setState(() {
        _testResults = results[0] as List<TestResultDto>;
        _treatmentPlans = results[1] as List<TreatmentPlan>;
      });
    } catch (e) {
      // Handle error silently for now
    } finally {
      if (mounted) setState(() => _loadingDetails = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0D0D0D),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        title: Text(_selectedUser != null 
            ? 'User Details' 
            : widget.showAllUsers ? 'All Users' : 'Consented Users'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            if (_selectedUser != null && widget.specificUser == null) {
              setState(() {
                _selectedUser = null;
                _testResults = [];
                _treatmentPlans = [];
              });
            } else {
              Navigator.pop(context);
            }
          },
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFDC2626)))
          : _selectedUser != null
              ? _buildUserDetails()
              : _buildUserList(),
    );
  }

  Widget _buildUserList() {
    if (_users.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.05),
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.people_outline,
                size: 64,
                color: Colors.white.withOpacity(0.3),
              ),
            ),
            const SizedBox(height: 24),
            Text(
              widget.showAllUsers ? 'No Users Found' : 'No Consented Users',
              style: TextStyle(
                color: Colors.white.withOpacity(0.8),
                fontSize: 20,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              widget.showAllUsers 
                  ? 'No users have registered yet'
                  : 'Users who give consent will appear here',
              style: TextStyle(
                color: Colors.white.withOpacity(0.5),
                fontSize: 14,
              ),
            ),
          ],
        ),
      );
    }

    // Separate consented and non-consented users
    final consentedUsers = _users.where((u) => u.consentGiven ?? false).toList();
    final nonConsentedUsers = _users.where((u) => !(u.consentGiven ?? false)).toList();

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // Show consented users first
        if (consentedUsers.isNotEmpty) ...[
          _buildSectionHeader('Consented Users', consentedUsers.length, AppColors.success),
          const SizedBox(height: 12),
          ...consentedUsers.map((user) => _buildUserCard(user)),
        ],
        // Then show non-consented users
        if (nonConsentedUsers.isNotEmpty && widget.showAllUsers) ...[
          const SizedBox(height: 20),
          _buildSectionHeader('No Consent', nonConsentedUsers.length, Colors.red),
          const SizedBox(height: 12),
          ...nonConsentedUsers.map((user) => _buildUserCard(user)),
        ],
      ],
    );
  }

  Widget _buildSectionHeader(String title, int count, Color color) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: color.withOpacity(0.15),
            borderRadius: BorderRadius.circular(20),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                title.contains('Consent') && !title.contains('No') 
                    ? Icons.check_circle 
                    : Icons.block,
                color: color,
                size: 16,
              ),
              const SizedBox(width: 6),
              Text(
                '$title ($count)',
                style: TextStyle(
                  color: color,
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildUserCard(User user) {
    final hasConsent = user.consentGiven ?? false;
    
    return GestureDetector(
      onTap: hasConsent 
          ? () => _loadUserDetails(user)
          : () => _showNoConsentDialog(user),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: hasConsent 
              ? Colors.white.withOpacity(0.05)
              : Colors.white.withOpacity(0.02),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: hasConsent 
                ? Colors.white.withOpacity(0.08)
                : Colors.red.withOpacity(0.2),
          ),
        ),
        child: Row(
          children: [
            CircleAvatar(
              radius: 26,
              backgroundColor: hasConsent 
                  ? const Color(0xFF6366F1).withOpacity(0.2)
                  : Colors.grey.withOpacity(0.2),
              child: hasConsent
                  ? Text(
                      (user.displayName?.isNotEmpty ?? false)
                          ? user.displayName![0].toUpperCase()
                          : 'U',
                      style: const TextStyle(
                        color: Color(0xFF6366F1),
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                      ),
                    )
                  : Icon(
                      Icons.lock_outline,
                      color: Colors.grey.withOpacity(0.5),
                      size: 24,
                    ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    hasConsent ? (user.displayName ?? 'Unknown') : 'User #${user.id}',
                    style: TextStyle(
                      color: hasConsent ? Colors.white : Colors.white.withOpacity(0.5),
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    hasConsent ? (user.email ?? '') : '••••••@••••••',
                    style: TextStyle(
                      color: Colors.white.withOpacity(hasConsent ? 0.5 : 0.3),
                      fontSize: 13,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      if (hasConsent) ...[
                        _buildTag(user.gender ?? 'N/A', const Color(0xFF6366F1)),
                        const SizedBox(width: 8),
                        _buildTag('Consented', AppColors.success),
                      ] else
                        _buildTag('No Consent', Colors.red),
                    ],
                  ),
                ],
              ),
            ),
            if (hasConsent)
              Icon(
                Icons.arrow_forward_ios,
                color: Colors.white.withOpacity(0.3),
                size: 16,
              )
            else
              Icon(
                Icons.block,
                color: Colors.red.withOpacity(0.5),
                size: 20,
              ),
          ],
        ),
      ),
    );
  }

  void _showNoConsentDialog(User user) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1A1A2E),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.red.withOpacity(0.2),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(Icons.lock, color: Colors.red, size: 24),
            ),
            const SizedBox(width: 12),
            const Text(
              'Access Restricted',
              style: TextStyle(color: Colors.white, fontSize: 18),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'This user has not given consent to share their data.',
              style: TextStyle(color: Colors.white.withOpacity(0.8)),
            ),
            const SizedBox(height: 12),
            Text(
              'You cannot view their psychological profile, test results, or treatment plans until they consent to data sharing.',
              style: TextStyle(color: Colors.white.withOpacity(0.5), fontSize: 13),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Understood'),
          ),
        ],
      ),
    );
  }

  Widget _buildTag(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        text,
        style: TextStyle(
          color: color,
          fontSize: 11,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  Widget _buildUserDetails() {
    final user = _selectedUser!;
    
    return Column(
      children: [
        // User header
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.05),
            borderRadius: const BorderRadius.vertical(bottom: Radius.circular(24)),
          ),
          child: Column(
            children: [
              Row(
                children: [
                  CircleAvatar(
                    radius: 32,
                    backgroundColor: const Color(0xFF6366F1).withOpacity(0.2),
                    child: Text(
                      (user.displayName?.isNotEmpty ?? false)
                          ? user.displayName![0].toUpperCase()
                          : 'U',
                      style: const TextStyle(
                        color: Color(0xFF6366F1),
                        fontWeight: FontWeight.bold,
                        fontSize: 24,
                      ),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user.displayName ?? 'Unknown',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          user.email ?? '',
                          style: TextStyle(
                            color: Colors.white.withOpacity(0.6),
                            fontSize: 14,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  _buildInfoChip(Icons.cake_outlined, user.birthDate ?? 'N/A'),
                  const SizedBox(width: 12),
                  _buildInfoChip(
                    user.gender == 'Male' ? Icons.male : Icons.female,
                    user.gender ?? 'N/A',
                  ),
                  const Spacer(),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppColors.success.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.verified, color: AppColors.success, size: 16),
                        SizedBox(width: 6),
                        Text(
                          'Consent Given',
                          style: TextStyle(
                            color: AppColors.success,
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        
        // Tabs
        Container(
          margin: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.05),
            borderRadius: BorderRadius.circular(12),
          ),
          child: TabBar(
            controller: _tabController,
            indicator: BoxDecoration(
              color: const Color(0xFFDC2626),
              borderRadius: BorderRadius.circular(10),
            ),
            indicatorSize: TabBarIndicatorSize.tab,
            indicatorPadding: const EdgeInsets.all(4),
            labelColor: Colors.white,
            unselectedLabelColor: Colors.white54,
            tabs: const [
              Tab(text: 'Test Results'),
              Tab(text: 'Treatment Plans'),
            ],
          ),
        ),
        
        // Tab content
        Expanded(
          child: _loadingDetails
              ? const Center(
                  child: CircularProgressIndicator(color: Color(0xFFDC2626)),
                )
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildTestResults(),
                    _buildTreatmentPlans(),
                  ],
                ),
        ),
      ],
    );
  }

  Widget _buildInfoChip(IconData icon, String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.08),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: Colors.white54, size: 16),
          const SizedBox(width: 6),
          Text(
            text,
            style: TextStyle(
              color: Colors.white.withOpacity(0.7),
              fontSize: 12,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTestResults() {
    if (_testResults.isEmpty) {
      return _buildEmptyState('No Test Results', 'User has not completed any tests yet');
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _testResults.length,
      itemBuilder: (context, index) {
        final result = _testResults[index];
        return _buildTestResultCard(result);
      },
    );
  }

  Widget _buildTestResultCard(TestResultDto result) {
    final score = result.totalScore;
    
    Color scoreColor;
    final level = result.interpretationLevel.toLowerCase();
    if (level.contains('minimal') || level.contains('low') || level.contains('good')) {
      scoreColor = AppColors.success;
    } else if (level.contains('moderate') || level.contains('medium')) {
      scoreColor = const Color(0xFFF59E0B);
    } else {
      scoreColor = AppColors.error;
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.08)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0xFF6366F1).withOpacity(0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.psychology, color: Color(0xFF6366F1), size: 24),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      result.testTitle,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      result.submittedAt ?? 'Date unknown',
                      style: TextStyle(
                        color: Colors.white.withOpacity(0.5),
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                decoration: BoxDecoration(
                  color: scoreColor.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  '$score pts',
                  style: TextStyle(
                    color: scoreColor,
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          _buildTag(result.interpretationLevel, scoreColor),
          if (result.clinicalInterpretation.isNotEmpty) ...[
            const SizedBox(height: 14),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.03),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Text(
                result.clinicalInterpretation,
                style: TextStyle(
                  color: Colors.white.withOpacity(0.7),
                  fontSize: 13,
                  height: 1.4,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildTreatmentPlans() {
    if (_treatmentPlans.isEmpty) {
      return _buildEmptyState('No Treatment Plans', 'User has no treatment plans yet');
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _treatmentPlans.length,
      itemBuilder: (context, index) {
        final plan = _treatmentPlans[index];
        return _buildTreatmentPlanCard(plan);
      },
    );
  }

  Widget _buildTreatmentPlanCard(TreatmentPlan plan) {
    final isActive = plan.status?.toUpperCase() == 'ACTIVE';
    
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isActive
              ? AppColors.success.withOpacity(0.3)
              : Colors.white.withOpacity(0.08),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: (isActive ? AppColors.success : Colors.grey).withOpacity(0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  Icons.assignment,
                  color: isActive ? AppColors.success : Colors.grey,
                  size: 24,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      plan.title,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        _buildTag(plan.status ?? 'Unknown', isActive ? AppColors.success : Colors.grey),
                        const SizedBox(width: 8),
                        Text(
                          '${plan.medicationsList.length} medications',
                          style: TextStyle(
                            color: Colors.white.withOpacity(0.5),
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
          if (plan.medicationsList.isNotEmpty) ...[
            const SizedBox(height: 14),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: plan.medicationsList.map((med) {
                return Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.08),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.medication, color: Color(0xFF6366F1), size: 14),
                      const SizedBox(width: 6),
                      Text(
                        '${med.name} ${med.dosage ?? ''}',
                        style: TextStyle(
                          color: Colors.white.withOpacity(0.8),
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                );
              }).toList(),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildEmptyState(String title, String subtitle) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.inbox_outlined,
            size: 48,
            color: Colors.white.withOpacity(0.3),
          ),
          const SizedBox(height: 16),
          Text(
            title,
            style: TextStyle(
              color: Colors.white.withOpacity(0.7),
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            subtitle,
            style: TextStyle(
              color: Colors.white.withOpacity(0.4),
              fontSize: 13,
            ),
          ),
        ],
      ),
    );
  }
}

