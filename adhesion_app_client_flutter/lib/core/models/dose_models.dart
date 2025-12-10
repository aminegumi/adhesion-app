// Dose Log Models for tracking individual medication doses

enum DoseStatus {
  pending,
  taken,
  skipped,
  missed;

  static DoseStatus fromString(String? value) {
    if (value == null) return DoseStatus.pending;
    return DoseStatus.values.firstWhere(
      (e) => e.name.toUpperCase() == value.toUpperCase(),
      orElse: () => DoseStatus.pending,
    );
  }

  String get displayName {
    switch (this) {
      case DoseStatus.pending:
        return 'Pending';
      case DoseStatus.taken:
        return 'Taken';
      case DoseStatus.skipped:
        return 'Skipped';
      case DoseStatus.missed:
        return 'Missed';
    }
  }
}

enum SkipReason {
  forgot,
  sideEffects,
  ranOut,
  feelingBetter,
  doctorAdvised,
  other;

  static SkipReason? fromString(String? value) {
    if (value == null) return null;
    final normalized = value.replaceAll('_', '').toLowerCase();
    return SkipReason.values.firstWhere(
      (e) => e.name.toLowerCase() == normalized,
      orElse: () => SkipReason.other,
    );
  }

  String get displayName {
    switch (this) {
      case SkipReason.forgot:
        return 'I forgot';
      case SkipReason.sideEffects:
        return 'Side effects';
      case SkipReason.ranOut:
        return 'Ran out of medication';
      case SkipReason.feelingBetter:
        return 'Feeling better';
      case SkipReason.doctorAdvised:
        return 'Doctor advised';
      case SkipReason.other:
        return 'Other reason';
    }
  }
}

class DoseLog {
  final int id;
  final int medicationId;
  final String medicationName;
  final String? dosage;
  final String scheduledDate;
  final String scheduledTime;
  final DoseStatus status;
  final String? takenAt;
  final int? delayMinutes;
  final String? notes;
  final SkipReason? skipReason;
  final bool reminderSent;

  DoseLog({
    required this.id,
    required this.medicationId,
    required this.medicationName,
    this.dosage,
    required this.scheduledDate,
    required this.scheduledTime,
    required this.status,
    this.takenAt,
    this.delayMinutes,
    this.notes,
    this.skipReason,
    this.reminderSent = false,
  });

  factory DoseLog.fromJson(Map<String, dynamic> json) {
    return DoseLog(
      id: json['id'] as int,
      medicationId: json['medicationId'] as int,
      medicationName: json['medicationName'] as String? ?? 'Unknown',
      dosage: json['dosage'] as String?,
      scheduledDate: json['scheduledDate'] as String? ?? '',
      scheduledTime: json['scheduledTime'] as String? ?? '',
      status: DoseStatus.fromString(json['status'] as String?),
      takenAt: json['takenAt'] as String?,
      delayMinutes: json['delayMinutes'] as int?,
      notes: json['notes'] as String?,
      skipReason: SkipReason.fromString(json['skipReason'] as String?),
      reminderSent: json['reminderSent'] as bool? ?? false,
    );
  }

  bool get isPending => status == DoseStatus.pending;
  bool get isTaken => status == DoseStatus.taken;
  bool get isSkipped => status == DoseStatus.skipped;
  bool get isMissed => status == DoseStatus.missed;

  /// Returns true if dose is overdue (pending and past scheduled time)
  bool get isOverdue {
    if (status != DoseStatus.pending) return false;
    try {
      final now = DateTime.now();
      final scheduled = DateTime.parse('${scheduledDate}T$scheduledTime');
      return now.isAfter(scheduled);
    } catch (e) {
      return false;
    }
  }

  /// Time remaining until dose is due (negative if overdue)
  Duration? get timeUntilDue {
    try {
      final now = DateTime.now();
      final scheduled = DateTime.parse('${scheduledDate}T$scheduledTime');
      return scheduled.difference(now);
    } catch (e) {
      return null;
    }
  }

  /// Formatted scheduled time (e.g., "8:00 AM")
  String get formattedTime {
    try {
      final parts = scheduledTime.split(':');
      final hour = int.parse(parts[0]);
      final minute = int.parse(parts[1]);
      final period = hour >= 12 ? 'PM' : 'AM';
      final displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
      return '$displayHour:${minute.toString().padLeft(2, '0')} $period';
    } catch (e) {
      return scheduledTime;
    }
  }
}

class AdherenceStats {
  final int userId;
  final String? userName;
  final double overallAdherenceRate;
  final int currentStreak;
  final int longestStreak;
  final int totalDosesTaken;
  final int totalDosesMissed;
  final int totalDosesSkipped;
  final double last7DaysRate;
  final double last30DaysRate;
  final List<MedicationAdherence> byMedication;
  final Map<String, double> byTimeOfDay;
  final Map<String, int> skipReasonCounts;
  final double trendPercentage;

  AdherenceStats({
    required this.userId,
    this.userName,
    required this.overallAdherenceRate,
    required this.currentStreak,
    required this.longestStreak,
    required this.totalDosesTaken,
    required this.totalDosesMissed,
    required this.totalDosesSkipped,
    required this.last7DaysRate,
    required this.last30DaysRate,
    required this.byMedication,
    required this.byTimeOfDay,
    required this.skipReasonCounts,
    required this.trendPercentage,
  });

  factory AdherenceStats.fromJson(Map<String, dynamic> json) {
    return AdherenceStats(
      userId: json['userId'] as int,
      userName: json['userName'] as String?,
      overallAdherenceRate:
          (json['overallAdherenceRate'] as num?)?.toDouble() ?? 0,
      currentStreak: json['currentStreak'] as int? ?? 0,
      longestStreak: json['longestStreak'] as int? ?? 0,
      totalDosesTaken: json['totalDosesTaken'] as int? ?? 0,
      totalDosesMissed: json['totalDosesMissed'] as int? ?? 0,
      totalDosesSkipped: json['totalDosesSkipped'] as int? ?? 0,
      last7DaysRate: (json['last7DaysRate'] as num?)?.toDouble() ?? 0,
      last30DaysRate: (json['last30DaysRate'] as num?)?.toDouble() ?? 0,
      byMedication:
          (json['byMedication'] as List?)
              ?.map(
                (e) => MedicationAdherence.fromJson(e as Map<String, dynamic>),
              )
              .toList() ??
          [],
      byTimeOfDay:
          (json['byTimeOfDay'] as Map<String, dynamic>?)?.map(
            (k, v) => MapEntry(k, (v as num).toDouble()),
          ) ??
          {},
      skipReasonCounts:
          (json['skipReasonCounts'] as Map<String, dynamic>?)?.map(
            (k, v) => MapEntry(k, (v as num).toInt()),
          ) ??
          {},
      trendPercentage: (json['trendPercentage'] as num?)?.toDouble() ?? 0,
    );
  }

  bool get isImproving => trendPercentage > 0;
  bool get isDeclining => trendPercentage < 0;
}

class MedicationAdherence {
  final int medicationId;
  final String medicationName;
  final double adherenceRate;
  final int dosesTaken;
  final int dosesTotal;

  MedicationAdherence({
    required this.medicationId,
    required this.medicationName,
    required this.adherenceRate,
    required this.dosesTaken,
    required this.dosesTotal,
  });

  factory MedicationAdherence.fromJson(Map<String, dynamic> json) {
    return MedicationAdherence(
      medicationId: json['medicationId'] as int,
      medicationName: json['medicationName'] as String? ?? 'Unknown',
      adherenceRate: (json['adherenceRate'] as num?)?.toDouble() ?? 0,
      dosesTaken: json['dosesTaken'] as int? ?? 0,
      dosesTotal: json['dosesTotal'] as int? ?? 0,
    );
  }
}
