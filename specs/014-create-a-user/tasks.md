# Tasks: User-Facing Audit Log Viewer

**Input**: Design documents from `/specs/014-create-a-user/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → If not found: ERROR "No implementation plan found"
   → Extract: tech stack, libraries, structure
2. Load optional design documents:
   → data-model.md: Extract entities → model tasks
   → contracts/: Each file → contract test task
   → research.md: Extract decisions → setup tasks
3. Generate tasks by category:
   → Setup: project init, dependencies, linting
   → Tests: contract tests, integration tests
   → Core: models, services, CLI commands
   → Integration: DB, middleware, logging
   → Polish: unit tests, performance, docs
4. Apply task rules:
   → Different files = mark [P] for parallel
   → Same file = sequential (no [P])
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001, T002...)
6. Generate dependency graph
7. Create parallel execution examples
8. Validate task completeness:
   → All contracts have tests?
   → All entities have models?
   → All endpoints implemented?
9. Return: SUCCESS (tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
- **Web app**: `backend/src/`, `frontend/src/`
- Paths shown below use web app structure from plan.md

## Phase 3.1: Setup & Database Preparation
- [ ] T001 Create database migration V017__create_audit_log_viewer_indexes.sql with performance indexes for audit log queries
- [ ] T002 Create database migration V018__create_audit_export_table.sql for audit log export tracking
- [ ] T003 [P] Add audit log viewer dependencies to backend/build.gradle (no new deps needed, verify existing)

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### Contract Tests (Backend API)
- [ ] T004 [P] Contract test GET /api/audit/logs in backend/src/test/java/com/platform/audit/api/AuditLogViewerContractTest.java
- [ ] T005 [P] Contract test GET /api/audit/logs/{id} in backend/src/test/java/com/platform/audit/api/AuditLogDetailContractTest.java
- [ ] T006 [P] Contract test POST /api/audit/export in backend/src/test/java/com/platform/audit/api/AuditExportContractTest.java
- [ ] T007 [P] Contract test GET /api/audit/export/{exportId}/status in backend/src/test/java/com/platform/audit/api/AuditExportStatusContractTest.java
- [ ] T008 [P] Contract test GET /api/audit/export/{token}/download in backend/src/test/java/com/platform/audit/api/AuditExportDownloadContractTest.java

### Integration Tests (Full User Journeys)
- [ ] T009 [P] Integration test audit log viewing with permissions in backend/src/test/java/com/platform/audit/integration/AuditLogViewerIntegrationTest.java
- [ ] T010 [P] Integration test audit log search and filtering in backend/src/test/java/com/platform/audit/integration/AuditLogSearchIntegrationTest.java
- [ ] T011 [P] Integration test audit log export workflow in backend/src/test/java/com/platform/audit/integration/AuditLogExportIntegrationTest.java
- [ ] T012 [P] Integration test role-based access control in backend/src/test/java/com/platform/audit/integration/AuditLogPermissionsIntegrationTest.java

### Frontend Component Tests
- [ ] T013 [P] Unit test AuditLogViewer component in frontend/src/components/audit/__tests__/AuditLogViewer.test.tsx
- [ ] T014 [P] Unit test AuditLogFilters component in frontend/src/components/audit/__tests__/AuditLogFilters.test.tsx
- [ ] T015 [P] Unit test AuditLogExport component in frontend/src/components/audit/__tests__/AuditLogExport.test.tsx

## Phase 3.3: Backend Core Implementation (ONLY after tests are failing)

### Data Models and DTOs
- [ ] T016 [P] AuditLogEntryDTO in backend/src/main/java/com/platform/audit/api/AuditLogEntryDTO.java
- [ ] T017 [P] AuditLogDetailDTO in backend/src/main/java/com/platform/audit/api/AuditLogDetailDTO.java
- [ ] T018 [P] AuditLogSearchResponse in backend/src/main/java/com/platform/audit/api/AuditLogSearchResponse.java
- [ ] T019 [P] AuditLogFilter in backend/src/main/java/com/platform/audit/internal/AuditLogFilter.java
- [ ] T020 [P] AuditLogExportRequest entity in backend/src/main/java/com/platform/audit/internal/AuditLogExportRequest.java

### Repository Layer
- [ ] T021 [P] AuditLogViewRepository with filtered queries in backend/src/main/java/com/platform/audit/internal/AuditLogViewRepository.java
- [ ] T022 [P] AuditLogExportRepository in backend/src/main/java/com/platform/audit/internal/AuditLogExportRepository.java

### Service Layer
- [ ] T023 AuditLogViewService with permission filtering in backend/src/main/java/com/platform/audit/internal/AuditLogViewService.java
- [ ] T024 AuditLogSearchService with full-text search in backend/src/main/java/com/platform/audit/internal/AuditLogSearchService.java
- [ ] T025 AuditLogExportService with async processing in backend/src/main/java/com/platform/audit/internal/AuditLogExportService.java
- [ ] T026 [P] UserPermissionScopeService in backend/src/main/java/com/platform/audit/internal/UserPermissionScopeService.java

### REST Controllers
- [ ] T027 AuditLogViewController with all endpoints in backend/src/main/java/com/platform/audit/api/AuditLogViewController.java
- [ ] T028 AuditLogExportController with export endpoints in backend/src/main/java/com/platform/audit/api/AuditLogExportController.java

## Phase 3.4: Frontend Implementation

### State Management (Redux/RTK Query)
- [ ] T029 [P] Audit log API slice in frontend/src/store/api/auditLogApi.ts
- [ ] T030 [P] Audit log slice for client state in frontend/src/store/slices/auditLogSlice.ts

### React Components
- [ ] T031 [P] AuditLogViewer container component in frontend/src/components/audit/AuditLogViewer.tsx
- [ ] T032 [P] AuditLogTable with pagination in frontend/src/components/audit/AuditLogTable.tsx
- [ ] T033 [P] AuditLogFilters with date/search/type filters in frontend/src/components/audit/AuditLogFilters.tsx
- [ ] T034 [P] AuditLogDetails modal component in frontend/src/components/audit/AuditLogDetails.tsx
- [ ] T035 [P] AuditLogExport with format selection in frontend/src/components/audit/AuditLogExport.tsx

### Page Components
- [ ] T036 AuditLogPage main page component in frontend/src/pages/audit/AuditLogPage.tsx
- [ ] T037 Add audit log route to React Router in frontend/src/App.tsx

### Type Definitions
- [ ] T038 [P] TypeScript types for audit log API in frontend/src/types/auditLog.ts

## Phase 3.5: Integration & Security

### Backend Integration
- [ ] T039 Security configuration for audit viewer endpoints in backend/src/main/java/com/platform/shared/config/SessionSecurityConfiguration.java
- [ ] T040 Permission evaluation integration with user module in backend/src/main/java/com/platform/audit/internal/AuditLogViewService.java
- [ ] T041 Rate limiting configuration for export endpoints in backend/src/main/java/com/platform/shared/config/SecurityConfig.java

### Database Integration
- [ ] T042 Apply database migrations and verify indexes work correctly
- [ ] T043 Seed test audit data for development in backend/src/main/resources/db/migration/V019__seed_audit_test_data.sql

### Frontend Integration
- [ ] T044 Integrate audit log viewer with navigation menu in frontend/src/components/layouts/DashboardLayout.tsx
- [ ] T045 Add error handling and loading states in frontend/src/components/audit/AuditLogViewer.tsx

## Phase 3.6: End-to-End Tests

### E2E Test Implementation
- [ ] T046 [P] E2E test complete audit log viewing workflow in frontend/tests/e2e/audit-log-viewer.spec.ts
- [ ] T047 [P] E2E test audit log search and filtering in frontend/tests/e2e/audit-log-search.spec.ts
- [ ] T048 [P] E2E test audit log export workflow in frontend/tests/e2e/audit-log-export.spec.ts
- [ ] T049 [P] E2E test permission-based access control in frontend/tests/e2e/audit-log-permissions.spec.ts

## Phase 3.7: Polish & Performance

### Performance Optimization
- [ ] T050 Database query performance optimization and monitoring in backend/src/main/java/com/platform/audit/internal/AuditLogViewRepository.java
- [ ] T051 [P] Frontend performance optimization with React.memo and useMemo in frontend/src/components/audit/AuditLogTable.tsx
- [ ] T052 [P] Implement export progress tracking in frontend/src/components/audit/AuditLogExport.tsx

### Unit Tests for Business Logic
- [ ] T053 [P] Unit tests for permission filtering logic in backend/src/test/java/com/platform/audit/internal/UserPermissionScopeServiceTest.java
- [ ] T054 [P] Unit tests for search functionality in backend/src/test/java/com/platform/audit/internal/AuditLogSearchServiceTest.java
- [ ] T055 [P] Unit tests for export generation in backend/src/test/java/com/platform/audit/internal/AuditLogExportServiceTest.java

### Documentation and Validation
- [ ] T056 [P] Update API documentation with new audit viewer endpoints in backend/src/main/java/com/platform/audit/api/
- [ ] T057 Execute quickstart.md validation scenarios to verify complete implementation
- [ ] T058 Performance testing to ensure <2s page loads and <1s search responses

## Dependencies

### Critical Dependencies (Must be Sequential)
- Database setup (T001-T002) → Contract tests (T004-T008)
- Contract tests (T004-T008) → Backend implementation (T016-T028)
- Backend API (T027-T028) → Frontend API integration (T029-T030)
- Core components (T031-T035) → Page integration (T036-T037)
- Backend services → Security integration (T039-T041)

### Parallel Groups
**Group A - Contract Tests**: T004, T005, T006, T007, T008
**Group B - Integration Tests**: T009, T010, T011, T012
**Group C - Frontend Tests**: T013, T014, T015
**Group D - DTOs**: T016, T017, T018, T019, T020
**Group E - Repositories**: T021, T022
**Group F - Frontend State**: T029, T030
**Group G - React Components**: T031, T032, T033, T034, T035
**Group H - E2E Tests**: T046, T047, T048, T049
**Group I - Unit Tests**: T053, T054, T055

## Parallel Example
```bash
# Launch Group A (Contract Tests) together:
Task: "Contract test GET /api/audit/logs in backend/src/test/java/com/platform/audit/api/AuditLogViewerContractTest.java"
Task: "Contract test GET /api/audit/logs/{id} in backend/src/test/java/com/platform/audit/api/AuditLogDetailContractTest.java"
Task: "Contract test POST /api/audit/export in backend/src/test/java/com/platform/audit/api/AuditExportContractTest.java"
Task: "Contract test GET /api/audit/export/{exportId}/status in backend/src/test/java/com/platform/audit/api/AuditExportStatusContractTest.java"

# Launch Group D (DTOs) together after contracts are failing:
Task: "AuditLogEntryDTO in backend/src/main/java/com/platform/audit/api/AuditLogEntryDTO.java"
Task: "AuditLogDetailDTO in backend/src/main/java/com/platform/audit/api/AuditLogDetailDTO.java"
Task: "AuditLogSearchResponse in backend/src/main/java/com/platform/audit/api/AuditLogSearchResponse.java"
```

## Notes
- [P] tasks = different files, no dependencies between them
- Verify ALL tests fail before implementing (RED phase of TDD)
- Each commit should make at least one test pass (GREEN phase)
- Refactor after tests pass (REFACTOR phase)
- Run integration tests with TestContainers (real PostgreSQL)

## Task Generation Rules Applied

1. **From Contracts**: 5 endpoints → 5 contract tests (T004-T008)
2. **From Data Model**: 5 entities → 5 model tasks (T016-T020)
3. **From User Stories**: 4 journeys → 4 integration tests (T009-T012) + 4 E2E tests (T046-T049)
4. **Ordering**: Setup → Tests → Models → Services → Controllers → Frontend → Integration → Polish

## Validation Checklist

- ✓ All 5 API contracts have corresponding tests (T004-T008)
- ✓ All 5 entities have model tasks (T016-T020)
- ✓ All tests come before implementation (Phase 3.2 before 3.3)
- ✓ Parallel tasks are truly independent (different files)
- ✓ Each task specifies exact file path
- ✓ No [P] task modifies same file as another [P] task
- ✓ TDD order enforced: Contract → Integration → E2E → Implementation
- ✓ Real dependencies used (TestContainers for integration tests)

## Constitutional Compliance

- **Library-First**: Extends existing audit module with new viewer functionality
- **TDD Required**: 15 test tasks (T004-T015) before any implementation
- **Module Boundaries**: All new code in audit module, proper API boundaries
- **Real Dependencies**: TestContainers for PostgreSQL, actual Redis for sessions
- **Observability**: Audit logging of viewer access, structured logging throughout

---

**Total Tasks**: 58 tasks across 6 phases
**Estimated Completion**: 15-20 development days
**Ready for Implementation**: YES - All tests designed to fail initially, strict TDD ordering enforced