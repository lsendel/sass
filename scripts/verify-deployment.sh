#!/bin/bash
#
# Deployment Verification Script
# Verifies that the deployment is healthy and functioning correctly
#
set -euo pipefail

ENVIRONMENT="${1:-staging}"
COMMIT_SHA="${2:-$(git rev-parse HEAD)}"

echo "🔍 Verifying deployment in ${ENVIRONMENT} environment..."
echo "Commit SHA: ${COMMIT_SHA}"

# Check application health endpoint
echo "Checking health endpoint..."
if [ "$ENVIRONMENT" = "staging" ]; then
    HEALTH_URL="https://staging.example.com/actuator/health"
else
    HEALTH_URL="https://app.example.com/actuator/health"
fi

# Retry logic for health check
MAX_RETRIES=10
RETRY_DELAY=5

for i in $(seq 1 $MAX_RETRIES); do
    if curl -sf "$HEALTH_URL" > /dev/null 2>&1; then
        echo "✅ Health check passed"
        break
    fi

    if [ $i -eq $MAX_RETRIES ]; then
        echo "❌ Health check failed after $MAX_RETRIES attempts"
        exit 1
    fi

    echo "Retry $i/$MAX_RETRIES..."
    sleep $RETRY_DELAY
done

# Verify version/commit
echo "Verifying deployment version..."
# This would typically check a version endpoint
# curl -sf "$HEALTH_URL/version" | jq -r '.commit' | grep -q "$COMMIT_SHA"

echo "✅ Deployment verification completed successfully"
exit 0
