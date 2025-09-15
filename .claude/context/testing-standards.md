# Testing Standards Context

This context defines comprehensive testing standards and constitutional testing requirements for all agents working on the Spring Boot Modulith payment platform.

## Constitutional Testing Hierarchy

### I. Test Order Enforcement (NON-NEGOTIABLE)
**Mandated Order**: Contract → Integration → E2E → Unit

#### 1. Contract Tests (FIRST PRIORITY)
**Purpose**: API schema validation and contract compliance
**Tools**: OpenAPI validators, Pact consumer-driven testing
**Scope**: External API interfaces, event schemas, webhook contracts

```java
@Test
void contractTest_userRegistrationAPI_validatesOpenAPISchema() {
    // Validates actual API response against OpenAPI specification
    given()
        .contentType(ContentType.JSON)
        .body(validUserRegistrationRequest())
    .when()
        .post("/api/users")
    .then()
        .statusCode(201)
        .body(matchesJsonSchema(getUserSchema()));
}
```

#### 2. Integration Tests (SECOND PRIORITY)
**Purpose**: Cross-module communication with real dependencies
**Tools**: TestContainers, @SpringBootTest with full context
**Scope**: Database interactions, external service integration, event publishing

```java
@SpringBootTest
@Testcontainers
class UserRegistrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Test
    void integrationTest_userRegistration_publishesEventAndPersistsUser() {
        // Test with real database and event publishing
        UserRegistrationRequest request = createValidRequest();

        User user = userService.registerUser(request);

        // Verify database persistence
        assertThat(userRepository.findById(user.getId())).isPresent();

        // Verify event publishing
        verify(eventPublisher).publishEvent(any(UserCreatedEvent.class));
    }
}
```

#### 3. E2E Tests (THIRD PRIORITY)
**Purpose**: Complete user journey validation
**Tools**: Playwright with MCP integration
**Scope**: Full user workflows, cross-browser testing, accessibility

```typescript
test('e2e_userRegistrationFlow_completesSuccessfully', async ({ page }) => {
    // Navigate to registration
    await page.goto('/register');

    // Fill registration form
    await page.fill('[data-testid="email"]', 'test@example.com');
    await page.fill('[data-testid="password"]', 'SecurePass123!');

    // Submit and verify success
    await page.click('[data-testid="register-button"]');
    await expect(page.locator('[data-testid="welcome-message"]')).toBeVisible();

    // Verify subscription creation
    await expect(page.locator('[data-testid="trial-subscription"]')).toBeVisible();
});
```

#### 4. Unit Tests (FOURTH PRIORITY)
**Purpose**: Pure logic testing with minimal dependencies
**Tools**: JUnit 5, Mockito for external dependencies only
**Scope**: Business logic, validation, calculations

```java
@Test
void unitTest_calculateSubscriptionPrice_appliesDiscountCorrectly() {
    // Test pure business logic
    PricingCalculator calculator = new PricingCalculator();

    Money basePrice = Money.of(100, "USD");
    Discount discount = Discount.percentage(20);

    Money finalPrice = calculator.calculatePrice(basePrice, discount);

    assertThat(finalPrice).isEqualTo(Money.of(80, "USD"));
}
```

## TDD Constitutional Requirements

### RED-GREEN-Refactor Cycle Enforcement
**Phase 1 - RED**: Write failing tests
```java
@Test
void testFeatureNotYetImplemented() {
    // This test MUST fail initially
    assertThrows(UnsupportedOperationException.class, () -> {
        featureService.newFeature();
    });
}
```

**Phase 2 - GREEN**: Minimal implementation to pass tests
```java
public class FeatureService {
    public void newFeature() {
        // Minimal implementation to make test pass
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
```

**Phase 3 - REFACTOR**: Improve implementation while keeping tests green
```java
public class FeatureService {
    public FeatureResult newFeature() {
        // Full implementation after refactoring
        return processFeature();
    }
}
```

### Forbidden Anti-Patterns
❌ **Implementation before tests**
❌ **Skipping RED phase**
❌ **Mock-heavy integration tests**
❌ **Test-after development**
❌ **Testing implementation details instead of behavior**

## Real Dependencies Requirement

### TestContainers Usage (MANDATORY)
All integration tests must use real dependencies via TestContainers:

```java
@Testcontainers
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }
}
```

### External Service Integration
Real external services for integration testing:

```java
@Test
void integrationTest_stripePayment_processesRealPayment() {
    // Use Stripe test keys for real API calls
    StripePaymentRequest request = StripePaymentRequest.builder()
            .amount(Money.of(100, "USD"))
            .paymentMethodId("pm_card_visa") // Test payment method
            .build();

    PaymentResult result = paymentService.processPayment(request);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
}
```

## Testing Agent Responsibilities

### TDD Compliance Agent
**Responsibilities**:
- Enforce RED-GREEN-Refactor cycle
- Validate test-first development
- Detect anti-patterns in test implementation
- Coordinate testing hierarchy compliance

**Validation Patterns**:
```python
def validate_tdd_compliance(git_commits):
    for commit in git_commits:
        if has_implementation_without_tests(commit):
            raise ConstitutionalViolation("TDD Required: Tests must precede implementation")

        if has_passing_tests_without_red_phase(commit):
            raise ConstitutionalViolation("RED phase required: Tests must fail initially")
```

### Contract Testing Agent
**Responsibilities**:
- OpenAPI schema validation
- Consumer-driven contract testing with Pact
- API versioning compliance
- Breaking change detection

**Schema Validation**:
```java
@Test
void contractTest_validateAPIResponseSchema() {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    JsonSchema schema = factory.getSchema(getClass().getResourceAsStream("/schemas/user-response.json"));

    String response = callUserAPI();
    JsonNode responseJson = objectMapper.readTree(response);

    Set<ValidationMessage> errors = schema.validate(responseJson);
    assertThat(errors).isEmpty();
}
```

### Integration Testing Agent
**Responsibilities**:
- TestContainers configuration and management
- Cross-module event testing
- Database transaction testing
- Real dependency validation

**Event Integration Testing**:
```java
@Test
void integrationTest_userCreation_triggersSubscriptionCreation() {
    // Create user and capture events
    TestEventCaptor eventCaptor = new TestEventCaptor();

    userService.registerUser(validUserRequest());

    // Verify event publishing and subscription creation
    UserCreatedEvent event = eventCaptor.getEvent(UserCreatedEvent.class);
    assertThat(event.getUserId()).isNotNull();

    // Verify subscription was created via event
    Optional<Subscription> subscription = subscriptionRepository
            .findByUserId(event.getUserId());
    assertThat(subscription).isPresent();
}
```

### Playwright E2E Agent
**Responsibilities**:
- Complete user journey automation
- Cross-browser compatibility testing
- Accessibility compliance validation
- Performance testing integration

**Accessibility Testing**:
```typescript
test('e2e_paymentFlow_meetsAccessibilityStandards', async ({ page }) => {
    await page.goto('/subscription/payment');

    // Inject axe-core for accessibility testing
    await page.addInitScript(() => {
        window.axe = require('axe-core');
    });

    // Run accessibility audit
    const accessibilityResults = await page.evaluate(() => window.axe.run());
    expect(accessibilityResults.violations).toEqual([]);

    // Test keyboard navigation
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');

    // Verify screen reader compatibility
    const ariaLabel = await page.locator('[data-testid="payment-form"]').getAttribute('aria-label');
    expect(ariaLabel).toBeTruthy();
});
```

## Test Organization and Structure

### Directory Structure
```
tests/
├── contract/                   # Contract tests (OpenAPI, Pact)
│   ├── api/
│   └── events/
├── integration/               # Integration tests with real dependencies
│   ├── database/
│   ├── external-services/
│   └── cross-module/
├── e2e/                      # End-to-end Playwright tests
│   ├── user-journeys/
│   ├── admin-workflows/
│   └── accessibility/
├── unit/                     # Unit tests (lowest priority)
│   ├── domain/
│   ├── service/
│   └── validation/
└── agent/                    # Agent validation tests
    ├── test_agent_validation.py
    └── test_constitutional_compliance.py
```

### Test Naming Conventions
```java
// Pattern: [testType]_[methodUnderTest]_[expectedBehavior]
@Test
void contractTest_createUser_returnsValidSchema() { }

@Test
void integrationTest_userRegistration_persistsToDatabase() { }

@Test
void unitTest_calculatePrice_appliesDiscountCorrectly() { }
```

### Test Data Management
```java
// Test builders for consistent data creation
public class UserTestDataBuilder {
    public static User.Builder validUser() {
        return User.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .organizationId(OrganizationId.generate());
    }

    public static UserRegistrationRequest validRegistrationRequest() {
        return UserRegistrationRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .organizationName("Test Organization")
                .build();
    }
}
```

## Performance and Quality Requirements

### Test Execution Performance
- **Unit tests**: < 5 seconds total execution
- **Integration tests**: < 2 minutes total execution
- **Contract tests**: < 30 seconds total execution
- **E2E tests**: < 10 minutes for full suite

### Coverage Requirements
- **Unit test coverage**: > 80% for business logic
- **Integration test coverage**: 100% for cross-module communication
- **Contract test coverage**: 100% for public APIs
- **E2E test coverage**: 100% for critical user journeys

### Quality Gates
```yaml
quality_gates:
  test_execution:
    - all_tests_must_pass: true
    - no_flaky_tests: true
    - performance_within_limits: true

  coverage:
    - unit_coverage: "> 80%"
    - integration_coverage: "100% for modules"
    - e2e_coverage: "100% for critical paths"

  constitutional_compliance:
    - tdd_cycle_followed: true
    - test_hierarchy_respected: true
    - real_dependencies_used: true
```

## Constitutional Enforcement in Testing

### Test Validation Checklist
- [ ] Tests written before implementation (TDD compliance)
- [ ] Test hierarchy followed (Contract → Integration → E2E → Unit)
- [ ] Real dependencies used in integration tests
- [ ] TestContainers configured for database/external services
- [ ] Accessibility testing included for UI components
- [ ] Performance testing integrated for critical paths
- [ ] Event-driven communication tested
- [ ] Module boundaries respected in tests

### Agent Coordination for Testing
```python
def coordinate_testing_workflow():
    """Multi-agent testing workflow coordination."""

    # 1. Contract Testing Agent validates APIs
    contract_results = contract_agent.validate_api_contracts()

    # 2. Integration Testing Agent validates cross-module communication
    integration_results = integration_agent.test_module_communication()

    # 3. Playwright E2E Agent validates user journeys
    e2e_results = playwright_agent.test_user_journeys()

    # 4. TDD Compliance Agent validates overall compliance
    tdd_compliance = tdd_agent.validate_testing_compliance()

    return TestingReport(
        contract=contract_results,
        integration=integration_results,
        e2e=e2e_results,
        tdd_compliance=tdd_compliance
    )
```

---

**Testing Standards Version**: 1.3.0
**Last Updated**: 2024-09-14
**Constitutional Compliance**: Required for all agents

This testing standards context is the authoritative guide for all testing-related decisions and implementations. Agents must prioritize constitutional testing compliance and coordinate to ensure comprehensive testing coverage.