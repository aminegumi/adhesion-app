import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/ai_models.dart';

class MotivationPage extends StatefulWidget {
  final String baseUrl;
  const MotivationPage({super.key, required this.baseUrl});

  @override
  State<MotivationPage> createState() => _MotivationPageState();
}

class _MotivationPageState extends State<MotivationPage> {
  late final ApiClient _api;
  String? _message;
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _getMotivation() async {
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

      final response = await _api.getMotivation(userId);
      setState(() => _message = response.message);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              Colors.indigo.shade400,
              Colors.purple.shade300,
              Colors.pink.shade200,
            ],
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              // App Bar
              Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    IconButton(
                      icon: const Icon(Icons.arrow_back, color: Colors.white),
                      onPressed: () => Navigator.pop(context),
                    ),
                    const Expanded(
                      child: Text(
                        'Daily Motivation',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                    const SizedBox(width: 48), // Balance the back button
                  ],
                ),
              ),

              Expanded(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      // Decorative icon
                      Container(
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.2),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.favorite,
                          size: 64,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(height: 32),

                      if (_message == null && !_loading && _error == null) ...[
                        const Text(
                          'Need a boost?',
                          style: TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                        const SizedBox(height: 12),
                        Text(
                          'Tap the button below to receive a personalized motivational message powered by AI.',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 16,
                            color: Colors.white.withOpacity(0.9),
                          ),
                        ),
                      ],

                      if (_loading) ...[
                        const CircularProgressIndicator(color: Colors.white),
                        const SizedBox(height: 16),
                        const Text(
                          'Generating your motivation...',
                          style: TextStyle(color: Colors.white),
                        ),
                      ],

                      if (_error != null) ...[
                        Icon(
                          Icons.error_outline,
                          size: 48,
                          color: Colors.white.withOpacity(0.8),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          _error!,
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: Colors.white.withOpacity(0.9),
                          ),
                        ),
                      ],

                      if (_message != null) ...[
                        Expanded(
                          child: SingleChildScrollView(
                            child: Card(
                              elevation: 8,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(20),
                              ),
                              child: Padding(
                                padding: const EdgeInsets.all(24),
                                child: Column(
                                  children: [
                                    const Icon(
                                      Icons.format_quote,
                                      size: 32,
                                      color: Colors.indigo,
                                    ),
                                    const SizedBox(height: 16),
                                    Text(
                                      _message!,
                                      style: const TextStyle(
                                        fontSize: 16,
                                        height: 1.6,
                                      ),
                                      textAlign: TextAlign.center,
                                    ),
                                    const SizedBox(height: 16),
                                    Row(
                                      mainAxisAlignment:
                                          MainAxisAlignment.center,
                                      children: [
                                        Icon(
                                          Icons.auto_awesome,
                                          size: 16,
                                          color: Colors.grey[400],
                                        ),
                                        const SizedBox(width: 4),
                                        Text(
                                          'AI-powered motivation',
                                          style: TextStyle(
                                            fontSize: 12,
                                            color: Colors.grey[500],
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],

                      const SizedBox(height: 32),

                      // Action button
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton.icon(
                          onPressed: _loading ? null : _getMotivation,
                          icon: Icon(
                            _message == null
                                ? Icons.auto_awesome
                                : Icons.refresh,
                          ),
                          label: Text(
                            _message == null
                                ? 'Get Motivated!'
                                : 'Get Another Message',
                          ),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.white,
                            foregroundColor: Colors.indigo,
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(30),
                            ),
                            elevation: 4,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
