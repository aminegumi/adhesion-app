import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, Test } from '../../../core/services/api.service';

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
        <button class="btn btn-danger" (click)="showCreateDialog = true">+ New Test</button>
      </header>

      <div class="content">
        @for (test of tests(); track test.id) {
          <div class="test-card">
            <div class="test-header">
              <h3>{{ test.title }}</h3>
              <span class="badge">{{ test.code }}</span>
            </div>
            <p>{{ test.description }}</p>
            <div class="test-footer">
              <span>{{ test.questions?.length || 0 }} questions</span>
              <div class="actions">
                <button class="btn btn-sm btn-ghost" (click)="editTest(test)">Edit</button>
                <button class="btn btn-sm btn-danger" (click)="deleteTest(test.id)">Delete</button>
              </div>
            </div>
          </div>
        } @empty {
          <div class="empty-state">
            <p>No tests created yet</p>
          </div>
        }
      </div>

      @if (showCreateDialog) {
        <div class="dialog-overlay" (click)="showCreateDialog = false">
          <div class="dialog" (click)="$event.stopPropagation()">
            <h2>{{ editingTest ? 'Edit Test' : 'Create Test' }}</h2>
            <div class="form-group">
              <label>Test Title</label>
              <input type="text" [(ngModel)]="formData.title" class="form-input-dark" placeholder="e.g., PHQ-9 Depression Scale">
            </div>
            <div class="form-group">
              <label>Test Code</label>
              <input type="text" [(ngModel)]="formData.code" class="form-input-dark" placeholder="e.g., PHQ9">
            </div>
            <div class="form-group">
              <label>Description</label>
              <textarea [(ngModel)]="formData.description" class="form-input-dark" rows="3" placeholder="Brief description..."></textarea>
            </div>
            <div class="dialog-actions">
              <button class="btn btn-ghost" (click)="showCreateDialog = false">Cancel</button>
              <button class="btn btn-danger" (click)="saveTest()">{{ editingTest ? 'Update' : 'Create' }}</button>
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
    .back-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: rgba(255,255,255,0.1);
      border-radius: 10px;
      color: white;
    }
    .content { max-width: 800px; margin: 0 auto; padding: 24px; }
    .test-card {
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 16px;
      padding: 24px;
      margin-bottom: 16px;
    }
    .test-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 12px;
      h3 { color: white; font-size: 18px; }
    }
    .badge {
      padding: 4px 10px;
      background: rgba(99, 102, 241, 0.2);
      color: var(--primary);
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
    }
    .test-card p { color: rgba(255,255,255,0.6); margin-bottom: 16px; }
    .test-footer {
      display: flex; justify-content: space-between; align-items: center;
      span { color: rgba(255,255,255,0.5); font-size: 13px; }
    }
    .actions { display: flex; gap: 8px; }
    .btn-sm { padding: 6px 12px; font-size: 13px; }
    .btn-ghost { background: rgba(255,255,255,0.1); color: white; }
    .empty-state { text-align: center; padding: 48px; color: rgba(255,255,255,0.5); }
    .dialog-overlay {
      position: fixed; inset: 0;
      background: rgba(0,0,0,0.7);
      display: flex; align-items: center; justify-content: center;
      z-index: 1000;
    }
    .dialog {
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 20px;
      padding: 32px;
      width: 100%; max-width: 480px;
      h2 { color: white; margin-bottom: 24px; }
    }
    .form-group {
      margin-bottom: 16px;
      label { display: block; color: rgba(255,255,255,0.7); font-size: 14px; margin-bottom: 8px; }
    }
    .form-input-dark {
      width: 100%; padding: 12px 16px;
      background: rgba(255,255,255,0.05);
      border: 1px solid var(--dark-border);
      border-radius: 10px;
      color: white; font-size: 15px;
      &:focus { outline: none; border-color: var(--admin-primary); }
    }
    textarea.form-input-dark { resize: none; font-family: inherit; }
    .dialog-actions { display: flex; gap: 12px; margin-top: 24px; }
    .dialog-actions .btn { flex: 1; }
  `]
})
export class TestManagementComponent implements OnInit {
  private api = inject(ApiService);

  tests = signal<Test[]>([]);
  showCreateDialog = false;
  editingTest: Test | null = null;
  formData = { title: '', code: '', description: '' };

  ngOnInit(): void { this.loadTests(); }

  loadTests(): void {
    this.api.getTests().subscribe(t => this.tests.set(t));
  }

  editTest(test: Test): void {
    this.editingTest = test;
    this.formData = { title: test.title || '', code: test.code || '', description: test.description || '' };
    this.showCreateDialog = true;
  }

  saveTest(): void {
    if (!this.formData.title || !this.formData.code) return;
    const data = { ...this.formData, active: true, questions: [] };
    if (this.editingTest) {
      this.api.updateTest(this.editingTest.id, data).subscribe(() => {
        this.loadTests();
        this.closeDialog();
      });
    } else {
      this.api.createTest(data).subscribe(() => {
        this.loadTests();
        this.closeDialog();
      });
    }
  }

  deleteTest(id: number): void {
    if (confirm('Are you sure you want to delete this test?')) {
      this.api.deleteTest(id).subscribe(() => this.loadTests());
    }
  }

  closeDialog(): void {
    this.showCreateDialog = false;
    this.editingTest = null;
    this.formData = { title: '', code: '', description: '' };
  }
}

