# 🔐 Authentication Demo Guide

## Quick Start

```bash
# Start demo environment (no timers)
make demo-start

# Stop demo environment
make demo-stop

# Test authentication with demo credentials
make demo-test

# Run complete demo with Playwright
make demo
```

## Demo Credentials

When using the test profile, the database is automatically seeded with demo data:

| Field                 | Value                                  |
| --------------------- | -------------------------------------- |
| **Email**             | `demo@example.com`                     |
| **Password**          | `DemoPassword123!`                     |
| **Organization ID**   | `00000000-0000-0000-0000-000000000001` |
| **Organization Name** | `Demo Organization`                    |

## Available Commands

### `make demo-start`

- Starts backend and frontend services
- No timers - runs until you stop it
- Shows demo credentials
- Waits for services to be healthy before continuing

### `make demo-stop`

- Stops all running services
- Cleans up background processes

### `make demo-test`

- Tests authentication endpoint with demo credentials
- Shows API response
- Useful for verifying backend is working

### `make demo` or `make demo-login`

- Starts services automatically
- Runs Playwright demonstration
- Creates visual evidence (screenshot)
- Shows working login form

## Authentication Flow

1. **Frontend Form** → User enters credentials
2. **API Call** → `POST /api/v1/auth/login`
3. **Backend Validation** → Checks credentials against database
4. **Response** → Returns user info and token (if successful)
5. **Navigation** → Frontend redirects to dashboard

## Testing the Flow

### Manual Testing

1. Start the demo: `make demo-start`
2. Open browser: http://localhost:3000/auth/login
3. Enter demo credentials
4. Submit form
5. Check network tab for API calls

### Automated Testing

```bash
# Run complete authentication flow test
make auth-flow

# View results
cat frontend/auth-flow-test-results.json
```

## Database Setup

The demo data is created by Flyway migration:

- File: `backend/src/main/resources/db/migration/V999__demo_data.sql`
- Creates demo organization and user
- Password is pre-hashed with BCrypt
- Email is pre-verified for testing

## Troubleshooting

### Login Fails

- Check backend is running: `make health`
- Verify demo data exists in database
- Check password authentication is enabled
- Look at backend logs for errors

### Port Conflicts

```bash
# Stop all services
make stop-all

# Check for port usage
lsof -i :8082  # Backend
lsof -i :3000  # Frontend
```

### Reset Demo Data

If you need to reset the demo data:

1. Stop services: `make demo-stop`
2. Clean backend: `make clean-backend`
3. Start again: `make demo-start`

## API Endpoints

### Authentication Methods

```bash
curl http://localhost:8082/api/v1/auth/methods
```

### Login

```bash
curl -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "DemoPassword123!",
    "organizationId": "00000000-0000-0000-0000-000000000001"
  }'
```

## Visual Evidence

After running `make demo`, check:

- `frontend/login-form-evidence.png` - Screenshot of working form
- `frontend/auth-flow-test-results.json` - Detailed test results

## Notes

- Demo data only loads in `test` profile
- Password is hashed using BCrypt strength 12
- Email verification is bypassed for demo user
- Organization is pre-created with fixed UUID
