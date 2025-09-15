# Agent Coordination Context

This context file defines coordination patterns, communication protocols, and shared knowledge for multi-agent workflows in the Spring Boot Modulith payment platform.

## Constitutional Coordination Principles

### Supreme Authority Hierarchy
1. **Constitutional Enforcement Agent** - Supreme authority over all constitutional matters
2. **Task Coordination Agent** - Orchestrates multi-agent workflows
3. **Module-Specific Agents** - Domain expertise within constitutional boundaries
4. **Testing Agents** - Enforce testing hierarchy and quality gates
5. **DevOps Agents** - Infrastructure and deployment coordination

### Coordination Protocol Stack

#### Layer 1: Constitutional Validation
```yaml
authority: "Constitutional Enforcement Agent"
scope: "All agent interactions"
requirements:
  - "Constitutional compliance verification before any action"
  - "Immediate halt on constitutional violation"
  - "Cross-agent constitutional validation"
```

#### Layer 2: Task Orchestration
```yaml
authority: "Task Coordination Agent"
scope: "Multi-agent workflow management"
capabilities:
  - "Agent selection and sequencing"
  - "Parallel vs sequential execution"
  - "Error handling and rollback coordination"
  - "Progress tracking and reporting"
```

#### Layer 3: Domain Coordination
```yaml
authority: "Domain-Specific Agents"
scope: "Module and specialty area coordination"
patterns:
  - "Event-driven communication between modules"
  - "Shared context and knowledge management"
  - "Specialized expertise coordination"
```

## Multi-Agent Communication Patterns

### 1. Command-Response Pattern
**Use Case**: Direct agent-to-agent communication for specific tasks
```yaml
pattern: "command_response"
protocol:
  - requesting_agent: "Sends command with parameters"
  - receiving_agent: "Executes command and returns result"
  - validation: "Constitutional compliance checked at each step"
example:
  command: "Task: Security Testing Agent - validate OWASP compliance"
  response: "OWASP validation completed - 0 critical vulnerabilities found"
```

### 2. Event-Driven Coordination
**Use Case**: Asynchronous coordination between loosely coupled agents
```yaml
pattern: "event_driven"
protocol:
  - event_publisher: "Publishes domain events"
  - event_subscribers: "React to relevant events"
  - event_store: "Maintains event history for audit"
example:
  event: "PaymentProcessingCompleted"
  subscribers: ["Audit Module Agent", "Security Testing Agent"]
```

### 3. Orchestrated Workflow
**Use Case**: Complex multi-phase workflows requiring central coordination
```yaml
pattern: "orchestrated_workflow"
coordinator: "Task Coordination Agent"
protocol:
  - workflow_definition: "YAML-based workflow specification"
  - phase_execution: "Sequential or parallel phase execution"
  - quality_gates: "Constitutional and quality validations"
  - rollback_capability: "Automatic rollback on failure"
```

### 4. Peer-to-Peer Collaboration
**Use Case**: Agents working together on complementary tasks
```yaml
pattern: "peer_collaboration"
protocol:
  - shared_context: "Common context files and knowledge base"
  - mutual_validation: "Cross-agent validation and review"
  - consensus_building: "Agreement on approach and execution"
example:
  collaboration: "Frontend agents working together on UI implementation"
  agents: ["React Frontend Agent", "TypeScript Development Agent", "Redux State Agent"]
```

## Agent Context Sharing

### Shared Context Repository
```yaml
location: ".claude/context/"
files:
  - "project-constitution.md": "Supreme constitutional principles"
  - "module-boundaries.md": "Module communication patterns"
  - "testing-standards.md": "TDD and testing hierarchy requirements"
  - "security-guidelines.md": "Security requirements and standards"
  - "ui-ux-guidelines.md": "Frontend development standards"
  - "agent-coordination.md": "This file - coordination patterns"
```

### Context Access Patterns
```yaml
read_access:
  - "All agents can read constitutional context"
  - "Domain agents access relevant context files"
  - "Constitutional agent has access to all context"

write_access:
  - "Constitutional Enforcement Agent: All context files"
  - "Task Coordination Agent: Workflow and coordination context"
  - "Domain agents: Domain-specific context updates"

validation:
  - "All context changes validated by Constitutional Enforcement Agent"
  - "Version control and audit trail for context changes"
```

### Knowledge Sharing Protocol
```yaml
shared_knowledge:
  domain_models:
    - "Consistent domain entity definitions"
    - "Branded type system understanding"
    - "Event schema specifications"

  implementation_patterns:
    - "Constitutional compliance patterns"
    - "Testing hierarchy implementation"
    - "Module communication patterns"
    - "Security implementation standards"

  quality_standards:
    - "Code quality metrics and thresholds"
    - "Performance requirements"
    - "Security compliance standards"
    - "Accessibility requirements"
```

## Workflow Coordination Mechanisms

### 1. Sequential Coordination
```yaml
description: "Agents execute one after another in defined order"
use_cases:
  - "TDD workflow: Contract → Integration → E2E → Unit"
  - "Security validation: OWASP → PCI DSS → Constitutional"
  - "Deployment: Build → Test → Security → Deploy"

coordination_protocol:
  - "Previous agent must complete successfully"
  - "Output artifacts passed to next agent"
  - "Constitutional validation at each step"
  - "Automatic rollback on failure"
```

### 2. Parallel Coordination
```yaml
description: "Multiple agents execute simultaneously"
use_cases:
  - "Module implementation across different domains"
  - "Security scanning with multiple tools"
  - "Performance testing with different scenarios"

coordination_protocol:
  - "Agents execute independently"
  - "Shared context for coordination"
  - "Aggregated results and validation"
  - "Partial rollback for failed agents"
```

### 3. Orchestrated Coordination
```yaml
description: "Central coordinator manages complex workflows"
coordinator: "Task Coordination Agent"
use_cases:
  - "Feature development workflow"
  - "Complex deployment pipelines"
  - "Cross-module refactoring"

coordination_protocol:
  - "Workflow defined in YAML"
  - "Phase-based execution with dependencies"
  - "Quality gates and validations"
  - "Centralized error handling and rollback"
```

## Error Handling and Rollback

### Constitutional Violation Handling
```yaml
trigger: "Constitutional principle violation detected"
authority: "Constitutional Enforcement Agent"
actions:
  - "Immediate workflow halt"
  - "Violation analysis and documentation"
  - "Rollback to constitutional compliant state"
  - "Re-education and guidance provision"
escalation: "Supreme authority - no override possible"
```

### Security Failure Handling
```yaml
trigger: "Security vulnerability or compliance failure"
authority: "Security Testing Agent + Constitutional Enforcement Agent"
actions:
  - "Security assessment and impact analysis"
  - "Immediate remediation requirements"
  - "Compliance validation before continuation"
  - "Security audit trail update"
escalation: "Platform security team notification"
```

### Quality Gate Failure
```yaml
trigger: "Quality standards not met"
authority: "Relevant testing agent + Task Coordination Agent"
actions:
  - "Quality assessment and gap analysis"
  - "Remediation plan generation"
  - "Re-execution with improvements"
  - "Quality metrics update"
escalation: "Development team review required"
```

## Performance and Scalability

### Agent Performance Monitoring
```yaml
metrics:
  - "Agent execution time"
  - "Resource utilization"
  - "Coordination overhead"
  - "Success/failure rates"

optimization:
  - "Parallel execution where possible"
  - "Efficient context sharing"
  - "Minimal coordination overhead"
  - "Smart caching of results"
```

### Scalability Patterns
```yaml
horizontal_scaling:
  - "Multiple agent instances for parallel work"
  - "Load balancing across agent instances"
  - "Stateless agent design"

vertical_scaling:
  - "Agent specialization and optimization"
  - "Efficient resource utilization"
  - "Context sharing optimization"
```

## Agent Lifecycle Management

### Agent Registration
```yaml
registration_process:
  - "Agent metadata registration"
  - "Capability declaration"
  - "Context file associations"
  - "Constitutional compliance validation"
```

### Agent Discovery
```yaml
discovery_mechanism:
  - "Capability-based agent selection"
  - "Context-aware routing"
  - "Load balancing consideration"
  - "Constitutional authority ranking"
```

### Agent Health Monitoring
```yaml
health_checks:
  - "Agent availability monitoring"
  - "Performance metrics tracking"
  - "Constitutional compliance status"
  - "Context synchronization status"
```

## Best Practices for Agent Coordination

### 1. Constitutional First
- Always validate constitutional compliance before agent coordination
- Constitutional Enforcement Agent has supreme authority
- No agent action should proceed without constitutional validation

### 2. Clear Responsibility Boundaries
- Each agent has well-defined responsibilities
- Avoid overlapping authority except for constitutional matters
- Clear escalation paths for conflicts

### 3. Efficient Communication
- Use appropriate coordination patterns for each use case
- Minimize coordination overhead
- Leverage shared context effectively

### 4. Robust Error Handling
- Comprehensive error detection and handling
- Automatic rollback capabilities
- Clear escalation procedures

### 5. Observability and Monitoring
- Complete audit trails for all agent interactions
- Performance monitoring and optimization
- Constitutional compliance tracking

---

**Version**: 1.0.0
**Authority**: Constitutional Enforcement Agent
**Scope**: All multi-agent coordination activities

This context file serves as the definitive guide for agent coordination and must be followed by all agents participating in multi-agent workflows.