# Implementation Plan: Alternative Password Authentication


**Branch**: `009-check-existing-project` | **Date**: 2025-09-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/009-check-existing-project/spec.md`

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
Add conditional password authentication alongside existing OAuth2 implementation in Spring Boot Modulith payment platform. Authentication method is controlled via environment flag (ENABLE_PASSWORD_AUTH). When enabled, users see both OAuth2 and password options. Implementation extends existing auth module, reuses opaque token infrastructure, and maintains constitutional compliance with TDD requirements.

## Technical Context
**Language/Version**: Java 21, TypeScript 5.0+
**Primary Dependencies**: Spring Boot 3.2+, Spring Modulith 1.1+, React 18+, Redux Toolkit
**Storage**: PostgreSQL 15+ (primary), Redis 7+ (sessions)
**Testing**: JUnit 5, TestContainers, Playwright E2E
**Target Platform**: Linux server (backend), Web browsers (frontend)
**Project Type**: web (frontend + backend)
**Performance Goals**: API latency <200ms (p99), authentication <250ms
**Constraints**: Opaque tokens only (no JWT), GDPR compliance, multi-tenant isolation
**Scale/Scope**: 10k users, enterprise-grade security, production-ready

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Simplicity**:
- Projects: 2 (backend, frontend) ✓
- Using framework directly? Yes - Spring Boot, React ✓
- Single data model? Yes - extend existing User entity ✓
- Avoiding patterns? Yes - no unnecessary abstractions ✓

**Architecture**:
- EVERY feature as library? Yes - password-auth library within auth module ✓
- Libraries listed:
  - password-auth: Handles password authentication flows
  - password-cli: CLI for password management operations
- CLI per library: auth-cli with --login, --reset, --verify commands ✓
- Library docs: llms.txt format planned? Yes ✓

**Testing (NON-NEGOTIABLE)**:
- RED-GREEN-Refactor cycle enforced? Yes - tests written first ✓
- Git commits show tests before implementation? Yes ✓
- Order: Contract→Integration→E2E→Unit strictly followed? Yes ✓
- Real dependencies used? Yes - TestContainers for PostgreSQL/Redis ✓
- Integration tests for: password auth endpoints, session management ✓
- FORBIDDEN: Will not implement before tests ✓

**Observability**:
- Structured logging included? Yes - with correlation IDs ✓
- Frontend logs → backend? Yes - existing log streaming ✓
- Error context sufficient? Yes - includes auth method, user context ✓

**Versioning**:
- Version number assigned? 1.0.0 for initial release ✓
- BUILD increments on every change? Yes - CI/CD configured ✓
- Breaking changes handled? No breaking changes - additive only ✓

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

**Structure Decision**: Option 2 (Web application) - Project has frontend + backend structure

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
- Password auth API contract tests (8 endpoints) → 8 contract test tasks [P]
- Database schema changes → 2 migration tasks
- Backend entities/services → 6 implementation tasks
- Frontend components → 4 React/Redux tasks
- CLI implementation → 2 CLI tasks
- Integration tests → 5 cross-system test tasks
- E2E scenarios → 3 user journey tests

**Ordering Strategy**:
- Constitutional TDD order: Contract → Integration → E2E → Unit
- Dependency order: Database → Models → Services → API → Frontend
- Mark [P] for parallel execution within same test type
- Database migrations must complete before any backend code
- Contract tests before any endpoint implementation
- Integration tests before E2E tests

**Specific Task Categories**:
1. **Database Tasks (Sequential)**:
   - Migration for password fields
   - Migration for authentication_attempts table

2. **Contract Tests (Parallel)**:
   - /auth/login endpoint contract test [P]
   - /auth/register endpoint contract test [P]
   - /auth/verify-email endpoint contract test [P]
   - /auth/request-password-reset contract test [P]
   - /auth/reset-password contract test [P]
   - /auth/change-password contract test [P]
   - /auth/resend-verification contract test [P]
   - /auth/methods endpoint contract test [P]

3. **Backend Implementation (Sequential after contracts)**:
   - Password authentication service
   - Password validation logic
   - Email service integration
   - Rate limiting service
   - Audit logging extension
   - API controller endpoints

4. **Frontend Implementation (Parallel after backend)**:
   - Login form component [P]
   - Registration form component [P]
   - Password reset flow components [P]
   - Redux state management updates [P]

5. **Integration Tests (After implementation)**:
   - End-to-end authentication flows
   - Rate limiting behavior
   - Email verification process
   - Password reset workflow
   - OAuth + password coexistence

6. **CLI Implementation (Parallel)**:
   - auth-cli command structure [P]
   - Administrative commands [P]

**Estimated Output**: 30-35 numbered, ordered tasks in tasks.md

**Task Dependencies**:
- All contract tests can run in parallel (must fail initially)
- Backend implementation depends on contract tests existing
- Frontend depends on backend API availability
- Integration tests depend on both backend and frontend
- E2E tests depend on complete implementation

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