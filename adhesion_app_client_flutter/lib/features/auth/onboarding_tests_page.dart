import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/user_status.dart';
import '../../core/models/test_models.dart';
import '../../core/models/profile_models.dart';
import '../dashboard/dashboard_page.dart';

/// Onboarding page that requires users to complete psychological tests
/// before accessing the full app.
class OnboardingTestsPage extends StatefulWidget {
  final String baseUrl;
  final int userId;
  final String displayName;
  final UserStatus status;

  const OnboardingTestsPage({
    super.key,
    required this.baseUrl,
    required this.userId,
    required this.displayName,
    required this.status,
  });

  @override
  State<OnboardingTestsPage> createState() => _OnboardingTestsPageState();
}

class _OnboardingTestsPageState extends State<OnboardingTestsPage> {
  late final ApiClient _api;
  List<TestDto> _availableTests = [];
  Set<String> _completedTestCodes = {};
  List<int> _completedSessionIds = []; // Track session IDs for profile creation
  bool _loading = true;
  int _testsCompleted = 0;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _testsCompleted = widget.status.completedTestsCount;
    _loadTests();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _loadTests() async {
    try {
      final tests = await _api.getTests();
      // Get user's test history to see what they've already completed
      final history = await _api.getTestHistory(widget.userId);
      final completedCodes = history.map((r) => r.testCode).toSet();

      if (!mounted) return;
      setState(() {
        _availableTests = tests;
        _completedTestCodes = completedCodes;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Failed to load tests: $e')));
    }
  }

  Future<void> _startTest(TestDto test) async {
    try {
      // Start a session
      final session = await _api.startTestSession(test.id!, widget.userId);

      if (!mounted) return;

      // Navigate to test taking page
      final result = await Navigator.of(context).push<bool>(
        MaterialPageRoute(
          builder: (_) => _TestTakingPage(
            baseUrl: widget.baseUrl,
            session: session,
            testTitle: test.title ?? 'Test',
          ),
        ),
      );

      if (result == true) {
        // Test completed successfully - track session ID for profile creation
        setState(() {
          _testsCompleted++;
          _completedTestCodes.add(test.code ?? '');
          _completedSessionIds.add(session.id);
        });

        // Check if onboarding is now complete
        if (_testsCompleted >= widget.status.requiredTestsCount) {
          await _createProfileAndShowCompletion();
        }
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Failed to start test: $e')));
    }
  }

  /// Create psychological profile from completed test sessions
  Future<void> _createProfileAndShowCompletion() async {
    // Show loading
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => const AlertDialog(
        content: Row(
          children: [
            CircularProgressIndicator(),
            SizedBox(width: 16),
            Text('Creating your profile...'),
          ],
        ),
      ),
    );

    try {
      // Create the psychological profile using completed session IDs
      final request = ProfileCreateRequest(
        userId: widget.userId,
        sessionIds: _completedSessionIds,
      );
      await _api.createProfile(request);

      if (!mounted) return;

      // Close loading dialog
      Navigator.of(context).pop();

      // Show success
      _showCompletionDialog();
    } catch (e) {
      if (!mounted) return;

      // Close loading dialog
      Navigator.of(context).pop();

      // Show error but still allow to continue
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Profile creation had an issue: $e'),
          backgroundColor: Colors.orange,
        ),
      );

      // Still show completion dialog - user can proceed
      _showCompletionDialog();
    }
  }

  void _showCompletionDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        title: const Row(
          children: [
            Icon(Icons.check_circle, color: Colors.green, size: 28),
            SizedBox(width: 8),
            Text('Profile Created!'),
          ],
        ),
        content: const Text(
          'Congratulations! You\'ve completed the required assessments. '
          'Your psychological profile has been created and you now have '
          'full access to all features of the app.',
        ),
        actions: [
          FilledButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              Navigator.of(context).pushReplacement(
                MaterialPageRoute(
                  builder: (_) => DashboardPage(
                    baseUrl: widget.baseUrl,
                    userName: widget.displayName,
                  ),
                ),
              );
            },
            child: const Text('Continue to Dashboard'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isRetake = widget.status.needsRetake;
    final remaining = widget.status.requiredTestsCount - _testsCompleted;

    return Scaffold(
      appBar: AppBar(
        title: Text(isRetake ? 'Reassessment Required' : 'Welcome Assessment'),
        automaticallyImplyLeading: false,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Welcome card
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              CircleAvatar(
                                backgroundColor: Colors.blue.shade100,
                                child: Text(
                                  widget.displayName.isNotEmpty
                                      ? widget.displayName[0].toUpperCase()
                                      : 'U',
                                  style: TextStyle(
                                    color: Colors.blue.shade700,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Hello, ${widget.displayName}!',
                                      style: const TextStyle(
                                        fontSize: 18,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                    Text(
                                      isRetake
                                          ? 'Time for your periodic reassessment'
                                          : 'Let\'s set up your profile',
                                      style: TextStyle(color: Colors.grey[600]),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),
                          if (isRetake)
                            Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: Colors.orange.shade50,
                                borderRadius: BorderRadius.circular(8),
                                border: Border.all(
                                  color: Colors.orange.shade200,
                                ),
                              ),
                              child: Row(
                                children: [
                                  Icon(
                                    Icons.refresh,
                                    color: Colors.orange.shade700,
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      'It\'s been 15 days since your last assessment. '
                                      'Please retake the tests to keep your profile updated.',
                                      style: TextStyle(
                                        color: Colors.orange.shade800,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            )
                          else
                            Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: Colors.blue.shade50,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Row(
                                children: [
                                  Icon(
                                    Icons.info_outline,
                                    color: Colors.blue.shade700,
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      'Complete ${widget.status.requiredTestsCount} psychological tests '
                                      'to build your personalized profile and unlock all app features.',
                                      style: TextStyle(
                                        color: Colors.blue.shade800,
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
                  const SizedBox(height: 16),

                  // Progress indicator
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text(
                                'Progress',
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 16,
                                ),
                              ),
                              Text(
                                '$_testsCompleted / ${widget.status.requiredTestsCount} tests',
                                style: TextStyle(
                                  color: Colors.grey[600],
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          LinearProgressIndicator(
                            value:
                                _testsCompleted /
                                widget.status.requiredTestsCount,
                            backgroundColor: Colors.grey.shade200,
                            minHeight: 8,
                            borderRadius: BorderRadius.circular(4),
                          ),
                          if (remaining > 0) ...[
                            const SizedBox(height: 8),
                            Text(
                              '$remaining more test${remaining > 1 ? 's' : ''} to go!',
                              style: TextStyle(color: Colors.grey[600]),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Available tests
                  const Text(
                    'Available Tests',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  ..._availableTests.map((test) {
                    final isCompleted = _completedTestCodes.contains(test.code);
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: isCompleted
                              ? Colors.green.shade100
                              : Colors.purple.shade100,
                          child: Icon(
                            isCompleted ? Icons.check : Icons.psychology,
                            color: isCompleted
                                ? Colors.green.shade700
                                : Colors.purple.shade700,
                          ),
                        ),
                        title: Text(
                          test.title ?? 'Test',
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                        subtitle: Text(
                          isCompleted ? 'Completed ✓' : 'Tap to start',
                          style: TextStyle(
                            color: isCompleted
                                ? Colors.green
                                : Colors.grey[600],
                          ),
                        ),
                        trailing: isCompleted
                            ? const Icon(
                                Icons.check_circle,
                                color: Colors.green,
                              )
                            : const Icon(Icons.arrow_forward_ios, size: 16),
                        onTap: isCompleted ? null : () => _startTest(test),
                      ),
                    );
                  }),
                ],
              ),
            ),
    );
  }
}

/// Page for taking a single test
class _TestTakingPage extends StatefulWidget {
  final String baseUrl;
  final TestSessionDto session;
  final String testTitle;

  const _TestTakingPage({
    required this.baseUrl,
    required this.session,
    required this.testTitle,
  });

  @override
  State<_TestTakingPage> createState() => _TestTakingPageState();
}

class _TestTakingPageState extends State<_TestTakingPage> {
  late final ApiClient _api;
  final Map<int, int> _answers = {};
  int _currentQuestion = 0;
  bool _submitting = false;

  List<QuestionDto> get questions => widget.session.questions;

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  void _selectAnswer(int value) {
    final q = questions[_currentQuestion];
    setState(() {
      _answers[q.id] = value;
    });
  }

  void _nextQuestion() {
    if (_currentQuestion < questions.length - 1) {
      setState(() => _currentQuestion++);
    }
  }

  void _previousQuestion() {
    if (_currentQuestion > 0) {
      setState(() => _currentQuestion--);
    }
  }

  Future<void> _submitTest() async {
    if (_answers.length < questions.length) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please answer all questions')),
      );
      return;
    }

    setState(() => _submitting = true);

    try {
      final submissions = _answers.entries
          .map((e) => AnswerSubmission(questionId: e.key, score: e.value))
          .toList();

      await _api.submitTestAnswers(widget.session.id, submissions);

      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Test completed successfully!'),
          backgroundColor: Colors.green,
        ),
      );

      Navigator.of(context).pop(true); // Return success
    } catch (e) {
      if (!mounted) return;
      setState(() => _submitting = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Failed to submit: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final q = questions[_currentQuestion];
    final selectedValue = _answers[q.id];
    final isLastQuestion = _currentQuestion == questions.length - 1;
    final allAnswered = _answers.length == questions.length;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.testTitle),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () {
            showDialog(
              context: context,
              builder: (ctx) => AlertDialog(
                title: const Text('Cancel Test?'),
                content: const Text(
                  'Your progress will be lost. Are you sure?',
                ),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.pop(ctx),
                    child: const Text('Continue Test'),
                  ),
                  TextButton(
                    onPressed: () {
                      Navigator.pop(ctx);
                      Navigator.pop(context, false);
                    },
                    child: const Text('Cancel'),
                  ),
                ],
              ),
            );
          },
        ),
      ),
      body: Column(
        children: [
          // Progress bar
          LinearProgressIndicator(
            value: (_currentQuestion + 1) / questions.length,
            minHeight: 4,
          ),
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Question number
                  Text(
                    'Question ${_currentQuestion + 1} of ${questions.length}',
                    style: TextStyle(
                      color: Colors.grey[600],
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Question text
                  Text(
                    q.text,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Answer options
                  ...List.generate(q.maxScore - q.minScore + 1, (i) {
                    final value = q.minScore + i;
                    final isSelected = selectedValue == value;
                    String label;
                    if (q.maxScore == 1) {
                      label = value == 0 ? 'No' : 'Yes';
                    } else if (q.maxScore == 4) {
                      const labels = [
                        'Never',
                        'Rarely',
                        'Sometimes',
                        'Often',
                        'Always',
                      ];
                      label = labels[value];
                    } else {
                      label = value.toString();
                    }

                    return Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: InkWell(
                        onTap: () => _selectAnswer(value),
                        borderRadius: BorderRadius.circular(12),
                        child: Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: isSelected
                                ? Colors.blue.shade50
                                : Colors.grey.shade100,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                              color: isSelected
                                  ? Colors.blue.shade400
                                  : Colors.grey.shade300,
                              width: isSelected ? 2 : 1,
                            ),
                          ),
                          child: Row(
                            children: [
                              Icon(
                                isSelected
                                    ? Icons.radio_button_checked
                                    : Icons.radio_button_off,
                                color: isSelected ? Colors.blue : Colors.grey,
                              ),
                              const SizedBox(width: 12),
                              Text(
                                label,
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: isSelected
                                      ? FontWeight.w600
                                      : FontWeight.normal,
                                  color: isSelected
                                      ? Colors.blue.shade700
                                      : Colors.black87,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    );
                  }),
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
                  offset: const Offset(0, -2),
                ),
              ],
            ),
            child: Row(
              children: [
                if (_currentQuestion > 0)
                  Expanded(
                    child: OutlinedButton(
                      onPressed: _previousQuestion,
                      child: const Text('Previous'),
                    ),
                  ),
                if (_currentQuestion > 0) const SizedBox(width: 12),
                Expanded(
                  flex: 2,
                  child: FilledButton(
                    onPressed: _submitting
                        ? null
                        : (isLastQuestion
                              ? (allAnswered ? _submitTest : null)
                              : (selectedValue != null ? _nextQuestion : null)),
                    child: _submitting
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : Text(isLastQuestion ? 'Submit Test' : 'Next'),
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
