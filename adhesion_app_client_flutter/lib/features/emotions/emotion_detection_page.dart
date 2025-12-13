import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/ai_models.dart';

class EmotionDetectionPage extends StatefulWidget {
  final String baseUrl;
  const EmotionDetectionPage({super.key, required this.baseUrl});

  @override
  State<EmotionDetectionPage> createState() => _EmotionDetectionPageState();
}

class _EmotionDetectionPageState extends State<EmotionDetectionPage>
    with SingleTickerProviderStateMixin {
  late final ApiClient _api;
  final TextEditingController _textController = TextEditingController();
  late AnimationController _animationController;
  late Animation<double> _pulseAnimation;
  
  EmotionResult? _result;
  bool _loading = false;
  String? _error;

  final Map<String, EmotionConfig> _emotionConfigs = {
    'happy': EmotionConfig(
      icon: Icons.sentiment_very_satisfied_rounded,
      color: Color(0xFFFBBF24),
      gradient: [Color(0xFFFBBF24), Color(0xFFF59E0B)],
    ),
    'sad': EmotionConfig(
      icon: Icons.sentiment_dissatisfied_rounded,
      color: Color(0xFF6366F1),
      gradient: [Color(0xFF6366F1), Color(0xFF818CF8)],
    ),
    'anxious': EmotionConfig(
      icon: Icons.psychology_rounded,
      color: Color(0xFFF97316),
      gradient: [Color(0xFFF97316), Color(0xFFFB923C)],
    ),
    'stressed': EmotionConfig(
      icon: Icons.local_fire_department_rounded,
      color: Color(0xFFEF4444),
      gradient: [Color(0xFFEF4444), Color(0xFFF87171)],
    ),
    'calm': EmotionConfig(
      icon: Icons.spa_rounded,
      color: Color(0xFF10B981),
      gradient: [Color(0xFF10B981), Color(0xFF34D399)],
    ),
    'hopeful': EmotionConfig(
      icon: Icons.wb_sunny_rounded,
      color: Color(0xFF8B5CF6),
      gradient: [Color(0xFF8B5CF6), Color(0xFFA78BFA)],
    ),
    'frustrated': EmotionConfig(
      icon: Icons.sentiment_very_dissatisfied_rounded,
      color: Color(0xFFDC2626),
      gradient: [Color(0xFFDC2626), Color(0xFFEF4444)],
    ),
    'angry': EmotionConfig(
      icon: Icons.mood_bad_rounded,
      color: Color(0xFFB91C1C),
      gradient: [Color(0xFFB91C1C), Color(0xFFDC2626)],
    ),
    'overwhelmed': EmotionConfig(
      icon: Icons.waves_rounded,
      color: Color(0xFF0891B2),
      gradient: [Color(0xFF0891B2), Color(0xFF22D3EE)],
    ),
    'neutral': EmotionConfig(
      icon: Icons.sentiment_neutral_rounded,
      color: Color(0xFF6B7280),
      gradient: [Color(0xFF6B7280), Color(0xFF9CA3AF)],
    ),
  };

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _animationController = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    )..repeat(reverse: true);
    _pulseAnimation = Tween<double>(begin: 1.0, end: 1.08).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _animationController.dispose();
    _textController.dispose();
    _api.close();
    super.dispose();
  }

  EmotionConfig _getEmotionConfig(String emotion) {
    return _emotionConfigs[emotion.toLowerCase()] ?? _emotionConfigs['neutral']!;
  }

  Future<void> _analyzeEmotion() async {
    if (_textController.text.trim().isEmpty) {
      _showSnackBar('Please enter how you\'re feeling', isError: true);
      return;
    }

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

      final result = await _api.detectEmotions(userId, _textController.text);
      setState(() => _result = result);
    } catch (e) {
      String errorMessage = e.toString();
      if (errorMessage.contains('Connection refused')) {
        errorMessage = 'Could not connect to server.';
      }
      setState(() => _error = errorMessage);
    } finally {
      setState(() => _loading = false);
    }
  }

  void _showSnackBar(String message, {bool isError = false}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? Colors.red.shade400 : const Color(0xFF10B981),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        margin: const EdgeInsets.all(16),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFA855F7)],
            stops: [0.0, 0.5, 1.0],
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              _buildAppBar(),
              Expanded(
                child: _result != null
                    ? _buildResultView()
                    : _buildInputView(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
      child: Row(
        children: [
          IconButton(
            icon: Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(
                Icons.arrow_back_ios_new,
                color: Colors.white,
                size: 18,
              ),
            ),
            onPressed: () => Navigator.pop(context),
          ),
          const Expanded(
            child: Text(
              'Emotion Check-in',
              style: TextStyle(
                color: Colors.white,
                fontSize: 22,
                fontWeight: FontWeight.w700,
                letterSpacing: 0.5,
              ),
              textAlign: TextAlign.center,
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildInputView() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          ScaleTransition(
            scale: _pulseAnimation,
            child: Container(
              padding: const EdgeInsets.all(28),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    Colors.white.withOpacity(0.3),
                    Colors.white.withOpacity(0.1),
                  ],
                ),
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.1),
                    blurRadius: 30,
                    spreadRadius: 5,
                  ),
                ],
              ),
              child: const Icon(
                Icons.favorite_rounded,
                size: 70,
                color: Colors.white,
              ),
            ),
          ),
          const SizedBox(height: 32),
          const Text(
            'How are you feeling?',
            style: TextStyle(
              fontSize: 28,
              fontWeight: FontWeight.bold,
              color: Colors.white,
              letterSpacing: 0.5,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            'Share what\'s on your mind and let AI help\nyou understand your emotions',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 16,
              color: Colors.white.withOpacity(0.85),
              height: 1.5,
            ),
          ),
          const SizedBox(height: 32),
          if (_loading)
            _buildLoadingIndicator()
          else if (_error != null)
            _buildErrorView()
          else
            _buildInputField(),
        ],
      ),
    );
  }

  Widget _buildInputField() {
    return Column(
      children: [
        Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.1),
                blurRadius: 20,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: TextField(
            controller: _textController,
            maxLines: 5,
            style: const TextStyle(
              fontSize: 16,
              color: Color(0xFF1F2937),
              height: 1.5,
            ),
            decoration: InputDecoration(
              hintText: 'Express how you\'re feeling today...\n\nFor example: "I\'ve been feeling anxious about my medication schedule and sometimes forget to take them on time."',
              hintStyle: TextStyle(
                color: Colors.grey.shade400,
                fontSize: 15,
              ),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(20),
                borderSide: BorderSide.none,
              ),
              contentPadding: const EdgeInsets.all(20),
            ),
          ),
        ),
        const SizedBox(height: 24),
        SizedBox(
          width: double.infinity,
          height: 56,
          child: ElevatedButton(
            onPressed: _analyzeEmotion,
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.white,
              foregroundColor: const Color(0xFF6366F1),
              elevation: 0,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.psychology_rounded, size: 24),
                SizedBox(width: 10),
                Text(
                  'Analyze My Emotions',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildLoadingIndicator() {
    return Column(
      children: [
        SizedBox(
          width: 60,
          height: 60,
          child: CircularProgressIndicator(
            strokeWidth: 3,
            valueColor: AlwaysStoppedAnimation<Color>(
              Colors.white.withOpacity(0.9),
            ),
          ),
        ),
        const SizedBox(height: 24),
        const Text(
          'Analyzing your emotions...',
          style: TextStyle(
            color: Colors.white,
            fontSize: 16,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Powered by AI ✨',
          style: TextStyle(color: Colors.white.withOpacity(0.7), fontSize: 14),
        ),
      ],
    );
  }

  Widget _buildErrorView() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.15),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white.withOpacity(0.2)),
      ),
      child: Column(
        children: [
          Icon(
            Icons.info_outline_rounded,
            size: 48,
            color: Colors.white.withOpacity(0.9),
          ),
          const SizedBox(height: 16),
          Text(
            _error!,
            textAlign: TextAlign.center,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 15,
              height: 1.5,
            ),
          ),
          const SizedBox(height: 16),
          TextButton(
            onPressed: () => setState(() => _error = null),
            child: const Text(
              'Try Again',
              style: TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildResultView() {
    final config = _getEmotionConfig(_result!.primaryEmotion);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          // Primary Emotion Card
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(
                  color: config.color.withOpacity(0.3),
                  blurRadius: 30,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Column(
              children: [
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    gradient: LinearGradient(colors: config.gradient),
                    shape: BoxShape.circle,
                    boxShadow: [
                      BoxShadow(
                        color: config.color.withOpacity(0.4),
                        blurRadius: 20,
                        offset: const Offset(0, 6),
                      ),
                    ],
                  ),
                  child: Icon(
                    config.icon,
                    size: 50,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 20),
                Text(
                  _capitalize(_result!.primaryEmotion),
                  style: TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                    color: config.color,
                  ),
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                  decoration: BoxDecoration(
                    color: config.color.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    '${(_result!.confidence * 100).toStringAsFixed(0)}% confident',
                    style: TextStyle(
                      color: config.color,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          
          // Emotion Breakdown
          if (_result!.allEmotions.isNotEmpty) ...[
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(20),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Emotion Breakdown',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF1F2937),
                    ),
                  ),
                  const SizedBox(height: 16),
                  ..._result!.allEmotions.map((e) => _buildEmotionBar(e)),
                ],
              ),
            ),
            const SizedBox(height: 20),
          ],
          
          // Analysis
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: const Color(0xFF6366F1).withOpacity(0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Icon(
                        Icons.lightbulb_rounded,
                        color: Color(0xFF6366F1),
                      ),
                    ),
                    const SizedBox(width: 12),
                    const Text(
                      'Understanding',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF1F2937),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Text(
                  _result!.analysis,
                  style: const TextStyle(
                    fontSize: 15,
                    height: 1.6,
                    color: Color(0xFF4B5563),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          
          // Recommendation
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [config.color.withOpacity(0.1), config.color.withOpacity(0.05)],
              ),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: config.color.withOpacity(0.2)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: config.color.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Icon(
                        Icons.favorite_rounded,
                        color: config.color,
                      ),
                    ),
                    const SizedBox(width: 12),
                    const Text(
                      'Supportive Advice',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF1F2937),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Text(
                  _result!.recommendation,
                  style: const TextStyle(
                    fontSize: 15,
                    height: 1.6,
                    color: Color(0xFF4B5563),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          
          // Action Button
          SizedBox(
            width: double.infinity,
            height: 56,
            child: ElevatedButton(
              onPressed: () {
                setState(() {
                  _result = null;
                  _textController.clear();
                });
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.white,
                foregroundColor: const Color(0xFF6366F1),
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              child: const Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.refresh_rounded, size: 22),
                  SizedBox(width: 10),
                  Text(
                    'Check In Again',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmotionBar(EmotionScore emotion) {
    final config = _getEmotionConfig(emotion.emotion);
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(config.icon, size: 18, color: config.color),
                  const SizedBox(width: 8),
                  Text(
                    _capitalize(emotion.emotion),
                    style: const TextStyle(
                      fontWeight: FontWeight.w500,
                      color: Color(0xFF4B5563),
                    ),
                  ),
                ],
              ),
              Text(
                '${(emotion.score * 100).toStringAsFixed(0)}%',
                style: TextStyle(
                  fontWeight: FontWeight.w600,
                  color: config.color,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: emotion.score,
              backgroundColor: config.color.withOpacity(0.15),
              valueColor: AlwaysStoppedAnimation<Color>(config.color),
              minHeight: 6,
            ),
          ),
        ],
      ),
    );
  }

  String _capitalize(String s) {
    if (s.isEmpty) return s;
    return s[0].toUpperCase() + s.substring(1).toLowerCase();
  }
}

class EmotionConfig {
  final IconData icon;
  final Color color;
  final List<Color> gradient;

  const EmotionConfig({
    required this.icon,
    required this.color,
    required this.gradient,
  });
}
