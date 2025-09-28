-- Security Observability Dashboard Tables
-- Migration for Security Event, Dashboard, Widget, Alert Rule, and related tables

-- Create security_events table for real-time security event tracking
CREATE TABLE security_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('LOGIN_ATTEMPT', 'PERMISSION_CHECK', 'PAYMENT_FRAUD', 'DATA_ACCESS', 'API_ABUSE')),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    source_module VARCHAR(100) NOT NULL,
    source_ip INET,
    user_agent TEXT,
    details JSONB NOT NULL DEFAULT '{}',
    correlation_id VARCHAR(255) NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_by VARCHAR(255),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create dashboards table for security dashboard configurations
CREATE TABLE dashboards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    permissions TEXT[] NOT NULL DEFAULT '{}',
    is_default BOOLEAN NOT NULL DEFAULT false,
    tags TEXT[] NOT NULL DEFAULT '{}',
    owner VARCHAR(255) NOT NULL,
    shared BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_dashboard_name_owner UNIQUE (name, owner)
);

-- Create dashboard_widgets table for individual dashboard components
CREATE TABLE dashboard_widgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('CHART', 'TABLE', 'METRIC', 'ALERT_LIST', 'THREAT_MAP')),
    configuration JSONB NOT NULL DEFAULT '{}',
    position JSONB NOT NULL DEFAULT '{"x":0,"y":0,"width":4,"height":4}',
    permissions TEXT[] NOT NULL DEFAULT '{}',
    refresh_interval INTERVAL NOT NULL DEFAULT INTERVAL '30 seconds',
    data_source TEXT,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_widget_name_dashboard UNIQUE (name, dashboard_id)
);

-- Create alert_rules table for automated security alerting
CREATE TABLE alert_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    condition TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    enabled BOOLEAN NOT NULL DEFAULT true,
    threshold NUMERIC NOT NULL CHECK (threshold > 0),
    time_window INTERVAL NOT NULL CHECK (time_window >= INTERVAL '1 minute'),
    cooldown_period INTERVAL NOT NULL DEFAULT INTERVAL '30 seconds' CHECK (cooldown_period >= INTERVAL '30 seconds'),
    notification_channels TEXT[] NOT NULL DEFAULT '{}',
    escalation_rules JSONB NOT NULL DEFAULT '{}',
    created_by VARCHAR(255) NOT NULL,
    last_triggered TIMESTAMPTZ,
    trigger_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create security_metrics table for aggregated security metrics (PostgreSQL metadata)
CREATE TABLE security_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_name VARCHAR(255) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    tags JSONB NOT NULL DEFAULT '{}',
    unit VARCHAR(50) NOT NULL,
    aggregation_type VARCHAR(20) NOT NULL CHECK (aggregation_type IN ('SUM', 'AVERAGE', 'MAX', 'MIN', 'COUNT')),
    time_granularity VARCHAR(10) NOT NULL CHECK (time_granularity IN ('MINUTE', 'HOUR', 'DAY')),
    retention_days INTEGER NOT NULL CHECK (retention_days > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create threat_indicators table for external threat intelligence
CREATE TABLE threat_indicators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL CHECK (type IN ('IP_ADDRESS', 'DOMAIN', 'HASH', 'EMAIL')),
    value VARCHAR(500) NOT NULL,
    source VARCHAR(255) NOT NULL,
    confidence INTEGER NOT NULL CHECK (confidence >= 0 AND confidence <= 100),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    first_seen TIMESTAMPTZ NOT NULL,
    last_seen TIMESTAMPTZ NOT NULL,
    description TEXT,
    tags TEXT[] NOT NULL DEFAULT '{}',
    active BOOLEAN NOT NULL DEFAULT true,
    false_positive BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_not_active_and_false_positive CHECK (NOT (active = true AND false_positive = true)),
    CONSTRAINT chk_last_seen_after_first_seen CHECK (last_seen >= first_seen),
    CONSTRAINT uk_threat_indicator_type_value UNIQUE (type, value)
);

-- Create indexes for performance optimization

-- Security events indexes for real-time queries
CREATE INDEX idx_security_events_timestamp ON security_events(timestamp DESC);
CREATE INDEX idx_security_events_event_type ON security_events(event_type);
CREATE INDEX idx_security_events_severity ON security_events(severity);
CREATE INDEX idx_security_events_user_id ON security_events(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_security_events_correlation_id ON security_events(correlation_id);
CREATE INDEX idx_security_events_resolved ON security_events(resolved) WHERE resolved = false;
CREATE INDEX idx_security_events_source_module ON security_events(source_module);

-- Dashboard indexes
CREATE INDEX idx_dashboards_owner ON dashboards(owner);
CREATE INDEX idx_dashboards_is_default ON dashboards(is_default) WHERE is_default = true;
CREATE INDEX idx_dashboards_shared ON dashboards(shared) WHERE shared = true;
CREATE INDEX idx_dashboards_tags ON dashboards USING GIN(tags);

-- Widget indexes
CREATE INDEX idx_dashboard_widgets_dashboard_id ON dashboard_widgets(dashboard_id);
CREATE INDEX idx_dashboard_widgets_type ON dashboard_widgets(type);

-- Alert rule indexes
CREATE INDEX idx_alert_rules_enabled ON alert_rules(enabled) WHERE enabled = true;
CREATE INDEX idx_alert_rules_severity ON alert_rules(severity);
CREATE INDEX idx_alert_rules_last_triggered ON alert_rules(last_triggered);

-- Security metrics indexes
CREATE INDEX idx_security_metrics_name_timestamp ON security_metrics(metric_name, timestamp DESC);
CREATE INDEX idx_security_metrics_timestamp ON security_metrics(timestamp);
CREATE INDEX idx_security_metrics_tags ON security_metrics USING GIN(tags);

-- Threat indicators indexes
CREATE INDEX idx_threat_indicators_type ON threat_indicators(type);
CREATE INDEX idx_threat_indicators_active ON threat_indicators(active) WHERE active = true;
CREATE INDEX idx_threat_indicators_severity ON threat_indicators(severity);
CREATE INDEX idx_threat_indicators_last_seen ON threat_indicators(last_seen DESC);

-- Add triggers for updating timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_security_events_updated_at BEFORE UPDATE ON security_events FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_dashboards_updated_at BEFORE UPDATE ON dashboards FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_dashboard_widgets_updated_at BEFORE UPDATE ON dashboard_widgets FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_alert_rules_updated_at BEFORE UPDATE ON alert_rules FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_threat_indicators_updated_at BEFORE UPDATE ON threat_indicators FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add row level security policies (will be configured with actual roles)
ALTER TABLE security_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE dashboards ENABLE ROW LEVEL SECURITY;
ALTER TABLE dashboard_widgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE alert_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE security_metrics ENABLE ROW LEVEL SECURITY;
ALTER TABLE threat_indicators ENABLE ROW LEVEL SECURITY;

-- Add comments for documentation
COMMENT ON TABLE security_events IS 'Real-time security events from all platform modules';
COMMENT ON TABLE dashboards IS 'Security dashboard configurations and metadata';
COMMENT ON TABLE dashboard_widgets IS 'Individual widgets within security dashboards';
COMMENT ON TABLE alert_rules IS 'Automated alerting rules for security events';
COMMENT ON TABLE security_metrics IS 'Aggregated security metrics metadata (time-series data in InfluxDB)';
COMMENT ON TABLE threat_indicators IS 'External threat intelligence indicators';