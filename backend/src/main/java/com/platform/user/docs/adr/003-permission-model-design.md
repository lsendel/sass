# ADR-003: Resource×Action Permission Model

## Status
**ACCEPTED** - 2024-01-15

## Context

The RBAC system needs a permission model that is:
- **Granular**: Fine-grained control over specific operations
- **Scalable**: Easy to add new resources and actions
- **Intuitive**: Clear meaning for developers and administrators
- **Performant**: Fast permission checking and caching
- **Secure**: Principle of least privilege by default

## Decision

We will implement a **Resource×Action permission model** with the following structure:

### Permission Format
**`RESOURCE:ACTION`** (e.g., `PAYMENTS:WRITE`, `USERS:READ`)

### Core Resources
- **USERS**: User management operations
- **ORGANIZATIONS**: Organization administration
- **PAYMENTS**: Payment processing operations
- **SUBSCRIPTIONS**: Subscription management
- **AUDIT**: Audit log access

### Standard Actions
- **READ**: View/list operations
- **WRITE**: Create/update operations
- **DELETE**: Deletion operations
- **ADMIN**: Administrative operations (full control)

### Permission Examples
```
USERS:READ          # Can view user information
USERS:WRITE         # Can create/update users
USERS:DELETE        # Can delete users
USERS:ADMIN         # Full user management

PAYMENTS:READ       # Can view payment information
PAYMENTS:WRITE      # Can process payments
PAYMENTS:ADMIN      # Payment system administration

ORGANIZATIONS:READ  # Can view organization details
ORGANIZATIONS:ADMIN # Organization administration
```

## Rationale

### Why Resource×Action Model?

1. **Clarity**: Clear separation of what (resource) and how (action)
2. **Granularity**: Fine-grained control without explosion of permissions
3. **Scalability**: Easy to add new resources as the system grows
4. **Consistency**: Standard pattern across all modules
5. **Performance**: Simple string comparisons and efficient caching

### Alternative Models Considered

#### Role-Based Permissions Only
- **Rejected**: Not granular enough for complex business rules
- **Example**: Can't separate "view payments" from "process payments"
- **Impact**: Would require many more roles

#### Hierarchical Permissions
- **Rejected**: Complex inheritance rules and edge cases
- **Example**: Does `PAYMENTS:ADMIN` include `PAYMENTS:READ`?
- **Complexity**: Harder to reason about and implement correctly

#### Attribute-Based Access Control (ABAC)
- **Rejected**: Too complex for current requirements
- **Performance**: Dynamic rule evaluation would violate <200ms requirement
- **Complexity**: Significant implementation and maintenance overhead

#### Custom Permission Strings
- **Rejected**: No standardization across modules
- **Example**: `can_view_payments`, `payment_processor`, `user_manager`
- **Inconsistency**: Harder to understand and manage

## Implementation Details

### Database Schema
```sql
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(resource, action)
);
```

### System Permissions (Auto-populated)
```sql
-- User Management
INSERT INTO permissions (resource, action, description) VALUES
('USERS', 'READ', 'View user information and profiles'),
('USERS', 'WRITE', 'Create and update user accounts'),
('USERS', 'DELETE', 'Delete user accounts'),
('USERS', 'ADMIN', 'Full user management including security settings');

-- Payment Processing
INSERT INTO permissions (resource, action, description) VALUES
('PAYMENTS', 'READ', 'View payment transactions and history'),
('PAYMENTS', 'WRITE', 'Process payments and refunds'),
('PAYMENTS', 'ADMIN', 'Payment system configuration and reconciliation');

-- Organization Management
INSERT INTO permissions (resource, action, description) VALUES
('ORGANIZATIONS', 'READ', 'View organization details and settings'),
('ORGANIZATIONS', 'ADMIN', 'Full organization administration');

-- Subscription Management
INSERT INTO permissions (resource, action, description) VALUES
('SUBSCRIPTIONS', 'READ', 'View subscription information'),
('SUBSCRIPTIONS', 'WRITE', 'Manage subscription plans and billing'),
('SUBSCRIPTIONS', 'ADMIN', 'Subscription system administration');

-- Audit Access
INSERT INTO permissions (resource, action, description) VALUES
('AUDIT', 'READ', 'View audit logs and security events'),
('AUDIT', 'ADMIN', 'Audit system configuration and data retention');
```

### Predefined Roles
```
OWNER (Predefined)
├── All permissions (RESOURCE:ADMIN for all resources)

ADMIN (Predefined)
├── USERS:READ, USERS:WRITE, USERS:DELETE
├── ORGANIZATIONS:READ, ORGANIZATIONS:ADMIN
├── PAYMENTS:READ, PAYMENTS:WRITE
├── SUBSCRIPTIONS:READ, SUBSCRIPTIONS:WRITE
└── AUDIT:READ

MEMBER (Predefined)
├── USERS:READ (own profile only)
├── ORGANIZATIONS:READ
├── PAYMENTS:READ (own payments)
└── SUBSCRIPTIONS:READ (own subscriptions)

VIEWER (Predefined)
├── USERS:READ
├── ORGANIZATIONS:READ
├── PAYMENTS:READ
├── SUBSCRIPTIONS:READ
└── AUDIT:READ
```

### Permission Checking Logic
```java
// Single permission check
boolean hasPermission = permissionApi.hasUserPermission(
    userId, organizationId, "PAYMENTS", "WRITE"
);

// Batch permission check
List<PermissionCheckRequest> requests = List.of(
    new PermissionCheckRequest("PAYMENTS", "READ"),
    new PermissionCheckRequest("USERS", "WRITE")
);
List<PermissionCheckResult> results = permissionApi.checkUserPermissions(
    userId, organizationId, requests
);

// Spring Security integration
@PreAuthorize("hasAuthority('PAYMENTS:WRITE') and @organizationService.hasAccess(authentication.name, #orgId)")
public void processPayment(@PathVariable Long orgId, ...) {
    // Implementation
}
```

## Permission Validation Rules

### Resource Names
- **Format**: UPPERCASE with underscores
- **Examples**: `USERS`, `PAYMENTS`, `ORGANIZATIONS`
- **Pattern**: `^[A-Z_]+$`
- **Max Length**: 50 characters

### Action Names
- **Format**: UPPERCASE with underscores
- **Standard Actions**: `READ`, `WRITE`, `DELETE`, `ADMIN`
- **Custom Actions**: Allowed for specific business needs
- **Pattern**: `^[A-Z_]+$`
- **Max Length**: 50 characters

### Permission Keys
- **Format**: `{RESOURCE}:{ACTION}`
- **Examples**: `PAYMENTS:WRITE`, `USERS:READ`
- **Case Sensitive**: Always uppercase
- **Validation**: Both resource and action must exist

## Security Considerations

### Principle of Least Privilege
- Default roles have minimal necessary permissions
- Custom roles require explicit permission assignment
- No implicit permission inheritance

### Permission Escalation Prevention
- No way to grant permissions you don't have
- Role creation requires `ORGANIZATIONS:ADMIN`
- Permission assignment audited with correlation IDs

### Multi-Tenant Isolation
- All permissions scoped to organizations
- No cross-organization permission leakage
- Database queries include organization filters

## Performance Considerations

### Caching Strategy
- Permission keys cached as Set<String> for O(1) lookup
- Cache key: `user_permissions:{userId}:{organizationId}`
- TTL: 15 minutes with event-driven invalidation

### Database Optimization
- Indexes on (resource, action) for fast lookup
- Composite indexes for user permission queries
- Database functions for optimized permission resolution

### Batch Operations
- Support for checking multiple permissions in single call
- Reduces network round trips and improves performance
- Batch size limited to prevent abuse

## Future Extensibility

### Adding New Resources
1. Add resource to system permissions via migration
2. Create standard actions (READ, WRITE, DELETE, ADMIN)
3. Update predefined roles as needed
4. Document in permission matrix

### Adding Custom Actions
1. Validate business case for custom action
2. Follow naming conventions
3. Update documentation and examples
4. Consider if standard actions would suffice

### Resource Hierarchies (Future)
If needed, could extend to support hierarchical resources:
- `PAYMENTS:CARDS:READ`
- `PAYMENTS:BANK_TRANSFERS:WRITE`
- `ORGANIZATIONS:BILLING:ADMIN`

## Compliance

This permission model supports:
- **Security Requirements**: Granular access control
- **Performance Requirements**: Fast permission checking
- **Auditability**: Clear permission assignment tracking
- **Maintainability**: Consistent and intuitive model

## Related Decisions
- ADR-001: Spring Modulith Architecture
- ADR-002: Event-Driven Cache Management
- ADR-004: Database Function Optimization

## Migration Strategy

### Phase 1: Core Permissions (Current)
- Implement basic Resource×Action model
- 5 resources × 4 actions = 20 base permissions
- 4 predefined roles with standard permission sets

### Phase 2: Extended Permissions (Future)
- Add resource-specific custom actions
- Support for more granular business permissions
- Extended predefined roles

### Phase 3: Advanced Features (Future)
- Conditional permissions based on data attributes
- Time-based permission restrictions
- Resource hierarchies if business case emerges

## Review Date
**Next Review**: 2024-06-15 (5 months)

## Success Metrics
- Permission check performance <200ms (99th percentile)
- Zero privilege escalation incidents
- Developer satisfaction with permission model clarity
- Audit compliance for all permission changes