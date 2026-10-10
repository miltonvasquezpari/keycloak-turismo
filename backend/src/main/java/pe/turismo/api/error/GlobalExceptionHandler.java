package pe.turismo.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RecursoNoEncontradoException.class)
  public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex,
                                                    HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage(), req);
  }

  @ExceptionHandler(ConflictoEstadoException.class)
  public ResponseEntity<ErrorResponse> conflicto(ConflictoEstadoException ex,
                                                 HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, "Conflicto de estado", ex.getMessage(), req);
  }

  /** @PreAuthorize / @PostAuthorize que fallan dentro del controller
   *  lanzan AccessDeniedException; mapear a 403 con JSON uniforme. */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex,
                                                      HttpServletRequest req) {
    return build(HttpStatus.FORBIDDEN, "Acceso denegado",
        "No tiene permisos para este recurso", req);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> noAutenticado(AuthenticationException ex,
                                                     HttpServletRequest req) {
    return build(HttpStatus.UNAUTHORIZED, "No autenticado", ex.getMessage(), req);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex,
                                                  HttpServletRequest req) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return build(HttpStatus.BAD_REQUEST, "Solicitud invalida", msg, req);
  }

  @ExceptionHandler({HttpMessageNotReadableException.class,
                     MethodArgumentTypeMismatchException.class,
                     IllegalArgumentException.class})
  public ResponseEntity<ErrorResponse> solicitudMal(Exception ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, "Solicitud invalida", ex.getMessage(), req);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest req) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR,
        "Error interno", ex.getClass().getSimpleName(), req);
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus s, String err, String msg,
                                              HttpServletRequest req) {
    return ResponseEntity.status(s).body(
        ErrorResponse.de(s.value(), err, msg, req.getRequestURI()));
  }
}
