-- Initial schema for Spring Boot Modulith Payment Platform
-- Created: 2024-01-01
-- Version: V001

-- =============================================================================
-- ORGANIZATIONS TABLE
-- =============================================================================

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(255),
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Organizations indexes
CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_domain ON organizations(domain);
CREATE INDEX idx_organizations_created_at ON organizations(created_at);

-- =============================================================================
-- USERS TABLE
-- =============================================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    account_locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_users_email_org UNIQUE (email, organization_id)
);

-- Users indexes
CREATE INDEX idx_users_organization_id ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_users_email_verified ON users(email_verified);
CREATE INDEX idx_users_last_login ON users(last_login_at);

-- =============================================================================
-- OAUTH2 PROVIDERS TABLE
-- =============================================================================

CREATE TABLE oauth2_providers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    authorization_url VARCHAR(500) NOT NULL,
    token_url VARCHAR(500) NOT NULL,
    user_info_url VARCHAR(500) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    jwk_set_uri VARCHAR(500),
    user_name_attribute VARCHAR(50) NOT NULL DEFAULT 'sub',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration TEXT,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- OAuth2 providers indexes
CREATE INDEX idx_oauth2_provider_name ON oauth2_providers(name);
CREATE INDEX idx_oauth2_provider_enabled ON oauth2_providers(enabled);

-- OAuth2 provider scopes table
CREATE TABLE oauth2_provider_scopes (
    provider_id BIGINT NOT NULL REFERENCES oauth2_providers(id) ON DELETE CASCADE,
    scope VARCHAR(100) NOT NULL,
    PRIMARY KEY (provider_id, scope)
);

-- =============================================================================
-- OAUTH2 USER INFO TABLE
-- =============================================================================

CREATE TABLE oauth2_user_info (
    id BIGSERIAL PRIMARY KEY,
    provider_user_id VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    email_verified BOOLEAN,
    name VARCHAR(255),
    given_name VARCHAR(100),
    family_name VARCHAR(100),
    picture VARCHAR(500),
    locale VARCHAR(10),
    timezone VARCHAR(50),
    raw_attributes TEXT,
    last_updated_from_provider TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_oauth2_user_provider_id UNIQUE (provider_user_id, provider),
    CONSTRAINT uq_oauth2_user_email_provider UNIQUE (email, provider)
);

-- OAuth2 user info indexes
CREATE INDEX idx_oauth2_user_provider_id ON oauth2_user_info(provider_user_id);
CREATE INDEX idx_oauth2_user_email ON oauth2_user_info(email);
CREATE INDEX idx_oauth2_user_provider ON oauth2_user_info(provider);
CREATE INDEX idx_oauth2_user_verified ON oauth2_user_info(email_verified);

-- =============================================================================
-- OAUTH2 SESSIONS TABLE
-- =============================================================================

CREATE TABLE oauth2_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    user_info_id BIGINT NOT NULL REFERENCES oauth2_user_info(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_accessed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_from_ip VARCHAR(45),
    created_from_user_agent VARCHAR(500),
    last_accessed_from_ip VARCHAR(45),
    authorization_code_hash VARCHAR(255),
    pkce_code_verifier_hash VARCHAR(255),
    oauth2_state_hash VARCHAR(255),
    terminated_at TIMESTAMP WITH TIME ZONE,
    termination_reason VARCHAR(50),
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- OAuth2 sessions indexes
CREATE INDEX idx_oauth2_session_id ON oauth2_sessions(session_id);
CREATE INDEX idx_oauth2_session_user ON oauth2_sessions(user_info_id);
CREATE INDEX idx_oauth2_session_expires ON oauth2_sessions(expires_at);
CREATE INDEX idx_oauth2_session_provider ON oauth2_sessions(provider);
CREATE INDEX idx_oauth2_session_active ON oauth2_sessions(is_active);

-- =============================================================================
-- OAUTH2 AUDIT EVENTS TABLE
-- =============================================================================

CREATE TABLE oauth2_audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    provider VARCHAR(50),
    description VARCHAR(500) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    event_details TEXT,
    error_code VARCHAR(100),
    error_message VARCHAR(1000),
    correlation_id VARCHAR(255),
    authorization_code_hash VARCHAR(255),
    state_hash VARCHAR(255),
    duration_ms BIGINT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- OAuth2 audit events indexes
CREATE INDEX idx_oauth2_audit_event_type ON oauth2_audit_events(event_type);
CREATE INDEX idx_oauth2_audit_user ON oauth2_audit_events(user_id);
CREATE INDEX idx_oauth2_audit_session ON oauth2_audit_events(session_id);
CREATE INDEX idx_oauth2_audit_provider ON oauth2_audit_events(provider);
CREATE INDEX idx_oauth2_audit_timestamp ON oauth2_audit_events(event_timestamp);
CREATE INDEX idx_oauth2_audit_ip ON oauth2_audit_events(ip_address);
CREATE INDEX idx_oauth2_audit_severity ON oauth2_audit_events(severity);

-- =============================================================================
-- SUBSCRIPTION PLANS TABLE
-- =============================================================================

CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    stripe_price_id VARCHAR(255) NOT NULL UNIQUE,
    amount_amount BIGINT NOT NULL,
    amount_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    interval VARCHAR(20) NOT NULL,
    interval_count INTEGER NOT NULL DEFAULT 1,
    trial_days INTEGER DEFAULT 0,
    display_order INTEGER NOT NULL DEFAULT 0,
    features JSONB DEFAULT '{}',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Plans indexes
CREATE INDEX idx_plans_active ON plans(active);
CREATE INDEX idx_plans_stripe_price ON plans(stripe_price_id);

-- =============================================================================
-- SUBSCRIPTIONS TABLE
-- =============================================================================

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES plans(id),
    stripe_subscription_id VARCHAR(255) UNIQUE,
    status VARCHAR(50) NOT NULL,
    current_period_start DATE,
    current_period_end DATE,
    trial_end DATE,
    cancel_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Subscriptions indexes
CREATE INDEX idx_subscriptions_org ON subscriptions(organization_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_stripe ON subscriptions(stripe_subscription_id);
CREATE INDEX idx_subscriptions_period ON subscriptions(current_period_end);

-- =============================================================================
-- PAYMENTS TABLE
-- =============================================================================

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    amount_amount BIGINT NOT NULL,
    amount_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    invoice_id UUID,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Payments indexes
CREATE INDEX idx_payments_organization ON payments(organization_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_stripe_intent ON payments(stripe_payment_intent_id);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- =============================================================================
-- PAYMENT METHODS TABLE
-- =============================================================================

CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    stripe_payment_method_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    billing_details JSONB DEFAULT '{}',
    card_details JSONB DEFAULT '{}',
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Payment methods indexes
CREATE INDEX idx_payment_methods_org ON payment_methods(organization_id);
CREATE INDEX idx_payment_methods_stripe ON payment_methods(stripe_payment_method_id);
CREATE INDEX idx_payment_methods_type ON payment_methods(type);
CREATE INDEX idx_payment_methods_default ON payment_methods(is_default);
CREATE INDEX idx_payment_methods_deleted ON payment_methods(deleted_at);

-- =============================================================================
-- INVOICES TABLE
-- =============================================================================

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES subscriptions(id),
    stripe_invoice_id VARCHAR(255) UNIQUE,
    invoice_number VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    subtotal_amount BIGINT NOT NULL,
    tax_amount BIGINT NOT NULL DEFAULT 0,
    total_amount_amount BIGINT NOT NULL,
    total_amount_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    due_date TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Invoices indexes
CREATE INDEX idx_invoices_organization ON invoices(organization_id);
CREATE INDEX idx_invoices_subscription ON invoices(subscription_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_stripe ON invoices(stripe_invoice_id);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_invoices_created_at ON invoices(created_at);

-- =============================================================================
-- AUDIT EVENTS TABLE (General platform audit)
-- =============================================================================

CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    entity_type VARCHAR(100),
    entity_id VARCHAR(255),
    description TEXT NOT NULL,
    metadata JSONB DEFAULT '{}',
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    correlation_id VARCHAR(255),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Audit events indexes
CREATE INDEX idx_audit_organization_type ON audit_events(organization_id, event_type);
CREATE INDEX idx_audit_user_timestamp ON audit_events(user_id, timestamp DESC);
CREATE INDEX idx_audit_correlation_id ON audit_events(correlation_id);
CREATE INDEX idx_audit_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_timestamp ON audit_events(timestamp);
CREATE INDEX idx_audit_severity ON audit_events(severity);

-- =============================================================================
-- AUTHENTICATION ATTEMPTS TABLE
-- =============================================================================

CREATE TABLE authentication_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    email VARCHAR(255) NOT NULL,
    method VARCHAR(50) NOT NULL,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    failure_reason VARCHAR(255),
    metadata JSONB DEFAULT '{}',
    attempt_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Authentication attempts indexes
CREATE INDEX idx_auth_attempts_user ON authentication_attempts(user_id);
CREATE INDEX idx_auth_attempts_email ON authentication_attempts(email);
CREATE INDEX idx_auth_attempts_ip ON authentication_attempts(ip_address);
CREATE INDEX idx_auth_attempts_time ON authentication_attempts(attempt_time);
CREATE INDEX idx_auth_attempts_success ON authentication_attempts(success);

-- =============================================================================
-- TOKEN METADATA TABLE
-- =============================================================================

CREATE TABLE token_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token_type VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Token metadata indexes
CREATE INDEX idx_token_hash ON token_metadata(token_hash);
CREATE INDEX idx_token_user ON token_metadata(user_id);
CREATE INDEX idx_token_type ON token_metadata(token_type);
CREATE INDEX idx_token_expires ON token_metadata(expires_at);
CREATE INDEX idx_token_revoked ON token_metadata(revoked);

-- =============================================================================
-- DATA RETENTION POLICIES
-- =============================================================================

-- Comments documenting retention policies for GDPR compliance
COMMENT ON TABLE audit_events IS 'Retention: 7 years for security/payment events, 1 year for user actions, 90 days for system events';
COMMENT ON TABLE oauth2_audit_events IS 'Retention: 7 years for security events, 1 year for normal authentication events';
COMMENT ON TABLE authentication_attempts IS 'Retention: 1 year for security monitoring';
COMMENT ON TABLE token_metadata IS 'Automatic cleanup on expiry + 30 days grace period';

-- =============================================================================
-- INITIAL DATA
-- =============================================================================

-- Insert default OAuth2 providers (if needed)
INSERT INTO oauth2_providers (name, display_name, authorization_url, token_url, user_info_url, client_id, enabled)
VALUES
    ('google', 'Google', 'https://accounts.google.com/o/oauth2/v2/auth',
     'https://oauth2.googleapis.com/token', 'https://openidconnect.googleapis.com/v1/userinfo',
     'placeholder-client-id', false),
    ('github', 'GitHub', 'https://github.com/login/oauth/authorize',
     'https://github.com/login/oauth/access_token', 'https://api.github.com/user',
     'placeholder-client-id', false);

-- Insert default OAuth2 provider scopes
INSERT INTO oauth2_provider_scopes (provider_id, scope) VALUES
    ((SELECT id FROM oauth2_providers WHERE name = 'google'), 'openid'),
    ((SELECT id FROM oauth2_providers WHERE name = 'google'), 'profile'),
    ((SELECT id FROM oauth2_providers WHERE name = 'google'), 'email'),
    ((SELECT id FROM oauth2_providers WHERE name = 'github'), 'read:user'),
    ((SELECT id FROM oauth2_providers WHERE name = 'github'), 'user:email');

-- Create default plans
INSERT INTO plans (name, slug, description, stripe_price_id, amount_amount, amount_currency, interval)
VALUES
    ('Starter', 'starter', 'Perfect for small teams getting started', 'price_starter_monthly', 999, 'USD', 'MONTH'),
    ('Professional', 'professional', 'For growing businesses', 'price_pro_monthly', 2999, 'USD', 'MONTH'),
    ('Enterprise', 'enterprise', 'For large organizations', 'price_enterprise_monthly', 9999, 'USD', 'MONTH');