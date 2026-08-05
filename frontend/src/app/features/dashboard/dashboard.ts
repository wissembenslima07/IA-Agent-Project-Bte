import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar';
import { DossierService, Dossier as DossierBackend } from '../../core/services/dossier.service';

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

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardComponent implements OnInit {
  private router = inject(Router);
  private dossierService = inject(DossierService);

  termeRecherche = '';
  isLoading = signal(true);

  dossiers = signal<DossierBackend[]>([]);
  totalDossiers = signal(0);

  ngOnInit(): void {
    this.chargerDossiers();
  }

  chargerDossiers(): void {
    this.isLoading.set(true);
    // On recupere une page suffisamment large pour calculer des stats correctes
    // (a terme, un endpoint /api/dossiers/stats dedie serait plus propre - voir note)
    this.dossierService.lister(0, 100).subscribe({
      next: (res) => {
        this.dossiers.set(res.content);
        this.totalDossiers.set(res.totalElements);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  // Les 4 derniers dossiers, tries par date de creation decroissante
  dernierDossiers = computed(() =>
    [...this.dossiers()]
      .sort((a, b) => new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime())
      .slice(0, 4)
  );

  nombreDossiersEnCours = computed(() =>
    this.dossiers().filter(d => d.statut === 'EN_COURS' || d.statut === 'EN_ANALYSE').length
  );

  nombreDossiersValidesCeMois = computed(() => {
    const maintenant = new Date();
    return this.dossiers().filter(d => {
      if (d.statut !== 'VALIDE') return false;
      const date = new Date(d.dateCreation);
      return date.getMonth() === maintenant.getMonth() && date.getFullYear() === maintenant.getFullYear();
    }).length;
  });

  statCards = computed<StatCard[]>(() => [
    {
      icone: 'pending_actions',
      iconeBgClass: 'bg-blue-50',
      iconeColorClass: 'text-secondary',
      label: 'Dossiers en cours',
      valeur: String(this.nombreDossiersEnCours()),
      suffixe: 'unités',
      badge: 'TEMPS RÉEL',
      badgeClass: 'text-on-surface-variant bg-surface-container',
      sousTexte: `${this.totalDossiers()} dossiers au total`
    },
    {
      icone: 'task_alt',
      iconeBgClass: 'bg-green-50',
      iconeColorClass: 'text-green-600',
      label: 'Dossiers validés ce mois',
      valeur: String(this.nombreDossiersValidesCeMois()),
      sousTexte: 'Sur le mois en cours'
    },
    {
      icone: 'analytics',
      iconeBgClass: 'bg-orange-50',
      iconeColorClass: 'text-orange-600',
      label: 'Score moyen de risque',
      valeur: '—',
      suffixe: '/100',
      badge: 'BIENTÔT',
      badgeClass: 'text-on-surface-variant bg-surface-container',
      sousTexte: "Disponible apres integration de l'agent IA"
    }
  ]);

  // Pas encore de vraies alertes IA (Rule Engine = sprint ulterieur) :
  // tableau vide plutot que des donnees inventees
  alertes: AlerteRisque[] = [];

  precisionScoreIA: number | null = null;

  voirDossier(dossier: DossierBackend): void {
    this.router.navigate(['/dossiers', dossier.id]);
  }

  nouveauDossier(): void {
    this.router.navigate(['/dossiers/nouveau']);
  }

  lancerAnalyseGlobale(): void {
    console.log("Fonctionnalite disponible apres integration de l'agent IA");
  }

  filtrerDossiers(): void {
    this.router.navigate(['/dossiers']);
  }

  exporterDossiers(): void {
    console.log('Export a implementer');
  }

  statutBadgeClass(statut: string): string {
    switch (statut) {
      case 'EN_COURS': return 'bg-[#E1F1F8] text-[#2E86AB]';
      case 'EN_ANALYSE': return 'bg-[#FFF3E0] text-[#E67E22]';
      case 'VALIDE': return 'bg-[#E9F7EF] text-[#28A745]';
      case 'REFUSE': return 'bg-[#FDECEA] text-[#DC3545]';
      default: return 'bg-surface-container text-on-surface-variant';
    }
  }

  statutDotClass(statut: string): string {
    switch (statut) {
      case 'EN_COURS': return 'bg-[#2E86AB]';
      case 'EN_ANALYSE': return 'bg-[#E67E22]';
      case 'VALIDE': return 'bg-[#28A745]';
      case 'REFUSE': return 'bg-[#DC3545]';
      default: return 'bg-on-surface-variant';
    }
  }

  statutLabel(statut: string): string {
    switch (statut) {
      case 'EN_COURS': return 'En cours';
      case 'EN_ANALYSE': return 'En analyse';
      case 'VALIDE': return 'Validé';
      case 'REFUSE': return 'Refusé';
      default: return statut;
    }
  }

  initiales(dossier: DossierBackend): string {
    return `${dossier.clientPrenom.charAt(0)}${dossier.clientNom.charAt(0)}`.toUpperCase();
  }
}