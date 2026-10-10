#!/bin/bash
# Corrida de la matriz §3 del PLAN contra el backend corriendo en 8081.
# Omite E12/E13 (Fase 5). Contabiliza aciertos.
PASS='Turismo#2026a'
PASS_OK=0; PASS_FAIL=0

tok() {
  curl -s -X POST http://localhost:8080/realms/turismo/protocol/openid-connect/token \
    -d client_id=turismo-web -d grant_type=password \
    -d "username=$1" -d "password=$PASS" -d "scope=openid ${2:-}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])"
}
tok_partner() {
  curl -s -X POST http://localhost:8080/realms/agencias-partners/protocol/openid-connect/token \
    -d client_id=partners-portal -d grant_type=password \
    -d "username=$1" -d "password=$PASS" -d scope=openid \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])"
}
check() {
  local label="$1" exp="$2"; shift 2
  local code=$(curl -s -o /dev/null -w "%{http_code}" "$@")
  if [ "$code" = "$exp" ]; then
    PASS_OK=$((PASS_OK+1))
    printf "  OK   %-55s %s\n" "$label" "$exp"
  else
    PASS_FAIL=$((PASS_FAIL+1))
    printf "  FAIL %-55s esperado=%s recibido=%s\n" "$label" "$exp" "$code"
  fi
}

ANA=$(tok ana.cliente)
CARLOS=$(tok carlos.premium)
LUCIA=$(tok lucia.agente)
JORGE=$(tok jorge.supervisor)
MARIO=$(tok mario.agente)
OLGA=$(tok olga.operador)
FIONA=$(tok fiona.finanzas)
FIONA_PG=$(tok fiona.finanzas "pagos:write")
ADMIN=$(tok admin.turismo)
ADMIN_PG=$(tok admin.turismo "pagos:write")
PEDRO=$(tok_partner pedro.partner)

API=http://localhost:8081

echo "=== E1 GET /api/public/tours ==="
check "sin token" 200 "$API/api/public/tours"
check "ana"       200 -H "Authorization: Bearer $ANA" "$API/api/public/tours"
check "pedro"     200 -H "Authorization: Bearer $PEDRO" "$API/api/public/tours"

echo "=== E3 GET /api/reservas/mias ==="
check "sin token" 401 "$API/api/reservas/mias"
check "ana"       200 -H "Authorization: Bearer $ANA"   "$API/api/reservas/mias"
check "carlos"    200 -H "Authorization: Bearer $CARLOS" "$API/api/reservas/mias"
check "lucia"     403 -H "Authorization: Bearer $LUCIA" "$API/api/reservas/mias"
check "jorge"     403 -H "Authorization: Bearer $JORGE" "$API/api/reservas/mias"
check "fiona"     403 -H "Authorization: Bearer $FIONA" "$API/api/reservas/mias"
check "admin"     200 -H "Authorization: Bearer $ADMIN" "$API/api/reservas/mias"
check "pedro"     403 -H "Authorization: Bearer $PEDRO" "$API/api/reservas/mias"

echo "=== E5 GET /api/reservas/R-1001 (owner=carlos, agencia=LIM01) ==="
check "sin token" 401 "$API/api/reservas/R-1001"
check "ana (NO)"       403 -H "Authorization: Bearer $ANA"    "$API/api/reservas/R-1001"
check "carlos (dueno)" 200 -H "Authorization: Bearer $CARLOS" "$API/api/reservas/R-1001"
check "lucia (LIM01)"  200 -H "Authorization: Bearer $LUCIA"  "$API/api/reservas/R-1001"
check "jorge (LIM01)"  200 -H "Authorization: Bearer $JORGE"  "$API/api/reservas/R-1001"
check "mario (CUS01)"  403 -H "Authorization: Bearer $MARIO"  "$API/api/reservas/R-1001"
check "fiona (finanzas)" 200 -H "Authorization: Bearer $FIONA" "$API/api/reservas/R-1001"
check "admin"            200 -H "Authorization: Bearer $ADMIN" "$API/api/reservas/R-1001"
check "pedro"            403 -H "Authorization: Bearer $PEDRO" "$API/api/reservas/R-1001"

echo "=== E5 GET /api/reservas/R-1002 (owner=ana, agencia=CUS01) ==="
check "ana (dueno)"    200 -H "Authorization: Bearer $ANA"    "$API/api/reservas/R-1002"
check "carlos (NO)"    403 -H "Authorization: Bearer $CARLOS" "$API/api/reservas/R-1002"
check "lucia (LIM01, otra)" 403 -H "Authorization: Bearer $LUCIA" "$API/api/reservas/R-1002"
check "mario (CUS01)"  200 -H "Authorization: Bearer $MARIO"  "$API/api/reservas/R-1002"
check "admin"          200 -H "Authorization: Bearer $ADMIN"  "$API/api/reservas/R-1002"

echo "=== E6 PUT /api/reservas/R-1002/cancelar (PENDIENTE, ana) ==="
check "ana (200 primera vez)"  200 -X PUT -H "Authorization: Bearer $ANA" "$API/api/reservas/R-1002/cancelar"
echo "=== E6 PUT /api/reservas/R-1003/cancelar (CONFIRMADA, ana) ==="
check "ana (409 por estado)"   409 -X PUT -H "Authorization: Bearer $ANA" "$API/api/reservas/R-1003/cancelar"
check "carlos (403 no dueno)"  403 -X PUT -H "Authorization: Bearer $CARLOS" "$API/api/reservas/R-1003/cancelar"

echo "=== E7 GET /api/agencia/reservas ==="
check "sin token" 401 "$API/api/agencia/reservas"
check "ana"       403 -H "Authorization: Bearer $ANA"   "$API/api/agencia/reservas"
check "lucia (LIM01)" 200 -H "Authorization: Bearer $LUCIA" "$API/api/agencia/reservas"
check "mario (CUS01)" 200 -H "Authorization: Bearer $MARIO" "$API/api/agencia/reservas"
check "jorge (LIM01)" 200 -H "Authorization: Bearer $JORGE" "$API/api/agencia/reservas"
check "admin"         200 -H "Authorization: Bearer $ADMIN" "$API/api/agencia/reservas"
check "pedro"         403 -H "Authorization: Bearer $PEDRO" "$API/api/agencia/reservas"

echo "=== E8 PUT /api/reservas/R-1001/confirmar (PENDIENTE, LIM01) ==="
check "sin token"     401 -X PUT "$API/api/reservas/R-1001/confirmar"
check "lucia (200)"   200 -X PUT -H "Authorization: Bearer $LUCIA" "$API/api/reservas/R-1001/confirmar"
check "jorge (403: supervisor sin rol)" 403 -X PUT -H "Authorization: Bearer $JORGE" "$API/api/reservas/R-1001/confirmar"
check "mario (403: otra agencia)"       403 -X PUT -H "Authorization: Bearer $MARIO" "$API/api/reservas/R-1001/confirmar"
check "ana (403)"     403 -X PUT -H "Authorization: Bearer $ANA" "$API/api/reservas/R-1001/confirmar"

echo "=== E9 POST /api/admin/tours ==="
BODY='{"codigo":"T-ZZ-99","nombre":"Z","precio":100,"duracionHoras":5,"cupoDiario":10,"destinoId":1,"activo":true}'
check "sin token" 401 -X POST -H "Content-Type: application/json" -d "$BODY" "$API/api/admin/tours"
check "ana"       403 -X POST -H "Content-Type: application/json" -d "$BODY" -H "Authorization: Bearer $ANA" "$API/api/admin/tours"
check "olga (operador)" 201 -X POST -H "Content-Type: application/json" -d "$BODY" -H "Authorization: Bearer $OLGA" "$API/api/admin/tours"
check "fiona"     403 -X POST -H "Content-Type: application/json" -d "$BODY" -H "Authorization: Bearer $FIONA" "$API/api/admin/tours"

echo "=== E11 POST /api/pagos (R-1003 CONFIRMADA) ==="
PAG='{"reservaId":"R-1003","metodo":"TARJETA"}'
check "sin token" 401 -X POST -H "Content-Type: application/json" -d "$PAG" "$API/api/pagos"
check "ana"       403 -X POST -H "Content-Type: application/json" -d "$PAG" -H "Authorization: Bearer $ANA" "$API/api/pagos"
check "fiona SIN scope" 403 -X POST -H "Content-Type: application/json" -d "$PAG" -H "Authorization: Bearer $FIONA" "$API/api/pagos"
check "fiona CON scope" 201 -X POST -H "Content-Type: application/json" -d "$PAG" -H "Authorization: Bearer $FIONA_PG" "$API/api/pagos"
check "admin SIN scope" 403 -X POST -H "Content-Type: application/json" -d "$PAG" -H "Authorization: Bearer $ADMIN" "$API/api/pagos"

echo "=== E14 GET /api/partners/disponibilidad ==="
URL="$API/api/partners/disponibilidad?tourId=1&fecha=2026-11-15"
check "sin token" 401 "$URL"
check "pedro"     200 -H "Authorization: Bearer $PEDRO" "$URL"
check "ana"       403 -H "Authorization: Bearer $ANA"   "$URL"

echo "=== E12 GET /api/reportes/ventas (Policy Enforcer, scope view) ==="
check "sin token"            401                                   "$API/api/reportes/ventas"
check "ana"                  403 -H "Authorization: Bearer $ANA"   "$API/api/reportes/ventas"
check "jorge (supervisor)"   200 -H "Authorization: Bearer $JORGE" "$API/api/reportes/ventas"
check "fiona (reportes.ver)" 200 -H "Authorization: Bearer $FIONA" "$API/api/reportes/ventas"
check "admin"                200 -H "Authorization: Bearer $ADMIN" "$API/api/reportes/ventas"
check "pedro (otro realm)"   401 -H "Authorization: Bearer $PEDRO" "$API/api/reportes/ventas"

echo "=== E13 GET /api/reportes/ventas/export (scope export + Horario Laboral) ==="
# Nota: jorge y fiona dependen de la hora actual (08-20 Lima) y de la policy
# agregada (Supervision Lima + Horario). Admin PERMIT siempre.
check "sin token" 401 "$API/api/reportes/ventas/export"
check "ana"       403 -H "Authorization: Bearer $ANA"   "$API/api/reportes/ventas/export"
check "admin"     200 -H "Authorization: Bearer $ADMIN" "$API/api/reportes/ventas/export"

echo
echo "============================================"
echo "TOTAL: OK=$PASS_OK  FAIL=$PASS_FAIL"
echo "============================================"
