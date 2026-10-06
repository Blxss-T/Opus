-- Refresh tokens for stateful JWT sessions. Only a SHA-256 hash of the raw
-- token is stored, so a database leak cannot be replayed as a session.

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Invalidates outstanding refresh token sessions after a password change or
-- reset. JWT access token validation enforces the same boundary.
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP WITH TIME ZONE NULL;
