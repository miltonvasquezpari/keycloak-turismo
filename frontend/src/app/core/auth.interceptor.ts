import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { from, switchMap } from 'rxjs';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const esApi = req.url.startsWith(environment.apiBase + '/api');
  const esPublico = req.url.startsWith(environment.apiBase + '/api/public');
  if (!esApi || esPublico) {
    return next(req);
  }
  const kc = inject(KeycloakService);
  return from(kc.getToken()).pipe(
    switchMap((token) => {
      const auth = token
        ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
        : req;
      return next(auth);
    }),
  );
};
