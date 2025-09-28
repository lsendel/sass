# Data Model: RBAC for Organizations

## Entity Overview

The RBAC system extends the existing user and organization entities with new role and permission management capabilities. The model supports multi-tenant organizations with hierarchical permissions and role inheritance.

## Core Entities

### Role Entity
Represents a collection of permissions that can be assigned to users within an organization context.

**Attributes**:
- `id` (Long) - Primary key
- `name` (String, 50 chars max) - Role name (unique per organization)
- `description` (String, 255 chars max) - Role description
- `organizationId` (Long) - Foreign key to Organization
- `roleType` (RoleType enum) - PREDEFINED or CUSTOM
- `isActive` (Boolean) - Whether role can be assigned
- `createdAt` (Timestamp) - Creation timestamp
- `createdBy` (Long) - User who created the role
- `updatedAt` (Timestamp) - Last modification timestamp
- `updatedBy` (Long) - User who last updated the role

**Relationships**:
- Many-to-One with Organization (through organizationId)
- Many-to-Many with Permission (through RolePermission)
- One-to-Many with UserOrganizationRole

**Validation Rules**:
- Name must be unique within organization
- Description is optional for predefined roles
- Custom roles require non-empty description
- Cannot delete role if assigned to any users
- Name cannot contain special characters except hyphens and underscores

### Permission Entity
Represents a specific action that can be performed on a resource within the system.

**Attributes**:
- `id` (Long) - Primary key
- `resource` (Resource enum) - Target resource (PAYMENTS, ORGANIZATIONS, USERS, SUBSCRIPTIONS, AUDIT)
- `action` (Action enum) - Permitted action (READ, WRITE, DELETE, ADMIN)
- `description` (String) - Human-readable permission description
- `isActive` (Boolean) - Whether permission is available for assignment

**Relationships**:
- Many-to-Many with Role (through RolePermission)

**Validation Rules**:
- Combination of resource and action must be unique
- All permissions are system-defined (no custom permissions)
- Cannot delete permission if referenced by any role

**State Transitions**:
- Permissions can be deactivated but not deleted
- Deactivated permissions are removed from all roles automatically

### RolePermission Entity
Junction table linking roles to their assigned permissions.

**Attributes**:
- `roleId` (Long) - Foreign key to Role
- `permissionId` (Long) - Foreign key to Permission
- `grantedAt` (Timestamp) - When permission was granted to role
- `grantedBy` (Long) - User who granted the permission

**Relationships**:
- Many-to-One with Role
- Many-to-One with Permission

**Validation Rules**:
- Composite primary key (roleId, permissionId)
- Cannot grant same permission to role twice
- Cannot grant permission to inactive role

### UserOrganizationRole Entity
Links users to roles within specific organization contexts, supporting multi-tenant role assignments.

**Attributes**:
- `id` (Long) - Primary key
- `userId` (Long) - Foreign key to User
- `organizationId` (Long) - Foreign key to Organization
- `roleId` (Long) - Foreign key to Role
- `assignedAt` (Timestamp) - When role was assigned
- `assignedBy` (Long) - User who assigned the role
- `expiresAt` (Timestamp, nullable) - Optional role expiration
- `isActive` (Boolean) - Whether assignment is currently active

**Relationships**:
- Many-to-One with User (through userId)
- Many-to-One with Organization (through organizationId)
- Many-to-One with Role (through roleId)

**Validation Rules**:
- Combination of (userId, organizationId, roleId) must be unique
- Cannot assign inactive role
- Cannot assign role from different organization
- User must be member of organization
- Maximum 5 roles per user per organization (configurable)

**State Transitions**:
- Active → Inactive (role removal, maintains audit trail)
- Expired assignments automatically become inactive
- Cannot reactivate expired assignments (must create new)

## Extended Entities

### User Entity (Extended)
The existing User entity is extended with RBAC-related convenience methods and caching.

**New Attributes**:
- `permissionsCache` (JSON, transient) - Cached permissions per organization
- `cacheTimestamp` (Timestamp, transient) - Cache validity timestamp

### Organization Entity (Extended)
The existing Organization entity is extended with RBAC configuration.

**New Attributes**:
- `maxCustomRoles` (Integer, default 10) - Maximum custom roles allowed
- `maxRolesPerUser` (Integer, default 5) - Maximum roles per user
- `rbacEnabled` (Boolean, default true) - Whether RBAC is enabled

## Enumerations

### RoleType
- `PREDEFINED` - System-defined roles (Owner, Admin, Member, Viewer)
- `CUSTOM` - Organization-defined custom roles

### Resource
- `PAYMENTS` - Payment processing and management
- `ORGANIZATIONS` - Organization settings and management
- `USERS` - User management within organization
- `SUBSCRIPTIONS` - Subscription and billing management
- `AUDIT` - Audit logs and compliance reporting

### Action
- `READ` - View/read access to resource
- `WRITE` - Create/update access to resource
- `DELETE` - Delete access to resource
- `ADMIN` - Full administrative access to resource

## Database Schema Considerations

### Indexes
```sql
-- Performance indexes for frequent queries
CREATE INDEX idx_user_org_roles_user_org ON user_organization_roles(user_id, organization_id);
CREATE INDEX idx_user_org_roles_active ON user_organization_roles(is_active, expires_at);
CREATE INDEX idx_roles_organization ON roles(organization_id, is_active);
CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);

-- Unique constraints
CREATE UNIQUE INDEX idx_roles_name_org ON roles(name, organization_id) WHERE is_active = true;
CREATE UNIQUE INDEX idx_permissions_resource_action ON permissions(resource, action);
CREATE UNIQUE INDEX idx_user_org_role_unique ON user_organization_roles(user_id, organization_id, role_id) WHERE is_active = true;
```

### Foreign Key Constraints
- All foreign keys have CASCADE DELETE with audit trail preservation
- Soft deletes used for roles and assignments to maintain audit history
- Permission entity uses hard deletes (system-managed)

## Data Integrity Rules

### Role Management
1. Predefined roles cannot be modified or deleted
2. Custom roles cannot be deleted if assigned to users
3. Role name uniqueness enforced per organization
4. Maximum role limits enforced at application level

### Permission Assignment
1. Permissions cannot be assigned to inactive roles
2. Permission hierarchy enforced (ADMIN implies WRITE, WRITE implies READ)
3. Resource-level permissions validated against user's organization access

### User Role Assignment
1. Users can only be assigned roles from organizations they belong to
2. Role assignments automatically expire based on organization policies
3. Maximum roles per user enforced per organization
4. Inactive users cannot receive new role assignments

## Caching Strategy

### Permission Cache Structure
```json
{
  "userId": 123,
  "organizationId": 456,
  "permissions": [
    {"resource": "PAYMENTS", "action": "READ"},
    {"resource": "PAYMENTS", "action": "WRITE"},
    {"resource": "USERS", "action": "READ"}
  ],
  "roles": ["admin", "payment-manager"],
  "cachedAt": "2025-09-27T10:00:00Z",
  "expiresAt": "2025-09-27T10:15:00Z"
}
```

### Cache Invalidation Triggers
- Role permission changes
- User role assignments/removals
- Role activation/deactivation
- User organization membership changes
- Manual cache refresh (admin action)

## Migration Considerations

### From Existing System
1. Existing admin users receive "Owner" role automatically
2. Existing regular users receive "Member" role automatically
3. Organization creators receive "Owner" role
4. Backward compatibility maintained for existing permission checks

### Future Extensibility
1. Support for permission delegation and temporary assignments
2. Role inheritance and hierarchical roles
3. Conditional permissions based on resource attributes
4. Integration with external identity providers

## Performance Characteristics

### Expected Query Patterns
1. **High Frequency**: Permission checks for current user in current organization
2. **Medium Frequency**: Role assignments, user role listings
3. **Low Frequency**: Role creation, permission modifications

### Optimization Strategies
1. Denormalized permission cache for hot paths
2. Batch permission loading for UI components
3. Asynchronous cache warming for organization switches
4. Database connection pooling for permission queries

## Audit and Compliance

### Audit Events
- Role creation, modification, deletion
- Permission grant/revoke operations
- User role assignments/removals
- Permission check denials
- Cache invalidation events

### GDPR Compliance
- User role data included in data export requests
- Role assignments removed on user deletion requests
- Audit trail maintained for compliance reporting
- Anonymization of deleted user references in audit logs

## Security Considerations

### Data Protection
- No sensitive data stored in RBAC entities
- Permission checks enforce tenant isolation
- All queries filtered by organization context
- SQL injection prevention through parameterized queries

### Access Control
- Role management requires organization admin permissions
- System permissions cannot be modified through application
- Privilege escalation prevention through role assignment validation
- Audit logging for all permission-related operations