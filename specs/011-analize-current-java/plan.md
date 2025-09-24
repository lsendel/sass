# Implementation Plan: Java API Integration Testing Strategy


**Branch**: `011-analize-current-java` | **Date**: 2025-09-24 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/011-analize-current-java/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Update Progress Tracking: Initial Constitution Check
4. Execute Phase 0 → research.md
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
5. Execute Phase 1 → contracts, data-model.md, quickstart.md, agent-specific template file (e.g., `CLAUDE.md` for Claude Code, `.github/copilot-instructions.md` for GitHub Copilot, or `GEMINI.md` for Gemini CLI).
6. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
7. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
8. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Create a comprehensive integration testing framework for the Spring Boot Modulith Payment Platform API that provides complete test coverage across all 6 modules (auth, payment, subscription, user, audit, shared), with clear visibility into test execution progress, robust automation capabilities, and seamless CI/CD integration. The framework will support parallel test execution, environment-specific configurations, and detailed reporting for stakeholder visibility.

## Technical Context
**Language/Version**: Java 21 / Spring Boot 3.2.0 / Spring Modulith 1.1.0
**Primary Dependencies**: RestAssured, TestContainers, WireMock, ArchUnit, JUnit 5
**Storage**: PostgreSQL (production), H2 (unit tests), Redis (session management)
**Testing**: JUnit 5, TestContainers, RestAssured, MockMvc, ArchUnit
**Target Platform**: Linux server (Docker containers), CI/CD pipelines (GitHub Actions)
**Project Type**: web (backend API + frontend React application)
**Performance Goals**: <200ms p95 response time, support 100 concurrent tests, <10min total execution
**Constraints**: Must maintain module boundaries, require real dependencies (no mocks), GDPR compliance
**Scale/Scope**: 60+ API endpoints, 6 modules, 25 functional requirements, parallel test execution

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Simplicity**:
- Projects: 2 (test framework, test execution engine) ✓
- Using framework directly? Yes - RestAssured, TestContainers used directly ✓
- Single data model? Yes - using existing API models, no separate test DTOs ✓
- Avoiding patterns? Yes - direct test implementation, no unnecessary abstractions ✓

**Architecture**:
- EVERY feature as library? Yes - test framework as reusable library ✓
- Libraries listed:
  - test-framework-core: Base test utilities and configurations
  - test-data-factory: Test data generation and fixtures
  - test-reporting: Report generation and metrics tracking
  - test-environment: Environment configuration management
- CLI per library: test-runner CLI with --help/--version/--format/--parallel/--env ✓
- Library docs: llms.txt format planned for each test library ✓

**Testing (NON-NEGOTIABLE)**:
- RED-GREEN-Refactor cycle enforced? YES - tests written first, must fail ✓
- Git commits show tests before implementation? YES - enforced in process ✓
- Order: Contract→Integration→E2E→Unit strictly followed? YES ✓
- Real dependencies used? YES - TestContainers for PostgreSQL/Redis ✓
- Integration tests for: new libraries, contract changes, shared schemas? YES ✓
- FORBIDDEN: Implementation before test, skipping RED phase ✓

**Observability**:
- Structured logging included? Yes - JSON logging for test execution ✓
- Frontend logs → backend? N/A (testing backend API only)
- Error context sufficient? Yes - full request/response capture on failures ✓

**Versioning**:
- Version number assigned? 1.0.0 for initial release ✓
- BUILD increments on every change? Yes - CI/CD auto-increment ✓
- Breaking changes handled? Yes - parallel test execution for migration ✓

## Project Structure

### Documentation (this feature)
```
specs/[###-feature]/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
# Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure]
```

**Structure Decision**: Option 2 (Web application) - Backend API testing framework with future frontend test support

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - For each NEEDS CLARIFICATION → research task
   - For each dependency → best practices task
   - For each integration → patterns task

2. **Generate and dispatch research agents**:
   ```
   For each unknown in Technical Context:
     Task: "Research {unknown} for {feature context}"
   For each technology choice:
     Task: "Find best practices for {tech} in {domain}"
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]

**Output**: research.md with all NEEDS CLARIFICATION resolved

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Entity name, fields, relationships
   - Validation rules from requirements
   - State transitions if applicable

2. **Generate API contracts** from functional requirements:
   - For each user action → endpoint
   - Use standard REST/GraphQL patterns
   - Output OpenAPI/GraphQL schema to `/contracts/`

3. **Generate contract tests** from contracts:
   - One test file per endpoint
   - Assert request/response schemas
   - Tests must fail (no implementation yet)

4. **Extract test scenarios** from user stories:
   - Each story → integration test scenario
   - Quickstart test = story validation steps

5. **Update agent file incrementally** (O(1) operation):
   - Run `/scripts/bash/update-agent-context.sh claude` for your AI assistant
   - If exists: Add only NEW tech from current plan
   - Preserve manual additions between markers
   - Update recent changes (keep last 3)
   - Keep under 150 lines for token efficiency
   - Output to repository root

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, agent-specific file

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Each API endpoint (20 endpoints) → contract test task [P]
- Each entity (8 core entities) → model creation task [P]
- Each test framework component → library creation task
- Each module (6 modules) → integration test suite task
- Implementation tasks to make contract tests pass

**Ordering Strategy**:
- TDD order: Contract tests → Integration tests → Implementation
- Dependency order:
  1. Test framework libraries (core, data-factory, reporting, environment)
  2. Contract tests for API endpoints
  3. Model creation for test entities
  4. Integration test suites per module
  5. Cross-module integration scenarios
  6. E2E test scenarios
  7. Performance test baseline
- Mark [P] for parallel execution (independent modules/endpoints)

**Task Categories**:
1. **Framework Setup** (5 tasks): Core libraries, CLI, configuration
2. **Contract Tests** (20 tasks): One per API endpoint from OpenAPI spec
3. **Data Models** (8 tasks): Test entity models and repositories
4. **Module Test Suites** (6 tasks): Per-module integration tests
5. **Integration Scenarios** (12 tasks): Cross-module test scenarios
6. **Reporting & Monitoring** (4 tasks): Test reports, metrics, dashboards
7. **Performance Testing** (3 tasks): Load tests, benchmarks, monitoring
8. **CI/CD Integration** (2 tasks): Pipeline config, automation

**Estimated Output**: 60 numbered, ordered tasks in tasks.md with clear dependencies

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented (none required)

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*