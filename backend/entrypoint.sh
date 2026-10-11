#!/bin/sh
# Utsav container entrypoint (Render).
#
# Render injects DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD via
# render.yaml (fromDatabase). The app expects DATABASE_URL in JDBC format plus
# DATABASE_USER / DATABASE_PASSWORD (see backend/src/main/resources/application.yml),
# so we compose them here. Render's own `connectionString` property is a
# `postgres://` URL, which the JDBC driver does NOT accept — hence the parts.
set -e

if [ -n "$DB_HOST" ]; then
  export DATABASE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT:-5432}/${DB_NAME}"
  export DATABASE_USER="${DB_USER}"
  export DATABASE_PASSWORD="${DB_PASSWORD}"
fi

# Render sets $PORT (its router only talks to that port). Default 8080 for
# local `docker run` without -e PORT.
# JAVA_OPTS (e.g. "-Xmx350m -XX:+UseSerialGC") is honored for memory tuning
# on small instances — set it via render.yaml.
# shellcheck disable=SC2086
exec java "-Dserver.port=${PORT:-8080}" ${JAVA_OPTS:-} -jar /app/app.jar
