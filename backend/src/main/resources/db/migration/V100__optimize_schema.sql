-- Performance and reliability optimizations
-- Add missing indexes for common queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email_active ON users(email) WHERE active = true;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_status_created ON payments(status, created_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_entity_action ON audit_logs(entity_type, action);

-- Add constraints for data integrity
ALTER TABLE users ADD CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
ALTER TABLE payments ADD CONSTRAINT chk_amount_positive CHECK (amount > 0);

-- Optimize for common queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_org_role ON users(organization_id, role) WHERE active = true;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_user_recent ON payments(user_id, created_at DESC) WHERE status != 'CANCELLED';
