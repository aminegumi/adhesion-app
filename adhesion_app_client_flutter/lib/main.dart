import 'package:flutter/foundation.dart'
    show kIsWeb, defaultTargetPlatform, TargetPlatform;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'features/auth/login_page.dart';
import 'core/theme/app_theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Set system UI style
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.light,
    systemNavigationBarColor: Color(0xFF1A1A2E),
    systemNavigationBarIconBrightness: Brightness.light,
  ));
  
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
    
    return MaterialApp(
      title: 'Serenity - Mental Health',
      debugShowCheckedModeBanner: false,
      theme: buildAppTheme(),
      home: LoginPage(baseUrl: baseUrl),
    );
  }
}
