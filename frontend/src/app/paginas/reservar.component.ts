import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';

@Component({
  selector: 'app-reservar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h2>Reservar tour {{tourId}}</h2>
    <form (ngSubmit)="enviar()" #f="ngForm">
      <label>Personas <input name="personas" type="number" min="1" [(ngModel)]="personas" required></label>
      <label>Fecha <input name="fecha" type="date" [(ngModel)]="fecha" required></label>
      <button type="submit" [disabled]="f.invalid || enviando()">Reservar</button>
    </form>
    <p *ngIf="error()" class="err">{{error()}}</p>
  `,
})
export class ReservarComponent {
  private api = inject(ApiService);
  private ruta = inject(ActivatedRoute);
  private router = inject(Router);
  tourId = this.ruta.snapshot.paramMap.get('tourId')!;
  personas = 1;
  fecha = '';
  enviando = signal(false);
  error = signal<string | null>(null);

  enviar() {
    this.enviando.set(true);
    this.api.crearReserva({ tourCodigo: this.tourId, personas: this.personas, fecha: this.fecha }).subscribe({
      next: () => this.router.navigate(['/mis-reservas']),
      error: (e) => { this.error.set(e.error?.mensaje ?? 'Error al crear la reserva'); this.enviando.set(false); },
    });
  }
}
