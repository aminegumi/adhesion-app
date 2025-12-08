// Treatment Plan Models

class Medication {
  final int? id;
  final int? planId;
  final String name;
  final String? dosage;
  final String? instructions;
  final int timesPerDay;
  final bool isChronic;
  final String? startDate;
  final String? endDate;
  final List<String> scheduledTimes;
  final String? notes;
  final bool notificationsEnabled;
  final int reminderMinutesBefore;
  final String? createdAt;

  Medication({
    this.id,
    this.planId,
    required this.name,
    this.dosage,
    this.instructions,
    this.timesPerDay = 1,
    this.isChronic = false,
    this.startDate,
    this.endDate,
    this.scheduledTimes = const [],
    this.notes,
    this.notificationsEnabled = true,
    this.reminderMinutesBefore = 15,
    this.createdAt,
  });

  factory Medication.fromJson(Map<String, dynamic> json) {
    return Medication(
      id: json['id'] as int?,
      planId: json['planId'] as int?,
      name: json['name'] as String,
      dosage: json['dosage'] as String?,
      instructions: json['instructions'] as String?,
      timesPerDay: json['timesPerDay'] as int? ?? 1,
      isChronic: json['isChronic'] as bool? ?? false,
      startDate: json['startDate'] as String?,
      endDate: json['endDate'] as String?,
      scheduledTimes:
          (json['scheduledTimes'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          [],
      notes: json['notes'] as String?,
      notificationsEnabled: json['notificationsEnabled'] as bool? ?? true,
      reminderMinutesBefore: json['reminderMinutesBefore'] as int? ?? 15,
      createdAt: json['createdAt'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
    if (id != null) 'id': id,
    'name': name,
    if (dosage != null) 'dosage': dosage,
    if (instructions != null) 'instructions': instructions,
    'timesPerDay': timesPerDay,
    'isChronic': isChronic,
    if (startDate != null) 'startDate': startDate,
    if (endDate != null) 'endDate': endDate,
    if (scheduledTimes.isNotEmpty) 'scheduledTimes': scheduledTimes,
    if (notes != null) 'notes': notes,
    'notificationsEnabled': notificationsEnabled,
    'reminderMinutesBefore': reminderMinutesBefore,
  };

  Medication copyWith({
    int? id,
    int? planId,
    String? name,
    String? dosage,
    String? instructions,
    int? timesPerDay,
    bool? isChronic,
    String? startDate,
    String? endDate,
    List<String>? scheduledTimes,
    String? notes,
    bool? notificationsEnabled,
    int? reminderMinutesBefore,
  }) {
    return Medication(
      id: id ?? this.id,
      planId: planId ?? this.planId,
      name: name ?? this.name,
      dosage: dosage ?? this.dosage,
      instructions: instructions ?? this.instructions,
      timesPerDay: timesPerDay ?? this.timesPerDay,
      isChronic: isChronic ?? this.isChronic,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      scheduledTimes: scheduledTimes ?? this.scheduledTimes,
      notes: notes ?? this.notes,
      notificationsEnabled: notificationsEnabled ?? this.notificationsEnabled,
      reminderMinutesBefore:
          reminderMinutesBefore ?? this.reminderMinutesBefore,
    );
  }
}

class TreatmentPlan {
  final int id;
  final int userId;
  final String? userName;
  final int? profileId;
  final String title;
  final String? description;
  final String? planContent;
  final int? durationWeeks;
  final String? startDate;
  final String? endDate;
  final String? medications;
  final List<Medication> medicationsList;
  final String? identifiedIssues;
  final int progressPercentage;
  final String? status;
  final String? createdAt;

  TreatmentPlan({
    required this.id,
    required this.userId,
    this.userName,
    this.profileId,
    required this.title,
    this.description,
    this.planContent,
    this.durationWeeks,
    this.startDate,
    this.endDate,
    this.medications,
    this.medicationsList = const [],
    this.identifiedIssues,
    required this.progressPercentage,
    this.status,
    this.createdAt,
  });

  factory TreatmentPlan.fromJson(Map<String, dynamic> json) {
    return TreatmentPlan(
      id: json['id'] as int,
      userId: json['userId'] as int,
      userName: json['userName'] as String?,
      profileId: json['profileId'] as int?,
      title: json['title'] as String,
      description: json['description'] as String?,
      planContent: json['planContent'] as String?,
      durationWeeks: json['durationWeeks'] as int?,
      startDate: json['startDate'] as String?,
      endDate: json['endDate'] as String?,
      medications: json['medications'] as String?,
      medicationsList:
          (json['medicationsList'] as List<dynamic>?)
              ?.map((e) => Medication.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      identifiedIssues: json['identifiedIssues'] as String?,
      progressPercentage: json['progressPercentage'] as int? ?? 0,
      status: json['status'] as String?,
      createdAt: json['createdAt'] as String?,
    );
  }
}

class DailyTask {
  final int id;
  final int planId;
  final String title;
  final String? description;
  final String? category;
  final String? scheduledDate;
  final String? timeOfDay;
  final int? durationMinutes;
  final bool isRecurring;
  final bool completed;
  final String? completedAt;
  final String? notes;

  DailyTask({
    required this.id,
    required this.planId,
    required this.title,
    this.description,
    this.category,
    this.scheduledDate,
    this.timeOfDay,
    this.durationMinutes,
    required this.isRecurring,
    required this.completed,
    this.completedAt,
    this.notes,
  });

  factory DailyTask.fromJson(Map<String, dynamic> json) {
    return DailyTask(
      id: json['id'] as int,
      planId: json['planId'] as int,
      title: json['title'] as String,
      description: json['description'] as String?,
      category: json['category'] as String?,
      scheduledDate: json['scheduledDate'] as String?,
      timeOfDay: json['timeOfDay'] as String?,
      durationMinutes: json['durationMinutes'] as int?,
      isRecurring: json['isRecurring'] as bool? ?? false,
      completed: json['completed'] as bool? ?? false,
      completedAt: json['completedAt'] as String?,
      notes: json['notes'] as String?,
    );
  }
}

class CreatePlanRequest {
  final int userId;
  final String? title;
  final String? description;
  final List<String>? identifiedIssues;
  final String? medications;
  final List<Medication>? medicationsList;
  final int? durationWeeks;
  final String? startDate;

  CreatePlanRequest({
    required this.userId,
    this.title,
    this.description,
    this.identifiedIssues,
    this.medications,
    this.medicationsList,
    this.durationWeeks,
    this.startDate,
  });

  Map<String, dynamic> toJson() => {
    'userId': userId,
    if (title != null) 'title': title,
    if (description != null) 'description': description,
    if (identifiedIssues != null) 'identifiedIssues': identifiedIssues,
    if (medications != null) 'medications': medications,
    if (medicationsList != null && medicationsList!.isNotEmpty)
      'medicationsList': medicationsList!.map((m) => m.toJson()).toList(),
    if (durationWeeks != null) 'durationWeeks': durationWeeks,
    if (startDate != null) 'startDate': startDate,
  };
}
