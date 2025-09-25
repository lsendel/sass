#!/bin/bash

# Secure Deployment Script for SASS Payment Platform
set -euo pipefail

echo "🔒 Starting secure deployment process..."

# Check required environment variables
required_vars=(
    "DATABASE_URL"
    "DATABASE_USERNAME" 
    "DATABASE_PASSWORD"
    "STRIPE_SECRET_KEY"
    "STRIPE_WEBHOOK_SECRET"
)

for var in "${required_vars[@]}"; do
    if [[ -z "${!var:-}" ]]; then
        echo "❌ Error: Required environment variable $var is not set"
        exit 1
    fi
done

# Validate secret formats
if [[ ! "$STRIPE_SECRET_KEY" =~ ^sk_(live|test)_ ]]; then
    echo "❌ Error: Invalid Stripe secret key format"
    exit 1
fi

if [[ ! "$STRIPE_WEBHOOK_SECRET" =~ ^whsec_ ]]; then
    echo "❌ Error: Invalid Stripe webhook secret format"
    exit 1
fi

echo "✅ Environment validation passed"

# Build application
echo "🔨 Building application..."
./gradlew clean build -x test

# Run security tests
echo "🧪 Running security tests..."
./gradlew test --tests "*SecurityTest*"

# Deploy with production profile
echo "🚀 Deploying with production configuration..."
export SPRING_PROFILES_ACTIVE=prod

# Start application
java -jar build/libs/payment-platform-*.jar

echo "✅ Secure deployment completed"
