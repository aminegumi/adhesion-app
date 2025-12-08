import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/test_models.dart';

class TakeTestPage extends StatefulWidget {
  final String baseUrl;
  final int testId;
  final String testTitle;

  const TakeTestPage({
    super.key,
    required this.baseUrl,
    required this.testId,
    required this.testTitle,
  });

  @override
  State<TakeTestPage> createState() => _TakeTestPageState();
}

class _TakeTestPageState extends State<TakeTestPage> {
  late final ApiClient _api;
  TestSessionDto? _session;
  bool _loading = true;
  String _error = '';
  final Map<int, int> _answers = {};
  int _currentQuestionIndex = 0;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _startSession();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _startSession() async {
    setState(() {
      _loading = true;
      _error = '';
    });
    try {
      final userId = await ApiClient.getStoredUserId();
      if (userId == null) {
        setState(() => _error = 'User not logged in');
        return;
      }
      final session = await _api.startTestSession(widget.testId, userId);
      setState(() => _session = session);
    } catch (e) {
      setState(() => _error = '$e');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _submitTest() async {
    if (_session == null) return;

    setState(() => _submitting = true);
    try {
      final answers = _answers.entries
          .map((e) => AnswerSubmission(questionId: e.key, score: e.value))
          .toList();

      final result = await _api.submitTestAnswers(_session!.id, answers);

      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => TestResultPage(result: result)),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error submitting: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: Text(widget.testTitle)),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (_error.isNotEmpty) {
      return Scaffold(
        appBar: AppBar(title: Text(widget.testTitle)),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 64, color: Colors.red),
              const SizedBox(height: 16),
              Text(
                'Failed to start test\n$_error',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _startSession,
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      );
    }

    final session = _session!;
    final questions = session.questions;

    if (questions.isEmpty) {
      return Scaffold(
        appBar: AppBar(title: Text(widget.testTitle)),
        body: const Center(child: Text('No questions available for this test')),
      );
    }

    final question = questions[_currentQuestionIndex];
    final isLast = _currentQuestionIndex == questions.length - 1;
    final isFirst = _currentQuestionIndex == 0;
    final allAnswered = _answers.length == questions.length;

    return Scaffold(
      appBar: AppBar(
        title: Text(session.testCode),
        actions: [
          Center(
            child: Padding(
              padding: const EdgeInsets.only(right: 16),
              child: Text(
                '${_currentQuestionIndex + 1}/${questions.length}',
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            // Progress bar
            LinearProgressIndicator(
              value: (_currentQuestionIndex + 1) / questions.length,
              backgroundColor: Colors.grey.shade200,
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Question text
                    Container(
                      padding: const EdgeInsets.all(20),
                      decoration: BoxDecoration(
                        color: Colors.blue.shade50,
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Question ${_currentQuestionIndex + 1}',
                            style: TextStyle(
                              color: Colors.blue.shade700,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const SizedBox(height: 12),
                          Text(
                            question.text,
                            style: const TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 24),
                    // Answer options
                    Text(
                      'Select your answer:',
                      style: TextStyle(
                        color: Colors.grey.shade600,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    const SizedBox(height: 12),
                    ..._buildAnswerOptions(question),
                  ],
                ),
              ),
            ),
            // Navigation buttons
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 10,
                    offset: const Offset(0, -5),
                  ),
                ],
              ),
              child: Row(
                children: [
                  if (!isFirst)
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () {
                          setState(() => _currentQuestionIndex--);
                        },
                        icon: const Icon(Icons.arrow_back),
                        label: const Text('Previous'),
                      ),
                    ),
                  if (!isFirst) const SizedBox(width: 12),
                  Expanded(
                    child: isLast
                        ? ElevatedButton.icon(
                            onPressed: allAnswered && !_submitting
                                ? _submitTest
                                : null,
                            icon: _submitting
                                ? const SizedBox(
                                    width: 20,
                                    height: 20,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                      color: Colors.white,
                                    ),
                                  )
                                : const Icon(Icons.check),
                            label: Text(
                              _submitting ? 'Submitting...' : 'Submit',
                            ),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.green,
                              foregroundColor: Colors.white,
                            ),
                          )
                        : ElevatedButton.icon(
                            onPressed: _answers.containsKey(question.id)
                                ? () {
                                    setState(() => _currentQuestionIndex++);
                                  }
                                : null,
                            icon: const Icon(Icons.arrow_forward),
                            label: const Text('Next'),
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

  List<Widget> _buildAnswerOptions(QuestionDto question) {
    final options = <Widget>[];
    final labels = _getScoreLabels(question);

    for (int score = question.minScore; score <= question.maxScore; score++) {
      final isSelected = _answers[question.id] == score;
      final label = labels[score] ?? 'Score $score';

      options.add(
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Material(
            color: isSelected ? Colors.blue.shade100 : Colors.white,
            borderRadius: BorderRadius.circular(12),
            child: InkWell(
              onTap: () {
                setState(() => _answers[question.id] = score);
              },
              borderRadius: BorderRadius.circular(12),
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: isSelected ? Colors.blue : Colors.grey.shade300,
                    width: isSelected ? 2 : 1,
                  ),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 32,
                      height: 32,
                      decoration: BoxDecoration(
                        color: isSelected ? Colors.blue : Colors.grey.shade200,
                        shape: BoxShape.circle,
                      ),
                      child: Center(
                        child: Text(
                          '$score',
                          style: TextStyle(
                            color: isSelected
                                ? Colors.white
                                : Colors.grey.shade700,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        label,
                        style: TextStyle(
                          fontWeight: isSelected
                              ? FontWeight.w600
                              : FontWeight.normal,
                        ),
                      ),
                    ),
                    if (isSelected)
                      const Icon(Icons.check_circle, color: Colors.blue),
                  ],
                ),
              ),
            ),
          ),
        ),
      );
    }

    return options;
  }

  Map<int, String> _getScoreLabels(QuestionDto question) {
    // For PHQ-9 and GAD-7 (0-3 scale)
    if (question.maxScore == 3 && question.minScore == 0) {
      return {
        0: 'Not at all',
        1: 'Several days',
        2: 'More than half the days',
        3: 'Nearly every day',
      };
    }
    // For BMQ (1-5 Likert scale)
    if (question.maxScore == 5 && question.minScore == 1) {
      return {
        1: 'Strongly disagree',
        2: 'Disagree',
        3: 'Uncertain',
        4: 'Agree',
        5: 'Strongly agree',
      };
    }
    // For MMAS-8 binary questions (0-1)
    if (question.maxScore == 1 && question.minScore == 0) {
      return {0: 'No', 1: 'Yes'};
    }
    // For MMAS-8 last question (0-4)
    if (question.maxScore == 4 && question.minScore == 0) {
      return {
        0: 'Never/Rarely',
        1: 'Once in a while',
        2: 'Sometimes',
        3: 'Usually',
        4: 'All the time',
      };
    }
    // Default labels
    return {};
  }
}

class TestResultPage extends StatelessWidget {
  final TestResultDto result;

  const TestResultPage({super.key, required this.result});

  @override
  Widget build(BuildContext context) {
    final levelColor = _getLevelColor(result.interpretationLevel);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Test Results'),
        automaticallyImplyLeading: false,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Success icon
              const Icon(Icons.check_circle, size: 80, color: Colors.green),
              const SizedBox(height: 16),
              Text(
                'Test Completed!',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                result.testTitle,
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey.shade600),
              ),
              const SizedBox(height: 32),

              // Score card
              Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [levelColor.withOpacity(0.8), levelColor],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Column(
                  children: [
                    const Text(
                      'Your Score',
                      style: TextStyle(color: Colors.white70, fontSize: 16),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '${result.totalScore}',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 56,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 8,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        result.interpretationLevel,
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),

              // Score description
              _buildSection(
                'What This Means',
                result.scoreDescription,
                Icons.info_outline,
              ),
              const SizedBox(height: 16),

              // Clinical interpretation
              _buildSection(
                'Clinical Interpretation',
                result.clinicalInterpretation,
                Icons.psychology,
              ),
              const SizedBox(height: 16),

              // Recommendations
              if (result.recommendations.isNotEmpty) ...[
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.blue.shade50,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(
                            Icons.lightbulb_outline,
                            color: Colors.blue.shade700,
                          ),
                          const SizedBox(width: 8),
                          Text(
                            'Recommendations',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              color: Colors.blue.shade700,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      ...result.recommendations.map(
                        (rec) => Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                '• ',
                                style: TextStyle(fontWeight: FontWeight.bold),
                              ),
                              Expanded(child: Text(rec)),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),
              ],

              // Done button
              ElevatedButton.icon(
                onPressed: () {
                  Navigator.of(context).popUntil((route) => route.isFirst);
                },
                icon: const Icon(Icons.home),
                label: const Text('Back to Dashboard'),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.all(16),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSection(String title, String content, IconData icon) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: Colors.grey.shade600),
              const SizedBox(width: 8),
              Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
            ],
          ),
          const SizedBox(height: 12),
          Text(content),
        ],
      ),
    );
  }

  Color _getLevelColor(String level) {
    final lowerLevel = level.toLowerCase();
    if (lowerLevel.contains('minimal') ||
        lowerLevel.contains('high adherence') ||
        lowerLevel.contains('low')) {
      return Colors.green;
    }
    if (lowerLevel.contains('mild') || lowerLevel.contains('medium')) {
      return Colors.orange;
    }
    if (lowerLevel.contains('moderate')) {
      return Colors.deepOrange;
    }
    if (lowerLevel.contains('severe') || lowerLevel.contains('low adherence')) {
      return Colors.red;
    }
    return Colors.blue;
  }
}
