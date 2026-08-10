# ESTValgus

B2C lighting shop with a server-backed shopping cart, checkout, sandbox payments (Stripe / CARD / PayPal simulation), and order management. Also includes account features (email login, Google OAuth, 2FA, password reset).

**Stack:** React (Vite) + Spring Boot + PostgreSQL + RabbitMQ, run with Docker Compose.

---

## Setup

**Prerequisites:** Docker (and Docker Compose). Optional: Stripe CLI for webhook testing.

```bash
git clone <repo>
cd projects
cp example.env .env    # optional: OAuth, SMTP, Stripe keys
docker compose up --build
```

| Service | URL |
|---------|-----|
| Shop | http://localhost:3000 |
| API | http://localhost:8080/api |
| RabbitMQ UI | http://localhost:15672 (`guest` / `guest`) |

Stop: `docker compose down` · Reset data: `docker compose down -v`

**Local dev:** run Postgres (or full compose stack), then `cd backend && mvn spring-boot:run` and `cd frontend && npm install && npm run dev` (http://localhost:5173). See [example.env](example.env) for variables.

---

## Usage

### Shopping cart

- Open the cart from the shop header. Each line shows **name, price, thumbnail**, quantity controls, and line subtotal.
- **Guests:** cart stored in PostgreSQL, keyed by httpOnly `guestCartToken` cookie.
- **Logged-in users:** persistent user cart; guest cart **merges on login**.
- **Recommendations:** “You may also like” suggestions based on cart categories.
- Out-of-stock adds/updates return clear API errors (no silent failures).

### Checkout

Single route `/checkout`:

1. Contact, shipping address, payment method (STRIPE / CARD / PAYPAL sandbox).
2. Place order → pay on the same page (no redirect on failure; errors shown inline).
3. Success → confirmation view; **confirmation email sent after payment succeeds**.

Logged-in users get **email and name prefilled**. Address and payment fields are validated on the client and server.

**Sandbox test cards** (CARD mode): `4242…` success, `4000…9995` insufficient funds, `4000…0002` invalid, `4000…0069` expired, `4000…0010` timeout. PayPal sandbox uses approve/decline buttons (no card form).

### Orders & payments

- **Orders** (`/orders`): filter by status, sort by date; detail page with status history.
- **Cancel** unprocessed orders (`PENDING_PAYMENT`) — stock is restored.
- **Payments:** Stripe Elements when keys are set; otherwise local sandbox. Card data never stored on the server.
- **RabbitMQ:** payment events published to a queue; consumer sends failure notification emails (`APP_MESSAGING_ENABLED`).
- **Inventory:** stock reserved at place-order (`SELECT … FOR UPDATE`); restored on payment failure or cancel; re-reserved on retry.

### Auth (summary)

Register / login, Google OAuth, forgot password, optional 2FA under Profile. JWT access token in memory; refresh token in httpOnly cookie.

---

## Tests

```bash
cd backend && mvn test
```

| Test class | Type | What it checks |
|------------|------|----------------|
| `CartServiceTest` | Unit | Cart totals, stock limits, remove item |
| `OrderServiceTest` | Unit | Checkout validation, order totals, cancel restock |
| `CheckoutFlowIntegrationTest` | Integration | Register, cart, checkout, pay, **payment fail → retry → success**, cancel |
| `AuthIntegrationTest` | Integration | Auth flow, refresh token rotation |
| `ProductCatalogIntegrationTest` | Integration | Search, filters, categories |
| `AuthServiceTest` | Unit | Register, login, 2FA, reset |
| `ProductServiceTest` | Unit | Catalog mapping |
| `AuthSecurityTest` | Security | Injection / weak password on register |
| `ProductCatalogSecurityTest` | Security | Search injection |
| `OAuthEndpointSecurityTest` | Security | OAuth endpoints |
| `RateLimitingFilterTest` | Unit | Auth rate limits |
| `AuthControllerTest` | API | Controller validation |

---

## Architecture (commerce flow)

```
Customer → Cart API (guest cookie / user cart)
         → POST /orders (PENDING_PAYMENT, stock reserved)
         → Payment API (Stripe Elements or sandbox)
         → Order PAID or FAILED (stock adjusted)
         → RabbitMQ event → failure email (async)
         → Confirmation email on success (sync)
```

Order PII and payment failure messages are **encrypted at rest** (AES-GCM, `APP_ENCRYPTION_SECRET`).

---

## Entity-relationship diagram

Flyway migrations **V1–V13**. Guest carts use `guest_token`; logged-in carts use `user_id` (mutually exclusive). Order contact fields and payment failure messages are encrypted at rest (V13).

### Commerce

```mermaid
erDiagram
    USERS ||--o| CARTS : owns
    CARTS ||--|{ CART_ITEMS : contains
    PRODUCTS ||--o{ CART_ITEMS : in
    USERS ||--o{ ORDERS : places
    ORDERS ||--|{ ORDER_ITEMS : contains
    PRODUCTS ||--o{ ORDER_ITEMS : in
    ORDERS ||--|{ ORDER_STATUS_HISTORY : tracks
    ORDERS ||--|{ PAYMENT_TRANSACTIONS : paid_by
    CATEGORIES ||--|{ PRODUCTS : contains
    PRODUCTS ||--|{ PRODUCT_IMAGES : has

    USERS {
        bigint id PK
        varchar email UK
    }
    CARTS {
        bigint id PK
        bigint user_id FK UK
        varchar guest_token UK
    }
    CART_ITEMS {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        int quantity
    }
    CATEGORIES {
        bigint id PK
        varchar slug UK
        varchar name
    }
    PRODUCTS {
        bigint id PK
        bigint category_id FK
        varchar name
        decimal price
        int stock_quantity
    }
    PRODUCT_IMAGES {
        bigint id PK
        bigint product_id FK
        varchar url_path
        boolean is_primary
    }
    ORDERS {
        bigint id PK
        varchar order_number UK
        bigint user_id FK
        varchar status
        varchar payment_method
        decimal total_amount
    }
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        varchar product_name
        int quantity
        decimal line_total
    }
    ORDER_STATUS_HISTORY {
        bigint id PK
        bigint order_id FK
        varchar status
        varchar note
    }
    PAYMENT_TRANSACTIONS {
        bigint id PK
        bigint order_id FK
        varchar provider
        varchar status
        decimal amount
    }
```

### Auth (supporting tables)

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : has
    USERS ||--o{ PASSWORD_RESET_TOKENS : has
    USERS ||--o{ TWO_FACTOR_BACKUP_CODES : has

    USERS {
        bigint id PK
        varchar email UK
        varchar password
        boolean two_factor_enabled
    }
    REFRESH_TOKENS {
        bigint id PK
        bigint user_id FK
        varchar token UK
        timestamp expires_at
    }
    PASSWORD_RESET_TOKENS {
        bigint id PK
        bigint user_id FK
        varchar token UK
        timestamp expires_at
    }
    TWO_FACTOR_BACKUP_CODES {
        bigint id PK
        bigint user_id FK
        varchar code_hash
        boolean used
    }
    REVOKED_ACCESS_TOKENS {
        bigint id PK
        varchar jti UK
        timestamp expires_at
    }
```

`REVOKED_ACCESS_TOKENS` has no FK to `USERS` (keyed by access-token `jti`).

| From | To | Notes |
|------|-----|--------|
| User | Cart | 0..1; guest carts via `guest_token` |
| Cart | Cart items | 1:N; cleared after checkout |
| User | Orders | 1:N; guest checkout may leave `user_id` null |
| Order | Items / history / payments | 1:N |
| Product | Cart items / order items | Stock locked at checkout |

---

## Additional features

- Product search, category/brand/price filters, sort
- Google OAuth, reCAPTCHA v3 (optional), TOTP 2FA
- JWT refresh rotation and access-token revocation
- Password reset email flow
- Dockerized full stack (frontend, backend, Postgres, RabbitMQ)

Configuration template: [example.env](example.env)
