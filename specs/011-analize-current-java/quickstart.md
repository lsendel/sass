# Quickstart Guide: API Integration Testing Framework

**Date**: 2025-09-24
**Feature**: Getting Started with Integration Testing

## Overview

This quickstart guide will walk you through setting up and running the API integration testing framework for the Spring Boot Modulith Payment Platform. You'll learn how to execute tests, understand results, and customize the testing process.

## Prerequisites

Before you begin, ensure you have:
- Java 21 installed
- Docker and Docker Compose running
- Git repository access
- IDE with Lombok plugin (IntelliJ IDEA recommended)

## Quick Start (5 minutes)

### 1. Run Basic Test Suite
```bash
# Clone and setup
git clone <repository-url>
cd sass/backend

# Start containers and run critical tests
./gradlew test --tests "*CriticalIntegrationTest"
```

### 2. View Results
```bash
# Open HTML report
open build/reports/tests/test/index.html

# Or view JSON summary
cat build/test-results/integration/execution-summary.json
```

### 3. Run Module-Specific Tests
```bash
# Test only auth module
./gradlew test --tests "*AuthModuleIntegrationTest"

# Test payment workflows
./gradlew test --tests "*PaymentFlowIntegrationTest"
```

## Detailed Setup (15 minutes)

### 1. Environment Configuration

**Local Development Environment:**
```bash
# Copy environment template
cp backend/src/test/resources/application-test.yml.template \
   backend/src/test/resources/application-test.yml

# Start dependencies
docker-compose up -d postgres redis wiremock
```

**CI/CD Environment:**
```bash
# Set required environment variables
export SPRING_PROFILES_ACTIVE=integration
export TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/testdb
export TEST_REDIS_URL=redis://localhost:6379
export STRIPE_TEST_API_KEY=sk_test_...
```

### 2. Test Data Setup

**Automatic Fixture Loading:**
```bash
# Generate test data
./gradlew generateTestData

# Verify fixtures
./gradlew test --tests "*FixtureValidationTest"
```

**Manual Data Creation:**
```java
// Using TestDataFactory
@Test
public void testUserRegistration() {
    User testUser = TestDataFactory.createUser()
        .withEmail("test@example.com")
        .withOrganization(TestDataFactory.createOrganization())
        .build();

    // Test implementation...
}
```

### 3. Running Test Suites

**Sequential Execution (Default):**
```bash
./gradlew test --tests "*IntegrationTest"
```

**Parallel Execution:**
```bash
./gradlew test --parallel --tests "*IntegrationTest"
```

**Category-Specific Testing:**
```bash
# Contract tests only
./gradlew test --tests "*ContractTest"

# End-to-end scenarios
./gradlew test --tests "*E2ETest"

# Performance tests
./gradlew test --tests "*PerformanceTest"
```

### 4. Environment-Specific Testing

**Local Environment:**
```bash
./gradlew test -Pspring.profiles.active=test
```

**CI Environment:**
```bash
./gradlew test -Pspring.profiles.active=ci \
  -PtestContainers.reuse.enable=true
```

**Staging Environment:**
```bash
./gradlew test -Pspring.profiles.active=staging \
  -Ptest.base.url=https://api-staging.platform.com
```

## Test Execution Workflow

### Phase 1: Environment Setup (30-60 seconds)
```
✓ Starting TestContainers (PostgreSQL, Redis)
✓ Loading database schema
✓ Initializing test fixtures
✓ Starting WireMock services
✓ Validating external service connectivity
```

### Phase 2: Module Testing (2-5 minutes)
```
Auth Module Tests:
  ✓ OAuth2 login flow
  ✓ Session management
  ✓ Password validation
  ✓ Rate limiting

Payment Module Tests:
  ✓ Stripe integration
  ✓ Webhook processing
  ✓ Idempotency handling

[... similar for other modules]
```

### Phase 3: Integration Testing (2-3 minutes)
```
Cross-Module Scenarios:
  ✓ User registration → Organization creation
  ✓ Payment processing → Subscription update
  ✓ Auth events → Audit logging
```

### Phase 4: End-to-End Testing (3-5 minutes)
```
User Journeys:
  ✓ Complete registration flow
  ✓ Payment and subscription setup
  ✓ Organization onboarding
```

### Phase 5: Cleanup (10-30 seconds)
```
✓ Database cleanup
✓ Container shutdown
✓ Report generation
```

## Understanding Results

### Console Output
```
[INFO] Test Execution Progress: 85% (68/80 tests)
[INFO] Current Phase: Cross-Module Integration
[INFO] Current Test: Payment Processing Updates Subscription
[WARN] Retry attempt 1/3 for AuthSessionTimeoutTest
[INFO] Performance: Auth API avg 145ms (target: 200ms)
```

### HTML Report Structure
```
/build/reports/tests/test/
├── index.html                  # Main dashboard
├── packages/                   # Package-level results
├── classes/                   # Class-level results
└── css/                      # Styling assets
```

### Key Metrics to Monitor
- **Success Rate**: Should be > 95%
- **Average Response Time**: Should be < 200ms
- **P95 Response Time**: Should be < 500ms
- **Total Execution Time**: Should be < 10 minutes
- **Flaky Test Count**: Should be 0

## Common Test Scenarios

### 1. Happy Path Testing
```java
@Test
@DisplayName("User can successfully register and login")
public void userRegistrationAndLogin() {
    // Given: New user data
    UserRegistrationRequest request = TestDataFactory.userRegistrationRequest();

    // When: User registers
    String userId = registrationApi.registerUser(request);

    // Then: User can login
    LoginResponse login = authApi.login(request.getEmail(), request.getPassword());
    assertThat(login.getUserId()).isEqualTo(userId);
    assertThat(login.isSuccess()).isTrue();
}
```

### 2. Error Handling Testing
```java
@Test
@DisplayName("API returns proper error for invalid payment method")
public void invalidPaymentMethodHandling() {
    // Given: Invalid payment data
    PaymentRequest request = PaymentRequest.builder()
        .amount(1000)
        .currency("USD")
        .paymentMethodId("invalid_pm_id")
        .build();

    // When: Payment is processed
    // Then: Proper error response
    assertThatThrownBy(() -> paymentApi.processPayment(request))
        .isInstanceOf(PaymentException.class)
        .hasMessage("Invalid payment method")
        .satisfies(ex -> {
            assertThat(((PaymentException) ex).getErrorCode()).isEqualTo("INVALID_PAYMENT_METHOD");
        });
}
```

### 3. Performance Testing
```java
@Test
@DisplayName("Auth API meets performance requirements")
@Timeout(value = 5, unit = TimeUnit.SECONDS)
public void authApiPerformance() {
    // Given: Valid login credentials
    List<CompletableFuture<Duration>> futures = IntStream.range(0, 50)
        .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
            Instant start = Instant.now();
            authApi.login("user" + i + "@example.com", "password");
            return Duration.between(start, Instant.now());
        }))
        .collect(Collectors.toList());

    // When: Concurrent logins complete
    List<Duration> durations = futures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());

    // Then: Performance targets are met
    Duration average = durations.stream()
        .reduce(Duration.ZERO, Duration::plus)
        .dividedBy(durations.size());

    assertThat(average).isLessThan(Duration.ofMillis(200));
}
```

## Troubleshooting

### Common Issues and Solutions

**1. TestContainers startup failures**
```bash
# Check Docker daemon
docker info

# Clean Docker state
docker system prune -f
docker volume prune -f

# Verify TestContainers configuration
./gradlew test --info --tests "*ContainerSetupTest"
```

**2. Database connection issues**
```bash
# Check PostgreSQL container
docker ps | grep postgres

# Validate connection
docker exec -it <postgres-container> psql -U test -d testdb -c "\dt"

# Reset database state
./gradlew flywayClean flywayMigrate -Pflyway-test
```

**3. Stripe webhook testing issues**
```bash
# Verify WireMock is running
curl http://localhost:8089/__admin/mappings

# Check webhook signature validation
./gradlew test --tests "*StripeWebhookSignatureTest" --info
```

**4. Session management failures**
```bash
# Check Redis container
docker exec -it <redis-container> redis-cli ping

# Verify session configuration
./gradlew test --tests "*SessionConfigurationTest" --debug
```

**5. Flaky test identification**
```bash
# Run tests multiple times
for i in {1..5}; do ./gradlew test --tests "*FlakyCandidate*"; done

# Enable detailed logging
./gradlew test --tests "*FlakyTest" --debug --stacktrace
```

### Performance Optimization

**Parallel Execution Setup:**
```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=4

# junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
```

**Container Reuse:**
```java
@Testcontainers
public class BaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withReuse(true);
}
```

### Debugging Test Failures

**Enable Detailed Logging:**
```yaml
# application-test.yml
logging:
  level:
    com.platform: DEBUG
    org.springframework.web: DEBUG
    org.testcontainers: INFO
```

**Request/Response Capture:**
```java
@Test
public void debugFailedTest() {
    given()
        .filter(new RequestLoggingFilter())
        .filter(new ResponseLoggingFilter())
        .contentType(ContentType.JSON)
        .body(request)
    .when()
        .post("/api/v1/payments/intents")
    .then()
        .statusCode(200);
}
```

## Next Steps

Once you're comfortable with basic testing:

1. **Custom Test Scenarios**: Create organization-specific test cases
2. **CI/CD Integration**: Set up automated testing in your pipeline
3. **Performance Monitoring**: Implement continuous performance testing
4. **Test Data Management**: Create domain-specific fixtures
5. **Reporting Customization**: Build custom dashboards for your team

## Support and Resources

- **Documentation**: `/docs/testing/`
- **Code Examples**: `/backend/src/test/java/examples/`
- **Troubleshooting**: `/docs/testing/troubleshooting.md`
- **Performance Guidelines**: `/docs/testing/performance.md`
- **Contributing**: `/docs/testing/contributing.md`

For additional help, contact the Testing Team or create an issue in the repository.