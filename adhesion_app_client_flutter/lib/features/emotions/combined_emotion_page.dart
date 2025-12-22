import 'package:flutter/foundation.dart';
import 'package:flutter/foundation.dart' show kIsWeb, defaultTargetPlatform, TargetPlatform;
import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../../core/api_client.dart';
import '../../core/services/openrouter_service.dart';

class CombinedEmotionPage extends StatefulWidget {
  final String baseUrl;

  const CombinedEmotionPage({super.key, required this.baseUrl});

  @override
  State<CombinedEmotionPage> createState() => _CombinedEmotionPageState();
}

class _CombinedEmotionPageState extends State<CombinedEmotionPage>
    with TickerProviderStateMixin {
  late final ApiClient _apiClient;
  late final OpenRouterService _aiService;
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;
  
  String _currentEmotion = 'Neutral';
  double _confidence = 0.5;
  bool _isConnected = false;
  bool _flaskConnected = false;
  bool _isStreaming = false;
  bool _isAutoDetecting = false;
  int _autoDetectCountdown = 0;
  Timer? _emotionTimer;
  Timer? _autoDetectTimer;
  String? _error;
  String? _aiAdvice;
  bool _loadingAdvice = false;
  final Map<String, int> _emotionCounts = {}; // Track emotion frequencies for auto-detect
  
  // Dynamic Flask URL based on platform
  String get _flaskUrl {
    // For web, use localhost
    if (kIsWeb) return 'http://localhost:5000';
    // For Android emulator use 10.0.2.2
    if (defaultTargetPlatform == TargetPlatform.android) return 'http://10.0.2.2:5000';
    // For other platforms (Windows, iOS, macOS)
    return 'http://localhost:5000';
  }
  final TextEditingController _textController = TextEditingController();
  final List<Map<String, dynamic>> _emotionHistory = [];

  final Map<String, EmotionVisual> _emotionVisuals = {
    'happy': EmotionVisual(
      icon: Icons.sentiment_very_satisfied_rounded,
      color: const Color(0xFFFBBF24),
      gradient: [const Color(0xFFFBBF24), const Color(0xFFF59E0B)],
      label: 'Happy',
    ),
    'sad': EmotionVisual(
      icon: Icons.sentiment_dissatisfied_rounded,
      color: const Color(0xFF6366F1),
      gradient: [const Color(0xFF6366F1), const Color(0xFF818CF8)],
      label: 'Sad',
    ),
    'angry': EmotionVisual(
      icon: Icons.mood_bad_rounded,
      color: const Color(0xFFEF4444),
      gradient: [const Color(0xFFEF4444), const Color(0xFFF87171)],
      label: 'Angry',
    ),
    'fear': EmotionVisual(
      icon: Icons.psychology_rounded,
      color: const Color(0xFFF97316),
      gradient: [const Color(0xFFF97316), const Color(0xFFFB923C)],
      label: 'Fearful',
    ),
    'anxious': EmotionVisual(
      icon: Icons.self_improvement_rounded,
      color: const Color(0xFF8B5CF6),
      gradient: [const Color(0xFF8B5CF6), const Color(0xFFA78BFA)],
      label: 'Anxious',
    ),
    'surprise': EmotionVisual(
      icon: Icons.face_rounded,
      color: const Color(0xFF10B981),
      gradient: [const Color(0xFF10B981), const Color(0xFF34D399)],
      label: 'Surprised',
    ),
    'neutral': EmotionVisual(
      icon: Icons.sentiment_neutral_rounded,
      color: const Color(0xFF6B7280),
      gradient: [const Color(0xFF6B7280), const Color(0xFF9CA3AF)],
      label: 'Neutral',
    ),
  };

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient(baseUrl: widget.baseUrl);
    _aiService = OpenRouterService();
    _setupAnimations();
    _initializeConnection();
  }

  void _setupAnimations() {
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat(reverse: true);
    
    _pulseAnimation = Tween<double>(begin: 1.0, end: 1.1).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _pulseController.dispose();
    _emotionTimer?.cancel();
    _autoDetectTimer?.cancel();
    _textController.dispose();
    _aiService.close();
    super.dispose();
  }

  Future<void> _initializeConnection() async {
    try {
      final userId = await ApiClient.getStoredUserId();
      setState(() => _isConnected = userId != null);

      // Test Flask connection
      try {
        final response = await http.get(Uri.parse('$_flaskUrl/health')).timeout(
          const Duration(seconds: 3),
        );
        if (response.statusCode == 200) {
          setState(() => _flaskConnected = true);
        }
      } catch (e) {
        print('Flask server not available: $e');
      }
    } catch (e) {
      setState(() => _error = 'Connection error: $e');
    }
  }

  Future<void> _analyzeTextEmotion() async {
    final text = _textController.text.trim();
    if (text.isEmpty) return;

    setState(() {
      _loadingAdvice = true;
      _error = null;
    });

    try {
      // Use OpenRouter AI to analyze emotion
      final analysis = await _aiService.analyzeTextEmotion(text);
      
      setState(() {
        _currentEmotion = analysis.primaryEmotion;
        _confidence = analysis.confidence;
        _aiAdvice = '${analysis.observation}\n\n${analysis.recommendation}';
        _emotionHistory.insert(0, {
          'emotion': _currentEmotion,
          'text': text,
          'timestamp': DateTime.now(),
        });
        if (_emotionHistory.length > 10) _emotionHistory.removeLast();
      });
    } catch (e) {
      // Fallback to simple keyword detection
      _detectEmotionFromKeywords(text);
    } finally {
      setState(() => _loadingAdvice = false);
      _textController.clear();
    }
  }

  void _detectEmotionFromKeywords(String text) {
    final lowerText = text.toLowerCase();
    String emotion = 'neutral';
    double confidence = 0.6;

    if (lowerText.contains('happy') || lowerText.contains('great') || lowerText.contains('joy')) {
      emotion = 'happy';
      confidence = 0.75;
    } else if (lowerText.contains('sad') || lowerText.contains('depress') || lowerText.contains('down')) {
      emotion = 'sad';
      confidence = 0.75;
    } else if (lowerText.contains('angry') || lowerText.contains('frustrat') || lowerText.contains('mad')) {
      emotion = 'angry';
      confidence = 0.75;
    } else if (lowerText.contains('anxious') || lowerText.contains('worried') || lowerText.contains('stress')) {
      emotion = 'anxious';
      confidence = 0.75;
    } else if (lowerText.contains('scared') || lowerText.contains('fear') || lowerText.contains('afraid')) {
      emotion = 'fear';
      confidence = 0.75;
    }

    setState(() {
      _currentEmotion = emotion;
      _confidence = confidence;
      _aiAdvice = _getDefaultAdvice(emotion);
    });
  }

  String _getDefaultAdvice(String emotion) {
    switch (emotion.toLowerCase()) {
      case 'happy':
        return 'Wonderful! Your positive energy is valuable. Consider sharing this joy with others or journaling about what made you feel this way.';
      case 'sad':
        return 'It\'s okay to feel sad. Be gentle with yourself. Consider reaching out to someone you trust or engaging in a comforting activity.';
      case 'angry':
        return 'Anger is a valid emotion. Try deep breathing exercises or physical activity to help process these feelings constructively.';
      case 'anxious':
        return 'Anxiety can be challenging. Try grounding techniques: focus on 5 things you can see, 4 you can touch, 3 you can hear.';
      case 'fear':
        return 'Fear often signals a need for safety. You\'re safe here. Consider what specific concern is causing this feeling.';
      default:
        return 'Take a moment to check in with yourself. How are you really feeling today?';
    }
  }

  void _toggleStreaming() {
    if (!_flaskConnected) {
      setState(() => _error = 'Camera AI service is not available. Start the Flask server first.');
      return;
    }

    setState(() {
      _isStreaming = !_isStreaming;
      _error = null;
    });

    if (_isStreaming) {
      _startEmotionPolling();
    } else {
      _emotionTimer?.cancel();
      _autoDetectTimer?.cancel();
      _isAutoDetecting = false;
    }
  }

  void _startAutoDetect() {
    if (!_flaskConnected) {
      setState(() => _error = 'Camera AI service is not available. Start the Flask server first.');
      return;
    }

    setState(() {
      _isAutoDetecting = true;
      _autoDetectCountdown = 10;
      _emotionCounts.clear();
      _error = null;
      if (!_isStreaming) {
        _isStreaming = true;
      }
    });

    _startEmotionPolling();

    // Countdown timer
    _autoDetectTimer?.cancel();
    _autoDetectTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }
      
      setState(() {
        _autoDetectCountdown--;
      });

      if (_autoDetectCountdown <= 0) {
        timer.cancel();
        _finishAutoDetect();
      }
    });
  }

  void _finishAutoDetect() {
    _emotionTimer?.cancel();
    _autoDetectTimer?.cancel();

    // Find dominant emotion
    String dominantEmotion = 'neutral';
    int maxCount = 0;
    _emotionCounts.forEach((emotion, count) {
      if (count > maxCount) {
        maxCount = count;
        dominantEmotion = emotion;
      }
    });

    setState(() {
      _isAutoDetecting = false;
      _isStreaming = false;
      _currentEmotion = dominantEmotion;
      _aiAdvice = _getDefaultAdvice(dominantEmotion);
    });

    // Add to history
    _emotionHistory.insert(0, {
      'emotion': dominantEmotion,
      'timestamp': DateTime.now(),
      'source': 'camera_auto',
      'samples': maxCount,
    });

    // Show result dialog
    _showDominantEmotionResult(dominantEmotion);
  }

  void _showDominantEmotionResult(String emotion) {
    final visual = _getVisual(emotion);
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(
          children: [
            Icon(visual.icon, color: visual.color, size: 32),
            const SizedBox(width: 12),
            const Text('Detection Complete'),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Your dominant emotion is:',
              style: TextStyle(color: Colors.grey.shade600),
            ),
            const SizedBox(height: 12),
            Text(
              visual.label,
              style: TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.bold,
                color: visual.color,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              _getDefaultAdvice(emotion),
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey.shade700, fontSize: 14),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text('OK', style: TextStyle(color: visual.color)),
          ),
        ],
      ),
    );
  }

  void _startEmotionPolling() {
    _emotionTimer?.cancel();
    _emotionTimer = Timer.periodic(const Duration(seconds: 1), (timer) async {
      if (!_isStreaming) {
        timer.cancel();
        return;
      }
      await _fetchCurrentEmotion();
    });
  }

  Future<void> _fetchCurrentEmotion() async {
    try {
      final response = await http.get(Uri.parse('$_flaskUrl/emotion'));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final emotion = (data['emotion'] ?? 'Neutral').toString().toLowerCase();
        final confidence = (data['confidence'] ?? 0.5).toDouble();
        
        setState(() {
          _currentEmotion = emotion;
          _confidence = confidence;
        });

        // Track emotion for auto-detect
        if (_isAutoDetecting) {
          _emotionCounts[emotion] = (_emotionCounts[emotion] ?? 0) + 1;
        }
      }
    } catch (e) {
      print('Failed to fetch emotion: $e');
    }
  }

  EmotionVisual _getVisual(String emotion) {
    return _emotionVisuals[emotion.toLowerCase()] ?? _emotionVisuals['neutral']!;
  }

  @override
  Widget build(BuildContext context) {
    final visual = _getVisual(_currentEmotion);
    
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              visual.gradient[0].withOpacity(0.15),
              Colors.white,
              Colors.white,
            ],
          ),
        ),
        child: SafeArea(
          child: CustomScrollView(
            slivers: [
              _buildAppBar(visual),
              SliverToBoxAdapter(child: _buildEmotionDisplay(visual)),
              if (_isStreaming) SliverToBoxAdapter(child: _buildCameraFeed()),
              SliverToBoxAdapter(child: _buildTextAnalysis()),
              if (_aiAdvice != null) SliverToBoxAdapter(child: _buildAdviceCard(visual)),
              if (_emotionHistory.isNotEmpty) SliverToBoxAdapter(child: _buildHistory()),
              const SliverToBoxAdapter(child: SizedBox(height: 100)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAppBar(EmotionVisual visual) {
    return SliverAppBar(
      floating: true,
      backgroundColor: Colors.transparent,
      elevation: 0,
      leading: IconButton(
        icon: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: visual.color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(Icons.arrow_back_ios_new, color: visual.color, size: 18),
        ),
        onPressed: () => Navigator.pop(context),
      ),
      title: const Text(
        'Emotion Detection',
        style: TextStyle(
          color: Color(0xFF1F2937),
          fontWeight: FontWeight.w700,
        ),
      ),
      actions: [
        _buildConnectionBadge('Backend', _isConnected, Icons.cloud_done_rounded),
        const SizedBox(width: 8),
        _buildConnectionBadge('Camera', _flaskConnected, Icons.camera_alt_rounded),
        const SizedBox(width: 16),
      ],
    );
  }

  Widget _buildConnectionBadge(String label, bool connected, IconData icon) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: connected ? const Color(0xFF10B981).withOpacity(0.1) : const Color(0xFFF59E0B).withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        children: [
          Icon(
            icon,
            size: 14,
            color: connected ? const Color(0xFF10B981) : const Color(0xFFF59E0B),
          ),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: connected ? const Color(0xFF10B981) : const Color(0xFFF59E0B),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmotionDisplay(EmotionVisual visual) {
    return Container(
      margin: const EdgeInsets.all(20),
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: LinearGradient(colors: visual.gradient),
        borderRadius: BorderRadius.circular(28),
        boxShadow: [
          BoxShadow(
            color: visual.color.withOpacity(0.3),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            children: [
              ScaleTransition(
                scale: _pulseAnimation,
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Icon(visual.icon, size: 48, color: Colors.white),
                ),
              ),
              const SizedBox(width: 20),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Current Emotion',
                      style: TextStyle(color: Colors.white70, fontSize: 14),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      visual.label,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 28,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 8),
                    _buildConfidenceBar(),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          // Camera toggle button
          Material(
            color: Colors.white.withOpacity(0.2),
            borderRadius: BorderRadius.circular(16),
            child: InkWell(
              onTap: _toggleStreaming,
              borderRadius: BorderRadius.circular(16),
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 14),
                width: double.infinity,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      _isStreaming ? Icons.videocam_off_rounded : Icons.videocam_rounded,
                      color: Colors.white,
                    ),
                    const SizedBox(width: 10),
                    Text(
                      _isStreaming ? 'Stop Camera' : 'Start Camera Detection',
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w600,
                        fontSize: 15,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          // Auto detect button (10 seconds)
          Material(
            color: _isAutoDetecting ? Colors.white.withOpacity(0.4) : Colors.white.withOpacity(0.2),
            borderRadius: BorderRadius.circular(16),
            child: InkWell(
              onTap: _isAutoDetecting ? null : _startAutoDetect,
              borderRadius: BorderRadius.circular(16),
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 14),
                width: double.infinity,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      _isAutoDetecting ? Icons.hourglass_top_rounded : Icons.timer_rounded,
                      color: Colors.white,
                    ),
                    const SizedBox(width: 10),
                    Text(
                      _isAutoDetecting 
                          ? 'Detecting... $_autoDetectCountdown s'
                          : 'Auto Detect (10s)',
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w600,
                        fontSize: 15,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  const Icon(Icons.info_outline, color: Colors.white, size: 18),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      _error!,
                      style: const TextStyle(color: Colors.white, fontSize: 13),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildConfidenceBar() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Text(
              'Confidence',
              style: TextStyle(color: Colors.white70, fontSize: 12),
            ),
            const Spacer(),
            Text(
              '${(_confidence * 100).toStringAsFixed(0)}%',
              style: const TextStyle(
                color: Colors.white,
                fontSize: 12,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: _confidence,
            backgroundColor: Colors.white.withOpacity(0.2),
            valueColor: const AlwaysStoppedAnimation<Color>(Colors.white),
            minHeight: 6,
          ),
        ),
      ],
    );
  }

  Widget _buildCameraFeed() {
    // For web, use HtmlElementView with img tag that supports MJPEG
    if (kIsWeb) {
      return Container(
        height: 280,
        margin: const EdgeInsets.symmetric(horizontal: 20),
        decoration: BoxDecoration(
          color: Colors.black,
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.2),
              blurRadius: 20,
              offset: const Offset(0, 10),
            ),
          ],
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(20),
          child: _MjpegStreamWidget(
            streamUrl: '$_flaskUrl/video',
          ),
        ),
      );
    }
    
    // For mobile/desktop, use Image.network
    return Container(
      height: 280,
      margin: const EdgeInsets.symmetric(horizontal: 20),
      decoration: BoxDecoration(
        color: Colors.black,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: Image.network(
          '$_flaskUrl/video',
          fit: BoxFit.cover,
          loadingBuilder: (context, child, loadingProgress) {
            if (loadingProgress == null) return child;
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(color: Colors.white),
                  SizedBox(height: 12),
                  Text('Connecting to camera...', style: TextStyle(color: Colors.white70)),
                ],
              ),
            );
          },
          errorBuilder: (context, error, stackTrace) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.videocam_off, size: 48, color: Colors.grey.shade600),
                  const SizedBox(height: 12),
                  Text(
                    'Camera feed unavailable',
                    style: TextStyle(color: Colors.grey.shade400),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Ensure Flask server is running',
                    style: TextStyle(color: Colors.grey.shade600, fontSize: 12),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildTextAnalysis() {
    return Container(
      margin: const EdgeInsets.all(20),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 15,
            offset: const Offset(0, 5),
          ),
        ],
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
                child: const Icon(Icons.edit_note_rounded, color: Color(0xFF6366F1)),
              ),
              const SizedBox(width: 14),
              const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Text-Based Detection',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      color: Color(0xFF1F2937),
                    ),
                  ),
                  Text(
                    'Share how you\'re feeling',
                    style: TextStyle(color: Color(0xFF6B7280), fontSize: 13),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _textController,
            maxLines: 3,
            decoration: InputDecoration(
              hintText: 'Describe your current mood or thoughts...',
              hintStyle: TextStyle(color: Colors.grey.shade400),
              filled: true,
              fillColor: const Color(0xFFF8FAFC),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: BorderSide.none,
              ),
            ),
          ),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _loadingAdvice ? null : _analyzeTextEmotion,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF6366F1),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              child: _loadingAdvice
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.psychology_rounded),
                        SizedBox(width: 8),
                        Text('Analyze Emotion'),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAdviceCard(EmotionVisual visual) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: visual.color.withOpacity(0.05),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: visual.color.withOpacity(0.2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.auto_awesome, color: visual.color),
              const SizedBox(width: 10),
              Text(
                'AI Wellness Advice',
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.w700,
                  color: visual.color,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            _aiAdvice!,
            style: const TextStyle(
              fontSize: 15,
              color: Color(0xFF374151),
              height: 1.6,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHistory() {
    return Container(
      margin: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Recent Emotions',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: Color(0xFF1F2937),
            ),
          ),
          const SizedBox(height: 14),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: _emotionHistory.take(5).map((item) {
              final visual = _getVisual(item['emotion']);
              return Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                decoration: BoxDecoration(
                  color: visual.color.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(visual.icon, size: 18, color: visual.color),
                    const SizedBox(width: 6),
                    Text(
                      visual.label,
                      style: TextStyle(
                        color: visual.color,
                        fontWeight: FontWeight.w600,
                        fontSize: 13,
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}

/// MJPEG Stream Widget for Web platform
/// Displays information about viewing the stream externally
class _MjpegStreamWidget extends StatelessWidget {
  final String streamUrl;
  
  const _MjpegStreamWidget({required this.streamUrl});
  
  @override
  Widget build(BuildContext context) {
    // For Flutter web, MJPEG streams don't work with Image.network
    // Show a helpful message with a link to view externally
    return Container(
      color: Colors.black,
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.1),
                borderRadius: BorderRadius.circular(50),
              ),
              child: const Icon(Icons.videocam_rounded, size: 40, color: Colors.white70),
            ),
            const SizedBox(height: 20),
            const Text(
              'Camera Feed Active',
              style: TextStyle(
                color: Colors.white,
                fontSize: 18,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Emotion detection is running',
              style: TextStyle(color: Colors.white.withOpacity(0.6), fontSize: 14),
            ),
            const SizedBox(height: 20),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                color: const Color(0xFF10B981).withOpacity(0.2),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: Color(0xFF10B981),
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 8),
                  const Text(
                    'Live',
                    style: TextStyle(
                      color: Color(0xFF10B981),
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'View stream at: $streamUrl',
              style: TextStyle(
                color: Colors.white.withOpacity(0.4),
                fontSize: 11,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class EmotionVisual {
  final IconData icon;
  final Color color;
  final List<Color> gradient;
  final String label;

  EmotionVisual({
    required this.icon,
    required this.color,
    required this.gradient,
    required this.label,
  });
}
