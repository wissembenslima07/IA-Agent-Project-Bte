import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DocumentForAi {
  typeDocument: string;
  contenu: string;
}

export interface AiAnalysisRequest {
  dossierId: number;
  clientNom: string;
  clientPrenom: string;
  documents: DocumentForAi[];
  contexteSupplementaire?: string;
}

export interface AiAnalysisResponse {
  dossierId: number;
  score_risque: number;
  verdict: 'VALIDE' | 'RISQUE' | 'REJETE';
  justification: string;
  recommandations: string[];
}

@Injectable({ providedIn: 'root' })
export class AiAnalysisService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  lancerAnalyse(request: AiAnalysisRequest): Observable<AiAnalysisResponse> {
    return this.http.post<AiAnalysisResponse>(
      `${this.apiUrl}/analyse`,
      request
    );
  }
}