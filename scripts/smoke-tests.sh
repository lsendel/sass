#!/bin/bash
#
# Smoke Tests Script
# Runs basic smoke tests against the deployed environment
#
set -euo pipefail

ENVIRONMENT="${1:-staging}"

echo "🧪 Running smoke tests in ${ENVIRONMENT} environment..."

# Set base URL based on environment
if [ "$ENVIRONMENT" = "staging" ] || [ "$ENVIRONMENT" = "staging-green" ]; then
    BASE_URL="https://staging.example.com"
elif [ "$ENVIRONMENT" = "production" ] || [ "$ENVIRONMENT" = "production-green" ]; then
    BASE_URL="https://app.example.com"
else
    echo "❌ Unknown environment: $ENVIRONMENT"
    exit 1
fi

# Test 1: Health check
echo "Test 1: Health endpoint..."
if curl -sf "$BASE_URL/actuator/health" | grep -q "UP"; then
    echo "✅ Health check passed"
else
    echo "❌ Health check failed"
    exit 1
fi

# Test 2: API responsiveness
echo "Test 2: API responsiveness..."
RESPONSE_TIME=$(curl -sf -w '%{time_total}' -o /dev/null "$BASE_URL/actuator/info")
if (( $(echo "$RESPONSE_TIME < 2.0" | bc -l) )); then
    echo "✅ API response time: ${RESPONSE_TIME}s"
else
    echo "⚠️  Slow API response time: ${RESPONSE_TIME}s"
fi

# Test 3: Database connectivity
echo "Test 3: Database connectivity..."
if curl -sf "$BASE_URL/actuator/health/db" | grep -q "UP"; then
    echo "✅ Database connectivity passed"
else
    echo "❌ Database connectivity failed"
    exit 1
fi

# Test 4: Redis connectivity
echo "Test 4: Redis connectivity..."
if curl -sf "$BASE_URL/actuator/health/redis" | grep -q "UP"; then
    echo "✅ Redis connectivity passed"
else
    echo "⚠️  Redis connectivity check failed (non-critical)"
fi

echo "✅ All smoke tests passed"
exit 0
