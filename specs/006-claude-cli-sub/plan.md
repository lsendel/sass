# Implementation Plan: Claude CLI Sub-Agents Integration

<!-- VARIANT:sh - Run `/scripts/bash/update-agent-context.sh claude-code` for your AI assistant -->
<!-- VARIANT:ps - Run `/scripts/powershell/update-agent-context.ps1 -AgentType claude-code` for your AI assistant -->

**Branch**: `006-claude-cli-sub` | **Date**: 2024-09-14 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-claude-cli-sub/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → COMPLETE: Spec loaded with 26 specialized agents defined
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type: single (Spring Boot Modulith architecture)
   → Set Structure Decision: DEFAULT (single project with .claude/ additions)
3. Evaluate Constitution Check section below
   → PASS: All constitutional principles maintained by agents
   → Update Progress Tracking: Initial Constitution Check ✅
4. Execute Phase 0 → research.md
   → COMPLETE: Agent framework analysis and integration patterns researched
5. Execute Phase 1 → contracts, data-model.md, quickstart.md, CLAUDE.md
   → Ready to execute: Agent configurations, context templates, workflow definitions
6. Re-evaluate Constitution Check section
   → Expected PASS: Agents enforce constitutional principles
   → Update Progress Tracking: Post-Design Constitution Check (pending)
7. Plan Phase 2 → Task generation approach for agent implementation
8. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Primary requirement: Integrate 26 specialized Claude Code sub-agents into the Spring Boot Modulith payment platform development workflow, providing domain-specific AI assistance for development, testing, architecture, and UI/UX optimization while maintaining strict constitutional compliance (TDD, module boundaries, security, observability).

Technical approach: Create structured .claude/ directory with agent configurations in Markdown format with YAML frontmatter, implement context management system for project-specific knowledge, establish agent orchestration workflows, and integrate with existing .specify framework and Playwright MCP for comprehensive testing capabilities.

## Technical Context
**Language/Version**: Java 21 (Spring Boot 3.2+), TypeScript 5.0+, YAML configuration
**Primary Dependencies**: Claude Code CLI, Spring Boot Modulith 1.1+, Playwright MCP, axe-core
**Storage**: File-based agent configurations, context files in .claude/ directory
**Testing**: Agent functionality validation, constitutional compliance testing, workflow integration tests
**Target Platform**: Development environment integration (macOS/Linux/Windows)
**Project Type**: single - Spring Boot Modulith with agent overlay
**Performance Goals**: Agent response time < 10s, context loading < 2s, workflow completion < 30s
**Constraints**: Constitutional compliance enforcement, security principle validation, TDD hierarchy maintenance
**Scale/Scope**: 26 specialized agents, 6 agent categories, multi-agent workflow coordination

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Simplicity**:
- Projects: 1 (adding .claude/ overlay to existing Spring Boot Modulith project)
- Using framework directly? YES (Claude Code sub-agent framework, no wrappers)
- Single data model? YES (agent configurations as data, no DTOs needed)
- Avoiding patterns? YES (direct YAML configuration, no complex abstraction layers)

**Architecture**:
- EVERY feature as library? YES (each agent is self-contained library with specific expertise)
- Libraries listed:
  - TDD Compliance Agent (enforces constitutional testing principles)
  - Spring Boot Modulith Architect (module boundary validation)
  - Authentication Module Agent (OAuth2/PKCE expertise)
  - Payment Processing Agent (Stripe integration expertise)
  - Playwright E2E Agent (UI/UX testing with MCP)
  - Security Compliance Agent (PCI DSS, GDPR validation)
  - [20 additional specialized agents per spec.md]
- CLI per library: Each agent accessible via Claude Code CLI with context-aware invocation
- Library docs: Agent documentation in Markdown format with usage examples

**Testing (NON-NEGOTIABLE)**:
- RED-GREEN-Refactor cycle enforced? YES (TDD Compliance Agent specifically enforces this)
- Git commits show tests before implementation? YES (agents validate test-first approach)
- Order: Contract→Integration→E2E→Unit strictly followed? YES (testing agents enforce hierarchy)
- Real dependencies used? YES (agents mandate TestContainers for integration tests)
- Integration tests for: YES (agent functionality, multi-agent workflows, constitutional compliance)
- FORBIDDEN: Implementation before test, skipping RED phase (agents reject non-TDD approaches)

**Observability**:
- Structured logging included? YES (agents monitor their own performance and usage)
- Frontend logs → backend? N/A (agent system is development-time only)
- Error context sufficient? YES (agent error reporting and debugging capabilities)

**Versioning**:
- Version number assigned? YES (1.0.0 for initial agent ecosystem)
- BUILD increments on every change? YES (agent configuration versioning)
- Breaking changes handled? YES (agent backward compatibility and migration paths)

## Project Structure

### Documentation (this feature)
```
specs/006-claude-cli-sub/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
# Option 1: Single project (DEFAULT) - Extended with .claude/ overlay
.claude/
├── agents/              # Agent configurations
│   ├── architecture/
│   ├── development/
│   ├── testing/
│   ├── ui-ux/
│   ├── security/
│   └── devops/
├── context/             # Project-specific context
├── workflows/           # Agent orchestration
└── templates/           # Configuration templates

src/                     # Existing Spring Boot Modulith structure
├── models/
├── services/
├── cli/
└── lib/

tests/                   # Existing test structure
├── contract/
├── integration/
└── unit/
```

**Structure Decision**: DEFAULT (Option 1) - Single project with .claude/ directory overlay for agent ecosystem

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - Claude Code sub-agent configuration patterns ✅
   - Agent context management and synchronization ✅
   - Multi-agent workflow orchestration ✅
   - Playwright MCP integration capabilities ✅

2. **Generate and dispatch research agents** ✅:
   ```
   Task: "Research Claude Code sub-agent framework architecture and configuration patterns"
   Task: "Analyze existing agent repositories for implementation patterns and best practices"
   Task: "Investigate Playwright MCP integration capabilities for comprehensive UI/UX testing"
   Task: "Evaluate multi-agent workflow coordination and context sharing mechanisms"
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]

**Output**: research.md with all technical unknowns resolved

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Agent Configuration (name, model, triggers, tools, context_files)
   - Context Document (constitutional_principles, module_boundaries, testing_standards)
   - Workflow Definition (steps, agents, validation_gates)
   - Agent Category (architecture, development, testing, ui-ux, security, devops)

2. **Generate API contracts** from functional requirements:
   - Agent Configuration Schema (YAML frontmatter validation)
   - Context Management API (context synchronization, validation)
   - Workflow Orchestration API (multi-agent coordination)
   - Output to `/contracts/agent-config-schema.yml`, `/contracts/context-api.yml`

3. **Generate contract tests** from contracts:
   - Agent configuration validation tests
   - Context loading and synchronization tests
   - Workflow coordination tests
   - Tests must fail (no agent implementations yet)

4. **Extract test scenarios** from user stories:
   - Developer requests TDD guidance → TDD Compliance Agent validates and guides
   - Architecture review needed → Spring Boot Modulith Architect analyzes and advises
   - E2E testing required → Playwright Agent with MCP generates comprehensive test suite
   - Quickstart test = successful agent invocation and constitutional compliance validation

5. **Update CLAUDE.md incrementally** (O(1) operation):
   - Add agent configuration patterns to existing project context
   - Include constitutional enforcement mechanisms
   - Update recent changes with agent integration approach
   - Keep under 150 lines for token efficiency
   - Output to repository root

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, CLAUDE.md updated

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (agent configs, context templates, workflows)
- Each agent category → configuration creation tasks [P]
- Each agent → individual agent implementation task [P]
- Each context template → context file creation task [P]
- Workflow orchestration → multi-agent coordination tasks
- Constitutional compliance validation tasks

**Ordering Strategy**:
- TDD order: Agent validation tests before agent implementations
- Dependency order: Context management before agents before workflows
- Mark [P] for parallel execution (independent agent configurations)
- Critical path: TDD Compliance Agent → Spring Boot Modulith Architect → module-specific agents

**Estimated Output**: 35-40 numbered, ordered tasks in tasks.md covering:
1. Context management system (5 tasks)
2. Core agent implementations (8 tasks)
3. Specialized agent implementations (18 tasks)
4. Workflow orchestration (4 tasks)
5. Integration and validation (5-8 tasks)

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation (execute tasks.md following constitutional principles)
**Phase 5**: Validation (agent functionality tests, constitutional compliance validation, workflow integration testing)

## Complexity Tracking
*No constitutional violations - all principles maintained through agent enforcement*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [ ] Phase 1: Design complete (/plan command)
- [ ] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [ ] Post-Design Constitution Check: PASS (pending Phase 1 completion)
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented (none)

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*