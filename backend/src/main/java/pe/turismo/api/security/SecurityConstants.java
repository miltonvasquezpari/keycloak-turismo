package pe.turismo.api.security;

/** Nombres canonicos de roles y scopes usados en SpEL de {@code @PreAuthorize}
 *  y {@code @PostAuthorize}. No contiene logica; solo constantes. */
public final class SecurityConstants {

  private SecurityConstants() {}

  // Roles de realm (ambos realms)
  public static final String ROL_CLIENTE            = "cliente";
  public static final String ROL_AGENTE             = "agente";
  public static final String ROL_SUPERVISOR_AGENCIA = "supervisor-agencia";
  public static final String ROL_OPERADOR           = "operador";
  public static final String ROL_ADMIN              = "admin";
  public static final String ROL_PARTNER_AGENTE     = "partner-agente";
  public static final String ROL_PARTNER_ADMIN      = "partner-admin";

  // Roles del cliente turismo-api (expuestos por el converter como ROLE_<r>)
  public static final String ROL_TOURS_GESTIONAR     = "tours.gestionar";
  public static final String ROL_RESERVAS_CONFIRMAR  = "reservas.confirmar";
  public static final String ROL_REPORTES_VER        = "reportes.ver";
  public static final String ROL_PAGOS_PROCESAR      = "pagos.procesar";

  // Roles del cliente partners-integracion
  public static final String ROL_DISPONIBILIDAD_CONSULTAR = "disponibilidad.consultar";

  // Scopes
  public static final String SCOPE_RESERVAS_WRITE      = "reservas:write";
  public static final String SCOPE_PAGOS_WRITE         = "pagos:write";
  public static final String SCOPE_REPORTES_READ       = "reportes:read";
  public static final String SCOPE_DISPONIBILIDAD_READ = "disponibilidad:read";

  // Prefijos que agrega el converter
  public static final String PREFIX_ROLE  = "ROLE_";
  public static final String PREFIX_SCOPE = "SCOPE_";
}
