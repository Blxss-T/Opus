-- OpsFlow Migration V10: Sales Orders

CREATE TABLE sales_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    customer_id UUID NOT NULL REFERENCES customers (id) ON DELETE RESTRICT,
    so_number VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    total_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    expected_delivery_date DATE,
    created_by UUID NOT NULL,
    created_by_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_so_org_number UNIQUE (organization_id, so_number),
    CONSTRAINT ck_so_total_amount CHECK (total_amount >= 0)
);

CREATE INDEX idx_so_organization_id ON sales_orders (organization_id);
CREATE INDEX idx_so_customer_id ON sales_orders (customer_id);
CREATE INDEX idx_so_status ON sales_orders (organization_id, status);

CREATE TABLE sales_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sales_order_id UUID NOT NULL REFERENCES sales_orders (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    subtotal NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_soi_quantity CHECK (quantity > 0),
    CONSTRAINT ck_soi_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_soi_subtotal CHECK (subtotal >= 0)
);

CREATE INDEX idx_soi_sales_order_id ON sales_order_items (sales_order_id);
CREATE INDEX idx_soi_product_id ON sales_order_items (product_id);
