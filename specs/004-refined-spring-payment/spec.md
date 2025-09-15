/# Feature Specification: Refined Spring Boot Modulith Payment Platform

## Executive Summary
A production-ready, modular micro-SaaS payment management platform built with Spring Boot Modulith architecture, featuring proper opaque token implementation, right-sized CI/CD pipeline, and clear module boundaries. The system emphasizes local-first development with optional cloud deployment, consistent servlet stack, and comprehensive testing strategy.

## Key Concepts

### Primary Actors
- **End Users**: Customers accessing React SPA with ShadCN/UI for subscription management
- **Administrators**: Platform operators using Spring MVC Thymeleaf admin console
- **Notification Service**: Separate microservice for cross-process communication (PoC)
- **CI/CD System**: Right-sized automated pipeline with conditional steps
- **External Integrators**: Third-party services consuming JSON APIs

### Core Actions
- **Standard Opaque Token Flow**: Spring Authorization Server with built-in introspection
- **Module Event Communication**: Spring Modulith events for intra-module interaction
- **Conditional CI/CD Pipeline**: Optimized workflow with artifact reuse and caching
- **Social Provider Integration**: OAuth2 with PKCE and proper CORS/cookie settings
- **Stripe Payment Processing**: Idempotent webhook handling with signature verification
- **Comprehensive Observability**: Micrometer metrics with structured logging and PII protection

### Data Entities
- **User**: OAuth2 profiles with hashed token storage and RBAC permissions
- **Organization**: Multi-tenant accounts with proper tenant isolation
- **Subscription**: Stripe-integrated billing with idempotency key handling
- **Invoice**: PDF-ready documents with audit-compliant versioning
- **Payment**: Transaction records with correlation ID tracing
- **AuditEvent**: Immutable compliance records with PII redaction
- **ModuleVersion**: Schema-validated compatibility matrix
- **TokenMetadata**: Hashed opaque tokens with TTL indexes

### Constraints (MANDATORY)
- **Standard Opaque Token Implementation**: Use Spring's built-in introspection, no custom JWT
- **Servlet Stack Consistency**: WebMVC end-to-end, no reactive mixing
- **Local-First Development**: Docker Compose primary, cloud deployment optional
- **Module Boundary Enforcement**: ArchUnit rules with forbidden dependencies
- **Conditional CI/CD Steps**: Guard optional stacks, fail gracefully on missing secrets
- **Spring Modulith Events**: Intra-module communication, gRPC only for true service boundaries
- **Schema Validation**: JSON Schema for module-versions.yml with CI validation
- **PII Protection**: GDPR-compliant logging with field redaction

## Architecture & Implementation

### Corrected Opaque Token Implementation

#### Standard Spring Configuration (NO Custom Implementation)
```yaml
# application.yml - Authorization Server
spring:
  security:
    oauth2:
      authorizationserver:
        client:
          react-spa:
            client-id: "payment-platform-spa"
            client-authentication-methods: ["none"]
            authorization-grant-types: ["authorization_code", "refresh_token"]
            redirect-uris:
              - "http://localhost:3000/auth/callback"
              - "https://app.payment-platform.com/auth/callback"
            scopes: ["openid", "profile", "payment.read", "payment.write", "subscription.manage"]
            require-proof-key: true
            token-settings:
              access-token-time-to-live: "PT15M"
              refresh-token-time-to-live: "P7D"
              reuse-refresh-tokens: false  # Rotating refresh tokens

          admin-console:
            client-id: "payment-admin-console"
            client-secret: "{bcrypt}$2a$10$..."
            client-authentication-methods: ["client_secret_basic"]
            authorization-grant-types: ["authorization_code", "refresh_token"]
            redirect-uris:
              - "http://localhost:8080/login/oauth2/code/admin"
              - "https://admin.payment-platform.com/login/oauth2/code/admin"
            scopes: ["openid", "profile", "admin.read", "admin.write", "audit.read"]
      resourceserver:
        opaque-token:
          introspection-uri: http://localhost:9000/oauth2/introspect
          client-id: payment-api-resource-server
          client-secret: resource-server-secret
```

#### Caching Opaque Token Introspector
```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector(
            OAuth2AuthorizationService authorizationService) {

        // Use Spring's standard introspector with caching
        NimbusOpaqueTokenIntrospector delegate = new NimbusOpaqueTokenIntrospector(
            introspectionUri, clientId, clientSecret);

        return new CachingOpaqueTokenIntrospector(delegate,
            Duration.ofMinutes(5));  // Cache for 5 minutes
    }
}

@Component
public class CachingOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final OpaqueTokenIntrospector delegate;
    private final Cache<String, OAuth2AuthenticatedPrincipal> cache;

    public CachingOpaqueTokenIntrospector(OpaqueTokenIntrospector delegate, Duration ttl) {
        this.delegate = delegate;
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(10_000)
            .build();
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        String tokenHash = DigestUtils.sha256Hex(token);

        return cache.get(tokenHash, key -> {
            try {
                return delegate.introspect(token);
            } catch (OAuth2IntrospectionException e) {
                log.warn("Token introspection failed for hash: {}", tokenHash);
                throw e;
            }
        });
    }
}
```

#### Token Storage with Proper Hashing
```java
@Entity
@Table(name = "oauth2_authorizations")
public class OAuth2AuthorizationEntity {

    @Id
    private String id;

    @Column(name = "access_token_hash")
    private String accessTokenHash;  // SHA-256 + salt

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @CreationTimestamp
    private Instant createdAt;

    // TTL index for automatic cleanup
    @Index(name = "idx_access_token_expires", expireAfterSeconds = 0)
    private Instant ttlIndex;

    public void setAccessToken(String token) {
        // Hash token before storage
        this.accessTokenHash = hashToken(token);
        this.ttlIndex = accessTokenExpiresAt;  // TTL cleanup
    }

    private String hashToken(String token) {
        String salt = generateSalt();
        return DigestUtils.sha256Hex(token + salt) + ":" + salt;
    }
}
```

### Module Architecture with Clear Boundaries

#### Spring Modulith Module Structure
```java
// Module organization with proper boundaries
com.paymentplatform/
├── auth/                    # Authentication module
│   ├── AuthModule.java      # @ApplicationModule
│   ├── domain/
│   │   ├── User.java
│   │   └── UserRepository.java
│   ├── application/
│   │   └── AuthService.java
│   └── infrastructure/
│       └── OAuth2Controller.java
├── payment/                 # Payment module
│   ├── PaymentModule.java
│   ├── domain/
│   │   ├── Payment.java
│   │   ├── Subscription.java
│   │   └── PaymentRepository.java
│   ├── application/
│   │   └── PaymentService.java
│   └── infrastructure/
│       ├── PaymentController.java
│       └── StripeWebhookController.java
├── user/                    # User management module
│   └── ...
├── audit/                   # Audit logging module
│   └── ...
└── shared/                  # Shared kernel
    ├── events/
    │   ├── PaymentProcessedEvent.java
    │   └── UserCreatedEvent.java
    └── types/
        ├── Money.java
        └── CorrelationId.java
```

#### ArchUnit Module Boundary Tests
```java
// tests/architecture/ModuleBoundaryTest.java
@AnalyzeClasses(packages = "com.paymentplatform")
public class ModuleBoundaryTest {

    @ArchTest
    static final ArchRule payment_module_isolation =
        noClasses()
            .that().resideInAPackage("..payment..")
            .should().dependOnClassesThat()
            .resideInAPackage("..user..")
            .because("Payment module should not directly depend on user module");

    @ArchTest
    static final ArchRule auth_module_isolation =
        noClasses()
            .that().resideInAPackage("..auth..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..payment..", "..user..", "..audit..")
            .because("Auth module should only depend on shared kernel");

    @ArchTest
    static final ArchRule controllers_are_in_infrastructure =
        classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..infrastructure..")
            .because("Controllers should be in infrastructure layer");

    @ArchTest
    static final ArchRule services_are_in_application =
        classes()
            .that().haveSimpleNameEndingWith("Service")
            .should().resideInAPackage("..application..")
            .because("Services should be in application layer");

    @ArchTest
    static final ArchRule entities_are_in_domain =
        classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..domain..")
            .because("Entities should be in domain layer");
}
```

#### Spring Modulith Event-Driven Communication
```java
// Payment module publishes events
@Component
@Slf4j
public class PaymentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Async
    public void handleStripeWebhook(StripeWebhookEvent webhookEvent) {
        if ("invoice.payment_succeeded".equals(webhookEvent.getType())) {

            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .paymentId(webhookEvent.getPaymentId())
                .amount(webhookEvent.getAmount())
                .customerId(webhookEvent.getCustomerId())
                .correlationId(webhookEvent.getCorrelationId())
                .occurredAt(Instant.now())
                .build();

            log.info("Publishing payment processed event: {}", event.getPaymentId());
            eventPublisher.publishEvent(event);
        }
    }
}

// Notification service listens to events
@Component
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @EventListener
    @Async
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Handling payment processed event: {}", event.getPaymentId());

        notificationService.sendPaymentConfirmation(
            event.getCustomerId(),
            event.getAmount(),
            event.getCorrelationId()
        );
    }

    @EventListener
    @Async
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("Handling user created event: {}", event.getUserId());

        notificationService.sendWelcomeEmail(
            event.getUserId(),
            event.getEmail(),
            event.getCorrelationId()
        );
    }
}
```

### Right-Sized CI/CD Pipeline

#### Conditional and Cached Pipeline
```yaml
# .github/workflows/ci-pipeline.yml
name: Right-Sized CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

concurrency:
  group: ${{ github.ref }}
  cancel-in-progress: true

env:
  JAVA_VERSION: '17'
  NODE_VERSION: '18'

jobs:
  # Detect changes for conditional execution
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.changes.outputs.backend }}
      frontend: ${{ steps.changes.outputs.frontend }}
      terraform: ${{ steps.changes.outputs.terraform }}
      k8s: ${{ steps.changes.outputs.k8s }}
      dockerfile: ${{ steps.changes.outputs.dockerfile }}

    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v2
        id: changes
        with:
          filters: |
            backend:
              - 'src/**'
              - 'pom.xml'
              - '**/*.java'
            frontend:
              - 'frontend/**'
              - 'frontend/package*.json'
            terraform:
              - 'terraform/**'
            k8s:
              - 'k8s/**'
            dockerfile:
              - 'Dockerfile'
              - 'docker-compose*.yml'

  # Backend CI (only if backend changes detected)
  backend-ci:
    needs: detect-changes
    if: needs.detect-changes.outputs.backend == 'true'
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      # Cache Maven dependencies
      - name: Cache Maven Dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

      - name: Build Backend
        run: mvn clean compile -DskipTests

      # Upload build artifacts for reuse
      - name: Upload Build Artifacts
        uses: actions/upload-artifact@v3
        with:
          name: backend-build
          path: target/
          retention-days: 1

  # Security scanning (conditional and graceful failure)
  security-scan:
    needs: [detect-changes, backend-ci]
    if: needs.detect-changes.outputs.backend == 'true' || needs.detect-changes.outputs.frontend == 'true'
    runs-on: ubuntu-latest
    continue-on-error: true  # Don't fail CI for community forks

    steps:
      - uses: actions/checkout@v4

      # CodeQL (graceful failure if not available)
      - name: Initialize CodeQL
        uses: github/codeql-action/init@v2
        with:
          languages: java, javascript
        continue-on-error: true

      # Download build artifacts
      - name: Download Build Artifacts
        if: needs.detect-changes.outputs.backend == 'true'
        uses: actions/download-artifact@v3
        with:
          name: backend-build
          path: target/

      # OWASP Dependency Check (soft-fail for community)
      - name: OWASP Dependency Check
        if: needs.detect-changes.outputs.backend == 'true'
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'payment-platform'
          path: '.'
          format: 'JSON'
        continue-on-error: ${{ github.event.pull_request.head.repo.fork == true }}

      # Snyk (only if token available)
      - name: Run Snyk Security Scan
        if: env.SNYK_TOKEN != '' && needs.detect-changes.outputs.backend == 'true'
        uses: snyk/actions/maven@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
        continue-on-error: true

      - name: Complete CodeQL Analysis
        uses: github/codeql-action/analyze@v2
        continue-on-error: true

  # Testing with proper test mapping
  test-suite:
    needs: [detect-changes, backend-ci]
    if: needs.detect-changes.outputs.backend == 'true'
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      # Download build artifacts
      - name: Download Build Artifacts
        uses: actions/download-artifact@v3
        with:
          name: backend-build
          path: target/

      # Test pyramid execution with clear mapping
      - name: Unit Tests (@Test)
        run: mvn test -Dtest="**/*Test.java" -Dmaven.test.failure.ignore=false

      - name: Module Tests (@ApplicationModuleTest)
        run: mvn test -Dtest="**/*ModuleTest.java" -Dspring.profiles.active=test

      - name: Architecture Tests (@ArchTest)
        run: mvn test -Dtest="**/*ArchTest.java"

      - name: Contract Tests (Pact)
        run: mvn test -Dtest="**/*ContractTest.java" -Dpact.verifier.publishResults=true

      - name: Integration Tests (@SpringBootTest)
        run: mvn test -Dtest="**/*IT.java" -Dspring.profiles.active=integration-test
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/test

      # Coverage with quality gate
      - name: Generate Coverage Report
        run: mvn jacoco:report

      - name: Coverage Quality Gate
        run: mvn jacoco:check -Dcoverage.minimum=0.80

  # Frontend CI (conditional)
  frontend-ci:
    needs: detect-changes
    if: needs.detect-changes.outputs.frontend == 'true'
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: 'frontend/package-lock.json'

      - name: Install Dependencies
        run: |
          cd frontend
          npm ci

      - name: Lint and Security Check
        run: |
          cd frontend
          npm run lint:security
          npm run test:a11y

      - name: Build Frontend
        run: |
          cd frontend
          npm run build

      # Upload frontend build artifacts
      - name: Upload Frontend Build
        uses: actions/upload-artifact@v3
        with:
          name: frontend-build
          path: frontend/dist/
          retention-days: 1

  # Container build (conditional)
  container-build:
    needs: [detect-changes, backend-ci, frontend-ci]
    if: needs.detect-changes.outputs.dockerfile == 'true' || (needs.detect-changes.outputs.backend == 'true' && needs.detect-changes.outputs.frontend == 'true')
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      # Download all build artifacts
      - name: Download Backend Build
        if: needs.detect-changes.outputs.backend == 'true'
        uses: actions/download-artifact@v3
        with:
          name: backend-build
          path: target/

      - name: Download Frontend Build
        if: needs.detect-changes.outputs.frontend == 'true'
        uses: actions/download-artifact@v3
        with:
          name: frontend-build
          path: frontend/dist/

      - name: Build Container Image
        run: docker build -t payment-platform:${{ github.sha }} .

      - name: Container Security Scan
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'payment-platform:${{ github.sha }}'
          format: 'table'
          exit-code: '1'
          severity: 'CRITICAL,HIGH'

  # Infrastructure validation (conditional)
  infrastructure-validate:
    needs: detect-changes
    if: needs.detect-changes.outputs.terraform == 'true' || needs.detect-changes.outputs.k8s == 'true'
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Terraform Validate
        if: needs.detect-changes.outputs.terraform == 'true'
        run: |
          cd terraform
          terraform init -backend=false
          terraform validate

      - name: Terraform Security Scan
        if: needs.detect-changes.outputs.terraform == 'true'
        uses: aquasecurity/tfsec-action@v1.0.0
        with:
          working_directory: terraform/

      - name: Kubernetes Manifests Validation
        if: needs.detect-changes.outputs.k8s == 'true'
        uses: azure/k8s-lint@v1
        with:
          manifests: k8s/*.yaml
```

### Testing Strategy Matrix

#### Acceptance Criteria → Test Type Mapping
```yaml
# tests/test-mapping.yml
acceptance-criteria-mapping:
  authentication:
    - criterion: "Users can sign up with email/password or social providers"
      test-types:
        - contract: "OAuth2ContractTest.java"
        - integration: "AuthIntegrationTest.java"
        - e2e: "user-registration.spec.js"

    - criterion: "Sessions use opaque tokens with configurable expiration"
      test-types:
        - unit: "OpaqueTokenServiceTest.java"
        - integration: "TokenIntrospectionIT.java"
        - security: "TokenSecurityTest.java"

  payment-processing:
    - criterion: "Stripe Checkout integration for payment collection"
      test-types:
        - contract: "StripeContractTest.java"
        - integration: "StripeWebhookIT.java"
        - e2e: "checkout-flow.spec.js"

    - criterion: "Failed payment retry logic with notifications"
      test-types:
        - unit: "PaymentRetryServiceTest.java"
        - integration: "PaymentRetryIT.java"
        - module: "PaymentModuleTest.java"

  module-boundaries:
    - criterion: "Clear module interfaces with defined contracts"
      test-types:
        - architecture: "ModuleBoundaryArchTest.java"
        - module: "PaymentModuleTest.java, AuthModuleTest.java"

    - criterion: "Event-driven communication between modules"
      test-types:
        - unit: "EventPublisherTest.java"
        - integration: "ModuleEventIT.java"
        - module: "CrossModuleEventTest.java"
```

#### Spring Modulith Testing Examples
```java
// Test package structure
src/test/java/
├── architecture/
│   ├── ModuleBoundaryArchTest.java
│   ├── LayerArchTest.java
│   └── SecurityArchTest.java
├── module/
│   ├── payment/
│   │   ├── PaymentModuleTest.java
│   │   └── PaymentServiceTest.java
│   ├── auth/
│   │   ├── AuthModuleTest.java
│   │   └── OAuth2ServiceTest.java
│   └── user/
│       └── UserModuleTest.java
├── integration/
│   ├── payment/
│   │   ├── StripeWebhookIT.java
│   │   └── PaymentFlowIT.java
│   └── auth/
│       └── OAuth2FlowIT.java
└── contract/
    ├── StripeContractTest.java
    └── OAuth2ContractTest.java

// Module slice testing
@ApplicationModuleTest
class PaymentModuleTest {

    @Test
    void shouldPublishEventWhenPaymentProcessed() {
        // Given
        Payment payment = createTestPayment();

        // When
        paymentService.processPayment(payment);

        // Then
        verify(eventPublisher).publishEvent(any(PaymentProcessedEvent.class));
    }

    @Test
    void shouldHandleStripeWebhookIdempotently() {
        // Given
        String idempotencyKey = "test-key-123";
        StripeWebhookEvent event = createWebhookEvent(idempotencyKey);

        // When - process twice
        webhookHandler.handle(event);
        webhookHandler.handle(event);

        // Then - should only process once
        verify(paymentService, times(1)).processPayment(any());
    }
}

// Cross-module event testing
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class CrossModuleEventIT {

    @Test
    @Order(1)
    void shouldNotifyWhenUserCreated() {
        // Given
        UserCreatedEvent event = new UserCreatedEvent("user-123", "test@example.com");

        // When
        eventPublisher.publishEvent(event);

        // Then
        await().atMost(5, SECONDS)
            .untilAsserted(() -> {
                verify(notificationService).sendWelcomeEmail("user-123", "test@example.com");
            });
    }
}
```

### Stripe Integration with Idempotency and Security

#### Webhook Handler with Proper Verification
```java
@RestController
@RequestMapping("/api/webhooks")
@Slf4j
public class StripeWebhookController {

    private final StripeWebhookService webhookService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            // Verify webhook signature (MANDATORY)
            Event event = Webhook.constructEvent(payload, sigHeader, webhookEndpointSecret);

            String idempotencyKey = event.getId();

            // Handle idempotency
            if (idempotencyService.isProcessed(idempotencyKey)) {
                log.info("Webhook already processed: {}", idempotencyKey);
                return ResponseEntity.ok("Already processed");
            }

            // Process event based on type
            switch (event.getType()) {
                case "invoice.payment_succeeded":
                    handlePaymentSucceeded(event, idempotencyKey);
                    break;
                case "invoice.payment_failed":
                    handlePaymentFailed(event, idempotencyKey);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event, idempotencyKey);
                    break;
                default:
                    log.info("Unhandled webhook event type: {}", event.getType());
            }

            // Mark as processed
            idempotencyService.markProcessed(idempotencyKey);

            return ResponseEntity.ok("Webhook processed");

        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing failed");
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void handlePaymentSucceeded(Event event, String idempotencyKey) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);

        if (invoice != null) {
            webhookService.processPaymentSucceeded(invoice, idempotencyKey);
        }
    }
}

@Service
@Transactional
public class IdempotencyService {

    private final IdempotencyRepository repository;

    public boolean isProcessed(String key) {
        return repository.existsByKey(key);
    }

    public void markProcessed(String key) {
        IdempotencyRecord record = new IdempotencyRecord(key, Instant.now());
        repository.save(record);
    }
}
```

### Observability with PII Protection

#### Structured Logging Configuration
```yaml
# application.yml
logging:
  level:
    com.paymentplatform: INFO
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId}] [%X{tenantId}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId}] [%X{tenantId}] %logger{36} - %msg%n"

# Micrometer and Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,httptrace
  endpoint:
    health:
      show-details: when-authorized
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: payment-platform
      environment: ${SPRING_PROFILES_ACTIVE:local}

# Required metrics to collect
micrometer:
  custom-meters:
    - name: "payment.processing.duration"
      type: "timer"
      description: "Time taken to process payments"
    - name: "user.registration.count"
      type: "counter"
      description: "Number of user registrations"
    - name: "stripe.webhook.processing.duration"
      type: "timer"
      description: "Stripe webhook processing time"
    - name: "token.introspection.duration"
      type: "timer"
      description: "Opaque token introspection time"
```

#### PII Redaction Configuration
```java
@Component
@Slf4j
public class PiiRedactionFilter implements Filter {

    private static final Set<String> PII_FIELDS = Set.of(
        "email", "firstName", "lastName", "phoneNumber", "address"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Add correlation ID to MDC
        String correlationId = getOrCreateCorrelationId(httpRequest);
        MDC.put("correlationId", correlationId);

        // Add tenant ID if available
        String tenantId = extractTenantId(httpRequest);
        if (tenantId != null) {
            MDC.put("tenantId", tenantId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    // Custom JSON layout for PII redaction
    @Bean
    public JsonEncoder jsonEncoder() {
        return new JsonEncoder() {
            @Override
            public byte[] encode(ILoggingEvent event) {
                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("timestamp", event.getTimeStamp());
                logEntry.put("level", event.getLevel().toString());
                logEntry.put("logger", event.getLoggerName());
                logEntry.put("message", redactPii(event.getFormattedMessage()));
                logEntry.put("correlationId", event.getMDCPropertyMap().get("correlationId"));
                logEntry.put("tenantId", event.getMDCPropertyMap().get("tenantId"));

                return objectMapper.writeValueAsBytes(logEntry);
            }
        };
    }

    private String redactPii(String message) {
        for (String piiField : PII_FIELDS) {
            message = message.replaceAll(
                "(?i)" + piiField + "\"?:\\s*\"?([^,}\"\\s]+)",
                piiField + ": [REDACTED]"
            );
        }
        return message;
    }
}

// GDPR compliance configuration
@ConfigurationProperties(prefix = "gdpr")
@Data
public class GdprProperties {

    private Duration dataRetention = Duration.ofDays(2555);  // 7 years
    private Duration auditLogRetention = Duration.ofDays(2555);
    private boolean enablePiiRedaction = true;
    private Set<String> piiFields = Set.of("email", "firstName", "lastName", "phoneNumber");
}
```

This refined specification addresses all the high-impact fixes you mentioned, providing a production-ready, standards-compliant platform with proper architectural boundaries and right-sized CI/CD processes.