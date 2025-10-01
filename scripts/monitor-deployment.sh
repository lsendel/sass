#!/bin/bash
#
# Deployment Monitoring Script
# Monitors the deployment for the specified duration
#
set -euo pipefail

DURATION="${1:-300}"  # Default 5 minutes (300 seconds)

echo "📊 Monitoring deployment for ${DURATION} seconds..."

START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))

ERROR_COUNT=0
MAX_ERRORS=5

while [ $(date +%s) -lt $END_TIME ]; do
    # Check health endpoint
    if curl -sf "https://app.example.com/actuator/health" > /dev/null 2>&1; then
        echo "✅ Health check passed at $(date '+%Y-%m-%d %H:%M:%S')"
        ERROR_COUNT=0
    else
        ERROR_COUNT=$((ERROR_COUNT + 1))
        echo "❌ Health check failed ($ERROR_COUNT/$MAX_ERRORS) at $(date '+%Y-%m-%d %H:%M:%S')"

        if [ $ERROR_COUNT -ge $MAX_ERRORS ]; then
            echo "❌ Maximum error threshold reached. Deployment monitoring failed."
            exit 1
        fi
    fi

    # Calculate remaining time
    CURRENT_TIME=$(date +%s)
    REMAINING=$((END_TIME - CURRENT_TIME))

    if [ $REMAINING -gt 0 ]; then
        echo "⏱️  ${REMAINING}s remaining..."
        sleep 10
    fi
done

echo "✅ Deployment monitoring completed successfully"
exit 0
