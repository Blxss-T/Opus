# Opus API integration guide

For developers connecting another application to Opus. The full endpoint reference is the Swagger UI at `/swagger-ui.html` (machine-readable spec at `/v3/api-docs`); this guide covers what you need around it.

Base URL: `https://<your-opus-host>/api/v1` (locally `http://localhost:8080/api/v1`).

## 1. Get an API key

An organization's `ORG_ADMIN` creates the key while signed in:

```bash
curl -X POST "$BASE/api-keys" \
  -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Storefront", "scopes": ["products:read", "sales-orders:write"]}'
```

The response contains the key in `data.key` (it starts with `opk_`). It is shown once and cannot be retrieved later, so store it in your secret manager. Add `"expiresAt": "2027-01-01T00:00:00Z"` for a key that expires. `GET /api-keys/scopes` lists every scope.

## 2. Authenticate

Send the key on every request:

```bash
curl "$BASE/products" -H "X-API-Key: $OPUS_API_KEY"
```

A key always acts inside the organization that created it; there is no organization parameter.

## 3. Scopes

| Scope | Allows |
| --- | --- |
| `products:read` / `products:write` | Catalog, stock levels, stock adjustments and movement history |
| `customers:read` / `customers:write` | Customers |
| `suppliers:read` / `suppliers:write` | Suppliers |
| `purchase-orders:read` / `purchase-orders:write` | Purchase orders and their status changes |
| `sales-orders:read` / `sales-orders:write` | Sales orders and their status changes |
| `employees:read` | Employee directory |
| `reports:read` | Dashboard and reports |

`GET` requests need the `:read` scope; `POST`, `PUT`, `PATCH` and `DELETE` need `:write`. A `:write` scope also allows reads of the same resource. User accounts, API keys and the `/auth` endpoints are not available to API keys.

## 4. Responses

Every response is a JSON envelope:

```json
{ "success": true, "data": { }, "timestamp": "2026-01-01T12:00:00Z" }
```

```json
{
  "success": false,
  "error": {
    "status": 422,
    "error": "ERR_422",
    "message": "Insufficient inventory: current stock is 3, requested adjustment delta is -5",
    "path": "/api/v1/sales-orders/…/status",
    "details": [],
    "timestamp": "2026-01-01T12:00:00Z"
  },
  "timestamp": "2026-01-01T12:00:00Z"
}
```

Branch on the HTTP status or `error.error`, not on `error.message`.

| Status | Code | Meaning | What to do |
| --- | --- | --- | --- |
| 400 | `ERR_400` | Invalid or malformed request; `error.details` lists field errors | Fix the request |
| 401 | `ERR_401` | Key missing, invalid, expired or revoked | Check the key; do not retry unchanged |
| 403 | `ERR_403` | Key lacks the scope, or the endpoint is not open to keys | Ask the admin for a key with the scope named in the message |
| 404 | `ERR_404` | No such record in your organization | |
| 409 | `ERR_409` | The record was changed by another request at the same time | Re-read and retry |
| 422 | `ERR_422` | A business rule was violated (e.g. not enough stock, invalid status change) | Fix the request |
| 429 | `ERR_429` | Rate limit exceeded | Wait for the `Retry-After` header (seconds), then retry |
| 500 / 503 | `ERR_500` / `ERR_503` | Server-side problem | Retry with backoff |

## 5. Lists

List endpoints take `page` (0-based), `size` (default 20, maximum 100) and `sort` (for example `sort=createdAt,desc`), plus the filters shown in Swagger.

```json
{
  "success": true,
  "data": {
    "content": [ ],
    "page": { "size": 20, "number": 0, "totalElements": 57, "totalPages": 3 }
  }
}
```

Read pages until `page.number + 1 == page.totalPages`.

## 6. Creating orders safely

Send an `Idempotency-Key` header (any unique string up to 100 characters, such as a UUID) when creating purchase or sales orders:

```bash
curl -X POST "$BASE/sales-orders" \
  -H "X-API-Key: $OPUS_API_KEY" \
  -H "Idempotency-Key: 7c1f0c1e-2b0e-4a57-9f0b-6f1d1c0a9a11" \
  -H "Content-Type: application/json" \
  -d '{"customerId": "…", "items": [{"productId": "…", "quantity": 2, "unitPrice": 18.00}]}'
```

If the request times out or the connection drops, send it again with the same key. You get the order that was created the first time, with status `200` and the header `Idempotency-Replayed: true`, instead of a second order. Use a new key for each distinct order. The first request's body wins; a replay's body is not compared.

## 7. Order lifecycles

- Sales order: `DRAFT` → `CONFIRMED` → `FULFILLED`, or `CANCELLED` before fulfilment. Fulfilling removes the sold quantities from stock and fails with `ERR_422` if there is not enough.
- Purchase order: `DRAFT` → `ORDERED` → `RECEIVED`, or `CANCELLED` before receipt. Receiving adds the quantities to stock.

Change status with `PATCH /sales-orders/{id}/status` or `/purchase-orders/{id}/status` and a body of `{"status": "CONFIRMED"}`. While an order is `DRAFT` its contents can be replaced with `PUT /sales-orders/{id}` or `/purchase-orders/{id}`.

## 8. Rate limits

Each key may make 120 requests per minute across all endpoints (the operator can change this). Beyond that the API returns `429` with a `Retry-After` header.

## 9. Good practice

- Give each integration its own key with only the scopes it needs, so one can be revoked without affecting the others.
- Changes made with a key appear in the organization's audit log as `api-key:<name>`; pick a name that identifies the integration.
- If a key may have leaked, ask the admin to revoke it (`DELETE /api-keys/{id}`) and create a new one. Revocation takes effect immediately.
