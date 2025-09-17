-- Audit module tables
-- Audit events table for compliance and security logging
CREATE TABLE audit_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    organization_id UUID,
    user_id UUID,
    user_email VARCHAR(255),
    ip_address VARCHAR(45), -- IPv6 compatible
    user_agent TEXT,
    description TEXT,
    event_data TEXT, -- JSON data
    correlation_id VARCHAR(255),
    severity VARCHAR(20) NOT NULL DEFAULT 'LOW',
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    module VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(255),
    sensitive_data BOOLEAN NOT NULL DEFAULT false,
    retention_expiry TIMESTAMP NOT NULL,
    FOREIGN KEY (organization_id) REFERENCES organizations(organization_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Comprehensive indexes for audit queries
CREATE INDEX idx_audit_organization_timestamp ON audit_events(organization_id, timestamp DESC);
CREATE INDEX idx_audit_user_timestamp ON audit_events(user_id, timestamp DESC);
CREATE INDEX idx_audit_correlation ON audit_events(correlation_id);
CREATE INDEX idx_audit_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_module_action ON audit_events(module, action);
CREATE INDEX idx_audit_severity ON audit_events(severity);
CREATE INDEX idx_audit_timestamp ON audit_events(timestamp DESC);
CREATE INDEX idx_audit_retention_expiry ON audit_events(retention_expiry);
CREATE INDEX idx_audit_sensitive_data ON audit_events(sensitive_data);

-- Partial indexes for performance
CREATE INDEX idx_audit_high_severity ON audit_events(timestamp DESC) WHERE severity IN ('HIGH', 'CRITICAL');
CREATE INDEX idx_audit_sensitive ON audit_events(timestamp DESC) WHERE sensitive_data = true;