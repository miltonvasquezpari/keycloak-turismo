package pe.turismo.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Lee la seccion `turismo.security` de application.yml. Las reglas de ruta
 *  evitan hard-code en el {@link SecurityConfig}: cada regla declara los
 *  authorities exigidos para una combinacion ruta+metodo. */
@ConfigurationProperties(prefix = "turismo.security")
public record TurismoSecurityProperties(
    List<String> issuers,
    String audience,
    List<String> roleClients,
    List<String> corsOrigins,
    List<String> publicPaths,
    List<Rule>   rules,
    List<String> authenticatedPaths,
    /** URL base interna de Keycloak accesible desde el backend (p.ej.
     *  http://turismo-keycloak:8080 cuando el backend corre en Docker).
     *  Si viene vacia o nula, se usa el issuer publico tambien para fetch
     *  del JWKS. Las claims `iss` del JWT siempre se validan contra la URL
     *  publica listada en {@code issuers}. */
    String kcInternalBaseUrl
) {
  public record Rule(String path, List<String> methods, List<String> allOf) {}
}
