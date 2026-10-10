package pe.turismo.api.web;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CrearTourRequest(
    @NotBlank @Size(max = 20) String codigo,
    @NotBlank @Size(max = 120) String nombre,
    @NotNull @Positive BigDecimal precio,
    @Positive int duracionHoras,
    @Positive int cupoDiario,
    @NotNull Long destinoId,
    boolean activo
) {}
