import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, Test, TestResult } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-tests',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="tests-page">
      <header class="page-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>Psychological Tests</h1>
      </header>

      <div class="content">
        <!-- Tabs -->
        <div class="tabs">
          <button class="tab" [class.active]="activeTab() === 'available'" (click)="activeTab.set('available')">Available Tests</button>
          <button class="tab" [class.active]="activeTab() === 'history'" (click)="activeTab.set('history')">History</button>
        </div>

        @if (activeTab() === 'available') {
          <div class="tests-grid">
            @for (test of tests(); track test.id) {
              <div class="test-card">
                <div class="test-header">
                  <h3>{{ test.title }}</h3>
                  <span class="badge">{{ test.code }}</span>
                </div>
                <p>{{ test.description }}</p>
                <a [routerLink]="['/test', test.id]" class="btn btn-primary">Take Test</a>
              </div>
            } @empty {
              <div class="empty-state">
                <p>No tests available</p>
              </div>
            }
          </div>
        } @else {
          <div class="history-list">
            @for (result of history(); track result.sessionId) {
              <div class="history-card">
                <div class="result-header">
                  <h3>{{ result.testTitle }}</h3>
                  <span class="score-badge" [class]="getScoreClass(result.totalScore)">{{ result.totalScore }}</span>
                </div>
                <p class="interpretation">{{ result.interpretationLevel }}</p>
                <span class="date">{{ formatDate(result.submittedAt) }}</span>
              </div>
            } @empty {
              <div class="empty-state">
                <p>No test history yet</p>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .tests-page { min-height: 100vh; background: var(--gray-50); }
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
    .content { max-width: 800px; margin: 0 auto; padding: 24px; }
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
      transition: all 0.2s;
      &.active {
        background: var(--primary);
        color: white;
      }
    }
    .tests-grid { display: flex; flex-direction: column; gap: 16px; }
    .test-card {
      background: white;
      border-radius: 16px;
      padding: 24px;
      box-shadow: var(--shadow-sm);
    }
    .test-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      h3 { font-size: 18px; }
    }
    .badge {
      padding: 4px 10px;
      background: rgba(99, 102, 241, 0.1);
      color: var(--primary);
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
    }
    .test-card p { color: var(--gray-600); margin-bottom: 16px; line-height: 1.5; }
    .history-list { display: flex; flex-direction: column; gap: 12px; }
    .history-card {
      background: white;
      border-radius: 14px;
      padding: 20px;
      box-shadow: var(--shadow-sm);
    }
    .result-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }
    .score-badge {
      padding: 6px 12px;
      border-radius: 20px;
      font-weight: 700;
      &.low { background: rgba(16, 185, 129, 0.1); color: var(--success); }
      &.medium { background: rgba(245, 158, 11, 0.1); color: var(--warning); }
      &.high { background: rgba(239, 68, 68, 0.1); color: var(--error); }
    }
    .interpretation { color: var(--gray-600); margin-bottom: 8px; }
    .date { font-size: 13px; color: var(--gray-400); }
    .empty-state {
      text-align: center;
      padding: 48px;
      color: var(--gray-500);
    }
  `]
})
export class TestsComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  tests = signal<Test[]>([]);
  history = signal<TestResult[]>([]);
  activeTab = signal<'available' | 'history'>('available');

  ngOnInit(): void {
    this.api.getTests().subscribe(t => this.tests.set(t));
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getTestHistory(userId).subscribe(h => this.history.set(h));
    }
  }

  getScoreClass(score: number): string {
    if (score < 10) return 'low';
    if (score < 20) return 'medium';
    return 'high';
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric'
    });
  }
}
