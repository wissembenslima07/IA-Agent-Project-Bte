import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DossierService, Dossier, HistoriqueAction } from '../../../core/services/dossier.service';
import { DocumentService, Document } from '../../../core/services/document';
import { AiAnalysisService, AiAnalysisRequest, AnalyseCompleteFiches } from '../../../core/services/ai-analysis';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { FileUploadComponent } from '../../../shared/components/file-upload/file-upload';
import { DonneesExtraites, OCRService } from '../../../core/services/ocr';

@Component({
  selector: 'app-dossier-detail',
  standalone: true,
  imports: [CommonModule, SidebarComponent, FileUploadComponent],
  templateUrl: './dossier-detail.html',
  styleUrl: './dossier-detail.scss'
})
export class DossierDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dossierService = inject(DossierService);
  private readonly documentService = inject(DocumentService);
  private readonly aiAnalysisService = inject(AiAnalysisService);

  private dossierId!: number;

  // --- Données chargées depuis l'API ---
  readonly dossier = signal<Dossier | null>(null);
  readonly historique = signal<HistoriqueAction[]>([]);
  readonly documents = signal<Document[]>([]);

  // --- Etats UI ---
  readonly isLoading = signal(false);
  readonly isLoadingHistorique = signal(false);
  readonly isLoadingDocuments = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isChangingStatus = signal(false);
  readonly isUpdatingStatus = signal(false);

  // --- Analyse IA ---
  readonly aiAnalysis = signal<AnalyseCompleteFiches | null>(null);
  readonly isAnalyzing = signal(false);
  readonly analysisError = signal<string | null>(null);
  readonly MIN_FICHES_ANALYSE = 2;

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
    this.loadDocuments();
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
        this.historique.set(actions);
        this.isLoadingHistorique.set(false);
      },
      error: (err) => {
        console.error("Erreur lors du chargement de l'historique", err);
        this.isLoadingHistorique.set(false);
      }
    });
  }

  loadDocuments(): void {
    this.isLoadingDocuments.set(true);

    this.documentService.listerDocuments(this.dossierId).subscribe({
      next: (docs) => {
        this.documents.set(docs);
        this.isLoadingDocuments.set(false);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des documents', err);
        this.isLoadingDocuments.set(false);
      }
    });
  }

  onUploadComplete(newDocuments: Document[]): void {
    this.documents.update(docs => [...docs, ...newDocuments]);
    newDocuments.forEach(doc => this.lancerExtractionOCR(doc));
  }

  consulterDocument(doc: Document): void {
    const dossierId = this.dossier()?.id;
    if (!dossierId) return;

    this.documentService.consulterDocument(dossierId, doc.id).subscribe({
      next: (blob) => {
        const fileUrl = window.URL.createObjectURL(blob);
        window.open(fileUrl, '_blank', 'noopener,noreferrer');
        setTimeout(() => window.URL.revokeObjectURL(fileUrl), 30_000);
      },
      error: (err) => {
        console.error('Erreur lors de la consultation du document', err);
        this.errorMessage.set('Impossible de consulter ce document.');
      }
    });
  }

  supprimerDocument(docId: number): void {
    const dossierId = this.dossier()?.id;
    if (!dossierId) return;

    this.documentService.supprimerDocument(dossierId, docId).subscribe({
      next: () => {
        this.documents.update(docs => docs.filter(d => d.id !== docId));
      },
      error: (err) => {
        console.error('Erreur lors de la suppression du document', err);
        this.errorMessage.set('Impossible de supprimer ce document.');
      }
    });
  }

  isEntreeCreation(index: number): boolean {
    return index === 0;
  }

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
        this.loadHistorique();
      },
      error: (err) => {
        console.error('Erreur lors du changement de statut', err);
        this.isUpdatingStatus.set(false);
        this.errorMessage.set('Impossible de mettre à jour le statut.');
      }
    });
  }

  // --- Analyse IA ---

  lancerAnalyseIA(): void {
    const d = this.dossier();
    if (!d) return;

    const fichesDisponibles = Array.from(this.donneesExtraites().values());
    if (fichesDisponibles.length < this.MIN_FICHES_ANALYSE) {
      this.analysisError.set(
        `Il faut au moins ${this.MIN_FICHES_ANALYSE} documents avec extraction OCR réussie pour lancer l'analyse (actuellement ${fichesDisponibles.length}).`
      );
      return;
    }

    this.isAnalyzing.set(true);
    this.analysisError.set(null);

    const request: AiAnalysisRequest = {
      dossierId: d.id,
      clientName: `${d.clientPrenom} ${d.clientNom}`.trim(),
      clientEmail: '',
      fiches: fichesDisponibles.map(donnees => ({
        mois: this.moisPourDocument(donnees.documentId),
        texteExtrait: donnees.textComplet
      }))
    };

    this.aiAnalysisService.lancerAnalyse(request).subscribe({
      next: (response) => {
        this.aiAnalysis.set(response.data);
        this.isAnalyzing.set(false);
        this.loadHistorique();
      },
      error: (err) => {
        console.error('Erreur analyse IA', err);
        this.analysisError.set('Erreur lors de l\'analyse IA: ' + (err.error?.error || err.error?.detail || err.message));
        this.isAnalyzing.set(false);
      }
    });
  }

  private static readonly MOIS_REGEX = new RegExp(
    '(janvier|f[eé]vrier|mars|avril|mai|juin|juillet|ao[uû]t|septembre|octobre|novembre|d[eé]cembre)[_ -]+(\\d{4})',
    'i'
  );

  // Le nom de fichier contient souvent la vraie période de la fiche (ex: "..._Juin_2026_..."),
  // plus fiable que la date d'upload si plusieurs fiches sont déposées le même jour.
  private moisPourDocument(documentId: number): string {
    const doc = this.documents().find(d => d.id === documentId);
    if (!doc) return `Document ${documentId}`;

    const match = doc.nomFichier.match(DossierDetailComponent.MOIS_REGEX);
    if (match) {
      const mois = match[1].charAt(0).toUpperCase() + match[1].slice(1).toLowerCase();
      return `${mois} ${match[2]}`;
    }

    return new Date(doc.dateUpload).toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }

  verdictBadgeClass(verdict: string): string {
    switch (verdict) {
      case 'VALIDE': return 'bg-green-100 text-green-800';
      case 'RISQUE': return 'bg-orange-100 text-orange-800';
      case 'REJETE': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  retourListe(): void {
    this.router.navigate(['/dossiers']);
  }

  private ocrService = inject(OCRService);

readonly donneesExtraites = signal<Map<number, DonneesExtraites>>(new Map());
readonly isExtractingOCR = signal(false);
readonly ocrError = signal<string | null>(null);

lancerExtractionOCR(document: Document): void {
  const dossierId = this.dossier()?.id;
  if (!dossierId || !document) return;

  this.isExtractingOCR.set(true);
  this.ocrError.set(null);

  this.documentService.consulterDocument(dossierId, document.id).subscribe({
    next: (blob) => {
      const file = new File([blob], document.nomFichier, { type: document.mimeType });

      this.ocrService.extraireDocument(file, document.id).subscribe({
        next: (donnees) => {
          this.donneesExtraites.update(map => new Map(map).set(document.id, donnees));
          this.isExtractingOCR.set(false);
        },
        error: (err) => {
          console.error('Erreur OCR', err);
          this.ocrError.set('Erreur extraction: ' + (err.error?.detail || err.message));
          this.isExtractingOCR.set(false);
        }
      });
    },
    error: (err) => {
      console.error('Erreur téléchargement document pour OCR', err);
      this.ocrError.set('Impossible de récupérer le document pour extraction OCR.');
      this.isExtractingOCR.set(false);
    }
  });
}
}