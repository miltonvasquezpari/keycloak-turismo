package pe.turismo.api.error;

import java.time.OffsetDateTime;

/** JSON uniforme de error en espanol. */
public record ErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String error,
    String mensaje,
    String ruta
) {
  public static ErrorResponse de(int status, String error, String mensaje, String ruta) {
    return new ErrorResponse(OffsetDateTime.now(), status, error, mensaje, ruta);
  }
}
