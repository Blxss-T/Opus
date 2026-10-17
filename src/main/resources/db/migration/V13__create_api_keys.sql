-- API keys let other applications call the API on behalf of an organization.
-- Only a SHA-256 hash of the key is stored; the raw key is shown once at
-- creation. key_prefix is kept so a key can be recognised in listings.

CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    scopes VARCHAR(500) NOT NULL,
    created_by UUID NOT NULL,
    created_by_email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NULL,
    revoked_at TIMESTAMP WITH TIME ZONE NULL,
    last_used_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_api_keys_hash UNIQUE (key_hash)
);

CREATE INDEX idx_api_keys_organization ON api_keys (organization_id, created_at DESC);
