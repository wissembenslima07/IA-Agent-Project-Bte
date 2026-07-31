import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'dossiers',
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./features/dossiers/dossier-list/dossier-list').then(m => m.DossierListComponent) },
      { path: 'nouveau', loadComponent: () => import('./features/dossiers/dossier-create/dossier-create').then(m => m.DossierCreateComponent) },
      { path: ':id', loadComponent: () => import('./features/dossiers/dossier-detail/dossier-detail').then(m => m.DossierDetailComponent) },
    ]
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];