import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../core/api_client.dart';
import '../../core/models/login_models.dart';
import '../../core/theme/app_theme.dart';
import 'admin_dashboard_page.dart';

class AdminLoginPage extends StatefulWidget {
  final String baseUrl;
  const AdminLoginPage({super.key, required this.baseUrl});

  @override
  State<AdminLoginPage> createState() => _AdminLoginPageState();
}

class _AdminLoginPageState extends State<AdminLoginPage> with TickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _emailCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _obscure = true;
  bool _loading = false;
  
  late final ApiClient _api;
  late final AnimationController _backgroundController;
  late final AnimationController _fadeController;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    
    _backgroundController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 15),
    )..repeat();
    
    _fadeController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    )..forward();
  }

  @override
  void dispose() {
    _backgroundController.dispose();
    _fadeController.dispose();
    _api.close();
    _emailCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _onLogin() async {
    if (!_formKey.currentState!.validate()) return;
    
    setState(() => _loading = true);
    HapticFeedback.mediumImpact();
    
    try {
      final LoginResponse resp = await _api.login(
        _emailCtrl.text.trim(),
        _passwordCtrl.text,
      );

      // Check if user is admin
      if (resp.user.role?.toUpperCase() != 'ADMIN') {
        throw Exception('Access denied. Admin privileges required.');
      }

      await ApiClient.saveUserInfo(
        resp.user.id!,
        resp.user.displayName ?? 'Admin',
        resp.user.email ?? '',
        role: 'ADMIN',
      );

      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('auth_token', resp.token);

      if (!mounted) return;
      
      HapticFeedback.heavyImpact();

      Navigator.of(context).pushReplacement(
        PageRouteBuilder(
          pageBuilder: (context, animation, secondaryAnimation) => AdminDashboardPage(
            baseUrl: widget.baseUrl,
            adminName: resp.user.displayName ?? 'Admin',
            adminEmail: resp.user.email,
          ),
          transitionsBuilder: (context, animation, secondaryAnimation, child) {
            return FadeTransition(
              opacity: animation,
              child: child,
            );
          },
          transitionDuration: const Duration(milliseconds: 400),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      HapticFeedback.vibrate();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.white),
              const SizedBox(width: 12),
              Expanded(child: Text(e.toString())),
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
          // Dark gradient background
          _buildBackground(size),
          
          // Animated grid pattern
          _buildGridPattern(size),
          
          // Content
          SafeArea(
            child: FadeTransition(
              opacity: _fadeController,
              child: _buildContent(size),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBackground(Size size) {
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
                  const Color(0xFF0D0D0D),
                  const Color(0xFF1A1A2E),
                  _backgroundController.value,
                )!,
                const Color(0xFF0F0F23),
                Color.lerp(
                  const Color(0xFF1A0A2E),
                  const Color(0xFF0D0D0D),
                  _backgroundController.value,
                )!,
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildGridPattern(Size size) {
    return Positioned.fill(
      child: CustomPaint(
        painter: _GridPainter(_backgroundController),
      ),
    );
  }

  Widget _buildContent(Size size) {
    return SingleChildScrollView(
      physics: const BouncingScrollPhysics(),
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          minHeight: size.height - MediaQuery.of(context).padding.top - MediaQuery.of(context).padding.bottom,
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const SizedBox(height: 40),
            
            // Admin badge
            _buildAdminBadge(),
            
            const SizedBox(height: 48),
            
            // Login form
            _buildLoginCard(),
            
            const SizedBox(height: 24),
            
            // Back to user login
            _buildBackLink(),
            
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildAdminBadge() {
    return Column(
      children: [
        TweenAnimationBuilder<double>(
          tween: Tween(begin: 0, end: 1),
          duration: const Duration(milliseconds: 1200),
          curve: Curves.elasticOut,
          builder: (context, value, child) {
            return Transform.scale(
              scale: value,
              child: Container(
                width: 100,
                height: 100,
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [Color(0xFFDC2626), Color(0xFF991B1B)],
                  ),
                  borderRadius: BorderRadius.circular(28),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFFDC2626).withOpacity(0.4),
                      blurRadius: 30,
                      spreadRadius: 5,
                    ),
                  ],
                ),
                child: const Center(
                  child: Icon(
                    Icons.admin_panel_settings_rounded,
                    color: Colors.white,
                    size: 48,
                  ),
                ),
              ),
            );
          },
        ),
        
        const SizedBox(height: 24),
        
        ShaderMask(
          shaderCallback: (bounds) => const LinearGradient(
            colors: [Colors.white, Color(0xFFFCA5A5)],
          ).createShader(bounds),
          child: const Text(
            'Admin Portal',
            style: TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.w800,
              letterSpacing: -1,
              color: Colors.white,
            ),
          ),
        ),
        
        const SizedBox(height: 8),
        
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: const Color(0xFFDC2626).withOpacity(0.2),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: const Color(0xFFDC2626).withOpacity(0.3)),
          ),
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.lock, color: Color(0xFFFCA5A5), size: 14),
              SizedBox(width: 6),
              Text(
                'Restricted Access',
                style: TextStyle(
                  color: Color(0xFFFCA5A5),
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildLoginCard() {
    return Container(
      padding: const EdgeInsets.all(28),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withOpacity(0.08)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.3),
            blurRadius: 40,
            offset: const Offset(0, 20),
          ),
        ],
      ),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Email field
            _buildTextField(
              controller: _emailCtrl,
              label: 'Admin Email',
              hint: 'Enter admin email',
              icon: Icons.admin_panel_settings_outlined,
              keyboardType: TextInputType.emailAddress,
              validator: (v) {
                final value = v?.trim() ?? '';
                if (value.isEmpty) return 'Email is required';
                if (!RegExp(r'^[^@]+@[^@]+\.[^@]+').hasMatch(value)) {
                  return 'Enter a valid email';
                }
                return null;
              },
            ),
            
            const SizedBox(height: 20),
            
            // Password field
            _buildTextField(
              controller: _passwordCtrl,
              label: 'Password',
              hint: 'Enter admin password',
              icon: Icons.lock_outline,
              obscureText: _obscure,
              suffixIcon: IconButton(
                icon: Icon(
                  _obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                  color: Colors.white.withOpacity(0.5),
                ),
                onPressed: () => setState(() => _obscure = !_obscure),
              ),
              validator: (v) => (v == null || v.isEmpty) ? 'Password is required' : null,
            ),
            
            const SizedBox(height: 28),
            
            // Login button
            _buildLoginButton(),
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
            color: Colors.white.withOpacity(0.7),
          ),
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: controller,
          keyboardType: keyboardType,
          obscureText: obscureText,
          style: const TextStyle(color: Colors.white, fontSize: 15),
          validator: validator,
          decoration: InputDecoration(
            hintText: hint,
            hintStyle: TextStyle(color: Colors.white.withOpacity(0.3)),
            prefixIcon: Icon(icon, color: const Color(0xFFFCA5A5), size: 20),
            suffixIcon: suffixIcon,
            filled: true,
            fillColor: Colors.white.withOpacity(0.06),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: BorderSide.none,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: BorderSide(color: Colors.white.withOpacity(0.08)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: const BorderSide(color: Color(0xFFDC2626), width: 2),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: const BorderSide(color: AppColors.error),
            ),
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
          ),
        ),
      ],
    );
  }

  Widget _buildLoginButton() {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      height: 56,
      child: ElevatedButton(
        onPressed: _loading ? null : _onLogin,
        style: ElevatedButton.styleFrom(
          backgroundColor: const Color(0xFFDC2626),
          foregroundColor: Colors.white,
          disabledBackgroundColor: const Color(0xFFDC2626).withOpacity(0.6),
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
                      'Access Dashboard',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
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

  Widget _buildBackLink() {
    return TextButton.icon(
      onPressed: () => Navigator.pop(context),
      icon: const Icon(Icons.arrow_back, size: 18),
      label: const Text('Back to User Login'),
      style: TextButton.styleFrom(
        foregroundColor: Colors.white.withOpacity(0.6),
      ),
    );
  }
}

class _GridPainter extends CustomPainter {
  final Animation<double> animation;
  
  _GridPainter(this.animation) : super(repaint: animation);
  
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.white.withOpacity(0.03)
      ..strokeWidth = 1;
    
    const spacing = 40.0;
    final offset = animation.value * spacing;
    
    // Vertical lines
    for (double x = offset; x < size.width; x += spacing) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    
    // Horizontal lines
    for (double y = offset; y < size.height; y += spacing) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }
  
  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}

