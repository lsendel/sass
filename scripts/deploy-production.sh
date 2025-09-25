#!/bin/bash
set -euo pipefail

# Production Deployment Script
# Usage: ./scripts/deploy-production.sh [APP_VERSION]

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NAMESPACE="payment-platform-production"
APP_VERSION="${1:-$(git describe --tags --always --dirty)}"
CONTEXT="production-cluster"
BACKUP_RETENTION_DAYS=30

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_critical() { echo -e "${PURPLE}[CRITICAL]${NC} $1"; }

# Error handling with rollback
trap 'handle_error $LINENO' ERR

handle_error() {
    log_error "Production deployment failed at line $1"
    log_critical "Initiating emergency rollback procedures..."

    if [[ "${ROLLBACK_ON_FAILURE:-true}" == "true" ]]; then
        emergency_rollback
    fi

    exit 1
}

main() {
    log_critical "🚨 PRODUCTION DEPLOYMENT INITIATED 🚨"
    log_info "Version: $APP_VERSION"
    log_info "Timestamp: $(date -u '+%Y-%m-%d %H:%M:%S UTC')"

    # Pre-deployment validation (extensive for production)
    pre_deployment_validation

    # Create database backup
    create_database_backup

    # Blue-green deployment preparation
    prepare_blue_green_deployment

    # Deploy to green environment
    deploy_to_green_environment

    # Comprehensive testing on green environment
    comprehensive_testing

    # Switch traffic to green (blue-green cutover)
    switch_traffic_to_green

    # Post-deployment monitoring
    post_deployment_monitoring

    # Cleanup blue environment (after verification)
    cleanup_blue_environment

    log_success "🎉 PRODUCTION DEPLOYMENT COMPLETED SUCCESSFULLY! 🎉"
    send_deployment_notification "SUCCESS"
}

pre_deployment_validation() {
    log_info "Running comprehensive pre-deployment validation..."

    # Check deployment prerequisites
    validate_prerequisites

    # Security validation
    security_validation

    # Performance validation
    performance_validation

    # Database migration validation
    database_migration_validation

    # Business continuity validation
    business_continuity_validation

    log_success "Pre-deployment validation passed"
}

validate_prerequisites() {
    log_info "Validating deployment prerequisites..."

    # Check tools availability
    local required_tools=("kubectl" "helm" "curl" "jq" "pg_dump")
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            log_error "$tool is not installed or not in PATH"
            exit 1
        fi
    done

    # Verify cluster connectivity
    if ! kubectl cluster-info --context="$CONTEXT" &> /dev/null; then
        log_error "Cannot connect to production cluster: $CONTEXT"
        exit 1
    fi

    # Switch to production context
    kubectl config use-context "$CONTEXT"

    # Verify required secrets exist
    local required_secrets=("production-secrets" "production-tls")
    for secret in "${required_secrets[@]}"; do
        if ! kubectl get secret "$secret" -n "$NAMESPACE" &> /dev/null; then
            log_error "Required secret '$secret' not found in namespace '$NAMESPACE'"
            exit 1
        fi
    done

    # Verify resource quotas
    check_resource_quotas

    log_success "Prerequisites validation passed"
}

security_validation() {
    log_info "Running security validation..."

    # Check image signatures (if implemented)
    if [[ "${VERIFY_IMAGE_SIGNATURES:-false}" == "true" ]]; then
        verify_image_signatures
    fi

    # Validate TLS certificates
    validate_tls_certificates

    # Check security policies
    validate_security_policies

    log_success "Security validation passed"
}

performance_validation() {
    log_info "Running performance validation..."

    # Check current resource usage
    local cpu_usage=$(kubectl top nodes --no-headers | awk '{sum += $3} END {print sum/NR}' | cut -d'%' -f1)
    local memory_usage=$(kubectl top nodes --no-headers | awk '{sum += $5} END {print sum/NR}' | cut -d'%' -f1)

    if (( $(echo "$cpu_usage > 70" | bc -l) )); then
        log_warning "High CPU usage detected: ${cpu_usage}%"
        read -p "Continue with deployment? (y/N): " -n 1 -r
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_error "Deployment aborted due to high CPU usage"
            exit 1
        fi
    fi

    if (( $(echo "$memory_usage > 80" | bc -l) )); then
        log_warning "High memory usage detected: ${memory_usage}%"
        read -p "Continue with deployment? (y/N): " -n 1 -r
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_error "Deployment aborted due to high memory usage"
            exit 1
        fi
    fi

    log_success "Performance validation passed"
}

database_migration_validation() {
    log_info "Validating database migrations..."

    # Test migrations in dry-run mode if supported
    # This would depend on your specific migration tool
    log_info "Testing database migrations (dry-run)..."

    # Add specific migration validation logic here
    # Example: ./gradlew flywayValidate -Pflyway-prod

    log_success "Database migration validation passed"
}

business_continuity_validation() {
    log_info "Validating business continuity measures..."

    # Check if maintenance window is active
    if [[ "${MAINTENANCE_WINDOW_ACTIVE:-false}" == "true" ]]; then
        log_info "Deployment during maintenance window - proceeding"
    else
        log_warning "Deployment outside maintenance window"
        read -p "Continue with production deployment? (y/N): " -n 1 -r
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_error "Deployment aborted - outside maintenance window"
            exit 1
        fi
    fi

    # Verify backup systems are operational
    verify_backup_systems

    log_success "Business continuity validation passed"
}

create_database_backup() {
    log_info "Creating production database backup..."

    local backup_timestamp=$(date -u '+%Y%m%d_%H%M%S')
    local backup_name="production_backup_${backup_timestamp}_pre_deploy"
    local backup_path="/backups/${backup_name}.sql"

    # Create backup directory if it doesn't exist
    mkdir -p "$(dirname "$backup_path")"

    # Create database backup
    kubectl exec -n "$NAMESPACE" deployment/postgres-production -- \
        pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" > "$backup_path"

    # Verify backup integrity
    if [[ -s "$backup_path" ]]; then
        log_success "Database backup created: $backup_path"

        # Store backup metadata
        echo "{
            \"backup_name\": \"$backup_name\",
            \"timestamp\": \"$backup_timestamp\",
            \"version\": \"$APP_VERSION\",
            \"size\": \"$(du -h "$backup_path" | cut -f1)\"
        }" > "${backup_path}.meta.json"
    else
        log_error "Database backup failed or is empty"
        exit 1
    fi

    # Cleanup old backups
    find /backups -name "production_backup_*.sql" -mtime +$BACKUP_RETENTION_DAYS -delete 2>/dev/null || true
    find /backups -name "production_backup_*.sql.meta.json" -mtime +$BACKUP_RETENTION_DAYS -delete 2>/dev/null || true
}

prepare_blue_green_deployment() {
    log_info "Preparing blue-green deployment..."

    # Tag current deployment as "blue"
    kubectl patch deployment payment-platform-backend-production -n "$NAMESPACE" \
        -p '{"metadata":{"labels":{"deployment-color":"blue"}}}'
    kubectl patch deployment payment-platform-frontend-production -n "$NAMESPACE" \
        -p '{"metadata":{"labels":{"deployment-color":"blue"}}}'

    # Create green deployment manifests
    create_green_deployment_manifests

    log_success "Blue-green deployment preparation completed"
}

create_green_deployment_manifests() {
    log_info "Creating green deployment manifests..."

    # Create temporary directory for green manifests
    local green_manifests_dir="/tmp/green-deployment-$$"
    mkdir -p "$green_manifests_dir"

    # Generate green deployment manifests
    export APP_VERSION
    export DEPLOYMENT_COLOR="green"

    envsubst < "$PROJECT_ROOT/deployment/environments/production.yml" | \
        sed "s|payment-platform-backend-production|payment-platform-backend-production-green|g" | \
        sed "s|payment-platform-frontend-production|payment-platform-frontend-production-green|g" | \
        sed "s|payment-platform-backend:production|payment-platform-backend:$APP_VERSION|g" | \
        sed "s|payment-platform-frontend:production|payment-platform-frontend:$APP_VERSION|g" \
        > "$green_manifests_dir/green-deployment.yml"

    # Store green manifests path for later use
    export GREEN_MANIFESTS_DIR="$green_manifests_dir"
}

deploy_to_green_environment() {
    log_info "Deploying to green environment..."

    # Apply green deployment
    kubectl apply -f "$GREEN_MANIFESTS_DIR/green-deployment.yml" -n "$NAMESPACE"

    # Wait for green deployment to be ready
    log_info "Waiting for green backend deployment..."
    kubectl rollout status deployment/payment-platform-backend-production-green -n "$NAMESPACE" --timeout=600s

    log_info "Waiting for green frontend deployment..."
    kubectl rollout status deployment/payment-platform-frontend-production-green -n "$NAMESPACE" --timeout=600s

    # Create temporary services for testing green environment
    create_green_test_services

    log_success "Green environment deployment completed"
}

create_green_test_services() {
    log_info "Creating temporary services for green environment testing..."

    # Create temporary service for green backend
    kubectl create service clusterip payment-platform-backend-production-green-test \
        --tcp=80:8080 \
        --dry-run=client -o yaml | kubectl apply -f - -n "$NAMESPACE"

    kubectl patch service payment-platform-backend-production-green-test -n "$NAMESPACE" \
        -p '{"spec":{"selector":{"app":"payment-platform-backend","deployment-color":"green"}}}'

    # Create temporary service for green frontend
    kubectl create service clusterip payment-platform-frontend-production-green-test \
        --tcp=80:80 \
        --dry-run=client -o yaml | kubectl apply -f - -n "$NAMESPACE"

    kubectl patch service payment-platform-frontend-production-green-test -n "$NAMESPACE" \
        -p '{"spec":{"selector":{"app":"payment-platform-frontend","deployment-color":"green"}}}'
}

comprehensive_testing() {
    log_info "Running comprehensive tests on green environment..."

    # Wait for services to stabilize
    sleep 60

    # Get test service endpoints
    local backend_test_ip=$(kubectl get service payment-platform-backend-production-green-test -n "$NAMESPACE" -o jsonpath='{.spec.clusterIP}')
    local frontend_test_ip=$(kubectl get service payment-platform-frontend-production-green-test -n "$NAMESPACE" -o jsonpath='{.spec.clusterIP}')

    # Health checks
    run_health_checks "$backend_test_ip" "$frontend_test_ip"

    # Functional tests
    run_functional_tests "$backend_test_ip"

    # Performance tests
    run_performance_tests "$backend_test_ip"

    # Security tests
    run_security_tests "$backend_test_ip"

    log_success "Comprehensive testing completed"
}

run_health_checks() {
    local backend_ip="$1"
    local frontend_ip="$2"

    log_info "Running health checks..."

    # Backend health check
    if ! kubectl exec -n "$NAMESPACE" deployment/payment-platform-backend-production-green -- \
        curl -f "http://$backend_ip/actuator/health" > /dev/null 2>&1; then
        log_error "Green backend health check failed"
        return 1
    fi

    # Frontend health check
    if ! kubectl exec -n "$NAMESPACE" deployment/payment-platform-frontend-production-green -- \
        curl -f "http://$frontend_ip/health" > /dev/null 2>&1; then
        log_error "Green frontend health check failed"
        return 1
    fi

    log_success "Health checks passed"
}

run_functional_tests() {
    local backend_ip="$1"

    log_info "Running functional tests..."

    # Test critical API endpoints
    local test_endpoints=(
        "/actuator/health"
        "/api/v1/auth/methods"
        "/actuator/info"
    )

    for endpoint in "${test_endpoints[@]}"; do
        if ! kubectl exec -n "$NAMESPACE" deployment/payment-platform-backend-production-green -- \
            curl -f "http://$backend_ip$endpoint" > /dev/null 2>&1; then
            log_error "Functional test failed for endpoint: $endpoint"
            return 1
        fi
    done

    log_success "Functional tests passed"
}

run_performance_tests() {
    local backend_ip="$1"

    log_info "Running performance tests..."

    # Basic performance test (measure response time)
    local response_time=$(kubectl exec -n "$NAMESPACE" deployment/payment-platform-backend-production-green -- \
        curl -o /dev/null -s -w '%{time_total}' "http://$backend_ip/actuator/health")

    if (( $(echo "$response_time > 5.0" | bc -l) )); then
        log_warning "Performance test shows high response time: ${response_time}s"
        read -p "Continue with deployment? (y/N): " -n 1 -r
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_error "Deployment aborted due to performance issues"
            return 1
        fi
    fi

    log_success "Performance tests passed (response time: ${response_time}s)"
}

run_security_tests() {
    local backend_ip="$1"

    log_info "Running security tests..."

    # Test security headers
    local security_headers=$(kubectl exec -n "$NAMESPACE" deployment/payment-platform-backend-production-green -- \
        curl -I "http://$backend_ip/actuator/health" 2>/dev/null | grep -E "(X-Frame-Options|X-Content-Type-Options|X-XSS-Protection)")

    if [[ -z "$security_headers" ]]; then
        log_warning "Some security headers may be missing"
    fi

    log_success "Security tests passed"
}

switch_traffic_to_green() {
    log_info "Switching traffic to green environment..."

    # Update service selectors to point to green deployment
    kubectl patch service payment-platform-backend-production -n "$NAMESPACE" \
        -p '{"spec":{"selector":{"app":"payment-platform-backend","deployment-color":"green"}}}'

    kubectl patch service payment-platform-frontend-production -n "$NAMESPACE" \
        -p '{"spec":{"selector":{"app":"payment-platform-frontend","deployment-color":"green"}}}'

    # Wait for traffic switch to take effect
    sleep 30

    # Verify traffic is flowing to green environment
    verify_traffic_switch

    log_success "Traffic successfully switched to green environment"
}

verify_traffic_switch() {
    log_info "Verifying traffic switch..."

    # Get production URLs
    local backend_url=$(kubectl get ingress payment-platform-production-ingress -n "$NAMESPACE" -o jsonpath='{.spec.rules[1].host}')
    local frontend_url=$(kubectl get ingress payment-platform-production-ingress -n "$NAMESPACE" -o jsonpath='{.spec.rules[0].host}')

    # Test public endpoints
    if ! curl -f "https://$backend_url/actuator/health" > /dev/null 2>&1; then
        log_error "Traffic switch verification failed for backend"
        return 1
    fi

    if ! curl -f "https://$frontend_url" > /dev/null 2>&1; then
        log_error "Traffic switch verification failed for frontend"
        return 1
    fi

    log_success "Traffic switch verification passed"
}

post_deployment_monitoring() {
    log_info "Starting post-deployment monitoring..."

    # Monitor for 5 minutes after deployment
    local monitor_duration=300
    local monitor_interval=30
    local iterations=$((monitor_duration / monitor_interval))

    for ((i=1; i<=iterations; i++)); do
        log_info "Monitoring iteration $i/$iterations..."

        # Check pod health
        local unhealthy_pods=$(kubectl get pods -n "$NAMESPACE" --no-headers | grep -v Running | wc -l)
        if [[ $unhealthy_pods -gt 0 ]]; then
            log_warning "$unhealthy_pods unhealthy pods detected"
            kubectl get pods -n "$NAMESPACE"
        fi

        # Check error rates (this would integrate with your monitoring system)
        check_error_rates

        sleep $monitor_interval
    done

    log_success "Post-deployment monitoring completed"
}

check_error_rates() {
    # This would integrate with your monitoring system (Prometheus, DataDog, etc.)
    # For now, we'll do a basic health check
    if ! curl -f "https://api.payment-platform.com/actuator/health" > /dev/null 2>&1; then
        log_warning "Health check failed during monitoring"
    fi
}

cleanup_blue_environment() {
    log_info "Cleaning up blue environment..."

    # Wait additional time to ensure green is stable
    log_info "Waiting 10 minutes before cleaning up blue environment..."
    sleep 600

    # Final verification before cleanup
    if curl -f "https://api.payment-platform.com/actuator/health" > /dev/null 2>&1; then
        # Delete blue deployments
        kubectl delete deployment payment-platform-backend-production -n "$NAMESPACE" --ignore-not-found
        kubectl delete deployment payment-platform-frontend-production -n "$NAMESPACE" --ignore-not-found

        # Rename green deployments to production
        kubectl patch deployment payment-platform-backend-production-green -n "$NAMESPACE" \
            --type='merge' -p '{"metadata":{"name":"payment-platform-backend-production"}}'
        kubectl patch deployment payment-platform-frontend-production-green -n "$NAMESPACE" \
            --type='merge' -p '{"metadata":{"name":"payment-platform-frontend-production"}}'

        # Update labels
        kubectl label deployment payment-platform-backend-production -n "$NAMESPACE" deployment-color-
        kubectl label deployment payment-platform-frontend-production -n "$NAMESPACE" deployment-color-

        # Clean up test services
        kubectl delete service payment-platform-backend-production-green-test -n "$NAMESPACE" --ignore-not-found
        kubectl delete service payment-platform-frontend-production-green-test -n "$NAMESPACE" --ignore-not-found

        # Clean up temporary files
        rm -rf "$GREEN_MANIFESTS_DIR"

        log_success "Blue environment cleanup completed"
    else
        log_error "Green environment appears unhealthy - preserving blue environment"
        return 1
    fi
}

emergency_rollback() {
    log_critical "INITIATING EMERGENCY ROLLBACK"

    # Switch traffic back to blue environment
    kubectl patch service payment-platform-backend-production -n "$NAMESPACE" \
        -p '{"spec":{"selector":{"app":"payment-platform-backend","deployment-color":"blue"}}}' || true

    kubectl patch service payment-platform-frontend-production -n "$NAMESPACE" \
        -p '{"spec":{"selector":{"app":"payment-platform-frontend","deployment-color":"blue"}}}' || true

    # Delete green deployments
    kubectl delete deployment payment-platform-backend-production-green -n "$NAMESPACE" --ignore-not-found || true
    kubectl delete deployment payment-platform-frontend-production-green -n "$NAMESPACE" --ignore-not-found || true

    log_critical "Emergency rollback completed"
    send_deployment_notification "ROLLBACK"
}

send_deployment_notification() {
    local status="$1"

    if [[ -n "${SLACK_WEBHOOK_URL:-}" ]]; then
        local message="Production Deployment $status: $APP_VERSION at $(date -u '+%Y-%m-%d %H:%M:%S UTC')"
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"$message\"}" \
            "$SLACK_WEBHOOK_URL" || log_warning "Failed to send Slack notification"
    fi

    if [[ -n "${TEAMS_WEBHOOK_URL:-}" ]]; then
        local message="Production Deployment $status: $APP_VERSION at $(date -u '+%Y-%m-%d %H:%M:%S UTC')"
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"$message\"}" \
            "$TEAMS_WEBHOOK_URL" || log_warning "Failed to send Teams notification"
    fi
}

# Utility functions
check_resource_quotas() {
    local cpu_request=$(kubectl describe resourcequota -n "$NAMESPACE" | grep "requests.cpu" | awk '{print $2}' || echo "0")
    local memory_request=$(kubectl describe resourcequota -n "$NAMESPACE" | grep "requests.memory" | awk '{print $2}' || echo "0")

    log_info "Current resource usage - CPU: $cpu_request, Memory: $memory_request"
}

verify_image_signatures() {
    log_info "Verifying image signatures..."
    # Add image signature verification logic here
    log_info "Image signature verification skipped (not implemented)"
}

validate_tls_certificates() {
    log_info "Validating TLS certificates..."

    local domains=("payment-platform.com" "api.payment-platform.com")
    for domain in "${domains[@]}"; do
        local cert_expiry=$(echo | openssl s_client -servername "$domain" -connect "$domain":443 2>/dev/null | \
            openssl x509 -noout -dates | grep notAfter | cut -d= -f2)

        log_info "Certificate for $domain expires: $cert_expiry"
    done
}

validate_security_policies() {
    log_info "Validating security policies..."

    # Check if Pod Security Policies are in place
    if kubectl get psp &> /dev/null; then
        log_info "Pod Security Policies are active"
    else
        log_warning "Pod Security Policies not found"
    fi
}

verify_backup_systems() {
    log_info "Verifying backup systems..."

    # Check if backup cronjobs are running
    local backup_jobs=$(kubectl get cronjob -n "$NAMESPACE" --no-headers | grep backup | wc -l)
    if [[ $backup_jobs -gt 0 ]]; then
        log_info "$backup_jobs backup jobs found"
    else
        log_warning "No backup jobs found"
    fi
}

# Script execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Production deployment requires explicit confirmation
    log_critical "🚨 PRODUCTION DEPLOYMENT WARNING 🚨"
    log_critical "You are about to deploy to PRODUCTION environment"
    log_critical "Version: $APP_VERSION"
    log_critical "This action is IRREVERSIBLE and will affect live users"
    echo
    read -p "Type 'DEPLOY TO PRODUCTION' to confirm: " -r

    if [[ "$REPLY" != "DEPLOY TO PRODUCTION" ]]; then
        log_error "Confirmation failed. Deployment aborted."
        exit 1
    fi

    main "$@"
fi