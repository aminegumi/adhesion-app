import { Component, inject, signal, OnInit, computed, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, Prediction } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { interval, Subscription } from 'rxjs';

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
        <div class="header-actions">
          @if (sortedPredictions().length > 0) {
            <button class="clear-all-btn" (click)="clearAllPredictions()" [disabled]="generating()">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
              Clear All
            </button>
          }
          <button class="generate-btn" (click)="generatePrediction()" [disabled]="generating()">
            @if (generating()) {
              <svg class="spinner" width="18" height="18" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" fill="none" stroke-dasharray="60" stroke-linecap="round"/></svg>
            } @else {
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
            }
            {{ generating() ? 'Generating...' : 'New Prediction' }}
          </button>
        </div>
      </header>

      <!-- Stats Summary -->
      @if (sortedPredictions().length > 0) {
        <div class="stats-bar">
          <div class="stat">
            <span class="stat-value">{{ sortedPredictions().length }}</span>
            <span class="stat-label">Total Predictions</span>
          </div>
          <div class="stat">
            <span class="stat-value" [class.high-risk]="latestRisk() > 50">{{ latestRisk().toFixed(0) }}%</span>
            <span class="stat-label">Latest Risk</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ averageRisk().toFixed(0) }}%</span>
            <span class="stat-label">Average Risk</span>
          </div>
        </div>
      }

      <div class="content">
        @if (sortedPredictions().length > 0) {
          <div class="predictions-list">
            @for (pred of sortedPredictions(); track pred.id; let i = $index) {
              <div class="prediction-card animate-in" [class.high-risk]="pred.probNonAdherence > 0.5" [class.latest]="i === 0" [style.animation-delay.ms]="i * 50">
                @if (i === 0) {
                  <div class="latest-badge">Latest</div>
                }
                <div class="card-header">
                  <div class="risk-indicator" [class.high]="pred.probNonAdherence > 0.5" [class.medium]="pred.probNonAdherence > 0.3 && pred.probNonAdherence <= 0.5">
                    <span class="risk-icon">
                      @if (pred.probNonAdherence > 0.5) { ⚠️ }
                      @else if (pred.probNonAdherence > 0.3) { ⚡ }
                      @else { ✅ }
                    </span>
                  </div>
                  <div class="risk-info">
                    <span class="risk-value">{{ (pred.probNonAdherence * 100).toFixed(1) }}%</span>
                    <span class="risk-label">Non-adherence Risk</span>
                  </div>
                  <div class="risk-badge" [class.high]="pred.probNonAdherence > 0.5" [class.medium]="pred.probNonAdherence > 0.3 && pred.probNonAdherence <= 0.5">
                    @if (pred.probNonAdherence > 0.5) { High Risk }
                    @else if (pred.probNonAdherence > 0.3) { Medium Risk }
                    @else { Low Risk }
                  </div>
                </div>
                
                <div class="risk-meter">
                  <div class="meter-fill" [style.width.%]="pred.probNonAdherence * 100" [class.high]="pred.probNonAdherence > 0.5" [class.medium]="pred.probNonAdherence > 0.3 && pred.probNonAdherence <= 0.5"></div>
                </div>
                
                <div class="card-footer">
                  <span class="date">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>
                    {{ formatDateTime(pred.date) }}
                  </span>
                  <div class="card-actions">
                    @if (pred.probNonAdherence > 0.5) {
                      <span class="action-hint">Consider reviewing your treatment plan</span>
                    }
                    <button class="delete-btn" (click)="deletePrediction(pred, $event)" title="Delete prediction">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
                    </button>
                  </div>
                </div>
              </div>
            }
          </div>
        } @else {
          <div class="empty-state">
            <div class="empty-icon">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"/></svg>
            </div>
            <h3>No Predictions Yet</h3>
            <p>Generate your first AI-powered adherence prediction to understand your treatment risk factors.</p>
            <button class="btn btn-primary" (click)="generatePrediction()" [disabled]="generating()">
              @if (generating()) {
                <svg class="spinner" width="18" height="18" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" fill="none" stroke-dasharray="60" stroke-linecap="round"/></svg>
                Generating...
              } @else {
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                Generate First Prediction
              }
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
      h1 { flex: 1; font-size: 20px; font-weight: 700; }
    }
    .back-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: var(--gray-100);
      border: none;
      border-radius: 12px;
      color: var(--gray-600);
      cursor: pointer;
      transition: all 0.2s;
      &:hover { background: var(--gray-200); }
    }
    .generate-btn {
      display: flex; align-items: center; gap: 8px;
      padding: 10px 16px;
      background: linear-gradient(135deg, #3B82F6, #2563EB);
      color: white;
      border: none;
      border-radius: 12px;
      font-weight: 600;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.2s;
      &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3); }
      &:disabled { opacity: 0.6; cursor: not-allowed; }
    }
    .clear-all-btn {
      display: flex; align-items: center; gap: 6px;
      padding: 10px 14px;
      background: rgba(239, 68, 68, 0.1);
      color: var(--error);
      border: 1px solid rgba(239, 68, 68, 0.3);
      border-radius: 12px;
      font-weight: 600;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s;
      &:hover:not(:disabled) { background: var(--error); color: white; }
      &:disabled { opacity: 0.6; cursor: not-allowed; }
    }
    
    .stats-bar {
      display: flex;
      background: white;
      padding: 20px 24px;
      gap: 24px;
      border-bottom: 1px solid var(--gray-200);
    }
    .stat {
      text-align: center;
      flex: 1;
      .stat-value {
        display: block;
        font-size: 28px;
        font-weight: 700;
        color: var(--primary);
        &.high-risk { color: var(--error); }
      }
      .stat-label {
        font-size: 12px;
        color: var(--gray-500);
        margin-top: 4px;
      }
    }
    
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }
    
    .prediction-card {
      background: white;
      border-radius: 20px;
      padding: 24px;
      box-shadow: var(--shadow-md);
      margin-bottom: 16px;
      position: relative;
      overflow: hidden;
      transition: all 0.2s;
      &:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }
      &.high-risk { border-left: 4px solid var(--error); }
      &.latest { 
        border: 2px solid var(--primary);
        box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
      }
    }
    .latest-badge {
      position: absolute;
      top: 12px;
      right: 12px;
      padding: 4px 10px;
      background: var(--primary);
      color: white;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 600;
    }
    .card-header {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 16px;
    }
    .risk-indicator {
      width: 48px;
      height: 48px;
      background: rgba(16, 185, 129, 0.1);
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      &.medium { background: rgba(245, 158, 11, 0.1); }
      &.high { background: rgba(239, 68, 68, 0.1); }
    }
    .risk-info { flex: 1; }
    .risk-value {
      font-size: 28px;
      font-weight: 800;
      color: var(--gray-900);
      display: block;
    }
    .risk-label {
      font-size: 13px;
      color: var(--gray-500);
    }
    .risk-badge {
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      background: rgba(16, 185, 129, 0.1);
      color: var(--success);
      &.medium { background: rgba(245, 158, 11, 0.1); color: #D97706; }
      &.high { background: rgba(239, 68, 68, 0.1); color: var(--error); }
    }
    .risk-meter {
      height: 8px;
      background: var(--gray-200);
      border-radius: 4px;
      overflow: hidden;
      margin-bottom: 16px;
      .meter-fill {
        height: 100%;
        background: var(--success);
        border-radius: 4px;
        transition: width 0.5s ease-out;
        &.medium { background: linear-gradient(90deg, var(--success), #F59E0B); }
        &.high { background: linear-gradient(90deg, #F59E0B, var(--error)); }
      }
    }
    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .card-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .delete-btn {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(239, 68, 68, 0.1);
      color: var(--error);
      border: none;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      &:hover {
        background: var(--error);
        color: white;
      }
    }
    .date {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: var(--gray-500);
    }
    .action-hint {
      font-size: 12px;
      color: var(--error);
      font-style: italic;
    }
    
    .empty-state {
      text-align: center;
      padding: 60px 24px;
      background: white;
      border-radius: 24px;
      box-shadow: var(--shadow-md);
      .empty-icon {
        width: 100px;
        height: 100px;
        background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(99, 102, 241, 0.1));
        border-radius: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 24px;
        color: var(--primary);
      }
      h3 { font-size: 20px; margin-bottom: 8px; }
      p { color: var(--gray-600); margin-bottom: 24px; max-width: 300px; margin-left: auto; margin-right: auto; }
      .btn { display: inline-flex; align-items: center; gap: 8px; }
    }
    
    /* Animations */
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
    .spinner { animation: spin 1s linear infinite; }
    .animate-in {
      animation: slideIn 0.3s ease-out forwards;
      opacity: 0;
    }
    @keyframes slideIn {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class PredictionsComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private autoRefreshSub?: Subscription;

  predictions = signal<Prediction[]>([]);
  generating = signal(false);

  // Sort predictions by date (newest first)
  sortedPredictions = computed(() => {
    return [...this.predictions()].sort((a, b) => 
      new Date(b.date).getTime() - new Date(a.date).getTime()
    );
  });

  latestRisk = computed(() => {
    const sorted = this.sortedPredictions();
    return sorted.length > 0 ? sorted[0].probNonAdherence * 100 : 0;
  });

  averageRisk = computed(() => {
    const preds = this.predictions();
    if (preds.length === 0) return 0;
    const sum = preds.reduce((acc, p) => acc + p.probNonAdherence, 0);
    return (sum / preds.length) * 100;
  });

  ngOnInit(): void { 
    this.loadPredictions();
    // Auto-refresh every 30 seconds
    this.autoRefreshSub = interval(30000).subscribe(() => this.loadPredictions());
  }

  ngOnDestroy(): void {
    this.autoRefreshSub?.unsubscribe();
  }

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

  deletePrediction(pred: Prediction, event: Event): void {
    event.stopPropagation();
    if (!confirm('Are you sure you want to delete this prediction?')) return;
    
    this.api.deletePrediction(pred.id).subscribe({
      next: () => {
        this.predictions.update(preds => preds.filter(p => p.id !== pred.id));
      },
      error: (err) => console.error('Failed to delete prediction:', err)
    });
  }

  clearAllPredictions(): void {
    if (!confirm('Are you sure you want to delete ALL predictions? This cannot be undone.')) return;
    
    const predictions = this.predictions();
    if (predictions.length === 0) return;
    
    this.generating.set(true);
    
    // Delete all predictions one by one
    const deletePromises = predictions.map(p => 
      this.api.deletePrediction(p.id).toPromise()
    );
    
    Promise.all(deletePromises).then(() => {
      this.predictions.set([]);
      this.generating.set(false);
    }).catch((err) => {
      console.error('Failed to clear all predictions:', err);
      this.loadPredictions();
      this.generating.set(false);
    });
  }

  formatDateTime(date: string): string {
    const d = new Date(date);
    return d.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}

