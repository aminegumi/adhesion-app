import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <div class="logo">
        <span class="material-icons">psychology</span>
        <span class="logo-text">Adhesion Admin</span>
      </div>
      
      <nav class="nav-menu">
        <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">
          <span class="material-icons">dashboard</span>
          <span>Dashboard</span>
        </a>
        <a routerLink="/patients" routerLinkActive="active" class="nav-item">
          <span class="material-icons">people</span>
          <span>Patients</span>
        </a>
        <a routerLink="/tests" routerLinkActive="active" class="nav-item">
          <span class="material-icons">assignment</span>
          <span>Tests</span>
        </a>
        <a routerLink="/profiles" routerLinkActive="active" class="nav-item">
          <span class="material-icons">psychology</span>
          <span>Profiles</span>
        </a>
        <a routerLink="/treatment-plans" routerLinkActive="active" class="nav-item">
          <span class="material-icons">healing</span>
          <span>Treatment Plans</span>
        </a>
        <a routerLink="/analytics" routerLinkActive="active" class="nav-item">
          <span class="material-icons">analytics</span>
          <span>Analytics</span>
        </a>
      </nav>
      
      <div class="sidebar-footer">
        <div class="version">v1.0.0</div>
      </div>
    </aside>
  `,
  styles: [`
    .sidebar {
      position: fixed;
      left: 0;
      top: 0;
      bottom: 0;
      width: 260px;
      background: linear-gradient(180deg, #1e1b4b 0%, #312e81 100%);
      color: white;
      display: flex;
      flex-direction: column;
      z-index: 100;
    }
    
    .logo {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 24px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }
    
    .logo .material-icons {
      font-size: 32px;
      color: #a5b4fc;
    }
    
    .logo-text {
      font-size: 18px;
      font-weight: 700;
    }
    
    .nav-menu {
      flex: 1;
      padding: 16px 12px;
    }
    
    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      color: rgba(255, 255, 255, 0.7);
      text-decoration: none;
      border-radius: 8px;
      margin-bottom: 4px;
      transition: all 0.2s;
    }
    
    .nav-item:hover {
      background: rgba(255, 255, 255, 0.1);
      color: white;
    }
    
    .nav-item.active {
      background: rgba(165, 180, 252, 0.2);
      color: white;
    }
    
    .nav-item .material-icons {
      font-size: 20px;
    }
    
    .sidebar-footer {
      padding: 16px 24px;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }
    
    .version {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.5);
    }
  `]
})
export class SidebarComponent {}
