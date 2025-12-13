import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/services/openrouter_service.dart';

class GetMotivationPage extends StatefulWidget {
  final String baseUrl;
  const GetMotivationPage({super.key, required this.baseUrl});

  @override
  State<GetMotivationPage> createState() => _GetMotivationPageState();
}

class _GetMotivationPageState extends State<GetMotivationPage>
    with SingleTickerProviderStateMixin {
  late final ApiClient _api;
  late final OpenRouterService _aiService;
  late AnimationController _animationController;
  late Animation<double> _scaleAnimation;
  
  String? _motivationMessage;
  bool _loading = false;
  String? _error;
  int _currentCategory = 0;
  
  final List<MotivationCategory> _categories = [
    MotivationCategory(
      name: 'General Wellness',
      icon: Icons.favorite_rounded,
      color: const Color(0xFFEF4444),
      prompt: 'mental wellness and self-care',
    ),
    MotivationCategory(
      name: 'Medication Adherence',
      icon: Icons.medication_rounded,
      color: const Color(0xFF10B981),
      prompt: 'staying consistent with medication and treatment',
    ),
    MotivationCategory(
      name: 'Stress Relief',
      icon: Icons.spa_rounded,
      color: const Color(0xFF8B5CF6),
      prompt: 'managing stress and finding inner peace',
    ),
    MotivationCategory(
      name: 'Positive Mindset',
      icon: Icons.psychology_rounded,
      color: const Color(0xFFF59E0B),
      prompt: 'developing a positive mindset and overcoming negative thoughts',
    ),
    MotivationCategory(
      name: 'Daily Motivation',
      icon: Icons.wb_sunny_rounded,
      color: const Color(0xFF0EA5E9),
      prompt: 'starting the day with energy and purpose',
    ),
  ];

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _aiService = OpenRouterService();
    _setupAnimation();
    _getMotivation();
  }

  void _setupAnimation() {
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );
    _scaleAnimation = Tween<double>(begin: 0.8, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.elasticOut),
    );
  }

  @override
  void dispose() {
    _animationController.dispose();
    _aiService.close();
    _api.close();
    super.dispose();
  }

  Future<void> _getMotivation() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final category = _categories[_currentCategory];
      
      // Get user context
      double? adherenceScore;
      try {
        final userId = await ApiClient.getStoredUserId();
        if (userId != null) {
          adherenceScore = await _api.getAdherenceScore(userId);
        }
      } catch (e) {
        // Continue without adherence score
      }

      final response = await _aiService.chat(
        userMessage: 'Give me a short, powerful motivational message about ${category.prompt}. Keep it inspiring, warm, and under 80 words. Do not use asterisks or special formatting. Just provide the motivational message directly.',
        conversationHistory: [],
        systemPrompt: '''You are a compassionate motivational coach focused on mental health and wellness. 
Your only job is to provide brief, powerful motivational messages.
Rules:
- Keep messages under 80 words
- Be warm, genuine, and inspiring
- Focus only on health, wellness, and positive mental state
- Do not use asterisks, bullet points, or special formatting
- Do not ask questions or engage in conversation
- Just provide the motivational message directly''',
        adherenceScore: adherenceScore,
      );

      setState(() => _motivationMessage = response);
      _animationController.forward(from: 0);
    } catch (e) {
      setState(() => _error = 'Could not get motivation. Please try again.');
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final category = _categories[_currentCategory];
    
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              category.color.withOpacity(0.1),
              Colors.white,
              category.color.withOpacity(0.05),
            ],
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              _buildAppBar(category),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      _buildCategorySelector(),
                      const SizedBox(height: 30),
                      _buildMotivationCard(category),
                      const SizedBox(height: 30),
                      _buildRefreshButton(category),
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

  Widget _buildAppBar(MotivationCategory category) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: category.color.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(Icons.arrow_back_ios_new, color: category.color, size: 18),
            ),
          ),
          const Expanded(
            child: Text(
              'Get Motivated',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.w700,
                color: Color(0xFF1F2937),
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildCategorySelector() {
    return SizedBox(
      height: 100,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        itemCount: _categories.length,
        itemBuilder: (context, index) {
          final cat = _categories[index];
          final isSelected = index == _currentCategory;
          return GestureDetector(
            onTap: () {
              setState(() => _currentCategory = index);
              _getMotivation();
            },
            child: Container(
              width: 85,
              margin: const EdgeInsets.only(right: 12),
              decoration: BoxDecoration(
                color: isSelected ? cat.color : Colors.white,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(
                  color: isSelected ? cat.color : const Color(0xFFE5E7EB),
                  width: isSelected ? 2 : 1,
                ),
                boxShadow: isSelected
                    ? [
                        BoxShadow(
                          color: cat.color.withOpacity(0.3),
                          blurRadius: 12,
                          offset: const Offset(0, 4),
                        ),
                      ]
                    : null,
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    cat.icon,
                    color: isSelected ? Colors.white : cat.color,
                    size: 28,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    cat.name.split(' ').first,
                    style: TextStyle(
                      color: isSelected ? Colors.white : const Color(0xFF6B7280),
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildMotivationCard(MotivationCategory category) {
    return ScaleTransition(
      scale: _scaleAnimation,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(28),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [category.color, category.color.withOpacity(0.8)],
          ),
          borderRadius: BorderRadius.circular(28),
          boxShadow: [
            BoxShadow(
              color: category.color.withOpacity(0.4),
              blurRadius: 24,
              offset: const Offset(0, 12),
            ),
          ],
        ),
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                shape: BoxShape.circle,
              ),
              child: Icon(
                category.icon,
                size: 48,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 24),
            if (_loading)
              Column(
                children: [
                  const SizedBox(
                    width: 40,
                    height: 40,
                    child: CircularProgressIndicator(
                      color: Colors.white,
                      strokeWidth: 3,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Finding inspiration...',
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.8),
                      fontSize: 14,
                    ),
                  ),
                ],
              )
            else if (_error != null)
              Column(
                children: [
                  const Icon(Icons.error_outline, color: Colors.white, size: 40),
                  const SizedBox(height: 12),
                  Text(
                    _error!,
                    style: const TextStyle(color: Colors.white),
                    textAlign: TextAlign.center,
                  ),
                ],
              )
            else if (_motivationMessage != null)
              Column(
                children: [
                  const Icon(
                    Icons.format_quote_rounded,
                    color: Colors.white24,
                    size: 40,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    _motivationMessage!,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.w500,
                      height: 1.6,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 12),
                  const Icon(
                    Icons.format_quote_rounded,
                    color: Colors.white24,
                    size: 40,
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildRefreshButton(MotivationCategory category) {
    return ElevatedButton.icon(
      onPressed: _loading ? null : _getMotivation,
      icon: const Icon(Icons.refresh_rounded),
      label: const Text('Get New Motivation'),
      style: ElevatedButton.styleFrom(
        backgroundColor: category.color,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
      ),
    );
  }
}

class MotivationCategory {
  final String name;
  final IconData icon;
  final Color color;
  final String prompt;

  MotivationCategory({
    required this.name,
    required this.icon,
    required this.color,
    required this.prompt,
  });
}

