#!/bin/sh
# Crea la BD y el usuario de la aplicación (turismo_app) desde variables
# de entorno. Se ejecuta UNA sola vez: solo cuando el volumen está vacío.
# Si cambia .env, hay que `docker compose down -v`.
set -eu

: "${APP_DB_NAME:?APP_DB_NAME no definido}"
: "${APP_DB_USER:?APP_DB_USER no definido}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD no definido}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-SQL
    CREATE USER "${APP_DB_USER}" WITH PASSWORD '${APP_DB_PASSWORD}';
    CREATE DATABASE "${APP_DB_NAME}" OWNER "${APP_DB_USER}";
    GRANT ALL PRIVILEGES ON DATABASE "${APP_DB_NAME}" TO "${APP_DB_USER}";
SQL
