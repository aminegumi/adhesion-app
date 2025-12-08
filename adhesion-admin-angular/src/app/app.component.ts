import { Component } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { HeaderComponent } from './shared/components/header/header.component';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, HeaderComponent],
  template: `
    <div class="app-layout" [class.login-layout]="isLoginPage">
      <ng-container *ngIf="!isLoginPage">
        <app-sidebar></app-sidebar>
      </ng-container>
      <div class="main-content" [class.full-width]="isLoginPage">
        <ng-container *ngIf="!isLoginPage">
          <app-header></app-header>
        </ng-container>
        <main class="page-content" [class.login-content]="isLoginPage">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .app-layout {
      display: flex;
      min-height: 100vh;
    }
    .app-layout.login-layout {
      display: block;
    }
    .main-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      margin-left: 260px;
    }
    .main-content.full-width {
      margin-left: 0;
    }
    .page-content {
      padding: 24px;
      flex: 1;
    }
    .page-content.login-content {
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
  `]
})
export class AppComponent {
  title = 'Adhesion Admin';
  isLoginPage = false;

  constructor(private router: Router) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.isLoginPage = event.urlAfterRedirects.includes('/login');
    });
    
    // Check initial route
    this.isLoginPage = this.router.url.includes('/login');
  }
}
