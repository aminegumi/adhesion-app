import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="dashboard-page">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <div class="logo">
            <div class="logo-icon">
              <svg width="28" height="28" viewBox="0 0 48 48" fill="none">
                <path d="M24 4C12.954 4 4 12.954 4 24s8.954 20 20 20 20-8.954 20-20S35.046 4 24 4z" fill="url(#grad)"/>
                <path d="M24 12c-6.627 0-12 5.373-12 12s5.373 12 12 12 12-5.373 12-12-5.373-12-12-12zm0 18c-3.314 0-6-2.686-6-6s2.686-6 6-6 6 2.686 6 6-2.686 6-6 6z" fill="white"/>
                <defs>
                  <linearGradient id="grad" x1="4" y1="4" x2="44" y2="44">
                    <stop offset="0%" stop-color="#6366F1"/>
                    <stop offset="100%" stop-color="#A855F7"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <span>Adhesion</span>
          </div>
        </div>

        <nav class="sidebar-nav">
          <div class="nav-section">
            <span class="nav-section-title">Main</span>
            <a routerLink="/dashboard" class="nav-item active">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>
              <span>Dashboard</span>
            </a>
            <a routerLink="/profile" class="nav-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              <span>My Profile</span>
            </a>
          </div>

          <div class="nav-section">
            <span class="nav-section-title">Health Tools</span>
            <a routerLink="/tests" class="nav-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19.35 10.04A7.49 7.49 0 0012 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 000 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z"/></svg>
              <span>Tests</span>
            </a>
            <a routerLink="/emotions" class="nav-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/></svg>
              <span>Emotions</span>
            </a>
            <a routerLink="/predictions" class="nav-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"/></svg>
              <span>Predictions</span>
            </a>
          </div>

          <div class="nav-section">
            <span class="nav-section-title">AI Assistants</span>
            <a routerLink="/ai-assistant" class="nav-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
              <span>AI Chat</span>
            </a>
            <a routerLink="/motivation" class="nav-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
              <span>Motivation</span>
            </a>
            <a routerLink="/recommendations" class="nav-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z"/></svg>
              <span>Tips</span>
            </a>
          </div>

          <div class="nav-section">
            <span class="nav-section-title">Treatment</span>
            <a routerLink="/treatment-plans" class="nav-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
              <span>Plans</span>
            </a>
            <a routerLink="/medications" class="nav-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M6 3h12v2H6zm11 3H7c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-1 9h-2.5v2.5h-3V15H8v-3h2.5V9.5h3V12H16v3z"/></svg>
              <span>Medications</span>
            </a>
            <a routerLink="/history" class="nav-item">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/></svg>
              <span>History</span>
            </a>
          </div>
        </nav>

        <div class="sidebar-footer">
          <button class="logout-btn" (click)="logout()">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="main-content">
        <!-- Header -->
        <header class="dashboard-header">
          <div class="header-left">
            <h1>{{ getGreeting() }}, {{ userName() }}!</h1>
            <p>Here's your wellness overview for today</p>
          </div>
          <div class="header-right">
            <a routerLink="/edit-profile" class="header-btn">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19.14 12.94c.04-.31.06-.63.06-.94 0-.31-.02-.63-.06-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>
            </a>
            <a routerLink="/profile" class="avatar">
              {{ userName().charAt(0).toUpperCase() }}
            </a>
          </div>
        </header>

        <!-- Stats Grid -->
        <div class="stats-grid animate-fade-in-up">
          <div class="stat-card wellness">
            <div class="stat-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
            </div>
            <div class="stat-content">
              <span class="stat-label">Wellness Score</span>
              <div class="stat-value-row">
                <span class="stat-value">{{ wellnessScore() }}</span>
                <span class="stat-unit">/100</span>
              </div>
              <div class="stat-bar">
                <div class="stat-bar-fill" [style.width.%]="wellnessScore()"></div>
              </div>
            </div>
          </div>

          <div class="stat-card tests">
            <div class="stat-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
            </div>
            <div class="stat-content">
              <span class="stat-label">Tests Completed</span>
              <span class="stat-value">{{ testsCompleted() }}</span>
            </div>
          </div>

          <div class="stat-card plans">
            <div class="stat-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1z"/></svg>
            </div>
            <div class="stat-content">
              <span class="stat-label">Active Plans</span>
              <span class="stat-value">{{ activePlans() }}</span>
            </div>
          </div>

          <div class="stat-card adherence">
            <div class="stat-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor"><path d="M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z"/></svg>
            </div>
            <div class="stat-content">
              <span class="stat-label">Adherence Rate</span>
              <span class="stat-value">{{ adherenceRate() }}%</span>
            </div>
          </div>
        </div>

        <!-- Quick Actions -->
        <section class="quick-actions animate-fade-in-up stagger-1">
          <h2>Quick Actions</h2>
          <div class="actions-grid">
            <a routerLink="/motivation" class="action-card motivation">
              <div class="action-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
              </div>
              <span>Get Motivated</span>
            </a>
            <a routerLink="/tests" class="action-card tests">
              <div class="action-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M19.35 10.04A7.49 7.49 0 0012 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 000 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z"/></svg>
              </div>
              <span>Take Test</span>
            </a>
            <a routerLink="/ai-assistant" class="action-card ai">
              <div class="action-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>
              </div>
              <span>AI Assistant</span>
            </a>
            <a routerLink="/treatment-plans" class="action-card plans">
              <div class="action-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/></svg>
              </div>
              <span>Action Plan</span>
            </a>
          </div>
        </section>

        <!-- Features Grid -->
        <section class="features-section animate-fade-in-up stagger-2">
          <h2>Health Features</h2>
          <div class="features-grid">
            <a routerLink="/emotions" class="feature-card">
              <div class="feature-icon emotions">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2z"/></svg>
              </div>
              <div class="feature-content">
                <h3>Emotions</h3>
                <p>AI Detection</p>
              </div>
              <svg class="feature-arrow" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8-8-8z"/></svg>
            </a>
            <a routerLink="/predictions" class="feature-card">
              <div class="feature-icon predictions">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"/></svg>
              </div>
              <div class="feature-content">
                <h3>Predictions</h3>
                <p>AI Analysis</p>
              </div>
              <svg class="feature-arrow" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8-8-8z"/></svg>
            </a>
            <a routerLink="/medications" class="feature-card">
              <div class="feature-icon medications">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M6 3h12v2H6zm11 3H7c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2z"/></svg>
              </div>
              <div class="feature-content">
                <h3>Medications</h3>
                <p>My Meds</p>
              </div>
              <svg class="feature-arrow" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8-8-8z"/></svg>
            </a>
            <a routerLink="/recommendations" class="feature-card">
              <div class="feature-icon recommendations">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z"/></svg>
              </div>
              <div class="feature-content">
                <h3>Tips</h3>
                <p>Recommendations</p>
              </div>
              <svg class="feature-arrow" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8-8-8z"/></svg>
            </a>
          </div>
        </section>
      </main>
    </div>
  `,
  styles: [`
    .dashboard-page {
      display: flex;
      min-height: 100vh;
      background: var(--gray-50);
    }

    /* Sidebar Styles */
    .sidebar {
      width: 260px;
      background: white;
      border-right: 1px solid var(--gray-200);
      display: flex;
      flex-direction: column;
      position: fixed;
      height: 100vh;
      z-index: 100;
    }

    .sidebar-header {
      padding: 24px;
      border-bottom: 1px solid var(--gray-100);
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 20px;
      font-weight: 700;
      color: var(--gray-900);
    }

    .logo-icon {
      width: 40px;
      height: 40px;
      background: var(--primary-gradient);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .sidebar-nav {
      flex: 1;
      padding: 16px 12px;
      overflow-y: auto;
    }

    .nav-section {
      margin-bottom: 24px;
    }

    .nav-section-title {
      display: block;
      padding: 0 12px;
      margin-bottom: 8px;
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: var(--gray-400);
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border-radius: 12px;
      color: var(--gray-600);
      font-weight: 500;
      transition: all 0.2s;

      &:hover {
        background: var(--gray-100);
        color: var(--gray-900);
      }

      &.active {
        background: var(--primary);
        color: white;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
      }
    }

    .sidebar-footer {
      padding: 16px;
      border-top: 1px solid var(--gray-100);
    }

    .logout-btn {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
      padding: 12px;
      background: rgba(239, 68, 68, 0.1);
      border: none;
      border-radius: 12px;
      color: var(--error);
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: rgba(239, 68, 68, 0.2);
      }
    }

    /* Main Content */
    .main-content {
      flex: 1;
      margin-left: 260px;
      padding: 32px;
    }

    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 32px;

      h1 {
        font-size: 28px;
        color: var(--gray-900);
        margin-bottom: 4px;
      }

      p {
        color: var(--gray-500);
        font-size: 15px;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .header-btn {
      width: 44px;
      height: 44px;
      background: white;
      border: 1px solid var(--gray-200);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--gray-600);
      transition: all 0.2s;

      &:hover {
        border-color: var(--primary);
        color: var(--primary);
      }
    }

    .avatar {
      width: 44px;
      height: 44px;
      background: var(--primary-gradient);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: 700;
      font-size: 18px;
    }

    /* Stats Grid */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 20px;
      margin-bottom: 32px;
    }

    .stat-card {
      background: white;
      border-radius: 20px;
      padding: 24px;
      display: flex;
      align-items: flex-start;
      gap: 16px;
      box-shadow: var(--shadow-sm);
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-4px);
        box-shadow: var(--shadow-lg);
      }
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .stat-card.wellness .stat-icon {
      background: linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(239, 68, 68, 0.2));
      color: var(--error);
    }

    .stat-card.tests .stat-icon {
      background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.2));
      color: var(--secondary);
    }

    .stat-card.plans .stat-icon {
      background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.2));
      color: var(--success);
    }

    .stat-card.adherence .stat-icon {
      background: linear-gradient(135deg, rgba(14, 165, 233, 0.1), rgba(14, 165, 233, 0.2));
      color: var(--info);
    }

    .stat-content {
      flex: 1;
    }

    .stat-label {
      display: block;
      font-size: 13px;
      color: var(--gray-500);
      margin-bottom: 4px;
    }

    .stat-value-row {
      display: flex;
      align-items: baseline;
      gap: 4px;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 800;
      color: var(--gray-900);
    }

    .stat-unit {
      font-size: 14px;
      color: var(--gray-400);
    }

    .stat-bar {
      height: 6px;
      background: var(--gray-100);
      border-radius: 3px;
      margin-top: 12px;
      overflow: hidden;
    }

    .stat-bar-fill {
      height: 100%;
      background: var(--primary-gradient);
      border-radius: 3px;
      transition: width 1s ease;
    }

    /* Quick Actions */
    .quick-actions {
      margin-bottom: 32px;

      h2 {
        font-size: 20px;
        margin-bottom: 16px;
      }
    }

    .actions-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
    }

    .action-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 20px;
      border-radius: 16px;
      color: white;
      font-weight: 600;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-4px);
      }

      &.motivation {
        background: linear-gradient(135deg, #F59E0B, #FBBF24);
        box-shadow: 0 8px 20px rgba(245, 158, 11, 0.3);
      }

      &.tests {
        background: linear-gradient(135deg, #6366F1, #8B5CF6);
        box-shadow: 0 8px 20px rgba(99, 102, 241, 0.3);
      }

      &.ai {
        background: linear-gradient(135deg, #14B8A6, #2DD4BF);
        box-shadow: 0 8px 20px rgba(20, 184, 166, 0.3);
      }

      &.plans {
        background: linear-gradient(135deg, #8B5CF6, #A855F7);
        box-shadow: 0 8px 20px rgba(139, 92, 246, 0.3);
      }
    }

    .action-icon {
      width: 40px;
      height: 40px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    /* Features */
    .features-section h2 {
      font-size: 20px;
      margin-bottom: 16px;
    }

    .features-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
    }

    .feature-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;
      background: white;
      border-radius: 16px;
      border: 1px solid var(--gray-100);
      transition: all 0.3s ease;

      &:hover {
        border-color: var(--primary);
        box-shadow: var(--shadow-md);

        .feature-arrow {
          transform: translateX(4px);
        }
      }
    }

    .feature-icon {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;

      &.emotions {
        background: rgba(168, 85, 247, 0.1);
        color: var(--accent);
      }

      &.predictions {
        background: rgba(14, 165, 233, 0.1);
        color: var(--info);
      }

      &.medications {
        background: rgba(16, 185, 129, 0.1);
        color: var(--success);
      }

      &.recommendations {
        background: rgba(245, 158, 11, 0.1);
        color: var(--warning);
      }
    }

    .feature-content {
      flex: 1;

      h3 {
        font-size: 16px;
        margin-bottom: 2px;
        color: var(--gray-900);
      }

      p {
        font-size: 13px;
        color: var(--gray-500);
      }
    }

    .feature-arrow {
      color: var(--gray-400);
      transition: transform 0.2s;
    }

    @media (max-width: 1200px) {
      .stats-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      .actions-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (max-width: 768px) {
      .sidebar {
        display: none;
      }

      .main-content {
        margin-left: 0;
        padding: 20px;
      }

      .stats-grid, .actions-grid, .features-grid {
        grid-template-columns: 1fr;
      }

      .dashboard-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 16px;
      }
    }
  `]
})
export class DashboardComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  userName = signal('User');
  wellnessScore = signal(50);
  testsCompleted = signal(0);
  activePlans = signal(0);
  adherenceRate = signal(0);

  ngOnInit(): void {
    this.loadUserData();
    this.loadDashboardData();
  }

  loadUserData(): void {
    const user = this.auth.user();
    if (user) {
      this.userName.set(user.displayName || 'User');
    }
  }

  loadDashboardData(): void {
    const userId = this.auth.getUserId();
    if (!userId) return;

    // Load test history
    this.api.getTestHistory(userId).subscribe({
      next: (tests) => {
        this.testsCompleted.set(tests.length);
        this.calculateWellnessScore(tests.length);
      },
      error: () => {}
    });

    // Load treatment plans
    this.api.getTreatmentPlans(userId).subscribe({
      next: (plans) => {
        const active = plans.filter(p => p.status === 'ACTIVE').length;
        this.activePlans.set(active);
      },
      error: () => {}
    });

    // Load adherence
    this.api.getAdherenceScore(userId).subscribe({
      next: (score) => {
        this.adherenceRate.set(Math.round(score));
      },
      error: () => {}
    });
  }

  calculateWellnessScore(testsCompleted: number): void {
    let score = 50;
    if (testsCompleted > 0) {
      score = Math.min(100, 50 + testsCompleted * 10);
    }
    this.wellnessScore.set(score);
  }

  getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good Morning';
    if (hour < 17) return 'Good Afternoon';
    return 'Good Evening';
  }

  logout(): void {
    this.auth.logout();
  }
}
