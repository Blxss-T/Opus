-- OTP codes for email verification and future sensitive-action proof.
-- The value stored is a SHA-256 hex digest, never the raw 6-digit code.

CREATE TABLE otp_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'EMAIL_VERIFICATION',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_codes_lookup ON otp_codes (email, type, used, created_at DESC);
