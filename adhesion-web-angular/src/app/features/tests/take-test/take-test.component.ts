import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, Test, TestSession, TestResult } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-take-test',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="take-test-page">
      <header class="page-header">
        <a routerLink="/tests" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>{{ session()?.testTitle || 'Loading...' }}</h1>
      </header>

      <div class="content">
        @if (result()) {
          <div class="result-card">
            <div class="result-icon">✅</div>
            <h2>Test Completed!</h2>
            <div class="score-display">
              <span class="score">{{ result()?.totalScore }}</span>
              <span class="label">Total Score</span>
            </div>
            <div class="interpretation">
              <h3>{{ result()?.interpretationLevel }}</h3>
              <p>{{ result()?.scoreDescription }}</p>
            </div>
            @if (result()?.recommendations?.length) {
              <div class="recommendations">
                <h4>Recommendations</h4>
                <ul>
                  @for (rec of result()?.recommendations; track rec) {
                    <li>{{ rec }}</li>
                  }
                </ul>
              </div>
            }
            <a routerLink="/tests" class="btn btn-primary">Back to Tests</a>
          </div>
        } @else if (session()) {
          <div class="progress-bar">
            <div class="fill" [style.width.%]="progress()"></div>
          </div>
          <span class="progress-text">Question {{ currentIndex() + 1 }} of {{ session()?.questions?.length }}</span>

          <div class="question-card">
            <p class="question-text">{{ currentQuestion()?.text }}</p>
            <div class="options">
              @for (score of getScoreRange(); track score) {
                <button 
                  class="option-btn" 
                  [class.selected]="answers()[currentQuestion()?.id || 0] === score"
                  (click)="selectAnswer(score)"
                >
                  {{ getOptionLabel(score) }}
                </button>
              }
            </div>
          </div>

          <div class="nav-buttons">
            <button class="btn btn-secondary" (click)="prevQuestion()" [disabled]="currentIndex() === 0">Previous</button>
            @if (currentIndex() < (session()?.questions?.length || 0) - 1) {
              <button class="btn btn-primary" (click)="nextQuestion()" [disabled]="answers()[currentQuestion()?.id || 0] === undefined">Next</button>
            } @else {
              <button class="btn btn-success" (click)="submitTest()" [disabled]="!canSubmit() || submitting()">
                @if (submitting()) { Submitting... } @else { Submit }
              </button>
            }
          </div>
        } @else {
          <div class="loading">Loading test...</div>
        }
      </div>
    </div>
  `,
  styles: [`
    .take-test-page { min-height: 100vh; background: linear-gradient(180deg, #EEF2FF 0%, #E0E7FF 100%); }
    .page-header {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: white;
      h1 { flex: 1; font-size: 18px; }
    }
    .back-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: var(--gray-100);
      border-radius: 10px;
      color: var(--gray-600);
    }
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }
    .progress-bar {
      height: 8px; background: rgba(99, 102, 241, 0.2); border-radius: 4px; overflow: hidden;
      .fill { height: 100%; background: var(--primary); transition: width 0.3s; }
    }
    .progress-text { display: block; text-align: center; margin: 12px 0 24px; color: var(--gray-500); font-size: 14px; }
    .question-card {
      background: white;
      border-radius: 20px;
      padding: 32px;
      box-shadow: var(--shadow-lg);
      margin-bottom: 24px;
    }
    .question-text { font-size: 18px; line-height: 1.6; margin-bottom: 24px; color: var(--gray-800); }
    .options { display: flex; flex-direction: column; gap: 12px; }
    .option-btn {
      padding: 16px;
      background: var(--gray-50);
      border: 2px solid var(--gray-200);
      border-radius: 12px;
      text-align: left;
      cursor: pointer;
      transition: all 0.2s;
      &:hover { border-color: var(--primary); }
      &.selected { background: rgba(99, 102, 241, 0.1); border-color: var(--primary); }
    }
    .nav-buttons { display: flex; gap: 12px; }
    .nav-buttons .btn { flex: 1; }
    .result-card {
      background: white;
      border-radius: 24px;
      padding: 40px;
      text-align: center;
      box-shadow: var(--shadow-lg);
    }
    .result-icon { font-size: 64px; margin-bottom: 16px; }
    .result-card h2 { font-size: 24px; margin-bottom: 24px; }
    .score-display {
      background: var(--primary-gradient);
      color: white;
      padding: 24px;
      border-radius: 16px;
      margin-bottom: 24px;
      .score { display: block; font-size: 48px; font-weight: 800; }
      .label { opacity: 0.8; }
    }
    .interpretation { text-align: left; margin-bottom: 24px; h3 { margin-bottom: 8px; } p { color: var(--gray-600); } }
    .recommendations { text-align: left; margin-bottom: 24px; h4 { margin-bottom: 12px; } ul { padding-left: 20px; } li { margin-bottom: 8px; color: var(--gray-600); } }
    .loading { text-align: center; padding: 48px; color: var(--gray-500); }
  `]
})
export class TakeTestComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private auth = inject(AuthService);

  session = signal<TestSession | null>(null);
  result = signal<TestResult | null>(null);
  currentIndex = signal(0);
  answers = signal<Record<number, number>>({});
  submitting = signal(false);

  ngOnInit(): void {
    const testId = this.route.snapshot.params['id'];
    const userId = this.auth.getUserId();
    if (testId && userId) {
      this.api.startTestSession(+testId, userId).subscribe({
        next: (session) => this.session.set(session),
        error: () => this.router.navigate(['/tests'])
      });
    }
  }

  currentQuestion() { return this.session()?.questions?.[this.currentIndex()]; }
  progress() { return ((this.currentIndex() + 1) / (this.session()?.questions?.length || 1)) * 100; }
  canSubmit() { return Object.keys(this.answers()).length === this.session()?.questions?.length; }

  getScoreRange(): number[] {
    const q = this.currentQuestion();
    if (!q) return [0, 1, 2, 3];
    const range = [];
    for (let i = q.minScore; i <= q.maxScore; i++) range.push(i);
    return range;
  }

  getOptionLabel(score: number): string {
    const labels = ['Not at all', 'Several days', 'More than half the days', 'Nearly every day'];
    return labels[score] || `Score: ${score}`;
  }

  selectAnswer(score: number): void {
    const qId = this.currentQuestion()?.id;
    if (qId) this.answers.update(a => ({ ...a, [qId]: score }));
  }

  prevQuestion(): void { if (this.currentIndex() > 0) this.currentIndex.update(i => i - 1); }
  nextQuestion(): void {
    if (this.currentIndex() < (this.session()?.questions?.length || 0) - 1) {
      this.currentIndex.update(i => i + 1);
    }
  }

  submitTest(): void {
    const session = this.session();
    if (!session) return;
    this.submitting.set(true);
    const answerData = Object.entries(this.answers()).map(([qId, score]) => ({ questionId: +qId, score }));
    this.api.submitTestAnswers(session.id, answerData).subscribe({
      next: (result) => { this.result.set(result); this.submitting.set(false); },
      error: () => this.submitting.set(false)
    });
  }
}
