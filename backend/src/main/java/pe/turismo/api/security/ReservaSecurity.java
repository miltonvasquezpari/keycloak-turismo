package pe.turismo.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import pe.turismo.api.dominio.Reserva;
import pe.turismo.api.repositorio.ReservaRepository;
import pe.turismo.api.web.ReservaResponse;

import java.util.Optional;

/** Reglas de autorizacion sobre el recurso Reserva. Se invoca desde SpEL
 *  en {@code @PreAuthorize} y {@code @PostAuthorize}. */
@Component("reservaSecurity")
public class ReservaSecurity {

  private final ReservaRepository repo;

  public ReservaSecurity(ReservaRepository repo) { this.repo = repo; }

  /** El usuario autenticado es el cliente que creo la reserva. */
  public boolean esDueno(String reservaId, Authentication auth) {
    if (auth == null) return false;
    return repo.findById(reservaId)
        .map(r -> r.getClienteUsername().equals(auth.getName()))
        .orElse(false);
  }

  /** El usuario autenticado pertenece a la misma agencia que la reserva. */
  public boolean mismaAgencia(String reservaId, Authentication auth) {
    String ag = agenciaIdDe(auth);
    if (ag == null) return false;
    return repo.findById(reservaId)
        .map(r -> ag.equals(r.getAgenciaId()))
        .orElse(false);
  }

  /** Regla de E5 para @PostAuthorize sobre un ReservaResponse ya cargado:
   *  dueno, admin, finanzas (ROLE_pagos.procesar), o agente/supervisor
   *  cuya agencia coincide con la de la reserva. */
  public boolean puedeVer(ReservaResponse r, Authentication auth) {
    if (auth == null || r == null) return false;
    if (r.clienteUsername().equals(auth.getName())) return true;
    if (hasAuth(auth, "ROLE_admin")) return true;
    if (hasAuth(auth, "ROLE_pagos.procesar")) return true;
    boolean esAgenteOSup = hasAuth(auth, "ROLE_agente")
        || hasAuth(auth, "ROLE_supervisor-agencia");
    if (esAgenteOSup) {
      String ag = agenciaIdDe(auth);
      return ag != null && ag.equals(r.agenciaId());
    }
    return false;
  }

  /** Lee el claim `agencia_id` del JWT. Nulo si no esta. */
  public String agenciaIdDe(Authentication auth) {
    if (auth instanceof JwtAuthenticationToken jat) {
      Jwt jwt = jat.getToken();
      Object v = jwt.getClaims().get("agencia_id");
      if (v != null) return v.toString();
    }
    return null;
  }

  private boolean hasAuth(Authentication auth, String name) {
    return Optional.ofNullable(auth.getAuthorities()).orElse(java.util.Collections.emptyList())
        .stream().map(GrantedAuthority::getAuthority).anyMatch(name::equals);
  }
}
