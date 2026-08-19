# Manual Test Log

Record manual QA during Part 3. Copy the template row for each session.

---

## Template

| Date | Tester | Scenario | Viewport | Browser | Result | Notes |
|------|--------|----------|----------|---------|--------|-------|
| YYYY-MM-DD | | | 320 / 768 / 1024 / 1440 | | PASS / FAIL | |

---

## Sessions

<!-- Add rows below as you test -->

| Date | Tester | Scenario | Viewport | Browser | Result | Notes |
|------|--------|----------|----------|---------|--------|-------|
| | | Baseline: docker compose up, mvn test | — | — | | Parts 1–2 smoke |

---

## Required scenarios (complete before Part 3 sign-off)

- [ ] Guest checkout end-to-end (sandbox card)
- [ ] Logged-in checkout with cart merge
- [ ] Quick cart dropdown from any page
- [ ] Search typeahead → full search results
- [ ] Contact form success and validation errors
- [ ] 404 page from unknown URL
- [ ] Admin login blocked without 2FA
- [ ] Admin product CRUD reflected on storefront
- [ ] Bulk CSV upload (valid + one invalid row)
- [ ] Review submit → admin approve → visible on product
- [ ] Order status update visible to customer
- [ ] Refund issues and order status updates
- [ ] 200% browser zoom on checkout and product detail
- [ ] Responsive pass at 320, 768, 1024, 1440
