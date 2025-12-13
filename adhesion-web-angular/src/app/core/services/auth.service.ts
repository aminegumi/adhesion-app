import { Injectable, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService, User, LoginResponse, RegisterRequest } from './api.service';
import { Observable, tap, catchError, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private api = inject(ApiService);
  private router = inject(Router);

  private currentUser = signal<User | null>(null);
  private isAuthenticated = signal<boolean>(false);

  user = computed(() => this.currentUser());
  loggedIn = computed(() => this.isAuthenticated());
  isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  constructor() {
    this.loadUserFromStorage();
  }

  private loadUserFromStorage(): void {
    const userJson = localStorage.getItem('user');
    const token = localStorage.getItem('auth_token');
    
    if (userJson && token) {
      try {
        const user = JSON.parse(userJson) as User;
        this.currentUser.set(user);
        this.isAuthenticated.set(true);
      } catch {
        this.clearStorage();
      }
    }
  }

  private saveUserToStorage(user: User, token: string): void {
    localStorage.setItem('user', JSON.stringify(user));
    localStorage.setItem('auth_token', token);
    localStorage.setItem('user_id', user.id.toString());
    localStorage.setItem('user_role', user.role);
  }

  private clearStorage(): void {
    localStorage.removeItem('user');
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_id');
    localStorage.removeItem('user_role');
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.api.login(email, password).pipe(
      tap(response => {
        this.currentUser.set(response.user);
        this.isAuthenticated.set(true);
        this.saveUserToStorage(response.user, response.token);
      }),
      catchError(error => {
        console.error('Login failed:', error);
        return throwError(() => error);
      })
    );
  }

  register(request: RegisterRequest): Observable<User> {
    return this.api.register(request).pipe(
      catchError(error => {
        console.error('Registration failed:', error);
        return throwError(() => error);
      })
    );
  }

  logout(): void {
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    this.clearStorage();
    this.router.navigate(['/login']);
  }

  getUserId(): number | null {
    const userId = localStorage.getItem('user_id');
    return userId ? parseInt(userId, 10) : null;
  }

  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  updateCurrentUser(user: User): void {
    this.currentUser.set(user);
    const token = this.getToken();
    if (token) {
      this.saveUserToStorage(user, token);
    }
  }
}
