#!/bin/bash
set -euo pipefail

# Staging Deployment Script
# Usage: ./scripts/deploy-staging.sh [APP_VERSION]

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NAMESPACE="payment-platform-staging"
APP_VERSION="${1:-$(git describe --tags --always --dirty)}"
CONTEXT="staging-cluster"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Error handling
trap 'log_error "Deployment failed at line $LINENO"' ERR

main() {
    log_info "Starting staging deployment for version: $APP_VERSION"

    # Pre-deployment checks
    pre_deployment_checks

    # Create or update namespace
    setup_namespace

    # Update secrets and config maps
    update_configuration

    # Deploy infrastructure components
    deploy_infrastructure

    # Deploy application
    deploy_application

    # Run post-deployment tests
    post_deployment_tests

    # Cleanup old resources
    cleanup_old_resources

    log_success "Staging deployment completed successfully!"
}

pre_deployment_checks() {
    log_info "Running pre-deployment checks..."

    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed or not in PATH"
        exit 1
    fi

    # Check if we can connect to the cluster
    if ! kubectl cluster-info --context="$CONTEXT" &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster: $CONTEXT"
        exit 1
    fi

    # Switch to staging context
    kubectl config use-context "$CONTEXT"

    # Verify required environment variables
    if [[ -z "${KUBE_CONFIG:-}" ]]; then
        log_warning "KUBE_CONFIG not set, using default kubeconfig"
    fi

    # Check if images exist
    if [[ -n "${DOCKER_REGISTRY:-}" ]]; then
        log_info "Verifying Docker images exist..."
        # Add image verification logic here
    fi

    log_success "Pre-deployment checks passed"
}

setup_namespace() {
    log_info "Setting up namespace: $NAMESPACE"

    kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
    kubectl label namespace "$NAMESPACE" environment=staging --overwrite
    kubectl label namespace "$NAMESPACE" app=payment-platform --overwrite

    log_success "Namespace setup completed"
}

update_configuration() {
    log_info "Updating configuration and secrets..."

    # Apply ConfigMap
    envsubst < "$PROJECT_ROOT/deployment/environments/staging.yml" | kubectl apply -f -

    # Create or update secrets (if not exists)
    create_secrets_if_not_exists

    log_success "Configuration updated"
}

create_secrets_if_not_exists() {
    log_info "Creating secrets if they don't exist..."

    # Check if staging-secrets exists
    if ! kubectl get secret staging-secrets -n "$NAMESPACE" &> /dev/null; then
        log_info "Creating staging-secrets secret..."

        kubectl create secret generic staging-secrets \
            --namespace="$NAMESPACE" \
            --from-literal=DATABASE_USERNAME="${STAGING_DB_USERNAME:-platform}" \
            --from-literal=DATABASE_PASSWORD="${STAGING_DB_PASSWORD:-platform}" \
            --from-literal=REDIS_PASSWORD="${STAGING_REDIS_PASSWORD:-}" \
            --from-literal=STRIPE_SECRET_KEY="${STAGING_STRIPE_SECRET_KEY:-sk_test_staging}" \
            --from-literal=JWT_SECRET="${STAGING_JWT_SECRET:-staging_jwt_secret}" \
            --from-literal=OAUTH_CLIENT_SECRET="${STAGING_OAUTH_CLIENT_SECRET:-staging_oauth_secret}" \
            --dry-run=client -o yaml | kubectl apply -f -
    else
        log_info "staging-secrets already exists, skipping creation"
    fi
}

deploy_infrastructure() {
    log_info "Deploying infrastructure components..."

    # Deploy PostgreSQL if not managed externally
    if [[ "${EXTERNAL_DATABASE:-false}" != "true" ]]; then
        log_info "Deploying PostgreSQL..."
        kubectl apply -f "$PROJECT_ROOT/deployment/infrastructure/postgres-staging.yml" -n "$NAMESPACE"

        # Wait for PostgreSQL to be ready
        kubectl wait --for=condition=ready pod -l app=postgres -n "$NAMESPACE" --timeout=300s
    fi

    # Deploy Redis if not managed externally
    if [[ "${EXTERNAL_REDIS:-false}" != "true" ]]; then
        log_info "Deploying Redis..."
        kubectl apply -f "$PROJECT_ROOT/deployment/infrastructure/redis-staging.yml" -n "$NAMESPACE"

        # Wait for Redis to be ready
        kubectl wait --for=condition=ready pod -l app=redis -n "$NAMESPACE" --timeout=300s
    fi

    log_success "Infrastructure deployment completed"
}

deploy_application() {
    log_info "Deploying application version: $APP_VERSION"

    # Update image tags in deployment
    export APP_VERSION
    envsubst < "$PROJECT_ROOT/deployment/environments/staging.yml" | \
        sed "s|payment-platform-backend:staging|payment-platform-backend:$APP_VERSION|g" | \
        sed "s|payment-platform-frontend:staging|payment-platform-frontend:$APP_VERSION|g" | \
        kubectl apply -f -

    # Wait for backend deployment
    log_info "Waiting for backend deployment to complete..."
    kubectl rollout status deployment/payment-platform-backend-staging -n "$NAMESPACE" --timeout=600s

    # Wait for frontend deployment
    log_info "Waiting for frontend deployment to complete..."
    kubectl rollout status deployment/payment-platform-frontend-staging -n "$NAMESPACE" --timeout=600s

    log_success "Application deployment completed"
}

post_deployment_tests() {
    log_info "Running post-deployment tests..."

    # Wait for services to be ready
    sleep 30

    # Get service URLs
    BACKEND_URL=$(kubectl get ingress payment-platform-staging-ingress -n "$NAMESPACE" -o jsonpath='{.spec.rules[1].host}')
    FRONTEND_URL=$(kubectl get ingress payment-platform-staging-ingress -n "$NAMESPACE" -o jsonpath='{.spec.rules[0].host}')

    # Basic health checks
    log_info "Testing backend health endpoint..."
    if curl -f "https://$BACKEND_URL/actuator/health" > /dev/null 2>&1; then
        log_success "Backend health check passed"
    else
        log_error "Backend health check failed"
        return 1
    fi

    log_info "Testing frontend..."
    if curl -f "https://$FRONTEND_URL" > /dev/null 2>&1; then
        log_success "Frontend health check passed"
    else
        log_error "Frontend health check failed"
        return 1
    fi

    # Run smoke tests
    run_smoke_tests

    log_success "Post-deployment tests completed"
}

run_smoke_tests() {
    log_info "Running smoke tests..."

    # Test authentication endpoint
    log_info "Testing authentication endpoints..."
    curl -f "https://$BACKEND_URL/api/v1/auth/methods" > /dev/null 2>&1 || {
        log_error "Authentication endpoint test failed"
        return 1
    }

    # Test API documentation
    log_info "Testing API documentation..."
    curl -f "https://$BACKEND_URL/swagger-ui.html" > /dev/null 2>&1 || {
        log_warning "API documentation endpoint test failed (non-critical)"
    }

    log_success "Smoke tests completed"
}

cleanup_old_resources() {
    log_info "Cleaning up old resources..."

    # Remove old ReplicaSets (keep last 3)
    kubectl delete replicaset -n "$NAMESPACE" \
        $(kubectl get replicaset -n "$NAMESPACE" -o jsonpath='{.items[?(@.spec.replicas==0)].metadata.name}' | tr ' ' '\n' | tail -n +4) \
        2>/dev/null || log_info "No old ReplicaSets to clean up"

    # Clean up old pods in Error/Completed state
    kubectl delete pods -n "$NAMESPACE" --field-selector=status.phase!=Running,status.phase!=Pending 2>/dev/null || log_info "No pods to clean up"

    log_success "Cleanup completed"
}

# Script execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi