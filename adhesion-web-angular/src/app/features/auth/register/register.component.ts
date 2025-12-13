import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="register-page">
      <div class="bg-animation">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
      </div>

      <div class="register-container">
        <div class="register-card">
          <div class="logo-section">
            <div class="logo-icon">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                <path d="M24 4C12.954 4 4 12.954 4 24s8.954 20 20 20 20-8.954 20-20S35.046 4 24 4z" fill="url(#grad)"/>
                <defs>
                  <linearGradient id="grad" x1="4" y1="4" x2="44" y2="44">
                    <stop offset="0%" stop-color="#6366F1"/>
                    <stop offset="100%" stop-color="#A855F7"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <h1>Create Account</h1>
            <p>Start your wellness journey today</p>
          </div>

          @if (error()) {
            <div class="error-banner">{{ error() }}</div>
          }

          <form (ngSubmit)="onRegister()">
            <div class="form-group">
              <label>Full Name</label>
              <input type="text" [(ngModel)]="form.displayName" name="displayName" class="form-input" placeholder="Enter your name" required>
            </div>

            <div class="form-group">
              <label>Email</label>
              <input type="email" [(ngModel)]="form.email" name="email" class="form-input" placeholder="Enter your email" required>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Birth Date</label>
                <input type="date" [(ngModel)]="form.birthDate" name="birthDate" class="form-input" required>
              </div>
              <div class="form-group">
                <label>Gender</label>
                <select [(ngModel)]="form.gender" name="gender" class="form-input">
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Other">Other</option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label>Password</label>
              <input type="password" [(ngModel)]="form.password" name="password" class="form-input" placeholder="Create a password" required>
            </div>

            <div class="consent-group">
              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="form.consentGiven" name="consent">
                <span class="checkmark"></span>
                <span>I agree to share my data with administrators for better support</span>
              </label>
            </div>

            <button type="submit" class="btn btn-primary btn-lg w-full" [disabled]="loading()">
              @if (loading()) {
                <div class="spinner spinner-sm spinner-white"></div>
              } @else {
                Create Account
              }
            </button>
          </form>

          <p class="login-link">Already have an account? <a routerLink="/login">Sign In</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .register-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #0D0D0D 0%, #1A1A2E 100%);
      position: relative;
      overflow: hidden;
      padding: 40px 20px;
    }

    .bg-animation { position: absolute; inset: 0; }
    .orb {
      position: absolute;
      border-radius: 50%;
      filter: blur(80px);
      animation: float 15s ease-in-out infinite;
    }
    .orb-1 {
      width: 500px; height: 500px;
      background: radial-gradient(circle, rgba(99, 102, 241, 0.3) 0%, transparent 70%);
      top: -200px; left: -200px;
    }
    .orb-2 {
      width: 400px; height: 400px;
      background: radial-gradient(circle, rgba(168, 85, 247, 0.2) 0%, transparent 70%);
      bottom: -100px; right: -100px;
    }

    .register-container {
      position: relative;
      z-index: 10;
      width: 100%;
      max-width: 480px;
    }

    .register-card {
      background: rgba(255, 255, 255, 0.03);
      backdrop-filter: blur(20px);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 24px;
      padding: 40px;
    }

    .logo-section {
      text-align: center;
      margin-bottom: 32px;
    }

    .logo-icon {
      width: 70px; height: 70px;
      margin: 0 auto 16px;
      background: var(--primary-gradient);
      border-radius: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .logo-section h1 { color: white; font-size: 26px; margin-bottom: 6px; }
    .logo-section p { color: rgba(255,255,255,0.6); font-size: 14px; }

    .error-banner {
      padding: 12px 16px;
      background: rgba(239, 68, 68, 0.15);
      border: 1px solid rgba(239, 68, 68, 0.3);
      border-radius: 10px;
      margin-bottom: 20px;
      color: #FCA5A5;
      font-size: 14px;
    }

    .form-group {
      margin-bottom: 16px;
      label {
        display: block;
        margin-bottom: 6px;
        color: rgba(255,255,255,0.7);
        font-size: 13px;
        font-weight: 600;
      }
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .form-input {
      width: 100%;
      padding: 12px 14px;
      background: rgba(255,255,255,0.06);
      border: 2px solid rgba(255,255,255,0.08);
      border-radius: 10px;
      color: white;
      font-size: 14px;
      &::placeholder { color: rgba(255,255,255,0.35); }
      &:focus {
        outline: none;
        border-color: var(--primary);
      }
    }

    .consent-group {
      margin-bottom: 20px;
    }

    .checkbox-label {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      color: rgba(255,255,255,0.6);
      font-size: 13px;
      cursor: pointer;
      input { margin-top: 2px; }
    }

    .login-link {
      text-align: center;
      margin-top: 20px;
      color: rgba(255,255,255,0.6);
      font-size: 14px;
      a { color: var(--primary-light); font-weight: 600; }
    }

    @keyframes float {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-20px); }
    }
  `]
})
export class RegisterComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  form = {
    email: '',
    password: '',
    displayName: '',
    birthDate: '',
    gender: 'Other',
    consentGiven: false
  };

  loading = signal(false);
  error = signal<string | null>(null);

  onRegister(): void {
    if (!this.form.email || !this.form.password || !this.form.displayName || !this.form.birthDate) {
      this.error.set('Please fill in all required fields');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.auth.register(this.form).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Registration failed');
      }
    });
  }
}
