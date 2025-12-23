import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, UserMedication, TreatmentPlan, CreateMedicationRequest } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-medications',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="medications-page">
      <header class="page-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>My Medications</h1>
        <button class="add-btn" (click)="showAddDialog.set(true)">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
        </button>
      </header>

      <!-- Stats Section -->
      <div class="stats-bar">
        <div class="stat">
          <span class="stat-value">{{ activeMedsCount() }}</span>
          <span class="stat-label">Active</span>
        </div>
        <div class="stat">
          <span class="stat-value">{{ chronicMedsCount() }}</span>
          <span class="stat-label">Chronic</span>
        </div>
        <div class="stat">
          <span class="stat-value">{{ totalDosesToday() }}</span>
          <span class="stat-label">Doses Today</span>
        </div>
      </div>

      <div class="content">
        <!-- Search/Filter -->
        <div class="search-bar">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
          <input type="text" placeholder="Search medications..." [(ngModel)]="searchQuery">
        </div>

        <!-- Filter Tabs -->
        <div class="filter-tabs">
          <button 
            class="filter-tab" 
            [class.active]="activeFilter() === 'all'"
            (click)="activeFilter.set('all')"
          >All ({{ medications().length }})</button>
          <button 
            class="filter-tab" 
            [class.active]="activeFilter() === 'active'"
            (click)="activeFilter.set('active')"
          >Active</button>
          <button 
            class="filter-tab" 
            [class.active]="activeFilter() === 'chronic'"
            (click)="activeFilter.set('chronic')"
          >Chronic</button>
        </div>

        @if (filteredMedications().length > 0) {
          <div class="medications-list">
            @for (med of filteredMedications(); track med.id) {
              <div class="medication-card" [class.inactive]="!med.active">
                <div class="med-icon" [class.chronic]="med.isChronic">
                  💊
                </div>
                <div class="med-info">
                  <div class="med-header">
                    <h3>{{ med.name }}</h3>
                    @if (med.isChronic) {
                      <span class="chronic-badge">Chronic</span>
                    }
                  </div>
                  <p class="med-dosage">{{ med.dosage }}</p>
                  <div class="med-meta">
                    <span class="frequency">{{ med.frequencyPerDay }}x daily</span>
                    @if (med.instructions) {
                      <span class="plan-link">• {{ med.instructions }}</span>
                    }
                  </div>
                </div>
                <div class="med-actions">
                  <button 
                    class="toggle-btn" 
                    [class.active]="med.active"
                    (click)="toggleMedication(med)"
                    title="{{ med.active ? 'Deactivate' : 'Activate' }}"
                  >
                    @if (med.active) {
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
                    } @else {
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z"/></svg>
                    }
                  </button>
                  <button class="action-btn edit" (click)="editMedication(med)">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
                  </button>
                  <button class="action-btn delete" (click)="deleteMedication(med)">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
                  </button>
                </div>
              </div>
            }
          </div>
        } @else {
          <div class="empty-state">
            <div class="icon">💊</div>
            <h3>No Medications Found</h3>
            @if (searchQuery) {
              <p>Try a different search term</p>
            } @else {
              <p>Add your first medication to start tracking</p>
              <button class="btn btn-primary" (click)="showAddDialog.set(true)">
                Add Medication
              </button>
            }
          </div>
        }
      </div>

      <!-- Add/Edit Medication Dialog -->
      @if (showAddDialog()) {
        <div class="dialog-overlay" (click)="closeDialog()">
          <div class="dialog" (click)="$event.stopPropagation()">
            <div class="dialog-header">
              <h2>{{ editingMed() ? 'Edit' : 'Add' }} Medication</h2>
              <button class="close-btn" (click)="closeDialog()">×</button>
            </div>

            <div class="dialog-content">
              <div class="form-group">
                <label>Medication Name *</label>
                <input type="text" [(ngModel)]="newMed.name" placeholder="e.g., Lisinopril">
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>Dosage</label>
                  <input type="text" [(ngModel)]="newMed.dosage" placeholder="e.g., 10mg">
                </div>
                <div class="form-group">
                  <label>Form</label>
                  <select [(ngModel)]="newMed.form">
                    @for (f of forms; track f) {
                      <option [value]="f">{{ f }}</option>
                    }
                  </select>
                </div>
              </div>

              <div class="form-group">
                <label>Scheduled Times</label>
                <div class="times-grid">
                  @for (time of commonTimes; track time) {
                    <label class="time-chip" [class.selected]="newMed.scheduledTimes.includes(time)">
                      <input type="checkbox" [checked]="newMed.scheduledTimes.includes(time)" (change)="toggleScheduledTime(time)">
                      {{ time }}
                    </label>
                  }
                </div>
              </div>

              <div class="form-row checkboxes">
                <label class="checkbox-label">
                  <input type="checkbox" [(ngModel)]="newMed.active">
                  <span>Active</span>
                </label>
                <label class="checkbox-label">
                  <input type="checkbox" [(ngModel)]="newMed.isChronic">
                  <span>Chronic Medication</span>
                </label>
                <label class="checkbox-label">
                  <input type="checkbox" [(ngModel)]="newMed.remindersEnabled">
                  <span>Reminders</span>
                </label>
              </div>

              <div class="form-group">
                <label>Instructions / Notes</label>
                <textarea [(ngModel)]="newMed.instructions" placeholder="Take with food, avoid alcohol..."></textarea>
              </div>
            </div>

            <div class="dialog-footer">
              <button class="btn btn-secondary" (click)="closeDialog()">Cancel</button>
              <button 
                class="btn btn-primary" 
                (click)="saveMedication()"
                [disabled]="!newMed.name"
              >
                {{ editingMed() ? 'Update' : 'Add' }} Medication
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .medications-page { min-height: 100vh; background: linear-gradient(180deg, #F0FDF4 0%, #DCFCE7 100%); }
    
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
      background: linear-gradient(135deg, var(--success), #34D399);
      color: white;
      &:hover { transform: scale(1.05); }
    }

    .stats-bar {
      display: flex;
      background: white;
      padding: 16px 24px;
      gap: 24px;
      border-bottom: 1px solid var(--gray-200);
    }

    .stat {
      text-align: center;
      flex: 1;
      
      .stat-value {
        display: block;
        font-size: 24px;
        font-weight: 700;
        color: var(--success);
      }
      
      .stat-label {
        font-size: 12px;
        color: var(--gray-500);
      }
    }
    
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }

    .search-bar {
      display: flex;
      align-items: center;
      gap: 12px;
      background: white;
      border-radius: 14px;
      padding: 12px 16px;
      margin-bottom: 16px;
      box-shadow: var(--shadow-sm);

      svg { color: var(--gray-400); }

      input {
        flex: 1;
        border: none;
        outline: none;
        font-size: 14px;
        &::placeholder { color: var(--gray-400); }
      }
    }

    .filter-tabs {
      display: flex;
      gap: 8px;
      margin-bottom: 20px;
    }

    .filter-tab {
      padding: 8px 16px;
      background: white;
      border: none;
      border-radius: 20px;
      font-size: 13px;
      color: var(--gray-600);
      cursor: pointer;
      transition: all 0.2s;

      &.active {
        background: var(--success);
        color: white;
      }

      &:hover:not(.active) {
        background: var(--gray-100);
      }
    }

    .medications-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    
    .medication-card {
      display: flex;
      align-items: center;
      gap: 16px;
      background: white;
      border-radius: 16px;
      padding: 20px;
      box-shadow: var(--shadow-sm);
      transition: all 0.2s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: var(--shadow-md);
      }

      &.inactive {
        opacity: 0.6;
        background: var(--gray-50);
      }
    }
    
    .med-icon {
      width: 48px; height: 48px;
      background: rgba(16, 185, 129, 0.1);
      border-radius: 14px;
      display: flex; align-items: center; justify-content: center;
      font-size: 24px;
      flex-shrink: 0;
      &.chronic { background: rgba(139, 92, 246, 0.1); }
    }
    
    .med-info { 
      flex: 1;
      min-width: 0;
    }

    .med-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;

      h3 { 
        font-size: 16px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .med-dosage {
      color: var(--gray-600);
      font-size: 14px;
      margin-bottom: 4px;
    }

    .med-meta {
      font-size: 12px;
      color: var(--gray-400);
      display: flex;
      gap: 6px;
    }
    
    .chronic-badge {
      padding: 2px 8px;
      background: rgba(139, 92, 246, 0.1);
      color: var(--secondary);
      border-radius: 12px;
      font-size: 10px;
      font-weight: 600;
      flex-shrink: 0;
    }

    .med-actions {
      display: flex;
      gap: 8px;
      flex-shrink: 0;
    }

    .toggle-btn {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--gray-100);
      border: none;
      border-radius: 10px;
      color: var(--gray-400);
      cursor: pointer;
      transition: all 0.2s;

      &.active {
        background: rgba(16, 185, 129, 0.1);
        color: var(--success);
      }

      &:hover { transform: scale(1.1); }
    }

    .action-btn {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: none;
      border-radius: 10px;
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
      p { color: var(--gray-600); margin-bottom: 24px; }
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
      max-width: 480px;
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
          border-color: var(--success);
          box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
        }
      }

      textarea {
        min-height: 80px;
        resize: vertical;
      }
    }

    .form-row {
      display: flex;
      gap: 16px;

      .form-group { flex: 1; }

      &.checkboxes {
        gap: 24px;
        margin-bottom: 20px;
      }
    }

    .times-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .time-chip {
      display: flex;
      align-items: center;
      padding: 8px 14px;
      background: var(--gray-100);
      border-radius: 20px;
      cursor: pointer;
      font-size: 13px;
      transition: all 0.2s;

      input[type="checkbox"] {
        display: none;
      }

      &.selected {
        background: linear-gradient(135deg, var(--success), #34D399);
        color: white;
      }

      &:hover:not(.selected) {
        background: var(--gray-200);
      }
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      input[type="checkbox"] {
        width: 18px;
        height: 18px;
        accent-color: var(--success);
      }

      span {
        font-size: 14px;
        color: var(--gray-700);
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
        background: linear-gradient(135deg, var(--success), #34D399);
        color: white;
        &:hover:not(:disabled) { transform: translateY(-2px); }
        &:disabled { opacity: 0.6; cursor: not-allowed; }
      }
    }
  `]
})
export class MedicationsComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  medications = signal<UserMedication[]>([]);
  plans = signal<TreatmentPlan[]>([]);
  showAddDialog = signal(false);
  editingMed = signal<UserMedication | null>(null);
  activeFilter = signal<'all' | 'active' | 'chronic'>('all');
  searchQuery = '';

  newMed = {
    name: '',
    dosage: '',
    form: 'TABLET',
    frequencyPerDay: 1,
    scheduledTimes: ['08:00'] as string[],
    instructions: '',
    isChronic: false,
    active: true,
    remindersEnabled: true,
    reminderMinutesBefore: 15,
    notes: ''
  };

  forms = ['TABLET', 'CAPSULE', 'LIQUID', 'INJECTION', 'CREAM', 'DROPS', 'INHALER', 'PATCH', 'OTHER'];
  commonTimes = ['06:00', '08:00', '12:00', '14:00', '18:00', '20:00', '22:00'];

  filteredMedications = computed(() => {
    let meds = this.medications();

    // Apply search filter
    if (this.searchQuery) {
      const query = this.searchQuery.toLowerCase();
      meds = meds.filter(m => 
        m.name.toLowerCase().includes(query) ||
        (m.dosage && m.dosage.toLowerCase().includes(query))
      );
    }

    // Apply status filter
    if (this.activeFilter() === 'active') {
      meds = meds.filter(m => m.active);
    } else if (this.activeFilter() === 'chronic') {
      meds = meds.filter(m => m.isChronic);
    }

    return meds;
  });

  activeMedsCount = computed(() => this.medications().filter(m => m.active).length);
  chronicMedsCount = computed(() => this.medications().filter(m => m.isChronic).length);
  totalDosesToday = computed(() => {
    return this.medications()
      .filter(m => m.active)
      .reduce((total, m) => total + (m.frequencyPerDay || 1), 0);
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    const userId = this.auth.getUserId();
    if (userId) {
      // Load user's medications from the medications API
      this.api.getUserMedications(userId).subscribe({
        next: meds => this.medications.set(meds),
        error: err => console.error('Failed to load medications:', err)
      });
      // Also load plans for reference
      this.api.getTreatmentPlans(userId).subscribe({
        next: plans => this.plans.set(plans),
        error: err => console.error('Failed to load plans:', err)
      });
    }
  }

  toggleMedication(med: UserMedication): void {
    this.api.toggleMedicationActive(med.id).subscribe({
      next: updatedMed => {
        this.medications.update(meds => 
          meds.map(m => m.id === med.id ? updatedMed : m)
        );
      },
      error: err => console.error('Failed to toggle medication:', err)
    });
  }

  editMedication(med: UserMedication): void {
    this.editingMed.set(med);
    this.newMed = {
      name: med.name,
      dosage: med.dosage || '',
      form: med.form || 'TABLET',
      frequencyPerDay: med.frequencyPerDay || 1,
      scheduledTimes: med.scheduledTimes?.length > 0 ? [...med.scheduledTimes] : ['08:00'],
      instructions: med.instructions || '',
      isChronic: med.isChronic || false,
      active: med.active !== false,
      remindersEnabled: med.remindersEnabled !== false,
      reminderMinutesBefore: med.reminderMinutesBefore || 15,
      notes: med.notes || ''
    };
    this.showAddDialog.set(true);
  }

  deleteMedication(med: UserMedication): void {
    if (confirm(`Are you sure you want to delete ${med.name}?`)) {
      this.api.deleteMedication(med.id).subscribe({
        next: () => {
          this.medications.update(meds => meds.filter(m => m.id !== med.id));
        },
        error: err => {
          console.error('Failed to delete medication:', err);
          alert('Failed to delete medication');
        }
      });
    }
  }

  saveMedication(): void {
    const userId = this.auth.getUserId();
    if (!userId || !this.newMed.name) return;

    const request: CreateMedicationRequest = {
      userId,
      name: this.newMed.name,
      dosage: this.newMed.dosage,
      form: this.newMed.form,
      frequencyPerDay: this.newMed.frequencyPerDay,
      scheduledTimes: this.newMed.scheduledTimes,
      instructions: this.newMed.instructions,
      isChronic: this.newMed.isChronic,
      active: this.newMed.active,
      remindersEnabled: this.newMed.remindersEnabled,
      reminderMinutesBefore: this.newMed.reminderMinutesBefore,
      notes: this.newMed.notes
    };

    if (this.editingMed()) {
      this.api.updateMedication(this.editingMed()!.id, request).subscribe({
        next: updatedMed => {
          this.medications.update(meds => 
            meds.map(m => m.id === updatedMed.id ? updatedMed : m)
          );
          this.closeDialog();
        },
        error: err => {
          console.error('Failed to update medication:', err);
          alert('Failed to update medication');
        }
      });
    } else {
      this.api.addMedication(request).subscribe({
        next: newMed => {
          this.medications.update(meds => [...meds, newMed]);
          this.closeDialog();
        },
        error: err => {
          console.error('Failed to add medication:', err);
          alert('Failed to add medication');
        }
      });
    }
  }

  toggleScheduledTime(time: string): void {
    const idx = this.newMed.scheduledTimes.indexOf(time);
    if (idx >= 0) {
      this.newMed.scheduledTimes.splice(idx, 1);
    } else {
      this.newMed.scheduledTimes.push(time);
      this.newMed.scheduledTimes.sort();
    }
    this.newMed.frequencyPerDay = this.newMed.scheduledTimes.length;
  }

  closeDialog(): void {
    this.showAddDialog.set(false);
    this.editingMed.set(null);
    this.newMed = {
      name: '',
      dosage: '',
      form: 'TABLET',
      frequencyPerDay: 1,
      scheduledTimes: ['08:00'],
      instructions: '',
      isChronic: false,
      active: true,
      remindersEnabled: true,
      reminderMinutesBefore: 15,
      notes: ''
    };
  }
}

