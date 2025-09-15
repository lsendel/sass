# Quickstart Guide: Spring Boot Modulith Payment Platform

## Prerequisites

### Required Software
- Java 21 LTS (OpenJDK or Oracle)
- Node.js 20+ and npm
- Docker & Docker Compose
- PostgreSQL 15+ client tools
- Redis client tools (optional)
- Git

### Development Accounts
- Stripe account (test mode API keys)
- Google OAuth2 application (for authentication)
- GitHub OAuth2 application (optional)
- Email service account (SendGrid/AWS SES)

## Quick Setup (5 minutes)

### 1. Environment Setup

```bash
# Clone and setup
git clone https://github.com/your-org/payment-platform.git
cd payment-platform

# Copy environment template
cp .env.example .env

# Start required services
docker-compose up -d postgres redis mailhog

# Wait for services to be ready
docker-compose ps
```

### 2. Configure Environment Variables

Edit `.env` file:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=platform
DB_USERNAME=platform
DB_PASSWORD=platform

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Stripe (get from Stripe Dashboard -> Developers -> API Keys)
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# OAuth2 (Google Cloud Console -> APIs & Credentials)
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Application
APP_BASE_URL=http://localhost:8080
FRONTEND_URL=http://localhost:3000
```

### 3. Backend Setup

```bash
# Navigate to backend
cd backend

# Run database migrations
./gradlew flywayMigrate

# Build and test
./gradlew build

# Start backend server
./gradlew bootRun
```

Backend will be available at `http://localhost:8080`

### 4. Frontend Setup

```bash
# Navigate to frontend (new terminal)
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend will be available at `http://localhost:3000`

## Validation Steps

### 1. Health Check ✅

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","groups":["liveness","readiness"]}
```

### 2. Authentication Flow ✅

1. Open `http://localhost:3000`
2. Click "Sign in with Google"
3. Complete OAuth flow
4. Verify you're redirected back and logged in
5. Check browser dev tools for session token

### 3. API Contracts ✅

```bash
# List OAuth providers
curl http://localhost:8080/auth/providers

# List available plans
curl http://localhost:8080/plans

# Access protected endpoint (should fail without auth)
curl http://localhost:8080/auth/session
# Expected: 401 Unauthorized
```

### 4. Database Validation ✅

```bash
# Connect to database
psql -h localhost -U platform -d platform

# Check tables exist
\dt

# Verify sample data
SELECT name, amount, currency FROM plans;
SELECT email, name FROM users;
```

### 5. Payment Integration ✅

1. Login to application
2. Navigate to subscription page
3. Select a plan
4. Use Stripe test card: `4242424242424242`
5. Complete payment flow
6. Verify subscription appears in dashboard

**Test Cards for Different Scenarios:**
- Success: `4242424242424242`
- Decline: `4000000000000002`
- 3D Secure: `4000000000003220`

### 6. Webhook Testing ✅

```bash
# Install Stripe CLI
stripe listen --forward-to localhost:8080/webhooks/stripe

# In another terminal, create test event
stripe trigger payment_intent.succeeded

# Check backend logs for webhook processing
docker-compose logs -f backend
```

## Core User Journeys

### Journey 1: New User Registration ✅

**Steps:**
1. Visit application homepage
2. Click "Get Started"
3. Sign in with OAuth provider
4. Complete profile setup
5. Select subscription plan
6. Enter payment details
7. Confirm subscription

**Expected Results:**
- User account created
- Organization created (personal workspace)
- Subscription active
- Welcome email sent
- Dashboard accessible

### Journey 2: Team Collaboration ✅

**Steps:**
1. Login as organization owner
2. Navigate to team settings
3. Send invitation to team member
4. Team member receives email
5. Team member accepts invitation
6. Team member can access organization

**Expected Results:**
- Invitation email delivered
- Team member added to organization
- Permissions correctly applied
- Audit log updated

### Journey 3: Subscription Management ✅

**Steps:**
1. Login as organization admin
2. Navigate to billing settings
3. View current subscription
4. Change plan (upgrade/downgrade)
5. Update payment method
6. Cancel subscription

**Expected Results:**
- Plan change processed correctly
- Proration calculated
- Payment method updated
- Cancellation scheduled

### Journey 4: Admin Operations ✅

**Steps:**
1. Login as platform admin
2. View admin dashboard
3. Search for user/organization
4. View audit logs
5. Generate compliance report

**Expected Results:**
- All organizations visible
- Search functionality working
- Audit logs comprehensive
- Reports accurate

## Testing Commands

### Run Full Test Suite

```bash
# Backend tests
cd backend
./gradlew test jacocoTestReport

# Frontend tests
cd frontend
npm test

# E2E tests
cd e2e
npm run test:e2e
```

### Architecture Validation

```bash
# Check module boundaries
./gradlew archTest

# Expected: All ArchUnit rules pass
```

### Load Testing

```bash
# Install k6
brew install k6  # macOS
# or download from https://k6.io/

# Run load test
k6 run scripts/load-test.js

# Expected: No errors, acceptable response times
```

## Troubleshooting

### Common Issues

**Database Connection Fails**
```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check logs
docker-compose logs postgres

# Restart if needed
docker-compose restart postgres
```

**OAuth Login Fails**
- Verify Google OAuth credentials in .env
- Check redirect URI matches in Google Console
- Ensure HTTPS in production

**Stripe Webhooks Not Working**
- Verify webhook secret in .env
- Check Stripe CLI is forwarding correctly
- Ensure webhook endpoint is accessible

**Tests Failing**
```bash
# Clean and rebuild
./gradlew clean build

# Reset test database
./gradlew flywayClean flywayMigrate
```

### Logs and Debugging

```bash
# Backend application logs
docker-compose logs -f backend

# Database logs
docker-compose logs postgres

# Frontend development server
npm run dev

# Enable debug logging
export LOGGING_LEVEL_ROOT=DEBUG
```

## Performance Benchmarks

### Expected Performance (Local Development)

- **API Response Time**: < 100ms (p95)
- **Database Query Time**: < 50ms (average)
- **Frontend Load Time**: < 2s (first load)
- **Authentication Flow**: < 5s (complete)

### Production Readiness Checklist

- [ ] All tests passing (unit, integration, e2e)
- [ ] No security vulnerabilities (dependency check)
- [ ] Database migrations work correctly
- [ ] Monitoring and logging configured
- [ ] Error handling comprehensive
- [ ] Performance within targets
- [ ] GDPR compliance validated
- [ ] Backup/restore procedures tested

## Next Steps

1. **Development**: Start implementing features following TDD approach
2. **Configuration**: Setup production environment variables
3. **Deployment**: Configure CI/CD pipeline
4. **Monitoring**: Setup observability stack
5. **Security**: Perform security audit
6. **Documentation**: Update API documentation

## Support

- **Architecture Questions**: Review `specs/005-spring-boot-modulith/spec.md`
- **Implementation Guide**: See `specs/005-spring-boot-modulith/plan.md`
- **API Reference**: OpenAPI specs in `specs/005-spring-boot-modulith/contracts/`
- **Issues**: Create issue in repository with reproduction steps

---

*Quickstart guide validated for development environment setup and core functionality*