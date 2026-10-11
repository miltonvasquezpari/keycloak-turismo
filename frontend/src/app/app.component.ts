import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { CommonModule } from '@angular/common';
import { TieneRolDirective } from './core/tiene-rol.directive';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, TieneRolDirective],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  private kc = inject(KeycloakService);
  logueado = signal(this.kc.isLoggedIn());
  usuario = signal(this.kc.isLoggedIn() ? this.kc.getUsername() : '');
  roles = signal(this.kc.isLoggedIn() ? this.kc.getUserRoles(true) : []);
  agenciaId = computed(() => this.extraerClaim('agencia_id'));

  async login() {
    await this.kc.login();
  }
  async logout() {
    await this.kc.logout(window.location.origin);
  }

  private extraerClaim(claim: string): string | null {
    if (!this.kc.isLoggedIn()) return null;
    const parsed = (this.kc.getKeycloakInstance().tokenParsed ?? {}) as Record<string, unknown>;
    const v = parsed[claim];
    return typeof v === 'string' ? v : null;
  }
}
