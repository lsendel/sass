# Implementation Plan: Spring Boot Modulith + React Micro-SaaS Payment Platform

**Branch**: `005-spring-boot-modulith` | **Date**: 2024-01-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-spring-boot-modulith/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path ✅
2. Fill Technical Context (scan for NEEDS CLARIFICATION) ✅
   → Detect Project Type: web application (frontend+backend)
   → Set Structure Decision: Option 2 (Web application)
3. Evaluate Constitution Check section ✅
   → No major violations, some complexity justified for production SaaS
4. Execute Phase 0 → research.md ✅
5. Execute Phase 1 → contracts, data-model.md, quickstart.md, CLAUDE.md ✅
6. Re-evaluate Constitution Check section ✅
7. Plan Phase 2 → Task generation approach described ✅
8. STOP - Ready for /tasks command
```

## Summary
Multi-tenant SaaS payment platform with Spring Boot Modulith backend and React frontend. Implements OAuth2/PKCE authentication with opaque tokens, Stripe payment processing, subscription management, and comprehensive audit logging with GDPR compliance. Uses event-driven module communication with strict boundary enforcement.

## Technical Context
**Language/Version**: Java 21 LTS, TypeScript 5.0+
**Primary Dependencies**: Spring Boot 3.2+, Spring Modulith 1.1+, React 18+, PostgreSQL 15+, Redis 7+
**Storage**: PostgreSQL (primary), Redis (sessions/cache)
**Testing**: JUnit 5, TestContainers, Playwright E2E
**Target Platform**: Linux containers, Kubernetes-ready
**Project Type**: web - determines backend/frontend structure
**Performance Goals**: <200ms API latency p99, >95% payment success rate, 99.9% availability
**Constraints**: GDPR compliance, PCI DSS considerations, opaque tokens only (no JWT)
**Scale/Scope**: Multi-tenant, 10K+ organizations, event-driven architecture

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Simplicity**:
- Projects: 4 (backend library, frontend library, admin-console library, notification-service library) - JUSTIFIED: Production SaaS requires separation of concerns
- Using framework directly? ✅ Spring Boot, React directly (no unnecessary wrappers)
- Single data model? ✅ Domain entities, no DTOs except for API boundaries
- Avoiding patterns? ✅ No Repository/UoW pattern, direct JPA usage

**Architecture**:
- EVERY feature as library? ✅ Each module exposes library interface
- Libraries listed:
  - payment-platform-auth (OAuth2/PKCE, session management)
  - payment-platform-payments (Stripe integration, webhook processing)
  - payment-platform-subscriptions (plan management, billing cycles)
  - payment-platform-users (user/org management, invitations)
  - payment-platform-audit (compliance logging, retention)
  - payment-platform-notifications (email, event processing)
- CLI per library: ✅ Each with --help/--version/--format flags
- Library docs: ✅ llms.txt format for each module

**Testing (NON-NEGOTIABLE)**:
- RED-GREEN-Refactor cycle enforced? ✅ TDD required, tests fail first
- Git commits show tests before implementation? ✅ Contract tests → Integration → Unit → Implementation
- Order: Contract→Integration→E2E→Unit strictly followed? ✅
- Real dependencies used? ✅ TestContainers for PostgreSQL/Redis, actual Stripe test mode
- Integration tests for: ✅ All modules, Stripe webhooks, OAuth flows, event communication
- FORBIDDEN: Implementation before test ✅

**Observability**:
- Structured logging included? ✅ JSON format with correlation IDs, PII redaction
- Frontend logs → backend? ✅ Unified log stream via API
- Error context sufficient? ✅ Full request context, user/organization IDs

**Versioning**:
- Version number assigned? ✅ 1.0.0 (MAJOR.MINOR.BUILD)
- BUILD increments on every change? ✅ Automated in CI/CD
- Breaking changes handled? ✅ API versioning, backward compatibility

## Project Structure

### Documentation (this feature)
```
specs/005-spring-boot-modulith/
├── plan.md              # This file (/plan command output) ✅
├── research.md          # Phase 0 output (/plan command) ✅
├── data-model.md        # Phase 1 output (/plan command) ✅
├── quickstart.md        # Phase 1 output (/plan command) ✅
├── contracts/           # Phase 1 output (/plan command) ✅
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
# Option 2: Web application (detected "Spring Boot" + "React")
backend/
├── src/main/java/com/platform/
│   ├── auth/                    # Authentication Module
│   ├── payment/                 # Payment Module
│   ├── user/                    # User Module
│   ├── subscription/            # Subscription Module
│   ├── audit/                   # Audit Module
│   └── shared/                  # Shared Module
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/            # Flyway migrations
└── src/test/java/
    ├── contract/                # Contract tests
    ├── integration/             # Integration tests
    ├── architecture/            # ArchUnit tests
    └── unit/                    # Unit tests

frontend/
├── src/
│   ├── components/              # React components
│   ├── hooks/                   # Custom hooks
│   ├── services/                # API services
│   ├── store/                   # Redux state
│   └── utils/                   # Utilities
├── public/
└── tests/
    ├── components/              # Component tests
    ├── integration/             # Integration tests
    └── e2e/                     # Playwright E2E

admin-console/
├── src/                         # Admin-specific React app
└── tests/

notification-service/
├── src/main/java/               # Spring Boot service
└── src/test/java/
```

**Structure Decision**: Option 2 (Web application) - backend/frontend separation required for multi-tenant SaaS

## Phase 0: Outline & Research

**Research completed - key findings**:

1. **Spring Modulith Integration**:
   - Decision: Spring Modulith 1.1+ with ArchUnit enforcement
   - Rationale: Compile-time boundary enforcement, event-driven communication, microservice-ready
   - Alternatives: Plain Spring Boot (no boundaries), full microservices (too complex)

2. **OAuth2/PKCE Implementation**:
   - Decision: Spring Security OAuth2 Client with custom token storage
   - Rationale: RFC-compliant, security best practices, opaque token requirement
   - Alternatives: Custom OAuth2 (risky), JWT tokens (not allowed per constraints)

3. **Payment Processing Architecture**:
   - Decision: Stripe integration with webhook idempotency
   - Rationale: Industry standard, comprehensive API, strong security model
   - Alternatives: Square/PayPal (less comprehensive), custom payment (not viable)

4. **Database Architecture**:
   - Decision: PostgreSQL with partitioned audit tables, Redis for sessions
   - Rationale: ACID compliance, JSON support, proven scalability
   - Alternatives: MongoDB (no ACID), MySQL (limited JSON), in-memory only (not persistent)

5. **Frontend Architecture**:
   - Decision: React 18+ with TypeScript, Redux Toolkit for state
   - Rationale: Component reuse, type safety, proven patterns
   - Alternatives: Vue.js (smaller ecosystem), Angular (too heavy), vanilla JS (no types)

**Output**: research.md with all technical decisions documented

## Phase 1: Design & Contracts

### Data Model Entities
Core entities extracted from specification:
- **User**: Authentication, profile, preferences
- **Organization**: Multi-tenancy, team management
- **Subscription**: Plan management, billing cycles
- **Payment**: Transaction processing, history
- **Invoice**: Billing documents, downloads
- **AuditEvent**: Compliance logging, retention
- **TokenMetadata**: Session management, security

### API Contracts Generated
Contract endpoints for each module:
- **Auth Module**: `/auth/login`, `/auth/callback`, `/auth/logout`, `/auth/session`
- **User Module**: `/users`, `/organizations`, `/invitations`
- **Payment Module**: `/payments`, `/payment-methods`, `/webhooks/stripe`
- **Subscription Module**: `/subscriptions`, `/plans`, `/usage`
- **Audit Module**: `/audit-logs`, `/export`

### Contract Tests Created
Failing tests created for all endpoints:
- Schema validation (OpenAPI 3.0)
- Security headers verification
- Error response formats
- CORS policy validation

### Agent Context Updated
CLAUDE.md updated with:
- Module boundaries and communication patterns
- Testing requirements (TDD, integration-first)
- Security constraints (opaque tokens, GDPR)
- Build and deployment processes

**Output**: data-model.md, contracts/*, failing tests, quickstart.md, CLAUDE.md updated

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
1. **Foundation Tasks**: Project setup, database, CI/CD pipeline
2. **Module Tasks**: Each Spring Modulith module with tests-first approach
3. **Integration Tasks**: Inter-module communication, event handling
4. **Security Tasks**: OAuth2, token management, audit logging
5. **Payment Tasks**: Stripe integration, webhook processing
6. **Frontend Tasks**: React components, state management, API integration
7. **Deployment Tasks**: Containerization, environment configuration

**Ordering Strategy**:
- **Wave 1** [P]: Foundation (project structure, database, shared module)
- **Wave 2** [P]: Core modules (auth, user) - parallel after shared
- **Wave 3**: Payment integration (depends on auth + user)
- **Wave 4**: Subscription management (depends on payment)
- **Wave 5** [P]: Frontend components - parallel after API contracts
- **Wave 6**: Integration testing, E2E flows
- **Wave 7**: Production deployment, monitoring

**TDD Task Pattern**:
For each feature:
1. Write contract test [MUST FAIL]
2. Write integration test [MUST FAIL]
3. Write unit tests [MUST FAIL]
4. Implement minimal code to pass tests
5. Refactor while keeping tests green

**Estimated Output**: 45-50 numbered, ordered tasks in tasks.md with [P] parallel markers

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation following TDD principles and constitutional requirements
**Phase 5**: Production deployment, monitoring setup, performance validation

## Complexity Tracking
*Constitutional violations justified for production SaaS requirements*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 4 projects instead of 3 | Production SaaS separation | Single project would violate module boundaries |
| Complex CI/CD pipeline | Production requirements | Simple build insufficient for multi-env deployment |
| Event-driven architecture | Module decoupling | Direct calls would create tight coupling |

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
- [x] Initial Constitution Check: PASS (with justified complexity)
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented

---
*Based on Constitution v2.1.1 - Specification-Driven Development Framework*