import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../../core/api_client.dart';

class CameraEmotionPage extends StatefulWidget {
  final String baseUrl;
  const CameraEmotionPage({super.key, required this.baseUrl});

  @override
  State<CameraEmotionPage> createState() => _CameraEmotionPageState();
}

class _CameraEmotionPageState extends State<CameraEmotionPage>
    with SingleTickerProviderStateMixin {
  late final ApiClient _api;
  final List<ChatMessage> _messages = [];
  bool _loading = false;
  String? _error;
  final TextEditingController _questionController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  String _flaskUrl = 'http://localhost:5000'; // Flask app URL
  String _currentEmotion = 'Neutral';
  bool _isConnected = false;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _initializeFlaskConnection();
  }

  @override
  void dispose() {
    _questionController.dispose();
    _scrollController.dispose();
    _api.close();
    super.dispose();
  }

  Future<void> _initializeFlaskConnection() async {
    try {
      // Test connection to Flask app
      final response = await http.get(Uri.parse('$_flaskUrl/'));
      if (response.statusCode == 200) {
        setState(() => _isConnected = true);
        // Initialize the bot with current emotion
        await _initializeBot();
      } else {
        setState(() => _error = 'Could not connect to emotion detection service');
      }
    } catch (e) {
      setState(() => _error = 'Flask service not available: $e');
    }
  }

  Future<void> _initializeBot() async {
    try {
      final response = await http.post(Uri.parse('$_flaskUrl/submit'));
      if (response.statusCode == 200) {
        // Bot initialized successfully
        _addBotMessage('Hello! I\'m Fido, your personal therapist. I can see your emotions through the camera. How are you feeling today?');
      }
    } catch (e) {
      print('Error initializing bot: $e');
    }
  }

  void _addBotMessage(String message) {
    final botMessage = ChatMessage(
      role: 'assistant',
      content: message,
      timestamp: DateTime.now(),
    );
    setState(() => _messages.add(botMessage));
    _scrollToBottom();
  }

  Future<void> _sendMessage() async {
    final question = _questionController.text.trim();
    if (question.isEmpty) {
      setState(() => _error = 'Please enter a message.');
      return;
    }

    // Add user message to chat
    final userMessage = ChatMessage(
      role: 'user',
      content: question,
      timestamp: DateTime.now(),
    );

    setState(() {
      _messages.add(userMessage);
      _loading = true;
      _error = null;
      _questionController.clear();
    });

    // Scroll to bottom
    _scrollToBottom();

    try {
      final response = await http.post(
        Uri.parse('$_flaskUrl/chat'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'message': question}),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final botResponse = data['bot_message'] ?? 'I\'m here to listen.';

        // Add assistant response to chat
        final assistantMessage = ChatMessage(
          role: 'assistant',
          content: botResponse,
          timestamp: DateTime.now(),
        );

        setState(() => _messages.add(assistantMessage));

        // Scroll to bottom after response
        _scrollToBottom();
      } else {
        setState(() => _error = 'Failed to get response from therapist');
      }
    } catch (e) {
      setState(() => _error = 'Connection error: $e');
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
      appBar: AppBar(
        title: const Text('Fido - Your Personal Therapist'),
        backgroundColor: const Color(0xFF667eea),
        actions: [
          Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: _isConnected ? Colors.green.withOpacity(0.2) : Colors.red.withOpacity(0.2),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              children: [
                Icon(
                  _isConnected ? Icons.videocam : Icons.videocam_off,
                  size: 16,
                  color: _isConnected ? Colors.green : Colors.red,
                ),
                const SizedBox(width: 4),
                Text(
                  _isConnected ? 'Connected' : 'Disconnected',
                  style: TextStyle(
                    fontSize: 12,
                    color: _isConnected ? Colors.green : Colors.red,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
      body: _isConnected ? _buildTherapistInterface() : _buildConnectionError(),
    );
  }

  Widget _buildConnectionError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.error_outline,
              size: 80,
              color: Colors.red.shade300,
            ),
            const SizedBox(height: 24),
            const Text(
              'Emotion Detection Service Unavailable',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            Text(
              _error ?? 'Please make sure the Flask emotion detection service is running on localhost:5000',
              style: TextStyle(
                fontSize: 16,
                color: Colors.white.withOpacity(0.8),
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),
            ElevatedButton(
              onPressed: _initializeFlaskConnection,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF667eea),
                padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(25),
                ),
              ),
              child: const Text(
                'Retry Connection',
                style: TextStyle(fontSize: 16),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTherapistInterface() {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF667eea), Color(0xFF764ba2), Color(0xFFf093fb)],
          stops: [0.0, 0.5, 1.0],
        ),
      ),
      child: SafeArea(
        child: Column(
          children: [
            // Video Stream Section
            Container(
              height: 200,
              margin: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.black,
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.3),
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
                    return Center(
                      child: CircularProgressIndicator(
                        value: loadingProgress.expectedTotalBytes != null
                            ? loadingProgress.cumulativeBytesLoaded /
                                loadingProgress.expectedTotalBytes!
                            : null,
                      ),
                    );
                  },
                  errorBuilder: (context, error, stackTrace) {
                    return const Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.videocam_off,
                            size: 48,
                            color: Colors.white54,
                          ),
                          SizedBox(height: 8),
                          Text(
                            'Camera feed unavailable',
                            style: TextStyle(color: Colors.white54),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ),
            ),

            // Chat Section
            Expanded(
              child: Container(
                margin: const EdgeInsets.symmetric(horizontal: 16),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.95),
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.1),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    // Chat header
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [Color(0xFF667eea), Color(0xFF764ba2)],
                        ),
                        borderRadius: const BorderRadius.only(
                          topLeft: Radius.circular(20),
                          topRight: Radius.circular(20),
                        ),
                      ),
                      child: Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(8),
                            decoration: BoxDecoration(
                              color: Colors.white.withOpacity(0.2),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: const Text(
                              '🐼',
                              style: TextStyle(fontSize: 16),
                            ),
                          ),
                          const SizedBox(width: 12),
                          const Expanded(
                            child: Text(
                              'Fido - Your Personal Therapist',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                            decoration: BoxDecoration(
                              color: Colors.white.withOpacity(0.2),
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: const Text(
                              'Online',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 12,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),

                    // Messages list
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
              ),
            ),

            // Message input
            _buildMessageInput(),
          ],
        ),
      ),
    );
  }

  Widget _buildMessageBubble(ChatMessage message) {
    final isUser = message.role == 'user';
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        children: [
          if (!isUser) ...[
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF667eea), Color(0xFF764ba2)],
                ),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Text(
                '🐼',
                style: TextStyle(fontSize: 16),
              ),
            ),
            const SizedBox(width: 8),
          ],
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: isUser
                    ? const Color(0xFF667eea)
                    : Colors.grey.shade100,
                borderRadius: BorderRadius.only(
                  topLeft: Radius.circular(isUser ? 20 : 4),
                  topRight: Radius.circular(isUser ? 4 : 20),
                  bottomLeft: const Radius.circular(20),
                  bottomRight: const Radius.circular(20),
                ),
              ),
              child: Text(
                message.content,
                style: TextStyle(
                  color: isUser ? Colors.white : Colors.black87,
                  fontSize: 16,
                  height: 1.4,
                ),
              ),
            ),
          ),
          if (isUser) ...[
            const SizedBox(width: 8),
            CircleAvatar(
              radius: 16,
              backgroundColor: Colors.white.withOpacity(0.2),
              child: const Icon(
                Icons.person,
                color: Colors.white,
                size: 16,
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
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF667eea), Color(0xFF764ba2)],
              ),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Text(
              '🐼',
              style: TextStyle(fontSize: 16),
            ),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: Colors.grey.shade100,
              borderRadius: const BorderRadius.only(
                topRight: Radius.circular(20),
                bottomLeft: Radius.circular(20),
                bottomRight: Radius.circular(20),
              ),
            ),
            child: Row(
              children: [
                Text(
                  'Fido is thinking...',
                  style: TextStyle(
                    color: Colors.grey.shade600,
                    fontSize: 16,
                  ),
                ),
                const SizedBox(width: 8),
                SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation<Color>(
                      const Color(0xFF667eea).withOpacity(0.6),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessageInput() {
    return Container(
      margin: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(25),
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
              style: const TextStyle(color: Colors.black87, fontSize: 16),
              decoration: InputDecoration(
                hintText: 'Share how you\'re feeling...',
                hintStyle: TextStyle(color: Colors.grey.shade500),
                border: InputBorder.none,
                contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
              ),
              maxLines: 3,
              minLines: 1,
              enabled: !_loading,
              onSubmitted: _loading ? null : (_) => _sendMessage(),
            ),
          ),
          Container(
            margin: const EdgeInsets.all(8),
            child: ElevatedButton(
              onPressed: _loading ? null : () {
                if (_questionController.text.trim().isNotEmpty) {
                  _sendMessage();
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF667eea),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20),
                ),
                padding: const EdgeInsets.all(12),
                elevation: 0,
              ),
              child: Icon(
                _loading ? Icons.hourglass_empty : Icons.send,
                size: 20,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class ChatMessage {
  final String role;
  final String content;
  final DateTime timestamp;

  ChatMessage({
    required this.role,
    required this.content,
    required this.timestamp,
  });
}