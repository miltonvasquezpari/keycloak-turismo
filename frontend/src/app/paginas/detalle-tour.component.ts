import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';
import { ApiService, TourDetalle } from '../core/api.service';
import { TieneRolDirective } from '../core/tiene-rol.directive';

@Component({
  selector: 'app-detalle-tour',
  standalone: true,
  imports: [CommonModule, RouterLink, TieneRolDirective],
  template: `
    <a routerLink="/">← volver</a>
    <h2 *ngIf="tour() as t">{{t.nombre}}</h2>
    <div *ngIf="tour() as t">
      <p>{{t.descripcion}}</p>
      <p><strong>Destino:</strong> {{t.destino}}</p>
      <p><strong>Precio:</strong> S/ {{t.precio}}</p>
      <a *tieneRol="'cliente'" [routerLink]="['/reservar', t.id]"><button>Reservar</button></a>
      <p *ngIf="!logueado"><em>Inicia sesión como cliente para reservar.</em></p>
    </div>
    <p *ngIf="error()" class="err">{{error()}}</p>
  `,
})
export class DetalleTourComponent {
  private api = inject(ApiService);
  private ruta = inject(ActivatedRoute);
  private kc = inject(KeycloakService);
  tour = signal<TourDetalle | null>(null);
  error = signal<string | null>(null);
  logueado = this.kc.isLoggedIn();
  constructor() {
    const id = this.ruta.snapshot.paramMap.get('id')!;
    this.api.tour(id).subscribe({
      next: (t) => this.tour.set(t),
      error: () => this.error.set('Tour no encontrado'),
    });
  }
}
