# Part 3 — Demo Script (Evaluator / Student Presentation)

Use this script to **explain and demonstrate** automated tests and live functionality. Target duration: **12–15 minutes**.

---

## Before you start

```bash
docker compose up --build -d
cd backend && mvn test
# Optional: cd frontend && npm test -- --run
```

Have ready:

- Admin credentials (from seed / `.env`)
- Sandbox card `4242424242424242`
- Browser at http://localhost:3000 (or https if TLS profile enabled)

---

## 1. Testing approach (2 min — oral)

**Talking points:**

- **Unit tests** isolate business rules (cart totals, review averages, rate limits).
- **Integration tests** exercise real HTTP + DB flows (checkout, auth, catalog).
- **Security tests** probe injection, weak passwords, unauthorized admin access.
- **Manual tests** cover responsive layout, zoom, and UX edge cases logged in `MANUAL_TEST_LOG.md`.
- Tests run on every backend build via `mvn test`; new Part 3 features add tests in the same PR.

---

## 2. Run automated tests live (3 min)

### Full suite

```bash
cd backend && mvn test
```

Point out passing count and categories:

| Command | What to highlight |
|---------|-------------------|
| `mvn -Dtest=CartServiceTest test` | Unit — stock limits, line totals |
| `mvn -Dtest=CheckoutFlowIntegrationTest test` | User flow — pay fail → retry → success |
| `mvn -Dtest=AuthSecurityTest test` | Security — injection blocked |
| `mvn -Dtest=RateLimitingFilterTest test` | Rate limiting on auth |
| `mvn -Dtest=ReviewIntegrationTest test` | *(Part 3C)* Purchase-verified review |
| `mvn -Dtest=AdminTwoFactorEnforcementTest test` | *(Part 3B)* Admin 2FA gate |

**Explain one test in detail** (recommended: `CheckoutFlowIntegrationTest`):

1. Registers user, adds product to cart.
2. Places order → reserves stock.
3. Simulates payment failure → stock restored.
4. Retries payment → success → order PAID.

Open the test file and walk through `@Test` method names as documentation of behavior.

---

## 3. Storefront demo (3 min)

| Step | Action | Rubric item |
|------|--------|-------------|
| 1 | Open **Home** — featured products, collections | Home page |
| 2 | Use **quick search** — type "lamp", pick suggestion | Dynamic search |
| 3 | **Products** — toggle grid/list, filter, sort | Listing |
| 4 | Open **product detail** — images, related products, ratings | Detail page |
| 5 | Click **cart icon** — quick cart preview | Quick cart |
| 6 | **Checkout** as guest — sandbox payment | Checkout |
| 7 | **Confirmation** — order number, delivery estimate | Order confirmation |
| 8 | Submit **review** (logged-in buyer) | Reviews |
| 9 | Visit **About** and **Contact** — submit form | Content pages |
| 10 | Navigate to `/nope` — **404** page | Error page |

Resize browser to **320px** and **768px** briefly to show responsiveness.

---

## 4. Admin demo (3 min)

1. Log in as **admin** — show **2FA required** if not enabled.
2. **Products** — create/edit/delete product.
3. **Bulk upload** — import `testing/bulk_upload_template.csv`.
4. **Orders** — change status, assign delivery option.
5. **Refunds** — issue refund on paid order.
6. **Users** — assign CUSTOMER / ADMIN role.
7. **Reviews** — approve pending review → visible on storefront.

---

## 5. Security & encryption (2 min — oral + optional DB peek)

**CIA (Confidentiality, Integrity, Availability):**

| Principle | How the platform addresses it |
|-----------|-------------------------------|
| **Confidentiality** | TLS in transit; AES-GCM encryption for order PII at rest; bcrypt passwords; JWT short-lived access tokens |
| **Integrity** | Server-side validation; stock locking; payment status transitions; Flyway migrations |
| **Availability** | Docker healthchecks; rate limiting; RabbitMQ async emails; load test baseline |

**Encryption demo (optional, dev DB):**

```sql
SELECT email, address_line1 FROM orders LIMIT 1;
-- Values should be Base64 ciphertext blobs, not plaintext
```

Run `mvn -Dtest=EncryptionAtRestTest test` if available.

**Rate limiting:** Show 429 after repeated failed logins (stay within demo limits).

---

## 6. Load test summary (1 min)

Open `testing/reports/load-test-*.md` and present:

- Max concurrent users before p95 response > **5 seconds**
- Throughput (requests/sec, checkouts/min)
- Top bottleneck (e.g. product search query) and proposed fix (index, cache)

---

## 7. Accessibility & SEO (1 min — oral)

- **Semantic HTML:** `<header>`, `<nav>`, `<main>`, `<article>` for products; `<button>` vs `<div>` for actions.
- **SEO:** Unique `<title>` under 60 chars, single H1, descriptive URLs, `alt` on product images.
- **Zoom:** Layout uses relative units; tested at 200% browser zoom.

See `ACCESSIBILITY_SEO.md` for full checklist.

---

## Troubleshooting during demo

| Issue | Fallback |
|-------|----------|
| Docker not up | `docker compose up --build` |
| Test failure | Run single test class; explain known flake |
| Stripe not configured | Use CARD sandbox mode |
| Email not sent | Show API 200 + logs; contact form stores audit row |

---

## Evaluator questions (prep)

1. *Why token bucket vs fixed window?* — Smoother burst handling; refills continuously.
2. *Why encrypt order fields but hash passwords?* — Hashing is one-way for verification; encryption allows decrypt for shipping labels.
3. *How do you verify review authenticity?* — `order_id` + `user_id` must match a PAID order containing the product.
4. *What would you load test first?* — Product search and checkout (highest business impact).
