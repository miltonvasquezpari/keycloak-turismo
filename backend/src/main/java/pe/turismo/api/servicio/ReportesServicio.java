package pe.turismo.api.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.turismo.api.repositorio.ReservaRepository;
import pe.turismo.api.web.VentaReporteItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ReportesServicio {

  private final ReservaRepository reservas;

  public ReportesServicio(ReservaRepository reservas) {
    this.reservas = reservas;
  }

  @Transactional(readOnly = true)
  public List<VentaReporteItem> ventasPorAgenciaYTour() {
    return reservas.agregarVentasPorAgenciaYTour().stream()
        .map(m -> new VentaReporteItem(
            (String) m.get("agenciaId"),
            (String) m.get("tourCodigo"),
            (String) m.get("tourNombre"),
            ((Number) m.get("cantidadReservas")).longValue(),
            ((Number) m.get("personas")).longValue(),
            (BigDecimal) m.get("montoTotal")))
        .toList();
  }
}
