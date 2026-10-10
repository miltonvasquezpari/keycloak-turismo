package pe.turismo.api.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.turismo.api.dominio.*;
import pe.turismo.api.error.ConflictoEstadoException;
import pe.turismo.api.error.RecursoNoEncontradoException;
import pe.turismo.api.repositorio.PagoRepository;
import pe.turismo.api.repositorio.ReservaRepository;
import pe.turismo.api.web.CrearPagoRequest;
import pe.turismo.api.web.PagoResponse;

import java.time.Instant;

@Service
public class PagoServicio {

  private final PagoRepository pagos;
  private final ReservaRepository reservas;

  public PagoServicio(PagoRepository pagos, ReservaRepository reservas) {
    this.pagos = pagos;
    this.reservas = reservas;
  }

  /** E11 · registrar pago. La regla ROL_pagos.procesar + SCOPE_pagos:write
   *  la aplica el filter chain. 409 si la reserva no esta CONFIRMADA. */
  @Transactional
  public PagoResponse registrar(String username, CrearPagoRequest req) {
    Reserva r = reservas.findById(req.reservaId())
        .orElseThrow(() -> new RecursoNoEncontradoException(
            "Reserva " + req.reservaId() + " no existe"));
    if (r.getEstado() != EstadoReserva.CONFIRMADA) {
      throw new ConflictoEstadoException(
          "La reserva " + r.getId() + " no esta CONFIRMADA y no puede pagarse");
    }
    Pago p = Pago.builder()
        .reserva(r)
        .monto(r.getMontoTotal())
        .metodo(req.metodo())
        .procesadoPor(username)
        .procesadoEn(Instant.now())
        .build();
    r.setEstado(EstadoReserva.PAGADA);
    reservas.save(r);
    return PagoResponse.de(pagos.save(p));
  }
}
