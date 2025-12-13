import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <!-- Logo -->
      <div class="sidebar-header">
        <div class="logo">
          <span class="material-icons-round">favorite</span>
        </div>
        <span class="logo-text">Adhesion</span>
      </div>

      <!-- Navigation -->
      <nav class="sidebar-nav">
        <div class="nav-section">
          <span class="nav-label">Main</span>
          <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">home</span>
            <span>Dashboard</span>
          </a>
          <a routerLink="/profile" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">person</span>
            <span>My Profile</span>
          </a>
        </div>

        <div class="nav-section">
          <span class="nav-label">Health Tools</span>
          <a routerLink="/tests" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">science</span>
            <span>Psychological Tests</span>
          </a>
          <a routerLink="/emotions" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">face</span>
            <span>Emotion Detection</span>
          </a>
          <a routerLink="/predictions" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">insights</span>
            <span>AI Predictions</span>
          </a>
        </div>

        <div class="nav-section">
          <span class="nav-label">AI Assistants</span>
          <a routerLink="/motivation" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">auto_awesome</span>
            <span>Get Motivated</span>
          </a>
          <a routerLink="/ai-assistant" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">spa</span>
            <span>Serenity AI</span>
          </a>
          <a routerLink="/recommendations" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">lightbulb</span>
            <span>Recommendations</span>
          </a>
        </div>

        <div class="nav-section">
          <span class="nav-label">Treatment</span>
          <a routerLink="/treatment-plans" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">assignment</span>
            <span>Treatment Plans</span>
          </a>
          <a routerLink="/medications" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">medication</span>
            <span>My Medications</span>
          </a>
          <a routerLink="/history" routerLinkActive="active" class="nav-item">
            <span class="material-icons-round">history</span>
            <span>Activity History</span>
          </a>
        </div>
      </nav>

      <!-- User Section -->
      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-avatar">
            {{ authService.userName().charAt(0).toUpperCase() }}
          </div>
          <div class="user-details">
            <span class="user-name">{{ authService.userName() }}</span>
            <span class="user-role">Health Member</span>
          </div>
        </div>
        <button class="logout-btn" (click)="logout()">
          <span class="material-icons-round">logout</span>
          <span>Sign Out</span>
        </button>
      </div>
    </aside>
  `,
  styles: [`
    .sidebar {
      position: fixed;
      left: 0;
      top: 0;
      bottom: 0;
      width: 280px;
      background: white;
      border-right: 1px solid var(--gray-200);
      display: flex;
      flex-direction: column;
      z-index: 100;
    }

    .sidebar-header {
      padding: 24px;
      display: flex;
      align-items: center;
      gap: 14px;
      border-bottom: 1px solid var(--gray-100);

      .logo {
        width: 44px;
        height: 44px;
        background: var(--primary-gradient);
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;

        .material-icons-round {
          color: white;
          font-size: 24px;
        }
      }

      .logo-text {
        font-size: 1.375rem;
        font-weight: 800;
        color: var(--gray-900);
        letter-spacing: -0.02em;
      }
    }

    .sidebar-nav {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
    }

    .nav-section {
      margin-bottom: 24px;
    }

    .nav-label {
      display: block;
      font-size: 0.6875rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.1em;
      color: var(--gray-400);
      padding: 0 12px;
      margin-bottom: 8px;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border-radius: var(--radius-md);
      color: var(--gray-600);
      font-weight: 500;
      transition: all 0.2s ease;

      .material-icons-round {
        font-size: 20px;
      }

      &:hover {
        background: var(--gray-50);
        color: var(--gray-900);
      }

      &.active {
        background: rgba(99, 102, 241, 0.1);
        color: var(--primary);

        .material-icons-round {
          color: var(--primary);
        }
      }
    }

    .sidebar-footer {
      padding: 16px;
      border-top: 1px solid var(--gray-100);
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background: var(--gray-50);
      border-radius: var(--radius-lg);
      margin-bottom: 12px;

      .user-avatar {
        width: 40px;
        height: 40px;
        background: var(--primary-gradient);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-weight: 700;
        font-size: 1rem;
      }

      .user-details {
        display: flex;
        flex-direction: column;

        .user-name {
          font-weight: 600;
          color: var(--gray-900);
          font-size: 0.9375rem;
        }

        .user-role {
          font-size: 0.75rem;
          color: var(--gray-500);
        }
      }
    }

    .logout-btn {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 12px;
      background: rgba(239, 68, 68, 0.1);
      border: none;
      border-radius: var(--radius-md);
      color: var(--error);
      font-weight: 600;
      font-size: 0.9375rem;
      cursor: pointer;
      transition: all 0.2s ease;

      .material-icons-round { font-size: 20px; }

      &:hover {
        background: rgba(239, 68, 68, 0.15);
      }
    }

    @media (max-width: 1024px) {
      .sidebar {
        transform: translateX(-100%);
        transition: transform 0.3s ease;

        &.open {
          transform: translateX(0);
        }
      }
    }
  `]
})
export class SidebarComponent {
  authService = inject(AuthService);

  logout(): void {
    this.authService.logout();
  }
}

