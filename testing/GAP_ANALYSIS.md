# Part 3 — Gap Analysis (Rubric vs Current Codebase)

Status key: **Done** | **Partial** | **Missing**

Last reviewed against repo state: Part 3A implemented.

---

## Documentation & architecture

| Requirement | Status | Notes |
|-------------|--------|-------|
| README with overview, ERD, performance report, setup, usage | **Partial** | README has setup, usage, ERD, tests table. Missing: performance/load report, Part 3 pages, admin, reviews |
| Entity relationship diagram | **Partial** | Commerce + auth ERD in README. Needs reviews, roles, delivery options, refunds |
| Docker containerization | **Done** | `docker-compose.yml`, backend/frontend Dockerfiles |
| Host prereqs limited to Docker + payment CLI | **Done** | Documented in README |

---

## Storefront & UX

| Requirement | Status | Notes |
|-------------|--------|-------|
| Home page — featured products & collections | **Done** | `HomePage`, `GET /api/home`, V14 featured flag |
| Product listing — details, ratings, filters, grid/list, search, sort | **Done** | `CatalogBrowsePage` — grid/list, pagination, star display from seed ratings |
| Product detail page — info, images, reviews, CTA, related products | **Partial** | `ProductDetailPage` complete except live reviews (Part 3C) |
| Shopping cart — thumbnails, prices, qty, total, CTA | **Done** | `Cart.tsx`, cart API |
| Quick cart preview (dropdown/popup) | **Done** | `QuickCartDropdown` in global header |
| Checkout — guest/signed-in, address, shipping, payment | **Done** | `CheckoutPage` |
| Order confirmation — summary, delivery estimate, reference | **Done** | `/orders/confirmation/:orderNumber` |
| Search results — filtering, sorting, count, pagination | **Done** | `/search` + `CatalogBrowsePage` |
| Quick search with dynamic suggestions | **Done** | `QuickSearch` + `GET /products/suggest` |
| Contact/Support — functioning contact form | **Done** | `ContactPage`, `POST /api/contact` |
| About page — company info, mission, team, social links | **Done** | `AboutPage`, `config/site.ts` |
| Error page 404 — catch-all | **Done** | `NotFoundPage`, `*` route |
| Responsive viewports 320/768/1024/1440 | **Partial** | Responsive Tailwind layout; manual QA pending |
| UI/UX consistency | **Done** | Shared `Layout` shell across storefront pages |

---

## Reviews & ratings

| Requirement | Status | Notes |
|-------------|--------|-------|
| Star rating system with average from all reviews | **Missing** | No `reviews` table or rating fields on product |
| Text reviews for purchased products | **Missing** | |
| Review sorting by helpfulness votes | **Missing** | |
| Admin review moderation | **Missing** | Depends on admin panel |

---

## Admin

| Requirement | Status | Notes |
|-------------|--------|-------|
| Product CRUD (all required fields) | **Missing** | Read-only catalog via public API |
| Category CRUD | **Missing** | Read-only `CategoryController` |
| Order management & status updates | **Partial** | User-facing orders; no admin order list/update |
| Refund management | **Missing** | |
| Delivery options management | **Missing** | |
| View all users & assign roles | **Missing** | No `role` on `User` entity |
| Bulk upload products (JSON/CSV) | **Missing** | |
| Admin 2FA enforced for all admin accounts | **Missing** | 2FA optional for all users via Profile |
| Admin page — CRUD, orders, users, moderation, bulk | **Missing** | |

---

## Security

| Requirement | Status | Notes |
|-------------|--------|-------|
| Self-signed TLS certificate | **Missing** | HTTP only in docker-compose |
| Encryption at rest — user credentials | **Partial** | Passwords bcrypt-hashed (not encrypted); acceptable for credentials |
| Encryption at rest — PII | **Partial** | Order contact fields encrypted (V13, `EncryptedStringConverter`) |
| Encryption at rest — shipping addresses | **Done** | On `Order` entity fields |
| Encryption at rest — order details | **Partial** | Line items not encrypted; contact/shipping encrypted |
| Encryption at rest — session tokens | **Partial** | Refresh tokens stored hashed; access tokens JWT (not DB); revoked JTIs in DB |
| Token bucket rate limiting | **Partial** | Fixed window counter on `/auth/*` only; not token bucket |
| CIA principles (student explanation) | **Missing** | Document in `SECURITY_AUDIT.md` + oral prep |
| reCAPTCHA / OAuth / JWT | **Done** | From Part 1–2 |

---

## Images & media

| Requirement | Status | Notes |
|-------------|--------|-------|
| Multiple image sizes (thumbnail, full) | **Missing** | Single `urlPath` per `ProductImage`; placeholder PNGs |
| Descriptive alt text on meaningful images | **Partial** | Some img tags; inconsistent product alt |

---

## Accessibility & SEO

| Requirement | Status | Notes |
|-------------|--------|-------|
| SEO — title tags < 60 chars | **Missing** | No react-helmet or per-route titles |
| SEO — heading hierarchy H2–H6 | **Partial** | Inconsistent across pages |
| SEO — logical URL structure | **Partial** | `/products` ok; missing `/products/:slug` |
| Semantic HTML (student explanation) | **Missing** | See `ACCESSIBILITY_SEO.md` |
| Text readable at 200% zoom | **Partial** | Needs audit (rem units, no fixed px overflow) |
| Alt text on all meaningful images | **Partial** | |

---

## Testing & performance

| Requirement | Status | Notes |
|-------------|--------|-------|
| Unit tests | **Done** | Cart, Order, Auth, Product, RateLimit |
| API integration tests | **Done** | Checkout, Auth, Catalog |
| User flow tests | **Partial** | Checkout integration covers main flow; no browser E2E |
| Security tests | **Done** | AuthSecurity, CatalogSecurity, OAuthSecurity |
| Student demo of tests | **Missing** | Use `DEMO_SCRIPT.md` |
| Load test — max users before p95 > 5s | **Missing** | See `LOAD_TEST_PLAN.md` |
| Load test — transaction throughput | **Missing** | |
| Bottleneck identification & proposals | **Missing** | |

---

## Priority order for closing gaps

1. **3A** — Unblocks demo of full customer journey (home → detail → cart → checkout).
2. **3B** — Required for admin rubric and review moderation.
3. **3C** — Ratings on listing/detail pages.
4. **3D** — TLS + encryption audit + token bucket (security rubric).
5. **3E** — Tests tied to each part; demo script rehearsal.
6. **3F** — Load test report + README performance section.

Update this file as each item moves to **Done**.
