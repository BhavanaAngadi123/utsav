# Utsav — Every celebration, one app

**Utsav** is a marketplace for booking event vendors for celebrations — Indian, American, and everything in between. Customers browse verified vendors, see portfolio photos tagged with what they *actually cost*, read real reviews, and book time slots directly. Vendors — especially newcomers and upcoming entrepreneurs — list their businesses, prove themselves with budget-tagged past work, and grow.

## Two parts

| | Static prototype | Full-stack backend |
|---|---|---|
| **What** | Interactive demo of the full product vision | Production-grade Java backend + Vaadin UI |
| **Where** | `index.html` + `images/` (repo root) | `backend/` |
| **Live** | https://bhavanaangadi123.github.io/utsav/ (GitHub Pages) | Run locally (see below) |

The prototype at the repo root is untouched and keeps working via GitHub Pages. The `backend/` directory is the real application: Spring Boot + Vaadin + PostgreSQL.

---

## The idea

Finding vendors for a sangeet, mehendi, birthday, wedding, or Halloween party today means scrolling Instagram or chasing word-of-mouth. Utsav puts it in one app, with a **budget-first** differentiator:

- **❄ Budget Freeze (two-sided)** — customers freeze a total budget once; every search, filter, and AI-built team stays strictly inside it. Vendors tag every portfolio photo with the budget it was executed at ("This mandap: $1,800 · 120 guests") — proof over promises.
- **AI event concierge** — a REAL AI chatbot (Spring AI + Ollama local LLM). Type a plain sentence ("small outdoor mehendi, 100 guests, budget around $5000, near Boston") and the AI understands it, calls Java tools to search/score real vendors, and curates recommendations with reasons ("highest rated near you", "fits your budget"). Especially guides first-time users.
- **Build your team** — bundle decorator + photographer + makeup artist, see one combined price and the dates when *everyone* is free.
- **Vendor compare** — side-by-side comparison of up to 3 vendors.
- **Slot-based booking** — pick a real date & time, confirm instantly. No quote-chasing.
- **Vendor-to-vendor collaboration graph** — vendors recommend each other; book the whole trusted crew.
- **Payments** — Stripe TEST mode: payment intents for deposits/full payments, webhooks, refunds. Provider-agnostic interface (Razorpay later).
- **Diaspora + India** — 8 cities (Boston, New York, Houston, Bay Area, Dallas, Hyderabad, Chennai, Bangalore) with $ / ₹ pricing.

## Prototype (static site)

- `index.html` — the entire demo app: 8 vendor categories, 14 occasions, 19 seed vendors, hash-routed views (Home, Browse, Vendor profile, Compare, Team builder, Onboarding, My bookings, Occasions), booking flow with confetti, AI concierge, Budget Freeze.
- `images/` — 16 photorealistic images (hero, 8 category shots, 6 portfolio shots), warm tones, no watermarks.

### How to run the prototype

No build step. Just open `index.html` in a browser — or serve it:

```bash
python3 -m http.server 8000
# open http://localhost:8000
```

Data (bookings, vendor signups, reviews, frozen budget, team) persists in `localStorage`. Demo data is illustrative — business names, reviews, and Instagram handles are fictional.

### Prototype tech

Tailwind CSS via CDN, vanilla JS, Google Fonts (Fraunces + Inter). Single self-contained file.

---

## Backend (full-stack Java)

Production-grade rebuild: **Java 25 LTS**, **Spring Boot 4.1.1**, **Vaadin 25.3.1** (pure-Java UI), **PostgreSQL 18** + **Flyway**, **Spring AI 2.0.1** + **Ollama**, **Stripe Java SDK 34.0.0**, JWT auth, OpenAPI docs.

### Features

- **Auth**: JWT access + refresh token rotation, BCrypt, account lockout, password strength, Google OAuth2 (optional), RBAC (CUSTOMER/VENDOR/ADMIN) with `@PreAuthorize`
- **Vendors**: profiles, search, nearby (Haversine), 13-country ID verification (status-only, never stores real IDs)
- **Budget Freeze**: lock guest count + max budget; discovery filters to vendors that fit
- **Bookings**: direct slot booking, availability checking
- **AI Concierge**: Spring AI ChatClient + Ollama (Qwen3) with agentic Java tools (search, nearby, recommend with 0-100 scoring); pure-Java fallback when Ollama is down
- **Team Builder**: bundle vendors, combined pricing, shared availability
- **Payments**: Stripe TEST mode — intents, webhooks, refunds, history
- **Admin**: verification queue, role management, audit log
- **Security**: CORS, rate limiting, Bean Validation, audit log, security headers, fail-fast on missing secrets, OWASP Top 10 self-review (see below)

### Prerequisites

- Java 25 LTS (Temurin)
- Maven 3.9+
- PostgreSQL 18 (or use H2 for tests)
- Ollama with `qwen3:8b` pulled: `ollama pull qwen3:8b`

### How to run locally

```bash
cd backend

# 1. Set required secrets (fail-fast at startup if missing)
export UTSAV_JWT_SECRET="your-256-bit-secret-here"
export STRIPE_SECRET_KEY="sk_test_..."        # Stripe TEST key
export STRIPE_WEBHOOK_SECRET="whsec_..."      # Stripe webhook secret

# 2. Configure database (PostgreSQL)
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/utsav"
export SPRING_DATASOURCE_USERNAME="utsav"
export SPRING_DATASOURCE_PASSWORD="secret"

# 3. Build and run
mvn -Pdev verify    # build + tests (H2)
mvn -Pdev spring-boot:run
```

Open http://localhost:8080 — Vaadin UI. API docs at http://localhost:8080/swagger-ui.html.

### API overview

| Area | Endpoints |
|------|-----------|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout` |
| Vendors | `GET /api/vendors/search`, `GET /api/vendors/nearby`, `GET /api/vendors/{id}`, `GET /api/vendors/{id}/collaborators` |
| Verification | `POST /api/vendors/me/verification`, `GET /api/admin/verifications` |
| Budget | `POST /api/budget/freeze`, `GET /api/budget/me` |
| Bookings | `POST /api/bookings`, `GET /api/bookings/me`, `PATCH /api/bookings/{id}/cancel` |
| Concierge | `POST /api/concierge/chat`, `POST /api/concierge/recommend` |
| Teams | `POST /api/teams`, `POST /api/teams/{id}/members` |
| Payments | `POST /api/payments/intent`, `GET /api/payments/{id}`, `POST /api/payments/{id}/refund`, `POST /api/payments/webhook` |
| Admin | `PATCH /api/admin/users/{id}/role`, `GET /api/admin/audit` |
| Catalog | `GET /api/catalog/categories` |

### What's stubbed vs real

| Real | Stubbed / needs input |
|------|----------------------|
| JWT auth, RBAC, lockout | Google OAuth2 (needs client ID/secret) |
| Vendor search/scoring | Real ID verification provider (status-only model ready) |
| Stripe TEST mode | Live Stripe keys (swap `sk_test_` → `sk_live_`) |
| AI concierge via Ollama | Cloud LLM (swap via Spring AI ChatClient) |
| Flyway migrations | Production deployment target (TBD) |
| Audit logging | Email/SMS notifications |

### OWASP Top 10 self-review

✅ **A01 Broken Access Control**: `@PreAuthorize` on all endpoints, ownership checks  
✅ **A02 Cryptographic Failures**: BCrypt, TLS-only cookies, no secrets in code  
✅ **A03 Injection**: JPA parameterized queries only, Bean Validation  
✅ **A04 Insecure Design**: Rate limiting, lockout, budget validation  
✅ **A05 Security Misconfiguration**: Fail-fast on secrets, security headers  
✅ **A06 Vulnerable Components**: Latest versions (SB 4.1.1, Vaadin 25.3.1)  
✅ **A07 Auth Failures**: JWT rotation, strong passwords, OAuth2 option  
✅ **A08 Data Integrity**: Webhook signature verification, audit log  
✅ **A09 Logging Failures**: Audit table for sensitive actions  
✅ **A10 SSRF**: No user-controlled outbound requests (Ollama is localhost-only)

### Backend tech

Java 25, Spring Boot 4.1.1, Vaadin 25.3.1, Spring AI 2.0.1, PostgreSQL 18, Flyway 13, jjwt 0.13.0, stripe-java 34.0.0, springdoc-openapi 3.1.1. See `backend/README.md` for details.
