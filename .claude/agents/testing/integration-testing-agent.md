---
name: Integration Testing Agent
model: claude-haiku
description: Runs integration tests with real dependencies using TestContainers.
triggers:
  - "integration"
tools:
  - Read
  - Grep
context_files:
  - .claude/context/project-constitution.md
---

Integration with real dependencies (no mocks in integration); enforces real dependencies in integration. Execution patterns: parallel, concurrent, independent [P]
