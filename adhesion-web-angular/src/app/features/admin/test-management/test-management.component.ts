import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, Test, Question } from '../../../core/services/api.service';

interface QuestionForm {
  text: string;
  minScore: number;
  maxScore: number;
  orderIndex: number;
}

@Component({
  selector: 'app-test-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="admin-page">
      <header class="page-header">
        <a routerLink="/admin/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>Test Management</h1>
        <button class="refresh-btn" (click)="loadTests()">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M17.65 6.35A7.958 7.958 0 0012 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0112 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
        </button>
        <button class="btn btn-danger" (click)="openCreateDialog()">+ New Test</button>
      </header>

      <div class="content">
        @for (test of tests(); track test.id) {
          <div class="test-card">
            <div class="test-header">
              <div class="test-icon" [style.background]="getTypeColor(test.code) + '20'" [style.color]="getTypeColor(test.code)">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3c-1.27 0-2.4.8-2.82 2H3v2h1.95L2 14c-.47 2 1 3 3.5 3s4.06-1 3.5-3L6.05 7h3.12c.33.85 1.03 1.5 1.91 1.79V20H5v2h14v-2h-6V8.79c.88-.29 1.58-.94 1.91-1.79h3.12L15 14c-.47 2 1 3 3.5 3s4.06-1 3.5-3l-2.95-7H21V5h-6.18C14.4 3.8 13.27 3 12 3z"/></svg>
              </div>
              <div class="test-info">
                <h3>{{ test.title }}</h3>
                <div class="test-badges">
                  <span class="badge code" [style.background]="getTypeColor(test.code) + '20'" [style.color]="getTypeColor(test.code)">{{ test.code }}</span>
                  <span class="badge status" [class.active]="test.active">{{ test.active ? 'Active' : 'Inactive' }}</span>
                </div>
              </div>
              <div class="test-actions">
                <button class="action-btn edit" (click)="openEditDialog(test)" title="Edit">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
                </button>
                <button class="action-btn delete" (click)="deleteTest(test)" title="Delete">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
                </button>
              </div>
            </div>

            @if (test.description) {
              <p class="test-description">{{ test.description }}</p>
            }

            <!-- Questions section (expandable) -->
            @if (test.questions?.length) {
              <div class="questions-section">
                <button class="questions-toggle" (click)="toggleQuestions(test.id)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" [class.rotated]="expandedTests().includes(test.id)"><path d="M10 17l5-5-5-5v10z"/></svg>
                  {{ test.questions.length }} Questions
                </button>
                @if (expandedTests().includes(test.id)) {
                  <div class="questions-list">
                    @for (q of test.questions; track q.id; let i = $index) {
                      <div class="question-item">
                        <span class="q-number">Q{{ i + 1 }}</span>
                        <span class="q-text">{{ q.text }}</span>
                        <span class="q-range">{{ q.minScore }} - {{ q.maxScore }}</span>
                      </div>
                    }
                  </div>
                }
              </div>
            }
          </div>
        } @empty {
          <div class="empty-state">
            <div class="empty-icon">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="currentColor"><path d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9h-4v4h-2v-4H9V9h4V5h2v4h4v2z"/></svg>
            </div>
            <h3>No Tests Yet</h3>
            <p>Create your first psychological test to get started.</p>
            <button class="btn btn-danger" (click)="openCreateDialog()">Create First Test</button>
          </div>
        }
      </div>

      <!-- Create/Edit Dialog -->
      @if (showDialog()) {
        <div class="dialog-overlay" (click)="closeDialog()">
          <div class="dialog large" (click)="$event.stopPropagation()">
            <div class="dialog-header">
              <h2>{{ editingTest() ? 'Edit Test' : 'Create New Test' }}</h2>
              <button class="close-btn" (click)="closeDialog()">×</button>
            </div>

            <div class="dialog-content">
              <div class="form-row">
                <div class="form-group">
                  <label>Test Title *</label>
                  <input type="text" [(ngModel)]="formData.title" class="form-input-dark" placeholder="e.g., PHQ-9 Depression Scale">
                </div>
                <div class="form-group small">
                  <label>Test Code *</label>
                  <input type="text" [(ngModel)]="formData.code" class="form-input-dark" placeholder="e.g., PHQ9">
                </div>
              </div>

              <div class="form-group">
                <label>Description</label>
                <textarea [(ngModel)]="formData.description" class="form-input-dark" rows="2" placeholder="Brief description of the test..."></textarea>
              </div>

              <div class="form-row">
                <div class="form-group small">
                  <label>Version</label>
                  <input type="text" [(ngModel)]="formData.version" class="form-input-dark" placeholder="1.0">
                </div>
                <div class="form-group small">
                  <label>Status</label>
                  <label class="switch">
                    <input type="checkbox" [(ngModel)]="formData.active">
                    <span class="slider"></span>
                    <span class="label-text">{{ formData.active ? 'Active' : 'Inactive' }}</span>
                  </label>
                </div>
              </div>

              <!-- Questions Section -->
              <div class="questions-form-section">
                <div class="section-header">
                  <h3>Questions</h3>
                  <button class="add-question-btn" (click)="addQuestion()">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                    Add Question
                  </button>
                </div>

                @if (formQuestions.length === 0) {
                  <div class="no-questions">
                    <p>No questions added yet. Click "Add Question" to start.</p>
                  </div>
                } @else {
                  <div class="questions-form-list">
                    @for (q of formQuestions; track $index; let i = $index) {
                      <div class="question-form-item">
                        <div class="question-header">
                          <span class="q-badge">Q{{ i + 1 }}</span>
                          <button class="remove-q-btn" (click)="removeQuestion(i)">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                          </button>
                        </div>
                        <div class="question-body">
                          <input type="text" [(ngModel)]="q.text" class="form-input-dark" placeholder="Enter question text...">
                          <div class="score-inputs">
                            <div class="score-field">
                              <label>Min Score</label>
                              <input type="number" [(ngModel)]="q.minScore" class="form-input-dark">
                            </div>
                            <div class="score-field">
                              <label>Max Score</label>
                              <input type="number" [(ngModel)]="q.maxScore" class="form-input-dark">
                            </div>
                          </div>
                        </div>
                      </div>
                    }
                  </div>
                }
              </div>
            </div>

            <div class="dialog-footer">
              <button class="btn btn-ghost" (click)="closeDialog()">Cancel</button>
              <button class="btn btn-danger" (click)="saveTest()" [disabled]="saving()">
                @if (saving()) {
                  <span class="spinner-small"></span>
                }
                {{ editingTest() ? 'Update Test' : 'Create Test' }}
              </button>
            </div>
          </div>
        </div>
      }
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
    .back-btn, .refresh-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: rgba(255,255,255,0.1);
      border: none;
      border-radius: 10px;
      color: white;
      cursor: pointer;
      transition: all 0.2s;
      &:hover { background: rgba(255,255,255,0.15); }
    }
    .content { max-width: 800px; margin: 0 auto; padding: 24px; }

    .test-card {
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 16px;
      padding: 20px;
      margin-bottom: 16px;
      transition: all 0.2s;
      &:hover { border-color: rgba(255,255,255,0.15); }
    }
    .test-header {
      display: flex;
      align-items: flex-start;
      gap: 14px;
    }
    .test-icon {
      width: 48px; height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .test-info { flex: 1; }
    .test-info h3 {
      color: white;
      font-size: 17px;
      font-weight: 600;
      margin-bottom: 8px;
    }
    .test-badges {
      display: flex;
      gap: 8px;
    }
    .badge {
      padding: 4px 10px;
      border-radius: 20px;
      font-size: 11px;
      font-weight: 600;
    }
    .badge.status {
      background: rgba(255,255,255,0.1);
      color: rgba(255,255,255,0.5);
      &.active { background: rgba(16, 185, 129, 0.2); color: var(--success); }
    }
    .test-actions {
      display: flex;
      gap: 8px;
    }
    .action-btn {
      width: 36px; height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: none;
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.2s;
      &.edit {
        background: rgba(99, 102, 241, 0.15);
        color: var(--primary);
        &:hover { background: var(--primary); color: white; }
      }
      &.delete {
        background: rgba(239, 68, 68, 0.15);
        color: var(--error);
        &:hover { background: var(--error); color: white; }
      }
    }
    .test-description {
      color: rgba(255,255,255,0.6);
      font-size: 14px;
      line-height: 1.5;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid var(--dark-border);
    }

    .questions-section {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid var(--dark-border);
    }
    .questions-toggle {
      display: flex;
      align-items: center;
      gap: 6px;
      background: none;
      border: none;
      color: rgba(255,255,255,0.6);
      font-size: 13px;
      cursor: pointer;
      padding: 0;
      &:hover { color: white; }
      svg { transition: transform 0.2s; }
      svg.rotated { transform: rotate(90deg); }
    }
    .questions-list {
      margin-top: 12px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .question-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.05);
      border-radius: 10px;
    }
    .q-number {
      padding: 4px 8px;
      background: rgba(99, 102, 241, 0.2);
      color: var(--primary);
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
    }
    .q-text {
      flex: 1;
      color: rgba(255,255,255,0.9);
      font-size: 13px;
    }
    .q-range {
      color: rgba(255,255,255,0.4);
      font-size: 11px;
    }

    .empty-state {
      text-align: center;
      padding: 60px 24px;
      .empty-icon {
        width: 80px; height: 80px;
        background: rgba(255,255,255,0.05);
        border-radius: 20px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 24px;
        color: rgba(255,255,255,0.3);
      }
      h3 { color: white; font-size: 20px; margin-bottom: 8px; }
      p { color: rgba(255,255,255,0.5); margin-bottom: 24px; }
    }

    /* Dialog Styles */
    .dialog-overlay {
      position: fixed; inset: 0;
      background: rgba(0,0,0,0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 24px;
    }
    .dialog {
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 20px;
      width: 100%;
      max-width: 520px;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      &.large { max-width: 640px; }
    }
    .dialog-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 20px 24px;
      border-bottom: 1px solid var(--dark-border);
      h2 { color: white; font-size: 18px; }
    }
    .close-btn {
      width: 32px; height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(255,255,255,0.1);
      border: none;
      border-radius: 8px;
      color: white;
      font-size: 20px;
      cursor: pointer;
      &:hover { background: rgba(255,255,255,0.15); }
    }
    .dialog-content {
      padding: 24px;
      overflow-y: auto;
      flex: 1;
    }
    .dialog-footer {
      display: flex;
      gap: 12px;
      padding: 16px 24px;
      border-top: 1px solid var(--dark-border);
      .btn { flex: 1; }
    }

    .form-row {
      display: flex;
      gap: 16px;
      margin-bottom: 16px;
    }
    .form-group {
      flex: 1;
      margin-bottom: 16px;
      &.small { flex: 0 0 140px; }
      label {
        display: block;
        color: rgba(255,255,255,0.7);
        font-size: 13px;
        font-weight: 500;
        margin-bottom: 8px;
      }
    }
    .form-input-dark {
      width: 100%;
      padding: 12px 14px;
      background: rgba(255,255,255,0.05);
      border: 1px solid var(--dark-border);
      border-radius: 10px;
      color: white;
      font-size: 14px;
      &:focus { outline: none; border-color: var(--admin-primary); }
      &::placeholder { color: rgba(255,255,255,0.3); }
    }
    textarea.form-input-dark { resize: none; font-family: inherit; }

    .switch {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      input { display: none; }
      .slider {
        width: 44px;
        height: 24px;
        background: rgba(255,255,255,0.2);
        border-radius: 12px;
        position: relative;
        transition: background 0.2s;
        &::after {
          content: '';
          position: absolute;
          width: 20px; height: 20px;
          background: white;
          border-radius: 50%;
          top: 2px; left: 2px;
          transition: transform 0.2s;
        }
      }
      input:checked + .slider {
        background: var(--success);
        &::after { transform: translateX(20px); }
      }
      .label-text { color: rgba(255,255,255,0.7); font-size: 14px; }
    }

    .questions-form-section {
      margin-top: 8px;
      padding-top: 16px;
      border-top: 1px solid var(--dark-border);
    }
    .section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;
      h3 { color: white; font-size: 15px; }
    }
    .add-question-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 14px;
      background: rgba(99, 102, 241, 0.15);
      color: var(--primary);
      border: none;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      &:hover { background: rgba(99, 102, 241, 0.25); }
    }
    .no-questions {
      padding: 24px;
      text-align: center;
      background: rgba(255,255,255,0.03);
      border: 1px dashed rgba(255,255,255,0.1);
      border-radius: 12px;
      p { color: rgba(255,255,255,0.5); font-size: 13px; margin: 0; }
    }
    .questions-form-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-height: 300px;
      overflow-y: auto;
    }
    .question-form-item {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 12px;
      padding: 12px;
    }
    .question-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 10px;
    }
    .q-badge {
      padding: 4px 10px;
      background: rgba(99, 102, 241, 0.2);
      color: var(--primary);
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
    }
    .remove-q-btn {
      width: 24px; height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(239, 68, 68, 0.15);
      color: var(--error);
      border: none;
      border-radius: 6px;
      cursor: pointer;
      &:hover { background: var(--error); color: white; }
    }
    .question-body input {
      margin-bottom: 10px;
    }
    .score-inputs {
      display: flex;
      gap: 12px;
    }
    .score-field {
      flex: 1;
      label {
        display: block;
        color: rgba(255,255,255,0.5);
        font-size: 11px;
        margin-bottom: 4px;
      }
      input {
        margin-bottom: 0;
        padding: 8px 10px;
        font-size: 13px;
      }
    }

    .btn {
      padding: 12px 20px;
      border: none;
      border-radius: 10px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      transition: all 0.2s;
    }
    .btn-ghost {
      background: rgba(255,255,255,0.1);
      color: white;
      &:hover { background: rgba(255,255,255,0.15); }
    }
    .btn-danger {
      background: var(--admin-primary);
      color: white;
      &:hover { background: #B91C1C; }
      &:disabled { opacity: 0.6; cursor: not-allowed; }
    }

    .spinner-small {
      width: 16px; height: 16px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class TestManagementComponent implements OnInit {
  private api = inject(ApiService);

  tests = signal<Test[]>([]);
  showDialog = signal(false);
  editingTest = signal<Test | null>(null);
  saving = signal(false);
  expandedTests = signal<number[]>([]);

  formData = {
    title: '',
    code: '',
    description: '',
    version: '1.0',
    active: true
  };
  formQuestions: QuestionForm[] = [];

  ngOnInit(): void {
    this.loadTests();
  }

  loadTests(): void {
    this.api.getTests().subscribe({
      next: (t) => this.tests.set(t),
      error: (err) => console.error('Failed to load tests:', err)
    });
  }

  getTypeColor(code: string): string {
    const lowerCode = (code || '').toLowerCase();
    if (lowerCode.includes('anx') || lowerCode.includes('gad')) return '#EF4444';
    if (lowerCode.includes('dep') || lowerCode.includes('phq')) return '#6366F1';
    if (lowerCode.includes('mot')) return '#10B981';
    if (lowerCode.includes('mmas')) return '#F59E0B';
    return '#6366F1';
  }

  toggleQuestions(testId: number): void {
    this.expandedTests.update(ids => {
      if (ids.includes(testId)) {
        return ids.filter(id => id !== testId);
      }
      return [...ids, testId];
    });
  }

  openCreateDialog(): void {
    this.editingTest.set(null);
    this.formData = { title: '', code: '', description: '', version: '1.0', active: true };
    this.formQuestions = [];
    this.showDialog.set(true);
  }

  openEditDialog(test: Test): void {
    this.editingTest.set(test);
    this.formData = {
      title: test.title || '',
      code: test.code || '',
      description: test.description || '',
      version: '1.0',
      active: test.active ?? true
    };
    this.formQuestions = (test.questions || []).map((q, i) => ({
      text: q.text,
      minScore: q.minScore,
      maxScore: q.maxScore,
      orderIndex: i
    }));
    this.showDialog.set(true);
  }

  closeDialog(): void {
    this.showDialog.set(false);
    this.editingTest.set(null);
  }

  addQuestion(): void {
    this.formQuestions.push({
      text: '',
      minScore: 0,
      maxScore: 3,
      orderIndex: this.formQuestions.length
    });
  }

  removeQuestion(index: number): void {
    this.formQuestions.splice(index, 1);
    this.formQuestions.forEach((q, i) => q.orderIndex = i);
  }

  saveTest(): void {
    if (!this.formData.title || !this.formData.code) {
      alert('Please fill in the test title and code.');
      return;
    }

    if (this.formQuestions.length === 0) {
      alert('Please add at least one question.');
      return;
    }

    this.saving.set(true);

    // Build test data matching the Flutter/API format exactly
    const testData = {
      title: this.formData.title.trim(),
      code: this.formData.code.trim().toUpperCase(),
      description: this.formData.description.trim(),
      version: this.formData.version.trim() || '1.0',
      active: this.formData.active,
      questions: this.formQuestions.map((q, i) => ({
        text: q.text,
        code: `${this.formData.code.trim().toUpperCase()}_Q${i + 1}`,
        orderIndex: i,
        minScore: q.minScore,
        maxScore: q.maxScore,
        reverseScored: false
      }))
    };

    const request = this.editingTest()
      ? this.api.updateTest(this.editingTest()!.id, testData)
      : this.api.createTest(testData);

    request.subscribe({
      next: () => {
        this.loadTests();
        this.closeDialog();
        this.saving.set(false);
      },
      error: (err) => {
        console.error('Failed to save test:', err);
        alert('Failed to save test: ' + (err.error?.message || err.message || 'Please try again.'));
        this.saving.set(false);
      }
    });
  }

  deleteTest(test: Test): void {
    if (!confirm(`Are you sure you want to delete "${test.title}"? This action cannot be undone.`)) {
      return;
    }

    this.api.deleteTest(test.id).subscribe({
      next: () => this.loadTests(),
      error: (err) => {
        console.error('Failed to delete test:', err);
        alert('Failed to delete test. Please try again.');
      }
    });
  }
}

