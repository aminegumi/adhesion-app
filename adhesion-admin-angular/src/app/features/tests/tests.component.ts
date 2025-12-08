import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Test, TestType, Question, AnswerOption } from '../../core/models';

@Component({
  selector: 'app-tests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="tests-page">
      <div class="page-header">
        <h2>Psychological Tests</h2>
        <button class="btn btn-primary" (click)="showAddModal = true">
          <span class="material-icons">add</span>
          Create Test
        </button>
      </div>
      
      @if (loading()) {
        <div class="loading-container card">
          <span class="loading-spinner"></span>
          <p>Loading tests...</p>
        </div>
      } @else {
        <div class="tests-grid">
          @for (test of tests(); track test.id) {
            <div class="test-card card">
              <div class="test-header">
                <span class="test-type badge" [ngClass]="getTypeBadgeClass(test.type)">
                  {{ test.type }}
                </span>
                <div class="test-actions">
                  <button class="btn-icon" (click)="editTest(test)">
                    <span class="material-icons">edit</span>
                  </button>
                  <button class="btn-icon danger" (click)="confirmDelete(test)">
                    <span class="material-icons">delete</span>
                  </button>
                </div>
              </div>
              <h3>{{ test.name }}</h3>
              <p class="test-description">{{ test.description }}</p>
              <div class="test-meta">
                <span>
                  <span class="material-icons">help_outline</span>
                  {{ test.questions?.length || 0 }} Questions
                </span>
                <span>
                  <span class="material-icons">event</span>
                  {{ test.createdAt | date:'mediumDate' }}
                </span>
              </div>
              <button class="btn btn-secondary btn-block" (click)="viewTest(test)">
                View Details
              </button>
            </div>
          }
          
          @if (tests().length === 0) {
            <div class="empty-state card">
              <span class="material-icons icon">assignment</span>
              <h3>No tests created yet</h3>
              <p>Create your first psychological test to start assessing patients</p>
            </div>
          }
        </div>
      }
      
      <!-- Create/Edit Test Modal -->
      @if (showAddModal) {
        <div class="modal-overlay" (click)="closeModal()">
          <div class="modal modal-large" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2 class="modal-title">{{ editingTest ? 'Edit Test' : 'Create New Test' }}</h2>
              <button class="modal-close" (click)="closeModal()">&times;</button>
            </div>
            
            <form (ngSubmit)="saveTest()">
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label">Test Name</label>
                  <input type="text" class="form-input" [(ngModel)]="testForm.name" name="name" required />
                </div>
                
                <div class="form-group">
                  <label class="form-label">Test Type</label>
                  <select class="form-input" [(ngModel)]="testForm.type" name="type" required>
                    <option value="ANXIETY">Anxiety</option>
                    <option value="DEPRESSION">Depression</option>
                    <option value="MOTIVATION">Motivation</option>
                    <option value="ADHERENCE">Adherence</option>
                    <option value="GENERAL">General</option>
                  </select>
                </div>
              </div>
              
              <div class="form-group">
                <label class="form-label">Description</label>
                <textarea class="form-input" [(ngModel)]="testForm.description" name="description" rows="3"></textarea>
              </div>
              
              <div class="questions-section">
                <div class="section-header">
                  <h4>Questions</h4>
                  <button type="button" class="btn btn-secondary" (click)="addQuestion()">
                    <span class="material-icons">add</span>
                    Add Question
                  </button>
                </div>
                
                @for (question of testForm.questions; track question.orderIndex; let i = $index) {
                  <div class="question-card">
                    <div class="question-header">
                      <span class="question-number">Q{{ i + 1 }}</span>
                      <button type="button" class="btn-icon danger" (click)="removeQuestion(i)">
                        <span class="material-icons">close</span>
                      </button>
                    </div>
                    <div class="form-group">
                      <input 
                        type="text" 
                        class="form-input" 
                        [(ngModel)]="question.text" 
                        [name]="'question_' + i"
                        placeholder="Enter question text"
                        required
                      />
                    </div>
                    
                    <div class="options-section">
                      <label class="form-label">Answer Options</label>
                      @for (option of question.answerOptions; track option; let j = $index) {
                        <div class="option-row">
                          <input 
                            type="text" 
                            class="form-input" 
                            [(ngModel)]="option.text" 
                            [name]="'option_' + i + '_' + j"
                            placeholder="Option text"
                          />
                          <input 
                            type="number" 
                            class="form-input score-input" 
                            [(ngModel)]="option.score" 
                            [name]="'score_' + i + '_' + j"
                            placeholder="Score"
                            min="0"
                            max="10"
                          />
                          <button type="button" class="btn-icon danger" (click)="removeOption(i, j)">
                            <span class="material-icons">close</span>
                          </button>
                        </div>
                      }
                      <button type="button" class="btn btn-secondary btn-small" (click)="addOption(i)">
                        + Add Option
                      </button>
                    </div>
                  </div>
                }
              </div>
              
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" (click)="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  {{ editingTest ? 'Update Test' : 'Create Test' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }
      
      <!-- View Test Modal -->
      @if (selectedTest) {
        <div class="modal-overlay" (click)="selectedTest = null">
          <div class="modal modal-large" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2 class="modal-title">{{ selectedTest.name }}</h2>
              <button class="modal-close" (click)="selectedTest = null">&times;</button>
            </div>
            
            <div class="test-details">
              <p class="test-description">{{ selectedTest.description }}</p>
              
              <div class="questions-list">
                @for (question of selectedTest.questions; track question.id; let i = $index) {
                  <div class="question-item">
                    <div class="question-text">
                      <span class="q-number">{{ i + 1 }}.</span>
                      {{ question.text }}
                    </div>
                    <div class="options-list">
                      @for (option of question.answerOptions; track option.id) {
                        <div class="option-item">
                          <span class="option-text">{{ option.text }}</span>
                          <span class="option-score">Score: {{ option.score }}</span>
                        </div>
                      }
                    </div>
                  </div>
                }
              </div>
            </div>
          </div>
        </div>
      }
      
      <!-- Delete Confirmation -->
      @if (testToDelete) {
        <div class="modal-overlay" (click)="testToDelete = null">
          <div class="modal" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2 class="modal-title">Delete Test</h2>
              <button class="modal-close" (click)="testToDelete = null">&times;</button>
            </div>
            <p>Are you sure you want to delete <strong>{{ testToDelete.name }}</strong>?</p>
            <div class="modal-footer">
              <button class="btn btn-secondary" (click)="testToDelete = null">Cancel</button>
              <button class="btn btn-danger" (click)="deleteTest()">Delete</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 60px;
      gap: 16px;
    }
    
    .tests-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 20px;
    }
    
    .test-card {
      display: flex;
      flex-direction: column;
    }
    
    .test-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;
    }
    
    .test-actions {
      display: flex;
      gap: 4px;
    }
    
    .test-card h3 {
      margin-bottom: 8px;
    }
    
    .test-description {
      color: var(--text-secondary);
      font-size: 14px;
      flex: 1;
      margin-bottom: 16px;
    }
    
    .test-meta {
      display: flex;
      gap: 16px;
      margin-bottom: 16px;
      font-size: 13px;
      color: var(--text-secondary);
    }
    
    .test-meta span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    
    .test-meta .material-icons {
      font-size: 16px;
    }
    
    .btn-block {
      width: 100%;
      justify-content: center;
    }
    
    .btn-icon {
      background: none;
      border: none;
      padding: 6px;
      border-radius: 6px;
      cursor: pointer;
      color: var(--text-secondary);
    }
    
    .btn-icon:hover {
      background: var(--bg-primary);
    }
    
    .btn-icon.danger:hover {
      background: #fee2e2;
      color: var(--danger-color);
    }
    
    .modal-large {
      max-width: 800px;
      max-height: 90vh;
      overflow-y: auto;
    }
    
    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }
    
    .questions-section {
      margin-top: 24px;
      border-top: 1px solid var(--border-color);
      padding-top: 24px;
    }
    
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    
    .question-card {
      background: var(--bg-primary);
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 16px;
    }
    
    .question-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12px;
    }
    
    .question-number {
      font-weight: 600;
      color: var(--primary-color);
    }
    
    .options-section {
      margin-top: 12px;
    }
    
    .option-row {
      display: flex;
      gap: 8px;
      margin-bottom: 8px;
    }
    
    .score-input {
      width: 80px;
    }
    
    .btn-small {
      padding: 6px 12px;
      font-size: 12px;
    }
    
    .test-details {
      max-height: 60vh;
      overflow-y: auto;
    }
    
    .questions-list {
      margin-top: 20px;
    }
    
    .question-item {
      background: var(--bg-primary);
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 12px;
    }
    
    .question-text {
      font-weight: 500;
      margin-bottom: 12px;
    }
    
    .q-number {
      color: var(--primary-color);
      margin-right: 8px;
    }
    
    .options-list {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 8px;
    }
    
    .option-item {
      display: flex;
      justify-content: space-between;
      padding: 8px 12px;
      background: white;
      border-radius: 6px;
      font-size: 14px;
    }
    
    .option-score {
      color: var(--text-secondary);
      font-size: 12px;
    }
    
    @media (max-width: 1024px) {
      .tests-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
    
    @media (max-width: 768px) {
      .tests-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class TestsComponent implements OnInit {
  private api = inject(ApiService);

  tests = signal<Test[]>([]);
  loading = signal(true);
  showAddModal = false;
  editingTest: Test | null = null;
  selectedTest: Test | null = null;
  testToDelete: Test | null = null;

  testForm: {
    name: string;
    description: string;
    type: TestType;
    questions: { text: string; orderIndex: number; answerOptions: { text: string; score: number }[] }[];
  } = {
    name: '',
    description: '',
    type: 'GENERAL',
    questions: []
  };

  ngOnInit(): void {
    this.loadTests();
  }

  loadTests(): void {
    this.loading.set(true);
    this.api.getTests().subscribe({
      next: (tests) => {
        this.tests.set(tests);
        this.loading.set(false);
      },
      error: () => {
        // Mock data
        this.tests.set([
          {
            id: '1',
            name: 'GAD-7 Anxiety Assessment',
            description: 'Generalized Anxiety Disorder 7-item scale for measuring anxiety severity.',
            type: 'ANXIETY',
            createdAt: new Date().toISOString(),
            questions: [
              { id: 'q1', text: 'Feeling nervous, anxious, or on edge', orderIndex: 1, answerOptions: [
                { id: 'o1', text: 'Not at all', score: 0 },
                { id: 'o2', text: 'Several days', score: 1 },
                { id: 'o3', text: 'More than half the days', score: 2 },
                { id: 'o4', text: 'Nearly every day', score: 3 }
              ]}
            ]
          },
          {
            id: '2',
            name: 'PHQ-9 Depression Screening',
            description: 'Patient Health Questionnaire for depression screening and severity measurement.',
            type: 'DEPRESSION',
            createdAt: new Date().toISOString(),
            questions: []
          },
          {
            id: '3',
            name: 'Medication Adherence Scale',
            description: 'Assessment of patient adherence to prescribed medication regimens.',
            type: 'ADHERENCE',
            createdAt: new Date().toISOString(),
            questions: []
          }
        ]);
        this.loading.set(false);
      }
    });
  }

  getTypeBadgeClass(type: TestType): string {
    const classes: Record<TestType, string> = {
      'ANXIETY': 'badge-warning',
      'DEPRESSION': 'badge-danger',
      'MOTIVATION': 'badge-info',
      'ADHERENCE': 'badge-success',
      'GENERAL': 'badge-info'
    };
    return classes[type];
  }

  viewTest(test: Test): void {
    this.selectedTest = test;
  }

  editTest(test: Test): void {
    this.editingTest = test;
    this.testForm = {
      name: test.name,
      description: test.description,
      type: test.type,
      questions: test.questions.map(q => ({
        text: q.text,
        orderIndex: q.orderIndex,
        answerOptions: q.answerOptions.map(o => ({ text: o.text, score: o.score }))
      }))
    };
    this.showAddModal = true;
  }

  addQuestion(): void {
    this.testForm.questions.push({
      text: '',
      orderIndex: this.testForm.questions.length + 1,
      answerOptions: [
        { text: '', score: 0 },
        { text: '', score: 1 },
        { text: '', score: 2 },
        { text: '', score: 3 }
      ]
    });
  }

  removeQuestion(index: number): void {
    this.testForm.questions.splice(index, 1);
  }

  addOption(questionIndex: number): void {
    this.testForm.questions[questionIndex].answerOptions.push({ text: '', score: 0 });
  }

  removeOption(questionIndex: number, optionIndex: number): void {
    this.testForm.questions[questionIndex].answerOptions.splice(optionIndex, 1);
  }

  saveTest(): void {
    if (this.editingTest) {
      this.api.updateTest(this.editingTest.id, this.testForm as Partial<Test>).subscribe({
        next: () => {
          this.closeModal();
          this.loadTests();
        }
      });
    } else {
      this.api.createTest(this.testForm as Partial<Test>).subscribe({
        next: () => {
          this.closeModal();
          this.loadTests();
        }
      });
    }
  }

  confirmDelete(test: Test): void {
    this.testToDelete = test;
  }

  deleteTest(): void {
    if (this.testToDelete) {
      this.api.deleteTest(this.testToDelete.id).subscribe({
        next: () => {
          this.testToDelete = null;
          this.loadTests();
        }
      });
    }
  }

  closeModal(): void {
    this.showAddModal = false;
    this.editingTest = null;
    this.testForm = { name: '', description: '', type: 'GENERAL', questions: [] };
  }
}
