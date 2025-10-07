# SASS API Documentation

## Overview

The SASS (Spring Boot Application with Security System) provides a comprehensive RESTful API for managing users, organizations, subscriptions, and payments. The API follows REST principles and uses JSON for request/response payloads.

## Base URL

- **Development**: `http://localhost:8082`
- **Production**: `https://api.yourdomain.com`

## Authentication

The API uses session-based authentication with OAuth2. After successful authentication, a session cookie is provided for subsequent requests.

### Authentication Endpoints

#### Register User

```http
POST /auth/register
Content-Type: application/json

{
  \"email\": \"user@example.com\",
  \"password\": \"SecurePassword123!\",
  \"displayName\": \"John Doe\",
  \"organizationId\": \"123e4567-e89b-12d3-a456-426614174000\"
}
```

**Response (201 Created):**

```json
{
  \"success\": true,
  \"user\": {
    \"id\": \"123e4567-e89b-12d3-a456-426614174000\",
    \"email\": \"user@example.com\",
    \"displayName\": \"John Doe\",
    \"emailVerified\": false
  }
}
```

#### Login User

```http
POST /auth/login
Content-Type: application/json

{
  \"email\": \"user@example.com\",
  \"password\": \"SecurePassword123!\"
}
```

**Response (200 OK):**

```json
{
  \"success\": true,
  \"user\": {
    \"id\": \"123e4567-e89b-12d3-a456-426614174000\",
    \"email\": \"user@example.com\",
    \"displayName\": \"John Doe\"
  }
}
```

#### OAuth2 Login

```http
GET /oauth2/authorization/{provider}
```

Redirects to the specified OAuth2 provider for authentication. Supported providers: `google`, `github`.

#### Logout

```http
POST /auth/logout
```

**Response (200 OK):**

```json
{
  \"success\": true,
  \"message\": \"Successfully logged out\"
}
```

#### Get Current User

```http
GET /auth/me
```

**Response (200 OK):**

```json
{
  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",
  \"email\": \"user@example.com\",
  \"displayName\": \"John Doe\",
  \"emailVerified\": true,
  \"organizationId\": \"123e4567-e89b-12d3-a456-426614174001\"
}
```

## User Management

### Get User Profile

```http
GET /api/v1/users/{id}
```

**Response (200 OK):**

```json
{
  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",
  \"email\": \"user@example.com\",
  \"displayName\": \"John Doe\",
  \"emailVerified\": true,
  \"createdAt\": \"2023-01-01T00:00:00Z\",
  \"updatedAt\": \"2023-01-02T00:00:00Z\"
}
```

### Update User Profile

```http
PUT /api/v1/users/{id}
Content-Type: application/json

{
  \"displayName\": \"John Smith\",
  \"email\": \"john.smith@example.com\"
}
```

**Response (200 OK):**

```json
{
  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",
  \"email\": \"john.smith@example.com\",
  \"displayName\": \"John Smith\",
  \"emailVerified\": true
}
```

## Organization Management

### Get User's Organizations

```http
GET /api/v1/organizations
```

**Response (200 OK):**

```json
{
  \"organizations\": [
    {
      \"id\": \"123e4567-e89b-12d3-a456-426614174001\",
      \"name\": \"Acme Corp\",
      \"domain\": \"acme.com\",
      \"createdAt\": \"2023-01-01T00:00:00Z\"
    }
  ]
}
```

### Create Organization

```http
POST /api/v1/organizations
Content-Type: application/json

{
  \"name\": \"New Organization\",
  \"domain\": \"neworg.com\"
}
```

**Response (201 Created):**

```json
{
  \"id\": \"123e4567-e89b-12d3-a456-426614174002\",
  \"name\": \"New Organization\",
  \"domain\": \"neworg.com\",
  \"ownerId\": \"123e4567-e89b-12d3-a456-426614174000\",
  \"createdAt\": \"2023-01-02T00:00:00Z\"
}
```

### Get Organization Details

```http
GET /api/v1/organizations/{id}
```

**Response (200 OK):**

```json
{
  \"id\": \"123e4567-e89b-12d3-a456-426614174001\",
  \"name\": \"Acme Corp\",
  \"domain\": \"acme.com\",
  \"ownerId\": \"123e4567-e89b-12d3-a456-426614174000\",
  \"createdAt\": \"2023-01-01T00:00:00Z\",
  \"members\": [
    {
      \"id\": \"123e4567-e89b-12d3-a456-426614174000\",
      \"email\": \"user@example.com\",
      \"displayName\": \"John Doe\",
      \"role\": \"OWNER\"
    }
  ]
}
```

## Subscription Management

### Get Available Plans

```http
GET /api/v1/plans
```

**Response (200 OK):**

```json
{
  \"plans\": [
    {
      \"id\": \"plan_starter_123\",
      \"name\": \"Starter Plan\",
      \"description\": \"Perfect for small teams\",
      \"price\": {
        \"amount\": 999,
        \"currency\": \"usd\",
        \"interval\": \"month\"
      },
      \"features\": [\"Up to 5 users\", \"Basic support\"],
      \"stripePriceId\": \"price_starter_123\"
    }
  ]
}
```

### Get User's Subscription

```http
GET /api/v1/subscriptions
```

**Response (200 OK):**

```json
{
  \"id\": \"sub_abc123\",
  \"status\": \"active\",
  \"currentPeriodStart\": \"2023-01-01T00:00:00Z\",
  \"currentPeriodEnd\": \"2023-02-01T00:00:00Z\",
  \"plan\": {
    \"id\": \"plan_starter_123\",
    \"name\": \"Starter Plan\",
    \"price\": {
      \"amount\": 999,
      \"currency\": \"usd\"
    }
  },
  \"organizationId\": \"123e4567-e89b-12d3-a456-426614174001\"
}
```

### Create Subscription

```http
POST /api/v1/subscriptions
Content-Type: application/json

{
  \"planId\": \"plan_starter_123\",
  \"organizationId\": \"123e4567-e89b-12d3-a456-426614174001\"
}
```

**Response (201 Created):**

```json
{
  \"id\": \"sub_def456\",
  \"status\": \"active\",
  \"currentPeriodStart\": \"2023-01-01T00:00:00Z\",
  \"currentPeriodEnd\": \"2023-02-01T00:00:00Z\",
  \"planId\": \"plan_starter_123\",
  \"organizationId\": \"123e4567-e89b-12d3-a456-426614174001\"
}
```

### Cancel Subscription

```http
DELETE /api/v1/subscriptions/{id}
```

**Response (200 OK):**

```json
{
  \"id\": \"sub_def456\",
  \"status\": \"canceled\",
  \"canceledAt\": \"2023-01-15T00:00:00Z\"
}
```

## Payment Management

### Create Payment Intent

```http
POST /api/v1/payments/intents
Content-Type: application/json

{
  \"amount\": 999,
  \"currency\": \"usd\",
  \"organizationId\": \"123e4567-e89b-12d3-a456-426614174001\",
  \"description\": \"Subscription payment\"
}
```

**Response (201 Created):**

```json
{
  \"id\": \"pi_abc123\",
  \"clientSecret\": \"pi_abc123_secret_def456\",
  \"amount\": 999,
  \"currency\": \"usd\",
  \"status\": \"requires_payment_method\"
}
```

### Get Payment Intent

```http
GET /api/v1/payments/intents/{id}
```

**Response (200 OK):**

```json
{
  \"id\": \"pi_abc123\",
  \"amount\": 999,
  \"currency\": \"usd\",
  \"status\": \"succeeded\",
  \"created\": \"2023-01-01T00:00:00Z\",
  \"organizationId\": \"123e4567-e89b-12d3-a456-426614174001\"
}
```

### List Organization Payments

```http
GET /api/v1/payments?organizationId={organizationId}
```

**Response (200 OK):**

```json
{
  \"payments\": [
    {
      \"id\": \"pi_abc123\",
      \"amount\": 999,
      \"currency\": \"usd\",
      \"status\": \"succeeded\",
      \"created\": \"2023-01-01T00:00:00Z\",
      \"description\": \"Subscription payment\"
    }
  ]
}
```

## Webhook Endpoints

### Stripe Webhook Handler

```http
POST /webhooks/stripe
Content-Type: application/json
```

Handles Stripe webhook events for payment confirmation, subscription updates, etc. This endpoint should be configured in your Stripe dashboard.

## Error Handling

The API uses standard HTTP status codes:

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

### Error Response Format

```json
{
  \"error\": {
    \"code\": \"VALIDATION_ERROR\",
    \"message\": \"Email is required\",
    \"details\": [
      {
        \"field\": \"email\",
        \"message\": \"Email cannot be empty\"
      }
    ]
  }
}
```

## Rate Limiting

The API implements rate limiting to prevent abuse:

- Authenticated requests: 1000 requests per hour per user
- Unauthenticated requests: 100 requests per hour per IP
- Burst limit: 10 requests per second

## Security Headers

All API responses include security headers:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`

## API Versioning

The API uses URI versioning. Current version is v1. Future breaking changes will increment the version number (e.g., `/api/v2/`).

## Testing the API

### Using cURL

```bash
# Get current user
curl -b cookies.txt -c cookies.txt http://localhost:8082/auth/me

# Create an organization
curl -b cookies.txt -c cookies.txt -X POST \\
  -H \"Content-Type: application/json\" \\
  -d '{\"name\":\"Test Org\",\"domain\":\"test.com\"}' \\
  http://localhost:8082/api/v1/organizations
```

### Using the Interactive Documentation

Visit `http://localhost:8082/swagger-ui.html` to interactively test the API endpoints.

## SDK Examples

### JavaScript/TypeScript

```javascript
// Using the frontend's API client
import { userApi } from "./api/userApi";

try {
  const user = await userApi.getUserProfile("user-id");
  console.log("User:", user);
} catch (error) {
  console.error("Error fetching user:", error);
}
```

### Java (Backend-to-Backend)

```java
// Using RestTemplate or WebClient
@Service
public class ApiClientService {

    @Autowired
    private WebClient webClient;

    public User getUser(String userId) {
        return webClient
            .get()
            .uri(\"/api/v1/users/{id}\", userId)
            .retrieve()
            .bodyToMono(User.class)
            .block();
    }
}
```

## Monitoring and Health Checks

### Health Check Endpoint

```http
GET /actuator/health
```

### Metrics Endpoint

```http
GET /actuator/metrics
```

For more information on monitoring, see the [Deployment Guide](./DEPLOYMENT.md).

## Support

For API questions or issues:

- Check the [Troubleshooting Guide](./TROUBLESHOOTING.md)
- Search the [Issue Tracker](https://github.com/your-organization/sass/issues)
- Contact the development team
