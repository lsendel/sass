# Quickstart: Password Authentication

**Feature**: Alternative Password Authentication
**Duration**: 15 minutes
**Prerequisites**: Running Spring Boot Modulith payment platform

## Overview

This guide demonstrates the password authentication feature that works alongside OAuth2. The feature is controlled by an environment flag and provides traditional email/password login when enabled.

## Setup

### 1. Enable Password Authentication

Add to your `.env` file or environment variables:

```bash
# Enable password authentication feature
ENABLE_PASSWORD_AUTH=true

# Password policy configuration (optional, these are defaults)
PASSWORD_MIN_LENGTH=12
PASSWORD_REQUIRE_UPPERCASE=true
PASSWORD_REQUIRE_LOWERCASE=true
PASSWORD_REQUIRE_NUMBERS=true
PASSWORD_REQUIRE_SYMBOLS=true

# Email configuration (required for password reset)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
```

### 2. Run Database Migration

Apply the database changes to support password authentication:

```bash
cd backend
./gradlew flywayMigrate
```

### 3. Start the Application

```bash
# Backend
cd backend
./gradlew bootRun --args='--spring.profiles.active=development'

# Frontend
cd frontend
npm run dev
```

## Feature Validation Scenarios

### Scenario 1: Check Authentication Methods Available

**When password auth is disabled (default)**:
```bash
curl http://localhost:8080/api/v1/auth/methods

# Expected response:
{
  "methods": ["OAUTH2"],
  "passwordAuthEnabled": false,
  "oauth2Providers": ["google", "github", "microsoft"]
}
```

**When password auth is enabled**:
```bash
# Set ENABLE_PASSWORD_AUTH=true and restart
curl http://localhost:8080/api/v1/auth/methods

# Expected response:
{
  "methods": ["OAUTH2", "PASSWORD"],
  "passwordAuthEnabled": true,
  "oauth2Providers": ["google", "github", "microsoft"]
}
```

### Scenario 2: Register with Password

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecureP@ssw0rd123!",
    "confirmPassword": "SecureP@ssw0rd123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# Expected response:
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "test@example.com",
  "message": "Account created successfully. Please check your email to verify your account."
}
```

### Scenario 3: Verify Email

Click the link in the verification email or:

```bash
curl -X POST http://localhost:8080/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "token": "verification-token-from-email"
  }'

# Expected response:
{
  "message": "Email verified successfully. You can now log in."
}
```

### Scenario 4: Login with Password

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "SecureP@ssw0rd123!"
  }'

# Expected response:
{
  "token": "sesh_abc123xyz789...",
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "test@example.com",
    "emailVerified": true,
    "firstName": "Test",
    "lastName": "User",
    "authenticationMethods": ["PASSWORD"]
  },
  "expiresAt": "2025-09-17T10:00:00Z"
}
```

### Scenario 5: Test Failed Login (Rate Limiting)

```bash
# Attempt login with wrong password 5 times
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "username": "test@example.com",
      "password": "WrongPassword!"
    }'
done

# 6th attempt should be locked:
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "SecureP@ssw0rd123!"
  }'

# Expected response:
{
  "error": "ACCOUNT_LOCKED",
  "message": "Account temporarily locked due to too many failed attempts. Try again in 1 minute.",
  "timestamp": "2025-09-16T10:00:00Z"
}
```

### Scenario 6: Password Reset Flow

**Request password reset**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/request-password-reset \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'

# Expected response:
{
  "message": "If an account exists with this email, a password reset link has been sent."
}
```

**Reset password with token**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "reset-token-from-email",
    "password": "NewSecureP@ssw0rd456!",
    "confirmPassword": "NewSecureP@ssw0rd456!"
  }'

# Expected response:
{
  "message": "Password reset successfully. You can now log in with your new password."
}
```

### Scenario 7: Change Password (Authenticated)

```bash
curl -X POST http://localhost:8080/api/v1/auth/change-password \
  -H "Authorization: Bearer sesh_abc123xyz789..." \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "SecureP@ssw0rd123!",
    "newPassword": "EvenMoreSecure@789!",
    "confirmPassword": "EvenMoreSecure@789!"
  }'

# Expected response:
{
  "message": "Password changed successfully."
}
```

### Scenario 8: OAuth Still Works

Navigate to http://localhost:3000/login and verify:
1. Both "Login with Email" and OAuth provider buttons are visible
2. OAuth login still works as before
3. Users can switch between authentication methods

## Frontend Validation

### 1. Login Page UI
- Navigate to http://localhost:3000/login
- When `ENABLE_PASSWORD_AUTH=false`: Only OAuth buttons visible
- When `ENABLE_PASSWORD_AUTH=true`: Both email/password form and OAuth buttons visible

### 2. Registration Flow
- Click "Sign up" link when password auth enabled
- Fill registration form with valid password
- Check email for verification link
- Verify account and log in

### 3. Password Requirements Display
- Enter weak password in registration
- See real-time validation feedback:
  - ✓ At least 12 characters
  - ✓ Contains uppercase letter
  - ✓ Contains lowercase letter
  - ✓ Contains number
  - ✓ Contains special character

## Testing Password Policy Violations

### Test Weak Password
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "weak@example.com",
    "password": "weak",
    "confirmPassword": "weak"
  }'

# Expected response:
{
  "error": "PASSWORD_POLICY_VIOLATION",
  "message": "Password does not meet security requirements",
  "details": {
    "minLength": false,
    "hasUppercase": false,
    "hasNumbers": false,
    "hasSymbols": false
  }
}
```

## Monitoring & Debugging

### Check Authentication Logs
```bash
# View authentication attempts
tail -f backend/logs/auth-audit.log

# Sample output:
2025-09-16T10:00:00 INFO  [AUTH] Login attempt: user=test@example.com, method=PASSWORD, success=true, ip=127.0.0.1
2025-09-16T10:01:00 WARN  [AUTH] Failed login: user=test@example.com, method=PASSWORD, reason=INVALID_PASSWORD, ip=127.0.0.1
```

### Database Queries
```sql
-- Check user authentication methods
SELECT email, authentication_methods, email_verified, failed_login_attempts
FROM users
WHERE email = 'test@example.com';

-- View recent authentication attempts
SELECT * FROM authentication_attempts
WHERE username = 'test@example.com'
ORDER BY attempted_at DESC
LIMIT 10;
```

## Troubleshooting

### Issue: Password authentication endpoints return 503
**Solution**: Ensure `ENABLE_PASSWORD_AUTH=true` is set and application restarted

### Issue: Email verification not received
**Solution**: Check SMTP configuration and logs for email sending errors

### Issue: Account locked after failed attempts
**Solution**: Wait for lockout period (exponential backoff) or use admin API to unlock

### Issue: Password reset token expired
**Solution**: Request new reset token (valid for 24 hours)

## Security Considerations

1. **Password Storage**: BCrypt with strength 12 (never visible in logs)
2. **Token Security**: 32-byte secure random tokens, single-use
3. **Rate Limiting**: Exponential backoff on failed attempts
4. **Session Security**: Opaque tokens (not JWT), Redis-backed
5. **Audit Trail**: All authentication events logged

## Performance Metrics

Expected performance with password authentication:
- Login endpoint: <250ms (includes BCrypt verification)
- Registration: <500ms (includes email sending)
- Token validation: <50ms
- Session lookup: <5ms (Redis)

## Next Steps

1. Configure production SMTP server
2. Set up monitoring alerts for failed logins
3. Review and adjust password policy for your requirements
4. Implement 2FA for additional security (future feature)
5. Set up automated testing for all scenarios

## CLI Commands

The feature includes a CLI for administrative tasks:

```bash
# Check user authentication methods
./auth-cli user-info --email test@example.com

# Manually verify email (admin)
./auth-cli verify-email --email test@example.com --admin

# Unlock user account
./auth-cli unlock-account --email test@example.com

# Generate password reset link
./auth-cli reset-password --email test@example.com --generate-link

# View authentication stats
./auth-cli stats --period 24h
```

---

**Support**: For issues, check logs at `backend/logs/` or contact the development team.