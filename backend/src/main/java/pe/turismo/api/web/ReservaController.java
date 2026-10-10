package pe.turismo.api.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.turismo.api.servicio.ReservaServicio;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

  private final ReservaServicio servicio;

  public ReservaController(ReservaServicio servicio) {
    this.servicio = servicio;
  }

  /** E3 · GET /api/reservas/mias (regla de rol en application.yml). */
  @GetMapping("/mias")
  public List<ReservaResponse> mias(@AuthenticationPrincipal Jwt jwt) {
    return servicio.misReservas(jwt.getClaimAsString("preferred_username"));
  }

  /** E4 · POST /api/reservas (rol + scope en application.yml). */
  @PostMapping
  public ResponseEntity<ReservaResponse> crear(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CrearReservaRequest req) {
    ReservaResponse r = servicio.crear(jwt.getClaimAsString("preferred_username"), req);
    return ResponseEntity.created(URI.create("/api/reservas/" + r.id())).body(r);
  }

  /** E5 · GET /api/reservas/{id} (@PostAuthorize en el servicio). */
  @GetMapping("/{id}")
  public ReservaResponse obtener(@PathVariable String id) {
    return servicio.obtener(id);
  }

  /** E6 · PUT /api/reservas/{id}/cancelar (@PreAuthorize en el servicio). */
  @PutMapping("/{id}/cancelar")
  public ReservaResponse cancelar(@PathVariable String id) {
    return servicio.cancelar(id);
  }

  /** E8 · PUT /api/reservas/{id}/confirmar
   *  Rol de ruta en application.yml: ROLE_reservas.confirmar.
   *  @PreAuthorize adicional en el servicio: admin o misma agencia. */
  @PutMapping("/{id}/confirmar")
  public ReservaResponse confirmar(@PathVariable String id) {
    return servicio.confirmar(id);
  }
}
