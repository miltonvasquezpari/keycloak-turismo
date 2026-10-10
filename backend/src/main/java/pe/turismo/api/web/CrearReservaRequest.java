package pe.turismo.api.web;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Body de E4. `clienteUsername` NO viene aqui; se extrae del JWT. */
public record CrearReservaRequest(
    @NotNull Long tourId,
    @NotNull @FutureOrPresent LocalDate fechaTour,
    @Positive int personas,
    /** Si es cliente final sin agencia_id, lo elige del catalogo
     *  (p.ej. "LIM01"). Validacion basica: no vacio, longitud razonable. */
    @NotBlank @Size(max = 20) String agenciaId
) {}
