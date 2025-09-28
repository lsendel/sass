# Research: User-Facing Audit Log Viewer

## Research Overview

This document consolidates research findings for implementing a user-facing audit log viewer in the Spring Boot Modulith Payment Platform. The research addresses implementation patterns, security considerations, and integration approaches.

## 1. Export Format Requirements

**Decision**: Support CSV, JSON, and PDF export formats
**Rationale**:
- CSV for spreadsheet analysis and data processing
- JSON for programmatic access and API integration
- PDF for compliance documentation and formal reporting
**Alternatives considered**:
- Excel format (rejected due to licensing complexity)
- XML format (rejected due to verbosity and poor human readability)

## 2. Authorization Rules

**Decision**: Role-based access with organization-level filtering
**Rationale**:
- Users see only audit logs for resources they have permission to access
- Organization admins see all logs within their organization
- Super admins see logs across organizations
- Filtering based on user's current role assignments and organizational membership
**Alternatives considered**:
- Resource-based permissions (rejected due to complexity)
- Time-based access windows (deferred to future enhancement)

## 3. Session Persistence Rules

**Decision**: 30-minute session with automatic refresh on activity
**Rationale**:
- Balances user experience with security requirements
- Follows existing platform session management patterns
- Supports long audit review sessions while maintaining security
**Alternatives considered**:
- Persistent client-side storage (rejected for security)
- No session persistence (rejected for poor UX)

## 4. Timezone Handling

**Decision**: Display in user's browser timezone with UTC option
**Rationale**:
- Automatic timezone detection for better user experience
- UTC toggle for consistency in multi-timezone organizations
- Follows international compliance standards for audit logs
**Alternatives considered**:
- Server-timezone only (rejected for poor international UX)
- User-configurable timezone preference (deferred to settings feature)

## 5. Backend Integration Patterns

**Decision**: Extend existing audit module with viewer-specific endpoints
**Rationale**:
- Leverages existing audit infrastructure and data model
- Maintains separation of concerns within audit module
- Reuses existing permission and security systems
**Implementation approach**:
- Add new API endpoints in `audit.api` package
- Extend existing `AuditService` with viewer-specific methods
- Create DTOs for filtered and paginated audit responses

## 6. Frontend Architecture

**Decision**: RTK Query-based state management with React components
**Rationale**:
- Consistent with existing frontend architecture
- Automatic caching and invalidation for audit data
- Type-safe API integration with backend
**Key components**:
- `AuditLogViewer` main container component
- `AuditLogFilters` for date range, search, and type filtering
- `AuditLogEntry` for individual log display
- `AuditLogExport` for export functionality

## 7. Performance and Pagination

**Decision**: Server-side pagination with 50 entries per page
**Rationale**:
- Prevents performance issues with large audit datasets
- Consistent with existing platform pagination patterns
- Supports efficient searching and filtering on server side
**Implementation**:
- Cursor-based pagination for consistent results
- Elasticsearch or database indexes for search performance
- Client-side virtual scrolling for smooth UX

## 8. Search and Filtering

**Decision**: Full-text search with structured filters
**Rationale**:
- Supports both quick text search and precise filtering
- Matches user expectations from modern audit tools
- Efficient backend implementation with existing audit schema
**Search fields**:
- Action type (create, update, delete, login, etc.)
- Actor/user email and name
- Affected resource type and ID
- Timestamp ranges
- Free-text search across action descriptions

## 9. Security Considerations

**Decision**: Strict access control with audit trail of viewer access
**Rationale**:
- Audit log access is itself auditable for compliance
- Prevents unauthorized data exposure
- Supports incident investigation workflows
**Security measures**:
- All audit viewer access logged with correlation IDs
- Rate limiting on export functionality
- Data masking for sensitive information based on user permissions
- Secure export token generation for download links

## 10. GDPR and Compliance

**Decision**: Full GDPR compliance with data subject rights
**Rationale**:
- Legal requirement for EU users
- Consistent with platform's existing GDPR implementation
- Supports data subject access requests
**Compliance features**:
- PII redaction in audit logs based on user permissions
- Data retention enforcement (automatic expiry)
- Support for data subject deletion requests
- Audit trail of compliance actions

## Technical Implementation Summary

### Backend Components
- **AuditLogViewController**: REST controller for viewer endpoints
- **AuditLogViewService**: Business logic for filtering and formatting
- **AuditLogViewRepository**: Data access with security filtering
- **AuditLogExportService**: Export generation and secure downloads

### Frontend Components
- **AuditLogViewer**: Main container with search and filters
- **AuditLogTable**: Paginated table with sorting
- **AuditLogDetails**: Modal for detailed entry view
- **AuditLogExport**: Export format selection and download

### API Design
```
GET /api/audit/logs?page=0&size=50&search=&dateFrom=&dateTo=&actions=
POST /api/audit/export (returns download token)
GET /api/audit/export/{token} (secure download)
```

### Database Considerations
- Existing audit_events table supports all requirements
- Additional indexes on (user_id, timestamp) and (organization_id, timestamp)
- Consider partitioning by month for large datasets

## Dependencies and Integration Points

### Backend Dependencies
- Existing audit module infrastructure
- Spring Security for permission enforcement
- User module for role and organization data
- Shared module for security utilities

### Frontend Dependencies
- Existing RTK Query setup
- React Table or similar for data display
- Date picker components for range selection
- Export progress indicators and download handling

## Risks and Mitigation

### Performance Risks
- **Risk**: Large audit datasets causing slow queries
- **Mitigation**: Proper indexing, pagination, and caching strategies

### Security Risks
- **Risk**: Unauthorized access to sensitive audit data
- **Mitigation**: Strict role-based filtering and access logging

### Compliance Risks
- **Risk**: GDPR violations in audit data display
- **Mitigation**: Automated PII redaction and retention policies

## Next Steps for Phase 1 Design

1. Create detailed data model based on existing audit schema
2. Design API contracts for audit viewer endpoints
3. Define frontend component hierarchy and state management
4. Generate contract tests for API endpoints
5. Create quickstart guide for development setup

---

*Research completed on 2025-09-27*
*Ready for Phase 1 Design & Contracts*