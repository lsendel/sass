# RBAC Quickstart Guide

## Overview

This quickstart guide demonstrates the complete RBAC implementation for the Spring Boot Modulith Payment Platform. It covers role creation, user assignment, permission enforcement, and testing scenarios.

## Prerequisites

- Running Spring Boot application with authentication
- User authenticated with organization admin privileges
- PostgreSQL and Redis available (via TestContainers in tests)
- Proper Spring Security configuration

## Quick Test Scenarios

### Scenario 1: Organization Admin Creates Custom Role

**Given**: Authenticated organization admin
**When**: Creating a custom role for payment management
**Then**: Role is created with specified permissions and available for assignment

#### API Calls
```bash
# 1. List available permissions
curl -X GET http://localhost:8080/api/permissions \
  -H "Content-Type: application/json" \
  -b "SESSION=valid_session_cookie"

# Expected Response: List of all system permissions
{
  "permissions": [
    {"id": 1, "resource": "PAYMENTS", "action": "READ", "description": "Read payment records"},
    {"id": 2, "resource": "PAYMENTS", "action": "WRITE", "description": "Create and update payments"},
    {"id": 3, "resource": "PAYMENTS", "action": "DELETE", "description": "Delete payment records"},
    {"id": 4, "resource": "USERS", "action": "READ", "description": "View user information"}
  ]
}

# 2. Create custom role with selected permissions
curl -X POST http://localhost:8080/api/organizations/123/roles \
  -H "Content-Type: application/json" \
  -b "SESSION=valid_session_cookie" \
  -d '{
    "name": "payment-manager",
    "description": "Manages payment processing and customer billing",
    "permissionIds": [1, 2, 4]
  }'

# Expected Response: Created role
{
  "id": 5,
  "name": "payment-manager",
  "description": "Manages payment processing and customer billing",
  "organizationId": 123,
  "roleType": "CUSTOM",
  "isActive": true,
  "createdAt": "2025-09-27T10:00:00Z",
  "createdBy": 456
}

# 3. Verify role creation
curl -X GET http://localhost:8080/api/organizations/123/roles/5 \
  -H "Content-Type: application/json" \
  -b "SESSION=valid_session_cookie"

# Expected Response: Role details with permissions
{
  "id": 5,
  "name": "payment-manager",
  "description": "Manages payment processing and customer billing",
  "organizationId": 123,
  "roleType": "CUSTOM",
  "isActive": true,
  "permissions": [
    {"id": 1, "resource": "PAYMENTS", "action": "READ"},
    {"id": 2, "resource": "PAYMENTS", "action": "WRITE"},
    {"id": 4, "resource": "USERS", "action": "READ"}
  ],
  "assignedUserCount": 0
}
```

#### Expected Audit Events
- `ROLE_CREATED` - Custom role created with specified permissions
- `PERMISSIONS_GRANTED` - Permissions assigned to new role

### Scenario 2: Assign Role to Organization User

**Given**: Custom role exists and target user is organization member
**When**: Admin assigns role to user
**Then**: User receives role and gains associated permissions

#### API Calls
```bash
# 1. Assign role to user
curl -X POST http://localhost:8080/api/organizations/123/users/789/roles \
  -H "Content-Type: application/json" \
  -b "SESSION=valid_session_cookie" \
  -d '{
    "roleId": 5,
    "expiresAt": null
  }'

# Expected Response: Role assignment confirmation
{
  "id": 10,
  "userId": 789,
  "organizationId": 123,
  "roleId": 5,
  "role": {
    "id": 5,
    "name": "payment-manager",
    "roleType": "CUSTOM"
  },
  "assignedAt": "2025-09-27T10:15:00Z",
  "assignedBy": 456,
  "expiresAt": null,
  "isActive": true
}

# 2. Verify user permissions
curl -X GET http://localhost:8080/api/organizations/123/users/789/roles \
  -H "Content-Type: application/json" \
  -b "SESSION=valid_session_cookie"

# Expected Response: User's role assignments and effective permissions
{
  "assignments": [
    {
      "id": 10,
      "userId": 789,
      "organizationId": 123,
      "roleId": 5,
      "role": {"id": 5, "name": "payment-manager"},
      "assignedAt": "2025-09-27T10:15:00Z",
      "isActive": true
    }
  ],
  "effectivePermissions": [
    {"resource": "PAYMENTS", "action": "READ"},
    {"resource": "PAYMENTS", "action": "WRITE"},
    {"resource": "USERS", "action": "READ"}
  ]
}
```

#### Expected Audit Events
- `ROLE_ASSIGNED` - Role assigned to user in organization context
- `PERMISSIONS_CACHE_INVALIDATED` - User's permission cache cleared

### Scenario 3: Permission Enforcement Testing

**Given**: User with payment-manager role
**When**: User attempts various actions
**Then**: Access granted/denied based on role permissions

#### API Calls
```bash
# Test as user 789 (with payment-manager role)

# 1. Check multiple permissions at once
curl -X POST http://localhost:8080/api/auth/permissions/check \
  -H "Content-Type: application/json" \
  -b "SESSION=user_789_session" \
  -d '{
    "organizationId": 123,
    "checks": [
      {"resource": "PAYMENTS", "action": "READ"},
      {"resource": "PAYMENTS", "action": "WRITE"},
      {"resource": "PAYMENTS", "action": "DELETE"},
      {"resource": "USERS", "action": "WRITE"}
    ]
  }'

# Expected Response: Permission check results
{
  "results": [
    {"resource": "PAYMENTS", "action": "READ", "granted": true},
    {"resource": "PAYMENTS", "action": "WRITE", "granted": true},
    {"resource": "PAYMENTS", "action": "DELETE", "granted": false, "reason": "Insufficient permissions"},
    {"resource": "USERS", "action": "WRITE", "granted": false, "reason": "Insufficient permissions"}
  ]
}

# 2. Attempt allowed action - should succeed
curl -X GET http://localhost:8080/api/payments/456 \
  -H "Content-Type: application/json" \
  -b "SESSION=user_789_session"

# Expected Response: 200 OK with payment data

# 3. Attempt forbidden action - should fail
curl -X DELETE http://localhost:8080/api/payments/456 \
  -H "Content-Type: application/json" \
  -b "SESSION=user_789_session"

# Expected Response: 403 Forbidden
{
  "code": "RBAC_003",
  "message": "Insufficient permissions to delete payment records",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "timestamp": "2025-09-27T10:30:00Z"
}
```

#### Expected Audit Events
- `PERMISSION_CHECK_GRANTED` - Successful permission checks logged
- `PERMISSION_CHECK_DENIED` - Failed permission attempts logged

### Scenario 4: Role Modification and Real-time Updates

**Given**: Active role with users assigned
**When**: Admin modifies role permissions
**Then**: Changes take effect immediately for all assigned users

#### API Calls
```bash
# 1. Update role to add DELETE permission
curl -X PUT http://localhost:8080/api/organizations/123/roles/5 \
  -H "Content-Type: application/json" \
  -b "SESSION=admin_session" \
  -d '{
    "name": "payment-manager",
    "description": "Manages payment processing and customer billing with delete access",
    "permissionIds": [1, 2, 3, 4]
  }'

# Expected Response: Updated role
{
  "id": 5,
  "name": "payment-manager",
  "description": "Manages payment processing and customer billing with delete access",
  "organizationId": 123,
  "roleType": "CUSTOM",
  "isActive": true,
  "updatedAt": "2025-09-27T10:45:00Z",
  "updatedBy": 456
}

# 2. Verify user now has DELETE permission (as user 789)
curl -X POST http://localhost:8080/api/auth/permissions/check \
  -H "Content-Type: application/json" \
  -b "SESSION=user_789_session" \
  -d '{
    "organizationId": 123,
    "checks": [
      {"resource": "PAYMENTS", "action": "DELETE"}
    ]
  }'

# Expected Response: Permission now granted
{
  "results": [
    {"resource": "PAYMENTS", "action": "DELETE", "granted": true}
  ]
}

# 3. Confirm DELETE action now works
curl -X DELETE http://localhost:8080/api/payments/456 \
  -H "Content-Type: application/json" \
  -b "SESSION=user_789_session"

# Expected Response: 204 No Content (successful deletion)
```

#### Expected Audit Events
- `ROLE_UPDATED` - Role permissions modified
- `PERMISSIONS_CACHE_INVALIDATED` - All users with this role have cache cleared
- `PERMISSION_GRANTED` - New permission granted through role update

### Scenario 5: Multi-Organization User Role Switching

**Given**: User belongs to multiple organizations with different roles
**When**: User switches organization context
**Then**: Permissions update based on roles in new organization

#### API Calls
```bash
# Assume user 789 is member of organization 123 (payment-manager) and 456 (viewer)

# 1. Check permissions in organization 123
curl -X POST http://localhost:8080/api/auth/permissions/check \
  -H "Content-Type: application/json" \
  -H "X-Organization-Context: 123" \
  -b "SESSION=user_789_session" \
  -d '{
    "organizationId": 123,
    "checks": [
      {"resource": "PAYMENTS", "action": "WRITE"}
    ]
  }'

# Expected Response: Write permission granted
{
  "results": [
    {"resource": "PAYMENTS", "action": "WRITE", "granted": true}
  ]
}

# 2. Switch to organization 456 and check same permission
curl -X POST http://localhost:8080/api/auth/permissions/check \
  -H "Content-Type: application/json" \
  -H "X-Organization-Context: 456" \
  -b "SESSION=user_789_session" \
  -d '{
    "organizationId": 456,
    "checks": [
      {"resource": "PAYMENTS", "action": "WRITE"}
    ]
  }'

# Expected Response: Write permission denied (viewer role)
{
  "results": [
    {"resource": "PAYMENTS", "action": "WRITE", "granted": false, "reason": "User has viewer role in this organization"}
  ]
}
```

## Performance Testing Scenarios

### Load Testing Permission Checks

```bash
# Simulate 100 concurrent users checking permissions
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/auth/permissions/check \
    -H "Content-Type: application/json" \
    -b "SESSION=user_${i}_session" \
    -d '{
      "organizationId": 123,
      "checks": [
        {"resource": "PAYMENTS", "action": "READ"},
        {"resource": "USERS", "action": "READ"}
      ]
    }' &
done
wait

# Expected: All requests complete within 200ms
# Expected: Redis cache hit rate >90% after first few requests
```

### Cache Performance Verification

```bash
# 1. First permission check (cache miss)
time curl -X POST http://localhost:8080/api/auth/permissions/check \
  -H "Content-Type: application/json" \
  -b "SESSION=user_789_session" \
  -d '{"organizationId": 123, "checks": [{"resource": "PAYMENTS", "action": "READ"}]}'

# Expected: ~50-100ms (database query)

# 2. Immediate second check (cache hit)
time curl -X POST http://localhost:8080/api/auth/permissions/check \
  -H "Content-Type: application/json" \
  -b "SESSION=user_789_session" \
  -d '{"organizationId": 123, "checks": [{"resource": "PAYMENTS", "action": "READ"}]}'

# Expected: <10ms (Redis cache)
```

## Error Scenarios

### Invalid Role Assignment

```bash
# Attempt to assign non-existent role
curl -X POST http://localhost:8080/api/organizations/123/users/789/roles \
  -H "Content-Type: application/json" \
  -b "SESSION=admin_session" \
  -d '{"roleId": 9999}'

# Expected Response: 404 Not Found
{
  "code": "RBAC_004",
  "message": "Role not found",
  "correlationId": "...",
  "timestamp": "2025-09-27T10:00:00Z"
}
```

### Maximum Roles Exceeded

```bash
# Attempt to assign 6th role (exceeds limit of 5)
curl -X POST http://localhost:8080/api/organizations/123/users/789/roles \
  -H "Content-Type: application/json" \
  -b "SESSION=admin_session" \
  -d '{"roleId": 6}'

# Expected Response: 409 Conflict
{
  "code": "RBAC_005",
  "message": "Maximum roles per user exceeded (5)",
  "correlationId": "...",
  "details": {"currentRoles": 5, "maxAllowed": 5}
}
```

### Insufficient Permissions

```bash
# Non-admin user attempts to create role
curl -X POST http://localhost:8080/api/organizations/123/roles \
  -H "Content-Type: application/json" \
  -b "SESSION=member_user_session" \
  -d '{
    "name": "test-role",
    "description": "Test role",
    "permissionIds": [1]
  }'

# Expected Response: 403 Forbidden
{
  "code": "RBAC_001",
  "message": "Insufficient permissions to create roles",
  "correlationId": "..."
}
```

## Integration Testing

### Database Consistency

```sql
-- Verify role-permission relationships
SELECT r.name, p.resource, p.action
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE r.organization_id = 123;

-- Verify user role assignments
SELECT u.email, r.name, uor.assigned_at, uor.is_active
FROM users u
JOIN user_organization_roles uor ON u.id = uor.user_id
JOIN roles r ON uor.role_id = r.id
WHERE uor.organization_id = 123;
```

### Cache Consistency

```bash
# Verify Redis cache contents
redis-cli KEYS "permissions:*"
redis-cli GET "permissions:789:123"

# Expected: JSON with user's permissions and expiration timestamp
```

## Success Criteria Validation

### Functional Requirements Verification

- ✅ **FR-001**: Custom roles created with specific permissions
- ✅ **FR-002**: Roles assigned/removed from users successfully
- ✅ **FR-003**: Permission checks enforced on all endpoints
- ✅ **FR-004**: Hierarchical permissions working (ADMIN implies WRITE implies READ)
- ✅ **FR-005**: Role permissions viewable in API responses
- ✅ **FR-006**: User roles viewable with effective permissions
- ✅ **FR-007**: All changes logged to audit system
- ✅ **FR-008**: Users cannot escalate own privileges
- ✅ **FR-009**: Permission changes take effect immediately

### Performance Requirements

- ✅ Permission checks complete <200ms (cached: <10ms, uncached: <100ms)
- ✅ Role assignment operations complete <500ms
- ✅ Cache hit rate >90% for repeated permission checks
- ✅ System supports 1000+ concurrent permission checks

### Security Requirements

- ✅ Tenant isolation enforced (users only see own organization's roles)
- ✅ Permission escalation prevented
- ✅ All RBAC operations require appropriate admin permissions
- ✅ Audit trail maintained for all permission-related actions
- ✅ Session-based authentication required for all endpoints

## Troubleshooting

### Common Issues

1. **Permissions not updating immediately**
   - Check Redis connectivity
   - Verify cache invalidation events in audit logs
   - Manually clear cache: `redis-cli DEL "permissions:userId:orgId"`

2. **Role assignment failing**
   - Verify user is member of organization
   - Check role limits configuration
   - Ensure role is active and exists

3. **Permission checks always failing**
   - Verify organization context in request headers
   - Check user's role assignments in database
   - Validate Spring Security configuration

### Debug Commands

```bash
# Check user's cached permissions
redis-cli GET "permissions:789:123"

# View audit logs for user
curl -X GET "http://localhost:8080/api/audit/events?userId=789&category=RBAC" \
  -b "SESSION=admin_session"

# Force permission cache refresh
curl -X POST "http://localhost:8080/api/auth/permissions/refresh" \
  -H "Content-Type: application/json" \
  -b "SESSION=user_session" \
  -d '{"organizationId": 123}'
```

This quickstart demonstrates the complete RBAC system functionality, from role creation through permission enforcement, with real API examples and expected responses for validation.