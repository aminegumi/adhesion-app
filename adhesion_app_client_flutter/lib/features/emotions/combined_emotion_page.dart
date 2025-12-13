import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'dart:html' as html;
import 'dart:ui_web' as ui_web;
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
  String _lastAdviceEmotion = '';
  double _confidence = 0.5;
  bool _isConnected = false;
  bool _flaskConnected = false;
  bool _isStreaming = false;
  bool _isScanning = false; // 12-second scan in progress
  int _scanSecondsLeft = 0;
  Timer? _emotionTimer;
  Timer? _countdownTimer;
  String? _error;
  String? _aiAdvice;
  bool _loadingAdvice = false;
  
  // Collect emotions during 12-second scan
  final List<Map<String, dynamic>> _scanResults = [];
  
  // Use localhost for web, the Flask server runs on your PC
  final String _flaskUrl = 'http://localhost:5000';
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
    _textController.dispose();
    _aiService.close();
    super.dispose();
  }

  Future<void> _initializeConnection() async {
    try {
      final userId = await ApiClient.getStoredUserId();
      setState(() => _isConnected = userId != null);

      // Test Flask connection with retry
      await _connectToFlask();
    } catch (e) {
      setState(() => _error = 'Connection error: $e');
    }
  }

  Future<void> _connectToFlask() async {
    for (int attempt = 0; attempt < 3; attempt++) {
      try {
        final response = await http.get(Uri.parse('$_flaskUrl/health')).timeout(
          const Duration(seconds: 5),
        );
        if (response.statusCode == 200) {
          setState(() {
            _flaskConnected = true;
            _error = null;
          });
          print('✅ Flask connected successfully');
          return;
        }
      } catch (e) {
        print('Flask connection attempt ${attempt + 1} failed: $e');
        if (attempt < 2) {
          await Future.delayed(const Duration(milliseconds: 500));
        }
      }
    }
    print('⚠️ Flask server not available after 3 attempts');
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

    if (_isScanning) {
      // Cancel ongoing scan
      _cancelScan();
      return;
    }

    if (_isStreaming) {
      // Stop streaming
      setState(() {
        _isStreaming = false;
        _error = null;
      });
      _emotionTimer?.cancel();
      _countdownTimer?.cancel();
    } else {
      // Start 5-second emotion scan
      _startEmotionScan();
    }
  }

  void _startEmotionScan() {
    setState(() {
      _isStreaming = true;
      _isScanning = true;
      _scanSecondsLeft = 12;
      _scanResults.clear();
      _error = null;
      _aiAdvice = null;
    });

    // Start countdown timer
    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() => _scanSecondsLeft--);
      if (_scanSecondsLeft <= 0) {
        timer.cancel();
        _finishScan();
      }
    });

    // Poll emotions every 500ms during scan
    _emotionTimer = Timer.periodic(const Duration(milliseconds: 500), (timer) async {
      if (!_isScanning) {
        timer.cancel();
        return;
      }
      await _collectEmotion();
    });
  }

  void _cancelScan() {
    _emotionTimer?.cancel();
    _countdownTimer?.cancel();
    setState(() {
      _isStreaming = false;
      _isScanning = false;
      _scanSecondsLeft = 0;
      _scanResults.clear();
    });
  }

  Future<void> _collectEmotion() async {
    try {
      final response = await http.get(Uri.parse('$_flaskUrl/emotion'));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final emotion = (data['emotion'] ?? 'Neutral').toString().toLowerCase();
        final confidence = (data['confidence'] ?? 0.5).toDouble();
        
        // Update display in real-time
        setState(() {
          _currentEmotion = emotion;
          _confidence = confidence;
        });
        
        // Collect for analysis
        _scanResults.add({
          'emotion': emotion,
          'confidence': confidence,
        });
      }
    } catch (e) {
      print('Failed to fetch emotion: $e');
    }
  }

  void _finishScan() {
    _emotionTimer?.cancel();
    _countdownTimer?.cancel();
    
    // Calculate dominant emotion
    final emotionCounts = <String, int>{};
    final emotionConfidences = <String, double>{};
    
    for (final result in _scanResults) {
      final emotion = result['emotion'] as String;
      final confidence = result['confidence'] as double;
      
      emotionCounts[emotion] = (emotionCounts[emotion] ?? 0) + 1;
      emotionConfidences[emotion] = (emotionConfidences[emotion] ?? 0) + confidence;
    }
    
    // Find dominant emotion (most frequent with highest average confidence)
    String dominantEmotion = 'neutral';
    int maxCount = 0;
    double maxAvgConfidence = 0;
    
    emotionCounts.forEach((emotion, count) {
      final avgConfidence = emotionConfidences[emotion]! / count;
      if (count > maxCount || (count == maxCount && avgConfidence > maxAvgConfidence)) {
        maxCount = count;
        maxAvgConfidence = avgConfidence;
        dominantEmotion = emotion;
      }
    });
    
    setState(() {
      _isScanning = false;
      _isStreaming = false;
      _currentEmotion = dominantEmotion;
      _confidence = maxAvgConfidence;
    });
    
    // Get AI advice for the dominant emotion
    _getEmotionAdvice(dominantEmotion);
  }

  Future<void> _fetchCurrentEmotion() async {
    try {
      final response = await http.get(Uri.parse('$_flaskUrl/emotion'));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final newEmotion = (data['emotion'] ?? 'Neutral').toString().toLowerCase();
        final newConfidence = (data['confidence'] ?? 0.5).toDouble();
        
        setState(() {
          _currentEmotion = newEmotion;
          _confidence = newConfidence;
        });
      }
    } catch (e) {
      print('Failed to fetch emotion: $e');
    }
  }

  Future<void> _getEmotionAdvice(String emotion) async {
    if (_loadingAdvice) return;
    
    setState(() => _loadingAdvice = true);
    
    try {
      // Get advice from Flask AI backend
      final response = await http.post(
        Uri.parse('$_flaskUrl/chat'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'message': 'I am feeling $emotion right now. Can you give me some supportive advice?',
          'history': [],
        }),
      );
      
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        setState(() {
          _aiAdvice = data['bot_message'] ?? _getDefaultAdvice(emotion);
        });
      } else {
        setState(() => _aiAdvice = _getDefaultAdvice(emotion));
      }
    } catch (e) {
      print('Failed to get AI advice: $e');
      setState(() => _aiAdvice = _getDefaultAdvice(emotion));
    } finally {
      setState(() => _loadingAdvice = false);
    }
  }

  Future<void> _analyzeCurrentEmotion() async {
    // Get advice for the current detected emotion from camera
    _lastAdviceEmotion = ''; // Reset to force new advice
    await _getEmotionAdvice(_currentEmotion);
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
          // Camera toggle button with scan progress
          Material(
            color: _isScanning 
                ? Colors.orange.withOpacity(0.3) 
                : Colors.white.withOpacity(0.2),
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
                    if (_isScanning) ...[
                      SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          value: (12 - _scanSecondsLeft) / 12,
                          strokeWidth: 2.5,
                          color: Colors.white,
                          backgroundColor: Colors.white.withOpacity(0.3),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        'Scanning... ${_scanSecondsLeft}s',
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                          fontSize: 14,
                        ),
                      ),
                    ] else ...[
                      Icon(
                        _isStreaming ? Icons.videocam_off_rounded : Icons.center_focus_strong_rounded,
                        color: Colors.white,
                      ),
                      const SizedBox(width: 10),
                      Text(
                        _isStreaming ? 'Stop Camera' : '🎯 Start 12s Emotion Scan',
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                          fontSize: 15,
                        ),
                      ),
                    ],
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
    // Register the HTML element for web
    final String viewType = 'camera-feed-${DateTime.now().millisecondsSinceEpoch}';
    
    // ignore: undefined_prefixed_name
    ui_web.platformViewRegistry.registerViewFactory(viewType, (int viewId) {
      final img = html.ImageElement()
        ..src = '$_flaskUrl/video'
        ..style.width = '100%'
        ..style.height = '100%'
        ..style.objectFit = 'cover'
        ..style.borderRadius = '20px';
      return img;
    });

    return Stack(
      children: [
        Container(
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
            child: HtmlElementView(viewType: viewType),
          ),
        ),
        // Scanning overlay - small timer at bottom left
        if (_isScanning)
          Positioned(
            left: 32,
            bottom: 16,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.black.withOpacity(0.6),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: Colors.orange,
                  width: 2,
                ),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(
                      value: (12 - _scanSecondsLeft) / 12,
                      strokeWidth: 3,
                      color: Colors.orange,
                      backgroundColor: Colors.white.withOpacity(0.3),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    '${_scanSecondsLeft}s',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),
          ),
      ],
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
