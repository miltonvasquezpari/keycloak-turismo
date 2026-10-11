# Fase 7 — GitHub como Identity Provider

Realm `turismo` acepta login con cuenta GitHub, además del usuario/
contraseña propio. El usuario que entra por primera vez queda en el
grupo `/Clientes/Estandar` con el atributo `origen=github`.

## 1. OAuth App en GitHub (manual, Milton)

GitHub → **Settings → Developer settings → OAuth Apps → New**.

| Campo | Valor |
|---|---|
| Application name | `TurismoPeru Galaxy` |
| Homepage URL | `http://localhost:4200` |
| Redirect URI (antes "Authorization callback URL") | `http://localhost:8080/realms/turismo/broker/github/endpoint` |
| Allow wildcard matching | OFF |
| Enable Device Flow | OFF |

Al registrarla GitHub entrega:

- **Client ID** — público, se guarda como `GITHUB_CLIENT_ID`.
- **Client Secret** — se genera con *Generate a new client secret*,
  solo se muestra una vez; se guarda como `GITHUB_CLIENT_SECRET`.

Ambos van al archivo local `.env` (ignorado por git); el realm JSON
los referencia como placeholders `${GITHUB_CLIENT_ID}` /
`${GITHUB_CLIENT_SECRET}` que Keycloak resuelve al importar.

## 2. IdP en el realm turismo

Creado vía `kcadm.sh` dentro del contenedor y persistido en
`keycloak/realms/turismo-realm.json`:

```json
"identityProviders": [{
  "alias": "github",
  "providerId": "github",
  "enabled": true,
  "trustEmail": true,
  "firstBrokerLoginFlowAlias": "first broker login",
  "config": {
    "syncMode": "IMPORT",
    "clientId": "${GITHUB_CLIENT_ID}",
    "clientSecret": "${GITHUB_CLIENT_SECRET}",
    "defaultScope": "user:email read:user",
    "useJwksUrl": "true"
  }
}]
```

- `trustEmail=true`: el email de GitHub se da por verificado, el
  usuario no entra en flujo *verify email*.
- `syncMode=IMPORT`: Keycloak copia los datos del perfil GitHub al
  usuario local la primera vez; cambios posteriores en GitHub no se
  propagan.
- `firstBrokerLoginFlowAlias="first broker login"`: flujo por defecto
  de Keycloak para la primera entrada por un IdP externo (ofrece
  vincular con un usuario existente si el email coincide).

## 3. Mappers (hardcoded)

Se aplican a cualquier usuario que entre por GitHub:

| Mapper | Tipo | Config |
|---|---|---|
| `grupo-clientes-estandar` | `hardcoded-group-idp-mapper` | `group=/Clientes/Estandar` |
| `origen-github` | `hardcoded-attribute-idp-mapper` | `attribute=origen`, `attribute.value=github` |

El primer mapper garantiza que el usuario llega con **rol `cliente`**
(ese rol lo da el grupo `Clientes`) — puede entrar directo a
`/mis-reservas` y reservar sin que un admin lo coloque a mano. El
segundo etiqueta el origen para auditoría futura (Fase 9).

## 4. Variables de entorno

`.env` (NO versionado):

```
GITHUB_CLIENT_ID=<del paso 1>
GITHUB_CLIENT_SECRET=<del paso 1>
```

`.env.example` muestra el formato con `cambiar-en-local`.

`docker-compose.yml` ya propaga ambas al contenedor Keycloak como
variables de entorno, de modo que al importar el realm JSON los
placeholders se resuelven con los valores reales.

## 5. Prueba manual

1. `docker compose up -d` (si Keycloak no está arriba) o `docker
   restart turismo-keycloak` si solo cambiaron las env.
2. En `http://localhost:4200` → **Iniciar sesión** → aparece botón
   **GitHub** en la pantalla de Keycloak.
3. Autorizar la OAuth App en GitHub.
4. Keycloak crea el usuario, lo mete en `/Clientes/Estandar` con
   `origen=github`, lo marca con email verificado y vuelve al
   frontend ya autenticado.
5. En **Mis reservas** se puede listar y crear reservas igual que
   `ana.cliente`.

## 6. Verificación de que no se filtra el secret

```
grep -nE 'Ov23|clientSecret.*[0-9a-f]{30}' keycloak/realms/turismo-realm.json
# -> ninguno; solo aparecen los placeholders ${GITHUB_CLIENT_ID|SECRET}
```

El `.env` está en `.gitignore` desde Fase 0.
