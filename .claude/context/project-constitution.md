# Project Constitution Context

This context file provides constitutional principles and enforcement guidelines for all Claude Code sub-agents working on the Spring Boot Modulith payment platform.

## Core Constitutional Principles

### I. Library-First Architecture (NON-NEGOTIABLE)
**Principle**: Every feature must be implemented as a standalone library with CLI interface.

**Implementation Requirements**:
- Self-contained functionality with clear boundaries
- Independent testability without external dependencies
- Documented CLI interface with --help, --version, --format flags
- Clear public API that can be consumed by other modules

**Agent Enforcement**:
- Architecture agents must validate library boundaries
- Development agents must implement CLI interfaces
- Testing agents must verify independent testability

### II. TDD Required (NON-NEGOTIABLE)
**Principle**: Test-Driven Development with RED-GREEN-Refactor cycle is mandatory.

**Implementation Requirements**:
- Tests must be written before implementation
- All tests must initially fail (RED phase)
- Implementation follows until tests pass (GREEN phase)
- Refactoring with continuous test validation

**Forbidden Patterns**:
- Implementation before tests
- Skipping RED phase
- Mock-heavy integration tests
- Test-after development

**Agent Enforcement**:
- TDD Compliance Agent validates test-first approach
- All development agents must reference TDD requirements
- Testing agents enforce proper test hierarchy

### III. Test Order Hierarchy (NON-NEGOTIABLE)
**Principle**: Contract → Integration → E2E → Unit test order strictly enforced.

**Test Hierarchy**:
1. **Contract Tests**: API schema validation, OpenAPI compliance
2. **Integration Tests**: Cross-module communication with real dependencies
3. **E2E Tests**: Complete user journey validation
4. **Unit Tests**: Pure logic testing (lowest priority)

**Agent Enforcement**:
- Contract Testing Agent handles API validation
- Integration Testing Agent manages real dependency testing
- Playwright E2E Agent orchestrates user journey testing
- TDD Compliance Agent coordinates hierarchy enforcement

### IV. Real Dependencies in Integration (NON-NEGOTIABLE)
**Principle**: Integration tests must use real dependencies, not mocks.

**Implementation Requirements**:
- TestContainers for database testing
- Real PostgreSQL and Redis instances
- Actual Stripe webhook testing
- Real OAuth2 provider integration

**Agent Enforcement**:
- Integration Testing Agent mandates TestContainers usage
- Security agents validate real authentication flows
- Payment agents test actual Stripe integration

### V. Observability Required (NON-NEGOTIABLE)
**Principle**: Structured logging and comprehensive observability.

**Implementation Requirements**:
- Structured logging with correlation IDs
- Multi-tier log streaming
- Metrics collection with Micrometer
- SLA/SLO monitoring and alerting

**Agent Enforcement**:
- Observability Agent validates logging patterns
- Performance agents monitor SLA compliance
- Security agents ensure audit trail completeness

### VI. Module Communication via Events (NON-NEGOTIABLE)
**Principle**: Inter-module communication must use events, not direct service calls.

**Implementation Requirements**:
- ApplicationEventPublisher for module communication
- Event-driven architecture patterns
- ArchUnit tests for boundary enforcement
- No direct cross-module service dependencies

**Agent Enforcement**:
- Spring Boot Modulith Architect validates boundaries
- Architecture agents enforce event patterns
- Code review agents detect boundary violations

### VII. Opaque Tokens Only (NON-NEGOTIABLE)
**Principle**: Authentication must use opaque tokens with SHA-256 + salt, no custom JWT.

**Implementation Requirements**:
- SHA-256 hashed tokens with secure salt
- Redis-backed session management
- OAuth2/PKCE implementation
- No JWT libraries or custom token implementations

**Agent Enforcement**:
- OAuth Security Agent validates token implementation
- Authentication Module Agent enforces opaque tokens
- Security agents reject JWT implementations

### VIII. GDPR Compliance (NON-NEGOTIABLE)
**Principle**: Full GDPR compliance with PII redaction and retention policies.

**Implementation Requirements**:
- Automatic PII redaction in logs
- Data retention policies implementation
- Right to erasure capabilities
- Audit trail for data processing

**Agent Enforcement**:
- GDPR Compliance Agent validates data handling
- Audit Module Agent ensures trail completeness
- Security agents verify PII protection

## Constitutional Enforcement Framework

### Agent Responsibility Matrix

| Constitutional Principle | Primary Enforcing Agent | Supporting Agents |
|---|---|---|
| Library-First Architecture | Spring Boot Modulith Architect | Code Review, Architecture |
| TDD Required | TDD Compliance Agent | All Development Agents |
| Test Order Hierarchy | Contract/Integration/E2E/Unit Agents | TDD Compliance Agent |
| Real Dependencies | Integration Testing Agent | Security, Payment Agents |
| Observability Required | Observability Agent | Performance, Audit Agents |
| Module Communication | Spring Boot Modulith Architect | Code Review Agent |
| Opaque Tokens Only | OAuth Security Agent | Authentication Module Agent |
| GDPR Compliance | GDPR Compliance Agent | Audit, Security Agents |

### Enforcement Mechanisms

#### Proactive Enforcement
- Agents must validate constitutional compliance before proceeding
- Constitutional Enforcement Agent coordinates compliance checks
- Task Coordination Agent ensures constitutional workflows

#### Reactive Enforcement
- Code Review Agent detects constitutional violations
- Constitutional Enforcement Agent blocks non-compliant implementations
- Audit agents log constitutional compliance events

#### Escalation Procedures
1. **Warning**: Agent warns about potential constitutional violation
2. **Block**: Agent prevents non-compliant code from proceeding
3. **Escalation**: Constitutional Enforcement Agent reviews and provides guidance
4. **Documentation**: Violation logged for future prevention

### Constitutional Compliance Gates

#### Phase Gates
- **Specification Phase**: Constitutional principles integrated into requirements
- **Design Phase**: Architecture compliance validated by Architect agents
- **Implementation Phase**: TDD compliance enforced by Testing agents
- **Validation Phase**: Full constitutional compliance verified

#### Continuous Compliance
- Every agent interaction includes constitutional validation
- Task Coordination Agent ensures constitutional workflows
- Constitutional Enforcement Agent provides ongoing oversight

## Context Integration for Agents

### Required Context Loading
All agents must load and reference this constitutional context:

```yaml
context_files:
  - ".claude/context/project-constitution.md"
  - ".claude/context/module-boundaries.md"
  - ".claude/context/testing-standards.md"
```

### Constitutional Principle References
Agents must explicitly reference constitutional principles in their guidance:

- **Architecture Agents**: Reference Library-First, Module Communication
- **Development Agents**: Reference TDD Required, specific domain principles
- **Testing Agents**: Reference Test Hierarchy, Real Dependencies
- **Security Agents**: Reference Opaque Tokens, GDPR Compliance
- **UI/UX Agents**: Reference Observability, User Experience standards

### Compliance Validation Patterns
Agents should use these patterns to validate constitutional compliance:

```python
def validate_constitutional_compliance(implementation):
    """Validate implementation against constitutional principles."""
    violations = []

    # Check TDD compliance
    if not has_tests_before_implementation(implementation):
        violations.append("TDD Required: Tests must be written before implementation")

    # Check library boundaries
    if has_direct_module_dependencies(implementation):
        violations.append("Module Communication: Use events, not direct calls")

    # Check observability
    if not has_structured_logging(implementation):
        violations.append("Observability Required: Must include structured logging")

    return violations
```

## Constitutional Amendment Process

### Amendment Requirements
- Constitutional changes require explicit documentation
- Amendment impact analysis on all affected agents
- Migration plan for existing implementations
- Updated agent context distribution

### Change Management
- Constitutional changes versioned with semantic versioning
- All agents updated with new constitutional context
- Compliance validation updated for new requirements
- Team training on constitutional changes

---

**Constitutional Version**: 2.1.1
**Last Updated**: 2024-09-14
**Next Review**: 2024-12-14

This constitutional context is the foundational document for all agent decision-making and enforcement actions. All agents must prioritize constitutional compliance over implementation convenience.