# CLAUDE.md - Spring Boot Modulith Payment Platform Backend

This file provides specific guidance for Claude Code when working within the **backend** module of the Spring Boot Modulith Payment Platform.

## Project Overview

This is a **Spring Boot 3.2.0 + Java 21** modular monolith payment platform built with **Spring Modulith 1.1.0** architecture pattern. The system implements strict module boundaries with event-driven communication between modules.

## Technology Stack

### Core Framework
- **Java 21** (with modern features like records, pattern matching)
- **Spring Boot 3.2.0** (latest stable)
- **Spring Modulith 1.1.0** (modular monolith architecture)
- **Gradle** (build system)

### Key Dependencies
- **Spring Security** + **OAuth2 Client** (authentication)
- **Spring Data JPA** + **PostgreSQL** (persistence)
- **Spring Session** + **Redis** (session management)
- **Flyway** (database migrations)
- **Stripe Java SDK 24.2.0** (payment processing)
- **TestContainers** (integration testing)
- **ArchUnit** (architecture testing)

### Code Quality Tools
- **Checkstyle** (code style enforcement)
- **JaCoCo** (test coverage)
- **Lombok** (boilerplate reduction)

## Module Architecture

The system is organized into **6 main modules** following Spring Modulith patterns:

```
com.platform/
├── auth/           # OAuth2/PKCE authentication, session management
├── payment/        # Stripe integration, webhook processing
├── user/           # User and organization management
├── subscription/   # Plans, billing cycles, invoicing
├── audit/          # Compliance logging, GDPR compliance
└── shared/         # Common utilities, security, configuration
```

### Module Structure Pattern
Each module follows this consistent structure:
```
module-name/
├── internal/       # Implementation details (not accessible to other modules)
├── api/           # Public interfaces and DTOs for other modules
└── events/        # Event definitions for inter-module communication
```

## Development Guidelines

### 1. Module Boundary Rules (CRITICAL)
- **Never import from `internal/` packages** across modules
- **Only use `api/` and `events/` packages** for cross-module dependencies
- **Use ApplicationEventPublisher** for inter-module communication
- **Run ArchUnit tests** regularly to verify boundaries: `./gradlew test --tests "*ArchitectureTest"`

### 2. Java 21 Best Practices
- **Use records** for immutable data structures (DTOs, events, value objects)
- **Leverage pattern matching** in switch expressions where applicable
- **Use Optional<T>** for nullable returns, never return null
- **Constructor injection only** - avoid field or setter injection
- **Use @NonNull/@Nullable annotations** from Spring

### 3. Testing Strategy (TDD Required)
Execute tests in this priority order:
1. **Contract Tests**: `./gradlew test --tests "*ContractTest"`
2. **Integration Tests**: `./gradlew test --tests "*IntegrationTest"`
3. **E2E Tests**: `./gradlew test --tests "*E2EIntegrationTest"`
4. **Unit Tests**: `./gradlew test --tests "*UnitTest"`

### 4. Database Development
- **Use Flyway migrations** in `src/main/resources/db/migration/`
- **Name migrations**: `V{version}__{description}.sql` (e.g., `V001__create_users_table.sql`)
- **Use H2 for tests**, PostgreSQL for runtime
- **Use TestContainers** for integration tests requiring real databases

### 5. Security Implementation
- **Custom opaque tokens only** (no JWT - security requirement)
- **Store tokens in Redis** with proper TTL and hashing (SHA-256 + salt)
- **Use @PreAuthorize** for method-level security
- **Validate tenant isolation** in all multi-tenant operations
- **Log all security events** through audit module

### 6. Payment Processing
- **Verify Stripe webhook signatures** on all webhook endpoints
- **Use idempotency keys** for all Stripe operations
- **Handle payment failures gracefully** with proper error recovery
- **Log all payment operations** through audit module
- **Test webhook handlers** with Stripe CLI in development

## Key Commands

### Development
```bash
# Run application (test profile with H2)
./gradlew bootRun --args='--spring.profiles.active=test'

# Run all tests with coverage
./gradlew test jacocoTestReport

# Check architecture compliance
./gradlew test --tests "*ArchitectureTest"

# Check code style
./gradlew checkstyleMain checkstyleTest

# Clean and build
./gradlew clean build
```

### Database Management
```bash
# Create new migration
touch src/main/resources/db/migration/V$(date +%03d)__description.sql

# Check migration status (use appropriate profile)
./gradlew flywayInfo -Pflyway-dev
./gradlew flywayInfo -Pflyway-prod
```

### Testing with TestContainers
```bash
# Run integration tests (starts Docker containers)
./gradlew test --tests "*IntegrationTest"

# Run with specific test containers
./gradlew test --tests "*PostgresIntegrationTest"
```

## Module-Specific Guidance

### Auth Module (`com.platform.auth`)
- **OAuth2/PKCE flow implementation** with multiple providers
- **Session management** via Redis with proper cleanup
- **Token validation and refresh** handling
- **Audit all authentication events**

### Payment Module (`com.platform.payment`)
- **Stripe webhook endpoint security** (signature verification)
- **Idempotent payment processing** with retry logic
- **PCI compliance** considerations (no card storage)
- **Payment method management** and customer synchronization

### User Module (`com.platform.user`)
- **Multi-tenant organization management** with proper isolation
- **User invitation system** with role-based access
- **Organization settings** and member management
- **GDPR-compliant user data** handling

### Subscription Module (`com.platform.subscription`)
- **Plan management** and pricing tiers
- **Billing cycle processing** with proration
- **Invoice generation** and payment collection
- **Subscription lifecycle** management (active, canceled, etc.)

### Audit Module (`com.platform.audit`)
- **Compliance logging** with retention policies
- **PII redaction** for GDPR compliance
- **Event correlation** with tracking IDs
- **Structured logging** for observability

### Shared Module (`com.platform.shared`)
- **Common security configurations**
- **Utility classes** and type definitions
- **Cross-cutting concerns** (validation, serialization)
- **Configuration properties** management

## Error Handling Patterns

### API Error Responses
```java
// Use consistent error response format
@ExceptionHandler(ValidationException.class)
public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message(e.getMessage())
            .correlationId(MDC.get("correlationId"))
            .build());
}
```

### Event Publishing Errors
```java
// Use @EventListener with proper error handling
@EventListener
@Async("eventExecutor")
public void handleUserRegistered(UserRegisteredEvent event) {
    try {
        // Process event
    } catch (Exception e) {
        log.error("Failed to process user registration event", e);
        // Publish compensation event if needed
    }
}
```

## Performance Considerations

- **Use @Cacheable** for expensive operations (Redis backing)
- **Implement pagination** for all list endpoints (Spring Data)
- **Use lazy loading** judiciously in JPA relationships
- **Monitor query performance** and use appropriate indexes
- **Use connection pooling** (HikariCP configured)

## Security Checklist

- [ ] **Opaque tokens only** (no JWT implementation)
- [ ] **Tenant isolation** enforced in all queries
- [ ] **Input validation** on all endpoints (@Valid)
- [ ] **Rate limiting** on authentication endpoints
- [ ] **Audit logging** for all security events
- [ ] **HTTPS enforcement** in production
- [ ] **Secure headers** configured (HSTS, CSP, etc.)

## Important File Locations

```
backend/
├── src/main/java/com/platform/           # Source code modules
├── src/main/resources/application.yml    # Main configuration
├── src/main/resources/db/migration/      # Flyway migrations
├── src/test/java/com/platform/          # Test modules
├── config/checkstyle/checkstyle.xml     # Code style rules
└── build.gradle                         # Dependencies and build config
```

## Troubleshooting

### Common Issues
1. **Module boundary violations**: Check ArchUnit test output
2. **Database migration failures**: Verify SQL syntax and existing data
3. **Redis connection issues**: Check Spring Session configuration
4. **Stripe webhook failures**: Verify endpoint signature validation
5. **Test container failures**: Ensure Docker is running and accessible

### Debug Commands
```bash
# Check module dependencies
./gradlew dependencies --configuration compileClasspath

# Analyze test failures
./gradlew test --info --stacktrace

# Check Spring Boot configuration
./gradlew bootRun --debug
```

## Constitutional Compliance

This project follows **strict TDD and architectural principles**:
- **Tests must be written first** (RED-GREEN-Refactor cycle)
- **Module boundaries are non-negotiable** (enforced by ArchUnit)
- **Real dependencies in integration tests** (TestContainers required)
- **GDPR compliance is mandatory** (audit module integration)
- **Security-first approach** (opaque tokens, proper session management)

## Environment Setup

### Required Tools
- **Java 21** (OpenJDK recommended)
- **Docker** (for TestContainers and local Redis/PostgreSQL)
- **Gradle 8.5+** (wrapper included)
- **IDE with Lombok support** (IntelliJ IDEA/Eclipse with plugin)

### Environment Variables
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH=$JAVA_HOME/bin:$PATH
```

---

**Remember**: This is a **production-ready payment platform** with strict security, compliance, and architectural requirements. Always prioritize security, test coverage, and module boundary compliance.