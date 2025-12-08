import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { TreatmentPlan, DailyTask } from '../../core/models';

@Component({
  selector: 'app-treatment-plans',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="treatment-plans-page">
      <div class="page-header">
        <h2>Treatment Plans</h2>
        <div class="filters">
          <select class="form-input" [(ngModel)]="filterStatus" (change)="filterPlans()">
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>
      
      @if (loading()) {
        <div class="loading-container card">
          <span class="loading-spinner"></span>
          <p>Loading treatment plans...</p>
        </div>
      } @else if (filteredPlans().length === 0) {
        <div class="empty-state card">
          <span class="material-icons icon">healing</span>
          <h3>No treatment plans found</h3>
          <p>Treatment plans will appear here once created for patients</p>
        </div>
      } @else {
        <div class="plans-list">
          @for (plan of filteredPlans(); track plan.id) {
            <div class="plan-card card">
              <div class="plan-header">
                <div class="plan-info">
                  <h3>{{ plan.title }}</h3>
                  <p>{{ plan.description }}</p>
                  <div class="plan-meta">
                    <span class="meta-item">
                      <span class="material-icons">person</span>
                      Patient {{ plan.userId.slice(0, 8) }}...
                    </span>
                    <span class="meta-item">
                      <span class="material-icons">event</span>
                      {{ plan.startDate | date:'mediumDate' }} - {{ plan.endDate | date:'mediumDate' }}
                    </span>
                    <span class="meta-item">
                      <span class="material-icons">task_alt</span>
                      {{ getCompletedTasks(plan) }}/{{ plan.dailyTasks.length }} tasks completed
                    </span>
                  </div>
                </div>
                <div class="plan-status">
                  <span class="badge" [ngClass]="getStatusBadgeClass(plan.status)">
                    {{ plan.status }}
                  </span>
                </div>
              </div>
              
              <div class="progress-section">
                <div class="progress-header">
                  <span>Progress</span>
                  <span>{{ getProgressPercentage(plan) }}%</span>
                </div>
                <div class="progress-bar">
                  <div class="progress" [style.width.%]="getProgressPercentage(plan)"></div>
                </div>
              </div>
              
              <div class="tasks-section">
                <div class="tasks-header">
                  <h4>Daily Tasks</h4>
                  <button class="btn btn-secondary btn-small" (click)="toggleTasks(plan.id)">
                    {{ expandedPlan === plan.id ? 'Hide' : 'Show' }} Tasks
                  </button>
                </div>
                
                @if (expandedPlan === plan.id) {
                  <div class="tasks-grid">
                    @for (task of plan.dailyTasks; track task.id) {
                      <div class="task-card" [class.completed]="task.completed">
                        <div class="task-header">
                          <span class="material-icons task-icon">
                            {{ task.completed ? 'check_circle' : 'radio_button_unchecked' }}
                          </span>
                          <span class="task-type badge" [ngClass]="getTaskTypeBadgeClass(task.taskType)">
                            {{ task.taskType }}
                          </span>
                        </div>
                        <h5>{{ task.title }}</h5>
                        <p>{{ task.description }}</p>
                        <div class="task-meta">
                          <span class="material-icons">schedule</span>
                          {{ task.scheduledTime }}
                          <span class="day-badge">Day {{ task.dayNumber }}</span>
                        </div>
                      </div>
                    }
                  </div>
                }
              </div>
            </div>
          }
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
    
    .filters .form-input {
      width: 160px;
    }
    
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 60px;
      gap: 16px;
    }
    
    .plans-list {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    
    .plan-card {
      padding: 24px;
    }
    
    .plan-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 20px;
    }
    
    .plan-info h3 {
      margin-bottom: 8px;
    }
    
    .plan-info p {
      color: var(--text-secondary);
      margin-bottom: 12px;
    }
    
    .plan-meta {
      display: flex;
      gap: 24px;
      flex-wrap: wrap;
    }
    
    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: var(--text-secondary);
    }
    
    .meta-item .material-icons {
      font-size: 16px;
    }
    
    .progress-section {
      margin-bottom: 20px;
    }
    
    .progress-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      font-size: 14px;
    }
    
    .progress-bar {
      height: 8px;
      background: #e5e7eb;
      border-radius: 4px;
      overflow: hidden;
    }
    
    .progress {
      height: 100%;
      background: linear-gradient(90deg, #4f46e5, #7c3aed);
      border-radius: 4px;
      transition: width 0.3s;
    }
    
    .tasks-section {
      border-top: 1px solid var(--border-color);
      padding-top: 20px;
    }
    
    .tasks-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    
    .tasks-header h4 {
      margin: 0;
    }
    
    .btn-small {
      padding: 6px 12px;
      font-size: 12px;
    }
    
    .tasks-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
    }
    
    .task-card {
      background: var(--bg-primary);
      border-radius: 10px;
      padding: 16px;
      border: 1px solid var(--border-color);
      transition: all 0.2s;
    }
    
    .task-card.completed {
      background: #d1fae5;
      border-color: #a7f3d0;
    }
    
    .task-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }
    
    .task-icon {
      color: var(--text-secondary);
    }
    
    .task-card.completed .task-icon {
      color: var(--secondary-color);
    }
    
    .task-card h5 {
      margin-bottom: 8px;
      font-size: 14px;
    }
    
    .task-card p {
      font-size: 12px;
      color: var(--text-secondary);
      margin-bottom: 12px;
      line-height: 1.4;
    }
    
    .task-meta {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: var(--text-secondary);
    }
    
    .task-meta .material-icons {
      font-size: 14px;
    }
    
    .day-badge {
      background: white;
      padding: 2px 8px;
      border-radius: 10px;
      margin-left: auto;
      font-weight: 500;
    }
    
    @media (max-width: 1200px) {
      .tasks-grid {
        grid-template-columns: repeat(3, 1fr);
      }
    }
    
    @media (max-width: 900px) {
      .tasks-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
    
    @media (max-width: 600px) {
      .tasks-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class TreatmentPlansComponent implements OnInit {
  private api = inject(ApiService);

  plans = signal<TreatmentPlan[]>([]);
  filteredPlans = signal<TreatmentPlan[]>([]);
  loading = signal(true);
  filterStatus = '';
  expandedPlan: string | null = null;

  ngOnInit(): void {
    this.loadPlans();
  }

  loadPlans(): void {
    this.loading.set(true);
    this.api.getTreatmentPlans().subscribe({
      next: (plans) => {
        this.plans.set(plans);
        this.filteredPlans.set(plans);
        this.loading.set(false);
      },
      error: () => {
        // Mock data
        const mockPlans: TreatmentPlan[] = [
          {
            id: '1',
            userId: 'user-123-abc-def',
            title: '14-Day Anxiety Management Program',
            description: 'A comprehensive program designed to help manage anxiety through structured daily activities.',
            startDate: new Date().toISOString(),
            endDate: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString(),
            status: 'ACTIVE',
            createdAt: new Date().toISOString(),
            dailyTasks: [
              { id: 't1', title: 'Morning Meditation', description: '10-minute guided meditation', taskType: 'THERAPY', scheduledTime: '08:00', completed: true, dayNumber: 1 },
              { id: 't2', title: 'Take Medication', description: 'Morning dose of prescribed medication', taskType: 'MEDICATION', scheduledTime: '09:00', completed: true, dayNumber: 1 },
              { id: 't3', title: 'Breathing Exercises', description: 'Deep breathing exercises for 5 minutes', taskType: 'THERAPY', scheduledTime: '12:00', completed: false, dayNumber: 1 },
              { id: 't4', title: 'Evening Walk', description: '20-minute relaxing walk', taskType: 'EXERCISE', scheduledTime: '18:00', completed: false, dayNumber: 1 },
              { id: 't5', title: 'Journaling', description: 'Write about your day and feelings', taskType: 'LIFESTYLE', scheduledTime: '21:00', completed: false, dayNumber: 1 },
              { id: 't6', title: 'Morning Medication', description: 'Day 2 morning dose', taskType: 'MEDICATION', scheduledTime: '09:00', completed: false, dayNumber: 2 }
            ]
          },
          {
            id: '2',
            userId: 'user-456-ghi-jkl',
            title: 'Depression Recovery Plan',
            description: 'A supportive plan focused on gradual recovery and building healthy habits.',
            startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
            endDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
            status: 'ACTIVE',
            createdAt: new Date().toISOString(),
            dailyTasks: [
              { id: 't7', title: 'Morning Stretch', description: '5-minute gentle stretching', taskType: 'EXERCISE', scheduledTime: '07:00', completed: true, dayNumber: 1 },
              { id: 't8', title: 'Medication', description: 'Take prescribed antidepressant', taskType: 'MEDICATION', scheduledTime: '08:00', completed: true, dayNumber: 1 },
              { id: 't9', title: 'Social Interaction', description: 'Call or meet a friend/family member', taskType: 'LIFESTYLE', scheduledTime: '14:00', completed: true, dayNumber: 1 },
              { id: 't10', title: 'Therapy Session', description: 'Attend scheduled therapy session', taskType: 'THERAPY', scheduledTime: '16:00', completed: true, dayNumber: 1 }
            ]
          }
        ];
        this.plans.set(mockPlans);
        this.filteredPlans.set(mockPlans);
        this.loading.set(false);
      }
    });
  }

  filterPlans(): void {
    let filtered = this.plans();
    
    if (this.filterStatus) {
      filtered = filtered.filter(p => p.status === this.filterStatus);
    }
    
    this.filteredPlans.set(filtered);
  }

  getCompletedTasks(plan: TreatmentPlan): number {
    return plan.dailyTasks.filter(t => t.completed).length;
  }

  getProgressPercentage(plan: TreatmentPlan): number {
    if (plan.dailyTasks.length === 0) return 0;
    return Math.round((this.getCompletedTasks(plan) / plan.dailyTasks.length) * 100);
  }

  getStatusBadgeClass(status: string): string {
    const classes: Record<string, string> = {
      'ACTIVE': 'badge-success',
      'COMPLETED': 'badge-info',
      'CANCELLED': 'badge-danger'
    };
    return classes[status] || 'badge-info';
  }

  getTaskTypeBadgeClass(taskType: string): string {
    const classes: Record<string, string> = {
      'MEDICATION': 'badge-danger',
      'EXERCISE': 'badge-success',
      'THERAPY': 'badge-info',
      'LIFESTYLE': 'badge-warning'
    };
    return classes[taskType] || 'badge-info';
  }

  toggleTasks(planId: string): void {
    this.expandedPlan = this.expandedPlan === planId ? null : planId;
  }
}
