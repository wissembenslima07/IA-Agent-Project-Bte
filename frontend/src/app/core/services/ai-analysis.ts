import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FichePayeInput {
  mois: string;
  texteExtrait: string;
}

export interface AiAnalysisRequest {
  dossierId: number;
  clientName: string;
  clientEmail: string;
  fiches: FichePayeInput[];
}

export interface AnalyseCompleteFiches {
  verdictId: number;
  dossierId: number;
  nombreFichesAnalysees: number;
  periode: string;
  timestamp: string;
  scoreRisque: number;
  verdict: 'VALIDE' | 'RISQUE' | 'REJETE';
  confiance: number;
  pointsForts: string[];
  risquesMajeurs: string[];
  tendancesObservees: string[];
  montantMaxRecommande: number | null;
  dureeMaxRecommandee: string | null;
  conditionsSpeciales: string[];
  tauxInteretRecommande: string | null;
  justification: string;
  resumeCourt: string;
}

export interface AiAnalysisResponse {
  success: boolean;
  verdict_id: number;
  dossier_id: number;
  verdict: 'VALIDE' | 'RISQUE' | 'REJETE';
  score_risque: number;
  confiance: number;
  montant_max_recommande: number | null;
  duree_max_recommandee: string | null;
  justification: string;
  data: AnalyseCompleteFiches;
}

@Injectable({ providedIn: 'root' })
export class AiAnalysisService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  lancerAnalyse(request: AiAnalysisRequest): Observable<AiAnalysisResponse> {
    return this.http.post<AiAnalysisResponse>(
      `${this.apiUrl}/analyse/evaluate-multiple-fiches`,
      request
    );
  }
}
