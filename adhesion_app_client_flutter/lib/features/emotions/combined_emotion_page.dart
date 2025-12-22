import 'package:flutter/foundation.dart';
import 'package:flutter/foundation.dart' show kIsWeb, defaultTargetPlatform, TargetPlatform;
import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../../core/api_client.dart';
import '../../core/services/openrouter_service.dart';
import 'mobile_camera_view.dart';

// Conditional import for web
import 'web_camera_stub.dart' if (dart.library.html) 'web_camera_view.dart';

class CombinedEmotionPage extends StatefulWidget {
  final String baseUrl;

  const CombinedEmotionPage({super.key, required this.baseUrl});

  @override
  State<CombinedEmotionPage> createState() => _CombinedEmotionPageState();
}

class _CombinedEmotionPageState extends State<CombinedEmotionPage>
    with TickerProviderStateMixin, AutomaticKeepAliveClientMixin {
  late final ApiClient _apiClient;
  late final OpenRouterService _aiService;
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;
  
  // Keep alive to prevent disconnect on page refresh/navigation
  @override
  bool get wantKeepAlive => true;
  
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
  // For REAL PHONE testing: Replace with your computer's IP address
  // Run 'ipconfig' in terminal to find your IP (e.g., 192.168.1.x)
  static const String _realDeviceIP = '192.168.1.100'; // <-- CHANGE THIS to your PC's IP for real phone testing
  static const bool _useRealDevice = false; // Set to true when testing on real phone
  
  String get _flaskUrl {
    // For real device testing (physical phone)
    if (_useRealDevice && !kIsWeb) return 'http://$_realDeviceIP:5000';
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
      if (mounted) {
        setState(() => _isConnected = userId != null);
      }

      // Test Flask connection with retry
      await _connectToFlask();
    } catch (e) {
      if (mounted) {
        setState(() => _error = 'Connection error: $e');
      }
    }
  }

  Future<void> _connectToFlask() async {
    for (int attempt = 0; attempt < 3; attempt++) {
      try {
        final response = await http.get(Uri.parse('$_flaskUrl/health')).timeout(
          const Duration(seconds: 5),
        );
        if (response.statusCode == 200) {
          if (mounted) {
            setState(() {
              _flaskConnected = true;
              _error = null;
            });
          }
          print('✅ Flask connected on attempt ${attempt + 1}');
          return;
        }
      } catch (e) {
        print('Flask connection attempt ${attempt + 1} failed: $e');
        if (attempt < 2) {
          await Future.delayed(const Duration(seconds: 1));
        }
      }
    }
    print('⚠️ Flask server not available after 3 attempts');
    if (mounted) {
      setState(() => _flaskConnected = false);
    }
  }

  Future<void> _retryConnection() async {
    setState(() {
      _error = null;
      _flaskConnected = false;
    });
    await _initializeConnection();
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
      setState(() => _error = 'Camera AI service is not available. Trying to reconnect...');
      _retryConnection();
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
    final healthMessage = _getHealthMessage(emotion);
    
    showDialog(
      context: context,
      builder: (ctx) => Dialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        child: Container(
          constraints: BoxConstraints(
            maxHeight: MediaQuery.of(ctx).size.height * 0.85,
            maxWidth: 400,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Header with emotion
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  gradient: LinearGradient(colors: visual.gradient),
                  borderRadius: const BorderRadius.only(
                    topLeft: Radius.circular(24),
                    topRight: Radius.circular(24),
                  ),
                ),
                child: Column(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.2),
                        shape: BoxShape.circle,
                      ),
                      child: Icon(visual.icon, color: Colors.white, size: 40),
                    ),
                    const SizedBox(height: 16),
                    const Text(
                      'Emotion Detected',
                      style: TextStyle(color: Colors.white70, fontSize: 14),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      visual.label,
                      style: const TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        '${(_confidence * 100).toStringAsFixed(0)}% confidence',
                        style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w500),
                      ),
                    ),
                  ],
                ),
              ),
              // Scrollable content
              Flexible(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Health Insight section
                      Row(
                        children: [
                          Icon(Icons.health_and_safety_rounded, color: visual.color, size: 22),
                          const SizedBox(width: 8),
                          Text(
                            'AI Wellness Advice',
                            style: TextStyle(
                              color: visual.color,
                              fontWeight: FontWeight.w700,
                              fontSize: 16,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: visual.color.withOpacity(0.08),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: visual.color.withOpacity(0.15)),
                        ),
                        child: Text(
                          healthMessage,
                          style: TextStyle(
                            color: Colors.grey.shade800,
                            fontSize: 14,
                            height: 1.6,
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      // Quick tip
                      Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: Colors.amber.shade50,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: Colors.amber.shade200),
                        ),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(Icons.lightbulb_rounded, color: Colors.amber.shade700, size: 22),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Text(
                                _getQuickTip(emotion),
                                style: TextStyle(color: Colors.amber.shade900, fontSize: 13, height: 1.4),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              // Action button
              Padding(
                padding: const EdgeInsets.all(16),
                child: SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () => Navigator.pop(ctx),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: visual.color,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      elevation: 0,
                    ),
                    child: const Text(
                      'Got it!',
                      style: TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _getHealthMessage(String emotion) {
    switch (emotion.toLowerCase()) {
      case 'happy':
        return '''🌟 Physical Benefits:
• Boosts immune system by increasing antibody production
• Lowers cortisol (stress hormone) levels by up to 23%
• Reduces heart rate and blood pressure
• Releases endorphins - natural mood elevators and painkillers
• Improves sleep quality and duration

💪 Wellness Recommendations:
• Practice gratitude journaling to maintain this positive state
• Share your happiness with loved ones - social connection amplifies joy
• Engage in activities that brought you here
• Take a photo or write about this moment to revisit later
• Use this energy for creative or challenging tasks''';
      case 'sad':
        return '''💙 Understanding Sadness:
• Sadness is a natural response that allows emotional processing
• It signals a need for self-compassion and reflection
• Prolonged sadness can affect sleep, appetite, and immune function
• Your brain is processing loss, disappointment, or unmet needs

🌱 Wellness Recommendations:
• Allow yourself to feel - suppressing emotions prolongs them
• Gentle movement: a 15-minute walk can boost serotonin by 25%
• Reach out to one trusted person - social support is healing
• Practice self-compassion: speak to yourself as you would a friend
• Ensure adequate sleep (7-9 hours) - fatigue worsens mood
• Consider warm beverages, comforting music, or nature exposure''';
      case 'angry':
        return '''🔥 Physical Impact:
• Activates fight-or-flight response instantly
• Heart rate can increase by 30+ beats per minute
• Blood pressure spikes; blood vessels constrict
• Stress hormones flood your system for hours afterward
• Chronic anger increases risk of heart disease by 19%

🧘 Wellness Recommendations:
• STOP: Step back, Take a breath, Observe, Proceed mindfully
• 4-7-8 breathing: Inhale 4 sec, hold 7 sec, exhale 8 sec
• Physical release: brisk walk, push-ups, or squeeze a stress ball
• Identify the underlying need (respect, fairness, boundaries)
• Write down your thoughts before responding
• Splash cold water on face to activate calming dive reflex''';
      case 'fear':
      case 'fearful':
        return '''⚡ Body Response:
• Amygdala triggers immediate stress response
• Cortisol and adrenaline surge through your system
• Heart rate increases; breathing becomes shallow
• Blood flows away from digestive system to muscles
• Chronic fear weakens immune function and disrupts sleep

🛡️ Wellness Recommendations:
• Grounding technique (5-4-3-2-1): Name 5 things you see, 4 you hear, 3 you touch, 2 you smell, 1 you taste
• Box breathing: 4 counts in, 4 hold, 4 out, 4 hold
• Place hand on heart - physical touch releases oxytocin
• Reality check: "Is this fear based on facts or assumptions?"
• Progressive muscle relaxation from toes to head
• Remember: Fear is a signal, not a command to act''';
      case 'anxious':
        return '''😰 Physical Effects:
• Body stays in constant "alert mode"
• Muscle tension, especially in neck, shoulders, and jaw
• Digestive issues (IBS symptoms increase with anxiety)
• Shallow breathing reduces oxygen to brain
• Sleep disruption creates a negative feedback loop
• Weakened immune response over time

🌿 Wellness Recommendations:
• Diaphragmatic breathing: breathe into belly, not chest
• Reduce caffeine - it mimics anxiety symptoms
• "Worry window": schedule 15 min daily to address concerns
• Body scan meditation to release held tension
• Regular exercise reduces anxiety by 20% on average
• Limit news/social media if it triggers anxiety
• Maintain consistent sleep schedule (same time daily)''';
      case 'surprise':
      case 'surprised':
        return '''✨ What\'s Happening:
• Brain temporarily pauses to assess the unexpected
• Heightened attention and awareness activated
• Brief release of norepinephrine sharpens focus
• Generally neutral impact on long-term health
• Can transition to positive (delight) or negative (shock) states

🎯 Wellness Recommendations:
• Take a moment to process before reacting
• Use heightened awareness for mindfulness practice
• Notice how your body responds to the unexpected
• If positive surprise: savor the moment consciously
• If negative surprise: grounding techniques help stabilize
• Journal about what surprised you and why''';
      case 'neutral':
        return '''☯️ The Balanced State:
• Your nervous system is in optimal rest-and-digest mode
• Body performs maintenance and repair functions
• Cortisol levels are at healthy baseline
• Ideal state for clear thinking and decision-making
• Supports healthy digestion and immune function

🌸 Wellness Recommendations:
• This is an excellent time for:
  - Important decisions requiring clarity
  - Learning new skills or information
  - Creative work and problem-solving
  - Meditation and mindfulness practice
• Practice gratitude to potentially shift toward happiness
• Check in: are basic needs met? (sleep, nutrition, movement)
• Use this stable foundation for personal growth activities''';
      default:
        return '''🔍 Emotional Awareness:
• Recognizing your emotions is the first step to wellness
• All emotions carry valuable information about your needs
• Emotional awareness improves decision-making by 36%
• Regular check-ins build emotional intelligence over time

📝 Wellness Recommendations:
• Name your emotion specifically (frustrated vs. angry vs. irritated)
• Ask: "What is this emotion trying to tell me?"
• Journal about your feelings without judgment
• Practice the RAIN technique: Recognize, Allow, Investigate, Nurture
• Consider speaking with a mental health professional for deeper exploration''';
    }
  }

  String _getQuickTip(String emotion) {
    switch (emotion.toLowerCase()) {
      case 'happy':
        return '💡 Quick Action: Write down 3 things you\'re grateful for right now to extend this positive feeling!';
      case 'sad':
        return '💡 Quick Action: Step outside for 5 minutes - natural light boosts serotonin production naturally.';
      case 'angry':
        return '💡 Quick Action: Try the 4-7-8 breath now: Inhale 4 sec → Hold 7 sec → Exhale 8 sec. Repeat 3x.';
      case 'fear':
      case 'fearful':
        return '💡 Quick Action: Ground yourself - feel your feet on the floor and name 5 blue things you can see.';
      case 'anxious':
        return '💡 Quick Action: Place one hand on chest, one on belly. Breathe so only the belly hand moves.';
      case 'surprise':
      case 'surprised':
        return '💡 Quick Action: Take 3 slow breaths to process this moment before deciding how to respond.';
      default:
        return '💡 Quick Action: Close your eyes, take 3 deep breaths, and ask yourself: "What do I need right now?"';
    }
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
      final response = await http.get(Uri.parse('$_flaskUrl/emotion')).timeout(
        const Duration(seconds: 3),
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final emotion = (data['emotion'] ?? 'Neutral').toString().toLowerCase();
        final confidence = (data['confidence'] ?? 0.5).toDouble();
        
        if (mounted) {
          setState(() {
            _currentEmotion = emotion;
            _confidence = confidence;
            _flaskConnected = true; // Confirm connection is still good
          });
        }

        // Track emotion for auto-detect
        if (_isAutoDetecting) {
          _emotionCounts[emotion] = (_emotionCounts[emotion] ?? 0) + 1;
        }
      }
    } catch (e) {
      print('Failed to fetch emotion: $e');
      // If connection fails repeatedly, mark as disconnected
      if (mounted && _isStreaming) {
        setState(() {
          _flaskConnected = false;
          _error = 'Lost connection to camera AI. Trying to reconnect...';
        });
        // Try to reconnect
        await _connectToFlask();
      }
    }
  }

  EmotionVisual _getVisual(String emotion) {
    return _emotionVisuals[emotion.toLowerCase()] ?? _emotionVisuals['neutral']!;
  }

  @override
  Widget build(BuildContext context) {
    super.build(context); // Required for AutomaticKeepAliveClientMixin
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
          // Auto detect button (10 seconds) - Main feature
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
        child: kIsWeb
            ? WebCameraView(
                streamUrl: '$_flaskUrl/video',
                isStreaming: _isStreaming,
              )
            : MobileCameraView(
                streamUrl: '$_flaskUrl/video',
                isStreaming: _isStreaming,
                currentEmotion: _currentEmotion,
                confidence: _confidence,
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
