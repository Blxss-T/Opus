-- OpsFlow Migration V8: Suppliers and Purchase Orders

CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    tax_number VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_suppliers_org_name UNIQUE (organization_id, name)
);

CREATE INDEX idx_suppliers_organization_id ON suppliers (organization_id);

CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    supplier_id UUID NOT NULL REFERENCES suppliers (id) ON DELETE RESTRICT,
    po_number VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    total_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    expected_delivery_date DATE,
    created_by UUID NOT NULL,
    created_by_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_po_org_number UNIQUE (organization_id, po_number),
    CONSTRAINT ck_po_total_amount CHECK (total_amount >= 0)
);

CREATE INDEX idx_po_organization_id ON purchase_orders (organization_id);
CREATE INDEX idx_po_supplier_id ON purchase_orders (supplier_id);
CREATE INDEX idx_po_status ON purchase_orders (organization_id, status);

CREATE TABLE purchase_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL,
    unit_cost NUMERIC(12, 2) NOT NULL,
    subtotal NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_poi_quantity CHECK (quantity > 0),
    CONSTRAINT ck_poi_unit_cost CHECK (unit_cost >= 0),
    CONSTRAINT ck_poi_subtotal CHECK (subtotal >= 0)
);

CREATE INDEX idx_poi_purchase_order_id ON purchase_order_items (purchase_order_id);
CREATE INDEX idx_poi_product_id ON purchase_order_items (product_id);
