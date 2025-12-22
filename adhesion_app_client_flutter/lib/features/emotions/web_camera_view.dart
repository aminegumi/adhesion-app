// ignore_for_file: avoid_web_libraries_in_flutter
import 'package:flutter/material.dart';
import 'dart:html' as html;
import 'dart:ui_web' as ui_web;

/// Web-specific camera view widget that uses HtmlElementView
/// to display MJPEG stream directly in an img element
class WebCameraView extends StatefulWidget {
  final String streamUrl;
  final bool isStreaming;
  
  const WebCameraView({
    super.key,
    required this.streamUrl,
    required this.isStreaming,
  });
  
  @override
  State<WebCameraView> createState() => _WebCameraViewState();
}

class _WebCameraViewState extends State<WebCameraView> {
  late String _viewType;
  bool _registered = false;
  
  @override
  void initState() {
    super.initState();
    _viewType = 'camera-feed-${DateTime.now().millisecondsSinceEpoch}';
    _registerView();
  }
  
  void _registerView() {
    if (_registered) return;
    
    // Register the HTML element factory
    // ignore: undefined_prefixed_name
    ui_web.platformViewRegistry.registerViewFactory(_viewType, (int viewId) {
      final img = html.ImageElement()
        ..src = widget.streamUrl
        ..style.width = '100%'
        ..style.height = '100%'
        ..style.objectFit = 'cover'
        ..style.backgroundColor = 'black';
      return img;
    });
    
    _registered = true;
  }
  
  @override
  Widget build(BuildContext context) {
    if (!widget.isStreaming) {
      return _buildPlaceholder();
    }
    
    return Stack(
      children: [
        // The actual MJPEG stream via HtmlElementView
        HtmlElementView(viewType: _viewType),
        
        // Live indicator overlay
        Positioned(
          top: 12,
          right: 12,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              color: Colors.black.withOpacity(0.6),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: Color(0xFFEF4444),
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 6),
                const Text(
                  'LIVE',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 11,
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
}
