import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, User, Test } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="admin-page">
      <!-- Sidebar -->
      <aside class="admin-sidebar">
        <div class="sidebar-header">
          <div class="logo">
            <div class="logo-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/>
              </svg>
            </div>
            <span>Admin Portal</span>
          </div>
        </div>

        <nav class="sidebar-nav">
          <a routerLink="/admin/dashboard" class="nav-item active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>
            <span>Dashboard</span>
          </a>
          <a routerLink="/admin/tests" class="nav-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
            <span>Test Management</span>
          </a>
          <a routerLink="/admin/users" class="nav-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
            <span>User Data</span>
          </a>
        </nav>

        <div class="sidebar-footer">
          <div class="admin-info">
            <div class="admin-avatar">{{ adminName().charAt(0) }}</div>
            <div class="admin-details">
              <span class="admin-name">{{ adminName() }}</span>
              <span class="admin-role">Administrator</span>
            </div>
          </div>
          <button class="logout-btn" (click)="logout()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
          </button>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="admin-content">
        <header class="admin-header">
          <div>
            <h1>Admin Dashboard</h1>
            <p>Manage tests, users, and view analytics</p>
          </div>
          <button class="btn btn-danger" (click)="showCreateAdminDialog = true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            Create Admin
          </button>
        </header>

        <!-- Stats -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon users">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3z"/></svg>
            </div>
            <div class="stat-details">
              <span class="stat-value">{{ consentedUsers().length }}</span>
              <span class="stat-label">Consented Users</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon tests">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/></svg>
            </div>
            <div class="stat-details">
              <span class="stat-value">{{ tests().length }}</span>
              <span class="stat-label">Active Tests</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon total">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            </div>
            <div class="stat-details">
              <span class="stat-value">{{ allUsers().length }}</span>
              <span class="stat-label">Total Users</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon admins">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/></svg>
            </div>
            <div class="stat-details">
              <span class="stat-value">{{ adminCount() }}</span>
              <span class="stat-label">Administrators</span>
            </div>
          </div>
        </div>

        <!-- Quick Actions -->
        <section class="quick-actions">
          <h2>Quick Actions</h2>
          <div class="actions-grid">
            <a routerLink="/admin/tests" class="action-card">
              <div class="action-icon tests">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/></svg>
              </div>
              <div class="action-content">
                <h3>Manage Tests</h3>
                <p>Create and edit psychological tests</p>
              </div>
            </a>
            <a routerLink="/admin/users" class="action-card">
              <div class="action-icon users">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3z"/></svg>
              </div>
              <div class="action-content">
                <h3>View Users</h3>
                <p>Access consented user data</p>
              </div>
            </a>
            <button class="action-card" (click)="importTests()">
              <div class="action-icon import">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z"/></svg>
              </div>
              <div class="action-content">
                <h3>Import Tests</h3>
                <p>Import from JSON file</p>
              </div>
            </button>
          </div>
        </section>

        <!-- Recent Users -->
        <section class="recent-users">
          <h2>Recent Consented Users</h2>
          @if (consentedUsers().length === 0) {
            <div class="empty-state">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3z"/></svg>
              <p>No users have given consent yet</p>
            </div>
          } @else {
            <div class="users-list">
              @for (user of consentedUsers().slice(0, 5); track user.id) {
                <a [routerLink]="['/admin/users']" [queryParams]="{userId: user.id}" class="user-card">
                  <div class="user-avatar">{{ user.displayName?.charAt(0) || 'U' }}</div>
                  <div class="user-info">
                    <span class="user-name">{{ user.displayName }}</span>
                    <span class="user-email">{{ user.email }}</span>
                  </div>
                  <span class="consent-badge">Consented</span>
                </a>
              }
            </div>
          }
        </section>

        <!-- JSON Import Guide -->
        <section class="json-guide">
          <div class="guide-header" (click)="showJsonGuide = !showJsonGuide">
            <h2>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/></svg>
              JSON Test Import Guide
            </h2>
            <svg class="chevron" [class.open]="showJsonGuide" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"/></svg>
          </div>
          @if (showJsonGuide) {
            <div class="guide-content">
              <p class="guide-intro">Use this JSON structure to import psychological tests with questions. Click "Import Tests" to upload your JSON file.</p>
              
              <div class="code-block">
                <div class="code-header">
                  <span>tests.json</span>
                  <button class="copy-btn" (click)="copyJsonExample()">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg>
                    {{ copiedJson ? 'Copied!' : 'Copy' }}
                  </button>
                </div>
                <pre class="code-content">{{ jsonExample }}</pre>
              </div>

              <div class="field-guide">
                <h3>Field Descriptions</h3>
                <div class="field-list">
                  <div class="field-item">
                    <span class="field-name">code</span>
                    <span class="field-desc">Unique identifier for the test (e.g., "PHQ-9", "GAD-7")</span>
                  </div>
                  <div class="field-item">
                    <span class="field-name">title</span>
                    <span class="field-desc">Display name of the test</span>
                  </div>
                  <div class="field-item">
                    <span class="field-name">description</span>
                    <span class="field-desc">Brief explanation of the test purpose</span>
                  </div>
                  <div class="field-item">
                    <span class="field-name">active</span>
                    <span class="field-desc">Set to true to make test available to users</span>
                  </div>
                  <div class="field-item">
                    <span class="field-name">questions</span>
                    <span class="field-desc">Array of question objects</span>
                  </div>
                </div>

                <h4>Question Fields</h4>
                <div class="field-list">
                  <div class="field-item">
                    <span class="field-name">code</span>
                    <span class="field-desc">Question identifier (e.g., "Q1", "Q2")</span>
                  </div>
                  <div class="field-item">
                    <span class="field-name">text</span>
                    <span class="field-desc">The question text shown to user</span>
                  </div>
                  <div class="field-item">
                    <span class="field-name">orderIndex</span>
                    <span class="field-desc">Display order (0, 1, 2...)</span>
                  </div>
                  <div class="field-item">
                    <span class="field-name">minScore / maxScore</span>
                    <span class="field-desc">Score range (typically 0-3 or 0-4)</span>
                  </div>
                  <div class="field-item">
                    <span class="field-name">reverseScored</span>
                    <span class="field-desc">Set true if higher answer means lower score</span>
                  </div>
                </div>
              </div>
            </div>
          }
        </section>
      </main>

      <!-- Create Admin Dialog -->
      @if (showCreateAdminDialog) {
        <div class="dialog-overlay" (click)="showCreateAdminDialog = false">
          <div class="dialog" (click)="$event.stopPropagation()">
            <h2>Create New Admin</h2>
            <div class="form-group">
              <label>Full Name</label>
              <input type="text" [(ngModel)]="newAdmin.displayName" class="form-input-dark" placeholder="Enter name">
            </div>
            <div class="form-group">
              <label>Email</label>
              <input type="email" [(ngModel)]="newAdmin.email" class="form-input-dark" placeholder="Enter email">
            </div>
            <div class="form-group">
              <label>Password</label>
              <input type="password" [(ngModel)]="newAdmin.password" class="form-input-dark" placeholder="Enter password">
            </div>
            <div class="dialog-actions">
              <button class="btn btn-ghost" (click)="showCreateAdminDialog = false">Cancel</button>
              <button class="btn btn-danger" (click)="createAdmin()" [disabled]="creatingAdmin()">
                @if (creatingAdmin()) {
                  <div class="spinner spinner-sm spinner-white"></div>
                } @else {
                  Create Admin
                }
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .admin-page {
      display: flex;
      min-height: 100vh;
      background: var(--dark-bg);
    }

    .admin-sidebar {
      width: 260px;
      background: var(--dark-surface);
      border-right: 1px solid var(--dark-border);
      display: flex;
      flex-direction: column;
      position: fixed;
      height: 100vh;
    }

    .sidebar-header {
      padding: 24px;
      border-bottom: 1px solid var(--dark-border);
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 18px;
      font-weight: 700;
      color: white;
    }

    .logo-icon {
      width: 40px;
      height: 40px;
      background: var(--admin-gradient);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
    }

    .sidebar-nav {
      flex: 1;
      padding: 16px;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 16px;
      border-radius: 12px;
      color: rgba(255, 255, 255, 0.6);
      font-weight: 500;
      margin-bottom: 4px;
      transition: all 0.2s;

      &:hover {
        background: rgba(255, 255, 255, 0.05);
        color: white;
      }

      &.active {
        background: var(--admin-primary);
        color: white;
      }
    }

    .sidebar-footer {
      padding: 16px;
      border-top: 1px solid var(--dark-border);
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .admin-info {
      display: flex;
      align-items: center;
      gap: 10px;
      flex: 1;
    }

    .admin-avatar {
      width: 36px;
      height: 36px;
      background: var(--admin-gradient);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: 700;
    }

    .admin-details {
      display: flex;
      flex-direction: column;
    }

    .admin-name {
      color: white;
      font-weight: 600;
      font-size: 14px;
    }

    .admin-role {
      color: rgba(255, 255, 255, 0.5);
      font-size: 12px;
    }

    .logout-btn {
      width: 36px;
      height: 36px;
      background: rgba(239, 68, 68, 0.2);
      border: none;
      border-radius: 10px;
      color: var(--admin-primary-light);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;

      &:hover {
        background: rgba(239, 68, 68, 0.3);
      }
    }

    .admin-content {
      flex: 1;
      margin-left: 260px;
      padding: 32px;
    }

    .admin-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 32px;

      h1 {
        color: white;
        font-size: 28px;
        margin-bottom: 4px;
      }

      p {
        color: rgba(255, 255, 255, 0.5);
      }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 20px;
      margin-bottom: 32px;
    }

    .stat-card {
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 16px;
      padding: 24px;
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;

      &.users { background: rgba(16, 185, 129, 0.2); color: var(--success); }
      &.tests { background: rgba(99, 102, 241, 0.2); color: var(--primary); }
      &.total { background: rgba(245, 158, 11, 0.2); color: var(--warning); }
      &.admins { background: rgba(220, 38, 38, 0.2); color: var(--admin-primary); }
    }

    .stat-value {
      display: block;
      font-size: 32px;
      font-weight: 800;
      color: white;
    }

    .stat-label {
      color: rgba(255, 255, 255, 0.5);
      font-size: 14px;
    }

    .quick-actions h2, .recent-users h2, .json-guide h2 {
      color: white;
      font-size: 20px;
      margin-bottom: 16px;
    }

    .actions-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
      margin-bottom: 32px;
    }

    .action-card {
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 16px;
      padding: 24px;
      display: flex;
      align-items: center;
      gap: 16px;
      cursor: pointer;
      transition: all 0.2s;
      text-align: left;

      &:hover {
        border-color: var(--admin-primary);
        transform: translateY(-2px);
      }
    }

    .action-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;

      &.tests { background: rgba(99, 102, 241, 0.2); color: var(--primary); }
      &.users { background: rgba(16, 185, 129, 0.2); color: var(--success); }
      &.import { background: rgba(245, 158, 11, 0.2); color: var(--warning); }
    }

    .action-content {
      h3 {
        color: white;
        font-size: 16px;
        margin-bottom: 4px;
      }

      p {
        color: rgba(255, 255, 255, 0.5);
        font-size: 13px;
      }
    }

    .empty-state {
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 16px;
      padding: 48px;
      text-align: center;
      color: rgba(255, 255, 255, 0.3);

      p {
        margin-top: 12px;
        color: rgba(255, 255, 255, 0.5);
      }
    }

    .users-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .user-card {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 16px;
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 12px;
      transition: all 0.2s;

      &:hover {
        border-color: rgba(255, 255, 255, 0.2);
      }
    }

    .user-avatar {
      width: 44px;
      height: 44px;
      background: rgba(99, 102, 241, 0.2);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--primary);
      font-weight: 700;
    }

    .user-info {
      flex: 1;
    }

    .user-name {
      display: block;
      color: white;
      font-weight: 600;
    }

    .user-email {
      color: rgba(255, 255, 255, 0.5);
      font-size: 13px;
    }

    .consent-badge {
      padding: 6px 12px;
      background: rgba(16, 185, 129, 0.2);
      border-radius: 20px;
      color: var(--success);
      font-size: 12px;
      font-weight: 600;
    }

    .dialog-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.7);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .dialog {
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 20px;
      padding: 32px;
      width: 100%;
      max-width: 400px;

      h2 {
        color: white;
        margin-bottom: 24px;
      }
    }

    .form-group {
      margin-bottom: 16px;

      label {
        display: block;
        color: rgba(255, 255, 255, 0.7);
        font-size: 14px;
        margin-bottom: 8px;
      }
    }

    .form-input-dark {
      width: 100%;
      padding: 12px 16px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--dark-border);
      border-radius: 10px;
      color: white;
      font-size: 15px;

      &::placeholder {
        color: rgba(255, 255, 255, 0.3);
      }

      &:focus {
        outline: none;
        border-color: var(--admin-primary);
      }
    }

    .dialog-actions {
      display: flex;
      gap: 12px;
      margin-top: 24px;
    }

    .btn-ghost {
      flex: 1;
      background: transparent;
      color: rgba(255, 255, 255, 0.6);

      &:hover {
        background: rgba(255, 255, 255, 0.05);
      }
    }

    .btn-danger {
      flex: 2;
    }

    /* JSON Guide Styles */
    .json-guide {
      margin-top: 32px;
      background: var(--dark-surface);
      border: 1px solid var(--dark-border);
      border-radius: 16px;
      overflow: hidden;
    }

    .guide-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
      cursor: pointer;
      transition: background 0.2s;

      &:hover {
        background: rgba(255, 255, 255, 0.02);
      }

      h2 {
        display: flex;
        align-items: center;
        gap: 12px;
        margin: 0;
        font-size: 16px;
      }

      .chevron {
        color: rgba(255, 255, 255, 0.5);
        transition: transform 0.3s;

        &.open {
          transform: rotate(180deg);
        }
      }
    }

    .guide-content {
      padding: 0 24px 24px;
      border-top: 1px solid var(--dark-border);
    }

    .guide-intro {
      color: rgba(255, 255, 255, 0.6);
      font-size: 14px;
      margin: 20px 0;
      line-height: 1.6;
    }

    .code-block {
      background: rgba(0, 0, 0, 0.3);
      border-radius: 12px;
      overflow: hidden;
      margin-bottom: 24px;
    }

    .code-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      background: rgba(0, 0, 0, 0.2);
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);

      span {
        color: rgba(255, 255, 255, 0.6);
        font-size: 13px;
      }
    }

    .copy-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      background: rgba(255, 255, 255, 0.1);
      border: none;
      border-radius: 6px;
      color: rgba(255, 255, 255, 0.7);
      font-size: 12px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: rgba(255, 255, 255, 0.15);
        color: white;
      }
    }

    .code-content {
      padding: 16px;
      margin: 0;
      font-family: 'Fira Code', 'Consolas', monospace;
      font-size: 12px;
      line-height: 1.6;
      color: #A5D6FF;
      overflow-x: auto;
      white-space: pre;
    }

    .field-guide {
      h3, h4 {
        color: white;
        font-size: 14px;
        margin-bottom: 12px;
      }

      h4 {
        margin-top: 20px;
        color: rgba(255, 255, 255, 0.8);
      }
    }

    .field-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .field-item {
      display: flex;
      gap: 12px;
      padding: 10px 14px;
      background: rgba(255, 255, 255, 0.03);
      border-radius: 8px;
    }

    .field-name {
      font-family: 'Fira Code', monospace;
      font-size: 13px;
      color: #7EE787;
      min-width: 120px;
    }

    .field-desc {
      color: rgba(255, 255, 255, 0.6);
      font-size: 13px;
    }

    @media (max-width: 1200px) {
      .stats-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      .actions-grid {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 768px) {
      .admin-sidebar {
        display: none;
      }

      .admin-content {
        margin-left: 0;
      }

      .stats-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class AdminDashboardComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  consentedUsers = signal<User[]>([]);
  allUsers = signal<User[]>([]);
  tests = signal<Test[]>([]);
  adminCount = signal(0);

  showCreateAdminDialog = false;
  showJsonGuide = false;
  copiedJson = false;
  creatingAdmin = signal(false);
  newAdmin = { displayName: '', email: '', password: '' };

  jsonExample = `{
  "tests": [
    {
      "code": "PHQ-9",
      "title": "Patient Health Questionnaire-9",
      "description": "Depression screening tool",
      "active": true,
      "questions": [
        {
          "code": "Q1",
          "text": "Little interest or pleasure in doing things?",
          "orderIndex": 0,
          "minScore": 0,
          "maxScore": 3,
          "reverseScored": false
        },
        {
          "code": "Q2",
          "text": "Feeling down, depressed, or hopeless?",
          "orderIndex": 1,
          "minScore": 0,
          "maxScore": 3,
          "reverseScored": false
        }
      ]
    }
  ]
}`;

  ngOnInit(): void {
    this.loadData();
  }

  adminName(): string {
    return this.auth.user()?.displayName || 'Admin';
  }

  loadData(): void {
    this.api.getConsentedUsers().subscribe({
      next: users => this.consentedUsers.set(users),
      error: () => {}
    });

    this.api.getAllUsers().subscribe({
      next: users => {
        this.allUsers.set(users);
        this.adminCount.set(users.filter(u => u.role === 'ADMIN').length);
      },
      error: () => {}
    });

    this.api.getTests().subscribe({
      next: tests => this.tests.set(tests),
      error: () => {}
    });
  }

  createAdmin(): void {
    if (!this.newAdmin.displayName || !this.newAdmin.email || !this.newAdmin.password) {
      return;
    }

    this.creatingAdmin.set(true);
    this.api.createAdmin(this.newAdmin).subscribe({
      next: () => {
        this.showCreateAdminDialog = false;
        this.newAdmin = { displayName: '', email: '', password: '' };
        this.creatingAdmin.set(false);
        this.loadData();
      },
      error: () => {
        this.creatingAdmin.set(false);
      }
    });
  }

  importTests(): void {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    input.onchange = (e: any) => {
      const file = e.target.files[0];
      if (file) {
        const reader = new FileReader();
        reader.onload = (event: any) => {
          try {
            const json = JSON.parse(event.target.result);
            const tests = Array.isArray(json) ? json : json.tests || [];
            tests.forEach((test: any) => {
              this.api.createTest(test).subscribe();
            });
            setTimeout(() => this.loadData(), 1000);
          } catch (e) {
            console.error('Invalid JSON file');
          }
        };
        reader.readAsText(file);
      }
    };
    input.click();
  }

  copyJsonExample(): void {
    navigator.clipboard.writeText(this.jsonExample).then(() => {
      this.copiedJson = true;
      setTimeout(() => this.copiedJson = false, 2000);
    });
  }

  logout(): void {
    this.auth.logout();
  }
}
