# ESTValgus

B2C lighting shop built for a coursework/viva project. Customers browse a product catalog, filter and sort results, and use a **server-backed cart**. Accounts support email/password login, Google OAuth, optional 2FA, and password reset. Checkout creates orders; payments use Stripe sandbox when keys are set, otherwise CARD/PAYPAL local sandbox scenarios.

**Stack:** React (Vite) + Spring Boot + PostgreSQL + RabbitMQ, packaged with Docker Compose.

---

## What is implemented

| Area | Status |
|------|--------|
| B2C storefront (browse, search, cart) | Done |
| Email/password auth | Done |
| Google OAuth | Done (needs Google Client ID in `.env`) |
| reCAPTCHA v3 on registration | Done (skipped if keys are empty) |
| JWT access token in memory + httpOnly refresh cookie | Done |
| Refresh token rotation (single-use) | Done |
| Access + refresh revocation on logout | Done |
| Password reset flow | Done (real email only if SMTP is configured) |
| TOTP 2FA (QR, backup codes) | Done |
| Product API: search, facets, sort, images | Done |
| Cart (guest cookie + logged-in carts, merge on login) | Done |
| Checkout / orders / payments | Done |
| Stripe sandbox + CARD/PAYPAL sandbox scenarios | Done |
| RabbitMQ payment status to notification emails | Done (when `APP_MESSAGING_ENABLED`) |
| AES-GCM field encryption at rest | Done (`APP_ENCRYPTION_SECRET`) |
| Automated backend tests | 52 (`mvn test`) |

---

## Run with Docker

You only need Docker installed on the host.

```bash
git clone <repo>
cd projects   # repository root
cp example.env .env          # optional: Google OAuth, reCAPTCHA, SMTP, Stripe, RabbitMQ
docker compose up --build
```

| Service | URL |
|---------|-----|
| Shop | http://localhost:3000 |
| API | http://localhost:8080/api |
| Database | `localhost:5432`, db `lampify_db`, user/pass `postgres` |
| RabbitMQ UI | http://localhost:15672 (default `guest` / `guest`) |

Stop: `docker compose down`  
Reset DB + uploads: `docker compose down -v`

Rebuild after changing `VITE_*` build args: `docker compose up --build`

### Local dev (without full Docker stack)

```bash
cd backend && docker compose up -d postgres
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev   # http://localhost:5173
```

The backend loads `GOOGLE_CLIENT_ID` from the root `.env` when you run Maven locally. For payment emails and MQ locally, also run RabbitMQ (or use the compose `rabbitmq` service) and set `APP_MESSAGING_ENABLED` / mail vars as in `example.env`.

---

## Usage

### Register and log in

1. Open the shop (port 3000 or 5173 in dev).
2. **Register** with a strong password (8+ chars, upper, lower, digit, special). reCAPTCHA runs when keys are set.
3. **Log in** - access token stays in a JavaScript variable; refresh token is an httpOnly cookie.
4. **Google login** - configure `VITE_GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_ID` (see `example.env`).
5. **Forgot password** - triggers reset token; without SMTP the link is printed in the **backend log** (look for `Mail not configured. Password reset link for ...`).
6. **2FA** - Profile then **Two-factor auth** then generate QR then verify with authenticator app. Google accounts skip the password step.

### Catalog

- Search bar (debounced) matches name, description, brand.
- Sidebar: category, brand, price range.
- Sort: relevance, price, rating, name.

### Cart, checkout, orders, payments

- **Cart** persists via the API (not browser-only). Guests get an httpOnly `guestCartToken` cookie; logged-in users have a user-owned cart. Guest and user carts **merge on login**.
- **Checkout** at `/checkout`: contact, shipping address, payment method, and order summary.
- **Payments:** Stripe Elements when Stripe keys are set; otherwise CARD/PAYPAL **sandbox** scenarios with a test-card info box on the payment step.
- **Orders** page: list with filter/sort, order detail (including status history), cancel when status is `PENDING_PAYMENT` (restocks inventory).
- **RabbitMQ:** payment success/fail publishes to the queue; a consumer sends notification emails when `APP_MESSAGING_ENABLED` is true. Management UI: http://localhost:15672.
- **Emails** (order confirmation, payment success/fail, password reset) share the same `MAIL_*` / `EmailService` path.

### API samples

```bash
curl "http://localhost:8080/api/products?search=bulb&sort=price_asc"
curl http://localhost:8080/api/categories
```

### Tests

```bash
cd backend && mvn test
```

| Test class | Type | What it checks |
|------------|------|----------------|
| `AuthServiceTest` | Unit | Register, login, 2FA, refresh, reset, logout |
| `ProductServiceTest` | Unit | Catalog mapping and search response |
| `CartServiceTest` | Unit | Cart totals and stock checks |
| `OrderServiceTest` | Unit | Checkout validation, order totals, cancel restock |
| `RateLimitingFilterTest` | Unit | Auth rate limits |
| `AuthIntegrationTest` | Integration | Full auth flow; **refresh token reuse rejected** |
| `ProductCatalogIntegrationTest` | Integration | Search, filters, pagination, categories |
| `CheckoutFlowIntegrationTest` | Integration | Register then cart then checkout then sandbox pay |
| `AuthSecurityTest` | Security | XSS/SQL payloads on register, weak password |
| `ProductCatalogSecurityTest` | Security | Injection in search, public catalog access |
| `OAuthEndpointSecurityTest` | Security | OAuth endpoint reachable without prior auth |
| `AuthControllerTest` | API | Controller validation |

Viva / testing checklists and notes live under [testing/](testing/) (safe to delete before submission).

---

## Architecture

**Why this shape**

- **Spring Boot** - REST API, security, JPA, Flyway migrations in one place.
- **React + Vite** - product grid, auth forms, cart, checkout, and orders without full page reloads.
- **PostgreSQL** - relational catalog, auth, cart, and order tables; ACID transactions for token rotation and checkout.
- **RabbitMQ** - payment status events drive async notification emails (optional via `APP_MESSAGING_ENABLED`).
- **Docker Compose** - postgres, RabbitMQ, API, and nginx frontend start together for demos.

**JWT**

| Token | Lifetime | Where it lives |
|-------|----------|----------------|
| Access | 15 min | In-memory in `AuthContext.tsx` |
| Refresh | 7 days | httpOnly cookie |

A JWT has three parts:

1. **Header** - algorithm (`HS256`) and type (`JWT`)
2. **Payload** - `sub` (email), `jti` (ID for revocation), `type`, `exp`, `iat`
3. **Signature** - HMAC over `header.payload` with `app.jwt.secret`

Code: `backend/src/main/java/com/lampify/security/JwtUtil.java`

On refresh, the old refresh token is marked `used` and `revoked`; a new one is issued. Reusing the old token fails (`AuthIntegrationTest.refreshTokenRotationRejectsOldToken`). On logout, the access token `jti` goes into `revoked_access_tokens`.

**ACID (why it matters here)**

- **Atomicity** - refresh rotation updates the old row and inserts a new one inside one `@Transactional` method; both commit or neither does. Checkout place-order and cancel/restock are likewise transactional.
- **Consistency** - FK constraints (e.g. `products.category_id`, cart/order item FKs) stop orphan rows.
- **Isolation** - concurrent logins get separate refresh rows; checkout uses `SELECT ... FOR UPDATE` on product stock to prevent overselling.
- **Durability** - committed tokens, cart, and order data survive a Postgres restart.

**PostgreSQL and growth**

- B-tree indexes on `brand`, `price`, `rating`, `category_id`
- GIN index on `products.search_vector` (maintained by trigger)
- HikariCP pool (10 connections) in `application.properties`
- Read replicas / Redis caching are not set up - reasonable next steps if traffic grows

**Search**

- **Schema:** trigger fills `search_vector` (tsvector) from name (weight A), brand (B), description (C).
- **Runtime:** `ProductRepositoryImpl` uses `ILIKE` on name, description, brand for the `search` query param.
- **Facets:** response includes categories, brands, min/max price for the current filter set.
- **Sort:** `relevance`, `price_asc`, `price_desc`, `rating`, `name`.

Example:  
`GET /api/products?search=bulb&category=smart-bulbs&brand=LuminaTech&minPrice=20&maxPrice=50&sort=price_asc`

---

## Entity-relationship diagram

Below matches **Flyway migrations V1-V13**. Mermaid preview shrinks huge diagrams, so relationships are split into readable charts; column details are in the tables underneath.

**Cardinality / modality**

| Notation | Meaning |
|----------|-----------|
| `1:N` | One parent, many children |
| `(1,1)` | Exactly one - required FK |
| `(0,N)` | Zero or many - optional side |
| UK | Unique key |

### Auth & security

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : has
    USERS ||--o{ PASSWORD_RESET_TOKENS : has
    USERS ||--o{ TWO_FACTOR_BACKUP_CODES : has
    USERS {
        bigint id PK
        varchar email UK
    }
    REFRESH_TOKENS {
        bigint id PK
        bigint user_id FK
    }
    PASSWORD_RESET_TOKENS {
        bigint id PK
        bigint user_id FK
    }
    TWO_FACTOR_BACKUP_CODES {
        bigint id PK
        bigint user_id FK
    }
    REVOKED_ACCESS_TOKENS {
        bigint id PK
        varchar jti UK
    }
```

`REVOKED_ACCESS_TOKENS` is standalone (no FK to `USERS`); rows are keyed by access-token `jti`.

### Catalog

```mermaid
erDiagram
    CATEGORIES ||--|{ PRODUCTS : contains
    PRODUCTS ||--|{ PRODUCT_IMAGES : has
    CATEGORIES {
        bigint id PK
        varchar slug UK
    }
    PRODUCTS {
        bigint id PK
        bigint category_id FK
    }
    PRODUCT_IMAGES {
        bigint id PK
        bigint product_id FK
    }
```

### Commerce (cart, orders, payments)

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
    USERS {
        bigint id PK
    }
    CARTS {
        bigint id PK
        bigint user_id FK
        varchar guest_token UK
    }
    CART_ITEMS {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
    }
    ORDERS {
        bigint id PK
        bigint user_id FK
        varchar order_number UK
    }
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
    }
    ORDER_STATUS_HISTORY {
        bigint id PK
        bigint order_id FK
    }
    PAYMENT_TRANSACTIONS {
        bigint id PK
        bigint order_id FK
    }
    PRODUCTS {
        bigint id PK
    }
```

- Users **0..1** cart (`carts.user_id` unique); guests use `guest_token` instead (XOR check).
- Carts **1:N** cart_items; products **1:N** cart_items.
- Users **1:N** orders; orders **1:N** order_items / order_status_history / payment_transactions.
- Products **1:N** order_items (product may be null after delete; snapshot name/price kept on the line).

### Entity columns

**USERS**

| Column | Notes |
|--------|--------|
| `id` | PK |
| `email` | UK, NOT NULL |
| `password` | BCrypt, NOT NULL |
| `username` | NOT NULL |
| `provider` | nullable - OAuth provider |
| `enabled` | default true |
| `two_factor_enabled` | default false |
| `two_factor_secret` | nullable |
| `last_login_at` | nullable |
| `failed_login_attempts` | |
| `account_locked_until` | nullable |
| `created_at`, `updated_at` | |

**REFRESH_TOKENS** - `user_id` FK, `token` UK, `expires_at`, `revoked`, `used`, `created_at`

**PASSWORD_RESET_TOKENS** - `user_id` FK, `token` UK, `expires_at`, `used`, `created_at`

**REVOKED_ACCESS_TOKENS** - `jti` UK, `expires_at`, `revoked_at`

**TWO_FACTOR_BACKUP_CODES** - `user_id` FK, `code_hash`, `used`, `created_at`

**CATEGORIES** - `name`, `slug` UK, `description`, `created_at`

**PRODUCTS** - `category_id` FK, `name`, `description`, `price`, `stock_quantity`, `brand`, `rating`, dimensions/weight fields, `search_vector`, timestamps

**PRODUCT_IMAGES** - `product_id` FK, `file_name`, `url_path`, `is_primary`, `sort_order`, `created_at`

**CARTS** (V10) - `user_id` FK UK (nullable), `guest_token` UK (nullable), timestamps; exactly one of user/guest

**CART_ITEMS** (V10) - `cart_id` FK, `product_id` FK, `quantity` (>0), unique `(cart_id, product_id)`, timestamps

**ORDERS** (V11, widened V13 for AES-GCM) - `order_number` UK, `user_id` FK, `status`, `payment_method`, contact/address fields (encrypted at rest), `total_amount`, timestamps

**ORDER_ITEMS** (V11) - `order_id` FK, `product_id` FK (nullable), `product_name`, `unit_price`, `quantity`, `line_total`

**ORDER_STATUS_HISTORY** (V11) - `order_id` FK, `status`, `note`, `created_at`

**PAYMENT_TRANSACTIONS** (V12, widened V13) - `order_id` FK, `provider`, `provider_payment_id`, `status`, failure fields, `amount`, `currency`, timestamps

**Relationship summary**

| From | To | Cardinality | Modality |
|------|-----|-------------|----------|
| User | Refresh tokens | 1:N | User must exist; many tokens over time (rotation) |
| User | Password reset tokens | 1:N | Optional until reset requested |
| User | 2FA backup codes | 1:N | Only when 2FA setup runs |
| Category | Products | 1:N | Each product belongs to exactly one category |
| Product | Product images | 1:N | At least zero images; UI uses placeholder if none |
| User | Cart | 0..1 | Optional; guest carts use `guest_token` instead |
| Cart | Cart items | 1:N | Items cleared after successful checkout |
| User | Orders | 1:N | Guest checkout may leave `user_id` null |
| Order | Order items / status history / payments | 1:N | Created at place-order / payment |

Migrations: **V1-V13** (commerce: V10 carts, V11 orders, V12 payment_transactions, V13 widen encrypted columns).

---

## Other docs

| File | Purpose |
|------|---------|
| [example.env](example.env) | OAuth, reCAPTCHA, SMTP, Stripe, RabbitMQ, encryption template |
| [docs/RECAPTCHA_SETUP.md](docs/RECAPTCHA_SETUP.md) | reCAPTCHA keys, localhost, and Render |
| [testing/](testing/) | Viva / audit / Part 2 checklists - **not required to run the shop**; delete before submission if you want a lean hand-in |
