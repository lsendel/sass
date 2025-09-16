# Tasks: Spring Boot Modulith Payment Platform

**Input**: Design documents from `/specs/005-spring-boot-modulith/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/

## Execution Flow (main)
```
1. Load plan.md from feature directory ✅
   → Extract: Java 21, Spring Boot 3.2+, React 18+, PostgreSQL, Redis
   → Structure: Web application (backend/frontend separation)
2. Load design documents ✅
   → data-model.md: 9 core entities → model tasks
   → contracts/: 3 API files → contract test tasks
   → quickstart.md: User journeys → integration tests
3. Generate tasks by category ✅
   → Setup: Project structure, dependencies, database
   → Tests: Contract tests for all APIs, integration tests
   → Core: Entity models, service layer, Spring Modulith modules
   → Integration: Payment processing, OAuth2, event handling
   → Frontend: React components, Redux state, API integration
   → Polish: Unit tests, performance, monitoring
4. Apply task rules ✅
   → Different modules/files = mark [P] for parallel
   → Same module = sequential dependencies
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001-T055) ✅
6. Generate dependency graph and parallel execution examples ✅
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files/modules, no dependencies)
- Include exact file paths in descriptions

## Phase 3.1: Foundation Setup
- [ ] T001 Create project structure: backend/, frontend/, admin-console/, notification-service/
- [ ] T002 Initialize backend Spring Boot project with Gradle and Spring Modulith dependencies
- [ ] T003 [P] Configure backend linting (Checkstyle) and formatting tools
- [ ] T004 [P] Initialize frontend React project with TypeScript and Redux Toolkit dependencies
- [ ] T005 [P] Configure frontend linting (ESLint, Prettier) and formatting tools
- [ ] T006 Setup PostgreSQL database with Docker Compose
- [ ] T007 Setup Redis for session storage with Docker Compose
- [ ] T008 Create Flyway database migrations for all entities in backend/src/main/resources/db/migration/

## Phase 3.2: Contract Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
- [ ] T009 [P] Contract test for Auth API endpoints in backend/src/test/java/contract/AuthApiContractTest.java
- [ ] T010 [P] Contract test for Payment API endpoints in backend/src/test/java/contract/PaymentApiContractTest.java
- [ ] T011 [P] Contract test for Subscription API endpoints in backend/src/test/java/contract/SubscriptionApiContractTest.java
- [ ] T012 [P] ArchUnit tests for module boundaries in backend/src/test/java/architecture/ModulithArchitectureTest.java

## Phase 3.3: Core Entity Models (ONLY after tests are failing)
- [ ] T013 [P] User entity with JPA annotations in backend/src/main/java/com/platform/user/internal/User.java
- [ ] T014 [P] Organization entity in backend/src/main/java/com/platform/user/internal/Organization.java
- [ ] T015 [P] OrganizationMember entity in backend/src/main/java/com/platform/user/internal/OrganizationMember.java
- [ ] T016 [P] Plan entity in backend/src/main/java/com/platform/subscription/internal/Plan.java
- [ ] T017 [P] Subscription entity in backend/src/main/java/com/platform/subscription/internal/Subscription.java
- [ ] T018 [P] Payment entity in backend/src/main/java/com/platform/payment/internal/Payment.java
- [ ] T019 [P] Invoice entity in backend/src/main/java/com/platform/payment/internal/Invoice.java
- [ ] T020 [P] AuditEvent entity with partitioning in backend/src/main/java/com/platform/audit/internal/AuditEvent.java
- [ ] T021 [P] TokenMetadata entity in backend/src/main/java/com/platform/auth/internal/TokenMetadata.java

## Phase 3.4: Shared Module and Security
- [ ] T022 TenantContext for multi-tenancy in backend/src/main/java/com/platform/shared/security/TenantContext.java
- [ ] T023 Money value object in backend/src/main/java/com/platform/shared/types/Money.java
- [ ] T024 [P] Email value object in backend/src/main/java/com/platform/shared/types/Email.java
- [ ] T025 Security configuration with OAuth2 in backend/src/main/java/com/platform/shared/config/SecurityConfig.java
- [ ] T026 Custom token storage implementation in backend/src/main/java/com/platform/auth/internal/OpaqueTokenStore.java

## Phase 3.5: Auth Module Implementation
- [ ] T027 OAuth2 providers configuration in backend/src/main/java/com/platform/auth/internal/OAuth2ProvidersService.java
- [ ] T028 OAuth2 authorization flow in backend/src/main/java/com/platform/auth/api/AuthController.java
- [ ] T029 [P] Session management service in backend/src/main/java/com/platform/auth/internal/SessionService.java
- [ ] T030 Auth module integration tests with TestContainers in backend/src/test/java/integration/AuthIntegrationTest.java

## Phase 3.6: User Module Implementation
- [ ] T031 UserService with CRUD operations in backend/src/main/java/com/platform/user/internal/UserService.java
- [ ] T032 OrganizationService in backend/src/main/java/com/platform/user/internal/OrganizationService.java
- [ ] T033 User API endpoints in backend/src/main/java/com/platform/user/api/UserController.java
- [ ] T034 [P] Organization API endpoints in backend/src/main/java/com/platform/user/api/OrganizationController.java
- [ ] T035 User module integration tests in backend/src/test/java/integration/UserIntegrationTest.java

## Phase 3.7: Payment Module with Stripe Integration
- [ ] T036 Stripe configuration and client setup in backend/src/main/java/com/platform/payment/internal/StripeConfig.java
- [ ] T037 PaymentService with Stripe integration in backend/src/main/java/com/platform/payment/internal/PaymentService.java
- [ ] T038 Stripe webhook handler with signature verification in backend/src/main/java/com/platform/payment/api/StripeWebhookController.java
- [ ] T039 [P] Payment API endpoints in backend/src/main/java/com/platform/payment/api/PaymentController.java
- [ ] T040 Payment integration tests with Stripe test mode in backend/src/test/java/integration/PaymentIntegrationTest.java

## Phase 3.8: Subscription Module Implementation
- [ ] T041 SubscriptionService with billing logic in backend/src/main/java/com/platform/subscription/internal/SubscriptionService.java
- [ ] T042 PlanService for subscription plans in backend/src/main/java/com/platform/subscription/internal/PlanService.java
- [ ] T043 [P] Subscription API endpoints in backend/src/main/java/com/platform/subscription/api/SubscriptionController.java
- [ ] T044 Subscription integration tests in backend/src/test/java/integration/SubscriptionIntegrationTest.java

## Phase 3.9: Audit Module for Compliance
- [ ] T045 AuditService with GDPR compliance in backend/src/main/java/com/platform/audit/internal/AuditService.java
- [ ] T046 [P] Event listeners for audit logging in backend/src/main/java/com/platform/audit/internal/AuditEventListener.java
- [ ] T047 [P] Audit API for compliance reporting in backend/src/main/java/com/platform/audit/api/AuditController.java
- [ ] T048 Audit retention and cleanup scheduled tasks in backend/src/main/java/com/platform/audit/internal/AuditCleanupTask.java

## Phase 3.10: Frontend React Application
- [ ] T049 [P] Redux store setup with RTK Query in frontend/src/store/index.ts
- [ ] T050 [P] Authentication slice and API in frontend/src/store/auth/authSlice.ts
- [ ] T051 [P] User management components in frontend/src/components/user/
- [ ] T052 [P] Subscription management components in frontend/src/components/subscription/
- [ ] T053 [P] Payment components with Stripe Elements in frontend/src/components/payment/

## Phase 3.11: End-to-End Integration and Testing
- [ ] T054 Cross-module integration tests for complete user journeys in backend/src/test/java/integration/E2EIntegrationTest.java
- [ ] T055 [P] Playwright E2E tests for critical user flows in frontend/tests/e2e/

## Dependencies
- Foundation (T001-T008) blocks all other phases
- Contract tests (T009-T012) block implementation (T013+)
- Entity models (T013-T021) block service implementation
- Auth module (T027-T030) blocks user module (T031-T035)
- User module blocks payment module (T036-T040)
- Payment module blocks subscription module (T041-T044)
- All backend modules block frontend (T049-T053)
- Everything blocks E2E testing (T054-T055)

## Parallel Execution Examples

### Setup Phase (can run together):
```
Task: "Configure backend linting (Checkstyle) and formatting tools"
Task: "Initialize frontend React project with TypeScript and Redux Toolkit"
Task: "Configure frontend linting (ESLint, Prettier) and formatting tools"
```

### Contract Tests Phase (can run together):
```
Task: "Contract test for Auth API endpoints"
Task: "Contract test for Payment API endpoints"
Task: "Contract test for Subscription API endpoints"
Task: "ArchUnit tests for module boundaries"
```

### Entity Models Phase (can run together):
```
Task: "User entity with JPA annotations"
Task: "Organization entity"
Task: "Plan entity"
Task: "Payment entity"
Task: "AuditEvent entity with partitioning"
```

### Shared Types Phase (can run together):
```
Task: "Money value object"
Task: "Email value object"
```

### Frontend Phase (can run together):
```
Task: "Redux store setup with RTK Query"
Task: "Authentication slice and API"
Task: "User management components"
Task: "Subscription management components"
Task: "Payment components with Stripe Elements"
```

## Module Communication Patterns
- **Event-Driven**: Use Spring's ApplicationEventPublisher for inter-module communication
- **API Boundaries**: Each module exposes public API through dedicated packages
- **Dependency Direction**: Shared ← Auth/User/Payment/Subscription/Audit

## Testing Strategy
- **Contract Tests**: OpenAPI schema validation for all endpoints
- **Integration Tests**: Real PostgreSQL/Redis with TestContainers
- **ArchUnit Tests**: Module boundary enforcement
- **E2E Tests**: Complete user journeys with Playwright

## Performance Targets
- API response time: <200ms (p99)
- Database queries: <50ms average
- Payment processing: >95% success rate
- Frontend load time: <2s initial load

## Security Considerations
- Opaque tokens only (no JWT)
- PII redaction in audit logs
- CORS and security headers
- Stripe webhook signature verification
- Multi-tenant isolation enforced

## Validation Checklist
- [x] All contracts have corresponding tests
- [x] All entities have model tasks
- [x] All tests come before implementation
- [x] Parallel tasks are truly independent
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] TDD approach with failing tests first
- [x] Module boundaries respected in task organization

---
*Ready for implementation following constitutional TDD requirements*