import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../core/api_client.dart';
import '../../core/models/register_models.dart';
import '../../core/theme/app_theme.dart';

class RegisterPage extends StatefulWidget {
  final String baseUrl;
  const RegisterPage({super.key, required this.baseUrl});

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> with TickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _nameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  final _birthCtrl = TextEditingController();
  bool _obscure = true;
  bool _loading = false;
  String _gender = 'Male';
  bool _consentGiven = false;

  late final ApiClient _api;
  late final AnimationController _backgroundController;
  late final AnimationController _fadeController;
  late final Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    
    _backgroundController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 20),
    )..repeat();
    
    _fadeController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );
    _fadeAnimation = CurvedAnimation(
      parent: _fadeController,
      curve: Curves.easeOut,
    );
    
    _fadeController.forward();
  }

  @override
  void dispose() {
    _backgroundController.dispose();
    _fadeController.dispose();
    _api.close();
    _nameCtrl.dispose();
    _emailCtrl.dispose();
    _passwordCtrl.dispose();
    _birthCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickBirthDate() async {
    final now = DateTime.now();
    final initial = DateTime(now.year - 25, now.month, now.day);
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(1900, 1, 1),
      lastDate: now,
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.dark(
              primary: AppColors.primary,
              onPrimary: Colors.white,
              surface: Color(0xFF1E1E2E),
              onSurface: Colors.white,
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

  Future<void> _showConsentDialog() async {
    final result = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (context) => _ConsentDialog(),
    );
    if (result == true) {
      setState(() => _consentGiven = true);
    }
  }

  Future<void> _onRegister() async {
    if (!_formKey.currentState!.validate()) return;
    
    // Show consent dialog if not already given
    if (!_consentGiven) {
      await _showConsentDialog();
    }
    
    setState(() => _loading = true);
    HapticFeedback.mediumImpact();
    
    try {
      final req = RegisterRequest(
        email: _emailCtrl.text.trim(),
        password: _passwordCtrl.text,
        displayName: _nameCtrl.text.trim(),
        birthDate: _birthCtrl.text,
        gender: _gender,
        consentGiven: _consentGiven,
      );
      final user = await _api.register(req);
      if (!mounted) return;
      
      HapticFeedback.heavyImpact();
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.check_circle, color: Colors.white),
              const SizedBox(width: 12),
              Text('Welcome, ${user.displayName ?? 'User'}!'),
            ],
          ),
          backgroundColor: AppColors.success,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.all(16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      );
      Navigator.of(context).pop();
    } on ApiException catch (e) {
      if (!mounted) return;
      HapticFeedback.vibrate();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.white),
              const SizedBox(width: 12),
              Expanded(child: Text('Registration failed: ${e.message}')),
            ],
          ),
          backgroundColor: AppColors.error,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.all(16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      );
    } catch (_) {
      if (!mounted) return;
      HapticFeedback.vibrate();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Row(
            children: [
              Icon(Icons.error_outline, color: Colors.white),
              SizedBox(width: 12),
              Text('Unexpected error occurred'),
            ],
          ),
          backgroundColor: AppColors.error,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.all(16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      );
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    
    return Scaffold(
      body: Stack(
        children: [
          // Animated background
          _buildAnimatedBackground(size),
          
          // Floating orbs
          ..._buildFloatingOrbs(size),
          
          // Content
          SafeArea(
            child: FadeTransition(
              opacity: _fadeAnimation,
              child: _buildContent(size),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAnimatedBackground(Size size) {
    return AnimatedBuilder(
      animation: _backgroundController,
      builder: (context, child) {
        return Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                Color.lerp(
                  const Color(0xFF0F172A),
                  const Color(0xFF1E1B4B),
                  _backgroundController.value,
                )!,
                Color.lerp(
                  const Color(0xFF1E1B4B),
                  const Color(0xFF312E81),
                  _backgroundController.value,
                )!,
                Color.lerp(
                  const Color(0xFF312E81),
                  const Color(0xFF4C1D95),
                  ((_backgroundController.value - 0.5) * 2).clamp(0, 1),
                )!,
              ],
            ),
          ),
        );
      },
    );
  }

  List<Widget> _buildFloatingOrbs(Size size) {
    return [
      Positioned(
        top: -size.width * 0.2,
        right: -size.width * 0.2,
        child: AnimatedBuilder(
          animation: _backgroundController,
          builder: (context, child) {
            return Transform.translate(
              offset: Offset(
                math.sin(_backgroundController.value * 2 * math.pi) * 20,
                math.cos(_backgroundController.value * 2 * math.pi) * 20,
              ),
              child: Container(
                width: size.width * 0.6,
                height: size.width * 0.6,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      AppColors.secondary.withOpacity(0.3),
                      AppColors.secondary.withOpacity(0.0),
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
      Positioned(
        bottom: size.height * 0.1,
        left: -size.width * 0.15,
        child: AnimatedBuilder(
          animation: _backgroundController,
          builder: (context, child) {
            return Transform.translate(
              offset: Offset(
                math.cos(_backgroundController.value * 2 * math.pi) * 15,
                math.sin(_backgroundController.value * 2 * math.pi) * 20,
              ),
              child: Container(
                width: size.width * 0.5,
                height: size.width * 0.5,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      AppColors.accent1.withOpacity(0.25),
                      AppColors.accent1.withOpacity(0.0),
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
    ];
  }

  Widget _buildContent(Size size) {
    return SingleChildScrollView(
      physics: const BouncingScrollPhysics(),
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          const SizedBox(height: 20),
          
          // Back button
          Align(
            alignment: Alignment.centerLeft,
            child: IconButton(
              onPressed: () => Navigator.pop(context),
              icon: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.arrow_back, color: Colors.white),
              ),
            ),
          ),
          
          const SizedBox(height: 20),
          
          // Logo
          _buildLogo(),
          
          const SizedBox(height: 32),
          
          // Form Card
          _buildFormCard(),
          
          const SizedBox(height: 24),
          
          // Login link
          _buildLoginLink(),
          
          const SizedBox(height: 40),
        ],
      ),
    );
  }

  Widget _buildLogo() {
    return Column(
      children: [
        TweenAnimationBuilder<double>(
          tween: Tween(begin: 0, end: 1),
          duration: const Duration(milliseconds: 1000),
          curve: Curves.elasticOut,
          builder: (context, value, child) {
            return Transform.scale(
              scale: value,
              child: Container(
                width: 80,
                height: 80,
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [AppColors.secondary, AppColors.primary],
                  ),
                  borderRadius: BorderRadius.circular(24),
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.primary.withOpacity(0.4),
                      blurRadius: 25,
                      spreadRadius: 5,
                    ),
                  ],
                ),
                child: const Center(
                  child: Text('✨', style: TextStyle(fontSize: 36)),
                ),
              ),
            );
          },
        ),
        const SizedBox(height: 20),
        ShaderMask(
          shaderCallback: (bounds) => const LinearGradient(
            colors: [Colors.white, Color(0xFFE0E7FF)],
          ).createShader(bounds),
          child: const Text(
            'Create Account',
            style: TextStyle(
              fontSize: 28,
              fontWeight: FontWeight.w800,
              letterSpacing: -0.5,
              color: Colors.white,
            ),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Start your wellness journey today',
          style: TextStyle(
            fontSize: 15,
            color: Colors.white.withOpacity(0.6),
          ),
        ),
      ],
    );
  }

  Widget _buildFormCard() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.07),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withOpacity(0.1)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 30,
            offset: const Offset(0, 15),
          ),
        ],
      ),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildTextField(
              controller: _nameCtrl,
              label: 'Full Name',
              hint: 'Enter your full name',
              icon: Icons.person_outline,
              validator: (v) => (v == null || v.trim().isEmpty) ? 'Name required' : null,
            ),
            const SizedBox(height: 16),
            _buildTextField(
              controller: _emailCtrl,
              label: 'Email',
              hint: 'Enter your email',
              icon: Icons.email_outlined,
              keyboardType: TextInputType.emailAddress,
              validator: (v) {
                final value = v?.trim() ?? '';
                if (value.isEmpty) return 'Email required';
                if (!RegExp(r'^[^@]+@[^@]+\.[^@]+').hasMatch(value)) return 'Invalid email';
                return null;
              },
            ),
            const SizedBox(height: 16),
            _buildTextField(
              controller: _passwordCtrl,
              label: 'Password',
              hint: 'Create a password',
              icon: Icons.lock_outline,
              obscureText: _obscure,
              suffixIcon: IconButton(
                icon: Icon(
                  _obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                  color: Colors.white.withOpacity(0.5),
                ),
                onPressed: () => setState(() => _obscure = !_obscure),
              ),
              validator: (v) => (v == null || v.isEmpty) ? 'Password required' : null,
            ),
            const SizedBox(height: 16),
            _buildTextField(
              controller: _birthCtrl,
              label: 'Birth Date',
              hint: 'Select your birth date',
              icon: Icons.cake_outlined,
              readOnly: true,
              onTap: _pickBirthDate,
              suffixIcon: IconButton(
                icon: Icon(Icons.calendar_today, color: Colors.white.withOpacity(0.5)),
                onPressed: _pickBirthDate,
              ),
              validator: (v) => (v == null || v.isEmpty) ? 'Birth date required' : null,
            ),
            const SizedBox(height: 20),
            
            // Gender Selection
            Text(
              'Gender',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: Colors.white.withOpacity(0.8),
              ),
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: _buildGenderChip('Male', Icons.male, _gender == 'Male'),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _buildGenderChip('Female', Icons.female, _gender == 'Female'),
                ),
              ],
            ),
            
            const SizedBox(height: 20),
            
            // Consent checkbox
            _buildConsentCheckbox(),
            
            const SizedBox(height: 24),
            
            // Register button
            _buildRegisterButton(),
          ],
        ),
      ),
    );
  }

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    required String hint,
    required IconData icon,
    TextInputType? keyboardType,
    bool obscureText = false,
    bool readOnly = false,
    VoidCallback? onTap,
    Widget? suffixIcon,
    String? Function(String?)? validator,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: Colors.white.withOpacity(0.8),
          ),
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: controller,
          keyboardType: keyboardType,
          obscureText: obscureText,
          readOnly: readOnly,
          onTap: onTap,
          style: const TextStyle(color: Colors.white, fontSize: 15),
          validator: validator,
          decoration: InputDecoration(
            hintText: hint,
            hintStyle: TextStyle(color: Colors.white.withOpacity(0.3)),
            prefixIcon: Icon(icon, color: Colors.white.withOpacity(0.5), size: 20),
            suffixIcon: suffixIcon,
            filled: true,
            fillColor: Colors.white.withOpacity(0.08),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: BorderSide.none,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: BorderSide(color: Colors.white.withOpacity(0.1)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: const BorderSide(color: AppColors.secondary, width: 2),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: const BorderSide(color: AppColors.error),
            ),
            errorStyle: const TextStyle(color: AppColors.error),
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          ),
        ),
      ],
    );
  }

  Widget _buildGenderChip(String label, IconData icon, bool selected) {
    return GestureDetector(
      onTap: () => setState(() => _gender = label),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          gradient: selected
              ? LinearGradient(
                  colors: label == 'Male'
                      ? [const Color(0xFF3B82F6), const Color(0xFF1D4ED8)]
                      : [const Color(0xFFEC4899), const Color(0xFFDB2777)],
                )
              : null,
          color: selected ? null : Colors.white.withOpacity(0.08),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: selected ? Colors.transparent : Colors.white.withOpacity(0.1),
          ),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: Colors.white, size: 20),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                color: Colors.white,
                fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConsentCheckbox() {
    return GestureDetector(
      onTap: () async {
        if (!_consentGiven) {
          await _showConsentDialog();
        } else {
          setState(() => _consentGiven = false);
        }
      },
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: _consentGiven 
              ? AppColors.success.withOpacity(0.15)
              : Colors.white.withOpacity(0.05),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: _consentGiven 
                ? AppColors.success.withOpacity(0.5)
                : Colors.white.withOpacity(0.1),
          ),
        ),
        child: Row(
          children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              width: 24,
              height: 24,
              decoration: BoxDecoration(
                color: _consentGiven ? AppColors.success : Colors.transparent,
                borderRadius: BorderRadius.circular(6),
                border: Border.all(
                  color: _consentGiven ? AppColors.success : Colors.white.withOpacity(0.3),
                  width: 2,
                ),
              ),
              child: _consentGiven
                  ? const Icon(Icons.check, color: Colors.white, size: 16)
                  : null,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Data Sharing Consent',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    'Allow healthcare providers to view your progress',
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.5),
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
            Icon(
              Icons.info_outline,
              color: Colors.white.withOpacity(0.4),
              size: 20,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRegisterButton() {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      height: 56,
      child: ElevatedButton(
        onPressed: _loading ? null : _onRegister,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.secondary,
          foregroundColor: Colors.white,
          disabledBackgroundColor: AppColors.secondary.withOpacity(0.6),
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
        child: AnimatedSwitcher(
          duration: const Duration(milliseconds: 200),
          child: _loading
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
                    const Text(
                      'Create Account',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        letterSpacing: 0.3,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.all(4),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(Icons.arrow_forward, size: 16),
                    ),
                  ],
                ),
        ),
      ),
    );
  }

  Widget _buildLoginLink() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          'Already have an account? ',
          style: TextStyle(
            color: Colors.white.withOpacity(0.6),
            fontSize: 14,
          ),
        ),
        TextButton(
          onPressed: _loading ? null : () => Navigator.pop(context),
          style: TextButton.styleFrom(
            foregroundColor: AppColors.secondary,
            padding: EdgeInsets.zero,
          ),
          child: const Text(
            'Sign In',
            style: TextStyle(
              fontWeight: FontWeight.w600,
              fontSize: 14,
            ),
          ),
        ),
      ],
    );
  }
}

class _ConsentDialog extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: Colors.transparent,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 400),
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF1E1B4B), Color(0xFF312E81)],
          ),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: Colors.white.withOpacity(0.1)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.3),
              blurRadius: 30,
              offset: const Offset(0, 15),
            ),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Icon
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.primary.withOpacity(0.2),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.security_rounded,
                color: AppColors.primary,
                size: 40,
              ),
            ),
            const SizedBox(height: 20),
            
            // Title
            const Text(
              'Data Sharing Consent',
              style: TextStyle(
                color: Colors.white,
                fontSize: 22,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 12),
            
            // Description
            Text(
              'By giving consent, you allow healthcare administrators to:',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Colors.white.withOpacity(0.7),
                fontSize: 14,
              ),
            ),
            const SizedBox(height: 20),
            
            // Consent items
            _buildConsentItem(Icons.psychology_outlined, 'View your test results'),
            _buildConsentItem(Icons.assignment_outlined, 'Access your treatment plans'),
            _buildConsentItem(Icons.insights_outlined, 'Monitor your progress'),
            _buildConsentItem(Icons.support_agent_outlined, 'Provide better support'),
            
            const SizedBox(height: 20),
            
            // Note
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.warning.withOpacity(0.15),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.warning.withOpacity(0.3)),
              ),
              child: Row(
                children: [
                  Icon(Icons.info_outline, color: AppColors.warning, size: 20),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'You can change this anytime in your profile settings.',
                      style: TextStyle(
                        color: Colors.white.withOpacity(0.8),
                        fontSize: 12,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Buttons
            Row(
              children: [
                Expanded(
                  child: TextButton(
                    onPressed: () => Navigator.pop(context, false),
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                        side: BorderSide(color: Colors.white.withOpacity(0.2)),
                      ),
                    ),
                    child: const Text(
                      'Decline',
                      style: TextStyle(color: Colors.white70),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  flex: 2,
                  child: ElevatedButton(
                    onPressed: () => Navigator.pop(context, true),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.success,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.check_circle_outline, size: 20),
                        SizedBox(width: 8),
                        Text('I Agree'),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConsentItem(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: AppColors.success.withOpacity(0.15),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(icon, color: AppColors.success, size: 18),
          ),
          const SizedBox(width: 12),
          Text(
            text,
            style: const TextStyle(color: Colors.white, fontSize: 14),
          ),
        ],
      ),
    );
  }
}
