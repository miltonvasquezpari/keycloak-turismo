import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';
import { ApiService, Reserva } from '../core/api.service';

@Component({
  selector: 'app-agencia',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Reservas de la agencia</h2>
    <p>Agencia: <strong>{{agenciaId() || '—'}}</strong></p>
    <ul class="lista">
      <li *ngFor="let r of reservas()">
        #{{r.id}} {{r.tourCodigo}} · <em>{{r.estado}}</em>
        <button *ngIf="r.estado === 'PENDIENTE'" (click)="confirmar(r.id)">Confirmar</button>
      </li>
    </ul>
    <p *ngIf="error()" class="err">{{error()}}</p>
  `,
})
export class AgenciaComponent {
  private api = inject(ApiService);
  private kc = inject(KeycloakService);
  reservas = signal<Reserva[]>([]);
  error = signal<string | null>(null);
  agenciaId = signal<string | null>(this.leerAgencia());

  constructor() { this.cargar(); }

  cargar() {
    this.api.reservasAgencia().subscribe({
      next: (r) => this.reservas.set(r),
      error: () => this.error.set('No se pudo cargar'),
    });
  }
  confirmar(id: number) {
    this.api.confirmarReserva(id).subscribe({ next: () => this.cargar() });
  }
  private leerAgencia(): string | null {
    const t = (this.kc.getKeycloakInstance().tokenParsed ?? {}) as Record<string, unknown>;
    const v = t['agencia_id'];
    return typeof v === 'string' ? v : null;
  }
}
