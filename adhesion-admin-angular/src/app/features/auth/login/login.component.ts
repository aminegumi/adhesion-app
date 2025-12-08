import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <span class="material-icons logo-icon">psychology</span>
          <h1>Adhesion Admin</h1>
          <p>Sign in to manage patients and analytics</p>
        </div>
        
        @if (error()) {
          <div class="alert alert-error">
            {{ error() }}
          </div>
        }
        
        <form (ngSubmit)="onSubmit()" class="login-form">
          <div class="form-group">
            <label class="form-label">Email</label>
            <input 
              type="email" 
              class="form-input"
              [(ngModel)]="email"
              name="email"
              placeholder="admin@example.com"
              required
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">Password</label>
            <input 
              type="password" 
              class="form-input"
              [(ngModel)]="password"
              name="password"
              placeholder="••••••••"
              required
            />
          </div>
          
          <button type="submit" class="btn btn-primary btn-block" [disabled]="loading()">
            @if (loading()) {
              <span class="loading-spinner"></span>
            } @else {
              Sign In
            }
          </button>
        </form>
        
        <div class="login-footer">
          <p>Demo credentials: admin&#64;test.com / admin123</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4f46e5 100%);
      margin-left: -260px;
    }
    
    .login-card {
      background: white;
      border-radius: 16px;
      padding: 40px;
      width: 100%;
      max-width: 420px;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
    }
    
    .login-header {
      text-align: center;
      margin-bottom: 32px;
    }
    
    .logo-icon {
      font-size: 48px;
      color: #4f46e5;
      margin-bottom: 16px;
    }
    
    .login-header h1 {
      font-size: 24px;
      font-weight: 700;
      margin-bottom: 8px;
    }
    
    .login-header p {
      color: var(--text-secondary);
    }
    
    .login-form {
      margin-bottom: 24px;
    }
    
    .btn-block {
      width: 100%;
      justify-content: center;
      padding: 14px;
      font-size: 16px;
    }
    
    .login-footer {
      text-align: center;
      color: var(--text-secondary);
      font-size: 13px;
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

  onSubmit(): void {
    if (!this.email || !this.password) {
      this.error.set('Please enter email and password');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Login failed. Please check your credentials.');
      }
    });
  }
}
