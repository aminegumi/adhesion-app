import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, DoseLog, DoseStatus, SkipReason } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-todays-doses',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="doses-page">
      <header class="page-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>Today's Medications</h1>
        <button class="refresh-btn" (click)="loadDoses()" [disabled]="loading()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M17.65 6.35A7.958 7.958 0 0012 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0112 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
        </button>
      </header>

      <div class="content">
        @if (loading()) {
          <div class="loading-state">
            <div class="spinner"></div>
            <p>Loading today's doses...</p>
          </div>
        } @else if (error()) {
          <div class="error-state">
            <div class="icon">⚠️</div>
            <h3>Error Loading Doses</h3>
            <p>{{ error() }}</p>
            <button class="btn btn-primary" (click)="loadDoses()">Try Again</button>
          </div>
        } @else if (doses().length === 0) {
          <div class="empty-state">
            <div class="icon">💊</div>
            <h3>No Medications Scheduled</h3>
            <p>You don't have any medications scheduled for today.</p>
            <p class="hint">Add medications from your treatment plan or medications page.</p>
            <div class="action-buttons">
              <a routerLink="/medications" class="btn btn-secondary">Manage Medications</a>
              <button class="btn btn-primary" (click)="generateSchedule()">Generate Schedule</button>
            </div>
          </div>
        } @else {
          <!-- Progress Card -->
          <div class="progress-card">
            <div class="progress-info">
              <span class="progress-title">Today's Progress</span>
              <span class="progress-subtitle">{{ completedCount() }} of {{ doses().length }} doses taken</span>
            </div>
            <div class="progress-stats">
              <div class="stat pending">
                <span class="value">{{ pendingDoses().length }}</span>
                <span class="label">Pending</span>
              </div>
              <div class="stat done">
                <span class="value">{{ takenDoses().length }}</span>
                <span class="label">Taken</span>
              </div>
              <div class="stat missed">
                <span class="value">{{ missedDoses().length + skippedDoses().length }}</span>
                <span class="label">Missed</span>
              </div>
            </div>
            <div class="progress-bar">
              <div class="fill" [style.width.%]="progressPercent()"></div>
            </div>
            <div class="progress-percent">{{ progressPercent() }}%</div>
          </div>

          <!-- Pending Doses -->
          @if (pendingDoses().length > 0) {
            <div class="section">
              <div class="section-header pending">
                <span class="icon">⏰</span>
                <span class="title">Upcoming</span>
                <span class="count">{{ pendingDoses().length }}</span>
              </div>
              <div class="doses-list">
                @for (dose of pendingDoses(); track dose.id) {
                  <div class="dose-card" [class.overdue]="isOverdue(dose)">
                    <div class="dose-time-box" [class.overdue]="isOverdue(dose)">
                      <span class="time-icon">💊</span>
                      <span class="time">{{ formatTime(dose.scheduledTime) }}</span>
                    </div>
                    <div class="dose-info">
                      <h4>{{ dose.medicationName }}</h4>
                      @if (dose.dosage) {
                        <span class="dosage">{{ dose.dosage }}</span>
                      }
                      @if (isOverdue(dose)) {
                        <span class="overdue-badge">Overdue</span>
                      }
                    </div>
                    <div class="dose-actions">
                      <button class="skip-btn" (click)="openSkipDialog(dose)" title="Skip">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                      </button>
                      <button class="take-btn" (click)="takeDose(dose)">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                        Take
                      </button>
                    </div>
                  </div>
                }
              </div>
            </div>
          }

          <!-- Missed Doses -->
          @if (missedDoses().length > 0) {
            <div class="section">
              <div class="section-header missed">
                <span class="icon">⚠️</span>
                <span class="title">Missed</span>
                <span class="count">{{ missedDoses().length }}</span>
              </div>
              <div class="doses-list">
                @for (dose of missedDoses(); track dose.id) {
                  <div class="dose-card missed">
                    <div class="dose-time-box missed">
                      <span class="time-icon">⚠️</span>
                      <span class="time">{{ formatTime(dose.scheduledTime) }}</span>
                    </div>
                    <div class="dose-info">
                      <h4>{{ dose.medicationName }}</h4>
                      @if (dose.dosage) {
                        <span class="dosage">{{ dose.dosage }}</span>
                      }
                    </div>
                    <div class="status-badge missed">Missed</div>
                  </div>
                }
              </div>
            </div>
          }

          <!-- Completed Doses -->
          @if (takenDoses().length > 0 || skippedDoses().length > 0) {
            <div class="section">
              <div class="section-header completed">
                <span class="icon">✓</span>
                <span class="title">Completed</span>
                <span class="count">{{ takenDoses().length + skippedDoses().length }}</span>
              </div>
              <div class="doses-list">
                @for (dose of takenDoses(); track dose.id) {
                  <div class="dose-card completed taken">
                    <div class="dose-time-box taken">
                      <span class="time-icon">✓</span>
                      <span class="time">{{ formatTime(dose.scheduledTime) }}</span>
                    </div>
                    <div class="dose-info">
                      <h4>{{ dose.medicationName }}</h4>
                      @if (dose.dosage) {
                        <span class="dosage">{{ dose.dosage }}</span>
                      }
                      @if (dose.delayMinutes !== undefined && dose.delayMinutes !== null) {
                        <span class="delay-info">
                          {{ dose.delayMinutes > 0 ? 'Taken ' + dose.delayMinutes + ' min late' : 'Taken on time' }}
                        </span>
                      }
                    </div>
                    <div class="status-badge taken">Taken</div>
                  </div>
                }
                @for (dose of skippedDoses(); track dose.id) {
                  <div class="dose-card completed skipped">
                    <div class="dose-time-box skipped">
                      <span class="time-icon">⏭️</span>
                      <span class="time">{{ formatTime(dose.scheduledTime) }}</span>
                    </div>
                    <div class="dose-info">
                      <h4>{{ dose.medicationName }}</h4>
                      @if (dose.dosage) {
                        <span class="dosage">{{ dose.dosage }}</span>
                      }
                      @if (dose.skipReason) {
                        <span class="skip-reason">{{ getSkipReasonText(dose.skipReason) }}</span>
                      }
                    </div>
                    <div class="status-badge skipped">Skipped</div>
                  </div>
                }
              </div>
            </div>
          }
        }
      </div>

      <!-- Skip Dialog -->
      @if (showSkipDialog()) {
        <div class="dialog-overlay" (click)="closeSkipDialog()">
          <div class="dialog" (click)="$event.stopPropagation()">
            <div class="dialog-header">
              <h2>Skip {{ skipDialogDose()?.medicationName }}?</h2>
              <button class="close-btn" (click)="closeSkipDialog()">×</button>
            </div>
            <div class="dialog-content">
              <p class="dialog-subtitle">Please select a reason:</p>
              <div class="reason-options">
                @for (reason of skipReasons; track reason.value) {
                  <label class="reason-option" [class.selected]="selectedSkipReason() === reason.value">
                    <input type="radio" name="skipReason" [value]="reason.value" 
                           [checked]="selectedSkipReason() === reason.value"
                           (change)="selectedSkipReason.set(reason.value)">
                    <span class="reason-icon">{{ reason.icon }}</span>
                    <span class="reason-text">{{ reason.label }}</span>
                  </label>
                }
              </div>
              <div class="form-group">
                <label>Additional notes (optional)</label>
                <textarea [(ngModel)]="skipNotes" placeholder="Any additional details..."></textarea>
              </div>
            </div>
            <div class="dialog-footer">
              <button class="btn btn-secondary" (click)="closeSkipDialog()">Cancel</button>
              <button class="btn btn-warning" (click)="confirmSkip()" [disabled]="!selectedSkipReason()">
                Skip Dose
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .doses-page { min-height: 100vh; background: linear-gradient(180deg, #EEF2FF 0%, #E0E7FF 100%); }
    
    .page-header {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: white;
      border-bottom: 1px solid var(--gray-200);
      h1 { flex: 1; font-size: 20px; }
    }
    
    .back-btn, .refresh-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: var(--gray-100);
      border: none;
      border-radius: 10px;
      color: var(--gray-600);
      cursor: pointer;
      transition: all 0.2s;
      &:hover { background: var(--gray-200); }
      &:disabled { opacity: 0.5; }
    }
    
    .content { max-width: 600px; margin: 0 auto; padding: 24px; }

    .loading-state, .error-state {
      text-align: center;
      padding: 60px 24px;
      .spinner {
        width: 48px; height: 48px;
        border: 3px solid rgba(99, 102, 241, 0.2);
        border-top-color: var(--primary);
        border-radius: 50%;
        margin: 0 auto 20px;
        animation: spin 0.8s linear infinite;
      }
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .empty-state {
      text-align: center;
      padding: 60px 24px;
      background: white;
      border-radius: 24px;
      box-shadow: var(--shadow-md);
      .icon { font-size: 64px; margin-bottom: 16px; }
      h3 { margin-bottom: 8px; }
      p { color: var(--gray-600); margin-bottom: 8px; }
      .hint { font-size: 13px; color: var(--gray-500); margin-bottom: 20px; }
      .action-buttons { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }
    }

    .progress-card {
      background: linear-gradient(135deg, var(--primary), #8B5CF6);
      border-radius: 20px;
      padding: 24px;
      margin-bottom: 24px;
      color: white;
      box-shadow: 0 8px 24px rgba(99, 102, 241, 0.3);
    }

    .progress-info {
      display: flex;
      flex-direction: column;
      margin-bottom: 16px;
    }

    .progress-title { font-size: 18px; font-weight: 600; }
    .progress-subtitle { font-size: 14px; opacity: 0.8; }

    .progress-stats {
      display: flex;
      justify-content: space-around;
      margin-bottom: 16px;
    }

    .stat {
      text-align: center;
      .value { font-size: 28px; font-weight: 700; display: block; }
      .label { font-size: 12px; opacity: 0.8; }
      &.done .value { color: #34D399; }
      &.missed .value { color: #FCA5A5; }
    }

    .progress-bar {
      height: 10px;
      background: rgba(255,255,255,0.3);
      border-radius: 5px;
      overflow: hidden;
      margin-bottom: 8px;
      .fill {
        height: 100%;
        background: white;
        border-radius: 5px;
        transition: width 0.3s;
      }
    }

    .progress-percent {
      text-align: right;
      font-size: 24px;
      font-weight: 700;
    }

    .section { margin-bottom: 24px; }

    .section-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      padding: 8px 14px;
      border-radius: 20px;
      width: fit-content;
      font-weight: 600;
      font-size: 14px;

      &.pending { background: rgba(245, 158, 11, 0.1); color: var(--warning); }
      &.missed { background: rgba(239, 68, 68, 0.1); color: var(--error); }
      &.completed { background: rgba(16, 185, 129, 0.1); color: var(--success); }

      .count {
        background: currentColor;
        color: white;
        padding: 2px 8px;
        border-radius: 10px;
        font-size: 12px;
      }
    }

    .doses-list { display: flex; flex-direction: column; gap: 12px; }

    .dose-card {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 16px;
      background: white;
      border-radius: 16px;
      box-shadow: var(--shadow-sm);
      border-left: 4px solid var(--warning);
      transition: all 0.2s;

      &.overdue { border-left-color: var(--error); background: rgba(239, 68, 68, 0.02); }
      &.completed { opacity: 0.8; }
      &.taken { border-left-color: var(--success); }
      &.skipped { border-left-color: #F59E0B; }
      &.missed { border-left-color: var(--error); }
    }

    .dose-time-box {
      width: 60px;
      height: 60px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: rgba(245, 158, 11, 0.1);
      border-radius: 12px;
      flex-shrink: 0;

      &.overdue { background: rgba(239, 68, 68, 0.1); }
      &.taken { background: rgba(16, 185, 129, 0.1); }
      &.skipped { background: rgba(245, 158, 11, 0.1); }
      &.missed { background: rgba(239, 68, 68, 0.1); }

      .time-icon { font-size: 20px; }
      .time { font-size: 11px; font-weight: 600; color: var(--gray-600); }
    }

    .dose-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;

      h4 { font-size: 16px; font-weight: 600; margin: 0; }
      .dosage { font-size: 14px; color: var(--gray-600); }
      .overdue-badge {
        display: inline-block;
        padding: 2px 8px;
        background: rgba(239, 68, 68, 0.1);
        color: var(--error);
        font-size: 11px;
        font-weight: 600;
        border-radius: 4px;
        width: fit-content;
      }
      .delay-info, .skip-reason {
        font-size: 12px;
        color: var(--gray-500);
      }
    }

    .dose-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .skip-btn {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(245, 158, 11, 0.1);
      border: none;
      border-radius: 10px;
      color: var(--warning);
      cursor: pointer;
      transition: all 0.2s;
      &:hover { background: var(--warning); color: white; }
    }

    .take-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px 20px;
      background: var(--success);
      color: white;
      border: none;
      border-radius: 10px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      &:hover { background: #059669; transform: scale(1.02); }
    }

    .status-badge {
      padding: 8px 14px;
      border-radius: 10px;
      font-size: 13px;
      font-weight: 600;

      &.taken { background: rgba(16, 185, 129, 0.1); color: var(--success); }
      &.skipped { background: rgba(245, 158, 11, 0.1); color: var(--warning); }
      &.missed { background: rgba(239, 68, 68, 0.1); color: var(--error); }
    }

    /* Dialog */
    .dialog-overlay {
      position: fixed; inset: 0;
      background: rgba(0,0,0,0.5);
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
      max-width: 400px;
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
      h2 { font-size: 18px; }
    }

    .close-btn {
      width: 36px; height: 36px;
      display: flex; align-items: center; justify-content: center;
      background: var(--gray-100);
      border: none;
      border-radius: 50%;
      font-size: 20px;
      cursor: pointer;
      &:hover { background: var(--gray-200); }
    }

    .dialog-content { padding: 20px 24px; overflow-y: auto; }

    .dialog-subtitle {
      color: var(--gray-600);
      margin-bottom: 16px;
    }

    .reason-options {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 20px;
    }

    .reason-option {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      background: var(--gray-50);
      border: 2px solid transparent;
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s;

      input { display: none; }
      &.selected { border-color: var(--warning); background: rgba(245, 158, 11, 0.1); }
      &:hover { background: var(--gray-100); }

      .reason-icon { font-size: 20px; }
      .reason-text { font-weight: 500; }
    }

    .form-group {
      label { display: block; font-weight: 500; margin-bottom: 8px; }
      textarea {
        width: 100%;
        padding: 12px;
        border: 1px solid var(--gray-300);
        border-radius: 12px;
        resize: vertical;
        min-height: 80px;
        font-family: inherit;
      }
    }

    .dialog-footer {
      display: flex;
      gap: 12px;
      padding: 20px 24px;
      border-top: 1px solid var(--gray-200);
    }

    .btn {
      flex: 1;
      padding: 12px 20px;
      border: none;
      border-radius: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;

      &.btn-secondary { background: var(--gray-100); color: var(--gray-700); }
      &.btn-primary { background: var(--primary); color: white; }
      &.btn-warning { background: var(--warning); color: white; }
      &:disabled { opacity: 0.5; cursor: not-allowed; }
    }
  `]
})
export class TodaysDosesComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  doses = signal<DoseLog[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  
  showSkipDialog = signal(false);
  skipDialogDose = signal<DoseLog | null>(null);
  selectedSkipReason = signal<SkipReason | null>(null);
  skipNotes = '';

  skipReasons: { value: SkipReason; label: string; icon: string }[] = [
    { value: 'FORGOT', label: 'I forgot', icon: '🤔' },
    { value: 'SIDE_EFFECTS', label: 'Side effects', icon: '😷' },
    { value: 'RAN_OUT', label: 'Ran out of medication', icon: '📦' },
    { value: 'FEELING_BETTER', label: 'Feeling better', icon: '😊' },
    { value: 'DOCTOR_ADVISED', label: 'Doctor advised', icon: '👨‍⚕️' },
    { value: 'OTHER', label: 'Other reason', icon: '📝' },
  ];

  pendingDoses = computed(() => this.doses().filter(d => d.status === 'PENDING'));
  takenDoses = computed(() => this.doses().filter(d => d.status === 'TAKEN'));
  skippedDoses = computed(() => this.doses().filter(d => d.status === 'SKIPPED'));
  missedDoses = computed(() => this.doses().filter(d => d.status === 'MISSED'));
  completedCount = computed(() => this.takenDoses().length);
  progressPercent = computed(() => {
    const total = this.doses().length;
    if (total === 0) return 0;
    return Math.round((this.takenDoses().length / total) * 100);
  });

  ngOnInit(): void {
    this.loadDoses();
  }

  loadDoses(): void {
    const userId = this.auth.getUserId();
    if (!userId) {
      this.error.set('Please log in first');
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.api.getTodaysDoses(userId).subscribe({
      next: (doses) => {
        this.doses.set(doses);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load doses:', err);
        this.error.set('Failed to load medications');
        this.loading.set(false);
      }
    });
  }

  generateSchedule(): void {
    const userId = this.auth.getUserId();
    if (!userId) return;

    this.api.generateDailySchedule(userId).subscribe({
      next: () => {
        this.loadDoses();
      },
      error: (err) => {
        console.error('Failed to generate schedule:', err);
        alert('Failed to generate schedule. Make sure you have medications added.');
      }
    });
  }

  takeDose(dose: DoseLog): void {
    this.api.takeDose(dose.id).subscribe({
      next: (updated) => {
        this.doses.update(doses => 
          doses.map(d => d.id === dose.id ? updated : d)
        );
      },
      error: (err) => {
        console.error('Failed to take dose:', err);
        alert('Failed to mark dose as taken');
      }
    });
  }

  openSkipDialog(dose: DoseLog): void {
    this.skipDialogDose.set(dose);
    this.selectedSkipReason.set(null);
    this.skipNotes = '';
    this.showSkipDialog.set(true);
  }

  closeSkipDialog(): void {
    this.showSkipDialog.set(false);
    this.skipDialogDose.set(null);
  }

  confirmSkip(): void {
    const dose = this.skipDialogDose();
    const reason = this.selectedSkipReason();
    if (!dose || !reason) return;

    this.api.skipDose(dose.id, reason, this.skipNotes || undefined).subscribe({
      next: (updated) => {
        this.doses.update(doses => 
          doses.map(d => d.id === dose.id ? updated : d)
        );
        this.closeSkipDialog();
      },
      error: (err) => {
        console.error('Failed to skip dose:', err);
        alert('Failed to skip dose');
      }
    });
  }

  isOverdue(dose: DoseLog): boolean {
    if (dose.status !== 'PENDING') return false;
    try {
      const now = new Date();
      const scheduled = new Date(`${dose.scheduledDate}T${dose.scheduledTime}`);
      return now > scheduled;
    } catch {
      return false;
    }
  }

  formatTime(time: string): string {
    try {
      const [hours, minutes] = time.split(':').map(Number);
      const period = hours >= 12 ? 'PM' : 'AM';
      const displayHour = hours > 12 ? hours - 12 : (hours === 0 ? 12 : hours);
      return `${displayHour}:${minutes.toString().padStart(2, '0')} ${period}`;
    } catch {
      return time;
    }
  }

  getSkipReasonText(reason: SkipReason): string {
    const found = this.skipReasons.find(r => r.value === reason);
    return found ? found.label : reason;
  }
}
