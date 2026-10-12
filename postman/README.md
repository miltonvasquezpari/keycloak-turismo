# Postman / Newman — TurismoPerú

Colección automatizada para probar los 14 endpoints E1..E14 contra
Keycloak + backend corriendo en local.

## Archivos

- `TurismoPeru-Keycloak.postman_collection.json` — colección con ~35 requests
  en 7 carpetas (Discovery, Tokens, Públicos, Privados, Especiales,
  Granular, Multi-realm). Cada request tiene al menos un `pm.test`.
- `local.postman_environment.json` — variables locales. Los campos
  `turismo_api_secret` y `partners_integracion_secret` están vacíos
  por seguridad; se rellenan antes de correr (ver abajo).

## Preparación

1. Backend y Keycloak arriba:
   ```bash
   docker compose up -d
   cd backend && mvn spring-boot:run &
   ```
2. Variables del `.env` necesarias en la colección:
   `PARTNERS_INTEGRACION_SECRET` (para la carpeta 01 client_credentials).

## Correr con Newman (desde CLI)

### Opción A — editar el environment file y correr
Abre `postman/local.postman_environment.json`, pega los secretos
reales en los campos vacíos, y:
```bash
newman run postman/TurismoPeru-Keycloak.postman_collection.json \
  -e postman/local.postman_environment.json \
  -r cli,htmlextra --reporter-htmlextra-export docs/evidencias/newman.html
```

### Opción B — pasar secretos por línea de comandos (no toca archivos)
```bash
set -a; source .env; set +a
newman run postman/TurismoPeru-Keycloak.postman_collection.json \
  -e postman/local.postman_environment.json \
  --env-var "partners_integracion_secret=$PARTNERS_INTEGRACION_SECRET" \
  --env-var "turismo_api_secret=$TURISMO_API_SECRET" \
  -r cli,htmlextra --reporter-htmlextra-export docs/evidencias/newman.html
```

## Correr desde Postman (GUI)

1. Importa ambos archivos.
2. Selecciona el environment "TurismoPeru Local" arriba a la derecha.
3. Edita el environment (ícono del ojo) y pega los secretos reales
   en `turismo_api_secret` y `partners_integracion_secret`.
4. Clic derecho en la colección → **Run**. Marca "Keep variable
   values" (así los tokens de la carpeta 01 quedan disponibles para
   las siguientes).

## Qué cubre

| Carpeta | Endpoints / casos |
|---|---|
| 00 Discovery | `.well-known` de ambos realms |
| 01 Tokens | password grant para 9 usuarios, client_credentials de partners, refresh ok, scope `pagos:write` para fiona |
| 02 Públicos | E1, E2 |
| 03 Privados | E3, E4, E7 (lucia y mario), E9, E10 + E3 sin token (401) + E7 ana sin rol (403) |
| 04 Especiales | E5 dueño/no dueño, E6 cancelar dueño/no dueño, E8 agente misma/otra agencia, E11 fiona con/sin scope + 409 por estado |
| 05 Granular | E12/E13 admin ok, E13 lucia 403, E12 fiona 200 |
| 06 Multi-realm | E14 con partner_cc, con pedro, con ana 403 |

## Notas

- Las carpetas 03/04 crean una reserva en E4, luego la usan en E5 y
  E6. Si corres solo una carpeta aislada, esos tests fallan por falta
  de dato encadenado.
- E8 confirma `R-1001`; si ya está CONFIRMADA de un run previo,
  Keycloak sigue dando 200 pero el backend puede devolver 409 por
  estado. En ese caso resetea datos con
  `docker compose down -v && docker compose up -d`.
- E11 marca R-1003 como PAGADA. Para re-correr limpio, igual.
