import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="admin-login-page">
      <div class="bg-animation">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
        <div class="grid-overlay"></div>
      </div>

      <div class="login-container">
        <div class="login-card">
          <div class="logo-section">
            <div class="logo-icon">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/>
              </svg>
            </div>
            <h1>Admin Portal</h1>
            <div class="badge">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2z"/></svg>
              Restricted Access
            </div>
          </div>

          @if (error()) {
            <div class="error-banner">{{ error() }}</div>
          }

          <form (ngSubmit)="onLogin()">
            <div class="form-group">
              <label>Admin Email</label>
              <input type="email" [(ngModel)]="email" name="email" class="form-input" placeholder="Enter admin email" required>
            </div>
            <div class="form-group">
              <label>Password</label>
              <input type="password" [(ngModel)]="password" name="password" class="form-input" placeholder="Enter password" required>
            </div>
            <button type="submit" class="btn btn-admin btn-lg w-full" [disabled]="loading()">
              @if (loading()) {
                <div class="spinner spinner-sm spinner-white"></div>
              } @else {
                Access Dashboard
              }
            </button>
          </form>

          <a routerLink="/login" class="back-link">← Back to User Login</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-login-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #0D0D0D 0%, #1A0A2E 100%);
      position: relative;
      overflow: hidden;
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
      background: radial-gradient(circle, rgba(220, 38, 38, 0.2) 0%, transparent 70%);
      top: -200px; left: -200px;
    }
    .orb-2 {
      width: 400px; height: 400px;
      background: radial-gradient(circle, rgba(153, 27, 27, 0.2) 0%, transparent 70%);
      bottom: -100px; right: -100px;
    }

    .grid-overlay {
      position: absolute;
      inset: 0;
      background-image: 
        linear-gradient(rgba(220,38,38,0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(220,38,38,0.03) 1px, transparent 1px);
      background-size: 50px 50px;
    }

    .login-container {
      position: relative;
      z-index: 10;
      width: 100%;
      max-width: 420px;
      padding: 20px;
    }

    .login-card {
      background: rgba(255, 255, 255, 0.03);
      backdrop-filter: blur(20px);
      border: 1px solid rgba(220, 38, 38, 0.2);
      border-radius: 24px;
      padding: 40px;
    }

    .logo-section {
      text-align: center;
      margin-bottom: 32px;
    }

    .logo-icon {
      width: 80px; height: 80px;
      margin: 0 auto 16px;
      background: linear-gradient(135deg, #DC2626, #991B1B);
      border-radius: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      box-shadow: 0 10px 40px rgba(220, 38, 38, 0.4);
    }

    .logo-section h1 { color: white; font-size: 28px; margin-bottom: 12px; }

    .badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      background: rgba(220, 38, 38, 0.2);
      border: 1px solid rgba(220, 38, 38, 0.3);
      border-radius: 20px;
      color: #FCA5A5;
      font-size: 12px;
      font-weight: 600;
    }

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
      margin-bottom: 20px;
      label {
        display: block;
        margin-bottom: 8px;
        color: rgba(255,255,255,0.7);
        font-size: 13px;
        font-weight: 600;
      }
    }

    .form-input {
      width: 100%;
      padding: 14px 16px;
      background: rgba(255,255,255,0.06);
      border: 2px solid rgba(255,255,255,0.08);
      border-radius: 12px;
      color: white;
      font-size: 15px;
      &::placeholder { color: rgba(255,255,255,0.35); }
      &:focus {
        outline: none;
        border-color: #DC2626;
      }
    }

    .btn-admin {
      background: linear-gradient(135deg, #DC2626, #991B1B);
      color: white;
      box-shadow: 0 4px 20px rgba(220, 38, 38, 0.4);
      &:hover { transform: translateY(-2px); }
    }

    .back-link {
      display: block;
      text-align: center;
      margin-top: 20px;
      color: rgba(255,255,255,0.5);
      font-size: 14px;
      &:hover { color: white; }
    }

    @keyframes float {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-20px); }
    }
  `]
})
export class AdminLoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  loading = signal(false);
  error = signal<string | null>(null);

  onLogin(): void {
    if (!this.email || !this.password) {
      this.error.set('Please enter email and password');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.auth.login(this.email, this.password).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.user.role !== 'ADMIN') {
          this.error.set('Access denied. Admin privileges required.');
          return;
        }
        this.router.navigate(['/admin/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Invalid credentials');
      }
    });
  }
}
