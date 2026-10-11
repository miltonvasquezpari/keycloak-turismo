import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

export interface TourResumen { id: number; codigo: string; nombre: string; destino?: string; precio?: number; }
export interface TourDetalle extends TourResumen { descripcion?: string; cuposTotales?: number; }
export interface Reserva {
  id: string;
  tourId: number;
  tourNombre: string;
  fechaTour: string;
  personas: number;
  montoTotal: number;
  estado: string;
  agenciaId?: string;
  clienteUsername?: string;
}
export interface Pago { id: number; monto: number; estado: string; }

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = environment.apiBase + '/api';

  // publicos
  tours(): Observable<TourResumen[]> {
    return this.http.get<TourResumen[]>(`${this.base}/public/tours`);
  }
  tour(id: string | number): Observable<TourDetalle> {
    return this.http.get<TourDetalle>(`${this.base}/public/tours/${id}`);
  }

  // cliente
  misReservas(): Observable<Reserva[]> {
    return this.http.get<Reserva[]>(`${this.base}/reservas/mias`);
  }
  detalleReserva(id: string): Observable<Reserva> {
    return this.http.get<Reserva>(`${this.base}/reservas/${id}`);
  }
  crearReserva(body: unknown): Observable<Reserva> {
    return this.http.post<Reserva>(`${this.base}/reservas`, body);
  }
  cancelarReserva(id: string): Observable<void> {
    return this.http.put<void>(`${this.base}/reservas/${id}/cancelar`, {});
  }

  // agencia
  reservasAgencia(): Observable<Reserva[]> {
    return this.http.get<Reserva[]>(`${this.base}/agencia/reservas`);
  }
  confirmarReserva(id: string): Observable<void> {
    return this.http.put<void>(`${this.base}/reservas/${id}/confirmar`, {});
  }

  // operaciones (admin de tours; el listado se toma del publico)
  listarTours(): Observable<TourDetalle[]> {
    return this.http.get<TourDetalle[]>(`${this.base}/public/tours`);
  }
  crearTour(body: unknown): Observable<TourDetalle> {
    return this.http.post<TourDetalle>(`${this.base}/admin/tours`, body);
  }
  actualizarTour(id: number, body: unknown): Observable<TourDetalle> {
    return this.http.put<TourDetalle>(`${this.base}/admin/tours/${id}`, body);
  }

  // finanzas
  crearPago(body: unknown): Observable<Pago> {
    return this.http.post<Pago>(`${this.base}/pagos`, body);
  }

  // reportes
  reporteVentas(): Observable<unknown[]> {
    return this.http.get<unknown[]>(`${this.base}/reportes/ventas`);
  }
  exportarVentas(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.base}/reportes/ventas/export`, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}
