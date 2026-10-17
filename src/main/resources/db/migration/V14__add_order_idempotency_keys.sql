-- Optional client-supplied key (Idempotency-Key header) recorded on the order
-- it created, so a retried create returns that order instead of a duplicate.
-- The partial unique indexes are a backstop; the application also checks the
-- key under the per-organization lock taken for order numbering.

ALTER TABLE purchase_orders ADD COLUMN idempotency_key VARCHAR(100) NULL;
ALTER TABLE sales_orders ADD COLUMN idempotency_key VARCHAR(100) NULL;

CREATE UNIQUE INDEX uk_purchase_orders_idempotency_key
    ON purchase_orders (organization_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX uk_sales_orders_idempotency_key
    ON sales_orders (organization_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
