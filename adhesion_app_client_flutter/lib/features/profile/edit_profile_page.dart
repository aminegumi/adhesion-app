import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../core/api_client.dart';
import '../../core/models/login_models.dart';
import '../../core/theme/app_theme.dart';

class EditProfilePage extends StatefulWidget {
  final String baseUrl;
  const EditProfilePage({super.key, required this.baseUrl});

  @override
  State<EditProfilePage> createState() => _EditProfilePageState();
}

class _EditProfilePageState extends State<EditProfilePage> with SingleTickerProviderStateMixin {
  late final ApiClient _api;
  late final AnimationController _animController;
  
  final _formKey = GlobalKey<FormState>();
  final _nameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _birthCtrl = TextEditingController();
  
  User? _user;
  String _gender = 'Male';
  bool _consentGiven = false;
  bool _loading = true;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    )..forward();
    _loadProfile();
  }

  @override
  void dispose() {
    _animController.dispose();
    _api.close();
    _nameCtrl.dispose();
    _emailCtrl.dispose();
    _birthCtrl.dispose();
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

      final user = await _api.getUserProfile(userId);
      setState(() {
        _user = user;
        _nameCtrl.text = user.displayName ?? '';
        _emailCtrl.text = user.email ?? '';
        _birthCtrl.text = user.birthDate ?? '';
        _gender = user.gender ?? 'Male';
        _consentGiven = user.consentGiven ?? false;
      });
    } catch (e) {
      setState(() => _error = 'Failed to load profile: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _pickBirthDate() async {
    final now = DateTime.now();
    DateTime? initial;
    if (_birthCtrl.text.isNotEmpty) {
      try {
        initial = DateTime.parse(_birthCtrl.text);
      } catch (_) {}
    }
    initial ??= DateTime(now.year - 25, now.month, now.day);
    
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(1900, 1, 1),
      lastDate: now,
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: ColorScheme.light(
              primary: AppColors.primary,
              onPrimary: Colors.white,
              surface: Colors.white,
              onSurface: AppColors.textPrimary,
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      final y = picked.year.toString().padLeft(4, '0');
      final m = picked.month.toString().padLeft(2, '0');
      final d = picked.day.toString().padLeft(2, '0');
      _birthCtrl.text = '$y-$m-$d';
      setState(() {});
    }
  }

  Future<void> _saveProfile() async {
    if (!_formKey.currentState!.validate()) return;
    
    setState(() => _saving = true);
    HapticFeedback.mediumImpact();

    try {
      final userId = await ApiClient.getStoredUserId();
      if (userId == null) throw Exception('Not logged in');

      final updatedUser = await _api.updateUserProfile(
        userId,
        displayName: _nameCtrl.text.trim(),
        email: _emailCtrl.text.trim(),
        gender: _gender,
        birthDate: _birthCtrl.text,
        consentGiven: _consentGiven,
      );

      // Update stored info
      await ApiClient.saveUserInfo(
        userId,
        updatedUser.displayName ?? '',
        updatedUser.email ?? '',
        consent: _consentGiven,
      );

      if (!mounted) return;
      
      HapticFeedback.heavyImpact();
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Row(
            children: [
              Icon(Icons.check_circle, color: Colors.white),
              SizedBox(width: 12),
              Text('Profile updated successfully!'),
            ],
          ),
          backgroundColor: AppColors.success,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.all(16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      );

      Navigator.pop(context, true);
    } catch (e) {
      if (!mounted) return;
      HapticFeedback.vibrate();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.white),
              const SizedBox(width: 12),
              Expanded(child: Text('Failed to save: $e')),
            ],
          ),
          backgroundColor: AppColors.error,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.all(16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      );
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
          : _error != null
              ? _buildError()
              : _buildContent(),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: AppColors.error.withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.error_outline, size: 48, color: AppColors.error),
            ),
            const SizedBox(height: 20),
            Text(
              _error!,
              textAlign: TextAlign.center,
              style: AppTextStyles.bodyLarge,
            ),
            const SizedBox(height: 20),
            ElevatedButton.icon(
              onPressed: _loadProfile,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    return CustomScrollView(
      physics: const BouncingScrollPhysics(),
      slivers: [
        // Header
        SliverToBoxAdapter(child: _buildHeader()),
        
        // Form
        SliverToBoxAdapter(
          child: SlideTransition(
            position: Tween<Offset>(
              begin: const Offset(0, 0.3),
              end: Offset.zero,
            ).animate(CurvedAnimation(
              parent: _animController,
              curve: Curves.easeOutCubic,
            )),
            child: FadeTransition(
              opacity: _animController,
              child: _buildForm(),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildHeader() {
    return Container(
      padding: EdgeInsets.only(
        top: MediaQuery.of(context).padding.top + 16,
        left: 20,
        right: 20,
        bottom: 30,
      ),
      decoration: const BoxDecoration(
        gradient: AppColors.primaryGradient,
        borderRadius: BorderRadius.only(
          bottomLeft: Radius.circular(32),
          bottomRight: Radius.circular(32),
        ),
      ),
      child: Column(
        children: [
          // App bar
          Row(
            children: [
              GestureDetector(
                onTap: () => Navigator.pop(context),
                child: Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(Icons.arrow_back, color: Colors.white),
                ),
              ),
              const Spacer(),
              const Text(
                'Edit Profile',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const Spacer(),
              const SizedBox(width: 44), // Balance
            ],
          ),
          const SizedBox(height: 24),
          
          // Avatar
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(color: Colors.white, width: 3),
            ),
            child: CircleAvatar(
              radius: 45,
              backgroundColor: Colors.white.withOpacity(0.2),
              child: Text(
                (_user?.displayName?.isNotEmpty ?? false)
                    ? _user!.displayName![0].toUpperCase()
                    : 'U',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 36,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            _user?.displayName ?? 'User',
            style: const TextStyle(
              color: Colors.white,
              fontSize: 22,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            _user?.email ?? '',
            style: TextStyle(
              color: Colors.white.withOpacity(0.7),
              fontSize: 14,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildForm() {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Personal Information',
              style: AppTextStyles.h2,
            ),
            const SizedBox(height: 20),
            
            // Name field
            _buildFormField(
              controller: _nameCtrl,
              label: 'Full Name',
              icon: Icons.person_outline,
              validator: (v) => (v == null || v.trim().isEmpty) ? 'Name required' : null,
            ),
            const SizedBox(height: 16),
            
            // Email field
            _buildFormField(
              controller: _emailCtrl,
              label: 'Email Address',
              icon: Icons.email_outlined,
              keyboardType: TextInputType.emailAddress,
              validator: (v) {
                if (v == null || v.isEmpty) return 'Email required';
                if (!RegExp(r'^[^@]+@[^@]+\.[^@]+').hasMatch(v)) return 'Invalid email';
                return null;
              },
            ),
            const SizedBox(height: 16),
            
            // Birth date field
            _buildFormField(
              controller: _birthCtrl,
              label: 'Birth Date',
              icon: Icons.cake_outlined,
              readOnly: true,
              onTap: _pickBirthDate,
              suffixIcon: Icons.calendar_today,
            ),
            const SizedBox(height: 20),
            
            // Gender
            const Text(
              'Gender',
              style: AppTextStyles.labelLarge,
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: _buildGenderOption('Male', Icons.male)),
                const SizedBox(width: 12),
                Expanded(child: _buildGenderOption('Female', Icons.female)),
              ],
            ),
            
            const SizedBox(height: 32),
            
            // Privacy Section
            const Text(
              'Privacy & Data Sharing',
              style: AppTextStyles.h2,
            ),
            const SizedBox(height: 16),
            
            _buildConsentCard(),
            
            const SizedBox(height: 32),
            
            // Save button
            _buildSaveButton(),
            
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildFormField({
    required TextEditingController controller,
    required String label,
    required IconData icon,
    TextInputType? keyboardType,
    bool readOnly = false,
    VoidCallback? onTap,
    IconData? suffixIcon,
    String? Function(String?)? validator,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: AppShadows.sm,
      ),
      child: TextFormField(
        controller: controller,
        keyboardType: keyboardType,
        readOnly: readOnly,
        onTap: onTap,
        validator: validator,
        style: AppTextStyles.bodyLarge.copyWith(color: AppColors.textPrimary),
        decoration: InputDecoration(
          labelText: label,
          labelStyle: AppTextStyles.bodyMedium,
          prefixIcon: Icon(icon, color: AppColors.primary, size: 22),
          suffixIcon: suffixIcon != null
              ? Icon(suffixIcon, color: AppColors.textTertiary, size: 20)
              : null,
          filled: true,
          fillColor: Colors.white,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(16),
            borderSide: BorderSide.none,
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(16),
            borderSide: const BorderSide(color: AppColors.border),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(16),
            borderSide: const BorderSide(color: AppColors.primary, width: 2),
          ),
          errorBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(16),
            borderSide: const BorderSide(color: AppColors.error),
          ),
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
        ),
      ),
    );
  }

  Widget _buildGenderOption(String label, IconData icon) {
    final selected = _gender == label;
    return GestureDetector(
      onTap: () => setState(() => _gender = label),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          color: selected ? AppColors.primary : Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: selected ? AppColors.primary : AppColors.border,
            width: selected ? 2 : 1,
          ),
          boxShadow: selected ? AppShadows.colored(AppColors.primary, opacity: 0.2) : AppShadows.sm,
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              icon,
              color: selected ? Colors.white : AppColors.textSecondary,
              size: 22,
            ),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                color: selected ? Colors.white : AppColors.textPrimary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConsentCard() {
    return GestureDetector(
      onTap: () => setState(() => _consentGiven = !_consentGiven),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: _consentGiven
              ? AppColors.success.withOpacity(0.08)
              : Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: _consentGiven
                ? AppColors.success.withOpacity(0.5)
                : AppColors.border,
            width: _consentGiven ? 2 : 1,
          ),
          boxShadow: AppShadows.sm,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: _consentGiven
                        ? AppColors.success.withOpacity(0.15)
                        : AppColors.primary.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    _consentGiven ? Icons.verified_user : Icons.shield_outlined,
                    color: _consentGiven ? AppColors.success : AppColors.primary,
                    size: 24,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Data Sharing Consent',
                        style: AppTextStyles.h3.copyWith(
                          color: _consentGiven ? AppColors.success : AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        _consentGiven ? 'Enabled' : 'Disabled',
                        style: AppTextStyles.bodySmall.copyWith(
                          color: _consentGiven ? AppColors.success : AppColors.textTertiary,
                        ),
                      ),
                    ],
                  ),
                ),
                Transform.scale(
                  scale: 1.2,
                  child: Switch(
                    value: _consentGiven,
                    onChanged: (v) => setState(() => _consentGiven = v),
                    activeColor: AppColors.success,
                    activeTrackColor: AppColors.success.withOpacity(0.3),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.surfaceVariant,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    _consentGiven
                        ? 'Healthcare administrators can view:'
                        : 'When enabled, administrators can view:',
                    style: AppTextStyles.labelMedium,
                  ),
                  const SizedBox(height: 10),
                  _buildConsentItem('Your psychological test results'),
                  _buildConsentItem('Your treatment plans and progress'),
                  _buildConsentItem('Your adherence statistics'),
                ],
              ),
            ),
            if (_consentGiven) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(
                  color: AppColors.warning.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.info_outline, color: AppColors.warning, size: 18),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'You can disable this at any time',
                        style: AppTextStyles.bodySmall.copyWith(color: AppColors.warning),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildConsentItem(String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        children: [
          Icon(
            Icons.check_circle_outline,
            size: 16,
            color: _consentGiven ? AppColors.success : AppColors.textTertiary,
          ),
          const SizedBox(width: 8),
          Text(
            text,
            style: AppTextStyles.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSaveButton() {
    return SizedBox(
      width: double.infinity,
      height: 56,
      child: ElevatedButton(
        onPressed: _saving ? null : _saveProfile,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
        child: AnimatedSwitcher(
          duration: const Duration(milliseconds: 200),
          child: _saving
              ? const SizedBox(
                  width: 24,
                  height: 24,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.5,
                    valueColor: AlwaysStoppedAnimation(Colors.white),
                  ),
                )
              : Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.save_rounded, size: 22),
                    const SizedBox(width: 10),
                    const Text(
                      'Save Changes',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}

