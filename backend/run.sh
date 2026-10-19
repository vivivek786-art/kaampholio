#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ -f ../.env ]]; then
  set -a
  source ../.env
  set +a
fi
: "${DB_PASSWORD:?Set DB_PASSWORD in the root .env}"
: "${APP_PASSWORD:?Set APP_PASSWORD in the root .env}"
exec mvn spring-boot:run
