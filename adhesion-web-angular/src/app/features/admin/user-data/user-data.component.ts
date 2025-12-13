import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ApiService, User, TestResult, TreatmentPlan } from '../../../core/services/api.service';

@Component({
  selector: 'app-user-data',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="admin-page">
      <header class="page-header">
        <a routerLink="/admin/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>User Data</h1>
      </header>

      <div class="content">
        @if (!selectedUser()) {
          <div class="users-list">
            @for (user of users(); track user.id) {
              <div class="user-card" (click)="selectUser(user)">
                <div class="user-avatar">{{ user.displayName?.charAt(0) || 'U' }}</div>
                <div class="user-info">
                  <span class="name">{{ user.displayName }}</span>
                  <span class="email">{{ user.email }}</span>
                </div>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8-8-8z"/></svg>
              </div>
            } @empty {
              <div class="empty-state">
                <p>No consented users found</p>
              </div>
            }
          </div>
        } @else {
          <div class="user-details">
            <button class="back-link" (click)="selectedUser.set(null)">← Back to list</button>
            
            <div class="user-header">
              <div class="avatar-large">{{ selectedUser()?.displayName?.charAt(0) }}</div>
              <div class="user-meta">
                <h2>{{ selectedUser()?.displayName }}</h2>
                <p>{{ selectedUser()?.email }}</p>
              </div>
            </div>

            <div class="tabs">
              <button class="tab" [class.active]="activeTab() === 'tests'" (click)="activeTab.set('tests')">Test Results</button>
              <button class="tab" [class.active]="activeTab() === 'plans'" (click)="activeTab.set('plans')">Treatment Plans</button>
            </div>

            @if (activeTab() === 'tests') {
              @for (result of testResults(); track result.sessionId) {
                <div class="result-card">
                  <div class="result-header">
                    <h3>{{ result.testTitle }}</h3>
                    <span class="score">{{ result.totalScore }}</span>
                  </div>
                  <p>{{ result.interpretationLevel }}</p>
                  <span class="date">{{ formatDate(result.submittedAt) }}</span>
                </div>
              } @empty {
                <div class="empty-state">No test results</div>
              }
            } @else {
              @for (plan of plans(); track plan.id) {
                <div class="plan-card">
                  <h3>{{ plan.title }}</h3>
                  <p>{{ plan.description }}</p>
                  <div class="plan-meta">
                    <span class="status" [class]="plan.status?.toLowerCase()">{{ plan.status }}</span>
                    <span>{{ plan.progressPercentage }}% complete</span>
                  </div>
                </div>
              } @empty {
                <div class="empty-state">No treatment plans</div>
              }
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .admin-page { min-height: 100vh; background: var(--dark-bg); }
    .page-header {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: var(--dark-surface);
      border-bottom: 1px solid var(--dark-border);
      h1 { flex: 1; font-size: 20px; color: white; }
    }
    .back-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: rgba(255,255,255,0.1);
      border-radius: 10px;
      color: white;
    }
    .content { max-width: 800px; margin: 0 auto; padding: 24px; }
    .users-list { display: flex; flex-direction: column; gap: 12px; }
    .user-card {
      display: flex; align-items: center; gap: 14px;
      padding: 16px;
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s;
      color: rgba(255,255,255,0.5);
      &:hover { border-color: rgba(255,255,255,0.2); }
    }
    .user-avatar {
      width: 44px; height: 44px;
      background: rgba(99, 102, 241, 0.2);
      border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
      color: var(--primary);
      font-weight: 700;
    }
    .user-info { flex: 1; }
    .name { display: block; color: white; font-weight: 600; }
    .email { color: rgba(255,255,255,0.5); font-size: 13px; }
    .back-link {
      background: none; border: none;
      color: rgba(255,255,255,0.6);
      cursor: pointer;
      margin-bottom: 20px;
      &:hover { color: white; }
    }
    .user-header {
      display: flex; align-items: center; gap: 16px;
      margin-bottom: 24px;
    }
    .avatar-large {
      width: 64px; height: 64px;
      background: rgba(99, 102, 241, 0.2);
      border-radius: 16px;
      display: flex; align-items: center; justify-content: center;
      color: var(--primary);
      font-size: 28px;
      font-weight: 700;
    }
    .user-meta h2 { color: white; margin-bottom: 4px; }
    .user-meta p { color: rgba(255,255,255,0.5); }
    .tabs {
      display: flex;
      background: var(--dark-surface);
      border-radius: 12px;
      padding: 4px;
      margin-bottom: 24px;
    }
    .tab {
      flex: 1; padding: 12px;
      border: none; background: transparent;
      border-radius: 10px;
      font-weight: 600;
      color: rgba(255,255,255,0.5);
      cursor: pointer;
      &.active { background: var(--admin-primary); color: white; }
    }
    .result-card, .plan-card {
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 14px;
      padding: 20px;
      margin-bottom: 12px;
    }
    .result-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 8px;
      h3 { color: white; font-size: 16px; }
    }
    .score {
      padding: 4px 12px;
      background: rgba(99, 102, 241, 0.2);
      color: var(--primary);
      border-radius: 20px;
      font-weight: 700;
    }
    .result-card p, .plan-card p { color: rgba(255,255,255,0.6); margin-bottom: 8px; }
    .date { font-size: 13px; color: rgba(255,255,255,0.4); }
    .plan-card h3 { color: white; margin-bottom: 8px; }
    .plan-meta {
      display: flex; justify-content: space-between; align-items: center;
      font-size: 13px;
      color: rgba(255,255,255,0.5);
    }
    .status {
      padding: 4px 10px;
      border-radius: 20px;
      font-weight: 600;
      font-size: 11px;
      &.active { background: rgba(16, 185, 129, 0.2); color: var(--success); }
    }
    .empty-state { text-align: center; padding: 48px; color: rgba(255,255,255,0.5); }
  `]
})
export class UserDataComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);

  users = signal<User[]>([]);
  selectedUser = signal<User | null>(null);
  testResults = signal<TestResult[]>([]);
  plans = signal<TreatmentPlan[]>([]);
  activeTab = signal<'tests' | 'plans'>('tests');

  ngOnInit(): void {
    this.api.getConsentedUsers().subscribe(u => this.users.set(u));
    const userId = this.route.snapshot.queryParams['userId'];
    if (userId) {
      this.api.getConsentedUsers().subscribe(users => {
        const user = users.find(u => u.id === +userId);
        if (user) this.selectUser(user);
      });
    }
  }

  selectUser(user: User): void {
    this.selectedUser.set(user);
    this.api.getUserTestResults(user.id).subscribe(r => this.testResults.set(r));
    this.api.getUserTreatmentPlansAdmin(user.id).subscribe(p => this.plans.set(p));
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }
}

