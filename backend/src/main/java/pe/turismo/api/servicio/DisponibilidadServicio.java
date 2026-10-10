package pe.turismo.api.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.turismo.api.dominio.Tour;
import pe.turismo.api.error.RecursoNoEncontradoException;
import pe.turismo.api.repositorio.ReservaRepository;
import pe.turismo.api.repositorio.TourRepository;
import pe.turismo.api.web.DisponibilidadResponse;

import java.time.LocalDate;

@Service
public class DisponibilidadServicio {

  private final TourRepository    tours;
  private final ReservaRepository reservas;

  public DisponibilidadServicio(TourRepository tours, ReservaRepository reservas) {
    this.tours = tours;
    this.reservas = reservas;
  }

  @Transactional(readOnly = true)
  public DisponibilidadResponse consultar(Long tourId, LocalDate fecha) {
    Tour t = tours.findById(tourId)
        .orElseThrow(() -> new RecursoNoEncontradoException(
            "Tour con id " + tourId + " no existe"));
    int tomadas = reservas.sumPersonasPorTourYFecha(tourId, fecha);
    int disponibles = Math.max(t.getCupoDiario() - tomadas, 0);
    return new DisponibilidadResponse(t.getId(), t.getCodigo(), t.getNombre(),
        fecha, t.getCupoDiario(), tomadas, disponibles);
  }
}
