# API Testing Guide - SASS Platform

## Overview

This guide provides comprehensive instructions for testing all APIs in the SASS (Spring Boot Modulith Payment Platform) project. The testing infrastructure has been enhanced with proper configuration, fixtures, and orchestration.

## Test Architecture

### Backend Test Structure

```
backend/src/test/java/
├── com/platform/
│   ├── audit/api/          # API endpoint tests
│   ├── config/             # Test configurations
│   ├── fixtures/           # Test data factories
│   └── integration/        # Integration tests
```

### Frontend Test Structure

```
frontend/tests/
├── e2e/                    # End-to-end tests
├── integration/            # Integration tests
└── unit/                   # Unit tests
```

## Test Types & Execution Order

### 1. Contract Tests (Backend)

Tests API contracts and endpoint behavior.

```bash
cd backend
./gradlew test --tests "*ContractTest"
```

### 2. Unit Tests (Backend)

Tests individual components in isolation.

```bash
cd backend
./gradlew test --tests "*UnitTest"
```

### 3. Integration Tests (Backend)

Tests with real database connections.

```bash
cd backend
./gradlew test --tests "*IntegrationTest"
```

### 4. Unit Tests (Frontend)

Tests React components and utilities.

```bash
cd frontend
npm run test:unit
```

### 5. End-to-End Tests (Full Stack)

Tests complete user workflows with backend running.

```bash
# Use orchestration script (recommended)
./test-e2e.sh

# Or manually:
cd frontend
npm run test:e2e
```

## Enhanced Test Infrastructure

### Fixed Configurations

#### 1. Spring Test Configuration

- **File**: `backend/src/test/resources/application-test.yml`
- **Changes**:
  - Enabled H2 in-memory database
  - Fixed Hibernate configuration
  - Removed deprecated OAuth2 exclusions
  - Added H2 console for debugging

#### 2. Test Bean Configuration

- **File**: `backend/src/test/java/com/platform/config/TestBeanConfiguration.java`
- **Purpose**: Provides mocked beans for testing
- **Features**:
  - Mock repositories and services
  - Password encoder bean
  - Test-specific configurations

#### 3. Test Data Fixtures

- **File**: `backend/src/test/java/com/platform/fixtures/AuditTestDataFixtures.java`
- **Purpose**: Factory methods for test data
- **Features**:
  - Reusable audit events and DTOs
  - Configurable test scenarios
  - Aligned with actual entity structures

### Test Orchestration

#### E2E Test Script

- **File**: `test-e2e.sh`
- **Features**:
  - Automatic backend startup if needed
  - Health check verification
  - Clean shutdown after tests
  - Error handling and reporting

```bash
# Make executable
chmod +x test-e2e.sh

# Run E2E tests with orchestration
./test-e2e.sh
```

## Gradle Test Tasks

### New Enhanced Tasks

#### Test Pipeline

```bash
cd backend
./gradlew testPipeline
```

Runs: compilation → tests → coverage reports

#### E2E Pipeline

```bash
cd backend
./gradlew e2ePipeline
```

Runs: E2E tests with backend orchestration

#### Security Pipeline

```bash
cd backend
./gradlew securityPipeline
```

Runs: checkstyle → security tests → coverage

## Configuration Files

### Backend Test Profiles

#### application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true
```

#### application-integration-test.yml

For integration tests with TestContainers.

#### application-contract-test.yml

For contract testing configuration.

### Frontend Test Configuration

#### vitest.config.ts

- Unit test configuration
- Coverage settings
- Test environment setup

#### playwright.config.ts

- E2E test configuration
- Browser settings
- Test parallelization

## Test Data Management

### Audit Test Fixtures

```java
// Create single audit event
AuditEvent event = AuditTestDataFixtures.createAuditEvent();

// Create multiple events
List<AuditEvent> events = AuditTestDataFixtures.createAuditEvents(5);

// Create DTOs
AuditLogEntryDTO entry = AuditTestDataFixtures.createAuditLogEntryDTO();
AuditLogDetailDTO detail = AuditTestDataFixtures.createAuditLogDetailDTO();

// Create search filters
Map<String, String> filters = AuditTestDataFixtures.createSearchFilters();
```

### Best Practices

1. **Use Factory Methods**: Always use test fixtures for consistent data
2. **Isolation**: Each test should be independent
3. **Cleanup**: Tests clean up after themselves
4. **Realistic Data**: Test data should mirror production scenarios

## Common Issues & Solutions

### Spring Context Failures

**Problem**: Tests fail with ApplicationContext errors

**Solution**:

1. Ensure correct test profile is active: `@ActiveProfiles("test")`
2. Import test configuration: `@Import(TestBeanConfiguration.class)`
3. Check application-test.yml is properly configured

### Database Issues

**Problem**: JPA/Hibernate errors in tests

**Solution**:

1. Verify H2 database configuration
2. Check entity mappings
3. Ensure proper DDL settings (`ddl-auto: create-drop`)

### E2E Test Timeouts

**Problem**: E2E tests timeout waiting for backend

**Solution**:

1. Use the test orchestration script: `./test-e2e.sh`
2. Verify backend health endpoint is accessible
3. Check port conflicts (default: 8080)

### Mock Bean Issues

**Problem**: NoSuchBeanDefinitionException

**Solution**:

1. Ensure TestBeanConfiguration is imported
2. Add missing @MockBean annotations
3. Verify bean names match exactly

## Performance Testing

### Coverage Requirements

- **Minimum Line Coverage**: 85%
- **Branch Coverage**: 80%
- **Enforced by**: jacocoTestCoverageVerification task

### Execution Times

- **Unit Tests**: < 30 seconds
- **Integration Tests**: < 2 minutes
- **E2E Tests**: < 5 minutes
- **Full Pipeline**: < 10 minutes

## Continuous Integration

### Pipeline Stages

1. **Compilation**: `./gradlew compileTestJava`
2. **Unit Tests**: `./gradlew test`
3. **Integration Tests**: `./gradlew test --tests "*IntegrationTest"`
4. **E2E Tests**: `./test-e2e.sh`
5. **Security Tests**: `./gradlew securityTest`
6. **Coverage**: `./gradlew jacocoTestReport`

### Success Criteria

- All tests pass
- Coverage thresholds met
- No security vulnerabilities
- Code style compliance (Checkstyle)

## Debugging Tests

### Backend Debug

```bash
# Enable debug logging
./gradlew test --debug --tests "*YourTest"

# View H2 console (when tests are running)
http://localhost:<port>/h2-console
```

### Frontend Debug

```bash
# Run tests with UI
npm run test:ui

# Debug E2E tests
npm run test:e2e:debug
```

### Logs and Reports

- **Test Reports**: `backend/build/reports/tests/test/`
- **Coverage Reports**: `backend/build/reports/jacoco/`
- **Checkstyle Reports**: `backend/build/reports/checkstyle/`
- **E2E Reports**: `frontend/test-results/`

## Quick Reference

### Essential Commands

```bash
# Backend unit tests
cd backend && ./gradlew test

# Frontend unit tests
cd frontend && npm run test:unit

# E2E tests (orchestrated)
./test-e2e.sh

# Full test pipeline
cd backend && ./gradlew testPipeline

# Coverage report
cd backend && ./gradlew jacocoTestReport

# Security tests
cd backend && ./gradlew securityTest
```

### Test File Naming Conventions

- **Contract Tests**: `*ContractTest.java`
- **Integration Tests**: `*IntegrationTest.java`
- **Unit Tests**: `*Test.java` or `*UnitTest.java`
- **E2E Tests**: `*.spec.ts`

---

**Note**: This testing infrastructure provides a robust foundation for comprehensive API testing. The configuration fixes and orchestration ensure reliable test execution across all components of the SASS platform.
