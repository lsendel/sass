# API Reference - Spring Boot Modulith Payment Platform

## Overview

This document provides comprehensive API reference for the Spring Boot Modulith Payment Platform. All endpoints are documented with actual implementation details, request/response schemas, and authentication requirements.

## Base URLs

- **Development**: `http://localhost:8080`
- **Production**: `https://api.paymentplatform.com`

## Authentication

The platform uses session-based authentication with secure HTTP-only cookies. OAuth2/PKCE flow is supported for external providers.

### Security Requirements
- All authenticated endpoints require a valid session
- Organization-specific endpoints require tenant authorization
- Admin endpoints require `ADMIN` role

## API Endpoints

### Authentication Module

#### OAuth2 Authentication

##### GET /auth/oauth2/providers
List available OAuth2 providers

**Authentication**: None required

**Response**: 200 OK
```json
{
  "providers": [
    {
      "name": "google",
      "displayName": "Google",
      "authorizationUrl": "https://accounts.google.com/oauth/authorize",
      "scopes": ["openid", "email", "profile"]
    }
  ],
  "count": 1,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

##### GET /auth/oauth2/authorize/{provider}
Initiate OAuth2 authorization flow with PKCE

**Authentication**: None required

**Parameters**:
- `provider` (path): OAuth2 provider name (google, github, microsoft)
- `redirect_uri` (query, optional): Custom redirect URI

**Response**: 200 OK
```json
{
  "authorizationUrl": "https://accounts.google.com/oauth/authorize?...",
  "state": "uuid-state-value",
  "codeChallenge": "base64-encoded-challenge",
  "codeChallengeMethod": "S256",
  "provider": "google",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

##### GET /auth/oauth2/callback/{provider}
Handle OAuth2 authorization callback

**Authentication**: Automatic via OAuth2 flow

**Parameters**:
- `provider` (path): OAuth2 provider name
- `code` (query): Authorization code from provider
- `state` (query): State parameter for CSRF protection
- `error` (query, optional): Error code if authorization failed

**Response**: 200 OK (Success)
```json
{
  "success": true,
  "session": {
    "sessionId": "session-uuid",
    "userId": "user-uuid",
    "provider": "google",
    "isAuthenticated": true,
    "expiresAt": "2025-01-15T11:30:00Z",
    "userInfo": {
      "sub": "provider-user-id",
      "email": "user@example.com",
      "name": "John Doe",
      "picture": "https://example.com/avatar.jpg",
      "email_verified": true,
      "provider": "google"
    }
  },
  "redirectTo": "http://localhost:3000/dashboard",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

##### GET /auth/oauth2/session
Get current OAuth2 session information

**Authentication**: Session required

**Response**: 200 OK (Authenticated)
```json
{
  "isAuthenticated": true,
  "session": {
    "sessionId": "session-uuid",
    "userId": "user-uuid",
    "provider": "google",
    "isValid": true,
    "expiresAt": "2025-01-15T11:30:00Z",
    "lastAccessedAt": "2025-01-15T10:30:00Z",
    "sessionDurationSeconds": 3600,
    "timeToExpirationSeconds": 1800,
    "userInfo": {
      "sub": "provider-user-id",
      "email": "user@example.com",
      "name": "John Doe",
      "picture": "https://example.com/avatar.jpg",
      "email_verified": true,
      "provider": "google"
    }
  },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

**Response**: 200 OK (Not Authenticated)
```json
{
  "isAuthenticated": false,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

##### POST /auth/oauth2/logout
Terminate OAuth2 session

**Authentication**: Session required

**Request Body** (Optional):
```json
{
  "terminateProviderSession": false
}
```

**Response**: 200 OK
```json
{
  "success": true,
  "message": "Logout successful",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### Payment Module

#### Payment Processing

##### POST /api/v1/payments/intents
Create payment intent

**Authentication**: Session required, organization access required

**Request Body**:
```json
{
  "organizationId": "org-uuid",
  "amount": 20.00,
  "currency": "usd",
  "description": "Subscription payment"
}
```

**Response**: 201 Created
```json
{
  "id": "pi_1234567890",
  "clientSecret": "pi_1234567890_secret_abc123",
  "amount": 20.00,
  "currency": "usd",
  "status": "requires_payment_method"
}
```

##### POST /api/v1/payments/intents/{paymentIntentId}/confirm
Confirm payment intent

**Authentication**: Session required

**Parameters**:
- `paymentIntentId` (path): Stripe payment intent ID

**Request Body**:
```json
{
  "paymentMethodId": "pm_1234567890"
}
```

**Response**: 200 OK
```json
{
  "id": "pi_1234567890",
  "clientSecret": "pi_1234567890_secret_abc123",
  "amount": 20.00,
  "currency": "usd",
  "status": "succeeded"
}
```

##### GET /api/v1/payments/organizations/{organizationId}
Get organization payments

**Authentication**: Session required, organization access required

**Parameters**:
- `organizationId` (path): Organization UUID

**Response**: 200 OK
```json
[
  {
    "id": "payment-uuid",
    "paymentIntentId": "pi_1234567890",
    "amount": 20.00,
    "currency": "usd",
    "status": "succeeded",
    "createdAt": "2025-01-15T10:30:00Z"
  }
]
```

##### GET /api/v1/payments/organizations/{organizationId}/status/{status}
Get organization payments by status

**Authentication**: Session required, organization access required

**Parameters**:
- `organizationId` (path): Organization UUID
- `status` (path): Payment status (succeeded, failed, pending)

**Response**: 200 OK
```json
[
  {
    "id": "payment-uuid",
    "paymentIntentId": "pi_1234567890",
    "amount": 20.00,
    "currency": "usd",
    "status": "succeeded",
    "createdAt": "2025-01-15T10:30:00Z"
  }
]
```

##### GET /api/v1/payments/organizations/{organizationId}/statistics
Get payment statistics

**Authentication**: Session required, organization access required

**Parameters**:
- `organizationId` (path): Organization UUID

**Response**: 200 OK
```json
{
  "totalPayments": 150,
  "successfulPayments": 145,
  "failedPayments": 5,
  "totalAmount": 3000.00,
  "currency": "usd",
  "averageAmount": 20.00,
  "lastPaymentDate": "2025-01-15T10:30:00Z"
}
```

#### Payment Methods

##### POST /api/v1/payments/methods
Attach payment method

**Authentication**: Session required, organization access required

**Request Body**:
```json
{
  "organizationId": "org-uuid",
  "stripePaymentMethodId": "pm_1234567890"
}
```

**Response**: 201 Created
```json
{
  "id": "method-uuid",
  "stripePaymentMethodId": "pm_1234567890",
  "type": "card",
  "brand": "visa",
  "last4": "4242",
  "isDefault": false
}
```

##### DELETE /api/v1/payments/methods/{paymentMethodId}
Detach payment method

**Authentication**: Session required, organization access required

**Parameters**:
- `paymentMethodId` (path): Payment method UUID
- `organizationId` (query): Organization UUID

**Response**: 204 No Content

##### PUT /api/v1/payments/methods/{paymentMethodId}/default
Set default payment method

**Authentication**: Session required, organization access required

**Parameters**:
- `paymentMethodId` (path): Payment method UUID
- `organizationId` (query): Organization UUID

**Response**: 200 OK
```json
{
  "id": "method-uuid",
  "stripePaymentMethodId": "pm_1234567890",
  "type": "card",
  "brand": "visa",
  "last4": "4242",
  "isDefault": true
}
```

##### GET /api/v1/payments/methods/organizations/{organizationId}
Get organization payment methods

**Authentication**: Session required, organization access required

**Parameters**:
- `organizationId` (path): Organization UUID

**Response**: 200 OK
```json
[
  {
    "id": "method-uuid",
    "stripePaymentMethodId": "pm_1234567890",
    "type": "card",
    "brand": "visa",
    "last4": "4242",
    "isDefault": true
  }
]
```

### Testing Endpoints

#### Payment Testing

##### GET /api/v1/payments/{paymentIntentId}/status
Get payment status (testing)

**Authentication**: Session required

**Parameters**:
- `paymentIntentId` (path): Payment intent ID

**Response**: 200 OK
```json
{
  "paymentIntentId": "pi_1234567890",
  "status": "succeeded"
}
```

##### POST /api/v1/payments
Create payment (testing - returns error)

**Authentication**: Session required

**Request Body**:
```json
{
  "amount": -10
}
```

**Response**: 400 Bad Request
```json
{
  "error": "INVALID_AMOUNT",
  "message": "Invalid amount specified"
}
```

##### GET /api/v1/payments/history
Get payment history (testing)

**Authentication**: Session required

**Parameters**:
- `page` (query, optional): Page number (default: 0)
- `size` (query, optional): Page size (default: 20)

**Response**: 200 OK
```json
{
  "content": [],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 0
}
```

##### POST /api/v1/payments/customers
Create customer (testing)

**Authentication**: Session required

**Request Body**:
```json
{
  "email": "test@example.com"
}
```

**Response**: 200 OK
```json
{
  "customerId": "cust_test123",
  "email": "test@example.com"
}
```

##### POST /api/v1/payments/{paymentIntentId}/refund
Create refund (admin only)

**Authentication**: Session required, ADMIN role required

**Parameters**:
- `paymentIntentId` (path): Payment intent ID

**Request Body**:
```json
{
  "amount": 10.00
}
```

**Response**: 200 OK
```json
{
  "refundId": "re_test123",
  "amount": 10.00
}
```

##### POST /api/v1/payments/webhook
Process Stripe webhook

**Authentication**: Stripe signature verification

**Headers**:
- `Stripe-Signature`: Webhook signature

**Request Body**: Stripe webhook payload

**Response**: 200 OK

## Error Responses

All API endpoints return consistent error responses:

### 400 Bad Request
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request data"
  }
}
```

### 401 Unauthorized
```json
{
  "error": {
    "code": "AUTHENTICATION_REQUIRED",
    "message": "Authentication required"
  }
}
```

### 403 Forbidden
```json
{
  "error": {
    "code": "ACCESS_DENIED",
    "message": "Insufficient permissions"
  }
}
```

### 404 Not Found
```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Requested resource not found"
  }
}
```

### 500 Internal Server Error
```json
{
  "error": {
    "code": "INTERNAL_ERROR",
    "message": "Internal server error"
  }
}
```

## OpenAPI Documentation

Interactive API documentation is available at:
- **Development**: `http://localhost:8080/swagger-ui.html`
- **Production**: `https://api.paymentplatform.com/swagger-ui.html`

## Rate Limiting

Rate limiting is applied to sensitive endpoints:
- **Authentication endpoints**: 5 requests per minute per IP
- **Payment endpoints**: 100 requests per minute per organization

## Security Headers

All responses include security headers:
- `Content-Security-Policy`
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`

## CORS Configuration

CORS is configured for:
- **Development**: `http://localhost:3000`
- **Production**: Configured frontend domain
- **Credentials**: Allowed for session cookies

## Versioning

API versioning follows the pattern `/api/v{version}/` where version is the major version number.

Current version: **v1**

## Support

For API support and issues:
- Check endpoint documentation above
- Review OpenAPI specification
- Check application logs for detailed error information
- Contact development team for implementation questions