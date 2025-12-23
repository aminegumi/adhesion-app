import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, TestResult, AdherenceSummary, Prediction } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="history-page">
      <header class="page-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>History</h1>
      </header>

      <div class="content">
        <!-- Tabs -->
        <div class="tabs">
          <button class="tab" [class.active]="activeTab() === 'tests'" (click)="activeTab.set('tests')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
            Tests
          </button>
          <button class="tab" [class.active]="activeTab() === 'adherence'" (click)="activeTab.set('adherence')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
            Adherence
          </button>
          <button class="tab" [class.active]="activeTab() === 'predictions'" (click)="activeTab.set('predictions')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"/></svg>
            Predictions
          </button>
        </div>

        <!-- Tests Tab -->
        @if (activeTab() === 'tests') {
          <div class="tab-content">
            @if (testHistory().length > 0) {
              <div class="stats-mini">
                <div class="stat-item">
                  <span class="stat-value">{{ testHistory().length }}</span>
                  <span class="stat-label">Tests Taken</span>
                </div>
              </div>
            }
            @for (result of sortedTestHistory(); track result.sessionId; let i = $index) {
              <div class="history-card animate-in" [style.animation-delay.ms]="i * 50">
                <div class="card-icon tests">📋</div>
                <div class="card-body">
                  <div class="card-header">
                    <h3>{{ result.testTitle }}</h3>
                    <span class="score" [class]="getScoreClass(result.interpretationLevel)">{{ result.totalScore }}</span>
                  </div>
                  <p class="interpretation">{{ result.interpretationLevel }}</p>
                  <span class="date">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>
                    {{ formatDateTime(result.submittedAt) }}
                  </span>
                </div>
              </div>
            } @empty {
              <div class="empty-state">
                <div class="empty-icon">📋</div>
                <h3>No Test History</h3>
                <p>Complete some psychological tests to see your history here.</p>
                <a routerLink="/tests" class="btn btn-primary">Take a Test</a>
              </div>
            }
          </div>
        }

        <!-- Adherence Tab -->
        @if (activeTab() === 'adherence') {
          <div class="tab-content">
            @if (adherenceHistory().length > 0) {
              <div class="stats-mini">
                <div class="stat-item">
                  <span class="stat-value">{{ averageAdherence().toFixed(0) }}%</span>
                  <span class="stat-label">Average</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value">{{ adherenceHistory().length }}</span>
                  <span class="stat-label">Days Tracked</span>
                </div>
              </div>
            }
            @for (summary of sortedAdherenceHistory(); track summary.date; let i = $index) {
              <div class="history-card adherence animate-in" [style.animation-delay.ms]="i * 50">
                <div class="card-icon" [class.good]="summary.adherenceScore >= 80" [class.medium]="summary.adherenceScore >= 50 && summary.adherenceScore < 80" [class.low]="summary.adherenceScore < 50">
                  @if (summary.adherenceScore >= 80) { ✅ }
                  @else if (summary.adherenceScore >= 50) { ⚡ }
                  @else { ⚠️ }
                </div>
                <div class="card-body">
                  <div class="card-header">
                    <h3>{{ formatDate(summary.date) }}</h3>
                    <span class="score adherence" [class.good]="summary.adherenceScore >= 80" [class.medium]="summary.adherenceScore >= 50 && summary.adherenceScore < 80" [class.low]="summary.adherenceScore < 50">
                      {{ summary.adherenceScore }}%
                    </span>
                  </div>
                  <div class="progress-mini">
                    <div class="progress-bar">
                      <div class="fill" [style.width.%]="summary.adherenceScore" [class.good]="summary.adherenceScore >= 80"></div>
                    </div>
                  </div>
                  <div class="doses-info">
                    <span class="doses">{{ summary.dosesTaken }} / {{ summary.dosesTotal }} doses taken</span>
                  </div>
                </div>
              </div>
            } @empty {
              <div class="empty-state">
                <div class="empty-icon">💊</div>
                <h3>No Adherence Data</h3>
                <p>Start tracking your medication to see adherence history.</p>
              </div>
            }
          </div>
        }

        <!-- Predictions Tab -->
        @if (activeTab() === 'predictions') {
          <div class="tab-content">
            @if (predictions().length > 0) {
              <div class="stats-mini">
                <div class="stat-item">
                  <span class="stat-value">{{ predictions().length }}</span>
                  <span class="stat-label">Predictions</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value">{{ averageRisk().toFixed(0) }}%</span>
                  <span class="stat-label">Avg Risk</span>
                </div>
              </div>
            }
            @for (pred of sortedPredictions(); track pred.id; let i = $index) {
              <div class="history-card prediction animate-in" [class.high-risk]="pred.probNonAdherence > 0.5" [style.animation-delay.ms]="i * 50">
                <div class="card-icon prediction" [class.high]="pred.probNonAdherence > 0.5">
                  @if (pred.probNonAdherence > 0.5) { ⚠️ }
                  @else if (pred.probNonAdherence > 0.3) { ⚡ }
                  @else { ✅ }
                </div>
                <div class="card-body">
                  <div class="card-header">
                    <h3>Risk Assessment</h3>
                    <span class="score prediction" [class.high]="pred.probNonAdherence > 0.5">
                      {{ (pred.probNonAdherence * 100).toFixed(0) }}%
                    </span>
                  </div>
                  <div class="risk-meter">
                    <div class="meter-fill" [style.width.%]="pred.probNonAdherence * 100" [class.high]="pred.probNonAdherence > 0.5"></div>
                  </div>
                  <span class="date">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>
                    {{ formatDateTime(pred.date) }}
                  </span>
                </div>
              </div>
            } @empty {
              <div class="empty-state">
                <div class="empty-icon">📊</div>
                <h3>No Predictions</h3>
                <p>Generate AI predictions to see your risk history.</p>
                <a routerLink="/predictions" class="btn btn-primary">View Predictions</a>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .history-page { min-height: 100vh; background: linear-gradient(180deg, #F8FAFC 0%, #F1F5F9 100%); }
    .page-header {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: white;
      border-bottom: 1px solid var(--gray-200);
      h1 { flex: 1; font-size: 20px; font-weight: 700; }
    }
    .back-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: var(--gray-100);
      border-radius: 12px;
      color: var(--gray-600);
      transition: all 0.2s;
      &:hover { background: var(--gray-200); }
    }
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }
    
    .tabs {
      display: flex;
      background: white;
      border-radius: 16px;
      padding: 6px;
      margin-bottom: 24px;
      box-shadow: var(--shadow-sm);
    }
    .tab {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 12px 16px;
      border: none;
      background: transparent;
      border-radius: 12px;
      font-weight: 600;
      font-size: 13px;
      color: var(--gray-500);
      cursor: pointer;
      transition: all 0.2s;
      &.active { 
        background: var(--primary); 
        color: white;
        box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
      }
      &:hover:not(.active) { background: var(--gray-100); }
    }
    
    .stats-mini {
      display: flex;
      gap: 16px;
      margin-bottom: 20px;
      padding: 16px;
      background: white;
      border-radius: 16px;
      box-shadow: var(--shadow-sm);
    }
    .stat-item {
      flex: 1;
      text-align: center;
      .stat-value {
        display: block;
        font-size: 24px;
        font-weight: 700;
        color: var(--primary);
      }
      .stat-label {
        font-size: 12px;
        color: var(--gray-500);
      }
    }
    
    .history-card {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      background: white;
      border-radius: 16px;
      padding: 20px;
      margin-bottom: 12px;
      box-shadow: var(--shadow-sm);
      transition: all 0.2s;
      &:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
      &.high-risk { border-left: 4px solid var(--error); }
    }
    .card-icon {
      width: 44px;
      height: 44px;
      background: var(--gray-100);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      flex-shrink: 0;
      &.tests { background: rgba(99, 102, 241, 0.1); }
      &.good { background: rgba(16, 185, 129, 0.1); }
      &.medium { background: rgba(245, 158, 11, 0.1); }
      &.low { background: rgba(239, 68, 68, 0.1); }
      &.high { background: rgba(239, 68, 68, 0.1); }
    }
    .card-body { flex: 1; min-width: 0; }
    .card-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 8px;
      h3 { font-size: 15px; font-weight: 600; }
    }
    .score {
      padding: 4px 12px;
      background: rgba(99, 102, 241, 0.1);
      color: var(--primary);
      border-radius: 20px;
      font-weight: 700;
      font-size: 13px;
      &.adherence.good { background: rgba(16, 185, 129, 0.1); color: var(--success); }
      &.adherence.medium { background: rgba(245, 158, 11, 0.1); color: #D97706; }
      &.adherence.low { background: rgba(239, 68, 68, 0.1); color: var(--error); }
      &.prediction { background: rgba(59, 130, 246, 0.1); color: #2563EB; }
      &.prediction.high { background: rgba(239, 68, 68, 0.1); color: var(--error); }
    }
    .interpretation { color: var(--gray-600); font-size: 13px; margin-bottom: 8px; }
    .date { 
      display: flex; align-items: center; gap: 4px;
      font-size: 12px; color: var(--gray-400); 
    }
    
    .progress-mini { margin-bottom: 8px; }
    .progress-bar {
      height: 6px;
      background: var(--gray-200);
      border-radius: 3px;
      overflow: hidden;
      .fill {
        height: 100%;
        background: var(--primary);
        border-radius: 3px;
        transition: width 0.3s;
        &.good { background: var(--success); }
      }
    }
    .doses-info { font-size: 13px; color: var(--gray-600); }
    
    .risk-meter {
      height: 6px;
      background: var(--gray-200);
      border-radius: 3px;
      overflow: hidden;
      margin-bottom: 8px;
      .meter-fill {
        height: 100%;
        background: var(--success);
        border-radius: 3px;
        transition: width 0.3s;
        &.high { background: linear-gradient(90deg, #F59E0B, var(--error)); }
      }
    }
    
    .empty-state {
      text-align: center;
      padding: 48px 24px;
      background: white;
      border-radius: 20px;
      box-shadow: var(--shadow-sm);
      .empty-icon { font-size: 48px; margin-bottom: 16px; }
      h3 { margin-bottom: 8px; color: var(--gray-800); }
      p { color: var(--gray-500); margin-bottom: 20px; }
    }
    
    .animate-in {
      animation: slideIn 0.3s ease-out forwards;
      opacity: 0;
    }
    @keyframes slideIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class HistoryComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  activeTab = signal<'tests' | 'adherence' | 'predictions'>('tests');
  testHistory = signal<TestResult[]>([]);
  adherenceHistory = signal<AdherenceSummary[]>([]);
  predictions = signal<Prediction[]>([]);

  // Sorted computed values
  sortedTestHistory = computed(() => 
    [...this.testHistory()].sort((a, b) => 
      new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime()
    )
  );

  sortedAdherenceHistory = computed(() => 
    [...this.adherenceHistory()].sort((a, b) => 
      new Date(b.date).getTime() - new Date(a.date).getTime()
    )
  );

  sortedPredictions = computed(() => 
    [...this.predictions()].sort((a, b) => 
      new Date(b.date).getTime() - new Date(a.date).getTime()
    )
  );

  averageAdherence = computed(() => {
    const history = this.adherenceHistory();
    if (history.length === 0) return 0;
    return history.reduce((acc, h) => acc + h.adherenceScore, 0) / history.length;
  });

  averageRisk = computed(() => {
    const preds = this.predictions();
    if (preds.length === 0) return 0;
    return (preds.reduce((acc, p) => acc + p.probNonAdherence, 0) / preds.length) * 100;
  });

  ngOnInit(): void {
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getTestHistory(userId).subscribe(h => this.testHistory.set(h));
      
      const today = new Date();
      const from = new Date(today.getFullYear(), today.getMonth() - 1, 1).toISOString().split('T')[0];
      const to = today.toISOString().split('T')[0];
      this.api.getAdherenceHistory(userId, from, to).subscribe(h => this.adherenceHistory.set(h));
      
      this.api.getPredictions(userId).subscribe(p => this.predictions.set(p));
    }
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  formatDateTime(date: string): string {
    return new Date(date).toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getScoreClass(level: string): string {
    const lower = level.toLowerCase();
    if (lower.includes('minimal') || lower.includes('low') || lower.includes('normal')) return 'good';
    if (lower.includes('mild') || lower.includes('moderate')) return 'medium';
    if (lower.includes('severe') || lower.includes('high')) return 'low';
    return '';
  }
}

