import 'dart:convert';
import 'package:adhesion_app_client_flutter/core/api_client.dart';
import '../models/user_medication.dart';

/// Service for managing user medications
class MedicationService {
  /// Get all medications for a user
  static Future<List<UserMedication>> getUserMedications(int userId) async {
    final response = await ApiClient.get('/api/medications/user/$userId');
    final List<dynamic> data = json.decode(response.body);
    return data.map((json) => UserMedication.fromJson(json)).toList();
  }

  /// Get only active medications for a user
  static Future<List<UserMedication>> getActiveMedications(int userId) async {
    final response = await ApiClient.get(
      '/api/medications/user/$userId/active',
    );
    final List<dynamic> data = json.decode(response.body);
    return data.map((json) => UserMedication.fromJson(json)).toList();
  }

  /// Get a single medication by ID
  static Future<UserMedication> getMedication(int medicationId) async {
    final response = await ApiClient.get('/api/medications/$medicationId');
    return UserMedication.fromJson(json.decode(response.body));
  }

  /// Add a new medication
  static Future<UserMedication> addMedication(
    CreateMedicationRequest request,
  ) async {
    final response = await ApiClient.post('/api/medications', request.toJson());
    return UserMedication.fromJson(json.decode(response.body));
  }

  /// Update an existing medication
  static Future<UserMedication> updateMedication(
    int medicationId,
    CreateMedicationRequest request,
  ) async {
    final response = await ApiClient.put(
      '/api/medications/$medicationId',
      request.toJson(),
    );
    return UserMedication.fromJson(json.decode(response.body));
  }

  /// Delete a medication
  static Future<void> deleteMedication(int medicationId) async {
    await ApiClient.delete('/api/medications/$medicationId');
  }

  /// Toggle medication active status
  static Future<UserMedication> toggleActive(int medicationId) async {
    final response = await ApiClient.patch(
      '/api/medications/$medicationId/toggle-active',
      {},
    );
    return UserMedication.fromJson(json.decode(response.body));
  }

  /// Update scheduled times for a medication
  static Future<UserMedication> updateScheduledTimes(
    int medicationId,
    List<String> times,
  ) async {
    final response = await ApiClient.put(
      '/api/medications/$medicationId/times',
      times,
    );
    return UserMedication.fromJson(json.decode(response.body));
  }

  /// Toggle reminders for a medication
  static Future<UserMedication> toggleReminders(int medicationId) async {
    final response = await ApiClient.patch(
      '/api/medications/$medicationId/toggle-reminders',
      {},
    );
    return UserMedication.fromJson(json.decode(response.body));
  }

  /// Generate daily schedule for a user
  static Future<Map<String, dynamic>> generateDailySchedule(int userId) async {
    final response = await ApiClient.post(
      '/api/medications/user/$userId/generate-schedule',
      {},
    );
    return json.decode(response.body);
  }
}
