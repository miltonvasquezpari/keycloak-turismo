package pe.turismo.api.web;

import pe.turismo.api.dominio.MetodoPago;
import pe.turismo.api.dominio.Pago;

import java.math.BigDecimal;
import java.time.Instant;

public record PagoResponse(
    Long id,
    String reservaId,
    BigDecimal monto,
    MetodoPago metodo,
    String procesadoPor,
    Instant procesadoEn
) {
  public static PagoResponse de(Pago p) {
    return new PagoResponse(p.getId(), p.getReserva().getId(), p.getMonto(),
        p.getMetodo(), p.getProcesadoPor(), p.getProcesadoEn());
  }
}
