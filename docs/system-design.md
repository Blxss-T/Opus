# Opus — System Design

**Product:** Opus (Java packages remain `com.opsflow`)  
**Type:** Multi-tenant business operations platform (modular monolith)  
**Audience:** UI/UX design, frontend, and reviewers  
**Scope:** Architecture as implemented in this repository, plus production gaps the UI and later backend work must still plan for.

---

## 1. Purpose

Opus gives one organization a single backend for identity, people, catalog, buying, selling, reporting, and integrations. Tenants share one PostgreSQL database. Isolation is `organization_id` in Java **and** FORCE row-level security on org-owned tables (GUC `app.organization_id`). The browser never sends the organization id; the API takes it from the JWT or API key.

---

## 2. High-level architecture

```text
Browser (React + Vite, localhost:5173)
        |
        |  HTTPS later / HTTP locally
        |  Authorization: Bearer <access JWT>
        |  JSON envelope { success, data, error }
        v
Spring Boot 3.3 API  (com.opsflow, port 8080)
        |
        +-- Security filters (JWT, API key, rate limit, CORS)
        +-- Controllers  /api/v1/...
        +-- Services (transactions, tenant checks, stock)
        +-- Spring Data JPA
        v
PostgreSQL  (schemas: identity, catalog, purchasing, sales, ops, reporting)
Redis       (rate-limit counters, dashboard/low-stock cache)
```

Supporting processes in local Compose: PostgreSQL, Redis, Mailpit (OTP/reset emails). The API image can run under `docker compose --profile app`.

```text
Internet
   |
   v
[ Not implemented in this repo: Nginx TLS reverse proxy in front of the SPA + API ]
   |
   +--> Opus SPA (static)
   +--> Opus API
           +--> PostgreSQL (FORCE RLS)
           +--> Redis (rate limits + short dashboard cache)
           +--> Mail (SMTP / Mailpit in dev)
           +--> [ Not implemented: object storage for files ]
```

---

## 3. Runtime stack

| Layer | Choice |
| --- | --- |
| API | Java 21 target, Spring Boot 3.3.3, Maven |
| Web | Spring MVC, JSON, OpenAPI (`/swagger-ui.html`) |
| Security | Spring Security, BCrypt, JWT access tokens, hashed refresh tokens, hashed API keys |
| Persistence | PostgreSQL, Flyway V1–V18, Hibernate/JPA, FORCE RLS |
| Cache / limits | Redis (auth + authenticated + API-key counters; dashboard TTL cache) |
| Mail | Spring Mail; OTP and password reset |
| Frontend | React 19, Vite, TypeScript, TanStack Query, axios |
| Tests | JUnit/MockMvc/Testcontainers; Vitest + MSW on the SPA |

---

## 4. Request flow

1. Client calls `http://localhost:8080/api/v1/...` (SPA uses `VITE_API_BASE_URL`).
2. CORS: `dev` allows `http://localhost:5173` and `http://localhost:3000`. Other profiles require `CORS_ALLOWED_ORIGINS`.
3. Public auth routes do not need a JWT (login, register, Google, OTP, refresh, password forgot/reset).
4. Everything else needs a Bearer JWT **or** `X-API-Key: opk_...`.
5. Controllers stay thin. Services enforce organization from `UserPrincipal` (or the key’s org), roles via `@PreAuthorize`, and business rules. PostgreSQL RLS is a second filter. Sensitive mutations need a recent `POST /auth/step-up` (`reauthExp` on the access token).
6. Responses always wrap in `ApiResponse`: `{ success, data, error, timestamp }`.
7. List endpoints return `{ content: [], page: { size, number, totalElements, totalPages } }` with `page`, `size` (max 100), `sort`, and module filters.

---

## 5. Modules (bounded contexts)

| Module | Responsibility | UI today |
| --- | --- | --- |
| Auth / sessions | Register org+admin, login, Google ID token, OTP, refresh/logout, password change/reset | Screens exist |
| Organizations | Own org get/rename | Placeholder |
| Users | Invite MANAGER/EMPLOYEE, list, role, deactivate | Placeholder |
| Employees | HR records, optional link to a login user | Placeholder |
| Customers | Org customers, unique email per org, soft delete | List/create/detail |
| Suppliers | Vendors, unique name per org, soft delete | List/create/detail |
| Inventory | Products, stock, movements, optimistic `version` | List/create/detail + adjust stock |
| Purchasing | PO DRAFT → ORDERED → RECEIVED/CANCELLED; stock in on RECEIVED | Nav disabled |
| Sales | SO DRAFT → CONFIRMED → FULFILLED/CANCELLED; stock out on FULFILLED | Nav disabled |
| Reporting | Dashboard aggregates, low stock, trends | Dashboard only |
| API keys | Scoped machine access | Placeholder |
| Audit | Successful mutating API calls | Placeholder |

Pilot rule: **one `ORG_ADMIN` per organization** (partial unique index). `SUPER_ADMIN` exists on the enum and has **no** platform console.

---

## 6. Identity and authorization (for UI)

**Roles**

| Role | Typical UI |
| --- | --- |
| ORG_ADMIN | Full org: users, employees, catalog, orders, API keys, audit |
| MANAGER | Operations: catalog, customers, suppliers, orders, reports; not users/API keys/audit |
| EMPLOYEE | Read lists and self employee record; no create/update/delete on catalog/orders |

**Tokens**

- Access JWT ~15 minutes (`userId`, `organizationId`, `role`; optional `reauthExp` after step-up).
- Refresh token in `sessionStorage` on the SPA (tab close logs out). API stores only SHA-256 hashes; rotation + reuse detection.
- Password change/reset invalidates old access tokens and all refresh sessions.

**Do not** put `organizationId` on forms. **Do** hide nav items the role cannot use (see `frontend/src/nav/navConfig.ts`).

---

## 7. Business workflows the UI must express

**Purchase order**

```text
DRAFT --order--> ORDERED --receive--> RECEIVED
  |                 |
  +---- cancel -----+---- cancel ----> CANCELLED
```

Receiving adds stock (`INBOUND`) in one transaction.

**Sales order**

```text
DRAFT --confirm--> CONFIRMED --fulfil--> FULFILLED
  |                    |
  +------ cancel ------+---- cancel ----> CANCELLED
```

Fulfilling subtracts stock (`OUTBOUND`) and fails the whole transaction if stock would go negative.

**Stock** never goes below zero (`CHECK` + service). Concurrent updates use product `version` (optimistic lock). Show `409` / retry on conflict.

**Document numbers:** `PO-YYYY-####` and `SO-YYYY-####` per organization. Clients do not invent them.

---

## 8. Frontend vs backend (integration map)

| Screen | Backend | Notes for design |
| --- | --- | --- |
| Login / register / forgot / reset / OTP | Yes | Mailpit in dev for codes |
| Dashboard | Yes | Admin/manager summary; all roles see low stock |
| Products / customers / suppliers | Yes | Paginated tables, search `q`, soft delete |
| Purchase orders / sales orders | Yes API, no SPA | Design list + detail + status actions + line items |
| Users / employees / organization | Yes API, no SPA | Design role-gated admin |
| API keys / audit | Yes API, no SPA | Show raw key **once**; audit is append-only; create/revoke need step-up |

Shared UI patterns already in the SPA: app shell, role guards, pager, toasts, empty states. New modules should reuse them.

---

## 9. What a production backend still needs (not done, or not production-grade)

These are **real gaps**. Design the UI to tolerate them later (empty cache, 429, CDN), but do not assume they exist today.

| Capability | Status |
| --- | --- |
| **Redis / response caching** | Redis is in Compose. Dashboard summary and low-stock lists are cached (~45s) and invalidated on catalog/order writes. Not a general HTTP/CDN cache. |
| **Distributed rate limiting** | Redis counters for public auth, authenticated users, and API keys (`429` + `Retry-After`). Fail-open in dev if Redis is down; fail-closed in prod. Not a WAF/DDoS product. |
| **Pagination at CDN/search scale** | List `page`/`size` exists on the API. There is no cursor pagination, Elasticsearch, or infinite-scroll contract. |
| **Edge / API gateway security** | No Nginx TLS termination in the default run path, no WAF, no mTLS, no IP allowlists, no bot protection. |
| **Security headers / CSP** | Not a first-class hardened browser policy set for the SPA. |
| **Secrets management** | Env vars / `.env`; no Vault/KMS. Never commit `.env`. |
| **Object storage / files** | No invoices PDFs, employee IDs, or S3 abstraction in use. |
| **Invoices / payments** | No invoice or payment tables. |
| **Async jobs / queues** | `@Scheduled` MV refresh and OTP/refresh cleanup. Email is still synchronous; no Kafka/RabbitMQ. |
| **Observability stack** | Actuator health/metrics only. No Prometheus/Grafana/OpenTelemetry in the running product. |
| **SUPER_ADMIN / platform tenancy billing** | Not implemented. |

**Already present (do not design as if missing):** JWT + refresh, step-up re-auth, tenant isolation in Java + FORCE RLS, BCrypt, API keys, Redis rate limits, dashboard cache, idempotency on order create, audit of successful writes, Flyway V1–V18, OpenAPI, CORS, validation envelope.

---

## 10. Deployment (current)

Local: Postgres + Redis + Mailpit via Compose; API via `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,seed`; SPA via `npm run dev`.  
Optional: `docker compose --profile app up --build` for the API container.  
CI: GitHub Actions `./mvnw verify` + image build. Load scripts: `perf/k6/` (not gated on every PR). UI UAT: `docs/uat-user-journeys.md`.

Target later: TLS → reverse proxy / WAF → SPA + API → PostgreSQL → Redis.
