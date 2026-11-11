# Software Requirements Specification (SRS)

**Product name:** Opus  
**Document version:** 1.1  
**Date:** 2026-11-11  
**Status:** Reflects the current repository (schemas, FORCE RLS, Redis rate limits/cache, step-up). Functional requirements below are implemented unless marked **[NOT IMPLEMENTED]**.

---

## 1. Introduction

### 1.1 Purpose

This SRS specifies Opus, a cloud-oriented business operations backend with a React SPA. It is written so UI design can map screens to requirements, and so reviewers can see what a production-grade backend still lacks.

### 1.2 Product overview

Opus is a **modular monolith**: one Spring Boot application, one PostgreSQL database, organization-level multi-tenancy. Java packages use `com.opsflow`; the product name is Opus.

### 1.3 Users

| Actor | Description |
| --- | --- |
| ORG_ADMIN | Creates the org (or is the first user), manages users, settings, keys, audit, and all operations |
| MANAGER | Day-to-day operations: catalog, parties, orders, reports |
| EMPLOYEE | Limited read access and own employee profile |
| External application | Calls the API with a scoped API key (no user UI) |
| SUPER_ADMIN | **[NOT IMPLEMENTED]** Platform operator across organizations |

### 1.4 References

- [system-design.md](system-design.md)  
- [database-design.md](database-design.md)  
- [integration-guide.md](integration-guide.md)  
- OpenAPI: `/swagger-ui.html`  
- Postman: `postman/opus.postman_collection.json`

---

## 2. Overall description

### 2.1 Operating environment

- API: Java 21 / Spring Boot 3.3, PostgreSQL, Redis  
- SPA: browser, Vite dev server or static host  
- Local: Mailpit + Redis via Compose  

### 2.2 Design constraints

- REST + JSON under `/api/v1`  
- Tenant id never trusted from the client  
- Passwords BCrypt; OTP and API keys hashed at rest  
- Flyway is the only schema source  

### 2.3 Assumptions

- One organization per logged-in user (no user switching orgs)  
- Demo seed: `admin@opus-demo.test` / `DemoPass123!`  

---

## 3. Functional requirements (implemented)

### 3.1 Authentication and accounts

| ID | Requirement |
| --- | --- |
| FR-A1 | A visitor can register an organization and become its single ORG_ADMIN |
| FR-A2 | A user can log in with email and password and receive access + refresh tokens |
| FR-A3 | A user can sign in with a Google ID token; a new Google user gets a personal organization |
| FR-A4 | `GET /auth/me` returns the current user |
| FR-A5 | Access tokens expire; `POST /auth/refresh` rotates the refresh token; reuse of an old refresh token revokes all sessions |
| FR-A6 | `POST /auth/logout` revokes refresh sessions |
| FR-A7 | Authenticated password change requires the current password and invalidates sessions |
| FR-A8 | Forgot/reset password uses emailed OTP; responses do not reveal whether the email exists |
| FR-A9 | Email OTP send/verify (hashed codes, expiry, attempt lock) |
| FR-A10 | Inactive user or organization cannot authenticate |
| FR-A11 | `POST /auth/step-up` with the current password issues an access token with `reauthExp` |
| FR-A12 | API key create/revoke, user role change/deactivate, org rename, PO receive, and SO fulfil require a non-expired `reauthExp` (API keys themselves are exempt) |

### 3.2 Users and employees

| ID | Requirement |
| --- | --- |
| FR-U1 | ORG_ADMIN creates MANAGER/EMPLOYEE login users |
| FR-U2 | ORG_ADMIN cannot create a second ORG_ADMIN |
| FR-U3 | ORG_ADMIN updates names/role (except demoting/deactivating themselves as the sole admin) |
| FR-U4 | ORG_ADMIN deactivates/reactivates users; deactivation revokes sessions |
| FR-U5 | Employee CRUD with optional link to a user; EMPLOYEE may only read self |
| FR-U6 | Organization get; ORG_ADMIN may rename (slug unchanged) |

### 3.3 Parties and catalog

| ID | Requirement |
| --- | --- |
| FR-C1 | Customer CRUD, unique email per org, soft delete |
| FR-S1 | Supplier CRUD, unique name per org, soft delete |
| FR-P1 | Product CRUD, unique SKU per org, prices and stock >= 0 |
| FR-P2 | Stock adjustment writes an inventory movement |
| FR-P3 | Movement history is listable per product |

### 3.4 Orders

| ID | Requirement |
| --- | --- |
| FR-PO1 | Create/list/get/update DRAFT purchase orders with lines |
| FR-PO2 | Status: DRAFT→ORDERED→RECEIVED or CANCELLED from DRAFT/ORDERED |
| FR-PO3 | RECEIVED increases product stock atomically |
| FR-SO1 | Create/list/get/update DRAFT sales orders with lines |
| FR-SO2 | Status: DRAFT→CONFIRMED→FULFILLED or CANCELLED from DRAFT/CONFIRMED |
| FR-SO3 | FULFILLED decreases stock atomically; insufficient stock → 422 |
| FR-O1 | Server generates PO/SO numbers |
| FR-O2 | Optional `Idempotency-Key` on create |

### 3.5 Reporting

| ID | Requirement |
| --- | --- |
| FR-R1 | Dashboard counters (ORG_ADMIN, MANAGER) |
| FR-R2 | Low-stock list (all org roles) |
| FR-R3 | Sales/purchases by category, top customers/suppliers, daily and monthly sales |

### 3.6 Integrations and audit

| ID | Requirement |
| --- | --- |
| FR-K1 | ORG_ADMIN creates/lists/revokes API keys; raw key shown once |
| FR-K2 | Keys are org-scoped and scope-checked (`products:read`, etc.) |
| FR-G1 | Successful non-GET 2xx calls are written to the audit log |
| FR-G2 | ORG_ADMIN can list/filter audit events |

### 3.7 Cross-cutting (present in the API)

| ID | Requirement |
| --- | --- |
| FR-X1 | Consistent error envelope; no stack traces to clients |
| FR-X2 | Jakarta Validation on request bodies |
| FR-X3 | Role annotations + tenant queries; PostgreSQL FORCE RLS as defense in depth |
| FR-X4 | List endpoints accept `page`, `size`, `sort`, and documented filters |
| FR-X5 | Redis rate limits: public auth (IP+path), authenticated (org+user), API keys; `429` + `Retry-After` |
| FR-X6 | Dashboard and low-stock responses may be Redis-cached with write invalidation |

---

## 4. Non-functional requirements — current vs production

The items in **4.2** are what a real backend still needs. Treat them as **[NOT IMPLEMENTED]** or **not production-grade** even if a small local substitute exists. UI design should still reserve states for them (429, slow lists, cached freshness).

### 4.1 Met today (local / single node)

| ID | Requirement | Notes |
| --- | --- | --- |
| NFR-1 | Password hashing (BCrypt) | |
| NFR-2 | JWT signature + expiry | |
| NFR-3 | HTTPS-ready app (TLS is the proxy’s job) | Dev is HTTP |
| NFR-4 | Health endpoint | `/api/v1/health`, `/actuator/health` |
| NFR-5 | Schema migrations | Flyway V1–V18; PostgreSQL schemas + FORCE RLS |
| NFR-6 | Automated tests | Maven + Vitest |

### 4.2 Not implemented or not production-grade **[NOT IMPLEMENTED]**

| ID | Requirement | Why it is listed |
| --- | --- | --- |
| NFR-P1 | **Production API security** | No API gateway, WAF, mTLS, IP allowlist, bot management, or hardened security-header/CSP profile for the SPA. JWT/API keys exist; edge security does not. |
| NFR-P2 | **Edge DDoS / WAF rate limiting** | App Redis limits exist (auth, session, API keys). There is still no gateway/WAF. Do not claim DDoS protection. |
| NFR-P3 | **HTTP/CDN caching** | Redis caches dashboard + low stock only. No CDN, no cache for arbitrary list endpoints. |
| NFR-P4 | **Pagination at product scale** | The API has offset pagination (`page`/`size`). There is **no** cursor pagination, no search index, no guaranteed performance past modest org data. UI should still use the existing pager; a “load 10k rows at once” design is out of scope. |
| NFR-P5 | **Session/store scale-out** | Refresh tokens are in Postgres; rate-limit counters are in Redis. Some OTP send-cooldown behaviour is still JVM-local. |
| NFR-P6 | **Secrets management** | No Vault/KMS; secrets in env. |
| NFR-P7 | **Observability platform** | No Prometheus, Grafana, tracing, or centralized log drain. |
| NFR-P8 | **File storage** | No uploads, virus scan, or S3. |
| NFR-P9 | **Durable async processing** | Scheduled MV refresh and session cleanup exist. Email is still synchronous; no Kafka/RabbitMQ. |
| NFR-P10 | **Backup/HA/DR** | Not specified in-app. |
| NFR-P11 | **Dependency CVE pipeline in product ops** | CI builds/tests; no stated production SCA gate. |
| NFR-P12 | **2FA login** | Enum `LOGIN_2FA` exists; no login challenge. |
| NFR-P13 | **Invoices, payments, tax** | Not in schema. |
| NFR-P14 | **Multi-region / read replicas** | Not implemented. |

---

## 5. UI requirements derived from this SRS

Must-have screens (API ready):

1. Auth suite (done in SPA)  
2. Dashboard + low stock (done)  
3. Products, customers, suppliers CRUD (done)  
4. Purchase order list/detail/status (to design)  
5. Sales order list/detail/status with stock errors (to design)  
6. Users + employees (to design)  
7. Organization profile (to design)  
8. API keys with one-time secret modal (to design)  
9. Audit log table (to design)  

UX rules:

- Role-gated navigation  
- Pagination controls on every collection  
- Toast/banner for `ERR_422`, `ERR_409`, `ERR_429`; password modal for step-up `403` (see [uat-user-journeys.md](uat-user-journeys.md))  
- Never collect organization id  
- Confirm destructive status changes (receive, fulfil, cancel, revoke key)

---

## 6. Out of scope (this version)

Microservices, Kafka, GraphQL, mobile apps, SSO/SAML, multi-currency tax engines, warehouse bins, e-commerce storefront, AI analytics.

---

## 7. Acceptance snapshot

The backend is acceptable for UI design when:

- `GET /api/v1/health` is UP  
- Demo admin can log in  
- Swagger lists the modules in §3  
- SPA on `:5173` can call `:8080` under the `dev` CORS policy  

Production launch additionally requires the remaining items in §4.2 (gateway/WAF, TLS, HA, files, invoices as product decides).
