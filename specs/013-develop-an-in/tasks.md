# Tasks: Security Observability Dashboard

**Input**: Design documents from `/specs/013-develop-an-in/`
**Prerequisites**: plan.md (✓), research.md (✓), data-model.md (✓), contracts/ (✓)

## Path Conventions
**Spring Boot Modulith Structure**:
- **Backend**: `backend/src/main/java/com/platform/security/dashboard/`
- **Frontend**: `frontend/src/components/security/dashboard/`
- **Tests**: `backend/src/test/java/com/platform/security/dashboard/`, `frontend/src/tests/`
- **Contracts**: `/specs/013-develop-an-in/contracts/`

**Key Architecture Decisions from Research**:
- Hybrid storage: PostgreSQL + InfluxDB + Redis
- Real-time: WebSocket + Server-Sent Events fallback
- Visualization: React + D3.js + Chart.js
- Libraries: security-monitoring-lib, dashboard-ui-lib with CLI tools

## Phase 3.1: Setup
- [ ] T001 Create database migrations for SecurityEvent, Dashboard, DashboardWidget, AlertRule, SecurityMetric, ThreatIndicator in `backend/src/main/resources/db/migration/`
- [ ] T002 [P] Configure InfluxDB connection and security metrics bucket setup in `backend/src/main/resources/application.yml`
- [ ] T003 [P] Configure Redis caching layer for dashboard data in `backend/src/main/java/com/platform/shared/config/CacheConfig.java`
- [ ] T004 [P] Set up WebSocket configuration for real-time events in `backend/src/main/java/com/platform/security/config/WebSocketConfig.java`

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
- [ ] T005 [P] Contract test SecurityEventsApi endpoints in `backend/src/test/java/com/platform/security/api/SecurityEventsApiContractTest.java`
- [ ] T006 [P] Contract test DashboardApi endpoints in `backend/src/test/java/com/platform/security/api/DashboardApiContractTest.java`
- [ ] T007 [P] Contract test MetricsApi endpoints in `backend/src/test/java/com/platform/security/api/MetricsApiContractTest.java`
- [ ] T008 [P] Integration test security analyst monitoring workflow in `backend/src/test/java/com/platform/security/integration/SecurityAnalystWorkflowIntegrationTest.java`
- [ ] T009 [P] Integration test incident response workflow in `backend/src/test/java/com/platform/security/integration/IncidentResponseWorkflowIntegrationTest.java`
- [ ] T010 [P] Integration test dashboard customization workflow in `backend/src/test/java/com/platform/security/integration/DashboardCustomizationIntegrationTest.java`
- [ ] T011 [P] Frontend contract test SecurityEventsApi integration in `frontend/src/tests/contract/SecurityEventsApi.test.ts`
- [ ] T012 [P] Frontend contract test DashboardApi integration in `frontend/src/tests/contract/DashboardApi.test.ts`
- [ ] T013 [P] Frontend contract test MetricsApi integration in `frontend/src/tests/contract/MetricsApi.test.ts`

## Phase 3.3: Core Implementation - Backend Models (ONLY after tests are failing)
- [ ] T014 [P] SecurityEvent entity with JPA annotations in `backend/src/main/java/com/platform/security/internal/SecurityEvent.java`
- [ ] T015 [P] Dashboard entity with widget relationships in `backend/src/main/java/com/platform/security/internal/Dashboard.java`
- [ ] T016 [P] DashboardWidget entity with configuration JSON handling in `backend/src/main/java/com/platform/security/internal/DashboardWidget.java`
- [ ] T017 [P] AlertRule entity with condition expressions in `backend/src/main/java/com/platform/security/internal/AlertRule.java`
- [ ] T018 [P] SecurityMetric entity for time-series data in `backend/src/main/java/com/platform/security/internal/SecurityMetric.java`
- [ ] T019 [P] ThreatIndicator entity for external threat intel in `backend/src/main/java/com/platform/security/internal/ThreatIndicator.java`

## Phase 3.4: Core Implementation - Backend Repositories
- [ ] T020 [P] SecurityEventRepository with time-based queries in `backend/src/main/java/com/platform/security/internal/SecurityEventRepository.java`
- [ ] T021 [P] DashboardRepository with permission filtering in `backend/src/main/java/com/platform/security/internal/DashboardRepository.java`
- [ ] T022 [P] AlertRuleRepository with trigger tracking in `backend/src/main/java/com/platform/security/internal/AlertRuleRepository.java`
- [ ] T023 [P] SecurityMetricRepository with InfluxDB queries in `backend/src/main/java/com/platform/security/internal/SecurityMetricRepository.java`

## Phase 3.5: Core Implementation - Backend Services
- [ ] T024 [P] SecurityEventService with real-time event processing in `backend/src/main/java/com/platform/security/internal/SecurityEventService.java`
- [ ] T025 [P] DashboardService with widget management in `backend/src/main/java/com/platform/security/internal/DashboardService.java`
- [ ] T026 [P] MetricsService with time-series aggregation in `backend/src/main/java/com/platform/security/internal/MetricsService.java`
- [ ] T027 [P] AlertRuleService with condition evaluation in `backend/src/main/java/com/platform/security/internal/AlertRuleService.java`
- [ ] T028 [P] ThreatIntelService for external threat correlation in `backend/src/main/java/com/platform/security/internal/ThreatIntelService.java`

## Phase 3.6: Backend API Controllers
- [ ] T029 SecurityEventsController implementing all endpoints in `backend/src/main/java/com/platform/security/api/SecurityEventsController.java`
- [ ] T030 DashboardController implementing CRUD operations in `backend/src/main/java/com/platform/security/api/DashboardController.java`
- [ ] T031 MetricsController with real-time streaming in `backend/src/main/java/com/platform/security/api/MetricsController.java`

## Phase 3.7: Real-time Communication
- [ ] T032 WebSocket handler for security event streaming in `backend/src/main/java/com/platform/security/websocket/SecurityEventWebSocketHandler.java`
- [ ] T033 Server-Sent Events controller for metrics streaming in `backend/src/main/java/com/platform/security/api/SSEController.java`
- [ ] T034 Event publisher integration with Spring Events in `backend/src/main/java/com/platform/security/events/SecurityEventPublisher.java`

## Phase 3.8: Frontend Core Components
- [ ] T035 [P] SecurityEvent TypeScript interfaces and DTOs in `frontend/src/types/SecurityEvent.ts`
- [ ] T036 [P] Dashboard TypeScript interfaces and DTOs in `frontend/src/types/Dashboard.ts`
- [ ] T037 [P] API client services for security events in `frontend/src/services/SecurityEventsApiClient.ts`
- [ ] T038 [P] API client services for dashboards in `frontend/src/services/DashboardApiClient.ts`
- [ ] T039 [P] API client services for metrics in `frontend/src/services/MetricsApiClient.ts`
- [ ] T040 [P] WebSocket service for real-time events in `frontend/src/services/WebSocketService.ts`

## Phase 3.9: Frontend Dashboard Components
- [ ] T041 [P] SecurityEventsList component with filtering in `frontend/src/components/security/SecurityEventsList.tsx`
- [ ] T042 [P] SecurityMetricChart component with Chart.js in `frontend/src/components/security/SecurityMetricChart.tsx`
- [ ] T043 [P] ThreatMapWidget component with D3.js visualization in `frontend/src/components/security/ThreatMapWidget.tsx`
- [ ] T044 [P] AlertListWidget component with real-time updates in `frontend/src/components/security/AlertListWidget.tsx`
- [ ] T045 [P] DashboardGrid component with drag-and-drop layout in `frontend/src/components/dashboard/DashboardGrid.tsx`
- [ ] T046 [P] WidgetConfigPanel component for customization in `frontend/src/components/dashboard/WidgetConfigPanel.tsx`

## Phase 3.10: Frontend Pages and Navigation
- [ ] T047 SecurityDashboardPage main dashboard view in `frontend/src/pages/SecurityDashboardPage.tsx`
- [ ] T048 DashboardCustomizationPage for widget management in `frontend/src/pages/DashboardCustomizationPage.tsx`
- [ ] T049 AlertManagementPage for rule configuration in `frontend/src/pages/AlertManagementPage.tsx`

## Phase 3.11: Integration
- [ ] T050 Security event collection integration from existing modules in `backend/src/main/java/com/platform/security/collectors/ModuleEventCollector.java`
- [ ] T051 Authentication integration with existing Spring Security in `backend/src/main/java/com/platform/security/config/SecurityDashboardSecurityConfig.java`
- [ ] T052 Frontend routing and navigation integration in `frontend/src/App.tsx`
- [ ] T053 Error handling and logging integration in both frontend and backend

## Phase 3.12: Libraries (Constitutional Requirement)
- [ ] T054 [P] Create security-monitoring-lib with event collection utilities in `backend/src/main/java/com/platform/security/lib/monitoring/`
- [ ] T055 [P] Create dashboard-ui-lib with reusable widget components in `frontend/src/lib/dashboard/`
- [ ] T056 [P] Security-monitor CLI tool with --help/--version/--format in `backend/src/main/java/com/platform/security/cli/SecurityMonitorCLI.java`
- [ ] T057 [P] Dashboard-config CLI tool for dashboard management in `backend/src/main/java/com/platform/security/cli/DashboardConfigCLI.java`

## Phase 3.13: Polish
- [ ] T058 [P] Unit tests for SecurityEventService in `backend/src/test/java/com/platform/security/internal/SecurityEventServiceTest.java`
- [ ] T059 [P] Unit tests for DashboardService in `backend/src/test/java/com/platform/security/internal/DashboardServiceTest.java`
- [ ] T060 [P] Unit tests for MetricsService in `backend/src/test/java/com/platform/security/internal/MetricsServiceTest.java`
- [ ] T061 [P] Frontend component unit tests with Vitest in `frontend/src/tests/unit/components/`
- [ ] T062 [P] Performance testing for dashboard load (<200ms) in `backend/src/test/java/com/platform/security/performance/DashboardPerformanceTest.java`
- [ ] T063 [P] E2E user journey tests with Playwright in `frontend/src/tests/e2e/SecurityDashboardE2E.spec.ts`
- [ ] T064 [P] Load testing for concurrent users (100+) in `backend/src/test/java/com/platform/security/performance/LoadTest.java`
- [ ] T065 [P] API documentation updates in `docs/api/security-dashboard.md`
- [ ] T066 Manual testing execution from quickstart.md scenarios

## Dependencies
- **Setup before everything**: T001-T004
- **Tests before implementation**: T005-T013 before T014-T053
- **Models before services**: T014-T019 before T024-T028
- **Services before controllers**: T024-T028 before T029-T031
- **API clients before components**: T037-T040 before T041-T046
- **Components before pages**: T041-T046 before T047-T049
- **Core before integration**: T014-T049 before T050-T053
- **Implementation before polish**: All core tasks before T058-T066

## Parallel Execution Examples

### Phase 3.2: Contract Tests (All Parallel)
```bash
# Launch T005-T013 together:
Task: "Contract test SecurityEventsApi endpoints in backend/src/test/java/com/platform/security/api/SecurityEventsApiContractTest.java"
Task: "Contract test DashboardApi endpoints in backend/src/test/java/com/platform/security/api/DashboardApiContractTest.java"
Task: "Contract test MetricsApi endpoints in backend/src/test/java/com/platform/security/api/MetricsApiContractTest.java"
Task: "Integration test security analyst monitoring workflow in backend/src/test/java/com/platform/security/integration/SecurityAnalystWorkflowIntegrationTest.java"
```

### Phase 3.3: Entity Models (All Parallel)
```bash
# Launch T014-T019 together:
Task: "SecurityEvent entity with JPA annotations in backend/src/main/java/com/platform/security/internal/SecurityEvent.java"
Task: "Dashboard entity with widget relationships in backend/src/main/java/com/platform/security/internal/Dashboard.java"
Task: "DashboardWidget entity with configuration JSON handling in backend/src/main/java/com/platform/security/internal/DashboardWidget.java"
```

### Phase 3.12: Libraries (All Parallel)
```bash
# Launch T054-T057 together:
Task: "Create security-monitoring-lib with event collection utilities in backend/src/main/java/com/platform/security/lib/monitoring/"
Task: "Create dashboard-ui-lib with reusable widget components in frontend/src/lib/dashboard/"
Task: "Security-monitor CLI tool with --help/--version/--format in backend/src/main/java/com/platform/security/cli/SecurityMonitorCLI.java"
```

## Performance Requirements
- Dashboard initial load: <200ms (T047, T062)
- Real-time event delivery: <1s (T032, T033, T040)
- API response times: <200ms 95th percentile (T029-T031, T062)
- Concurrent user support: 100+ users (T064)

## Security Requirements
- All endpoints require JWT authentication (T029-T031, T051)
- Role-based dashboard access controls (T025, T051)
- Audit logging for all security events (T024, T050)
- Real-time threat correlation (T028, T050)

## Constitutional Compliance
✅ **TDD Enforced**: Tests T005-T013 must be written first and fail before implementation T014-T066
✅ **Library-First Architecture**: security-monitoring-lib (T054, T056) and dashboard-ui-lib (T055, T057) with CLI tools
✅ **Performance Validated**: <200ms dashboard load (T062), <1s real-time updates (T032-T034), 100+ concurrent users (T064)
✅ **Observability**: Structured logging (T053), error handling (T053), audit trail (T024, T050)

## Notes
- [P] tasks can run in parallel (different files, no dependencies)
- Sequential tasks modify shared files or have clear dependencies
- All tests must be written first and must fail (TDD enforcement)
- Libraries are constitutional requirement - cannot be skipped
- Performance targets must be validated in T062, T064
- Manual testing scenarios from quickstart.md in T066