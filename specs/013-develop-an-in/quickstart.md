# Security Observability Dashboard Quickstart

## Prerequisites

- Java 21 installed
- Node.js 18+ installed
- PostgreSQL 14+ running
- InfluxDB 2.0+ running (for time-series metrics)
- Redis 6+ running (for caching)

## Backend Setup

### 1. Environment Configuration

Create `backend/src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sass_security
    username: sass_user
    password: sass_password

  influx:
    url: http://localhost:8086
    token: your-influx-token
    org: sass-org
    bucket: security-metrics

  redis:
    host: localhost
    port: 6379

security:
  dashboard:
    websocket:
      enabled: true
      max-connections: 100
    alert:
      enabled: true
      cooldown-period: PT30S
```

### 2. Database Setup

```bash
# Create PostgreSQL database
createdb sass_security

# Run migrations
./gradlew flywayMigrate
```

### 3. Start Backend Services

```bash
# Start the Spring Boot application
./gradlew bootRun --args="--spring.profiles.active=local"
```

The backend will start on `http://localhost:8080`.

## Frontend Setup

### 1. Install Dependencies

```bash
cd frontend
npm install
```

### 2. Environment Configuration

Create `frontend/.env.local`:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WEBSOCKET_URL=ws://localhost:8080/ws
VITE_SECURITY_DASHBOARD_ENABLED=true
```

### 3. Start Frontend Development Server

```bash
npm run dev
```

The frontend will start on `http://localhost:3000`.

## Quick Validation Tests

### 1. Backend Health Check

```bash
# Check API health
curl http://localhost:8080/actuator/health

# Check security events endpoint
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/api/v1/security-events

# Expected: 200 OK with empty events list
```

### 2. WebSocket Connection Test

```javascript
// Open browser console and test WebSocket
const ws = new WebSocket('ws://localhost:8080/ws');
ws.onopen = () => console.log('Connected to security events stream');
ws.onmessage = (event) => console.log('Security event:', JSON.parse(event.data));
```

### 3. Dashboard Access Test

1. Navigate to `http://localhost:3000/security/dashboard`
2. Login with test credentials
3. Verify default security dashboard loads
4. Check that widgets display "No data" initially

### 4. Create Test Security Event

```bash
# Create a test security event
curl -X POST http://localhost:8080/api/v1/security-events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "eventType": "LOGIN_ATTEMPT",
    "severity": "MEDIUM",
    "sourceModule": "auth",
    "sourceIp": "127.0.0.1",
    "userAgent": "Test Browser",
    "userId": "test-user",
    "sessionId": "test-session",
    "correlationId": "test-correlation",
    "details": {
      "success": false,
      "failureReason": "Invalid password"
    }
  }'

# Expected: 201 Created with event details
```

### 5. Verify Event Display

1. Refresh the dashboard at `http://localhost:3000/security/dashboard`
2. Check that the test event appears in the events widget
3. Verify real-time updates by creating another event
4. Confirm WebSocket receives the new event

## Default Dashboard Widgets

The quickstart includes these pre-configured widgets:

### 1. Security Events Summary
- **Type**: METRIC
- **Data**: Count of events by severity in last 24h
- **Refresh**: 30 seconds

### 2. Failed Login Attempts
- **Type**: CHART (line chart)
- **Data**: Failed login attempts over time
- **Refresh**: 1 minute

### 3. Recent Critical Events
- **Type**: TABLE
- **Data**: Last 10 critical security events
- **Refresh**: 15 seconds

### 4. Threat Level Indicator
- **Type**: METRIC
- **Data**: Current threat level based on event patterns
- **Refresh**: 30 seconds

### 5. Module Security Status
- **Type**: CHART (pie chart)
- **Data**: Security events by module
- **Refresh**: 5 minutes

## Test User Stories

### Story 1: Security Analyst Monitoring
1. **Given** I am a security analyst
2. **When** I access the security dashboard
3. **Then** I see real-time security events from all platform modules
4. **And** I can filter events by severity and type
5. **And** Critical events are highlighted prominently

**Test Steps**:
- Login as security analyst role
- Navigate to main security dashboard
- Verify all widgets load within 3 seconds
- Create events of different severities
- Confirm filtering works correctly

### Story 2: Incident Response
1. **Given** A critical security event occurs
2. **When** The event is detected by the system
3. **Then** An alert notification is sent immediately
4. **And** The event appears on all relevant dashboards
5. **And** I can mark the event as resolved

**Test Steps**:
- Create critical security event via API
- Verify alert notification appears within 5 seconds
- Check event appears in dashboard real-time
- Mark event as resolved and verify status update

### Story 3: Dashboard Customization
1. **Given** I am viewing the security dashboard
2. **When** I add a new widget
3. **Then** The widget is saved to my dashboard
4. **And** The widget displays relevant security data
5. **And** Other users see their own customized views

**Test Steps**:
- Access dashboard customization mode
- Add new chart widget for payment fraud events
- Configure widget data source and refresh interval
- Save dashboard and reload page
- Verify widget persists and displays data

## Performance Validation

### Expected Response Times
- Dashboard initial load: < 3 seconds
- Widget data refresh: < 1 second
- Real-time event delivery: < 500ms
- API response times: < 200ms (95th percentile)

### Load Testing
```bash
# Install Apache Bench for basic load testing
# Test API endpoint performance
ab -n 1000 -c 10 -H "Authorization: Bearer YOUR_TOKEN" \
   http://localhost:8080/api/v1/security-events

# Expected: All requests complete successfully
# 95% of requests under 200ms
```

## Troubleshooting

### Common Issues

**WebSocket Connection Fails**
- Check if port 8080 is accessible
- Verify JWT token is valid
- Check browser console for CORS errors

**Dashboard Widgets Show "No Data"**
- Verify database connection
- Check if InfluxDB is running and accessible
- Ensure security events are being created

**Slow Dashboard Performance**
- Check Redis connection for caching
- Verify InfluxDB query performance
- Review network connectivity

**Alert Notifications Not Working**
- Check application logs for alert processing errors
- Verify notification channel configuration
- Test webhook endpoints manually

### Development Tips

1. **Real-time Testing**: Use browser developer tools to monitor WebSocket messages
2. **API Testing**: Use Postman collection (available in `/docs/postman/`)
3. **Database Inspection**: Connect to PostgreSQL and InfluxDB directly to verify data
4. **Performance Monitoring**: Enable Spring Actuator metrics for backend monitoring

## Next Steps

After completing the quickstart:

1. **Configure Production Environment**: Update configuration for production databases and security
2. **Set Up Monitoring**: Configure Prometheus metrics and Grafana dashboards
3. **Customize Dashboards**: Create role-specific dashboards for different user types
4. **Integrate External Systems**: Connect to SIEM tools and notification services
5. **Performance Tuning**: Optimize database queries and caching strategies

## Security Considerations

- **Authentication**: All API endpoints require valid JWT tokens
- **Authorization**: Role-based access controls which dashboard data users can see
- **Data Encryption**: All sensitive data encrypted in transit and at rest
- **Audit Logging**: All dashboard access and configuration changes are logged
- **Rate Limiting**: API endpoints are rate-limited to prevent abuse