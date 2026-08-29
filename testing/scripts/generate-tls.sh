#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CERT_DIR="$ROOT/certs"
mkdir -p "$CERT_DIR"

KEY="$CERT_DIR/server.key"
CRT="$CERT_DIR/server.crt"

if [[ -f "$KEY" && -f "$CRT" ]]; then
  echo "TLS cert already exists at $CRT"
  echo "Delete those files and re-run this script to regenerate."
  exit 0
fi

if openssl req -help 2>&1 | grep -q -- '-addext'; then
  openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
    -keyout "$KEY" \
    -out "$CRT" \
    -subj "/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
else
  openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
    -keyout "$KEY" \
    -out "$CRT" \
    -subj "/CN=localhost"
fi

chmod 600 "$KEY"
echo "Wrote $CRT and $KEY"
echo "Start HTTPS with:"
echo "  docker compose -f docker-compose.yml -f docker-compose.tls.yml up --build"
echo "Then open https://localhost:3000 and accept the self-signed certificate warning."
