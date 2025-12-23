import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  // Public Routes
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
  },
  {
    path: 'admin/login',
    loadComponent: () => import('./features/admin/admin-login/admin-login.component').then(m => m.AdminLoginComponent),
  },

  // User Routes (Protected)
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard],
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [authGuard],
  },
  {
    path: 'edit-profile',
    loadComponent: () => import('./features/profile/edit-profile/edit-profile.component').then(m => m.EditProfileComponent),
    canActivate: [authGuard],
  },
  {
    path: 'tests',
    loadComponent: () => import('./features/tests/tests.component').then(m => m.TestsComponent),
    canActivate: [authGuard],
  },
  {
    path: 'test/:id',
    loadComponent: () => import('./features/tests/take-test/take-test.component').then(m => m.TakeTestComponent),
    canActivate: [authGuard],
  },
  {
    path: 'emotions',
    loadComponent: () => import('./features/emotions/emotions.component').then(m => m.EmotionsComponent),
    canActivate: [authGuard],
  },
  {
    path: 'predictions',
    loadComponent: () => import('./features/predictions/predictions.component').then(m => m.PredictionsComponent),
    canActivate: [authGuard],
  },
  {
    path: 'medications',
    loadComponent: () => import('./features/medications/medications.component').then(m => m.MedicationsComponent),
    canActivate: [authGuard],
  },
  {
    path: 'todays-doses',
    loadComponent: () => import('./features/doses/todays-doses.component').then(m => m.TodaysDosesComponent),
    canActivate: [authGuard],
  },
  {
    path: 'treatment-plans',
    loadComponent: () => import('./features/treatment-plans/treatment-plans.component').then(m => m.TreatmentPlansComponent),
    canActivate: [authGuard],
  },
  {
    path: 'ai-assistant',
    loadComponent: () => import('./features/ai-assistant/ai-assistant.component').then(m => m.AiAssistantComponent),
    canActivate: [authGuard],
  },
  {
    path: 'motivation',
    loadComponent: () => import('./features/motivation/motivation.component').then(m => m.MotivationComponent),
    canActivate: [authGuard],
  },
  {
    path: 'recommendations',
    loadComponent: () => import('./features/recommendations/recommendations.component').then(m => m.RecommendationsComponent),
    canActivate: [authGuard],
  },
  {
    path: 'history',
    loadComponent: () => import('./features/history/history.component').then(m => m.HistoryComponent),
    canActivate: [authGuard],
  },

  // Admin Routes (Protected)
  {
    path: 'admin/dashboard',
    loadComponent: () => import('./features/admin/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/tests',
    loadComponent: () => import('./features/admin/test-management/test-management.component').then(m => m.TestManagementComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/users',
    loadComponent: () => import('./features/admin/user-data/user-data.component').then(m => m.UserDataComponent),
    canActivate: [adminGuard],
  },

  // Wildcard
  {
    path: '**',
    redirectTo: 'login',
  },
];

