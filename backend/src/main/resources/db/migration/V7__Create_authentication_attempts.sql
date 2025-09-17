-- Create authentication_attempts table for security auditing
-- Tracks all authentication attempts for rate limiting and security monitoring

CREATE TABLE authentication_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    authentication_method VARCHAR(20) NOT NULL CHECK (authentication_method IN ('OAUTH2', 'PASSWORD')),
    correlation_id VARCHAR(255),
    organization_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient queries
CREATE INDEX idx_auth_attempts_username ON authentication_attempts(username);
CREATE INDEX idx_auth_attempts_ip ON authentication_attempts(ip_address);
CREATE INDEX idx_auth_attempts_timestamp ON authentication_attempts(attempted_at);
CREATE INDEX idx_auth_attempts_method ON authentication_attempts(authentication_method);
CREATE INDEX idx_auth_attempts_success ON authentication_attempts(successful);
CREATE INDEX idx_auth_attempts_org ON authentication_attempts(organization_id);

-- Composite index for rate limiting queries (username + recent attempts)
CREATE INDEX idx_auth_attempts_rate_limiting ON authentication_attempts(username, attempted_at DESC)
WHERE successful = false;

-- Composite index for IP-based monitoring
CREATE INDEX idx_auth_attempts_ip_monitoring ON authentication_attempts(ip_address, attempted_at DESC);

-- Partition by month for efficient archiving (optional for high-volume systems)
-- Note: This would require additional setup for automatic partition management
-- CREATE INDEX idx_auth_attempts_created_month ON authentication_attempts(DATE_TRUNC('month', created_at));

-- Add table comments for documentation
COMMENT ON TABLE authentication_attempts IS 'Audit log of all authentication attempts for security monitoring and rate limiting';
COMMENT ON COLUMN authentication_attempts.username IS 'Email or username attempted (may not exist in users table)';
COMMENT ON COLUMN authentication_attempts.ip_address IS 'Client IP address (IPv4 or IPv6)';
COMMENT ON COLUMN authentication_attempts.user_agent IS 'Client User-Agent header for device tracking';
COMMENT ON COLUMN authentication_attempts.successful IS 'Whether the authentication attempt succeeded';
COMMENT ON COLUMN authentication_attempts.failure_reason IS 'Reason for failure: INVALID_CREDENTIALS, ACCOUNT_LOCKED, EMAIL_NOT_VERIFIED, etc.';
COMMENT ON COLUMN authentication_attempts.authentication_method IS 'Method used: OAUTH2 or PASSWORD';
COMMENT ON COLUMN authentication_attempts.correlation_id IS 'Request correlation ID for tracing';
COMMENT ON COLUMN authentication_attempts.organization_id IS 'Organization context (if user authenticated)';

-- Grant appropriate permissions (adjust for your setup)
-- GRANT SELECT, INSERT ON authentication_attempts TO app_user;
-- GRANT SELECT ON authentication_attempts TO app_readonly;