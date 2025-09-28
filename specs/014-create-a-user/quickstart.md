# Quickstart Guide: User-Facing Audit Log Viewer

## Overview
This guide walks through the implementation and testing of the User-Facing Audit Log Viewer feature. The feature allows platform users to view, search, filter, and export audit logs with proper access controls.

## Prerequisites

### Backend Requirements
- Java 21 installed and configured
- Docker running (for TestContainers)
- PostgreSQL database (local or containerized)
- Redis instance (for session management)
- Existing Spring Boot Modulith platform running

### Frontend Requirements
- Node.js 18+ installed
- npm or yarn package manager
- React development environment set up

## Quick Setup

### 1. Environment Setup
```bash
# Navigate to project root
cd /Users/lsendel/IdeaProjects/sass

# Ensure Docker is running for TestContainers
docker --version

# Start local dependencies (if using docker-compose)
docker-compose up -d postgres redis

# Verify backend runs
cd backend
./gradlew bootRun --args='--spring.profiles.active=test'

# In another terminal, verify frontend runs
cd ../frontend
npm run dev
```

### 2. Database Preparation
```bash
# Backend terminal - create audit log test data
cd backend
./gradlew test --tests "*AuditDataSeeder*"

# Verify audit events exist
./gradlew flywayInfo -Pflyway-dev
```

### 3. API Contract Validation
```bash
# Run contract tests to ensure API compatibility
cd backend
./gradlew test --tests "*AuditLogViewControllerContractTest"

# Should initially fail - this is expected for TDD
```

## Development Workflow

### Phase 1: Backend Implementation

#### Step 1: Create API Controller
```bash
# Create the controller structure
mkdir -p backend/src/main/java/com/platform/audit/api
touch backend/src/main/java/com/platform/audit/api/AuditLogViewController.java
```

#### Step 2: Implement Service Layer
```bash
# Create service interfaces and implementations
mkdir -p backend/src/main/java/com/platform/audit/internal
touch backend/src/main/java/com/platform/audit/internal/AuditLogViewService.java
touch backend/src/main/java/com/platform/audit/internal/AuditLogExportService.java
```

#### Step 3: Run Contract Tests
```bash
# Run tests to verify API contracts
cd backend
./gradlew test --tests "*ContractTest" --info

# Tests should pass after implementation
```

### Phase 2: Frontend Implementation

#### Step 1: Create Redux Slice
```bash
# Create RTK Query API slice
mkdir -p frontend/src/store/api
touch frontend/src/store/api/auditApi.ts
```

#### Step 2: Implement Components
```bash
# Create component structure
mkdir -p frontend/src/components/audit
touch frontend/src/components/audit/AuditLogViewer.tsx
touch frontend/src/components/audit/AuditLogFilters.tsx
touch frontend/src/components/audit/AuditLogTable.tsx
touch frontend/src/components/audit/AuditLogExport.tsx
```

#### Step 3: Add Routing
```bash
# Update routing configuration
# Edit frontend/src/App.tsx to add audit log routes
```

### Phase 3: Integration Testing

#### Step 1: Backend Integration Tests
```bash
cd backend
# Run integration tests with real database
./gradlew test --tests "*AuditLogIntegrationTest"

# Run security tests
./gradlew test --tests "*AuditLogSecurityTest"
```

#### Step 2: Frontend E2E Tests
```bash
cd frontend
# Create E2E test files
mkdir -p tests/e2e
touch tests/e2e/audit-log-viewer.spec.ts

# Run E2E tests
npm run test:e2e
```

## Testing Strategy

### Contract Testing (Priority 1)
```bash
# Backend contract tests - must pass
cd backend
./gradlew test --tests "*AuditLogViewControllerContractTest"
./gradlew test --tests "*AuditLogExportControllerContractTest"
```

### Integration Testing (Priority 2)
```bash
# Backend integration tests with TestContainers
./gradlew test --tests "*AuditLogViewIntegrationTest"

# Frontend integration tests
cd ../frontend
npm run test:integration
```

### E2E Testing (Priority 3)
```bash
# Full user journey tests
cd frontend
npm run test:e2e -- --grep "audit log viewer"
```

### Unit Testing (Priority 4)
```bash
# Backend unit tests
cd backend
./gradlew test --tests "*AuditLogViewServiceUnitTest"

# Frontend component tests
cd ../frontend
npm run test:unit
```

## Key Test Scenarios

### Authentication & Authorization
1. **User Login Required**: Verify unauthenticated users are redirected
2. **Role-Based Access**: Test different user roles see appropriate audit data
3. **Tenant Isolation**: Ensure users only see their organization's data
4. **Permission Enforcement**: Verify export permissions are enforced

### Core Functionality
1. **Default View**: Load last 30 days of audit logs on initial access
2. **Date Range Filtering**: Filter logs by custom date ranges
3. **Search Functionality**: Search across descriptions and actor names
4. **Action Type Filtering**: Filter by specific action types
5. **Pagination**: Navigate through large result sets
6. **Sorting**: Sort by timestamp, action type, actor, etc.

### Export Functionality
1. **Export Request**: Successfully request CSV/JSON/PDF exports
2. **Export Status**: Check status of export processing
3. **Export Download**: Download completed exports with secure tokens
4. **Export Expiration**: Verify tokens expire after 24 hours
5. **Large Exports**: Handle exports with thousands of entries

### Performance & Security
1. **Response Time**: Queries complete within 500ms for typical datasets
2. **Large Dataset Handling**: Performance with 100k+ audit entries
3. **Rate Limiting**: Export requests are rate-limited per user
4. **Data Redaction**: Sensitive fields redacted based on permissions
5. **Audit Trail**: Viewing audit logs is itself audited

## Common Issues & Troubleshooting

### Backend Issues
```bash
# Database connection issues
./gradlew flywayInfo -Pflyway-dev

# TestContainers not starting
docker ps
docker logs <container-id>

# Permission/security issues
./gradlew test --tests "*SecurityTest" --info
```

### Frontend Issues
```bash
# API connection issues
npm run dev -- --host 0.0.0.0
# Check network tab in browser dev tools

# State management issues
# Use Redux DevTools to inspect state

# Component rendering issues
npm run test:ui
```

### Integration Issues
```bash
# CORS issues between frontend/backend
# Check backend CORS configuration

# Session/authentication issues
# Clear browser cookies and retry

# Performance issues
# Check database indexes and query performance
```

## Validation Checklist

### Feature Completeness
- [ ] Users can view audit logs with proper permissions
- [ ] Date range filtering works correctly
- [ ] Free-text search finds relevant entries
- [ ] Action and resource type filtering functions
- [ ] Pagination handles large datasets
- [ ] Export to CSV, JSON, and PDF formats
- [ ] Download tokens expire appropriately
- [ ] Error handling provides clear user feedback

### Security & Compliance
- [ ] Authentication required for all endpoints
- [ ] Role-based access control enforced
- [ ] Tenant isolation prevents data leakage
- [ ] Sensitive data redacted appropriately
- [ ] Audit log access is audited
- [ ] Export rate limiting prevents abuse
- [ ] GDPR compliance for data export

### Performance & Reliability
- [ ] Initial load under 500ms
- [ ] Search/filter under 300ms
- [ ] Large exports complete without timeout
- [ ] Database queries use proper indexes
- [ ] Frontend handles loading states gracefully
- [ ] Error states provide actionable information

### Integration & Deployment
- [ ] Backend integrates with existing audit module
- [ ] Frontend integrates with existing authentication
- [ ] Database migrations run successfully
- [ ] All tests pass in CI/CD pipeline
- [ ] Feature toggles allow gradual rollout

## Next Steps

### Production Deployment
1. **Database Migration**: Apply audit log schema changes
2. **Feature Flags**: Enable feature for beta users
3. **Monitoring**: Set up alerts for export failures and performance
4. **Documentation**: Update user documentation and API docs

### Future Enhancements
1. **Real-time Updates**: WebSocket integration for live audit feeds
2. **Advanced Filtering**: More sophisticated search and filter options
3. **Audit Analytics**: Dashboard views of audit activity trends
4. **Scheduled Exports**: Automatic recurring exports for compliance

## Support Resources

### Documentation
- [Spring Boot Modulith Documentation](https://spring.io/projects/spring-modulith)
- [Redux Toolkit Documentation](https://redux-toolkit.js.org/)
- [Playwright Testing Documentation](https://playwright.dev/)

### Internal Resources
- Backend CLAUDE.md: `../backend/CLAUDE.md`
- Frontend CLAUDE.md: `../frontend/CLAUDE.md`
- API Contracts: `./contracts/audit-log-api.yaml`
- Data Model: `./data-model.md`

### Troubleshooting
- Check application logs: `backend/logs/`
- Database queries: Use PostgreSQL query logs
- Frontend debugging: Browser DevTools + Redux DevTools
- Integration issues: Check CORS and authentication configuration

This quickstart guide provides a foundation for implementing and testing the User-Facing Audit Log Viewer feature following TDD principles and constitutional requirements.