#!/bin/sh
set -e

# Docker Compose default: internal service name "backend".
# Render: set BACKEND_URL=https://your-api.onrender.com (no trailing slash).
BACKEND_URL="${BACKEND_URL:-http://backend:8080}"
BACKEND_URL="${BACKEND_URL%/}"
BACKEND_HOST="$(printf '%s' "$BACKEND_URL" | sed -E 's|https?://([^/:]+).*|\1|')"

export BACKEND_URL BACKEND_HOST

TEMPLATE="/etc/nginx/templates/default.conf.template"
if [ "${TLS_ENABLED:-false}" = "true" ]; then
  TEMPLATE="/etc/nginx/templates/tls.conf.template"
fi

envsubst '${BACKEND_URL} ${BACKEND_HOST}' \
  < "$TEMPLATE" \
  > /etc/nginx/conf.d/default.conf

exec "$@"
