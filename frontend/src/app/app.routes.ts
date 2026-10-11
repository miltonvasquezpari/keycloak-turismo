import { Routes } from '@angular/router';
import { hasRole } from './core/role.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./paginas/catalogo.component').then(m => m.CatalogoComponent) },
  { path: 'tours/:id', loadComponent: () => import('./paginas/detalle-tour.component').then(m => m.DetalleTourComponent) },
  {
    path: 'reservar/:tourId',
    canActivate: [hasRole('cliente')],
    loadComponent: () => import('./paginas/reservar.component').then(m => m.ReservarComponent),
  },
  {
    path: 'mis-reservas',
    canActivate: [hasRole('cliente')],
    loadComponent: () => import('./paginas/mis-reservas.component').then(m => m.MisReservasComponent),
  },
  {
    path: 'agencia',
    canActivate: [hasRole('agente')],
    loadComponent: () => import('./paginas/agencia.component').then(m => m.AgenciaComponent),
  },
  {
    path: 'operaciones/tours',
    canActivate: [hasRole('tours.gestionar')],
    loadComponent: () => import('./paginas/operaciones-tours.component').then(m => m.OperacionesToursComponent),
  },
  {
    path: 'finanzas/pagos',
    canActivate: [hasRole('pagos.procesar')],
    loadComponent: () => import('./paginas/finanzas-pagos.component').then(m => m.FinanzasPagosComponent),
  },
  {
    path: 'reportes',
    canActivate: [hasRole()],
    loadComponent: () => import('./paginas/reportes.component').then(m => m.ReportesComponent),
  },
  {
    path: 'acceso-denegado',
    loadComponent: () => import('./paginas/acceso-denegado.component').then(m => m.AccesoDenegadoComponent),
  },
  { path: '**', redirectTo: '' },
];
