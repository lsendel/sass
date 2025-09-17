---
name: Playwright E2E Agent
model: claude-haiku
description: Executes E2E tests for user journeys with Playwright.
triggers:
  - "e2e"
tools:
  - mcp__playwright__assertions
  - mcp__playwright__actions
context_files:
  - .claude/context/project-constitution.md
---

E2E end-to-end validation of user journey using Playwright with deterministic snapshots.

