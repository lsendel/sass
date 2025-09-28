# User Module - Role-Based Access Control (RBAC)

## Overview

The User module implements a comprehensive Role-Based Access Control (RBAC) system for the Spring Boot Modulith payment platform. It provides multi-tenant permission management with organization isolation, custom role creation, and real-time permission checking with sub-200ms performance requirements.

## Architecture

### Module Structure

Following Spring Modulith principles, this module maintains strict package boundaries:

```
com.platform.user/
├── api/                    # Public interfaces for external module access
│   ├── PermissionApi       # Permission checking and validation
│   ├── RoleApi            # Role information and management
│   ├── UserRoleApi        # User role assignment operations
│   └── *Controller        # REST endpoints
├── internal/               # Implementation details (not accessible externally)
│   ├── entities/          # JPA entities
│   ├── repositories/      # Data access layer
│   ├── services/          # Business logic
│   └── *ApiImpl          # API implementations
└── events/                # Event definitions for inter-module communication
    ├── UserRoleAssignedEvent
    ├── UserRoleRemovedEvent
    ├── RoleCreatedEvent
    ├── RoleModifiedEvent
    └── RoleDeletedEvent
```

### Core Entities

- **Permission**: System-level permissions with Resource×Action model
- **Role**: Organization-scoped roles (predefined & custom)
- **UserRole**: User role assignments with expiration support
- **RolePermission**: Role-permission relationship mapping

### Performance Features

- **Redis Caching**: 15-minute TTL with intelligent invalidation
- **Database Functions**: Optimized permission lookup queries
- **Event-Driven Cache Management**: Automatic cache coherence
- **Batch Operations**: Efficient multi-permission validation

## Quick Start

### Prerequisites

- Java 21+
- Spring Boot 3.5.6
- PostgreSQL 15+
- Redis 7+
- Docker (for TestContainers)

### Basic Usage

#### 1. Check User Permissions

```java
@Autowired
private PermissionApi permissionApi;

// Check single permission
boolean hasAccess = permissionApi.hasUserPermission(
    userId, organizationId, "PAYMENTS", "WRITE"
);

// Check multiple permissions
List<PermissionCheckRequest> requests = List.of(
    new PermissionCheckRequest("PAYMENTS", "READ"),
    new PermissionCheckRequest("USERS", "WRITE")
);
List<PermissionCheckResult> results = permissionApi.checkUserPermissions(
    userId, organizationId, requests
);
```

#### 2. Manage Roles

```java
@Autowired
private RoleApi roleApi;

// Get organization roles
List<RoleInfo> roles = roleApi.getRolesByOrganization(organizationId);

// Get role details with permissions
Optional<RoleDetails> roleDetails = roleApi.getRoleDetails(roleId, organizationId);
```

#### 3. Assign Roles to Users

```java
@Autowired
private UserRoleApi userRoleApi;

// Assign permanent role
UserRoleAssignment assignment = userRoleApi.assignRoleToUser(
    userId, roleId, organizationId, assignedBy, null
);

// Assign temporary role (expires in 30 days)
LocalDateTime expiration = LocalDateTime.now().plusDays(30);
UserRoleAssignment tempAssignment = userRoleApi.assignRoleToUser(
    userId, roleId, organizationId, assignedBy, expiration
);
```

## API Documentation

### REST Endpoints

#### Permission Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/permissions` | List all system permissions | `ORGANIZATIONS:READ` |
| GET | `/api/permissions/resources` | Get all permission resources | `ORGANIZATIONS:READ` |
| POST | `/api/organizations/{orgId}/permissions/check` | Batch permission checking | `USERS:READ` |
| GET | `/api/organizations/{orgId}/permissions/my-permissions` | Get current user permissions | Organization access |

#### Role Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/organizations/{orgId}/roles` | List organization roles | `ORGANIZATIONS:READ` |
| GET | `/api/organizations/{orgId}/roles/{roleId}` | Get role details | `ORGANIZATIONS:READ` |
| POST | `/api/organizations/{orgId}/roles` | Create custom role | `ORGANIZATIONS:ADMIN` |
| PUT | `/api/organizations/{orgId}/roles/{roleId}` | Update custom role | `ORGANIZATIONS:ADMIN` |
| DELETE | `/api/organizations/{orgId}/roles/{roleId}` | Delete custom role | `ORGANIZATIONS:ADMIN` |

#### User Role Assignment

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/organizations/{orgId}/users/{userId}/roles` | Get user role assignments | `USERS:READ` |
| POST | `/api/organizations/{orgId}/users/{userId}/roles` | Assign role to user | `USERS:WRITE` |
| DELETE | `/api/organizations/{orgId}/users/{userId}/roles/{roleId}` | Remove user role | `USERS:WRITE` |
| PUT | `/api/organizations/{orgId}/users/{userId}/roles/{roleId}/extend` | Extend role assignment | `USERS:WRITE` |

### Request/Response Examples

#### Create Custom Role

**Request:**
```json
POST /api/organizations/1/roles
{
  "name": "payment-manager",
  "description": "Manages payment processing and customer billing",
  "permissionIds": [1, 2, 5]
}
```

**Response:**
```json
{
  "status": "Success",
  "roleName": "payment-manager",
  "message": "Role created with 3 permissions"
}
```

#### Check User Permissions

**Request:**
```json
POST /api/organizations/1/permissions/check
{
  "permissions": [
    {"resource": "PAYMENTS", "action": "READ"},
    {"resource": "USERS", "action": "WRITE"}
  ]
}
```

**Response:**
```json
{
  "results": [
    {"resource": "PAYMENTS", "action": "READ", "hasPermission": true},
    {"resource": "USERS", "action": "WRITE", "hasPermission": false}
  ]
}
```

## Configuration

### Application Properties

```yaml
# RBAC Configuration
rbac:
  roles:
    max-custom-per-organization: 50
    default-expiration-days: 90
  permissions:
    cache-ttl-minutes: 15
    batch-check-limit: 100
  cache:
    user-permissions-ttl: 900
    role-permissions-ttl: 1800
  audit:
    log-permission-checks: true
    log-role-changes: true
    retention-days: 365

# Cache Configuration
spring:
  cache:
    type: redis
    redis:
      time-to-live: 900s
      key-prefix: "rbac:"
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `RBAC_MAX_ROLES_PER_ORG` | Maximum custom roles per organization | 50 |
| `RBAC_PERMISSION_CACHE_TTL` | Permission cache TTL in seconds | 900 |
| `RBAC_AUDIT_ENABLED` | Enable audit logging | true |
| `RBAC_PERFORMANCE_MONITORING` | Enable performance monitoring | true |

## Database Schema

### Tables

#### permissions
```sql
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(resource, action)
);
```

#### roles
```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    role_type VARCHAR(20) NOT NULL CHECK (role_type IN ('PREDEFINED', 'CUSTOM')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT,
    UNIQUE(organization_id, name, role_type)
);
```

#### user_roles
```sql
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    assigned_by BIGINT NOT NULL,
    expires_at TIMESTAMP,
    removed_at TIMESTAMP WITH TIME ZONE,
    removed_by BIGINT,
    UNIQUE(user_id, role_id)
);
```

#### role_permissions
```sql
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(role_id, permission_id)
);
```

### Database Functions

#### get_user_permissions
```sql
CREATE OR REPLACE FUNCTION get_user_permissions(p_user_id BIGINT, p_organization_id BIGINT)
RETURNS TABLE(resource VARCHAR, action VARCHAR) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT p.resource, p.action
    FROM permissions p
    JOIN role_permissions rp ON p.id = rp.permission_id
    JOIN roles r ON rp.role_id = r.id
    JOIN user_roles ur ON r.id = ur.role_id
    WHERE ur.user_id = p_user_id
      AND r.organization_id = p_organization_id
      AND r.is_active = true
      AND p.is_active = true
      AND ur.removed_at IS NULL
      AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql;
```

## Events

### Published Events

The module publishes the following events for cross-module integration:

#### UserRoleAssignedEvent
- **When**: Role is assigned to a user
- **Consumers**: Audit module, notification service, cache manager
- **Data**: userId, roleId, organizationId, assignedBy, expiresAt

#### UserRoleRemovedEvent
- **When**: Role is removed from a user
- **Consumers**: Audit module, notification service, cache manager
- **Data**: userId, roleId, organizationId, removedBy, reason

#### RoleCreatedEvent
- **When**: Custom role is created
- **Consumers**: Audit module, analytics service
- **Data**: roleId, organizationId, permissions, createdBy

### Event Listeners

#### Cache Management
- **UserRoleCacheManager**: Handles cache invalidation based on role events
- **Performance Impact**: Ensures cache coherence with <10ms invalidation

## Security

### Authorization Model

- **Organization Isolation**: All operations scoped to organizations
- **Permission-Based**: Uses granular Resource×Action permissions
- **Role Hierarchy**: Predefined roles (Owner, Admin, Member, Viewer) + Custom roles
- **Temporal Access**: Support for expiring role assignments

### Security Controls

1. **Input Validation**: Jakarta Bean Validation on all endpoints
2. **Authorization**: Spring Security `@PreAuthorize` annotations
3. **Audit Logging**: All permission changes logged with correlation IDs
4. **Rate Limiting**: Configurable rate limits on sensitive endpoints
5. **Data Protection**: No sensitive data in cache keys or logs

### Best Practices

- **Principle of Least Privilege**: Default to minimal permissions
- **Regular Audit**: Review role assignments and permissions quarterly
- **Temporary Access**: Use expiring assignments for temporary needs
- **Monitor High-Privilege**: Alert on admin role assignments
- **Cache Security**: Encrypt cache data in production

## Testing

### Test Categories

1. **Unit Tests**: Service layer business logic
2. **Contract Tests**: API interface validation
3. **Integration Tests**: End-to-end scenarios with TestContainers
4. **Architecture Tests**: Module boundary enforcement with ArchUnit
5. **Performance Tests**: Sub-200ms permission checking validation

### Running Tests

```bash
# All tests
./gradlew test

# Contract tests only
./gradlew test --tests "*ContractTest"

# Integration tests only
./gradlew test --tests "*IntegrationTest"

# Architecture compliance tests
./gradlew test --tests "*ArchitectureTest"

# Performance tests
./gradlew test --tests "*PerformanceTest"
```

### Test Data

- **Test Containers**: PostgreSQL 15-alpine, Redis 7-alpine
- **Test Profiles**: `application-test.yml` with H2 for unit tests
- **Fixtures**: Predefined roles and permissions loaded via migrations

## Monitoring

### Key Metrics

- **Permission Check Latency**: Target <200ms, Alert >500ms
- **Cache Hit Ratio**: Target >90%, Alert <80%
- **Role Assignment Rate**: Monitor unusual spikes
- **Failed Authorization Rate**: Alert on increasing failures

### Health Checks

- **Database Connectivity**: Role and permission table access
- **Cache Connectivity**: Redis connection and basic operations
- **Permission Function**: Database function execution time

### Dashboards

- **Performance Dashboard**: Response times, cache metrics
- **Security Dashboard**: Failed authorizations, role changes
- **Usage Dashboard**: Active roles, permission distributions

## Troubleshooting

### Common Issues

#### Slow Permission Checks
1. Check cache hit ratio in Redis
2. Verify database function performance
3. Check for expired cache entries
4. Review database connection pool

#### Cache Inconsistency
1. Verify event publishing is working
2. Check cache invalidation listeners
3. Review correlation IDs in logs
4. Manually clear affected cache entries

#### Role Assignment Failures
1. Check role limits configuration
2. Verify user exists in organization
3. Check for duplicate assignments
4. Review audit logs for errors

### Debug Mode

Enable debug logging:
```yaml
logging:
  level:
    com.platform.user: DEBUG
    org.springframework.cache: DEBUG
    org.springframework.security: DEBUG
```

### Performance Tuning

1. **Database Optimization**
   - Review index usage
   - Analyze query execution plans
   - Consider read replicas for heavy loads

2. **Cache Optimization**
   - Adjust TTL based on usage patterns
   - Monitor memory usage
   - Consider cache warming strategies

3. **Application Optimization**
   - Profile permission checking code
   - Optimize batch operations
   - Review event publishing performance

## Contributing

### Development Setup

1. **Prerequisites**: Java 21, Docker, PostgreSQL, Redis
2. **Database**: Run migrations with `./gradlew flywayMigrate`
3. **Tests**: Ensure all tests pass before commits
4. **Architecture**: Validate with `./gradlew test --tests "*ArchitectureTest"`

### Code Standards

- **Module Boundaries**: Never import from `internal/` packages externally
- **API Design**: Use records for immutable DTOs
- **Event Design**: Include correlation IDs for tracing
- **Cache Design**: Consider invalidation strategy for all cached data
- **Security**: Use `@PreAuthorize` on all endpoints

### Pull Request Process

1. **Architecture Tests**: Must pass ArchUnit validations
2. **Integration Tests**: All scenarios must pass with TestContainers
3. **Performance Tests**: Permission checks must be <200ms
4. **Documentation**: Update relevant documentation
5. **Security Review**: Required for permission or role changes

## License

Copyright (c) 2024 Platform Team. All rights reserved.