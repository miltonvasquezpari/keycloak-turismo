# CLAUDE.md — reglas del proyecto keycloak-turismo

## Contexto
Trabajo final del curso *Security with Keycloak* (Galaxy). Detalle en `PLAN.md`.

**Principio rector**: el alcance se limita a lo que pide la rúbrica. **No añadir** Flyway, Swagger, tests unitarios formales, scopes técnicos extra, clientes extra, endpoints extra ni "mejoras de ingeniería" que no sumen puntos directos. Si falta algo que la rúbrica no exige pero es necesario para que algo funcione, documentarlo brevemente y hacerlo con lo mínimo.

Fuentes de verdad, en orden:
1. "Enunciado original (Galaxy)" en `PLAN.md`.
2. §1 Modelo de seguridad (nombres exactos).
3. §3 Matriz de pruebas (comportamiento esperado).
4. §5 Trampas conocidas (T1–T14, leer antes de cada fase).

Si una API cambió en la versión instalada (p.ej. `keycloak-angular`), aplicar la alternativa más cercana y anotarlo en el `docs/` de la fase.

## Forma de trabajo
- Ejecutar **solo la fase pedida**.
- Al empezar la fase, crear lista de tareas.
- Al terminar:
  1. Verificar criterios de aceptación con comandos reales (`curl`, `mvn`, `newman`, capturas).
  2. Comparar con el texto del componente de la rúbrica.
  3. Si tocó Keycloak: exportar realms, reemplazar secretos por `${…}`, revisar `git diff`.
  4. Commit `feat(fase-N): <resumen>`.
- No avanzar si una prueba falla.
- Verificar versiones reales en Maven Central / npm:
  - Imagen Keycloak 26.x.
  - `keycloak-policy-enforcer` **igual** al servidor.
  - `keycloak-angular` / `keycloak-js` compatibles.
- Tareas **manuales** (OAuth App en GitHub, crear repo remoto) se piden a Milton con pasos exactos.

## Stack y puertos
- Keycloak 26.x `start-dev --import-realm` en **8080** (health **9000**).
- PostgreSQL 16 en **5433** → 5432. BDs: `keycloak`, `turismo_app`.
- Backend Spring Boot 3.5.x / Java 21 / Maven en **8081**. Paquete `pe.turismo.api`. **Sin Flyway** (`ddl-auto=update` + `data.sql`). **Sin Swagger**. **Sin tests unitarios formales** (opcionales si sobra tiempo).
- Frontend Angular 18+ standalone en **4200**.
- Mailpit **opcional** (solo si F9 incluye verificación de email).
- TZ `America/Lima` en todos.

## Alcance explícito
- **4 clientes totales** (2 por realm). No crear más.
- **14 endpoints** (E1–E14). No crear más.
- **4 client scopes** funcionales (`reservas:write`, `pagos:write`, `reportes:read`, `disponibilidad:read`). Audience y mappers de atributos van en el **scope dedicado** del cliente (`turismo-api-dedicated`, existente), no en scopes nuevos.
- **F9**: 3 features suficientes (auditoría, brute force, B2B service account).

## Seguridad y secretos
- `.env` nunca versionado. `.env.example` como plantilla.
- JSONs de realm → placeholders `${TURISMO_API_SECRET}`, `${PARTNERS_INTEGRACION_SECRET}`, `${GITHUB_CLIENT_ID}`, `${GITHUB_CLIENT_SECRET}`.
- Usuarios de prueba: contraseña `Turismo#2026a`, **no temporal**, email + nombre + apellido + `emailVerified=true`.
- `turismo-web` y `partners-portal` con direct grants ON **solo en dev**. Documentarlo.

## Convenciones
- Dominio, UI y mensajes en español.
- Authorities Spring: `ROLE_<rol>` para roles de realm y de cliente (clientes listados en `turismo.security.role-clients`). `SCOPE_<scope>` para scopes.
- Reglas de rutas en `application.yml` bajo `turismo.security`. Nombres en `SecurityConstants`.
- `@PreAuthorize` en servicios. `@PostAuthorize` solo para E5 (detalle de reserva).
- Diagramas Mermaid en `docs/`.
- RACI: una sola "A" por fila.
- Capturas en `docs/evidencias/item-NN-descripcion.png`.

## Comandos útiles
- Levantar: `docker compose up -d`
- Reset total: `docker compose down -v && docker compose up -d`
- Backend: `cd backend && mvn spring-boot:run`
- Frontend: `cd frontend && npm start`
- Token rápido:
  `curl -s -d client_id=turismo-web -d grant_type=password -d username=ana.cliente -d 'password=Turismo#2026a' -d scope=openid http://localhost:8080/realms/turismo/protocol/openid-connect/token`
- Newman:
  `newman run postman/TurismoPeru-Keycloak.postman_collection.json -e postman/local.postman_environment.json -r cli,htmlextra --reporter-htmlextra-export docs/evidencias/newman.html`
