import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

/**
 * Guard parametrizable. Si no hay sesion => login().
 * Si falta algun rol => /acceso-denegado.
 */
export function hasRole(...roles: string[]): CanActivateFn {
  return async (route, state): Promise<boolean | UrlTree> => {
    const kc = inject(KeycloakService);
    const router = inject(Router);
    if (!kc.isLoggedIn()) {
      await kc.login({ redirectUri: window.location.origin + state.url });
      return false;
    }
    if (roles.length === 0) return true;
    const userRoles = kc.getUserRoles(true);
    const ok = roles.every((r) => userRoles.includes(r));
    return ok ? true : router.createUrlTree(['/acceso-denegado']);
  };
}
