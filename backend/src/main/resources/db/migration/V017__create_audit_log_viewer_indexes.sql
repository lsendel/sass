-- V017: Create audit log viewer performance indexes
-- Description: Add indexes to optimize audit log queries for the user-facing viewer
-- Date: 2025-09-27

-- Primary index for tenant isolation and recency queries (most common pattern)
CREATE INDEX IF NOT EXISTS idx_audit_events_org_timestamp
ON audit_events(organization_id, timestamp DESC);

-- Index for actor-based queries (find all actions by specific user)
CREATE INDEX IF NOT EXISTS idx_audit_events_actor_timestamp
ON audit_events(organization_id, actor_id, timestamp DESC);

-- Index for resource-based queries (find all actions affecting specific resource)
CREATE INDEX IF NOT EXISTS idx_audit_events_resource_timestamp
ON audit_events(organization_id, resource_type, resource_id, timestamp DESC);

-- Index for correlation ID tracking (incident investigation)
CREATE INDEX IF NOT EXISTS idx_audit_events_correlation
ON audit_events(correlation_id)
WHERE correlation_id IS NOT NULL;

-- Index for action type filtering
CREATE INDEX IF NOT EXISTS idx_audit_events_action_type
ON audit_events(organization_id, action_type, timestamp DESC);

-- Full-text search index for descriptions and actor names
CREATE INDEX IF NOT EXISTS idx_audit_events_search
ON audit_events USING GIN(to_tsvector('english',
    COALESCE(description, '') || ' ' || COALESCE(actor_email, '') || ' ' || COALESCE(actor_name, '')
));

-- Composite index for common filter combinations
CREATE INDEX IF NOT EXISTS idx_audit_events_filters
ON audit_events(organization_id, action_type, resource_type, outcome, timestamp DESC);

-- Index for cleanup and retention queries
CREATE INDEX IF NOT EXISTS idx_audit_events_retention
ON audit_events(timestamp)
WHERE timestamp < CURRENT_DATE - INTERVAL '2 years';

-- Add comments for maintenance
COMMENT ON INDEX idx_audit_events_org_timestamp IS 'Primary index for audit log viewer - tenant isolation and recency';
COMMENT ON INDEX idx_audit_events_actor_timestamp IS 'Index for actor-based audit queries';
COMMENT ON INDEX idx_audit_events_resource_timestamp IS 'Index for resource-based audit queries';
COMMENT ON INDEX idx_audit_events_correlation IS 'Index for correlation ID tracking';
COMMENT ON INDEX idx_audit_events_action_type IS 'Index for action type filtering';
COMMENT ON INDEX idx_audit_events_search IS 'Full-text search index for audit descriptions';
COMMENT ON INDEX idx_audit_events_filters IS 'Composite index for common filter combinations';
COMMENT ON INDEX idx_audit_events_retention IS 'Index for data retention and cleanup queries';