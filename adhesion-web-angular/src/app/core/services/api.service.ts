import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
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
  active: boolean;
  questions: Question[];
}

export interface Question {
  id: number;
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
  medicationsList: Medication[];
}

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
  features: any;
}

export interface AdherenceSummary {
  date: string;
  adherenceScore: number;
  dosesTaken: number;
  dosesTotal: number;
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

  createTreatmentPlan(userId: number, plan: Partial<TreatmentPlan>): Observable<TreatmentPlan> {
    return this.http.post<TreatmentPlan>(`${this.baseUrl}/api/treatment-plans`, { ...plan, userId }, { headers: this.getHeaders() });
  }

  updateTreatmentPlan(planId: number, plan: Partial<TreatmentPlan>): Observable<TreatmentPlan> {
    return this.http.put<TreatmentPlan>(`${this.baseUrl}/api/treatment-plans/${planId}`, plan, { headers: this.getHeaders() });
  }

  deleteTreatmentPlan(planId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/treatment-plans/${planId}`, { headers: this.getHeaders() });
  }

  // ==================== PREDICTIONS ====================
  getPredictions(userId: number): Observable<Prediction[]> {
    return this.http.get<Prediction[]>(`${this.baseUrl}/api/predictions/patient/${userId}/history`, { headers: this.getHeaders() });
  }

  generatePrediction(userId: number): Observable<Prediction> {
    return this.http.post<Prediction>(`${this.baseUrl}/api/predictions/predict/${userId}`, {}, { headers: this.getHeaders() });
  }

  // ==================== ADHERENCE ====================
  getAdherenceHistory(userId: number, from: string, to: string): Observable<AdherenceSummary[]> {
    return this.http.get<AdherenceSummary[]>(
      `${this.baseUrl}/api/adherence/patient/${userId}/history?from=${from}&to=${to}`,
      { headers: this.getHeaders() }
    );
  }

  getAdherenceScore(userId: number): Observable<number> {
    return this.http.get<{ score: number }>(`${this.baseUrl}/api/adherence/patient/${userId}/score`, { headers: this.getHeaders() })
      .pipe(map(res => res.score));
  }

  // ==================== RECOMMENDATIONS ====================
  getRecommendations(userId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/api/recommendations/patient/${userId}`, { headers: this.getHeaders() });
  }

  generateRecommendations(userId: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/api/recommendations/generate/${userId}`, {}, { headers: this.getHeaders() });
  }

  // ==================== PSYCHOLOGICAL PROFILE ====================
  getPsychologicalProfile(userId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/api/profile/patient/${userId}`, { headers: this.getHeaders() });
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

  createTest(test: Partial<Test>): Observable<Test> {
    return this.http.post<Test>(`${this.baseUrl}/api/tests`, test, { headers: this.getHeaders() });
  }

  updateTest(id: number, test: Partial<Test>): Observable<Test> {
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
}
