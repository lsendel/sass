# Quickstart Guide: OAuth2 Authentication System

**Feature**: User Authentication and Login System with OAuth2 Providers
**Branch**: `007-user-authentication-and`
**Date**: 2025-01-15

## Overview
This quickstart guide provides step-by-step instructions to validate the OAuth2/PKCE authentication system implementation. It covers setup, configuration, testing, and verification of all OAuth2 flows for Google, GitHub, and Microsoft providers.

## Prerequisites

### Development Environment
- Java 21+ (OpenJDK recommended)
- Node.js 18+ with npm
- PostgreSQL 15+ (for user data storage)
- Redis 7+ (for session management)
- Git (for version control)

### OAuth2 Provider Setup
Before testing, you'll need to configure OAuth2 applications with each provider:

#### Google OAuth2 Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create new project or select existing project
3. Enable Google+ API and Google Identity API
4. Create OAuth2 credentials (Web application)
5. Add authorized redirect URI: `http://localhost:8080/api/v1/auth/oauth2/callback/google`
6. Note down Client ID and Client Secret

#### GitHub OAuth2 Setup
1. Go to GitHub Settings → Developer settings → OAuth Apps
2. Create new OAuth App
3. Set Authorization callback URL: `http://localhost:8080/api/v1/auth/oauth2/callback/github`
4. Note down Client ID and Client Secret

#### Microsoft OAuth2 Setup
1. Go to [Azure Portal](https://portal.azure.com/) → Azure Active Directory
2. Register new application
3. Add web redirect URI: `http://localhost:8080/api/v1/auth/oauth2/callback/microsoft`
4. Generate client secret in Certificates & secrets
5. Note down Application (client) ID and Client Secret

## Quick Setup (5 minutes)

### 1. Environment Configuration
Create `.env` file in project root:

```bash
# OAuth2 Provider Configuration
OAUTH2_GOOGLE_CLIENT_ID=your_google_client_id
OAUTH2_GOOGLE_CLIENT_SECRET=your_google_client_secret

OAUTH2_GITHUB_CLIENT_ID=your_github_client_id
OAUTH2_GITHUB_CLIENT_SECRET=your_github_client_secret

OAUTH2_MICROSOFT_CLIENT_ID=your_microsoft_client_id
OAUTH2_MICROSOFT_CLIENT_SECRET=your_microsoft_client_secret

# Database Configuration
DATABASE_URL=postgresql://localhost:5432/payment_platform
DATABASE_USERNAME=platform_user
DATABASE_PASSWORD=secure_password

# Redis Configuration
REDIS_URL=redis://localhost:6379
REDIS_PASSWORD=redis_password

# Session Configuration
SESSION_TIMEOUT=86400  # 24 hours in seconds
SESSION_SECURE=false   # Set to true in production
```

### 2. Database Setup
```bash
# Start PostgreSQL
brew services start postgresql  # macOS
sudo systemctl start postgresql  # Linux

# Create database and user
psql -U postgres -c "CREATE DATABASE payment_platform;"
psql -U postgres -c "CREATE USER platform_user WITH PASSWORD 'secure_password';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE payment_platform TO platform_user;"
```

### 3. Redis Setup
```bash
# Start Redis
brew services start redis  # macOS
sudo systemctl start redis  # Linux

# Set Redis password (optional for development)
redis-cli config set requirepass redis_password
```

### 4. Backend Startup
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Backend should start on `http://localhost:8080` with OAuth2 endpoints available.

### 5. Frontend Startup
```bash
cd frontend
npm install
npm run dev
```

Frontend should start on `http://localhost:3000` with OAuth2 login interface.

## Validation Tests (10 minutes)

### Test 1: OAuth2 Providers Endpoint
Verify all OAuth2 providers are configured and available:

```bash
curl -X GET http://localhost:8080/api/v1/auth/oauth2/providers \
  -H "Accept: application/json" | jq
```

**Expected Response:**
```json
{
  "providers": [
    {
      "name": "google",
      "displayName": "Google",
      "authorizationUrl": "https://accounts.google.com/oauth/authorize",
      "scopes": ["openid", "email", "profile"]
    },
    {
      "name": "github",
      "displayName": "GitHub",
      "authorizationUrl": "https://github.com/login/oauth/authorize",
      "scopes": ["read:user", "user:email"]
    },
    {
      "name": "microsoft",
      "displayName": "Microsoft",
      "authorizationUrl": "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
      "scopes": ["openid", "email", "profile"]
    }
  ]
}
```

### Test 2: OAuth2 Authorization Flow
Test OAuth2 authorization initiation for each provider:

```bash
# Test Google OAuth2 flow
curl -I -X GET "http://localhost:8080/api/v1/auth/oauth2/authorize/google"

# Test GitHub OAuth2 flow
curl -I -X GET "http://localhost:8080/api/v1/auth/oauth2/authorize/github"

# Test Microsoft OAuth2 flow
curl -I -X GET "http://localhost:8080/api/v1/auth/oauth2/authorize/microsoft"
```

**Expected Response:** HTTP 302 redirect to OAuth2 provider with PKCE parameters.

### Test 3: Frontend OAuth2 Integration
1. Open browser to `http://localhost:3000/auth/login`
2. Verify OAuth2 provider buttons are displayed
3. Click on each provider button
4. Verify redirect to provider authorization page
5. Complete authorization flow
6. Verify redirect back to application with session

### Test 4: Complete Authentication Flow
Full end-to-end OAuth2 authentication test:

1. **Initiate Login:**
   - Navigate to `http://localhost:3000/auth/login`
   - Click "Sign in with Google" (or preferred provider)

2. **Provider Authorization:**
   - Verify redirect to Google OAuth2 authorization page
   - Sign in with Google account
   - Grant permissions to application

3. **Callback Processing:**
   - Verify redirect back to `http://localhost:3000/dashboard`
   - Check browser network tab for successful callback
   - Verify session cookie is set

4. **Session Validation:**
   ```bash
   # Get session info (use session cookie from browser)
   curl -X GET http://localhost:8080/api/v1/auth/oauth2/session \
     -H "Cookie: JSESSIONID=your_session_id" | jq
   ```

5. **Logout Test:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/oauth2/logout \
     -H "Cookie: JSESSIONID=your_session_id" \
     -H "Content-Type: application/json" \
     -d '{"terminateProviderSession": false}' | jq
   ```

## Automated Testing

### Backend Contract Tests
```bash
cd backend
./gradlew test --tests "*OAuth2*Contract*"
```

### Frontend Component Tests
```bash
cd frontend
npm test -- OAuth2
```

### Integration Tests
```bash
cd backend
./gradlew test --tests "*OAuth2*Integration*"
```

### End-to-End Tests
```bash
cd frontend
npx playwright test oauth2-flows.spec.ts
```

## Performance Validation

### Load Testing OAuth2 Endpoints
```bash
# Install k6 for load testing
brew install k6  # macOS

# Test OAuth2 providers endpoint
k6 run --vus 100 --duration 30s - <<'EOF'
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  const response = http.get('http://localhost:8080/api/v1/auth/oauth2/providers');
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 50ms': (r) => r.timings.duration < 50,
  });
}
EOF
```

### Session Performance Test
```bash
# Test session validation performance
k6 run --vus 50 --duration 20s - <<'EOF'
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  const sessionCookie = { JSESSIONID: 'valid_session_id' };
  const response = http.get('http://localhost:8080/api/v1/auth/oauth2/session', {
    cookies: sessionCookie
  });
  check(response, {
    'session validation < 50ms': (r) => r.timings.duration < 50,
  });
}
EOF
```

## Security Validation

### PKCE Verification
1. Inspect OAuth2 authorization URL in browser network tab
2. Verify presence of `code_challenge` and `code_challenge_method=S256`
3. Verify `state` parameter for CSRF protection
4. Confirm redirect URI matches registered callback

### Token Security
1. Verify OAuth2 tokens are never exposed in:
   - Browser localStorage or sessionStorage
   - URL parameters or fragments
   - JavaScript variables accessible via console
   - Network request/response bodies (except during token exchange)

2. Confirm session cookies are secure:
   - HttpOnly flag set
   - Secure flag set (in production)
   - SameSite=Strict attribute
   - Appropriate expiration time

### Audit Logging
```bash
# Check OAuth2 audit events in database
psql -U platform_user -d payment_platform -c \
  "SELECT event_type, provider, created_at FROM oauth2_audit_events ORDER BY created_at DESC LIMIT 10;"
```

## Troubleshooting

### Common Issues

#### Provider Configuration Errors
- **Issue:** "Provider not found" error
- **Solution:** Verify OAuth2 provider configuration in `application.yml`
- **Check:** Client ID and secret are correctly set in environment variables

#### PKCE Validation Failures
- **Issue:** "Invalid code_verifier" error
- **Solution:** Ensure PKCE code verifier matches code challenge
- **Check:** Browser session storage for PKCE state

#### Session Management Issues
- **Issue:** Session not persisting across requests
- **Solution:** Verify Redis connection and session configuration
- **Check:** Redis logs and session cookie settings

#### Callback Processing Errors
- **Issue:** OAuth2 callback fails with "Invalid state" error
- **Solution:** Verify state parameter matches and hasn't expired
- **Check:** Session storage and CSRF protection implementation

### Debug Mode
Enable debug logging for OAuth2 flows:

```yaml
# application-dev.yml
logging:
  level:
    org.springframework.security.oauth2: DEBUG
    com.platform.auth.oauth2: DEBUG
    org.springframework.session: DEBUG
```

### Health Checks
Verify system components are healthy:

```bash
# Backend health
curl http://localhost:8080/actuator/health | jq

# Database connectivity
curl http://localhost:8080/actuator/health/db | jq

# Redis connectivity
curl http://localhost:8080/actuator/health/redis | jq
```

## Success Criteria

✅ **All OAuth2 providers return valid configuration**
✅ **OAuth2 authorization flow initiates correctly for all providers**
✅ **PKCE parameters are properly generated and validated**
✅ **OAuth2 callback processing completes successfully**
✅ **User sessions are created and managed securely**
✅ **Logout terminates sessions properly**
✅ **Audit events are logged for all OAuth2 activities**
✅ **Performance meets requirements (<50ms session validation)**
✅ **Security validations pass (PKCE, secure cookies, no token exposure)**
✅ **Integration tests pass with real OAuth2 providers**

## Next Steps

After successful validation:
1. Configure production OAuth2 provider applications
2. Set up production environment variables
3. Deploy to staging environment for user acceptance testing
4. Configure monitoring and alerting for OAuth2 flows
5. Implement user onboarding and account linking workflows

## Support

For issues with this quickstart guide:
- Check troubleshooting section above
- Review OAuth2 provider documentation
- Consult Spring Security OAuth2 Client documentation
- Contact development team for platform-specific issues