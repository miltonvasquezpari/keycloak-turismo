package pe.turismo.api.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;

/** Convierte un JWT de Keycloak en authorities de Spring:
 *  - {@code realm_access.roles[]}                 → {@code ROLE_<r>}
 *  - {@code resource_access.<cliente>.roles[]}    → {@code ROLE_<r>} para cada
 *    cliente listado en {@code turismo.security.role-clients}
 *  - {@code scope} (string separado por espacios) → {@code SCOPE_<s>}
 *  Principal = claim {@code preferred_username}. */
public class JwtAuthoritiesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final List<String> roleClients;

  public JwtAuthoritiesConverter(List<String> roleClients) {
    this.roleClients = roleClients == null ? List.of() : roleClients;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Set<GrantedAuthority> auths = new LinkedHashSet<>();

    // realm_access.roles
    Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
    if (realm != null && realm.get("roles") instanceof Collection<?> roles) {
      roles.forEach(r -> auths.add(
          new SimpleGrantedAuthority(SecurityConstants.PREFIX_ROLE + r)));
    }

    // resource_access.<cliente>.roles
    Map<String, Object> resource = jwt.getClaimAsMap("resource_access");
    if (resource != null) {
      for (String cliente : roleClients) {
        Object entry = resource.get(cliente);
        if (entry instanceof Map<?, ?> m && m.get("roles") instanceof Collection<?> rs) {
          rs.forEach(r -> auths.add(
              new SimpleGrantedAuthority(SecurityConstants.PREFIX_ROLE + r)));
        }
      }
    }

    // scope string -> SCOPE_<s>
    String scope = jwt.getClaimAsString("scope");
    if (scope != null && !scope.isBlank()) {
      for (String s : scope.split("\\s+")) {
        auths.add(new SimpleGrantedAuthority(SecurityConstants.PREFIX_SCOPE + s));
      }
    }

    String name = jwt.getClaimAsString("preferred_username");
    if (name == null || name.isBlank()) name = jwt.getSubject();
    return new JwtAuthenticationToken(jwt, auths, name);
  }
}
