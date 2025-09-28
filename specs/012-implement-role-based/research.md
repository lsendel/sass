# Research: RBAC Implementation for Spring Boot Modulith Platform

## Research Questions from Specification

Based on the feature specification, several clarifications were needed to proceed with implementation design.

## Permission Granularity Level

**Decision**: Implement both resource-level and action-level permissions with hierarchical composition

**Rationale**:
- Resource-level permissions (e.g., "payments", "organizations", "users") provide coarse-grained access control
- Action-level permissions (e.g., "read", "write", "delete", "admin") provide fine-grained control within resources
- Hierarchical composition allows permissions like "payments:read", "organizations:admin", "users:write"
- Aligns with Spring Security's expression-based access control (@PreAuthorize annotations)

**Alternatives considered**:
- Single-level permissions: Too simplistic for enterprise needs
- Custom permission strings: Less maintainable and harder to validate

**Implementation approach**:
```java
public enum Resource { PAYMENTS, ORGANIZATIONS, USERS, SUBSCRIPTIONS, AUDIT }
public enum Action { READ, WRITE, DELETE, ADMIN }
public record Permission(Resource resource, Action action) {}
```

## Multi-Organization User Support

**Decision**: Support users belonging to multiple organizations with organization-scoped roles

**Rationale**:
- Payment platform users often work across multiple organizations (agencies, contractors, consultants)
- Each user-organization relationship should have independent role assignments
- Aligns with existing multi-tenant architecture in the codebase
- Follows industry best practices for B2B SaaS platforms

**Alternatives considered**:
- Single organization per user: Too restrictive for business use cases
- Global roles across organizations: Security risk, violates tenant isolation

**Implementation approach**:
- `UserOrganizationRole` entity linking User, Organization, and Role
- Context-aware permission checks based on current organization
- Organization switching in UI with role context updates

## Default/Predefined Roles

**Decision**: Implement 4 predefined roles with upgradeability to custom roles

**Rationale**:
- Follows common RBAC patterns in enterprise applications
- Provides immediate value without configuration overhead
- Allows gradual adoption of more complex custom roles
- Aligns with existing auth module structure

**Predefined roles**:
1. **Owner** - Full access including organization management and billing
2. **Admin** - Administrative access excluding organization settings and billing
3. **Member** - Standard user access for core features
4. **Viewer** - Read-only access for reporting and monitoring

**Implementation approach**:
- Enum-based predefined roles with fixed permission sets
- Custom roles table for organization-specific role definitions
- Migration path from predefined to custom roles

## Role and User Limits

**Decision**: Implement configurable limits with sensible defaults

**Rationale**:
- Prevents abuse and ensures system performance
- Allows scaling based on organization size and plan
- Provides upgrade paths for enterprise customers
- Follows SaaS platform best practices

**Default limits**:
- Maximum 10 roles per organization (configurable)
- Maximum 5 roles per user per organization (configurable)
- No limit on users per organization (subscription-based)
- Maximum 100 permissions per custom role (configurable)

**Implementation approach**:
```java
@ConfigurationProperties("platform.rbac.limits")
public record RbacLimits(
    int maxRolesPerOrganization,
    int maxRolesPerUser,
    int maxPermissionsPerRole
) {}
```

## User Access on Organization Exit

**Decision**: Immediate access revocation with optional data retention period

**Rationale**:
- Security best practice requires immediate access revocation
- Business needs may require temporary data access for transition
- Compliance requirements for audit trail retention
- Follows industry standards for employee offboarding

**Implementation approach**:
- Immediate role removal and session invalidation on organization exit
- Optional "suspended" status for temporary access removal
- Audit trail preservation for compliance requirements
- Configurable data retention policies

## Spring Security Integration Research

**Decision**: Integrate with Spring Security using custom UserDetails and method-level security

**Rationale**:
- Leverages existing Spring Security infrastructure in the codebase
- Provides standardized security annotations (@PreAuthorize, @Secured)
- Integrates with existing OAuth2/PKCE authentication flow
- Supports fine-grained method-level permission checks

**Implementation approach**:
```java
@PreAuthorize("hasPermission(#organizationId, 'ORGANIZATIONS', 'ADMIN')")
public void deleteOrganization(Long organizationId) { }

@PreAuthorize("hasPermission(#paymentId, 'PAYMENTS', 'READ')")
public Payment getPayment(Long paymentId) { }
```

## Database Schema Research

**Decision**: Use normalized schema with composite indexes for performance

**Rationale**:
- Supports complex queries efficiently
- Maintains referential integrity
- Allows for role hierarchy and inheritance
- Optimized for common permission check patterns

**Key entities**:
- `roles` - Role definitions
- `permissions` - Permission catalog
- `role_permissions` - Many-to-many mapping
- `user_organization_roles` - User role assignments per organization
- Indexes on frequently queried combinations (user_id, organization_id)

## Performance Considerations

**Decision**: Implement caching strategy with Redis for permission lookups

**Rationale**:
- Permission checks occur on every secured endpoint
- User permissions change infrequently but are read frequently
- Redis integration already exists in the platform
- Cache invalidation tied to role/permission changes

**Caching strategy**:
- Cache user permissions per organization context
- TTL of 15 minutes with manual invalidation on changes
- Cache key: `permissions:{userId}:{organizationId}`
- Batch permission loading for UI components

## Module Architecture Integration

**Decision**: Extend existing user module with RBAC capabilities, integrate with auth module

**Rationale**:
- User module already manages organizations and users
- Auth module handles authentication and authorization
- Shared module provides common types and utilities
- Maintains Spring Modulith boundary principles

**Module responsibilities**:
- **User Module**: Role and permission entities, assignment logic
- **Auth Module**: Permission evaluation, Spring Security integration
- **Shared Module**: Common RBAC types and constants
- **Audit Module**: RBAC change logging and compliance

## Testing Strategy Research

**Decision**: Comprehensive testing strategy following TDD principles

**Testing levels**:
1. **Contract Tests**: API endpoint security contracts
2. **Integration Tests**: Database operations with TestContainers
3. **Architecture Tests**: Module boundary enforcement with ArchUnit
4. **E2E Tests**: Complete permission flows with real authentication

**Key test scenarios**:
- Permission inheritance and hierarchy
- Multi-organization role switching
- Concurrent role modifications
- Cache consistency during role changes
- Performance under load with permission checks

## Conclusion

All NEEDS CLARIFICATION items have been resolved with decisions that:
- Align with existing platform architecture
- Follow Spring Security best practices
- Support enterprise-scale requirements
- Maintain constitutional compliance (TDD, module boundaries, real dependencies)
- Provide clear upgrade paths for future enhancements

The research supports implementing a comprehensive RBAC system that integrates seamlessly with the existing Spring Boot Modulith payment platform while providing the flexibility and security required for multi-tenant operations.