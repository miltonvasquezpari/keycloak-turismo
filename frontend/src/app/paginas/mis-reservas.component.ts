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
        <strong>#{{r.id}}</strong> {{r.tourCodigo}} · <em>{{r.estado}}</em> · {{r.personas}} pers.
        <button (click)="ver(r.id)">Ver</button>
        <button *ngIf="r.estado === 'PENDIENTE'" (click)="cancelar(r.id)">Cancelar</button>
      </li>
    </ul>
    <pre *ngIf="detalle()">{{ detalle() | json }}</pre>
    <p *ngIf="error()" class="err">{{error()}}</p>
  `,
})
export class MisReservasComponent {
  private api = inject(ApiService);
  reservas = signal<Reserva[]>([]);
  detalle = signal<Reserva | null>(null);
  error = signal<string | null>(null);
  constructor() { this.cargar(); }

  cargar() {
    this.api.misReservas().subscribe({
      next: (r) => this.reservas.set(r),
      error: () => this.error.set('No se pudo cargar'),
    });
  }
  ver(id: number) {
    this.api.detalleReserva(id).subscribe({ next: (d) => this.detalle.set(d) });
  }
  cancelar(id: number) {
    this.api.cancelarReserva(id).subscribe({ next: () => this.cargar() });
  }
}
