import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ApiService } from '../core/api.service';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Reporte de ventas</h2>
    <button (click)="cargar()">Ver</button>
    <!-- fallback: siempre visible, maneja 403 -->
    <button (click)="exportar()">Exportar CSV</button>
    <pre *ngIf="data()">{{data() | json}}</pre>
    <p *ngIf="error()" class="err">{{error()}}</p>
  `,
})
export class ReportesComponent {
  private api = inject(ApiService);
  data = signal<unknown[] | null>(null);
  error = signal<string | null>(null);

  cargar() {
    this.error.set(null);
    this.api.reporteVentas().subscribe({
      next: (d) => this.data.set(d),
      error: (e) => this.error.set(e.status === 403 ? 'Sin permiso para ver' : 'Error al cargar'),
    });
  }
  exportar() {
    this.error.set(null);
    this.api.exportarVentas().subscribe({
      next: (resp) => {
        const blob = resp.body!;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'ventas.csv';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => this.error.set(e.status === 403 ? 'Sin permiso para exportar (horario o rol)' : 'Error al exportar'),
    });
  }
}
