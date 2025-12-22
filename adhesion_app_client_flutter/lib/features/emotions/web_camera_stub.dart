import 'package:flutter/material.dart';

/// Stub for WebCameraView - used on non-web platforms
/// This file is replaced by web_camera_view.dart on web via conditional import
class WebCameraView extends StatelessWidget {
  final String streamUrl;
  final bool isStreaming;
  
  const WebCameraView({
    super.key,
    required this.streamUrl,
    required this.isStreaming,
  });
  
  @override
  Widget build(BuildContext context) {
    // This should never be called on non-web platforms
    // The main file uses MobileCameraView for non-web
    return const SizedBox.shrink();
  }
}
