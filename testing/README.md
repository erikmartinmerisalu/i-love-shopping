# Testing & ops extras

Automated tests live with the code: `cd backend && mvn test` (see the table in the root [README](../README.md)).

This folder holds scripts and templates used against a running Docker stack.

| Path | Purpose |
|------|---------|
| `k6/browse.js` | Catalog browse load (home, list, detail, suggest) |
| `k6/checkout.js` | Guest cart → order → sandbox payment |
| `scripts/run-load-tests.sh` | `smoke` or `full` k6 run; writes JSON under `reports/` |
| `scripts/generate-tls.sh` | Self-signed cert for `docker-compose.tls.yml` |
| `bulk_upload_template.csv` / `.json` | Sample files for admin bulk product import |
| `reports/load-test-2026-08-26.md` | Load-test notes and (if filled) measured p95 / VU numbers |

```bash
# API must already be up
./testing/scripts/run-load-tests.sh smoke
./testing/scripts/run-load-tests.sh full
```
