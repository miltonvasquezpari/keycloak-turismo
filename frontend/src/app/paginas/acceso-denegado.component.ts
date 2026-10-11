import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-acceso-denegado',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h2>403 · Acceso denegado</h2>
    <p>Tu sesión no tiene los permisos requeridos para esta pantalla.</p>
    <a routerLink="/">← volver al catálogo</a>
  `,
})
export class AccesoDenegadoComponent {}
