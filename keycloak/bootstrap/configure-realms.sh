#!/bin/bash
# Fase 2 · Item B — configuracion de la rubrica item 9 en AMBOS realms:
# idioma, politica de contrasena y tiempos de token/sesion.
# Es idempotente: aplica update sobre el realm.
#
# Uso:  keycloak/bootstrap/configure-realms.sh <ADMIN_USER> <ADMIN_PASS>
set -euo pipefail

KC_USER="${1:-admin}"
KC_PASS="${2:-admin}"
CONTAINER="turismo-keycloak"
KCADM="docker exec -i $CONTAINER /opt/keycloak/bin/kcadm.sh"

PASSWORD_POLICY='length(10) and upperCase(1) and lowerCase(1) and digits(1) and regexPattern(^(?=.*[@%&#.+/!=?¿]).*$) and passwordHistory(3) and notUsername'

echo "== Login =="
$KCADM config credentials --server http://localhost:8080 \
  --realm master --user "$KC_USER" --password "$KC_PASS"

for REALM in turismo agencias-partners; do
  echo
  echo "== Configurando realm: $REALM =="

  # 1) Idioma: internacionalizacion ON, soportados [es, en], default es.
  $KCADM update "realms/$REALM" \
    -s internationalizationEnabled=true \
    -s 'supportedLocales=["es","en"]' \
    -s defaultLocale=es

  # 2) Politica de contrasena.
  $KCADM update "realms/$REALM" \
    -s "passwordPolicy=$PASSWORD_POLICY"

  # 3) Tiempos de token y sesion.
  #    accessTokenLifespan        : 300 s   (5 min)
  #    ssoSessionIdleTimeout      : 1800 s  (30 min)
  #    ssoSessionMaxLifespan      : 36000 s (10 h)
  #    revokeRefreshToken         : true
  #    refreshTokenMaxReuse       : 0
  $KCADM update "realms/$REALM" \
    -s accessTokenLifespan=300 \
    -s ssoSessionIdleTimeout=1800 \
    -s ssoSessionMaxLifespan=36000 \
    -s revokeRefreshToken=true \
    -s refreshTokenMaxReuse=0
done

echo
echo "== Configuracion aplicada =="
