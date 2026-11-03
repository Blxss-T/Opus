# Opus Frontend

React + Vite + TypeScript single-page app for the Opus Business Operations Management Platform. Talks to the Spring Boot API in the repo root via `Authorization: Bearer <JWT>` (no cookies).

## Current scope (Phase 1)

- Auth: login, register (creates a new organization + its `ORG_ADMIN`), forgot/reset password (OTP-based), email verification OTP screen.
- Dashboard: role-aware summary (`ORG_ADMIN`/`MANAGER` only) plus a low-stock table visible to every role.
- App shell with role-gated navigation; every other module (products, customers, suppliers, purchasing, sales, employees, users, organizations, API keys, audit) is scaffolded as a placeholder folder for a later phase — see each folder's `README.md`.

## Quick start

```bash
npm install
npm run dev       # http://localhost:5173
```

Requires the backend running separately (see the repo root README): `docker compose up -d` then `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,seed`. The backend's `dev` profile already allows CORS from `http://localhost:5173`, so no backend changes are needed.

### Environment

This repo does not track any `.env*` file (see the root `.gitignore`). Create `frontend/.env` (or `.env.development`) yourself with:

```
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### Demo accounts

Seeded by the backend's `seed` profile, org "Opus Demo Trading":

| Role | Email | Password |
| --- | --- | --- |
| `ORG_ADMIN` | `admin@opus-demo.test` | `DemoPass123!` |
| `MANAGER` | `manager@opus-demo.test` | `DemoPass123!` |
| `EMPLOYEE` | `employee@opus-demo.test` | `DemoPass123!` |

## Scripts

```bash
npm run dev            # Vite dev server
npm run build          # type-check (tsc -b) + production build
npm run test           # Vitest (unit/integration, MSW-mocked backend)
npm run test:watch     # Vitest in watch mode
npm run lint           # oxlint
npm run format         # prettier --write .
npm run format:check   # prettier --check .
```

## Architecture

- `src/api/` — axios client with envelope unwrapping and a single-retry-then-logout flow on `401`.
- `src/auth/` — auth API calls, `AuthContext` (session state), token storage (access token in memory, refresh token in `sessionStorage` — logging out when the tab closes is intentional, see code comments in `tokenStorage.ts`).
- `src/app/` — route tree, protected/role-gated route wrappers, the authenticated app shell.
- `src/dashboard/` — the one fully-built feature module; use it as the reference pattern for later modules (`types.ts`, `*Api.ts`, `pages/`, `components/`).
- `src/shared/` — cross-feature UI primitives and utilities.
