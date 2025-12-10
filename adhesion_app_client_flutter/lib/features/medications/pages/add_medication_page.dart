import 'package:flutter/material.dart';
import 'package:adhesion_app_client_flutter/core/api_client.dart';
import '../models/user_medication.dart';
import '../services/medication_service.dart';

/// Page to add or edit a medication
class AddMedicationPage extends StatefulWidget {
  final UserMedication? medication;

  const AddMedicationPage({super.key, this.medication});

  @override
  State<AddMedicationPage> createState() => _AddMedicationPageState();
}

class _AddMedicationPageState extends State<AddMedicationPage> {
  final _formKey = GlobalKey<FormState>();
  bool _isLoading = false;

  // Form fields
  late TextEditingController _nameController;
  late TextEditingController _dosageController;
  late TextEditingController _doctorController;
  late TextEditingController _instructionsController;
  late TextEditingController _notesController;
  late TextEditingController _reasonController;
  late TextEditingController _stockController;
  late TextEditingController _thresholdController;

  MedicationForm _selectedForm = MedicationForm.TABLET;
  int _frequencyPerDay = 1;
  List<TimeOfDay> _scheduledTimes = [];
  DateTime? _startDate;
  DateTime? _endDate;
  bool _isChronic = false;
  bool _remindersEnabled = true;
  int _reminderMinutesBefore = 15;
  String _selectedColor = '#2196F3';

  final List<String> _colorOptions = [
    '#2196F3', // Blue
    '#4CAF50', // Green
    '#FF9800', // Orange
    '#E91E63', // Pink
    '#9C27B0', // Purple
    '#00BCD4', // Cyan
    '#F44336', // Red
    '#795548', // Brown
  ];

  bool get _isEditing => widget.medication != null;

  @override
  void initState() {
    super.initState();
    _initializeFields();
  }

  void _initializeFields() {
    final med = widget.medication;

    _nameController = TextEditingController(text: med?.name ?? '');
    _dosageController = TextEditingController(text: med?.dosage ?? '');
    _doctorController = TextEditingController(text: med?.prescribedBy ?? '');
    _instructionsController = TextEditingController(
      text: med?.instructions ?? '',
    );
    _notesController = TextEditingController(text: med?.notes ?? '');
    _reasonController = TextEditingController(text: med?.reason ?? '');
    _stockController = TextEditingController(
      text: med?.currentStock?.toString() ?? '',
    );
    _thresholdController = TextEditingController(
      text: med?.lowStockThreshold?.toString() ?? '',
    );

    if (med != null) {
      _selectedForm = med.form;
      _frequencyPerDay = med.frequencyPerDay;
      _scheduledTimes = med.scheduledTimes.map((t) {
        final parts = t.split(':');
        return TimeOfDay(
          hour: int.parse(parts[0]),
          minute: int.parse(parts[1]),
        );
      }).toList();
      _startDate = med.startDate;
      _endDate = med.endDate;
      _isChronic = med.isChronic;
      _remindersEnabled = med.remindersEnabled;
      _reminderMinutesBefore = med.reminderMinutesBefore;
      _selectedColor = med.color ?? '#2196F3';
    } else {
      _startDate = DateTime.now();
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _dosageController.dispose();
    _doctorController.dispose();
    _instructionsController.dispose();
    _notesController.dispose();
    _reasonController.dispose();
    _stockController.dispose();
    _thresholdController.dispose();
    super.dispose();
  }

  Future<void> _saveMedication() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      final userId = await ApiClient.getStoredUserId();
      if (userId == null) {
        throw Exception('User not logged in');
      }

      // Convert times to strings
      final times = _scheduledTimes
          .map(
            (t) =>
                '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}',
          )
          .toList();

      final request = CreateMedicationRequest(
        userId: userId,
        name: _nameController.text.trim(),
        dosage: _dosageController.text.trim().isEmpty
            ? null
            : _dosageController.text.trim(),
        form: _selectedForm,
        frequencyPerDay: _frequencyPerDay,
        scheduledTimes: times.isEmpty ? null : times,
        prescribedBy: _doctorController.text.trim().isEmpty
            ? null
            : _doctorController.text.trim(),
        instructions: _instructionsController.text.trim().isEmpty
            ? null
            : _instructionsController.text.trim(),
        startDate: _startDate,
        endDate: _isChronic ? null : _endDate,
        isChronic: _isChronic,
        currentStock: _stockController.text.isEmpty
            ? null
            : int.tryParse(_stockController.text),
        lowStockThreshold: _thresholdController.text.isEmpty
            ? null
            : int.tryParse(_thresholdController.text),
        remindersEnabled: _remindersEnabled,
        reminderMinutesBefore: _reminderMinutesBefore,
        notes: _notesController.text.trim().isEmpty
            ? null
            : _notesController.text.trim(),
        reason: _reasonController.text.trim().isEmpty
            ? null
            : _reasonController.text.trim(),
        color: _selectedColor,
      );

      if (_isEditing) {
        await MedicationService.updateMedication(
          widget.medication!.id,
          request,
        );
      } else {
        await MedicationService.addMedication(request);
      }

      if (mounted) {
        Navigator.pop(context, true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Failed to save: $e')));
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _pickTime(int index) async {
    final initial = index < _scheduledTimes.length
        ? _scheduledTimes[index]
        : const TimeOfDay(hour: 8, minute: 0);

    final picked = await showTimePicker(context: context, initialTime: initial);

    if (picked != null) {
      setState(() {
        if (index < _scheduledTimes.length) {
          _scheduledTimes[index] = picked;
        } else {
          _scheduledTimes.add(picked);
        }
      });
    }
  }

  Future<void> _pickDate(bool isStart) async {
    final initial = isStart ? _startDate : _endDate;
    final picked = await showDatePicker(
      context: context,
      initialDate: initial ?? DateTime.now(),
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );

    if (picked != null) {
      setState(() {
        if (isStart) {
          _startDate = picked;
        } else {
          _endDate = picked;
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? 'Edit Medication' : 'Add Medication'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _buildBasicInfoSection(),
            const SizedBox(height: 24),
            _buildScheduleSection(),
            const SizedBox(height: 24),
            _buildDurationSection(),
            const SizedBox(height: 24),
            _buildRemindersSection(),
            const SizedBox(height: 24),
            _buildStockSection(),
            const SizedBox(height: 24),
            _buildAppearanceSection(),
            const SizedBox(height: 32),
            _buildSaveButton(),
          ],
        ),
      ),
    );
  }

  Widget _buildBasicInfoSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Basic Information',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Medication Name *',
                hintText: 'e.g., Metformin',
                prefixIcon: Icon(Icons.medication),
              ),
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return 'Please enter medication name';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: TextFormField(
                    controller: _dosageController,
                    decoration: const InputDecoration(
                      labelText: 'Dosage',
                      hintText: 'e.g., 500mg',
                      prefixIcon: Icon(Icons.science),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: DropdownButtonFormField<MedicationForm>(
                    value: _selectedForm,
                    decoration: const InputDecoration(labelText: 'Form'),
                    items: MedicationForm.values.map((form) {
                      return DropdownMenuItem(
                        value: form,
                        child: Row(
                          children: [
                            Text(form.icon),
                            const SizedBox(width: 8),
                            Text(form.displayName),
                          ],
                        ),
                      );
                    }).toList(),
                    onChanged: (value) {
                      if (value != null) {
                        setState(() => _selectedForm = value);
                      }
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _doctorController,
              decoration: const InputDecoration(
                labelText: 'Prescribed By',
                hintText: 'Doctor\'s name',
                prefixIcon: Icon(Icons.person),
              ),
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _reasonController,
              decoration: const InputDecoration(
                labelText: 'Reason',
                hintText: 'Why are you taking this?',
                prefixIcon: Icon(Icons.help_outline),
              ),
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _instructionsController,
              decoration: const InputDecoration(
                labelText: 'Instructions',
                hintText: 'e.g., Take with food',
                prefixIcon: Icon(Icons.description),
              ),
              maxLines: 2,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildScheduleSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Schedule', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 16),
            Text('How many times per day?'),
            const SizedBox(height: 8),
            Slider(
              value: _frequencyPerDay.toDouble(),
              min: 1,
              max: 6,
              divisions: 5,
              label: '$_frequencyPerDay times/day',
              onChanged: (value) {
                setState(() {
                  _frequencyPerDay = value.toInt();
                  // Adjust times list
                  if (_scheduledTimes.length > _frequencyPerDay) {
                    _scheduledTimes = _scheduledTimes.sublist(
                      0,
                      _frequencyPerDay,
                    );
                  }
                });
              },
            ),
            const SizedBox(height: 16),
            Text('Scheduled Times (optional - defaults will be used if empty)'),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (int i = 0; i < _frequencyPerDay; i++)
                  ActionChip(
                    avatar: const Icon(Icons.access_time, size: 18),
                    label: Text(
                      i < _scheduledTimes.length
                          ? _formatTime(_scheduledTimes[i])
                          : _getDefaultTime(i),
                    ),
                    onPressed: () => _pickTime(i),
                    backgroundColor: i < _scheduledTimes.length
                        ? Theme.of(context).primaryColor.withOpacity(0.2)
                        : null,
                  ),
              ],
            ),
            if (_scheduledTimes.isNotEmpty) ...[
              const SizedBox(height: 8),
              TextButton.icon(
                onPressed: () {
                  setState(() => _scheduledTimes.clear());
                },
                icon: const Icon(Icons.clear),
                label: const Text('Clear custom times'),
              ),
            ],
          ],
        ),
      ),
    );
  }

  String _formatTime(TimeOfDay time) {
    return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
  }

  String _getDefaultTime(int index) {
    final defaults = ['08:00', '14:00', '20:00', '12:00', '17:00', '21:00'];
    return defaults[index % defaults.length];
  }

  Widget _buildDurationSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Duration', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 16),
            SwitchListTile(
              title: const Text('Chronic/Long-term'),
              subtitle: const Text('No end date'),
              value: _isChronic,
              onChanged: (value) {
                setState(() => _isChronic = value);
              },
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: ListTile(
                    title: const Text('Start Date'),
                    subtitle: Text(
                      _startDate != null
                          ? '${_startDate!.day}/${_startDate!.month}/${_startDate!.year}'
                          : 'Not set',
                    ),
                    trailing: const Icon(Icons.calendar_today),
                    onTap: () => _pickDate(true),
                  ),
                ),
                if (!_isChronic)
                  Expanded(
                    child: ListTile(
                      title: const Text('End Date'),
                      subtitle: Text(
                        _endDate != null
                            ? '${_endDate!.day}/${_endDate!.month}/${_endDate!.year}'
                            : 'Not set',
                      ),
                      trailing: const Icon(Icons.calendar_today),
                      onTap: () => _pickDate(false),
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRemindersSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Reminders', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 16),
            SwitchListTile(
              title: const Text('Enable Reminders'),
              subtitle: const Text('Get notified before each dose'),
              value: _remindersEnabled,
              onChanged: (value) {
                setState(() => _remindersEnabled = value);
              },
            ),
            if (_remindersEnabled) ...[
              const SizedBox(height: 16),
              Text('Remind me before:'),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: [5, 10, 15, 30, 60].map((minutes) {
                  return ChoiceChip(
                    label: Text(minutes >= 60 ? '1 hour' : '$minutes min'),
                    selected: _reminderMinutesBefore == minutes,
                    onSelected: (selected) {
                      if (selected) {
                        setState(() => _reminderMinutesBefore = minutes);
                      }
                    },
                  );
                }).toList(),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildStockSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Stock Tracking (Optional)',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: TextFormField(
                    controller: _stockController,
                    decoration: const InputDecoration(
                      labelText: 'Current Stock',
                      hintText: 'e.g., 30',
                      prefixIcon: Icon(Icons.inventory),
                    ),
                    keyboardType: TextInputType.number,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: TextFormField(
                    controller: _thresholdController,
                    decoration: const InputDecoration(
                      labelText: 'Low Stock Alert',
                      hintText: 'e.g., 5',
                      prefixIcon: Icon(Icons.warning),
                    ),
                    keyboardType: TextInputType.number,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppearanceSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Appearance', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 16),
            Text('Color'),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: _colorOptions.map((color) {
                final colorValue = Color(
                  int.parse(color.replaceFirst('#', '0xFF')),
                );
                return GestureDetector(
                  onTap: () {
                    setState(() => _selectedColor = color);
                  },
                  child: Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: colorValue,
                      shape: BoxShape.circle,
                      border: _selectedColor == color
                          ? Border.all(color: Colors.black, width: 3)
                          : null,
                    ),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _notesController,
              decoration: const InputDecoration(
                labelText: 'Additional Notes',
                hintText: 'Any other information...',
                prefixIcon: Icon(Icons.note),
              ),
              maxLines: 3,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSaveButton() {
    return ElevatedButton(
      onPressed: _isLoading ? null : _saveMedication,
      style: ElevatedButton.styleFrom(
        padding: const EdgeInsets.symmetric(vertical: 16),
      ),
      child: _isLoading
          ? const SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : Text(
              _isEditing ? 'Save Changes' : 'Add Medication',
              style: const TextStyle(fontSize: 16),
            ),
    );
  }
}
