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

-- Statistics update for better query planning
ANALYZE audit_events;
ANALYZE payment_methods;
ANALYZE organizations;
ANALYZE subscriptions;