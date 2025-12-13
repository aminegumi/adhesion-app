import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, TreatmentPlan } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

interface PlanTask {
  id: number;
  name: string;
  time: string;
  completed: boolean;
  type: 'medication' | 'activity' | 'appointment';
  planId: number;
  planTitle: string;
}

interface NewMedication {
  name: string;
  dosage: string;
  frequency: string;
  times: string[];
}

@Component({
  selector: 'app-treatment-plans',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="plans-page">
      <header class="page-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>Treatment Plans</h1>
        <button class="add-btn" (click)="showCreateDialog.set(true)">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
        </button>
      </header>

      <!-- Tab Navigation -->
      <div class="tab-nav">
        <button 
          class="tab-btn" 
          [class.active]="activeTab() === 'today'"
          (click)="activeTab.set('today')"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM9 10H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zm-8 4H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2z"/></svg>
          Today's Tasks
        </button>
        <button 
          class="tab-btn" 
          [class.active]="activeTab() === 'plans'"
          (click)="activeTab.set('plans')"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
          My Plans
        </button>
      </div>

      <div class="content">
        <!-- Today's Tasks Tab -->
        @if (activeTab() === 'today') {
          <div class="today-section">
            <div class="date-header">
              <span class="day">{{ todayDate }}</span>
              <span class="progress-badge">{{ completedTasksCount() }}/{{ todayTasks().length }} completed</span>
            </div>

            @if (todayTasks().length > 0) {
              <div class="tasks-list">
                @for (task of todayTasks(); track task.id) {
                  <div class="task-item" [class.completed]="task.completed">
                    <button class="task-checkbox" (click)="toggleTask(task)">
                      @if (task.completed) {
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
                      } @else {
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z"/></svg>
                      }
                    </button>
                    <div class="task-content">
                      <span class="task-name">{{ task.name }}</span>
                      <span class="task-meta">
                        <span class="task-time">{{ task.time }}</span>
                        <span class="task-plan">{{ task.planTitle }}</span>
                      </span>
                    </div>
                    <span class="task-type-icon" [class]="task.type">
                      @if (task.type === 'medication') { 💊 }
                      @else if (task.type === 'activity') { 🏃 }
                      @else { 📅 }
                    </span>
                  </div>
                }
              </div>
            } @else {
              <div class="empty-state small">
                <div class="icon">🎉</div>
                <h3>All caught up!</h3>
                <p>No tasks scheduled for today.</p>
              </div>
            }
          </div>
        }

        <!-- Plans Tab -->
        @if (activeTab() === 'plans') {
          @for (plan of plans(); track plan.id) {
            <div class="plan-card" [class.active]="plan.status === 'ACTIVE'" (click)="selectPlan(plan)">
              <div class="plan-header">
                <h3>{{ plan.title }}</h3>
                <span class="status-badge" [class]="plan.status?.toLowerCase()">{{ plan.status }}</span>
              </div>
              <p>{{ plan.description }}</p>
              <div class="progress-section">
                <div class="progress-bar">
                  <div class="fill" [style.width.%]="plan.progressPercentage"></div>
                </div>
                <span>{{ plan.progressPercentage }}% complete</span>
              </div>
              <div class="plan-footer">
                <div class="meds-count">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M4.22 11.29l4.95-4.95a4.97 4.97 0 0 1 7.07 0l2.12 2.12a4.97 4.97 0 0 1 0 7.07l-4.95 4.95a4.97 4.97 0 0 1-7.07 0l-2.12-2.12a4.97 4.97 0 0 1 0-7.07z"/></svg>
                  {{ plan.medicationsList?.length || 0 }} medications
                </div>
                <div class="plan-actions">
                  <button class="action-btn edit" (click)="editPlan(plan, $event)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
                  </button>
                  <button class="action-btn delete" (click)="deletePlan(plan, $event)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
                  </button>
                </div>
              </div>
            </div>
          } @empty {
            <div class="empty-state">
              <div class="icon">📋</div>
              <h3>No Treatment Plans</h3>
              <p>Create a treatment plan to start tracking your medications and health goals.</p>
              <button class="btn btn-primary" (click)="showCreateDialog.set(true)">
                Create Your First Plan
              </button>
            </div>
          }
        }
      </div>

      <!-- Create Plan Dialog -->
      @if (showCreateDialog()) {
        <div class="dialog-overlay" (click)="closeDialog()">
          <div class="dialog" (click)="$event.stopPropagation()">
            <div class="dialog-header">
              <h2>{{ editingPlan() ? 'Edit' : 'Create' }} Treatment Plan</h2>
              <button class="close-btn" (click)="closeDialog()">×</button>
            </div>

            <div class="dialog-content">
              <div class="form-group">
                <label>Plan Name</label>
                <input type="text" [(ngModel)]="newPlan.title" placeholder="e.g., Heart Health Plan">
              </div>

              <div class="form-group">
                <label>Description</label>
                <textarea [(ngModel)]="newPlan.description" placeholder="Describe your treatment goals..."></textarea>
              </div>

              <!-- Medications Section -->
              <div class="medications-section">
                <div class="section-header">
                  <h3>Medications</h3>
                  <button class="add-med-btn" (click)="addMedicationField()">+ Add Medication</button>
                </div>

                @for (med of newPlanMedications; track $index; let i = $index) {
                  <div class="medication-form">
                    <div class="med-header">
                      <span>Medication {{ i + 1 }}</span>
                      <button class="remove-btn" (click)="removeMedicationField(i)">×</button>
                    </div>
                    <div class="med-row">
                      <input type="text" [(ngModel)]="med.name" placeholder="Medication name">
                      <input type="text" [(ngModel)]="med.dosage" placeholder="Dosage (e.g., 10mg)">
                    </div>
                    <div class="med-row">
                      <select [(ngModel)]="med.frequency">
                        <option value="daily">Daily</option>
                        <option value="twice">Twice Daily</option>
                        <option value="weekly">Weekly</option>
                      </select>
                    </div>
                    <div class="times-row">
                      <label>Times:</label>
                      <div class="time-chips">
                        @for (time of commonTimes; track time) {
                          <button 
                            class="time-chip" 
                            [class.selected]="med.times.includes(time)"
                            (click)="toggleMedTime(med, time)"
                          >{{ time }}</button>
                        }
                      </div>
                    </div>
                  </div>
                }
              </div>
            </div>

            <div class="dialog-footer">
              <button class="btn btn-secondary" (click)="closeDialog()">Cancel</button>
              <button 
                class="btn btn-primary" 
                (click)="savePlan()"
                [disabled]="!newPlan.title"
              >
                {{ editingPlan() ? 'Update' : 'Create' }} Plan
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Plan Details Dialog -->
      @if (selectedPlan()) {
        <div class="dialog-overlay" (click)="selectedPlan.set(null)">
          <div class="dialog plan-details" (click)="$event.stopPropagation()">
            <div class="dialog-header">
              <h2>{{ selectedPlan()?.title }}</h2>
              <button class="close-btn" (click)="selectedPlan.set(null)">×</button>
            </div>
            <div class="dialog-content">
              <p class="plan-description">{{ selectedPlan()?.description }}</p>
              
              <div class="detail-section">
                <h3>Progress</h3>
                <div class="progress-bar large">
                  <div class="fill" [style.width.%]="selectedPlan()?.progressPercentage"></div>
                </div>
                <span class="progress-text">{{ selectedPlan()?.progressPercentage }}% complete</span>
              </div>

              <div class="detail-section">
                <h3>Medications ({{ selectedPlan()?.medicationsList?.length || 0 }})</h3>
                <div class="meds-list">
                  @for (med of selectedPlan()?.medicationsList; track med.id) {
                    <div class="med-item">
                      <span class="med-icon">💊</span>
                      <div class="med-info">
                        <span class="med-name">{{ med.name }}</span>
                        <span class="med-dosage">{{ med.dosage }} - {{ med.frequency }}</span>
                      </div>
                      <span class="med-status" [class.active]="med.isActive">
                        {{ med.isActive ? 'Active' : 'Inactive' }}
                      </span>
                    </div>
                  } @empty {
                    <p class="no-meds">No medications in this plan</p>
                  }
                </div>
              </div>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .plans-page { min-height: 100vh; background: linear-gradient(180deg, #FDF4FF 0%, #FAF5FF 100%); }
    
    .page-header {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: white;
      border-bottom: 1px solid var(--gray-200);
      h1 { flex: 1; font-size: 20px; }
    }
    
    .back-btn, .add-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: var(--gray-100);
      border: none;
      border-radius: 10px;
      color: var(--gray-600);
      cursor: pointer;
      transition: all 0.2s;
      &:hover { background: var(--gray-200); }
    }
    
    .add-btn {
      background: linear-gradient(135deg, var(--primary), #8B5CF6);
      color: white;
      &:hover { transform: scale(1.05); }
    }

    .tab-nav {
      display: flex;
      background: white;
      padding: 12px 24px;
      gap: 8px;
      border-bottom: 1px solid var(--gray-200);
    }

    .tab-btn {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 12px 16px;
      background: var(--gray-100);
      border: none;
      border-radius: 12px;
      font-size: 14px;
      font-weight: 500;
      color: var(--gray-600);
      cursor: pointer;
      transition: all 0.2s;

      &.active {
        background: var(--primary);
        color: white;
      }

      &:hover:not(.active) {
        background: var(--gray-200);
      }
    }
    
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }

    /* Today's Tasks */
    .today-section {
      .date-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
        
        .day {
          font-size: 18px;
          font-weight: 600;
          color: var(--gray-800);
        }
        
        .progress-badge {
          padding: 6px 12px;
          background: rgba(16, 185, 129, 0.1);
          color: var(--success);
          border-radius: 20px;
          font-size: 13px;
          font-weight: 500;
        }
      }
    }

    .tasks-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .task-item {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 16px;
      background: white;
      border-radius: 16px;
      box-shadow: var(--shadow-sm);
      transition: all 0.2s;

      &.completed {
        opacity: 0.6;
        .task-name { text-decoration: line-through; }
      }
    }

    .task-checkbox {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: transparent;
      border: none;
      color: var(--gray-400);
      cursor: pointer;
      transition: all 0.2s;

      &:hover { color: var(--success); transform: scale(1.1); }
    }

    .task-item.completed .task-checkbox {
      color: var(--success);
    }

    .task-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .task-name {
      font-weight: 500;
      color: var(--gray-800);
    }

    .task-meta {
      display: flex;
      gap: 12px;
      font-size: 12px;
      color: var(--gray-500);
    }

    .task-type-icon {
      font-size: 20px;
    }
    
    /* Plan Cards */
    .plan-card {
      background: white;
      border-radius: 20px;
      padding: 24px;
      margin-bottom: 16px;
      box-shadow: var(--shadow-md);
      border-left: 4px solid var(--gray-300);
      cursor: pointer;
      transition: all 0.2s;
      
      &:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }
      &.active { border-left-color: var(--success); }
    }
    
    .plan-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 12px;
      h3 { font-size: 18px; }
    }
    
    .status-badge {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      &.active { background: rgba(16, 185, 129, 0.1); color: var(--success); }
      &.completed { background: rgba(99, 102, 241, 0.1); color: var(--primary); }
      &.paused { background: rgba(245, 158, 11, 0.1); color: var(--warning); }
    }
    
    .plan-card p { color: var(--gray-600); margin-bottom: 16px; }
    
    .progress-section {
      margin-bottom: 12px;
      span { font-size: 12px; color: var(--gray-500); }
    }
    
    .progress-bar {
      height: 6px; background: var(--gray-200); border-radius: 3px; overflow: hidden; margin-bottom: 6px;
      .fill { height: 100%; background: linear-gradient(90deg, var(--success), #34D399); transition: width 0.3s; }
      &.large { height: 10px; border-radius: 5px; }
    }

    .plan-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .meds-count { 
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px; 
      color: var(--gray-500);
      svg { color: var(--primary); }
    }

    .plan-actions {
      display: flex;
      gap: 8px;
    }

    .action-btn {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;

      &.edit {
        background: rgba(99, 102, 241, 0.1);
        color: var(--primary);
        &:hover { background: rgba(99, 102, 241, 0.2); }
      }

      &.delete {
        background: rgba(239, 68, 68, 0.1);
        color: var(--error);
        &:hover { background: rgba(239, 68, 68, 0.2); }
      }
    }
    
    .empty-state {
      text-align: center;
      padding: 60px 24px;
      background: white;
      border-radius: 24px;
      .icon { font-size: 64px; margin-bottom: 16px; }
      h3 { margin-bottom: 8px; }
      p { color: var(--gray-600); margin-bottom: 20px; }

      &.small {
        padding: 40px 24px;
        .icon { font-size: 48px; }
      }
    }

    /* Dialog Styles */
    .dialog-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 20px;
    }

    .dialog {
      background: white;
      border-radius: 24px;
      width: 100%;
      max-width: 500px;
      max-height: 90vh;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
      border-bottom: 1px solid var(--gray-200);

      h2 { font-size: 20px; }
    }

    .close-btn {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--gray-100);
      border: none;
      border-radius: 50%;
      font-size: 24px;
      cursor: pointer;
      &:hover { background: var(--gray-200); }
    }

    .dialog-content {
      padding: 24px;
      overflow-y: auto;
      flex: 1;
    }

    .form-group {
      margin-bottom: 20px;

      label {
        display: block;
        font-size: 14px;
        font-weight: 500;
        color: var(--gray-700);
        margin-bottom: 8px;
      }

      input, textarea, select {
        width: 100%;
        padding: 12px 16px;
        border: 1px solid var(--gray-300);
        border-radius: 12px;
        font-size: 14px;
        transition: all 0.2s;

        &:focus {
          outline: none;
          border-color: var(--primary);
          box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
        }
      }

      textarea {
        min-height: 100px;
        resize: vertical;
      }
    }

    .medications-section {
      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;

        h3 {
          font-size: 16px;
          color: var(--gray-800);
        }
      }
    }

    .add-med-btn {
      padding: 8px 16px;
      background: rgba(99, 102, 241, 0.1);
      color: var(--primary);
      border: none;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      &:hover { background: rgba(99, 102, 241, 0.2); }
    }

    .medication-form {
      background: var(--gray-50);
      border-radius: 16px;
      padding: 16px;
      margin-bottom: 12px;
    }

    .med-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      font-weight: 500;
      color: var(--gray-600);
    }

    .remove-btn {
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(239, 68, 68, 0.1);
      color: var(--error);
      border: none;
      border-radius: 50%;
      cursor: pointer;
      font-size: 18px;
      &:hover { background: rgba(239, 68, 68, 0.2); }
    }

    .med-row {
      display: flex;
      gap: 12px;
      margin-bottom: 12px;

      input, select {
        flex: 1;
        padding: 10px 14px;
        border: 1px solid var(--gray-300);
        border-radius: 10px;
        font-size: 14px;
      }
    }

    .times-row {
      label {
        font-size: 13px;
        color: var(--gray-600);
        margin-bottom: 8px;
        display: block;
      }
    }

    .time-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .time-chip {
      padding: 8px 14px;
      background: white;
      border: 1px solid var(--gray-300);
      border-radius: 20px;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s;

      &.selected {
        background: var(--primary);
        color: white;
        border-color: var(--primary);
      }

      &:hover:not(.selected) {
        border-color: var(--primary);
      }
    }

    .dialog-footer {
      display: flex;
      gap: 12px;
      padding: 16px 24px;
      border-top: 1px solid var(--gray-200);
      background: var(--gray-50);
    }

    .btn {
      flex: 1;
      padding: 14px 20px;
      border-radius: 12px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      border: none;

      &.btn-secondary {
        background: white;
        border: 1px solid var(--gray-300);
        color: var(--gray-700);
        &:hover { background: var(--gray-100); }
      }

      &.btn-primary {
        background: linear-gradient(135deg, var(--primary), #8B5CF6);
        color: white;
        &:hover:not(:disabled) { transform: translateY(-2px); }
        &:disabled { opacity: 0.6; cursor: not-allowed; }
      }
    }

    /* Plan Details Dialog */
    .plan-details {
      .plan-description {
        color: var(--gray-600);
        margin-bottom: 24px;
        line-height: 1.6;
      }

      .detail-section {
        margin-bottom: 24px;

        h3 {
          font-size: 14px;
          color: var(--gray-500);
          margin-bottom: 12px;
        }
      }

      .progress-text {
        display: block;
        text-align: center;
        margin-top: 8px;
        font-size: 14px;
        color: var(--gray-600);
      }

      .meds-list {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }

      .med-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 14px;
        background: var(--gray-50);
        border-radius: 12px;
      }

      .med-icon {
        font-size: 24px;
      }

      .med-info {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 2px;
      }

      .med-name {
        font-weight: 500;
        color: var(--gray-800);
      }

      .med-dosage {
        font-size: 13px;
        color: var(--gray-500);
      }

      .med-status {
        padding: 4px 10px;
        border-radius: 12px;
        font-size: 11px;
        font-weight: 600;
        background: rgba(156, 163, 175, 0.1);
        color: var(--gray-500);

        &.active {
          background: rgba(16, 185, 129, 0.1);
          color: var(--success);
        }
      }

      .no-meds {
        text-align: center;
        color: var(--gray-500);
        padding: 20px;
      }
    }
  `]
})
export class TreatmentPlansComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  plans = signal<TreatmentPlan[]>([]);
  activeTab = signal<'today' | 'plans'>('today');
  showCreateDialog = signal(false);
  selectedPlan = signal<TreatmentPlan | null>(null);
  editingPlan = signal<TreatmentPlan | null>(null);

  newPlan = {
    title: '',
    description: ''
  };

  newPlanMedications: NewMedication[] = [];

  commonTimes = ['08:00', '12:00', '18:00', '22:00'];

  todayDate = new Date().toLocaleDateString('en-US', { 
    weekday: 'long', 
    month: 'long', 
    day: 'numeric' 
  });

  todayTasks = computed(() => {
    const tasks: PlanTask[] = [];
    let taskId = 1;
    
    this.plans().forEach(plan => {
      if (plan.status === 'ACTIVE' && plan.medicationsList) {
        plan.medicationsList.forEach(med => {
          if (med.isActive) {
            tasks.push({
              id: taskId++,
              name: `Take ${med.name} (${med.dosage})`,
              time: '08:00',
              completed: false,
              type: 'medication',
              planId: plan.id,
              planTitle: plan.title
            });
          }
        });
      }
    });

    return tasks.sort((a, b) => a.time.localeCompare(b.time));
  });

  completedTasksCount = computed(() => {
    return this.todayTasks().filter(t => t.completed).length;
  });

  ngOnInit(): void {
    this.loadPlans();
  }

  loadPlans(): void {
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getTreatmentPlans(userId).subscribe(p => this.plans.set(p));
    }
  }

  selectPlan(plan: TreatmentPlan): void {
    this.selectedPlan.set(plan);
  }

  toggleTask(task: PlanTask): void {
    task.completed = !task.completed;
  }

  addMedicationField(): void {
    this.newPlanMedications.push({
      name: '',
      dosage: '',
      frequency: 'daily',
      times: []
    });
  }

  removeMedicationField(index: number): void {
    this.newPlanMedications.splice(index, 1);
  }

  toggleMedTime(med: NewMedication, time: string): void {
    const idx = med.times.indexOf(time);
    if (idx >= 0) {
      med.times.splice(idx, 1);
    } else {
      med.times.push(time);
    }
  }

  editPlan(plan: TreatmentPlan, event: Event): void {
    event.stopPropagation();
    this.editingPlan.set(plan);
    this.newPlan.title = plan.title;
    this.newPlan.description = plan.description || '';
    this.newPlanMedications = (plan.medicationsList || []).map(m => ({
      name: m.name,
      dosage: m.dosage || '',
      frequency: m.frequency || 'daily',
      times: []
    }));
    this.showCreateDialog.set(true);
  }

  deletePlan(plan: TreatmentPlan, event: Event): void {
    event.stopPropagation();
    if (confirm('Are you sure you want to delete this treatment plan?')) {
      this.api.deleteTreatmentPlan(plan.id).subscribe({
        next: () => this.loadPlans(),
        error: (err) => alert('Failed to delete plan')
      });
    }
  }

  savePlan(): void {
    const userId = this.auth.getUserId();
    if (!userId) return;

    const planData = {
      title: this.newPlan.title,
      description: this.newPlan.description,
      status: 'ACTIVE',
      medications: this.newPlanMedications.map(m => ({
        name: m.name,
        dosage: m.dosage,
        frequency: m.frequency,
        isActive: true
      }))
    };

    if (this.editingPlan()) {
      this.api.updateTreatmentPlan(this.editingPlan()!.id, planData).subscribe({
        next: () => {
          this.loadPlans();
          this.closeDialog();
        },
        error: (err) => alert('Failed to update plan')
      });
    } else {
      this.api.createTreatmentPlan(userId, planData).subscribe({
        next: () => {
          this.loadPlans();
          this.closeDialog();
        },
        error: (err) => alert('Failed to create plan')
      });
    }
  }

  closeDialog(): void {
    this.showCreateDialog.set(false);
    this.editingPlan.set(null);
    this.newPlan = { title: '', description: '' };
    this.newPlanMedications = [];
  }
}

