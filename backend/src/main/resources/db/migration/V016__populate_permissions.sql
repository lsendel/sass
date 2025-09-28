-- RBAC System Data Population
-- Inserts all system permissions (Resource × Action combinations)
-- Creates predefined roles (Owner, Admin, Member, Viewer) for all organizations
-- Sets up initial permission assignments for predefined roles

-- =============================================================================
-- System Permissions Population
-- =============================================================================

-- Insert all Resource × Action permission combinations
-- Resources: PAYMENTS, ORGANIZATIONS, USERS, SUBSCRIPTIONS, AUDIT
-- Actions: READ, WRITE, DELETE, ADMIN

INSERT INTO permissions (resource, action, description) VALUES
-- PAYMENTS permissions
('PAYMENTS', 'READ', 'View payment records and transaction history'),
('PAYMENTS', 'WRITE', 'Create and update payment records'),
('PAYMENTS', 'DELETE', 'Delete payment records and refund transactions'),
('PAYMENTS', 'ADMIN', 'Full administrative access to payment system'),

-- ORGANIZATIONS permissions
('ORGANIZATIONS', 'READ', 'View organization information and settings'),
('ORGANIZATIONS', 'WRITE', 'Update organization settings and configuration'),
('ORGANIZATIONS', 'DELETE', 'Delete organization and all associated data'),
('ORGANIZATIONS', 'ADMIN', 'Full administrative access to organization management'),

-- USERS permissions
('USERS', 'READ', 'View user profiles and information within organization'),
('USERS', 'WRITE', 'Create, update, and manage user accounts'),
('USERS', 'DELETE', 'Remove users from organization and delete accounts'),
('USERS', 'ADMIN', 'Full administrative access to user management'),

-- SUBSCRIPTIONS permissions
('SUBSCRIPTIONS', 'READ', 'View subscription plans and billing information'),
('SUBSCRIPTIONS', 'WRITE', 'Create and modify subscription plans and billing'),
('SUBSCRIPTIONS', 'DELETE', 'Cancel subscriptions and delete billing data'),
('SUBSCRIPTIONS', 'ADMIN', 'Full administrative access to subscription management'),

-- AUDIT permissions
('AUDIT', 'READ', 'View audit logs and compliance reports'),
('AUDIT', 'WRITE', 'Create custom audit reports and export data'),
('AUDIT', 'DELETE', 'Purge audit logs (subject to retention policies)'),
('AUDIT', 'ADMIN', 'Full administrative access to audit system and compliance')

-- Verify all permissions were inserted correctly
ON CONFLICT (resource, action) DO NOTHING;

-- =============================================================================
-- Create Predefined Roles for Each Existing Organization
-- =============================================================================

-- Insert predefined roles for all existing organizations
-- This ensures every organization has the four standard roles available

INSERT INTO roles (name, description, organization_id, role_type, created_at)
SELECT
    'owner' as name,
    'Full organization access including billing and member management' as description,
    o.id as organization_id,
    'PREDEFINED' as role_type,
    CURRENT_TIMESTAMP as created_at
FROM organizations o
WHERE NOT EXISTS (
    SELECT 1 FROM roles r
    WHERE r.organization_id = o.id
    AND r.name = 'owner'
    AND r.role_type = 'PREDEFINED'
);

INSERT INTO roles (name, description, organization_id, role_type, created_at)
SELECT
    'admin' as name,
    'Administrative access excluding billing and organization deletion' as description,
    o.id as organization_id,
    'PREDEFINED' as role_type,
    CURRENT_TIMESTAMP as created_at
FROM organizations o
WHERE NOT EXISTS (
    SELECT 1 FROM roles r
    WHERE r.organization_id = o.id
    AND r.name = 'admin'
    AND r.role_type = 'PREDEFINED'
);

INSERT INTO roles (name, description, organization_id, role_type, created_at)
SELECT
    'member' as name,
    'Standard user access for core platform features' as description,
    o.id as organization_id,
    'PREDEFINED' as role_type,
    CURRENT_TIMESTAMP as created_at
FROM organizations o
WHERE NOT EXISTS (
    SELECT 1 FROM roles r
    WHERE r.organization_id = o.id
    AND r.name = 'member'
    AND r.role_type = 'PREDEFINED'
);

INSERT INTO roles (name, description, organization_id, role_type, created_at)
SELECT
    'viewer' as name,
    'Read-only access for reporting and monitoring' as description,
    o.id as organization_id,
    'PREDEFINED' as role_type,
    CURRENT_TIMESTAMP as created_at
FROM organizations o
WHERE NOT EXISTS (
    SELECT 1 FROM roles r
    WHERE r.organization_id = o.id
    AND r.name = 'viewer'
    AND r.role_type = 'PREDEFINED'
);

-- =============================================================================
-- Assign Permissions to Predefined Roles
-- =============================================================================

-- OWNER Role: Full access to everything
INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.role_type = 'PREDEFINED'
  AND r.name = 'owner'
  AND p.is_active = true
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ADMIN Role: Administrative access excluding organization deletion and audit deletion
INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.role_type = 'PREDEFINED'
  AND r.name = 'admin'
  AND p.is_active = true
  AND NOT (p.resource = 'ORGANIZATIONS' AND p.action = 'DELETE')  -- Cannot delete organization
  AND NOT (p.resource = 'AUDIT' AND p.action = 'DELETE')         -- Cannot delete audit logs
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- MEMBER Role: Standard user permissions (read/write but no admin or delete)
INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.role_type = 'PREDEFINED'
  AND r.name = 'member'
  AND p.is_active = true
  AND p.action IN ('READ', 'WRITE')
  AND NOT (p.resource = 'ORGANIZATIONS' AND p.action = 'WRITE')  -- Cannot modify org settings
  AND NOT (p.resource = 'USERS' AND p.action = 'WRITE')         -- Cannot manage other users
  AND NOT (p.resource = 'AUDIT' AND p.action = 'WRITE')         -- Cannot create audit reports
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- VIEWER Role: Read-only access to most resources
INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.role_type = 'PREDEFINED'
  AND r.name = 'viewer'
  AND p.is_active = true
  AND p.action = 'READ'
  AND p.resource IN ('PAYMENTS', 'SUBSCRIPTIONS', 'AUDIT')  -- Limited read access
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- =============================================================================
-- Assign Default Roles to Existing Users
-- =============================================================================

-- Assign 'owner' role to organization creators (if we can identify them)
-- This assumes the first user in each organization is the creator
INSERT INTO user_organization_roles (user_id, organization_id, role_id, assigned_at, assigned_by)
SELECT DISTINCT ON (uo.organization_id)
    uo.user_id,
    uo.organization_id,
    r.id as role_id,
    CURRENT_TIMESTAMP as assigned_at,
    NULL as assigned_by  -- System assignment
FROM user_organizations uo
JOIN roles r ON r.organization_id = uo.organization_id
WHERE r.name = 'owner'
  AND r.role_type = 'PREDEFINED'
  AND NOT EXISTS (
      SELECT 1 FROM user_organization_roles uor
      WHERE uor.user_id = uo.user_id
        AND uor.organization_id = uo.organization_id
        AND uor.is_active = true
  )
ORDER BY uo.organization_id, uo.created_at ASC
ON CONFLICT (user_id, organization_id, role_id) DO NOTHING;

-- Assign 'member' role to all other existing organization users
INSERT INTO user_organization_roles (user_id, organization_id, role_id, assigned_at, assigned_by)
SELECT
    uo.user_id,
    uo.organization_id,
    r.id as role_id,
    CURRENT_TIMESTAMP as assigned_at,
    NULL as assigned_by  -- System assignment
FROM user_organizations uo
JOIN roles r ON r.organization_id = uo.organization_id
WHERE r.name = 'member'
  AND r.role_type = 'PREDEFINED'
  AND NOT EXISTS (
      SELECT 1 FROM user_organization_roles uor
      WHERE uor.user_id = uo.user_id
        AND uor.organization_id = uo.organization_id
        AND uor.is_active = true
  )
ON CONFLICT (user_id, organization_id, role_id) DO NOTHING;

-- =============================================================================
-- Create Trigger to Auto-Create Predefined Roles for New Organizations
-- =============================================================================

-- Function to automatically create predefined roles when a new organization is created
CREATE OR REPLACE FUNCTION create_predefined_roles_for_organization()
RETURNS TRIGGER AS $$
DECLARE
    new_org_id BIGINT;
    owner_role_id BIGINT;
    admin_role_id BIGINT;
    member_role_id BIGINT;
    viewer_role_id BIGINT;
    perm_id BIGINT;
BEGIN
    new_org_id := NEW.id;

    -- Create Owner role
    INSERT INTO roles (name, description, organization_id, role_type, created_at)
    VALUES ('owner', 'Full organization access including billing and member management',
            new_org_id, 'PREDEFINED', CURRENT_TIMESTAMP)
    RETURNING id INTO owner_role_id;

    -- Create Admin role
    INSERT INTO roles (name, description, organization_id, role_type, created_at)
    VALUES ('admin', 'Administrative access excluding billing and organization deletion',
            new_org_id, 'PREDEFINED', CURRENT_TIMESTAMP)
    RETURNING id INTO admin_role_id;

    -- Create Member role
    INSERT INTO roles (name, description, organization_id, role_type, created_at)
    VALUES ('member', 'Standard user access for core platform features',
            new_org_id, 'PREDEFINED', CURRENT_TIMESTAMP)
    RETURNING id INTO member_role_id;

    -- Create Viewer role
    INSERT INTO roles (name, description, organization_id, role_type, created_at)
    VALUES ('viewer', 'Read-only access for reporting and monitoring',
            new_org_id, 'PREDEFINED', CURRENT_TIMESTAMP)
    RETURNING id INTO viewer_role_id;

    -- Assign all permissions to Owner role
    INSERT INTO role_permissions (role_id, permission_id, granted_at)
    SELECT owner_role_id, p.id, CURRENT_TIMESTAMP
    FROM permissions p
    WHERE p.is_active = true;

    -- Assign Admin permissions (excluding org deletion and audit deletion)
    INSERT INTO role_permissions (role_id, permission_id, granted_at)
    SELECT admin_role_id, p.id, CURRENT_TIMESTAMP
    FROM permissions p
    WHERE p.is_active = true
      AND NOT (p.resource = 'ORGANIZATIONS' AND p.action = 'DELETE')
      AND NOT (p.resource = 'AUDIT' AND p.action = 'DELETE');

    -- Assign Member permissions (read/write but limited)
    INSERT INTO role_permissions (role_id, permission_id, granted_at)
    SELECT member_role_id, p.id, CURRENT_TIMESTAMP
    FROM permissions p
    WHERE p.is_active = true
      AND p.action IN ('READ', 'WRITE')
      AND NOT (p.resource = 'ORGANIZATIONS' AND p.action = 'WRITE')
      AND NOT (p.resource = 'USERS' AND p.action = 'WRITE')
      AND NOT (p.resource = 'AUDIT' AND p.action = 'WRITE');

    -- Assign Viewer permissions (read-only, limited resources)
    INSERT INTO role_permissions (role_id, permission_id, granted_at)
    SELECT viewer_role_id, p.id, CURRENT_TIMESTAMP
    FROM permissions p
    WHERE p.is_active = true
      AND p.action = 'READ'
      AND p.resource IN ('PAYMENTS', 'SUBSCRIPTIONS', 'AUDIT');

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for new organizations
CREATE TRIGGER trigger_create_predefined_roles
    AFTER INSERT ON organizations
    FOR EACH ROW
    EXECUTE FUNCTION create_predefined_roles_for_organization();

-- =============================================================================
-- Data Validation and Statistics
-- =============================================================================

-- Verify data integrity
DO $$
DECLARE
    permission_count INTEGER;
    role_count INTEGER;
    org_count INTEGER;
    assignment_count INTEGER;
BEGIN
    -- Count permissions
    SELECT COUNT(*) INTO permission_count FROM permissions WHERE is_active = true;
    RAISE NOTICE 'Created % active permissions', permission_count;

    -- Count predefined roles
    SELECT COUNT(*) INTO role_count FROM roles WHERE role_type = 'PREDEFINED';
    RAISE NOTICE 'Created % predefined roles across all organizations', role_count;

    -- Count organizations
    SELECT COUNT(*) INTO org_count FROM organizations;
    RAISE NOTICE 'Found % organizations', org_count;

    -- Count role assignments
    SELECT COUNT(*) INTO assignment_count FROM user_organization_roles WHERE is_active = true;
    RAISE NOTICE 'Created % user role assignments', assignment_count;

    -- Verify each organization has 4 predefined roles
    IF EXISTS (
        SELECT 1 FROM organizations o
        WHERE (SELECT COUNT(*) FROM roles r
               WHERE r.organization_id = o.id AND r.role_type = 'PREDEFINED') != 4
    ) THEN
        RAISE WARNING 'Some organizations do not have exactly 4 predefined roles';
    ELSE
        RAISE NOTICE 'All organizations have complete predefined role sets';
    END IF;

    -- Verify permission counts per role type
    RAISE NOTICE 'Owner roles have % permissions each',
        (SELECT COUNT(*) FROM role_permissions rp
         JOIN roles r ON rp.role_id = r.id
         WHERE r.name = 'owner' AND r.role_type = 'PREDEFINED'
         LIMIT 1);

    RAISE NOTICE 'Admin roles have % permissions each',
        (SELECT COUNT(*) FROM role_permissions rp
         JOIN roles r ON rp.role_id = r.id
         WHERE r.name = 'admin' AND r.role_type = 'PREDEFINED'
         LIMIT 1);

    RAISE NOTICE 'Member roles have % permissions each',
        (SELECT COUNT(*) FROM role_permissions rp
         JOIN roles r ON rp.role_id = r.id
         WHERE r.name = 'member' AND r.role_type = 'PREDEFINED'
         LIMIT 1);

    RAISE NOTICE 'Viewer roles have % permissions each',
        (SELECT COUNT(*) FROM role_permissions rp
         JOIN roles r ON rp.role_id = r.id
         WHERE r.name = 'viewer' AND r.role_type = 'PREDEFINED'
         LIMIT 1);
END $$;

-- =============================================================================
-- Comments and Documentation
-- =============================================================================

COMMENT ON TRIGGER trigger_create_predefined_roles ON organizations IS
'Automatically creates the four predefined roles (owner, admin, member, viewer) with appropriate permissions when a new organization is created';

COMMENT ON FUNCTION create_predefined_roles_for_organization() IS
'Creates owner, admin, member, and viewer roles with predefined permission sets for newly created organizations';

-- Create helpful views for role and permission management
CREATE OR REPLACE VIEW v_role_permissions AS
SELECT
    r.id as role_id,
    r.name as role_name,
    r.role_type,
    r.organization_id,
    p.resource,
    p.action,
    p.description as permission_description,
    rp.granted_at
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE r.is_active = true AND p.is_active = true;

CREATE OR REPLACE VIEW v_user_effective_permissions AS
SELECT
    uor.user_id,
    uor.organization_id,
    p.resource,
    p.action,
    p.description as permission_description,
    array_agg(DISTINCT r.name ORDER BY r.name) as granted_by_roles
FROM user_organization_roles uor
JOIN roles r ON uor.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE uor.is_active = true
  AND r.is_active = true
  AND p.is_active = true
  AND (uor.expires_at IS NULL OR uor.expires_at > CURRENT_TIMESTAMP)
GROUP BY uor.user_id, uor.organization_id, p.resource, p.action, p.description;

COMMENT ON VIEW v_role_permissions IS 'Comprehensive view of all role-permission assignments';
COMMENT ON VIEW v_user_effective_permissions IS 'View of effective permissions per user per organization with role attribution';

RAISE NOTICE 'RBAC system data population completed successfully';
RAISE NOTICE 'All organizations now have predefined roles with appropriate permissions';
RAISE NOTICE 'Existing users have been assigned default roles based on organization membership';