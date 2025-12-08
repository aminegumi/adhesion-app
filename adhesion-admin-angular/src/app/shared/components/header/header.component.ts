import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <header class="header">
      <div class="header-left">
        <h1 class="page-title">{{ getPageTitle() }}</h1>
      </div>
      
      <div class="header-right">
        <button class="notification-btn">
          <span class="material-icons">notifications</span>
          <span class="badge">3</span>
        </button>
        
        <div class="user-menu">
          <div class="avatar">
            <span class="material-icons">person</span>
          </div>
          <div class="user-info">
            <span class="user-name">{{ authService.currentUser()?.firstName || 'Admin' }}</span>
            <span class="user-role">Administrator</span>
          </div>
          <button class="logout-btn" (click)="logout()">
            <span class="material-icons">logout</span>
          </button>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .header {
      background: white;
      border-bottom: 1px solid var(--border-color);
      padding: 16px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .page-title {
      font-size: 24px;
      font-weight: 600;
      color: var(--text-primary);
    }
    
    .header-right {
      display: flex;
      align-items: center;
      gap: 24px;
    }
    
    .notification-btn {
      position: relative;
      background: none;
      border: none;
      cursor: pointer;
      padding: 8px;
      border-radius: 8px;
      transition: background 0.2s;
    }
    
    .notification-btn:hover {
      background: var(--bg-primary);
    }
    
    .notification-btn .badge {
      position: absolute;
      top: 4px;
      right: 4px;
      background: var(--danger-color);
      color: white;
      font-size: 10px;
      padding: 2px 6px;
      border-radius: 10px;
    }
    
    .user-menu {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    
    .avatar {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #4f46e5, #7c3aed);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
    }
    
    .user-info {
      display: flex;
      flex-direction: column;
    }
    
    .user-name {
      font-weight: 600;
      font-size: 14px;
    }
    
    .user-role {
      font-size: 12px;
      color: var(--text-secondary);
    }
    
    .logout-btn {
      background: none;
      border: none;
      cursor: pointer;
      padding: 8px;
      border-radius: 8px;
      color: var(--text-secondary);
      transition: all 0.2s;
    }
    
    .logout-btn:hover {
      background: #fee2e2;
      color: var(--danger-color);
    }
  `]
})
export class HeaderComponent {
  authService = inject(AuthService);

  getPageTitle(): string {
    const path = window.location.pathname;
    const titles: Record<string, string> = {
      '/dashboard': 'Dashboard',
      '/patients': 'Patients',
      '/tests': 'Psychological Tests',
      '/profiles': 'Psychological Profiles',
      '/treatment-plans': 'Treatment Plans',
      '/analytics': 'Analytics'
    };
    return titles[path] || 'Dashboard';
  }

  logout(): void {
    this.authService.logout();
  }
}
