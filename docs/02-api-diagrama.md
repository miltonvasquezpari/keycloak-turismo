# 02 · Diagrama de la API

## Arquitectura de componentes

```mermaid
flowchart LR
  subgraph Usuario["Usuario final"]
    NAV[Navegador]
    POSTMAN[Postman / Newman]
  end
  subgraph PartnerExt["Sistema partner (B2B)"]
    PART[Backend del partner]
  end
  subgraph Docker["Docker Compose · localhost"]
    KC[("Keycloak 26<br/>realms: turismo, agencias-partners")]
    PG[("PostgreSQL 16<br/>BDs: keycloak, turismo_app")]
  end
  subgraph App["App TurismoPerú"]
    NG["Angular 18<br/>turismo-web<br/>:4200"]
    API["Spring Boot 3.5<br/>turismo-api<br/>:8081"]
  end

  NAV -->|OIDC login PKCE| KC
  NAV -->|catalogo + reservas con bearer| API
  NG --- NAV
  POSTMAN -->|password / client_credentials| KC
  POSTMAN -->|API con bearer| API
  PART -->|client_credentials| KC
  PART -->|disponibilidad con bearer| API
  API -->|validar JWT via JWKS<br/>Policy Enforcer para /api/reportes/**| KC
  API -->|JPA| PG
  KC --> PG
```

**Puntos clave**
- La API **nunca** valida contraseñas; valida JWTs firmados por Keycloak usando JWKS.
- La API acepta tokens de **dos issuers** (multi-realm): `turismo` para la operación interna y `agencias-partners` para B2B. Resolución por `JwtIssuerAuthenticationManagerResolver`.
- Audience `turismo-api` requerida en cada token (mapper en el scope dedicado del cliente — T13).
- `/api/reportes/**` tiene un `SecurityFilterChain` propio (`@Order(1)`) que delega la decisión a Keycloak Authorization Services via `ServletPolicyEnforcerFilter`.

## Mapa de los 14 endpoints

```mermaid
flowchart TB
  subgraph Publico["Publico (sin token)"]
    E1["E1 · GET /api/public/tours"]
    E2["E2 · GET /api/public/tours/{id}"]
  end
  subgraph Privado["Privado (JWT + rol/scope)"]
    E3["E3 · GET /api/reservas/mias<br/>ROLE_cliente"]
    E4["E4 · POST /api/reservas<br/>ROLE_cliente + SCOPE_reservas:write"]
    E7["E7 · GET /api/agencia/reservas<br/>ROLE_agente, filtra por agencia_id"]
    E9["E9 · POST /api/admin/tours<br/>ROLE_tours.gestionar"]
    E10["E10 · PUT /api/admin/tours/{id}<br/>ROLE_tours.gestionar"]
    E14["E14 · GET /api/partners/disponibilidad<br/>token de agencias-partners + ROLE_disponibilidad.consultar"]
  end
  subgraph Especial["Especial (@Pre/PostAuthorize sobre el recurso)"]
    E5["E5 · GET /api/reservas/{id}<br/>@PostAuthorize: dueno / agencia / admin / finanzas"]
    E6["E6 · PUT /api/reservas/{id}/cancelar<br/>@PreAuthorize: dueno · 409 si no PENDIENTE"]
    E8["E8 · PUT /api/reservas/{id}/confirmar<br/>ROLE_reservas.confirmar + mismaAgencia(#id)"]
    E11["E11 · POST /api/pagos<br/>ROLE_pagos.procesar + SCOPE_pagos:write · 409 si no CONFIRMADA"]
  end
  subgraph Granular["Granular (Keycloak Authorization Services)"]
    E12["E12 · GET /api/reportes/ventas<br/>scope: view"]
    E13["E13 · GET /api/reportes/ventas/export<br/>scope: export (CSV, horario laboral)"]
  end

  classDef pub      fill:#E8F5E9,stroke:#2E7D32,color:#1B5E20
  classDef priv     fill:#E3F2FD,stroke:#1565C0,color:#0D47A1
  classDef esp      fill:#FFF3E0,stroke:#E65100,color:#BF360C
  classDef gran     fill:#F3E5F5,stroke:#6A1B9A,color:#4A148C
  class E1,E2 pub
  class E3,E4,E7,E9,E10,E14 priv
  class E5,E6,E8,E11 esp
  class E12,E13 gran
```

## Cuadro de reglas por endpoint

| #  | Metodo + ruta                              | Tipo       | Rol / Scope / Regla                                                   |
|----|--------------------------------------------|------------|-----------------------------------------------------------------------|
| E1 | GET  `/api/public/tours`                   | Publico    | `permitAll`                                                           |
| E2 | GET  `/api/public/tours/{id}`              | Publico    | `permitAll`                                                           |
| E3 | GET  `/api/reservas/mias`                  | Privado    | `ROLE_cliente`                                                        |
| E4 | POST `/api/reservas`                       | Privado    | `ROLE_cliente` AND `SCOPE_reservas:write`                             |
| E5 | GET  `/api/reservas/{id}`                  | Especial   | `@PostAuthorize`: dueno / agente de la misma agencia / admin / finanzas |
| E6 | PUT  `/api/reservas/{id}/cancelar`         | Especial   | `@PreAuthorize("hasRole('cliente') and @reservaSecurity.esDueno(#id,auth)")` · 409 si no PENDIENTE |
| E7 | GET  `/api/agencia/reservas`               | Privado    | `ROLE_agente` (filtra por `agencia_id` del token)                     |
| E8 | PUT  `/api/reservas/{id}/confirmar`        | Especial   | `ROLE_reservas.confirmar` AND (admin OR `@reservaSecurity.mismaAgencia(#id,auth)`) |
| E9 | POST `/api/admin/tours`                    | Privado    | `ROLE_tours.gestionar`                                                |
| E10| PUT  `/api/admin/tours/{id}`               | Privado    | `ROLE_tours.gestionar`                                                |
| E11| POST `/api/pagos`                          | Especial   | `ROLE_pagos.procesar` AND `SCOPE_pagos:write` · 409 si no CONFIRMADA  |
| E12| GET  `/api/reportes/ventas`                | Granular   | Policy Enforcer, scope `view`                                         |
| E13| GET  `/api/reportes/ventas/export`         | Granular   | Policy Enforcer, scope `export` (horario laboral L–S 08–20)            |
| E14| GET  `/api/partners/disponibilidad`        | Privado    | Token de realm `agencias-partners` + `ROLE_disponibilidad.consultar`  |

## Flujo OIDC (resumen)

```mermaid
sequenceDiagram
  participant U as Usuario
  participant NG as Angular
  participant KC as Keycloak
  participant API as Spring Boot
  U->>NG: abre /mis-reservas
  NG->>NG: authGuard: ¿tengo token?
  NG->>KC: Authorization Code + PKCE
  U->>KC: credenciales (o GitHub IdP)
  KC-->>NG: code → access_token + id_token + refresh_token
  NG->>API: GET /api/reservas/mias (Bearer)
  API->>KC: valida JWT con JWKS del issuer
  API->>API: SecurityFilterChain + @PreAuthorize
  API-->>NG: 200 [reservas]
  Note over NG,KC: cada ~4 min: silent refresh
  Note over KC: refreshTokenMaxReuse=0 → nuevo refresh en cada uso
```
