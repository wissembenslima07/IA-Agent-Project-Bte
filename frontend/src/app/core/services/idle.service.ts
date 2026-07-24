import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class IdleService {
  private authService = inject(AuthService);
  private timeout: ReturnType<typeof setTimeout> | undefined;
  private readonly LIMIT_MS = 15 * 60 * 1000; // 15 min

  start(): void {
    ['click', 'keydown', 'mousemove'].forEach(evt =>
      window.addEventListener(evt, () => this.resetTimer())
    );
    this.resetTimer();
  }

  private resetTimer(): void {
    clearTimeout(this.timeout);
    this.timeout = setTimeout(() => this.authService.logout(), this.LIMIT_MS);
  }
}