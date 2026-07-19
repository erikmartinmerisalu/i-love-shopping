# ESTValgus — Project Checking Checklist

Last reviewed: 2026-07-16

This document maps each viva/testing requirement to the current codebase state, then lists a recommended order for finishing what remains.

**Legend:** ✅ Complete · ⚠️ Partial · ❌ Not started · 📝 Explain only (no code required)

---

## 1. Documentation & Project Foundation

| # | Requirement | Status | What exists | What is missing |
|---|-------------|--------|-------------|-----------------|
| 1.1 | README with project overview, ERD, setup instructions, usage guide | ✅ | `README.md` — overview, expanded ERD, Docker setup, usage guide (auth, browse, search, test), architecture sections | Keep updated as checkout/orders are added |
| 1.2 | B2C e-commerce model | ✅ | ESTValgus sells lighting products directly to consumers; product browsing and cart UI exist | Checkout, orders, and payments not implemented |
| 1.3 | Docker containerization — Docker is the only host prerequisite | ✅ | Root `docker-compose.yml` + `backend/Dockerfile` + `frontend/Dockerfile`; `docker compose up --build` runs postgres + backend + frontend | Host dev mode (Maven/Node) still supported as alternative |
| 1.4 | Student can explain architectural approach and scalability alignment | ✅ | README sections: architecture justification, JWT, ACID, PostgreSQL scalability, search design | Practice viva walkthrough from README |

---

## 2. Authentication & Security

| # | Requirement | Status | What exists | What is missing |
|---|-------------|--------|-------------|-----------------|
| 2.1 | Email-password authentication | ✅ | `/auth/register`, `/auth/login`; BCrypt hashing; `AuthService` + `AuthPage` | — |
| 2.2 | OAuth authentication (email-password + OAuth) | ✅ | Backend + frontend: **Google** on login → `/auth/oauth/login`; `OAuthTokenValidator` also supports Facebook in API | Facebook button not in current UI; needs Google credentials in `.env` |
| 2.3 | CAPTCHA integrated into registration | ✅ | reCAPTCHA v3 on register form; `CaptchaValidator` on backend; skipped when secret unset | Requires `RECAPTCHA_SECRET_KEY` + `VITE_RECAPTCHA_SITE_KEY` for production |
| 2.4 | Student can explain JWT (header, payload, signature) | ✅ | `JwtUtil` + jwt.io demo in `docs/VIVA_PREP.md` | Practice live walkthrough |
| 2.5 | Access tokens stored in memory | ✅ | `AuthContext.tsx` module variable; auto-refresh scheduled before expiry | — |
| 2.6 | Refresh token rotation with single-use validation | ✅ | httpOnly cookie read in `AuthController`; `AuthService.refreshToken()` rotation | — |
| 2.7 | Old refresh tokens rejected; new token issued each refresh | ✅ | Cookie-based refresh + `AuthIntegrationTest.refreshTokenRotationRejectsOldToken` | — |
| 2.8 | Token revocation for access and refresh tokens | ✅ | Refresh revoked on logout; access JTI blacklist in `revoked_access_tokens` | — |
| 2.9 | Password recovery and reset via email | ✅ | Token entity, SMTP email, reset pages, password update | Requires SMTP credentials for real email |
| 2.10 | Optional 2FA (user-enabled) | ✅ | TOTP via `TotpService`, QR code, verify/disable, backup codes; **Profile → Two-factor auth** | Google OAuth users skip password on 2FA setup |
| 2.11 | Input validation on client and server (auth forms) | ✅ | Server rules + `authValidation.ts` client rules (8+ complexity) | — |
| 2.12 | Rate limiting on auth endpoints | ✅ | Time-window rate limiter + `RateLimitingFilterTest` | — |

**Extra — Authentication quality:** Full-stack auth complete. Optional features (OAuth, CAPTCHA, SMTP) need env credentials.

---

## 3. Database Design

| # | Requirement | Status | What exists | What is missing |
|---|-------------|--------|-------------|-----------------|
| 3.1 | ERD with entities, attributes, relationships, PKs, FKs, cardinality, modality | ✅ | Mermaid ERD in `README.md` matches V1–V7 migrations; `users` fields corrected (no `role` column); planned cart/orders labelled | Cart/order tables not migrated yet |
| 3.2 | Student can explain database scalability features | ✅ | README: indexes, HikariCP pooling, read replicas, future caching | Practice explaining with README as reference |
| 3.3 | Student can explain ACID properties and e-commerce importance | ✅ | README: `@Transactional` auth example + planned checkout transaction | Live order demo not yet possible |
| 3.4 | Database migrations aligned with code | ✅ | V1–V7: users, security fields, refresh_tokens, password reset, 2FA backup codes, revoked tokens, catalog tables (`categories`, `products`, `product_images`), seed data | Order/cart tables still not migrated |

---

## 4. Product Catalog & Search

| # | Requirement | Status | What exists | What is missing |
|---|-------------|--------|-------------|-----------------|
| 4.1 | Product data model with all required fields | ✅ | `Product` entity + V6 migration; frontend loads live data from `/api/products` | — |
| 4.2 | Categories with intuitive browsing structure | ✅ | `GET /categories`; `ProductsPage` sidebar loads categories from API facets | — |
| 4.3 | Search implementation (DB design + basic text search) | ✅ | `search_vector` index in V6; Criteria API ILIKE search; debounced search bar in UI | tsvector not used in query layer yet (ILIKE path) |
| 4.4 | Faceted search (price range, brand, category) | ✅ | API facet params + UI category buttons, brand checkboxes, price range sliders | — |
| 4.5 | Sorting (relevance, price, rating) | ✅ | `GET /products?sort=` + sort dropdown in UI | — |
| 4.6 | Product images — file handling and serving | ✅ | `product_images` table, upload endpoint, `/api/uploads/**` serving; UI falls back to placeholder on missing files | Seed placeholder image files not on disk until uploaded |
| 4.7 | Student can explain search implementation | 📝 | Criteria API filters + `search_vector` trigger documented in V6 migration | Prepare viva walkthrough of search/filter/sort flow |

**Extra — Catalog quality:** Full-stack catalog is wired — API-backed listing, search, facets, and sort in `ProductsPage`.

---

## 5. Testing

| # | Requirement | Status | What exists | What is missing |
|---|-------------|--------|-------------|-----------------|
| 5.1 | Student can explain testing approach (automated + manual) | ✅ | `TESTING_STRATEGY.md`, `docs/VIVA_PREP.md` (manual tests by phase), `TESTING_CHECKLIST.md` | Practice demo flow before viva |
| 5.2 | Unit tests | ✅ | `AuthServiceTest` (11), `ProductServiceTest` (2), `RateLimitingFilterTest` (2) | — |
| 5.3 | API integration tests | ✅ | `AuthIntegrationTest` (3), `ProductCatalogIntegrationTest` (5), `AuthControllerTest` (1) | — |
| 5.4 | Security tests | ✅ | `AuthSecurityTest` (5), `ProductCatalogSecurityTest` (5) | — |
| 5.5 | Product catalog tests | ✅ | Integration + security + unit tests for catalog | — |
| 5.6 | Demonstrate and explain test functionality | ✅ | `mvn test` runs **35** tests; see `docs/VIVA_PREP.md` Demo C | Live demo in viva |

---

## Summary Scorecard

| Area | Complete | Partial | Not started |
|------|----------|---------|-------------|
| Documentation & Docker | 4 | 0 | 0 |
| Authentication & Security | 12 | 0 | 0 |
| Database Design | 3 | 1 | 0 (+ 0 explain-only pending) |
| Product Catalog & Search | 5 | 1 | 0 |
| Testing | 6 | 0 | 0 |

**Overall:** All 7 phases complete. Remaining work is outside scope: checkout, orders, payments, server-side cart.

---

## Known Bugs to Fix Early

All critical bugs from Phases 1–3 have been resolved. No known blockers for viva demo.

---

## Recommended Completion Order

Work through these phases in order. Each phase unlocks the next and aligns with the testing/viva requirements.

### Phase 1 — Fix auth end-to-end (unblock security checklist items)
**Goal:** Make implemented backend auth actually work through the UI.
**Status:** ✅ Completed (2026-07-12)

1. ✅ Fix refresh-token cookie reading in `AuthController` (+ verify logout clears cookie and revokes token).
2. ✅ Align CAPTCHA field names; integrate reCAPTCHA v3 on the registration form.
3. ✅ Wire Google and Facebook OAuth buttons to `/auth/oauth/login` with real SDK tokens.
4. ✅ Strengthen frontend validation to match server rules (email format, password complexity, confirm password).
5. ✅ Add automatic access-token refresh in `AuthContext` before expiry.

**Validates:** 2.2, 2.3, 2.5, 2.6, 2.7, 2.11

---

### Phase 2 — Complete remaining auth features
**Goal:** Close all authentication requirement gaps.
**Status:** ✅ Completed (2026-07-14)

6. ✅ Implement password reset: reset token entity/migration, email service (SMTP), token expiry, actual password update, frontend forgot/reset pages.
7. ✅ Implement TOTP 2FA: secret generation, QR code, `/auth/2fa/verify-setup`, `/auth/2fa/verify-login`, backup codes, disable endpoint, frontend setup/verify UI.
8. ✅ Access-token revocation via JTI blacklist (documented in `docs/ACCESS_TOKEN_REVOCATION.md`).
9. ✅ Fix rate limiting to use a proper time window per endpoint/IP.

**Validates:** 2.8, 2.9, 2.10, 2.12

---

### Phase 3 — Expand automated tests (auth & security)
**Goal:** Meet "unit, API integration, and security tests" requirement before building catalog.
**Status:** ✅ Completed (2026-07-14)

10. ✅ Unit tests: email validation, duplicate registration, BCrypt hashing, refresh rotation, logout revocation, 2FA, rate limiting.
11. ✅ Integration tests: full flow register → login → refresh → logout with H2 test database.
12. ✅ Security tests: SQL injection, XSS payloads, invalid JWT/refresh, OAuth invalid token, weak password.
13. ✅ Document testing approach in `TESTING_STRATEGY.md`.

**Validates:** 5.1–5.4, 5.6

---

### Phase 4 — Database & product catalog backend
**Goal:** Replace mock data with a real, searchable catalog.
**Status:** ✅ Completed (2026-07-14)

14. ✅ Create Flyway migrations: `categories`, `products`, `product_images` (with metric + imperial dimensions, brand, stock, rating).
15. ✅ Implement JPA entities, repositories, and `ProductController` / `ProductService`.
16. ✅ Seed sample products and categories (`V7__seed_catalog_data.sql` — 7 categories, 8 products).
17. ✅ Implement basic text search (PostgreSQL `tsvector` index + Criteria API ILIKE on name/description/brand).
18. ✅ Add faceted filters: category, brand, price range (query params + facet metadata in response).
19. ✅ Add sorting: price asc/desc, rating, relevance (default), name.
20. ✅ Implement image upload + static file serving (`FileStorageService`, `WebMvcConfig`, `POST /products/{id}/images`).

**Validates:** 3.1, 3.4, 4.1–4.7

---

### Phase 5 — Product catalog frontend & catalog tests
**Goal:** Connect UI to real API and prove catalog behavior.
**Status:** ✅ Completed (2026-07-14)

21. ✅ Replace `MOCK_PRODUCTS` with API calls in `ProductsPage` (`fetchProducts` via `/api/products`).
22. ✅ Add search bar, facet controls (price sliders, brand checkboxes, category filter), and sort dropdown.
23. ✅ Write unit + integration tests for product listing, search, filters, and sorting (`ProductServiceTest`, `ProductCatalogIntegrationTest`).
24. ✅ Write security tests for product endpoints (`ProductCatalogSecurityTest` — unauthenticated access, SQL injection, XSS).

**Validates:** 4.2–4.5, 5.5

---

### Phase 6 — Docker & documentation (viva-ready)
**Goal:** Single-command setup and clear explanations.
**Status:** ✅ Completed (2026-07-14)

25. ✅ Add `Dockerfile` for backend and frontend; root `docker-compose.yml` for postgres + backend + frontend (Docker-only prerequisite).
26. ✅ Update README: accurate feature status, full setup via `docker compose up`, usage guide (auth, browse, search, test).
27. ✅ Expand ERD: add `refresh_tokens`, `password_reset_tokens`, `product_images`, imperial columns; label cardinality/modality.
28. ✅ Write short sections: JWT overview, ACID in e-commerce (orders/inventory example), PostgreSQL scalability (indexing, connection pooling, read replicas), architecture justification, search design.

**Validates:** 1.1, 1.3, 1.4, 2.4, 3.2, 3.3, 4.7

---

### Phase 7 — Viva preparation (explain & demonstrate)
**Goal:** Be ready to talk through everything assessors ask.
**Status:** ✅ Completed (2026-07-14)

29. ✅ Prepare JWT demo: show token decode (jwt.io), point to header/payload/signature in `JwtUtil`.
30. ✅ Live-demo refresh rotation: use token twice, show DB `used`/`revoked` flags, show rejection.
31. ✅ Live-demo test suite: `mvn test`, explain what each test class covers.
32. ✅ Live-demo search: query + facet + sort on real data.
33. ✅ Review `TESTING_CHECKLIST.md` talking points and update outdated claims — see `docs/VIVA_PREP.md` for full manual testing guide by phase.

**Validates:** all 📝 explain-only items

---

## Quick Reference — What You Can Demo Today

| Topic | Ready to demo? |
|-------|----------------|
| Email/password register & login | ✅ Yes |
| JWT structure explanation (code walkthrough) | ✅ Yes |
| Refresh token rotation (via curl with token in body) | ✅ Yes |
| Refresh via httpOnly cookie in browser | ✅ Yes |
| Logout / token revocation | ✅ Yes |
| OAuth login | ✅ Yes (with provider credentials) |
| CAPTCHA on registration | ✅ Yes (optional when secret unset) |
| 2FA | ✅ Yes |
| Password reset email | ✅ Yes (with SMTP configured) |
| Product search / facets / sort | ✅ Yes |
| Automated test suite | ✅ Yes (35 tests) |
| Full Docker deployment | ✅ Yes (`docker compose up --build`) |

---

## Related Project Documents

| File | Purpose |
|------|---------|
| `README.md` | Project overview, ERD, Docker setup, architecture |
| `docs/VIVA_PREP.md` | Viva demos, manual testing by phase, talking points |
| `TESTING_CHECKLIST.md` | Condensed pre-viva tick-list |
| `TESTING_STRATEGY.md` | Automated testing approach |
| `checking checklist.md` | Requirement audit and phased plan (this file) |
| `TESTING_CHECKLIST.md` | Viva talking points and curl examples |
| `IMPLEMENTATION_STATUS.md` | Earlier security TODO breakdown (partially outdated) |
| `SECURITY_CHECKLIST_FINAL.md` | CAPTCHA/OAuth/cookie implementation notes |
