-- OAuth2 Provider Configuration table
CREATE TABLE oauth2_providers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    client_secret VARCHAR(255) NOT NULL,
    authorization_uri VARCHAR(500) NOT NULL,
    token_uri VARCHAR(500) NOT NULL,
    user_info_uri VARCHAR(500) NOT NULL,
    jwk_set_uri VARCHAR(500),
    issuer_uri VARCHAR(500),
    scopes TEXT[], -- Array of scopes
    enabled BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    redirect_uri_template VARCHAR(500) NOT NULL DEFAULT '{baseUrl}/api/v1/auth/oauth2/callback/{registrationId}',
    client_name VARCHAR(100),
    provider_details JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_oauth2_providers_name ON oauth2_providers(name);
CREATE INDEX idx_oauth2_providers_enabled ON oauth2_providers(enabled);
CREATE INDEX idx_oauth2_providers_sort_order ON oauth2_providers(sort_order);
CREATE INDEX idx_oauth2_providers_client_id ON oauth2_providers(client_id);

-- OAuth2 User Information table
CREATE TABLE oauth2_user_info (
    id BIGSERIAL PRIMARY KEY,
    provider_user_id VARCHAR(255) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    name VARCHAR(255),
    given_name VARCHAR(100),
    family_name VARCHAR(100),
    picture VARCHAR(500),
    locale VARCHAR(10),
    attributes JSONB DEFAULT '{}',
    last_updated_from_provider TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Ensure unique provider user per provider
    CONSTRAINT uk_oauth2_user_provider UNIQUE (provider_user_id, provider)
);

CREATE INDEX idx_oauth2_user_info_provider_user ON oauth2_user_info(provider_user_id, provider);
CREATE INDEX idx_oauth2_user_info_email ON oauth2_user_info(email);
CREATE INDEX idx_oauth2_user_info_provider ON oauth2_user_info(provider);
CREATE INDEX idx_oauth2_user_info_email_provider ON oauth2_user_info(email, provider);
CREATE INDEX idx_oauth2_user_info_created_at ON oauth2_user_info(created_at);
CREATE INDEX idx_oauth2_user_info_last_updated ON oauth2_user_info(last_updated_from_provider);

-- OAuth2 Session table
CREATE TABLE oauth2_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) UNIQUE NOT NULL,
    user_info_id BIGINT NOT NULL REFERENCES oauth2_user_info(id) ON DELETE CASCADE,
    provider VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    terminated_at TIMESTAMP WITH TIME ZONE,
    last_accessed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_from_ip INET,
    last_accessed_from_ip INET,
    created_from_user_agent TEXT,
    session_attributes JSONB DEFAULT '{}'
);

CREATE INDEX idx_oauth2_sessions_session_id ON oauth2_sessions(session_id);
CREATE INDEX idx_oauth2_sessions_user_info ON oauth2_sessions(user_info_id);
CREATE INDEX idx_oauth2_sessions_provider ON oauth2_sessions(provider);
CREATE INDEX idx_oauth2_sessions_active ON oauth2_sessions(is_active);
CREATE INDEX idx_oauth2_sessions_expires_at ON oauth2_sessions(expires_at);
CREATE INDEX idx_oauth2_sessions_created_at ON oauth2_sessions(created_at);
CREATE INDEX idx_oauth2_sessions_terminated_at ON oauth2_sessions(terminated_at);
CREATE INDEX idx_oauth2_sessions_created_ip ON oauth2_sessions(created_from_ip);
CREATE INDEX idx_oauth2_sessions_user_agent ON oauth2_sessions USING gin (to_tsvector('english', created_from_user_agent));

-- OAuth2 Audit Events table (partitioned by month for performance)
CREATE TABLE oauth2_audit_events (
    id BIGSERIAL,
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    provider VARCHAR(100),
    event_type VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL DEFAULT true,
    severity VARCHAR(50) NOT NULL DEFAULT 'INFO',
    ip_address INET,
    user_agent TEXT,
    correlation_id VARCHAR(255),
    duration_ms INTEGER,
    error_code VARCHAR(100),
    error_message TEXT,
    additional_data JSONB DEFAULT '{}',

    PRIMARY KEY (id, event_timestamp)
) PARTITION BY RANGE (event_timestamp);

-- Create indexes on the parent table
CREATE INDEX idx_oauth2_audit_events_user_id ON oauth2_audit_events(user_id);
CREATE INDEX idx_oauth2_audit_events_session_id ON oauth2_audit_events(session_id);
CREATE INDEX idx_oauth2_audit_events_provider ON oauth2_audit_events(provider);
CREATE INDEX idx_oauth2_audit_events_event_type ON oauth2_audit_events(event_type);
CREATE INDEX idx_oauth2_audit_events_timestamp ON oauth2_audit_events(event_timestamp);
CREATE INDEX idx_oauth2_audit_events_success ON oauth2_audit_events(success);
CREATE INDEX idx_oauth2_audit_events_severity ON oauth2_audit_events(severity);
CREATE INDEX idx_oauth2_audit_events_ip_address ON oauth2_audit_events(ip_address);
CREATE INDEX idx_oauth2_audit_events_correlation_id ON oauth2_audit_events(correlation_id);

-- Create initial partitions for OAuth2 audit events (current and next 3 months)
CREATE TABLE oauth2_audit_events_2024_09 PARTITION OF oauth2_audit_events
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');

CREATE TABLE oauth2_audit_events_2024_10 PARTITION OF oauth2_audit_events
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');

CREATE TABLE oauth2_audit_events_2024_11 PARTITION OF oauth2_audit_events
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE TABLE oauth2_audit_events_2024_12 PARTITION OF oauth2_audit_events
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

CREATE TABLE oauth2_audit_events_2025_01 PARTITION OF oauth2_audit_events
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

-- Function to automatically create monthly partitions for OAuth2 audit events
CREATE OR REPLACE FUNCTION create_oauth2_audit_monthly_partition()
RETURNS trigger AS $$
DECLARE
    partition_date date;
    partition_name text;
    start_date date;
    end_date date;
BEGIN
    partition_date := date_trunc('month', NEW.event_timestamp);
    partition_name := 'oauth2_audit_events_' || to_char(partition_date, 'YYYY_MM');
    start_date := partition_date;
    end_date := partition_date + interval '1 month';

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = partition_name) THEN
        EXECUTE format('CREATE TABLE %I PARTITION OF oauth2_audit_events FOR VALUES FROM (%L) TO (%L)',
                      partition_name, start_date, end_date);
        -- Create indexes on the new partition
        EXECUTE format('CREATE INDEX %I ON %I(user_id)',
                      partition_name || '_user_id_idx', partition_name);
        EXECUTE format('CREATE INDEX %I ON %I(session_id)',
                      partition_name || '_session_id_idx', partition_name);
        EXECUTE format('CREATE INDEX %I ON %I(event_type)',
                      partition_name || '_event_type_idx', partition_name);
        EXECUTE format('CREATE INDEX %I ON %I(ip_address)',
                      partition_name || '_ip_address_idx', partition_name);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for automatic partition creation
CREATE TRIGGER oauth2_audit_events_partition_trigger
    BEFORE INSERT ON oauth2_audit_events
    FOR EACH ROW EXECUTE FUNCTION create_oauth2_audit_monthly_partition();

-- Insert default OAuth2 provider configurations
INSERT INTO oauth2_providers (name, client_id, client_secret, authorization_uri, token_uri, user_info_uri, jwk_set_uri, issuer_uri, scopes, enabled, sort_order, client_name) VALUES
('google',
 'GOOGLE_CLIENT_ID_PLACEHOLDER',
 'GOOGLE_CLIENT_SECRET_PLACEHOLDER',
 'https://accounts.google.com/o/oauth2/v2/auth',
 'https://oauth2.googleapis.com/token',
 'https://www.googleapis.com/oauth2/v2/userinfo',
 'https://www.googleapis.com/oauth2/v3/certs',
 'https://accounts.google.com',
 ARRAY['openid', 'profile', 'email'],
 true,
 1,
 'Google'),

('github',
 'GITHUB_CLIENT_ID_PLACEHOLDER',
 'GITHUB_CLIENT_SECRET_PLACEHOLDER',
 'https://github.com/login/oauth/authorize',
 'https://github.com/login/oauth/access_token',
 'https://api.github.com/user',
 NULL,
 NULL,
 ARRAY['read:user', 'user:email'],
 true,
 2,
 'GitHub'),

('microsoft',
 'MICROSOFT_CLIENT_ID_PLACEHOLDER',
 'MICROSOFT_CLIENT_SECRET_PLACEHOLDER',
 'https://login.microsoftonline.com/common/oauth2/v2.0/authorize',
 'https://login.microsoftonline.com/common/oauth2/v2.0/token',
 'https://graph.microsoft.com/v1.0/me',
 'https://login.microsoftonline.com/common/discovery/v2.0/keys',
 'https://login.microsoftonline.com/common/v2.0',
 ARRAY['openid', 'profile', 'email'],
 true,
 3,
 'Microsoft');

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for updated_at
CREATE TRIGGER update_oauth2_providers_updated_at
    BEFORE UPDATE ON oauth2_providers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_oauth2_user_info_updated_at
    BEFORE UPDATE ON oauth2_user_info
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function for OAuth2 audit event cleanup (GDPR compliance)
CREATE OR REPLACE FUNCTION cleanup_old_oauth2_audit_events(retention_days INTEGER DEFAULT 90)
RETURNS INTEGER AS $$
DECLARE
    cutoff_date TIMESTAMP WITH TIME ZONE;
    deleted_count INTEGER := 0;
    partition_name TEXT;
    partition_record RECORD;
BEGIN
    cutoff_date := CURRENT_TIMESTAMP - (retention_days || ' days')::INTERVAL;

    -- Find partitions older than retention period
    FOR partition_record IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE tablename LIKE 'oauth2_audit_events_%'
        AND tablename ~ '^oauth2_audit_events_[0-9]{4}_[0-9]{2}$'
    LOOP
        partition_name := partition_record.tablename;

        -- Extract date from partition name and check if it's older than cutoff
        IF TO_DATE(SUBSTRING(partition_name FROM 'oauth2_audit_events_([0-9]{4}_[0-9]{2})'), 'YYYY_MM') < cutoff_date THEN
            EXECUTE format('DROP TABLE IF EXISTS %I', partition_name);
            deleted_count := deleted_count + 1;
        END IF;
    END LOOP;

    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create function for session cleanup
CREATE OR REPLACE FUNCTION cleanup_expired_oauth2_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    UPDATE oauth2_sessions
    SET is_active = false, terminated_at = CURRENT_TIMESTAMP
    WHERE is_active = true
    AND expires_at < CURRENT_TIMESTAMP;

    GET DIAGNOSTICS deleted_count = ROW_COUNT;

    -- Delete old terminated sessions (older than 30 days)
    DELETE FROM oauth2_sessions
    WHERE is_active = false
    AND terminated_at IS NOT NULL
    AND terminated_at < CURRENT_TIMESTAMP - INTERVAL '30 days';

    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE oauth2_providers IS 'OAuth2 provider configurations (Google, GitHub, Microsoft, etc.)';
COMMENT ON TABLE oauth2_user_info IS 'User information retrieved from OAuth2 providers';
COMMENT ON TABLE oauth2_sessions IS 'Active OAuth2 authentication sessions';
COMMENT ON TABLE oauth2_audit_events IS 'Comprehensive audit trail for OAuth2 authentication events (partitioned by month)';

COMMENT ON COLUMN oauth2_providers.scopes IS 'Array of OAuth2 scopes requested from this provider';
COMMENT ON COLUMN oauth2_providers.provider_details IS 'Additional provider-specific configuration (JSONB)';
COMMENT ON COLUMN oauth2_user_info.attributes IS 'Additional user attributes from OAuth2 provider (JSONB)';
COMMENT ON COLUMN oauth2_sessions.session_attributes IS 'Session-specific data and metadata (JSONB)';
COMMENT ON COLUMN oauth2_audit_events.additional_data IS 'Event-specific metadata and context (JSONB)';