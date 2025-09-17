-- Performance indexes for frequently queried fields
-- Based on code review recommendations

-- User table indexes
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_organization_id ON users(organization_id);
CREATE INDEX idx_user_email_verification_token ON users(email_verification_token) WHERE email_verification_token IS NOT NULL;
CREATE INDEX idx_user_password_reset_token ON users(password_reset_token) WHERE password_reset_token IS NOT NULL;
CREATE INDEX idx_user_created_at ON users(created_at);
CREATE INDEX idx_user_authentication_method ON users(authentication_method);

-- Organization table indexes
CREATE INDEX idx_organization_name ON organizations(name);
CREATE INDEX idx_organization_status ON organizations(status);
CREATE INDEX idx_organization_created_at ON organizations(created_at);

-- Authentication attempts table indexes
CREATE INDEX idx_auth_attempt_email ON authentication_attempts(email);
CREATE INDEX idx_auth_attempt_attempted_at ON authentication_attempts(attempted_at);
CREATE INDEX idx_auth_attempt_successful ON authentication_attempts(successful);
CREATE INDEX idx_auth_attempt_email_time ON authentication_attempts(email, attempted_at DESC);

-- Composite indexes for common queries
CREATE INDEX idx_user_email_verified ON users(email, email_verified) WHERE authentication_method = 'PASSWORD';
CREATE INDEX idx_user_failed_attempts ON users(email, failed_login_attempts) WHERE failed_login_attempts > 0;
CREATE INDEX idx_user_account_locked ON users(email, account_locked_until) WHERE account_locked_until IS NOT NULL;