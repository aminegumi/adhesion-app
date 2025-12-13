import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/ai_models.dart';
import '../../core/services/openrouter_service.dart';

class MotivationPage extends StatefulWidget {
  final String baseUrl;
  const MotivationPage({super.key, required this.baseUrl});

  @override
  State<MotivationPage> createState() => _MotivationPageState();
}

class _MotivationPageState extends State<MotivationPage>
    with TickerProviderStateMixin {
  late final ApiClient _api;
  late final OpenRouterService _aiService;
  late AnimationController _pulseController;
  late AnimationController _fadeController;
  late Animation<double> _pulseAnimation;
  late Animation<double> _fadeAnimation;
  
  final List<ChatMessage> _messages = [];
  final List<ChatMessageData> _conversationHistory = [];
  bool _loading = false;
  String? _error;
  final TextEditingController _questionController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  String _currentMood = 'neutral';
  double? _adherenceScore;

  final List<String> _conversationStarters = [
    'How can I stay motivated with my treatment?',
    'I\'m feeling overwhelmed today',
    'Tips for better medication adherence?',
    'I need some encouragement',
    'How do I manage stress?',
  ];

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _aiService = OpenRouterService();
    _setupAnimations();
    _loadUserContext();
  }
  
  void _setupAnimations() {
    _pulseController = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    )..repeat(reverse: true);
    
    _fadeController = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    
    _pulseAnimation = Tween<double>(begin: 1.0, end: 1.08).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
    
    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _fadeController, curve: Curves.easeOut),
    );
    
    _fadeController.forward();
  }

  Future<void> _loadUserContext() async {
    try {
      final userId = await ApiClient.getStoredUserId();
      if (userId != null) {
        try {
          _adherenceScore = await _api.getAdherenceScore(userId);
        } catch (e) {
          _adherenceScore = 0.5;
        }
      }
    } catch (e) {
      print('Error loading user context: $e');
    }
  }

  @override
  void dispose() {
    _pulseController.dispose();
    _fadeController.dispose();
    _questionController.dispose();
    _scrollController.dispose();
    _api.close();
    _aiService.close();
    super.dispose();
  }

  String _cleanMarkdown(String text) {
    String cleaned = text;
    cleaned = cleaned.replaceAll(RegExp(r'\[Your Name\]', caseSensitive: false), '');
    cleaned = cleaned.replaceAll(RegExp(r'\[Name\]', caseSensitive: false), '');
    cleaned = cleaned.replaceAll(RegExp(r'^#{1,6}\s*', multiLine: true), '');
    cleaned = cleaned.replaceAllMapped(RegExp(r'\*\*(.+?)\*\*'), (m) => m.group(1) ?? '');
    cleaned = cleaned.replaceAllMapped(RegExp(r'\*(.+?)\*'), (m) => m.group(1) ?? '');
    cleaned = cleaned.replaceAll(RegExp(r'\n{3,}'), '\n\n');
    cleaned = cleaned.replaceAll(RegExp(r' +'), ' ');
    return cleaned.trim();
  }

  /// Check if the question is clearly off-topic (sports, entertainment, etc.)
  bool _isOffTopic(String question) {
    final offTopicKeywords = [
      'football', 'soccer', 'basketball', 'baseball', 'hockey', 'tennis',
      'movie', 'film', 'actor', 'actress', 'celebrity', 'music', 'song',
      'politics', 'election', 'president', 'vote', 'politician',
      'stock', 'crypto', 'bitcoin', 'investment', 'trading',
      'game', 'gaming', 'video game', 'playstation', 'xbox', 'nintendo',
      'weather forecast', 'recipe', 'cooking instructions', 'travel destination',
    ];
    final lowerQuestion = question.toLowerCase();
    return offTopicKeywords.any((keyword) => lowerQuestion.contains(keyword));
  }

  Future<void> _sendMessage({String? preset}) async {
    final question = preset ?? _questionController.text.trim();
    if (question.isEmpty) {
      setState(() => _error = 'Please enter a message.');
      return;
    }

    // Check if the question is clearly off-topic
    if (_isOffTopic(question)) {
      // Add a gentle redirect message instead of blocking
      final userMessage = ChatMessage(
        role: 'user',
        content: question,
        timestamp: DateTime.now(),
      );
      
      final redirectMessage = ChatMessage(
        role: 'assistant',
        content: "I'm Serenity, your health and wellness assistant. I specialize in mental health support, medication adherence, and wellness topics. How can I help you with your health journey today?",
        timestamp: DateTime.now(),
      );
      
      setState(() {
        _messages.add(userMessage);
        _messages.add(redirectMessage);
        _questionController.clear();
      });
      _scrollToBottom();
      return;
    }

    final userMessage = ChatMessage(
      role: 'user',
      content: question,
      timestamp: DateTime.now(),
    );

    setState(() {
      _messages.add(userMessage);
      _conversationHistory.add(ChatMessageData(
        role: 'user',
        content: question,
      ));
      _loading = true;
      _error = null;
      _questionController.clear();
    });

    _scrollToBottom();

    try {
      String response;
      
      // Use OpenRouter service with health-focused system prompt
      response = await _aiService.chat(
        userMessage: question,
        conversationHistory: _conversationHistory,
        currentEmotion: _currentMood,
        adherenceScore: _adherenceScore,
        systemPrompt: '''You are Serenity, a compassionate AI Health Assistant for the Adhesion mental health app.

Your ONLY purpose is to help with:
- Mental health support and emotional wellness
- Medication adherence and treatment motivation
- Stress management and coping strategies
- Healthy lifestyle habits
- General health and wellness advice

STRICT RULES:
1. ONLY discuss health, wellness, mental health, and medication topics
2. If asked about sports, entertainment, politics, technology, or ANY non-health topic, politely redirect: "I'm specialized in health and wellness support. How can I help you with your mental health or medication journey today?"
3. Never provide medical diagnoses - encourage consulting healthcare providers
4. Keep responses warm, supportive, and under 100 words
5. Ask follow-up questions to understand their needs better
6. Do not use asterisks or special formatting

Current user adherence score: ${_adherenceScore != null ? '${(_adherenceScore! * 100).toStringAsFixed(0)}%' : 'Unknown'}''',
      );

      final assistantMessage = ChatMessage(
        role: 'assistant',
        content: _cleanMarkdown(response),
        timestamp: DateTime.now(),
      );

      setState(() {
        _messages.add(assistantMessage);
        _conversationHistory.add(ChatMessageData(
          role: 'assistant',
          content: response,
        ));
      });

      _scrollToBottom();
    } catch (e) {
      String errorMessage = e.toString();
      if (errorMessage.contains('404') || errorMessage.contains('No psychological profile')) {
        errorMessage = 'Complete a psychological test first to receive personalized support.';
      } else if (errorMessage.contains('Connection')) {
        errorMessage = 'Could not connect to server. Please try again.';
      }
      setState(() => _error = errorMessage);
    } finally {
      setState(() => _loading = false);
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Color(0xFF1A1A2E),
              Color(0xFF16213E),
              Color(0xFF0F3460),
            ],
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              _buildAppBar(),
              Expanded(
                child: FadeTransition(
                  opacity: _fadeAnimation,
                  child: _messages.isEmpty ? _buildInitialView() : _buildChatView(),
                ),
              ),
              _buildMessageInput(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
      child: Row(
        children: [
          IconButton(
            icon: Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.1),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: Colors.white.withOpacity(0.1)),
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
            child: Column(
              children: [
                Text(
                  'Serenity',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 24,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 1,
                  ),
                ),
                Text(
                  'Your Mental Health Companion',
                  style: TextStyle(
                    color: Color(0xFF4FD1C5),
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            icon: Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.1),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: Colors.white.withOpacity(0.1)),
              ),
              child: const Icon(
                Icons.psychology_alt_rounded,
                color: Color(0xFF4FD1C5),
                size: 20,
              ),
            ),
            onPressed: () => _showInfoSheet(),
          ),
        ],
      ),
    );
  }

  Widget _buildInitialView() {
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          const SizedBox(height: 40),
          // Animated Logo
          ScaleTransition(
            scale: _pulseAnimation,
            child: Container(
              padding: const EdgeInsets.all(36),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    const Color(0xFF4FD1C5).withOpacity(0.3),
                    const Color(0xFF38B2AC).withOpacity(0.1),
                  ],
                ),
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF4FD1C5).withOpacity(0.3),
                    blurRadius: 40,
                    spreadRadius: 10,
                  ),
                ],
              ),
              child: Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: const Color(0xFF4FD1C5).withOpacity(0.2),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.spa_rounded,
                  size: 64,
                  color: Color(0xFF4FD1C5),
                ),
              ),
            ),
          ),
          const SizedBox(height: 40),
          const Text(
            'Hello, I\'m Serenity',
            style: TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.bold,
              color: Colors.white,
              letterSpacing: 0.5,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            'Your AI-powered companion for mental wellness.\nI\'m here to listen, support, and guide you.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 16,
              color: Colors.white.withOpacity(0.7),
              height: 1.6,
            ),
          ),
          const SizedBox(height: 48),
          // Conversation Starters
          _buildConversationStarters(),
          const SizedBox(height: 32),
          if (_loading) _buildLoadingIndicator(),
          if (_error != null) _buildErrorCard(),
          const SizedBox(height: 100),
        ],
      ),
    );
  }

  Widget _buildConversationStarters() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: const Color(0xFF4FD1C5).withOpacity(0.2),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.lightbulb_rounded,
                color: Color(0xFF4FD1C5),
                size: 18,
              ),
            ),
            const SizedBox(width: 12),
            const Text(
              'Start a conversation',
              style: TextStyle(
                color: Colors.white,
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: _conversationStarters.map((text) {
            return _buildStarterChip(text);
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildStarterChip(String text) {
    return GestureDetector(
      onTap: () => _sendMessage(preset: text),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.08),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: const Color(0xFF4FD1C5).withOpacity(0.3),
          ),
        ),
        child: Text(
          text,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }

  Widget _buildLoadingIndicator() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white.withOpacity(0.1)),
      ),
      child: Column(
        children: [
          SizedBox(
            width: 50,
            height: 50,
            child: CircularProgressIndicator(
              strokeWidth: 3,
              valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF4FD1C5)),
              backgroundColor: Colors.white.withOpacity(0.1),
            ),
          ),
          const SizedBox(height: 20),
          const Text(
            'Serenity is thinking...',
            style: TextStyle(
              color: Colors.white,
              fontSize: 16,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFFC8181).withOpacity(0.1),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFFC8181).withOpacity(0.3)),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.info_outline_rounded,
            color: Color(0xFFFC8181),
            size: 24,
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Text(
              _error!,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 14,
                height: 1.4,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildChatView() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withOpacity(0.1)),
      ),
      child: Column(
        children: [
          // Chat header
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  const Color(0xFF4FD1C5).withOpacity(0.3),
                  const Color(0xFF38B2AC).withOpacity(0.2),
                ],
              ),
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(24),
                topRight: Radius.circular(24),
              ),
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Icon(
                    Icons.spa_rounded,
                    color: Colors.white,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 14),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Serenity',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      Text(
                        'Always here for you',
                        style: TextStyle(
                          color: Colors.white70,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                  decoration: BoxDecoration(
                    color: const Color(0xFF4FD1C5),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: const Row(
                    children: [
                      Icon(Icons.circle, size: 8, color: Colors.white),
                      SizedBox(width: 6),
                      Text(
                        'Online',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          // Messages
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.all(16),
              itemCount: _messages.length + (_loading ? 1 : 0),
              itemBuilder: (context, index) {
                if (index < _messages.length) {
                  return _buildMessageBubble(_messages[index]);
                } else {
                  return _buildTypingIndicator();
                }
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessageBubble(ChatMessage message) {
    final isUser = message.role == 'user';
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          if (!isUser) ...[
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF4FD1C5), Color(0xFF38B2AC)],
                ),
                borderRadius: BorderRadius.circular(14),
              ),
              child: const Icon(
                Icons.spa_rounded,
                color: Colors.white,
                size: 18,
              ),
            ),
            const SizedBox(width: 10),
          ],
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
              decoration: BoxDecoration(
                gradient: isUser
                    ? const LinearGradient(
                        colors: [Color(0xFF4FD1C5), Color(0xFF38B2AC)],
                      )
                    : null,
                color: isUser ? null : Colors.white.withOpacity(0.1),
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(20),
                  topRight: const Radius.circular(20),
                  bottomLeft: Radius.circular(isUser ? 20 : 4),
                  bottomRight: Radius.circular(isUser ? 4 : 20),
                ),
                border: isUser ? null : Border.all(
                  color: Colors.white.withOpacity(0.1),
                ),
              ),
              child: Text(
                message.content,
                style: TextStyle(
                  color: isUser ? Colors.white : Colors.white.withOpacity(0.9),
                  fontSize: 15,
                  height: 1.5,
                ),
              ),
            ),
          ),
          if (isUser) ...[
            const SizedBox(width: 10),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                borderRadius: BorderRadius.circular(14),
              ),
              child: const Icon(
                Icons.person_rounded,
                color: Colors.white,
                size: 18,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildTypingIndicator() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF4FD1C5), Color(0xFF38B2AC)],
              ),
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Icon(
              Icons.spa_rounded,
              color: Colors.white,
              size: 18,
            ),
          ),
          const SizedBox(width: 10),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.1),
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(4),
                topRight: Radius.circular(20),
                bottomLeft: Radius.circular(20),
                bottomRight: Radius.circular(20),
              ),
              border: Border.all(color: Colors.white.withOpacity(0.1)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                _buildTypingDot(0),
                const SizedBox(width: 4),
                _buildTypingDot(150),
                const SizedBox(width: 4),
                _buildTypingDot(300),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTypingDot(int delay) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: Duration(milliseconds: 600 + delay),
      builder: (context, value, child) {
        return Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(
            color: const Color(0xFF4FD1C5).withOpacity(0.4 + (value * 0.6)),
            shape: BoxShape.circle,
          ),
        );
      },
    );
  }

  Widget _buildMessageInput() {
    return Container(
      margin: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: const Color(0xFFE5E7EB)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _questionController,
              style: const TextStyle(color: Color(0xFF1F2937), fontSize: 16),
              decoration: InputDecoration(
                hintText: 'Share what\'s on your mind...',
                hintStyle: TextStyle(color: Colors.grey.shade500),
                border: InputBorder.none,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 18,
                ),
              ),
              maxLines: 3,
              minLines: 1,
              enabled: !_loading,
              onSubmitted: _loading ? null : (_) => _sendMessage(),
            ),
          ),
          Container(
            margin: const EdgeInsets.all(8),
            child: Material(
              color: _loading ? Colors.grey : const Color(0xFF4FD1C5),
              borderRadius: BorderRadius.circular(20),
              child: InkWell(
                onTap: _loading ? null : () {
                  if (_questionController.text.trim().isNotEmpty) {
                    _sendMessage();
                  }
                },
                borderRadius: BorderRadius.circular(20),
                child: Container(
                  padding: const EdgeInsets.all(14),
                  child: Icon(
                    _loading ? Icons.hourglass_empty : Icons.send_rounded,
                    color: Colors.white,
                    size: 22,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showInfoSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (context) => Container(
        height: MediaQuery.of(context).size.height * 0.6,
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF1A1A2E), Color(0xFF16213E)],
          ),
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        ),
        child: Column(
          children: [
            const SizedBox(height: 12),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.3),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            gradient: const LinearGradient(
                              colors: [Color(0xFF4FD1C5), Color(0xFF38B2AC)],
                            ),
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: const Icon(
                            Icons.spa_rounded,
                            color: Colors.white,
                            size: 28,
                          ),
                        ),
                        const SizedBox(width: 16),
                        const Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'About Serenity',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontSize: 22,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              Text(
                                'AI Mental Health Companion',
                                style: TextStyle(
                                  color: Color(0xFF4FD1C5),
                                  fontSize: 14,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 28),
                    _buildInfoItem(
                      Icons.chat_bubble_rounded,
                      'Therapeutic Conversations',
                      'Engage in supportive dialogue tailored to your emotional needs.',
                    ),
                    _buildInfoItem(
                      Icons.medication_rounded,
                      'Adherence Support',
                      'Get personalized tips to help maintain your treatment routine.',
                    ),
                    _buildInfoItem(
                      Icons.psychology_rounded,
                      'Emotional Intelligence',
                      'AI that understands and responds to your emotional state.',
                    ),
                    _buildInfoItem(
                      Icons.privacy_tip_rounded,
                      'Your Privacy',
                      'Your conversations are private and used only to provide support.',
                    ),
                    const SizedBox(height: 20),
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFBD38D).withOpacity(0.1),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: const Color(0xFFFBD38D).withOpacity(0.3),
                        ),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.info_outline_rounded,
                            color: Color(0xFFFBD38D),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Text(
                              'Serenity is not a replacement for professional mental health care. Please seek professional help if needed.',
                              style: TextStyle(
                                color: Colors.white.withOpacity(0.8),
                                fontSize: 13,
                                height: 1.4,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoItem(IconData icon, String title, String description) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: const Color(0xFF4FD1C5).withOpacity(0.15),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: const Color(0xFF4FD1C5), size: 22),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.6),
                    fontSize: 14,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
