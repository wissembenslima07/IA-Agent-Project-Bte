import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';

interface NavLink {
  icon: string;
  label: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class SidebarComponent {
  private authService = inject(AuthService);

  navLinks: NavLink[] = [
    { icon: 'dashboard', label: 'Tableau de bord', route: '/dashboard' },
    { icon: 'folder_open', label: 'Dossiers de crédit', route: '/dossiers' },
    { icon: 'add_box', label: 'Nouveau dossier', route: '/dossiers/nouveau' },
    { icon: 'analytics', label: 'Rapports', route: '/rapports' },
    { icon: 'settings', label: 'Paramètres', route: '/parametres' }
  ];

  utilisateurConnecte = this.authService.user;

  getAvatarUrl(): string {
    const user = this.utilisateurConnecte();
    if (!user) return '';
    return `https://ui-avatars.com/api/?name=${user.prenom}+${user.nom}&background=2E86AB&color=fff`;
  }

  seDeconnecter(): void {
    this.authService.logout();
  }
}