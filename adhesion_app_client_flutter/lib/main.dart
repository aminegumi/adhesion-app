import 'package:flutter/foundation.dart'
    show kIsWeb, defaultTargetPlatform, TargetPlatform;
import 'package:flutter/material.dart';
import 'features/auth/login_page.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const AdhesionApp());
}

class AdhesionApp extends StatelessWidget {
  const AdhesionApp({super.key});

  static String effectiveBaseUrl() {
    if (kIsWeb) return 'http://localhost:8082';
    if (defaultTargetPlatform == TargetPlatform.android)
      return 'http://10.0.2.2:8082';
    // Windows desktop, iOS simulator, macOS
    return 'http://localhost:8082';
  }

  @override
  Widget build(BuildContext context) {
    final baseUrl = effectiveBaseUrl();
    final color = Colors.blue;
    return MaterialApp(
      title: 'Adhesion App',
      theme: ThemeData(
        brightness: Brightness.light,
        scaffoldBackgroundColor: const Color(0xFFF7F8FA),
        colorScheme: ColorScheme.fromSeed(seedColor: color),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: Colors.white,
          hintStyle: const TextStyle(color: Color(0xFF9AA4B2)),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: Color(0xFFE5E7EB)),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: Color(0xFFE5E7EB)),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide(color: color.shade600, width: 1.5),
          ),
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 14,
            vertical: 14,
          ),
        ),
      ),
      home: LoginPage(baseUrl: baseUrl),
      // Optionally: decide start page based on stored token/user
    );
  }
}
