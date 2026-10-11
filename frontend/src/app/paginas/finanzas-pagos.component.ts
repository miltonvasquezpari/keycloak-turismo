import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { KeycloakService } from 'keycloak-angular';
import { ApiService } from '../core/api.service';

@Component({
  selector: 'app-finanzas-pagos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h2>Finanzas · Procesar pago</h2>
    <p *ngIf="!tieneScope()" class="aviso">
      Falta el scope <code>pagos:write</code> en el token. Vuelve a iniciar sesión pidiéndolo:
      <button (click)="reLogin()">Re-login con <code>pagos:write</code></button>
    </p>
    <form (ngSubmit)="pagar()" [class.disabled]="!tieneScope()">
      <label>Reserva ID <input name="reservaId" placeholder="R-1003" [(ngModel)]="form.reservaId" required></label>
      <label>Método
        <select name="metodo" [(ngModel)]="form.metodo" required>
          <option value="TARJETA">TARJETA</option>
          <option value="YAPE">YAPE</option>
          <option value="TRANSFERENCIA">TRANSFERENCIA</option>
        </select>
      </label>
      <button type="submit" [disabled]="!tieneScope()">Pagar</button>
    </form>
    <p *ngIf="ok()">Pago registrado #{{ok()}}</p>
    <p *ngIf="error()" class="err">{{error()}}</p>
  `,
  styles: [`.aviso{background:#fef3c7;padding:.75rem;border-radius:4px}.disabled{opacity:.5;pointer-events:none}`],
})
export class FinanzasPagosComponent {
  private api = inject(ApiService);
  private kc = inject(KeycloakService);
  form: { reservaId?: string; metodo?: string } = { metodo: 'TARJETA' };
  ok = signal<number | null>(null);
  error = signal<string | null>(null);

  tieneScope(): boolean {
    const t = this.kc.getKeycloakInstance().tokenParsed as { scope?: string } | undefined;
    return (t?.scope ?? '').split(' ').includes('pagos:write');
  }
  reLogin() {
    this.kc.login({ scope: 'openid pagos:write', redirectUri: window.location.href });
  }
  pagar() {
    this.api.crearPago(this.form).subscribe({
      next: (p) => this.ok.set(p.id),
      error: (e) => this.error.set(e.error?.mensaje ?? 'Error al procesar'),
    });
  }
}
