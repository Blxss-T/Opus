-- One row per successful change made through the API: who (a user or an API
-- key) did what to which resource. Written by AuditInterceptor.

CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    actor_id UUID NOT NULL,
    actor VARCHAR(255) NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID NULL,
    status_code INTEGER NOT NULL,
    ip_address VARCHAR(64) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_events_org_created ON audit_events (organization_id, created_at DESC);
CREATE INDEX idx_audit_events_resource ON audit_events (organization_id, resource_type, resource_id);
