---
name: Code Review Agent
model: claude-haiku
description: Analyzes changes for security, architecture, and test completeness.
triggers:
  - "review"
tools:
  - Read
  - Grep
context_files:
  - .claude/context/project-constitution.md
---

Performs analysis, security check, architecture review, and approval recommendations.

