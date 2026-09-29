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

## Environment

See `.env.example` for `DB_*`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `OTP_PEPPER`, and `MAIL_*`.
