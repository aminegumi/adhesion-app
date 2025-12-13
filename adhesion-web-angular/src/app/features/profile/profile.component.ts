import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="profile-page">
      <header class="page-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>My Profile</h1>
        <a routerLink="/edit-profile" class="edit-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
        </a>
      </header>

      <div class="profile-content">
        <div class="profile-card">
          <div class="avatar-section">
            <div class="avatar">{{ user()?.displayName?.charAt(0) || 'U' }}</div>
            <h2>{{ user()?.displayName || 'User' }}</h2>
            <p>{{ user()?.email }}</p>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">Gender</span>
              <span class="info-value">{{ user()?.gender || 'Not set' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Birth Date</span>
              <span class="info-value">{{ user()?.birthDate || 'Not set' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Account Status</span>
              <span class="info-value status active">Active</span>
            </div>
            <div class="info-item">
              <span class="info-label">Data Consent</span>
              <span class="info-value" [class.granted]="user()?.consentGiven">
                {{ user()?.consentGiven ? 'Granted' : 'Not Granted' }}
              </span>
            </div>
          </div>
        </div>

        <div class="stats-section">
          <h3>Your Activity</h3>
          <div class="stats-grid">
            <div class="stat-card">
              <span class="stat-value">{{ testsCompleted() }}</span>
              <span class="stat-label">Tests Completed</span>
            </div>
            <div class="stat-card">
              <span class="stat-value">{{ activePlans() }}</span>
              <span class="stat-label">Active Plans</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .profile-page { min-height: 100vh; background: var(--gray-50); }
    .page-header {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px 24px;
      background: white;
      border-bottom: 1px solid var(--gray-200);
      h1 { flex: 1; font-size: 20px; }
    }
    .back-btn, .edit-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: var(--gray-100);
      border-radius: 10px;
      color: var(--gray-600);
      &:hover { background: var(--gray-200); }
    }
    .profile-content { max-width: 600px; margin: 0 auto; padding: 24px; }
    .profile-card {
      background: white;
      border-radius: 20px;
      padding: 32px;
      box-shadow: var(--shadow-md);
      margin-bottom: 24px;
    }
    .avatar-section { text-align: center; margin-bottom: 32px; }
    .avatar {
      width: 80px; height: 80px;
      background: var(--primary-gradient);
      border-radius: 20px;
      display: flex; align-items: center; justify-content: center;
      color: white;
      font-size: 32px;
      font-weight: 700;
      margin: 0 auto 16px;
    }
    .avatar-section h2 { font-size: 24px; margin-bottom: 4px; }
    .avatar-section p { color: var(--gray-500); }
    .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .info-item {
      padding: 16px;
      background: var(--gray-50);
      border-radius: 12px;
    }
    .info-label { display: block; font-size: 12px; color: var(--gray-500); margin-bottom: 4px; }
    .info-value { font-weight: 600; color: var(--gray-800); }
    .info-value.active { color: var(--success); }
    .info-value.granted { color: var(--success); }
    .stats-section h3 { font-size: 18px; margin-bottom: 16px; }
    .stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .stat-card {
      background: white;
      border-radius: 16px;
      padding: 24px;
      text-align: center;
      box-shadow: var(--shadow-sm);
    }
    .stat-value { display: block; font-size: 32px; font-weight: 800; color: var(--primary); }
    .stat-label { color: var(--gray-500); font-size: 14px; }
  `]
})
export class ProfileComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  user = this.auth.user;
  testsCompleted = signal(0);
  activePlans = signal(0);

  ngOnInit(): void {
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getTestHistory(userId).subscribe(tests => this.testsCompleted.set(tests.length));
      this.api.getTreatmentPlans(userId).subscribe(plans => this.activePlans.set(plans.filter(p => p.status === 'ACTIVE').length));
    }
  }
}

