import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'patients',
    loadComponent: () => import('./features/patients/patients.component').then(m => m.PatientsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'patients/:id',
    loadComponent: () => import('./features/patients/patient-detail/patient-detail.component').then(m => m.PatientDetailComponent),
    canActivate: [authGuard]
  },
  {
    path: 'tests',
    loadComponent: () => import('./features/tests/tests.component').then(m => m.TestsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'profiles',
    loadComponent: () => import('./features/profiles/profiles.component').then(m => m.ProfilesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'analytics',
    loadComponent: () => import('./features/analytics/analytics.component').then(m => m.AnalyticsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'treatment-plans',
    loadComponent: () => import('./features/treatment-plans/treatment-plans.component').then(m => m.TreatmentPlansComponent),
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
