import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, TestResult, AdherenceSummary } from '../../core/services/api.service';
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
        <div class="tabs">
          <button class="tab" [class.active]="activeTab() === 'tests'" (click)="activeTab.set('tests')">Test History</button>
          <button class="tab" [class.active]="activeTab() === 'adherence'" (click)="activeTab.set('adherence')">Adherence</button>
        </div>

        @if (activeTab() === 'tests') {
          @for (result of testHistory(); track result.sessionId) {
            <div class="history-card">
              <div class="card-header">
                <h3>{{ result.testTitle }}</h3>
                <span class="score">{{ result.totalScore }}</span>
              </div>
              <p>{{ result.interpretationLevel }}</p>
              <span class="date">{{ formatDate(result.submittedAt) }}</span>
            </div>
          } @empty {
            <div class="empty-state">
              <p>No test history available</p>
            </div>
          }
        } @else {
          @for (summary of adherenceHistory(); track summary.date) {
            <div class="history-card adherence">
              <div class="card-header">
                <h3>{{ formatDate(summary.date) }}</h3>
                <span class="score">{{ summary.adherenceScore }}%</span>
              </div>
              <div class="doses-info">
                <span>{{ summary.dosesTaken }} / {{ summary.dosesTotal }} doses taken</span>
              </div>
            </div>
          } @empty {
            <div class="empty-state">
              <p>No adherence history available</p>
            </div>
          }
        }
      </div>
    </div>
  `,
  styles: [`
    .history-page { min-height: 100vh; background: var(--gray-50); }
    .page-header {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: white;
      border-bottom: 1px solid var(--gray-200);
      h1 { flex: 1; font-size: 20px; }
    }
    .back-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: var(--gray-100);
      border-radius: 10px;
      color: var(--gray-600);
    }
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }
    .tabs {
      display: flex;
      background: white;
      border-radius: 12px;
      padding: 4px;
      margin-bottom: 24px;
    }
    .tab {
      flex: 1;
      padding: 12px;
      border: none;
      background: transparent;
      border-radius: 10px;
      font-weight: 600;
      color: var(--gray-500);
      cursor: pointer;
      &.active { background: var(--primary); color: white; }
    }
    .history-card {
      background: white;
      border-radius: 14px;
      padding: 20px;
      margin-bottom: 12px;
      box-shadow: var(--shadow-sm);
    }
    .card-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 8px;
      h3 { font-size: 16px; }
    }
    .score {
      padding: 4px 12px;
      background: rgba(99, 102, 241, 0.1);
      color: var(--primary);
      border-radius: 20px;
      font-weight: 700;
    }
    .history-card p { color: var(--gray-600); margin-bottom: 8px; }
    .date { font-size: 13px; color: var(--gray-400); }
    .doses-info { color: var(--gray-600); font-size: 14px; }
    .empty-state {
      text-align: center;
      padding: 48px;
      color: var(--gray-500);
    }
  `]
})
export class HistoryComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  activeTab = signal<'tests' | 'adherence'>('tests');
  testHistory = signal<TestResult[]>([]);
  adherenceHistory = signal<AdherenceSummary[]>([]);

  ngOnInit(): void {
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getTestHistory(userId).subscribe(h => this.testHistory.set(h));
      const today = new Date();
      const from = new Date(today.getFullYear(), today.getMonth() - 1, 1).toISOString().split('T')[0];
      const to = today.toISOString().split('T')[0];
      this.api.getAdherenceHistory(userId, from, to).subscribe(h => this.adherenceHistory.set(h));
    }
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }
}

