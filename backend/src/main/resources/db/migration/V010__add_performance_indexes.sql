-- Performance optimization indexes for high-traffic queries
-- Migration: V010__add_performance_indexes.sql

-- Audit Events Performance Indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_org_time_action
ON audit_events(organization_id, created_at DESC, action);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_actor_time
ON audit_events(actor_id, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_security_analysis
ON audit_events(organization_id, action, created_at)
WHERE action IN ('LOGIN_FAILED', 'UNAUTHORIZED_ACCESS', 'SUSPICIOUS_ACTIVITY', 'ACCOUNT_LOCKOUT');

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_correlation
ON audit_events(correlation_id, created_at)
WHERE correlation_id IS NOT NULL;

-- Payment Methods Performance Indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payment_method_org_active
ON payment_methods(organization_id, deleted_at, is_default DESC)
WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payment_method_stripe
ON payment_methods(stripe_payment_method_id)
WHERE deleted_at IS NULL;

-- Organizations Performance Indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_org_slug_active
ON organizations(slug)
WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_org_owner_status
ON organizations(owner_id, status);

-- User Performance Indexes (assuming users table exists)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_email_active
ON users(email)
WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_org_role
ON organization_members(organization_id, user_id, role);

-- Subscription Performance Indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_subscription_org_status
ON subscriptions(organization_id, status, next_billing_date);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_subscription_billing
ON subscriptions(next_billing_date, status)
WHERE status = 'ACTIVE';

-- Partial indexes for common filter patterns
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_failed_logins
ON audit_events(organization_id, ip_address, created_at)
WHERE action = 'LOGIN_FAILED';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_export_compliance
ON audit_events(organization_id, actor_id, created_at)
WHERE action = 'export';

-- Optimize for time-based queries with proper ordering
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_time_partitioned
ON audit_events(DATE_TRUNC('day', created_at), organization_id, action);

-- Enhanced audit event indexes for new repository methods
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_user_timeframe
ON audit_events(actor_id, created_at DESC)
WHERE actor_id IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_recent_events
ON audit_events(created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_advanced_search
ON audit_events(actor_id, action, severity, created_at, ip_address, correlation_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_compression
ON audit_events(created_at, compressed)
WHERE compressed = false;

-- Partial index for archival operations
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_archival
ON audit_events(created_at)
WHERE created_at < NOW() - INTERVAL '90 days';

-- Audit events archive table for performance
CREATE TABLE IF NOT EXISTS audit_events_archive (
    LIKE audit_events INCLUDING ALL
);

-- Partition tables for better performance on large datasets
-- Create monthly partitions for audit_events if not exists
DO $$
DECLARE
    start_date date := date_trunc('month', CURRENT_DATE - INTERVAL '6 months');
    end_date date := date_trunc('month', CURRENT_DATE + INTERVAL '6 months');
    curr_date date := start_date;
    table_name text;
BEGIN
    WHILE curr_date < end_date LOOP
        table_name := 'audit_events_' || to_char(curr_date, 'YYYY_MM');

        -- Create partition if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_name = table_name
        ) THEN
            EXECUTE format('
                CREATE TABLE %I PARTITION OF audit_events
                FOR VALUES FROM (%L) TO (%L)',
                table_name,
                curr_date,
                curr_date + INTERVAL '1 month'
            );
        END IF;

        curr_date := curr_date + INTERVAL '1 month';
    END LOOP;
END $$;

-- Statistics update for better query planning
ANALYZE audit_events;
ANALYZE payment_methods;
ANALYZE organizations;
ANALYZE subscriptions;

-- Update table statistics for new indexes
ANALYZE audit_events_archive;