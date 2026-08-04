import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { switchMap, tap } from 'rxjs';

import { environment } from '../../../environments/environment';

interface AuthResponse {
  token: string;
  role: string;
  email: string;
}
export interface UtilisateurConnecte {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = `${environment.apiUrl}/auth`;

  private role = signal<string | null>(localStorage.getItem('role'));
  isAuthenticated = computed(() => !!localStorage.getItem('access_token'));
  currentRole = computed(() => this.role());

  
    private currentUser = signal<UtilisateurConnecte | null>(
    JSON.parse(localStorage.getItem('current_user') ?? 'null')
  );

  user = computed(() => this.currentUser());

  login(email: string, password: string) {
    return this.http.post<{ token: string; role: string; email: string }>(`${this.apiUrl}/login`, { email, password })
      .pipe(
        tap(res => {
          localStorage.setItem('access_token', res.token);
          localStorage.setItem('role', res.role);
          this.role.set(res.role);
        }),
        switchMap(() => this.chargerUtilisateurCourant())
      );
  }

  chargerUtilisateurCourant() {
    return this.http.get<UtilisateurConnecte>(`${this.apiUrl}/me`).pipe(
      tap(user => {
        localStorage.setItem('current_user', JSON.stringify(user));
        this.currentUser.set(user);
      })
    );
  }

  logout(): void {
    localStorage.clear();
    this.role.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }
}