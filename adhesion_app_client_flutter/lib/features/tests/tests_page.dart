import 'package:flutter/material.dart';
import '../../core/api_client.dart';
import '../../core/models/test_models.dart';
import 'take_test_page.dart';

class TestsPage extends StatefulWidget {
  final String baseUrl;
  const TestsPage({super.key, required this.baseUrl});

  @override
  State<TestsPage> createState() => _TestsPageState();
}

class _TestsPageState extends State<TestsPage> {
  late final ApiClient _api;
  final _searchCtrl = TextEditingController();
  List<TestDto> _all = [];
  List<TestDto> _filtered = [];
  bool _loading = true;
  String _error = '';

  @override
  void initState() {
    super.initState();
    _api = ApiClient(baseUrl: widget.baseUrl);
    _load();
    _searchCtrl.addListener(_applyFilter);
  }

  @override
  void dispose() {
    _api.close();
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = '';
    });
    try {
      final tests = await _api.getTests();
      setState(() {
        _all = tests;
        _filtered = tests;
      });
    } catch (e) {
      setState(() => _error = '$e');
    } finally {
      setState(() => _loading = false);
    }
  }

  void _applyFilter() {
    final q = _searchCtrl.text.toLowerCase();
    setState(() {
      _filtered = _all.where((t) {
        final title = t.title?.toLowerCase() ?? '';
        final code = t.code?.toLowerCase() ?? '';
        return title.contains(q) || code.contains(q);
      }).toList();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      bottomNavigationBar: NavigationBar(
        selectedIndex: 1,
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), label: 'Home'),
          NavigationDestination(
            icon: Icon(Icons.science_outlined),
            label: 'Tests',
          ),
          NavigationDestination(icon: Icon(Icons.history), label: 'History'),
          NavigationDestination(
            icon: Icon(Icons.insights),
            label: 'Predictions',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            label: 'Profile',
          ),
        ],
        onDestinationSelected: (i) {
          // TODO: navigate between tabs
          if (i == 0) {
            Navigator.of(context).pop(); // simplistic: go back to Dashboard
          }
        },
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _load,
          child: CustomScrollView(
            slivers: [
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Expanded(
                            child: Text(
                              'Available Tests',
                              style: TextStyle(
                                fontSize: 22,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                          IconButton(
                            icon: const Icon(Icons.filter_list),
                            onPressed: () {
                              // Optional: filter by active/inactive
                              showModalBottomSheet(
                                context: context,
                                builder: (_) => _FilterSheet(
                                  onSelect: (onlyActive) {
                                    setState(() {
                                      _filtered = onlyActive
                                          ? _all
                                                .where((t) => t.active == true)
                                                .toList()
                                          : _all;
                                    });
                                  },
                                ),
                              );
                            },
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      TextField(
                        controller: _searchCtrl,
                        decoration: InputDecoration(
                          hintText: 'Search tests',
                          prefixIcon: const Icon(Icons.search),
                          filled: true,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              if (_loading)
                const SliverFillRemaining(
                  hasScrollBody: false,
                  child: Center(child: CircularProgressIndicator()),
                )
              else if (_error.isNotEmpty)
                SliverFillRemaining(
                  hasScrollBody: false,
                  child: Center(child: Text('Failed to load tests\n$_error')),
                )
              else if (_filtered.isEmpty)
                const SliverFillRemaining(
                  hasScrollBody: false,
                  child: Center(child: Text('No tests found')),
                )
              else
                SliverList.builder(
                  itemCount: _filtered.length,
                  itemBuilder: (context, index) {
                    final t = _filtered[index];
                    return Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 8,
                      ),
                      child: _TestCard(
                        title: t.title ?? 'Untitled',
                        code: t.code ?? '',
                        version: t.version ?? '',
                        active: t.active ?? false,
                        onTap: () {
                          if (t.id != null && (t.active ?? false)) {
                            Navigator.of(context).push(
                              MaterialPageRoute(
                                builder: (_) => TakeTestPage(
                                  baseUrl: widget.baseUrl,
                                  testId: t.id!,
                                  testTitle: t.title ?? 'Test',
                                ),
                              ),
                            );
                          } else if (!(t.active ?? false)) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                content: Text(
                                  'This test is currently inactive',
                                ),
                                backgroundColor: Colors.orange,
                              ),
                            );
                          }
                        },
                      ),
                    );
                  },
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TestCard extends StatelessWidget {
  final String title;
  final String code;
  final String version;
  final bool active;
  final VoidCallback onTap;

  const _TestCard({
    required this.title,
    required this.code,
    required this.version,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      elevation: 0,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            boxShadow: const [
              BoxShadow(
                color: Color(0x11000000),
                blurRadius: 8,
                offset: Offset(0, 4),
              ),
            ],
          ),
          child: Row(
            children: [
              CircleAvatar(
                radius: 22,
                backgroundColor: const Color(0xFFF1F5F9),
                child: Icon(
                  active ? Icons.psychology : Icons.inventory_2_outlined,
                  color: Colors.blueAccent,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Code: $code, Version: $version',
                      style: const TextStyle(color: Color(0xFF6B7280)),
                    ),
                    const SizedBox(height: 6),
                    _StatusBadge(active: active),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final bool active;
  const _StatusBadge({required this.active});

  @override
  Widget build(BuildContext context) {
    final color = active ? Colors.green : Colors.grey;
    final label = active ? 'Active' : 'Inactive';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withOpacity(0.35)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          CircleAvatar(radius: 3, backgroundColor: color),
          const SizedBox(width: 6),
          Text(
            label,
            style: TextStyle(color: color, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }
}

class _FilterSheet extends StatelessWidget {
  final void Function(bool onlyActive) onSelect;
  const _FilterSheet({required this.onSelect});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'Filter',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 10),
            ListTile(
              leading: const Icon(Icons.list),
              title: const Text('All'),
              onTap: () {
                onSelect(false);
                Navigator.pop(context);
              },
            ),
            ListTile(
              leading: const Icon(Icons.check_circle, color: Colors.green),
              title: const Text('Active only'),
              onTap: () {
                onSelect(true);
                Navigator.pop(context);
              },
            ),
          ],
        ),
      ),
    );
  }
}
