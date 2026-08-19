# Part 3 — Load Test Plan

Identify **maximum concurrent users** before response times exceed **5 seconds** and measure **transaction throughput**.

---

## Tooling

**Recommended:** [k6](https://k6.io/) (scriptable, good reports).

**Alternative:** Apache JMeter, Gatling.

Install k6:

```bash
# Linux (example)
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6
```

---

## Environment

| Setting | Value |
|---------|-------|
| Target base URL | `http://localhost:8080/api` (backend direct) or `http://localhost:3000` (via nginx) |
| Database | Fresh Docker stack; seed catalog (V7) |
| Duration | Ramp + sustain + ramp-down |
| SLA | **p95 latency ≤ 5000 ms** for critical endpoints |

Warm up: 30s at 5 VUs before measuring.

---

## Scenarios

### S1 — Browse catalog (read-heavy)

**Simulates:** Home + listing + search.

| Step | Method | Path | Weight |
|------|--------|------|--------|
| Home featured | GET | `/home` | 20% |
| Product list | GET | `/products?page=0&size=20` | 40% |
| Product detail | GET | `/products/{id}` | 25% |
| Search suggest | GET | `/products/suggest?q=lamp` | 15% |

**Load profile:**

1. Ramp 0 → 50 VUs over 2 min
2. Hold 2 min
3. Ramp 50 → 100 → 150 → 200 (increment every 2 min until p95 > 5s)
4. Ramp down 1 min

**Record:** VU count at first p95 breach per endpoint and overall.

### S2 — Checkout transaction (write-heavy)

**Simulates:** Guest add to cart → checkout → sandbox payment.

| Step | Method | Path |
|------|--------|------|
| Add to cart | POST | `/cart/items` |
| Checkout | POST | `/orders` |
| Pay sandbox | POST | `/payments/sandbox` |

**Load profile:** Lower concurrency (10 → 20 → 30 VUs) — stock and DB locks are the bottleneck.

**Throughput metric:** Successful checkouts per minute.

### S3 — Admin API (optional)

10 VUs admin product list + update — validate RBAC overhead is negligible.

---

## Sample k6 script skeleton

Save as `testing/k6/browse.js` (create when executing):

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 50 },
    { duration: '2m', target: 100 },
    { duration: '2m', target: 150 },
    { duration: '2m', target: 200 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],
  },
};

const BASE = __ENV.API_BASE || 'http://localhost:8080/api';

export default function () {
  const list = http.get(`${BASE}/products?page=0&size=20`);
  check(list, { 'list 200': (r) => r.status === 200 });

  if (list.status === 200 && list.json('products')?.length) {
    const id = list.json('products')[0].id;
    http.get(`${BASE}/products/${id}`);
  }

  sleep(1);
}
```

Run:

```bash
k6 run testing/k6/browse.js
k6 run --out json=testing/reports/browse.json testing/k6/browse.js
```

---

## Report template

Save completed report to `testing/reports/load-test-YYYY-MM-DD.md`:

```markdown
# Load Test Report — ESTValgus

**Date:** YYYY-MM-DD  
**Environment:** Docker Compose local, 4 CPU / 8GB RAM  
**Tool:** k6 vX.X

## Summary

| Scenario | Max VUs (p95 ≤ 5s) | Throughput | Notes |
|----------|-------------------|------------|-------|
| S1 Browse | | req/s | |
| S2 Checkout | | orders/min | |

## S1 — Browse catalog

- **Breaking point:** XXX VUs (p95 = X.Xs on GET /products)
- **Peak throughput:** XXX req/s
- **Error rate at peak:** X%

### Latency table (S1 @ XXX VUs)

| Endpoint | p50 | p95 | p99 |
|----------|-----|-----|-----|
| GET /products | | | |
| GET /products/{id} | | | |
| GET /home | | | |

## S2 — Checkout

- **Max sustainable VUs:** XX
- **Throughput:** XX orders/min
- **Failure mode:** stock conflict / DB connection pool

## Bottlenecks identified

1. **Product search query** — full table scan on ILIKE name
   - *Fix:* GIN/trigram index or dedicated search (Elasticsearch)
2. **Connection pool** — Hikari default 10
   - *Fix:* `spring.datasource.hikari.maximum-pool-size=25`
3. **Image serving** — large PNGs
   - *Fix:* multi-size thumbnails (Part 3D)

## Recommendations

- Priority 1: …
- Priority 2: …
```

---

## Baseline expectations (local Docker)

These are planning estimates — **replace with measured values**:

| Endpoint | Expected p95 @ 50 VUs |
|----------|------------------------|
| GET /products | < 500 ms |
| GET /products/{id} | < 300 ms |
| POST /orders | < 2000 ms |

Checkout breaks first under load due to row-level stock locks.

---

## Integration with Part 3F

1. Run S1 and S2 after 3A–3D merged.
2. Apply one optimization from bottlenecks.
3. Re-run S1 to show improvement.
4. Paste summary into root `README.md` **Performance** section.
