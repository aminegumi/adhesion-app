import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
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
        @if (recommendations()) {
          <div class="rec-card">
            <div class="rec-header">
              <span class="icon">💡</span>
              <h2>AI Recommendations</h2>
            </div>
            <p>{{ recommendations() }}</p>
          </div>
        }

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
    }
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }
    .rec-card {
      background: white;
      border-radius: 20px;
      padding: 24px;
      margin-bottom: 24px;
      box-shadow: var(--shadow-lg);
    }
    .rec-header {
      display: flex; align-items: center; gap: 12px; margin-bottom: 16px;
      .icon { font-size: 28px; }
      h2 { font-size: 18px; }
    }
    .rec-card p { color: var(--gray-600); line-height: 1.7; }
    .tips-grid { display: grid; gap: 16px; }
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

  recommendations = signal<string | null>(null);
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
      this.api.getRecommendations(userId).subscribe(r => this.recommendations.set(r?.summary));
    }
  }

  generate(): void {
    const userId = this.auth.getUserId();
    if (!userId) return;
    this.generating.set(true);
    this.api.generateRecommendations(userId).subscribe({
      next: (r) => { this.recommendations.set(r?.summary); this.generating.set(false); },
      error: () => this.generating.set(false)
    });
  }
}

