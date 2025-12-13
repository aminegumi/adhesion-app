import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-emotions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="emotions-page">
      <header class="page-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>Emotion Detection</h1>
        <div class="status-badge" [class.connected]="flaskConnected()">
          {{ flaskConnected() ? 'Connected' : 'Offline' }}
        </div>
      </header>

      <div class="content">
        <!-- Emotion Display -->
        <div class="emotion-card" [style.--emotion-color]="getEmotionColor()">
          <div class="emotion-icon">
            @switch (currentEmotion().toLowerCase()) {
              @case ('happy') { 😊 }
              @case ('sad') { 😢 }
              @case ('angry') { 😠 }
              @case ('fear') { 😨 }
              @case ('surprise') { 😲 }
              @default { 😐 }
            }
          </div>
          <h2>{{ currentEmotion() }}</h2>
          <div class="confidence-bar">
            <div class="fill" [style.width.%]="confidence() * 100"></div>
          </div>
          <span class="confidence-text">{{ (confidence() * 100).toFixed(0) }}% confidence</span>
        </div>

        <!-- Camera Section -->
        <div class="camera-section">
          <h3>Camera Detection</h3>
          @if (flaskConnected()) {
            <div class="camera-feed">
              <img [src]="'http://localhost:5000/video'" alt="Camera feed">
            </div>
            <button class="btn btn-primary" (click)="startScan()" [disabled]="scanning()">
              @if (scanning()) {
                Scanning... {{ scanSeconds() }}s
              } @else {
                Start 12s Emotion Scan
              }
            </button>
          } @else {
            <div class="camera-offline">
              <p>Camera AI service is offline</p>
              <p class="hint">Start the Flask server on port 5000</p>
            </div>
          }
        </div>

        <!-- Text Analysis -->
        <div class="text-section">
          <h3>Text-Based Detection</h3>
          <textarea [(ngModel)]="textInput" placeholder="Share how you're feeling..." rows="3"></textarea>
          <button class="btn btn-secondary" (click)="analyzeText()" [disabled]="analyzing()">
            @if (analyzing()) {
              Analyzing...
            } @else {
              Analyze Text
            }
          </button>
        </div>

        <!-- AI Advice -->
        @if (aiAdvice()) {
          <div class="advice-card">
            <h3>✨ AI Wellness Advice</h3>
            <p>{{ aiAdvice() }}</p>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .emotions-page { min-height: 100vh; background: linear-gradient(180deg, #FDF4FF 0%, #FAF5FF 100%); }
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
    .status-badge {
      padding: 6px 12px;
      background: rgba(239, 68, 68, 0.1);
      color: var(--error);
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      &.connected { background: rgba(16, 185, 129, 0.1); color: var(--success); }
    }
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }
    .emotion-card {
      background: white;
      border-radius: 24px;
      padding: 32px;
      text-align: center;
      box-shadow: var(--shadow-lg);
      margin-bottom: 24px;
    }
    .emotion-icon { font-size: 64px; margin-bottom: 16px; }
    .emotion-card h2 { font-size: 28px; margin-bottom: 16px; }
    .confidence-bar {
      height: 8px; background: var(--gray-200); border-radius: 4px; overflow: hidden;
      .fill { height: 100%; background: var(--primary-gradient); }
    }
    .confidence-text { display: block; margin-top: 8px; color: var(--gray-500); font-size: 14px; }
    .camera-section, .text-section {
      background: white;
      border-radius: 20px;
      padding: 24px;
      margin-bottom: 20px;
      h3 { font-size: 18px; margin-bottom: 16px; }
    }
    .camera-feed {
      border-radius: 16px;
      overflow: hidden;
      margin-bottom: 16px;
      img { width: 100%; display: block; }
    }
    .camera-offline {
      padding: 40px;
      background: var(--gray-100);
      border-radius: 16px;
      text-align: center;
      color: var(--gray-500);
      .hint { font-size: 13px; margin-top: 8px; }
    }
    textarea {
      width: 100%; padding: 14px; border: 2px solid var(--gray-200);
      border-radius: 12px; resize: none; margin-bottom: 12px;
      font-family: inherit;
      &:focus { outline: none; border-color: var(--primary); }
    }
    .advice-card {
      background: linear-gradient(135deg, rgba(168, 85, 247, 0.1), rgba(139, 92, 246, 0.1));
      border: 1px solid rgba(168, 85, 247, 0.2);
      border-radius: 20px;
      padding: 24px;
      h3 { font-size: 16px; margin-bottom: 12px; color: var(--accent); }
      p { color: var(--gray-700); line-height: 1.6; }
    }
  `]
})
export class EmotionsComponent {
  private api = inject(ApiService);

  currentEmotion = signal('Neutral');
  confidence = signal(0.5);
  flaskConnected = signal(false);
  scanning = signal(false);
  scanSeconds = signal(12);
  textInput = '';
  analyzing = signal(false);
  aiAdvice = signal<string | null>(null);

  constructor() {
    this.checkFlaskConnection();
  }

  checkFlaskConnection(): void {
    this.api.getFlaskHealth().subscribe({
      next: () => this.flaskConnected.set(true),
      error: () => this.flaskConnected.set(false)
    });
  }

  startScan(): void {
    this.scanning.set(true);
    this.scanSeconds.set(12);
    
    const interval = setInterval(() => {
      this.scanSeconds.update(s => s - 1);
      this.api.getEmotion().subscribe(res => {
        this.currentEmotion.set(res.emotion);
        this.confidence.set(res.confidence);
      });
      
      if (this.scanSeconds() <= 0) {
        clearInterval(interval);
        this.scanning.set(false);
        this.getAdvice();
      }
    }, 1000);
  }

  analyzeText(): void {
    if (!this.textInput.trim()) return;
    this.analyzing.set(true);
    this.api.analyzeText(this.textInput).subscribe({
      next: (res) => {
        this.currentEmotion.set(res.primary_emotion || 'Neutral');
        this.confidence.set(res.confidence || 0.5);
        this.aiAdvice.set(res.recommendation);
        this.analyzing.set(false);
      },
      error: () => this.analyzing.set(false)
    });
  }

  getAdvice(): void {
    this.api.chat(`I'm feeling ${this.currentEmotion()}. Give me supportive advice.`, []).subscribe({
      next: (res) => this.aiAdvice.set(res.bot_message),
      error: () => {}
    });
  }

  getEmotionColor(): string {
    const colors: Record<string, string> = {
      happy: '#FBBF24', sad: '#6366F1', angry: '#EF4444',
      fear: '#F97316', neutral: '#6B7280', surprise: '#10B981'
    };
    return colors[this.currentEmotion().toLowerCase()] || '#6B7280';
  }
}
