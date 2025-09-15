# Module Boundaries Context

This context defines the modular architecture boundaries for the Spring Boot Modulith payment platform and provides guidance for agents working with inter-module communication.

## Spring Boot Modulith Architecture Overview

### Module Organization
The platform follows a modular monolith pattern with clear boundaries enforced by Spring Modulith and ArchUnit:

```
backend/src/main/java/com/platform/
├── auth/           # OAuth2/PKCE, session management
├── payment/        # Stripe integration, webhooks
├── user/           # User/organization management
├── subscription/   # Plans, billing cycles
├── audit/          # Compliance logging
└── shared/         # Common utilities, security
```

### Module Boundary Rules

#### 1. Public API Packages
Each module exposes functionality only through designated API packages:

```
[module]/
├── api/                 # PUBLIC: External interface
│   ├── events/         # Domain events published by module
│   ├── dto/            # Data transfer objects
│   └── facade/         # Service facades
├── internal/           # PRIVATE: Implementation details
│   ├── domain/         # Domain models
│   ├── repository/     # Data access
│   └── service/        # Business logic
└── config/             # PACKAGE: Configuration classes
```

#### 2. Communication Patterns
**REQUIRED**: Inter-module communication via ApplicationEventPublisher
**FORBIDDEN**: Direct service calls between modules

```java
// ✅ CORRECT: Event-driven communication
@EventListener
public void handleUserCreated(UserCreatedEvent event) {
    subscriptionService.createTrialSubscription(event.getUserId());
}

// ❌ FORBIDDEN: Direct service injection
@Autowired
private UserService userService; // Cross-module dependency
```

#### 3. Dependency Direction
Modules may only depend on:
- Their own internal packages
- Shared module utilities
- External libraries
- Published events from other modules

**Dependency Flow**:
```
shared ← [auth, payment, user, subscription, audit]
```

## Module Specifications

### Auth Module (`auth/`)
**Purpose**: OAuth2/PKCE authentication and session management

**Public API**:
- `auth/api/facade/AuthenticationFacade`
- `auth/api/events/UserAuthenticatedEvent`
- `auth/api/events/SessionExpiredEvent`

**Responsibilities**:
- OAuth2/PKCE flow management
- Opaque token generation and validation
- Redis session storage
- Authentication provider integration

**Constitutional Compliance**:
- Opaque tokens only (no JWT)
- Structured logging for security events
- Integration tests with real OAuth providers

**Agent Guidance**:
- Authentication Module Agent enforces opaque token requirements
- OAuth Security Agent validates PKCE implementation
- Integration Testing Agent ensures real provider testing

### Payment Module (`payment/`)
**Purpose**: Stripe integration and payment processing

**Public API**:
- `payment/api/facade/PaymentFacade`
- `payment/api/events/PaymentProcessedEvent`
- `payment/api/events/PaymentFailedEvent`
- `payment/api/events/WebhookReceivedEvent`

**Responsibilities**:
- Stripe payment method management
- Webhook signature validation
- Payment retry logic
- PCI compliance enforcement

**Constitutional Compliance**:
- Real Stripe integration in tests
- Webhook idempotency patterns
- Comprehensive audit logging

**Agent Guidance**:
- Payment Processing Agent ensures PCI compliance
- Payment Security Agent validates webhook security
- Integration Testing Agent mandates real Stripe testing

### User Module (`user/`)
**Purpose**: User and organization management

**Public API**:
- `user/api/facade/UserFacade`
- `user/api/facade/OrganizationFacade`
- `user/api/events/UserCreatedEvent`
- `user/api/events/OrganizationCreatedEvent`

**Responsibilities**:
- User profile management
- Organization multi-tenancy
- User invitation workflows
- Profile data validation

**Constitutional Compliance**:
- GDPR compliance with PII redaction
- Event-driven user lifecycle
- Real database integration testing

**Agent Guidance**:
- User Management Agent enforces GDPR compliance
- GDPR Compliance Agent validates PII handling
- Integration Testing Agent ensures multi-tenant isolation

### Subscription Module (`subscription/`)
**Purpose**: Subscription plans and billing management

**Public API**:
- `subscription/api/facade/SubscriptionFacade`
- `subscription/api/events/SubscriptionCreatedEvent`
- `subscription/api/events/SubscriptionCancelledEvent`
- `subscription/api/events/BillingCycleEvent`

**Responsibilities**:
- Plan configuration and management
- Trial period handling
- Usage tracking and billing
- Subscription lifecycle automation

**Constitutional Compliance**:
- Event-driven billing cycles
- Real Stripe subscription integration
- Usage metric observability

**Agent Guidance**:
- Subscription Management Agent coordinates billing logic
- Payment Processing Agent handles billing integration
- Observability Agent ensures usage tracking

### Audit Module (`audit/`)
**Purpose**: Compliance logging and audit trail

**Public API**:
- `audit/api/facade/AuditFacade`
- `audit/api/events/AuditEventPublished`

**Responsibilities**:
- GDPR compliance audit trails
- Security event logging
- Data retention policy enforcement
- Compliance report generation

**Constitutional Compliance**:
- Structured logging with correlation IDs
- PII redaction in audit logs
- Multi-tier log streaming

**Agent Guidance**:
- Audit Module Agent ensures compliance logging
- GDPR Compliance Agent validates PII redaction
- Observability Agent coordinates log streaming

### Shared Module (`shared/`)
**Purpose**: Common utilities and security infrastructure

**Public API**:
- `shared/security/` - Common security utilities
- `shared/validation/` - Input validation utilities
- `shared/events/` - Common event types
- `shared/config/` - Shared configuration

**Responsibilities**:
- Common security patterns
- Input validation frameworks
- Shared event base classes
- Cross-cutting configuration

**Constitutional Compliance**:
- No business logic (utilities only)
- Used by 2+ modules requirement
- Comprehensive unit testing

## Boundary Enforcement

### ArchUnit Tests
Each module must include ArchUnit tests for boundary validation:

```java
@ArchTest
static final ArchRule modules_should_only_access_their_own_packages =
    classes().that().resideInAPackage("..auth..")
        .should().onlyAccessClassesThat()
        .resideInAnyPackage("..auth..", "..shared..", "java..", "org.springframework..");

@ArchTest
static final ArchRule modules_should_not_depend_on_internal_packages =
    noClasses().that().resideOutsideOfPackage("..auth..")
        .should().accessClassesThat()
        .resideInAPackage("..auth.internal..");
```

### Event-Driven Communication Validation
```java
@ArchTest
static final ArchRule inter_module_communication_should_use_events =
    classes().that().resideInAPackage("..api.facade..")
        .should().onlyBeAccessed().byClassesThat()
        .resideInAnyPackage("..controller..", "..config..", "..test..");
```

### Spring Modulith Integration Tests
```java
@SpringBootTest
@Modulith
class ModuleBoundaryIntegrationTest {

    @Test
    void verifyModuleBoundaries() {
        ApplicationModules.of(PaymentPlatformApplication.class)
            .verify();
    }
}
```

## Agent Integration Patterns

### Architecture Agents
**Spring Boot Modulith Architect**:
- Validates module boundary compliance
- Ensures event-driven communication patterns
- Reviews ArchUnit test coverage
- Guides module separation decisions

**System Architect**:
- Evaluates overall system boundaries
- Validates performance implications
- Reviews scalability concerns
- Guides future modularization

### Development Agents
**Module-Specific Agents** (`auth-module-agent`, `payment-processing-agent`, etc.):
- Enforce module-specific constitutional principles
- Validate internal module architecture
- Ensure proper event publishing
- Guide domain-specific implementations

### Testing Agents
**Integration Testing Agent**:
- Validates cross-module event communication
- Tests module boundary enforcement
- Ensures ArchUnit test coverage
- Validates Spring Modulith compliance

**Contract Testing Agent**:
- Validates module API contracts
- Tests event schema compliance
- Ensures backward compatibility
- Validates API versioning

## Module Interaction Examples

### User Registration with Subscription
```java
// 1. User module publishes event
@Service
class UserRegistrationService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public User registerUser(UserRegistrationRequest request) {
        User user = userRepository.save(new User(request));

        // Publish event for other modules
        eventPublisher.publishEvent(new UserCreatedEvent(
            user.getId(),
            user.getEmail(),
            user.getOrganizationId()
        ));

        return user;
    }
}

// 2. Subscription module listens for event
@Service
class SubscriptionEventHandler {

    @EventListener
    @Transactional
    public void handleUserCreated(UserCreatedEvent event) {
        subscriptionService.createTrialSubscription(
            event.getUserId(),
            event.getOrganizationId()
        );
    }
}
```

### Payment Processing with Audit
```java
// 1. Payment module processes payment and publishes event
@Service
class PaymentProcessingService {

    public PaymentResult processPayment(PaymentRequest request) {
        PaymentResult result = stripeService.processPayment(request);

        eventPublisher.publishEvent(new PaymentProcessedEvent(
            result.getPaymentId(),
            result.getAmount(),
            result.getCustomerId(),
            result.getStatus()
        ));

        return result;
    }
}

// 2. Audit module listens and logs
@Service
class AuditEventHandler {

    @EventListener
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        auditService.logPaymentEvent(
            event.getPaymentId(),
            event.getAmount(),
            event.getCustomerId(),
            "PAYMENT_PROCESSED"
        );
    }
}
```

## Troubleshooting Module Boundaries

### Common Boundary Violations
1. **Direct Service Injection**: Injecting services from other modules
2. **Internal Package Access**: Accessing `.internal.` packages from other modules
3. **Circular Dependencies**: Modules depending on each other directly
4. **Missing Events**: Not publishing events for cross-module communication

### Violation Detection
- ArchUnit tests fail during build
- Spring Modulith verification fails
- Integration tests reveal coupling issues
- Code review agents detect violations

### Resolution Patterns
1. **Extract to Shared**: Move common code to shared module
2. **Publish Events**: Replace direct calls with event publishing
3. **Create Facade**: Expose functionality through public API facade
4. **Refactor Boundaries**: Adjust module boundaries if needed

---

**Module Boundaries Version**: 1.2.0
**Last Updated**: 2024-09-14
**Spring Modulith Version**: 1.1+

This context provides the definitive guide for module boundary enforcement and inter-module communication patterns. All agents must reference this context when working with modular architecture decisions.