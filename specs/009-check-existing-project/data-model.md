# Data Model: Password Authentication

**Date**: 2025-09-16
**Feature**: Alternative Password Authentication

## Entity Extensions

### User (Extended)
Extends the existing User entity with password authentication fields.

**New Fields**:
```
- passwordHash: String (nullable, BCrypt hash)
- passwordResetToken: String (nullable, secure random token)
- passwordResetExpiresAt: Timestamp (nullable)
- emailVerified: Boolean (default: false)
- emailVerificationToken: String (nullable, secure random token)
- emailVerificationExpiresAt: Timestamp (nullable)
- failedLoginAttempts: Integer (default: 0)
- lockoutExpiresAt: Timestamp (nullable)
- passwordChangedAt: Timestamp (nullable)
- authenticationMethods: Set<AuthMethod> (OAUTH2, PASSWORD)
```

**Validation Rules**:
- passwordHash: Must be BCrypt format when present
- passwordResetToken: 32-byte secure random, URL-safe Base64 encoded
- passwordResetExpiresAt: Must be future timestamp when token exists
- emailVerificationToken: 32-byte secure random, URL-safe Base64 encoded
- failedLoginAttempts: Non-negative integer, max 10
- lockoutExpiresAt: Must be future timestamp when user is locked
- authenticationMethods: At least one method required

**State Transitions**:
1. **Account Creation**:
   - OAuth → emailVerified=true, no password fields
   - Password → emailVerified=false, password fields set, verification token generated

2. **Email Verification**:
   - Unverified → Verified when token matches and not expired
   - Sets emailVerified=true, clears verification tokens

3. **Password Reset**:
   - Normal → Reset Requested (token generated, expiry set)
   - Reset Requested → Password Changed (new hash, clear tokens)
   - Reset expires after 24 hours

4. **Account Lockout**:
   - Active → Locked after 5 failed attempts
   - Locked → Active after timeout expires or admin unlock
   - Progressive lockout: 1, 2, 4, 8, 16, 32 minutes

### Session (Extended)
Extends existing Session entity to track authentication method.

**New Fields**:
```
- authenticationMethod: AuthMethod (OAUTH2, PASSWORD)
- loginIpAddress: String (for audit)
- userAgent: String (for device tracking)
```

**Validation Rules**:
- authenticationMethod: Required, must be valid enum value
- loginIpAddress: Valid IPv4 or IPv6 format
- userAgent: Max 500 characters

### AuthenticationAttempt (New)
Tracks all authentication attempts for security auditing.

**Fields**:
```
- id: UUID (primary key)
- username: String (email or username attempted)
- ipAddress: String
- userAgent: String
- attemptedAt: Timestamp
- successful: Boolean
- failureReason: String (nullable)
- authenticationMethod: AuthMethod
```

**Validation Rules**:
- username: Required, max 255 characters
- ipAddress: Valid IP format
- attemptedAt: Required, immutable
- failureReason: Required when successful=false

### PasswordPolicy (Configuration)
Defines password complexity requirements (stored in configuration).

**Fields**:
```
- minLength: Integer (default: 12)
- requireUppercase: Boolean (default: true)
- requireLowercase: Boolean (default: true)
- requireNumbers: Boolean (default: true)
- requireSymbols: Boolean (default: true)
- maxAge: Integer (days, nullable)
- preventReuse: Integer (number of previous passwords, default: 5)
```

## Relationships

### User ↔ Session
- One User can have multiple Sessions
- Session references User by userId
- Session includes authenticationMethod to track how user logged in

### User ↔ AuthenticationAttempt
- AuthenticationAttempts reference User by username (not FK)
- Allows tracking attempts for non-existent users
- Used for rate limiting and security monitoring

### User ↔ Organization (Existing)
- Unchanged from current implementation
- Password auth respects same multi-tenant boundaries

## Database Schema Changes

### Migration: Add Password Fields to Users Table
```sql
ALTER TABLE users
ADD COLUMN password_hash VARCHAR(255),
ADD COLUMN password_reset_token VARCHAR(255),
ADD COLUMN password_reset_expires_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN email_verified BOOLEAN DEFAULT false,
ADD COLUMN email_verification_token VARCHAR(255),
ADD COLUMN email_verification_expires_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN failed_login_attempts INTEGER DEFAULT 0,
ADD COLUMN lockout_expires_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN password_changed_at TIMESTAMP WITH TIME ZONE;

-- Add authentication methods as JSON array
ALTER TABLE users
ADD COLUMN authentication_methods JSON DEFAULT '["OAUTH2"]';

-- Indexes for performance
CREATE INDEX idx_users_password_reset_token ON users(password_reset_token) WHERE password_reset_token IS NOT NULL;
CREATE INDEX idx_users_email_verification_token ON users(email_verification_token) WHERE email_verification_token IS NOT NULL;
CREATE INDEX idx_users_lockout_expires ON users(lockout_expires_at) WHERE lockout_expires_at IS NOT NULL;
```

### New Table: Authentication Attempts
```sql
CREATE TABLE authentication_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    authentication_method VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for queries
CREATE INDEX idx_auth_attempts_username ON authentication_attempts(username);
CREATE INDEX idx_auth_attempts_ip ON authentication_attempts(ip_address);
CREATE INDEX idx_auth_attempts_timestamp ON authentication_attempts(attempted_at);

-- Partition by month for performance (optional)
-- Enables efficient cleanup of old audit data
```

### Update Sessions Table
```sql
ALTER TABLE sessions
ADD COLUMN authentication_method VARCHAR(20) DEFAULT 'OAUTH2',
ADD COLUMN login_ip_address VARCHAR(45),
ADD COLUMN user_agent VARCHAR(500);
```

## Security Considerations

### Password Storage
- BCrypt with strength 12 (already configured)
- Never store plaintext passwords
- Hash comparison in constant time
- Salt automatically handled by BCrypt

### Token Generation
- Use SecureRandom for all tokens
- Minimum 32 bytes of entropy
- URL-safe Base64 encoding for email links
- Single-use tokens (invalidate after use)

### Rate Limiting
- Track by IP address AND username
- Exponential backoff for lockouts
- Clear failed attempts on successful login
- Admin override capability

### Audit Requirements
- Log all authentication attempts
- Include sufficient context for investigation
- PII redaction in logs (GDPR compliance)
- Retain for compliance period (90 days default)

## Data Migration

### Existing OAuth Users
- Set authenticationMethods = ["OAUTH2"]
- Set emailVerified = true
- Leave password fields null
- No user action required

### Feature Flag Behavior
- When ENABLE_PASSWORD_AUTH=false:
  - Password fields ignored
  - Only OAuth2 authentication available
  - No password endpoints exposed
- When ENABLE_PASSWORD_AUTH=true:
  - Both authentication methods available
  - Users can add password to OAuth account
  - New users can register with password

## Validation Logic

### Password Validation
```java
public class PasswordValidator {
    boolean validate(String password, PasswordPolicy policy) {
        if (password.length() < policy.minLength) return false;
        if (policy.requireUppercase && !hasUppercase(password)) return false;
        if (policy.requireLowercase && !hasLowercase(password)) return false;
        if (policy.requireNumbers && !hasNumbers(password)) return false;
        if (policy.requireSymbols && !hasSymbols(password)) return false;
        return true;
    }
}
```

### Email Validation
- RFC 5322 compliant
- Check DNS MX records (optional)
- Normalize before storing (lowercase)
- Maximum 255 characters

### Token Validation
- Check token exists and matches
- Verify not expired
- Single use - invalidate after success
- Timing-safe comparison

## Performance Indexes

### Required Indexes
1. users.email (unique, already exists)
2. users.password_reset_token (partial, where not null)
3. users.email_verification_token (partial, where not null)
4. authentication_attempts.username
5. authentication_attempts.ip_address
6. authentication_attempts.attempted_at
7. sessions.token (already exists)
8. sessions.user_id (already exists)

### Query Optimization
- Partial indexes for nullable token fields
- Composite index for (username, attempted_at) for rate limiting
- Consider partitioning authentication_attempts by month

## Compliance Notes

### GDPR
- Password is personal data - handle accordingly
- Right to deletion includes authentication history
- Audit logs must support PII redaction
- Email verification required for password accounts

### Security Standards
- OWASP compliance for authentication
- NIST 800-63B guidelines for passwords
- PCI DSS not applicable (no payment card data in auth)
- SOC2 audit trail requirements met