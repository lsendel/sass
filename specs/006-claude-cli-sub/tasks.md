# Tasks: Claude CLI Sub-Agents Integration

**Input**: Design documents from `/specs/006-claude-cli-sub/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → COMPLETE: Implementation plan loaded with 26 agents and .claude/ structure
   → Extract: YAML configs, Spring Boot Modulith, Playwright MCP integration
2. Load optional design documents:
   → data-model.md: Agent entities and context structures
   → contracts/: Agent config schemas and API validation
   → research.md: Claude Code patterns and MCP integration decisions
3. Generate tasks by category:
   → Setup: .claude/ directory, context templates, configuration schemas
   → Tests: agent validation tests, workflow coordination tests
   → Core: agent implementations, context management, orchestration
   → Integration: Playwright MCP, constitutional compliance validation
   → Polish: documentation, performance validation, team training
4. Apply task rules:
   → Different agents = mark [P] for parallel configuration
   → Same category = sequential dependency order
   → Tests before implementation (TDD constitutional requirement)
5. Number tasks sequentially (T001, T002...)
6. Generate dependency graph
7. Create parallel execution examples
8. Validate task completeness:
   → All 26 agents have configuration tasks?
   → All context templates implemented?
   → All workflows validated?
9. Return: SUCCESS (tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
- **Project structure**: Single project with .claude/ overlay
- **Agent configs**: `.claude/agents/[category]/[agent-name].md`
- **Context files**: `.claude/context/[context-name].md`
- **Workflows**: `.claude/workflows/[workflow-name].yml`
- **Templates**: `.claude/templates/[template-name].md`
- **Tests**: `tests/agent/` for agent validation tests

## Phase 3.1: Setup & Foundation
- [ ] T001 Create .claude/ directory structure per implementation plan
- [ ] T002 [P] Create agent category directories (.claude/agents/{architecture,development,testing,ui-ux,security,devops})
- [ ] T003 [P] Create context directory structure (.claude/context/)
- [ ] T004 [P] Create workflows directory structure (.claude/workflows/)
- [ ] T005 [P] Create templates directory structure (.claude/templates/)

## Phase 3.2: Context Management System (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
- [ ] T006 [P] Context validation test in tests/agent/test_context_validation.py
- [ ] T007 [P] Agent configuration schema test in tests/agent/test_agent_config_schema.py
- [ ] T008 [P] Context synchronization test in tests/agent/test_context_sync.py
- [ ] T009 [P] Constitutional compliance validation test in tests/agent/test_constitutional_compliance.py
- [ ] T010 [P] Workflow orchestration test in tests/agent/test_workflow_orchestration.py

## Phase 3.3: Core Context Templates (ONLY after tests are failing)
- [ ] T011 [P] Project constitution context in .claude/context/project-constitution.md
- [ ] T012 [P] Module boundaries context in .claude/context/module-boundaries.md
- [ ] T013 [P] Testing standards context in .claude/context/testing-standards.md
- [ ] T014 [P] UI/UX guidelines context in .claude/context/ui-ux-guidelines.md
- [ ] T015 [P] Security policies context in .claude/context/security-policies.md
- [ ] T016 [P] DevOps standards context in .claude/context/devops-standards.md

## Phase 3.4: Critical Path Agents (Core Development)
- [ ] T017 TDD Compliance Agent in .claude/agents/testing/tdd-compliance-agent.md
- [ ] T018 Spring Boot Modulith Architect in .claude/agents/architecture/spring-boot-modulith-architect.md
- [ ] T019 Authentication Module Agent in .claude/agents/development/auth-module-agent.md
- [ ] T020 Payment Processing Agent in .claude/agents/development/payment-processing-agent.md

## Phase 3.5: Testing Agents (Parallel Implementation)
- [ ] T021 [P] Contract Testing Agent in .claude/agents/testing/contract-testing-agent.md
- [ ] T022 [P] Integration Testing Agent in .claude/agents/testing/integration-testing-agent.md
- [ ] T023 [P] Playwright E2E Agent in .claude/agents/testing/playwright-e2e-agent.md
- [ ] T024 [P] Security Testing Agent in .claude/agents/testing/security-testing-agent.md

## Phase 3.6: Architecture & System Agents
- [ ] T025 [P] System Architect in .claude/agents/architecture/system-architect.md
- [ ] T026 [P] Performance Architect in .claude/agents/architecture/performance-architect.md
- [ ] T027 [P] Code Review Agent in .claude/agents/development/code-review-agent.md
- [ ] T028 [P] Refactoring Agent in .claude/agents/development/refactoring-agent.md

## Phase 3.7: Module-Specific Agents
- [ ] T029 [P] Subscription Management Agent in .claude/agents/development/subscription-management-agent.md
- [ ] T030 [P] User Management Agent in .claude/agents/development/user-management-agent.md
- [ ] T031 [P] Audit Module Agent in .claude/agents/development/audit-module-agent.md

## Phase 3.8: UI/UX & Frontend Agents
- [ ] T032 [P] React Optimization Agent in .claude/agents/ui-ux/react-optimization-agent.md
- [ ] T033 [P] Accessibility Champion Agent in .claude/agents/ui-ux/accessibility-champion-agent.md
- [ ] T034 [P] UX Research Agent in .claude/agents/ui-ux/ux-research-agent.md
- [ ] T035 [P] Visual Design Agent in .claude/agents/ui-ux/visual-design-agent.md

## Phase 3.9: Security & Compliance Agents
- [ ] T036 [P] Payment Security Agent in .claude/agents/security/payment-security-agent.md
- [ ] T037 [P] GDPR Compliance Agent in .claude/agents/security/gdpr-compliance-agent.md
- [ ] T038 [P] OAuth Security Agent in .claude/agents/security/oauth-security-agent.md

## Phase 3.10: DevOps & Infrastructure Agents
- [ ] T039 [P] CI/CD Optimization Agent in .claude/agents/devops/cicd-optimization-agent.md
- [ ] T040 [P] Database Performance Agent in .claude/agents/devops/database-performance-agent.md
- [ ] T041 [P] Observability Agent in .claude/agents/devops/observability-agent.md
- [ ] T042 [P] Container Optimization Agent in .claude/agents/devops/container-optimization-agent.md

## Phase 3.11: Workflow Orchestration
- [ ] T043 Feature development workflow in .claude/workflows/feature-development.yml
- [ ] T044 Code review workflow in .claude/workflows/code-review-process.yml
- [ ] T045 Testing workflow in .claude/workflows/testing-workflow.yml
- [ ] T046 Security validation workflow in .claude/workflows/security-validation.yml

## Phase 3.12: Configuration Templates & Documentation
- [ ] T047 [P] Agent configuration template in .claude/templates/agent-template.md
- [ ] T048 [P] Context template in .claude/templates/context-template.md
- [ ] T049 [P] Workflow template in .claude/templates/workflow-template.yml
- [ ] T050 [P] Agent usage documentation in .claude/docs/agent-usage-guide.md

## Phase 3.13: Integration & Validation
- [ ] T051 Agent configuration validation script
- [ ] T052 Context synchronization mechanism
- [ ] T053 Playwright MCP integration testing
- [ ] T054 Constitutional compliance validation
- [ ] T055 Multi-agent workflow coordination testing

## Phase 3.14: Performance & Optimization
- [ ] T056 [P] Agent response time benchmarking
- [ ] T057 [P] Context loading performance optimization
- [ ] T058 [P] Memory usage optimization for agent contexts
- [ ] T059 [P] Concurrent agent execution validation

## Phase 3.15: Documentation & Training
- [ ] T060 [P] Update CLAUDE.md with agent integration patterns
- [ ] T061 [P] Create team training documentation
- [ ] T062 [P] Generate agent troubleshooting guide
- [ ] T063 [P] Create constitutional compliance checklist
- [ ] T064 [P] Document agent best practices

## Dependencies
**Critical Path**:
- Context system (T006-T016) before any agent implementations
- TDD Compliance Agent (T017) before all development agents
- Spring Boot Modulith Architect (T018) before module-specific agents
- Core agents (T017-T020) before specialized agents (T021+)

**Parallel Execution**:
- All agents within same category can be implemented in parallel (marked [P])
- Context templates are independent and can be created in parallel
- Documentation tasks can run in parallel with implementation

**Validation Gates**:
- Tests (T006-T010) must fail before implementation begins
- Constitutional compliance validation (T054) before production readiness
- Performance benchmarks (T056-T059) before team rollout

## Parallel Example
```bash
# Phase 3.5: Testing Agents (can run simultaneously)
Task: "Contract Testing Agent in .claude/agents/testing/contract-testing-agent.md"
Task: "Integration Testing Agent in .claude/agents/testing/integration-testing-agent.md"
Task: "Playwright E2E Agent in .claude/agents/testing/playwright-e2e-agent.md"
Task: "Security Testing Agent in .claude/agents/testing/security-testing-agent.md"

# Phase 3.8: UI/UX Agents (can run simultaneously)
Task: "React Optimization Agent in .claude/agents/ui-ux/react-optimization-agent.md"
Task: "Accessibility Champion Agent in .claude/agents/ui-ux/accessibility-champion-agent.md"
Task: "UX Research Agent in .claude/agents/ui-ux/ux-research-agent.md"
Task: "Visual Design Agent in .claude/agents/ui-ux/visual-design-agent.md"
```

## Notes
- [P] tasks = different files, no dependencies, can run in parallel
- All agents must enforce constitutional principles (TDD, module boundaries, security)
- Agent configurations use YAML frontmatter in Markdown format
- Context files must be loaded automatically by agents
- Playwright MCP integration required for UI/UX agents
- Constitutional compliance validation is non-negotiable
- Commit after each major phase completion
- Test agent functionality before moving to next phase

## Task Generation Rules
*Applied during main() execution*

1. **From Agent Categories**:
   - Each agent category → directory setup task [P]
   - Each agent → individual configuration task [P within category]

2. **From Context Requirements**:
   - Each context type → template creation task [P]
   - Constitutional principles → validation tasks

3. **From Workflow Definitions**:
   - Each workflow → YAML configuration task
   - Multi-agent coordination → integration tasks

4. **Ordering**:
   - Setup → Context → Tests → Core Agents → Specialized Agents → Workflows → Validation
   - Dependencies block parallel execution
   - Critical path agents before dependent agents

## Validation Checklist
*GATE: Checked before task execution*

- [x] All 26 agents have corresponding configuration tasks
- [x] All context templates have creation tasks
- [x] All workflow orchestrations have implementation tasks
- [x] Tests come before implementation (TDD compliance)
- [x] Parallel tasks truly independent (different files)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] Constitutional compliance validation included
- [x] Performance benchmarking included
- [x] Documentation and training tasks included

## Success Criteria
- [ ] All 64 tasks completed successfully
- [ ] Agent functionality validated through tests
- [ ] Constitutional compliance enforced by all agents
- [ ] Playwright MCP integration working for UI/UX agents
- [ ] Performance targets met (response time < 10s, context loading < 2s)
- [ ] Team trained and productive with agent-assisted workflows
- [ ] Documentation complete and accessible
- [ ] Multi-agent workflows coordinating effectively