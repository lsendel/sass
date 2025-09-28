# Tasks: Role-Based Access Control (RBAC) for Organizations

**Input**: Design documents from `/specs/012-implement-role-based/`
**Prerequisites**: plan.md (✓), research.md (✓), data-model.md (✓), contracts/ (✓)

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Tech stack: Java 21 + Spring Boot 3.5.6 + Spring Modulith
   → Structure: backend/ and frontend/ (web application)
2. Load design documents:
   → data-model.md: 4 core entities (Role, Permission, RolePermission, UserOrganizationRole)
   → contracts/rbac-api.yaml: 10 API endpoints across 4 resource groups
   → research.md: Hierarchical permissions, Redis caching, multi-tenant support
3. Generate tasks by category:
   → Setup: Database migrations, module structure, dependencies
   → Tests: 10 contract tests, 5 integration scenarios, architecture tests
   → Core: 4 JPA entities, 4 repositories, 5 service classes, 3 controllers
   → Integration: Spring Security, Redis cache, audit logging
   → Frontend: 6 React components for role management UI
   → Polish: Performance testing, documentation, validation
4. Task rules applied:
   → Different files marked [P] for parallel execution
   → Tests before implementation (TDD enforced)
   → Dependencies ordered: entities → repositories → services → controllers
5. 42 tasks numbered sequentially (T001-T042)
6. Parallel execution groups identified
7. SUCCESS (tasks ready for execution)
```

## Implementation Progress

**Overall Progress**: 15/45 tasks completed (33.3%)

**Phase Status**:
- ✅ **Phase 3.1 - Setup & Infrastructure**: 3/3 completed (100%)
- ✅ **Phase 3.2 - Tests First (TDD)**: 12/17 completed (71% - All contract tests done)
- ⏳ **Phase 3.3 - Core Implementation**: 0/15 pending
- ⏳ **Phase 3.4 - Integration & Security**: 0/3 pending
- ⏳ **Phase 3.5 - Frontend Implementation**: 0/4 pending
- ⏳ **Phase 3.6 - Polish & Validation**: 0/3 pending

**Recent Completions**:
- ✅ T007-T008: Role update and delete contract tests with business rule validation
- ✅ All 10 contract tests complete: Full API contract coverage achieved

**Next Phase Ready**: Integration tests (T014-T018) ready to start - end-to-end scenario validation.
**Remaining TDD Phase**: T014-T020 (Integration and architecture tests) before core implementation.

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Exact file paths included in descriptions

## Path Conventions
- **Backend**: `backend/src/main/java/com/platform/`
- **Frontend**: `frontend/src/`
- **Tests**: `backend/src/test/java/` and `frontend/src/test/`
- **Database**: `backend/src/main/resources/db/migration/`

## Phase 3.1: Setup & Infrastructure

- [x] **T001** Create database migration for RBAC tables ✅ **COMPLETED**
  - File: `backend/src/main/resources/db/migration/V015__create_rbac_tables.sql`
  - ✅ Created tables: roles, permissions, role_permissions, user_organization_roles
  - ✅ Added performance indexes for <200ms permission checks
  - ✅ Added database functions and triggers for cache invalidation

- [x] **T002** [P] Create RBAC configuration properties ✅ **COMPLETED**
  - File: `backend/src/main/java/com/platform/shared/config/RbacConfigurationProperties.java`
  - ✅ Defined limits: maxRolesPerOrganization, maxRolesPerUser, maxPermissionsPerRole
  - ✅ Added cache TTL settings for Redis and performance optimizations
  - ✅ Included audit configuration and convenience methods

- [x] **T003** [P] Initialize system permissions data ✅ **COMPLETED**
  - File: `backend/src/main/resources/db/migration/V016__populate_permissions.sql`
  - ✅ Inserted all Resource×Action combinations (20 permissions total)
  - ✅ Set up predefined roles: Owner, Admin, Member, Viewer for all organizations
  - ✅ Added auto-provisioning trigger for new organizations
  - ✅ Created helper views for role and permission management

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3

### Contract Tests [P] - All can run in parallel

- [x] **T004** [P] Contract test: List organization roles ✅ **COMPLETED**
  - File: `backend/src/test/java/com/platform/user/api/RoleControllerContractTest.java`
  - ✅ Test endpoint: `GET /organizations/{organizationId}/roles`
  - ✅ Validate schema matches OpenAPI spec
  - ✅ Include predefined roles verification and query parameters

- [x] **T005** [P] Contract test: Create custom role ✅ **COMPLETED**
  - File: `backend/src/test/java/com/platform/user/api/RoleControllerContractTest.java`
  - ✅ Test endpoint: `POST /organizations/{organizationId}/roles`
  - ✅ Validate request/response schemas and error cases
  - ✅ Test validation rules and duplicate name conflicts

- [x] **T006** [P] Contract test: Get role details ✅ **COMPLETED**
  - File: `backend/src/test/java/com/platform/user/api/RoleControllerContractTest.java`
  - ✅ Test endpoint: `GET /organizations/{organizationId}/roles/{roleId}`
  - ✅ Validate role details with permissions included
  - ✅ Test error cases and authorization requirements

- [x] **T007** [P] Contract test: Update role ✅ **COMPLETED**
  - File: `backend/src/test/java/com/platform/user/api/RoleControllerContractTest.java`
  - ✅ Test endpoint: `PUT /organizations/{organizationId}/roles/{roleId}`
  - ✅ Validate update operations and constraints
  - ✅ Test predefined role protection and name conflicts

- [x] **T008** [P] Contract test: Delete role ✅ **COMPLETED**
  - File: `backend/src/test/java/com/platform/user/api/RoleControllerContractTest.java`
  - ✅ Test endpoint: `DELETE /organizations/{organizationId}/roles/{roleId}`
  - ✅ Validate deletion rules and error responses
  - ✅ Test active assignment prevention and predefined role protection

- [x] **T009** [P] Contract test: Get user roles ✅ **COMPLETED**
  - File: `backend/src/test/java/com/platform/user/api/UserRoleControllerContractTest.java`
  - ✅ Test endpoint: `GET /organizations/{organizationId}/users/{userId}/roles`
  - ✅ Validate user role assignments and effective permissions
  - ✅ Test empty assignments and authorization

- [x] **T010** [P] Contract test: Assign user role ✅ **COMPLETED**
  - File: `backend/src/test/java/com/platform/user/api/UserRoleControllerContractTest.java`
  - ✅ Test endpoint: `POST /organizations/{organizationId}/users/{userId}/roles`
  - ✅ Validate role assignment with limits and conflicts
  - ✅ Test expiration dates and duplicate assignments

- [x] **T011** [P] Contract test: Remove user role ✅ **COMPLETED**
  - File: `backend/src/test/java/com/platform/user/api/UserRoleControllerContractTest.java`
  - ✅ Test endpoint: `DELETE /organizations/{organizationId}/users/{userId}/roles/{roleId}`
  - ✅ Validate role removal and immediate effect
  - ✅ Test business rules (e.g., last owner protection)

- [x] **T012** [P] Contract test: List permissions ✅ **COMPLETED**
  - File: `backend/src/test/java/com/platform/auth/api/PermissionControllerContractTest.java`
  - ✅ Test endpoint: `GET /permissions`
  - ✅ Validate permission catalog structure
  - ✅ Verify all 20 Resource×Action combinations

- [x] **T013** [P] Contract test: Check permissions ✅ **COMPLETED**
  - File: `backend/src/test/java/com/platform/auth/api/PermissionControllerContractTest.java`
  - ✅ Test endpoint: `POST /auth/permissions/check`
  - ✅ Validate permission check results and batch operations
  - ✅ Test organization isolation and resource-specific checks

### Integration Test Scenarios [P] - Based on quickstart.md

- [ ] **T014** [P] Integration test: Custom role creation flow
  - File: `backend/src/test/java/com/platform/user/internal/RoleManagementIntegrationTest.java`
  - Test scenario 1 from quickstart.md: admin creates payment-manager role
  - Verify database persistence and audit logging

- [ ] **T015** [P] Integration test: Role assignment flow
  - File: `backend/src/test/java/com/platform/user/internal/UserRoleAssignmentIntegrationTest.java`
  - Test scenario 2 from quickstart.md: assign role to user
  - Verify permission cache invalidation

- [ ] **T016** [P] Integration test: Permission enforcement
  - File: `backend/src/test/java/com/platform/auth/internal/PermissionEnforcementIntegrationTest.java`
  - Test scenario 3 from quickstart.md: permission checks and enforcement
  - Verify Spring Security integration

- [ ] **T017** [P] Integration test: Real-time permission updates
  - File: `backend/src/test/java/com/platform/user/internal/RoleUpdateIntegrationTest.java`
  - Test scenario 4 from quickstart.md: role modification effects
  - Verify immediate cache invalidation and effect

- [ ] **T018** [P] Integration test: Multi-organization role switching
  - File: `backend/src/test/java/com/platform/user/internal/MultiTenantRoleIntegrationTest.java`
  - Test scenario 5 from quickstart.md: organization context switching
  - Verify tenant isolation and role scoping

### Architecture Tests [P]

- [ ] **T019** [P] Architecture test: RBAC module boundaries
  - File: `backend/src/test/java/com/platform/user/RbacModuleBoundaryTest.java`
  - Verify Spring Modulith boundaries for RBAC components
  - Ensure no internal package access violations

- [ ] **T020** [P] Architecture test: Security annotations coverage
  - File: `backend/src/test/java/com/platform/SecurityAnnotationCoverageTest.java`
  - Verify all RBAC endpoints have @PreAuthorize annotations
  - Check permission enforcement completeness

## Phase 3.3: Core Implementation

### Data Layer - Entities [P]

- [ ] **T021** [P] Implement Role entity
  - File: `backend/src/main/java/com/platform/user/internal/Role.java`
  - JPA entity with validation rules from data-model.md
  - Include audit fields and relationships

- [ ] **T022** [P] Implement Permission entity
  - File: `backend/src/main/java/com/platform/user/internal/Permission.java`
  - JPA entity with Resource and Action enums
  - Unique constraint on resource+action combination

- [ ] **T023** [P] Implement RolePermission entity
  - File: `backend/src/main/java/com/platform/user/internal/RolePermission.java`
  - Junction table entity with composite key
  - Include audit fields for permission grants

- [ ] **T024** [P] Implement UserOrganizationRole entity
  - File: `backend/src/main/java/com/platform/user/internal/UserOrganizationRole.java`
  - Multi-tenant role assignment entity
  - Include expiration and active status fields

### Data Layer - Repositories [P]

- [ ] **T025** [P] Implement RoleRepository
  - File: `backend/src/main/java/com/platform/user/internal/RoleRepository.java`
  - Spring Data JPA repository with custom queries
  - Methods: findByOrganizationIdAndIsActive, countByOrganizationId

- [ ] **T026** [P] Implement PermissionRepository
  - File: `backend/src/main/java/com/platform/user/internal/PermissionRepository.java`
  - Repository for system permission catalog
  - Methods: findAllActive, findByResourceAndAction

- [ ] **T027** [P] Implement UserOrganizationRoleRepository
  - File: `backend/src/main/java/com/platform/user/internal/UserOrganizationRoleRepository.java`
  - Multi-tenant role assignment queries
  - Methods: findByUserIdAndOrganizationId, countActiveRolesByUser

- [ ] **T028** [P] Implement PermissionCacheRepository
  - File: `backend/src/main/java/com/platform/auth/internal/PermissionCacheRepository.java`
  - Redis-based permission caching operations
  - Methods: getUserPermissions, invalidateUserCache, cacheUserPermissions

### Service Layer

- [ ] **T029** Implement RoleService
  - File: `backend/src/main/java/com/platform/user/internal/RoleService.java`
  - Business logic for role CRUD operations
  - Depends on: RoleRepository, PermissionRepository, audit logging

- [ ] **T030** Implement UserRoleAssignmentService
  - File: `backend/src/main/java/com/platform/user/internal/UserRoleAssignmentService.java`
  - Role assignment and removal logic
  - Depends on: UserOrganizationRoleRepository, cache invalidation

- [ ] **T031** Implement PermissionEvaluationService
  - File: `backend/src/main/java/com/platform/auth/internal/PermissionEvaluationService.java`
  - Permission checking and caching logic
  - Depends on: PermissionCacheRepository, UserOrganizationRoleRepository

- [ ] **T032** Implement RbacAuditService
  - File: `backend/src/main/java/com/platform/user/internal/RbacAuditService.java`
  - RBAC-specific audit event logging
  - Depends on: existing audit module

### Controller Layer

- [ ] **T033** Implement RoleController
  - File: `backend/src/main/java/com/platform/user/api/RoleController.java`
  - REST endpoints for role management
  - Depends on: RoleService, security annotations

- [ ] **T034** Implement UserRoleController
  - File: `backend/src/main/java/com/platform/user/api/UserRoleController.java`
  - REST endpoints for user role assignments
  - Depends on: UserRoleAssignmentService, security annotations

- [ ] **T035** Implement PermissionController
  - File: `backend/src/main/java/com/platform/auth/api/PermissionController.java`
  - REST endpoints for permission catalog and checking
  - Depends on: PermissionEvaluationService

## Phase 3.4: Integration & Security

- [ ] **T036** Implement Spring Security RBAC integration
  - File: `backend/src/main/java/com/platform/shared/security/RbacSecurityConfiguration.java`
  - Custom PermissionEvaluator for @PreAuthorize annotations
  - Integration with existing OAuth2 authentication

- [ ] **T037** Configure Redis permission caching
  - File: `backend/src/main/java/com/platform/shared/config/RbacCacheConfiguration.java`
  - Redis cache configuration for permission data
  - TTL settings and serialization configuration

- [ ] **T038** Implement cache invalidation events
  - File: `backend/src/main/java/com/platform/user/internal/RbacCacheInvalidationHandler.java`
  - Event handlers for role/permission changes
  - Automatic cache cleanup on updates

## Phase 3.5: Frontend Implementation [P]

- [ ] **T039** [P] Implement RoleManagement component
  - File: `frontend/src/components/organizations/RoleManagement.tsx`
  - React component for listing and managing organization roles
  - Uses RTK Query for API integration

- [ ] **T040** [P] Implement RoleEditor component
  - File: `frontend/src/components/organizations/RoleEditor.tsx`
  - Form component for creating/editing custom roles
  - Permission selection with hierarchical display

- [ ] **T041** [P] Implement UserRoleAssignment component
  - File: `frontend/src/components/organizations/UserRoleAssignment.tsx`
  - Component for assigning/removing user roles
  - Real-time permission preview

- [ ] **T042** [P] Implement PermissionChecker hook
  - File: `frontend/src/hooks/usePermissions.ts`
  - React hook for permission-based UI rendering
  - Caching and optimistic updates

## Phase 3.6: Polish & Validation

- [ ] **T043** [P] Performance test: Permission check load testing
  - File: `backend/src/test/java/com/platform/auth/PermissionCheckPerformanceTest.java`
  - Load test 1000+ concurrent permission checks
  - Verify <200ms response time requirement

- [ ] **T044** [P] Unit tests: Service layer comprehensive coverage
  - Files: Multiple test files for each service class
  - Achieve >90% test coverage for all RBAC services
  - Mock external dependencies appropriately

- [ ] **T045** [P] End-to-end test: Complete RBAC workflow
  - File: `frontend/tests/e2e/rbac-workflow.spec.ts`
  - Playwright test covering full quickstart scenarios
  - Cross-browser validation

## Parallel Execution Strategy

### Immediate Parallel Groups (can start simultaneously):

**Group A - Setup & Configuration [P]**:
```bash
# Can run in parallel immediately
Task T002 & Task T003 & Task T019 & Task T020
```

**Group B - Contract Tests [P]** (after T001 database migration):
```bash
# All contract tests are independent
Task T004 & Task T005 & Task T006 & Task T007 & Task T008 &
Task T009 & Task T010 & Task T011 & Task T012 & Task T013
```

**Group C - Integration Tests [P]** (after contract tests pass):
```bash
# Integration scenarios are independent
Task T014 & Task T015 & Task T016 & Task T017 & Task T018
```

**Group D - Entities [P]** (after tests are written):
```bash
# JPA entities are independent
Task T021 & Task T022 & Task T023 & Task T024
```

**Group E - Repositories [P]** (after entities exist):
```bash
# Repository interfaces are independent
Task T025 & Task T026 & Task T027 & Task T028
```

**Group F - Frontend Components [P]** (after backend API available):
```bash
# React components are independent
Task T039 & Task T040 & Task T041 & Task T042
```

**Group G - Polish & Testing [P]** (after core implementation):
```bash
# Final validation tasks are independent
Task T043 & Task T044 & Task T045
```

### Sequential Dependencies:

1. **Database First**: T001 (migration) → All other tasks
2. **Tests Before Implementation**: T004-T020 → T021+
3. **Entities Before Repositories**: T021-T024 → T025-T028
4. **Repositories Before Services**: T025-T028 → T029-T032
5. **Services Before Controllers**: T029-T032 → T033-T035
6. **Backend Before Frontend**: T033-T035 → T039-T042

## Task Agent Commands

### Example parallel execution commands:

```bash
# Start contract tests in parallel (after database setup)
Task --parallel "T004,T005,T006,T007,T008,T009,T010,T011,T012,T013" \
  --description "Run all contract tests simultaneously"

# Start entity implementation in parallel (after tests written)
Task --parallel "T021,T022,T023,T024" \
  --description "Implement all JPA entities simultaneously"

# Start frontend development in parallel (after backend API ready)
Task --parallel "T039,T040,T041,T042" \
  --description "Implement all React RBAC components simultaneously"
```

## Success Criteria Validation

Upon completion of all tasks, verify:

✅ **Functional Requirements** (from spec.md):
- Custom role creation with permissions (T029, T033)
- User role assignment/removal (T030, T034)
- Permission enforcement on all endpoints (T036)
- Hierarchical permissions working (T031)
- Real-time permission updates (T038)
- Comprehensive audit logging (T032)

✅ **Performance Requirements** (from plan.md):
- <200ms permission check response time (T043)
- 1000+ concurrent users supported (T043)
- Redis caching operational (T037)

✅ **Security Requirements** (from research.md):
- Tenant isolation enforced (T018)
- Spring Security integration (T036)
- Privilege escalation prevention (T020)
- Session-based authentication (existing + T036)

✅ **Constitutional Compliance**:
- TDD enforced (tests T004-T020 before implementation T021+)
- Module boundaries maintained (T019)
- Real dependencies in tests (TestContainers used)
- Audit logging comprehensive (T032)

This implementation plan provides 45 specific, executable tasks that will deliver a complete RBAC system meeting all requirements from the feature specification and design documents.