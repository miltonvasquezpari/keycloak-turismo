#!/bin/bash
# Fase 5 · Item 5.1 — Authorization Services sobre el cliente `turismo-api`
# del realm `turismo`:
#   - scopes de autorizacion: view, export
#   - resource: Reporte Ventas (urn:turismo:reporte, URIs de /api/reportes/ventas*)
#   - 6 policies: 3 de rol, 1 de grupo, 1 de tiempo, 1 agregada
#   - 2 permissions: Ver (AFFIRMATIVE 3 policies) y Exportar (AFFIRMATIVE 2 policies)
#   - settings: ENFORCING + UNANIMOUS
#
# Idempotencia: NO. Si re-ejecuta, duplica. Para re-sembrar, borrar las
# entidades authz del cliente o re-crear el realm.
#
# Uso:
#   keycloak/bootstrap/seed-authz-turismo.sh <ADMIN_USER> <ADMIN_PASS>
set -euo pipefail

KC_USER="${1:-admin}"
KC_PASS="${2:-admin}"
CONTAINER="turismo-keycloak"
REALM="turismo"
KCADM="docker exec -i $CONTAINER /opt/keycloak/bin/kcadm.sh"

find_id_by() {   # find_id_by <endpoint> <field> <value>
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

# -------- IDs necesarios --------
API_CID=$(find_id_by clients clientId turismo-api)
echo "   turismo-api cid         = $API_CID"
AUTHZ="clients/$API_CID/authz/resource-server"

# Role IDs (realm roles + role de cliente turismo-api)
SUP_RID=$($KCADM get roles/supervisor-agencia -r "$REALM" --fields id \
  | grep -m1 '"id"' | sed -E 's/.*"id"[^"]*"([^"]+)".*/\1/')
ADM_RID=$($KCADM get roles/admin -r "$REALM" --fields id \
  | grep -m1 '"id"' | sed -E 's/.*"id"[^"]*"([^"]+)".*/\1/')
RVER_RID=$($KCADM get "clients/$API_CID/roles/reportes.ver" -r "$REALM" --fields id \
  | grep -m1 '"id"' | sed -E 's/.*"id"[^"]*"([^"]+)".*/\1/')
echo "   supervisor-agencia role = $SUP_RID"
echo "   admin role              = $ADM_RID"
echo "   reportes.ver client role= $RVER_RID"

# Group ID: /Agencias/Agencia-Lima/Supervision
LIMA_SUP_GID=$($KCADM get groups -r "$REALM" 2>/dev/null | python3 -c "
import sys,json
def walk(gs, parent='/'):
    for g in gs:
        path=parent+g['name']
        yield path, g['id']
        yield from walk(g.get('subGroups',[]) or [], path+'/')
for p,i in walk(json.load(sys.stdin)):
    if p=='/Agencias/Agencia-Lima/Supervision': print(i); break
")
# Keycloak subGroups via list may ser lazy; fallback con GET recursivo
if [ -z "$LIMA_SUP_GID" ]; then
  AG_ID=$(find_id_by groups name Agencias)
  LIMA_ID=$($KCADM get "groups/$AG_ID/children" -r "$REALM" 2>/dev/null \
    | python3 -c "import sys,json; print(next(g['id'] for g in json.load(sys.stdin) if g['name']=='Agencia-Lima'))")
  LIMA_SUP_GID=$($KCADM get "groups/$LIMA_ID/children" -r "$REALM" 2>/dev/null \
    | python3 -c "import sys,json; print(next(g['id'] for g in json.load(sys.stdin) if g['name']=='Supervision'))")
fi
echo "   Supervision Lima group  = $LIMA_SUP_GID"

# -------- Settings: ENFORCING + UNANIMOUS --------
echo "== Settings: policyEnforcementMode=ENFORCING, decisionStrategy=UNANIMOUS =="
$KCADM update "$AUTHZ" -r "$REALM" \
  -s policyEnforcementMode=ENFORCING \
  -s decisionStrategy=UNANIMOUS

# -------- Scopes --------
echo "== Scopes de autorizacion: view, export =="
$KCADM create "$AUTHZ/scope" -r "$REALM" -s name=view
$KCADM create "$AUTHZ/scope" -r "$REALM" -s name=export

VIEW_SID=$(find_id_by "$AUTHZ/scope" name view)
EXP_SID=$(find_id_by  "$AUTHZ/scope" name export)
echo "   view id   = $VIEW_SID"
echo "   export id = $EXP_SID"

# -------- Resource --------
echo "== Resource: Reporte Ventas =="
$KCADM create "$AUTHZ/resource" -r "$REALM" \
  -s name='Reporte Ventas' \
  -s type='urn:turismo:reporte' \
  -s 'uris=["/api/reportes/ventas","/api/reportes/ventas/*"]' \
  -s "scopes=[{\"id\":\"$VIEW_SID\",\"name\":\"view\"},{\"id\":\"$EXP_SID\",\"name\":\"export\"}]"

# -------- Policies --------
echo "== Policies =="

# Rol Supervisor (realm supervisor-agencia)
$KCADM create "$AUTHZ/policy/role" -r "$REALM" \
  -s name='Rol Supervisor' \
  -s decisionStrategy=UNANIMOUS \
  -s "roles=[{\"id\":\"$SUP_RID\",\"required\":false}]"

# Rol Reportes (cliente turismo-api role reportes.ver)
$KCADM create "$AUTHZ/policy/role" -r "$REALM" \
  -s name='Rol Reportes' \
  -s decisionStrategy=UNANIMOUS \
  -s "roles=[{\"id\":\"$RVER_RID\",\"required\":false}]"

# Rol Admin (realm admin)
$KCADM create "$AUTHZ/policy/role" -r "$REALM" \
  -s name='Rol Admin' \
  -s decisionStrategy=UNANIMOUS \
  -s "roles=[{\"id\":\"$ADM_RID\",\"required\":false}]"

# Grupo Supervision Lima
$KCADM create "$AUTHZ/policy/group" -r "$REALM" \
  -s name='Grupo Supervision Lima' \
  -s "groupsClaim=" \
  -s "groups=[{\"id\":\"$LIMA_SUP_GID\",\"extendChildren\":false}]"

# Horario Laboral (hora 08:00-20:00 Lima; Keycloak Time Policy no soporta
# dia-de-semana, asi que cubre todos los dias. Para prod se combina con JS)
$KCADM create "$AUTHZ/policy/time" -r "$REALM" \
  -s name='Horario Laboral' \
  -s hour=8 -s hourEnd=20 \
  -s minute=0 -s minuteEnd=0

# IDs para la agregada
SUP_LIMA_PID=$(find_id_by "$AUTHZ/policy" name 'Grupo Supervision Lima')
HOR_PID=$(find_id_by     "$AUTHZ/policy" name 'Horario Laboral')
echo "   Grupo Supervision Lima pid = $SUP_LIMA_PID"
echo "   Horario Laboral        pid = $HOR_PID"

# Supervisor Lima en Horario (aggregated UNANIMOUS)
$KCADM create "$AUTHZ/policy/aggregate" -r "$REALM" \
  -s name='Supervisor Lima en Horario' \
  -s decisionStrategy=UNANIMOUS \
  -s "policies=[\"$SUP_LIMA_PID\",\"$HOR_PID\"]"

# IDs de todas las policies que usan los permissions
ROL_SUP_PID=$(find_id_by "$AUTHZ/policy" name 'Rol Supervisor')
ROL_REP_PID=$(find_id_by "$AUTHZ/policy" name 'Rol Reportes')
ROL_ADM_PID=$(find_id_by "$AUTHZ/policy" name 'Rol Admin')
AGG_SUP_PID=$(find_id_by "$AUTHZ/policy" name 'Supervisor Lima en Horario')
echo "   Rol Supervisor pid = $ROL_SUP_PID"
echo "   Rol Reportes   pid = $ROL_REP_PID"
echo "   Rol Admin      pid = $ROL_ADM_PID"
echo "   Agregada       pid = $AGG_SUP_PID"

# -------- Permissions --------
echo "== Permissions =="

# Ver Reporte Ventas (scope view, 3 policies, AFFIRMATIVE)
$KCADM create "$AUTHZ/permission/scope" -r "$REALM" \
  -s name='Ver Reporte Ventas' \
  -s decisionStrategy=AFFIRMATIVE \
  -s "scopes=[\"$VIEW_SID\"]" \
  -s "policies=[\"$ROL_SUP_PID\",\"$ROL_REP_PID\",\"$ROL_ADM_PID\"]"

# Exportar Reporte Ventas (scope export, 2 policies, AFFIRMATIVE)
$KCADM create "$AUTHZ/permission/scope" -r "$REALM" \
  -s name='Exportar Reporte Ventas' \
  -s decisionStrategy=AFFIRMATIVE \
  -s "scopes=[\"$EXP_SID\"]" \
  -s "policies=[\"$AGG_SUP_PID\",\"$ROL_ADM_PID\"]"

echo
echo "== Seed authz OK =="
