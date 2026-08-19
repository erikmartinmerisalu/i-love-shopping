# Part 3 — Test Strategy

How automated and manual testing integrate throughout Part 3 development.

---

## Principles

1. **Test with the feature** — Each Part 3 sub-part ships with at least one new automated test before merge.
2. **Pyramid** — Many unit tests, fewer integration tests, selective E2E for critical journeys.
3. **Security is continuous** — Admin and review endpoints get authorization tests on day one.
4. **Manual fills gaps** — Responsive layout, 200% zoom, and UX polish use a written manual log.

---

## Test layers

### Unit tests (backend — JUnit 5 + Mockito)

Fast, isolated. Mock repositories and external services.

| Area | Existing | Part 3 additions |
|------|----------|------------------|
| Cart / orders | `CartServiceTest`, `OrderServiceTest` | — |
| Auth / 2FA | `AuthServiceTest` | `AdminTwoFactorEnforcementTest` (service layer) |
| Catalog | `ProductServiceTest` | `ReviewServiceTest`, rating average calculation |
| Security | `RateLimitingFilterTest` | `TokenBucketRateLimitTest` |
| Admin | — | `BulkUploadServiceTest`, validation of CSV rows |

**Run:** `cd backend && mvn test`

### API integration tests (backend — `@SpringBootTest` + MockMvc)

Full Spring context, real Postgres (Testcontainers or H2 if configured). Use `TestDatabaseCleaner` between tests.

| Area | Existing | Part 3 additions |
|------|----------|------------------|
| Auth flow | `AuthIntegrationTest` | Admin login + 2FA gate |
| Checkout | `CheckoutFlowIntegrationTest` | — |
| Catalog | `ProductCatalogIntegrationTest` | Home featured, search suggest, pagination |
| Security | `AuthSecurityTest`, `ProductCatalogSecurityTest`, `OAuthEndpointSecurityTest` | Admin RBAC 403, review injection |
| New | — | `ContactControllerIntegrationTest`, `ReviewIntegrationTest`, `BulkUploadIntegrationTest`, `EncryptionAtRestTest` |

**Pattern:**

```java
@Test
void adminWithoutTwoFactorCannotCreateProduct() throws Exception {
    // register admin, login without enabling 2FA
    mockMvc.perform(post("/api/admin/products").header("Authorization", token)...)
           .andExpect(status().isForbidden());
}
```

### User flow tests

**Backend integration (existing):** `CheckoutFlowIntegrationTest` covers register → cart → checkout → pay → retry → cancel.

**Part 3 additions:**

- `ReviewPurchaseFlowIntegrationTest`: place order → pay → submit review → admin approve → visible on product.
- `AdminOrderFlowIntegrationTest`: admin updates order status → customer sees update.

**Browser E2E (recommended — Playwright):**

Location: `testing/e2e/`

| Spec | Flow |
|------|------|
| `guest-checkout.spec.ts` | Home → product → cart dropdown → checkout (sandbox card) → confirmation |
| `admin-product.spec.ts` | Admin login + 2FA → create product → visible on storefront |
| `review-flow.spec.ts` | Purchase → submit review → appears after moderation |

**Run (when added):** `cd frontend && npx playwright test`

### Security tests

| Test | Validates |
|------|-----------|
| `AuthSecurityTest` | SQL injection, weak passwords on register |
| `ProductCatalogSecurityTest` | Search injection |
| `OAuthEndpointSecurityTest` | OAuth token validation |
| `AdminSecurityTest` *(new)* | Non-admin 403 on `/api/admin/**` |
| `ReviewSecurityTest` *(new)* | Cannot review without purchase; XSS escaped in body |
| `EncryptionAtRestTest` *(new)* | Order PII not plaintext in DB |

---

## Manual testing

Use `MANUAL_TEST_LOG.md` for each release candidate.

### Viewport matrix

| Page | 320px | 768px | 1024px | 1440px |
|------|-------|-------|--------|--------|
| Home | | | | |
| Product listing (grid/list) | | | | |
| Product detail | | | | |
| Cart / quick cart | | | | |
| Checkout | | | | |
| Admin dashboard | | | | |
| Contact / About | | | | |
| 404 | | | | |

### Accessibility spot checks

- Tab through header, search, cart, forms — visible focus ring.
- Screen reader: one H1 per page, landmarks (`header`, `main`, `footer`).
- Browser zoom 200% — no horizontal scroll on main content.
- All product images have non-empty `alt`.

### Exploratory scenarios

- Out-of-stock add to cart → error message.
- Guest cart merge on login.
- Admin bulk upload with invalid CSV row → partial success report.
- Rate limit: 61 auth attempts in 60s → 429.

---

## CI integration (recommended)

```yaml
# .github/workflows/test.yml (future)
- run: cd backend && mvn test
- run: cd frontend && npm test -- --run
# - run: cd frontend && npx playwright test  # when E2E added
```

Docker build should run `mvn test` in backend Dockerfile multi-stage or separate CI job.

---

## Definition of done (testing)

For each Part 3 feature:

- [ ] Unit and/or integration test covers happy path.
- [ ] At least one negative/security case where applicable.
- [ ] Manual log entry for UI-facing work.
- [ ] `DEMO_SCRIPT.md` section updated if evaluator-visible.

---

## Existing test inventory (Parts 1–2)

| Class | Type |
|-------|------|
| `CartServiceTest` | Unit |
| `OrderServiceTest` | Unit |
| `AuthServiceTest` | Unit |
| `ProductServiceTest` | Unit |
| `RateLimitingFilterTest` | Unit |
| `CheckoutFlowIntegrationTest` | Integration / user flow |
| `AuthIntegrationTest` | Integration |
| `ProductCatalogIntegrationTest` | Integration |
| `AuthControllerTest` | API |
| `AuthSecurityTest` | Security |
| `ProductCatalogSecurityTest` | Security |
| `OAuthEndpointSecurityTest` | Security |

Part 3 target: **+10–12 test classes**, **1–3 E2E specs**.
