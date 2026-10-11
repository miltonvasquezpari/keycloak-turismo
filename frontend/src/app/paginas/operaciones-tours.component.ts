import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, TourDetalle } from '../core/api.service';

@Component({
  selector: 'app-operaciones-tours',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h2>Operaciones · Tours</h2>
    <form (ngSubmit)="crear()" class="form">
      <input name="codigo" placeholder="Código" [(ngModel)]="nuevo.codigo" required>
      <input name="nombre" placeholder="Nombre" [(ngModel)]="nuevo.nombre" required>
      <input name="destino" placeholder="Destino" [(ngModel)]="nuevo.destino">
      <input name="precio" type="number" placeholder="Precio" [(ngModel)]="nuevo.precio">
      <button type="submit">Crear</button>
    </form>
    <ul class="lista">
      <li *ngFor="let t of tours()">
        <strong>{{t.codigo}}</strong> {{t.nombre}} — S/ {{t.precio}}
        <button (click)="editar(t)">Editar precio</button>
      </li>
    </ul>
    <p *ngIf="error()" class="err">{{error()}}</p>
  `,
  styles: [`.form{display:flex;gap:.5rem;margin-bottom:1rem;flex-wrap:wrap}`],
})
export class OperacionesToursComponent {
  private api = inject(ApiService);
  tours = signal<TourDetalle[]>([]);
  nuevo: Partial<TourDetalle> = {};
  error = signal<string | null>(null);
  constructor() { this.cargar(); }

  cargar() {
    this.api.listarTours().subscribe({ next: (t) => this.tours.set(t), error: () => this.error.set('No se pudo cargar') });
  }
  crear() {
    this.api.crearTour(this.nuevo).subscribe({ next: () => { this.nuevo = {}; this.cargar(); }, error: (e) => this.error.set(e.error?.mensaje ?? 'Error') });
  }
  editar(t: TourDetalle) {
    const v = prompt('Nuevo precio', String(t.precio ?? ''));
    if (v == null) return;
    this.api.actualizarTour(t.id, { ...t, precio: Number(v) }).subscribe({ next: () => this.cargar() });
  }
}
