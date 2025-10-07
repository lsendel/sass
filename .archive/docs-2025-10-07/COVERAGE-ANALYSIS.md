# Test Coverage Analysis - Current State (17% → Target: 85%)

## Executive Summary

**Current Coverage**: 17% (1,084 of 6,041 instructions covered)
**Target Coverage**: 85%
**Gap**: 68 percentage points
**Critical Finding**: **Auth module has 0% coverage** (495 missed instructions)

---

## Overall Metrics

| Metric           | Missed | Covered | Total | Coverage |
| ---------------- | ------ | ------- | ----- | -------- |
| **Instructions** | 4,957  | 1,084   | 6,041 | **17%**  |
| **Branches**     | 405    | 47      | 452   | **10%**  |
| **Lines**        | 1,260  | 229     | 1,489 | **15%**  |
| **Methods**      | 432    | 72      | 504   | **14%**  |
| **Classes**      | 44     | 32      | 76    | **42%**  |

---

## Coverage by Module (Worst to Best)

### 🔴 CRITICAL: Zero Coverage Modules (Must Test First)

#### 1. **com.platform.auth.internal** - 0% coverage ⚠️

- **Missed Instructions**: 495 (largest gap)
- **Missed Branches**: 30
- **Missed Lines**: 127
- **Missed Methods**: 35
- **Classes**: 4 (all uncovered)

**Files:**

- `OpaqueTokenService.java` - Token generation, validation, cleanup
- `AuthenticationService.java` - Authentication, login, logout
- `OpaqueTokenAuthenticationFilter.java` - Security filter
- `SecurityConfig.java` - Security configuration
- `UserRepository.java` - Database queries

**Priority**: **HIGHEST** - This module handles authentication security

---

#### 2. **com.platform.shared.types** - 0% coverage

- **Missed Instructions**: 178
- **Missed Branches**: 20
- **Missed Lines**: 29
- **Missed Methods**: 15
- **Classes**: 1

**Priority**: **HIGH** - Shared types used across all modules

---

#### 3. **com.platform.user.api.dto** - 0% coverage

- **Missed Instructions**: 91
- **Missed Lines**: 16
- **Missed Methods**: 4
- **Classes**: 3

**Priority**: **MEDIUM** - DTOs for user module API

---

#### 4. **com.platform.auth.api** - 0% coverage

- **Missed Instructions**: 90
- **Missed Branches**: 2
- **Missed Lines**: 26
- **Missed Methods**: 4
- **Classes**: 1

**Priority**: **HIGH** - Auth module public API

---

#### 5. **com.platform.user.events** - 0% coverage

- **Missed Instructions**: 60
- **Missed Lines**: 6
- **Missed Methods**: 5
- **Classes**: 4

**Priority**: **MEDIUM** - Event definitions for user module

---

#### 6. **com.platform.auth.api.dto** - 0% coverage

- **Missed Instructions**: 0 (minimal code)
- **Lines**: 2
- **Methods**: 2
- **Classes**: 2

**Priority**: **LOW** - Small DTOs, test with contract tests

---

#### 7. **com.platform.auth.events** - 0% coverage

- **Missed Instructions**: 0 (minimal code)
- **Lines**: 1
- **Methods**: 1
- **Classes**: 1

**Priority**: **LOW** - Single event class

---

#### 8. **com.platform.audit.domain** - 0% coverage

- **Missed Instructions**: 27
- **Lines**: 10
- **Methods**: 5
- **Classes**: 1

**Priority**: **MEDIUM** - Domain models for audit

---

#### 9. **com.platform.shared.exceptions** - 0% coverage

- **Missed Instructions**: 17
- **Lines**: 8
- **Methods**: 4
- **Classes**: 3

**Priority**: **LOW** - Exception classes (test via service tests)

---

#### 10. **com.platform** (main application) - 0% coverage

- **Missed Instructions**: 0 (minimal code)
- **Lines**: 2
- **Methods**: 1
- **Classes**: 1

**Priority**: **LOW** - Application bootstrap class

---

#### 11. **com.platform.shared.events** - 0% coverage

- **Missed Instructions**: 0 (minimal code)
- **Lines**: 1
- **Methods**: 1
- **Classes**: 1

**Priority**: **LOW** - Base event definitions

---

### 🟡 PARTIAL COVERAGE: Needs Improvement

#### 12. **com.platform.shared.security** - 37% coverage ⭐ (Best)

- **Covered Instructions**: 114 of 307 (37%)
- **Covered Branches**: 4 of 28 (14%)
- **Covered Lines**: 25 of 64 (39%)
- **Covered Methods**: 9 of 33 (27%)
- **Classes**: 2 of 2 (100%)

**Status**: Some tests exist, need to increase to 85%+

---

#### 13. **com.platform.audit.api** - 24% coverage

- **Covered Instructions**: 218 of 882 (24%)
- **Covered Branches**: 8 of 44 (18%)
- **Covered Lines**: 46 of 225 (20%)
- **Covered Methods**: 11 of 42 (26%)
- **Classes**: 3 of 5 (60%)

**Status**: Basic contract tests exist, need integration tests

---

#### 14. **com.platform.audit.internal** - 22% coverage

- **Covered Instructions**: 456 of 2,029 (22%)
- **Covered Branches**: 33 of 204 (16%)
- **Covered Lines**: 108 of 516 (21%)
- **Covered Methods**: 34 of 155 (22%)
- **Classes**: 12 of 16 (75%)

**Status**: Some integration tests exist, need comprehensive coverage

---

#### 15. **com.platform.audit.api.dto** - 20% coverage

- **Covered Instructions**: 162 of 786 (20%)
- **Covered Branches**: 2 of 74 (2%)
- **Covered Lines**: 9 of 144 (6%)
- **Covered Methods**: 6 of 68 (9%)
- **Classes**: 4 of 19 (21%)

**Status**: DTO tests needed (contract tests)

---

#### 16. **com.platform.auth** - 18% coverage

- **Covered Instructions**: 36 of 200 (18%)
- **Covered Branches**: 0 of 14 (0%)
- **Covered Lines**: 9 of 64 (14%)
- **Covered Methods**: 2 of 28 (7%)
- **Classes**: 2 of 2 (100%)

**Status**: Minimal coverage, need comprehensive tests

---

#### 17. **com.platform.user** - 16% coverage

- **Covered Instructions**: 60 of 353 (16%)
- **Covered Branches**: 0 of 22 (0%)
- **Covered Lines**: 15 of 119 (12%)
- **Covered Methods**: 4 of 61 (6%)
- **Classes**: 4 of 4 (100%)

**Status**: Basic structure tested, need full coverage

---

#### 18. **com.platform.user.api** - 10% coverage

- **Covered Instructions**: 0 of 105 (0% shown, but 10% calculated)
- **Lines**: 6 of 37 (16%)
- **Methods**: 2 of 11 (18%)
- **Classes**: 2 of 2 (100%)

**Status**: Minimal API tests exist

---

#### 19. **com.platform.config** - 8% coverage

- **Covered Instructions**: 0 of 54 (minimal coverage)
- **Covered Branches**: 0 of 6
- **Covered Lines**: 2 of 16 (12%)
- **Covered Methods**: 2 of 5 (40%)
- **Classes**: 1 of 2 (50%)

**Status**: Configuration class, test via integration tests

---

#### 20. **com.platform.user.internal** - 6% coverage

- **Covered Instructions**: 21 of 314 (6%)
- **Covered Branches**: 0 of 8 (0%)
- **Covered Lines**: 9 of 76 (12%)
- **Covered Methods**: 2 of 24 (8%)
- **Classes**: 2 of 2 (100%)

**Status**: Very low coverage, need comprehensive tests

---

## Test Coverage Gaps - Detailed Analysis

### Auth Module (0% → Need 90%)

**Gap**: 90 percentage points
**Test Count Needed**: ~25 tests

**Required Test Files:**

1. `OpaqueTokenServiceIntegrationTest.java` - 15 tests
   - Token generation (5 tests)
   - Token validation (5 tests)
   - Token invalidation (3 tests)
   - Token cleanup (2 tests)

2. `AuthenticationServiceIntegrationTest.java` - 12 tests
   - Authentication flows (5 tests)
   - Token refresh (3 tests)
   - Logout (2 tests)
   - Edge cases (2 tests)

3. `OpaqueTokenAuthenticationFilterIntegrationTest.java` - 8 tests
   - Filter success cases (3 tests)
   - Filter rejection cases (5 tests)

4. `SecurityConfigIntegrationTest.java` - 10 tests
   - Security rules (6 tests)
   - Authentication flow (4 tests)

5. `UserRepositoryIntegrationTest.java` - 10 tests
   - Query tests (6 tests)
   - CRUD tests (4 tests)

**Total**: 55 integration tests (real PostgreSQL + Redis via TestContainers)

---

### Audit Module (22% → Need 90%)

**Gap**: 68 percentage points
**Test Count Needed**: ~15 additional tests

**Existing Coverage**: Some contract tests exist
**Missing**: Integration tests with real database

**Required Test Files:**

1. `AuditServiceIntegrationTest.java` - 10 tests
2. `AuditExportServiceIntegrationTest.java` - 8 tests
3. `AuditRetentionServiceIntegrationTest.java` - 5 tests

**Total**: 23 additional tests

---

### User Module (16% → Need 90%)

**Gap**: 74 percentage points
**Test Count Needed**: ~20 tests

**Required Test Files:**

1. `UserServiceIntegrationTest.java` - 12 tests
2. `OrganizationServiceIntegrationTest.java` - 10 tests
3. `UserInvitationServiceIntegrationTest.java` - 8 tests

**Total**: 30 tests

---

### Shared Module (37% → Need 90%)

**Gap**: 53 percentage points
**Test Count Needed**: ~10 additional tests

**Best performing module** - build on existing tests

**Required Test Files:**

1. `SecurityUtilsTest.java` - 8 tests
2. `TypeConverterTest.java` - 6 tests
3. `ExceptionHandlerTest.java` - 4 tests

**Total**: 18 tests

---

## Execution Strategy: 4 Phases

### Phase 1: Critical Auth Module (Days 1-4)

**Objective**: 0% → 90% coverage in auth module
**Tests**: 55 integration tests
**Files**: 5 test files
**Infrastructure**: Setup TestContainers (PostgreSQL + Redis)

**Expected Coverage Increase**: +30% overall (auth is 8% of codebase)

---

### Phase 2: Audit Module (Days 5-6)

**Objective**: 22% → 90% coverage in audit module
**Tests**: 23 additional integration tests
**Files**: 3 test files

**Expected Coverage Increase**: +20% overall (audit is 33% of codebase)

---

### Phase 3: User Module (Days 7-8)

**Objective**: 16% → 90% coverage in user module
**Tests**: 30 integration tests
**Files**: 3 test files

**Expected Coverage Increase**: +15% overall (user is 20% of codebase)

---

### Phase 4: Shared + DTOs (Day 9)

**Objective**: Bring remaining modules to 85%+
**Tests**: 25 unit/integration tests
**Files**: 6 test files

**Expected Coverage Increase**: +20% overall (covers remaining 39% of codebase)

---

## Coverage Projection After All Tests

| Module          | Current | After Phase 1 | After Phase 2 | After Phase 3 | After Phase 4 | Target     |
| --------------- | ------- | ------------- | ------------- | ------------- | ------------- | ---------- |
| auth.internal   | 0%      | **90%**       | 90%           | 90%           | 90%           | 90% ✅     |
| audit.internal  | 22%     | 22%           | **90%**       | 90%           | 90%           | 90% ✅     |
| user.internal   | 16%     | 16%           | 16%           | **90%**       | 90%           | 90% ✅     |
| shared.security | 37%     | 37%           | 37%           | 37%           | **90%**       | 90% ✅     |
| DTOs/Events     | ~10%    | ~10%          | ~10%          | ~10%          | **85%**       | 85% ✅     |
| **Overall**     | **17%** | **47%**       | **67%**       | **82%**       | **87%**       | **85%** ✅ |

---

## Test Infrastructure Requirements

### TestContainers Setup

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");

@Container
static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379);
```

### Required Test Dependencies (Already in build.gradle)

- ✅ TestContainers PostgreSQL
- ✅ TestContainers Redis (generic)
- ✅ Spring Boot Test
- ✅ JUnit 5
- ✅ AssertJ
- ✅ Mockito (avoid using - use real dependencies)

---

## Validation Checklist

### After Each Phase

- [ ] Run tests: `./gradlew test`
- [ ] Generate coverage: `./gradlew jacocoTestReport`
- [ ] Check coverage: Open `build/jacocoHtml/index.html`
- [ ] Verify no mocks: `grep -r "@Mock" src/test/`
- [ ] Verify TestContainers: Check Docker logs
- [ ] Run checkstyle: `./gradlew checkstyleTest`

### Final Validation

- [ ] Overall coverage ≥ 85%
- [ ] Auth module coverage ≥ 90%
- [ ] Audit module coverage ≥ 90%
- [ ] User module coverage ≥ 90%
- [ ] All tests pass (100% pass rate)
- [ ] Build succeeds: `./gradlew clean build`
- [ ] No checkstyle violations
- [ ] No mocks in codebase

---

## Risk Assessment

### High Risks

1. **TestContainers Performance**: Docker startup adds 30-60s per test run
   - **Mitigation**: Use shared containers across tests

2. **Redis Connection Issues**: Redis container may fail to start
   - **Mitigation**: Add retry logic and health checks

3. **Database State Management**: Tests may interfere with each other
   - **Mitigation**: Use `@Transactional` on tests, rollback after each test

### Medium Risks

1. **Time Constraints**: 9 days is tight for 133 tests
   - **Mitigation**: Focus on critical paths first (auth → audit → user)

2. **Complexity of Auth Flow**: OAuth2 + opaque tokens is complex
   - **Mitigation**: Start with simple token generation, then validation

### Low Risks

1. **Checkstyle Conflicts**: New tests may violate style rules
   - **Mitigation**: Run checkstyle frequently during development

---

## Success Metrics

### Code Coverage

- ✅ Overall: 87% (target: 85%)
- ✅ Auth: 90%
- ✅ Audit: 90%
- ✅ User: 90%
- ✅ Shared: 90%

### Test Quality

- ✅ 133 integration tests with real dependencies
- ✅ 0 mocks (100% real dependencies)
- ✅ 100% test pass rate
- ✅ Average test execution time: <5 minutes

### Build Health

- ✅ 0 checkstyle warnings
- ✅ 0 build errors
- ✅ All ArchUnit tests passing
- ✅ Clean build: `./gradlew clean build`

---

**Analysis Date**: 2025-10-02
**Current Coverage**: 17%
**Target Coverage**: 85%
**Gap**: 68 percentage points
**Estimated Tests**: 133 integration tests
**Estimated Timeline**: 9 days
