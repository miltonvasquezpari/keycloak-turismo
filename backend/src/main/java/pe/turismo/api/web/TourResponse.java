package pe.turismo.api.web;

import pe.turismo.api.dominio.Tour;

import java.math.BigDecimal;

public record TourResponse(
    Long id,
    String codigo,
    String nombre,
    BigDecimal precio,
    int duracionHoras,
    int cupoDiario,
    boolean activo,
    String destino,
    String region
) {
  public static TourResponse de(Tour t) {
    return new TourResponse(
        t.getId(), t.getCodigo(), t.getNombre(), t.getPrecio(),
        t.getDuracionHoras(), t.getCupoDiario(), t.isActivo(),
        t.getDestino().getNombre(), t.getDestino().getRegion()
    );
  }
}
