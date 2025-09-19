-- Performance indexes for Spring Boot Modulith Payment Platform
-- Created: 2024-01-01
-- Version: V002

-- =============================================================================
-- COMPOSITE INDEXES FOR COMMON QUERY PATTERNS
-- =============================================================================

-- Users - Organization + Email lookup (multi-tenant queries)
CREATE INDEX CONCURRENTLY idx_users_org_email ON users(organization_id, email);

-- Users - Organization + Status for active user queries
CREATE INDEX CONCURRENTLY idx_users_org_enabled ON users(organization_id, enabled) WHERE enabled = true;

-- Payments - Organization + Status + Created (dashboard queries)
CREATE INDEX CONCURRENTLY idx_payments_org_status_created ON payments(organization_id, status, created_at DESC);

-- Payments - Organization + Date range (reporting queries)
CREATE INDEX CONCURRENTLY idx_payments_org_created ON payments(organization_id, created_at DESC);

-- Subscriptions - Organization + Status for active subscriptions
CREATE INDEX CONCURRENTLY idx_subscriptions_org_status ON subscriptions(organization_id, status);

-- Subscriptions - Active subscriptions by user
CREATE INDEX CONCURRENTLY idx_subscriptions_user_active ON subscriptions(organization_id, status) WHERE status = 'ACTIVE';

-- Payment Methods - Organization + Active (excluding deleted)
CREATE INDEX CONCURRENTLY idx_payment_methods_org_active ON payment_methods(organization_id, is_default) WHERE deleted_at IS NULL;

-- Invoices - Organization + Status + Due Date (payment processing)
CREATE INDEX CONCURRENTLY idx_invoices_org_status_due ON invoices(organization_id, status, due_date);

-- =============================================================================
-- AUDIT AND SECURITY INDEXES
-- =============================================================================

-- Audit Events - Organization + Type + Timestamp (compliance queries)
CREATE INDEX CONCURRENTLY idx_audit_org_type_time ON audit_events(organization_id, event_type, timestamp DESC);

-- Audit Events - Retention policy enforcement
CREATE INDEX CONCURRENTLY idx_audit_retention_cleanup ON audit_events(event_type, timestamp) WHERE timestamp < NOW() - INTERVAL '7 years';

-- OAuth2 Audit Events - Retention categories
CREATE INDEX CONCURRENTLY idx_oauth2_audit_retention ON oauth2_audit_events(event_type, event_timestamp, severity);

-- Authentication Attempts - IP + Time (security monitoring)
CREATE INDEX CONCURRENTLY idx_auth_attempts_ip_time ON authentication_attempts(ip_address, attempt_time DESC);

-- Authentication Attempts - Failed attempts by email (account security)
CREATE INDEX CONCURRENTLY idx_auth_attempts_email_failed ON authentication_attempts(email, attempt_time DESC) WHERE success = false;

-- Authentication Attempts - Cleanup old records
CREATE INDEX CONCURRENTLY idx_auth_attempts_cleanup ON authentication_attempts(attempt_time) WHERE attempt_time < NOW() - INTERVAL '1 year';

-- =============================================================================
-- TOKEN AND SESSION INDEXES
-- =============================================================================

-- Token Metadata - Cleanup expired tokens
CREATE INDEX CONCURRENTLY idx_token_expired_cleanup ON token_metadata(expires_at, revoked) WHERE expires_at < NOW() OR revoked = true;

-- OAuth2 Sessions - Active sessions by user
CREATE INDEX CONCURRENTLY idx_oauth2_sessions_user_active ON oauth2_sessions(user_info_id, is_active, expires_at) WHERE is_active = true;

-- OAuth2 Sessions - Cleanup expired sessions
CREATE INDEX CONCURRENTLY idx_oauth2_sessions_cleanup ON oauth2_sessions(expires_at, is_active) WHERE expires_at < NOW() OR is_active = false;

-- =============================================================================
-- FINANCIAL DATA INDEXES
-- =============================================================================

-- Payments - Amount-based queries for analytics
CREATE INDEX CONCURRENTLY idx_payments_amount_currency_date ON payments(amount_currency, amount_amount, created_at DESC) WHERE status = 'SUCCEEDED';

-- Invoices - Financial reporting
CREATE INDEX CONCURRENTLY idx_invoices_financial_reporting ON invoices(organization_id, status, created_at DESC, total_amount_amount) WHERE status IN ('PAID', 'OPEN');

-- Plans - Active plans by price
CREATE INDEX CONCURRENTLY idx_plans_active_price ON plans(active, amount_amount ASC) WHERE active = true;

-- =============================================================================
-- PARTIAL INDEXES FOR COMMON FILTERS
-- =============================================================================

-- Active organizations only
CREATE INDEX CONCURRENTLY idx_organizations_active ON organizations(created_at DESC) WHERE created_at IS NOT NULL;

-- Verified users only
CREATE INDEX CONCURRENTLY idx_users_verified ON users(organization_id, email_verified) WHERE email_verified = true;

-- Enabled OAuth2 providers
CREATE INDEX CONCURRENTLY idx_oauth2_providers_enabled ON oauth2_providers(sort_order, name) WHERE enabled = true;

-- Active plans by interval
CREATE INDEX CONCURRENTLY idx_plans_active_interval ON plans(interval, amount_amount ASC) WHERE active = true;

-- =============================================================================
-- COVERING INDEXES FOR READ-HEAVY QUERIES
-- =============================================================================

-- User dashboard data
CREATE INDEX CONCURRENTLY idx_users_dashboard_covering ON users(organization_id, id, email, full_name, last_login_at) WHERE enabled = true;

-- Payment summary data
CREATE INDEX CONCURRENTLY idx_payments_summary_covering ON payments(organization_id, id, amount_amount, amount_currency, status, created_at);

-- Subscription overview
CREATE INDEX CONCURRENTLY idx_subscriptions_overview_covering ON subscriptions(organization_id, id, status, current_period_end, plan_id);

-- =============================================================================
-- CONSTRAINT IMPROVEMENTS
-- =============================================================================

-- Ensure only one default payment method per organization
CREATE UNIQUE INDEX CONCURRENTLY idx_payment_methods_one_default_per_org
ON payment_methods(organization_id)
WHERE is_default = true AND deleted_at IS NULL;

-- Ensure unique active subscription per organization (if business rule requires it)
-- Note: Commented out as business may allow multiple subscriptions
-- CREATE UNIQUE INDEX CONCURRENTLY idx_subscriptions_one_active_per_org
-- ON subscriptions(organization_id)
-- WHERE status IN ('ACTIVE', 'TRIALING');

-- =============================================================================
-- PERFORMANCE MONITORING
-- =============================================================================

-- Add comments for monitoring and alerting
COMMENT ON INDEX idx_payments_org_status_created IS 'Critical for payment dashboard performance - monitor query times';
COMMENT ON INDEX idx_audit_org_type_time IS 'Critical for compliance reporting - monitor for slow queries';
COMMENT ON INDEX idx_users_org_email IS 'Critical for authentication performance - monitor for slow lookups';

-- =============================================================================
-- STATISTICS UPDATE
-- =============================================================================

-- Analyze tables to update statistics for query planner
ANALYZE organizations;
ANALYZE users;
ANALYZE payments;
ANALYZE subscriptions;
ANALYZE audit_events;
ANALYZE oauth2_audit_events;
ANALYZE authentication_attempts;