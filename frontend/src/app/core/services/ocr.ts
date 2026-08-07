import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DonneesExtraites {
  documentId: number;
  textComplet: string;
  methode: string;
  confidenceMoyenne: number;
  nombrePages?: number;
  nombreElements?: number;
  dateExtraction: string;
}

@Injectable({ providedIn: 'root' })
export class OCRService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  extraireDocument(file: File, documentId: number): Observable<DonneesExtraites> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentId', documentId.toString());

    return this.http.post<DonneesExtraites>(
      `${this.apiUrl}/ocr/extract`,
      formData
    );
  }
}