package pe.turismo.api.error;

/** Lanzada cuando una transicion de estado no esta permitida. Mapea a HTTP 409. */
public class ConflictoEstadoException extends RuntimeException {
  public ConflictoEstadoException(String mensaje) { super(mensaje); }
}
