import { Component, Input, Output, EventEmitter, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentService, Document } from '../../../core/services/document';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './file-upload.html',
  styleUrl: './file-upload.scss'
})
export class FileUploadComponent {
  @Input() dossierId!: number;
  @Output() uploadComplete = new EventEmitter<Document[]>();

  private documentService = inject(DocumentService);

  isDragOver = signal(false);
  isUploading = signal(false);
  uploadProgress = signal(0);
  errorMessage = signal<string | null>(null);

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(true);
  }

  onDragLeave(): void {
    this.isDragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(false);
    const files = event.dataTransfer?.files;
    if (files) {
      this.handleFiles(Array.from(files));
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (files) {
      this.handleFiles(Array.from(files));
      input.value = '';
    }
  }

  private handleFiles(files: File[]): void {
    if (files.length === 0) return;

    this.isUploading.set(true);
    this.errorMessage.set(null);

    this.documentService.uploadMultiple(this.dossierId, files).subscribe({
      next: (documents) => {
        this.isUploading.set(false);
        this.uploadProgress.set(0);
        this.uploadComplete.emit(documents);
      },
      error: (err) => {
        this.isUploading.set(false);
        this.errorMessage.set(err.error?.error || 'Erreur lors de l\'upload');
      }
    });
  }
}