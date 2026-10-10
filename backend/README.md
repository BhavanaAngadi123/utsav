# Utsav Backend — Full-stack Java

Production-grade backend for the Utsav event-vendor marketplace.

## Stack

| Component | Version |
|-----------|---------|
| Java | 25 LTS (Temurin 25.0.4.1) |
| Spring Boot | 4.1.1 |
| Vaadin | 25.3.1 |
| Spring AI | 2.0.1 |
| PostgreSQL | 18 |
| Flyway | 13.10.0 |
| jjwt | 0.13.0 |
| stripe-java | 34.0.0 |
| springdoc-openapi | 3.1.1 |
| Maven | 3.9.16 |

## Quick start

```bash
# Required secrets (app fails fast at startup if missing)
export UTSAV_JWT_SECRET="..."        # 256-bit secret for JWT signing
export STRIPE_SECRET_KEY="sk_test_..." 
export STRIPE_WEBHOOK_SECRET="whsec_..."

# Database (PostgreSQL)
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/utsav"
export SPRING_DATASOURCE_USERNAME="utsav"
export SPRING_DATASOURCE_PASSWORD="..."

# AI concierge (Ollama must be running)
ollama pull qwen3:8b

# Build + test
mvn -Pdev verify

# Run
mvn -Pdev spring-boot:run
```

## Architecture

```
com.utsav/
├── auth/          # JWT, login, refresh, lockout
├── vendor/        # Vendor profiles, search, nearby
├── verification/  # 13-country ID verification (status-only)
├── booking/       # Slot bookings
├── budget/        # Budget Freeze
├── concierge/     # AI concierge (Spring AI + tools)
├── team/          # Event team builder
├── payment/       # Stripe integration
├── admin/         # Admin endpoints, audit log
├── catalog/       # Categories
├── config/        # Security, CORS, rate limiting
└── ui/            # Vaadin views
```

## AI Concierge

The concierge uses **Spring AI** with **Ollama** running `qwen3:8b` locally (100% free, no API keys).

**Agentic pattern**: The LLM handles natural language understanding and conversation. It calls Java `@Tool` functions:
- `searchVendors(category, city, maxPrice)` — find vendors
- `nearbyVendors(lat, lon, category)` — "near me" search  
- `vendorDetails(vendorId)` — full vendor info
- `recommendVendors(eventType, guests, maxBudget, city)` — scored 0-100 with reasons

**Fallback**: If Ollama is down, the pure-Java scoring engine (`recommendVendors`) works standalone — no LLM required.

**Swap to cloud LLM**: Replace the Ollama ChatModel bean with any Spring AI provider (OpenAI, Anthropic, etc.) — the `@Tool` functions work unchanged via the portable `ChatClient` abstraction.

## Payments

**Stripe TEST mode only** — no real charges.

- `POST /api/payments/intent` — create PaymentIntent for a booking
- `POST /api/payments/webhook` — Stripe webhook (signature verified)
- `POST /api/payments/{id}/refund` — refund (vendor/admin)

**Provider-agnostic**: `PaymentProvider` interface. To add Razorpay, implement the interface — no changes to controllers or services.

**Go live**: Replace `sk_test_` with `sk_live_` in `STRIPE_SECRET_KEY`. Never commit keys.

## Security

- **RBAC**: CUSTOMER, VENDOR, ADMIN with `@PreAuthorize` on every endpoint
- **JWT**: Short-lived access (15min) + refresh (7d) with rotation
- **Passwords**: BCrypt (12 rounds), strength validation (12+ chars, mixed)
- **Lockout**: 5 failed attempts → 15min lockout
- **OAuth2**: Google login (optional, needs client ID/secret)
- **Rate limiting**: 5/min on auth, 10/min on payments
- **Audit**: All sensitive actions logged to `audit_logs` table
- **Headers**: CSP, HSTS, X-Frame-Options, etc.
- **CORS**: Strict, configurable via `UTSAV_CORS_ALLOWED_ORIGINS`

## Database

Flyway migrations in `src/main/resources/db/migration/`:
- V1: users, refresh tokens, OAuth accounts
- V2: vendors, ID verifications
- V3: slots, bookings, budget freezes, teams, reviews
- V4: payments, audit logs, concierge sessions
- V5: category seed data

All timestamps use `TIMESTAMP WITH TIME ZONE`.

## Testing

```bash
mvn -Pdev verify   # 31 tests, H2 in-memory
```

Tests cover: auth (JWT, lockout, refresh), bookings (overlap, authz), budget (freeze, filtering), payments (Stripe mock), concierge (scoring), reviews/teams, HTTP security (RBAC, CORS).
