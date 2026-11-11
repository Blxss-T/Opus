# UAT user journeys

Brief for the SPA. Personas map to JWT roles except Integrator, who uses API keys only.

| Persona | Builds UI for |
| --- | --- |
| ORG_ADMIN | Users, API keys, audit, org rename, step-up wall |
| MANAGER | Catalog, customers, suppliers, PO/SO status, dashboard |
| EMPLOYEE | Read lists, low stock, own employee record |
| Integrator | API keys + Postman, no SPA |

Keep the current screens. Next UAT-critical UI is **sales/purchase orders** (stock), then **users/employees**, then **API keys/audit**.

## Shared negatives

| Case | Expected |
| --- | --- |
| Resource id from another organization | `404` |
| Illegal PO/SO status transition | `422` |
| Fulfil SO that would drive stock negative | `422` |
| Missing/expired access token | `401` |
| Role cannot perform the action | `403` |
| Sensitive action without recent password | `403` `"Re-authentication required"` |
| Too many public auth or API calls | `429` + `Retry-After` |

## Step-up (SPA)

Before create/revoke API keys, deactivate user / change role, rename organization, receive a PO, or fulfil an SO, open a **confirm password** modal, `POST /api/v1/auth/step-up` with `{ "password" }`, replace the access token with `data.accessToken`, then retry the original request. The `reauthExp` claim lasts `STEP_UP_TTL_SECONDS` (default 5 minutes). Password change still requires the current password on its own endpoint. API keys skip step-up.

## ORG_ADMIN

1. Sign in, open organization, rename (step-up).
2. Create MANAGER and EMPLOYEE, change a role, deactivate (step-up on role change and deactivate).
3. Create an API key, copy it once, revoke it (step-up).
4. Open audit log and filter by actor.

## MANAGER

1. Create product, customer, supplier.
2. Draft PO → ORDERED → RECEIVED (step-up on RECEIVED; stock increases).
3. Draft SO → CONFIRMED → FULFILLED (step-up on FULFILLED; stock decreases).
4. Open dashboard and low-stock list (cached ~45s, invalidated on writes).

## EMPLOYEE

1. List products, customers, suppliers, orders (read).
2. Open low stock.
3. Open own employee record; cannot manage users or keys.

## Integrator

1. Admin creates a scoped key in the SPA or Postman.
2. Call `GET /api/v1/products` with `X-API-Key`.
3. Confirm 403 on `/api/v1/users` and `/api/v1/api-keys`.
