# 09 · Configuración del realm (rúbrica 9)

Este documento se completa en la **Fase 2** con la política de contraseñas, idioma y configuración de tokens/sesiones. En **Fase 1** solo se cubre la instalación del motor.

---

## Instalación

### Comandos
```bash
cp .env.example .env       # ajustar secretos si se requiere
docker compose up -d       # levanta postgres y keycloak
docker compose ps          # ambos deben quedar healthy
```

Para reiniciar desde cero (p. ej. tras cambiar `.env` o un realm JSON):
```bash
docker compose down -v && docker compose up -d
```

### Componentes

| Servicio   | Imagen                            | Puerto host | Rol                                                   |
|------------|-----------------------------------|-------------|-------------------------------------------------------|
| postgres   | `postgres:16-alpine`              | 5433 → 5432 | Persistencia de Keycloak y de la app                  |
| keycloak   | `quay.io/keycloak/keycloak:26.0.7`| 8080, 9000  | Identity Provider (modo `start-dev --import-realm`)   |

### Por qué PostgreSQL
- El enunciado pide BD persistente (PostgreSQL, MySQL u otros).
- Postgres es la BD oficialmente recomendada por Keycloak para producción, así que la configuración de dev se traslada sin fricción.
- La misma instancia aloja una segunda BD (`turismo_app`) usada por el backend (Fase 4), creada por `postgres/init/01-app.sh` cuando el volumen está vacío.
- Volumen nombrado `pg_data` → `down && up` conserva realms, usuarios y datos de la app.

### Modo dev vs. producción
Esta demo usa `start-dev` por simplicidad: HTTP, hostname laxo, caché de temas desactivada, re-import automático al arrancar. Para producción habría que:
- Correr `kc.sh build` + `start --optimized`.
- Habilitar HTTPS (`KC_HTTPS_*`) y definir `KC_HOSTNAME`.
- `KC_HOSTNAME_STRICT=true` y proxy TLS por delante (Traefik / Nginx).
- Reemplazar el admin bootstrap por un usuario personal creado tras el primer arranque.

### Variables sensibles
Todos los secretos viven en `.env` (ignorado por git) y se inyectan al contenedor. Los JSON de realms (Fase 2) solo contienen placeholders `${TURISMO_API_SECRET}`, `${PARTNERS_INTEGRACION_SECRET}`, `${GITHUB_CLIENT_ID}`, `${GITHUB_CLIENT_SECRET}` que Keycloak resuelve al importar.

### Health
- Postgres: `pg_isready` en el propio contenedor.
- Keycloak: endpoint `GET /health/ready` en el puerto de management `9000` (habilitado con `KC_HEALTH_ENABLED=true`). El healthcheck del compose abre un socket TCP a `localhost:9000` para evitar depender de `curl` dentro de la imagen.

---

---

## Realms y configuración (Fase 2)

### Realms creados
- **`turismo`** (TurismoPerú): clientes, agencias, operaciones, finanzas, admin.
- **`agencias-partners`** (TurismoPerú Partners): B2B, portal web y service account.

Bootstrap reproducible en `keycloak/bootstrap/`:
- `seed-turismo.sh`  — realm `turismo`: 2 clientes, 5 roles de realm + composites, 4 roles de cliente, 3 scopes con mappers, árbol de grupos, 8 usuarios.
- `seed-partners.sh` — realm `agencias-partners`: 2 clientes, 2 roles + composite, 1 rol de cliente, 1 scope, árbol de grupos, 2 usuarios + service account.
- `configure-realms.sh` — aplica a ambos: idioma, política de contraseña, tiempos de token.

Los secretos reales viven en `.env` y nunca en los JSON (T10).

### Idioma
En ambos realms:
```
internationalizationEnabled = true
supportedLocales            = [es, en]
defaultLocale               = es
```
Cada usuario tiene `locale=es` para que la UI de Keycloak le aparezca en español sin importar el navegador.

### Política de contraseña
Expresión aplicada en ambos realms:
```
length(10) and upperCase(1) and lowerCase(1) and digits(1)
and regexPattern(^(?=.*[@%&#.+/!=?¿]).*$)
and passwordHistory(3) and notUsername
```
- `regexPattern` fuerza al menos uno de los especiales **exactos** del enunciado (`@%&#.+/!=?¿`); `specialChars(1)` aceptaría cualquiera.
- `passwordHistory(3)` impide reusar las últimas 3.
- `notUsername` impide que la contraseña sea el username.

Validación:
- `Abcdefghi1` → **rechazada** (`invalidPasswordRegexPatternMessage`).
- `Turismo#2026a` → aceptada.

### Tokens y sesiones
| Parámetro               | Valor   | Significado |
|-------------------------|---------|-------------|
| `accessTokenLifespan`   | 300 s   | 5 min — el frontend hace refresh silencioso antes |
| `ssoSessionIdleTimeout` | 1800 s  | 30 min de inactividad y Keycloak corta la sesión |
| `ssoSessionMaxLifespan` | 36000 s | 10 h máximas de sesión aunque haya actividad |
| `revokeRefreshToken`    | true    | cada refresh usa invalida el anterior |
| `refreshTokenMaxReuse`  | 0       | cero reusos permitidos |

Validación: reusar el mismo refresh → `HTTP 400 invalid_grant` con mensaje `Maximum allowed refresh token reuse exceeded`.

### "Número de veces a reutilizar" (ambigüedad del enunciado)
El enunciado no precisa si se refiere a contraseñas o a refresh tokens. Se cubren las dos lecturas:
- **Contraseñas:** `passwordHistory(3)` → no se pueden reusar las últimas 3.
- **Refresh tokens:** `refreshTokenMaxReuse=0` + `revokeRefreshToken=true` → cada refresh invalida el anterior.

### Exportación e importación
Flujo que preserva TODO (realms, clientes, grupos, usuarios con credenciales, scopes, mappers, authorization settings):
```bash
docker compose stop keycloak
docker compose run --rm keycloak export \
  --dir /opt/keycloak/data/import \
  --realm turismo --users realm_file
docker compose run --rm keycloak export \
  --dir /opt/keycloak/data/import \
  --realm agencias-partners --users realm_file
# Reemplazar secretos reales por placeholders (T10)
sed -i 's|"secret" : "<valor>"|"secret" : "${TURISMO_API_SECRET}"|' \
  keycloak/realms/turismo-realm.json
sed -i 's|"secret" : "<valor>"|"secret" : "${PARTNERS_INTEGRACION_SECRET}"|' \
  keycloak/realms/agencias-partners-realm.json
# Importar al arrancar (gracias a `--import-realm` en el compose)
docker compose up -d
```

Nota sobre authorization settings: el export incluye `authorizationSettings` del cliente `turismo-api`. Se eliminan del JSON los defaults `Default Policy` (JavaScript), `Default Permission` y `Default Resource`, porque:
- `scripts` está deshabilitado por seguridad en Keycloak moderno → el import falla con `ERROR: Script upload is disabled`.
- La trampa T4 del PLAN exige borrarlos de todos modos antes de la Fase 5.

Keycloak resuelve `${TURISMO_API_SECRET}` / `${PARTNERS_INTEGRACION_SECRET}` dentro del JSON usando las env vars del contenedor.
