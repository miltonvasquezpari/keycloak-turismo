package pe.turismo.api.web;

import jakarta.validation.constraints.*;
import pe.turismo.api.dominio.MetodoPago;

public record CrearPagoRequest(
    @NotBlank @Size(max = 20) String reservaId,
    @NotNull MetodoPago metodo
) {}
