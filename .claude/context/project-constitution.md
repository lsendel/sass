# Project Constitution

This document defines non-negotiable constitutional principles guiding the project.

Principles:

- Library First Architecture: every feature is a standalone, self-contained, independent library.
- TDD Required (NON-NEGOTIABLE): test-first, RED-GREEN-Refactor cycle, no implementation before test, avoid skipping RED phase.
- Test Order Enforced: Contract → Integration → E2E → Unit test hierarchy.
- Real Dependencies in Integration: use TestContainers and real dependencies; no mocks in integration.
- Observability Required: structured logging, observability, monitoring, metrics.
- Module Communication via Events: event-driven architecture using ApplicationEventPublisher; enforce module boundaries.
- Opaque Tokens Only: no JWT; use SHA-256 opaque tokens and secure token security.
- GDPR Compliance: GDPR with PII redaction and retention policies for compliance.

