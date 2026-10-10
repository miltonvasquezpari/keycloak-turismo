package pe.turismo.api.web;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Update: todos los campos son opcionales. Solo los no nulos cambian. */
public record ActualizarTourRequest(
    @Size(max = 120) String nombre,
    @Positive BigDecimal precio,
    @Positive Integer duracionHoras,
    @Positive Integer cupoDiario,
    Long destinoId,
    Boolean activo
) {}
