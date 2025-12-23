import { Component, inject, signal, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { interval, Subscription } from 'rxjs';

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
        <div class="status-badges">
          <div class="status-badge" [class.connected]="flaskConnected()">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z"/></svg>
            Camera {{ flaskConnected() ? 'Online' : 'Offline' }}
          </div>
        </div>
      </header>

      <div class="content">
        <!-- Emotion Display Card -->
        <div class="emotion-card" [style.--emotion-color]="getEmotionColor()" [style.background]="getEmotionGradient()">
          <div class="emotion-header">
            <div class="emotion-icon-wrapper" [class.pulsing]="autoDetecting()">
              <span class="emotion-icon">{{ getEmotionEmoji() }}</span>
            </div>
            <div class="emotion-info">
              <span class="emotion-label">Current Emotion</span>
              <h2>{{ formatEmotion(currentEmotion()) }}</h2>
              <div class="confidence-section">
                <div class="confidence-bar">
                  <div class="fill" [style.width.%]="confidence() * 100"></div>
                </div>
                <span class="confidence-text">{{ (confidence() * 100).toFixed(0) }}% confidence</span>
              </div>
            </div>
          </div>
          
          <!-- Auto Detect Button - Main Feature -->
          <button 
            class="auto-detect-btn" 
            (click)="startAutoDetect()" 
            [disabled]="autoDetecting() || !flaskConnected()"
            [class.detecting]="autoDetecting()"
          >
            @if (autoDetecting()) {
              <svg class="spinner" width="20" height="20" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" fill="none" stroke-dasharray="60" stroke-linecap="round"/></svg>
              Detecting... {{ autoDetectSeconds() }}s
            } @else {
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>
              Auto Detect (10s)
            }
          </button>
        </div>

        <!-- Camera Section -->
        <div class="camera-section">
          <div class="section-header">
            <h3>📹 Live Camera Feed</h3>
            @if (autoDetecting()) {
              <span class="live-badge">LIVE</span>
            }
          </div>
          @if (flaskConnected()) {
            <div class="camera-feed" [class.active]="autoDetecting()">
              <img [src]="'http://localhost:5000/video'" alt="Camera feed" crossorigin="anonymous">
              @if (autoDetecting()) {
                <div class="scan-overlay">
                  <div class="scan-line"></div>
                </div>
              }
            </div>
          } @else {
            <div class="camera-offline">
              <div class="offline-icon">📷</div>
              <p>Camera AI service is offline</p>
              <p class="hint">Start the Flask server: <code>python together.py</code></p>
              <button class="retry-btn" (click)="checkFlaskConnection()">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M17.65 6.35A7.958 7.958 0 0012 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0112 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
                Retry Connection
              </button>
            </div>
          }
        </div>

        <!-- Text Analysis -->
        <div class="text-section">
          <h3>📝 Text-Based Detection</h3>
          <p class="section-desc">Share how you're feeling and let AI analyze your emotions</p>
          <textarea 
            [(ngModel)]="textInput" 
            placeholder="I'm feeling a bit overwhelmed today because..." 
            rows="3"
            (keydown.control.enter)="analyzeText()"
          ></textarea>
          <div class="text-actions">
            <span class="hint">Ctrl+Enter to analyze</span>
            <button class="btn btn-secondary" (click)="analyzeText()" [disabled]="analyzing() || !textInput.trim()">
              @if (analyzing()) {
                <svg class="spinner" width="16" height="16" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" fill="none" stroke-dasharray="60" stroke-linecap="round"/></svg>
                Analyzing...
              } @else {
                Analyze Text
              }
            </button>
          </div>
        </div>

        <!-- AI Wellness Advice - Enhanced -->
        @if (showResult()) {
          <div class="result-card animate-in">
            <div class="result-header" [style.background]="getEmotionGradient()">
              <div class="result-icon">{{ getEmotionEmoji() }}</div>
              <div class="result-info">
                <span class="detected-label">Emotion Detected</span>
                <h3>{{ formatEmotion(currentEmotion()) }}</h3>
                <span class="confidence-badge">{{ (confidence() * 100).toFixed(0) }}% confidence</span>
              </div>
            </div>
            
            <div class="advice-content">
              <div class="advice-section">
                <div class="advice-title">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
                  AI Wellness Advice
                </div>
                <div class="health-message" [innerHTML]="getHealthMessageHtml()"></div>
              </div>
              
              <div class="quick-tip">
                <span class="tip-icon">💡</span>
                <span class="tip-text">{{ getQuickTip() }}</span>
              </div>
            </div>
            
            <button class="dismiss-btn" (click)="showResult.set(false)">Got it!</button>
          </div>
        }

        <!-- Emotion History -->
        @if (emotionHistory().length > 0) {
          <div class="history-section">
            <h3>📊 Recent Detections</h3>
            <div class="history-list">
              @for (item of emotionHistory().slice(0, 5); track $index) {
                <div class="history-item">
                  <span class="history-emoji">{{ getEmotionEmojiFor(item.emotion) }}</span>
                  <span class="history-emotion">{{ formatEmotion(item.emotion) }}</span>
                  <span class="history-source">{{ item.source === 'camera' ? '📹' : '📝' }}</span>
                  <span class="history-time">{{ formatTime(item.timestamp) }}</span>
                </div>
              }
            </div>
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
    .status-badges { display: flex; gap: 8px; }
    .status-badge {
      display: flex; align-items: center; gap: 6px;
      padding: 6px 12px;
      background: rgba(239, 68, 68, 0.1);
      color: var(--error);
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      &.connected { background: rgba(16, 185, 129, 0.1); color: var(--success); }
    }
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }
    
    /* Emotion Card */
    .emotion-card {
      border-radius: 24px;
      padding: 24px;
      margin-bottom: 24px;
      box-shadow: 0 10px 40px -10px rgba(0,0,0,0.2);
    }
    .emotion-header { display: flex; align-items: center; gap: 20px; margin-bottom: 20px; }
    .emotion-icon-wrapper {
      width: 80px; height: 80px;
      background: rgba(255,255,255,0.2);
      border-radius: 20px;
      display: flex; align-items: center; justify-content: center;
      &.pulsing { animation: pulse 1.5s infinite; }
    }
    .emotion-icon { font-size: 48px; }
    .emotion-info { flex: 1; color: white; }
    .emotion-label { font-size: 13px; opacity: 0.8; }
    .emotion-info h2 { font-size: 28px; font-weight: 800; margin: 4px 0; }
    .confidence-section { margin-top: 8px; }
    .confidence-bar {
      height: 6px; background: rgba(255,255,255,0.3); border-radius: 3px; overflow: hidden;
      .fill { height: 100%; background: white; border-radius: 3px; transition: width 0.3s; }
    }
    .confidence-text { font-size: 12px; opacity: 0.9; margin-top: 4px; display: block; }
    
    .auto-detect-btn {
      width: 100%;
      padding: 14px 24px;
      background: rgba(255,255,255,0.2);
      border: 2px solid rgba(255,255,255,0.3);
      border-radius: 16px;
      color: white;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      display: flex; align-items: center; justify-content: center; gap: 10px;
      transition: all 0.2s;
      &:hover:not(:disabled) { background: rgba(255,255,255,0.3); }
      &:disabled { opacity: 0.6; cursor: not-allowed; }
      &.detecting { background: rgba(255,255,255,0.3); }
    }
    
    /* Camera Section */
    .camera-section {
      background: white;
      border-radius: 20px;
      padding: 24px;
      margin-bottom: 20px;
      box-shadow: var(--shadow-md);
    }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
    .section-header h3 { font-size: 18px; margin: 0; }
    .live-badge {
      padding: 4px 10px;
      background: var(--error);
      color: white;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 700;
      animation: blink 1s infinite;
    }
    .camera-feed {
      border-radius: 16px;
      overflow: hidden;
      position: relative;
      background: #000;
      &.active { box-shadow: 0 0 0 3px var(--primary); }
      img { width: 100%; display: block; }
    }
    .scan-overlay {
      position: absolute; top: 0; left: 0; right: 0; bottom: 0;
      pointer-events: none;
    }
    .scan-line {
      position: absolute;
      width: 100%;
      height: 2px;
      background: linear-gradient(90deg, transparent, var(--primary), transparent);
      animation: scan 2s linear infinite;
    }
    .camera-offline {
      padding: 48px 24px;
      background: var(--gray-100);
      border-radius: 16px;
      text-align: center;
      .offline-icon { font-size: 48px; margin-bottom: 16px; }
      p { color: var(--gray-600); margin: 8px 0; }
      .hint { font-size: 13px; color: var(--gray-500); }
      code { background: var(--gray-200); padding: 2px 8px; border-radius: 4px; font-size: 12px; }
    }
    .retry-btn {
      margin-top: 16px;
      padding: 10px 20px;
      background: var(--primary);
      color: white;
      border: none;
      border-radius: 10px;
      font-size: 14px;
      cursor: pointer;
      display: inline-flex; align-items: center; gap: 8px;
      &:hover { opacity: 0.9; }
    }
    
    /* Text Section */
    .text-section {
      background: white;
      border-radius: 20px;
      padding: 24px;
      margin-bottom: 20px;
      box-shadow: var(--shadow-md);
      h3 { font-size: 18px; margin-bottom: 8px; }
      .section-desc { font-size: 14px; color: var(--gray-500); margin-bottom: 16px; }
    }
    textarea {
      width: 100%; padding: 14px; border: 2px solid var(--gray-200);
      border-radius: 12px; resize: none; font-family: inherit; font-size: 14px;
      transition: border-color 0.2s;
      &:focus { outline: none; border-color: var(--primary); }
    }
    .text-actions {
      display: flex; justify-content: space-between; align-items: center; margin-top: 12px;
      .hint { font-size: 12px; color: var(--gray-400); }
    }
    
    /* Result Card */
    .result-card {
      background: white;
      border-radius: 24px;
      overflow: hidden;
      margin-bottom: 20px;
      box-shadow: var(--shadow-lg);
    }
    .result-header {
      padding: 24px;
      display: flex; align-items: center; gap: 16px;
    }
    .result-icon {
      width: 64px; height: 64px;
      background: rgba(255,255,255,0.2);
      border-radius: 16px;
      display: flex; align-items: center; justify-content: center;
      font-size: 36px;
    }
    .result-info { color: white; }
    .detected-label { font-size: 12px; opacity: 0.8; }
    .result-info h3 { font-size: 24px; font-weight: 700; margin: 4px 0; }
    .confidence-badge {
      display: inline-block;
      padding: 4px 12px;
      background: rgba(255,255,255,0.2);
      border-radius: 12px;
      font-size: 12px;
    }
    .advice-content { padding: 24px; }
    .advice-section { margin-bottom: 20px; }
    .advice-title {
      display: flex; align-items: center; gap: 8px;
      font-weight: 600; color: var(--primary);
      margin-bottom: 12px;
    }
    .health-message {
      background: var(--gray-50);
      border-radius: 16px;
      padding: 20px;
      font-size: 14px;
      line-height: 1.8;
      color: var(--gray-700);
      white-space: pre-line;
    }
    .health-message strong { color: var(--gray-900); }
    .quick-tip {
      display: flex; align-items: flex-start; gap: 12px;
      background: #FEF3C7;
      border: 1px solid #FCD34D;
      border-radius: 12px;
      padding: 16px;
    }
    .tip-icon { font-size: 20px; }
    .tip-text { font-size: 14px; color: #92400E; line-height: 1.5; }
    .dismiss-btn {
      width: calc(100% - 48px);
      margin: 0 24px 24px;
      padding: 14px;
      background: var(--primary);
      color: white;
      border: none;
      border-radius: 12px;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      &:hover { opacity: 0.9; }
    }
    
    /* History Section */
    .history-section {
      background: white;
      border-radius: 20px;
      padding: 24px;
      box-shadow: var(--shadow-md);
      h3 { font-size: 18px; margin-bottom: 16px; }
    }
    .history-list { display: flex; flex-direction: column; gap: 12px; }
    .history-item {
      display: flex; align-items: center; gap: 12px;
      padding: 12px 16px;
      background: var(--gray-50);
      border-radius: 12px;
    }
    .history-emoji { font-size: 24px; }
    .history-emotion { flex: 1; font-weight: 500; }
    .history-source { font-size: 16px; }
    .history-time { font-size: 12px; color: var(--gray-500); }
    
    /* Animations */
    @keyframes pulse {
      0%, 100% { transform: scale(1); }
      50% { transform: scale(1.05); }
    }
    @keyframes blink {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }
    @keyframes scan {
      0% { top: 0; }
      100% { top: 100%; }
    }
    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
    .spinner { animation: spin 1s linear infinite; }
    .animate-in {
      animation: slideIn 0.3s ease-out;
    }
    @keyframes slideIn {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class EmotionsComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private subscriptions: Subscription[] = [];
  private emotionCounts: Record<string, number> = {};

  currentEmotion = signal('Neutral');
  confidence = signal(0.5);
  flaskConnected = signal(false);
  autoDetecting = signal(false);
  autoDetectSeconds = signal(10);
  textInput = '';
  analyzing = signal(false);
  showResult = signal(false);
  emotionHistory = signal<Array<{emotion: string; source: string; timestamp: Date}>>([]);

  ngOnInit(): void {
    this.checkFlaskConnection();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  checkFlaskConnection(): void {
    this.api.getFlaskHealth().subscribe({
      next: () => this.flaskConnected.set(true),
      error: () => this.flaskConnected.set(false)
    });
  }

  startAutoDetect(): void {
    if (!this.flaskConnected()) return;
    
    this.autoDetecting.set(true);
    this.autoDetectSeconds.set(10);
    this.emotionCounts = {};
    this.showResult.set(false);

    // Poll emotion every second
    const pollSub = interval(1000).subscribe(() => {
      this.autoDetectSeconds.update(s => s - 1);
      
      this.api.getEmotion().subscribe({
        next: (res) => {
          const emotion = res.emotion.toLowerCase();
          this.currentEmotion.set(emotion);
          this.confidence.set(res.confidence);
          this.emotionCounts[emotion] = (this.emotionCounts[emotion] || 0) + 1;
        },
        error: () => {}
      });

      if (this.autoDetectSeconds() <= 0) {
        pollSub.unsubscribe();
        this.finishAutoDetect();
      }
    });

    this.subscriptions.push(pollSub);
  }

  private finishAutoDetect(): void {
    this.autoDetecting.set(false);
    
    // Find dominant emotion
    let dominantEmotion = 'neutral';
    let maxCount = 0;
    Object.entries(this.emotionCounts).forEach(([emotion, count]) => {
      if (count > maxCount) {
        maxCount = count;
        dominantEmotion = emotion;
      }
    });

    this.currentEmotion.set(dominantEmotion);
    this.addToHistory(dominantEmotion, 'camera');
    this.showResult.set(true);
  }

  analyzeText(): void {
    if (!this.textInput.trim()) return;
    this.analyzing.set(true);
    this.showResult.set(false);

    this.api.analyzeText(this.textInput).subscribe({
      next: (res) => {
        const emotion = (res.primary_emotion || 'neutral').toLowerCase();
        this.currentEmotion.set(emotion);
        this.confidence.set(res.confidence || 0.7);
        this.addToHistory(emotion, 'text');
        this.showResult.set(true);
        this.analyzing.set(false);
        this.textInput = '';
      },
      error: () => {
        // Fallback to keyword detection
        this.detectFromKeywords(this.textInput);
        this.analyzing.set(false);
      }
    });
  }

  private detectFromKeywords(text: string): void {
    const lower = text.toLowerCase();
    let emotion = 'neutral';
    
    if (/happy|joy|great|excited|wonderful|amazing/.test(lower)) emotion = 'happy';
    else if (/sad|depress|down|unhappy|miserable/.test(lower)) emotion = 'sad';
    else if (/angry|frustrat|mad|furious|annoyed/.test(lower)) emotion = 'angry';
    else if (/anxious|worried|stress|nervous|overwhelm/.test(lower)) emotion = 'anxious';
    else if (/scared|fear|afraid|terrified/.test(lower)) emotion = 'fear';
    else if (/surprise|shock|unexpected|wow/.test(lower)) emotion = 'surprise';

    this.currentEmotion.set(emotion);
    this.confidence.set(0.65);
    this.addToHistory(emotion, 'text');
    this.showResult.set(true);
  }

  private addToHistory(emotion: string, source: string): void {
    const history = this.emotionHistory();
    history.unshift({ emotion, source, timestamp: new Date() });
    if (history.length > 10) history.pop();
    this.emotionHistory.set([...history]);
  }

  getEmotionColor(): string {
    const colors: Record<string, string> = {
      happy: '#FBBF24', sad: '#6366F1', angry: '#EF4444',
      fear: '#F97316', anxious: '#8B5CF6', neutral: '#6B7280', surprise: '#10B981'
    };
    return colors[this.currentEmotion().toLowerCase()] || '#6B7280';
  }

  getEmotionGradient(): string {
    const gradients: Record<string, string> = {
      happy: 'linear-gradient(135deg, #FBBF24, #F59E0B)',
      sad: 'linear-gradient(135deg, #6366F1, #818CF8)',
      angry: 'linear-gradient(135deg, #EF4444, #F87171)',
      fear: 'linear-gradient(135deg, #F97316, #FB923C)',
      anxious: 'linear-gradient(135deg, #8B5CF6, #A78BFA)',
      neutral: 'linear-gradient(135deg, #6B7280, #9CA3AF)',
      surprise: 'linear-gradient(135deg, #10B981, #34D399)'
    };
    return gradients[this.currentEmotion().toLowerCase()] || gradients['neutral'];
  }

  getEmotionEmoji(): string {
    const emojis: Record<string, string> = {
      happy: '😊', sad: '😢', angry: '😠', fear: '😨',
      anxious: '😰', neutral: '😐', surprise: '😲'
    };
    return emojis[this.currentEmotion().toLowerCase()] || '😐';
  }

  getEmotionEmojiFor(emotion: string): string {
    const emojis: Record<string, string> = {
      happy: '😊', sad: '😢', angry: '😠', fear: '😨',
      anxious: '😰', neutral: '😐', surprise: '😲'
    };
    return emojis[emotion.toLowerCase()] || '😐';
  }

  formatEmotion(emotion: string): string {
    return emotion.charAt(0).toUpperCase() + emotion.slice(1);
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }

  getHealthMessageHtml(): string {
    const emotion = this.currentEmotion().toLowerCase();
    const messages: Record<string, string> = {
      happy: `<strong>🌟 Physical Benefits:</strong>
• Boosts immune system by increasing antibody production
• Lowers cortisol (stress hormone) levels by up to 23%
• Reduces heart rate and blood pressure
• Releases endorphins - natural mood elevators

<strong>💪 Wellness Recommendations:</strong>
• Practice gratitude journaling to maintain this positive state
• Share your happiness with loved ones
• Engage in activities that brought you here
• Use this energy for creative or challenging tasks`,

      sad: `<strong>💙 Understanding Sadness:</strong>
• Sadness is a natural response that allows emotional processing
• It signals a need for self-compassion and reflection
• Your brain is processing loss, disappointment, or unmet needs

<strong>🌱 Wellness Recommendations:</strong>
• Allow yourself to feel - suppressing emotions prolongs them
• Gentle movement: a 15-minute walk can boost serotonin
• Reach out to one trusted person - social support is healing
• Ensure adequate sleep (7-9 hours) - fatigue worsens mood`,

      angry: `<strong>🔥 Physical Impact:</strong>
• Activates fight-or-flight response instantly
• Heart rate can increase by 30+ beats per minute
• Blood pressure spikes; blood vessels constrict
• Chronic anger increases risk of heart disease by 19%

<strong>🧘 Wellness Recommendations:</strong>
• STOP: Step back, Take a breath, Observe, Proceed mindfully
• 4-7-8 breathing: Inhale 4 sec, hold 7 sec, exhale 8 sec
• Physical release: brisk walk, push-ups, or squeeze a stress ball
• Write down your thoughts before responding`,

      fear: `<strong>⚡ Body Response:</strong>
• Amygdala triggers immediate stress response
• Cortisol and adrenaline surge through your system
• Heart rate increases; breathing becomes shallow
• Chronic fear weakens immune function

<strong>🛡️ Wellness Recommendations:</strong>
• Grounding technique (5-4-3-2-1): Name 5 things you see, 4 you hear...
• Box breathing: 4 counts in, 4 hold, 4 out, 4 hold
• Place hand on heart - physical touch releases oxytocin
• Remember: Fear is a signal, not a command to act`,

      anxious: `<strong>😰 Physical Effects:</strong>
• Body stays in constant "alert mode"
• Muscle tension, especially in neck and shoulders
• Shallow breathing reduces oxygen to brain
• Sleep disruption creates a negative feedback loop

<strong>🌿 Wellness Recommendations:</strong>
• Diaphragmatic breathing: breathe into belly, not chest
• Reduce caffeine - it mimics anxiety symptoms
• "Worry window": schedule 15 min daily to address concerns
• Regular exercise reduces anxiety by 20% on average`,

      surprise: `<strong>✨ What's Happening:</strong>
• Brain temporarily pauses to assess the unexpected
• Heightened attention and awareness activated
• Brief release of norepinephrine sharpens focus
• Can transition to positive or negative states

<strong>🎯 Wellness Recommendations:</strong>
• Take a moment to process before reacting
• Use heightened awareness for mindfulness practice
• If positive surprise: savor the moment consciously
• Journal about what surprised you and why`,

      neutral: `<strong>☯️ The Balanced State:</strong>
• Your nervous system is in optimal rest-and-digest mode
• Body performs maintenance and repair functions
• Ideal state for clear thinking and decision-making
• Supports healthy digestion and immune function

<strong>🌸 Wellness Recommendations:</strong>
• This is an excellent time for important decisions
• Practice gratitude to potentially shift toward happiness
• Use this stable foundation for personal growth activities
• Check in: are basic needs met? (sleep, nutrition, movement)`
    };

    return messages[emotion] || messages['neutral'];
  }

  getQuickTip(): string {
    const tips: Record<string, string> = {
      happy: "Quick Action: Write down 3 things you're grateful for right now to extend this positive feeling!",
      sad: "Quick Action: Step outside for 5 minutes - natural light boosts serotonin production naturally.",
      angry: "Quick Action: Try the 4-7-8 breath now: Inhale 4 sec → Hold 7 sec → Exhale 8 sec. Repeat 3x.",
      fear: "Quick Action: Ground yourself - feel your feet on the floor and name 5 blue things you can see.",
      anxious: "Quick Action: Place one hand on chest, one on belly. Breathe so only the belly hand moves.",
      surprise: "Quick Action: Take 3 slow breaths to process this moment before deciding how to respond.",
      neutral: "Quick Action: Close your eyes, take 3 deep breaths, and ask yourself: 'What do I need right now?'"
    };
    return tips[this.currentEmotion().toLowerCase()] || tips['neutral'];
  }
}
