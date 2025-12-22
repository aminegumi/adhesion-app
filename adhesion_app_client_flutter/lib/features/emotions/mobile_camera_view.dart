import 'package:flutter/material.dart';

/// Mobile/Desktop camera view widget - shows placeholder
/// since MJPEG streams don't work with Image.network on mobile
/// The emotion detection still works via polling /emotion endpoint
class MobileCameraView extends StatelessWidget {
  final String streamUrl;
  final bool isStreaming;
  final String currentEmotion;
  final double confidence;
  
  const MobileCameraView({
    super.key,
    required this.streamUrl,
    required this.isStreaming,
    required this.currentEmotion,
    required this.confidence,
  });
  
  @override
  Widget build(BuildContext context) {
    if (!isStreaming) {
      return _buildPlaceholder();
    }
    
    return _buildActiveView();
  }
  
  Widget _buildActiveView() {
    return Container(
      color: Colors.black,
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Animated emotion icon
            TweenAnimationBuilder<double>(
              tween: Tween(begin: 0.9, end: 1.1),
              duration: const Duration(milliseconds: 1000),
              curve: Curves.easeInOut,
              builder: (context, scale, child) {
                return Transform.scale(
                  scale: scale,
                  child: Container(
                    padding: const EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      color: _getEmotionColor().withOpacity(0.2),
                      borderRadius: BorderRadius.circular(50),
                    ),
                    child: Icon(
                      _getEmotionIcon(),
                      size: 48,
                      color: _getEmotionColor(),
                    ),
                  ),
                );
              },
              onEnd: () {},
            ),
            const SizedBox(height: 20),
            const Text(
              'Camera AI Active',
              style: TextStyle(
                color: Colors.white,
                fontSize: 18,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Detecting: ${_capitalize(currentEmotion)}',
              style: TextStyle(color: Colors.white.withOpacity(0.8), fontSize: 16),
            ),
            const SizedBox(height: 4),
            Text(
              'Confidence: ${(confidence * 100).toStringAsFixed(0)}%',
              style: TextStyle(color: Colors.white.withOpacity(0.6), fontSize: 14),
            ),
            const SizedBox(height: 20),
            // Live indicator
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                color: const Color(0xFF10B981).withOpacity(0.2),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TweenAnimationBuilder<double>(
                    tween: Tween(begin: 0.3, end: 1.0),
                    duration: const Duration(milliseconds: 800),
                    builder: (context, value, child) {
                      return Container(
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: Color.lerp(
                            const Color(0xFF10B981).withOpacity(0.3),
                            const Color(0xFF10B981),
                            value,
                          ),
                          shape: BoxShape.circle,
                        ),
                      );
                    },
                    onEnd: () {},
                  ),
                  const SizedBox(width: 8),
                  const Text(
                    'Live Detection',
                    style: TextStyle(
                      color: Color(0xFF10B981),
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildPlaceholder() {
    return Container(
      color: Colors.black,
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.1),
                borderRadius: BorderRadius.circular(50),
              ),
              child: const Icon(Icons.videocam_rounded, size: 44, color: Colors.white70),
            ),
            const SizedBox(height: 20),
            const Text(
              'Camera Ready',
              style: TextStyle(
                color: Colors.white,
                fontSize: 18,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Press "Start Camera" to begin detection',
              style: TextStyle(color: Colors.white.withOpacity(0.6), fontSize: 14),
            ),
          ],
        ),
      ),
    );
  }
  
  IconData _getEmotionIcon() {
    switch (currentEmotion.toLowerCase()) {
      case 'happy':
        return Icons.sentiment_very_satisfied_rounded;
      case 'sad':
        return Icons.sentiment_dissatisfied_rounded;
      case 'angry':
        return Icons.mood_bad_rounded;
      case 'fear':
      case 'fearful':
        return Icons.psychology_rounded;
      case 'surprise':
      case 'surprised':
        return Icons.face_rounded;
      case 'anxious':
        return Icons.self_improvement_rounded;
      default:
        return Icons.sentiment_neutral_rounded;
    }
  }
  
  Color _getEmotionColor() {
    switch (currentEmotion.toLowerCase()) {
      case 'happy':
        return const Color(0xFFFBBF24);
      case 'sad':
        return const Color(0xFF6366F1);
      case 'angry':
        return const Color(0xFFEF4444);
      case 'fear':
      case 'fearful':
        return const Color(0xFFF97316);
      case 'surprise':
      case 'surprised':
        return const Color(0xFF10B981);
      case 'anxious':
        return const Color(0xFF8B5CF6);
      default:
        return const Color(0xFF6B7280);
    }
  }
  
  String _capitalize(String s) {
    if (s.isEmpty) return s;
    return s[0].toUpperCase() + s.substring(1).toLowerCase();
  }
}
