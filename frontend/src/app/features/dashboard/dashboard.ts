import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar';

export type DossierStatut = 'En cours' | 'Validé' | 'Refusé';

export interface Dossier {
  id: string;
  clientNom: string;
  initiales: string;
  statut: DossierStatut;
  dateCreation: string;
}

export interface AlerteRisque {
  type: 'warning' | 'info';
  titre: string;
  description: string;
}

export interface StatCard {
  icone: string;
  iconeBgClass: string;
  iconeColorClass: string;
  label: string;
  valeur: string;
  suffixe?: string;
  badge?: string;
  badgeClass?: string;
  tendance?: string;
  tendanceClass?: string;
  sousTexte: string;
}



export interface NavLink {
  icon: string;
  label: string;
  route: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink,  SidebarComponent],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardComponent implements OnInit {
  private router = inject(Router);
  private authService = inject(AuthService);

 
  termeRecherche = '';

  statCards: StatCard[] = [
    {
      icone: 'pending_actions',
      iconeBgClass: 'bg-blue-50',
      iconeColorClass: 'text-secondary',
      label: 'Dossiers en cours',
      valeur: '24',
      suffixe: 'unités',
      badge: 'TEMPS RÉEL',
      badgeClass: 'text-on-surface-variant bg-surface-container',
      sousTexte: "8 en attente d'IA"
    },
    {
      icone: 'task_alt',
      iconeBgClass: 'bg-green-50',
      iconeColorClass: 'text-green-600',
      label: 'Dossiers validés ce mois',
      valeur: '12',
      tendance: '15%',
      tendanceClass: 'text-green-600',
      sousTexte: 'vs mois dernier'
    },
    {
      icone: 'analytics',
      iconeBgClass: 'bg-orange-50',
      iconeColorClass: 'text-orange-600',
      label: 'Score moyen de risque',
      valeur: '72',
      suffixe: '/100',
      badge: 'MODÉRÉ',
      badgeClass: 'text-orange-600 bg-orange-50',
      sousTexte: 'Niveau de risque global'
    }
  ];

  dossiers: Dossier[] = [
    { id: '#45920-FR', clientNom: 'SOCIETE HEXAGON', initiales: 'SH', statut: 'En cours', dateCreation: '12/10/2023' },
    { id: '#45921-TN', clientNom: 'Ahmed Mansour', initiales: 'AM', statut: 'Validé', dateCreation: '11/10/2023' },
    { id: '#45922-TN', clientNom: "SARL L'OLIVIER", initiales: 'SL', statut: 'Refusé', dateCreation: '11/10/2023' },
    { id: '#45925-TN', clientNom: 'Tunis Prime Ltd', initiales: 'TP', statut: 'En cours', dateCreation: '10/10/2023' }
  ];

  totalDossiers = 24;

  alertes: AlerteRisque[] = [
    {
      type: 'warning',
      titre: "Incohérence détectée : SARL L'OLIVIER",
      description: "Le ratio d'endettement dépasse les limites autorisées par la politique BTE 2023."
    },
    {
      type: 'info',
      titre: 'Information manquante : SOCIETE HEXAGON',
      description: "Le bilan certifié de l'exercice 2022 est requis pour finaliser l'analyse."
    }
  ];

  precisionScoreIA = 98.2;

  ngOnInit(): void {}

  voirDossier(dossier: Dossier): void {
    this.router.navigate(['/dossiers', dossier.id]);
  }

  nouveauDossier(): void {
    this.router.navigate(['/dossiers/nouveau']);
  }

  lancerAnalyseGlobale(): void {
    console.log('Lancement de l\'analyse IA globale');
  }

  filtrerDossiers(): void {
    this.router.navigate(['/dossiers']);
  }

  exporterDossiers(): void {
    console.log('Exporter les dossiers');
  }

  seDeconnecter(): void {
    this.authService.logout();
  }

  pagePrecedente(): void {}
  pageSuivante(): void {}

  statutBadgeClass(statut: DossierStatut): string {
    switch (statut) {
      case 'En cours': return 'bg-[#E1F1F8] text-[#2E86AB]';
      case 'Validé': return 'bg-[#E9F7EF] text-[#28A745]';
      case 'Refusé': return 'bg-[#FDECEA] text-[#DC3545]';
    }
  }

  statutDotClass(statut: DossierStatut): string {
    switch (statut) {
      case 'En cours': return 'bg-[#2E86AB]';
      case 'Validé': return 'bg-[#28A745]';
      case 'Refusé': return 'bg-[#DC3545]';
    }
  }
}