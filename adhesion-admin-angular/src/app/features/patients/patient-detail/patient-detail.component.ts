import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartConfiguration } from 'chart.js';
import { ApiService } from '../../../core/services/api.service';
import { Patient, PsychologicalProfile, Recommendation, TreatmentPlan, AssessmentSession } from '../../../core/models';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, BaseChartDirective],
  template: `
    <div class="patient-detail">
      <div class="back-link">
        <a routerLink="/patients" class="btn btn-secondary">
          <span class="material-icons">arrow_back</span>
          Back to Patients
        </a>
      </div>
      
      @if (loading()) {
        <div class="loading-container">
          <span class="loading-spinner"></span>
        </div>
      } @else if (patient()) {
        <!-- Patient Header -->
        <div class="patient-header card">
          <div class="patient-info">
            <div class="avatar-large">{{ getInitials() }}</div>
            <div>
              <h1>{{ patient()?.firstName }} {{ patient()?.lastName }}</h1>
              <p>{{ patient()?.email }}</p>
              <div class="patient-meta">
                <span class="badge" [ngClass]="getProfileBadgeClass(profile()?.profileType)">
                  {{ profile()?.profileType || 'Not Assessed' }}
                </span>
                <span class="badge" [ngClass]="getRiskBadgeClass(profile()?.riskLevel || 'LOW')">
                  {{ profile()?.riskLevel || 'LOW' }} Risk
                </span>
              </div>
            </div>
          </div>
          <div class="patient-actions">
            <button class="btn btn-primary" (click)="generateRecommendations()">
              <span class="material-icons">lightbulb</span>
              Generate AI Recommendations
            </button>
            <button class="btn btn-secondary" (click)="generateTreatmentPlan()">
              <span class="material-icons">healing</span>
              Create Treatment Plan
            </button>
          </div>
        </div>
        
        <!-- Stats Row -->
        <div class="grid grid-cols-4">
          <div class="stat-card">
            <div class="icon" style="background: #dbeafe;">
              <span class="material-icons" style="color: #3b82f6;">psychology</span>
            </div>
            <div class="value">{{ profile()?.anxietyScore || 0 }}</div>
            <div class="label">Anxiety Score</div>
          </div>
          <div class="stat-card">
            <div class="icon" style="background: #fef3c7;">
              <span class="material-icons" style="color: #f59e0b;">mood_bad</span>
            </div>
            <div class="value">{{ profile()?.depressionScore || 0 }}</div>
            <div class="label">Depression Score</div>
          </div>
          <div class="stat-card">
            <div class="icon" style="background: #d1fae5;">
              <span class="material-icons" style="color: #10b981;">trending_up</span>
            </div>
            <div class="value">{{ profile()?.motivationScore || 0 }}</div>
            <div class="label">Motivation Score</div>
          </div>
          <div class="stat-card">
            <div class="icon" style="background: #e0e7ff;">
              <span class="material-icons" style="color: #4f46e5;">verified</span>
            </div>
            <div class="value">{{ ((profile()?.adherenceLikelihood || 0) * 100).toFixed(0) }}%</div>
            <div class="label">Adherence Likelihood</div>
          </div>
        </div>
        
        <!-- Score Chart and Sessions -->
        <div class="grid grid-cols-2">
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Psychological Profile</h3>
            </div>
            <canvas baseChart
              [data]="profileChartData"
              [options]="radarChartOptions"
              type="radar">
            </canvas>
          </div>
          
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Assessment Sessions</h3>
            </div>
            @if (sessions().length === 0) {
              <div class="empty-state">
                <span class="material-icons icon">assignment</span>
                <h3>No assessments yet</h3>
              </div>
            } @else {
              <table class="table">
                <thead>
                  <tr>
                    <th>Test</th>
                    <th>Status</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  @for (session of sessions(); track session.id) {
                    <tr>
                      <td>Assessment {{ session.id.slice(0, 8) }}</td>
                      <td>
                        <span class="badge" [ngClass]="session.status === 'COMPLETED' ? 'badge-success' : 'badge-warning'">
                          {{ session.status }}
                        </span>
                      </td>
                      <td>{{ session.startedAt | date:'short' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </div>
        </div>
        
        <!-- Recommendations -->
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">AI-Generated Recommendations</h3>
          </div>
          @if (recommendations().length === 0) {
            <div class="empty-state">
              <span class="material-icons icon">lightbulb</span>
              <h3>No recommendations yet</h3>
              <p>Click "Generate AI Recommendations" to get personalized suggestions</p>
            </div>
          } @else {
            <div class="recommendations-grid">
              @for (rec of recommendations(); track rec.id) {
                <div class="recommendation-card" [class]="'priority-' + rec.priority.toLowerCase()">
                  <div class="rec-header">
                    <span class="rec-category">{{ rec.category }}</span>
                    <span class="rec-priority">{{ rec.priority }}</span>
                  </div>
                  <h4>{{ rec.title }}</h4>
                  <p>{{ rec.description }}</p>
                </div>
              }
            </div>
          }
        </div>
        
        <!-- Treatment Plans -->
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Treatment Plans</h3>
          </div>
          @if (treatmentPlans().length === 0) {
            <div class="empty-state">
              <span class="material-icons icon">healing</span>
              <h3>No treatment plans</h3>
              <p>Create a personalized treatment plan for this patient</p>
            </div>
          } @else {
            @for (plan of treatmentPlans(); track plan.id) {
              <div class="treatment-plan">
                <div class="plan-header">
                  <div>
                    <h4>{{ plan.title }}</h4>
                    <p>{{ plan.description }}</p>
                  </div>
                  <span class="badge" [ngClass]="plan.status === 'ACTIVE' ? 'badge-success' : 'badge-info'">
                    {{ plan.status }}
                  </span>
                </div>
                <div class="plan-dates">
                  <span>{{ plan.startDate | date:'mediumDate' }} - {{ plan.endDate | date:'mediumDate' }}</span>
                </div>
                <div class="daily-tasks">
                  <h5>Daily Tasks ({{ plan.dailyTasks.length }})</h5>
                  <div class="tasks-grid">
                    @for (task of plan.dailyTasks.slice(0, 4); track task.id) {
                      <div class="task-item" [class.completed]="task.completed">
                        <span class="material-icons">{{ task.completed ? 'check_circle' : 'radio_button_unchecked' }}</span>
                        <span>{{ task.title }}</span>
                      </div>
                    }
                  </div>
                </div>
              </div>
            }
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .back-link {
      margin-bottom: 20px;
    }
    
    .loading-container {
      display: flex;
      justify-content: center;
      padding: 60px;
    }
    
    .patient-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .patient-info {
      display: flex;
      gap: 20px;
      align-items: center;
    }
    
    .avatar-large {
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #4f46e5, #7c3aed);
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 28px;
      font-weight: 600;
    }
    
    .patient-info h1 {
      font-size: 24px;
      margin-bottom: 4px;
    }
    
    .patient-info p {
      color: var(--text-secondary);
      margin-bottom: 8px;
    }
    
    .patient-meta {
      display: flex;
      gap: 8px;
    }
    
    .patient-actions {
      display: flex;
      gap: 12px;
    }
    
    .recommendations-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
    }
    
    .recommendation-card {
      padding: 16px;
      border-radius: 8px;
      border-left: 4px solid;
      background: var(--bg-primary);
    }
    
    .recommendation-card.priority-high {
      border-left-color: var(--danger-color);
    }
    
    .recommendation-card.priority-medium {
      border-left-color: var(--warning-color);
    }
    
    .recommendation-card.priority-low {
      border-left-color: var(--secondary-color);
    }
    
    .rec-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      font-size: 12px;
    }
    
    .rec-category {
      color: var(--primary-color);
      font-weight: 500;
    }
    
    .rec-priority {
      color: var(--text-secondary);
    }
    
    .recommendation-card h4 {
      margin-bottom: 8px;
    }
    
    .recommendation-card p {
      font-size: 14px;
      color: var(--text-secondary);
    }
    
    .treatment-plan {
      padding: 20px;
      background: var(--bg-primary);
      border-radius: 12px;
      margin-bottom: 16px;
    }
    
    .plan-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12px;
    }
    
    .plan-header h4 {
      margin-bottom: 4px;
    }
    
    .plan-header p {
      color: var(--text-secondary);
      font-size: 14px;
    }
    
    .plan-dates {
      color: var(--text-secondary);
      font-size: 13px;
      margin-bottom: 16px;
    }
    
    .daily-tasks h5 {
      margin-bottom: 12px;
      font-size: 14px;
    }
    
    .tasks-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 8px;
    }
    
    .task-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: white;
      border-radius: 6px;
      font-size: 14px;
    }
    
    .task-item.completed {
      color: var(--secondary-color);
    }
    
    .task-item .material-icons {
      font-size: 18px;
    }
  `]
})
export class PatientDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private api = inject(ApiService);

  loading = signal(true);
  patient = signal<Patient | null>(null);
  profile = signal<PsychologicalProfile | null>(null);
  sessions = signal<AssessmentSession[]>([]);
  recommendations = signal<Recommendation[]>([]);
  treatmentPlans = signal<TreatmentPlan[]>([]);

  profileChartData: ChartData<'radar'> = {
    labels: ['Anxiety', 'Depression', 'Motivation', 'Stress', 'Adherence'],
    datasets: [{
      data: [0, 0, 0, 0, 0],
      label: 'Score',
      backgroundColor: 'rgba(79, 70, 229, 0.2)',
      borderColor: '#4f46e5',
      pointBackgroundColor: '#4f46e5'
    }]
  };

  radarChartOptions: ChartConfiguration<'radar'>['options'] = {
    responsive: true,
    scales: {
      r: {
        beginAtZero: true,
        max: 100
      }
    }
  };

  ngOnInit(): void {
    const patientId = this.route.snapshot.paramMap.get('id');
    if (patientId) {
      this.loadPatientData(patientId);
    }
  }

  loadPatientData(patientId: string): void {
    this.loading.set(true);

    // Load patient
    this.api.getPatient(patientId).subscribe({
      next: (patient) => {
        this.patient.set(patient);
        this.loading.set(false);
      },
      error: () => {
        // Mock data
        this.patient.set({
          id: patientId,
          email: 'patient@example.com',
          firstName: 'John',
          lastName: 'Doe',
          role: 'PATIENT',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        });
        this.loading.set(false);
      }
    });

    // Load profile
    this.api.getProfilesByUser(patientId).subscribe({
      next: (profiles) => {
        if (profiles.length > 0) {
          const latestProfile = profiles[0];
          this.profile.set(latestProfile);
          this.updateChartData(latestProfile);
        }
      }
    });

    // Load sessions
    this.api.getSessionsByUser(patientId).subscribe({
      next: (sessions) => this.sessions.set(sessions)
    });

    // Load recommendations
    this.api.getRecommendationsByUser(patientId).subscribe({
      next: (recs) => this.recommendations.set(recs)
    });

    // Load treatment plans
    this.api.getTreatmentPlansByUser(patientId).subscribe({
      next: (plans) => this.treatmentPlans.set(plans)
    });
  }

  updateChartData(profile: PsychologicalProfile): void {
    this.profileChartData = {
      labels: ['Anxiety', 'Depression', 'Motivation', 'Stress', 'Adherence'],
      datasets: [{
        data: [
          profile.anxietyScore,
          profile.depressionScore,
          profile.motivationScore,
          profile.stressScore,
          profile.adherenceLikelihood * 100
        ],
        label: 'Score',
        backgroundColor: 'rgba(79, 70, 229, 0.2)',
        borderColor: '#4f46e5',
        pointBackgroundColor: '#4f46e5'
      }]
    };
  }

  getInitials(): string {
    const p = this.patient();
    return p ? `${p.firstName[0]}${p.lastName[0]}`.toUpperCase() : '';
  }

  getProfileBadgeClass(profileType?: string): string {
    const classes: Record<string, string> = {
      'RESILIENT': 'badge-success',
      'ANXIOUS': 'badge-warning',
      'DEPRESSIVE': 'badge-danger',
      'LOW_MOTIVATION': 'badge-info',
      'MIXED': 'badge-info',
      'COMPLEX': 'badge-danger'
    };
    return classes[profileType || ''] || 'badge-info';
  }

  getRiskBadgeClass(riskLevel: string): string {
    const classes: Record<string, string> = {
      'LOW': 'badge-success',
      'MEDIUM': 'badge-warning',
      'HIGH': 'badge-danger'
    };
    return classes[riskLevel] || 'badge-info';
  }

  generateRecommendations(): void {
    const p = this.patient();
    const pr = this.profile();
    if (p && pr) {
      this.api.generateRecommendations(p.id, pr.id).subscribe({
        next: (recs) => this.recommendations.set(recs)
      });
    }
  }

  generateTreatmentPlan(): void {
    const p = this.patient();
    const pr = this.profile();
    if (p && pr) {
      this.api.generateTreatmentPlan(p.id, pr.id, 14).subscribe({
        next: (plan) => this.treatmentPlans.update(plans => [plan, ...plans])
      });
    }
  }
}
