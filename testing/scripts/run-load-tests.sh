#!/usr/bin/env bash
# Run k6 load tests against a live stack (docker compose up).
# Usage:
#   ./testing/scripts/run-load-tests.sh smoke
#   ./testing/scripts/run-load-tests.sh full
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PROFILE="${1:-smoke}"
API_BASE="${API_BASE:-http://localhost:8080/api}"
REPORT_DIR="${ROOT}/testing/reports"
mkdir -p "${REPORT_DIR}"
STAMP="$(date +%Y-%m-%d)"

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is not installed. Install: https://k6.io/docs/get-started/installation/"
  exit 1
fi

echo "Checking ${API_BASE}/home ..."
code="$(curl -s -o /dev/null -w '%{http_code}' "${API_BASE}/home" || true)"
if [[ "${code}" != "200" ]]; then
  echo "API did not return 200 (got ${code}). Start the stack first:"
  echo "  docker compose up --build"
  exit 1
fi

echo "S1 browse (${PROFILE})..."
k6 run \
  -e "API_BASE=${API_BASE}" \
  -e "PROFILE=${PROFILE}" \
  --summary-export "${REPORT_DIR}/browse-${STAMP}-${PROFILE}.json" \
  "${ROOT}/testing/k6/browse.js"

echo "S2 checkout (${PROFILE})..."
k6 run \
  -e "API_BASE=${API_BASE}" \
  -e "PROFILE=${PROFILE}" \
  --summary-export "${REPORT_DIR}/checkout-${STAMP}-${PROFILE}.json" \
  "${ROOT}/testing/k6/checkout.js"

echo "Summaries written under ${REPORT_DIR}/"
echo "Copy p95 / reqs / counters into testing/reports/load-test-${STAMP}.md"
