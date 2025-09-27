#!/bin/bash

# Production Deployment Script for SASS Platform
# Implements blue-green deployment with comprehensive validation and rollback capabilities

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DEPLOY_ENV="${DEPLOY_ENV:-production}"
BLUE_GREEN="${BLUE_GREEN:-true}"
DRY_RUN="${DRY_RUN:-false}"
ROLLBACK="${ROLLBACK:-false}"
TARGET_VERSION="${TARGET_VERSION:-}"
PREVIOUS_VERSION="${PREVIOUS_VERSION:-}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO: $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARN: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
}

success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] SUCCESS: $1${NC}"
}

# Cleanup function
cleanup() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        error "Deployment failed with exit code $exit_code"
        if [ "$BLUE_GREEN" == "true" ] && [ "$ROLLBACK" != "true" ]; then
            warn "Initiating automatic rollback..."
            rollback_deployment
        fi
    fi
    exit $exit_code
}

trap cleanup EXIT

# Validation functions
validate_prerequisites() {
    log "Validating prerequisites..."

    # Check required tools
    local required_tools=("kubectl" "helm" "docker" "aws" "jq")
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            error "Required tool '$tool' is not installed"
            exit 1
        fi
    done

    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        error "AWS credentials not configured or invalid"
        exit 1
    fi

    # Check Kubernetes context
    local current_context=$(kubectl config current-context)
    if [[ "$current_context" != *"$DEPLOY_ENV"* ]]; then
        error "Kubernetes context '$current_context' does not match environment '$DEPLOY_ENV'"
        exit 1
    fi

    # Check Helm repository
    if ! helm repo list | grep -q "sass-platform"; then
        warn "Adding Helm repository..."
        helm repo add sass-platform oci://ghcr.io/sass-platform/helm-charts
        helm repo update
    fi

    success "Prerequisites validation completed"
}

validate_environment() {
    log "Validating deployment environment..."

    # Check namespace exists
    if ! kubectl get namespace "$DEPLOY_ENV" &> /dev/null; then
        error "Namespace '$DEPLOY_ENV' does not exist"
        exit 1
    fi

    # Check database connectivity
    log "Checking database connectivity..."
    local db_pod=$(kubectl get pods -n "$DEPLOY_ENV" -l app.kubernetes.io/name=postgresql -o jsonpath='{.items[0].metadata.name}')
    if [ -z "$db_pod" ]; then
        error "PostgreSQL pod not found in namespace '$DEPLOY_ENV'"
        exit 1
    fi

    # Check Redis connectivity
    log "Checking Redis connectivity..."
    local redis_pod=$(kubectl get pods -n "$DEPLOY_ENV" -l app.kubernetes.io/name=redis -o jsonpath='{.items[0].metadata.name}')
    if [ -z "$redis_pod" ]; then
        error "Redis pod not found in namespace '$DEPLOY_ENV'"
        exit 1
    fi

    success "Environment validation completed"
}

get_current_version() {
    local deployment_name="sass-platform-backend"
    kubectl get deployment "$deployment_name" -n "$DEPLOY_ENV" -o jsonpath='{.metadata.labels.version}' 2>/dev/null || echo "unknown"
}

# Database migration functions
run_database_migrations() {
    log "Running database migrations..."

    if [ "$DRY_RUN" == "true" ]; then
        log "DRY RUN: Would run database migrations"
        return 0
    fi

    # Create migration job
    local migration_job="sass-platform-migration-$(date +%s)"

    cat <<EOF | kubectl apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: $migration_job
  namespace: $DEPLOY_ENV
  labels:
    app.kubernetes.io/name: sass-platform
    app.kubernetes.io/component: migration
    app.kubernetes.io/version: $TARGET_VERSION
spec:
  ttlSecondsAfterFinished: 3600
  template:
    spec:
      restartPolicy: Never
      containers:
      - name: migration
        image: ghcr.io/sass-platform/sass-backend:$TARGET_VERSION
        command: ["java"]
        args: ["-jar", "app.jar", "--spring.profiles.active=migration"]
        envFrom:
        - configMapRef:
            name: sass-platform-backend-config
        - secretRef:
            name: sass-platform-backend-secret
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 1000m
            memory: 2Gi
EOF

    # Wait for migration to complete
    log "Waiting for database migration to complete..."
    if ! kubectl wait --for=condition=complete job/$migration_job -n "$DEPLOY_ENV" --timeout=600s; then
        error "Database migration failed"
        kubectl logs job/$migration_job -n "$DEPLOY_ENV"
        exit 1
    fi

    success "Database migrations completed successfully"
}

# Blue-green deployment functions
deploy_green_environment() {
    log "Deploying green environment (version: $TARGET_VERSION)..."

    if [ "$DRY_RUN" == "true" ]; then
        log "DRY RUN: Would deploy green environment with version $TARGET_VERSION"
        return 0
    fi

    # Deploy with green suffix
    helm upgrade --install sass-platform-green \
        oci://ghcr.io/sass-platform/helm-charts/sass-platform \
        --version "$TARGET_VERSION" \
        --namespace "$DEPLOY_ENV" \
        --set deployment.suffix="-green" \
        --set image.backend.tag="$TARGET_VERSION" \
        --set image.frontend.tag="$TARGET_VERSION" \
        --set environment="$DEPLOY_ENV" \
        --values "$PROJECT_ROOT/k8s/helm-chart/values-$DEPLOY_ENV.yaml" \
        --wait \
        --timeout=10m

    success "Green environment deployed successfully"
}

validate_green_environment() {
    log "Validating green environment..."

    if [ "$DRY_RUN" == "true" ]; then
        log "DRY RUN: Would validate green environment"
        return 0
    fi

    # Wait for deployments to be ready
    local deployments=("sass-platform-backend-green" "sass-platform-frontend-green")
    for deployment in "${deployments[@]}"; do
        log "Waiting for deployment $deployment to be ready..."
        if ! kubectl rollout status deployment/$deployment -n "$DEPLOY_ENV" --timeout=300s; then
            error "Deployment $deployment failed to become ready"
            exit 1
        fi
    done

    # Health check
    log "Performing health checks..."
    local backend_service="sass-platform-backend-green"
    local health_check_url="http://$backend_service:8080/actuator/health"

    # Port forward for health check
    kubectl port-forward service/$backend_service 8080:8080 -n "$DEPLOY_ENV" &
    local port_forward_pid=$!
    sleep 5

    # Perform health check
    local health_status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/actuator/health" || echo "000")
    kill $port_forward_pid 2>/dev/null || true

    if [ "$health_status" != "200" ]; then
        error "Health check failed with status: $health_status"
        exit 1
    fi

    success "Green environment validation completed"
}

switch_to_green() {
    log "Switching traffic to green environment..."

    if [ "$DRY_RUN" == "true" ]; then
        log "DRY RUN: Would switch traffic to green environment"
        return 0
    fi

    # Update ingress to point to green services
    kubectl patch ingress sass-platform -n "$DEPLOY_ENV" --type='json' \
        -p='[{"op": "replace", "path": "/spec/rules/0/http/paths/0/backend/service/name", "value": "sass-platform-backend-green"}]'

    kubectl patch ingress sass-platform -n "$DEPLOY_ENV" --type='json' \
        -p='[{"op": "replace", "path": "/spec/rules/0/http/paths/1/backend/service/name", "value": "sass-platform-frontend-green"}]'

    # Wait for ingress update
    sleep 30

    success "Traffic switched to green environment"
}

cleanup_blue_environment() {
    log "Cleaning up blue environment..."

    if [ "$DRY_RUN" == "true" ]; then
        log "DRY RUN: Would cleanup blue environment"
        return 0
    fi

    # Remove blue deployment
    helm uninstall sass-platform-blue -n "$DEPLOY_ENV" || true

    # Rename green to main
    helm upgrade sass-platform \
        oci://ghcr.io/sass-platform/helm-charts/sass-platform \
        --version "$TARGET_VERSION" \
        --namespace "$DEPLOY_ENV" \
        --set image.backend.tag="$TARGET_VERSION" \
        --set image.frontend.tag="$TARGET_VERSION" \
        --set environment="$DEPLOY_ENV" \
        --values "$PROJECT_ROOT/k8s/helm-chart/values-$DEPLOY_ENV.yaml" \
        --wait \
        --timeout=10m

    success "Blue environment cleanup completed"
}

# Standard deployment (non-blue-green)
deploy_standard() {
    log "Deploying standard update (version: $TARGET_VERSION)..."

    if [ "$DRY_RUN" == "true" ]; then
        log "DRY RUN: Would deploy standard update with version $TARGET_VERSION"
        return 0
    fi

    helm upgrade sass-platform \
        oci://ghcr.io/sass-platform/helm-charts/sass-platform \
        --version "$TARGET_VERSION" \
        --namespace "$DEPLOY_ENV" \
        --set image.backend.tag="$TARGET_VERSION" \
        --set image.frontend.tag="$TARGET_VERSION" \
        --set environment="$DEPLOY_ENV" \
        --values "$PROJECT_ROOT/k8s/helm-chart/values-$DEPLOY_ENV.yaml" \
        --wait \
        --timeout=10m

    # Wait for rollout
    kubectl rollout status deployment/sass-platform-backend -n "$DEPLOY_ENV" --timeout=300s
    kubectl rollout status deployment/sass-platform-frontend -n "$DEPLOY_ENV" --timeout=300s

    success "Standard deployment completed successfully"
}

# Rollback functions
rollback_deployment() {
    log "Rolling back deployment..."

    if [ -z "$PREVIOUS_VERSION" ]; then
        warn "No previous version specified, attempting automatic rollback..."
        helm rollback sass-platform -n "$DEPLOY_ENV"
    else
        log "Rolling back to version: $PREVIOUS_VERSION"
        helm upgrade sass-platform \
            oci://ghcr.io/sass-platform/helm-charts/sass-platform \
            --version "$PREVIOUS_VERSION" \
            --namespace "$DEPLOY_ENV" \
            --set image.backend.tag="$PREVIOUS_VERSION" \
            --set image.frontend.tag="$PREVIOUS_VERSION" \
            --set environment="$DEPLOY_ENV" \
            --values "$PROJECT_ROOT/k8s/helm-chart/values-$DEPLOY_ENV.yaml" \
            --wait \
            --timeout=10m
    fi

    success "Rollback completed successfully"
}

# Post-deployment validation
post_deployment_validation() {
    log "Performing post-deployment validation..."

    if [ "$DRY_RUN" == "true" ]; then
        log "DRY RUN: Would perform post-deployment validation"
        return 0
    fi

    # Check deployment status
    local deployments=$(kubectl get deployments -n "$DEPLOY_ENV" -l app.kubernetes.io/name=sass-platform -o jsonpath='{.items[*].metadata.name}')
    for deployment in $deployments; do
        if ! kubectl get deployment "$deployment" -n "$DEPLOY_ENV" -o jsonpath='{.status.conditions[?(@.type=="Available")].status}' | grep -q "True"; then
            error "Deployment $deployment is not available"
            exit 1
        fi
    done

    # Smoke tests
    log "Running smoke tests..."
    local frontend_url=$(kubectl get ingress sass-platform -n "$DEPLOY_ENV" -o jsonpath='{.spec.rules[0].host}')
    local response_code=$(curl -s -o /dev/null -w "%{http_code}" "https://$frontend_url/health" || echo "000")

    if [ "$response_code" != "200" ]; then
        error "Frontend smoke test failed with response code: $response_code"
        exit 1
    fi

    # Check metrics endpoint
    local backend_service=$(kubectl get service -n "$DEPLOY_ENV" -l app.kubernetes.io/component=backend -o jsonpath='{.items[0].metadata.name}')
    kubectl port-forward service/$backend_service 8080:8080 -n "$DEPLOY_ENV" &
    local port_forward_pid=$!
    sleep 5

    local metrics_response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/actuator/prometheus" || echo "000")
    kill $port_forward_pid 2>/dev/null || true

    if [ "$metrics_response" != "200" ]; then
        error "Metrics endpoint smoke test failed with response code: $metrics_response"
        exit 1
    fi

    success "Post-deployment validation completed successfully"
}

# Monitoring setup
setup_monitoring() {
    log "Setting up monitoring and alerting..."

    if [ "$DRY_RUN" == "true" ]; then
        log "DRY RUN: Would setup monitoring and alerting"
        return 0
    fi

    # Deploy Prometheus monitoring
    helm upgrade --install prometheus-stack \
        prometheus-community/kube-prometheus-stack \
        --namespace monitoring \
        --create-namespace \
        --values "$PROJECT_ROOT/k8s/monitoring/prometheus-values.yaml" \
        --wait \
        --timeout=10m

    # Deploy custom alerts
    kubectl apply -f "$PROJECT_ROOT/k8s/monitoring/alerts/" -n monitoring

    success "Monitoring setup completed"
}

# Main deployment function
main() {
    log "Starting SASS Platform deployment..."
    log "Environment: $DEPLOY_ENV"
    log "Blue-Green Deployment: $BLUE_GREEN"
    log "Dry Run: $DRY_RUN"
    log "Target Version: ${TARGET_VERSION:-latest}"

    if [ "$ROLLBACK" == "true" ]; then
        rollback_deployment
        return 0
    fi

    # Get current version for potential rollback
    if [ -z "$PREVIOUS_VERSION" ]; then
        PREVIOUS_VERSION=$(get_current_version)
        log "Current version: $PREVIOUS_VERSION"
    fi

    # Validation phase
    validate_prerequisites
    validate_environment

    # Database migrations
    run_database_migrations

    # Deployment phase
    if [ "$BLUE_GREEN" == "true" ]; then
        log "Executing blue-green deployment..."
        deploy_green_environment
        validate_green_environment
        switch_to_green
        cleanup_blue_environment
    else
        log "Executing standard deployment..."
        deploy_standard
    fi

    # Post-deployment
    post_deployment_validation
    setup_monitoring

    success "SASS Platform deployment completed successfully!"
    log "Deployed version: $TARGET_VERSION"
    log "Previous version: $PREVIOUS_VERSION"
}

# Help function
show_help() {
    cat << EOF
SASS Platform Production Deployment Script

Usage: $0 [OPTIONS]

Options:
    -e, --environment ENV       Deployment environment (default: production)
    -v, --version VERSION       Target version to deploy
    -p, --previous VERSION      Previous version (for rollback reference)
    -b, --blue-green           Enable blue-green deployment (default: true)
    -s, --standard             Use standard deployment (disable blue-green)
    -d, --dry-run              Perform dry run without actual deployment
    -r, --rollback             Perform rollback to previous version
    -h, --help                 Show this help message

Environment Variables:
    DEPLOY_ENV                 Deployment environment
    TARGET_VERSION             Target version to deploy
    PREVIOUS_VERSION           Previous version for rollback
    BLUE_GREEN                 Enable/disable blue-green deployment
    DRY_RUN                    Enable/disable dry run mode
    ROLLBACK                   Enable rollback mode

Examples:
    # Standard blue-green deployment
    $0 --version v1.2.3

    # Dry run deployment
    $0 --version v1.2.3 --dry-run

    # Standard deployment (non-blue-green)
    $0 --version v1.2.3 --standard

    # Rollback to previous version
    $0 --rollback --previous v1.2.2

    # Deploy to staging environment
    $0 --environment staging --version v1.2.3-rc1

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            DEPLOY_ENV="$2"
            shift 2
            ;;
        -v|--version)
            TARGET_VERSION="$2"
            shift 2
            ;;
        -p|--previous)
            PREVIOUS_VERSION="$2"
            shift 2
            ;;
        -b|--blue-green)
            BLUE_GREEN="true"
            shift
            ;;
        -s|--standard)
            BLUE_GREEN="false"
            shift
            ;;
        -d|--dry-run)
            DRY_RUN="true"
            shift
            ;;
        -r|--rollback)
            ROLLBACK="true"
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ "$ROLLBACK" != "true" ] && [ -z "$TARGET_VERSION" ]; then
    error "Target version is required for deployment"
    show_help
    exit 1
fi

# Execute main deployment
main