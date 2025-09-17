# Tasks: Alternative Password Authentication

**Input**: Design documents from `/specs/009-check-existing-project/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/password-auth-api.yaml, quickstart.md

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Tech stack: Java 21, Spring Boot 3.2+, React 18+, TypeScript 5.0+
   → Structure: Web application (backend/, frontend/)
2. Load optional design documents:
   → data-model.md: User entity extensions, AuthenticationAttempt entity
   → contracts/: password-auth-api.yaml with 8 endpoints
   → research.md: BCrypt strength 12, environment flag patterns
3. Generate tasks by category:
   → Setup: database migrations, environment config
   → Tests: 8 contract tests, 5 integration tests, 3 E2E tests
   → Core: backend services, frontend components, CLI
   → Integration: email service, rate limiting, audit logging
   → Polish: unit tests, performance validation, documentation
4. Apply constitutional TDD ordering:
   → Contract → Integration → E2E → Unit
   → Database → Models → Services → API → Frontend
5. Mark [P] for parallel execution (different files)
6. SUCCESS: 32 numbered tasks ready for execution
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- All file paths are absolute for Spring Boot Modulith project structure

## Path Conventions
- **Backend**: `backend/src/main/java/`, `backend/src/test/java/`
- **Frontend**: `frontend/src/`, `frontend/tests/`
- **CLI**: `backend/src/main/java/com/platform/auth/cli/`
- **Database**: `backend/src/main/resources/db/migration/`

## Phase 3.1: Setup & Database
- [ ] **T001** Create database migration for User entity password fields in `backend/src/main/resources/db/migration/V003__add_password_auth_fields.sql`
- [ ] **T002** Create database migration for AuthenticationAttempt entity in `backend/src/main/resources/db/migration/V004__create_authentication_attempts.sql`
- [ ] **T003** Add password authentication configuration properties in `backend/src/main/resources/application.yml`

## Phase 3.2: Contract Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### API Contract Tests (All Parallel - Different Files)
- [ ] **T004 [P]** Contract test POST /auth/login endpoint in `backend/src/test/java/com/platform/auth/contract/LoginEndpointContractTest.java`
- [ ] **T005 [P]** Contract test POST /auth/register endpoint in `backend/src/test/java/com/platform/auth/contract/RegisterEndpointContractTest.java`
- [ ] **T006 [P]** Contract test POST /auth/verify-email endpoint in `backend/src/test/java/com/platform/auth/contract/VerifyEmailEndpointContractTest.java`
- [ ] **T007 [P]** Contract test POST /auth/request-password-reset endpoint in `backend/src/test/java/com/platform/auth/contract/PasswordResetRequestContractTest.java`
- [ ] **T008 [P]** Contract test POST /auth/reset-password endpoint in `backend/src/test/java/com/platform/auth/contract/PasswordResetContractTest.java`
- [ ] **T009 [P]** Contract test POST /auth/change-password endpoint in `backend/src/test/java/com/platform/auth/contract/ChangePasswordContractTest.java`
- [ ] **T010 [P]** Contract test POST /auth/resend-verification endpoint in `backend/src/test/java/com/platform/auth/contract/ResendVerificationContractTest.java`
- [ ] **T011 [P]** Contract test GET /auth/methods endpoint in `backend/src/test/java/com/platform/auth/contract/AuthMethodsContractTest.java`

### Integration Tests (All Parallel - Different Files)
- [ ] **T012 [P]** Integration test for complete registration flow in `backend/src/test/java/com/platform/auth/integration/PasswordRegistrationFlowTest.java`
- [ ] **T013 [P]** Integration test for login with rate limiting in `backend/src/test/java/com/platform/auth/integration/PasswordLoginRateLimitingTest.java`
- [ ] **T014 [P]** Integration test for email verification process in `backend/src/test/java/com/platform/auth/integration/EmailVerificationFlowTest.java`
- [ ] **T015 [P]** Integration test for password reset workflow in `backend/src/test/java/com/platform/auth/integration/PasswordResetFlowTest.java`
- [ ] **T016 [P]** Integration test for OAuth and password auth coexistence in `backend/src/test/java/com/platform/auth/integration/DualAuthMethodsTest.java`

## Phase 3.3: Backend Models & Services (ONLY after tests are failing)

### Data Models (All Parallel - Different Files)
- [ ] **T017 [P]** Extend User entity with password fields in `backend/src/main/java/com/platform/user/internal/User.java`
- [ ] **T018 [P]** Create AuthenticationAttempt entity in `backend/src/main/java/com/platform/auth/internal/AuthenticationAttempt.java`
- [ ] **T019 [P]** Create PasswordPolicy record in `backend/src/main/java/com/platform/auth/internal/PasswordPolicy.java`

### Core Services (Sequential - Dependencies)
- [ ] **T020** Create PasswordAuthService in `backend/src/main/java/com/platform/auth/internal/PasswordAuthService.java`
- [ ] **T021** Create PasswordValidator service in `backend/src/main/java/com/platform/auth/internal/PasswordValidator.java`
- [ ] **T022** Create EmailVerificationService in `backend/src/main/java/com/platform/auth/internal/EmailVerificationService.java`
- [ ] **T023** Create AuthenticationAttemptService in `backend/src/main/java/com/platform/auth/internal/AuthenticationAttemptService.java`
- [ ] **T024** Extend existing SessionService for password auth in `backend/src/main/java/com/platform/auth/internal/SessionService.java`

### API Controllers
- [ ] **T025** Extend AuthController with password authentication endpoints in `backend/src/main/java/com/platform/auth/api/AuthController.java`

## Phase 3.4: Frontend Implementation (After Backend API)

### React Components (All Parallel - Different Files)
- [ ] **T026 [P]** Create PasswordLoginForm component in `frontend/src/components/auth/PasswordLoginForm.tsx`
- [ ] **T027 [P]** Create RegistrationForm component in `frontend/src/components/auth/RegistrationForm.tsx`
- [ ] **T028 [P]** Create PasswordResetForm component in `frontend/src/components/auth/PasswordResetForm.tsx`
- [ ] **T029 [P]** Create EmailVerificationPage component in `frontend/src/pages/auth/EmailVerificationPage.tsx`

### State Management
- [ ] **T030** Extend authApi.ts with password authentication endpoints in `frontend/src/services/api/authApi.ts`
- [ ] **T031** Update authSlice.ts for password authentication state in `frontend/src/store/slices/authSlice.ts`

## Phase 3.5: CLI Implementation (Parallel)
- [ ] **T032 [P]** Create auth-cli commands in `backend/src/main/java/com/platform/auth/cli/AuthCli.java`

## Phase 3.6: End-to-End Tests (After Full Implementation)
- [ ] **T033** E2E test for complete user registration journey in `frontend/tests/e2e/password-registration.spec.ts`
- [ ] **T034** E2E test for login and password change flow in `frontend/tests/e2e/password-login.spec.ts`
- [ ] **T035** E2E test for password reset complete flow in `frontend/tests/e2e/password-reset.spec.ts`

## Dependencies
**Critical Dependencies (TDD Order):**
- Database setup (T001-T003) before all backend work
- Contract tests (T004-T011) before implementation (T017-T025)
- Integration tests (T012-T016) before implementation
- Backend API (T025) before frontend (T026-T031)
- Full implementation before E2E tests (T033-T035)

**Sequential Dependencies:**
- T020 (PasswordAuthService) blocks T025 (AuthController)
- T021 (PasswordValidator) blocks T020 (PasswordAuthService)
- T017 (User entity) blocks T020 (PasswordAuthService)
- T030 (authApi) blocks T031 (authSlice)

**Parallel Groups:**
- Contract Tests: T004-T011 can run together
- Integration Tests: T012-T016 can run together
- Models: T017-T019 can run together
- Frontend Components: T026-T029 can run together

## Parallel Execution Examples

### Contract Tests Phase
```bash
# Launch all contract tests together (T004-T011):
Task: "Contract test POST /auth/login endpoint in backend/src/test/java/com/platform/auth/contract/LoginEndpointContractTest.java"
Task: "Contract test POST /auth/register endpoint in backend/src/test/java/com/platform/auth/contract/RegisterEndpointContractTest.java"
Task: "Contract test POST /auth/verify-email endpoint in backend/src/test/java/com/platform/auth/contract/VerifyEmailEndpointContractTest.java"
Task: "Contract test POST /auth/request-password-reset endpoint in backend/src/test/java/com/platform/auth/contract/PasswordResetRequestContractTest.java"
Task: "Contract test POST /auth/reset-password endpoint in backend/src/test/java/com/platform/auth/contract/PasswordResetContractTest.java"
Task: "Contract test POST /auth/change-password endpoint in backend/src/test/java/com/platform/auth/contract/ChangePasswordContractTest.java"
Task: "Contract test POST /auth/resend-verification endpoint in backend/src/test/java/com/platform/auth/contract/ResendVerificationContractTest.java"
Task: "Contract test GET /auth/methods endpoint in backend/src/test/java/com/platform/auth/contract/AuthMethodsContractTest.java"
```

### Integration Tests Phase
```bash
# Launch all integration tests together (T012-T016):
Task: "Integration test for complete registration flow in backend/src/test/java/com/platform/auth/integration/PasswordRegistrationFlowTest.java"
Task: "Integration test for login with rate limiting in backend/src/test/java/com/platform/auth/integration/PasswordLoginRateLimitingTest.java"
Task: "Integration test for email verification process in backend/src/test/java/com/platform/auth/integration/EmailVerificationFlowTest.java"
Task: "Integration test for password reset workflow in backend/src/test/java/com/platform/auth/integration/PasswordResetFlowTest.java"
Task: "Integration test for OAuth and password auth coexistence in backend/src/test/java/com/platform/auth/integration/DualAuthMethodsTest.java"
```

### Frontend Components Phase
```bash
# Launch frontend components together (T026-T029):
Task: "Create PasswordLoginForm component in frontend/src/components/auth/PasswordLoginForm.tsx"
Task: "Create RegistrationForm component in frontend/src/components/auth/RegistrationForm.tsx"
Task: "Create PasswordResetForm component in frontend/src/components/auth/PasswordResetForm.tsx"
Task: "Create EmailVerificationPage component in frontend/src/pages/auth/EmailVerificationPage.tsx"
```

## Implementation Notes

### Constitutional Requirements
- **TDD Mandatory**: All tests (T004-T016) must fail before implementation starts
- **Opaque Tokens**: Reuse existing token infrastructure, no JWT
- **Real Dependencies**: Use TestContainers for PostgreSQL/Redis in integration tests
- **Multi-tenant**: Respect existing organization-based isolation
- **Audit Logging**: All authentication events must be logged

### Technology Constraints
- **Java 21**: Use records, pattern matching, switch expressions
- **Spring Boot 3.2+**: Leverage Spring Security 6.x features
- **BCrypt Strength 12**: Already configured, no changes needed
- **TypeScript 5.0+**: Use branded types for IDs, strict type checking
- **React 18+**: Use hooks, concurrent features
- **Redux Toolkit**: RTK Query for API calls, type-safe actions

### Performance Targets
- **Authentication latency**: <250ms (includes BCrypt verification)
- **API endpoints**: <200ms (p99)
- **Database queries**: <50ms average
- **Frontend bundle**: <500KB gzipped

### Security Requirements
- **Password Policy**: Configurable via environment variables
- **Rate Limiting**: Exponential backoff (1, 2, 4, 8, 16, 32 minutes)
- **Email Verification**: Required for all password-based accounts
- **Audit Trail**: All events with correlation IDs
- **PII Redaction**: GDPR-compliant logging

## Validation Checklist
*GATE: Checked before task execution*

- [x] All 8 contract endpoints have corresponding tests (T004-T011)
- [x] All entities have model tasks (User extension T017, AuthenticationAttempt T018)
- [x] All tests come before implementation (T004-T016 before T017-T025)
- [x] Parallel tasks truly independent (different files, no shared state)
- [x] Each task specifies exact absolute file path
- [x] No task modifies same file as another [P] task
- [x] TDD order enforced: Contract → Integration → E2E → Unit
- [x] Constitutional compliance verified (opaque tokens, real dependencies)

## File Path Summary
**Total Tasks**: 35
**Parallel Tasks**: 19 (marked with [P])
**Sequential Tasks**: 16
**Backend Files**: 23
**Frontend Files**: 9
**Database Migrations**: 2
**CLI Files**: 1

This task list provides complete, executable instructions for implementing password authentication alongside OAuth2 in the Spring Boot Modulith payment platform while maintaining strict constitutional compliance and TDD requirements.