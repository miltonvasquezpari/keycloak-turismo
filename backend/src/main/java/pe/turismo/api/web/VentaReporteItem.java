package pe.turismo.api.web;

import java.math.BigDecimal;

public record VentaReporteItem(
    String agenciaId,
    String tourCodigo,
    String tourNombre,
    long cantidadReservas,
    long personas,
    BigDecimal montoTotal
) {}
