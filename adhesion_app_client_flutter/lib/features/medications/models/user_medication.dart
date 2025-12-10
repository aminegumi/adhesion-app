/// Model for user medication
class UserMedication {
  final int id;
  final int userId;
  final String name;
  final String? dosage;
  final MedicationForm form;
  final int frequencyPerDay;
  final List<String> scheduledTimes;
  final String? prescribedBy;
  final String? instructions;
  final DateTime? startDate;
  final DateTime? endDate;
  final bool isChronic;
  final int? currentStock;
  final int? lowStockThreshold;
  final bool active;
  final bool remindersEnabled;
  final int reminderMinutesBefore;
  final String? notes;
  final String? reason;
  final String? color;
  final DateTime? createdAt;

  UserMedication({
    required this.id,
    required this.userId,
    required this.name,
    this.dosage,
    required this.form,
    required this.frequencyPerDay,
    required this.scheduledTimes,
    this.prescribedBy,
    this.instructions,
    this.startDate,
    this.endDate,
    required this.isChronic,
    this.currentStock,
    this.lowStockThreshold,
    required this.active,
    required this.remindersEnabled,
    required this.reminderMinutesBefore,
    this.notes,
    this.reason,
    this.color,
    this.createdAt,
  });

  factory UserMedication.fromJson(Map<String, dynamic> json) {
    return UserMedication(
      id: json['id'],
      userId: json['userId'],
      name: json['name'],
      dosage: json['dosage'],
      form: MedicationForm.fromString(json['form'] ?? 'TABLET'),
      frequencyPerDay: json['frequencyPerDay'] ?? 1,
      scheduledTimes: json['scheduledTimes'] != null
          ? List<String>.from(json['scheduledTimes'])
          : [],
      prescribedBy: json['prescribedBy'],
      instructions: json['instructions'],
      startDate: json['startDate'] != null
          ? DateTime.parse(json['startDate'])
          : null,
      endDate: json['endDate'] != null ? DateTime.parse(json['endDate']) : null,
      isChronic: json['isChronic'] ?? json['chronic'] ?? false,
      currentStock: json['currentStock'],
      lowStockThreshold: json['lowStockThreshold'],
      active: json['active'] ?? true,
      remindersEnabled: json['remindersEnabled'] ?? true,
      reminderMinutesBefore: json['reminderMinutesBefore'] ?? 15,
      notes: json['notes'],
      reason: json['reason'],
      color: json['color'],
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'])
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'userId': userId,
      'name': name,
      'dosage': dosage,
      'form': form.name,
      'frequencyPerDay': frequencyPerDay,
      'scheduledTimes': scheduledTimes,
      'prescribedBy': prescribedBy,
      'instructions': instructions,
      'startDate': startDate?.toIso8601String().split('T').first,
      'endDate': endDate?.toIso8601String().split('T').first,
      'isChronic': isChronic,
      'currentStock': currentStock,
      'lowStockThreshold': lowStockThreshold,
      'active': active,
      'remindersEnabled': remindersEnabled,
      'reminderMinutesBefore': reminderMinutesBefore,
      'notes': notes,
      'reason': reason,
      'color': color,
    };
  }

  /// Get formatted time display
  String get timesDisplay {
    if (scheduledTimes.isEmpty) {
      return _getDefaultTimeDisplay();
    }
    return scheduledTimes.join(', ');
  }

  String _getDefaultTimeDisplay() {
    switch (frequencyPerDay) {
      case 1:
        return '08:00';
      case 2:
        return '08:00, 20:00';
      case 3:
        return '08:00, 14:00, 20:00';
      case 4:
        return '08:00, 12:00, 17:00, 21:00';
      default:
        return '$frequencyPerDay times/day';
    }
  }

  /// Check if stock is low
  bool get isLowStock {
    if (currentStock == null || lowStockThreshold == null) return false;
    return currentStock! <= lowStockThreshold!;
  }

  /// Create a copy with modified fields
  UserMedication copyWith({
    int? id,
    int? userId,
    String? name,
    String? dosage,
    MedicationForm? form,
    int? frequencyPerDay,
    List<String>? scheduledTimes,
    String? prescribedBy,
    String? instructions,
    DateTime? startDate,
    DateTime? endDate,
    bool? isChronic,
    int? currentStock,
    int? lowStockThreshold,
    bool? active,
    bool? remindersEnabled,
    int? reminderMinutesBefore,
    String? notes,
    String? reason,
    String? color,
    DateTime? createdAt,
  }) {
    return UserMedication(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      name: name ?? this.name,
      dosage: dosage ?? this.dosage,
      form: form ?? this.form,
      frequencyPerDay: frequencyPerDay ?? this.frequencyPerDay,
      scheduledTimes: scheduledTimes ?? this.scheduledTimes,
      prescribedBy: prescribedBy ?? this.prescribedBy,
      instructions: instructions ?? this.instructions,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      isChronic: isChronic ?? this.isChronic,
      currentStock: currentStock ?? this.currentStock,
      lowStockThreshold: lowStockThreshold ?? this.lowStockThreshold,
      active: active ?? this.active,
      remindersEnabled: remindersEnabled ?? this.remindersEnabled,
      reminderMinutesBefore:
          reminderMinutesBefore ?? this.reminderMinutesBefore,
      notes: notes ?? this.notes,
      reason: reason ?? this.reason,
      color: color ?? this.color,
      createdAt: createdAt ?? this.createdAt,
    );
  }
}

/// Medication form types
enum MedicationForm {
  TABLET,
  CAPSULE,
  LIQUID,
  INJECTION,
  CREAM,
  DROPS,
  INHALER,
  PATCH,
  SUPPOSITORY,
  OTHER;

  static MedicationForm fromString(String value) {
    return MedicationForm.values.firstWhere(
      (e) => e.name == value.toUpperCase(),
      orElse: () => MedicationForm.TABLET,
    );
  }

  String get displayName {
    switch (this) {
      case MedicationForm.TABLET:
        return 'Tablet';
      case MedicationForm.CAPSULE:
        return 'Capsule';
      case MedicationForm.LIQUID:
        return 'Liquid';
      case MedicationForm.INJECTION:
        return 'Injection';
      case MedicationForm.CREAM:
        return 'Cream';
      case MedicationForm.DROPS:
        return 'Drops';
      case MedicationForm.INHALER:
        return 'Inhaler';
      case MedicationForm.PATCH:
        return 'Patch';
      case MedicationForm.SUPPOSITORY:
        return 'Suppository';
      case MedicationForm.OTHER:
        return 'Other';
    }
  }

  String get icon {
    switch (this) {
      case MedicationForm.TABLET:
        return '💊';
      case MedicationForm.CAPSULE:
        return '💊';
      case MedicationForm.LIQUID:
        return '🧴';
      case MedicationForm.INJECTION:
        return '💉';
      case MedicationForm.CREAM:
        return '🧴';
      case MedicationForm.DROPS:
        return '💧';
      case MedicationForm.INHALER:
        return '🫁';
      case MedicationForm.PATCH:
        return '🩹';
      case MedicationForm.SUPPOSITORY:
        return '💊';
      case MedicationForm.OTHER:
        return '💊';
    }
  }
}

/// Request model for creating/updating medications
class CreateMedicationRequest {
  final int userId;
  final String name;
  final String? dosage;
  final MedicationForm form;
  final int frequencyPerDay;
  final List<String>? scheduledTimes;
  final String? prescribedBy;
  final String? instructions;
  final DateTime? startDate;
  final DateTime? endDate;
  final bool isChronic;
  final int? currentStock;
  final int? lowStockThreshold;
  final bool remindersEnabled;
  final int reminderMinutesBefore;
  final String? notes;
  final String? reason;
  final String? color;

  CreateMedicationRequest({
    required this.userId,
    required this.name,
    this.dosage,
    this.form = MedicationForm.TABLET,
    this.frequencyPerDay = 1,
    this.scheduledTimes,
    this.prescribedBy,
    this.instructions,
    this.startDate,
    this.endDate,
    this.isChronic = false,
    this.currentStock,
    this.lowStockThreshold,
    this.remindersEnabled = true,
    this.reminderMinutesBefore = 15,
    this.notes,
    this.reason,
    this.color,
  });

  Map<String, dynamic> toJson() {
    return {
      'userId': userId,
      'name': name,
      'dosage': dosage,
      'form': form.name,
      'frequencyPerDay': frequencyPerDay,
      'scheduledTimes': scheduledTimes,
      'prescribedBy': prescribedBy,
      'instructions': instructions,
      'startDate': startDate?.toIso8601String().split('T').first,
      'endDate': endDate?.toIso8601String().split('T').first,
      'isChronic': isChronic,
      'currentStock': currentStock,
      'lowStockThreshold': lowStockThreshold,
      'remindersEnabled': remindersEnabled,
      'reminderMinutesBefore': reminderMinutesBefore,
      'notes': notes,
      'reason': reason,
      'color': color,
    };
  }
}
