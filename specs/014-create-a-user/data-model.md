# Data Model: User-Facing Audit Log Viewer

## Core Entities

### AuditLogEntry
Represents a single audit event visible to end users with appropriate access controls.

**Attributes:**
- `id`: Unique identifier (UUID)
- `timestamp`: When the event occurred (ISO 8601 timestamp)
- `correlationId`: Request correlation identifier for tracing
- `actorType`: Type of entity that performed the action (USER, SYSTEM, API_KEY)
- `actorId`: Identifier of the actor (user ID, system component, etc.)
- `actorDisplayName`: Human-readable name of the actor
- `actionType`: Category of action performed (CREATE, UPDATE, DELETE, LOGIN, PAYMENT, etc.)
- `actionDescription`: Human-readable description of the action
- `resourceType`: Type of resource affected (USER, PAYMENT, SUBSCRIPTION, ORGANIZATION, etc.)
- `resourceId`: Identifier of the affected resource
- `resourceDisplayName`: Human-readable name/description of the resource
- `organizationId`: Organization context for multi-tenant isolation
- `outcome`: Result of the action (SUCCESS, FAILURE, PARTIAL)
- `ipAddress`: Source IP address (potentially redacted based on permissions)
- `userAgent`: Client user agent (potentially redacted)
- `additionalData`: Map of additional context-specific information
- `sensitivity`: Data sensitivity level (PUBLIC, INTERNAL, CONFIDENTIAL)

**State Transitions:**
- Audit entries are immutable once created
- No state transitions - events represent historical facts

**Validation Rules:**
- `timestamp` must be valid ISO 8601 format
- `actorId` and `resourceId` must be valid UUIDs or system identifiers
- `actionType` must be from predefined enum
- `organizationId` required for tenant isolation
- `outcome` must be from predefined enum

### AuditLogFilter
Represents user-defined criteria for filtering audit log entries.

**Attributes:**
- `dateFrom`: Start date for time range filter (ISO 8601 date)
- `dateTo`: End date for time range filter (ISO 8601 date)
- `searchText`: Free-text search across descriptions and actor names
- `actionTypes`: List of action types to include (empty = all)
- `resourceTypes`: List of resource types to include (empty = all)
- `outcomes`: List of outcomes to include (empty = all)
- `pageNumber`: Page number for pagination (0-based)
- `pageSize`: Number of entries per page (default 50, max 100)
- `sortField`: Field to sort by (default: timestamp)
- `sortDirection`: Sort direction (ASC, DESC - default DESC)

**Validation Rules:**
- `dateFrom` must be before `dateTo`
- Date range cannot exceed 1 year
- `pageSize` must be between 1 and 100
- `searchText` limited to 255 characters
- Valid values for sort fields and directions

### AuditLogExportRequest
Represents a request to export audit log data in various formats.

**Attributes:**
- `id`: Unique identifier for the export request
- `requestedBy`: User ID who requested the export
- `organizationId`: Organization context
- `filters`: AuditLogFilter criteria for the export
- `format`: Export format (CSV, JSON, PDF)
- `status`: Export status (PENDING, IN_PROGRESS, COMPLETED, FAILED)
- `requestedAt`: When the export was requested
- `completedAt`: When the export was completed (if applicable)
- `downloadToken`: Secure token for downloading the export
- `tokenExpiresAt`: When the download token expires
- `errorMessage`: Error details if export failed
- `entryCount`: Number of entries included in the export
- `fileSizeBytes`: Size of the generated export file

**State Transitions:**
1. PENDING → IN_PROGRESS (when processing starts)
2. IN_PROGRESS → COMPLETED (when successfully generated)
3. IN_PROGRESS → FAILED (if processing fails)
4. COMPLETED → EXPIRED (when download token expires)

**Validation Rules:**
- `format` must be valid export format
- `downloadToken` must be cryptographically secure
- Token expiration maximum 24 hours from generation
- Export limited to 100,000 entries maximum

### UserAuditPermissions
Represents what audit information a user is authorized to view.

**Attributes:**
- `userId`: User identifier
- `organizationId`: Organization context
- `canViewOwnActions`: Can view their own audit entries
- `canViewOrganizationActions`: Can view organization-wide audit entries
- `canViewSensitiveData`: Can view unredacted sensitive information
- `canExportData`: Can generate exports of audit data
- `allowedResourceTypes`: List of resource types user can view audit data for
- `allowedActionTypes`: List of action types user can view
- `maxDateRange`: Maximum date range allowed for queries (in days)
- `maxExportEntries`: Maximum number of entries allowed in exports

**Validation Rules:**
- User must exist in the system
- Organization membership required for organization-level permissions
- Permissions consistent with user's role assignments
- Date range limits prevent excessive queries

## Entity Relationships

### AuditLogEntry Relationships
- **Actor Reference**: Links to User entity via `actorId` (when actorType = USER)
- **Resource Reference**: Links to various entities via `resourceId` and `resourceType`
- **Organization Context**: Belongs to Organization via `organizationId`

### AuditLogFilter Relationships
- **Applied To**: Filters apply to collections of AuditLogEntry entities
- **User Context**: Implicitly associated with requesting user's permissions

### AuditLogExportRequest Relationships
- **Requested By**: Links to User entity via `requestedBy`
- **Organization Context**: Belongs to Organization via `organizationId`
- **Filter Criteria**: Contains AuditLogFilter as embedded object
- **Source Data**: References AuditLogEntry entities matching filter criteria

### UserAuditPermissions Relationships
- **User Reference**: Links to User entity via `userId`
- **Organization Reference**: Links to Organization entity via `organizationId`
- **Permission Scope**: Determines which AuditLogEntry entities user can access

## Data Access Patterns

### Query Patterns
1. **Recent Activity**: Most common query - last 30 days for user's organization
2. **Search by Actor**: Find all actions by specific user
3. **Search by Resource**: Find all actions affecting specific resource
4. **Compliance Reporting**: Date range queries for audit reports
5. **Incident Investigation**: Correlation ID tracking across events

### Performance Considerations
- **Primary Index**: (organizationId, timestamp DESC) for tenant isolation and recency
- **Search Index**: (organizationId, actorId, timestamp) for actor-based queries
- **Resource Index**: (organizationId, resourceType, resourceId, timestamp) for resource tracking
- **Correlation Index**: (correlationId) for request tracing
- **Full-text Index**: On actionDescription and actorDisplayName for search

### Security Constraints
- **Tenant Isolation**: All queries must filter by user's authorized organizations
- **Role-Based Access**: Permission checks applied at service layer
- **Data Redaction**: Sensitive fields masked based on user permissions
- **Audit Trail**: All access to audit logs is itself audited

## Data Retention and Lifecycle

### Retention Policies
- **Standard Retention**: 7 years for compliance requirements
- **Sensitive Actions**: May have extended retention based on regulations
- **Export Data**: Temporary files deleted after 24 hours
- **Failed Exports**: Cleaned up after 7 days

### Archival Strategy
- **Monthly Partitioning**: Audit data partitioned by month for performance
- **Automatic Archival**: Data older than 2 years moved to archive storage
- **Purge Process**: Automated deletion after retention period expires

### GDPR Compliance
- **Right to Access**: Users can view their own audit data via this feature
- **Right to Portability**: Export functionality serves GDPR data portability
- **Right to Erasure**: Audit entries containing personal data subject to deletion
- **Data Minimization**: Only necessary audit data retained per policy

## Integration Points

### Backend Integration
- **Audit Module**: Extends existing `com.platform.audit` module
- **User Module**: Integrates with user and organization entities
- **Security Module**: Leverages existing permission and role systems
- **Shared Module**: Uses common security and validation utilities

### Frontend Integration
- **Redux Store**: AuditLogState with entries, filters, and export status
- **RTK Query**: auditApi slice with endpoints for logs and exports
- **Component State**: Local state for UI interactions and form data
- **Cache Strategy**: Time-based cache invalidation for audit data

### External Systems
- **Export Storage**: Temporary storage for generated export files
- **Notification System**: Alerts when exports are ready for download
- **Compliance Systems**: Integration with external audit and compliance tools

## Future Enhancements

### Real-time Updates
- **WebSocket Integration**: Live updates for new audit events
- **Event Streaming**: Kafka-based event streaming for real-time audit feeds
- **Notification Subscriptions**: User subscriptions to specific audit event types

### Advanced Analytics
- **Audit Dashboards**: Graphical representations of audit activity
- **Trend Analysis**: Pattern detection in audit data
- **Anomaly Detection**: Automated detection of unusual audit patterns
- **Compliance Metrics**: Automated compliance reporting and scoring

### Enhanced Export Features
- **Scheduled Exports**: Recurring export generation
- **Custom Report Templates**: User-defined export formats and layouts
- **Bulk Export API**: Programmatic access for large-scale data exports
- **Export Encryption**: Optional encryption for sensitive export data