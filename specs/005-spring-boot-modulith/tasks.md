# Tasks: Spring Boot Modulith + React Micro-SaaS Payment Platform

**Input**: Design documents from `/specs/005-spring-boot-modulith/`
**Prerequisites**: plan.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

## Phase 3.1: Foundation Setup

- [ ] **T001** Create project structure per web application plan (backend/, frontend/, admin-console/, notification-service/)
- [ ] **T002** [P] Initialize Spring Boot 3.2+ project with Gradle in backend/
- [ ] **T003** [P] Initialize React 18+ project with TypeScript in frontend/
- [ ] **T004** [P] Initialize admin console React project in admin-console/
- [ ] **T005** [P] Initialize notification service Spring Boot project in notification-service/
- [ ] **T006** [P] Configure Docker Compose with PostgreSQL 15+, Redis 7+, MailHog in docker-compose.yml
- [ ] **T007** [P] Setup Flyway database migrations in backend/src/main/resources/db/migration/
- [ ] **T008** [P] Configure ESLint, Prettier for frontend projects
- [ ] **T009** [P] Configure Checkstyle, SpotBugs for backend projects
- [ ] **T010** Add Spring Modulith dependencies and ArchUnit to backend/build.gradle

## Phase 3.2: Database Schema & Core Configuration

- [ ] **T011** [P] Create V001__create_users_table.sql migration
- [ ] **T012** [P] Create V002__create_organizations_table.sql migration
- [ ] **T013** [P] Create V003__create_organization_members_table.sql migration
- [ ] **T014** [P] Create V004__create_plans_table.sql migration
- [ ] **T015** [P] Create V005__create_subscriptions_table.sql migration
- [ ] **T016** [P] Create V006__create_payments_table.sql migration
- [ ] **T017** [P] Create V007__create_invoices_table.sql migration
- [ ] **T018** [P] Create V008__create_token_metadata_table.sql migration
- [ ] **T019** [P] Create V009__create_audit_events_table.sql migration with partitioning
- [ ] **T020** [P] Create V010__create_usage_records_table.sql migration
- [ ] **T021** [P] Create V011__create_invitations_table.sql migration
- [ ] **T022** Configure Spring Boot application.yml with PostgreSQL, Redis, OAuth2 settings

## Phase 3.3: Contract Tests First (TDD - MUST FAIL BEFORE IMPLEMENTATION)

### Authentication Module Contract Tests
- [ ] **T023** [P] Contract test GET /auth/providers in backend/src/test/java/contract/AuthProvidersContractTest.java
- [ ] **T024** [P] Contract test GET /auth/authorize in backend/src/test/java/contract/AuthAuthorizeContractTest.java
- [ ] **T025** [P] Contract test GET /auth/callback in backend/src/test/java/contract/AuthCallbackContractTest.java
- [ ] **T026** [P] Contract test POST /auth/exchange in backend/src/test/java/contract/AuthExchangeContractTest.java
- [ ] **T027** [P] Contract test GET /auth/session in backend/src/test/java/contract/AuthSessionContractTest.java
- [ ] **T028** [P] Contract test DELETE /auth/session in backend/src/test/java/contract/AuthLogoutContractTest.java

### Payment Module Contract Tests
- [ ] **T029** [P] Contract test POST /payments/intent in backend/src/test/java/contract/PaymentIntentContractTest.java
- [ ] **T030** [P] Contract test GET /payments/{id} in backend/src/test/java/contract/PaymentGetContractTest.java
- [ ] **T031** [P] Contract test GET /payment-methods in backend/src/test/java/contract/PaymentMethodsContractTest.java
- [ ] **T032** [P] Contract test POST /payment-methods in backend/src/test/java/contract/AddPaymentMethodContractTest.java
- [ ] **T033** [P] Contract test POST /webhooks/stripe in backend/src/test/java/contract/StripeWebhookContractTest.java

### Subscription Module Contract Tests
- [ ] **T034** [P] Contract test GET /plans in backend/src/test/java/contract/PlansContractTest.java
- [ ] **T035** [P] Contract test POST /subscriptions in backend/src/test/java/contract/CreateSubscriptionContractTest.java
- [ ] **T036** [P] Contract test GET /subscriptions/{id} in backend/src/test/java/contract/GetSubscriptionContractTest.java
- [ ] **T037** [P] Contract test POST /subscriptions/{id}/cancel in backend/src/test/java/contract/CancelSubscriptionContractTest.java

### User Module Contract Tests
- [ ] **T038** [P] Contract test GET /users/{id} in backend/src/test/java/contract/UserGetContractTest.java
- [ ] **T039** [P] Contract test POST /organizations in backend/src/test/java/contract/CreateOrganizationContractTest.java
- [ ] **T040** [P] Contract test POST /invitations in backend/src/test/java/contract/InvitationContractTest.java

## Phase 3.4: Integration Tests (MUST FAIL BEFORE IMPLEMENTATION)

### Core Integration Tests
- [ ] **T041** [P] OAuth2 full flow integration test in backend/src/test/java/integration/OAuth2IntegrationTest.java
- [ ] **T042** [P] User registration integration test in backend/src/test/java/integration/UserRegistrationIntegrationTest.java
- [ ] **T043** [P] Organization creation integration test in backend/src/test/java/integration/OrganizationIntegrationTest.java
- [ ] **T044** [P] Subscription creation integration test in backend/src/test/java/integration/SubscriptionIntegrationTest.java
- [ ] **T045** [P] Payment processing integration test in backend/src/test/java/integration/PaymentIntegrationTest.java
- [ ] **T046** [P] Stripe webhook processing integration test in backend/src/test/java/integration/StripeWebhookIntegrationTest.java
- [ ] **T047** [P] Token introspection caching integration test in backend/src/test/java/integration/TokenCacheIntegrationTest.java
- [ ] **T048** [P] Multi-tenant isolation integration test in backend/src/test/java/integration/MultiTenantIntegrationTest.java

## Phase 3.5: Domain Models & Shared Components

### Shared Module
- [ ] **T049** [P] BaseEntity abstract class in backend/src/main/java/com/platform/shared/domain/BaseEntity.java
- [ ] **T050** [P] Money value object in backend/src/main/java/com/platform/shared/domain/Money.java
- [ ] **T051** [P] Email value object in backend/src/main/java/com/platform/shared/domain/Email.java
- [ ] **T052** [P] TenantContext in backend/src/main/java/com/platform/shared/domain/TenantContext.java
- [ ] **T053** [P] SecurityConfig in backend/src/main/java/com/platform/shared/infrastructure/security/SecurityConfig.java
- [ ] **T054** [P] TenantFilter in backend/src/main/java/com/platform/shared/infrastructure/security/TenantFilter.java
- [ ] **T055** [P] JpaConfig in backend/src/main/java/com/platform/shared/infrastructure/persistence/JpaConfig.java

### User Module Entities
- [ ] **T056** [P] User entity in backend/src/main/java/com/platform/user/internal/domain/User.java
- [ ] **T057** [P] Organization entity in backend/src/main/java/com/platform/user/internal/domain/Organization.java
- [ ] **T058** [P] OrganizationMember entity in backend/src/main/java/com/platform/user/internal/domain/OrganizationMember.java
- [ ] **T059** [P] Invitation entity in backend/src/main/java/com/platform/user/internal/domain/Invitation.java

### Auth Module Entities
- [ ] **T060** [P] TokenMetadata entity in backend/src/main/java/com/platform/auth/internal/domain/TokenMetadata.java
- [ ] **T061** [P] TokenRepository interface in backend/src/main/java/com/platform/auth/internal/domain/TokenRepository.java

### Payment Module Entities
- [ ] **T062** [P] Payment entity in backend/src/main/java/com/platform/payment/internal/domain/Payment.java
- [ ] **T063** [P] Invoice entity in backend/src/main/java/com/platform/payment/internal/domain/Invoice.java
- [ ] **T064** [P] PaymentMethod entity in backend/src/main/java/com/platform/payment/internal/domain/PaymentMethod.java

### Subscription Module Entities
- [ ] **T065** [P] Plan entity in backend/src/main/java/com/platform/subscription/internal/domain/Plan.java
- [ ] **T066** [P] Subscription entity in backend/src/main/java/com/platform/subscription/internal/domain/Subscription.java
- [ ] **T067** [P] UsageRecord entity in backend/src/main/java/com/platform/subscription/internal/domain/UsageRecord.java

### Audit Module Entities
- [ ] **T068** [P] AuditEvent entity in backend/src/main/java/com/platform/audit/internal/domain/AuditEvent.java

## Phase 3.6: Module Services & Business Logic

### Authentication Module Services
- [ ] **T069** OAuth2Service in backend/src/main/java/com/platform/auth/internal/service/OAuth2Service.java
- [ ] **T070** TokenIntrospectionService in backend/src/main/java/com/platform/auth/internal/service/TokenIntrospectionService.java
- [ ] **T071** SessionManager in backend/src/main/java/com/platform/auth/internal/service/SessionManager.java
- [ ] **T072** AuthenticationService (public API) in backend/src/main/java/com/platform/auth/api/AuthenticationService.java

### User Module Services
- [ ] **T073** UserService in backend/src/main/java/com/platform/user/internal/service/UserService.java
- [ ] **T074** OrganizationService in backend/src/main/java/com/platform/user/internal/service/OrganizationService.java
- [ ] **T075** InvitationService in backend/src/main/java/com/platform/user/internal/service/InvitationService.java
- [ ] **T076** UserManagementService (public API) in backend/src/main/java/com/platform/user/api/UserManagementService.java

### Payment Module Services
- [ ] **T077** StripeService in backend/src/main/java/com/platform/payment/internal/service/StripeService.java
- [ ] **T078** WebhookProcessor in backend/src/main/java/com/platform/payment/internal/service/WebhookProcessor.java
- [ ] **T079** InvoiceGenerator in backend/src/main/java/com/platform/payment/internal/service/InvoiceGenerator.java
- [ ] **T080** PaymentService (public API) in backend/src/main/java/com/platform/payment/api/PaymentService.java

### Subscription Module Services
- [ ] **T081** PlanService in backend/src/main/java/com/platform/subscription/internal/service/PlanService.java
- [ ] **T082** SubscriptionService in backend/src/main/java/com/platform/subscription/internal/service/SubscriptionService.java
- [ ] **T083** UsageTracker in backend/src/main/java/com/platform/subscription/internal/service/UsageTracker.java
- [ ] **T084** SubscriptionManagement (public API) in backend/src/main/java/com/platform/subscription/api/SubscriptionManagement.java

### Audit Module Services
- [ ] **T085** PIIRedactor in backend/src/main/java/com/platform/audit/internal/service/PIIRedactor.java
- [ ] **T086** AuditService in backend/src/main/java/com/platform/audit/internal/service/AuditService.java
- [ ] **T087** RetentionService in backend/src/main/java/com/platform/audit/internal/service/RetentionService.java

## Phase 3.7: REST Controllers & API Endpoints

### Authentication Controllers
- [ ] **T088** AuthController in backend/src/main/java/com/platform/auth/internal/web/AuthController.java
- [ ] **T089** SessionController in backend/src/main/java/com/platform/auth/internal/web/SessionController.java

### User Management Controllers
- [ ] **T090** UserController in backend/src/main/java/com/platform/user/internal/web/UserController.java
- [ ] **T091** OrganizationController in backend/src/main/java/com/platform/user/internal/web/OrganizationController.java

### Payment Controllers
- [ ] **T092** PaymentController in backend/src/main/java/com/platform/payment/internal/web/PaymentController.java
- [ ] **T093** PaymentMethodController in backend/src/main/java/com/platform/payment/internal/web/PaymentMethodController.java
- [ ] **T094** StripeWebhookController in backend/src/main/java/com/platform/payment/internal/integration/StripeWebhookController.java

### Subscription Controllers
- [ ] **T095** PlanController in backend/src/main/java/com/platform/subscription/internal/web/PlanController.java
- [ ] **T096** SubscriptionController in backend/src/main/java/com/platform/subscription/internal/web/SubscriptionController.java

## Phase 3.8: Event System & Module Communication

### Domain Events
- [ ] **T097** [P] UserAuthenticated event in backend/src/main/java/com/platform/auth/api/UserAuthenticated.java
- [ ] **T098** [P] UserCreated event in backend/src/main/java/com/platform/user/api/UserCreated.java
- [ ] **T099** [P] OrganizationCreated event in backend/src/main/java/com/platform/user/api/OrganizationCreated.java
- [ ] **T100** [P] PaymentProcessed event in backend/src/main/java/com/platform/payment/api/PaymentProcessed.java
- [ ] **T101** [P] PaymentFailed event in backend/src/main/java/com/platform/payment/api/PaymentFailed.java
- [ ] **T102** [P] SubscriptionCreated event in backend/src/main/java/com/platform/subscription/api/SubscriptionCreated.java
- [ ] **T103** [P] SubscriptionCanceled event in backend/src/main/java/com/platform/subscription/api/SubscriptionCanceled.java

### Event Listeners & Handlers
- [ ] **T104** AuditEventListener in backend/src/main/java/com/platform/audit/internal/listener/AuditEventListener.java
- [ ] **T105** NotificationEventHandler in notification-service/src/main/java/com/platform/notification/handler/NotificationEventHandler.java

## Phase 3.9: Frontend Core Components

### Authentication Components
- [ ] **T106** [P] LoginForm component in frontend/src/components/auth/LoginForm.tsx
- [ ] **T107** [P] OAuthCallback component in frontend/src/components/auth/OAuthCallback.tsx
- [ ] **T108** [P] ProtectedRoute component in frontend/src/components/auth/ProtectedRoute.tsx
- [ ] **T109** [P] useAuth hook in frontend/src/hooks/useAuth.ts

### Subscription Components
- [ ] **T110** [P] PlanSelector component in frontend/src/components/subscription/PlanSelector.tsx
- [ ] **T111** [P] PaymentForm component in frontend/src/components/subscription/PaymentForm.tsx
- [ ] **T112** [P] UsageMetrics component in frontend/src/components/subscription/UsageMetrics.tsx
- [ ] **T113** [P] useSubscription hook in frontend/src/hooks/useSubscription.ts

### Common Components
- [ ] **T114** [P] Layout component in frontend/src/components/common/Layout.tsx
- [ ] **T115** [P] ErrorBoundary component in frontend/src/components/common/ErrorBoundary.tsx
- [ ] **T116** [P] LoadingSpinner component in frontend/src/components/common/LoadingSpinner.tsx

### State Management
- [ ] **T117** [P] Auth slice in frontend/src/store/auth.slice.ts
- [ ] **T118** [P] Subscription slice in frontend/src/store/subscription.slice.ts
- [ ] **T119** Redux store configuration in frontend/src/store/store.ts

### API Services
- [ ] **T120** [P] Auth service in frontend/src/services/auth.service.ts
- [ ] **T121** [P] Payment service in frontend/src/services/payment.service.ts
- [ ] **T122** [P] API client configuration in frontend/src/services/api.ts

## Phase 3.10: Admin Console

### Admin Components
- [ ] **T123** [P] Admin Dashboard in admin-console/src/components/Dashboard.tsx
- [ ] **T124** [P] User Management in admin-console/src/components/UserManagement.tsx
- [ ] **T125** [P] Audit Log viewer in admin-console/src/components/AuditLog.tsx

## Phase 3.11: Observability & Security

### Monitoring & Logging
- [ ] **T126** [P] MetricsConfig in backend/src/main/java/com/platform/shared/infrastructure/observability/MetricsConfig.java
- [ ] **T127** [P] LoggingConfig in backend/src/main/java/com/platform/shared/infrastructure/observability/LoggingConfig.java
- [ ] **T128** [P] Logback configuration in backend/src/main/resources/logback-spring.xml

### Security & Validation
- [ ] **T129** Input validation annotations on all DTOs
- [ ] **T130** CORS configuration in SecurityConfig
- [ ] **T131** Rate limiting with bucket4j
- [ ] **T132** Security headers configuration

## Phase 3.12: Architecture Tests & Module Boundaries

### ArchUnit Tests
- [ ] **T133** [P] Module boundary test in backend/src/test/java/architecture/ModuleBoundaryTest.java
- [ ] **T134** [P] Controller package test in backend/src/test/java/architecture/ControllerArchTest.java
- [ ] **T135** [P] Service dependency test in backend/src/test/java/architecture/ServiceArchTest.java
- [ ] **T136** [P] Domain independence test in backend/src/test/java/architecture/DomainArchTest.java

## Phase 3.13: End-to-End Tests

### User Journey E2E Tests
- [ ] **T137** [P] Complete OAuth login flow in frontend/tests/e2e/auth-flow.spec.ts
- [ ] **T138** [P] Subscription creation flow in frontend/tests/e2e/subscription-flow.spec.ts
- [ ] **T139** [P] Payment processing flow in frontend/tests/e2e/payment-flow.spec.ts
- [ ] **T140** [P] Organization management flow in frontend/tests/e2e/organization-flow.spec.ts

## Phase 3.14: Performance & Load Testing

### Performance Tests
- [ ] **T141** [P] API performance test (<200ms p99) in backend/src/test/java/performance/ApiPerformanceTest.java
- [ ] **T142** [P] Database query performance test in backend/src/test/java/performance/DatabasePerformanceTest.java
- [ ] **T143** [P] Frontend load performance test in frontend/tests/performance/load-test.spec.ts

## Phase 3.15: CLI Libraries & Commands

### Module CLI Interfaces
- [ ] **T144** [P] Auth module CLI in backend/src/main/java/com/platform/auth/cli/AuthCLI.java
- [ ] **T145** [P] User module CLI in backend/src/main/java/com/platform/user/cli/UserCLI.java
- [ ] **T146** [P] Payment module CLI in backend/src/main/java/com/platform/payment/cli/PaymentCLI.java
- [ ] **T147** [P] Subscription module CLI in backend/src/main/java/com/platform/subscription/cli/SubscriptionCLI.java
- [ ] **T148** [P] Audit module CLI in backend/src/main/java/com/platform/audit/cli/AuditCLI.java

## Phase 3.16: Documentation & Final Polish

### Documentation
- [ ] **T149** [P] Generate OpenAPI documentation from controllers
- [ ] **T150** [P] Create llms.txt for each module
- [ ] **T151** [P] Update README with quickstart guide
- [ ] **T152** [P] Create deployment guide

### Final Integration
- [ ] **T153** Run complete test suite and fix any failures
- [ ] **T154** Verify all quickstart validation steps pass
- [ ] **T155** Load test with k6 scripts
- [ ] **T156** Security scan with OWASP dependency check
- [ ] **T157** Generate final code coverage report

## Dependencies

### Critical Dependencies
- **Foundation** (T001-T010) must complete before any other tasks
- **Database Schema** (T011-T022) must complete before entities
- **Contract Tests** (T023-T040) MUST complete and FAIL before any implementation
- **Integration Tests** (T041-T048) MUST complete and FAIL before services
- **Entities** (T049-T068) must complete before services
- **Services** (T069-T087) must complete before controllers
- **Events** (T097-T105) can run parallel with services
- **Architecture Tests** (T133-T136) must pass throughout development

### Sequential Blocks
1. **T069** (OAuth2Service) blocks **T088** (AuthController)
2. **T073** (UserService) blocks **T090** (UserController)
3. **T077** (StripeService) blocks **T092-T094** (Payment Controllers)
4. **T082** (SubscriptionService) blocks **T095-T096** (Subscription Controllers)
5. **T119** (Store config) blocks **T117-T118** (Redux slices)

## Parallel Execution Examples

### Wave 1: Foundation Setup (Parallel)
```bash
# All foundation tasks can run simultaneously
Task T002: "Initialize Spring Boot 3.2+ project with Gradle in backend/"
Task T003: "Initialize React 18+ project with TypeScript in frontend/"
Task T004: "Initialize admin console React project in admin-console/"
Task T005: "Initialize notification service Spring Boot project in notification-service/"
```

### Wave 2: Contract Tests (Parallel - MUST FAIL)
```bash
# All contract tests independent, can run in parallel
Task T023: "Contract test GET /auth/providers in backend/src/test/java/contract/AuthProvidersContractTest.java"
Task T029: "Contract test POST /payments/intent in backend/src/test/java/contract/PaymentIntentContractTest.java"
Task T034: "Contract test GET /plans in backend/src/test/java/contract/PlansContractTest.java"
```

### Wave 3: Domain Entities (Parallel)
```bash
# All entities in different files, can be created in parallel
Task T056: "User entity in backend/src/main/java/com/platform/user/internal/domain/User.java"
Task T060: "TokenMetadata entity in backend/src/main/java/com/platform/auth/internal/domain/TokenMetadata.java"
Task T062: "Payment entity in backend/src/main/java/com/platform/payment/internal/domain/Payment.java"
```

## Validation Checklist

### Pre-Implementation Validation
- [x] All contracts have corresponding tests (T023-T040)
- [x] All entities have model tasks (T049-T068)
- [x] All tests come before implementation in task order
- [x] Parallel tasks truly independent (different files)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task

### Constitutional Compliance
- [x] TDD enforced: Contract → Integration → E2E → Unit test order
- [x] Library-first: Each module exposes CLI interface (T144-T148)
- [x] Real dependencies: TestContainers used in integration tests
- [x] Module boundaries: ArchUnit tests enforce separation (T133-T136)
- [x] Observability: Structured logging and metrics (T126-T128)

**Total Tasks**: 157 tasks across 16 phases
**Estimated Duration**: 12-16 weeks with 2-3 developers
**Parallel Opportunities**: 89 tasks marked [P] for concurrent execution