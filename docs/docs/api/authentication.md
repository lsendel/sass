---
sidebar_position: 1
title: Authentication API
---

# Authentication API

Comprehensive documentation for the enhanced authentication endpoints with advanced security features.

## Base URL
```
/api/v1/auth
```

## Security Features

### Enhanced Input Validation
All authentication endpoints now include:
- XSS prevention through input sanitization
- SQL injection protection
- Path traversal detection
- Security threat scanning

### Security Event Logging
All authentication activities are logged for:
- Successful and failed authentication attempts
- Suspicious activity detection
- Account lockout events
- Device fingerprinting

### Rate Limiting
Protection against brute force attacks:
- Per-IP rate limiting
- Exponential backoff for failed attempts
- Configurable thresholds per endpoint

## Endpoints

### GET /providers
List available OAuth2 providers.

**Response:**
```json
{
  "providers": [
    {
      "name": "google",
      "displayName": "Google",
      "enabled": true
    },
    {
      "name": "github",
      "displayName": "GitHub",
      "enabled": true
    }
  ]
}
```

**Security Features:**
- No authentication required
- Rate limited to prevent enumeration

---

### GET /methods
Get available authentication methods for the platform.

**Response:**
```json
{
  "methods": ["PASSWORD", "OAUTH2"],
  "passwordAuthEnabled": true,
  "oauth2Providers": ["google", "github"]
}
```

**Security Features:**
- No authentication required
- Configuration-based response

---

### GET /authorize
Initiate OAuth2 authorization with enhanced security validation.

**Parameters:**
- `provider` (string, required): OAuth2 provider name
- `redirect_uri` (string, required): Callback URI after authentication

**Enhanced Security:**
- Provider validation against enabled providers
- Enhanced redirect URI validation supporting dev/test domains
- Input sanitization for all parameters
- Security threat detection
- Suspicious activity logging

**Response:**
Redirects to OAuth2 provider authorization URL with state parameter.

**Error Responses:**
```json
{
  "error": "INVALID_PROVIDER",
  "message": "OAuth2 provider not supported or configured: invalid_provider"
}
```

```json
{
  "error": "SECURITY_THREAT",
  "message": "Security threat detected in request parameters"
}
```

---

### POST /callback
Handle OAuth2 callback with comprehensive security logging.

**Parameters:**
- `code` (string, required): Authorization code from provider
- `state` (string, optional): State parameter for CSRF protection

**Response:**
```json
{
  "accessToken": "secure_opaque_token_here",
  "user": {
    "id": "user-uuid",
    "email": "user@example.com",
    "name": "User Name"
  },
  "security": {
    "deviceFingerprint": "device_hash",
    "loginTime": 1234567890123
  }
}
```

**Security Features:**
- Token validation and sanitization
- Device fingerprinting for security monitoring
- Failed authentication logging
- Success event logging with IP tracking

---

### POST /password/login
Password-based authentication with enhanced security.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "user_password"
}
```

**Response:**
```json
{
  "accessToken": "secure_opaque_token_here",
  "user": {
    "id": "user-uuid",
    "email": "user@example.com",
    "name": "User Name"
  },
  "security": {
    "deviceFingerprint": "device_hash",
    "loginTime": 1234567890123
  }
}
```

**Security Features:**
- Email sanitization and validation
- Password authentication availability check
- Account lockout protection
- Failed attempt logging
- Device fingerprinting

**Error Responses:**
```json
{
  "error": "PASSWORD_AUTH_DISABLED",
  "message": "Password authentication is not enabled"
}
```

```json
{
  "error": "AUTHENTICATION_FAILED",
  "message": "Invalid credentials"
}
```

---

### GET /session
Get current user session information.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "user": {
    "id": "user-uuid",
    "email": "user@example.com",
    "name": "User Name",
    "role": "USER"
  },
  "organization": {
    "id": "org-uuid",
    "slug": "org-slug"
  },
  "session": {
    "activeTokens": 2,
    "lastActivity": "2024-01-01T12:00:00Z"
  }
}
```

**Security Features:**
- Token validation
- Session information retrieval
- Multi-tenant organization context

---

### POST /session/validate
Validate current session token.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "valid": true,
  "userId": "user-uuid",
  "expiresAt": "2024-01-01T12:00:00Z"
}
```

**Security Features:**
- Token validation
- Expiry checking
- Invalid token detection and logging

---

### POST /logout
Invalidate current session with comprehensive logging.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```
204 No Content
```

**Security Features:**
- Token revocation
- Logout success/failure logging
- Invalid session attempt detection

## Security Event Types

### Authentication Success
Logged for successful logins:
```json
{
  "eventType": "AUTH_SUCCESS",
  "subject": "user-uuid",
  "action": "OAUTH2|PASSWORD|LOGOUT",
  "ipAddress": "192.168.1.1",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Authentication Failure
Logged for failed authentication attempts:
```json
{
  "eventType": "AUTH_FAILURE",
  "subject": "user@example.com",
  "action": "OAUTH2|PASSWORD",
  "ipAddress": "192.168.1.1",
  "details": "Invalid credentials",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Suspicious Activity
Logged for security threats:
```json
{
  "eventType": "SUSPICIOUS_ACTIVITY",
  "subject": "unknown",
  "action": "security_threat_detected",
  "ipAddress": "192.168.1.1",
  "details": "Security threat detected in OAuth2 parameters",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## Device Fingerprinting

The API generates device fingerprints for security monitoring:

**Components:**
- User-Agent header
- Accept-Language header
- Accept-Encoding header
- Client IP address

**Usage:**
- Session correlation
- Anomaly detection
- Security monitoring
- Device tracking

## Rate Limiting

### Default Limits
- Authentication endpoints: 10 requests per minute per IP
- OAuth flows: 5 requests per minute per IP
- Session validation: 100 requests per minute per IP

### Rate Limit Headers
```
X-RateLimit-Limit: 10
X-RateLimit-Window: 60
Retry-After: 60
```

### Rate Limit Exceeded Response
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many authentication attempts. Please try again later.",
  "retryAfter": 60
}
```

## Error Handling

### Common Error Codes
- `INVALID_PROVIDER` - OAuth2 provider not supported
- `INVALID_REDIRECT_URI` - Invalid callback URI
- `SECURITY_THREAT` - Security threat detected
- `VALIDATION_ERROR` - Input validation failed
- `AUTHENTICATION_FAILED` - Invalid credentials
- `PASSWORD_AUTH_DISABLED` - Password auth not enabled
- `RATE_LIMIT_EXCEEDED` - Too many requests
- `UNAUTHORIZED` - Invalid or expired token
- `SESSION_ERROR` - Session retrieval failed

### Error Response Format
```json
{
  "error": "ERROR_CODE",
  "message": "Human readable error message",
  "correlationId": "request-correlation-id"
}
```

## Testing

### Using curl

**OAuth2 Authorization:**
```bash
curl -X GET "http://localhost:8080/api/v1/auth/authorize?provider=google&redirect_uri=http://localhost:3000/callback"
```

**Password Login:**
```bash
curl -X POST "http://localhost:8080/api/v1/auth/password/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

**Session Validation:**
```bash
curl -X POST "http://localhost:8080/api/v1/auth/session/validate" \
  -H "Authorization: Bearer your_token_here"
```

## Security Best Practices

### For Client Applications
1. **Always use HTTPS** in production
2. **Store tokens securely** (httpOnly cookies recommended)
3. **Implement token refresh** before expiration
4. **Validate server certificates**
5. **Handle rate limiting gracefully**

### For Administrators
1. **Monitor authentication logs** regularly
2. **Set up alerts** for suspicious activity
3. **Review failed authentication patterns**
4. **Implement IP allowlisting** for admin accounts
5. **Regularly audit OAuth2 providers**

## Compliance

### GDPR Compliance
- All authentication events are logged with retention policies
- PII is redacted in security logs
- User consent tracked for data processing
- Right to erasure supported

### Security Standards
- OWASP Top 10 compliance
- OAuth2/PKCE implementation
- Secure session management
- Input validation and sanitization
- Rate limiting and DDoS protection