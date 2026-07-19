# Access Token Revocation Strategy

## Decision

ESTValgus uses a **JTI blacklist** stored in PostgreSQL (`revoked_access_tokens` table) combined with **short-lived access tokens** (15 minutes).

## Why this approach

| Approach | Pros | Cons |
|----------|------|------|
| Short TTL only | Stateless, fast | Compromised token valid until expiry |
| Full server-side session store | Immediate revocation | Stateful, harder to scale horizontally |
| **JTI blacklist (chosen)** | Revoke on logout; still mostly stateless JWT validation | Requires DB lookup per request; cleanup needed |

For a B2C e-commerce platform, logout must invalidate access immediately when possible, while keeping JWT benefits for normal requests.

## How it works

1. Each access token includes a unique `jti` (JWT ID) claim in `JwtUtil.generateAccessToken()`.
2. On logout, `AuthService.logout()` reads the `Authorization: Bearer` header, extracts `jti` and expiration, and inserts a row into `revoked_access_tokens`.
3. `JwtAuthenticationFilter` checks `TokenRevocationService.isRevoked(jti)` before accepting the token.
4. Expired blacklist rows can be deleted via `TokenRevocationService.cleanupExpiredTokens()`.

## Refresh tokens

Refresh tokens use a separate mechanism: `used` and `revoked` flags in `refresh_tokens` with single-use rotation.

## Trade-offs accepted

- Small DB read on every authenticated request (indexed by `jti`)
- Blacklist grows until expired entries are cleaned up
- If logout is called without the access token header, the token remains valid until its 15-minute TTL

## Scalability note

At higher scale, the JTI blacklist can move to Redis with TTL matching token expiration, preserving the same semantics with lower latency.
