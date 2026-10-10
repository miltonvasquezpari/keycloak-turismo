package pe.turismo.api.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.turismo.api.servicio.TourServicio;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/tours")
public class TourAdminController {

  private final TourServicio servicio;

  public TourAdminController(TourServicio servicio) {
    this.servicio = servicio;
  }

  /** E9 · crear tour (regla ROLE_tours.gestionar en application.yml). */
  @PostMapping
  public ResponseEntity<TourResponse> crear(@Valid @RequestBody CrearTourRequest req) {
    TourResponse t = servicio.crear(req);
    return ResponseEntity.created(URI.create("/api/public/tours/" + t.id())).body(t);
  }

  /** E10 · actualizar tour. */
  @PutMapping("/{id}")
  public TourResponse actualizar(@PathVariable Long id,
                                 @Valid @RequestBody ActualizarTourRequest req) {
    return servicio.actualizar(id, req);
  }
}
