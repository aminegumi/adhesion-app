// User models
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'ADMIN' | 'PATIENT';
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: string;
  email: string;
  role: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

// Patient models
export interface Patient extends User {
  age?: number;
  gender?: string;
  medicalHistory?: string;
  profileType?: string;
}

// Test models
export interface Test {
  id: string;
  name: string;
  description: string;
  type: TestType;
  createdAt: string;
  questions: Question[];
}

export type TestType = 'ANXIETY' | 'DEPRESSION' | 'MOTIVATION' | 'ADHERENCE' | 'GENERAL';

export interface Question {
  id: string;
  text: string;
  orderIndex: number;
  answerOptions: AnswerOption[];
}

export interface AnswerOption {
  id: string;
  text: string;
  score: number;
}

// Assessment models
export interface AssessmentSession {
  id: string;
  userId: string;
  testId: string;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';
  startedAt: string;
  completedAt?: string;
  answers: AssessmentAnswer[];
}

export interface AssessmentAnswer {
  questionId: string;
  optionId: string;
  score: number;
}

export interface ProfileScore {
  id: string;
  sessionId: string;
  category: string;
  score: number;
  maxScore: number;
  interpretation: string;
}

// Psychological Profile models
export interface PsychologicalProfile {
  id: string;
  userId: string;
  profileType: ProfileType;
  anxietyScore: number;
  depressionScore: number;
  motivationScore: number;
  stressScore: number;
  adherenceLikelihood: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  notes: string;
  assessedAt: string;
  createdAt: string;
}

export type ProfileType = 'RESILIENT' | 'ANXIOUS' | 'DEPRESSIVE' | 'LOW_MOTIVATION' | 'MIXED' | 'COMPLEX';

// Recommendation models
export interface Recommendation {
  id: string;
  userId: string;
  profileId: string;
  category: RecommendationCategory;
  title: string;
  description: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'DISMISSED';
  createdAt: string;
}

export type RecommendationCategory = 'MEDICATION' | 'LIFESTYLE' | 'BEHAVIORAL' | 'SUPPORT';

// Treatment Plan models
export interface TreatmentPlan {
  id: string;
  userId: string;
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  dailyTasks: DailyTask[];
  createdAt: string;
}

export interface DailyTask {
  id: string;
  title: string;
  description: string;
  taskType: 'MEDICATION' | 'EXERCISE' | 'THERAPY' | 'LIFESTYLE';
  scheduledTime: string;
  completed: boolean;
  completedAt?: string;
  dayNumber: number;
}

// Prediction models
export interface AdherencePrediction {
  id: string;
  userId: string;
  probNonAdherence: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  predictedAt: string;
  factors: string[];
}

// Analytics models
export interface DashboardStats {
  totalPatients: number;
  activePatients: number;
  totalTests: number;
  completedAssessments: number;
  averageAdherence: number;
  highRiskPatients: number;
}

export interface AdherenceTrend {
  date: string;
  averageScore: number;
  patientCount: number;
}

export interface ProfileDistribution {
  profileType: ProfileType;
  count: number;
  percentage: number;
}
