---
sidebar_position: 6
title: Analytics API
---

# Analytics API

Comprehensive documentation for advanced analytics capabilities across user management, payment processing, and security monitoring.

## Overview

The Analytics API provides sophisticated querying capabilities for business intelligence, security analysis, and compliance reporting. All analytics endpoints require appropriate permissions and are subject to rate limiting.

## Base URLs
- User Analytics: `/api/v1/analytics/users`
- Payment Analytics: `/api/v1/analytics/payments`
- Security Analytics: `/api/v1/analytics/security`

## Authentication
All analytics endpoints require authentication with appropriate permissions:
```
Authorization: Bearer <access_token>
X-Organization-ID: <organization_uuid>
```

## User Analytics

### GET /analytics/users/growth
Analyze user growth patterns over time.

**Parameters:**
- `startDate` (ISO 8601): Start date for analysis
- `endDate` (ISO 8601): End date for analysis
- `period` (string): Aggregation period (`month`, `week`, `day`)

**Response:**
```json
{
  "data": [
    {
      "month": "2024-01-01T00:00:00Z",
      "userCount": 145
    },
    {
      "month": "2024-02-01T00:00:00Z",
      "userCount": 189
    }
  ],
  "summary": {
    "totalPeriods": 12,
    "averageGrowth": 15.2,
    "totalUsers": 1847
  }
}
```

---

### GET /analytics/users/retention
Calculate user retention rates between periods.

**Parameters:**
- `currentPeriodStart` (ISO 8601): Current period start
- `currentPeriodEnd` (ISO 8601): Current period end
- `previousPeriodStart` (ISO 8601): Previous period start
- `previousPeriodEnd` (ISO 8601): Previous period end

**Response:**
```json
{
  "retainedUsers": 234,
  "totalPreviousUsers": 298,
  "retentionRate": 78.52,
  "newUsers": 45,
  "churnedUsers": 64
}
```

---

### GET /analytics/users/authentication-methods
Analyze distribution of authentication methods.

**Response:**
```json
{
  "data": [
    {
      "provider": "google",
      "userCount": 456,
      "percentage": 67.4
    },
    {
      "provider": "github",
      "userCount": 123,
      "percentage": 18.2
    },
    {
      "provider": "password",
      "userCount": 98,
      "percentage": 14.4
    }
  ],
  "total": 677
}
```

---

### GET /analytics/users/activity-patterns
Analyze user activity patterns by time.

**Parameters:**
- `startDate` (ISO 8601): Analysis start date
- `endDate` (ISO 8601): Analysis end date

**Response:**
```json
{
  "data": [
    {
      "dayOfWeek": 1,
      "hourOfDay": 9,
      "activityCount": 234
    },
    {
      "dayOfWeek": 1,
      "hourOfDay": 10,
      "activityCount": 345
    }
  ],
  "peakHours": [
    {
      "hour": 9,
      "averageActivity": 287
    }
  ]
}
```

---

### GET /analytics/users/engagement
Score user engagement levels.

**Parameters:**
- `organizationId` (UUID): Organization to analyze
- `highlyActiveThreshold` (ISO 8601): Threshold for highly active users
- `moderatelyActiveThreshold` (ISO 8601): Threshold for moderately active users
- `lowActiveThreshold` (ISO 8601): Threshold for low activity users

**Response:**
```json
{
  "data": [
    {
      "userId": "user-uuid",
      "email": "user@example.com",
      "name": "John Doe",
      "engagementLevel": "highly_active",
      "lastActivity": "2024-01-15T10:30:00Z"
    }
  ],
  "summary": {
    "highlyActive": 45,
    "moderatelyActive": 123,
    "lowActive": 67,
    "inactive": 23
  }
}
```

---

### GET /analytics/users/churn-risk
Identify users at risk of churning.

**Parameters:**
- `churnThreshold` (ISO 8601): Inactivity threshold for churn risk
- `maxResults` (int): Maximum number of results (default: 100)

**Response:**
```json
{
  "data": [
    {
      "userId": "user-uuid",
      "email": "user@example.com",
      "name": "Jane Smith",
      "organization": "Example Corp",
      "lastActivity": "2023-12-01T15:20:00Z",
      "daysInactive": 45
    }
  ],
  "summary": {
    "totalAtRisk": 23,
    "averageDaysInactive": 32.5
  }
}
```

## Payment Analytics

### GET /analytics/payments/revenue
Analyze revenue patterns over time.

**Parameters:**
- `period` (string): Time period (`day`, `week`, `month`, `quarter`)
- `startDate` (ISO 8601): Analysis start date
- `endDate` (ISO 8601): Analysis end date

**Response:**
```json
{
  "data": [
    {
      "timePeriod": "2024-01-01T00:00:00Z",
      "paymentCount": 234,
      "totalRevenue": 45670.50,
      "averagePayment": 195.17,
      "uniqueOrganizations": 89
    }
  ],
  "summary": {
    "totalRevenue": 567890.25,
    "totalPayments": 2847,
    "averagePayment": 199.54,
    "growthRate": 12.5
  }
}
```

---

### GET /analytics/payments/status-distribution
Analyze payment success/failure rates.

**Parameters:**
- `startDate` (ISO 8601): Analysis start date
- `endDate` (ISO 8601): Analysis end date

**Response:**
```json
{
  "data": [
    {
      "status": "SUCCEEDED",
      "paymentCount": 1847,
      "percentage": 89.2,
      "averageAmount": 199.54
    },
    {
      "status": "FAILED",
      "paymentCount": 143,
      "percentage": 6.9,
      "averageAmount": 156.78
    },
    {
      "status": "CANCELLED",
      "paymentCount": 81,
      "percentage": 3.9,
      "averageAmount": 134.23
    }
  ]
}
```

---

### GET /analytics/payments/top-organizations
Find highest revenue-generating organizations.

**Parameters:**
- `startDate` (ISO 8601): Analysis start date
- `topN` (int): Number of top organizations to return (default: 10)

**Response:**
```json
{
  "data": [
    {
      "organizationName": "Tech Corp",
      "paymentCount": 456,
      "totalRevenue": 89234.50,
      "averagePayment": 195.69,
      "lastPaymentDate": "2024-01-15T14:30:00Z"
    }
  ],
  "summary": {
    "totalOrganizations": 234,
    "topNRevenue": 456789.25,
    "percentageOfTotal": 67.8
  }
}
```

---

### GET /analytics/payments/method-analysis
Analyze payment method usage and performance.

**Parameters:**
- `startDate` (ISO 8601): Analysis start date

**Response:**
```json
{
  "data": [
    {
      "paymentMethodType": "card",
      "paymentMethodBrand": "visa",
      "usageCount": 1234,
      "totalAmount": 234567.89,
      "averageAmount": 190.12
    },
    {
      "paymentMethodType": "card",
      "paymentMethodBrand": "mastercard",
      "usageCount": 567,
      "totalAmount": 123456.78,
      "averageAmount": 217.85
    }
  ]
}
```

---

### GET /analytics/payments/cohort-analysis
Analyze payment behavior by customer cohorts.

**Parameters:**
- `cohortStartDate` (ISO 8601): Start date for cohort analysis

**Response:**
```json
{
  "data": [
    {
      "cohortMonth": "2024-01-01T00:00:00Z",
      "monthNumber": 0,
      "activeOrganizations": 45,
      "revenue": 12345.67
    },
    {
      "cohortMonth": "2024-01-01T00:00:00Z",
      "monthNumber": 1,
      "activeOrganizations": 38,
      "revenue": 10987.43
    }
  ]
}
```

---

### GET /analytics/payments/churn-analysis
Identify organizations that have stopped making payments.

**Parameters:**
- `churnThreshold` (ISO 8601): Date threshold for considering churn

**Response:**
```json
{
  "data": [
    {
      "organizationName": "Example Corp",
      "lastPaymentDate": "2023-11-15T09:30:00Z",
      "totalPayments": 23,
      "totalSpent": 4567.89,
      "daysSinceLastPayment": 67
    }
  ],
  "summary": {
    "churnedOrganizations": 12,
    "lostRevenue": 45678.90,
    "averageDaysSincePayment": 45.6
  }
}
```

---

### GET /analytics/payments/fraud-detection
Detect suspicious payment patterns.

**Parameters:**
- `lookbackDate` (ISO 8601): How far back to analyze
- `failureThreshold` (int): Minimum failed attempts to flag

**Response:**
```json
{
  "data": [
    {
      "organizationId": "org-uuid",
      "failedAttempts": 15,
      "uniqueIntents": 8,
      "lastAttempt": "2024-01-15T16:45:00Z",
      "statuses": "FAILED, CANCELLED"
    }
  ],
  "summary": {
    "suspiciousOrganizations": 5,
    "totalFailedAttempts": 87,
    "riskScore": "HIGH"
  }
}
```

## Security Analytics

### GET /analytics/security/incident-trends
Analyze security incident patterns over time.

**Parameters:**
- `organizationId` (UUID): Organization to analyze
- `period` (string): Time period (`day`, `week`, `month`)
- `startDate` (ISO 8601): Analysis start date
- `endDate` (ISO 8601): Analysis end date

**Response:**
```json
{
  "data": [
    {
      "timePeriod": "2024-01-01T00:00:00Z",
      "action": "LOGIN_FAILED",
      "incidentCount": 23,
      "uniqueActors": 15,
      "uniqueIps": 12
    }
  ],
  "summary": {
    "totalIncidents": 156,
    "highRiskPeriods": ["2024-01-15T00:00:00Z"],
    "trendDirection": "increasing"
  }
}
```

---

### GET /analytics/security/user-anomalies
Detect unusual user behavior patterns.

**Parameters:**
- `organizationId` (UUID): Organization to analyze
- `lookbackDate` (ISO 8601): Analysis period start
- `activityThreshold` (int): Minimum activity count to flag
- `ipThreshold` (int): Minimum IP count to flag

**Response:**
```json
{
  "data": [
    {
      "actorId": "user-uuid",
      "totalActions": 567,
      "uniqueActions": 23,
      "uniqueIps": 8,
      "firstActivity": "2024-01-01T08:00:00Z",
      "lastActivity": "2024-01-15T18:30:00Z",
      "activitySpanHours": 342.5
    }
  ],
  "summary": {
    "anomalousUsers": 5,
    "averageRiskScore": 7.2
  }
}
```

---

### GET /analytics/security/risk-scores
Calculate risk scores for all users.

**Parameters:**
- `organizationId` (UUID): Organization to analyze
- `lookbackDate` (ISO 8601): Risk calculation period

**Response:**
```json
{
  "data": [
    {
      "userId": "user-uuid",
      "email": "user@example.com",
      "name": "John Doe",
      "riskScore": 15,
      "incidentCount": 5,
      "uniqueIpCount": 3
    }
  ],
  "summary": {
    "highRiskUsers": 3,
    "averageRiskScore": 2.4,
    "riskDistribution": {
      "low": 234,
      "medium": 23,
      "high": 3
    }
  }
}
```

---

### GET /analytics/security/failed-authentication
Analyze failed authentication patterns.

**Parameters:**
- `organizationId` (UUID): Organization to analyze
- `startDate` (ISO 8601): Analysis start date
- `failureThreshold` (int): Minimum failures to include

**Response:**
```json
{
  "data": [
    {
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0...",
      "failureCount": 25,
      "targetedUsers": 8,
      "firstAttempt": "2024-01-15T08:00:00Z",
      "lastAttempt": "2024-01-15T18:30:00Z",
      "failureReasons": "invalid_credentials; account_locked"
    }
  ],
  "summary": {
    "suspiciousSources": 5,
    "totalFailures": 156,
    "uniqueTargets": 23
  }
}
```

## Rate Limiting

Analytics endpoints have specific rate limits:
- **User Analytics**: 100 requests per hour per organization
- **Payment Analytics**: 50 requests per hour per organization
- **Security Analytics**: 200 requests per hour per organization

## Permissions

Analytics access requires specific permissions:
- `analytics:users:read` - User analytics access
- `analytics:payments:read` - Payment analytics access
- `analytics:security:read` - Security analytics access
- `analytics:admin` - Full analytics access

## Data Retention

Analytics data retention policies:
- **User Analytics**: 24 months
- **Payment Analytics**: 7 years (compliance)
- **Security Analytics**: 12 months
- **Aggregated Reports**: 5 years

## Export Formats

Analytics data can be exported in multiple formats:
- **JSON**: Default API response format
- **CSV**: Add `Accept: text/csv` header
- **Excel**: Add `Accept: application/vnd.ms-excel` header

Example CSV export:
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/users/growth?startDate=2024-01-01&endDate=2024-12-31" \
  -H "Authorization: Bearer your_token" \
  -H "Accept: text/csv" \
  -o user_growth.csv
```

## Error Handling

### Common Error Codes
- `INSUFFICIENT_PERMISSIONS` - Missing required analytics permissions
- `INVALID_DATE_RANGE` - Invalid or malformed date parameters
- `ORGANIZATION_NOT_FOUND` - Invalid organization ID
- `DATA_NOT_AVAILABLE` - Requested data not available for time period
- `RATE_LIMIT_EXCEEDED` - Too many analytics requests

### Error Response Format
```json
{
  "error": "INVALID_DATE_RANGE",
  "message": "Start date must be before end date",
  "correlationId": "analytics-req-123",
  "details": {
    "startDate": "2024-01-15",
    "endDate": "2024-01-01"
  }
}
```

## Performance Considerations

### Query Optimization
- Use appropriate date ranges to limit data size
- Leverage pagination for large result sets
- Consider caching for frequently accessed reports
- Use appropriate aggregation periods

### Best Practices
1. **Batch multiple queries** when possible
2. **Cache results** for repeated requests
3. **Use streaming** for large datasets
4. **Monitor query performance** and adjust parameters
5. **Implement retry logic** with exponential backoff

## Compliance

### Data Privacy
- Personal data is anonymized in analytics
- GDPR-compliant data handling
- User consent tracked for analytics

### Security
- All analytics queries are audited
- Access patterns monitored
- Sensitive data redacted in exports
- Role-based access control enforced