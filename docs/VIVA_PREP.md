# Viva Preparation & Manual Testing Guide

This guide covers **live demo scripts** for assessors and **step-by-step manual tests** for everything built across Phases 1–7.

**Prerequisites:** Stack running via Docker or dev mode.

```bash
# Docker (recommended for demos)
docker compose up --build

# Or dev mode
cd backend && docker compose up -d postgres && mvn spring-boot:run
cd frontend && npm run dev
```

| Mode | Storefront | API |
|------|------------|-----|
| Docker | http://localhost:3000 | http://localhost:8080/api |
| Dev | http://localhost:5173 | http://localhost:8080/api |

---

## Part 1 — Live demo scripts (say these out loud)

### Demo A: JWT structure (Phase 7 / checklist item 29)

**What to say:** "ESTValgus uses HS256-signed JWTs. Each token has three parts: header, payload, and signature."

**Steps:**

1. Register or log in via the UI (or curl below).
2. Open browser DevTools → **Application** → look for the access token in memory (not localStorage — it's held in a module variable in `AuthContext.tsx`).
3. Copy the access token from the login response (Network tab → `/api/auth/login` → response body `accessToken`).
4. Paste at [jwt.io](https://jwt.io) and point out:
   - **Header:** `alg: HS256`, `typ: JWT`
   - **Payload:** `sub` (email), `jti` (unique ID for revocation), `type` (`access` or `refresh`), `exp`, `iat`
   - **Signature:** HMAC over `header.payload` — proves the token wasn't tampered with

**Code to open:** `backend/src/main/java/com/lampify/security/JwtUtil.java` — `generateAccessToken()` and `validateToken()`.

**Key line to reference:**

```java
Jwts.builder()
    .setSubject(subject)       // email
    .setId(jti)                // revocation tracking
    .claim("type", "access")
    .setExpiration(expiration)
    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
    .compact();
```

---

### Demo B: Refresh token rotation (item 30)

**What to say:** "Refresh tokens are single-use. When refreshed, the old token is marked `used` and `revoked`, and a new one is issued via httpOnly cookie."

**Browser demo:**

1. Log in at http://localhost:3000
2. DevTools → **Application** → **Cookies** → note `refreshToken` (httpOnly)
3. DevTools → **Network** → trigger a refresh (wait ~14 min, or call refresh manually):

```bash
# Save cookies from login, then refresh twice
curl -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"StrongP@ss1"}'

curl -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json"

# Second refresh with OLD cookie file should fail
cp cookies.txt old-cookies.txt
curl -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/auth/refresh

curl -b old-cookies.txt -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json"
# Expected: 401 — "Refresh token has been invalidated"
```

**Database proof:**

```bash
docker compose exec postgres psql -U postgres -d lampify_db \
  -c "SELECT token, used, revoked FROM refresh_tokens ORDER BY created_at DESC LIMIT 3;"
```

Point at `used=true`, `revoked=true` on the old row.

**Code:** `AuthService.refreshToken()` — sets `used` and `revoked` before issuing new token.

---

### Demo C: Automated test suite (item 31)

```bash
cd backend && mvn test
```

**Expected:** `Tests run: 34, Failures: 0, Errors: 0`

| Test class | Tests | What it proves |
|------------|-------|----------------|
| `AuthServiceTest` | 11 | Password rules, BCrypt hashing, duplicate email, refresh rotation, logout + JTI blacklist, password reset, 2FA gate |
| `AuthIntegrationTest` | 3 | Full register → login → refresh → logout flow; rotation rejects old cookie; reset with invalid token |
| `AuthSecurityTest` | 5 | SQL injection, XSS, invalid refresh/OAuth tokens, weak password |
| `AuthControllerTest` | 1 | Register endpoint returns 400 for weak password |
| `RateLimitingFilterTest` | 2 | Requests within limit pass; excess returns 429 |
| `ProductCatalogIntegrationTest` | 5 | Listing, search, category/brand/price filters, sort, pagination, detail |
| `ProductCatalogSecurityTest` | 5 | Public catalog access, SQL/XSS in search, 404, unknown sort |
| `ProductServiceTest` | 2 | DTO mapping, page size clamping |

**Run one class:**

```bash
mvn -Dtest=AuthIntegrationTest test
```

---

### Demo D: Product search, facets, and sort (item 32)

**UI demo (http://localhost:3000):**

1. Log in → products page
2. Search `bulb` → should show Smart LED Bulb
3. Click **Desk Lamps** category → 2 products
4. Check a **brand** checkbox → filters further
5. Move **price sliders** → narrow range
6. Change **sort** to "Price: Low to High" → order updates
7. Add item to cart → toast appears

**API demo:**

```bash
curl "http://localhost:8080/api/products?search=bulb&sort=price_asc"
curl "http://localhost:8080/api/products?category=desk-lamps&minPrice=40&maxPrice=60"
curl "http://localhost:8080/api/categories"
```

**What to explain:** Criteria API ILIKE search + facet query params; `search_vector` TSVECTOR index in DB for future full-text; response includes facet metadata for the UI.

---

## Part 2 — Manual testing by phase

Use this as a checklist before your viva. Tick each box as you verify.

### Phase 1 — Auth end-to-end

| # | Test | How | Expected |
|---|------|-----|----------|
| 1.1 | Register with weak password | UI: password `short1` | Error about complexity (8+ chars, upper, lower, digit, special) |
| 1.2 | Register with strong password | UI: `StrongP@ss1` | Success → redirected to products |
| 1.3 | Login | Log out, log in again | Access to products page |
| 1.4 | Refresh cookie | DevTools → Application → Cookies | `refreshToken` httpOnly cookie present after login |
| 1.5 | Auto-refresh | Network tab: wait or call `/api/auth/refresh` | New access token without re-login |
| 1.6 | Client validation | Register form: mismatch confirm password | Error before submit |
| 1.7 | OAuth (optional) | Set `VITE_GOOGLE_CLIENT_ID` in frontend, click Google | Login succeeds if credentials configured |
| 1.8 | CAPTCHA (optional) | Set `VITE_RECAPTCHA_SITE_KEY` + `RECAPTCHA_SECRET_KEY` | Registration works with invisible reCAPTCHA |

---

### Phase 2 — Remaining auth features

| # | Test | How | Expected |
|---|------|-----|----------|
| 2.1 | Forgot password | Click "Forgot password", enter email | Success message (no email enumeration) |
| 2.2 | Reset password | Use link from email/logs, set new password | Can log in with new password |
| 2.3 | 2FA setup | Profile → Two-factor auth → Generate QR code | QR code shown; scan with authenticator app |
| 2.4 | 2FA verify setup | Enter 6-digit code from app | 2FA enabled; backup codes shown |
| 2.5 | 2FA login | Log out, log in | Prompt for TOTP code; login completes after code |
| 2.6 | 2FA disable | Profile → Two-factor auth → disable with code | 2FA off; normal login works |
| 2.7 | Logout revokes refresh | Log in, log out, try `/api/auth/refresh` with old cookie | 401 unauthorized |
| 2.8 | Access token revocation | Log in, copy access token, log out, reuse token on protected endpoint | 401 (JTI blacklisted) |
| 2.9 | Rate limiting | Send 100+ rapid login requests (script) | Eventually 429 Too Many Requests |

**Access token revocation curl:**

```bash
TOKEN="<access_token_from_login>"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/auth/2fa/setup ...
# After logout, same TOKEN should fail
```

---

### Phase 3 — Automated tests

| # | Test | How | Expected |
|---|------|-----|----------|
| 3.1 | Full suite | `cd backend && mvn test` | 35 tests pass |
| 3.2 | Explain unit vs integration | Open `AuthServiceTest` vs `AuthIntegrationTest` | Unit = mocked repos; integration = H2 in-memory DB |
| 3.3 | Security tests | `mvn -Dtest=AuthSecurityTest test` | SQL/XSS payloads don't cause 500 errors |

---

### Phase 4 — Catalog backend

| # | Test | How | Expected |
|---|------|-----|----------|
| 4.1 | List products | `curl http://localhost:8080/api/products` | 8 products, facets object |
| 4.2 | Search | `?search=strip` | Smart Light Strip |
| 4.3 | Category filter | `?category=smart-bulbs` | 2 products |
| 4.4 | Brand filter | `?brand=LuminaTech` | 2 products |
| 4.5 | Price filter | `?minPrice=30&maxPrice=50` | Products in range |
| 4.6 | Sort | `?sort=price_desc` | Highest price first |
| 4.7 | Product detail | `curl http://localhost:8080/api/products/1` | Dimensions, category, images |
| 4.8 | Categories | `curl http://localhost:8080/api/categories` | 7 categories |
| 4.9 | Migrations | `docker compose exec postgres psql -U postgres -d lampify_db -c "\dt"` | `categories`, `products`, `product_images` exist |

---

### Phase 5 — Catalog frontend

| # | Test | How | Expected |
|---|------|-----|----------|
| 5.1 | Products load from API | Open products page (not mock data) | 8 products from database |
| 5.2 | Search bar | Type `desk`, wait 300ms | Desk Lamp(s) shown |
| 5.3 | Category sidebar | Click "Floor Lamps" | Only floor lamps |
| 5.4 | Brand checkbox | Check "GlowHaus" | GlowHaus products only |
| 5.5 | Price sliders | Narrow range to €30–€50 | Filtered list + count updates |
| 5.6 | Sort dropdown | "Price: Low to High" | Cheapest first |
| 5.7 | Clear filters | Click "Clear filters" | All products, defaults restored |
| 5.8 | Add to cart | Click Add to Cart | Toast + cart count increases |
| 5.9 | Error state | Stop backend, reload page | Error message shown (not crash) |
| 5.10 | Image fallback | Products without uploaded files | Placeholder image shown |

---

### Phase 6 — Docker & documentation

| # | Test | How | Expected |
|---|------|-----|----------|
| 6.1 | Full stack | `docker compose up --build` | postgres, backend, frontend all healthy |
| 6.2 | Storefront | http://localhost:3000 | Login + products work |
| 6.3 | API proxy | http://localhost:3000/api/categories | JSON via nginx proxy |
| 6.4 | README | Open `README.md` | Docker setup, ERD, architecture sections present |
| 6.5 | Reset stack | `docker compose down -v && docker compose up --build` | Fresh DB with seed data |

---

### Phase 7 — Viva talking points

| Topic | Where to look | One-sentence answer |
|-------|---------------|---------------------|
| JWT | `JwtUtil.java`, jwt.io demo | Three-part signed token; short-lived access + long-lived refresh |
| Refresh rotation | `AuthService.refreshToken()`, DB `refresh_tokens` | Old token marked used+revoked; new cookie issued |
| Access revocation | `docs/ACCESS_TOKEN_REVOCATION.md`, `revoked_access_tokens` | JTI blacklist on logout; 15-min TTL limits exposure |
| ACID | README architecture section | `@Transactional` on auth; future checkout uses `FOR UPDATE` on stock |
| Scalability | README, indexes in V6 migration | B-tree + GIN indexes, HikariCP pool, read replicas at scale |
| Search | `ProductRepositoryImpl.java`, V6 trigger | ILIKE search now; TSVECTOR index ready for full-text |
| Testing | `TESTING_STRATEGY.md`, `mvn test` | 34 automated tests + manual checklist above |
| What's not done | README status table | Checkout, orders, payments, server-side cart |

---

## Part 3 — Quick curl reference

```bash
API=http://localhost:8080/api

# Register
curl -X POST $API/auth/register -H "Content-Type: application/json" \
  -d '{"email":"viva@example.com","password":"StrongP@ss1","confirmPassword":"StrongP@ss1"}'

# Login (saves cookies)
curl -c cookies.txt -X POST $API/auth/login -H "Content-Type: application/json" \
  -d '{"email":"viva@example.com","password":"StrongP@ss1"}'

# Refresh
curl -b cookies.txt -c cookies.txt -X POST $API/auth/refresh

# Logout
curl -b cookies.txt -X POST $API/auth/logout -H "Authorization: Bearer <access_token>"

# Products
curl "$API/products?search=bulb&sort=price_asc"
```

---

## Part 4 — Common viva questions & honest answers

**"Where is the access token stored?"**
In memory only (`AuthContext.tsx` module variable) — not localStorage, to reduce XSS theft risk.

**"Can you revoke an access token immediately?"**
Yes, on logout we add the token's `jti` to `revoked_access_tokens`. The filter rejects it on the next request.

**"What if OAuth/CAPTCHA/email isn't configured?"**
Those features are implemented but optional. Docker disables CAPTCHA by default. SMTP and OAuth need env credentials.

**"What's missing?"**
Server-side cart persistence, checkout, orders, and payments. The catalog and auth layers are complete.

---

## Related files

| File | Purpose |
|------|---------|
| [TESTING_STRATEGY.md](../TESTING_STRATEGY.md) | Automated test approach |
| [TESTING_CHECKLIST.md](../TESTING_CHECKLIST.md) | Condensed pre-viva checklist |
| [README.md](../README.md) | Architecture, ERD, Docker setup |
| [ACCESS_TOKEN_REVOCATION.md](ACCESS_TOKEN_REVOCATION.md) | JTI blacklist design |
