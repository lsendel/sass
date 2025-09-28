# Claude Code Development Context for SASS Platform

## Current Feature: Complete UI Documentation for Project Management & Collaboration Platform (Branch: 015-complete-ui-documentation)

### Feature Summary
Comprehensive web-based project management and collaboration platform enabling teams of 5-50 people to organize work, track progress, and collaborate effectively. Features include project creation, task management with drag-and-drop Kanban boards, real-time collaboration, file sharing, search capabilities, personalized dashboards, and mobile-responsive interfaces with offline support.

### Technology Stack
- **Backend**: Java 21, Spring Boot 3.5.5, Spring Security, PostgreSQL
- **Frontend**: React 18+, TypeScript, Redux Toolkit, WebSocket
- **Storage**: PostgreSQL (primary data), Redis (caching, sessions), File storage
- **Real-time**: WebSocket, Server-Sent Events (SSE fallback)
- **Testing**: JUnit 5, Testcontainers (backend), Vitest, Playwright (frontend)

### Key Components
- **Project Management**: Create and organize projects with team collaboration
- **Task Management**: Drag-and-drop Kanban boards with subtasks and time tracking
- **Real-time Collaboration**: WebSocket-based updates, comments, and presence indicators
- **Search & Navigation**: Global search with command palette and filtering
- **Mobile Interface**: Responsive design with offline capability and push notifications

### Architecture Decisions
- OAuth2/OpenID Connect authentication with opaque tokens
- RESTful APIs with GraphQL for complex queries
- Multi-layer caching strategy (Redis + browser caching)
- Normalized PostgreSQL schema with audit trails and soft deletes

### Recent Changes (Last 3 Features)
1. **015-complete-ui-documentation**: Project management & collaboration platform
2. **013-develop-an-in**: Security observability dashboard implementation
3. **012-implement-role-based**: Role-based access control with RBAC components

### Development Guidelines
- **TDD Required**: All tests must be written first and fail before implementation
- **Constitution Compliance**: Follow library-first, CLI-enabled, observable patterns
- **Real-time Collaboration**: WebSocket connections for live updates and presence
- **Performance**: Dashboard <2s, API <200ms, real-time updates <1s, 100+ concurrent users

### Module Integration Points
- **Auth Module**: Authentication/authorization event streaming
- **Payment Module**: Payment fraud detection and monitoring
- **Subscription Module**: Subscription security event tracking
- **User Module**: User activity and permission change monitoring
- **Audit Module**: Comprehensive audit logging integration

### Testing Strategy
- **Contract Tests**: API schema validation for all endpoints
- **Integration Tests**: Real database connections, WebSocket testing
- **E2E Tests**: Full user journey validation with Playwright
- **Performance Tests**: Load testing for concurrent dashboard users

### Key Files
- **Backend**: `backend/src/main/java/com/sass/project/`
- **Frontend**: `frontend/src/components/project/`, `frontend/src/components/task/`
- **Contracts**: `specs/015-complete-ui-documentation/contracts/`
- **Documentation**: `specs/015-complete-ui-documentation/`

### Constitutional Compliance
-  Library-first: security-monitoring-lib, dashboard-ui-lib
-  CLI interfaces: security-monitor CLI, dashboard-config CLI
-  Test-first: TDD enforced with contract�integration�e2e�unit order
-  Real dependencies: PostgreSQL, InfluxDB, Redis in tests
-  Observability: Structured JSON logging, correlation IDs
-  Versioning: 1.0.0 with BUILD increments

### Performance Requirements
- Dashboard load time: <200ms
- Real-time updates: <1s latency
- API response: <200ms (95th percentile)
- Concurrent users: 100+
- Data retention: 90 days detailed, 1 year aggregated

---
*Last Updated: 2025-09-28 | Feature: Complete UI Documentation for Project Management Platform*