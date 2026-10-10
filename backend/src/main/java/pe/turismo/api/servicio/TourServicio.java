package pe.turismo.api.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.turismo.api.dominio.Destino;
import pe.turismo.api.dominio.Tour;
import pe.turismo.api.error.RecursoNoEncontradoException;
import pe.turismo.api.repositorio.DestinoRepository;
import pe.turismo.api.repositorio.TourRepository;
import pe.turismo.api.web.ActualizarTourRequest;
import pe.turismo.api.web.CrearTourRequest;
import pe.turismo.api.web.TourResponse;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TourServicio {

  private final TourRepository tours;
  private final DestinoRepository destinos;

  public TourServicio(TourRepository tours, DestinoRepository destinos) {
    this.tours = tours;
    this.destinos = destinos;
  }

  /** Mapea dentro de la transaccion para que la relacion LAZY a Destino
   *  se resuelva antes de salir del contexto JPA. */
  public List<TourResponse> listar() {
    return tours.findAll().stream().map(TourResponse::de).toList();
  }

  public TourResponse obtener(Long id) {
    return tours.findById(id)
        .map(TourResponse::de)
        .orElseThrow(() -> new RecursoNoEncontradoException(
            "Tour con id " + id + " no existe"));
  }

  /** E9 · crear tour. La regla ROLE_tours.gestionar la impone el filter chain. */
  @Transactional
  public TourResponse crear(CrearTourRequest req) {
    Destino d = destinos.findById(req.destinoId())
        .orElseThrow(() -> new RecursoNoEncontradoException(
            "Destino con id " + req.destinoId() + " no existe"));
    Tour t = Tour.builder()
        .codigo(req.codigo()).nombre(req.nombre()).precio(req.precio())
        .duracionHoras(req.duracionHoras()).cupoDiario(req.cupoDiario())
        .activo(req.activo()).destino(d)
        .build();
    return TourResponse.de(tours.save(t));
  }

  /** E10 · actualizar tour. Solo campos no nulos cambian. */
  @Transactional
  public TourResponse actualizar(Long id, ActualizarTourRequest req) {
    Tour t = tours.findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException(
            "Tour con id " + id + " no existe"));
    if (req.nombre() != null)        t.setNombre(req.nombre());
    if (req.precio() != null)        t.setPrecio(req.precio());
    if (req.duracionHoras() != null) t.setDuracionHoras(req.duracionHoras());
    if (req.cupoDiario() != null)    t.setCupoDiario(req.cupoDiario());
    if (req.activo() != null)        t.setActivo(req.activo());
    if (req.destinoId() != null) {
      Destino d = destinos.findById(req.destinoId())
          .orElseThrow(() -> new RecursoNoEncontradoException(
              "Destino con id " + req.destinoId() + " no existe"));
      t.setDestino(d);
    }
    return TourResponse.de(tours.save(t));
  }
}
