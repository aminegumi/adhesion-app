import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { 
  Patient, 
  Test, 
  AssessmentSession, 
  PsychologicalProfile,
  Recommendation,
  TreatmentPlan,
  AdherencePrediction,
  DashboardStats,
  AdherenceTrend,
  ProfileDistribution,
  ProfileScore
} from '../models';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // ========== PATIENTS ==========
  getPatients(): Observable<Patient[]> {
    return this.http.get<Patient[]>(`${this.baseUrl}/identity/users`);
  }

  getPatient(id: string): Observable<Patient> {
    return this.http.get<Patient>(`${this.baseUrl}/identity/users/${id}`);
  }

  createPatient(patient: Partial<Patient>): Observable<Patient> {
    return this.http.post<Patient>(`${this.baseUrl}/identity/register`, patient);
  }

  updatePatient(id: string, patient: Partial<Patient>): Observable<Patient> {
    return this.http.put<Patient>(`${this.baseUrl}/identity/users/${id}`, patient);
  }

  deletePatient(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/identity/users/${id}`);
  }

  // ========== TESTS ==========
  getTests(): Observable<Test[]> {
    return this.http.get<Test[]>(`${this.baseUrl}/assessment/tests`);
  }

  getTest(id: string): Observable<Test> {
    return this.http.get<Test>(`${this.baseUrl}/assessment/tests/${id}`);
  }

  createTest(test: Partial<Test>): Observable<Test> {
    return this.http.post<Test>(`${this.baseUrl}/assessment/tests`, test);
  }

  updateTest(id: string, test: Partial<Test>): Observable<Test> {
    return this.http.put<Test>(`${this.baseUrl}/assessment/tests/${id}`, test);
  }

  deleteTest(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/assessment/tests/${id}`);
  }

  // ========== ASSESSMENT SESSIONS ==========
  getSessions(): Observable<AssessmentSession[]> {
    return this.http.get<AssessmentSession[]>(`${this.baseUrl}/assessment/sessions`);
  }

  getSessionsByUser(userId: string): Observable<AssessmentSession[]> {
    return this.http.get<AssessmentSession[]>(`${this.baseUrl}/assessment/sessions/user/${userId}`);
  }

  getSession(id: string): Observable<AssessmentSession> {
    return this.http.get<AssessmentSession>(`${this.baseUrl}/assessment/sessions/${id}`);
  }

  getSessionScores(sessionId: string): Observable<ProfileScore[]> {
    return this.http.get<ProfileScore[]>(`${this.baseUrl}/assessment/sessions/${sessionId}/scores`);
  }

  // ========== PSYCHOLOGICAL PROFILES ==========
  getProfiles(): Observable<PsychologicalProfile[]> {
    return this.http.get<PsychologicalProfile[]>(`${this.baseUrl}/profiles`);
  }

  getProfilesByUser(userId: string): Observable<PsychologicalProfile[]> {
    return this.http.get<PsychologicalProfile[]>(`${this.baseUrl}/profiles/user/${userId}`);
  }

  getProfile(id: string): Observable<PsychologicalProfile> {
    return this.http.get<PsychologicalProfile>(`${this.baseUrl}/profiles/${id}`);
  }

  createProfile(profile: Partial<PsychologicalProfile>): Observable<PsychologicalProfile> {
    return this.http.post<PsychologicalProfile>(`${this.baseUrl}/profiles`, profile);
  }

  // ========== RECOMMENDATIONS ==========
  getRecommendations(): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.baseUrl}/recommendations`);
  }

  getRecommendationsByUser(userId: string): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.baseUrl}/recommendations/user/${userId}`);
  }

  createRecommendation(recommendation: Partial<Recommendation>): Observable<Recommendation> {
    return this.http.post<Recommendation>(`${this.baseUrl}/recommendations`, recommendation);
  }

  // ========== TREATMENT PLANS ==========
  getTreatmentPlans(): Observable<TreatmentPlan[]> {
    return this.http.get<TreatmentPlan[]>(`${this.baseUrl}/treatment-plans`);
  }

  getTreatmentPlansByUser(userId: string): Observable<TreatmentPlan[]> {
    return this.http.get<TreatmentPlan[]>(`${this.baseUrl}/treatment-plans/user/${userId}`);
  }

  getTreatmentPlan(id: string): Observable<TreatmentPlan> {
    return this.http.get<TreatmentPlan>(`${this.baseUrl}/treatment-plans/${id}`);
  }

  createTreatmentPlan(plan: Partial<TreatmentPlan>): Observable<TreatmentPlan> {
    return this.http.post<TreatmentPlan>(`${this.baseUrl}/treatment-plans`, plan);
  }

  // ========== PREDICTIONS ==========
  getPredictions(): Observable<AdherencePrediction[]> {
    return this.http.get<AdherencePrediction[]>(`${this.baseUrl}/prediction/history`);
  }

  getPredictionsByUser(userId: string): Observable<AdherencePrediction[]> {
    return this.http.get<AdherencePrediction[]>(`${this.baseUrl}/prediction/history/${userId}`);
  }

  runPrediction(userId: string): Observable<AdherencePrediction> {
    return this.http.post<AdherencePrediction>(`${this.baseUrl}/prediction/predict`, { userId });
  }

  // ========== ANALYTICS ==========
  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.baseUrl}/analytics/dashboard`);
  }

  getAdherenceTrends(from: string, to: string): Observable<AdherenceTrend[]> {
    return this.http.get<AdherenceTrend[]>(`${this.baseUrl}/analytics/adherence/trends`, {
      params: { from, to }
    });
  }

  getProfileDistribution(): Observable<ProfileDistribution[]> {
    return this.http.get<ProfileDistribution[]>(`${this.baseUrl}/analytics/profiles/distribution`);
  }

  // ========== AI SERVICES ==========
  generateMotivation(userId: string, context: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/ai/motivation`, { userId, context });
  }

  generateRecommendations(userId: string, profileId: string): Observable<Recommendation[]> {
    return this.http.post<Recommendation[]>(`${this.baseUrl}/ai/recommendations`, { userId, profileId });
  }

  generateTreatmentPlan(userId: string, profileId: string, durationDays: number): Observable<TreatmentPlan> {
    return this.http.post<TreatmentPlan>(`${this.baseUrl}/ai/treatment-plan`, { 
      userId, 
      profileId, 
      durationDays 
    });
  }
}
