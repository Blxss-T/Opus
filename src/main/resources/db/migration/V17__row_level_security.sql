-- Tenant isolation at the database. FORCE RLS so even the table owner is subject
-- to policies. The API sets app.organization_id via SET LOCAL on each transaction.
-- An empty setting yields no rows (fail closed), except the lookup keys below.

CREATE OR REPLACE FUNCTION identity.current_org_id()
RETURNS uuid
LANGUAGE sql
STABLE
AS $$
    SELECT NULLIF(current_setting('app.organization_id', true), '')::uuid
$$;

CREATE OR REPLACE FUNCTION identity.apply_org_rls(schema_name text, table_name text)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY', schema_name, table_name);
    EXECUTE format('ALTER TABLE %I.%I FORCE ROW LEVEL SECURITY', schema_name, table_name);
    EXECUTE format(
        'CREATE POLICY %I ON %I.%I
           USING (organization_id = identity.current_org_id())
           WITH CHECK (organization_id = identity.current_org_id())',
        table_name || '_tenant',
        schema_name,
        table_name
    );
END;
$$;

ALTER TABLE identity.organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.organizations FORCE ROW LEVEL SECURITY;
CREATE POLICY organizations_tenant ON identity.organizations
    USING (id = identity.current_org_id())
    WITH CHECK (id = identity.current_org_id());

SELECT identity.apply_org_rls('identity', 'users');
DROP POLICY users_tenant ON identity.users;
CREATE POLICY users_tenant ON identity.users
    USING (
        organization_id = identity.current_org_id()
        OR email = NULLIF(current_setting('app.login_email', true), '')
        OR EXISTS (
            SELECT 1 FROM identity.refresh_tokens rt
            WHERE rt.user_id = users.id
              AND rt.token_hash = NULLIF(current_setting('app.refresh_token_hash', true), '')
        )
    )
    WITH CHECK (organization_id = identity.current_org_id());

SELECT identity.apply_org_rls('identity', 'employees');
SELECT identity.apply_org_rls('catalog', 'products');
SELECT identity.apply_org_rls('catalog', 'inventory_movements');
SELECT identity.apply_org_rls('purchasing', 'suppliers');
SELECT identity.apply_org_rls('purchasing', 'purchase_orders');
SELECT identity.apply_org_rls('sales', 'customers');
SELECT identity.apply_org_rls('sales', 'sales_orders');
SELECT identity.apply_org_rls('ops', 'api_keys');
DROP POLICY api_keys_tenant ON ops.api_keys;
CREATE POLICY api_keys_tenant ON ops.api_keys
    USING (
        organization_id = identity.current_org_id()
        OR key_hash = NULLIF(current_setting('app.api_key_hash', true), '')
    )
    WITH CHECK (organization_id = identity.current_org_id());

SELECT identity.apply_org_rls('ops', 'audit_events');

ALTER TABLE purchasing.purchase_order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE purchasing.purchase_order_items FORCE ROW LEVEL SECURITY;
CREATE POLICY purchase_order_items_tenant ON purchasing.purchase_order_items
    USING (
        EXISTS (
            SELECT 1 FROM purchasing.purchase_orders po
            WHERE po.id = purchase_order_id AND po.organization_id = identity.current_org_id()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM purchasing.purchase_orders po
            WHERE po.id = purchase_order_id AND po.organization_id = identity.current_org_id()
        )
    );

ALTER TABLE sales.sales_order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales.sales_order_items FORCE ROW LEVEL SECURITY;
CREATE POLICY sales_order_items_tenant ON sales.sales_order_items
    USING (
        EXISTS (
            SELECT 1 FROM sales.sales_orders so
            WHERE so.id = sales_order_id AND so.organization_id = identity.current_org_id()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM sales.sales_orders so
            WHERE so.id = sales_order_id AND so.organization_id = identity.current_org_id()
        )
    );

ALTER TABLE identity.refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.refresh_tokens FORCE ROW LEVEL SECURITY;
CREATE POLICY refresh_tokens_tenant ON identity.refresh_tokens
    USING (
        EXISTS (
            SELECT 1 FROM identity.users u
            WHERE u.id = user_id AND u.organization_id = identity.current_org_id()
        )
        OR token_hash = NULLIF(current_setting('app.refresh_token_hash', true), '')
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM identity.users u
            WHERE u.id = user_id AND u.organization_id = identity.current_org_id()
        )
    );

CREATE OR REPLACE FUNCTION identity.cleanup_expired_sessions()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = identity, pg_temp
AS $$
BEGIN
    EXECUTE 'SET LOCAL row_security = off';
    DELETE FROM identity.refresh_tokens WHERE revoked = true OR expires_at < now();
    DELETE FROM identity.otp_codes WHERE used = true OR expires_at < now();
END;
$$;

CREATE OR REPLACE FUNCTION reporting.refresh_sales_mvs()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = reporting, pg_temp
AS $$
BEGIN
    EXECUTE 'SET LOCAL row_security = off';
    REFRESH MATERIALIZED VIEW CONCURRENTLY reporting.mv_daily_sales;
    REFRESH MATERIALIZED VIEW CONCURRENTLY reporting.mv_monthly_sales;
EXCEPTION
    WHEN undefined_table THEN
        NULL;
    WHEN feature_not_supported THEN
        REFRESH MATERIALIZED VIEW reporting.mv_daily_sales;
        REFRESH MATERIALIZED VIEW reporting.mv_monthly_sales;
END;
$$;

GRANT EXECUTE ON FUNCTION reporting.refresh_sales_mvs() TO PUBLIC;
GRANT EXECUTE ON FUNCTION identity.cleanup_expired_sessions() TO PUBLIC;
