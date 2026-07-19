#!/bin/sh
set -e

# Docker Compose default: internal service name "backend".
# Render: set BACKEND_URL=https://your-api.onrender.com (no trailing slash).
BACKEND_URL="${BACKEND_URL:-http://backend:8080}"
BACKEND_URL="${BACKEND_URL%/}"
BACKEND_HOST="$(printf '%s' "$BACKEND_URL" | sed -E 's|https?://([^/:]+).*|\1|')"

export BACKEND_URL BACKEND_HOST

envsubst '${BACKEND_URL} ${BACKEND_HOST}' \
  < /etc/nginx/templates/default.conf.template \
  > /etc/nginx/conf.d/default.conf

exec "$@"
