-- =============================================================================
-- AUTH MODULE TABLES
-- =============================================================================
-- Migration: V021__create_auth_module_tables.sql
-- Module: Auth (com.platform.auth)
-- Purpose: Creates tables for the auth module with opaque token support
--
-- Constitutional Compliance:
-- - Opaque tokens stored with proper security (no JWT)
-- - Account lockout protection
-- - Soft delete support
-- - Audit trail integration
-- =============================================================================

-- =============================================================================
-- AUTH_USERS TABLE
-- =============================================================================
-- Main authentication table for the auth module
-- Note: This is separate from the 'users' table which may contain additional profile data

CREATE TABLE IF NOT EXISTS auth_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT chk_auth_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING_VERIFICATION')),
    CONSTRAINT chk_auth_users_failed_attempts CHECK (failed_login_attempts >= 0)
);

-- Indexes for auth_users
CREATE INDEX idx_auth_users_email ON auth_users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_auth_users_status ON auth_users(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_auth_users_locked_until ON auth_users(locked_until) WHERE locked_until IS NOT NULL;
CREATE INDEX idx_auth_users_created_at ON auth_users(created_at);

-- Comments for documentation
COMMENT ON TABLE auth_users IS 'Auth module user accounts with authentication credentials (opaque tokens, no JWT)';
COMMENT ON COLUMN auth_users.password_hash IS 'BCrypt password hash with strength 12';
COMMENT ON COLUMN auth_users.status IS 'Account status: ACTIVE, LOCKED, DISABLED, PENDING_VERIFICATION';
COMMENT ON COLUMN auth_users.failed_login_attempts IS 'Count of consecutive failed login attempts';
COMMENT ON COLUMN auth_users.locked_until IS 'Timestamp when account lockout expires (30 minutes default)';
COMMENT ON COLUMN auth_users.deleted_at IS 'Soft delete timestamp for GDPR compliance';

-- =============================================================================
-- OPAQUE_TOKENS TABLE
-- =============================================================================
-- Redis-backed opaque tokens are the primary storage, but this table provides:
-- - Backup/recovery capability
-- - Audit trail of token creation
-- - Ability to revoke all tokens for a user

CREATE TABLE IF NOT EXISTS opaque_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 hash of the token
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(45), -- IPv4 or IPv6
    user_agent VARCHAR(500),

    -- Constraints
    CONSTRAINT chk_opaque_tokens_expiry CHECK (expires_at > created_at)
);

-- Indexes for opaque_tokens
CREATE INDEX idx_opaque_tokens_user_id ON opaque_tokens(user_id) WHERE NOT revoked;
CREATE INDEX idx_opaque_tokens_token_hash ON opaque_tokens(token_hash) WHERE NOT revoked;
CREATE INDEX idx_opaque_tokens_expires_at ON opaque_tokens(expires_at) WHERE NOT revoked;
CREATE INDEX idx_opaque_tokens_revoked ON opaque_tokens(revoked, user_id);

-- Comments for documentation
COMMENT ON TABLE opaque_tokens IS 'Opaque authentication tokens (Redis primary, database backup). Constitutional requirement: NO JWT';
COMMENT ON COLUMN opaque_tokens.token_hash IS 'SHA-256 hash of the opaque token for lookup';
COMMENT ON COLUMN opaque_tokens.expires_at IS 'Token expiration (24h with sliding window)';
COMMENT ON COLUMN opaque_tokens.last_used_at IS 'Last time token was validated (for sliding expiration)';

-- =============================================================================
-- AUTH_LOGIN_ATTEMPTS TABLE
-- =============================================================================
-- Tracks all login attempts for security monitoring and audit

CREATE TABLE IF NOT EXISTS auth_login_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth_users(id) ON DELETE SET NULL,
    email VARCHAR(255) NOT NULL,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Additional audit fields
    correlation_id VARCHAR(36), -- For tracing through logs
    metadata JSONB DEFAULT '{}'
);

-- Indexes for auth_login_attempts
CREATE INDEX idx_auth_login_attempts_user_id ON auth_login_attempts(user_id, attempted_at DESC);
CREATE INDEX idx_auth_login_attempts_email ON auth_login_attempts(email, attempted_at DESC);
CREATE INDEX idx_auth_login_attempts_ip ON auth_login_attempts(ip_address, attempted_at DESC);
CREATE INDEX idx_auth_login_attempts_success ON auth_login_attempts(success, attempted_at DESC);
CREATE INDEX idx_auth_login_attempts_correlation ON auth_login_attempts(correlation_id);

-- Comments for documentation
COMMENT ON TABLE auth_login_attempts IS 'Audit trail of all login attempts for security monitoring';
COMMENT ON COLUMN auth_login_attempts.correlation_id IS 'Correlation ID for distributed tracing';

-- =============================================================================
-- DATA RETENTION POLICY
-- =============================================================================
-- Automatic cleanup function for expired tokens

CREATE OR REPLACE FUNCTION cleanup_expired_opaque_tokens()
RETURNS void AS $$
BEGIN
    -- Delete tokens that expired more than 30 days ago
    DELETE FROM opaque_tokens
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '30 days'
       OR (revoked = true AND revoked_at < CURRENT_TIMESTAMP - INTERVAL '30 days');
END;
$$ LANGUAGE plpgsql;

-- Comments for retention policy
COMMENT ON FUNCTION cleanup_expired_opaque_tokens() IS 'Cleanup expired opaque tokens (run daily via scheduled job)';
COMMENT ON TABLE auth_login_attempts IS 'Retention: 1 year for security monitoring, then archive/delete per GDPR';

-- =============================================================================
-- INITIAL DATA
-- =============================================================================
-- No default users - must be created via registration or admin API
