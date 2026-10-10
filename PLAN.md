# PLAN.md — TurismoPerú Reservas · Seguridad con Keycloak

Trabajo final del curso *Security with Keycloak* (Galaxy).
Plan de ejecución para **Claude Code**: una fase por sesión, en orden. Cada fase tiene criterios de aceptación verificables y termina en commit.

**Cómo leer**
- §1 **Modelo de seguridad** = fuente de verdad. No inventar nombres fuera de ahí.
- §3 **Matriz de pruebas** = qué responde cada endpoint a cada usuario. Los tests y Postman salen de ahí.
- §5 **Trampas** = leer antes de cada fase.

**Principio rector**: el plan está limitado a lo que pide la rúbrica. No se añade ingeniería "nice to have" (Flyway, Swagger, tests unitarios formales, scripts avanzados, scopes técnicos extra) salvo que sume puntos directos.

---

## Enunciado original (Galaxy)

### II. Actividades
- **a.** Caso de aplicación (API REST) con autenticación y autorización con Keycloak, Spring Security y OAuth2.
- **b.** Repositorio con el código, colecciones de Postman y JSON de configuración de Keycloak.
- **c.** 10 componentes, 2 puntos cada uno:

| # | Componente | Enunciado | Fase |
|---|---|---|---|
| 1 | Keycloak (Instalación) | Instalar con binario, Docker o Docker Compose, con BD persistente (PostgreSQL, MySQL u otros) | F1 |
| 2 | Keycloak (Configuración) | 2+ realms con clientes, usuarios, grupos y subgrupos, roles y alcances; a nivel de clientes también roles y alcances | F2 |
| 3 | APIs REST – Spring Boot | Diagrama de las APIs; accesos públicos, privados y especiales; gestionar recursos o proceso de negocio; matriz RACI con usuarios, grupos y subgrupos, roles y recursos | F3, F4 |
| 4 | Keycloak & Spring Security | SecurityFilterChain con accesos público y privado por rutas y roles; `@PreAuthorize` o `@PostAuthorize`; valores personalizados en constantes, properties o BD | F4 |
| 5 | App Web – Angular | Diagrama de la app; accesos públicos, privados y especiales; matriz RACI | F6 |
| 6 | Seguridad Granular | Scopes, Resources, Policy & Permission en un cliente específico; pruebas en back-end o front-end | F5 |
| 7 | Integración Externa | Social Login (GitHub, LinkedIn, Facebook, Google u otros) | F7 |
| 8 | Personalización UI | Tema personalizado con colores, logos e imágenes | F8 |
| 9 | Configuración | Español, política de contraseña (10 chars, 1 mayús, 1 minús, 1 dígito, especiales `@%&#.+/!=?¿`); duración de token y refresh token; número de reutilización | F2 |
| 10 | Otros | Componentes adicionales como valor agregado | F9 |

---

## 0. Decisiones fijas

| Tema | Decisión |
|---|---|
| Caso | **TurismoPerú Reservas**: clientes reservan tours; agencias confirman; Finanzas cobra; Operaciones administra el catálogo |
| Keycloak | **26.x** (tag exacto, no `latest`). Imagen `quay.io/keycloak/keycloak` |
| BD | PostgreSQL 16 (`postgres:16-alpine`), volumen persistente. BDs `keycloak` y `turismo_app` |
| Backend | Spring Boot **3.5.x**, Java **21**, Maven. `ddl-auto=update` + `data.sql` (sin Flyway) |
| Frontend | Angular **18+** standalone, `keycloak-angular` + `keycloak-js` |
| Social login | GitHub |
| Pruebas | Postman + Newman |
| Repositorio | GitHub, monorepo, commits `feat(fase-N): …` |
| Idioma | UI y mensajes en español |
| Zona horaria | `America/Lima` |

### Puertos
| Servicio | Host |
|---|---|
| Keycloak | 8080 (HTTP), 9000 (health) |
| PostgreSQL | 5433 → 5432 |
| Backend | 8081 |
| Frontend | 4200 |
| Mailpit (opcional, solo si F9 incluye email) | 8025 / 1025 |

### Estructura
```
keycloak-turismo/
├── CLAUDE.md  PLAN.md  README.md
├── .gitignore  .env.example
├── docker-compose.yml
├── keycloak/
│   ├── realms/                          # turismo-realm.json, agencias-partners-realm.json
│   └── themes/turismo-peru/login/
├── backend/                             # Spring Boot (turismo-api)
├── frontend/                            # Angular (turismo-web)
├── postman/
│   ├── TurismoPeru-Keycloak.postman_collection.json
│   └── local.postman_environment.json
└── docs/
    ├── 01-modelo-dominio.md
    ├── 02-api-diagrama.md
    ├── 03-web-diagrama.md
    ├── 04-raci-backend.md
    ├── 05-raci-frontend.md
    ├── 06-seguridad-granular.md
    ├── 07-github-idp.md
    ├── 08-tema-ui.md
    ├── 09-configuracion-realm.md
    ├── 10-valor-agregado.md
    └── evidencias/
```

---

## 1. Modelo de seguridad

### 1.1 Realms
| Realm | Display name | Propósito |
|---|---|---|
| `turismo` | TurismoPerú | Clientes, agencias, operaciones, finanzas, admin |
| `agencias-partners` | TurismoPerú Partners | Agencias aliadas externas (B2B). Portal y service account |

### 1.2 Clientes
| Realm | Client ID | Configuración | Uso |
|---|---|---|---|
| turismo | `turismo-web` | Público, PKCE S256. Standard flow ON. **Direct access grants ON (solo dev)** para que Postman use las mismas credenciales. Redirect `http://localhost:4200/*`, web origins `http://localhost:4200` | Angular + Postman |
| turismo | `turismo-api` | Confidencial. Client auth ON, **Authorization ON** (service account activo). Standard flow OFF | Backend. Dueño de roles de cliente y de la seguridad granular (F5) |
| agencias-partners | `partners-portal` | Público, PKCE. **Direct grants ON (dev)**. Redirect `http://localhost:4200/*` | Portal web + Postman para `pedro` |
| agencias-partners | `partners-integracion` | Confidencial, service account | Integración B2B (client credentials) |

Secretos en `.env` como `TURISMO_API_SECRET` y `PARTNERS_INTEGRACION_SECRET`. En los JSON de realm → placeholders `${…}`.

### 1.3 Roles
**Realm `turismo`:**
| Rol | Compuesto |
|---|---|
| `cliente` | — |
| `agente` | — |
| `supervisor-agencia` | `agente` + `turismo-api:reportes.ver` |
| `operador` | `turismo-api:tours.gestionar` |
| `admin` | todos los anteriores + roles de cliente |

**Cliente `turismo-api`:** `tours.gestionar`, `reservas.confirmar`, `reportes.ver`, `pagos.procesar`.
**Realm `agencias-partners`:** `partner-agente`, `partner-admin` (compuesto de `partner-agente`).
**Cliente `partners-integracion`:** `disponibilidad.consultar`.

### 1.4 Client scopes
Solo los que la API usa de verdad. Los mappers de audience y de atributos van sobre `turismo-api-dedicated` (scope dedicado del cliente, ya existe) para no inventar scopes técnicos.

| Realm | Scope | Default/Opt. en clientes | Uso |
|---|---|---|---|
| turismo | `reservas:write` | Default en `turismo-web` | exigido en `POST /api/reservas` |
| turismo | `pagos:write` | **Optional** en `turismo-web` | exigido en `POST /api/pagos` (se pide con re-login) |
| turismo | `reportes:read` | Default en `turismo-web` | informativo, usado por `/api/reportes/**` |
| partners | `disponibilidad:read` | Default en ambos clientes partners | exigido en `/api/partners/**` |

**Mappers del scope dedicado `turismo-api-dedicated`** (y equivalentes en los clientes partners):
- *Audience* → `turismo-api`.
- *User Attribute* `agencia_id` → claim `agencia_id`.
- *Group Membership* → claim `groups` (full path ON).

**Concepto demostrado:** scope + rol combinados. `fiona` sin el scope `pagos:write` recibe 403 aunque tenga el rol `pagos.procesar`.

### 1.5 Grupos y subgrupos — realm `turismo`
Roles asignados a grupos. `agencia_id` en cada subgrupo hoja (ver T6).

```
Clientes
├── Estandar                roles: cliente
└── Premium                 roles: cliente              attr: nivel=premium
Agencias
├── Agencia-Lima
│   ├── Ventas              roles: agente, reservas.confirmar   attr: agencia_id=LIM01
│   └── Supervision         roles: supervisor-agencia           attr: agencia_id=LIM01
└── Agencia-Cusco
    └── Ventas              roles: agente, reservas.confirmar   attr: agencia_id=CUS01
Operaciones
├── Catalogo                roles: operador
└── Finanzas                roles: pagos.procesar, reportes.ver
Administracion              roles: admin
```

### 1.6 Grupos y subgrupos — realm `agencias-partners`
```
Partners
├── Nacionales
│   └── Arequipa-Tours      roles: partner-agente, disponibilidad.consultar   attr: agencia_id=AQP-P01
└── Internacionales
    └── Andes-Travel-Chile  roles: partner-agente, disponibilidad.consultar   attr: agencia_id=SCL-P01
Partners-Admin              roles: partner-admin, disponibilidad.consultar
```
El service account de `partners-integracion` recibe directamente `disponibilidad.consultar`.

### 1.7 Usuarios de prueba
Reglas: contraseña `Turismo#2026a` **no temporal**, email, nombre y apellido completos, `emailVerified=true`, locale `es`. Sin acciones requeridas (T2, T3).

| Usuario | Realm | Grupo |
|---|---|---|
| `ana.cliente` | turismo | Clientes/Estandar |
| `carlos.premium` | turismo | Clientes/Premium |
| `lucia.agente` | turismo | Agencias/Agencia-Lima/Ventas |
| `jorge.supervisor` | turismo | Agencias/Agencia-Lima/Supervision |
| `mario.agente` | turismo | Agencias/Agencia-Cusco/Ventas |
| `olga.operador` | turismo | Operaciones/Catalogo |
| `fiona.finanzas` | turismo | Operaciones/Finanzas |
| `admin.turismo` | turismo | Administracion |
| `pedro.partner` | agencias-partners | Partners/Nacionales/Arequipa-Tours |
| `rosa.partner` | agencias-partners | Partners-Admin |

### 1.8 Dominio y proceso de negocio

**Entidades:** `Destino`, `Tour`, `Reserva`, `Pago` (detalle en `DEFINICION.md`).

**Máquina de estados**
```
PENDIENTE --confirmar (agente)--> CONFIRMADA --pagar (finanzas)--> PAGADA
PENDIENTE --cancelar (cliente dueño)--> CANCELADA
```
Transición inválida → **409 Conflict**. La autorización se evalúa antes que el estado.

**Datos semilla** (`src/main/resources/data.sql`, cargado por Spring):
- 5 destinos: Cusco, Ica, Arequipa, Puno, La Libertad.
- 6 tours: Machu Picchu, Valle Sagrado, Montaña 7 Colores, Islas Ballestas + Huacachina, Colca 2D/1N, Chan Chan.
- 4 reservas:

| Id | Cliente | Tour | Agencia | Estado |
|---|---|---|---|---|
| R-1001 | carlos.premium | Machu Picchu | LIM01 | PENDIENTE |
| R-1002 | ana.cliente | Valle Sagrado | CUS01 | PENDIENTE |
| R-1003 | ana.cliente | Chan Chan | LIM01 | CONFIRMADA |
| R-1004 | carlos.premium | Colca | CUS01 | PAGADA |

### 1.9 Endpoints de la API — 14 endpoints
Cuatro tipos: **Público** (sin token), **Privado** (token + rol/scope), **Especial** (regla sobre el recurso con `@Pre/PostAuthorize`), **Granular** (decisión de Keycloak).

| # | Método + ruta | Tipo | Regla |
|---|---|---|---|
| E1 | `GET /api/public/tours` | Público | `permitAll` |
| E2 | `GET /api/public/tours/{id}` | Público | `permitAll` |
| E3 | `GET /api/reservas/mias` | Privado | `ROLE_cliente` |
| E4 | `POST /api/reservas` | Privado | `ROLE_cliente` **y** `SCOPE_reservas:write` |
| E5 | `GET /api/reservas/{id}` | Especial | `@PostAuthorize`: dueño, o agente/supervisor con misma `agencia_id`, o admin, o finanzas |
| E6 | `PUT /api/reservas/{id}/cancelar` | Especial | `@PreAuthorize`: `ROLE_cliente` y dueño. 409 si no PENDIENTE |
| E7 | `GET /api/agencia/reservas` | Privado | `ROLE_agente`. Devuelve solo las de su `agencia_id` |
| E8 | `PUT /api/reservas/{id}/confirmar` | Especial | `ROLE_reservas.confirmar` (regla ruta) + `@PreAuthorize("hasRole('admin') or @reservaSecurity.mismaAgencia(#id, authentication)")` |
| E9 | `POST /api/admin/tours` | Privado | `ROLE_tours.gestionar` |
| E10 | `PUT /api/admin/tours/{id}` | Privado | `ROLE_tours.gestionar` |
| E11 | `POST /api/pagos` | Especial | `ROLE_pagos.procesar` **y** `SCOPE_pagos:write`. 409 si reserva no CONFIRMADA |
| E12 | `GET /api/reportes/ventas` | **Granular** | Policy Enforcer, scope `view` |
| E13 | `GET /api/reportes/ventas/export` | **Granular** | Policy Enforcer, scope `export` (CSV) |
| E14 | `GET /api/partners/disponibilidad?tourId=&fecha=` | Privado multi-realm | Token de `agencias-partners` con `ROLE_disponibilidad.consultar` |

Cubre todos los conceptos de la rúbrica con el mínimo de superficie.

---

## 2. Fases

### Fase 0 — Repositorio base (actividad b)
1. `git init`, estructura de carpetas.
2. `.gitignore`: `.env`, `**/node_modules`, `**/target`, `**/dist`, `.idea`, `.vscode`, `*.log`, `.angular`.
3. `.env.example` con BD, bootstrap admin de Keycloak, `TURISMO_API_SECRET`, `PARTNERS_INTEGRACION_SECRET`, `GITHUB_CLIENT_ID/SECRET`.
4. `cp .env.example .env`.

**Aceptación**: `.env` no versionado. Commit inicial.

### Fase 1 — Instalación de Keycloak (rúbrica 1)
1. `docker-compose.yml`:
   - **postgres**: `postgres:16-alpine`, `POSTGRES_DB=keycloak`, volumen `pg_data`, puerto `5433:5432`, healthcheck `pg_isready`. Un script en `postgres/init/01-app.sh` crea la BD `turismo_app` y su usuario desde variables de entorno.
   - **keycloak**: imagen 26.x fija, `command: start-dev --import-realm`. Variables: `KC_DB=postgres`, URL, user/pass, `KC_BOOTSTRAP_ADMIN_*`, `KC_HEALTH_ENABLED=true`, `TZ=America/Lima`, los secretos de clientes y GitHub. Volúmenes: `./keycloak/realms:/opt/keycloak/data/import` (escritura) y `./keycloak/themes:/opt/keycloak/themes`. Puertos 8080 y 9000. `depends_on` con `service_healthy`. Healthcheck via `/dev/tcp` a `9000/health/ready`.
2. En `docs/09-configuracion-realm.md`, sección "Instalación": comandos usados, por qué PostgreSQL, mención breve de la variante prod (`start --optimized`, HTTPS, hostname).

**Aceptación**
- `docker compose up -d` deja ambos healthy.
- Login en `http://localhost:8080/admin`.
- `psql -h localhost -p 5433 -U … -d keycloak -c '\dt'` muestra tablas.
- `down && up` conserva datos (persistencia).

### Fase 2 — Realms y configuración (rúbrica 2 y 9)
1. Crear realms, clientes, roles, scopes (con mappers), grupos/subgrupos, usuarios por consola o `kcadm.sh`.
2. **Configuración ítem 9**, en ambos realms:
   - **Idioma:** Localization ON, soportados `es`+`en`, por defecto `es`. Locale `es` en cada usuario.
   - **Política de contraseña:**
     ```
     length(10) and upperCase(1) and lowerCase(1) and digits(1) and regexPattern(^(?=.*[@%&#.+/!=?¿]).*$) and passwordHistory(3) and notUsername
     ```
     `regexPattern` fuerza los especiales del enunciado (`specialChars` aceptaría cualquiera).
   - **Tokens/sesiones:**

| Parámetro | Valor |
|---|---|
| Access Token Lifespan | 5 min |
| SSO Session Idle | 30 min |
| SSO Session Max | 10 h |
| Revoke Refresh Token | ON |
| Refresh Token Max Reuse | 0 |

3. **"Número de veces a reutilizar"** (ambigüedad del enunciado): se cubren ambas lecturas (contraseña vía `passwordHistory(3)`, refresh token vía `refreshTokenMaxReuse=0`) y se documenta.
4. Exportar realms a JSON:
   - `docker compose stop keycloak`
   - `docker compose run --rm keycloak export --dir /opt/keycloak/data/import --realm <X> --users realm_file`
   - Reemplazar manualmente (o con `sed`/`jq`) los secretos reales por `${TURISMO_API_SECRET}`, `${PARTNERS_INTEGRACION_SECRET}`, `${GITHUB_CLIENT_ID/SECRET}`.
   - `docker compose up -d`.

**Aceptación**
- `docker compose down -v && up -d` recrea los dos realms completos desde JSON.
- Token de `ana.cliente` por password grant con `turismo-web` incluye: `aud=turismo-api`, `realm_access.roles=[cliente]`, `scope` con `reservas:write`, `groups=["/Clientes/Estandar"]`.
- Token de `lucia.agente` incluye `agencia_id=LIM01`.
- Service account de `partners-integracion` por client_credentials incluye `resource_access.partners-integracion.roles=[disponibilidad.consultar]` y `aud=turismo-api`.
- Login en español.
- Contraseña `Abcdefghi1` (sin especial de la lista) → rechazada. `Turismo#2026a` → aceptada.
- Reuso del mismo refresh token → `400 invalid_grant`.
- Reusar una de las 3 últimas contraseñas → rechazada.

### Fase 3 — Diseño API + RACI (rúbrica 3, parte documental)
1. `docs/01-modelo-dominio.md`: diagrama de clases (Mermaid) y diagrama de estados.
2. `docs/02-api-diagrama.md`:
   - Arquitectura: Angular ↔ Keycloak (2 realms) ↔ API ↔ PostgreSQL.
   - Mapa de 14 endpoints coloreados por tipo (público/privado/especial/granular).
3. `docs/04-raci-backend.md`: tabla RACI con los 14 endpoints en filas y columnas por grupo/rol/usuarios (Anexo A).

**Aceptación**: Mermaid renderiza en GitHub. Cada endpoint E1–E14 aparece en diagrama y RACI. Una sola "A" por fila.

### Fase 4 — Backend Spring Boot (rúbrica 3 y 4)
**Dependencias:** `spring-boot-starter-web`, `-security`, `-oauth2-resource-server`, `-data-jpa`, `-validation`, `-actuator`; `postgresql`; H2 opcional para dev.

**Sin:** Flyway, Swagger, tests unitarios formales (si Claude Code tiene tiempo al final, puede añadir 2 o 3 tests simples con `jwt()` mock, no es requisito).

**Paquetes:** `config`, `security`, `dominio`, `repositorio`, `servicio`, `web`, `error`.

1. **Persistencia:**
   - `application.yml`: `spring.jpa.hibernate.ddl-auto=update`, `spring.sql.init.mode=always`, `defer-datasource-initialization=true`.
   - `data.sql`: destinos, tours, reservas (sección 1.8).
   - Conexión a `localhost:5433/turismo_app`.
2. **DTOs validados** (`@NotNull`, `@Positive`, `@FutureOrPresent`). `clienteUsername` sale del token, nunca del body.
3. **Manejo de errores:** `@RestControllerAdvice` con JSON `{timestamp, status, error, mensaje, ruta}` en español para 400/404/409. `AuthenticationEntryPoint` + `AccessDeniedHandler` propios para 401/403.
4. **Propiedades personalizadas** (`application.yml` → `@ConfigurationProperties("turismo.security")`):
   ```yaml
   server: { port: 8081 }
   turismo:
     security:
       issuers:
         - http://localhost:8080/realms/turismo
         - http://localhost:8080/realms/agencias-partners
       audience: turismo-api
       role-clients: [turismo-api, partners-integracion]
       cors-origins: [http://localhost:4200]
       public-paths: [/api/public/**, /actuator/health]
       rules:
         - { path: /api/reservas/mias,         methods: [GET], all-of: [ROLE_cliente] }
         - { path: /api/reservas,              methods: [POST], all-of: [ROLE_cliente, SCOPE_reservas:write] }
         - { path: /api/agencia/reservas,      methods: [GET], all-of: [ROLE_agente] }
         - { path: /api/reservas/*/confirmar,  methods: [PUT], all-of: [ROLE_reservas.confirmar] }
         - { path: /api/admin/tours/**,        methods: [POST, PUT], all-of: [ROLE_tours.gestionar] }
         - { path: /api/pagos,                 methods: [POST], all-of: [ROLE_pagos.procesar, SCOPE_pagos:write] }
         - { path: /api/partners/**,           methods: [GET], all-of: [ROLE_disponibilidad.consultar] }
       authenticated-paths: [/api/**]
   ```
   `SecurityConstants` (clase `final`): nombres de roles y scopes para SpEL (`T(…).ROL_ADMIN`).
5. **`SecurityFilterChain` principal** (`@Order(2)`):
   - Stateless, CSRF off, CORS desde propiedades.
   - `authorizeHttpRequests` construido en bucle: `public-paths` → `rules` (con `AuthorizationManagers.allOf(hasAuthority…)`) → `authenticated-paths` → `anyRequest().denyAll()`.
   - `oauth2ResourceServer` con `JwtIssuerAuthenticationManagerResolver`:
     - Un `JwtAuthenticationProvider` por issuer, con el mismo conversor.
     - Validadores: issuer + timestamp + audiencia `turismo-api`.
   - **Conversor de authorities:**
     - `realm_access.roles` → `ROLE_<rol>`.
     - `resource_access.<cliente>.roles` → `ROLE_<rol>` para cada `role-clients`.
     - `scope` → `SCOPE_<scope>`.
     - Principal = `preferred_username`.
6. **Seguridad por método:** `@EnableMethodSecurity`, bean `reservaSecurity`:
   - `esDueno(id, auth)`, `puedeVer(ReservaDto, auth)`, `mismaAgencia(id, auth)`.
   - E5: `@PostAuthorize("@reservaSecurity.puedeVer(returnObject, authentication)")`.
   - E6: `@PreAuthorize("hasRole(T(…).ROL_CLIENTE) and @reservaSecurity.esDueno(#id, authentication)")`.
   - E8: `@PreAuthorize("hasRole('admin') or @reservaSecurity.mismaAgencia(#id, authentication)")`.
7. **Multi-issuer**: lo justificado. Los tokens de partners no llevan roles del realm `turismo`, así que endpoints de ese realm les dan 403 natural; en `/api/partners/**` se les permite.

**Aceptación**
- `mvn spring-boot:run` arranca sin errores. BD poblada.
- 10 celdas de la matriz de §3 verificadas con `curl` + tokens reales (al menos: 401 sin token, 403 por rol, 403 por scope, 403 por dueño, 403 por agencia, 200 del partner).

### Fase 5 — Seguridad granular (rúbrica 6)
Todo en cliente `turismo-api` → Authorization.

1. **Settings:** mode `ENFORCING`, decision strategy `UNANIMOUS`. **Borrar** `Default Resource`, `Default Policy`, `Default Permission` (T4).
2. **Authorization scopes:** `view`, `export`.
3. **Resources:**

| Nombre | Tipo | URIs | Scopes |
|---|---|---|---|
| Reporte Ventas | `urn:turismo:reporte` | `/api/reportes/ventas`, `/api/reportes/ventas/*` | view, export |

4. **Policies:**

| Nombre | Tipo | Definición |
|---|---|---|
| Rol Supervisor | Role | realm `supervisor-agencia` |
| Rol Reportes | Role | cliente `turismo-api:reportes.ver` |
| Rol Admin | Role | realm `admin` |
| Grupo Supervision Lima | Group | `/Agencias/Agencia-Lima/Supervision` |
| Horario Laboral | Time | L-S 08:00–20:00 (hora Lima, T5) |
| Supervisor Lima en Horario | Aggregated | Grupo Supervision Lima + Horario Laboral (UNANIMOUS) |

5. **Permissions:**

| Nombre | Tipo | Recurso/scope | Policies | Estrategia |
|---|---|---|---|---|
| Ver Reporte Ventas | Scope-based | view | Rol Supervisor, Rol Reportes, Rol Admin | AFFIRMATIVE |
| Exportar Reporte Ventas | Scope-based | export | Supervisor Lima en Horario, Rol Admin | AFFIRMATIVE |

6. **Backend:**
   - Dependencia `org.keycloak:keycloak-policy-enforcer` (misma versión que servidor, T14).
   - **Segundo `SecurityFilterChain`** `@Order(1)` con `securityMatcher("/api/reportes/**")`:
     - JWT del realm `turismo` (confianza en un solo issuer, token de partners aquí → 401).
     - `ServletPolicyEnforcerFilter` registrado `addFilterAfter(…, BearerTokenAuthenticationFilter.class)`.
   - Configuración del enforcer **en código** desde `turismo.keycloak.*` (lee variables de entorno). Rutas:
     - `GET /api/reportes/ventas` → `view`.
     - `GET /api/reportes/ventas/export` → `export`.
7. Endpoints:
   - E12: ventas por agencia/tour (JSON).
   - E13: CSV con `Content-Disposition: attachment`.
8. Capturas con **Evaluate** (PERMIT/DENY por usuario).
9. Postman: RPT con `grant_type=urn:ietf:params:oauth:grant-type:uma-ticket`, `audience=turismo-api`.

**Aceptación**
- `jorge` ve y exporta (en horario). Fuera de horario (editar policy temporalmente): no exporta. Restaurar.
- `fiona` ve pero no exporta.
- `ana` recibe 403.
- `admin` ve y exporta siempre.
- `docs/06-seguridad-granular.md` con tablas + capturas + Mermaid recurso→scope→permiso→policies.
- Realm re-exportado (incluye `authorizationSettings`).

### Fase 6 — Frontend Angular (rúbrica 5)
1. `docs/03-web-diagrama.md`: árbol de rutas + `sequenceDiagram` de PKCE.
2. `docs/05-raci-frontend.md`: RACI con pantallas en filas, mismas columnas del Anexo A.
3. `ng new turismo-web --standalone --routing --style=scss`. Instalar `keycloak-angular` + `keycloak-js` (versiones compatibles).
4. Config en `environment.ts`: url 8080, realm `turismo`, clientId `turismo-web`.
   - Init `check-sso`, PKCE S256, `silentCheckSsoRedirectUri`. Refresh automático.
   - Usar API de la versión instalada (`provideKeycloak` + `createAuthGuard` o `KeycloakService`).
5. `silent-check-sso.html` en `public/` o `src/assets/`.
6. **Interceptor** que agrega bearer solo a `http://localhost:8081/api` **y no** a `/api/public`.
7. **Rutas:**

| Ruta | Tipo | Guard | Contenido |
|---|---|---|---|
| `/` | Público | — | Catálogo (E1) |
| `/tours/:id` | Público | — | Detalle (E2) + botón reservar |
| `/reservar/:tourId` | Privado | rol `cliente` | Formulario (E4) |
| `/mis-reservas` | Privado | rol `cliente` | Lista (E3), detalle (E5), cancelar si PENDIENTE (E6) |
| `/agencia` | Especial | rol `agente` + claim `agencia_id` | Reservas (E7), confirmar (E8). Muestra `agencia_id` |
| `/operaciones/tours` | Privado | rol de cliente `tours.gestionar` | CRUD (E9, E10) |
| `/finanzas/pagos` | Especial | rol `pagos.procesar` | Pagar (E11). Pide scope `pagos:write` con re-login |
| `/reportes` | Granular | autenticado | Ver (E12), exportar (E13). Visibilidad del botón export: según respuesta UMA; **fallback**: mostrar siempre y manejar 403 |
| `/acceso-denegado` | Público | — | 403 amable |

8. Directiva `*tieneRol="'agente'"` para ocultar acciones.
9. Encabezado con menú por rol, nombre del usuario, login/logout. Logout con `post_logout_redirect_uri`.
10. Diseño simple y responsive. No se exige coordinar paleta con el tema de Keycloak.

**Aceptación**
- Sin sesión: catálogo visible; `/mis-reservas` redirige al login.
- Cada usuario ve solo su menú.
- `lucia` ve solo LIM01; `mario` solo CUS01.
- Refresh automático funciona después de 5 min.
- Logout cierra sesión en Keycloak.

### Fase 7 — Integración GitHub (rúbrica 7)
**Manual (Milton):**
1. GitHub → Settings → Developer settings → OAuth Apps → New.
   - Homepage: `http://localhost:4200`.
   - Callback: `http://localhost:8080/realms/turismo/broker/github/endpoint`.
2. Client ID + secret → `.env`. Reiniciar Keycloak.

**Claude Code:**
3. IdP `github` en realm `turismo`: `trustEmail=ON`, `syncMode=IMPORT`, first login = `first broker login`.
4. Mappers: *Hardcoded Group* → `/Clientes/Estandar`; *Hardcoded Attribute* `origen=github`.
5. `docs/07-github-idp.md` con capturas (sin secret).
6. Re-exportar realm, verificar placeholder.

**Aceptación**: botón "GitHub" aparece; usuario nuevo queda en Clientes/Estandar y puede reservar; secret no en el JSON.

### Fase 8 — Tema personalizado (rúbrica 8)
1. `keycloak/themes/turismo-peru/login/`:
   - `theme.properties`: `parent=keycloak`, `import=common/keycloak`, `styles=css/login.css css/turismo.css`, `locales=es,en`. (Si en 26.x conviene `keycloak.v2`, documentar.)
   - `resources/css/turismo.css`: paleta terracota `#C0502A`, verde andino `#2F6B4F`, arena `#F4EDE1`. Fondo con overlay. Logo en `#kc-header-wrapper`. Botón primario y de GitHub.
   - `resources/img/logo.svg` (propio).
   - `resources/img/fondo.jpg` (< 500 KB, libre, atribución en docs).
   - `messages/messages_es.properties`:
     - `loginTitle`, `loginAccountTitle` → "Bienvenido a TurismoPerú"
     - `doLogIn` → "Ingresar"
     - `invalidPasswordRegexPatternMessage` → "La contraseña debe incluir al menos un carácter especial: @ % & # . + / ! = ? ¿"
2. Asignar `loginTheme=turismo-peru` en ambos realms. `start-dev` desactiva caché.
3. `docs/08-tema-ui.md` con capturas: login, registro, forgot password, error de contraseña.

**Aceptación**: login, registro y "forgot password" con el tema en español. Contraseña sin especial muestra el mensaje personalizado.

### Fase 9 — Valor agregado (rúbrica 10)
Mínimo **3** para asegurar el punto. Prioridad por simplicidad:
1. **Auditoría**: activar User events (LOGIN, LOGIN_ERROR, CODE_TO_TOKEN) y Admin events. Captura del log. (5 min de configuración.)
2. **Brute force detection**: *Lockout temporarily* 5 fallos / 1 min / máx 15 min. Probar con un usuario de sacrificio aparte (T9). (5 min.)
3. **Service account B2B**: `partners-integracion` llama a E14 por client credentials (ya está en la matriz). Documentar. (0 min adicional si F4/F5 funcionan.)
4. *(Opcional)* **MFA OTP condicional** para `admin` (duplicar flujo browser; T8).
5. *(Opcional)* **Verificación de email con Mailpit** (añadir servicio en compose, SMTP del realm).

Cada uno documentado en `docs/10-valor-agregado.md` con captura.

**Aceptación**: 3 features demostrados con capturas. Fuerza bruta no bloquea usuarios de la matriz.

### Fase 10 — Postman (actividad b)
**Environment:** `kc_url=http://localhost:8080`, `api_url=http://localhost:8081`, secretos, `password=Turismo#2026a`, variables `token_<usuario>`.

**Colección `TurismoPeru-Keycloak`:**

| Carpeta | Contenido |
|---|---|
| 00 Discovery | `.well-known` de ambos realms |
| 01 Tokens | Password grant con `turismo-web` para cada usuario del realm `turismo` (script guarda tokens). Password grant de `pedro.partner` con `partners-portal`. Client credentials de `partners-integracion`. Refresh ok, refresh reutilizado (`invalid_grant`), logout. Token de `fiona` con `scope=openid pagos:write` → `token_fiona_pagos` |
| 02 Públicos | E1, E2 → 200 |
| 03 Privados | Filas de E3, E4, E7, E9, E10 |
| 04 Especiales | E5, E6, E8, E11 (dueño/no dueño, misma/otra agencia, con/sin scope, 409 por estado) |
| 05 Granular | RPT de `jorge`, decisión `export` para `jorge` (true) y `fiona` (403). E12/E13 por usuario |
| 06 Multi-realm | E14 con token de partner (200) y de `ana` (403) |

Cada request con `pm.test` de status. Celdas que cambian estado al final de su carpeta; agregar una repetición que espera 409.

Ejecutar: `newman run postman/TurismoPeru-Keycloak.postman_collection.json -e postman/local.postman_environment.json -r cli,htmlextra --reporter-htmlextra-export docs/evidencias/newman.html`.

**Aceptación**: Newman en verde desde estado limpio. Reporte en `docs/evidencias/`.

### Fase 11 — README y entrega (actividad b)
1. `README.md`:
   - Descripción del caso.
   - Diagrama Mermaid.
   - Requisitos: Docker, JDK 21, Maven 3.9+, Node 20+, Newman.
   - Ejecución en 5 pasos.
   - URLs y usuarios de prueba.
   - **Tabla rúbrica → evidencia** (10 filas, con archivo/captura).
   - Explicación de "número de veces a reutilizar" (ambas lecturas).
2. `git grep` de cada secreto real → vacío. JSONs con placeholders.
3. **Prueba de humo**: clonar en otra carpeta, seguir README, correr Newman. Debe funcionar.
4. Tag `v1.0-entrega`, push a GitHub.

**Aceptación**: humo pasa, tag en GitHub, Anexo B con capturas.

---

## 3. Matriz de pruebas

| Endpoint | sin token | ana | carlos | lucia (LIM01) | jorge (LIM01 sup.) | mario (CUS01) | olga | fiona | admin | partner (pedro / integración) |
|---|---|---|---|---|---|---|---|---|---|---|
| E1 `GET /public/tours` | 200 | 200 | 200 | 200 | 200 | 200 | 200 | 200 | 200 | 200 |
| E3 `GET /reservas/mias` | 401 | 200 (R-1002, R-1003) | 200 (R-1001, R-1004) | 403 | 403 | 403 | 403 | 403 | 200 | 403 |
| E4 `POST /reservas` | 401 | 201 | 201 | 403 | 403 | 403 | 403 | 403 | 201 | 403 |
| E5 `GET /reservas/R-1001` | 401 | 403 | 200 | 200 | 200 | 403 | 403 | 200 | 200 | 403 |
| E5 `GET /reservas/R-1002` | 401 | 200 | 403 | 403 | 403 | 200 | 403 | 200 | 200 | 403 |
| E6 `PUT /reservas/R-1002/cancelar` | 401 | 200 | 403 | 403 | 403 | 403 | 403 | 403 | 403 | 403 |
| E6 `PUT /reservas/R-1003/cancelar` | 401 | 409 | 403 | 403 | 403 | 403 | 403 | 403 | 403 | 403 |
| E7 `GET /agencia/reservas` | 401 | 403 | 403 | 200 (LIM01) | 200 (LIM01) | 200 (CUS01) | 403 | 403 | 200 (todas) | 403 |
| E8 `PUT /reservas/R-1001/confirmar` | 401 | 403 | 403 | 200 | 403 ¹ | 403 | 403 | 403 | 200 | 403 |
| E9 `POST /admin/tours` | 401 | 403 | 403 | 403 | 403 | 403 | 201 | 403 | 201 | 403 |
| E11 `POST /pagos` (R-1003) | 401 | 403 | 403 | 403 | 403 | 403 | 403 | **403 sin scope / 201 con `pagos:write`** | 201 ² | 403 |
| E12 `GET /reportes/ventas` | 401 | 403 | 403 | 403 | 200 | 403 | 403 | 200 | 200 | 401 ³ |
| E13 `GET /reportes/ventas/export` | 401 | 403 | 403 | 403 | 200 (en horario) / 403 (fuera) | 403 | 403 | 403 | 200 | 401 ³ |
| E14 `GET /partners/disponibilidad` | 401 | 403 | 403 | 403 | 403 | 403 | 403 | 403 | 403 | 200 |

¹ `jorge` no tiene `reservas.confirmar` (su subgrupo Supervision no lo asigna). Supervisor **ve pero no opera**.
² `admin` también necesita el scope `pagos:write` por la regla del endpoint.
³ La cadena de reportes solo confía en issuer `turismo` → token de partner da 401, no 403.

**Pruebas que cambian estado** (E4, E6, E8, E11): orden de la matriz, con datos recién sembrados. En Postman cada 2xx va una sola vez y se agrega una repetición que espera 409. `scripts/reset.sh` (o un bounce de la BD) antes de cada run completo.

---

## 4. Orden
```
F0 → F1 → F2 → F3 → F4 → F5 → F6
                ├→ F7 (necesita F2)
                └→ F8 (necesita F2)
F4–F8 → F9 → F10 → F11
```
**Re-exportar realms** al cerrar F2, F5, F7, F8, F9. Commit por fase.

---

## 5. Trampas

| # | Problema | Síntoma | Prevención |
|---|---|---|---|
| T1 | Issuer no coincide | 401 "iss claim not valid" | Backend y Angular en host, obtener tokens contra `http://localhost:8080` |
| T2 | Contraseña temporal | "Account is not fully set up" | `temporary: false` en credenciales |
| T3 | User profile incompleto (KC 26) | Igual que T2 o "Update profile" | Completar email, nombre, apellido, `emailVerified=true` |
| T4 | Default resource/policy/permission | PERMIT universal en reportes | Borrarlos al activar Authorization |
| T5 | Time policy en UTC | Horario equivocado | `TZ=America/Lima` y `-Duser.timezone=America/Lima` en Keycloak |
| T6 | `agencia_id` no llega | Claim ausente | Atributo en el **subgrupo hoja**. Verificar con Client scopes → Evaluate → *Generated access token* |
| T7 | `--import-realm` no sobrescribe | Cambios del JSON no aparecen | `docker compose down -v` antes de reimportar |
| T8 | OTP en direct grant | Postman deja de obtener tokens | Condición OTP solo en flujo browser copiado |
| T9 | Fuerza bruta bloquea prueba | 401 intermitentes en Newman | Probar con usuario aparte |
| T10 | Secretos en export | Secret real en commit | Reemplazar por `${…}` antes del commit; `git grep` |
| T11 | Script init de Postgres no corre | BD `turismo_app` no existe | Solo corre con volumen vacío. `down -v` |
| T12 | CORS | Navegador bloquea | Backend con origen `http://localhost:4200`; web origins en Keycloak |
| T13 | Audience inválida | 401 por audience | Mapper Audience → `turismo-api` en el scope dedicado de cada cliente que llama a la API |
| T14 | Versión policy-enforcer ≠ servidor | Errores de clase | Fijar misma versión |

---

## Anexo A — RACI backend
R = ejecuta · **A = responsable (una por fila)** · C = consultado · I = informado

| Endpoint | Clientes · `cliente` · ana, carlos | Agencias/*/Ventas · `agente` · lucia, mario | Agencias/Lima/Supervision · `supervisor-agencia` · jorge | Operaciones/Catalogo · `operador` · olga | Operaciones/Finanzas · `pagos.procesar`, `reportes.ver` · fiona | Administracion · `admin` · admin.turismo | Partners · `partner-agente` · pedro / rosa |
|---|---|---|---|---|---|---|---|
| Catálogo público (E1, E2) | I | I | I | C | – | **A** | I |
| Gestionar tours (E9, E10) | I | I | – | R | – | **A** | – |
| Crear reserva (E4) | R | I | – | – | – | **A** | – |
| Mis reservas (E3) | R | – | – | – | – | **A** | – |
| Ver detalle reserva (E5) | R (propia) | C (su agencia) | C (su agencia) | – | I | **A** | – |
| Cancelar reserva (E6) | R (propia, PENDIENTE) | I | I | – | – | **A** | – |
| Reservas de la agencia (E7) | – | R | **A** | – | – | C | – |
| Confirmar reserva (E8) | I | R | **A** | – | I | C | – |
| Registrar pago (E11) | I | I | – | – | R | **A** | – |
| Reporte de ventas (E12) | – | – | R | – | R | **A** | – |
| Exportar ventas (E13) | – | – | R (en horario) | – | I | **A** | – |
| Disponibilidad B2B (E14) | – | – | – | C | – | I | R (pedro, integración) · **A** (rosa) |

En `docs/04-raci-backend.md`, separar la columna de Partners en dos (Nacionales → R, Partners-Admin → A).

---

## Anexo B — Checklist de evidencias (`docs/evidencias/`)

| Ítem | Capturas mínimas |
|---|---|
| 1 | `docker compose ps` healthy · tablas en PostgreSQL · persistencia tras reinicio |
| 2 | Realms, clientes, roles de realm y de cliente, composites, scopes, árbol de grupos, usuarios, token decodificado (jwt.io) con roles, scope, groups, agencia_id |
| 3 | Diagramas · RACI backend |
| 4 | `SecurityConfig` · `application.yml` · `SecurityConstants` · métodos con `@Pre/PostAuthorize` · 403 de `@PostAuthorize` en Postman |
| 5 | Diagramas · RACI frontend · menú de 3 usuarios distintos · redirección al login · 403 amable |
| 6 | Resources/scopes/policies/permissions · Evaluate PERMIT y DENY · RPT decodificado · export 200 (jorge) y 403 (fiona) · prueba fuera de horario |
| 7 | OAuth App (sin secret) · IdP + mappers · botón GitHub · usuario creado en Clientes/Estandar |
| 8 | Login, registro, forgot password y error de contraseña con el tema |
| 9 | Localización · password policy · Tokens/Sessions · `invalid_grant` al reusar refresh |
| 10 | 3 features con captura cada uno |
| b | Repo en GitHub · reporte Newman HTML · `keycloak/realms/` |
