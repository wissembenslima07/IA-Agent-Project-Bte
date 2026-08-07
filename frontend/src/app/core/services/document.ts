import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Document {
  id: number;
  typeDocument: string;
  nomFichier: string;
  tailleBytes: number;
  mimeType: string;
  dateUpload: string;
  uploadePar: string;
  statut: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  uploadDocument(dossierId: number, typeDocument: string, file: File): Observable<Document> {
    const formData = new FormData();
    formData.append('typeDocument', typeDocument);
    formData.append('file', file);

    return this.http.post<Document>(
      `${this.apiUrl}/dossiers/${dossierId}/documents/upload`,
      formData
    );
  }

  uploadMultiple(dossierId: number, files: File[]): Observable<Document[]> {
    const formData = new FormData();
    for (const file of files) {
      formData.append('files', file);
    }

    return this.http.post<Document[]>(
      `${this.apiUrl}/dossiers/${dossierId}/documents/upload-multiple`,
      formData
    );
  }

  listerDocuments(dossierId: number): Observable<Document[]> {
    return this.http.get<Document[]>(
      `${this.apiUrl}/dossiers/${dossierId}/documents`
    );
  }

  consulterDocument(dossierId: number, documentId: number): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/dossiers/${dossierId}/documents/${documentId}/download`,
      { responseType: 'blob' }
    );
  }

  supprimerDocument(dossierId: number, documentId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/dossiers/${dossierId}/documents/${documentId}`
    );
  }
}