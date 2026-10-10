package pe.turismo.api.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import pe.turismo.api.servicio.DisponibilidadServicio;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/partners")
public class PartnersController {

  private final DisponibilidadServicio servicio;

  public PartnersController(DisponibilidadServicio servicio) {
    this.servicio = servicio;
  }

  /** E14 · GET /api/partners/disponibilidad?tourId=&fecha=YYYY-MM-DD
   *  Acepta tokens de los DOS realms (resolver multi-issuer); la regla
   *  ROL_disponibilidad.consultar la impone el filter chain. En la practica
   *  solo los partners (realm agencias-partners) tienen ese rol. */
  @GetMapping("/disponibilidad")
  public DisponibilidadResponse disponibilidad(
      @RequestParam Long tourId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
    return servicio.consultar(tourId, fecha);
  }
}
