import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, Prediction } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-predictions',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="predictions-page">
      <header class="page-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>AI Predictions</h1>
        <button class="refresh-btn" (click)="generatePrediction()" [disabled]="generating()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M17.65 6.35A7.958 7.958 0 0012 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0112 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
        </button>
      </header>

      <div class="content">
        @if (predictions().length > 0) {
          @for (pred of predictions(); track pred.id) {
            <div class="prediction-card" [class.high-risk]="pred.probNonAdherence > 0.5">
              <div class="risk-meter">
                <div class="meter-fill" [style.width.%]="pred.probNonAdherence * 100"></div>
              </div>
              <div class="prediction-content">
                <div class="risk-value">{{ (pred.probNonAdherence * 100).toFixed(1) }}%</div>
                <div class="risk-label">Non-adherence Risk</div>
                <span class="date">{{ formatDate(pred.date) }}</span>
              </div>
              <div class="risk-badge" [class.high]="pred.probNonAdherence > 0.5">
                {{ pred.probNonAdherence > 0.5 ? 'High Risk' : 'Low Risk' }}
              </div>
            </div>
          }
        } @else {
          <div class="empty-state">
            <div class="icon">📊</div>
            <h3>No Predictions Yet</h3>
            <p>Generate your first adherence prediction to see your risk analysis.</p>
            <button class="btn btn-primary" (click)="generatePrediction()" [disabled]="generating()">
              @if (generating()) { Generating... } @else { Generate Prediction }
            </button>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .predictions-page { min-height: 100vh; background: linear-gradient(180deg, #F0F9FF 0%, #E0F2FE 100%); }
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
      &:hover { background: var(--gray-200); }
    }
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }
    .prediction-card {
      background: white;
      border-radius: 20px;
      padding: 24px;
      box-shadow: var(--shadow-md);
      margin-bottom: 16px;
      &.high-risk { border-left: 4px solid var(--error); }
    }
    .risk-meter {
      height: 8px; background: var(--gray-200); border-radius: 4px; overflow: hidden; margin-bottom: 16px;
      .meter-fill { height: 100%; background: linear-gradient(90deg, var(--success), var(--warning), var(--error)); }
    }
    .prediction-content { display: flex; align-items: center; gap: 16px; }
    .risk-value { font-size: 32px; font-weight: 800; color: var(--gray-900); }
    .risk-label { flex: 1; font-weight: 600; color: var(--gray-600); }
    .date { font-size: 13px; color: var(--gray-400); }
    .risk-badge {
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      background: rgba(16, 185, 129, 0.1);
      color: var(--success);
      &.high { background: rgba(239, 68, 68, 0.1); color: var(--error); }
    }
    .empty-state {
      text-align: center;
      padding: 60px 24px;
      background: white;
      border-radius: 24px;
      .icon { font-size: 64px; margin-bottom: 16px; }
      h3 { margin-bottom: 8px; }
      p { color: var(--gray-600); margin-bottom: 24px; }
    }
  `]
})
export class PredictionsComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  predictions = signal<Prediction[]>([]);
  generating = signal(false);

  ngOnInit(): void { this.loadPredictions(); }

  loadPredictions(): void {
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getPredictions(userId).subscribe(p => this.predictions.set(p));
    }
  }

  generatePrediction(): void {
    const userId = this.auth.getUserId();
    if (!userId) return;
    this.generating.set(true);
    this.api.generatePrediction(userId).subscribe({
      next: () => { this.loadPredictions(); this.generating.set(false); },
      error: () => this.generating.set(false)
    });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }
}

