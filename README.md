# Opus - Business Operations Management Platform

Opus is a cloud-based **Business Operations Management Platform** built with Java 21 and Spring Boot 3 as a modular monolith. Java packages remain `com.opsflow` for stability; the product name is Opus.

## Current scope

Increments 1-4:

- Foundation: Spring Boot 3.3, Java 21, Flyway, PostgreSQL, Actuator, JWT, API envelope
- Organizations and users with BCrypt passwords
- Auth: register, login, current user, email OTP (verification proof), Google ID token sign-in
- Pilot rule: at most one `ORG_ADMIN` per organization (Java check + unique partial index)
- Users: `ORG_ADMIN` creates and manages `MANAGER` / `EMPLOYEE` accounts in their organization
- Employees: org-scoped CRUD with role rules and tenant isolation
- Customers: org-scoped CRUD against Flyway V9 (`uk_customers_org_email`), soft delete
- Suppliers & Purchase Orders: vendor CRUD, PO lifecycle `DRAFT -> ORDERED -> RECEIVED/CANCELLED`, automatic inventory replenishment on receiving (Flyway V8)
- Sales Orders: customer sales with lifecycle `DRAFT -> CONFIRMED -> FULFILLED/CANCELLED`, automatic inventory depletion on fulfillment (Flyway V10)
- Reporting & Dashboard: read-only aggregated metrics (revenue, spend, low-stock alerts, top customers/suppliers, category breakdowns, daily/monthly sales trends) with tenant isolation
- Sessions & Hardening: refresh token rotation with reuse detection, logout, password change/reset (OTP-based), password-changed session invalidation, and IP rate limiting on public auth endpoints
- Integrations: scoped API keys (`X-API-Key`) with per-key rate limiting, and `Idempotency-Key` support on order creation
- Audit log: who (user or API key) changed what, readable by the `ORG_ADMIN`

Connecting another application? See the [integration guide](docs/integration-guide.md).

## Stack

- **Runtime**: Java 21, Spring Boot 3.3.3
- **Database**: PostgreSQL 16, schema owned by Flyway
- **Security**: Spring Security + JWT (BCrypt passwords)
- **Docs**: OpenAPI / Swagger UI (`/swagger-ui.html`)
- **Health**: `/api/v1/health` and `/actuator/health`

Redis is intentionally **not** in local compose until there is a caching or rate-limit need.

## Quick start

### Prerequisites

- JDK 21+
- Maven is not required: the bundled wrapper (`./mvnw`, or `mvnw.cmd` on Windows cmd) downloads it
- Docker and Docker Compose

### Infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL and Mailpit. Emails sent in the dev profile (OTP and password reset codes) are viewable at `http://localhost:8025`.

Copy `.env.example` to `.env` if you want local overrides. Do not commit real secrets.

### Run

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run everything in Docker

To run the API itself in a container alongside PostgreSQL and Mailpit, without a local JDK:

```bash
docker compose --profile app up -d --build
```

The API is then on `http://localhost:8080` with the `dev,seed` profiles (demo data, Mailpit email). Set `APP_PORT` or `DB_PORT` if 8080 or 5432 are already in use, and `APP_PROFILES=prod` with the required secrets in `.env` for a real deployment. The same `Dockerfile` builds the image for any host: `docker build -t opus-api .`

### Demo data

To start with a populated database, add the `seed` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,seed
```

On first start this creates the organization "Opus Demo Trading" with employees, customers, suppliers, products (two below their reorder level), and purchase and sales orders in every status, so each endpoint and report returns data. It is skipped on later starts and never runs in the `prod` profile.

| Role | Email | Password |
| --- | --- | --- |
| `ORG_ADMIN` | `admin@opus-demo.test` | `DemoPass123!` |
| `MANAGER` | `manager@opus-demo.test` | `DemoPass123!` |
| `EMPLOYEE` | `employee@opus-demo.test` | `DemoPass123!` |

Override the password with `OPUS_SEED_PASSWORD`.

### Verify

- Health: `http://localhost:8080/api/v1/health`
- Actuator: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`

### Postman

Import `postman/opus.postman_collection.json`. Run **Auth > Login** first: it stores the access and refresh tokens as collection variables and every other request sends them automatically. Create requests store the new record's id for the requests that follow. The collection defaults to `http://localhost:8080/api/v1` and the demo admin account; change `baseUrl`, `email` and `password` under the collection's Variables tab.

### Tests

```bash
./mvnw test
```

`./mvnw test` runs the unit and controller tests on in-memory H2. To also run the PostgreSQL integration tests (`*IT` classes, which apply the real Flyway migrations in a Testcontainers database), use:

```bash
./mvnw verify
```

Integration tests use the `it` profile and require Docker; they are skipped when Docker is not running.

GitHub Actions (`.github/workflows/ci.yml`) runs `./mvnw verify` and builds the Docker image on every push to `main` and every pull request.

## Auth APIs (`/api/v1`)

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| POST | `/auth/register` | public | Create organization + first `ORG_ADMIN` |
| POST | `/auth/login` | public | Password login, returns JWT |
| POST | `/auth/google` | public | Verify Google ID token; provision personal org if new |
| GET | `/auth/me` | JWT | Current user |
| POST | `/auth/otp/send` | public | Send 6-digit email OTP (hashed at rest, 10 minute expiry) |
| POST | `/auth/otp/verify` | public | Verify OTP; sets `email_verified_at` when a user exists |
| POST | `/auth/refresh` | public (refresh token) | Exchange refresh token for a new JWT + rotated refresh token; reuse revokes all sessions |
| POST | `/auth/logout` | JWT | Revoke all refresh token sessions for the current user |
| POST | `/auth/password/change` | JWT | Change password (current password required); revokes all sessions |
| POST | `/auth/password/forgot` | public | Send password reset code; response never reveals account existence |
| POST | `/auth/password/reset` | public | Reset password with emailed code; revokes all sessions |

Google mock tokens (`mock-google-token:email:First:Last`) work only when `security.google.allow-mock-tokens=true` (dev/test). Production must set a unique `JWT_SECRET` (32+ characters) and `OTP_PEPPER` (16+ characters); the app refuses to start in the `prod` profile if either is missing or still a committed dev/example value. Set `GOOGLE_CLIENT_ID` to enable Google sign-in; without it every Google token is rejected.

## Pagination, filtering & sorting

All list endpoints share a consistent contract:

- `page` (0-based, default 0) and `size` (default 20, max 100)
- `sort` — e.g. `?sort=name,asc` or `?sort=createdAt,desc`
- Optional filters per endpoint (see the module tables below); text search parameters are case-insensitive "contains" matches
- Every page response is serialized in a stable shape: `data.content[]` plus `data.page` (`size`, `number`, `totalElements`, `totalPages`)
- Invalid filter values (e.g. `?status=BOGUS`) return `400 ERR_400`

| Endpoint | Filters |
| --- | --- |
| GET `/customers` | `q` (name, email) |
| GET `/suppliers` | `q` (name, email, contact person) |
| GET `/products` | `q` (name, SKU), `category` (case-insensitive exact) |
| GET `/products/{id}/movements` | `type` (INBOUND, OUTBOUND, ADJUSTMENT, RETURN, DAMAGE) |
| GET `/employees` | `q` (first name, last name, email), `status` (ACTIVE, ON_LEAVE, TERMINATED) |
| GET `/users` | `q` (first name, last name, email), `role` (ORG_ADMIN, MANAGER, EMPLOYEE), `active` (true, false) |
| GET `/purchase-orders` | `status` (DRAFT, ORDERED, RECEIVED, CANCELLED) |
| GET `/sales-orders` | `status` (DRAFT, CONFIRMED, FULFILLED, CANCELLED) |

Default sorts: customers/suppliers/products by `name` asc, employees by name, orders and movements by `createdAt` desc.

## Organization APIs

The organization is always the caller's own, taken from the JWT.

| Method | Path | Roles |
| --- | --- | --- |
| GET | `/organization` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| PUT | `/organization` | `ORG_ADMIN` (renames it; the slug does not change) |

## User APIs

Organization is taken from the JWT, never from the client body. The `ORG_ADMIN` creates accounts with an initial password; the new user signs in with `/auth/login` and can change it via `/auth/password/change`. Only `MANAGER` and `EMPLOYEE` can be assigned, and the `ORG_ADMIN` account cannot be demoted or deactivated. Emails are unique across all organizations. Role changes and deactivation take effect immediately, including for access tokens already issued.

| Method | Path | Roles |
| --- | --- | --- |
| POST | `/users` | `ORG_ADMIN` |
| GET | `/users` | `ORG_ADMIN`, `MANAGER` |
| GET | `/users/{id}` | `ORG_ADMIN`, `MANAGER` |
| PUT | `/users/{id}` | `ORG_ADMIN` (first name, last name, role) |
| DELETE | `/users/{id}` | `ORG_ADMIN` (deactivates and revokes all sessions) |
| POST | `/users/{id}/activate` | `ORG_ADMIN` |

## Employee APIs

Organization is taken from the JWT, never from the client body.

| Method | Path | Roles |
| --- | --- | --- |
| GET | `/employees` | `ORG_ADMIN`, `MANAGER` |
| POST | `/employees` | `ORG_ADMIN` |
| GET | `/employees/me` | authenticated, linked employee record |
| GET | `/employees/{id}` | `ORG_ADMIN`, `MANAGER`; `EMPLOYEE` only for self |
| PUT | `/employees/{id}` | `ORG_ADMIN`, `MANAGER` |
| DELETE | `/employees/{id}` | `ORG_ADMIN` (marks `TERMINATED`) |

## Customer APIs

Organization is taken from the JWT, never from the client body. Email is unique per organization (normalized to lowercase). Delete is a soft delete (`active = false`). List supports `q` search (name/email).

| Method | Path | Roles |
| --- | --- | --- |
| GET | `/customers` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| POST | `/customers` | `ORG_ADMIN`, `MANAGER` |
| GET | `/customers/{id}` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| PUT | `/customers/{id}` | `ORG_ADMIN`, `MANAGER` |
| DELETE | `/customers/{id}` | `ORG_ADMIN`, `MANAGER` |

## Supplier APIs

List supports `q` search (name/email/contact person).

| Method | Path | Roles |
| --- | --- | --- |
| GET | `/suppliers` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| POST | `/suppliers` | `ORG_ADMIN`, `MANAGER` |
| GET | `/suppliers/{id}` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| PUT | `/suppliers/{id}` | `ORG_ADMIN`, `MANAGER` |
| DELETE | `/suppliers/{id}` | `ORG_ADMIN`, `MANAGER` |

## Product APIs

List supports `q` search (name/SKU) and `category` filter. Movement history supports `type` filter.

| Method | Path | Roles |
| --- | --- | --- |
| POST | `/products` | `ORG_ADMIN`, `MANAGER` |
| GET | `/products` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| GET | `/products/{id}` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| PUT | `/products/{id}` | `ORG_ADMIN`, `MANAGER` |
| DELETE | `/products/{id}` | `ORG_ADMIN`, `MANAGER` |
| POST | `/products/{id}/adjust-stock` | `ORG_ADMIN`, `MANAGER` |
| GET | `/products/{id}/movements` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |

## Purchase Order APIs

Organization is taken from the JWT, never from the client body. PO numbers are generated per organization (`PO-YYYY-####`). Receiving a PO automatically creates `INBOUND` stock movements. List supports `status` filter.

| Method | Path | Roles |
| --- | --- | --- |
| POST | `/purchase-orders` | `ORG_ADMIN`, `MANAGER` |
| GET | `/purchase-orders` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| GET | `/purchase-orders/{id}` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| PUT | `/purchase-orders/{id}` | `ORG_ADMIN`, `MANAGER` (replaces a `DRAFT` order's supplier, notes, date and items) |
| PATCH | `/purchase-orders/{id}/status` | `ORG_ADMIN`, `MANAGER` |

## Sales Order APIs

Organization is taken from the JWT, never from the client body. SO numbers are generated per organization (`SO-YYYY-####`). Fulfilling an SO automatically creates `OUTBOUND` stock movements and fails atomically if stock is insufficient. List supports `status` filter.

| Method | Path | Roles |
| --- | --- | --- |
| POST | `/sales-orders` | `ORG_ADMIN`, `MANAGER` |
| GET | `/sales-orders` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| GET | `/sales-orders/{id}` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| PUT | `/sales-orders/{id}` | `ORG_ADMIN`, `MANAGER` (replaces a `DRAFT` order's customer, notes, date and items) |
| PATCH | `/sales-orders/{id}/status` | `ORG_ADMIN`, `MANAGER` |

## Reporting APIs

All metrics are scoped to the caller's organization. Revenue counts `FULFILLED` sales orders; spend counts `RECEIVED` purchase orders. Drafts and cancelled orders are excluded.

| Method | Path | Roles | Purpose |
| --- | --- | --- | --- |
| GET | `/reports/dashboard` | `ORG_ADMIN`, `MANAGER` | Aggregate counters: products, low stock, customers, suppliers, orders, revenue, spend, 30-day movements, SO status breakdown |
| GET | `/reports/dashboard/low-stock` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` | Active products at or below reorder level (most urgent first) |
| GET | `/reports/sales/by-category` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` | Fulfilled sales revenue by product category |
| GET | `/reports/purchases/by-category` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` | Received purchase spend by product category |
| GET | `/reports/sales/top-customers` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` | Customers ranked by fulfilled revenue (`limit`, default 5, max 25) |
| GET | `/reports/purchases/top-suppliers` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` | Suppliers ranked by received spend (`limit`, default 5, max 25) |
| GET | `/reports/sales/daily` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` | Fulfilled sales per day, last 30 days |
| GET | `/reports/sales/monthly-trend` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` | Fulfilled sales per month |

## API keys (access for other applications)

Another application calls the API with an API key instead of a person's login. The `ORG_ADMIN` creates the key while signed in; the application then sends it on every request:

```bash
curl -H "X-API-Key: opk_..." http://localhost:8080/api/v1/products
```

| Method | Path | Roles | Purpose |
| --- | --- | --- | --- |
| GET | `/api-keys/scopes` | `ORG_ADMIN` | Scopes that can be granted |
| POST | `/api-keys` | `ORG_ADMIN` | Create a key (`name`, `scopes[]`, optional `expiresAt`); the raw key is returned once in `data.key` |
| GET | `/api-keys` | `ORG_ADMIN` | List keys (prefix only), including revoked and expired ones |
| DELETE | `/api-keys/{id}` | `ORG_ADMIN` | Revoke a key immediately |

- **Scopes.** `products`, `customers`, `suppliers`, `purchase-orders` and `sales-orders` each have `:read` and `:write`; `employees` and `reports` have `:read` only. `GET` requests need the read scope, everything else the write scope, and a write scope includes read. A request outside the key's scopes returns `403`.
- **Limits of a key.** A key only sees its own organization and cannot reach the user, auth or API key endpoints, so it cannot create users or other keys.
- **Storage.** Only a SHA-256 hash and a short display prefix are stored. A lost key cannot be recovered; revoke it and create a new one.
- **Rate limit.** Each key gets 120 requests per minute across all endpoints (`API_KEY_RATE_LIMIT`, `API_KEY_RATE_LIMIT_WINDOW_SECONDS`), then `429` with `Retry-After`. In-memory, single node.
- **Attribution.** Orders and stock movements written by a key are recorded as `api-key:<name>`.

## Idempotent order creation

`POST /purchase-orders` and `POST /sales-orders` accept an optional `Idempotency-Key` header (up to 100 characters, for example a UUID). If a request times out, retry it with the same key: the API returns the order created the first time with `200` and `Idempotency-Replayed: true` instead of creating a duplicate. Keys are scoped to the organization, and the first request's body wins; the body of a replay is not compared.

## Audit log

Every successful change made through the API (any non-`GET` request that returns `2xx`) by a signed-in user or an API key is recorded: who did it, the method and path, the resource type and id, the status code and the client IP. Reads, failed requests and unauthenticated calls such as login are not recorded. Request bodies are not stored.

| Method | Path | Roles | Filters |
| --- | --- | --- | --- |
| GET | `/audit-log` | `ORG_ADMIN` | `actor` (email or `api-key:<name>`, contains), `actorType` (USER, API_KEY), `resourceType` (e.g. `products`), `resourceId` |

Newest first, with the usual `page`, `size` and `sort`.

## Sessions

- Access tokens are short-lived JWTs (15 minutes by default, `JWT_EXPIRATION_MS` in milliseconds); clients call `/auth/refresh` to get a new one. Refresh tokens are random 256-bit values stored only as SHA-256 hashes (`refresh_tokens` table, 7-day TTL by default via `REFRESH_TOKEN_TTL` minutes).
- Refresh tokens are single use: each refresh returns a new token and revokes the old one. Presenting a revoked, expired, or already-rotated token revokes **all** sessions for that user (stolen-token mitigation).
- Password change/reset stamps `users.password_changed_at`; access tokens issued before that instant are rejected by the JWT filter, and all refresh sessions are revoked.

## Rate limiting

Public auth endpoints (`/auth/login`, `/auth/register`, `/auth/google`, `/auth/otp/send`, `/auth/password/forgot`, `/auth/password/reset`) are rate limited per IP + endpoint: default 10 requests per 60 seconds, returning `429 Too Many Requests` with a `Retry-After` header. Tune with `RATE_LIMIT_ENABLED`, `RATE_LIMIT_LIMIT`, `RATE_LIMIT_WINDOW_SECONDS`. The implementation is in-memory (single node); move to Redis when scaling beyond one instance.

The client IP is the connection's remote address; a client-supplied `X-Forwarded-For` header is ignored. When deploying behind a reverse proxy or load balancer, set `FORWARD_HEADERS_STRATEGY=native` so the real client IP is taken from the trusted proxy's forwarded headers — otherwise every user shares the proxy's IP and one limit.

## CORS

Browser frontends on another origin must be listed in `CORS_ALLOWED_ORIGINS` (comma-separated, applies to `/api/**`). The dev profile defaults to `http://localhost:3000` and `http://localhost:5173`; other profiles default to empty, which blocks all cross-origin requests. Authentication uses the `Authorization: Bearer` header, so cookies/credentials are not enabled.

## Environment

See `.env.example` for `DB_*`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `REFRESH_TOKEN_TTL`, `RATE_LIMIT_*`, `CORS_ALLOWED_ORIGINS`, `GOOGLE_CLIENT_ID`, `OTP_PEPPER`, `API_KEY_RATE_LIMIT*`, and `MAIL_*`.
