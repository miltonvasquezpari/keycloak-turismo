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

## Pendientes de Fase 2
- Idioma español + política de contraseñas (10 chars, 1 mayús, 1 minús, 1 dígito, especial `@%&#.+/!=?¿`).
- Tiempo de access/refresh token y reutilización.
- Exportación de los dos realms a JSON (en `keycloak/realms/`).
