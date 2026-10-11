import { Directive, Input, OnInit, TemplateRef, ViewContainerRef, inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

/**
 * *tieneRol="'agente'"  o  *tieneRol="['agente','supervisor-agencia']"
 * Oculta el bloque si el usuario no tiene al menos uno de los roles.
 */
@Directive({ selector: '[tieneRol]', standalone: true })
export class TieneRolDirective implements OnInit {
  private tpl = inject(TemplateRef<unknown>);
  private vcr = inject(ViewContainerRef);
  private kc = inject(KeycloakService);

  @Input('tieneRol') roles!: string | string[];

  ngOnInit(): void {
    const necesarios = Array.isArray(this.roles) ? this.roles : [this.roles];
    const actuales = this.kc.isLoggedIn() ? this.kc.getUserRoles(true) : [];
    const ok = necesarios.some((r) => actuales.includes(r));
    this.vcr.clear();
    if (ok) this.vcr.createEmbeddedView(this.tpl);
  }
}
