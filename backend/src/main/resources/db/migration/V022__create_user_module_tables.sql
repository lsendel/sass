-- =============================================================================
-- USER MODULE TABLES
-- =============================================================================
-- Migration: V022__create_user_module_tables.sql
-- Module: User (com.platform.user)
-- Purpose: Creates tables for user profiles and organizations
--
-- Multi-tenant Architecture:
-- - Organizations are tenants
-- - Users belong to organizations
-- - Data isolation by organization_id
-- =============================================================================

-- =============================================================================
-- ORGANIZATIONS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(255),
    billing_email VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    settings TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_organizations_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

-- Indexes for organizations
CREATE INDEX idx_organizations_slug ON organizations(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_organizations_domain ON organizations(domain) WHERE domain IS NOT NULL;
CREATE INDEX idx_organizations_status ON organizations(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_organizations_created_at ON organizations(created_at);

-- Comments
COMMENT ON TABLE organizations IS 'Multi-tenant organizations (tenants)';
COMMENT ON COLUMN organizations.slug IS 'URL-friendly unique identifier';
COMMENT ON COLUMN organizations.settings IS 'JSON string for organization settings';
COMMENT ON COLUMN organizations.status IS 'Organization status: ACTIVE, SUSPENDED, DELETED';

-- =============================================================================
-- USER_PROFILES TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    avatar_url VARCHAR(500),
    phone_number VARCHAR(20),
    timezone VARCHAR(100),
    locale VARCHAR(10),
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    preferences TEXT,
    last_active_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_user_profiles_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'GUEST')),
    CONSTRAINT uq_user_profiles_org_email UNIQUE (organization_id, email)
);

-- Indexes for user_profiles
CREATE INDEX idx_user_profiles_organization ON user_profiles(organization_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_profiles_email ON user_profiles(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_profiles_role ON user_profiles(role) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_profiles_last_active ON user_profiles(last_active_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_profiles_created_at ON user_profiles(created_at);

-- Comments
COMMENT ON TABLE user_profiles IS 'User profile information (separate from auth.User)';
COMMENT ON COLUMN user_profiles.id IS 'Same as auth.User.id (one-to-one relationship)';
COMMENT ON COLUMN user_profiles.preferences IS 'JSON string for user preferences';
COMMENT ON COLUMN user_profiles.role IS 'User role within organization: OWNER, ADMIN, MEMBER, GUEST';

-- =============================================================================
-- USER_INVITATIONS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS user_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    invited_by UUID NOT NULL REFERENCES user_profiles(id),
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_user_invitations_role CHECK (role IN ('ADMIN', 'MEMBER', 'GUEST')),
    CONSTRAINT chk_user_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'))
);

-- Indexes for user_invitations
CREATE INDEX idx_user_invitations_org ON user_invitations(organization_id);
CREATE INDEX idx_user_invitations_email ON user_invitations(email);
CREATE INDEX idx_user_invitations_token ON user_invitations(token);
CREATE INDEX idx_user_invitations_status ON user_invitations(status);
CREATE INDEX idx_user_invitations_expires ON user_invitations(expires_at);

-- Comments
COMMENT ON TABLE user_invitations IS 'User invitation system for onboarding new organization members';
COMMENT ON COLUMN user_invitations.token IS 'Unique invitation token sent via email';

-- =============================================================================
-- DATA RETENTION & CLEANUP
-- =============================================================================

-- Function to clean up expired invitations
CREATE OR REPLACE FUNCTION cleanup_expired_invitations()
RETURNS void AS $$
BEGIN
    -- Mark expired invitations
    UPDATE user_invitations
    SET status = 'EXPIRED'
    WHERE status = 'PENDING'
      AND expires_at < CURRENT_TIMESTAMP;

    -- Delete old expired invitations (30 days after expiry)
    DELETE FROM user_invitations
    WHERE status = 'EXPIRED'
      AND expires_at < CURRENT_TIMESTAMP - INTERVAL '30 days';
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_invitations() IS 'Cleanup expired invitations (run daily via scheduled job)';

-- =============================================================================
-- INITIAL DATA
-- =============================================================================
-- No default organizations - must be created via API
