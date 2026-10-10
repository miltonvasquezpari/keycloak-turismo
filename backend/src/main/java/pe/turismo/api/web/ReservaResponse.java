package pe.turismo.api.web;

import pe.turismo.api.dominio.EstadoReserva;
import pe.turismo.api.dominio.Reserva;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReservaResponse(
    String id,
    Long tourId,
    String tourNombre,
    LocalDate fechaTour,
    int personas,
    BigDecimal montoTotal,
    EstadoReserva estado,
    String clienteUsername,
    String agenciaId,
    Instant creadaEn
) {
  public static ReservaResponse de(Reserva r) {
    return new ReservaResponse(
        r.getId(), r.getTour().getId(), r.getTour().getNombre(),
        r.getFechaTour(), r.getPersonas(), r.getMontoTotal(),
        r.getEstado(), r.getClienteUsername(), r.getAgenciaId(), r.getCreadaEn());
  }
}
