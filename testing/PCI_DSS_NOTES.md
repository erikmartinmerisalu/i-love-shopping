# PCI DSS — viva talking points (ESTValgus)

PCI DSS (Payment Card Industry Data Security Standard) is a set of security
requirements for any organization that stores, processes, or transmits
cardholder data.

## Why we do not store sensitive payment data on application servers

1. **Scope** — Storing PAN (card number), CVV, or full track data brings the
   whole app and database into full PCI scope (audits, controls, liability).
2. **Breach risk** — A compromised application DB with card data is high impact.
   Tokenized provider IDs (`pi_…`, sandbox session ids) are low value if leaked.
3. **Delegation** — Stripe Payment Element / hosted fields keep card data in the
   provider’s PCI-compliant environment. We only receive payment status and ids.
4. **Least data** — Our `payment_transactions` table stores provider, amount,
   status, and failure codes — never raw card numbers or CVV.
5. **Sandbox form** — Client-side Luhn/expiry/CVV checks run in the browser;
   only a scenario token is POSTed to `/api/payments/sandbox/confirm`.

## What ESTValgus stores

- Order + shipping details (encryption at rest = Phase D)
- Payment transaction metadata (provider payment id, status, failure code)
- Not stored: card number, expiry, CVV, magnetic stripe data

## Demo line for viva

“We never handle card PAN on our servers. Stripe Elements (or our sandbox form
that only sends scenario tokens) keeps us out of full PCI card-storage scope.”
