import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, Recommendation } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-recommendations',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="recs-page">
      <header class="page-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>Tips & Recommendations</h1>
        <button class="refresh-btn" (click)="generate()" [disabled]="generating()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M17.65 6.35A7.958 7.958 0 0012 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0112 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
        </button>
      </header>

      <div class="content">
        <!-- Generate button when no recommendations -->
        @if (recommendations().length === 0 && !generating()) {
          <div class="generate-section">
            <div class="generate-card">
              <div class="generate-icon">💡</div>
              <h2>No Recommendations Yet</h2>
              <p>Generate personalized AI recommendations based on your psychological profile</p>
              <button class="generate-btn" (click)="generate()">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19 8l-4 4h3c0 3.31-2.69 6-6 6-1.01 0-1.97-.25-2.8-.7l-1.46 1.46C8.97 19.54 10.43 20 12 20c4.42 0 8-3.58 8-8h3l-4-4zM6 12c0-3.31 2.69-6 6-6 1.01 0 1.97.25 2.8.7l1.46-1.46C15.03 4.46 13.57 4 12 4c-4.42 0-8 3.58-8 8H1l4 4 4-4H6z"/></svg>
                Generate AI Recommendations
              </button>
            </div>
          </div>
        }

        @if (generating()) {
          <div class="loading-state">
            <div class="spinner-large"></div>
            <p>Generating personalized recommendations...</p>
          </div>
        }

        @if (recommendations().length > 0) {
          <div class="rec-list">
            <div class="list-header">
              <h2 class="section-title">
                <span class="icon">💡</span>
                AI Recommendations
              </h2>
              <button class="regenerate-btn" (click)="generate()" [disabled]="generating()">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M17.65 6.35A7.958 7.958 0 0012 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0112 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
                Regenerate
              </button>
            </div>
            @for (rec of recommendations(); track rec.id) {
              <div class="rec-card" [class.completed]="rec.completed">
                <div class="rec-header">
                  <span class="priority-badge" [class]="getPriorityClass(rec.priority)">{{ getPriorityLabel(rec.priority) }}</span>
                  <span class="rec-type">{{ rec.category }}</span>
                </div>
                <h3>{{ rec.title }}</h3>
                <p>{{ cleanDescription(rec.description || '') }}</p>
                @if (!rec.completed) {
                  <button class="complete-btn" (click)="completeRec(rec)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                    Mark Complete
                  </button>
                } @else {
                  <div class="completed-badge">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
                    Completed
                  </div>
                }
              </div>
            }
          </div>
        }

        <div class="tips-section">
          <h2 class="section-title">
            <span class="icon">✨</span>
            Daily Wellness Tips
          </h2>
          <div class="tips-grid">
            @for (tip of tips; track tip.title) {
              <div class="tip-card">
                <span class="tip-icon">{{ tip.icon }}</span>
                <h3>{{ tip.title }}</h3>
                <p>{{ tip.content }}</p>
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .recs-page { min-height: 100vh; background: linear-gradient(180deg, #FFFBEB 0%, #FEF3C7 100%); }
    .page-header {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: white;
      border-bottom: 1px solid var(--gray-200);
      h1 { flex: 1; font-size: 20px; }
    }
    .back-btn, .refresh-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: var(--gray-100);
      border: none;
      border-radius: 10px;
      color: var(--gray-600);
      cursor: pointer;
      transition: all 0.2s;
      &:hover { background: var(--gray-200); }
    }
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }

    .generate-section {
      margin-bottom: 32px;
    }

    .generate-card {
      background: white;
      border-radius: 24px;
      padding: 48px 32px;
      text-align: center;
      box-shadow: var(--shadow-md);

      .generate-icon {
        font-size: 64px;
        margin-bottom: 16px;
      }

      h2 {
        font-size: 22px;
        margin-bottom: 12px;
        color: var(--gray-800);
      }

      p {
        color: var(--gray-600);
        margin-bottom: 24px;
        line-height: 1.6;
      }
    }

    .generate-btn {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      background: linear-gradient(135deg, var(--success), #34D399);
      color: white;
      border: none;
      padding: 16px 32px;
      border-radius: 16px;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      box-shadow: 0 8px 24px rgba(16, 185, 129, 0.3);

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 12px 32px rgba(16, 185, 129, 0.4);
      }
    }

    .loading-state {
      text-align: center;
      padding: 60px;
      
      .spinner-large {
        width: 48px;
        height: 48px;
        border: 3px solid rgba(16, 185, 129, 0.2);
        border-top-color: var(--success);
        border-radius: 50%;
        margin: 0 auto 20px;
        animation: spin 0.8s linear infinite;
      }

      p {
        color: var(--gray-600);
      }
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .regenerate-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 14px;
      background: rgba(16, 185, 129, 0.1);
      color: var(--success);
      border: none;
      border-radius: 10px;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;

      &:hover { background: rgba(16, 185, 129, 0.2); }
      &:disabled { opacity: 0.5; cursor: not-allowed; }
    }
    
    .section-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 18px;
      margin-bottom: 16px;
      .icon { font-size: 24px; }
    }

    .rec-list { margin-bottom: 32px; }

    .rec-card {
      background: white;
      border-radius: 16px;
      padding: 20px;
      margin-bottom: 12px;
      box-shadow: var(--shadow-sm);

      &.completed {
        opacity: 0.7;
        background: var(--gray-50);
      }
    }

    .rec-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
    }

    .priority-badge {
      font-size: 11px;
      font-weight: 600;
      padding: 4px 10px;
      border-radius: 20px;
      text-transform: uppercase;

      &.high { background: #FEE2E2; color: #DC2626; }
      &.medium { background: #FEF3C7; color: #D97706; }
      &.low { background: #D1FAE5; color: #059669; }
    }

    .rec-type {
      font-size: 12px;
      color: var(--gray-500);
    }

    .rec-card h3 {
      font-size: 16px;
      margin-bottom: 8px;
    }

    .rec-card p {
      color: var(--gray-600);
      font-size: 14px;
      line-height: 1.6;
      margin-bottom: 12px;
    }

    .complete-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      background: linear-gradient(135deg, var(--primary), #8B5CF6);
      color: white;
      border: none;
      padding: 8px 16px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
      &:hover { transform: scale(1.02); }
    }

    .completed-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: var(--success);
      font-size: 13px;
      font-weight: 600;
      padding: 6px 12px;
      background: rgba(16, 185, 129, 0.1);
      border-radius: 20px;
    }

    .tips-section { margin-top: 24px; }
    .tips-grid { display: grid; gap: 12px; }
    .tip-card {
      background: white;
      border-radius: 16px;
      padding: 20px;
      box-shadow: var(--shadow-sm);
    }
    .tip-icon { font-size: 24px; margin-bottom: 12px; display: block; }
    .tip-card h3 { font-size: 16px; margin-bottom: 8px; }
    .tip-card p { color: var(--gray-600); font-size: 14px; line-height: 1.5; }
  `]
})
export class RecommendationsComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  recommendations = signal<Recommendation[]>([]);
  generating = signal(false);

  tips = [
    { icon: '🧘', title: 'Practice Mindfulness', content: 'Take 5 minutes each day to practice deep breathing and meditation.' },
    { icon: '💤', title: 'Quality Sleep', content: 'Aim for 7-9 hours of sleep and maintain a consistent sleep schedule.' },
    { icon: '🥗', title: 'Balanced Nutrition', content: 'Eat a variety of fruits, vegetables, and whole grains daily.' },
    { icon: '🏃', title: 'Stay Active', content: '30 minutes of moderate exercise can boost your mood significantly.' },
    { icon: '💧', title: 'Stay Hydrated', content: 'Drink at least 8 glasses of water daily for optimal health.' },
    { icon: '📵', title: 'Digital Detox', content: 'Take regular breaks from screens to reduce stress and anxiety.' },
  ];

  ngOnInit(): void {
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getRecommendations(userId).subscribe({
        next: (r) => {
          this.recommendations.set(r);
          // Auto-generate if no recommendations exist
          if (r.length === 0) {
            this.generate();
          }
        },
        error: (err) => {
          console.error('Failed to load recommendations:', err);
          // Try to generate on error
          this.generate();
        }
      });
    }
  }

  generate(): void {
    const userId = this.auth.getUserId();
    if (!userId) return;
    this.generating.set(true);
    this.api.generateRecommendations(userId).subscribe({
      next: (r) => { this.recommendations.set(r); this.generating.set(false); },
      error: () => this.generating.set(false)
    });
  }

  completeRec(rec: Recommendation): void {
    this.api.completeRecommendation(rec.id).subscribe({
      next: (updated) => {
        this.recommendations.update(recs => 
          recs.map(r => r.id === updated.id ? updated : r)
        );
      },
      error: (err) => console.error('Failed to complete recommendation:', err)
    });
  }

  cleanDescription(text: string): string {
    if (!text) return '';
    // Clean markdown formatting like the Flutter app does
    let cleaned = text;
    cleaned = cleaned.replace(/^#{1,6}\s*/gm, '');
    cleaned = cleaned.replace(/\*\*(.+?)\*\*/g, '$1');
    cleaned = cleaned.replace(/\*(.+?)\*/g, '$1');
    cleaned = cleaned.replace(/__(.+?)__/g, '$1');
    cleaned = cleaned.replace(/_(.+?)_/g, '$1');
    cleaned = cleaned.replace(/^\s*[-*+]\s+/gm, '• ');
    cleaned = cleaned.replace(/\n{3,}/g, '\n\n');
    return cleaned.trim();
  }

  getPriorityClass(priority: number): string {
    if (priority <= 2) return 'high';
    if (priority <= 4) return 'medium';
    return 'low';
  }

  getPriorityLabel(priority: number): string {
    if (priority <= 2) return 'High';
    if (priority <= 4) return 'Medium';
    return 'Low';
  }
}

