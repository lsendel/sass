# Payment Platform API Documentation

## Overview

The Payment Platform provides a comprehensive REST API for managing payments, subscriptions, organizations, and user authentication. The API follows RESTful principles and uses JSON for data exchange.

## Base URL

- **Development**: `http://localhost:8082`
- **Production**: `https://api.paymentplatform.com`

## Interactive Documentation

- **Swagger UI**: `http://localhost:8082/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8082/api-docs`

## Authentication

The API uses session-based authentication with cookies. All protected endpoints require a valid session.

### Session Cookie
- **Name**: `SESSION`
- **Type**: HttpOnly, Secure (in production)
- **Expiration**: 24 hours

## API Groups

### 1. Authentication APIs (`/auth/**`)

#### Register User
```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "displayName": "John Doe",
  "organizationId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "displayName": "John Doe",
    "emailVerified": false
  },
  "organization": {
    "id": "123e4567-e89b-12d3-a456-426614174001",
    "name": "John's Organization"
  }
}
```

#### Login User
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "organizationId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "displayName": "John Doe"
  },
  "organization": {
    "id": "123e4567-e89b-12d3-a456-426614174001",
    "name": "John's Organization"
  }
}
```

### 2. Payment APIs (`/api/v1/payments/**`)

#### Create Payment Intent
```http
POST /api/v1/payments/intents
Content-Type: application/json
Cookie: SESSION=<session-cookie>

{
  "organizationId": "123e4567-e89b-12d3-a456-426614174000",
  "amount": 2000,
  "currency": "usd",
  "description": "Payment for services"
}
```

**Response (201 Created):**
```json
{
  "id": "pi_1234567890",
  "clientSecret": "pi_1234567890_secret_abc123",
  "amount": 2000,
  "currency": "usd",
  "status": "requires_payment_method"
}
```

#### Get Payment History
```http
GET /api/v1/payments/history?organizationId=123e4567-e89b-12d3-a456-426614174000
Cookie: SESSION=<session-cookie>
```

### 3. Subscription APIs (`/api/v1/subscriptions/**`)

#### Create Subscription
```http
POST /api/v1/subscriptions
Content-Type: application/json
Cookie: SESSION=<session-cookie>

{
  "organizationId": "123e4567-e89b-12d3-a456-426614174000",
  "planId": "plan_basic_monthly",
  "paymentMethodId": "pm_1234567890"
}
```

#### Get Organization Subscription
```http
GET /api/v1/subscriptions/organization/123e4567-e89b-12d3-a456-426614174000
Cookie: SESSION=<session-cookie>
```

### 4. Organization APIs (`/api/v1/organizations/**`)

#### Get User Organizations
```http
GET /api/v1/organizations
Cookie: SESSION=<session-cookie>
```

#### Create Organization
```http
POST /api/v1/organizations
Content-Type: application/json
Cookie: SESSION=<session-cookie>

{
  "name": "My New Organization",
  "description": "Organization description"
}
```

## Error Handling

The API uses standard HTTP status codes and returns error details in JSON format.

### Error Response Format
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request data",
    "details": [
      {
        "field": "email",
        "message": "Email is required"
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/auth/register"
}
```

### Common Status Codes

- **200 OK**: Request successful
- **201 Created**: Resource created successfully
- **400 Bad Request**: Invalid request data
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Access denied
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource already exists
- **422 Unprocessable Entity**: Validation error
- **500 Internal Server Error**: Server error

## Rate Limiting

API endpoints are rate-limited to prevent abuse:

- **Authentication endpoints**: 5 requests per minute per IP
- **Payment endpoints**: 100 requests per minute per user
- **General endpoints**: 1000 requests per hour per user

## Webhooks

The platform supports webhooks for real-time event notifications.

### Stripe Webhooks
```http
POST /api/v1/webhooks/stripe
Content-Type: application/json
Stripe-Signature: <webhook-signature>
```

### Supported Events
- `payment_intent.succeeded`
- `payment_intent.payment_failed`
- `invoice.payment_succeeded`
- `invoice.payment_failed`
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`

## SDK and Examples

### cURL Examples

#### Register and Login Flow
```bash
# Register new user
curl -X POST http://localhost:8082/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePassword123!",
    "displayName": "Test User",
    "organizationId": "123e4567-e89b-12d3-a456-426614174000"
  }'

# Login user
curl -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "email": "test@example.com",
    "password": "SecurePassword123!",
    "organizationId": "123e4567-e89b-12d3-a456-426614174000"
  }'

# Create payment intent (using session cookie)
curl -X POST http://localhost:8082/api/v1/payments/intents \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "organizationId": "123e4567-e89b-12d3-a456-426614174000",
    "amount": 2000,
    "currency": "usd"
  }'
```

### JavaScript/TypeScript Example
```typescript
// API client configuration
const API_BASE_URL = 'http://localhost:8082';

// Login function
async function login(email: string, password: string, organizationId: string) {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include', // Include cookies
    body: JSON.stringify({ email, password, organizationId }),
  });
  
  if (!response.ok) {
    throw new Error('Login failed');
  }
  
  return response.json();
}

// Create payment intent
async function createPaymentIntent(organizationId: string, amount: number) {
  const response = await fetch(`${API_BASE_URL}/api/v1/payments/intents`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify({
      organizationId,
      amount,
      currency: 'usd',
    }),
  });
  
  if (!response.ok) {
    throw new Error('Payment intent creation failed');
  }
  
  return response.json();
}
```

## Testing

### Test Environment
- **Base URL**: `http://localhost:8082`
- **Test Database**: H2 in-memory database
- **Test Stripe Keys**: Use Stripe test keys for payment testing

### Test Data
The test environment includes sample data for testing:
- Test organizations
- Sample payment methods
- Mock subscription plans

## Support

For API support and questions:
- **Documentation**: This document and Swagger UI
- **Email**: support@paymentplatform.com
- **GitHub Issues**: [Repository Issues](https://github.com/platform/payment-platform/issues)

## Changelog

### Version 1.0.0
- Initial API release
- Authentication endpoints
- Payment processing
- Subscription management
- Organization management
- Comprehensive OpenAPI documentation
