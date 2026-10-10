package pe.turismo.api.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(TurismoSecurityProperties.class)
public class SecurityConfig {

  private final TurismoSecurityProperties props;

  public SecurityConfig(TurismoSecurityProperties props) {
    this.props = props;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
                                         AuthenticationEntryPoint restAuthEntryPoint,
                                         AccessDeniedHandler restAccessDeniedHandler) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(c -> c.configurationSource(corsSource()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(e -> e
            .authenticationEntryPoint(restAuthEntryPoint)
            .accessDeniedHandler(restAccessDeniedHandler))
        .authorizeHttpRequests(reg -> {
          // 1) Publicos
          if (props.publicPaths() != null) {
            props.publicPaths().forEach(p -> reg.requestMatchers(p).permitAll());
          }
          // 2) Reglas declarativas (ruta + metodos -> allOf authorities)
          if (props.rules() != null) {
            for (var r : props.rules()) {
              HttpMethod[] methods = r.methods() == null ? new HttpMethod[0]
                  : r.methods().stream().map(HttpMethod::valueOf).toArray(HttpMethod[]::new);
              AuthorizationManager<RequestAuthorizationContext> mgr = allOf(r.allOf());
              for (HttpMethod m : methods) {
                reg.requestMatchers(new AntPathRequestMatcher(r.path(), m.name())).access(mgr);
              }
              if (methods.length == 0) {
                reg.requestMatchers(r.path()).access(mgr);
              }
            }
          }
          // 3) Resto autenticado
          if (props.authenticatedPaths() != null) {
            props.authenticatedPaths().forEach(p -> reg.requestMatchers(p).authenticated());
          }
          reg.anyRequest().denyAll();
        })
        .oauth2ResourceServer(oauth -> oauth
            .authenticationManagerResolver(issuerResolver())
            .authenticationEntryPoint(restAuthEntryPoint)
            .accessDeniedHandler(restAccessDeniedHandler));

    return http.build();
  }

  /** AND de authorities (todas requeridas) para una regla. */
  @SuppressWarnings("unchecked")
  private AuthorizationManager<RequestAuthorizationContext> allOf(List<String> authorities) {
    if (authorities == null || authorities.isEmpty()) {
      return (sup, ctx) -> new org.springframework.security.authorization.AuthorizationDecision(true);
    }
    AuthorityAuthorizationManager<RequestAuthorizationContext>[] mgrs =
        authorities.stream()
            .map(a -> AuthorityAuthorizationManager.<RequestAuthorizationContext>hasAuthority(a))
            .toArray(AuthorityAuthorizationManager[]::new);
    return AuthorizationManagers.allOf(mgrs);
  }

  /** Multi-issuer: un JwtAuthenticationProvider por issuer, mismo converter y
   *  mismo validador de audience. */
  @Bean
  public JwtIssuerAuthenticationManagerResolver issuerResolver() {
    Map<String, AuthenticationManager> byIssuer = new LinkedHashMap<>();
    JwtAuthoritiesConverter converter = new JwtAuthoritiesConverter(props.roleClients());
    OAuth2TokenValidator<Jwt> audValidator = audienceValidator(props.audience());

    String internalBase = props.kcInternalBaseUrl();
    for (String issuer : props.issuers()) {
      NimbusJwtDecoder decoder;
      if (internalBase != null && !internalBase.isBlank()) {
        // Issuer publico http://localhost:8080/realms/X -> JWKS interno
        // http://<internalBase>/realms/X/protocol/openid-connect/certs
        String path = issuer.substring(issuer.indexOf("/realms/"));
        String jwkUri = internalBase.replaceAll("/$", "") + path
            + "/protocol/openid-connect/certs";
        decoder = NimbusJwtDecoder.withJwkSetUri(jwkUri).build();
      } else {
        decoder = JwtDecoders.fromIssuerLocation(issuer);
      }
      decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
          JwtValidators.createDefaultWithIssuer(issuer),
          audValidator));
      JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
      provider.setJwtAuthenticationConverter(converter);
      byIssuer.put(issuer, provider::authenticate);
    }
    return new JwtIssuerAuthenticationManagerResolver(byIssuer::get);
  }

  /** Rechaza tokens cuya claim `aud` no contenga el audience esperado. */
  static OAuth2TokenValidator<Jwt> audienceValidator(String expected) {
    return jwt -> {
      List<String> aud = jwt.getAudience();
      if (aud != null && aud.contains(expected)) return OAuth2TokenValidatorResult.success();
      return OAuth2TokenValidatorResult.failure(new OAuth2Error(
          "invalid_token", "Audience esperada: " + expected, null));
    };
  }

  CorsConfigurationSource corsSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(props.corsOrigins() == null ? List.of() : props.corsOrigins());
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }
}
