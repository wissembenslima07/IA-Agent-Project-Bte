import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = `${environment.apiUrl}/auth`;

  private role = signal<string | null>(localStorage.getItem('role'));
  isAuthenticated = computed(() => !!localStorage.getItem('access_token'));
  currentRole = computed(() => this.role());

  login(email: string, password: string) {
    return this.http.post<{ token: string; role: string }>(`${this.apiUrl}/login`, { email, password })
      .pipe(tap(res => {
        localStorage.setItem('access_token', res.token);
        localStorage.setItem('role', res.role);
        this.role.set(res.role);
      }));
  }

  logout(): void {
    localStorage.clear();
    this.role.set(null);
    this.router.navigate(['/login']);
  }
}