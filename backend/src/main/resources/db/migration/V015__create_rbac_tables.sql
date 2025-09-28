-- RBAC (Role-Based Access Control) Tables Migration
-- Creates tables for organizations, roles, permissions, and user role assignments
-- Supports multi-tenant role assignments with hierarchical permissions

-- =============================================================================
-- Core RBAC Tables
-- =============================================================================

-- System permissions catalog (Resource × Action combinations)
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT permissions_resource_action_unique UNIQUE (resource, action)
);

-- Organization roles (both predefined and custom)
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    organization_id BIGINT NOT NULL,
    role_type VARCHAR(20) NOT NULL DEFAULT 'CUSTOM',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,

    CONSTRAINT roles_organization_fk FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT roles_created_by_fk FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT roles_updated_by_fk FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT roles_name_org_unique UNIQUE (name, organization_id),
    CONSTRAINT roles_type_check CHECK (role_type IN ('PREDEFINED', 'CUSTOM'))
);

-- Junction table: Roles ← many-to-many → Permissions
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT,

    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT role_permissions_role_fk FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT role_permissions_permission_fk FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT role_permissions_granted_by_fk FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL
);

-- User role assignments within organization context (multi-tenant)
CREATE TABLE user_organization_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT user_org_roles_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT user_org_roles_org_fk FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT user_org_roles_role_fk FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT user_org_roles_assigned_by_fk FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT user_org_roles_unique UNIQUE (user_id, organization_id, role_id)
);

-- =============================================================================
-- Performance Indexes
-- =============================================================================

-- Permission lookup indexes
CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_permissions_active ON permissions(is_active) WHERE is_active = true;

-- Role management indexes
CREATE INDEX idx_roles_organization_active ON roles(organization_id, is_active);
CREATE INDEX idx_roles_type ON roles(role_type);
CREATE INDEX idx_roles_name_search ON roles(name) WHERE is_active = true;

-- Role-permission relationship indexes
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- User role assignment indexes (critical for permission checks)
CREATE INDEX idx_user_org_roles_user_org ON user_organization_roles(user_id, organization_id);
CREATE INDEX idx_user_org_roles_active ON user_organization_roles(is_active, expires_at) WHERE is_active = true;
CREATE INDEX idx_user_org_roles_org_role ON user_organization_roles(organization_id, role_id);
CREATE INDEX idx_user_org_roles_expiry ON user_organization_roles(expires_at) WHERE expires_at IS NOT NULL;

-- Composite index for high-frequency permission checks
CREATE INDEX idx_user_org_roles_permission_check ON user_organization_roles(user_id, organization_id, is_active)
WHERE is_active = true AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP);

-- =============================================================================
-- Database Functions for Performance
-- =============================================================================

-- Function to get all permissions for a user in an organization (for caching)
CREATE OR REPLACE FUNCTION get_user_permissions(p_user_id BIGINT, p_organization_id BIGINT)
RETURNS TABLE(resource VARCHAR, action VARCHAR) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT p.resource, p.action
    FROM user_organization_roles uor
    JOIN roles r ON uor.role_id = r.id
    JOIN role_permissions rp ON r.id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.id
    WHERE uor.user_id = p_user_id
      AND uor.organization_id = p_organization_id
      AND uor.is_active = true
      AND r.is_active = true
      AND p.is_active = true
      AND (uor.expires_at IS NULL OR uor.expires_at > CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to check if user has specific permission (for @PreAuthorize)
CREATE OR REPLACE FUNCTION user_has_permission(
    p_user_id BIGINT,
    p_organization_id BIGINT,
    p_resource VARCHAR,
    p_action VARCHAR
) RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1
        FROM user_organization_roles uor
        JOIN roles r ON uor.role_id = r.id
        JOIN role_permissions rp ON r.id = rp.role_id
        JOIN permissions p ON rp.permission_id = p.id
        WHERE uor.user_id = p_user_id
          AND uor.organization_id = p_organization_id
          AND uor.is_active = true
          AND r.is_active = true
          AND p.is_active = true
          AND p.resource = p_resource
          AND p.action = p_action
          AND (uor.expires_at IS NULL OR uor.expires_at > CURRENT_TIMESTAMP)
    );
END;
$$ LANGUAGE plpgsql STABLE;

-- =============================================================================
-- Triggers for Audit and Cache Invalidation
-- =============================================================================

-- Update timestamp trigger for roles table
CREATE OR REPLACE FUNCTION update_role_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_role_timestamp
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION update_role_timestamp();

-- Cache invalidation notification trigger
CREATE OR REPLACE FUNCTION notify_permission_cache_invalidation()
RETURNS TRIGGER AS $$
DECLARE
    user_id BIGINT;
    org_id BIGINT;
BEGIN
    -- Handle user role assignment changes
    IF TG_TABLE_NAME = 'user_organization_roles' THEN
        IF TG_OP = 'DELETE' THEN
            user_id := OLD.user_id;
            org_id := OLD.organization_id;
        ELSE
            user_id := NEW.user_id;
            org_id := NEW.organization_id;
        END IF;

        -- Notify cache invalidation for specific user-organization
        PERFORM pg_notify('permission_cache_invalidate',
            json_build_object('userId', user_id, 'organizationId', org_id)::text);
    END IF;

    -- Handle role permission changes (affects all users with that role)
    IF TG_TABLE_NAME = 'role_permissions' THEN
        -- Notify cache invalidation for all users with this role
        PERFORM pg_notify('role_permission_changed',
            json_build_object('roleId', COALESCE(NEW.role_id, OLD.role_id))::text);
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Triggers for cache invalidation
CREATE TRIGGER trigger_user_role_cache_invalidation
    AFTER INSERT OR UPDATE OR DELETE ON user_organization_roles
    FOR EACH ROW
    EXECUTE FUNCTION notify_permission_cache_invalidation();

CREATE TRIGGER trigger_role_permission_cache_invalidation
    AFTER INSERT OR UPDATE OR DELETE ON role_permissions
    FOR EACH ROW
    EXECUTE FUNCTION notify_permission_cache_invalidation();

-- =============================================================================
-- Comments for Documentation
-- =============================================================================

COMMENT ON TABLE permissions IS 'System catalog of all available permissions (Resource × Action combinations)';
COMMENT ON TABLE roles IS 'Organization-specific roles that can be assigned to users';
COMMENT ON TABLE role_permissions IS 'Junction table linking roles to their granted permissions';
COMMENT ON TABLE user_organization_roles IS 'Multi-tenant user role assignments with expiration support';

COMMENT ON FUNCTION get_user_permissions(BIGINT, BIGINT) IS 'Returns all permissions for a user in an organization (used for permission caching)';
COMMENT ON FUNCTION user_has_permission(BIGINT, BIGINT, VARCHAR, VARCHAR) IS 'Checks if user has specific permission (used by Spring Security @PreAuthorize)';

COMMENT ON INDEX idx_user_org_roles_permission_check IS 'Optimized index for high-frequency permission checks in Spring Security';

-- =============================================================================
-- Row Level Security (Future Enhancement)
-- =============================================================================

-- Enable RLS for additional security (can be activated later)
-- ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE user_organization_roles ENABLE ROW LEVEL SECURITY;

-- Example policy (commented out for now):
-- CREATE POLICY roles_organization_isolation ON roles
--     FOR ALL TO authenticated_user
--     USING (organization_id IN (SELECT organization_id FROM user_organizations WHERE user_id = current_user_id()));

-- =============================================================================
-- Constraints Validation
-- =============================================================================

-- Verify foreign key relationships exist
DO $$
BEGIN
    -- Check that organizations table exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'organizations') THEN
        RAISE EXCEPTION 'Organizations table does not exist. Run user module migrations first.';
    END IF;

    -- Check that users table exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        RAISE EXCEPTION 'Users table does not exist. Run user module migrations first.';
    END IF;

    RAISE NOTICE 'RBAC tables created successfully with all constraints and indexes.';
END $$;