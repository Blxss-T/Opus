CREATE INDEX IF NOT EXISTS idx_products_org_low_stock
    ON catalog.products (organization_id, stock_quantity)
    WHERE active = true;

CREATE INDEX IF NOT EXISTS idx_sales_orders_org_status_created
    ON sales.sales_orders (organization_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_purchase_orders_org_status_created
    ON purchasing.purchase_orders (organization_id, status, created_at DESC);

CREATE OR REPLACE VIEW reporting.v_low_stock AS
SELECT
    p.organization_id,
    p.id AS product_id,
    p.sku,
    p.name,
    p.category,
    p.stock_quantity,
    p.reorder_level
FROM catalog.products p
WHERE p.active = true
  AND p.stock_quantity <= p.reorder_level;

CREATE OR REPLACE VIEW reporting.v_open_orders AS
SELECT
    o.organization_id,
    COUNT(*) FILTER (
        WHERE o.kind = 'PO' AND o.status IN ('DRAFT', 'ORDERED')
    ) AS open_purchase_orders,
    COUNT(*) FILTER (
        WHERE o.kind = 'SO' AND o.status IN ('DRAFT', 'CONFIRMED')
    ) AS open_sales_orders
FROM (
    SELECT organization_id, status, 'PO'::text AS kind FROM purchasing.purchase_orders
    UNION ALL
    SELECT organization_id, status, 'SO'::text AS kind FROM sales.sales_orders
) o
GROUP BY o.organization_id;

CREATE MATERIALIZED VIEW reporting.mv_daily_sales AS
SELECT
    organization_id,
    (created_at AT TIME ZONE 'UTC')::date AS sale_date,
    COALESCE(SUM(total_amount), 0) AS total_sales
FROM sales.sales_orders
WHERE status = 'FULFILLED'
GROUP BY organization_id, (created_at AT TIME ZONE 'UTC')::date
WITH NO DATA;

CREATE UNIQUE INDEX uk_mv_daily_sales_org_date
    ON reporting.mv_daily_sales (organization_id, sale_date);

CREATE MATERIALIZED VIEW reporting.mv_monthly_sales AS
SELECT
    organization_id,
    date_trunc('month', created_at AT TIME ZONE 'UTC')::date AS sale_month,
    COALESCE(SUM(total_amount), 0) AS total_sales
FROM sales.sales_orders
WHERE status = 'FULFILLED'
GROUP BY organization_id, date_trunc('month', created_at AT TIME ZONE 'UTC')
WITH NO DATA;

CREATE UNIQUE INDEX uk_mv_monthly_sales_org_month
    ON reporting.mv_monthly_sales (organization_id, sale_month);

GRANT SELECT ON reporting.v_low_stock TO PUBLIC;
GRANT SELECT ON reporting.v_open_orders TO PUBLIC;
GRANT SELECT ON reporting.mv_daily_sales TO PUBLIC;
GRANT SELECT ON reporting.mv_monthly_sales TO PUBLIC;

-- Populate once; later refreshes use reporting.refresh_sales_mvs() CONCURRENTLY.
REFRESH MATERIALIZED VIEW reporting.mv_daily_sales;
REFRESH MATERIALIZED VIEW reporting.mv_monthly_sales;
