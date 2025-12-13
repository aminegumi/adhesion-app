import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="login-page">
      <!-- Animated Background -->
      <div class="bg-animation">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
        <div class="orb orb-3"></div>
        <div class="grid-overlay"></div>
      </div>

      <!-- Login Card -->
      <div class="login-container">
        <div class="login-card animate-scale-in">
          <!-- Logo -->
          <div class="logo-section">
            <div class="logo-icon animate-float">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                <path d="M24 4C12.954 4 4 12.954 4 24s8.954 20 20 20 20-8.954 20-20S35.046 4 24 4z" fill="url(#grad1)"/>
                <path d="M24 12c-6.627 0-12 5.373-12 12s5.373 12 12 12 12-5.373 12-12-5.373-12-12-12zm0 18c-3.314 0-6-2.686-6-6s2.686-6 6-6 6 2.686 6 6-2.686 6-6 6z" fill="white" fill-opacity="0.9"/>
                <defs>
                  <linearGradient id="grad1" x1="4" y1="4" x2="44" y2="44">
                    <stop offset="0%" stop-color="#6366F1"/>
                    <stop offset="50%" stop-color="#8B5CF6"/>
                    <stop offset="100%" stop-color="#A855F7"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <h1>Welcome Back</h1>
            <p>Sign in to continue your wellness journey</p>
          </div>

          <!-- Error Message -->
          @if (error()) {
            <div class="error-banner animate-fade-in-down">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
              </svg>
              <span>{{ error() }}</span>
            </div>
          }

          <!-- Login Form -->
          <form (ngSubmit)="onLogin()" class="login-form">
            <div class="form-group animate-fade-in-up stagger-1">
              <label class="form-label">Email Address</label>
              <div class="input-wrapper">
                <svg class="input-icon" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>
                </svg>
                <input
                  type="email"
                  class="form-input"
                  [(ngModel)]="email"
                  name="email"
                  placeholder="Enter your email"
                  required
                  autocomplete="email"
                />
              </div>
            </div>

            <div class="form-group animate-fade-in-up stagger-2">
              <label class="form-label">Password</label>
              <div class="input-wrapper">
                <svg class="input-icon" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
                </svg>
                <input
                  [type]="showPassword() ? 'text' : 'password'"
                  class="form-input"
                  [(ngModel)]="password"
                  name="password"
                  placeholder="Enter your password"
                  required
                  autocomplete="current-password"
                />
                <button type="button" class="toggle-password" (click)="togglePassword()">
                  @if (showPassword()) {
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>
                    </svg>
                  } @else {
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                    </svg>
                  }
                </button>
              </div>
            </div>

            <button 
              type="submit" 
              class="btn btn-primary btn-lg w-full animate-fade-in-up stagger-3"
              [disabled]="loading()"
            >
              @if (loading()) {
                <div class="spinner spinner-sm spinner-white"></div>
                <span>Signing in...</span>
              } @else {
                <span>Sign In</span>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8-8-8z"/>
                </svg>
              }
            </button>
          </form>

          <!-- Footer Links -->
          <div class="login-footer animate-fade-in-up stagger-4">
            <p>Don't have an account? <a routerLink="/register">Create one</a></p>
            <div class="divider">
              <span>or</span>
            </div>
            <a routerLink="/admin/login" class="admin-link">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/>
              </svg>
              Admin Portal
            </a>
          </div>
        </div>

        <!-- Decorative Elements -->
        <div class="floating-shapes">
          <div class="shape shape-1"></div>
          <div class="shape shape-2"></div>
          <div class="shape shape-3"></div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #0D0D0D 0%, #1A1A2E 50%, #0F0F23 100%);
      position: relative;
      overflow: hidden;
    }

    .bg-animation {
      position: absolute;
      inset: 0;
      overflow: hidden;
    }

    .orb {
      position: absolute;
      border-radius: 50%;
      filter: blur(80px);
      animation: float 15s ease-in-out infinite;
    }

    .orb-1 {
      width: 600px;
      height: 600px;
      background: radial-gradient(circle, rgba(99, 102, 241, 0.3) 0%, transparent 70%);
      top: -200px;
      left: -200px;
      animation-delay: 0s;
    }

    .orb-2 {
      width: 500px;
      height: 500px;
      background: radial-gradient(circle, rgba(139, 92, 246, 0.25) 0%, transparent 70%);
      bottom: -150px;
      right: -150px;
      animation-delay: -5s;
    }

    .orb-3 {
      width: 400px;
      height: 400px;
      background: radial-gradient(circle, rgba(168, 85, 247, 0.2) 0%, transparent 70%);
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      animation-delay: -10s;
    }

    .grid-overlay {
      position: absolute;
      inset: 0;
      background-image: 
        linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px),
        linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
      background-size: 50px 50px;
      animation: gridMove 20s linear infinite;
    }

    @keyframes gridMove {
      0% { transform: translate(0, 0); }
      100% { transform: translate(50px, 50px); }
    }

    .login-container {
      position: relative;
      z-index: 10;
      width: 100%;
      max-width: 440px;
      padding: 20px;
    }

    .login-card {
      background: rgba(255, 255, 255, 0.03);
      backdrop-filter: blur(20px);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 24px;
      padding: 40px;
      box-shadow: 
        0 25px 50px -12px rgba(0, 0, 0, 0.5),
        inset 0 1px 0 rgba(255, 255, 255, 0.1);
    }

    .logo-section {
      text-align: center;
      margin-bottom: 32px;
    }

    .logo-icon {
      width: 80px;
      height: 80px;
      margin: 0 auto 20px;
      background: var(--primary-gradient);
      border-radius: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 10px 40px rgba(99, 102, 241, 0.4);
    }

    .logo-section h1 {
      color: white;
      font-size: 28px;
      font-weight: 800;
      margin-bottom: 8px;
      letter-spacing: -0.5px;
    }

    .logo-section p {
      color: rgba(255, 255, 255, 0.6);
      font-size: 15px;
    }

    .error-banner {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 16px;
      background: rgba(239, 68, 68, 0.15);
      border: 1px solid rgba(239, 68, 68, 0.3);
      border-radius: 12px;
      margin-bottom: 24px;
      color: #FCA5A5;
      font-size: 14px;
    }

    .login-form {
      margin-bottom: 24px;
    }

    .form-group {
      margin-bottom: 20px;
      opacity: 0;
    }

    .form-label {
      display: block;
      margin-bottom: 8px;
      color: rgba(255, 255, 255, 0.8);
      font-size: 14px;
      font-weight: 600;
    }

    .input-wrapper {
      position: relative;
    }

    .input-icon {
      position: absolute;
      left: 16px;
      top: 50%;
      transform: translateY(-50%);
      color: rgba(255, 255, 255, 0.4);
    }

    .form-input {
      width: 100%;
      padding: 14px 16px 14px 48px;
      background: rgba(255, 255, 255, 0.06);
      border: 2px solid rgba(255, 255, 255, 0.08);
      border-radius: 12px;
      color: white;
      font-size: 15px;
      transition: all 0.2s ease;

      &::placeholder {
        color: rgba(255, 255, 255, 0.35);
      }

      &:focus {
        outline: none;
        border-color: var(--primary);
        background: rgba(255, 255, 255, 0.1);
        box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.15);
      }
    }

    .toggle-password {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      color: rgba(255, 255, 255, 0.4);
      cursor: pointer;
      padding: 8px;
      transition: color 0.2s;

      &:hover {
        color: rgba(255, 255, 255, 0.8);
      }
    }

    .btn {
      opacity: 0;
    }

    .login-footer {
      text-align: center;
      opacity: 0;

      p {
        color: rgba(255, 255, 255, 0.6);
        font-size: 14px;

        a {
          color: var(--primary-light);
          font-weight: 600;
          transition: color 0.2s;

          &:hover {
            color: white;
          }
        }
      }
    }

    .divider {
      display: flex;
      align-items: center;
      margin: 20px 0;

      &::before, &::after {
        content: '';
        flex: 1;
        height: 1px;
        background: rgba(255, 255, 255, 0.1);
      }

      span {
        padding: 0 16px;
        color: rgba(255, 255, 255, 0.4);
        font-size: 13px;
      }
    }

    .admin-link {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 10px 20px;
      background: rgba(220, 38, 38, 0.15);
      border: 1px solid rgba(220, 38, 38, 0.3);
      border-radius: 10px;
      color: #FCA5A5;
      font-size: 14px;
      font-weight: 600;
      transition: all 0.2s;

      &:hover {
        background: rgba(220, 38, 38, 0.25);
        border-color: rgba(220, 38, 38, 0.5);
      }
    }

    .floating-shapes {
      position: absolute;
      inset: 0;
      pointer-events: none;
      overflow: hidden;
    }

    .shape {
      position: absolute;
      border-radius: 50%;
      opacity: 0.5;
    }

    .shape-1 {
      width: 10px;
      height: 10px;
      background: var(--primary);
      top: 20%;
      left: 10%;
      animation: float 6s ease-in-out infinite;
    }

    .shape-2 {
      width: 6px;
      height: 6px;
      background: var(--secondary);
      top: 60%;
      right: 15%;
      animation: float 8s ease-in-out infinite reverse;
    }

    .shape-3 {
      width: 8px;
      height: 8px;
      background: var(--accent);
      bottom: 25%;
      left: 20%;
      animation: float 7s ease-in-out infinite;
      animation-delay: -2s;
    }

    @keyframes float {
      0%, 100% { transform: translateY(0) rotate(0deg); }
      50% { transform: translateY(-20px) rotate(180deg); }
    }

    @media (max-width: 480px) {
      .login-card {
        padding: 28px 20px;
      }

      .logo-section h1 {
        font-size: 24px;
      }
    }
  `]
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  loading = signal(false);
  error = signal<string | null>(null);
  showPassword = signal(false);

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  onLogin(): void {
    if (!this.email || !this.password) {
      this.error.set('Please enter your email and password');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.user.role === 'ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Invalid email or password');
      }
    });
  }
}
