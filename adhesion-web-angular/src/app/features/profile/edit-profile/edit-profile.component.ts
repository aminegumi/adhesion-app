import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-edit-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="edit-profile-page">
      <header class="page-header">
        <a routerLink="/profile" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <h1>Edit Profile</h1>
        <div style="width: 40px;"></div>
      </header>

      <div class="form-content">
        <form (ngSubmit)="onSave()">
          <div class="form-group">
            <label>Display Name</label>
            <input type="text" [(ngModel)]="form.displayName" name="displayName" class="form-input">
          </div>
          <div class="form-group">
            <label>Email</label>
            <input type="email" [(ngModel)]="form.email" name="email" class="form-input">
          </div>
          <div class="form-group">
            <label>Gender</label>
            <select [(ngModel)]="form.gender" name="gender" class="form-input">
              <option value="Male">Male</option>
              <option value="Female">Female</option>
              <option value="Other">Other</option>
            </select>
          </div>
          <div class="form-group">
            <label>Birth Date</label>
            <input type="date" [(ngModel)]="form.birthDate" name="birthDate" class="form-input">
          </div>

          <div class="consent-section">
            <h3>Data Sharing Consent</h3>
            <p>Allow administrators to view your test results and treatment plans for better support.</p>
            <label class="toggle-label">
              <input type="checkbox" [(ngModel)]="form.consentGiven" name="consent">
              <span class="toggle"></span>
              <span>{{ form.consentGiven ? 'Consent Granted' : 'Consent Not Granted' }}</span>
            </label>
          </div>

          <button type="submit" class="btn btn-primary btn-lg w-full" [disabled]="saving()">
            @if (saving()) {
              <div class="spinner spinner-sm spinner-white"></div>
            } @else {
              Save Changes
            }
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .edit-profile-page { min-height: 100vh; background: var(--gray-50); }
    .page-header {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: white;
      border-bottom: 1px solid var(--gray-200);
      h1 { flex: 1; font-size: 20px; text-align: center; }
    }
    .back-btn {
      width: 40px; height: 40px;
      display: flex; align-items: center; justify-content: center;
      background: var(--gray-100);
      border-radius: 10px;
      color: var(--gray-600);
    }
    .form-content { max-width: 500px; margin: 0 auto; padding: 24px; }
    .form-group {
      margin-bottom: 20px;
      label { display: block; margin-bottom: 8px; font-weight: 600; color: var(--gray-700); font-size: 14px; }
    }
    .form-input {
      width: 100%; padding: 14px 16px;
      border: 2px solid var(--gray-200);
      border-radius: 12px;
      font-size: 15px;
      &:focus { outline: none; border-color: var(--primary); }
    }
    .consent-section {
      background: var(--gray-100);
      border-radius: 16px;
      padding: 20px;
      margin-bottom: 24px;
      h3 { font-size: 16px; margin-bottom: 8px; }
      p { color: var(--gray-600); font-size: 14px; margin-bottom: 16px; }
    }
    .toggle-label {
      display: flex; align-items: center; gap: 12px; cursor: pointer;
      input { display: none; }
      .toggle {
        width: 48px; height: 28px;
        background: var(--gray-300);
        border-radius: 14px;
        position: relative;
        transition: 0.2s;
        &::after {
          content: '';
          position: absolute;
          width: 22px; height: 22px;
          background: white;
          border-radius: 50%;
          top: 3px; left: 3px;
          transition: 0.2s;
        }
      }
      input:checked + .toggle {
        background: var(--success);
        &::after { left: 23px; }
      }
    }
  `]
})
export class EditProfileComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private router = inject(Router);

  form = { displayName: '', email: '', gender: '', birthDate: '', consentGiven: false };
  saving = signal(false);

  ngOnInit(): void {
    const user = this.auth.user();
    if (user) {
      this.form = {
        displayName: user.displayName || '',
        email: user.email || '',
        gender: user.gender || 'Other',
        birthDate: user.birthDate || '',
        consentGiven: user.consentGiven || false
      };
    }
  }

  onSave(): void {
    const userId = this.auth.getUserId();
    if (!userId) return;

    this.saving.set(true);
    this.api.updateUserProfile(userId, this.form).subscribe({
      next: (user) => {
        this.auth.updateCurrentUser(user);
        this.saving.set(false);
        this.router.navigate(['/profile']);
      },
      error: () => this.saving.set(false)
    });
  }
}

