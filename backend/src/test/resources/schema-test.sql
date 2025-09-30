-- Test database schema for H2
-- This file ensures proper table setup for audit tests

-- Audit Events table
CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    organization_id UUID NOT NULL,
    actor_id UUID,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    details TEXT,
    outcome VARCHAR(20) DEFAULT 'SUCCESS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Audit Export Requests table
CREATE TABLE IF NOT EXISTS audit_export_requests (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    format VARCHAR(10) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    date_from TIMESTAMP,
    date_to TIMESTAMP,
    search_term VARCHAR(1000),
    action_types TEXT,
    total_records BIGINT,
    processed_records BIGINT,
    file_path VARCHAR(500),
    file_size_bytes BIGINT,
    download_token VARCHAR(255),
    download_expires_at TIMESTAMP,
    download_count INTEGER DEFAULT 0,
    max_downloads INTEGER DEFAULT 5,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Indexes for better test performance
CREATE INDEX IF NOT EXISTS idx_audit_events_org_id ON audit_events(organization_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_export_user_id ON audit_export_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_export_status ON audit_export_requests(status);