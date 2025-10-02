# Comprehensive Plan: Fix All 35 Warnings + Achieve 85% Coverage (Zero Mocks)

## Executive Summary
Fix 35 checkstyle warnings and increase test coverage from 15% to 85% using **real databases only** (PostgreSQL, Redis) with TestContainers. No mocks, no blank files, full integration testing.

---

## Phase 1: Code Quality Fixes (Checkstyle Warnings)

### Strategy
Auto-format all files using IntelliJ IDEA's built-in formatter, then apply manual fixes where needed.

### 1.1 Indentation Fixes (32 warnings)

#### Files Requiring Auto-Format:
1. **OpaqueTokenService.java** (53 indentation issues)
   - Lines 27-137: All methods and fields incorrectly indented at level 8 instead of 4
   - **Fix**: Apply IntelliJ reformat code (Cmd+Alt+L)
   - **Verification**: Run `./gradlew checkstyleMain`

2. **AuthenticationService.java** (40 indentation issues)
   - Lines 25-130: Constructor, fields, and all methods misaligned
   - **Fix**: Apply IntelliJ reformat code
   - **Root Cause**: Likely copy-paste from differently indented source

3. **OpaqueTokenAuthenticationFilter.java** (10 indentation issues)
   - Lines 31-69: Filter implementation body
   - **Fix**: Apply IntelliJ reformat code

4. **SecurityConfig.java** (5 indentation issues)
   - Lines 124-141: Security filter chain configuration
   - **Fix**: Apply IntelliJ reformat code

5. **UserRepository.java** (2 issues)
   - Line 34-35: Query string concatenation
   - **Fix**: Reformat to put operator on new line

### 1.2 Design Issues (2 warnings)

#### AuditApplication.java (1 warning)
```java
// BEFORE:
@SpringBootApplication
public class AuditApplication {

// AFTER:
@SpringBootApplication
public final class AuditApplication {
```
**Rationale**: Application main classes should be final - they're not designed for extension

#### JpaAuditingConfiguration.java (1 warning)
```java
// ADD javadoc:
/**
 * Provides the current auditor for JPA auditing.
 * This method is called by Spring Data JPA to populate createdBy and lastModifiedBy fields.
 *
 * @return AuditorAware that retrieves the current authenticated user
 */
@Bean
public AuditorAware<String> auditorProvider() {
```
**Rationale**: Methods in extensible classes need documentation for safe extension

### 1.3 Operator Wrapping (1 warning)

#### UserRepository.java
```java
// BEFORE:
@Query("SELECT u FROM User u WHERE u.email = :email " +
       "AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")

// AFTER:
@Query("SELECT u FROM User u WHERE u.email = :email "
       + "AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
```
**Rationale**: Checkstyle requires operators at start of continuation lines

---

## Phase 2: Test Coverage Strategy (15% → 85%)

### Architecture: Zero-Mock Integration Testing

```
┌─────────────────────────────────────────────────────────────┐
│                    Test Architecture                          │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐         ┌──────────────┐                  │
│  │ Spring Boot  │────────▶│  PostgreSQL  │                  │
│  │ Test Context │         │ TestContainer│                  │
│  └──────────────┘         └──────────────┘                  │
│         │                                                     │
│         │                 ┌──────────────┐                  │
│         └────────────────▶│    Redis     │                  │
│                           │ TestContainer│                  │
│                           └──────────────┘                  │
│                                                               │
│  Real HTTP Calls → Real Auth Filter → Real Service          │
│  → Real Repository → Real Database → Actual SQL              │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Testing Principles

1. **TDD Required**: Write failing test FIRST, then implement
2. **Real Dependencies**: PostgreSQL + Redis via TestContainers
3. **Full Integration**: Test complete request/response cycles
4. **Zero Mocks**: Every dependency is real or TestContainers
5. **Observable**: Structured logging with correlation IDs

### Coverage Targets by Component

| Component | Current | Target | Gap | Test Type |
|-----------|---------|--------|-----|-----------|
| OpaqueTokenService | ~5% | 90% | 85% | Integration |
| AuthenticationService | ~10% | 90% | 80% | Integration |
| OpaqueTokenAuthenticationFilter | 0% | 85% | 85% | Integration |
| SecurityConfig | 0% | 80% | 80% | Integration |
| UserRepository | ~20% | 95% | 75% | Integration |
| Domain Models | ~50% | 90% | 40% | Unit |
| DTOs/Events | ~30% | 90% | 60% | Unit |

---

## Phase 3: Integration Test Implementation

### 3.1 Base Test Infrastructure

#### Create: `AbstractIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
```

### 3.2 OpaqueTokenService Integration Tests

#### Test File: `OpaqueTokenServiceIntegrationTest.java`

**Test Cases (15 tests for 90% coverage):**

1. **Token Generation Tests (5 tests)**
   - `shouldGenerateTokenAndStoreInRedis()`
   - `shouldGenerateUniqueTokensForDifferentUsers()`
   - `shouldStoreTokenWithCorrectExpiration()`
   - `shouldIncludeAllRequiredClaimsInToken()`
   - `shouldHandleRedisConnectionFailureGracefully()`

2. **Token Validation Tests (5 tests)**
   - `shouldValidateValidTokenSuccessfully()`
   - `shouldRejectExpiredToken()`
   - `shouldRejectInvalidatedToken()`
   - `shouldRejectMalformedToken()`
   - `shouldRejectTokenForDeletedUser()`

3. **Token Invalidation Tests (3 tests)**
   - `shouldInvalidateTokenInRedis()`
   - `shouldInvalidateAllUserTokens()`
   - `shouldHandleInvalidationOfNonExistentToken()`

4. **Cleanup Tests (2 tests)**
   - `shouldCleanupExpiredTokensFromRedis()`
   - `shouldNotDeleteActiveTokensDuringCleanup()`

**Implementation Strategy:**
```java
@SpringBootTest
@Testcontainers
class OpaqueTokenServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OpaqueTokenService tokenService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldGenerateTokenAndStoreInRedis() {
        // GIVEN: Real user in PostgreSQL
        User user = createTestUser("test@example.com");
        userRepository.save(user);

        // WHEN: Generate token (no mocks!)
        String token = tokenService.generateToken(user.getId(), "USER");

        // THEN: Verify in Redis
        assertThat(token).isNotNull();
        String redisKey = "token:" + token;
        assertThat(redisTemplate.hasKey(redisKey)).isTrue();

        // AND: Verify expiration set
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(3500L, 3600L); // ~1 hour
    }
}
```

### 3.3 AuthenticationService Integration Tests

#### Test File: `AuthenticationServiceIntegrationTest.java`

**Test Cases (12 tests for 90% coverage):**

1. **Authentication Tests (5 tests)**
   - `shouldAuthenticateValidUserCredentials()`
   - `shouldRejectInvalidPassword()`
   - `shouldRejectNonExistentUser()`
   - `shouldRejectInactiveUser()`
   - `shouldCreateAuditLogOnSuccessfulAuth()`

2. **Token Refresh Tests (3 tests)**
   - `shouldRefreshValidToken()`
   - `shouldRejectExpiredRefreshToken()`
   - `shouldRevokeOldTokenOnRefresh()`

3. **Logout Tests (2 tests)**
   - `shouldInvalidateTokenOnLogout()`
   - `shouldLogoutAllSessionsForUser()`

4. **Edge Cases (2 tests)**
   - `shouldHandleConcurrentAuthenticationAttempts()`
   - `shouldEnforceRateLimitingOnFailedAttempts()`

**Implementation Strategy:**
```java
@SpringBootTest
@Testcontainers
class AuthenticationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private OpaqueTokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldAuthenticateValidUserCredentials() {
        // GIVEN: User in database with encoded password
        User user = User.builder()
            .email("test@example.com")
            .passwordHash(passwordEncoder.encode("SecurePass123!"))
            .status(UserStatus.ACTIVE)
            .build();
        userRepository.save(user);

        // WHEN: Authenticate with real service (no mocks!)
        AuthenticationResponse response = authService.authenticate(
            "test@example.com",
            "SecurePass123!"
        );

        // THEN: Verify token generated and stored
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();

        // AND: Verify tokens exist in Redis
        assertThat(tokenService.validateToken(response.getAccessToken()))
            .isPresent();

        // AND: Verify audit log created in PostgreSQL
        AuditLog log = auditRepository.findByUserId(user.getId()).get(0);
        assertThat(log.getAction()).isEqualTo("USER_LOGIN");
    }
}
```

### 3.4 OpaqueTokenAuthenticationFilter Integration Tests

#### Test File: `OpaqueTokenAuthenticationFilterIntegrationTest.java`

**Test Cases (8 tests for 85% coverage):**

1. **Filter Success Cases (3 tests)**
   - `shouldAuthenticateRequestWithValidToken()`
   - `shouldPopulateSecurityContextWithUserDetails()`
   - `shouldAllowAccessToProtectedEndpoint()`

2. **Filter Rejection Cases (5 tests)**
   - `shouldReject401WithMissingToken()`
   - `shouldReject401WithInvalidToken()`
   - `shouldReject401WithExpiredToken()`
   - `shouldReject403WithInsufficientPermissions()`
   - `shouldBypassFilterForPublicEndpoints()`

**Implementation Strategy:**
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class OpaqueTokenAuthenticationFilterIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldAuthenticateRequestWithValidToken() {
        // GIVEN: Real user and real token in database/Redis
        User user = createAndSaveUser("test@example.com");
        String token = tokenService.generateToken(user.getId(), "USER");

        // WHEN: Make HTTP request with token (real HTTP call!)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/protected/resource",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        // THEN: Request succeeds
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReject401WithInvalidToken() {
        // WHEN: Request with invalid token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-token-12345");
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/protected/resource",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        // THEN: 401 Unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

### 3.5 SecurityConfig Integration Tests

#### Test File: `SecurityConfigIntegrationTest.java`

**Test Cases (10 tests for 80% coverage):**

1. **Security Rules Tests (6 tests)**
   - `shouldAllowPublicEndpointsWithoutAuth()`
   - `shouldProtectApiEndpointsRequireAuth()`
   - `shouldEnforceCorsConfiguration()`
   - `shouldEnforceCsrfProtectionForStatefulEndpoints()`
   - `shouldConfigureSecurityHeadersCorrectly()`
   - `shouldDisableSessionCreationForStatelessApi()`

2. **Authentication Flow Tests (4 tests)**
   - `shouldConfigureOAuth2ResourceServer()`
   - `shouldIntegrateOpaqueTokenFilter()`
   - `shouldHandleAuthenticationExceptions()`
   - `shouldHandleAccessDeniedExceptions()`

### 3.6 UserRepository Integration Tests

#### Test File: `UserRepositoryIntegrationTest.java`

**Test Cases (10 tests for 95% coverage):**

1. **Query Tests (6 tests)**
   - `shouldFindUserByEmail()`
   - `shouldNotFindDeletedUser()`
   - `shouldNotFindInactiveUser()`
   - `shouldFindUserByIdWithRoles()`
   - `shouldHandleNullEmailGracefully()`
   - `shouldHandleDuplicateEmailConstraint()`

2. **CRUD Tests (4 tests)**
   - `shouldCreateUserWithAuditFields()`
   - `shouldUpdateUserAndModifyTimestamp()`
   - `shouldSoftDeleteUser()`
   - `shouldHandleConcurrentUpdates()`

---

## Phase 4: Execution Plan

### Step-by-Step Execution Order

#### Day 1: Quick Wins (Checkstyle)
1. ✅ Run IntelliJ reformat on all 5 Java files
2. ✅ Apply manual fixes for design issues (final, javadoc)
3. ✅ Verify with `./gradlew checkstyleMain`
4. ✅ Commit: "fix: Resolve all 35 checkstyle warnings"

#### Day 2: Test Infrastructure
5. ✅ Create `AbstractIntegrationTest.java`
6. ✅ Configure TestContainers for PostgreSQL + Redis
7. ✅ Create test application.yml with profiles
8. ✅ Verify containers start: `./gradlew test --tests AbstractIntegrationTest`

#### Day 3-4: OpaqueTokenService Tests
9. ✅ Write 15 integration tests (TDD: fail first!)
10. ✅ Run tests: `./gradlew test --tests OpaqueTokenServiceIntegrationTest`
11. ✅ Verify coverage: `./gradlew jacocoTestReport`

#### Day 5-6: AuthenticationService Tests
12. ✅ Write 12 integration tests
13. ✅ Include audit logging verification
14. ✅ Test concurrent scenarios

#### Day 7: Filter + SecurityConfig Tests
15. ✅ Write 8 filter tests (HTTP integration)
16. ✅ Write 10 security config tests
17. ✅ Test full request/response cycle

#### Day 8: UserRepository + Cleanup
18. ✅ Write 10 repository tests
19. ✅ Add missing domain/DTO tests
20. ✅ Final coverage check

#### Day 9: Verification
21. ✅ Run full build: `./gradlew clean build`
22. ✅ Verify 0 checkstyle warnings
23. ✅ Verify ≥85% coverage
24. ✅ Verify all tests pass with real databases

---

## Phase 5: Quality Gates

### Pre-Commit Checklist
- [ ] All 35 checkstyle warnings resolved
- [ ] Code coverage ≥ 85%
- [ ] All tests pass
- [ ] No mocks used (verify grep for "Mockito")
- [ ] All TestContainers start successfully
- [ ] Build completes: `./gradlew clean build`

### Test Execution Metrics
```bash
# Expected results:
Total Tests: ~70 integration tests
Pass Rate: 100%
Coverage: 85%+
Build Time: ~3-5 minutes (TestContainers overhead)
Database: PostgreSQL 15 + Redis 7
```

### Coverage Verification
```bash
cd backend
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html

# Should show:
# - Overall coverage: ≥85%
# - OpaqueTokenService: ≥90%
# - AuthenticationService: ≥90%
# - OpaqueTokenAuthenticationFilter: ≥85%
# - SecurityConfig: ≥80%
# - UserRepository: ≥95%
```

---

## Phase 6: Anti-Patterns to Avoid

### ❌ What NOT to Do
1. **No Mockito**: Do not use `@Mock`, `@MockBean`, `when()`, `verify()`
2. **No H2**: Do not use in-memory databases - use TestContainers
3. **No @MockMvc without real context**: Always use full Spring Boot context
4. **No blank test files**: Every test must have real assertions
5. **No @Disabled tests**: Fix or delete, never disable

### ✅ What TO Do
1. **Real PostgreSQL**: Every test uses TestContainers PostgreSQL
2. **Real Redis**: Token storage uses TestContainers Redis
3. **Real HTTP**: Use TestRestTemplate for filter tests
4. **Real Services**: Wire actual Spring beans, not mocks
5. **Real Transactions**: Test rollback/commit behavior
6. **Real SQL**: Verify actual database state with JDBC queries

---

## Phase 7: Success Criteria

### Build Success
```bash
cd backend && ./gradlew clean build

# Expected output:
> Task :checkstyleMain       ✓ 0 violations
> Task :test                 ✓ 70 tests passed
> Task :jacocoTestReport     ✓ Generated
> Task :jacocoTestCoverageVerification ✓ PASSED
BUILD SUCCESSFUL in 3m 45s
```

### Coverage Report
```
backend
├── auth
│   ├── OpaqueTokenService        92% ✓
│   ├── AuthenticationService     91% ✓
│   ├── OpaqueTokenAuthFilter     87% ✓
│   ├── SecurityConfig            82% ✓
│   └── UserRepository            96% ✓
├── audit                         88% ✓
├── user                          86% ✓
└── Overall                       87% ✓ (target: 85%)
```

---

## Appendix A: TestContainers Configuration

### docker-compose.test.yml (for reference)
```yaml
version: '3.8'
services:
  postgres-test:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: testdb
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "5432"

  redis-test:
    image: redis:7-alpine
    ports:
      - "6379"
```

### application-integration-test.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  redis:
    timeout: 2000ms

logging:
  level:
    com.platform: DEBUG
    org.hibernate.SQL: DEBUG
```

---

## Appendix B: Verification Commands

```bash
# 1. Check checkstyle
cd backend && ./gradlew checkstyleMain checkstyleTest

# 2. Run tests with coverage
cd backend && ./gradlew clean test jacocoTestReport

# 3. Verify coverage threshold
cd backend && ./gradlew jacocoTestCoverageVerification

# 4. Full build
cd backend && ./gradlew clean build

# 5. Check for mocks (should return nothing)
grep -r "Mockito\|@Mock\|@MockBean" backend/src/test/

# 6. View coverage report
open backend/build/reports/jacoco/test/html/index.html
```

---

## Timeline: 9 Days to Zero Warnings + 85% Coverage

| Day | Tasks | Deliverable |
|-----|-------|-------------|
| 1 | Fix all checkstyle issues | 0 warnings |
| 2 | Setup test infrastructure | TestContainers working |
| 3 | OpaqueTokenService tests (part 1) | 8 tests passing |
| 4 | OpaqueTokenService tests (part 2) | 15 tests passing |
| 5 | AuthenticationService tests (part 1) | 6 tests passing |
| 6 | AuthenticationService tests (part 2) | 12 tests passing |
| 7 | Filter + SecurityConfig tests | 18 tests passing |
| 8 | UserRepository + remaining tests | 70 tests passing |
| 9 | Verification + documentation | 85%+ coverage achieved |

---

**STATUS**: Ready to execute
**DEPENDENCIES**: PostgreSQL 15, Redis 7, Java 21, Gradle 8.5
**RISKS**: TestContainers requires Docker Desktop running
**MITIGATION**: Verify Docker with `docker ps` before starting

---
*Plan created: 2025-10-02*
*Target completion: 2025-10-11*
