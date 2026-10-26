import { Injectable, Optional } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { User } from '../models/user.model';
import { Login } from '../models/login.model';
import { API_BASE_URL } from '../config/api.config';
export interface LoginResponse {
  token: string;
  username: string;
  userRole: string;
  userId: number;
}
export interface AdminLoginStartResponse {
  requiresAdminVerification: boolean;
  message: string;
}
export interface MessageResponse {
  message: string;
}
@Injectable({ providedIn: 'root' })
export class AuthService {
  public apiUrl = API_BASE_URL;
  constructor(
    @Optional() private http: HttpClient | null,
    @Optional() private router: Router | null,
  ) {}
  register(user: User): Observable<User> {
    return this.http!.post<User>(this.apiUrl + '/register', user);
  }
  login(login: Login): Observable<LoginResponse | AdminLoginStartResponse> {
    return this.http!.post<LoginResponse | AdminLoginStartResponse>(
      this.apiUrl + '/login',
      login,
    ).pipe(
      tap((result) => {
        if (!('token' in result)) return;
        localStorage.setItem('token', result.token);
        localStorage.setItem('username', result.username);
        localStorage.setItem('userRole', result.userRole);
        localStorage.setItem('userId', String(result.userId));
      }),
    );
  }
  verifyAdminLogin(email: string, code: string): Observable<LoginResponse> {
    return this.http!.post<LoginResponse>(this.apiUrl + '/admin-login/verify', {
      email,
      code,
    }).pipe(tap((result) => this.storeSession(result)));
  }
  verifyEmail(token: string): Observable<MessageResponse> {
    return this.http!.post<MessageResponse>(this.apiUrl + '/email/verify', {
      token,
    });
  }
  resendVerification(email: string): Observable<MessageResponse> {
    return this.http!.post<MessageResponse>(
      this.apiUrl + '/email/verify/resend',
      { email },
    );
  }
  requestPasswordReset(email: string): Observable<MessageResponse> {
    return this.http!.post<MessageResponse>(this.apiUrl + '/password/forgot', {
      email,
    });
  }
  resetPassword(token: string, password: string): Observable<MessageResponse> {
    return this.http!.post<MessageResponse>(this.apiUrl + '/password/reset', {
      token,
      password,
    });
  }
  googleSignInUrl(): string {
    const backend = this.apiUrl.replace(/\/api$/, '');
    const portalHost = window.location.hostname.match(
      /^8081-([a-z0-9-]+)\.premiumproject\.examly\.io$/i,
    );
    // The authorization request and Google's callback must share one host.
    const origin = portalHost
      ? `https://8080-${portalHost[1]}.premiumproject.examly.io`
      : backend;
    return origin + '/oauth2/authorization/google';
  }
  acceptGoogleToken(token: string): boolean {
    try {
      const encoded = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(
        atob(encoded.padEnd(Math.ceil(encoded.length / 4) * 4, '=')),
      );
      if (!payload.sub || !payload.userId || payload.exp * 1000 < Date.now())
        return false;
      localStorage.setItem('token', token);
      localStorage.setItem('username', payload.username || payload.sub);
      localStorage.setItem('userRole', payload.role || 'User');
      localStorage.setItem('userId', String(payload.userId));
      return true;
    } catch {
      return false;
    }
  }
  logout(): void {
    this.clearSession();
    this.router?.navigate(['/login']);
  }
  private clearSession(): void {
    ['token', 'username', 'userRole', 'userId'].forEach((key) =>
      localStorage.removeItem(key),
    );
  }
  isAuthenticated(): boolean {
    const token = localStorage.getItem('token');
    if (!token) return false;
    try {
      const encoded = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(
        atob(encoded.padEnd(Math.ceil(encoded.length / 4) * 4, '=')),
      );
      if (payload.exp && payload.exp * 1000 < Date.now()) {
        this.clearSession();
        return false;
      }
      return true;
    } catch {
      return false;
    }
  }
  isAdmin(): boolean {
    return (
      this.isAuthenticated() && localStorage.getItem('userRole') === 'Admin'
    );
  }
  isCustomer(): boolean {
    return (
      this.isAuthenticated() && localStorage.getItem('userRole') === 'User'
    );
  }
  getUserId(): number {
    return Number(localStorage.getItem('userId') || 0);
  }
  getUsername(): string {
    return localStorage.getItem('username') || '';
  }
  updateUsername(username: string): void {
    localStorage.setItem('username', username);
  }
  private storeSession(result: LoginResponse): void {
    localStorage.setItem('token', result.token);
    localStorage.setItem('username', result.username);
    localStorage.setItem('userRole', result.userRole);
    localStorage.setItem('userId', String(result.userId));
  }
}
