import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/user_status.dart';
import '../dashboard/dashboard_page.dart';
import 'onboarding_tests_page.dart';

/// This page determines where to route the user based on their status.
/// - New users: Go to onboarding tests
/// - Users needing retake: Go to onboarding tests with retake message
/// - Completed users: Go to dashboard
class UserRouterPage extends StatefulWidget {
  final String baseUrl;
  final int userId;
  final String displayName;

  const UserRouterPage({
    super.key,
    required this.baseUrl,
    required this.userId,
    required this.displayName,
  });

  @override
  State<UserRouterPage> createState() => _UserRouterPageState();
}

class _UserRouterPageState extends State<UserRouterPage> {
  late final ApiClient _api;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _checkUserStatus();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _checkUserStatus() async {
    try {
      final status = await _api.getUserStatus(widget.userId);
      if (!mounted) return;

      if (status.canAccessFullApp) {
        // User has completed everything, go to dashboard
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => DashboardPage(
              baseUrl: widget.baseUrl,
              userName: widget.displayName,
            ),
          ),
        );
      } else {
        // User needs to complete tests or create profile
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => OnboardingTestsPage(
              baseUrl: widget.baseUrl,
              userId: widget.userId,
              displayName: widget.displayName,
              status: status,
            ),
          ),
        );
      }
    } catch (e) {
      // If status check fails, assume new user flow but allow access
      if (!mounted) return;
      setState(() {
        _error = 'Could not check status: $e';
        _loading = false;
      });

      // Fallback: go to dashboard anyway after a delay
      Future.delayed(const Duration(seconds: 2), () {
        if (mounted) {
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(
              builder: (_) => DashboardPage(
                baseUrl: widget.baseUrl,
                userName: widget.displayName,
              ),
            ),
          );
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (_loading) ...[
              const CircularProgressIndicator(),
              const SizedBox(height: 16),
              Text(
                'Welcome, ${widget.displayName}!',
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Checking your profile...',
                style: TextStyle(color: Colors.grey),
              ),
            ],
            if (_error != null) ...[
              const Icon(Icons.warning, size: 48, color: Colors.orange),
              const SizedBox(height: 16),
              Text(_error!, textAlign: TextAlign.center),
              const SizedBox(height: 8),
              const Text('Redirecting to dashboard...'),
            ],
          ],
        ),
      ),
    );
  }
}
