import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, Reserva } from '../core/api.service';

@Component({
  selector: 'app-mis-reservas',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Mis reservas</h2>
    <ul class="lista">
      <li *ngFor="let r of reservas()">
        <strong>#{{r.id}}</strong> {{r.tourNombre}} · {{r.fechaTour}} · <em>{{r.estado}}</em>
        · {{r.personas}} pers. · S/ {{r.montoTotal}}
        <button (click)="ver(r.id)">Ver</button>
        <button *ngIf="r.estado === 'PENDIENTE'" (click)="cancelar(r.id)">Cancelar</button>
      </li>
    </ul>
    <p *ngIf="error()" class="err">{{error()}}</p>
  `,
})
export class MisReservasComponent {
  private api = inject(ApiService);
  reservas = signal<Reserva[]>([]);
  error = signal<string | null>(null);
  constructor() { this.cargar(); }

  cargar() {
    this.api.misReservas().subscribe({
      next: (r) => this.reservas.set(r),
      error: () => this.error.set('No se pudo cargar'),
    });
  }
  ver(id: string) {
    this.api.detalleReserva(id).subscribe({
      next: (d) => alert(`Reserva ${d.id}\nTour: ${d.tourNombre}\nFecha: ${d.fechaTour}\nEstado: ${d.estado}\nPersonas: ${d.personas}\nTotal: S/ ${d.montoTotal}`),
      error: () => this.error.set('No se pudo cargar el detalle'),
    });
  }
  cancelar(id: string) {
    this.api.cancelarReserva(id).subscribe({ next: () => this.cargar() });
  }
}
