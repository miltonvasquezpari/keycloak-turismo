package pe.turismo.api.servicio;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.turismo.api.dominio.*;
import pe.turismo.api.error.ConflictoEstadoException;
import pe.turismo.api.error.RecursoNoEncontradoException;
import pe.turismo.api.repositorio.ReservaRepository;
import pe.turismo.api.repositorio.TourRepository;
import pe.turismo.api.web.CrearReservaRequest;
import pe.turismo.api.web.ReservaResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReservaServicio {

  private final ReservaRepository reservas;
  private final TourRepository    tours;

  public ReservaServicio(ReservaRepository reservas, TourRepository tours) {
    this.reservas = reservas;
    this.tours = tours;
  }

  /** E3 · lista de reservas del usuario autenticado (siempre). */
  @Transactional(readOnly = true)
  public List<ReservaResponse> misReservas(String username) {
    return reservas.findByClienteUsernameOrderByCreadaEnDesc(username)
        .stream().map(ReservaResponse::de).toList();
  }

  /** E4 · crear reserva. La regla de ROLE+SCOPE la aplica el filter chain. */
  @Transactional
  public ReservaResponse crear(String username, CrearReservaRequest req) {
    Tour tour = tours.findById(req.tourId())
        .orElseThrow(() -> new RecursoNoEncontradoException(
            "Tour con id " + req.tourId() + " no existe"));
    if (!tour.isActivo()) {
      throw new ConflictoEstadoException("El tour " + tour.getCodigo() + " no esta activo");
    }
    BigDecimal monto = tour.getPrecio().multiply(BigDecimal.valueOf(req.personas()));
    Reserva r = Reserva.builder()
        .id("R-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
        .tour(tour)
        .fechaTour(req.fechaTour())
        .personas(req.personas())
        .montoTotal(monto)
        .estado(EstadoReserva.PENDIENTE)
        .clienteUsername(username)
        .agenciaId(req.agenciaId())
        .creadaEn(Instant.now())
        .build();
    return ReservaResponse.de(reservas.save(r));
  }

  /** E5 · detalle. @PostAuthorize sobre el DTO ya cargado. */
  @Transactional(readOnly = true)
  @PostAuthorize("@reservaSecurity.puedeVer(returnObject, authentication)")
  public ReservaResponse obtener(String id) {
    return reservas.findById(id)
        .map(ReservaResponse::de)
        .orElseThrow(() -> new RecursoNoEncontradoException(
            "Reserva " + id + " no existe"));
  }

  /** E6 · cancelar (cliente dueno, solo si PENDIENTE). */
  @Transactional
  @PreAuthorize("hasRole('cliente') and @reservaSecurity.esDueno(#id, authentication)")
  public ReservaResponse cancelar(String id) {
    Reserva r = reservas.findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("Reserva " + id + " no existe"));
    if (r.getEstado() != EstadoReserva.PENDIENTE) {
      throw new ConflictoEstadoException(
          "La reserva " + id + " no esta PENDIENTE y no puede cancelarse");
    }
    r.setEstado(EstadoReserva.CANCELADA);
    return ReservaResponse.de(reservas.save(r));
  }
}
