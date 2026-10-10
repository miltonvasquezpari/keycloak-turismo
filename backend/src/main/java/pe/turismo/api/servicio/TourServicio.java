package pe.turismo.api.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.turismo.api.error.RecursoNoEncontradoException;
import pe.turismo.api.repositorio.TourRepository;
import pe.turismo.api.web.TourResponse;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TourServicio {

  private final TourRepository repo;

  public TourServicio(TourRepository repo) {
    this.repo = repo;
  }

  /** Mapea dentro de la transaccion para que la relacion LAZY a Destino
   *  se resuelva antes de salir del contexto JPA. */
  public List<TourResponse> listar() {
    return repo.findAll().stream().map(TourResponse::de).toList();
  }

  public TourResponse obtener(Long id) {
    return repo.findById(id)
        .map(TourResponse::de)
        .orElseThrow(() -> new RecursoNoEncontradoException(
            "Tour con id " + id + " no existe"));
  }
}
