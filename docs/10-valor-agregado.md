# Fase 9 — Valor agregado (rúbrica 10)

Tres features de seguridad adicional habilitadas en el realm `turismo`.
Cada una documentada aquí; evidencias con comandos ejecutados en
`docs/evidencias/fase9-*.txt`.

## 1. Auditoría de eventos (User events + Admin events)

Keycloak captura y persiste eventos para investigación forense.

**Configuración** (persistida en `turismo-realm.json`):

| Clave | Valor |
|---|---|
| `eventsEnabled` | `true` |
| `eventsExpiration` | `2592000` (30 días) |
| `eventsListeners` | `["jboss-logging"]` |
| `enabledEventTypes` | LOGIN, LOGIN_ERROR, LOGOUT, LOGOUT_ERROR, CODE_TO_TOKEN(+ERROR), REFRESH_TOKEN(+ERROR), REGISTER(+ERROR), IDENTITY_PROVIDER_LOGIN(+ERROR), CLIENT_LOGIN(+ERROR) |
| `adminEventsEnabled` | `true` |
| `adminEventsDetailsEnabled` | `true` |

**Consulta:**
```bash
kcadm.sh get events       -r turismo --query 'max=10'
kcadm.sh get events/admin -r turismo --query 'max=10'
```

Evidencia: `docs/evidencias/fase9-auditoria.txt`.

## 2. Brute force detection

Protección automática contra ataques de fuerza bruta por usuario.

**Configuración** (`turismo-realm.json`):

| Clave | Valor | Significado |
|---|---|---|
| `bruteForceProtected` | `true` | activa el detector |
| `failureFactor` | `5` | fallos antes de bloquear |
| `waitIncrementSeconds` | `60` | lockout inicial = 1 min |
| `maxFailureWaitSeconds` | `900` | lockout máximo = 15 min |
| `maxDeltaTimeSeconds` | `43200` | ventana para contar fallos (12 h) |
| `permanentLockout` | `false` | no bloquea permanentemente |
| `minimumQuickLoginWaitSeconds` | `60` | anti-quick-login |

**Usuario de sacrificio** `test.bruteforce` (grupo `/Clientes/Estandar`,
password `Turismo#2026a`): se usa para probar sin arriesgar
`ana`/`lucia`/etc.

Evidencia: `docs/evidencias/fase9-brute-force.txt`.

Desbloqueo manual:
```bash
kcadm.sh delete attack-detection/brute-force/users/<id> -r turismo
```

## 3. Service account B2B (`partners-integracion` → E14)

Flujo máquina-a-máquina sin usuario humano, usando OAuth 2.0 Client
Credentials. Permite que un partner integrado consulte la
disponibilidad desde su backend sin login interactivo.

**Pieza ya existente** desde Fase 4 (multi-issuer) y Fase 5 (reglas de
ruta); aquí se documenta y verifica en vivo.

**Cliente** `partners-integracion` (realm `agencias-partners`):
- `serviceAccountsEnabled: true`
- `standardFlowEnabled: false`, `directAccessGrantsEnabled: false`
- Audience `turismo-api` por *audience mapper* (clave para que lo
  acepte el backend en multi-issuer).
- Rol de cliente `disponibilidad.consultar`.

**Flujo:**
```
POST /realms/agencias-partners/protocol/openid-connect/token
  client_id=partners-integracion&client_secret=***&grant_type=client_credentials
-> access_token con aud=turismo-api

GET /api/partners/disponibilidad?tourId=1&fecha=2026-12-01
Authorization: Bearer <access_token>
-> 200 OK (regla de ruta exige ROLE_disponibilidad.consultar)
```

Evidencia: `docs/evidencias/fase9-service-account-b2b.txt`.

## Comandos útiles

```bash
# Reset del lockout de un usuario:
kcadm.sh delete attack-detection/brute-force/users/<id> -r turismo

# Limpieza de eventos antes de la demo:
kcadm.sh delete events -r turismo
```
