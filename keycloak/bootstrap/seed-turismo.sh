#!/bin/bash
# Fase 2 · Item A — crea el realm `turismo` con todo su contenido estructural:
# clientes, roles de realm y de cliente, client scopes con mappers, grupos
# con atributos y usuarios de prueba. La configuracion ITEM 9 (politica de
# clave, idioma, tiempos de token) la aplica otro script.
#
# Idempotencia: NO. Si corre dos veces falla por entidades duplicadas.
# Para re-sembrar:
#   docker exec turismo-keycloak /opt/keycloak/bin/kcadm.sh delete realms/turismo
# y volver a correr.
#
# Se ejecuta DESDE EL HOST contra el contenedor `turismo-keycloak`.
# Requiere python3 en el host.
#
# Uso:
#   keycloak/bootstrap/seed-turismo.sh <ADMIN_USER> <ADMIN_PASS> <TURISMO_API_SECRET>
set -euo pipefail

KC_USER="${1:-admin}"
KC_PASS="${2:-admin}"
API_SECRET="${3:-cambiar-en-local}"
PASSWORD="Turismo#2026a"
CONTAINER="turismo-keycloak"
KCADM="docker exec -i $CONTAINER /opt/keycloak/bin/kcadm.sh"

# --------------------------------------------------------------------------
# Helpers: filtran por nombre en el HOST porque `kcadm -q name=X` NO filtra;
# devuelve el primero de la lista. Hay que listar todo y filtrar localmente.
# --------------------------------------------------------------------------
find_id_by() {   # find_id_by <endpoint> <campo> <valor>
  local endpoint="$1" field="$2" value="$3"
  $KCADM get "$endpoint" -r turismo 2>/dev/null | python3 -c "
import sys,json
fld,val='$field','$value'
for e in json.load(sys.stdin):
    if e.get(fld)==val:
        print(e['id']); break
"
}

echo "== Login =="
$KCADM config credentials --server http://localhost:8080 \
  --realm master --user "$KC_USER" --password "$KC_PASS"

# --------------------------------------------------------------------------
# 1. Realm
# --------------------------------------------------------------------------
echo "== Realm turismo =="
$KCADM create realms \
  -s realm=turismo \
  -s displayName='TurismoPerú' \
  -s displayNameHtml='<strong>TurismoPerú</strong>' \
  -s enabled=true \
  -s registrationAllowed=true \
  -s resetPasswordAllowed=true \
  -s loginWithEmailAllowed=true

# --------------------------------------------------------------------------
# 2. Roles de realm
# --------------------------------------------------------------------------
echo "== Roles de realm =="
for r in cliente agente operador supervisor-agencia admin; do
  $KCADM create roles -r turismo -s "name=$r"
done

# --------------------------------------------------------------------------
# 3. Clientes
# --------------------------------------------------------------------------
echo "== Cliente turismo-api (confidencial, Authorization ON) =="
$KCADM create clients -r turismo \
  -s clientId=turismo-api \
  -s name='TurismoPerú API' \
  -s publicClient=false \
  -s serviceAccountsEnabled=true \
  -s authorizationServicesEnabled=true \
  -s standardFlowEnabled=false \
  -s directAccessGrantsEnabled=false \
  -s "secret=$API_SECRET"

echo "== Cliente turismo-web (publico, PKCE, direct grants en dev) =="
$KCADM create clients -r turismo \
  -s clientId=turismo-web \
  -s name='TurismoPerú Web' \
  -s publicClient=true \
  -s standardFlowEnabled=true \
  -s directAccessGrantsEnabled=true \
  -s 'redirectUris=["http://localhost:4200/*"]' \
  -s 'webOrigins=["http://localhost:4200"]' \
  -s 'attributes."pkce.code.challenge.method"=S256'

API_ID=$(find_id_by clients clientId turismo-api)
WEB_ID=$(find_id_by clients clientId turismo-web)
echo "   turismo-api id = $API_ID"
echo "   turismo-web id = $WEB_ID"

# --------------------------------------------------------------------------
# 4. Roles de cliente en turismo-api
# --------------------------------------------------------------------------
echo "== Roles de cliente turismo-api =="
for r in tours.gestionar reservas.confirmar reportes.ver pagos.procesar; do
  $KCADM create "clients/$API_ID/roles" -r turismo -s "name=$r"
done

# --------------------------------------------------------------------------
# 5. Composicion de roles
# --------------------------------------------------------------------------
echo "== Composicion de roles =="
$KCADM add-roles -r turismo --rname supervisor-agencia --rolename agente
$KCADM add-roles -r turismo --rname supervisor-agencia --cclientid turismo-api --rolename reportes.ver

$KCADM add-roles -r turismo --rname operador --cclientid turismo-api --rolename tours.gestionar

$KCADM add-roles -r turismo --rname admin \
  --rolename cliente --rolename agente \
  --rolename supervisor-agencia --rolename operador
$KCADM add-roles -r turismo --rname admin --cclientid turismo-api \
  --rolename tours.gestionar --rolename reservas.confirmar \
  --rolename reportes.ver --rolename pagos.procesar

# --------------------------------------------------------------------------
# 6. Client scopes (los que la API usa de verdad)
# --------------------------------------------------------------------------
echo "== Client scopes =="
for scope in reservas:write pagos:write reportes:read; do
  $KCADM create client-scopes -r turismo \
    -s "name=$scope" \
    -s protocol=openid-connect \
    -s 'attributes."include.in.token.scope"=true' \
    -s 'attributes."display.on.consent.screen"=false'
done

RESV_ID=$(find_id_by client-scopes name "reservas:write")
PAGO_ID=$(find_id_by client-scopes name "pagos:write")
REPT_ID=$(find_id_by client-scopes name "reportes:read")
echo "   reservas:write = $RESV_ID"
echo "   pagos:write    = $PAGO_ID"
echo "   reportes:read  = $REPT_ID"

echo "== Asigno scopes a turismo-web (default/optional) =="
$KCADM update "clients/$WEB_ID/default-client-scopes/$RESV_ID"  -r turismo
$KCADM update "clients/$WEB_ID/default-client-scopes/$REPT_ID"  -r turismo
$KCADM update "clients/$WEB_ID/optional-client-scopes/$PAGO_ID" -r turismo

# --------------------------------------------------------------------------
# 7. Mappers del scope dedicado de turismo-web (audience + agencia_id + groups).
#    En KC 26 el scope dedicado por cliente se accede directo en
#    clients/{id}/protocol-mappers/models. Van en turismo-WEB porque es el
#    cliente que usan los usuarios para pedir el token destinado a la API;
#    el mapper de audience agrega `turismo-api` al aud del access token (T13).
# --------------------------------------------------------------------------
echo "== Mappers dedicados en turismo-web =="
TARGET="clients/$WEB_ID/protocol-mappers/models"

$KCADM create "$TARGET" -r turismo \
  -s name=aud-turismo-api -s protocol=openid-connect \
  -s protocolMapper=oidc-audience-mapper \
  -s 'config."included.client.audience"=turismo-api' \
  -s 'config."id.token.claim"=false' \
  -s 'config."access.token.claim"=true'

$KCADM create "$TARGET" -r turismo \
  -s name=agencia_id -s protocol=openid-connect \
  -s protocolMapper=oidc-usermodel-attribute-mapper \
  -s 'config."user.attribute"=agencia_id' \
  -s 'config."claim.name"=agencia_id' \
  -s 'config."jsonType.label"=String' \
  -s 'config."id.token.claim"=true' \
  -s 'config."access.token.claim"=true' \
  -s 'config."userinfo.token.claim"=true' \
  -s 'config."aggregate.attrs"=false'

$KCADM create "$TARGET" -r turismo \
  -s name=groups-full-path -s protocol=openid-connect \
  -s protocolMapper=oidc-group-membership-mapper \
  -s 'config."claim.name"=groups' \
  -s 'config."full.path"=true' \
  -s 'config."id.token.claim"=true' \
  -s 'config."access.token.claim"=true' \
  -s 'config."userinfo.token.claim"=true'

# --------------------------------------------------------------------------
# 8. Grupos (arbol + atributos). agencia_id solo en subgrupos hoja (T6).
# --------------------------------------------------------------------------
echo "== Grupos =="
mk_group() {   # mk_group <parent-id-or-empty> <name>  -> imprime id creado
  local parent="$1" name="$2"
  if [ -z "$parent" ]; then
    $KCADM create groups -r turismo -s "name=$name" 2>&1 \
      | grep -oE "'[a-f0-9-]{36}'" | tr -d "'"
  else
    $KCADM create "groups/$parent/children" -r turismo -s "name=$name" 2>&1 \
      | grep -oE "'[a-f0-9-]{36}'" | tr -d "'"
  fi
}

CLIENTES=$(mk_group ""         "Clientes")
ESTANDAR=$(mk_group "$CLIENTES" "Estandar")
PREMIUM=$(mk_group  "$CLIENTES" "Premium")

AGENCIAS=$(mk_group ""         "Agencias")
AG_LIMA=$(mk_group  "$AGENCIAS" "Agencia-Lima")
LIM_VENT=$(mk_group "$AG_LIMA"  "Ventas")
LIM_SUP=$(mk_group  "$AG_LIMA"  "Supervision")
AG_CUSCO=$(mk_group "$AGENCIAS" "Agencia-Cusco")
CUS_VENT=$(mk_group "$AG_CUSCO" "Ventas")

OPER=$(mk_group ""     "Operaciones")
CATAL=$(mk_group "$OPER" "Catalogo")
FINAN=$(mk_group "$OPER" "Finanzas")

ADMINGR=$(mk_group "" "Administracion")

echo "== Atributos de grupos =="
$KCADM update "groups/$PREMIUM"  -r turismo -s 'attributes."nivel"=["premium"]'
$KCADM update "groups/$LIM_VENT" -r turismo -s 'attributes."agencia_id"=["LIM01"]'
$KCADM update "groups/$LIM_SUP"  -r turismo -s 'attributes."agencia_id"=["LIM01"]'
$KCADM update "groups/$CUS_VENT" -r turismo -s 'attributes."agencia_id"=["CUS01"]'

echo "== Roles por grupo =="
$KCADM add-roles -r turismo --gid "$ESTANDAR" --rolename cliente
$KCADM add-roles -r turismo --gid "$PREMIUM"  --rolename cliente

$KCADM add-roles -r turismo --gid "$LIM_VENT" --rolename agente
$KCADM add-roles -r turismo --gid "$LIM_VENT" --cclientid turismo-api --rolename reservas.confirmar
$KCADM add-roles -r turismo --gid "$CUS_VENT" --rolename agente
$KCADM add-roles -r turismo --gid "$CUS_VENT" --cclientid turismo-api --rolename reservas.confirmar

$KCADM add-roles -r turismo --gid "$LIM_SUP" --rolename supervisor-agencia

$KCADM add-roles -r turismo --gid "$CATAL" --rolename operador

$KCADM add-roles -r turismo --gid "$FINAN" --cclientid turismo-api \
  --rolename pagos.procesar --rolename reportes.ver

$KCADM add-roles -r turismo --gid "$ADMINGR" --rolename admin

# --------------------------------------------------------------------------
# 9. Usuarios (T2, T3): email, nombre, apellido, emailVerified, locale es,
#    sin requiredActions, password NO temporal.
# --------------------------------------------------------------------------
echo "== Usuarios =="
mk_user() {   # mk_user <username> <firstName> <lastName> <group-path>
  local user="$1" first="$2" last="$3" group="$4"
  $KCADM create users -r turismo \
    -s "username=$user" \
    -s enabled=true \
    -s emailVerified=true \
    -s "email=${user}@turismo.pe" \
    -s "firstName=$first" \
    -s "lastName=$last" \
    -s 'attributes."locale"=["es"]' \
    -s 'requiredActions=[]' \
    -s "groups=[\"$group\"]"
  $KCADM set-password -r turismo --username "$user" --new-password "$PASSWORD"
}

mk_user ana.cliente       "Ana"     "Quispe"      "/Clientes/Estandar"
mk_user carlos.premium    "Carlos"  "Mendoza"     "/Clientes/Premium"
mk_user lucia.agente      "Lucia"   "Rojas"       "/Agencias/Agencia-Lima/Ventas"
mk_user jorge.supervisor  "Jorge"   "Vargas"      "/Agencias/Agencia-Lima/Supervision"
mk_user mario.agente      "Mario"   "Huaman"      "/Agencias/Agencia-Cusco/Ventas"
mk_user olga.operador     "Olga"    "Flores"      "/Operaciones/Catalogo"
mk_user fiona.finanzas    "Fiona"   "Castillo"    "/Operaciones/Finanzas"
mk_user admin.turismo     "Admin"   "TurismoPeru" "/Administracion"

echo
echo "== Seed turismo OK =="
