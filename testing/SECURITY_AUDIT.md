# Part 3 — Security Audit & CIA Reference

Encryption at rest checklist, admin 2FA, rate limiting, and student oral prep for **Confidentiality, Integrity, Availability**.

---

## Encryption at rest — implementation matrix

| Data category | Field / artifact | Current (Parts 1–2) | Part 3 target | Mechanism |
|---------------|------------------|---------------------|---------------|-----------|
| **User credentials** | `users.password` | bcrypt hash | No change | One-way hash — not reversible encryption |
| **User credentials** | `users.two_factor_secret` | Plaintext in DB | Encrypt at rest | `@Convert(EncryptedStringConverter)` |
| **PII** | `users.email` | Plaintext | Optional encrypt | Rubric says PII — apply converter if evaluator requires |
| **Shipping addresses** | `orders.*` address fields | AES-GCM encrypted | Done | `EncryptedStringConverter` + V13 column widths |
| **Order details** | `orders.full_name`, `phone`, `email` | Encrypted | Done | Same converter |
| **Order details** | `order_items.product_name` | Plaintext snapshot | Encrypt | Add converter in 3D if "order details" required |
| **Payment** | `payment_transactions.failure_message` | Encrypted | Done | Converter on entity |
| **Session tokens** | `refresh_tokens.token` | Hashed | Verify hash only | Never store raw refresh token |
| **Session tokens** | `revoked_access_tokens.jti` | Plaintext identifier | OK | JTI is opaque ID, not secret |
| **Session tokens** | JWT access token | Not stored | N/A | Memory-only on client; short TTL |
| **Reviews** | `reviews.body` | N/A | Sanitize + store | HTML escape on output; optional encrypt for extra PII |

**Key management:** `APP_ENCRYPTION_SECRET` in environment (Docker `.env`). Production: use secrets manager, rotate with re-encryption migration.

**Verification test:** `EncryptionAtRestTest` — insert order via service, query JDBC `SELECT address_line1 FROM orders`, assert value ≠ plaintext input and matches Base64 pattern.

---

## In transit

| Layer | Part 3 target |
|-------|---------------|
| Browser ↔ frontend | HTTPS via self-signed cert (nginx) |
| Frontend ↔ backend | Proxy `/api` over HTTPS or internal Docker network |
| Cookies | `Secure`, `HttpOnly`, `SameSite` when TLS enabled |

Script: `testing/scripts/generate-tls.sh` (to be added in 3D).

---

## Admin 2FA enforcement

**Requirement:** All admin accounts must use 2FA.

| Check | Implementation |
|-------|----------------|
| Admin login without 2FA | Return `requiresTwoFactorSetup: true`; block `/api/admin/**` |
| Admin enables 2FA | TOTP via existing `TotpService` + backup codes |
| Existing optional 2FA for customers | Unchanged — optional for CUSTOMER role |
| Admin API middleware | `@PreAuthorize` + custom `AdminTwoFactorAspect` or filter |

**Tests:** `AdminTwoFactorEnforcementTest`, manual: admin cannot open dashboard until 2FA QR scanned.

---

## Rate limiting

**Current:** Fixed window counter on `/auth/*` (excluding refresh) — `RateLimitingFilter`.

**Part 3 target:** Token bucket algorithm.

| Endpoint group | Bucket size | Refill rate |
|----------------|-------------|-------------|
| `/auth/login`, `/auth/register` | 10 tokens | 1 / 6 sec |
| `/contact` | 5 tokens | 1 / 60 sec |
| General API (optional) | 100 tokens | 10 / sec |

**Client identity:** IP address (behind proxy: `X-Forwarded-For` first hop).

**Response:** `429 Too Many Requests`, header `Retry-After`.

**Test:** `TokenBucketRateLimitTest` — burst allowed, sustained block, refill after wait.

---

## CIA principles — student explanation guide

### Confidentiality

*Only authorized parties can access sensitive data.*

- HTTPS prevents interception on the wire.
- Encrypted order fields protect data if database is leaked.
- Passwords stored as bcrypt hashes — cannot be reversed to plaintext.
- JWT access tokens expire quickly; refresh tokens in httpOnly cookies.
- Role-based access: customers cannot call admin APIs.

### Integrity

*Data is accurate and cannot be tampered with undetected.*

- Server validates all checkout and review inputs.
- Stock reserved with `SELECT FOR UPDATE` — prevents overselling.
- Order status history append-only audit trail.
- Payment state machine: PENDING → PAID/FAILED with stock adjustments.
- Flyway migrations version schema changes.

### Availability

*System remains usable under load and failure.*

- Docker healthchecks restart unhealthy containers.
- Rate limiting prevents auth brute-force overload.
- RabbitMQ decouples payment failure emails from request thread.
- Load test identifies capacity; indexes and pool tuning improve headroom.
- Graceful error pages (404) and API error JSON — no stack traces to clients.

**Oral tip:** Give one concrete example per letter from this project (e.g. encryption → Confidentiality, stock lock → Integrity, rate limit → Availability).

---

## Additional security controls (existing)

| Control | Location |
|---------|----------|
| JWT + refresh rotation | `AuthService`, `AuthIntegrationTest` |
| Access token revocation | `TokenRevocationService`, `REVOKED_ACCESS_TOKENS` |
| reCAPTCHA (optional) | `CaptchaValidator` |
| CORS allowlist | `SecurityConfig` |
| SQL injection tests | `AuthSecurityTest`, `ProductCatalogSecurityTest` |
| Account lockout | `User.failedLoginAttempts`, `accountLockedUntil` |

---

## Pre-demo security checklist

- [ ] Default admin password changed in non-dev environments
- [ ] `APP_ENCRYPTION_SECRET` not committed to git
- [ ] TLS enabled for demo if rubric requires self-signed cert
- [ ] Admin routes return 403 without role + 2FA
- [ ] Contact form rate limited
- [ ] EncryptionAtRestTest passes
