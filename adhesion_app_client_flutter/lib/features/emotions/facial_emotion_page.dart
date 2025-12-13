import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:camera/camera.dart';
import 'package:image_picker/image_picker.dart';
import '../../core/api_client.dart';
import '../../core/models/ai_models.dart';

class FacialEmotionPage extends StatefulWidget {
  final String baseUrl;
  const FacialEmotionPage({super.key, required this.baseUrl});

  @override
  State<FacialEmotionPage> createState() => _FacialEmotionPageState();
}

class _FacialEmotionPageState extends State<FacialEmotionPage>
    with WidgetsBindingObserver {
  late final ApiClient _api;
  CameraController? _cameraController;
  List<CameraDescription> _cameras = [];
  bool _isCameraInitialized = false;
  bool _isProcessing = false;
  bool _isFrontCamera = true;
  String? _capturedImagePath;
  FacialEmotionResult? _result;
  String? _error;
  
  final Map<String, EmotionVisual> _emotionVisuals = {
    'happy': EmotionVisual(
      icon: Icons.sentiment_very_satisfied_rounded,
      color: const Color(0xFFFBBF24),
      gradient: [const Color(0xFFFBBF24), const Color(0xFFF59E0B)],
      label: 'Happy',
      description: 'You seem to be in a great mood!',
    ),
    'sad': EmotionVisual(
      icon: Icons.sentiment_dissatisfied_rounded,
      color: const Color(0xFF6366F1),
      gradient: [const Color(0xFF6366F1), const Color(0xFF818CF8)],
      label: 'Sad',
      description: 'It\'s okay to feel down sometimes.',
    ),
    'angry': EmotionVisual(
      icon: Icons.mood_bad_rounded,
      color: const Color(0xFFEF4444),
      gradient: [const Color(0xFFEF4444), const Color(0xFFF87171)],
      label: 'Angry',
      description: 'Take a deep breath and try to relax.',
    ),
    'fearful': EmotionVisual(
      icon: Icons.psychology_rounded,
      color: const Color(0xFFF97316),
      gradient: [const Color(0xFFF97316), const Color(0xFFFB923C)],
      label: 'Fearful',
      description: 'Remember, you are safe and supported.',
    ),
    'surprised': EmotionVisual(
      icon: Icons.face_rounded,
      color: const Color(0xFF8B5CF6),
      gradient: [const Color(0xFF8B5CF6), const Color(0xFFA78BFA)],
      label: 'Surprised',
      description: 'Something unexpected caught your attention!',
    ),
    'disgusted': EmotionVisual(
      icon: Icons.sick_rounded,
      color: const Color(0xFF10B981),
      gradient: [const Color(0xFF10B981), const Color(0xFF34D399)],
      label: 'Disgusted',
      description: 'Something doesn\'t feel right to you.',
    ),
    'neutral': EmotionVisual(
      icon: Icons.sentiment_neutral_rounded,
      color: const Color(0xFF6B7280),
      gradient: [const Color(0xFF6B7280), const Color(0xFF9CA3AF)],
      label: 'Neutral',
      description: 'You seem calm and composed.',
    ),
  };

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _api = ApiClient(baseUrl: widget.baseUrl);
    _initializeCamera();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _cameraController?.dispose();
    _api.close();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (_cameraController == null || !_cameraController!.value.isInitialized) {
      return;
    }

    if (state == AppLifecycleState.inactive) {
      _cameraController?.dispose();
    } else if (state == AppLifecycleState.resumed) {
      _initializeCamera();
    }
  }

  Future<void> _initializeCamera() async {
    // Check if running on web
    if (kIsWeb) {
      setState(() => _error = 'Camera not available on web. Please use the text-based emotion detection instead.');
      return;
    }

    try {
      _cameras = await availableCameras();
      if (_cameras.isEmpty) {
        setState(() => _error = 'No cameras available');
        return;
      }

      final camera = _cameras.firstWhere(
        (cam) => cam.lensDirection ==
            (_isFrontCamera ? CameraLensDirection.front : CameraLensDirection.back),
        orElse: () => _cameras.first,
      );

      _cameraController = CameraController(
        camera,
        ResolutionPreset.medium,
        enableAudio: false,
        imageFormatGroup: ImageFormatGroup.jpeg,
      );

      await _cameraController!.initialize();

      if (mounted) {
        setState(() => _isCameraInitialized = true);
      }
    } catch (e) {
      setState(() => _error = 'Failed to initialize camera: $e');
    }
  }

  Future<void> _switchCamera() async {
    setState(() {
      _isFrontCamera = !_isFrontCamera;
      _isCameraInitialized = false;
    });
    
    await _cameraController?.dispose();
    await _initializeCamera();
  }

  Future<void> _captureAndAnalyze() async {
    if (_cameraController == null || !_cameraController!.value.isInitialized) {
      return;
    }

    setState(() {
      _isProcessing = true;
      _error = null;
    });

    try {
      final XFile image = await _cameraController!.takePicture();
      setState(() => _capturedImagePath = image.path);
      
      await _analyzeImage(File(image.path));
    } catch (e) {
      setState(() {
        _error = 'Failed to capture image: $e';
        _isProcessing = false;
      });
    }
  }

  Future<void> _pickFromGallery() async {
    final picker = ImagePicker();
    final XFile? image = await picker.pickImage(source: ImageSource.gallery);
    
    if (image != null) {
      setState(() {
        _capturedImagePath = image.path;
        _isProcessing = true;
        _error = null;
      });
      
      await _analyzeImage(File(image.path));
    }
  }

  Future<void> _analyzeImage(File imageFile) async {
    try {
      final userId = await ApiClient.getStoredUserId();
      if (userId == null) {
        setState(() {
          _error = 'Please log in first';
          _isProcessing = false;
        });
        return;
      }

      // Convert image to base64
      final bytes = await imageFile.readAsBytes();
      final base64Image = base64Encode(bytes);
      
      // Send to backend for analysis
      final result = await _api.analyzeFacialEmotion(userId, bytes);
      
      setState(() {
        _result = result;
        _isProcessing = false;
      });
    } catch (e) {
      String errorMessage = e.toString();
      if (errorMessage.contains('Connection refused')) {
        errorMessage = 'Could not connect to server.';
      }
      setState(() {
        _error = errorMessage;
        _isProcessing = false;
      });
    }
  }

  void _resetAnalysis() {
    setState(() {
      _result = null;
      _capturedImagePath = null;
      _error = null;
    });
  }

  EmotionVisual _getEmotionVisual(String emotion) {
    return _emotionVisuals[emotion.toLowerCase()] ?? _emotionVisuals['neutral']!;
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
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              _buildAppBar(),
              Expanded(
                child: _result != null
                    ? _buildResultView()
                    : _buildCameraView(),
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
              'Facial Emotion Detection',
              style: TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.w700,
              ),
              textAlign: TextAlign.center,
            ),
          ),
          IconButton(
            icon: Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(
                Icons.info_outline,
                color: Colors.white,
                size: 18,
              ),
            ),
            onPressed: () => _showInfoDialog(),
          ),
        ],
      ),
    );
  }

  Widget _buildCameraView() {
    if (_error != null) {
      return _buildErrorView();
    }

    return Container(
      margin: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: Column(
          children: [
            Expanded(
              child: _capturedImagePath != null
                  ? Stack(
                      fit: StackFit.expand,
                      children: [
                        Image.file(
                          File(_capturedImagePath!),
                          fit: BoxFit.cover,
                        ),
                        if (_isProcessing)
                          Container(
                            color: Colors.black54,
                            child: const Center(
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  CircularProgressIndicator(
                                    valueColor: AlwaysStoppedAnimation<Color>(
                                      Colors.white,
                                    ),
                                  ),
                                  SizedBox(height: 16),
                                  Text(
                                    'Analyzing facial expression...',
                                    style: TextStyle(
                                      color: Colors.white,
                                      fontSize: 16,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                      ],
                    )
                  : _isCameraInitialized
                      ? Stack(
                          fit: StackFit.expand,
                          children: [
                            CameraPreview(_cameraController!),
                            _buildFaceOverlay(),
                          ],
                        )
                      : const Center(
                          child: CircularProgressIndicator(
                            valueColor: AlwaysStoppedAnimation<Color>(
                              Color(0xFF6366F1),
                            ),
                          ),
                        ),
            ),
            _buildCameraControls(),
          ],
        ),
      ),
    );
  }

  Widget _buildFaceOverlay() {
    return Center(
      child: Container(
        width: 220,
        height: 280,
        decoration: BoxDecoration(
          border: Border.all(
            color: Colors.white.withOpacity(0.6),
            width: 3,
          ),
          borderRadius: BorderRadius.circular(110),
        ),
        child: const Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.face_rounded,
                color: Colors.white54,
                size: 40,
              ),
              SizedBox(height: 8),
              Text(
                'Position your face here',
                style: TextStyle(
                  color: Colors.white70,
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCameraControls() {
    if (_capturedImagePath != null && !_isProcessing && _result == null) {
      return Container(
        padding: const EdgeInsets.all(20),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            _buildControlButton(
              icon: Icons.refresh,
              label: 'Retake',
              onTap: _resetAnalysis,
            ),
            _buildControlButton(
              icon: Icons.check_circle,
              label: 'Analyze',
              onTap: () => _analyzeImage(File(_capturedImagePath!)),
              isPrimary: true,
            ),
          ],
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.all(20),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          _buildControlButton(
            icon: Icons.photo_library_rounded,
            label: 'Gallery',
            onTap: _pickFromGallery,
          ),
          _buildCaptureButton(),
          _buildControlButton(
            icon: Icons.flip_camera_ios_rounded,
            label: 'Flip',
            onTap: _switchCamera,
          ),
        ],
      ),
    );
  }

  Widget _buildControlButton({
    required IconData icon,
    required String label,
    required VoidCallback onTap,
    bool isPrimary = false,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              gradient: isPrimary
                  ? const LinearGradient(
                      colors: [Color(0xFF6366F1), Color(0xFF8B5CF6)],
                    )
                  : null,
              color: isPrimary ? null : const Color(0xFFF3F4F6),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Icon(
              icon,
              color: isPrimary ? Colors.white : const Color(0xFF6B7280),
              size: 24,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            label,
            style: TextStyle(
              color: isPrimary ? const Color(0xFF6366F1) : const Color(0xFF6B7280),
              fontSize: 12,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCaptureButton() {
    return GestureDetector(
      onTap: _isProcessing ? null : _captureAndAnalyze,
      child: Container(
        width: 72,
        height: 72,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: const LinearGradient(
            colors: [Color(0xFF6366F1), Color(0xFF8B5CF6)],
          ),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF6366F1).withOpacity(0.4),
              blurRadius: 16,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: Container(
          margin: const EdgeInsets.all(4),
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(color: Colors.white, width: 3),
          ),
          child: const Center(
            child: Icon(
              Icons.camera_alt_rounded,
              color: Colors.white,
              size: 28,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildErrorView() {
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(32),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: Colors.red.shade50,
              borderRadius: BorderRadius.circular(50),
            ),
            child: Icon(
              Icons.error_outline,
              size: 48,
              color: Colors.red.shade400,
            ),
          ),
          const SizedBox(height: 24),
          Text(
            'Oops! Something went wrong',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.grey.shade800,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            _error ?? 'Unknown error',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey.shade600,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 32),
          ElevatedButton.icon(
            onPressed: () {
              setState(() => _error = null);
              _initializeCamera();
            },
            icon: const Icon(Icons.refresh),
            label: const Text('Try Again'),
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF6366F1),
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildResultView() {
    final emotion = _result?.dominantEmotion ?? 'neutral';
    final visual = _getEmotionVisual(emotion);
    
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          // Captured image with result
          Container(
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.15),
                  blurRadius: 20,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(24),
              child: Column(
                children: [
                  // Image with emotion overlay
                  Stack(
                    children: [
                      if (_capturedImagePath != null)
                        AspectRatio(
                          aspectRatio: 3 / 4,
                          child: Image.file(
                            File(_capturedImagePath!),
                            fit: BoxFit.cover,
                          ),
                        ),
                      Positioned(
                        bottom: 16,
                        left: 16,
                        right: 16,
                        child: Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              colors: visual.gradient,
                            ),
                            borderRadius: BorderRadius.circular(16),
                            boxShadow: [
                              BoxShadow(
                                color: visual.color.withOpacity(0.4),
                                blurRadius: 16,
                                offset: const Offset(0, 6),
                              ),
                            ],
                          ),
                          child: Row(
                            children: [
                              Icon(
                                visual.icon,
                                color: Colors.white,
                                size: 36,
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      visual.label,
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontSize: 22,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                    Text(
                                      '${((_result?.confidence ?? 0) * 100).toStringAsFixed(0)}% confidence',
                                      style: TextStyle(
                                        color: Colors.white.withOpacity(0.9),
                                        fontSize: 14,
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
                  // Emotion analysis
                  Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Analysis Result',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF1F2937),
                          ),
                        ),
                        const SizedBox(height: 12),
                        Text(
                          visual.description,
                          style: const TextStyle(
                            fontSize: 15,
                            color: Color(0xFF6B7280),
                            height: 1.5,
                          ),
                        ),
                        const SizedBox(height: 20),
                        // Emotion breakdown
                        if (_result?.emotionScores != null) ...[
                          const Text(
                            'Emotion Breakdown',
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w600,
                              color: Color(0xFF374151),
                            ),
                          ),
                          const SizedBox(height: 12),
                          ..._buildEmotionBars(),
                        ],
                        const SizedBox(height: 20),
                        // AI Insight
                        if (_result?.insight != null)
                          Container(
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              color: const Color(0xFFF3F4F6),
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(
                                    color: Colors.white,
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: const Icon(
                                    Icons.auto_awesome,
                                    color: Color(0xFF6366F1),
                                    size: 20,
                                  ),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      const Text(
                                        'AI Insight',
                                        style: TextStyle(
                                          fontWeight: FontWeight.w600,
                                          color: Color(0xFF374151),
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        _result!.insight!,
                                        style: const TextStyle(
                                          color: Color(0xFF6B7280),
                                          fontSize: 14,
                                          height: 1.4,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),
          // Action buttons
          Row(
            children: [
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: _resetAnalysis,
                  icon: const Icon(Icons.refresh),
                  label: const Text('Try Again'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.white,
                    foregroundColor: const Color(0xFF6366F1),
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.check_circle),
                  label: const Text('Done'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF10B981),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  List<Widget> _buildEmotionBars() {
    final scores = _result?.emotionScores ?? {};
    final sortedEntries = scores.entries.toList()
      ..sort((a, b) => b.value.compareTo(a.value));

    return sortedEntries.take(5).map((entry) {
      final visual = _getEmotionVisual(entry.key);
      final percentage = (entry.value * 100).toStringAsFixed(0);
      
      return Padding(
        padding: const EdgeInsets.only(bottom: 10),
        child: Row(
          children: [
            Icon(visual.icon, color: visual.color, size: 20),
            const SizedBox(width: 8),
            SizedBox(
              width: 70,
              child: Text(
                visual.label,
                style: const TextStyle(
                  fontSize: 13,
                  color: Color(0xFF6B7280),
                ),
              ),
            ),
            Expanded(
              child: Container(
                height: 8,
                decoration: BoxDecoration(
                  color: const Color(0xFFE5E7EB),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: FractionallySizedBox(
                  alignment: Alignment.centerLeft,
                  widthFactor: entry.value,
                  child: Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(colors: visual.gradient),
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 8),
            SizedBox(
              width: 36,
              child: Text(
                '$percentage%',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: visual.color,
                ),
                textAlign: TextAlign.end,
              ),
            ),
          ],
        ),
      );
    }).toList();
  }

  void _showInfoDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: const Color(0xFF6366F1).withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(
                Icons.face_retouching_natural,
                color: Color(0xFF6366F1),
              ),
            ),
            const SizedBox(width: 12),
            const Text('Facial Emotion Detection'),
          ],
        ),
        content: const Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'How it works:',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 8),
            Text('1. Position your face within the frame'),
            Text('2. Take a photo or select from gallery'),
            Text('3. AI analyzes your facial expression'),
            Text('4. Get insights about your emotional state'),
            SizedBox(height: 16),
            Text(
              'This feature uses AI to detect emotions from facial expressions and provides personalized insights to support your wellbeing.',
              style: TextStyle(color: Color(0xFF6B7280), fontSize: 13),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Got it'),
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
  final String description;

  const EmotionVisual({
    required this.icon,
    required this.color,
    required this.gradient,
    required this.label,
    required this.description,
  });
}
