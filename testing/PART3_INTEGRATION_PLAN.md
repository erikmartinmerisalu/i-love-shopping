# Part 3 — Detailed Integration Plan

Task-level breakdown for each sub-part. Complete tasks in order within a part unless marked **parallel**.

---

## Part 3A — Storefront UX & content pages

**Goal:** Full customer-facing site with shared layout, routing, and rubric pages.

### 3A.1 Shared layout shell

- [ ] Create `Layout.tsx`: header (logo, nav, search, cart badge, account), footer (About, Contact, social).
- [ ] Move shop entry from `/` (AuthPage) to `/products` or new `/` HomePage.
- [ ] Add routes: `/`, `/products`, `/products/:id`, `/search`, `/about`, `/contact`, `*` (404).
- [ ] Global quick cart dropdown in header (reuse `CartContext`, mini line items + subtotal + CTA).
- [ ] Wire `react-helmet-async` (or Vite equivalent) for per-page `<title>` and meta description.

### 3A.2 Home page (`/`)

- [ ] Backend: `GET /api/home` — featured products (`featured=true` flag or curated collection), category collections.
- [ ] Migration `V14__add_product_featured_flag.sql` (if using featured flag).
- [ ] Frontend: hero, featured grid, category tiles, CTA to catalog.

### 3A.3 Product detail page (`/products/:id`)

- [ ] Consume existing `GET /api/products/{id}` (`ProductDetailDto`).
- [ ] Image gallery (primary + thumbnails), dimensions, stock, add-to-cart CTA.
- [ ] Placeholder section for reviews (wired in 3C).
- [ ] Related products: `GET /api/products/{id}/related` (same category, exclude self, limit 4).

### 3A.4 Product listing enhancements (`/products`)

- [ ] Grid / list view toggle (CSS + state).
- [ ] Pagination controls (backend already supports page/size on search).
- [ ] Display average rating stars when 3C is merged (stub with `rating: null` until then).

### 3A.5 Search results page (`/search?q=`)

- [ ] Dedicated page reusing catalog API with query from URL.
- [ ] Show result count, filters sidebar, sort, pagination.
- [ ] Backend: `GET /api/products/suggest?q=` — top 8 title matches for typeahead (**parallel** with page).

### 3A.6 Quick search typeahead

- [ ] Header search input debounced 200ms → `/products/suggest`.
- [ ] Keyboard navigation (up/down/enter), link to full `/search?q=`.

### 3A.7 Static content pages

- [ ] **About** — static React page: mission, team cards, social links (config in `frontend/src/config/site.ts`).
- [ ] **Contact** — form: name, email, subject, message.
  - Backend: `POST /api/contact` → validate, rate-limit, send email via `EmailService`, store optional audit row.
- [ ] **404** — friendly message, link home, suggest search.

### 3A.8 Order confirmation

- [ ] Route `/orders/confirmation/:orderNumber` (or query param after checkout success).
- [ ] Show order summary, reference number, estimated delivery (business rule: e.g. +5 business days).

### 3A.9 Responsive & SEO pass

- [ ] Breakpoint QA at 320, 768, 1024, 1440 (see `ACCESSIBILITY_SEO.md`).
- [ ] Unique titles, one H1 per page, logical heading order.
- [ ] Product URLs: optional slug field later; use `/products/:id` for now.

**3A exit criteria:** Customer can browse home → listing → detail → cart dropdown → checkout → confirmation without login; contact form sends email; 404 works.

---

## Part 3B — Admin platform & RBAC

**Goal:** Secure admin area for catalog, orders, users, delivery, bulk import.

### 3B.1 Roles & authorization

- [ ] Migration `V15__add_user_roles.sql`: `role VARCHAR` enum `CUSTOMER`, `ADMIN` (default CUSTOMER).
- [ ] Seed admin user in migration or Flyway callback (dev only; document credentials in example.env).
- [ ] Spring Security: `@PreAuthorize("hasRole('ADMIN')")` on `/api/admin/**`.
- [ ] Frontend: `AdminRoute` wrapper, `/admin/*` layout.

### 3B.2 Admin 2FA enforcement

- [ ] On admin login: if `role=ADMIN` and `!twoFactorEnabled` → force redirect to 2FA setup before admin access.
- [ ] Backend: reject admin endpoints with 403 if admin without 2FA.
- [ ] Test: `AdminSecurityTest` — admin without 2FA cannot access CRUD.

### 3B.3 Product admin CRUD

- [ ] `AdminProductController`: list (paginated), create, update, delete, upload images.
- [ ] Required fields aligned with `Product` entity: name, description, price, stock, category, brand, dimensions, SKU, active flag.
- [ ] Image upload → `FileStorageService` (existing).

### 3B.4 Category admin CRUD

- [ ] `AdminCategoryController`: CRUD + slug uniqueness validation.

### 3B.5 Order & delivery admin

- [ ] Migration `V16__delivery_options_and_refunds.sql`:
  - `delivery_options` (id, name, price, estimated_days, active)
  - `orders.delivery_option_id`, `orders.estimated_delivery_at`
  - `refunds` (id, order_id, amount, reason, status, created_at)
- [ ] `AdminOrderController`: list all orders, filter by status, update status, assign delivery option.
- [ ] Refund workflow: create refund record, transition order status `REFUNDED` / partial.

### 3B.6 User management

- [ ] `AdminUserController`: list users (paginated), enable/disable, assign role.
- [ ] Cannot demote last admin.

### 3B.7 Bulk upload

- [ ] `POST /api/admin/products/bulk` — `multipart` CSV or JSON array.
- [ ] CSV columns documented in `testing/bulk_upload_template.csv`.
- [ ] Validation report: created/updated/skipped rows with errors.
- [ ] Transactional batch insert (chunk size 50).

### 3B.8 Admin UI (React)

- [ ] Dashboard: counts (orders pending, low stock).
- [ ] Tables: products, categories, orders, users, reviews (stub until 3C).
- [ ] Forms: product editor, order status dropdown, refund dialog, bulk upload dropzone.

**3B exit criteria:** Admin with 2FA can manage full catalog, orders, refunds, delivery, users; bulk CSV import works.

---

## Part 3C — Reviews & ratings

**Goal:** Purchase-verified reviews with stars, helpfulness, moderation.

### 3C.1 Database

- [ ] Migration `V17__create_reviews.sql`:

```sql
-- reviews: id, product_id, user_id, order_id, rating (1-5), body, status (PENDING/APPROVED/REJECTED), created_at
-- review_helpful_votes: review_id, user_id, created_at (unique pair)
-- products: average_rating DECIMAL(3,2), review_count INT (denormalized, updated on approve)
```

### 3C.2 Backend

- [ ] `ReviewService.submitReview(userId, productId, orderId, rating, body)` — verify user purchased product on that order.
- [ ] `ReviewService.listByProduct(productId, sort=helpful|recent)` — only APPROVED for public.
- [ ] `POST /api/reviews/{id}/helpful` — toggle or increment vote (one per user).
- [ ] Recalculate `average_rating` on approve/moderation.
- [ ] Include `averageRating`, `reviewCount` in `ProductDto` / `ProductDetailDto`.

### 3C.3 Admin moderation

- [ ] `AdminReviewController`: list pending, approve, reject.
- [ ] Admin UI tab: moderation queue.

### 3C.4 Frontend

- [ ] Product detail: star display, review list, sort dropdown, submit form (logged-in + purchased check).
- [ ] Product listing: star summary on cards.

**3C exit criteria:** Verified buyer can review; public sees approved reviews sorted by helpfulness; admin moderates.

---

## Part 3D — Security & infrastructure

**Goal:** TLS, encryption completeness, token-bucket rate limits, multi-size images.

### 3D.1 Self-signed TLS in Docker

- [ ] Add `nginx` service or extend frontend container with TLS termination.
- [ ] Script `testing/scripts/generate-tls.sh` → mount `certs/server.crt`, `server.key`.
- [ ] Document browser trust step in README.
- [ ] Set `APP_COOKIE_SECURE=true` when HTTPS enabled.

### 3D.2 Encryption audit

- [ ] Complete matrix in `SECURITY_AUDIT.md`.
- [ ] Gaps to close:
  - User email: consider `@Convert` if rubric requires all PII encrypted (passwords stay bcrypt).
  - Refresh token: verify stored as hash only (already).
  - Order line items: encrypt `product_name` if required for "order details".
- [ ] Integration test: `EncryptionAtRestTest` — persist order, raw JDBC confirms ciphertext in DB.

### 3D.3 Token bucket rate limiting

- [ ] Replace or augment `RateLimitingFilter` with token bucket (Guava `RateLimiter` or custom).
- [ ] Apply tiers: auth (strict), contact form, public API (generous).
- [ ] Update `RateLimitingFilterTest`.

### 3D.4 Multi-size product images

- [ ] On upload: generate `thumb` (150px), `medium` (600px), `full` (original) via ImageIO/Thumbnailator.
- [ ] Store paths in `product_images` or JSON column `variants`.
- [ ] Serve `/uploads/products/{id}/thumb/{file}` etc.
- [ ] Frontend: thumbnail in cart/listing, full in detail gallery.

**3D exit criteria:** HTTPS on localhost, encryption checklist green, token bucket tested, responsive images served.

---

## Part 3E — Testing & quality evidence

**Goal:** Rubric-aligned automated tests + demo readiness.

See `TEST_STRATEGY.md` and `DEMO_SCRIPT.md`.

### New backend tests (add as features ship)

| Test class | Part | Coverage |
|------------|------|----------|
| `HomeControllerTest` | 3A | Featured products endpoint |
| `ContactControllerTest` | 3A | Contact form validation + rate limit |
| `AdminProductControllerTest` | 3B | CRUD + 403 without ADMIN |
| `AdminTwoFactorEnforcementTest` | 3B | Admin blocked without 2FA |
| `BulkUploadIntegrationTest` | 3B | CSV import |
| `ReviewServiceTest` | 3C | Purchase verification, average rating |
| `ReviewIntegrationTest` | 3C | Submit, helpful vote, sort |
| `EncryptionAtRestTest` | 3D | DB ciphertext |
| `TokenBucketRateLimitTest` | 3D | Bucket refill behavior |

### Frontend tests (optional but recommended)

- [ ] Vitest + React Testing Library: `ContactForm`, `ProductDetail`, `QuickSearch`.
- [ ] Playwright E2E: `testing/e2e/checkout-and-review.spec.ts` — home → product → cart → checkout (sandbox pay).

### Manual test log

- [ ] Maintain `testing/MANUAL_TEST_LOG.md` — date, scenario, result, viewport.

**3E exit criteria:** All new tests green; demo script runnable in < 15 minutes.

---

## Part 3F — Performance & final integration

**Goal:** Load test evidence, README completion, release checklist.

### 3F.1 Load testing

- [ ] Execute scenarios in `LOAD_TEST_PLAN.md` (k6 recommended).
- [ ] Save report to `testing/reports/load-test-YYYY-MM-DD.md`.
- [ ] Document: max VUs before p95 > 5s, throughput (orders/min, requests/sec), bottlenecks + fixes.

### 3F.2 Performance optimizations (based on load test)

- [ ] DB indexes on `products(name)`, `reviews(product_id, status)`, `orders(status)`.
- [ ] Connection pool tuning in `application.properties`.
- [ ] Optional: Redis cache for home/featured (only if load test proves need).

### 3F.3 README updates

- [ ] Add Part 3 sections: admin, reviews, contact, TLS setup.
- [ ] Extend ERD with reviews, roles, delivery, refunds.
- [ ] Link load test summary and security audit.

### 3F.4 Final checklist

- [ ] All items in `GAP_ANALYSIS.md` → Done.
- [ ] `docker compose up --build` with TLS profile works.
- [ ] Oral prep: CIA, semantic HTML, testing approach (see supporting docs).

**3F exit criteria:** Load report in repo; README complete; gap analysis all Done.

---

## Migration sequence summary

| Version | Part | Description |
|---------|------|-------------|
| V14 | 3A | Product featured flag / home collections |
| V15 | 3B | User roles |
| V16 | 3B | Delivery options, refunds, order delivery fields |
| V17 | 3C | Reviews, helpful votes, product rating aggregates |
| V18 | 3D | Product image size variants (if new columns) |

Flyway versions must stay monotonic; never edit applied migrations.

---

## Branching strategy

```
main
 ├── part3a/storefront
 ├── part3b/admin-rbac      (branch from main after 3A merge, or rebase)
 ├── part3c/reviews         (after 3B)
 ├── part3d/security-tls    (can branch from main early)
 ├── part3e/tests           (ongoing; merge with each part)
 └── part3f/load-readme     (final)
```

Merge order: **3A → 3B → 3C → 3D → 3E → 3F**, with 3D able to merge in parallel after 3B if no conflicts.
