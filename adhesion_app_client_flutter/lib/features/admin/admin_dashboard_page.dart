import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:file_picker/file_picker.dart';
import '../../core/api_client.dart';
import '../../core/models/login_models.dart';
import '../../core/models/test_models.dart';
import '../../core/theme/app_theme.dart';
import '../auth/login_page.dart';
import 'test_management_page.dart';
import 'user_data_page.dart';

class AdminDashboardPage extends StatefulWidget {
  final String baseUrl;
  final String adminName;
  final String? adminEmail;
  
  const AdminDashboardPage({
    super.key,
    required this.baseUrl,
    required this.adminName,
    this.adminEmail,
  });

  @override
  State<AdminDashboardPage> createState() => _AdminDashboardPageState();
}

class _AdminDashboardPageState extends State<AdminDashboardPage> with SingleTickerProviderStateMixin {
  late final ApiClient _api;
  late final AnimationController _animController;
  
  List<User> _consentedUsers = [];
  List<User> _allUsers = [];
  List<TestDto> _tests = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    )..forward();
    _loadData();
  }

  @override
  void dispose() {
    _animController.dispose();
    _api.close();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final results = await Future.wait([
        _api.getConsentedUsers().catchError((_) => <User>[]),
        _api.getTests().catchError((_) => <TestDto>[]),
        _api.getAllUsers().catchError((_) => <User>[]),
      ]);
      
      setState(() {
        _consentedUsers = results[0] as List<User>;
        _tests = results[1] as List<TestDto>;
        _allUsers = results[2] as List<User>;
      });
    } catch (e) {
      setState(() => _error = 'Failed to load data: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => LoginPage(baseUrl: widget.baseUrl)),
      (route) => false,
    );
  }

  void _showCreateAdminDialog() {
    final nameCtrl = TextEditingController();
    final emailCtrl = TextEditingController();
    final passwordCtrl = TextEditingController();
    bool loading = false;

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF1A1A2E),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          title: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFFDC2626).withOpacity(0.2),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(Icons.admin_panel_settings, color: Color(0xFFFCA5A5)),
              ),
              const SizedBox(width: 12),
              const Text('Create Admin', style: TextStyle(color: Colors.white)),
            ],
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _buildDialogField(nameCtrl, 'Full Name', Icons.person),
                const SizedBox(height: 12),
                _buildDialogField(emailCtrl, 'Email', Icons.email),
                const SizedBox(height: 12),
                _buildDialogField(passwordCtrl, 'Password', Icons.lock, isPassword: true),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: loading ? null : () async {
                if (nameCtrl.text.isEmpty || emailCtrl.text.isEmpty || passwordCtrl.text.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Please fill all fields'), backgroundColor: AppColors.error),
                  );
                  return;
                }
                
                setDialogState(() => loading = true);
                try {
                  await _api.createAdmin(
                    email: emailCtrl.text.trim(),
                    password: passwordCtrl.text,
                    displayName: nameCtrl.text.trim(),
                  );
                  HapticFeedback.heavyImpact();
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Admin created successfully!'),
                      backgroundColor: AppColors.success,
                    ),
                  );
                  _loadData();
                } catch (e) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('Failed: $e'), backgroundColor: AppColors.error),
                  );
                } finally {
                  setDialogState(() => loading = false);
                }
              },
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFDC2626)),
              child: loading
                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Create'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDialogField(TextEditingController ctrl, String hint, IconData icon, {bool isPassword = false}) {
    return TextField(
      controller: ctrl,
      obscureText: isPassword,
      style: const TextStyle(color: Colors.white),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(color: Colors.white.withOpacity(0.4)),
        prefixIcon: Icon(icon, color: Colors.white54),
        filled: true,
        fillColor: Colors.white.withOpacity(0.08),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
      ),
    );
  }

  Future<void> _importTestsFromJson() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
      );

      if (result != null && result.files.single.bytes != null) {
        final jsonString = utf8.decode(result.files.single.bytes!);
        final jsonData = jsonDecode(jsonString);
        
        List<dynamic> tests;
        if (jsonData is List) {
          tests = jsonData;
        } else if (jsonData is Map && jsonData['tests'] != null) {
          tests = jsonData['tests'] as List;
        } else {
          throw 'Invalid JSON format';
        }

        int imported = 0;
        for (final testData in tests) {
          try {
            await _api.createTest(testData as Map<String, dynamic>);
            imported++;
          } catch (e) {
            print('Failed to import test: $e');
          }
        }

        HapticFeedback.heavyImpact();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Successfully imported $imported tests!'),
            backgroundColor: AppColors.success,
          ),
        );
        _loadData();
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Import failed: $e'),
          backgroundColor: AppColors.error,
        ),
      );
    }
  }

  void _showJsonFormatInfo() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: const Color(0xFF1A1A2E),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text('JSON Test Format', style: TextStyle(color: Colors.white)),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Use this format for importing tests:',
                style: TextStyle(color: Colors.white.withOpacity(0.7)),
              ),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.3),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const SelectableText(
                  '''[
  {
    "title": "GAD-7 Anxiety Scale",
    "code": "GAD7",
    "description": "Generalized Anxiety Disorder 7-item scale",
    "active": true,
    "questions": [
      {
        "text": "Feeling nervous, anxious, or on edge",
        "code": "GAD7_Q1",
        "orderIndex": 0,
        "minScore": 0,
        "maxScore": 3,
        "reverseScored": false
      }
    ]
  }
]''',
                  style: TextStyle(
                    color: Color(0xFF10B981),
                    fontFamily: 'monospace',
                    fontSize: 11,
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Text(
                'Standard tests you can import:\n• GAD-7 (Anxiety)\n• PHQ-9 (Depression)\n• MMAS-8 (Medication Adherence)\n• PSS-10 (Perceived Stress)',
                style: TextStyle(color: Colors.white.withOpacity(0.6), fontSize: 13),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Got it'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0D0D0D),
      body: SafeArea(
        child: _loading
            ? const Center(
                child: CircularProgressIndicator(color: Color(0xFFDC2626)),
              )
            : RefreshIndicator(
                onRefresh: _loadData,
                color: const Color(0xFFDC2626),
                child: CustomScrollView(
                  physics: const BouncingScrollPhysics(),
                  slivers: [
                    SliverToBoxAdapter(child: _buildHeader()),
                    SliverToBoxAdapter(child: _buildAdminProfile()),
                    SliverToBoxAdapter(child: _buildStats()),
                    SliverToBoxAdapter(child: _buildQuickActions()),
                    SliverToBoxAdapter(child: _buildRecentUsers()),
                    const SliverToBoxAdapter(child: SizedBox(height: 100)),
                  ],
                ),
              ),
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFFDC2626), Color(0xFF991B1B)],
                  ),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: const Icon(
                  Icons.admin_panel_settings_rounded,
                  color: Colors.white,
                  size: 28,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Welcome back,',
                      style: TextStyle(
                        color: Colors.white.withOpacity(0.6),
                        fontSize: 14,
                      ),
                    ),
                    Text(
                      widget.adminName,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 22,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: _logout,
                icon: Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Icon(Icons.logout, color: Colors.white, size: 20),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              color: const Color(0xFFDC2626).withOpacity(0.15),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.verified, color: Color(0xFFFCA5A5), size: 16),
                const SizedBox(width: 8),
                Text(
                  'Administrator Dashboard',
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.8),
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAdminProfile() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            const Color(0xFFDC2626).withOpacity(0.2),
            const Color(0xFF991B1B).withOpacity(0.1),
          ],
        ),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFDC2626).withOpacity(0.3)),
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 28,
            backgroundColor: const Color(0xFFDC2626).withOpacity(0.3),
            child: Text(
              widget.adminName.isNotEmpty ? widget.adminName[0].toUpperCase() : 'A',
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 22,
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.adminName,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                if (widget.adminEmail != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    widget.adminEmail!,
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.6),
                      fontSize: 13,
                    ),
                  ),
                ],
                const SizedBox(height: 6),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: const Color(0xFFDC2626).withOpacity(0.3),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: const Text(
                    '👑 SUPER ADMIN',
                    style: TextStyle(
                      color: Color(0xFFFCA5A5),
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: _showCreateAdminDialog,
            icon: Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: const Color(0xFF10B981).withOpacity(0.2),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(Icons.person_add, color: Color(0xFF10B981), size: 20),
            ),
            tooltip: 'Create New Admin',
          ),
        ],
      ),
    );
  }

  Widget _buildStats() {
    final adminCount = _allUsers.where((u) => u.role == 'ADMIN').length;
    
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: _buildStatCard(
                  icon: Icons.people_alt_rounded,
                  label: 'Consented Users',
                  value: '${_consentedUsers.length}',
                  color: const Color(0xFF10B981),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildStatCard(
                  icon: Icons.quiz_rounded,
                  label: 'Active Tests',
                  value: '${_tests.length}',
                  color: const Color(0xFF6366F1),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _buildStatCard(
                  icon: Icons.group_rounded,
                  label: 'Total Users',
                  value: '${_allUsers.length}',
                  color: const Color(0xFFF59E0B),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildStatCard(
                  icon: Icons.admin_panel_settings_rounded,
                  label: 'Admins',
                  value: '$adminCount',
                  color: const Color(0xFFDC2626),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard({
    required IconData icon,
    required String label,
    required String value,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white.withOpacity(0.08)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: color.withOpacity(0.15),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: color, size: 24),
          ),
          const SizedBox(height: 16),
          Text(
            value,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 32,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              color: Colors.white.withOpacity(0.6),
              fontSize: 13,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuickActions() {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Quick Actions',
            style: TextStyle(
              color: Colors.white,
              fontSize: 18,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _buildActionCard(
                  icon: Icons.add_circle_outline,
                  label: 'Manage Tests',
                  description: 'Create and edit tests',
                  color: const Color(0xFF6366F1),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => TestManagementPage(baseUrl: widget.baseUrl),
                    ),
                  ).then((_) => _loadData()),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildActionCard(
                  icon: Icons.people_outline,
                  label: 'View Users',
                  description: 'Consented data',
                  color: const Color(0xFF10B981),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => UserDataPage(baseUrl: widget.baseUrl),
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _buildActionCard(
                  icon: Icons.upload_file_rounded,
                  label: 'Import Tests',
                  description: 'From JSON file',
                  color: const Color(0xFFF59E0B),
                  onTap: _importTestsFromJson,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildActionCard(
                  icon: Icons.info_outline_rounded,
                  label: 'JSON Format',
                  description: 'View template',
                  color: const Color(0xFF8B5CF6),
                  onTap: _showJsonFormatInfo,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          _buildActionCard(
            icon: Icons.person_add_rounded,
            label: 'Create New Admin',
            description: 'Add administrator account',
            color: const Color(0xFFDC2626),
            onTap: _showCreateAdminDialog,
            fullWidth: true,
          ),
        ],
      ),
    );
  }

  Widget _buildActionCard({
    required IconData icon,
    required String label,
    required String description,
    required Color color,
    required VoidCallback onTap,
    bool fullWidth = false,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(18),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [color.withOpacity(0.2), color.withOpacity(0.1)],
          ),
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: color.withOpacity(0.3)),
        ),
        child: Row(
          children: [
            Icon(icon, color: color, size: 28),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    description,
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.5),
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
            if (fullWidth)
              Icon(Icons.arrow_forward_ios, color: color.withOpacity(0.5), size: 16),
          ],
        ),
      ),
    );
  }

  Widget _buildRecentUsers() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Recent Consented Users',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                ),
              ),
              if (_consentedUsers.isNotEmpty)
                TextButton(
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => UserDataPage(baseUrl: widget.baseUrl),
                    ),
                  ),
                  child: const Text('See All'),
                ),
            ],
          ),
          const SizedBox(height: 16),
          if (_consentedUsers.isEmpty)
            Container(
              padding: const EdgeInsets.all(32),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.05),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: Colors.white.withOpacity(0.08)),
              ),
              child: Column(
                children: [
                  Icon(
                    Icons.people_outline,
                    size: 48,
                    color: Colors.white.withOpacity(0.3),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'No users have given consent yet',
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.5),
                      fontSize: 14,
                    ),
                  ),
                ],
              ),
            )
          else
            ..._consentedUsers.take(5).map((user) => _buildUserCard(user)),
        ],
      ),
    );
  }

  Widget _buildUserCard(User user) {
    return GestureDetector(
      onTap: () => _viewUserData(user),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.05),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: Colors.white.withOpacity(0.08)),
        ),
        child: Row(
          children: [
            CircleAvatar(
              radius: 22,
              backgroundColor: const Color(0xFF6366F1).withOpacity(0.2),
              child: Text(
                (user.displayName?.isNotEmpty ?? false)
                    ? user.displayName![0].toUpperCase()
                    : 'U',
                style: const TextStyle(
                  color: Color(0xFF6366F1),
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    user.displayName ?? 'Unknown',
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    user.email ?? '',
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.5),
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: const Color(0xFF10B981).withOpacity(0.15),
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.check_circle, color: Color(0xFF10B981), size: 14),
                  SizedBox(width: 4),
                  Text(
                    'Consented',
                    style: TextStyle(
                      color: Color(0xFF10B981),
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Icon(
              Icons.arrow_forward_ios,
              color: Colors.white.withOpacity(0.5),
              size: 16,
            ),
          ],
        ),
      ),
    );
  }

  void _viewUserData(User user) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => UserDataPage(
          baseUrl: widget.baseUrl,
          specificUser: user,
        ),
      ),
    );
  }
}
