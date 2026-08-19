# Part 3 — Integration Overview (ESTValgus / Lampify)

Part 3 completes the platform against the full project rubric: storefront UX, admin operations, reviews, security hardening, testing evidence, load testing, and Docker/TLS polish. Parts 1–2 delivered auth, catalog search, cart, checkout, payments, orders, encryption for order PII, rate limiting on auth, and a Dockerized stack.

This document defines **six implementation parts**, their dependencies, and the order in which to integrate them.

---

## Part map

| Part | Name | Primary deliverables | Depends on |
|------|------|----------------------|------------|
| **3A** | Storefront UX & content pages | Home, product detail, search results, about, contact, 404, quick cart/search, SEO, responsive layout | Parts 1–2 (catalog, cart APIs) |
| **3B** | Admin platform & RBAC | Roles, admin 2FA gate, CRUD (products, categories, orders, refunds), delivery options, user management, bulk upload | 3A (shared layout/components) |
| **3C** | Reviews & ratings | Star ratings, text reviews, purchase verification, helpfulness votes, moderation | 3B (admin moderation UI) |
| **3D** | Security & infrastructure | TLS (self-signed), encryption audit, token-bucket rate limits, multi-size images | 3B (admin routes to protect) |
| **3E** | Testing & quality evidence | Unit, API integration, user-flow, security tests; demo script; manual test log | 3A–3D features as they land |
| **3F** | Performance & final integration | Load test report, bottleneck analysis, README/ERD updates, docker-compose TLS | 3A–3E |

---

## Recommended integration order

```
Part 1–2 (done)
    │
    ▼
3A Storefront UX ─────────────────────────────┐
    │                                         │
    ▼                                         │
3B Admin & RBAC                               │
    │                                         │
    ├──► 3C Reviews (after admin moderation)  │
    │                                         │
    └──► 3D Security/TLS/images (parallel)    │
              │                               │
              ▼                               │
         3E Tests (continuous, finalize) ◄────┘
              │
              ▼
         3F Load test + README + sign-off
```

**Parallel work:** While 3B admin APIs are being built, frontend 3A pages can consume existing public catalog endpoints. 3D TLS/nginx can start once Docker layout is stable. 3E tests should be added **per feature branch**, not only at the end.

---

## Success criteria (rubric alignment)

Each part has a checklist in `GAP_ANALYSIS.md`. Part 3 is complete when:

1. All rubric items marked **Missing** or **Partial** are **Done**.
2. `testing/DEMO_SCRIPT.md` walkthrough passes without manual workarounds.
3. `mvn test` and frontend test commands pass in CI/Docker.
4. Load test report shows max concurrent users before p95 > 5s and documents throughput.
5. Student can explain CIA, semantic HTML, testing approach, and demonstrate automated tests live.

---

## File index (this folder)

| File | Purpose |
|------|---------|
| `PART3_OVERVIEW.md` | This document — parts, order, success criteria |
| `PART3_INTEGRATION_PLAN.md` | Detailed tasks, API contracts, DB migrations per part |
| `GAP_ANALYSIS.md` | Rubric item vs current implementation status |
| `TEST_STRATEGY.md` | Automated + manual testing approach |
| `DEMO_SCRIPT.md` | Live demonstration script for evaluators |
| `LOAD_TEST_PLAN.md` | k6/JMeter scenarios and report template |
| `SECURITY_AUDIT.md` | Encryption at rest, CIA, 2FA admin, rate limiting |
| `ACCESSIBILITY_SEO.md` | Semantic HTML, alt text, zoom, SEO checklist |

---

## Quick start for developers

```bash
# Baseline (Parts 1–2)
docker compose up --build
cd backend && mvn test

# Part 3 work: create feature branch per sub-part
git checkout -b part3a-storefront
# … implement per PART3_INTEGRATION_PLAN.md
# … add tests per TEST_STRATEGY.md
```

Track progress by updating checkboxes in `GAP_ANALYSIS.md` as features ship.
