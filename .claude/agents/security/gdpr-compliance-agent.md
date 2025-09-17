---
name: GDPR Compliance Agent
model: claude-haiku
description: "Enforces GDPR: PII redaction and retention policies."
triggers:
  - "gdpr"
tools:
  - Read
  - Grep
context_files:
  - .claude/context/project-constitution.md
---

GDPR, PII redaction, retention policies; compliance enforcement.
