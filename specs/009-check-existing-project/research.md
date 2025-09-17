# Research: Alternative Password Authentication

**Date**: 2025-09-16
**Feature**: Password Authentication alongside OAuth2

## Decisions Made

### 1. Password Hashing Algorithm
**Decision**: Use BCrypt with strength 12
**Rationale**: Already configured in the project (`BCryptPasswordEncoder(12)`), Spring Security native support, mature and well-tested, optimal work factor for 2024
**Alternatives considered**:
- Argon2: Would require additional dependencies, marginal security improvement not worth the complexity
- PBKDF2: Less resistant to GPU attacks than BCrypt
- Scrypt: More memory-intensive but not significantly better than BCrypt for this use case

### 2. Configuration Approach
**Decision**: Use Spring Boot application.yml with environment variable overrides
**Rationale**: Project already uses this pattern consistently, supports profile-based configuration, secure externalization of sensitive values
**Alternatives considered**:
- Pure .env files: Less flexible, doesn't support profiles
- Java-based configuration only: Harder to change without recompilation
- Kubernetes ConfigMaps: Over-engineered for current deployment model

### 3. Authentication Flow Architecture
**Decision**: Extend existing OAuth2 infrastructure with conditional password endpoints
**Rationale**: Maintains consistency with existing patterns, reuses opaque token infrastructure, follows Spring Modulith module boundaries
**Alternatives considered**:
- Separate authentication module: Would violate existing module structure
- Replace OAuth2: Would break existing functionality
- JWT-based tokens: Violates constitutional requirement for opaque tokens

### 4. Session Management
**Decision**: Reuse existing Redis-backed session storage with opaque tokens
**Rationale**: Already implemented and tested, supports multi-tenancy, provides audit trail, 24-hour timeout already configured
**Alternatives considered**:
- Database sessions: Slower, already have Redis infrastructure
- In-memory sessions: Not suitable for production/clustering
- Stateless JWT: Constitutional violation (opaque tokens required)

### 5. Password Complexity Requirements
**Decision**: Configurable via environment variables with sensible defaults
**Rationale**: Different environments may have different requirements, compliance needs vary, easy to adjust without code changes
**Alternatives considered**:
- Hard-coded requirements: Too inflexible
- Database-stored configuration: Over-complicated for simple boolean/numeric values
- No requirements: Security risk

### 6. Account Lockout Strategy
**Decision**: Exponential backoff with Redis-backed attempt tracking
**Rationale**: Prevents brute force, doesn't require database changes, can leverage existing Redis infrastructure
**Alternatives considered**:
- Fixed lockout period: Less sophisticated, easier to game
- Database-tracked attempts: Slower, more complex
- No lockout: Security vulnerability

### 7. Email Verification Approach
**Decision**: Token-based verification with 72-hour expiry
**Rationale**: Industry standard, secure, simple to implement, aligns with password reset pattern
**Alternatives considered**:
- Magic links: More complex, requires additional state management
- SMS verification: Expensive, requires additional infrastructure
- No verification: Security and compliance risk

### 8. Frontend Integration
**Decision**: Conditional rendering based on feature flag in Redux state
**Rationale**: Maintains existing React/Redux patterns, easy to toggle, type-safe with TypeScript
**Alternatives considered**:
- Separate login pages: More code duplication
- Server-side rendering: Would require architectural changes
- Feature flags service: Over-engineered for single flag

## Existing Project Structure Analysis

### Current Authentication Components
- **Backend**: `/backend/src/main/java/com/platform/auth/`
  - OAuth2Controller, AuthController (API layer)
  - OAuth2SessionService, SessionService (Internal services)
  - OpaqueTokenStore (Token management)
  - Full audit logging integration

### Security Configuration
- BCryptPasswordEncoder(12) already configured
- Spring Security with OAuth2 support
- CORS properly configured
- Security headers (HSTS, Content-Type protection)
- CSRF disabled for stateless API

### Test Structure (TDD-Compliant)
- Contract tests for API validation
- Integration tests with TestContainers
- Unit tests for business logic
- Follows constitutional test order requirement

### Environment Configuration Pattern
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:}
```

## Implementation Constraints Resolved

### Password Reset Flow
**Resolution**: Implement secure token-based reset with email verification
- Generate cryptographically secure tokens
- Store in database with expiry timestamp
- Send reset link via email
- Validate token and allow password change
- Audit all password reset attempts

### Multi-Account Handling
**Resolution**: Link accounts by email address
- Users can have both OAuth and password credentials
- Same email = same user account
- Authentication method stored in session
- User can link/unlink authentication methods

### Rate Limiting Implementation
**Resolution**: Use Spring Security's built-in rate limiting with Redis
- Track failed attempts per IP and username
- Exponential backoff: 1, 2, 4, 8, 16, 32 minutes
- Reset counter on successful login
- Maximum lockout period: 24 hours
- Admin override capability for support

### Audit Requirements
**Resolution**: Extend existing audit system
- Log all authentication attempts (success and failure)
- Include authentication method in audit trail
- Track password changes and resets
- Maintain GDPR compliance with PII redaction
- Integrate with existing audit module

## Best Practices to Follow

1. **TDD Compliance**: Write tests in constitutional order (Contract → Integration → E2E → Unit)
2. **Module Boundaries**: Keep password auth within existing auth module
3. **Opaque Tokens**: No JWT implementation (constitutional requirement)
4. **Real Dependencies**: Use TestContainers for integration tests
5. **Library-First**: Create CLI interface for password management operations
6. **Structured Logging**: Include correlation IDs and context
7. **Multi-tenancy**: Respect organization-based isolation
8. **Security Headers**: Maintain existing security posture
9. **Error Messages**: Don't leak information about account existence
10. **Timing Attacks**: Use constant-time comparison for passwords

## Performance Considerations

- BCrypt with strength 12: ~250ms per hash (acceptable for auth)
- Redis session lookups: <5ms average
- Database queries optimized with proper indexes
- Connection pooling already configured
- Async email sending to avoid blocking

## Compliance Requirements

- GDPR: PII redaction in logs, right to deletion
- OWASP: Follow authentication best practices
- PCI DSS: Not storing credit cards, but maintain security standards
- SOC2: Audit trail and access controls
- Password storage: Never log or expose passwords

## Migration Strategy

Since this is a new feature with no existing password users:
1. Database migration to add password columns
2. No data migration needed
3. Feature flag allows gradual rollout
4. Backward compatible with OAuth-only flow
5. No breaking changes to existing APIs

## Monitoring and Observability

- Metrics: Login success/failure rates by method
- Alerts: Unusual login patterns, brute force attempts
- Dashboards: Authentication method distribution
- Logs: Structured with correlation IDs
- Tracing: Full request flow visibility

## Documentation Requirements

- API documentation via OpenAPI
- Developer guide for authentication flows
- Operations runbook for troubleshooting
- Security documentation for auditors
- User guide for password management

---

All technical clarifications have been resolved based on project analysis and industry best practices.