# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quick Reference Shortcuts

- **QNEW**: Understand and follow best practices - analyze existing code patterns first
- **QPLAN**: Analyze codebase consistency before implementing changes
- **QCODE**: Implement plan following TDD and validate all tests pass
- **QCHECK**: Skeptically review code changes for quality and consistency
- **QUX**: Generate comprehensive UX test scenarios for user journeys

## Project Overview

This is a Specification-Driven Development (SDD) framework that enforces constitutional principles for feature implementation. The project uses a strict TDD approach with library-first architecture and CLI interfaces.

## Key Commands

### Feature Development Workflow
```bash
# Create a new feature branch (format: ###-feature-name)
./.specify/scripts/bash/create-new-feature.sh

# Get current feature paths and status
./.specify/scripts/bash/get-feature-paths.sh

# Setup implementation plan for a feature
./.specify/scripts/bash/setup-plan.sh

# Check task prerequisites before execution
./.specify/scripts/bash/check-task-prerequisites.sh

# Update agent context (for AI assistants)
./.specify/scripts/bash/update-agent-context.sh [AGENT_TYPE]
```

### PowerShell Alternatives (Windows)
```powershell
# Same functionality available in PowerShell
./.specify/scripts/powershell/create-new-feature.ps1
./.specify/scripts/powershell/get-feature-paths.ps1
./.specify/scripts/powershell/setup-plan.ps1
./.specify/scripts/powershell/check-task-prerequisites.ps1
./.specify/scripts/powershell/update-agent-context.ps1 -AgentType [AGENT_TYPE]
```

## Architecture & Structure

### Feature Development Structure
Features are organized by branch name in the `specs/` directory:
```
specs/[###-feature-name]/
├── spec.md          # Feature specification (business requirements)
├── plan.md          # Implementation plan (technical approach)
├── research.md      # Research findings and decisions
├── data-model.md    # Data entities and relationships
├── quickstart.md    # Quick validation guide
├── contracts/       # API contracts and schemas
└── tasks.md         # Ordered implementation tasks
```

### Source Code Structure Options
The framework supports three project structures:

1. **Single Project (Default)**
   ```
   src/
   ├── models/
   ├── services/
   ├── cli/
   └── lib/
   tests/
   ├── contract/
   ├── integration/
   └── unit/
   ```

2. **Web Application** (frontend + backend)
   ```
   backend/
   ├── src/
   └── tests/
   frontend/
   ├── src/
   └── tests/
   ```

3. **Mobile + API** (iOS/Android + backend)
   ```
   api/
   └── [backend structure]
   ios/ or android/
   └── [platform-specific]
   ```

## Constitutional Principles

### Core Development Rules
1. **Library-First**: Every feature must be a standalone library with CLI interface
2. **Test-First (NON-NEGOTIABLE)**: TDD required - tests written → approved → fail → implement
3. **Test Order**: Contract → Integration → E2E → Unit (strictly enforced)
4. **Real Dependencies**: Use actual databases/services, not mocks in integration tests
5. **Observability**: Structured logging required with multi-tier log streaming
6. **Versioning**: MAJOR.MINOR.BUILD format with BUILD increments on every change
7. **Simplicity**: Maximum 3 projects, avoid patterns without proven need

### Development Workflow Gates
- Constitution compliance check before Phase 0 research
- Re-check after Phase 1 design
- All NEEDS CLARIFICATION must be resolved before proceeding
- Complexity deviations must be documented and justified

## Command Implementation Flow

### /specify Command
Creates feature specification from user description:
1. Parse user description
2. Extract key concepts (actors, actions, data, constraints)
3. Mark unclear aspects with [NEEDS CLARIFICATION]
4. Generate user scenarios and acceptance criteria
5. Create functional requirements (testable)
6. Identify key entities

### /plan Command
Creates implementation plan from specification:
1. Load feature spec
2. Detect project type and structure
3. Check constitution compliance
4. Execute Phase 0: Research (resolve unknowns)
5. Execute Phase 1: Design (contracts, data model, quickstart)
6. Plan Phase 2: Task generation approach
7. Stop (ready for /tasks command)

### /tasks Command
Generates ordered implementation tasks:
1. Load plan and design documents
2. Generate test tasks first (TDD)
3. Generate implementation tasks
4. Apply dependency ordering
5. Mark parallel tasks with [P]

## Testing Requirements

### Test Types Priority
1. **Contract Tests**: Verify API schemas and contracts
2. **Integration Tests**: Test inter-service communication
3. **End-to-End Tests**: Validate complete user journeys
4. **Unit Tests**: Test individual functions (lowest priority)

### When Integration Tests Required
- New library creation
- Contract changes
- Inter-service communication
- Shared schema modifications

## Working with Templates

Key templates available:
- `.specify/templates/spec-template.md` - Feature specification
- `.specify/templates/plan-template.md` - Implementation plan
- `.specify/templates/tasks-template.md` - Task list
- `.specify/templates/agent-file-template.md` - AI assistant context

## Current Active Feature: Spring Boot Modulith Payment Platform

**Branch**: `005-spring-boot-modulith`
**Status**: Planning complete, ready for tasks generation

### Technology Stack
- **Backend**: Java 21, Spring Boot 3.2+, Spring Modulith 1.1+
- **Frontend**: React 18+, TypeScript 5.0+, Redux Toolkit
- **Database**: PostgreSQL 15+ (primary), Redis 7+ (sessions)
- **Security**: OAuth2/PKCE, opaque tokens (SHA-256 + salt)
- **Payments**: Stripe integration with webhook processing
- **Testing**: JUnit 5, TestContainers, Playwright E2E

### Architecture Decisions
- **Modular Monolith**: Spring Modulith with ArchUnit boundary enforcement
- **Module Communication**: Event-driven via ApplicationEventPublisher
- **Authentication**: Custom token storage, no JWT (security requirement)
- **Multi-tenancy**: Organization-based isolation with tenant context
- **Compliance**: GDPR-ready with audit logging and PII redaction

### Module Structure
```
backend/src/main/java/com/platform/
├── auth/           # OAuth2/PKCE, session management
├── payment/        # Stripe integration, webhooks
├── user/           # User/organization management
├── subscription/   # Plans, billing cycles
├── audit/          # Compliance logging
└── shared/         # Common utilities, security
```

### Key Constraints
- **TDD Required**: Contract → Integration → E2E → Unit test order
- **Opaque Tokens Only**: No custom JWT implementation
- **Real Dependencies**: TestContainers for integration tests
- **GDPR Compliance**: PII redaction, retention policies
- **Production Ready**: Monitoring, structured logging, security

### Testing Strategy
- Contract tests for API schemas (OpenAPI validation)
- Integration tests with real PostgreSQL/Redis
- Stripe webhooks with signature verification
- E2E flows for complete user journeys
- ArchUnit tests for module boundaries

### Performance Targets
- API latency < 200ms (p99)
- Payment success rate > 95%
- Availability > 99.9%
- Multi-tenant isolation enforced

## Implementation Best Practices

### Pre-Coding Requirements
- **Ask clarifying questions** before starting any implementation
- **Draft and confirm approach** for complex features (>50 lines of code)
- **Analyze existing patterns** in codebase before adding new ones
- **List pros/cons** if multiple implementation approaches exist

### Coding Standards
- **Follow TDD strictly**: Write failing tests → implement → refactor
- **Use consistent domain vocabulary** for function/method names
- **Prefer simple, composable, testable functions** over complex ones
- **Use branded types for IDs** (e.g., `UserId`, `OrganizationId`)
- **Minimize comments** - rely on self-explanatory code
- **Default to interfaces over classes** unless inheritance needed

### Java-Specific Guidelines
- **Use records for immutable data** instead of traditional POJOs
- **Leverage Optional<T>** for nullable values, avoid null returns
- **Use Stream API** for collections processing
- **Prefer composition over inheritance**
- **Use Spring's dependency injection** properly (constructor injection)

### TypeScript-Specific Guidelines
- **Use `import type`** for type-only imports
- **Default to `type` over `interface`** unless extension needed
- **Use branded types** for IDs: `type UserId = string & { __brand: 'UserId' }`
- **Prefer `const` assertions** for immutable objects

## Testing Best Practices

### Test Organization
- **Java**: Colocate tests in `src/test/java/` mirroring source structure
- **TypeScript**: Colocate unit tests as `*.spec.ts` files
- **Integration tests**: Separate directory structure for cross-system tests

### Test Types & Priority
1. **Contract Tests**: API schema validation (OpenAPI compliance)
2. **Integration Tests**: Cross-module/service communication with real dependencies
3. **E2E Tests**: Complete user journey validation
4. **Unit Tests**: Pure logic testing (lowest priority)

### Test Guidelines
- **Use TestContainers** for integration tests (real databases/services)
- **Test entire data structures** in single assertions when possible
- **Test edge cases** and boundary values thoroughly
- **Avoid heavy mocking** - prefer integration tests
- **Each test should validate one behavior**
- **Use descriptive test names** that explain the scenario

### Database Testing
- **Separate pure logic tests** from database-touching tests
- **Use transaction rollback** for test isolation
- **Test with realistic data volumes** for performance validation

## Code Organization Rules

### Module Boundaries (Spring Modulith)
- **Keep internal packages truly internal** - no cross-module dependencies
- **Use events for module communication** - avoid direct service calls
- **Public APIs only in `api/` packages**
- **Enforce boundaries with ArchUnit tests**

### Shared Code Policy
- **Only place code in `shared/`** if used by 2+ modules
- **Avoid premature extraction** to shared packages
- **Keep domain-specific logic in respective modules**

### File Organization
- **Group by feature, not by layer** within modules
- **Keep related files close together**
- **Use consistent naming conventions** across the project

## Quality Gates

### Required Checks (Must Pass)
- **All tests pass**: Unit, integration, contract, E2E
- **ArchUnit tests pass**: Module boundary enforcement
- **Linting passes**: Checkstyle (Java), ESLint (TypeScript)
- **Formatting passes**: Prettier for TypeScript
- **Type checking passes**: TypeScript compilation
- **Security scan passes**: No critical vulnerabilities

### Performance Requirements
- **API response time**: < 200ms (p99)
- **Database queries**: < 50ms average
- **Frontend bundle size**: < 500KB gzipped
- **Test execution**: < 30s for unit tests, < 5min for integration

## Git Commit Standards

### Conventional Commits Format
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Commit Types
- **feat**: New feature
- **fix**: Bug fix
- **refactor**: Code refactoring
- **test**: Adding/updating tests
- **docs**: Documentation updates
- **chore**: Maintenance tasks

### Examples
```
feat(auth): implement OAuth2/PKCE authentication
fix(payment): resolve Stripe webhook signature validation
test(subscription): add integration tests for plan changes
refactor(user): simplify organization invitation logic
```

### Important Rules
- **Never mention AI tools** in commit messages
- **Write tests first, commit separately** from implementation
- **Keep commits atomic** - one logical change per commit
- **Include test files** in implementation commits when related

## Error Handling Standards

### Exception Handling
- **Use specific exception types** for different error conditions
- **Include context** in error messages for debugging
- **Log errors with correlation IDs** for traceability
- **Don't expose internal details** in API responses
- **Validate inputs early** and fail fast

### API Error Responses
- **Consistent error format** across all endpoints
- **Include error codes** for programmatic handling
- **Provide helpful error messages** for client developers
- **Log all errors** with sufficient context for debugging

## Security Guidelines

### Authentication & Authorization
- **Never store passwords in plain text**
- **Use secure session management** (Redis-backed, proper TTL)
- **Implement proper RBAC** with organization-level isolation
- **Validate all inputs** against injection attacks
- **Use HTTPS everywhere** in production

### Data Protection
- **Encrypt sensitive data** at rest
- **Redact PII from logs** automatically
- **Implement proper audit trails** for compliance
- **Follow GDPR requirements** for data retention and deletion

## Important Notes

- Feature branches must follow format: `###-feature-name` (e.g., 001-user-auth)
- All libraries must expose CLI with --help, --version, --format flags
- Library documentation should use llms.txt format
- Avoid wrapper classes - use frameworks directly
- No DTOs unless serialization differs from domain model
- No Repository/Unit of Work patterns without proven need
- Git commits must show tests before implementation (RED-GREEN-Refactor)