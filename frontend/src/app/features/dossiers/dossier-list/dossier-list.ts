import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DossierService, Dossier } from '../../../core/services/dossier.service';

type StatutFilter = 'TOUS' | Dossier['statut'];
type SortOption = 'date' | 'nom';

@Component({
  selector: 'app-dossier-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dossier-list.html'
})
export class DossierListComponent implements OnInit {
  private readonly dossierService = inject(DossierService);
  private readonly router = inject(Router);

  // --- Données chargées depuis l'API ---
  readonly dossiers = signal<Dossier[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0); // 0-based, convention Spring Data Pageable
  readonly pageSize = signal(10);

  // --- Etats UI ---
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  // --- Filtres/tri côté client ---
  // ATTENTION : DossierService.lister() ne prend pas de paramètres de filtre/tri.
  // Ces contrôles ne s'appliquent donc qu'à la page actuellement chargée en mémoire,
  // pas à l'ensemble des dossiers du back. A étendre côté API si besoin d'un vrai
  // filtre global.
  readonly searchTerm = signal('');
  readonly statutFilter = signal<StatutFilter>('TOUS');
  readonly sortBy = signal<SortOption>('date');

  readonly statutLabels: Record<Dossier['statut'], string> = {
    EN_COURS: 'En cours',
    EN_ANALYSE: 'En analyse',
    VALIDE: 'Validé',
    REFUSE: 'Refusé'
  };

  readonly statutBadgeClasses: Record<Dossier['statut'], string> = {
    EN_COURS: 'bg-blue-50 text-blue-700 border-blue-200',
    EN_ANALYSE: 'bg-orange-50 text-orange-700 border-orange-200',
    VALIDE: 'bg-green-50 text-green-700 border-green-200',
    REFUSE: 'bg-red-50 text-red-700 border-red-200'
  };

  readonly statutDotClasses: Record<Dossier['statut'], string> = {
    EN_COURS: 'bg-blue-500',
    EN_ANALYSE: 'bg-orange-500',
    VALIDE: 'bg-green-500',
    REFUSE: 'bg-red-500'
  };

  readonly filteredDossiers = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const statut = this.statutFilter();
    const sort = this.sortBy();

    let list = this.dossiers().filter(d => {
      const matchesStatut = statut === 'TOUS' || d.statut === statut;
      const haystack = `${d.clientPrenom} ${d.clientNom} ${d.id}`.toLowerCase();
      const matchesSearch = !term || haystack.includes(term);
      return matchesStatut && matchesSearch;
    });

    return [...list].sort((a, b) => {
      if (sort === 'nom') {
        return `${a.clientNom} ${a.clientPrenom}`.localeCompare(`${b.clientNom} ${b.clientPrenom}`);
      }
      return new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime();
    });
  });

  // Stats dérivées de la page chargée uniquement (voir signalement ci-dessus :
  // "Risque élevé" et "Délai moyen" ne sont pas calculables sans endpoint dédié).
  readonly stats = computed(() => {
    const list = this.dossiers();
    return {
      enCoursOuAnalyse: list.filter(d => d.statut === 'EN_COURS' || d.statut === 'EN_ANALYSE').length,
      valides: list.filter(d => d.statut === 'VALIDE').length
    };
  });

  readonly pageNumbers = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i));

  ngOnInit(): void {
    this.loadDossiers();
  }

  loadDossiers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.dossierService.lister(this.currentPage(), this.pageSize()).subscribe({
      next: (page) => {
        this.dossiers.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des dossiers', err);
        this.errorMessage.set('Impossible de charger les dossiers. Veuillez réessayer.');
        this.isLoading.set(false);
      }
    });
  }

  displayName(d: Pick<Dossier, 'clientPrenom' | 'clientNom'>): string {
    return `${d.clientPrenom} ${d.clientNom}`.trim();
  }

  initials(d: Pick<Dossier, 'clientPrenom' | 'clientNom'>): string {
    return `${d.clientPrenom.charAt(0)}${d.clientNom.charAt(0)}`.toUpperCase();
  }

  // Dossier n'expose pas de nom de conseiller, seulement l'email.
  // Placeholder en attendant un champ dédié côté back.
  conseillerDisplay(d: Pick<Dossier, 'conseillerEmail'>): string {
    return d.conseillerEmail?.split('@')[0] ?? '—';
  }

  onStatutFilterChange(value: string): void {
    this.statutFilter.set(value as StatutFilter);
  }

  onSortChange(value: string): void {
    this.sortBy.set(value as SortOption);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.currentPage()) return;
    this.currentPage.set(page);
    this.loadDossiers();
  }

  voirDossier(id: number): void {
    this.router.navigate(['/dossiers', id]);
  }

  nouveauDossier(): void {
    this.router.navigate(['/dossiers/nouveau']);
  }
}