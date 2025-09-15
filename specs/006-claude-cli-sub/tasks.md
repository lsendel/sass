# Tasks: Claude CLI Sub-Agents Integration

**Input**: Design documents from `/specs/006-claude-cli-sub/`
**Prerequisites**: plan.md (required), spec.md (comprehensive agent specifications)
**Context**: Claude Code sub-agents documentation patterns from https://docs.anthropic.com/en/docs/claude-code/sub-agents

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → COMPLETE: Implementation plan loaded with 26 agents and .claude/ structure
   → Extract: YAML configs, Spring Boot Modulith, Playwright MCP integration
2. Load optional design documents:
   → spec.md: 26 specialized agents with detailed capabilities and context
   → Constitutional principles enforcement through agent ecosystem
   → Multi-agent workflow orchestration patterns
3. Generate tasks by category:
   → Setup: .claude/ directory, agent context management, Task tool integration
   → Tests: agent validation tests, constitutional compliance tests, Task tool workflow tests
   → Core: agent implementations following Claude Code sub-agent patterns
   → Integration: Task tool coordination, multi-agent workflows, constitutional enforcement
   → Polish: documentation, performance validation, team training materials
4. Apply task rules:
   → Different agents = mark [P] for parallel configuration
   → Same category = sequential dependency order for context sharing
   → Tests before implementation (TDD constitutional requirement)
   → Task tool integration for agent coordination
5. Number tasks sequentially (T001, T002...)
6. Generate dependency graph with critical path agents
7. Create parallel execution examples using Task tool
8. Validate task completeness:
   → All 26 agents have configuration tasks with Task tool integration?
   → All context templates implemented for agent orchestration?
   → All constitutional compliance workflows validated?
9. Return: SUCCESS (tasks ready for execution with Task tool coordination)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions
- **Task Tool Integration**: Use Task tool for agent coordination and multi-agent workflows

## Path Conventions
- **Project structure**: Single project with .claude/ overlay for sub-agent ecosystem
- **Agent configs**: `.claude/agents/[category]/[agent-name].md` with YAML frontmatter
- **Context files**: `.claude/context/[context-name].md` for shared agent knowledge
- **Workflows**: `.claude/workflows/[workflow-name].yml` for Task tool orchestration
- **Templates**: `.claude/templates/[template-name].md` for consistent agent patterns
- **Tests**: `tests/agent/` for agent validation and constitutional compliance tests

## Phase 3.1: Setup & Foundation
- [ ] T001 Create .claude/ directory structure per Claude Code sub-agent patterns
- [ ] T002 [P] Create agent category directories (.claude/agents/{architecture,development,testing,ui-ux,security,devops})
- [ ] T003 [P] Create context directory structure (.claude/context/) for shared agent knowledge
- [ ] T004 [P] Create workflows directory (.claude/workflows/) for Task tool orchestration
- [ ] T005 [P] Create templates directory (.claude/templates/) for agent configuration patterns

## Phase 3.2: Context Management System (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
- [ ] T006 [P] Agent validation test in tests/agent/test_agent_validation.py
- [ ] T007 [P] Agent configuration schema test in tests/agent/test_agent_config_schema.py
- [ ] T008 [P] Task tool coordination test in tests/agent/test_task_tool_coordination.py
- [ ] T009 [P] Constitutional compliance enforcement test in tests/agent/test_constitutional_compliance.py
- [ ] T010 [P] Multi-agent workflow orchestration test in tests/agent/test_multi_agent_workflows.py

## Phase 3.3: Core Context Templates (ONLY after tests are failing)
- [ ] T011 [P] Project constitution context in .claude/context/project-constitution.md
- [ ] T012 [P] Module boundaries context in .claude/context/module-boundaries.md
- [ ] T013 [P] Testing standards context in .claude/context/testing-standards.md
- [ ] T014 [P] UI/UX guidelines context in .claude/context/ui-ux-guidelines.md
- [ ] T015 [P] Security policies context in .claude/context/security-policies.md
- [ ] T016 [P] Agent coordination patterns in .claude/context/agent-coordination.md

## Phase 3.4: Critical Path Agents (Core Development)
- [ ] T017 TDD Compliance Agent in .claude/agents/testing/tdd-compliance-agent.md
- [ ] T018 Spring Boot Modulith Architect in .claude/agents/architecture/spring-boot-modulith-architect.md
- [ ] T019 Task Coordination Agent in .claude/agents/development/task-coordination-agent.md
- [ ] T020 Constitutional Enforcement Agent in .claude/agents/development/constitutional-enforcement-agent.md

## Phase 3.5: Testing Agents (Parallel Implementation with Task Tool Integration)
- [ ] T021 [P] Contract Testing Agent in .claude/agents/testing/contract-testing-agent.md
- [ ] T022 [P] Integration Testing Agent in .claude/agents/testing/integration-testing-agent.md
- [ ] T023 [P] Playwright E2E Agent in .claude/agents/testing/playwright-e2e-agent.md
- [ ] T024 [P] Security Testing Agent in .claude/agents/testing/security-testing-agent.md

## Phase 3.6: Architecture & System Agents
- [ ] T025 [P] System Architect Agent in .claude/agents/architecture/system-architect.md
- [ ] T026 [P] Performance Architect Agent in .claude/agents/architecture/performance-architect.md
- [ ] T027 [P] Code Review Agent in .claude/agents/development/code-review-agent.md
- [ ] T028 [P] Refactoring Agent in .claude/agents/development/refactoring-agent.md

## Phase 3.7: Module-Specific Agents
- [ ] T029 [P] Authentication Module Agent in .claude/agents/development/auth-module-agent.md
- [ ] T030 [P] Payment Processing Agent in .claude/agents/development/payment-processing-agent.md
- [ ] T031 [P] Subscription Management Agent in .claude/agents/development/subscription-management-agent.md
- [ ] T032 [P] User Management Agent in .claude/agents/development/user-management-agent.md
- [ ] T033 [P] Audit Module Agent in .claude/agents/development/audit-module-agent.md

## Phase 3.8: UI/UX & Frontend Agents
- [ ] T034 [P] React Optimization Agent in .claude/agents/ui-ux/react-optimization-agent.md
- [ ] T035 [P] Accessibility Champion Agent in .claude/agents/ui-ux/accessibility-champion-agent.md
- [ ] T036 [P] UX Research Agent in .claude/agents/ui-ux/ux-research-agent.md
- [ ] T037 [P] Visual Design Agent in .claude/agents/ui-ux/visual-design-agent.md

## Phase 3.9: Security & Compliance Agents
- [ ] T038 [P] Payment Security Agent in .claude/agents/security/payment-security-agent.md
- [ ] T039 [P] GDPR Compliance Agent in .claude/agents/security/gdpr-compliance-agent.md
- [ ] T040 [P] OAuth Security Agent in .claude/agents/security/oauth-security-agent.md

## Phase 3.10: DevOps & Infrastructure Agents
- [ ] T041 [P] CI/CD Optimization Agent in .claude/agents/devops/cicd-optimization-agent.md
- [ ] T042 [P] Database Performance Agent in .claude/agents/devops/database-performance-agent.md
- [ ] T043 [P] Observability Agent in .claude/agents/devops/observability-agent.md
- [ ] T044 [P] Container Optimization Agent in .claude/agents/devops/container-optimization-agent.md

## Phase 3.11: Task Tool Workflow Orchestration
- [ ] T045 Feature development workflow in .claude/workflows/feature-development.yml
- [ ] T046 Code review workflow in .claude/workflows/code-review-process.yml
- [ ] T047 Testing workflow in .claude/workflows/testing-workflow.yml
- [ ] T048 Multi-agent coordination workflow in .claude/workflows/multi-agent-coordination.yml

## Phase 3.12: Agent Configuration Templates & Documentation
- [ ] T049 [P] Agent configuration template in .claude/templates/agent-template.md
- [ ] T050 [P] Context template in .claude/templates/context-template.md
- [ ] T051 [P] Workflow template in .claude/templates/workflow-template.yml
- [ ] T052 [P] Task tool integration guide in .claude/docs/task-tool-integration.md

## Phase 3.13: Integration & Validation
- [ ] T053 Agent configuration validation script with Task tool integration
- [ ] T054 Context synchronization mechanism for multi-agent workflows
- [ ] T055 Playwright MCP integration testing with UI/UX agents
- [ ] T056 Constitutional compliance validation across all agents
- [ ] T057 Task tool coordination testing for multi-agent workflows

## Phase 3.14: Performance & Optimization
- [ ] T058 [P] Agent response time benchmarking (target: < 10s)
- [ ] T059 [P] Context loading performance optimization (target: < 2s)
- [ ] T060 [P] Memory usage optimization for agent contexts
- [ ] T061 [P] Task tool workflow performance validation

## Phase 3.15: Documentation & Training
- [ ] T062 [P] Update CLAUDE.md with agent integration patterns and Task tool usage
- [ ] T063 [P] Create team training documentation for sub-agent workflows
- [ ] T064 [P] Generate agent troubleshooting guide with Task tool debugging
- [ ] T065 [P] Create constitutional compliance checklist for agent development
- [ ] T066 [P] Document multi-agent coordination best practices

## Dependencies
**Critical Path**:
- Context system (T006-T016) before any agent implementations
- TDD Compliance Agent (T017) before all development agents
- Constitutional Enforcement Agent (T020) before specialized agents
- Task Coordination Agent (T019) before multi-agent workflows
- Spring Boot Modulith Architect (T018) before module-specific agents

**Parallel Execution**:
- All agents within same category can be implemented in parallel (marked [P])
- Context templates are independent and can be created in parallel
- Documentation tasks can run in parallel with implementation
- Task tool workflows can be developed concurrently with agent configurations

**Validation Gates**:
- Tests (T006-T010) must fail before implementation begins
- Constitutional compliance validation (T056) before production readiness
- Performance benchmarks (T058-T061) before team rollout
- Task tool integration validation (T057) before multi-agent workflows

## Parallel Example (Using Task Tool)
```bash
# Phase 3.5: Testing Agents (can run simultaneously with Task tool coordination)
Task: "Contract Testing Agent in .claude/agents/testing/contract-testing-agent.md"
Task: "Integration Testing Agent in .claude/agents/testing/integration-testing-agent.md"
Task: "Playwright E2E Agent in .claude/agents/testing/playwright-e2e-agent.md"
Task: "Security Testing Agent in .claude/agents/testing/security-testing-agent.md"

# Phase 3.8: UI/UX Agents (can run simultaneously)
Task: "React Optimization Agent in .claude/agents/ui-ux/react-optimization-agent.md"
Task: "Accessibility Champion Agent in .claude/agents/ui-ux/accessibility-champion-agent.md"
Task: "UX Research Agent in .claude/agents/ui-ux/ux-research-agent.md"
Task: "Visual Design Agent in .claude/agents/ui-ux/visual-design-agent.md"

# Phase 3.11: Multi-Agent Workflow Coordination
Task: "Feature development workflow in .claude/workflows/feature-development.yml"
Task: "Multi-agent coordination workflow in .claude/workflows/multi-agent-coordination.yml"
```

## Constitutional Compliance Matrix
| Agent Category | Constitutional Enforcement | Task Tool Integration |
|---|---|---|
| TDD Compliance Agent | Enforces test-first development | Coordinates testing workflows |
| Spring Boot Modulith Architect | Validates module boundaries | Orchestrates architecture reviews |
| Authentication Module Agent | Enforces security principles | Coordinates auth implementation |
| Payment Processing Agent | Ensures PCI compliance | Manages payment workflow coordination |
| UI/UX Agents | Enforces accessibility standards | Coordinates design system compliance |
| Security Agents | Validates GDPR/security requirements | Orchestrates security validation workflows |

## Notes
- [P] tasks = different files, no dependencies, can run in parallel
- All agents must enforce constitutional principles (TDD, module boundaries, security)
- Agent configurations use YAML frontmatter in Markdown format following Claude Code patterns
- Context files must be loaded automatically by agents for knowledge sharing
- Task tool integration required for multi-agent workflow coordination
- Playwright MCP integration required for UI/UX agents
- Constitutional compliance validation is non-negotiable
- Commit after each major phase completion
- Test agent functionality before moving to next phase

## Task Generation Rules
*Applied during main() execution*

1. **From Agent Categories**:
   - Each agent category → directory setup task [P]
   - Each agent → individual configuration task [P within category]
<<<<<<< HEAD
=======
   - Task tool integration for coordination
>>>>>>> 006-claude-cli-sub

2. **From Context Requirements**:
   - Each context type → template creation task [P]
   - Constitutional principles → validation tasks
   - Agent coordination patterns → workflow tasks

3. **From Claude Code Sub-Agent Patterns**:
   - YAML frontmatter configuration for all agents
   - Context file integration for knowledge sharing
   - Task tool orchestration for multi-agent workflows
   - Constitutional compliance enforcement

4. **Ordering**:
   - Setup → Context → Tests → Core Agents → Specialized Agents → Workflows → Validation
   - Dependencies block parallel execution
   - Critical path agents before dependent agents
   - Task tool coordination after individual agent implementation

## Validation Checklist
*GATE: Checked before task execution*

- [x] All 26+ agents have corresponding configuration tasks
- [x] All context templates have creation tasks
- [x] All workflow orchestrations have implementation tasks
- [x] Tests come before implementation (TDD compliance)
- [x] Parallel tasks truly independent (different files)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] Constitutional compliance validation included
- [x] Performance benchmarking included
- [x] Documentation and training tasks included
- [x] Task tool integration for multi-agent coordination
- [x] Claude Code sub-agent patterns followed

## Success Criteria
- [ ] All 66 tasks completed successfully
- [ ] Agent functionality validated through tests
- [ ] Constitutional compliance enforced by all agents
- [ ] Task tool coordination working for multi-agent workflows
- [ ] Playwright MCP integration working for UI/UX agents
- [ ] Performance targets met (response time < 10s, context loading < 2s)
- [ ] Team trained and productive with agent-assisted workflows
- [ ] Documentation complete and accessible
- [ ] Multi-agent workflows coordinating effectively through Task tool
- [ ] Claude Code sub-agent patterns properly implemented
