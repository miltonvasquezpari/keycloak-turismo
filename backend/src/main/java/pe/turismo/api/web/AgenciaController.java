package pe.turismo.api.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.turismo.api.servicio.ReservaServicio;

import java.util.List;

@RestController
@RequestMapping("/api/agencia")
public class AgenciaController {

  private final ReservaServicio servicio;

  public AgenciaController(ReservaServicio servicio) {
    this.servicio = servicio;
  }

  /** E7 · reservas de la agencia del usuario (agencia_id del token).
   *  Admin ve todas (si el token no trae agencia_id). */
  @GetMapping("/reservas")
  public List<ReservaResponse> reservasDeAgencia(@AuthenticationPrincipal Jwt jwt) {
    String ag = jwt.getClaimAsString("agencia_id");
    return servicio.reservasDeAgencia(ag);
  }
}
