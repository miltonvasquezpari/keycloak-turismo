package pe.turismo.api.web;

import org.springframework.web.bind.annotation.*;
import pe.turismo.api.servicio.TourServicio;

import java.util.List;

@RestController
@RequestMapping("/api/public/tours")
public class TourPublicoController {

  private final TourServicio servicio;

  public TourPublicoController(TourServicio servicio) {
    this.servicio = servicio;
  }

  /** E1 · catalogo publico de tours. */
  @GetMapping
  public List<TourResponse> listar() {
    return servicio.listar();
  }

  /** E2 · detalle publico de un tour. */
  @GetMapping("/{id}")
  public TourResponse obtener(@PathVariable Long id) {
    return servicio.obtener(id);
  }
}
