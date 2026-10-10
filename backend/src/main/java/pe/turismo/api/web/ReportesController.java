package pe.turismo.api.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.turismo.api.servicio.ReportesServicio;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

  private final ReportesServicio servicio;

  public ReportesController(ReportesServicio servicio) {
    this.servicio = servicio;
  }

  /** E12 · JSON · scope `view` del permiso 'Ver Reporte Ventas'. */
  @GetMapping("/ventas")
  public List<VentaReporteItem> ventas() {
    return servicio.ventasPorAgenciaYTour();
  }

  /** E13 · CSV · scope `export` del permiso 'Exportar Reporte Ventas'. */
  @GetMapping("/ventas/export")
  public ResponseEntity<String> exportar() {
    String header = "agencia_id,tour_codigo,tour_nombre,cantidad_reservas,personas,monto_total";
    String csv = servicio.ventasPorAgenciaYTour().stream()
        .map(v -> String.join(",",
            esc(v.agenciaId()), esc(v.tourCodigo()), esc(v.tourNombre()),
            Long.toString(v.cantidadReservas()),
            Long.toString(v.personas()),
            v.montoTotal().toPlainString()))
        .collect(Collectors.joining("\n", header + "\n", "\n"));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ventas.csv\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csv);
  }

  /** Escapa comillas y envuelve si contiene coma, comilla o salto. */
  private static String esc(String s) {
    if (s == null) return "";
    boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n");
    String out = s.replace("\"", "\"\"");
    return needQuote ? "\"" + out + "\"" : out;
  }
}
