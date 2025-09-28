# Security Observability Dashboard Data Model

## Core Entities

### SecurityEvent
Primary entity for capturing all security-related events across the platform.

**Fields**:
- `id`: UUID - Unique event identifier
- `eventType`: Enum - Type of security event (LOGIN_ATTEMPT, PERMISSION_CHECK, PAYMENT_FRAUD, etc.)
- `severity`: Enum - CRITICAL, HIGH, MEDIUM, LOW, INFO
- `timestamp`: Instant - When the event occurred
- `userId`: String - User associated with the event (nullable)
- `sessionId`: String - Session identifier for correlation
- `sourceModule`: String - Which modulith module generated the event
- `sourceIp`: String - IP address of the request
- `userAgent`: String - Browser/client information
- `details`: Map<String, Object> - Event-specific data
- `correlationId`: String - For tracing across services
- `resolved`: Boolean - Whether security issue was resolved
- `resolvedBy`: String - Who resolved the issue (nullable)
- `resolvedAt`: Instant - When resolved (nullable)

**Validation Rules**:
- `eventType` must be from predefined enum
- `timestamp` cannot be in the future
- `severity` must be provided for all events
- `correlationId` required for cross-module events

**State Transitions**:
- NEW → ACKNOWLEDGED → INVESTIGATING → RESOLVED
- CRITICAL events cannot be directly resolved without investigation

### DashboardWidget
Configuration for individual dashboard components.

**Fields**:
- `id`: UUID - Widget identifier
- `name`: String - Display name
- `type`: Enum - CHART, TABLE, METRIC, ALERT_LIST, THREAT_MAP
- `configuration`: JSON - Widget-specific settings
- `position`: JSON - Dashboard layout position
- `permissions`: Set<String> - Required roles to view
- `refreshInterval`: Duration - How often to update
- `dataSource`: String - Query/filter for data
- `createdBy`: String - User who created widget
- `createdAt`: Instant - Creation timestamp
- `lastModified`: Instant - Last update timestamp

**Validation Rules**:
- `name` must be unique per dashboard
- `refreshInterval` minimum 5 seconds
- `permissions` cannot be empty
- `configuration` must match widget type schema

### Dashboard
Container for organizing widgets into security monitoring views.

**Fields**:
- `id`: UUID - Dashboard identifier
- `name`: String - Dashboard name
- `description`: String - Purpose description
- `widgets`: List<DashboardWidget> - Ordered widget list
- `permissions`: Set<String> - Required roles for access
- `isDefault`: Boolean - Default dashboard for role
- `tags`: Set<String> - Categorization tags
- `owner`: String - Dashboard owner/creator
- `shared`: Boolean - Shared with other users
- `createdAt`: Instant - Creation timestamp
- `lastModified`: Instant - Last update timestamp

**Validation Rules**:
- `name` must be unique per owner
- Maximum 20 widgets per dashboard
- At least one permission required
- Only one default dashboard per role

### AlertRule
Configuration for automated security alerting.

**Fields**:
- `id`: UUID - Rule identifier
- `name`: String - Rule name
- `description`: String - What this rule detects
- `condition`: String - Query/condition expression
- `severity`: Enum - Alert severity level
- `enabled`: Boolean - Whether rule is active
- `threshold`: Integer - Trigger threshold
- `timeWindow`: Duration - Evaluation time window
- `cooldownPeriod`: Duration - Minimum time between alerts
- `notificationChannels`: List<String> - Where to send alerts
- `escalationRules`: JSON - Escalation configuration
- `createdBy`: String - Rule creator
- `lastTriggered`: Instant - When last triggered (nullable)
- `triggerCount`: Integer - Total trigger count
- `createdAt`: Instant - Creation timestamp

**Validation Rules**:
- `threshold` must be positive
- `timeWindow` minimum 1 minute
- `cooldownPeriod` minimum 30 seconds
- `condition` must be valid query syntax

### SecurityMetric
Aggregated security metrics for performance monitoring.

**Fields**:
- `id`: UUID - Metric identifier
- `metricName`: String - Metric name (e.g., "failed_logins_per_hour")
- `value`: Double - Metric value
- `timestamp`: Instant - When metric was calculated
- `tags`: Map<String, String> - Metric dimensions
- `unit`: String - Metric unit (count, percentage, milliseconds)
- `aggregationType`: Enum - SUM, AVERAGE, MAX, MIN, COUNT
- `timeGranularity`: Enum - MINUTE, HOUR, DAY
- `retentionDays`: Integer - How long to keep this metric

**Validation Rules**:
- `value` cannot be negative for count metrics
- `timestamp` must align with time granularity
- `retentionDays` must be positive
- Metric name must follow naming convention

### ThreatIndicator
External threat intelligence integration.

**Fields**:
- `id`: UUID - Indicator identifier
- `type`: Enum - IP_ADDRESS, DOMAIN, HASH, EMAIL
- `value`: String - The indicator value
- `source`: String - Threat intel source
- `confidence`: Integer - Confidence score (0-100)
- `severity`: Enum - Threat severity
- `firstSeen`: Instant - When first detected
- `lastSeen`: Instant - Most recent detection
- `description`: String - Threat description
- `tags`: Set<String> - Threat categories
- `active`: Boolean - Whether still considered threat
- `falsePositive`: Boolean - Marked as false positive

**Validation Rules**:
- `confidence` between 0-100
- `value` must match type format
- `lastSeen` >= `firstSeen`
- Cannot be both active and false positive

## Entity Relationships

### SecurityEvent Relationships
- Many SecurityEvents → One User (via userId)
- Many SecurityEvents → One Session (via sessionId)
- SecurityEvents linked by correlationId for tracing

### Dashboard Relationships
- One Dashboard → Many DashboardWidgets (composition)
- Many Dashboards → Many Users (via permissions)
- One User → Many owned Dashboards

### AlertRule Relationships
- One AlertRule → Many SecurityEvents (triggers)
- Many AlertRules → Many NotificationChannels
- AlertRules operate on SecurityMetrics

### Metric Relationships
- SecurityMetrics aggregated from SecurityEvents
- Metrics feed into DashboardWidgets
- Metrics trigger AlertRules

## Data Flow

1. **Event Ingestion**: SecurityEvents created by Spring Security, module events
2. **Metric Calculation**: Background jobs aggregate SecurityEvents into SecurityMetrics
3. **Dashboard Display**: Widgets query SecurityMetrics and SecurityEvents
4. **Alert Processing**: AlertRules evaluate metrics and trigger notifications
5. **Threat Correlation**: ThreatIndicators enrich SecurityEvents with external intel

## Storage Strategy

### PostgreSQL Tables
- Dashboard configuration and permissions
- AlertRule definitions and history
- User preferences and settings
- Reference data and metadata

### InfluxDB Time Series
- SecurityEvent storage (high volume)
- SecurityMetric storage (aggregated data)
- Real-time data for dashboard updates
- Time-based queries and analysis

## Performance Considerations

### Indexing Strategy
- SecurityEvent: timestamp, eventType, severity, userId
- SecurityMetric: metricName, timestamp, tags
- Dashboard: permissions, owner, isDefault
- ThreatIndicator: type, value, active

### Data Retention
- SecurityEvents: 90 days detailed, 1 year aggregated
- SecurityMetrics: 2 years for all granularities
- AlertRule history: 1 year
- Dashboard audit logs: 1 year

### Caching Strategy
- Dashboard configurations: Redis cache (1 hour TTL)
- Active AlertRules: In-memory cache (5 minute TTL)
- Frequently accessed metrics: Redis cache (10 minute TTL)
- ThreatIndicators: Redis cache (1 hour TTL)