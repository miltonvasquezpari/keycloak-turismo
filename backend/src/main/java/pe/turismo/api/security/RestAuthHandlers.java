package pe.turismo.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import pe.turismo.api.error.ErrorResponse;

import java.io.IOException;

/** EntryPoint (401) y AccessDeniedHandler (403) con el mismo JSON uniforme en
 *  espanol que usa el {@code GlobalExceptionHandler}. */
@Configuration
public class RestAuthHandlers {

  @Autowired ObjectMapper mapper;

  @Bean
  public AuthenticationEntryPoint restAuthEntryPoint() {
    return (req, res, ex) -> write(req, res, HttpStatus.UNAUTHORIZED, "No autenticado",
        ex == null ? "Token ausente o invalido" : ex.getMessage());
  }

  @Bean
  public AccessDeniedHandler restAccessDeniedHandler() {
    return (req, res, ex) -> write(req, res, HttpStatus.FORBIDDEN, "Acceso denegado",
        ex == null ? "No tiene permisos para este recurso" : ex.getMessage());
  }

  private void write(HttpServletRequest req, HttpServletResponse res,
                     HttpStatus status, String error, String msg)
      throws IOException, ServletException {
    res.setStatus(status.value());
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(res.getOutputStream(),
        ErrorResponse.de(status.value(), error, msg, req.getRequestURI()));
  }
}
