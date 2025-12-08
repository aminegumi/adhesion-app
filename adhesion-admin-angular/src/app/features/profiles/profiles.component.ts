import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { PsychologicalProfile, ProfileType } from '../../core/models';

@Component({
  selector: 'app-profiles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="profiles-page">
      <div class="page-header">
        <h2>Psychological Profiles</h2>
        <div class="filters">
          <select class="form-input" [(ngModel)]="filterType" (change)="filterProfiles()">
            <option value="">All Types</option>
            <option value="RESILIENT">Resilient</option>
            <option value="ANXIOUS">Anxious</option>
            <option value="DEPRESSIVE">Depressive</option>
            <option value="LOW_MOTIVATION">Low Motivation</option>
            <option value="MIXED">Mixed</option>
            <option value="COMPLEX">Complex</option>
          </select>
          <select class="form-input" [(ngModel)]="filterRisk" (change)="filterProfiles()">
            <option value="">All Risk Levels</option>
            <option value="LOW">Low Risk</option>
            <option value="MEDIUM">Medium Risk</option>
            <option value="HIGH">High Risk</option>
          </select>
        </div>
      </div>
      
      @if (loading()) {
        <div class="loading-container card">
          <span class="loading-spinner"></span>
          <p>Loading profiles...</p>
        </div>
      } @else if (filteredProfiles().length === 0) {
        <div class="empty-state card">
          <span class="material-icons icon">psychology</span>
          <h3>No profiles found</h3>
          <p>Psychological profiles will appear here after patient assessments</p>
        </div>
      } @else {
        <div class="profiles-grid">
          @for (profile of filteredProfiles(); track profile.id) {
            <div class="profile-card card">
              <div class="profile-header">
                <span class="badge" [ngClass]="getProfileBadgeClass(profile.profileType)">
                  {{ formatProfileType(profile.profileType) }}
                </span>
                <span class="badge" [ngClass]="getRiskBadgeClass(profile.riskLevel)">
                  {{ profile.riskLevel }} Risk
                </span>
              </div>
              
              <div class="profile-patient">
                <div class="avatar">{{ profile.userId.slice(0, 2).toUpperCase() }}</div>
                <div>
                  <div class="patient-id">Patient {{ profile.userId.slice(0, 8) }}...</div>
                  <div class="assessed-date">Assessed {{ profile.assessedAt | date:'mediumDate' }}</div>
                </div>
              </div>
              
              <div class="scores-grid">
                <div class="score-item">
                  <div class="score-label">Anxiety</div>
                  <div class="score-bar">
                    <div class="bar" [style.width.%]="profile.anxietyScore" [class.high]="profile.anxietyScore > 70"></div>
                  </div>
                  <div class="score-value">{{ profile.anxietyScore }}</div>
                </div>
                <div class="score-item">
                  <div class="score-label">Depression</div>
                  <div class="score-bar">
                    <div class="bar" [style.width.%]="profile.depressionScore" [class.high]="profile.depressionScore > 70"></div>
                  </div>
                  <div class="score-value">{{ profile.depressionScore }}</div>
                </div>
                <div class="score-item">
                  <div class="score-label">Motivation</div>
                  <div class="score-bar">
                    <div class="bar motivation" [style.width.%]="profile.motivationScore"></div>
                  </div>
                  <div class="score-value">{{ profile.motivationScore }}</div>
                </div>
                <div class="score-item">
                  <div class="score-label">Stress</div>
                  <div class="score-bar">
                    <div class="bar" [style.width.%]="profile.stressScore" [class.high]="profile.stressScore > 70"></div>
                  </div>
                  <div class="score-value">{{ profile.stressScore }}</div>
                </div>
              </div>
              
              <div class="adherence-section">
                <div class="adherence-label">Adherence Likelihood</div>
                <div class="adherence-value" [class.low]="profile.adherenceLikelihood < 0.5">
                  {{ (profile.adherenceLikelihood * 100).toFixed(0) }}%
                </div>
              </div>
              
              @if (profile.notes) {
                <div class="notes-section">
                  <div class="notes-label">Notes</div>
                  <p>{{ profile.notes }}</p>
                </div>
              }
              
              <div class="profile-actions">
                <button class="btn btn-secondary btn-block" (click)="viewDetails(profile)">
                  View Full Details
                </button>
              </div>
            </div>
          }
        </div>
      }
      
      <!-- Profile Detail Modal -->
      @if (selectedProfile) {
        <div class="modal-overlay" (click)="selectedProfile = null">
          <div class="modal modal-large" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2 class="modal-title">Profile Details</h2>
              <button class="modal-close" (click)="selectedProfile = null">&times;</button>
            </div>
            
            <div class="profile-detail">
              <div class="detail-grid">
                <div class="detail-section">
                  <h4>Profile Type</h4>
                  <span class="badge badge-large" [ngClass]="getProfileBadgeClass(selectedProfile.profileType)">
                    {{ formatProfileType(selectedProfile.profileType) }}
                  </span>
                </div>
                
                <div class="detail-section">
                  <h4>Risk Level</h4>
                  <span class="badge badge-large" [ngClass]="getRiskBadgeClass(selectedProfile.riskLevel)">
                    {{ selectedProfile.riskLevel }}
                  </span>
                </div>
              </div>
              
              <div class="detail-section">
                <h4>Psychological Scores</h4>
                <div class="scores-detail">
                  <div class="score-row">
                    <span>Anxiety Score</span>
                    <div class="score-bar large">
                      <div class="bar" [style.width.%]="selectedProfile.anxietyScore"></div>
                    </div>
                    <span class="score">{{ selectedProfile.anxietyScore }}/100</span>
                  </div>
                  <div class="score-row">
                    <span>Depression Score</span>
                    <div class="score-bar large">
                      <div class="bar" [style.width.%]="selectedProfile.depressionScore"></div>
                    </div>
                    <span class="score">{{ selectedProfile.depressionScore }}/100</span>
                  </div>
                  <div class="score-row">
                    <span>Motivation Score</span>
                    <div class="score-bar large">
                      <div class="bar motivation" [style.width.%]="selectedProfile.motivationScore"></div>
                    </div>
                    <span class="score">{{ selectedProfile.motivationScore }}/100</span>
                  </div>
                  <div class="score-row">
                    <span>Stress Score</span>
                    <div class="score-bar large">
                      <div class="bar" [style.width.%]="selectedProfile.stressScore"></div>
                    </div>
                    <span class="score">{{ selectedProfile.stressScore }}/100</span>
                  </div>
                </div>
              </div>
              
              <div class="detail-section">
                <h4>Adherence Prediction</h4>
                <div class="adherence-detail">
                  <div class="adherence-circle" [class.low]="selectedProfile.adherenceLikelihood < 0.5">
                    {{ (selectedProfile.adherenceLikelihood * 100).toFixed(0) }}%
                  </div>
                  <p>Likelihood of medication adherence based on psychological profile analysis.</p>
                </div>
              </div>
              
              @if (selectedProfile.notes) {
                <div class="detail-section">
                  <h4>Clinical Notes</h4>
                  <p class="notes-text">{{ selectedProfile.notes }}</p>
                </div>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    
    .filters {
      display: flex;
      gap: 12px;
    }
    
    .filters .form-input {
      width: 160px;
    }
    
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 60px;
      gap: 16px;
    }
    
    .profiles-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 20px;
    }
    
    .profile-card {
      display: flex;
      flex-direction: column;
    }
    
    .profile-header {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
    }
    
    .profile-patient {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 20px;
    }
    
    .avatar {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #4f46e5, #7c3aed);
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 14px;
      font-weight: 600;
    }
    
    .patient-id {
      font-weight: 500;
    }
    
    .assessed-date {
      font-size: 12px;
      color: var(--text-secondary);
    }
    
    .scores-grid {
      display: grid;
      gap: 12px;
      margin-bottom: 16px;
    }
    
    .score-item {
      display: grid;
      grid-template-columns: 100px 1fr 40px;
      align-items: center;
      gap: 12px;
    }
    
    .score-label {
      font-size: 13px;
      color: var(--text-secondary);
    }
    
    .score-bar {
      height: 6px;
      background: #e5e7eb;
      border-radius: 3px;
      overflow: hidden;
    }
    
    .score-bar .bar {
      height: 100%;
      background: #f59e0b;
      border-radius: 3px;
      transition: width 0.3s;
    }
    
    .score-bar .bar.high {
      background: #ef4444;
    }
    
    .score-bar .bar.motivation {
      background: #10b981;
    }
    
    .score-value {
      font-size: 14px;
      font-weight: 500;
      text-align: right;
    }
    
    .adherence-section {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px;
      background: var(--bg-primary);
      border-radius: 8px;
      margin-bottom: 16px;
    }
    
    .adherence-label {
      font-size: 14px;
      color: var(--text-secondary);
    }
    
    .adherence-value {
      font-size: 24px;
      font-weight: 700;
      color: var(--secondary-color);
    }
    
    .adherence-value.low {
      color: var(--danger-color);
    }
    
    .notes-section {
      margin-bottom: 16px;
    }
    
    .notes-label {
      font-size: 12px;
      color: var(--text-secondary);
      margin-bottom: 4px;
    }
    
    .notes-section p {
      font-size: 14px;
      color: var(--text-primary);
    }
    
    .profile-actions {
      margin-top: auto;
    }
    
    .btn-block {
      width: 100%;
      justify-content: center;
    }
    
    .modal-large {
      max-width: 700px;
    }
    
    .profile-detail {
      padding: 10px 0;
    }
    
    .detail-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      margin-bottom: 24px;
    }
    
    .detail-section {
      margin-bottom: 24px;
    }
    
    .detail-section h4 {
      margin-bottom: 12px;
      color: var(--text-secondary);
      font-size: 14px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    
    .badge-large {
      padding: 8px 16px;
      font-size: 14px;
    }
    
    .scores-detail {
      display: grid;
      gap: 16px;
    }
    
    .score-row {
      display: grid;
      grid-template-columns: 140px 1fr 80px;
      align-items: center;
      gap: 16px;
    }
    
    .score-bar.large {
      height: 10px;
    }
    
    .score {
      font-weight: 500;
      text-align: right;
    }
    
    .adherence-detail {
      display: flex;
      align-items: center;
      gap: 24px;
    }
    
    .adherence-circle {
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #10b981, #059669);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 24px;
      font-weight: 700;
    }
    
    .adherence-circle.low {
      background: linear-gradient(135deg, #ef4444, #dc2626);
    }
    
    .notes-text {
      background: var(--bg-primary);
      padding: 16px;
      border-radius: 8px;
      font-style: italic;
    }
    
    @media (max-width: 1200px) {
      .profiles-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
    
    @media (max-width: 768px) {
      .profiles-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ProfilesComponent implements OnInit {
  private api = inject(ApiService);

  profiles = signal<PsychologicalProfile[]>([]);
  filteredProfiles = signal<PsychologicalProfile[]>([]);
  loading = signal(true);
  selectedProfile: PsychologicalProfile | null = null;
  filterType = '';
  filterRisk = '';

  ngOnInit(): void {
    this.loadProfiles();
  }

  loadProfiles(): void {
    this.loading.set(true);
    this.api.getProfiles().subscribe({
      next: (profiles) => {
        this.profiles.set(profiles);
        this.filteredProfiles.set(profiles);
        this.loading.set(false);
      },
      error: () => {
        // Mock data
        const mockProfiles: PsychologicalProfile[] = [
          {
            id: '1',
            userId: 'user-123-abc-def',
            profileType: 'RESILIENT',
            anxietyScore: 25,
            depressionScore: 20,
            motivationScore: 85,
            stressScore: 30,
            adherenceLikelihood: 0.92,
            riskLevel: 'LOW',
            notes: 'Patient shows excellent coping mechanisms and high motivation for treatment.',
            assessedAt: new Date().toISOString(),
            createdAt: new Date().toISOString()
          },
          {
            id: '2',
            userId: 'user-456-ghi-jkl',
            profileType: 'ANXIOUS',
            anxietyScore: 75,
            depressionScore: 45,
            motivationScore: 60,
            stressScore: 70,
            adherenceLikelihood: 0.65,
            riskLevel: 'MEDIUM',
            notes: 'Patient experiences significant anxiety that may impact medication adherence.',
            assessedAt: new Date().toISOString(),
            createdAt: new Date().toISOString()
          },
          {
            id: '3',
            userId: 'user-789-mno-pqr',
            profileType: 'DEPRESSIVE',
            anxietyScore: 40,
            depressionScore: 82,
            motivationScore: 35,
            stressScore: 55,
            adherenceLikelihood: 0.42,
            riskLevel: 'HIGH',
            notes: 'High depression scores indicate need for intensive support and monitoring.',
            assessedAt: new Date().toISOString(),
            createdAt: new Date().toISOString()
          }
        ];
        this.profiles.set(mockProfiles);
        this.filteredProfiles.set(mockProfiles);
        this.loading.set(false);
      }
    });
  }

  filterProfiles(): void {
    let filtered = this.profiles();
    
    if (this.filterType) {
      filtered = filtered.filter(p => p.profileType === this.filterType);
    }
    
    if (this.filterRisk) {
      filtered = filtered.filter(p => p.riskLevel === this.filterRisk);
    }
    
    this.filteredProfiles.set(filtered);
  }

  formatProfileType(type: ProfileType): string {
    const labels: Record<ProfileType, string> = {
      'RESILIENT': 'Resilient',
      'ANXIOUS': 'Anxious',
      'DEPRESSIVE': 'Depressive',
      'LOW_MOTIVATION': 'Low Motivation',
      'MIXED': 'Mixed',
      'COMPLEX': 'Complex'
    };
    return labels[type];
  }

  getProfileBadgeClass(profileType: ProfileType): string {
    const classes: Record<ProfileType, string> = {
      'RESILIENT': 'badge-success',
      'ANXIOUS': 'badge-warning',
      'DEPRESSIVE': 'badge-danger',
      'LOW_MOTIVATION': 'badge-info',
      'MIXED': 'badge-info',
      'COMPLEX': 'badge-danger'
    };
    return classes[profileType];
  }

  getRiskBadgeClass(riskLevel: string): string {
    const classes: Record<string, string> = {
      'LOW': 'badge-success',
      'MEDIUM': 'badge-warning',
      'HIGH': 'badge-danger'
    };
    return classes[riskLevel] || 'badge-info';
  }

  viewDetails(profile: PsychologicalProfile): void {
    this.selectedProfile = profile;
  }
}
