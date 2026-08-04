import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DossierService } from '../../../core/services/dossier.service';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-dossier-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SidebarComponent],
  templateUrl: './dossier-create.html',
  styleUrl: './dossier-create.scss'
})
export class DossierCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dossierService = inject(DossierService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  // NB : "phone" et "email" sont conservés dans le formulaire pour respecter
  // le design Stitch, mais DossierService.creer() ne les accepte pas.
  // Ils ne sont donc PAS envoyés à l'API tant que le back n'expose pas ces champs.
  readonly form = this.fb.nonNullable.group({
    firstname: ['', [Validators.required, Validators.minLength(2)]],
    lastname: ['', [Validators.required, Validators.minLength(2)]],
    phone: [''],
    email: ['', [Validators.email]]
  });

  get f() {
    return this.form.controls;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { firstname, lastname } = this.form.getRawValue();

    this.dossierService.creer(lastname, firstname).subscribe({
      next: (dossier) => {
        this.isSubmitting.set(false);
        this.router.navigate(['/dossiers', dossier.id]);
      },
      error: (err) => {
        console.error('Erreur lors de la création du dossier', err);
        this.isSubmitting.set(false);
        this.errorMessage.set(
          "Une erreur est survenue lors de la création du dossier. Veuillez réessayer."
        );
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/dossiers']);
  }
}