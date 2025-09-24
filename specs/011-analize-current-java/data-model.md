# Data Model: API Integration Testing Framework

**Date**: 2025-09-24
**Feature**: Integration Testing Framework Entities

## Core Entities

### TestSuite
**Purpose**: Represents a collection of related tests for a module or feature
**Fields**:
- `id`: UUID - Unique identifier
- `name`: String - Suite name (e.g., "AuthModuleIntegrationTests")
- `module`: Enum - Target module (AUTH, PAYMENT, USER, SUBSCRIPTION, AUDIT, SHARED)
- `category`: Enum - Test category (CONTRACT, INTEGRATION, E2E, UNIT)
- `priority`: Enum - Execution priority (CRITICAL, MAJOR, MINOR)
- `enabled`: Boolean - Whether suite is active
- `parallelizable`: Boolean - Can run in parallel with other suites
- `maxRetries`: Integer - Maximum retry attempts for flaky tests
- `timeoutSeconds`: Integer - Suite-level timeout
- `tags`: List<String> - Tags for filtering (e.g., ["smoke", "regression"])
- `testScenarios`: List<TestScenario> - Child test scenarios

**Relationships**:
- Contains multiple TestScenarios (1:N)
- Belongs to TestEnvironment (N:1)
- Produces TestReport (1:1 per execution)

### TestScenario
**Purpose**: Individual test case with setup, execution, and validation
**Fields**:
- `id`: UUID - Unique identifier
- `name`: String - Scenario name
- `description`: String - Test purpose and expected behavior
- `testSuiteId`: UUID - Parent suite reference
- `endpoint`: String - API endpoint being tested
- `httpMethod`: Enum - HTTP method (GET, POST, PUT, DELETE, PATCH)
- `setupSteps`: List<TestStep> - Preparation steps
- `executionSteps`: List<TestStep> - Main test steps
- `validationSteps`: List<TestStep> - Assertion steps
- `cleanupSteps`: List<TestStep> - Teardown steps
- `testData`: Map<String, Object> - Test-specific data
- `expectedResponseCode`: Integer - Expected HTTP status
- `expectedResponseTime`: Integer - Max response time (ms)
- `dependencies`: List<String> - Other scenarios that must run first

**Relationships**:
- Belongs to TestSuite (N:1)
- Uses TestFixtures (N:N)
- Generates TestResult (1:1 per execution)

### TestEnvironment
**Purpose**: Configuration profile for different deployment targets
**Fields**:
- `id`: UUID - Unique identifier
- `name`: String - Environment name (LOCAL, CI, STAGING, PRODUCTION)
- `baseUrl`: String - API base URL
- `databaseUrl`: String - Database connection string
- `redisUrl`: String - Redis connection string
- `stripeApiKey`: String - Stripe test API key (encrypted)
- `oauth2Config`: Map<String, String> - OAuth2 provider configurations
- `featureFlags`: Map<String, Boolean> - Feature toggles
- `maxParallelThreads`: Integer - Parallel execution limit
- `containerConfig`: ContainerConfig - TestContainers configuration
- `active`: Boolean - Whether environment is available

**Relationships**:
- Used by multiple TestSuites (1:N)
- Configures TestExecution (1:N)

### TestFixture
**Purpose**: Reusable test data and mock configurations
**Fields**:
- `id`: UUID - Unique identifier
- `name`: String - Fixture name
- `type`: Enum - Fixture type (USER, ORGANIZATION, PAYMENT, SUBSCRIPTION)
- `data`: JSON - Fixture data payload
- `sqlScript`: String - Database setup script
- `mockResponses`: List<MockResponse> - External service mocks
- `validFrom`: LocalDateTime - Fixture validity start
- `validUntil`: LocalDateTime - Fixture validity end
- `tags`: List<String> - Categorization tags

**Relationships**:
- Used by multiple TestScenarios (N:N)
- Belongs to TestDataFactory (N:1)

### TestExecution
**Purpose**: Represents a single test run instance
**Fields**:
- `id`: UUID - Unique identifier
- `executionId`: String - Unique execution identifier
- `testSuiteId`: UUID - Suite being executed
- `environmentId`: UUID - Target environment
- `startTime`: LocalDateTime - Execution start
- `endTime`: LocalDateTime - Execution end
- `status`: Enum - Execution status (RUNNING, PASSED, FAILED, SKIPPED)
- `triggeredBy`: String - User or system that triggered execution
- `triggerType`: Enum - Trigger type (MANUAL, SCHEDULED, CI_CD, API)
- `gitCommit`: String - Git commit hash
- `gitBranch`: String - Git branch name
- `parallelThreads`: Integer - Number of parallel threads used
- `testResults`: List<TestResult> - Individual test results

**Relationships**:
- Executes TestSuite (N:1)
- In TestEnvironment (N:1)
- Produces TestResults (1:N)
- Generates TestReport (1:1)

### TestResult
**Purpose**: Result of a single test scenario execution
**Fields**:
- `id`: UUID - Unique identifier
- `executionId`: UUID - Parent execution reference
- `testScenarioId`: UUID - Scenario that was executed
- `status`: Enum - Result status (PASSED, FAILED, SKIPPED, ERROR)
- `startTime`: LocalDateTime - Test start time
- `endTime`: LocalDateTime - Test end time
- `duration`: Long - Execution time in milliseconds
- `actualResponseCode`: Integer - Actual HTTP status received
- `actualResponseTime`: Long - Actual response time
- `request`: TestRequest - Request details
- `response`: TestResponse - Response details
- `assertions`: List<Assertion> - Assertion results
- `errorMessage`: String - Error description if failed
- `stackTrace`: String - Full stack trace if error
- `screenshots`: List<String> - Screenshot URLs if UI test
- `retryCount`: Integer - Number of retry attempts

**Relationships**:
- Part of TestExecution (N:1)
- For TestScenario (N:1)

### TestReport
**Purpose**: Comprehensive test execution report
**Fields**:
- `id`: UUID - Unique identifier
- `executionId`: UUID - Execution reference
- `reportType`: Enum - Report format (HTML, JSON, XML, PDF)
- `generatedAt`: LocalDateTime - Report generation time
- `summary`: ReportSummary - Execution summary
- `moduleResults`: Map<String, ModuleResult> - Per-module results
- `coverageMetrics`: CoverageMetrics - Code coverage data
- `performanceMetrics`: PerformanceMetrics - Performance statistics
- `failureAnalysis`: FailureAnalysis - Failure categorization
- `trends`: TrendAnalysis - Historical comparison
- `recommendations`: List<String> - Improvement suggestions
- `artifactLinks`: List<String> - Related artifact URLs

**Relationships**:
- For TestExecution (1:1)
- Contains aggregated TestResults

### TestStep
**Purpose**: Atomic test action within a scenario
**Fields**:
- `order`: Integer - Execution order
- `action`: Enum - Action type (REQUEST, ASSERT, WAIT, SET_VARIABLE, DATABASE_OPERATION)
- `target`: String - Target of the action
- `parameters`: Map<String, Object> - Action parameters
- `expectedOutcome`: String - Expected result
- `optional`: Boolean - Whether step failure should fail the test

### TestRequest
**Purpose**: Captured HTTP request details
**Fields**:
- `method`: String - HTTP method
- `url`: String - Full URL
- `headers`: Map<String, String> - Request headers
- `body`: String - Request body (JSON)
- `queryParams`: Map<String, String> - Query parameters
- `formParams`: Map<String, String> - Form parameters

### TestResponse
**Purpose**: Captured HTTP response details
**Fields**:
- `statusCode`: Integer - HTTP status code
- `headers`: Map<String, String> - Response headers
- `body`: String - Response body (JSON)
- `responseTime`: Long - Response time in ms
- `size`: Long - Response size in bytes

## Value Objects

### ContainerConfig
**Fields**:
- `postgresImage`: String - PostgreSQL Docker image
- `redisImage`: String - Redis Docker image
- `reuseContainers`: Boolean - Container reuse flag
- `networkAlias`: String - Docker network name

### ReportSummary
**Fields**:
- `totalTests`: Integer
- `passed`: Integer
- `failed`: Integer
- `skipped`: Integer
- `errorRate`: Double
- `averageResponseTime`: Double
- `p95ResponseTime`: Double
- `totalDuration`: Long

### ModuleResult
**Fields**:
- `moduleName`: String
- `testCount`: Integer
- `passCount`: Integer
- `failCount`: Integer
- `coverage`: Double
- `criticalFailures`: List<String>

### CoverageMetrics
**Fields**:
- `lineCoverage`: Double
- `branchCoverage`: Double
- `methodCoverage`: Double
- `classCoverage`: Double
- `packageCoverage`: Double

### PerformanceMetrics
**Fields**:
- `averageResponseTime`: Double
- `medianResponseTime`: Double
- `p95ResponseTime`: Double
- `p99ResponseTime`: Double
- `minResponseTime`: Long
- `maxResponseTime`: Long
- `throughput`: Double

### FailureAnalysis
**Fields**:
- `failureCategories`: Map<String, Integer>
- `flakyTests`: List<String>
- `newFailures`: List<String>
- `fixedTests`: List<String>
- `regressions`: List<String>

### TrendAnalysis
**Fields**:
- `executionTrend`: List<TrendPoint>
- `successRateTrend`: List<TrendPoint>
- `performanceTrend`: List<TrendPoint>
- `coverageTrend`: List<TrendPoint>

### Assertion
**Fields**:
- `type`: String - Assertion type
- `expected`: Object - Expected value
- `actual`: Object - Actual value
- `passed`: Boolean - Assertion result
- `message`: String - Assertion message

### MockResponse
**Fields**:
- `service`: String - External service name
- `endpoint`: String - Service endpoint
- `method`: String - HTTP method
- `requestMatcher`: String - Request matching pattern
- `responseStatus`: Integer - Response status code
- `responseBody`: String - Response body
- `responseHeaders`: Map<String, String>
- `delay`: Integer - Response delay in ms

## State Transitions

### TestExecution States
```
PENDING → INITIALIZING → RUNNING → [PASSED|FAILED|ERROR] → COMPLETED → REPORTED
                ↓                           ↓
             SKIPPED                    CANCELLED
```

### TestScenario States
```
NOT_STARTED → SETUP → EXECUTING → VALIDATING → CLEANUP → [PASSED|FAILED|ERROR|SKIPPED]
                ↓          ↓           ↓           ↓
              FAILED     FAILED     FAILED     FAILED
```

## Validation Rules

### TestSuite
- Name must be unique within module
- At least one TestScenario required
- Timeout must be between 1 and 3600 seconds
- MaxRetries between 0 and 3

### TestScenario
- Name must be unique within TestSuite
- Endpoint must be valid API path
- ExpectedResponseCode must be valid HTTP status
- ExpectedResponseTime must be positive
- At least one validation step required

### TestEnvironment
- Name must be unique
- BaseUrl must be valid URL
- Database and Redis URLs required for integration tests
- MaxParallelThreads between 1 and 100

### TestFixture
- Name must be unique within type
- ValidUntil must be after ValidFrom
- Data must be valid JSON
- SQL scripts must be validated before storage

### TestExecution
- Cannot start if another execution is RUNNING for same suite
- GitCommit must be valid SHA
- ParallelThreads cannot exceed environment maximum

### TestReport
- Can only be generated for COMPLETED executions
- Must include all TestResults from execution
- Metrics calculation must handle null/missing data

## Indexes

### Performance Indexes
- TestExecution: (testSuiteId, startTime)
- TestResult: (executionId, status)
- TestReport: (executionId)
- TestScenario: (testSuiteId, name)
- TestFixture: (type, tags)

### Query Optimization
- Composite index on TestExecution (status, environmentId, startTime)
- Composite index on TestResult (testScenarioId, status, duration)
- Full-text search index on TestResult.errorMessage
- Array index on TestSuite.tags and TestFixture.tags