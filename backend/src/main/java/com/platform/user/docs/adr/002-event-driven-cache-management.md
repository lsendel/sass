# ADR-002: Event-Driven Cache Management

## Status
**ACCEPTED** - 2024-01-15

## Context

The RBAC system requires aggressive caching to meet the <200ms performance requirement for permission checks. However, maintaining cache consistency across role and permission changes is critical for security. Cache invalidation must be immediate and reliable to prevent stale permission data.

## Decision

We will implement **event-driven cache management** using Spring's event publishing mechanism with the following approach:

### Cache Strategy
1. **Redis-based caching** with 15-minute TTL for permission data
2. **Immediate cache invalidation** triggered by role/permission change events
3. **Intelligent invalidation patterns** based on event context
4. **Pattern-based cache clearing** for affected user permissions

### Event-Driven Invalidation
- All role/permission changes publish domain events
- `UserRoleCacheManager` listens to events and invalidates affected caches
- Different invalidation strategies based on event types and impact

## Implementation Details

### Cache Keys
```
user_permissions:{userId}:{organizationId}
role_permissions:{roleId}
organization_roles:{organizationId}
user_roles:{userId}
```

### Invalidation Patterns
- **User Role Assignment**: Invalidate user-specific caches
- **Role Permission Changes**: Invalidate role caches + all users with that role
- **High-Privilege Changes**: Broader invalidation for security
- **Organization Changes**: Clear organization-wide caches

### Event Handlers
```java
@EventListener
@Async("eventExecutor")
public void handleUserRoleAssigned(UserRoleAssignedEvent event) {
    invalidateUserCaches(event.userId(), event.organizationId());
    if (event.isHighPrivilegeRole()) {
        invalidateOrganizationCaches(event.organizationId());
    }
}
```

## Rationale

### Why Event-Driven?

1. **Immediate Consistency**: Cache invalidation happens immediately when data changes
2. **Decoupled Design**: Cache management is separate from business logic
3. **Selective Invalidation**: Only invalidate affected cache entries
4. **Audit Trail**: Events provide traceability of cache operations

### Alternative Approaches Considered

#### Time-Based Cache Expiration Only
- **Rejected**: Could allow up to 15 minutes of stale data
- **Security Risk**: Users might retain permissions after removal
- **Performance Impact**: Cache misses would increase

#### Synchronous Cache Invalidation
- **Rejected**: Would block transaction processing
- **Performance Impact**: Violates response time requirements
- **Reliability Issues**: Cache failures could break business operations

#### Database Cache Invalidation Triggers
- **Rejected**: Tight coupling between database and cache layer
- **Complexity**: Database triggers are harder to test and maintain
- **Portability**: Reduces database independence

#### Manual Cache Invalidation
- **Rejected**: Error-prone and requires developers to remember
- **Maintenance Burden**: Easy to forget in new features
- **Consistency Risk**: Likely to cause cache inconsistencies

## Implementation Strategy

### Cache Invalidation Levels

1. **Targeted Invalidation** (Most Common)
   - Single user permission caches
   - Specific role permission caches
   - Minimal performance impact

2. **Selective Invalidation** (Role Changes)
   - All users with a specific role
   - Role-specific caches
   - Moderate performance impact

3. **Broad Invalidation** (High-Privilege Changes)
   - Organization-wide user caches
   - Security-sensitive operations
   - Higher performance impact but rare

### Event Processing

```java
@Component
@Async("eventExecutor")
class UserRoleCacheManager {

    @EventListener
    public void handleRoleModified(RoleModifiedEvent event) {
        if (event.hasPermissionChanges()) {
            invalidateUsersWithRole(event.roleId(), event.organizationId());
        }
        if (event.hasHighPrivilegePermissionsAdded()) {
            auditHighPrivilegeChange(event);
            invalidateOrganizationCaches(event.organizationId());
        }
    }
}
```

### Monitoring and Alerting

- **Cache Hit Ratio**: Monitor for unexpected drops
- **Invalidation Rate**: Alert on unusual invalidation spikes
- **Event Processing Time**: Ensure async processing performs well
- **Cache Consistency**: Periodic validation jobs

## Consequences

### Positive
- **Security**: Immediate invalidation prevents stale permission data
- **Performance**: Selective invalidation minimizes cache misses
- **Maintainability**: Centralized cache management logic
- **Auditability**: Event trail for debugging cache issues

### Negative
- **Complexity**: Additional event handling and async processing
- **Eventual Consistency**: Brief window where caches might be inconsistent
- **Dependencies**: Requires Redis and event infrastructure

### Risk Mitigation
- **Circuit Breaker**: Fallback to database if cache fails
- **Event Reliability**: Dead letter queues for failed events
- **Monitoring**: Comprehensive cache and event monitoring
- **Testing**: Integration tests with cache verification

## Performance Impact

### Cache Hit Ratios (Target)
- User permissions: >95%
- Role permissions: >90%
- Organization roles: >85%

### Invalidation Performance
- Targeted invalidation: <10ms
- Selective invalidation: <50ms
- Broad invalidation: <200ms

### Event Processing
- Async processing: No impact on request latency
- Event throughput: >1000 events/second
- Event lag: <100ms

## Compliance

This decision supports:
- **Performance Requirements**: <200ms permission checks maintained
- **Security Requirements**: Immediate permission revocation
- **Reliability Requirements**: Fallback mechanisms and monitoring
- **Maintainability**: Clear separation of concerns

## Related Decisions
- ADR-001: Spring Modulith Architecture
- ADR-003: Permission Model Design
- ADR-005: Redis Configuration and Deployment

## Validation

### Success Criteria
- [ ] Cache hit ratio >90% in production
- [ ] Permission check latency <200ms
- [ ] Zero stale permission incidents
- [ ] Event processing <100ms lag

### Testing Strategy
- Integration tests with cache verification
- Performance tests under load
- Chaos engineering for cache failures
- Security tests for permission revocation timing

## Review Date
**Next Review**: 2024-04-15 (3 months)