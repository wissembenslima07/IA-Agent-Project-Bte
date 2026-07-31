import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Dossier {
  id: number;
  clientNom: string;
  clientPrenom: string;
  conseillerEmail: string;
  statut: 'EN_COURS' | 'EN_ANALYSE' | 'VALIDE' | 'REFUSE';
  dateCreation: string;
}

export interface HistoriqueAction {
  id: number;
  action: string;
  details: string;
  utilisateurEmail: string;
  dateAction: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class DossierService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/dossiers`;

  creer(clientNom: string, clientPrenom: string): Observable<Dossier> {
    return this.http.post<Dossier>(this.apiUrl, { clientNom, clientPrenom });
  }

  lister(page = 0, size = 10): Observable<PageResponse<Dossier>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<Dossier>>(this.apiUrl, { params });
  }

  consulter(id: number): Observable<Dossier> {
    return this.http.get<Dossier>(`${this.apiUrl}/${id}`);
  }

  changerStatut(id: number, statut: string): Observable<Dossier> {
    return this.http.patch<Dossier>(`${this.apiUrl}/${id}/statut`, { statut });
  }

  historique(id: number): Observable<HistoriqueAction[]> {
    return this.http.get<HistoriqueAction[]>(`${this.apiUrl}/${id}/historique`);
  }
}