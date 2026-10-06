# Opus - Business Operations Management Platform

Opus is a cloud-based **Business Operations Management Platform** built with Java 21 and Spring Boot 3 as a modular monolith. Java packages remain `com.opsflow` for stability; the product name is Opus.

## Current scope

Increments 1-4:

- Foundation: Spring Boot 3.3, Java 21, Flyway, PostgreSQL, Actuator, JWT, API envelope
- Organizations and users with BCrypt passwords
- Auth: register, login, current user, email OTP (verification proof), Google ID token sign-in
- Pilot rule: at most one `ORG_ADMIN` per organization (Java check + unique partial index)
- Employees: org-scoped CRUD with role rules and tenant isolation
- Customers: org-scoped CRUD against Flyway V9 (`uk_customers_org_email`), soft delete
- Suppliers & Purchase Orders: vendor CRUD, PO lifecycle `DRAFT -> ORDERED -> RECEIVED/CANCELLED`, automatic inventory replenishment on receiving (Flyway V8)
- Sales Orders: customer sales with lifecycle `DRAFT -> CONFIRMED -> FULFILLED/CANCELLED`, automatic inventory depletion on fulfillment (Flyway V10)
- Reporting & Dashboard: read-only aggregated metrics (revenue, spend, low-stock alerts, top customers/suppliers, category breakdowns, daily/monthly sales trends) with tenant isolation
- Sessions & Hardening: refresh token rotation with reuse detection, logout, password change/reset (OTP-based), password-changed session invalidation, and IP rate limiting on public auth endpoints

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
- Apache Maven 3.9+
- Docker and Docker Compose

### Infrastructure

```bash
docker compose up -d
```

Copy `.env.example` to `.env` if you want local overrides. Do not commit real secrets.

### Run

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Verify

- Health: `http://localhost:8080/api/v1/health`
- Actuator: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`

### Tests

```bash
mvn test
```

PostgreSQL Testcontainers tests use the `it` profile and require Docker.

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

Google mock tokens (`mock-google-token:email:First:Last`) work only when `security.google.allow-mock-tokens=true` (dev/test). Production must set `GOOGLE_CLIENT_ID` and a unique `JWT_SECRET`.

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

Organization is taken from the JWT, never from the client body. Email is unique per organization (normalized to lowercase). Delete is a soft delete (`active = false`).

| Method | Path | Roles |
| --- | --- | --- |
| GET | `/customers` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| POST | `/customers` | `ORG_ADMIN`, `MANAGER` |
| GET | `/customers/{id}` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| PUT | `/customers/{id}` | `ORG_ADMIN`, `MANAGER` |
| DELETE | `/customers/{id}` | `ORG_ADMIN`, `MANAGER` |

## Supplier APIs

| Method | Path | Roles |
| --- | --- | --- |
| GET | `/suppliers` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| POST | `/suppliers` | `ORG_ADMIN`, `MANAGER` |
| GET | `/suppliers/{id}` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| PUT | `/suppliers/{id}` | `ORG_ADMIN`, `MANAGER` |
| DELETE | `/suppliers/{id}` | `ORG_ADMIN`, `MANAGER` |

## Product APIs

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

Organization is taken from the JWT, never from the client body. PO numbers are generated per organization (`PO-YYYY-####`). Receiving a PO automatically creates `INBOUND` stock movements.

| Method | Path | Roles |
| --- | --- | --- |
| POST | `/purchase-orders` | `ORG_ADMIN`, `MANAGER` |
| GET | `/purchase-orders` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| GET | `/purchase-orders/{id}` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| PATCH | `/purchase-orders/{id}/status` | `ORG_ADMIN`, `MANAGER` |

## Sales Order APIs

Organization is taken from the JWT, never from the client body. SO numbers are generated per organization (`SO-YYYY-####`). Fulfilling an SO automatically creates `OUTBOUND` stock movements and fails atomically if stock is insufficient.

| Method | Path | Roles |
| --- | --- | --- |
| POST | `/sales-orders` | `ORG_ADMIN`, `MANAGER` |
| GET | `/sales-orders` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
| GET | `/sales-orders/{id}` | `ORG_ADMIN`, `MANAGER`, `EMPLOYEE` |
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

## Sessions

- Access tokens are short-lived JWTs; refresh tokens are random 256-bit values stored only as SHA-256 hashes (`refresh_tokens` table, 7-day TTL by default via `REFRESH_TOKEN_TTL` minutes).
- Refresh tokens are single use: each refresh returns a new token and revokes the old one. Presenting a revoked, expired, or already-rotated token revokes **all** sessions for that user (stolen-token mitigation).
- Password change/reset stamps `users.password_changed_at`; access tokens issued before that instant are rejected by the JWT filter, and all refresh sessions are revoked.

## Rate limiting

Public auth endpoints (`/auth/login`, `/auth/register`, `/auth/google`, `/auth/otp/send`, `/auth/password/forgot`, `/auth/password/reset`) are rate limited per IP + endpoint: default 10 requests per 60 seconds, returning `429 Too Many Requests` with a `Retry-After` header. Tune with `RATE_LIMIT_ENABLED`, `RATE_LIMIT_LIMIT`, `RATE_LIMIT_WINDOW_SECONDS`. The implementation is in-memory (single node); move to Redis when scaling beyond one instance.

## Environment

See `.env.example` for `DB_*`, `JWT_SECRET`, `REFRESH_TOKEN_TTL`, `RATE_LIMIT_*`, `GOOGLE_CLIENT_ID`, `OTP_PEPPER`, and `MAIL_*`.
