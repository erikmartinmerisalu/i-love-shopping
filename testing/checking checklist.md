# ESTValgus â€” Project Checking Checklist

Last reviewed: 2026-07-16

This document maps each viva/testing requirement to the current codebase state, then lists a recommended order for finishing what remains.

**Legend:** âœ… Complete Â· âš ï¸ Partial Â· âŒ Not started Â· ðŸ“ Explain only (no code required)

---

## 1. Documentation & Project Foundation

| # | Requirement | Status | What exists | What is missing |
|---|-------------|--------|-------------|-----------------|
| 1.1 | README with project overview, ERD, setup instructions, usage guide | âœ… | `README.md` â€” overview, expanded ERD, Docker setup, usage guide (auth, browse, search, test), architecture sections | Keep updated as checkout/orders are added |
| 1.2 | B2C e-commerce model | âœ… | ESTValgus sells lighting products directly to consumers; product browsing and cart UI exist | Checkout, orders, and payments not implemented |
| 1.3 | Docker containerization â€” Docker is the only host prerequisite | âœ… | Root `docker-compose.yml` + `backend/Dockerfile` + `frontend/Dockerfile`; `docker compose up --build` runs postgres + backend + frontend | Host dev mode (Maven/Node) still supported as alternative |
| 1.4 | Student can explain architectural approach and scalability alignment | âœ… | README sections: architecture justification, JWT, ACID, PostgreSQL scalability, search design | Practice viva walkthrough from README |

---

## 2. Authentication & Security

| # | Requirement | Status | What exists | What is missing |
|---|-------------|--------|-------------|-----------------|
| 2.1 | Email-password authentication | âœ… | `/auth/register`, `/auth/login`; BCrypt hashing; `AuthService` + `AuthPage` | â€” |
| 2.2 | OAuth authentication (email-password + OAuth) | âœ… | Backend + frontend: **Google** on login â†’ `/auth/oauth/login`; `OAuthTokenValidator` also supports Facebook in API | Facebook button not in current UI; needs Google credentials in `.env` |
| 2.3 | CAPTCHA integrated into registration | âœ… | reCAPTCHA v3 on register form; `CaptchaValidator` on backend; skipped when secret unset | Requires `RECAPTCHA_SECRET_KEY` + `VITE_RECAPTCHA_SITE_KEY` for production |
| 2.4 | Student can explain JWT (header, payload, signature) | âœ… | `JwtUtil` + jwt.io demo in `testing/` | Practice live walkthrough |
| 2.5 | Access tokens stored in memory | âœ… | `AuthContext.tsx` module variable; auto-refresh scheduled before expiry | â€” |
| 2.6 | Refresh token rotation with single-use validation | âœ… | httpOnly cookie read in `AuthController`; `AuthService.refreshToken()` rotation | â€” |
| 2.7 | Old refresh tokens rejected; new token issued each refresh | âœ… | Cookie-based refresh + `AuthIntegrationTest.refreshTokenRotationRejectsOldToken` | â€” |
| 2.8 | Token revocation for access and refresh tokens | âœ… | Refresh revoked on logout; access JTI blacklist in `revoked_access_tokens` | â€” |
| 2.9 | Password recovery and reset via email | âœ… | Token entity, SMTP email, reset pages, password update | Requires SMTP credentials for real email |
| 2.10 | Optional 2FA (user-enabled) | âœ… | TOTP via `TotpService`, QR code, verify/disable, backup codes; **Profile â†’ Two-factor auth** | Google OAuth users skip password on 2FA setup |
| 2.11 | Input validation on client and server (auth forms) | âœ… | Server rules + `authValidation.ts` client rules (8+ complexity) | â€” |
| 2.12 | Rate limiting on auth endpoints | âœ… | Time-window rate limiter + `RateLimitingFilterTest` | â€” |

**Extra â€” Authentication quality:** Full-stack auth complete. Optional features (OAuth, CAPTCHA, SMTP) need env credentials.

---

## 3. Database Design

| # | Requirement | Status | What exists | What is missing |
|---|-------------|--------|-------------|-----------------|
| 3.1 | ERD with entities, attributes, relationships, PKs, FKs, cardinality, modality | âœ… | Mermaid ERD in `README.md` matches V1â€“V7 migrations; `users` fields corrected (no `role` column); planned cart/orders labelled | Cart/order tables not migrated yet |
| 3.2 | Student can explain database scalability features | âœ… | README: indexes, HikariCP pooling, read replicas, future caching | Practice explaining with README as reference |
| 3.3 | Student can explain ACID properties and e-commerce importance | âœ… | README: `@Transactional` auth example + planned checkout transaction | Live order demo not yet possible |
| 3.4 | Database migrations aligned with code | âœ… | V1â€“V7: users, security fields, refresh_tokens, password reset, 2FA backup codes, revoked tokens, catalog tables (`categories`, `products`, `product_images`), seed data | Order/cart tables still not migrated |

---

## 4. Product Catalog & Search

| # | Requirement | Status | What exists | What is missing |
|---|-------------|--------|-------------|-----------------|
| 4.1 | Product data model with all required fields | âœ… | `Product` entity + V6 migration; frontend loads live data from `/api/products` | â€” |
| 4.2 | Categories with intuitive browsing structure | âœ… | `GET /categories`; `ProductsPage` sidebar loads categories from API facets | â€” |
| 4.3 | Search implementation (DB design + basic text search) | âœ… | `search_vector` index in V6; Criteria API ILIKE search; debounced search bar in UI | tsvector not used in query layer yet (ILIKE path) |
| 4.4 | Faceted search (price range, brand, category) | âœ… | API facet params + UI category buttons, brand checkboxes, price range sliders | â€” |
| 4.5 | Sorting (relevance, price, rating) | âœ… | `GET /products?sort=` + sort dropdown in UI | â€” |
| 4.6 | Product images â€” file handling and serving | âœ… | `product_images` table, upload endpoint, `/api/uploads/**` serving; UI falls back to placeholder on missing files | Seed placeholder image files not on disk until uploaded |
| 4.7 | Student can explain search implementation | ðŸ“ | Criteria API filters + `search_vector` trigger documented in V6 migration | Prepare viva walkthrough of search/filter/sort flow |

**Extra â€” Catalog quality:** Full-stack catalog is wired â€” API-backed listing, search, facets, and sort in `ProductsPage`.

---

## 5. Testing

| # | Requirement | Status | What exists | What is missing |
|---|-------------|--------|-------------|-----------------|
| 5.1 | Student can explain testing approach (automated + manual) | âœ… | `testing/`, `testing/` (manual tests by phase), `testing/` | Practice demo flow before viva |
| 5.2 | Unit tests | âœ… | `AuthServiceTest` (11), `ProductServiceTest` (2), `RateLimitingFilterTest` (2) | â€” |
| 5.3 | API integration tests | âœ… | `AuthIntegrationTest` (3), `ProductCatalogIntegrationTest` (5), `AuthControllerTest` (1) | â€” |
| 5.4 | Security tests | âœ… | `AuthSecurityTest` (5), `ProductCatalogSecurityTest` (5) | â€” |
| 5.5 | Product catalog tests | âœ… | Integration + security + unit tests for catalog | â€” |
| 5.6 | Demonstrate and explain test functionality | âœ… | `mvn test` runs **52** tests; see `testing/` Demo C | Live demo in viva |

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

All critical bugs from Phases 1â€“3 have been resolved. No known blockers for viva demo.

---

## Recommended Completion Order

Work through these phases in order. Each phase unlocks the next and aligns with the testing/viva requirements.

### Phase 1 â€” Fix auth end-to-end (unblock security checklist items)
**Goal:** Make implemented backend auth actually work through the UI.
**Status:** âœ… Completed (2026-07-12)

1. âœ… Fix refresh-token cookie reading in `AuthController` (+ verify logout clears cookie and revokes token).
2. âœ… Align CAPTCHA field names; integrate reCAPTCHA v3 on the registration form.
3. âœ… Wire Google and Facebook OAuth buttons to `/auth/oauth/login` with real SDK tokens.
4. âœ… Strengthen frontend validation to match server rules (email format, password complexity, confirm password).
5. âœ… Add automatic access-token refresh in `AuthContext` before expiry.

**Validates:** 2.2, 2.3, 2.5, 2.6, 2.7, 2.11

---

### Phase 2 â€” Complete remaining auth features
**Goal:** Close all authentication requirement gaps.
**Status:** âœ… Completed (2026-07-14)

6. âœ… Implement password reset: reset token entity/migration, email service (SMTP), token expiry, actual password update, frontend forgot/reset pages.
7. âœ… Implement TOTP 2FA: secret generation, QR code, `/auth/2fa/verify-setup`, `/auth/2fa/verify-login`, backup codes, disable endpoint, frontend setup/verify UI.
8. âœ… Access-token revocation via JTI blacklist (documented in `docs/ACCESS_TOKEN_REVOCATION.md`).
9. âœ… Fix rate limiting to use a proper time window per endpoint/IP.

**Validates:** 2.8, 2.9, 2.10, 2.12

---

### Phase 3 â€” Expand automated tests (auth & security)
**Goal:** Meet "unit, API integration, and security tests" requirement before building catalog.
**Status:** âœ… Completed (2026-07-14)

10. âœ… Unit tests: email validation, duplicate registration, BCrypt hashing, refresh rotation, logout revocation, 2FA, rate limiting.
11. âœ… Integration tests: full flow register â†’ login â†’ refresh â†’ logout with H2 test database.
12. âœ… Security tests: SQL injection, XSS payloads, invalid JWT/refresh, OAuth invalid token, weak password.
13. âœ… Document testing approach in `testing/`.

**Validates:** 5.1â€“5.4, 5.6

---

### Phase 4 â€” Database & product catalog backend
**Goal:** Replace mock data with a real, searchable catalog.
**Status:** âœ… Completed (2026-07-14)

14. âœ… Create Flyway migrations: `categories`, `products`, `product_images` (with metric + imperial dimensions, brand, stock, rating).
15. âœ… Implement JPA entities, repositories, and `ProductController` / `ProductService`.
16. âœ… Seed sample products and categories (`V7__seed_catalog_data.sql` â€” 7 categories, 8 products).
17. âœ… Implement basic text search (PostgreSQL `tsvector` index + Criteria API ILIKE on name/description/brand).
18. âœ… Add faceted filters: category, brand, price range (query params + facet metadata in response).
19. âœ… Add sorting: price asc/desc, rating, relevance (default), name.
20. âœ… Implement image upload + static file serving (`FileStorageService`, `WebMvcConfig`, `POST /products/{id}/images`).

**Validates:** 3.1, 3.4, 4.1â€“4.7

---

### Phase 5 â€” Product catalog frontend & catalog tests
**Goal:** Connect UI to real API and prove catalog behavior.
**Status:** âœ… Completed (2026-07-14)

21. âœ… Replace `MOCK_PRODUCTS` with API calls in `ProductsPage` (`fetchProducts` via `/api/products`).
22. âœ… Add search bar, facet controls (price sliders, brand checkboxes, category filter), and sort dropdown.
23. âœ… Write unit + integration tests for product listing, search, filters, and sorting (`ProductServiceTest`, `ProductCatalogIntegrationTest`).
24. âœ… Write security tests for product endpoints (`ProductCatalogSecurityTest` â€” unauthenticated access, SQL injection, XSS).

**Validates:** 4.2â€“4.5, 5.5

---

### Phase 6 â€” Docker & documentation (viva-ready)
**Goal:** Single-command setup and clear explanations.
**Status:** âœ… Completed (2026-07-14)

25. âœ… Add `Dockerfile` for backend and frontend; root `docker-compose.yml` for postgres + backend + frontend (Docker-only prerequisite).
26. âœ… Update README: accurate feature status, full setup via `docker compose up`, usage guide (auth, browse, search, test).
27. âœ… Expand ERD: add `refresh_tokens`, `password_reset_tokens`, `product_images`, imperial columns; label cardinality/modality.
28. âœ… Write short sections: JWT overview, ACID in e-commerce (orders/inventory example), PostgreSQL scalability (indexing, connection pooling, read replicas), architecture justification, search design.

**Validates:** 1.1, 1.3, 1.4, 2.4, 3.2, 3.3, 4.7

---

### Phase 7 â€” Viva preparation (explain & demonstrate)
**Goal:** Be ready to talk through everything assessors ask.
**Status:** âœ… Completed (2026-07-14)

29. âœ… Prepare JWT demo: show token decode (jwt.io), point to header/payload/signature in `JwtUtil`.
30. âœ… Live-demo refresh rotation: use token twice, show DB `used`/`revoked` flags, show rejection.
31. âœ… Live-demo test suite: `mvn test`, explain what each test class covers.
32. âœ… Live-demo search: query + facet + sort on real data.
33. âœ… Review `testing/` talking points and update outdated claims â€” see `testing/` for full manual testing guide by phase.

**Validates:** all ðŸ“ explain-only items

---

## Quick Reference â€” What You Can Demo Today

| Topic | Ready to demo? |
|-------|----------------|
| Email/password register & login | âœ… Yes |
| JWT structure explanation (code walkthrough) | âœ… Yes |
| Refresh token rotation (via curl with token in body) | âœ… Yes |
| Refresh via httpOnly cookie in browser | âœ… Yes |
| Logout / token revocation | âœ… Yes |
| OAuth login | âœ… Yes (with provider credentials) |
| CAPTCHA on registration | âœ… Yes (optional when secret unset) |
| 2FA | âœ… Yes |
| Password reset email | âœ… Yes (with SMTP configured) |
| Product search / facets / sort | âœ… Yes |
| Automated test suite | âœ… Yes (35 tests) |
| Full Docker deployment | âœ… Yes (`docker compose up --build`) |

---

## Related Project Documents

| File | Purpose |
|------|---------|
| `README.md` | Project overview, ERD, Docker setup, architecture |
| `testing/` | Viva / audit notes, Part 2 plan, PCI & RabbitMQ notes (removable) |
| `checking checklist.md` | Requirement audit and phased plan (this file) |
| `IMPLEMENTATION_STATUS.md` | Earlier security TODO breakdown (partially outdated) |
| `SECURITY_CHECKLIST_FINAL.md` | CAPTCHA/OAuth/cookie implementation notes |
