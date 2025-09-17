---
name: Agent Name
model: claude-sonnet
description: >
  Short description of the agent’s purpose and domain.
triggers:
  - "when: keyword or pattern"
tools:
  - Read
  - Write
  - Grep
context_files:
  - .claude/context/project-constitution.md
  - specs/**
---

# Overview

Provide goals, constraints, and how this agent enforces constitutional principles.

## Responsibilities
- Domain-specific responsibilities

## Coordination
- How it collaborates with other agents and workflows

