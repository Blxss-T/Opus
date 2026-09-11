CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    user_id UUID UNIQUE REFERENCES users (id) ON DELETE SET NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    job_title VARCHAR(150),
    department VARCHAR(150),
    employment_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    hired_at DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_employees_org_email UNIQUE (organization_id, email),
    CONSTRAINT ck_employees_status CHECK (
        employment_status IN ('ACTIVE', 'ON_LEAVE', 'TERMINATED')
    )
);

CREATE INDEX idx_employees_organization_id ON employees (organization_id);
CREATE INDEX idx_employees_user_id ON employees (user_id);
