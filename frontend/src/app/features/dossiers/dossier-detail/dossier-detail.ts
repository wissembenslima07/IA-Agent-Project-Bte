import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DossierService, Dossier, HistoriqueAction } from '../../../core/services/dossier.service';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-dossier-detail',
  standalone: true,
  imports: [CommonModule,SidebarComponent],
  templateUrl: './dossier-detail.html',
  styleUrl: './dossier-detail.scss'
})
export class DossierDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dossierService = inject(DossierService);

  private dossierId!: number;

  // --- Données chargées depuis l'API ---
  readonly dossier = signal<Dossier | null>(null);
  readonly historique = signal<HistoriqueAction[]>([]);

  // --- Etats UI ---
  readonly isLoading = signal(false);
  readonly isLoadingHistorique = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isChangingStatus = signal(false);
  readonly isUpdatingStatus = signal(false);

  // Fonctionnalités non implémentées côté back : pas d'appel API, juste un
  // message local pour ne pas laisser le bouton silencieux.
  readonly analyseIaMessage = signal<string | null>(null);
  readonly documentUploadMessage = signal<string | null>(null);


private readonly statutLabels: Record<string, string> = {
  EN_COURS: 'En cours',
  EN_ANALYSE: 'En analyse',
  VALIDE: 'Validé',
  REFUSE: 'Refusé'
};

getStatutLabel(statut: string): string {
  return this.statutLabels[statut] ?? statut;
}
  readonly statutBadgeClasses: Record<Dossier['statut'], string> = {
    EN_COURS: 'bg-blue-50 text-blue-700',
    EN_ANALYSE: 'bg-orange-50 text-orange-700',
    VALIDE: 'bg-green-50 text-green-700',
    REFUSE: 'bg-error-container text-error'
  };

  readonly displayName = computed(() => {
    const d = this.dossier();
    return d ? `${d.clientPrenom} ${d.clientNom}`.trim() : '';
  });

  readonly initials = computed(() => {
    const d = this.dossier();
    if (!d) return '';
    return `${d.clientPrenom.charAt(0)}${d.clientNom.charAt(0)}`.toUpperCase();
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam || isNaN(Number(idParam))) {
      this.errorMessage.set('Identifiant de dossier invalide.');
      return;
    }
    this.dossierId = Number(idParam);
    this.loadDossier();
    this.loadHistorique();
  }

  loadDossier(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.dossierService.consulter(this.dossierId).subscribe({
      next: (dossier) => {
        this.dossier.set(dossier);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur lors du chargement du dossier', err);
        this.errorMessage.set('Impossible de charger ce dossier.');
        this.isLoading.set(false);
      }
    });
  }

  loadHistorique(): void {
    this.isLoadingHistorique.set(true);

    this.dossierService.historique(this.dossierId).subscribe({
      next: (actions) => {
        // Hypothèse : l'API renvoie les actions du plus ancien au plus récent.
        // Si ce n'est pas le cas, remplacer par actions.slice().reverse().
        this.historique.set(actions);
        this.isLoadingHistorique.set(false);
      },
      error: (err) => {
        console.error("Erreur lors du chargement de l'historique", err);
        this.isLoadingHistorique.set(false);
      }
    });
  }

  // Icône de timeline basée sur la position (1ère entrée = création),
  // faute de connaître les valeurs exactes de HistoriqueAction.action.
  isEntreeCreation(index: number): boolean {
    return index === 0;
  }

  // Heuristique de style : rouge si le détail mentionne un refus.
  isDetailAlerte(details: string): boolean {
    return details?.toUpperCase().includes('REFUSE') ?? false;
  }

  conseillerDisplay(d: Pick<Dossier, 'conseillerEmail'>): string {
    return d.conseillerEmail?.split('@')[0] ?? '—';
  }

  changerStatut(nouveauStatut: Dossier['statut']): void {
    const d = this.dossier();
    if (!d || this.isUpdatingStatus()) return;

    this.isUpdatingStatus.set(true);
    this.dossierService.changerStatut(d.id, nouveauStatut).subscribe({
      next: (dossierMisAJour) => {
        this.dossier.set(dossierMisAJour);
        this.isChangingStatus.set(false);
        this.isUpdatingStatus.set(false);
        // Le backend est censé journaliser ce changement : on recharge l'historique.
        this.loadHistorique();
      },
      error: (err) => {
        console.error('Erreur lors du changement de statut', err);
        this.isUpdatingStatus.set(false);
        this.errorMessage.set('Impossible de mettre à jour le statut.');
      }
    });
  }

  // Pas d'endpoint d'analyse IA disponible : on informe l'utilisateur
  // sans appeler d'API inventée.
  lancerAnalyse(): void {
    this.analyseIaMessage.set("L'analyse IA n'est pas encore disponible sur cette version.");
  }

  // Pas d'endpoint d'upload disponible : idem.
  ajouterDocument(): void {
    this.documentUploadMessage.set("L'ajout de documents n'est pas encore disponible sur cette version.");
  }

  retourListe(): void {
    this.router.navigate(['/dossiers']);
  }
}