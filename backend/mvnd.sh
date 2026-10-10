#!/bin/bash
# Wrapper que ejecuta Maven en Docker (imagen eclipse-temurin-21) sin necesidad
# de instalar JDK/Maven en el host. Usa un volumen `maven-cache` persistente
# para no re-descargar dependencias en cada corrida y comparte la red del
# compose (galaxy_default) para alcanzar Postgres y Keycloak por nombre.
#
# Uso:
#   backend/mvnd.sh clean package
#   backend/mvnd.sh spring-boot:run
set -euo pipefail

BACKEND_DIR="$(cd "$(dirname "$0")" && pwd)"

# Si estamos en una TTY interactiva, pasar -it. En background/scripts, solo -i.
TTY_FLAG="-i"
[ -t 0 ] && TTY_FLAG="-it"

docker run --rm $TTY_FLAG \
  --name turismo-mvn \
  --network galaxy_default \
  -v "$BACKEND_DIR":/app \
  -v maven-cache:/root/.m2 \
  -w /app \
  -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/turismo_app \
  -e SPRING_DATASOURCE_USERNAME=turismo \
  -e SPRING_DATASOURCE_PASSWORD=turismo \
  -e TZ=America/Lima \
  maven:3.9-eclipse-temurin-21 \
  mvn "$@"
