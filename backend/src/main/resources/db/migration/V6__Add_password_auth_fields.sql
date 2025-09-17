-- Add password authentication fields to users table
-- Migration for password authentication feature

-- Add password-related columns to users table
ALTER TABLE users
ADD COLUMN password_hash VARCHAR(255),
ADD COLUMN password_reset_token VARCHAR(255),
ADD COLUMN password_reset_expires_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN email_verified BOOLEAN DEFAULT false,
ADD COLUMN email_verification_token VARCHAR(255),
ADD COLUMN email_verification_expires_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN failed_login_attempts INTEGER DEFAULT 0,
ADD COLUMN lockout_expires_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN password_changed_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN authentication_methods JSONB DEFAULT '["OAUTH2"]';

-- Add indexes for performance on password authentication queries
CREATE INDEX idx_users_password_reset_token ON users(password_reset_token)
WHERE password_reset_token IS NOT NULL;

CREATE INDEX idx_users_email_verification_token ON users(email_verification_token)
WHERE email_verification_token IS NOT NULL;

CREATE INDEX idx_users_lockout_expires ON users(lockout_expires_at)
WHERE lockout_expires_at IS NOT NULL;

CREATE INDEX idx_users_email_verified ON users(email_verified);

CREATE INDEX idx_users_failed_attempts ON users(failed_login_attempts)
WHERE failed_login_attempts > 0;

-- For existing OAuth users, set email_verified to true (they already verified via OAuth)
UPDATE users
SET email_verified = true
WHERE provider IS NOT NULL AND provider != 'password';

-- Add comments for documentation
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash of user password (null for OAuth-only users)';
COMMENT ON COLUMN users.password_reset_token IS 'Secure token for password reset (single-use, expires after 24h)';
COMMENT ON COLUMN users.email_verified IS 'Whether email address has been verified (required for password auth)';
COMMENT ON COLUMN users.failed_login_attempts IS 'Number of consecutive failed login attempts (resets on success)';
COMMENT ON COLUMN users.lockout_expires_at IS 'When account lockout expires (exponential backoff)';
COMMENT ON COLUMN users.authentication_methods IS 'JSON array of enabled auth methods: ["OAUTH2"], ["PASSWORD"], or ["OAUTH2", "PASSWORD"]';