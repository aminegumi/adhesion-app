import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError, of, forkJoin } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface User {
  id: number;
  email: string;
  displayName: string;
  birthDate: string;
  gender: string;
  active: boolean;
  consentGiven: boolean;
  role: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  birthDate: string;
  gender: string;
  consentGiven?: boolean;
}

export interface Test {
  id: number;
  code: string;
  title: string;
  description: string;
  version?: string;
  active: boolean;
  questions: Question[];
}

export interface Question {
  id?: number;
  code: string;
  text: string;
  orderIndex: number;
  minScore: number;
  maxScore: number;
  reverseScored: boolean;
}

export interface TestSession {
  id: number;
  testId: number;
  testCode: string;
  testTitle: string;
  status: string;
  questions: Question[];
}

export interface TestResult {
  sessionId: number;
  testCode: string;
  testTitle: string;
  totalScore: number;
  interpretationLevel: string;
  scoreDescription: string;
  clinicalInterpretation: string;
  recommendations: string[];
  submittedAt: string;
}

export interface TreatmentPlan {
  id: number;
  userId: number;
  title: string;
  description: string;
  status: string;
  startDate: string;
  endDate: string;
  progressPercentage: number;
  medicationsList: PlanMedication[];
}

// Medication inside a treatment plan
export interface PlanMedication {
  name: string;
  dosage: string;
  frequency: string;
  times: string[];
}

// User's personal medication (separate entity)
export interface UserMedication {
  id: number;
  userId: number;
  name: string;
  dosage?: string;
  form: string;
  frequencyPerDay: number;
  scheduledTimes: string[];
  prescribedBy?: string;
  instructions?: string;
  startDate?: string;
  endDate?: string;
  isChronic: boolean;
  currentStock?: number;
  lowStockThreshold?: number;
  active: boolean;
  remindersEnabled: boolean;
  reminderMinutesBefore: number;
  notes?: string;
  reason?: string;
  color?: string;
  createdAt?: string;
}

export interface CreateMedicationRequest {
  userId: number;
  name: string;
  dosage?: string;
  form?: string;
  frequencyPerDay?: number;
  scheduledTimes?: string[];
  prescribedBy?: string;
  instructions?: string;
  startDate?: string;
  endDate?: string;
  isChronic?: boolean;
  currentStock?: number;
  lowStockThreshold?: number;
  active?: boolean;
  remindersEnabled?: boolean;
  reminderMinutesBefore?: number;
  notes?: string;
  reason?: string;
  color?: string;
}

// Legacy interface for backward compatibility
export interface Medication {
  id: number;
  name: string;
  dosage: string;
  instructions: string;
  timesPerDay: number;
  isChronic: boolean;
  isActive?: boolean;
  frequency?: string;
  scheduledTimes: string[];
}

export interface Prediction {
  id: number;
  userId: number;
  date: string;
  probNonAdherence: number;
  features?: any;
}

export interface AdherenceSummary {
  date: string;
  adherenceScore: number;
  dosesTaken: number;
  dosesTotal: number;
}

export interface DailyTask {
  id: number;
  planId: number;
  planTitle?: string;
  title: string;
  description?: string;
  category?: string;
  scheduledDate?: string;
  timeOfDay?: string;
  durationMinutes?: number;
  isRecurring?: boolean;
  completed: boolean;
  completedAt?: string;
  notes?: string;
}

export interface PsychologicalProfile {
  id: number;
  userId: number;
  motivationScore: number;
  selfEfficacyScore: number;
  anxietyScore: number;
  depressionScore: number;
  createdAt: string;
}

export interface Recommendation {
  id: number;
  userId: number;
  profileId?: number;
  category: string;
  priority: number;
  title: string;
  description?: string;
  actionableSteps?: string;
  expectedBenefits?: string;
  timeFrame?: string;
  difficulty?: string;
  status?: string;
  completed: boolean;
  completedAt?: string;
  createdAt?: string;
}

export interface CreatePlanRequest {
  userId: number;
  title: string;
  description: string;
  status: string;
  startDate: string;
  endDate: string;
  medicationsList: PlanMedication[];
}

// Dose tracking interfaces
export type DoseStatus = 'PENDING' | 'TAKEN' | 'SKIPPED' | 'MISSED';
export type SkipReason = 'FORGOT' | 'SIDE_EFFECTS' | 'RAN_OUT' | 'FEELING_BETTER' | 'DOCTOR_ADVISED' | 'OTHER';

export interface DoseLog {
  id: number;
  medicationId: number;
  medicationName: string;
  dosage?: string;
  scheduledDate: string;
  scheduledTime: string;
  status: DoseStatus;
  takenAt?: string;
  delayMinutes?: number;
  notes?: string;
  skipReason?: SkipReason;
  reminderSent: boolean;
}

export interface AdherenceStats {
  userId: number;
  userName?: string;
  overallAdherenceRate: number;
  currentStreak: number;
  longestStreak: number;
  totalDosesTaken: number;
  totalDosesMissed: number;
  totalDosesSkipped: number;
  last7DaysRate: number;
  last30DaysRate: number;
  trendPercentage: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl;
  private flaskUrl = environment.flaskUrl;

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('auth_token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    });
  }

  // ==================== AUTH ====================
  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/api/auth/login`, { email, password });
  }

  register(request: RegisterRequest): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/api/auth/register`, request);
  }

  // ==================== USER ====================
  getUserProfile(userId: number): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/api/users/${userId}`, { headers: this.getHeaders() });
  }

  updateUserProfile(userId: number, data: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/api/users/${userId}`, data, { headers: this.getHeaders() });
  }

  // ==================== TESTS ====================
  getTests(): Observable<Test[]> {
    return this.http.get<Test[]>(`${this.baseUrl}/api/tests`, { headers: this.getHeaders() });
  }

  getTest(id: number): Observable<Test> {
    return this.http.get<Test>(`${this.baseUrl}/api/tests/${id}`, { headers: this.getHeaders() });
  }

  startTestSession(testId: number, userId: number): Observable<TestSession> {
    return this.http.post<TestSession>(`${this.baseUrl}/api/sessions/start`, { testId, userId }, { headers: this.getHeaders() });
  }

  submitTestAnswers(sessionId: number, answers: { questionId: number; score: number }[]): Observable<TestResult> {
    return this.http.post<TestResult>(`${this.baseUrl}/api/sessions/${sessionId}/submit`, answers, { headers: this.getHeaders() });
  }

  getTestHistory(userId: number): Observable<TestResult[]> {
    return this.http.get<TestResult[]>(`${this.baseUrl}/api/sessions/patient/${userId}/history`, { headers: this.getHeaders() });
  }

  // ==================== TREATMENT PLANS ====================
  getTreatmentPlans(userId: number): Observable<TreatmentPlan[]> {
    return this.http.get<TreatmentPlan[]>(`${this.baseUrl}/api/treatment-plans/user/${userId}`, { headers: this.getHeaders() });
  }

  getTreatmentPlan(planId: number): Observable<TreatmentPlan> {
    return this.http.get<TreatmentPlan>(`${this.baseUrl}/api/treatment-plans/${planId}`, { headers: this.getHeaders() });
  }

  createTreatmentPlan(request: CreatePlanRequest): Observable<TreatmentPlan> {
    return this.http.post<TreatmentPlan>(`${this.baseUrl}/api/treatment-plans`, request, { headers: this.getHeaders() });
  }

  updateTreatmentPlan(planId: number, request: CreatePlanRequest): Observable<TreatmentPlan> {
    return this.http.put<TreatmentPlan>(`${this.baseUrl}/api/treatment-plans/${planId}`, request, { headers: this.getHeaders() });
  }

  deleteTreatmentPlan(planId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/treatment-plans/${planId}`, { headers: this.getHeaders() });
  }

  // Today's tasks from treatment plans
  getTodaysTasks(userId: number): Observable<DailyTask[]> {
    return this.http.get<DailyTask[]>(`${this.baseUrl}/api/treatment-plans/tasks/user/${userId}/today`, { headers: this.getHeaders() });
  }

  completeTask(taskId: number, notes?: string): Observable<DailyTask> {
    let url = `${this.baseUrl}/api/treatment-plans/tasks/${taskId}/complete`;
    if (notes) {
      url += `?notes=${encodeURIComponent(notes)}`;
    }
    return this.http.post<DailyTask>(url, {}, { headers: this.getHeaders() });
  }

  syncMedicationsFromPlan(planId: number, userId: number): Observable<UserMedication[]> {
    return this.http.post<UserMedication[]>(
      `${this.baseUrl}/api/treatment-plans/${planId}/sync-medications?userId=${userId}`,
      {},
      { headers: this.getHeaders() }
    );
  }

  // ==================== USER MEDICATIONS (separate from treatment plan meds) ====================
  getUserMedications(userId: number): Observable<UserMedication[]> {
    return this.http.get<UserMedication[]>(`${this.baseUrl}/api/medications/user/${userId}`, { headers: this.getHeaders() });
  }

  getActiveMedications(userId: number): Observable<UserMedication[]> {
    return this.http.get<UserMedication[]>(`${this.baseUrl}/api/medications/user/${userId}/active`, { headers: this.getHeaders() });
  }

  getMedication(medicationId: number): Observable<UserMedication> {
    return this.http.get<UserMedication>(`${this.baseUrl}/api/medications/${medicationId}`, { headers: this.getHeaders() });
  }

  addMedication(request: CreateMedicationRequest): Observable<UserMedication> {
    return this.http.post<UserMedication>(`${this.baseUrl}/api/medications`, request, { headers: this.getHeaders() });
  }

  updateMedication(medicationId: number, request: CreateMedicationRequest): Observable<UserMedication> {
    return this.http.put<UserMedication>(`${this.baseUrl}/api/medications/${medicationId}`, request, { headers: this.getHeaders() });
  }

  deleteMedication(medicationId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/medications/${medicationId}`, { headers: this.getHeaders() });
  }

  toggleMedicationActive(medicationId: number): Observable<UserMedication> {
    return this.http.patch<UserMedication>(`${this.baseUrl}/api/medications/${medicationId}/toggle-active`, {}, { headers: this.getHeaders() });
  }

  updateMedicationTimes(medicationId: number, times: string[]): Observable<UserMedication> {
    return this.http.put<UserMedication>(`${this.baseUrl}/api/medications/${medicationId}/times`, times, { headers: this.getHeaders() });
  }

  toggleMedicationReminders(medicationId: number): Observable<UserMedication> {
    return this.http.patch<UserMedication>(`${this.baseUrl}/api/medications/${medicationId}/toggle-reminders`, {}, { headers: this.getHeaders() });
  }

  generateDailySchedule(userId: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/api/medications/user/${userId}/generate-schedule`, {}, { headers: this.getHeaders() });
  }

  // ==================== PREDICTIONS ====================
  getPredictions(userId: number): Observable<Prediction[]> {
    return this.http.get<Prediction[]>(`${this.baseUrl}/api/predictions/user/${userId}`, { headers: this.getHeaders() });
  }

  getEnhancedPrediction(userId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/api/predictions/adherence/${userId}`, { headers: this.getHeaders() });
  }

  generatePrediction(userId: number): Observable<Prediction> {
    const now = new Date();
    const dateStr = now.toISOString().split('T')[0];

    // Generate prediction with default factors (like Flutter does)
    const body = {
      userId,
      date: dateStr,
      profileScore: 50.0,
      recentAdherenceRate: 0.5,
      testCompletionRate: 0.0,
      averageTestScore: 50.0,
      planProgressRate: 0.0,
      activePlansCount: 0,
      daysStreak: 0
    };

    return this.http.post<Prediction>(`${this.baseUrl}/api/predictions`, body, { headers: this.getHeaders() });
  }

  // Generate prediction with full context (like Flutter's comprehensive version)
  generatePredictionWithContext(userId: number): Observable<Prediction> {
    const now = new Date();
    const dateStr = now.toISOString().split('T')[0];
    const fromDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

    return forkJoin({
      profile: this.getPsychologicalProfile(userId).pipe(catchError(() => of(null))),
      adherence: this.getAdherenceHistory(userId, fromDate, dateStr).pipe(catchError(() => of([]))),
      tests: this.getTestHistory(userId).pipe(catchError(() => of([]))),
      plans: this.getTreatmentPlans(userId).pipe(catchError(() => of([])))
    }).pipe(
      switchMap(({ profile, adherence, tests, plans }) => {
        // Calculate profile score
        let profileScore = 50.0;
        if (profile) {
          profileScore = (
            (profile.motivationScore ?? 50) +
            (profile.selfEfficacyScore ?? 50) +
            (100 - (profile.anxietyScore ?? 50)) +
            (100 - (profile.depressionScore ?? 50))
          ) / 4;
        }

        // Calculate adherence rate and streak
        let recentAdherenceRate = 0.5;
        let daysStreak = 0;
        if (adherence.length > 0) {
          recentAdherenceRate = adherence.reduce((sum, h) => sum + h.adherenceScore, 0) / adherence.length / 100;
          for (let i = adherence.length - 1; i >= 0; i--) {
            if (adherence[i].adherenceScore >= 70) {
              daysStreak++;
            } else {
              break;
            }
          }
        }

        // Calculate test scores
        const testCompletionRate = Math.min(tests.length / 4.0, 1.0);
        let averageTestScore = 50.0;
        if (tests.length > 0) {
          const total = tests.reduce((sum, t) => sum + (100 - Math.min(t.totalScore * 5, 100)), 0);
          averageTestScore = total / tests.length;
        }

        // Calculate plan progress
        const activePlans = plans.filter(p => p.status === 'ACTIVE');
        const activePlansCount = activePlans.length;
        let planProgressRate = 0.0;
        if (activePlans.length > 0) {
          planProgressRate = activePlans.reduce((sum, p) => sum + (p.progressPercentage / 100), 0) / activePlans.length;
        }

        const body = {
          userId,
          date: dateStr,
          profileScore,
          recentAdherenceRate,
          testCompletionRate,
          averageTestScore,
          planProgressRate,
          activePlansCount,
          daysStreak
        };

        return this.http.post<Prediction>(`${this.baseUrl}/api/predictions`, body, { headers: this.getHeaders() });
      })
    );
  }

  deletePrediction(predictionId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/predictions/${predictionId}`, { headers: this.getHeaders() });
  }

  // ==================== ADHERENCE ====================
  getAdherenceHistory(userId: number, from: string, to: string): Observable<AdherenceSummary[]> {
    return this.http.get<AdherenceSummary[]>(
      `${this.baseUrl}/api/adherence/user/${userId}?from=${from}&to=${to}`,
      { headers: this.getHeaders() }
    );
  }

  getAdherenceScore(userId: number): Observable<number> {
    return this.http.get<{ score: number }>(`${this.baseUrl}/api/adherence/patient/${userId}/score`, { headers: this.getHeaders() })
      .pipe(map(res => res.score));
  }

  // ==================== RECOMMENDATIONS ====================
  getRecommendations(userId: number): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.baseUrl}/api/recommendations/patient/${userId}`, { headers: this.getHeaders() });
  }

  generateRecommendations(userId: number): Observable<Recommendation[]> {
    return this.http.post<Recommendation[]>(`${this.baseUrl}/api/recommendations/generate/${userId}`, {}, { headers: this.getHeaders() });
  }

  completeRecommendation(id: number, feedback?: string): Observable<Recommendation> {
    let url = `${this.baseUrl}/api/recommendations/${id}/complete`;
    if (feedback) {
      url += `?feedback=${encodeURIComponent(feedback)}`;
    }
    return this.http.post<Recommendation>(url, {}, { headers: this.getHeaders() });
  }

  // ==================== PSYCHOLOGICAL PROFILE ====================
  getPsychologicalProfile(userId: number): Observable<PsychologicalProfile> {
    return this.http.get<PsychologicalProfile>(`${this.baseUrl}/api/profile/patient/${userId}`, { headers: this.getHeaders() });
  }

  getLatestProfile(userId: number): Observable<PsychologicalProfile> {
    return this.getPsychologicalProfile(userId);
  }

  // ==================== ADMIN ====================
  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/api/admin/users`, { headers: this.getHeaders() });
  }

  getConsentedUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/api/admin/users/consented`, { headers: this.getHeaders() });
  }

  getUserTestResults(userId: number): Observable<TestResult[]> {
    return this.http.get<TestResult[]>(`${this.baseUrl}/api/admin/users/${userId}/test-results`, { headers: this.getHeaders() });
  }

  getUserTreatmentPlansAdmin(userId: number): Observable<TreatmentPlan[]> {
    return this.http.get<TreatmentPlan[]>(`${this.baseUrl}/api/admin/users/${userId}/treatment-plans`, { headers: this.getHeaders() });
  }

  createAdmin(data: { email: string; password: string; displayName: string }): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/api/admin/create`, {
      ...data,
      birthDate: '1990-01-01',
      gender: 'Other'
    }, { headers: this.getHeaders() });
  }

  createTest(test: any): Observable<Test> {
    return this.http.post<Test>(`${this.baseUrl}/api/tests`, test, { headers: this.getHeaders() });
  }

  updateTest(id: number, test: any): Observable<Test> {
    return this.http.put<Test>(`${this.baseUrl}/api/tests/${id}`, test, { headers: this.getHeaders() });
  }

  deleteTest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/tests/${id}`, { headers: this.getHeaders() });
  }

  // ==================== FLASK AI ====================
  getFlaskHealth(): Observable<any> {
    return this.http.get<any>(`${this.flaskUrl}/health`);
  }

  getEmotion(): Observable<{ emotion: string; confidence: number }> {
    return this.http.get<any>(`${this.flaskUrl}/emotion`);
  }

  chat(message: string, history: any[] = []): Observable<{ bot_message: string; detected_emotion: string }> {
    return this.http.post<any>(`${this.flaskUrl}/chat`, { message, history });
  }

  getMotivation(context: string = 'general wellbeing'): Observable<{ motivation: string }> {
    return this.http.post<any>(`${this.flaskUrl}/motivation`, { context });
  }

  analyzeText(text: string): Observable<any> {
    return this.http.post<any>(`${this.flaskUrl}/analyze-text`, { text });
  }

  // ==================== DOSE TRACKING ====================
  getTodaysDoses(userId: number): Observable<DoseLog[]> {
    return this.http.get<DoseLog[]>(`${this.baseUrl}/api/doses/user/${userId}/today`, { headers: this.getHeaders() });
  }

  takeDose(doseId: number, notes?: string): Observable<DoseLog> {
    const url = notes 
      ? `${this.baseUrl}/api/doses/${doseId}/take?notes=${encodeURIComponent(notes)}`
      : `${this.baseUrl}/api/doses/${doseId}/take`;
    return this.http.post<DoseLog>(url, {}, { headers: this.getHeaders() });
  }

  skipDose(doseId: number, reason: SkipReason, notes?: string): Observable<DoseLog> {
    return this.http.post<DoseLog>(`${this.baseUrl}/api/doses/${doseId}/action`, {
      action: 'skip',
      skipReason: reason,
      notes: notes
    }, { headers: this.getHeaders() });
  }

  getAdherenceStats(userId: number): Observable<AdherenceStats> {
    return this.http.get<AdherenceStats>(`${this.baseUrl}/api/doses/user/${userId}/stats`, { headers: this.getHeaders() });
  }
}
