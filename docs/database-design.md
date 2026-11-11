# Opus — Database Design

**Database:** PostgreSQL, Flyway `V1`–`V18`  
**Tenancy:** shared database; `organization_id` on org-owned tables; FORCE RLS from `V17`  
**Schemas:** `identity`, `catalog`, `purchasing`, `sales`, `ops`, `reporting` (`V16`)  
**IDs:** UUID (`gen_random_uuid()` / JPA UUID)

Use this for entity screens, forms, and relationship diagrams in Figma.

---

## 1. ER overview

```text
identity.organizations
    |
    +-- identity.users (1 ORG_ADMIN via partial unique index)
    |     +-- identity.refresh_tokens
    |     +-- identity.employees.user_id (optional unique)
    +-- identity.employees
    +-- sales.customers
    +-- purchasing.suppliers
    |     +-- purchasing.purchase_orders --> purchase_order_items --> catalog.products
    +-- catalog.products --> catalog.inventory_movements
    +-- sales.sales_orders --> sales.sales_order_items --> catalog.products
    |              \--> sales.customers
    +-- ops.api_keys
    +-- ops.audit_events

identity.otp_codes     (keyed by email, not org — pre-login; no tenant RLS)
reporting.v_low_stock, v_open_orders
reporting.mv_daily_sales, mv_monthly_sales
```

---

## 2. Conventions

- `id UUID PK`
- `created_at` / `updated_at` timestamptz (movements historically had only `created_at`; V12 adds `updated_at` on movements)
- Soft delete: `active boolean` on organizations, users, products, customers, suppliers
- Employees use `employment_status` instead of `active`
- Money: `NUMERIC(12,2)` lines, `NUMERIC(14,2)` order totals
- Emails stored lower-case in application code
- Org isolation: services filter by organization id from the token; PostgreSQL FORCE RLS uses `app.organization_id` (empty GUC ⇒ no rows). Login/API-key/refresh lookups use extra GUCs, not a bypass of tenant data.

---

## 3. Tables (fields for UI)

### organizations

| Column | Type | UI |
| --- | --- | --- |
| id | UUID | |
| name | varchar 255 | Display / rename (ORG_ADMIN) |
| slug | varchar 100 unique | Read-only identifier |
| active | boolean | |

### users

| Column | Type | UI |
| --- | --- | --- |
| id | UUID | |
| organization_id | FK | Hidden |
| email | unique globally | Login + user admin |
| password_hash | varchar | Never show |
| first_name, last_name | varchar 100 | |
| role | ORG_ADMIN / MANAGER / EMPLOYEE | Role badge; cannot assign second ORG_ADMIN |
| active | boolean | Deactivate |
| email_verified_at | timestamptz null | Verified badge |
| password_changed_at | timestamptz null | Hidden |

### refresh_tokens

Internal. Hash only, expiry, revoked. No UI except “log out all sessions” via password change / logout.

### otp_codes

| Column | Notes |
| --- | --- |
| email, type | EMAIL_VERIFICATION, LOGIN_2FA, PASSWORD_RESET |
| code_hash | Never display the hash; 6-digit code is emailed |
| expires_at, used, attempt_count | 10 min, lock after 5 tries |

`LOGIN_2FA` is an unused type (no 2FA login UI/API flow).

### employees

HR record, **not** a second login. Optional `user_id` unique.

| Column | UI |
| --- | --- |
| first_name, last_name, email | Unique email per org |
| job_title, department | |
| employment_status | ACTIVE, ON_LEAVE, TERMINATED |
| hired_at | date |
| user_id | Link to login account |

### customers

| Column | UI |
| --- | --- |
| name | required |
| email | required, unique per org |
| phone, address, notes | optional |
| active | soft delete |

List filter: `q` on name/email.

### suppliers

| Column | UI |
| --- | --- |
| name | unique per org |
| contact_person, email, phone, address, tax_number | |
| active | soft delete |

List filter: `q`.

### products

| Column | UI |
| --- | --- |
| sku | unique per org, uppercase in API |
| name, description, category | |
| unit_price, cost_price | money, >= 0 |
| stock_quantity | integer >= 0, not edited as a raw field except via adjust-stock / orders |
| reorder_level | default 10; dashboard “low stock” when stock <= this |
| version | optimistic lock; show conflict toast on stale edit |
| active | archive |

List filters: `q`, `category`.

### inventory_movements

Append-only history.

| Column | UI |
| --- | --- |
| quantity_delta, previous_quantity, new_quantity | |
| movement_type | INBOUND, OUTBOUND, ADJUSTMENT, RETURN, DAMAGE |
| reason | |
| performed_by_email | |

Filter: `type`. Newest first.

### purchase_orders + purchase_order_items

| Header | UI |
| --- | --- |
| po_number | generated `PO-YYYY-####` |
| supplier_id | picker |
| status | DRAFT, ORDERED, RECEIVED, CANCELLED |
| total_amount | sum of lines |
| notes, expected_delivery_date | |
| created_by_email | |
| idempotency_key | header on create, not a form field |

Lines: product, quantity > 0, unit_cost, subtotal. Replace whole line set only while DRAFT.

### sales_orders + sales_order_items

Same pattern: `SO-YYYY-####`, customer picker, statuses DRAFT, CONFIRMED, FULFILLED, CANCELLED. Lines use `unit_price`. Fulfilment decrements stock.

### api_keys

| Column | UI |
| --- | --- |
| name | |
| key_prefix | list display (`opk_…`) |
| scopes | chips; raw key **once** at create |
| expires_at, revoked_at, last_used_at | |

### audit_events

Read-only table for ORG_ADMIN: actor, actor_type USER/API_KEY, method, path, resource_type, resource_id, status, ip, time. Filters: `actor`, `actorType`, `resourceType`, `resourceId`.

---

## 4. Constraints that affect forms

| Rule | UX |
| --- | --- |
| Unique user email globally | “Email already registered” even across orgs |
| Unique customer email per org | 422 on duplicate |
| Unique supplier name per org | |
| Unique product SKU per org | |
| Unique one ORG_ADMIN | Hide “promote to admin” if one exists |
| stock_quantity >= 0 | Disable fulfil / outbound that would go negative; show 422 message |
| Order status machine | Disable illegal transitions in the UI; still handle 422 |
| Soft delete | Lists hide inactive; GET by id 404s inactive products/customers/suppliers |

---

## 5. Suggested UI entities (screens)

1. Auth: login, register (org name + admin), forgot/reset password, OTP  
2. Shell: role nav, org name, user menu (change password, logout)  
3. Dashboard: KPI cards, low-stock table, optional charts (API already has category/daily/monthly)  
4. Customers / suppliers / products: list (search + pager) → detail → create/edit  
5. Product detail: stock adjust + movements table  
6. Purchase orders: list by status → create lines → status buttons  
7. Sales orders: same with customer + stock warnings  
8. Employees / users: directory vs login accounts  
9. Organization: name  
10. API keys: create (copy secret once), list, revoke  
11. Audit log: filterable table  

---

## 6. Not in the database (do not design as live data)

No tables for: invoices, payments, files/attachments, notifications inbox, subscriptions/billing, organization settings JSON, warehouses/multi-location stock, tax engines.

Redis is **not** in PostgreSQL; it holds rate-limit counters and dashboard keys only. Reporting views/MVs are in `reporting` (`V18`). Never edit V1–V15 in place; add `V19+`.
