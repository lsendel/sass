#!/bin/bash
#
# Pre-Deployment Checks Script
# Validates environment before deployment
#
set -euo pipefail

ENVIRONMENT="${1:-production}"

echo "🔍 Running pre-deployment checks for ${ENVIRONMENT}..."

# Check 1: Verify kubectl access
echo "Check 1: Verifying kubectl access..."
if kubectl cluster-info > /dev/null 2>&1; then
    echo "✅ kubectl access verified"
else
    echo "❌ kubectl access failed"
    exit 1
fi

# Check 2: Verify namespace exists
echo "Check 2: Verifying namespace..."
if kubectl get namespace "$ENVIRONMENT" > /dev/null 2>&1; then
    echo "✅ Namespace '$ENVIRONMENT' exists"
else
    echo "❌ Namespace '$ENVIRONMENT' not found"
    exit 1
fi

# Check 3: Check current deployment status
echo "Check 3: Checking current deployment status..."
if kubectl get deployment payment-platform -n "$ENVIRONMENT" > /dev/null 2>&1; then
    CURRENT_REPLICAS=$(kubectl get deployment payment-platform -n "$ENVIRONMENT" -o jsonpath='{.status.readyReplicas}')
    echo "✅ Current deployment has ${CURRENT_REPLICAS} ready replicas"
else
    echo "⚠️  No existing deployment found (first deployment)"
fi

# Check 4: Verify secrets exist
echo "Check 4: Verifying required secrets..."
REQUIRED_SECRETS=("app-secrets" "db-credentials")
for secret in "${REQUIRED_SECRETS[@]}"; do
    if kubectl get secret "$secret" -n "$ENVIRONMENT" > /dev/null 2>&1; then
        echo "✅ Secret '$secret' exists"
    else
        echo "⚠️  Secret '$secret' not found (may need manual creation)"
    fi
done

# Check 5: Verify ConfigMaps
echo "Check 5: Verifying required ConfigMaps..."
if kubectl get configmap app-config -n "$ENVIRONMENT" > /dev/null 2>&1; then
    echo "✅ ConfigMap 'app-config' exists"
else
    echo "⚠️  ConfigMap 'app-config' not found"
fi

echo "✅ Pre-deployment checks completed"
exit 0
