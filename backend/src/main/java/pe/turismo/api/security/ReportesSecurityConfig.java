package pe.turismo.api.security;

import org.keycloak.adapters.authorization.integration.jakarta.ServletPolicyEnforcerFilter;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;
import java.util.Map;

/** Cadena de seguridad DEDICADA a /api/reportes/** (E12, E13).
 *  Se registra antes que la cadena principal (@Order(1)) y:
 *   1. Confia SOLO en el issuer del realm 'turismo' (token de partners -> 401).
 *   2. Delega la autorizacion a Keycloak Authorization Services via
 *      ServletPolicyEnforcerFilter (modo ENFORCING).
 *  La cadena principal (apiFilterChain, @Order(2)) sigue atendiendo todo
 *  lo demas con las reglas por rol/scope de application.yml. */
@Configuration
public class ReportesSecurityConfig {

  private final TurismoSecurityProperties props;

  @Value("${turismo.keycloak.auth-server-url}") String kcAuthServerUrl;
  @Value("${turismo.keycloak.realm}")           String kcRealm;
  @Value("${turismo.keycloak.resource}")        String kcResource;
  @Value("${turismo.keycloak.secret}")          String kcSecret;

  public ReportesSecurityConfig(TurismoSecurityProperties props) {
    this.props = props;
  }

  @Bean
  @Order(1)
  public SecurityFilterChain reportesFilterChain(
      HttpSecurity http,
      CorsConfigurationSource corsSource,
      AuthenticationEntryPoint restAuthEntryPoint,
      AccessDeniedHandler restAccessDeniedHandler) throws Exception {

    JwtDecoder decoder = buildTurismoRealmDecoder();

    http
        .securityMatcher(new AntPathRequestMatcher("/api/reportes/**"))
        .csrf(AbstractHttpConfigurer::disable)
        .cors(c -> c.configurationSource(corsSource))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(e -> e
            .authenticationEntryPoint(restAuthEntryPoint)
            .accessDeniedHandler(restAccessDeniedHandler))
        .authorizeHttpRequests(r -> r.anyRequest().authenticated())
        .oauth2ResourceServer(oauth -> oauth
            .jwt(j -> j.decoder(decoder)
                       .jwtAuthenticationConverter(
                           new JwtAuthoritiesConverter(props.roleClients())))
            .authenticationEntryPoint(restAuthEntryPoint)
            .accessDeniedHandler(restAccessDeniedHandler))
        .addFilterAfter(policyEnforcerFilter(), BearerTokenAuthenticationFilter.class);

    return http.build();
  }

  /** Decoder confia unicamente en el issuer http://localhost:8080/realms/turismo
   *  — un token del realm agencias-partners aqui recibe 401 (nota ³ del PLAN). */
  private JwtDecoder buildTurismoRealmDecoder() {
    String publicIssuer = props.issuers().stream()
        .filter(i -> i.endsWith("/realms/turismo")).findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No hay issuer 'turismo' en turismo.security.issuers"));
    NimbusJwtDecoder dec;
    String internal = props.kcInternalBaseUrl();
    if (internal != null && !internal.isBlank()) {
      String path = publicIssuer.substring(publicIssuer.indexOf("/realms/"));
      String jwkUri = internal.replaceAll("/$", "") + path
          + "/protocol/openid-connect/certs";
      dec = NimbusJwtDecoder.withJwkSetUri(jwkUri).build();
    } else {
      dec = JwtDecoders.fromIssuerLocation(publicIssuer);
    }
    OAuth2TokenValidator<Jwt> aud = SecurityConfig.audienceValidator(props.audience());
    dec.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(publicIssuer), aud));
    return dec;
  }

  /** Enforcer configurado EN CODIGO (no keycloak.json). Mapea cada ruta a
   *  los scopes exigidos por el permiso correspondiente. */
  private ServletPolicyEnforcerFilter policyEnforcerFilter() {
    PolicyEnforcerConfig cfg = new PolicyEnforcerConfig();
    cfg.setAuthServerUrl(kcAuthServerUrl);
    cfg.setRealm(kcRealm);
    cfg.setResource(kcResource);
    cfg.setCredentials(Map.of("secret", kcSecret));
    cfg.setEnforcementMode(PolicyEnforcerConfig.EnforcementMode.ENFORCING);

    // Ambas rutas apuntan al MISMO resource "Reporte Ventas" (el que registramos
    // en Fase 5.1), pero piden scopes diferentes. Sin `name` el enforcer usa el
    // path como id de resource y Keycloak responde invalid_resource.
    PolicyEnforcerConfig.PathConfig ventas = new PolicyEnforcerConfig.PathConfig();
    ventas.setName("Reporte Ventas");
    ventas.setPath("/api/reportes/ventas");
    ventas.setMethods(List.of(method("GET", "view")));

    PolicyEnforcerConfig.PathConfig export = new PolicyEnforcerConfig.PathConfig();
    export.setName("Reporte Ventas");
    export.setPath("/api/reportes/ventas/export");
    export.setMethods(List.of(method("GET", "export")));

    cfg.setPaths(List.of(ventas, export));

    return new ServletPolicyEnforcerFilter(new ConfigurationResolver() {
      @Override public PolicyEnforcerConfig resolve(HttpRequest req) { return cfg; }
    });
  }

  private static PolicyEnforcerConfig.MethodConfig method(String http, String... scopes) {
    PolicyEnforcerConfig.MethodConfig m = new PolicyEnforcerConfig.MethodConfig();
    m.setMethod(http);
    m.setScopes(List.of(scopes));
    return m;
  }
}
