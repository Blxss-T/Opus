CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    sku VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    cost_price NUMERIC(12, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    reorder_level INTEGER NOT NULL DEFAULT 10,
    active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_products_org_sku UNIQUE (organization_id, sku),
    CONSTRAINT ck_products_stock_quantity CHECK (stock_quantity >= 0),
    CONSTRAINT ck_products_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_products_cost_price CHECK (cost_price >= 0),
    CONSTRAINT ck_products_reorder_level CHECK (reorder_level >= 0)
);

CREATE INDEX idx_products_organization_id ON products (organization_id);
CREATE INDEX idx_products_sku ON products (organization_id, sku);
CREATE INDEX idx_products_category ON products (organization_id, category);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity_delta INTEGER NOT NULL,
    previous_quantity INTEGER NOT NULL,
    new_quantity INTEGER NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    performed_by UUID NOT NULL,
    performed_by_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inv_movements_lookup ON inventory_movements (organization_id, product_id, created_at DESC);
