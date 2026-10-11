#!/bin/bash
set -u
TOK_URL="http://localhost:8080/realms/turismo/protocol/openid-connect/token"
API="http://localhost:8081"

get_tok() {
  curl -s -d client_id=turismo-web -d grant_type=password \
    -d "username=$1" -d 'password=Turismo#2026a' -d scope=openid "$TOK_URL" \
    | python3 -c "import sys,json;print(json.load(sys.stdin).get('access_token',''))"
}

probe() {
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $2" "$API$3")
  printf "%-32s %-30s %-3s\n" "$1" "$3" "$code"
}

for u in ana.cliente fiona.finanzas jorge.supervisor admin.turismo; do
  TOK=$(get_tok "$u")
  if [ -z "$TOK" ]; then
    printf "%-32s TOKEN FAIL\n" "$u"
    continue
  fi
  probe "$u / view"   "$TOK" "/api/reportes/ventas"
  probe "$u / export" "$TOK" "/api/reportes/ventas/export"
done

echo "---- partners cross-realm (debe 401) ----"
PTOK=$(curl -s -d client_id=partners-portal -d grant_type=password \
  -d username=pedro.partner -d 'password=Turismo#2026a' -d scope=openid \
  "http://localhost:8080/realms/agencias-partners/protocol/openid-connect/token" \
  | python3 -c "import sys,json;print(json.load(sys.stdin).get('access_token',''))")
if [ -z "$PTOK" ]; then echo "partners token FAIL"; else probe "partners -> /ventas" "$PTOK" "/api/reportes/ventas"; fi
