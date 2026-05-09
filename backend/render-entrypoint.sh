#!/usr/bin/env sh
set -eu

if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
  db_without_scheme="${DATABASE_URL#postgresql://}"
  host_and_path="${db_without_scheme#*@}"
  host_port="${host_and_path%%/*}"
  db_from_url="${host_and_path#*/}"
  db_from_url="${db_from_url%%\?*}"

  export SPRING_DATASOURCE_URL="jdbc:postgresql://${host_port}/${DB_NAME:-$db_from_url}"
  export SPRING_DATASOURCE_USERNAME="${DB_USERNAME:-}"
  export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD:-}"
fi

exec java -jar /app/app.jar
