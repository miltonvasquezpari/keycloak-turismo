import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, TourResumen } from '../core/api.service';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h2>Catálogo de tours</h2>
    <p *ngIf="cargando()">Cargando…</p>
    <p *ngIf="error()" class="err">{{error()}}</p>
    <ul class="lista">
      <li *ngFor="let t of tours()">
        <a [routerLink]="['/tours', t.id]"><strong>{{t.nombre}}</strong></a>
        <small>{{t.destino}} · S/ {{t.precio}}</small>
      </li>
    </ul>
  `,
  styles: [`.lista{list-style:none;padding:0}.lista li{padding:.5rem 0;border-bottom:1px solid #e5e7eb}.lista a{display:block}.err{color:#b91c1c}`],
})
export class CatalogoComponent {
  private api = inject(ApiService);
  tours = signal<TourResumen[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  constructor() {
    this.api.tours().subscribe({
      next: (t) => { this.tours.set(t); this.cargando.set(false); },
      error: (e) => { this.error.set('No se pudo cargar el catálogo'); this.cargando.set(false); console.error(e); },
    });
  }
}
