#!/bin/bash
# Fase 2 · Item A (parte 2) — crea el realm `agencias-partners` para B2B:
# 2 clientes (portal + integracion), 2 roles de realm, 1 rol de cliente,
# 1 client scope, grupos y 2 usuarios. Service account de partners-integracion
# recibe directamente el rol disponibilidad.consultar.
#
# Idempotencia: NO. Para re-sembrar:
#   docker exec turismo-keycloak /opt/keycloak/bin/kcadm.sh delete realms/agencias-partners
#
# Se ejecuta DESDE EL HOST. Requiere python3.
#
# Uso:
#   keycloak/bootstrap/seed-partners.sh <ADMIN_USER> <ADMIN_PASS> <PARTNERS_INTEGRACION_SECRET>
set -euo pipefail

KC_USER="${1:-admin}"
KC_PASS="${2:-admin}"
INT_SECRET="${3:-cambiar-en-local}"
PASSWORD="Turismo#2026a"
REALM="agencias-partners"
CONTAINER="turismo-keycloak"
KCADM="docker exec -i $CONTAINER /opt/keycloak/bin/kcadm.sh"

find_id_by() {   # find_id_by <endpoint> <campo> <valor>
  local endpoint="$1" field="$2" value="$3"
  $KCADM get "$endpoint" -r "$REALM" 2>/dev/null | python3 -c "
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
echo "== Realm $REALM =="
$KCADM create realms \
  -s "realm=$REALM" \
  -s displayName='TurismoPerú Partners' \
  -s displayNameHtml='<strong>TurismoPerú</strong> · Partners' \
  -s enabled=true \
  -s registrationAllowed=false \
  -s resetPasswordAllowed=true \
  -s loginWithEmailAllowed=true

# --------------------------------------------------------------------------
# 2. Roles de realm
# --------------------------------------------------------------------------
echo "== Roles de realm =="
$KCADM create roles -r "$REALM" -s name=partner-agente
$KCADM create roles -r "$REALM" -s name=partner-admin

# --------------------------------------------------------------------------
# 3. Clientes
# --------------------------------------------------------------------------
echo "== Cliente partners-portal (publico, PKCE, direct grants en dev) =="
$KCADM create clients -r "$REALM" \
  -s clientId=partners-portal \
  -s name='Portal de Partners' \
  -s publicClient=true \
  -s standardFlowEnabled=true \
  -s directAccessGrantsEnabled=true \
  -s 'redirectUris=["http://localhost:4200/*"]' \
  -s 'webOrigins=["http://localhost:4200"]' \
  -s 'attributes."pkce.code.challenge.method"=S256'

echo "== Cliente partners-integracion (confidencial, service account) =="
$KCADM create clients -r "$REALM" \
  -s clientId=partners-integracion \
  -s name='Integracion B2B' \
  -s publicClient=false \
  -s serviceAccountsEnabled=true \
  -s standardFlowEnabled=false \
  -s directAccessGrantsEnabled=false \
  -s "secret=$INT_SECRET"

PORTAL_ID=$(find_id_by clients clientId partners-portal)
INT_ID=$(find_id_by clients clientId partners-integracion)
echo "   partners-portal      id = $PORTAL_ID"
echo "   partners-integracion id = $INT_ID"

# --------------------------------------------------------------------------
# 4. Rol de cliente en partners-integracion
# --------------------------------------------------------------------------
echo "== Rol de cliente partners-integracion =="
$KCADM create "clients/$INT_ID/roles" -r "$REALM" -s name=disponibilidad.consultar

# --------------------------------------------------------------------------
# 5. Composicion de roles: partner-admin = partner-agente
# --------------------------------------------------------------------------
echo "== Composicion de roles =="
$KCADM add-roles -r "$REALM" --rname partner-admin --rolename partner-agente

# --------------------------------------------------------------------------
# 6. Client scope disponibilidad:read (default en ambos clientes)
# --------------------------------------------------------------------------
echo "== Client scope disponibilidad:read =="
$KCADM create client-scopes -r "$REALM" \
  -s "name=disponibilidad:read" \
  -s protocol=openid-connect \
  -s 'attributes."include.in.token.scope"=true' \
  -s 'attributes."display.on.consent.screen"=false'

DISP_ID=$(find_id_by client-scopes name "disponibilidad:read")
echo "   disponibilidad:read = $DISP_ID"

echo "== Asigno scope a ambos clientes como default =="
$KCADM update "clients/$PORTAL_ID/default-client-scopes/$DISP_ID" -r "$REALM"
$KCADM update "clients/$INT_ID/default-client-scopes/$DISP_ID"    -r "$REALM"

# --------------------------------------------------------------------------
# 7. Mappers dedicados.
#    partners-portal: igual que turismo-web (audience + agencia_id + groups).
#    partners-integracion: solo audience → turismo-api, para que su token de
#      client_credentials sea aceptado por la API multi-issuer (T13).
# --------------------------------------------------------------------------
echo "== Mappers dedicados en partners-portal =="
TGT_P="clients/$PORTAL_ID/protocol-mappers/models"

$KCADM create "$TGT_P" -r "$REALM" \
  -s name=aud-turismo-api -s protocol=openid-connect \
  -s protocolMapper=oidc-audience-mapper \
  -s 'config."included.client.audience"=turismo-api' \
  -s 'config."id.token.claim"=false' \
  -s 'config."access.token.claim"=true'

$KCADM create "$TGT_P" -r "$REALM" \
  -s name=agencia_id -s protocol=openid-connect \
  -s protocolMapper=oidc-usermodel-attribute-mapper \
  -s 'config."user.attribute"=agencia_id' \
  -s 'config."claim.name"=agencia_id' \
  -s 'config."jsonType.label"=String' \
  -s 'config."id.token.claim"=true' \
  -s 'config."access.token.claim"=true' \
  -s 'config."userinfo.token.claim"=true' \
  -s 'config."aggregate.attrs"=false'

$KCADM create "$TGT_P" -r "$REALM" \
  -s name=groups-full-path -s protocol=openid-connect \
  -s protocolMapper=oidc-group-membership-mapper \
  -s 'config."claim.name"=groups' \
  -s 'config."full.path"=true' \
  -s 'config."id.token.claim"=true' \
  -s 'config."access.token.claim"=true' \
  -s 'config."userinfo.token.claim"=true'

echo "== Mapper dedicado en partners-integracion (solo audience) =="
TGT_I="clients/$INT_ID/protocol-mappers/models"
$KCADM create "$TGT_I" -r "$REALM" \
  -s name=aud-turismo-api -s protocol=openid-connect \
  -s protocolMapper=oidc-audience-mapper \
  -s 'config."included.client.audience"=turismo-api' \
  -s 'config."id.token.claim"=false' \
  -s 'config."access.token.claim"=true'

# --------------------------------------------------------------------------
# 8. Service account de partners-integracion -> rol disponibilidad.consultar
# --------------------------------------------------------------------------
echo "== Rol al service account de partners-integracion =="
SA_UID=$($KCADM get "clients/$INT_ID/service-account-user" -r "$REALM" --fields id 2>/dev/null \
  | grep -m1 '"id"' | sed -E 's/.*"id"[^"]*"([^"]+)".*/\1/')
echo "   service account user id = $SA_UID"
$KCADM add-roles -r "$REALM" --uid "$SA_UID" \
  --cclientid partners-integracion --rolename disponibilidad.consultar

# --------------------------------------------------------------------------
# 9. Grupos (arbol + atributos). agencia_id solo en hojas (T6).
# --------------------------------------------------------------------------
echo "== Grupos =="
mk_group() {
  local parent="$1" name="$2"
  if [ -z "$parent" ]; then
    $KCADM create groups -r "$REALM" -s "name=$name" 2>&1 \
      | grep -oE "'[a-f0-9-]{36}'" | tr -d "'"
  else
    $KCADM create "groups/$parent/children" -r "$REALM" -s "name=$name" 2>&1 \
      | grep -oE "'[a-f0-9-]{36}'" | tr -d "'"
  fi
}

PARTNERS=$(mk_group ""         "Partners")
NAC=$(mk_group      "$PARTNERS" "Nacionales")
AQP=$(mk_group      "$NAC"      "Arequipa-Tours")
INTER=$(mk_group    "$PARTNERS" "Internacionales")
AND=$(mk_group      "$INTER"    "Andes-Travel-Chile")
PADMIN=$(mk_group   ""          "Partners-Admin")

echo "== Atributos de grupos (agencia_id en hojas) =="
$KCADM update "groups/$AQP" -r "$REALM" -s 'attributes."agencia_id"=["AQP-P01"]'
$KCADM update "groups/$AND" -r "$REALM" -s 'attributes."agencia_id"=["SCL-P01"]'

echo "== Roles por grupo =="
# Hojas de Partners: partner-agente + disponibilidad.consultar
for G in "$AQP" "$AND"; do
  $KCADM add-roles -r "$REALM" --gid "$G" --rolename partner-agente
  $KCADM add-roles -r "$REALM" --gid "$G" \
    --cclientid partners-integracion --rolename disponibilidad.consultar
done

# Partners-Admin: partner-admin (compuesto) + disponibilidad.consultar
$KCADM add-roles -r "$REALM" --gid "$PADMIN" --rolename partner-admin
$KCADM add-roles -r "$REALM" --gid "$PADMIN" \
  --cclientid partners-integracion --rolename disponibilidad.consultar

# --------------------------------------------------------------------------
# 10. Usuarios (T2, T3)
# --------------------------------------------------------------------------
echo "== Usuarios =="
mk_user() {
  local user="$1" first="$2" last="$3" group="$4"
  $KCADM create users -r "$REALM" \
    -s "username=$user" \
    -s enabled=true \
    -s emailVerified=true \
    -s "email=${user}@partners.pe" \
    -s "firstName=$first" \
    -s "lastName=$last" \
    -s 'attributes."locale"=["es"]' \
    -s 'requiredActions=[]' \
    -s "groups=[\"$group\"]"
  $KCADM set-password -r "$REALM" --username "$user" --new-password "$PASSWORD"
}

mk_user pedro.partner "Pedro" "Zegarra"  "/Partners/Nacionales/Arequipa-Tours"
mk_user rosa.partner  "Rosa"  "Maldonado" "/Partners-Admin"

echo
echo "== Seed partners OK =="
