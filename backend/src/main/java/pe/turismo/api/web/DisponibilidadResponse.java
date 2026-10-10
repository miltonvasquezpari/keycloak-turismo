package pe.turismo.api.web;

import java.time.LocalDate;

public record DisponibilidadResponse(
    Long tourId,
    String tourCodigo,
    String tourNombre,
    LocalDate fecha,
    int cupoDiario,
    int reservadas,
    int disponibles
) {}
