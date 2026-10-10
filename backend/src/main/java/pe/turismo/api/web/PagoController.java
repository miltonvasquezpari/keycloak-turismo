package pe.turismo.api.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.turismo.api.servicio.PagoServicio;

import java.net.URI;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

  private final PagoServicio servicio;

  public PagoController(PagoServicio servicio) {
    this.servicio = servicio;
  }

  /** E11 · POST /api/pagos (rol + scope en application.yml). */
  @PostMapping
  public ResponseEntity<PagoResponse> registrar(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CrearPagoRequest req) {
    PagoResponse p = servicio.registrar(jwt.getClaimAsString("preferred_username"), req);
    return ResponseEntity.created(URI.create("/api/pagos/" + p.id())).body(p);
  }
}
