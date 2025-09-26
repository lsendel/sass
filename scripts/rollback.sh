#!/bin/bash
set -euo pipefail

# Production Rollback Script for Payment Platform
# Usage: ./scripts/rollback.sh [environment] [target-version]

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENVIRONMENT="${1:-production}"
TARGET_VERSION="${2:-}"
ROLLBACK_TIMESTAMP=$(date -u '+%Y%m%d_%H%M%S')

# Kubernetes configuration
NAMESPACE="payment-platform-${ENVIRONMENT}"
CONTEXT="${ENVIRONMENT}-cluster"

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

# Error handling
trap 'log_error "Rollback failed at line $LINENO"' ERR

main() {
    log_critical "🚨 INITIATING ROLLBACK PROCEDURE 🚨"
    log_info "Environment: $ENVIRONMENT"
    log_info "Target Version: ${TARGET_VERSION:-latest stable}"
    log_info "Rollback Timestamp: $ROLLBACK_TIMESTAMP"

    # Pre-rollback validation
    pre_rollback_validation

    # Determine rollback strategy and target
    determine_rollback_target

    # Create rollback backup point
    create_rollback_backup

    # Execute rollback based on deployment type
    execute_rollback

    # Verify rollback success
    verify_rollback

    # Post-rollback cleanup
    post_rollback_cleanup

    # Send notifications
    send_rollback_notification "SUCCESS"

    log_success "🎉 ROLLBACK COMPLETED SUCCESSFULLY! 🎉"
}

pre_rollback_validation() {
    log_info "Running pre-rollback validation..."

    # Check if kubectl is available and connected
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed or not in PATH"
        exit 1
    fi

    if ! kubectl cluster-info --context="$CONTEXT" &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster: $CONTEXT"
        exit 1
    fi

    # Switch to correct context
    kubectl config use-context "$CONTEXT"

    # Check if namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_error "Namespace $NAMESPACE does not exist"
        exit 1
    fi

    # Validate current deployment state
    validate_current_deployment_state

    # Check for ongoing operations
    check_ongoing_operations

    log_success "Pre-rollback validation completed"
}

validate_current_deployment_state() {
    log_info "Validating current deployment state..."

    # Check if deployments exist
    local deployments=("payment-platform-backend-$ENVIRONMENT" "payment-platform-frontend-$ENVIRONMENT")

    for deployment in "${deployments[@]}"; do
        if kubectl get deployment "$deployment" -n "$NAMESPACE" &> /dev/null; then
            local ready_replicas=$(kubectl get deployment "$deployment" -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}')
            local desired_replicas=$(kubectl get deployment "$deployment" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}')

            if [[ "$ready_replicas" != "$desired_replicas" ]]; then
                log_warning "Deployment $deployment is not fully ready ($ready_replicas/$desired_replicas replicas)"
            else
                log_info "Deployment $deployment is healthy ($ready_replicas/$desired_replicas replicas)"
            fi
        else
            log_error "Deployment $deployment not found"
            exit 1
        fi
    done
}

check_ongoing_operations() {
    log_info "Checking for ongoing operations..."

    # Check for pending rollouts
    local pending_rollouts=$(kubectl get deployments -n "$NAMESPACE" -o json | jq -r '.items[] | select(.status.conditions[]? | select(.type=="Progressing" and .status=="True" and .reason=="ReplicaSetUpdated")) | .metadata.name')

    if [[ -n "$pending_rollouts" ]]; then
        log_warning "Found pending rollouts: $pending_rollouts"
        read -p "Continue with rollback despite pending rollouts? (y/N): " -n 1 -r
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_error "Rollback aborted due to pending rollouts"
            exit 1
        fi
    fi

    # Check for recent failed pods
    local failed_pods=$(kubectl get pods -n "$NAMESPACE" --field-selector=status.phase=Failed --no-headers | wc -l)
    if [[ $failed_pods -gt 0 ]]; then
        log_warning "$failed_pods failed pods detected"
        kubectl get pods -n "$NAMESPACE" --field-selector=status.phase=Failed
    fi
}

determine_rollback_target() {
    log_info "Determining rollback target..."

    if [[ -n "$TARGET_VERSION" ]]; then
        log_info "Using specified target version: $TARGET_VERSION"
        return
    fi

    # Get rollout history for backend deployment
    local backend_deployment="payment-platform-backend-$ENVIRONMENT"

    log_info "Available rollout history for $backend_deployment:"
    kubectl rollout history deployment/"$backend_deployment" -n "$NAMESPACE"

    # Get the previous successful revision
    local current_revision=$(kubectl get deployment "$backend_deployment" -n "$NAMESPACE" -o jsonpath='{.metadata.annotations.deployment\.kubernetes\.io/revision}')
    local previous_revision=$((current_revision - 1))

    if [[ $previous_revision -lt 1 ]]; then
        log_error "No previous revision available for rollback"
        exit 1
    fi

    log_info "Current revision: $current_revision"
    log_info "Target revision for rollback: $previous_revision"

    # Set target version to previous revision
    TARGET_VERSION="revision-$previous_revision"
}

create_rollback_backup() {
    log_info "Creating rollback backup point..."

    # Create backup directory
    local backup_dir="$PROJECT_ROOT/rollback-backups/$ROLLBACK_TIMESTAMP"
    mkdir -p "$backup_dir"

    # Backup current deployment configurations
    local deployments=("payment-platform-backend-$ENVIRONMENT" "payment-platform-frontend-$ENVIRONMENT")

    for deployment in "${deployments[@]}"; do
        log_info "Backing up deployment configuration: $deployment"
        kubectl get deployment "$deployment" -n "$NAMESPACE" -o yaml > "$backup_dir/${deployment}-deployment.yaml"
        kubectl get service "$deployment" -n "$NAMESPACE" -o yaml > "$backup_dir/${deployment}-service.yaml" 2>/dev/null || true
    done

    # Backup ingress configuration
    kubectl get ingress -n "$NAMESPACE" -o yaml > "$backup_dir/ingress.yaml" 2>/dev/null || true

    # Backup configmaps and secrets
    kubectl get configmap -n "$NAMESPACE" -o yaml > "$backup_dir/configmaps.yaml" 2>/dev/null || true

    # Create database backup if configured
    if [[ "${ROLLBACK_CREATE_DB_BACKUP:-true}" == "true" ]]; then
        log_info "Creating database backup before rollback..."
        "$SCRIPT_DIR/backup-database.sh" "$ENVIRONMENT" "rollback" || log_warning "Database backup failed"
    fi

    log_success "Rollback backup created: $backup_dir"
}

execute_rollback() {
    log_info "Executing rollback procedure..."

    case "$TARGET_VERSION" in
        revision-*)
            execute_revision_rollback
            ;;
        *)
            execute_version_rollback
            ;;
    esac
}

execute_revision_rollback() {
    log_info "Executing revision-based rollback..."

    local revision_number=$(echo "$TARGET_VERSION" | sed 's/revision-//')
    local deployments=("payment-platform-backend-$ENVIRONMENT" "payment-platform-frontend-$ENVIRONMENT")

    for deployment in "${deployments[@]}"; do
        log_info "Rolling back deployment: $deployment to revision $revision_number"

        # Perform rollback
        kubectl rollout undo deployment/"$deployment" -n "$NAMESPACE" --to-revision="$revision_number"

        # Wait for rollback to complete
        log_info "Waiting for rollback of $deployment to complete..."
        kubectl rollout status deployment/"$deployment" -n "$NAMESPACE" --timeout=600s

        log_success "Rollback completed for $deployment"
    done
}

execute_version_rollback() {
    log_info "Executing version-based rollback to: $TARGET_VERSION"

    # Update image tags to target version
    local backend_image="payment-platform-backend:$TARGET_VERSION"
    local frontend_image="payment-platform-frontend:$TARGET_VERSION"

    # Backend rollback
    log_info "Rolling back backend to $backend_image"
    kubectl set image deployment/payment-platform-backend-$ENVIRONMENT \
        backend="$backend_image" -n "$NAMESPACE"

    # Frontend rollback
    log_info "Rolling back frontend to $frontend_image"
    kubectl set image deployment/payment-platform-frontend-$ENVIRONMENT \
        frontend="$frontend_image" -n "$NAMESPACE"

    # Wait for rollouts to complete
    log_info "Waiting for rollback deployments to complete..."
    kubectl rollout status deployment/payment-platform-backend-$ENVIRONMENT -n "$NAMESPACE" --timeout=600s
    kubectl rollout status deployment/payment-platform-frontend-$ENVIRONMENT -n "$NAMESPACE" --timeout=600s
}

verify_rollback() {
    log_info "Verifying rollback success..."

    # Wait for services to stabilize
    sleep 30

    # Health checks
    verify_backend_health
    verify_frontend_health
    verify_database_connectivity
    verify_critical_functionality

    log_success "Rollback verification completed successfully"
}

verify_backend_health() {
    log_info "Verifying backend health after rollback..."

    # Get backend service endpoint
    local backend_service="payment-platform-backend-$ENVIRONMENT"

    # Port forward to test health endpoint
    kubectl port-forward service/"$backend_service" 8080:80 -n "$NAMESPACE" &
    local port_forward_pid=$!
    sleep 5

    # Test health endpoint
    local health_check_attempts=0
    local max_health_attempts=10

    while [[ $health_check_attempts -lt $max_health_attempts ]]; do
        if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_success "Backend health check passed"
            break
        else
            log_warning "Backend health check failed, attempt $((health_check_attempts + 1))/$max_health_attempts"
            sleep 10
            ((health_check_attempts++))
        fi
    done

    # Clean up port forward
    kill $port_forward_pid 2>/dev/null || true

    if [[ $health_check_attempts -eq $max_health_attempts ]]; then
        log_error "Backend health verification failed after rollback"
        return 1
    fi
}

verify_frontend_health() {
    log_info "Verifying frontend health after rollback..."

    # Get frontend service endpoint
    local frontend_service="payment-platform-frontend-$ENVIRONMENT"

    # Port forward to test frontend
    kubectl port-forward service/"$frontend_service" 3000:80 -n "$NAMESPACE" &
    local port_forward_pid=$!
    sleep 5

    # Test frontend endpoint
    if curl -sf http://localhost:3000 > /dev/null 2>&1; then
        log_success "Frontend health check passed"
    else
        log_error "Frontend health verification failed after rollback"
        kill $port_forward_pid 2>/dev/null || true
        return 1
    fi

    # Clean up port forward
    kill $port_forward_pid 2>/dev/null || true
}

verify_database_connectivity() {
    log_info "Verifying database connectivity..."

    # Test database connection through backend health endpoint
    kubectl port-forward service/payment-platform-backend-$ENVIRONMENT 8080:80 -n "$NAMESPACE" &
    local port_forward_pid=$!
    sleep 5

    # Check database health
    if curl -sf http://localhost:8080/actuator/health/db > /dev/null 2>&1; then
        log_success "Database connectivity verified"
    else
        log_warning "Database connectivity check failed"
    fi

    kill $port_forward_pid 2>/dev/null || true
}

verify_critical_functionality() {
    log_info "Verifying critical functionality..."

    # Test critical API endpoints
    kubectl port-forward service/payment-platform-backend-$ENVIRONMENT 8080:80 -n "$NAMESPACE" &
    local port_forward_pid=$!
    sleep 5

    local critical_endpoints=(
        "/actuator/health"
        "/api/v1/auth/methods"
        "/actuator/info"
    )

    local failed_endpoints=()

    for endpoint in "${critical_endpoints[@]}"; do
        if curl -sf "http://localhost:8080$endpoint" > /dev/null 2>&1; then
            log_info "✓ Critical endpoint working: $endpoint"
        else
            log_warning "✗ Critical endpoint failed: $endpoint"
            failed_endpoints+=("$endpoint")
        fi
    done

    kill $port_forward_pid 2>/dev/null || true

    if [[ ${#failed_endpoints[@]} -gt 0 ]]; then
        log_warning "Some critical endpoints are not working after rollback: ${failed_endpoints[*]}"
        read -p "Continue despite failed endpoints? (y/N): " -n 1 -r
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_error "Rollback verification failed due to critical endpoint failures"
            return 1
        fi
    else
        log_success "All critical endpoints are working"
    fi
}

post_rollback_cleanup() {
    log_info "Performing post-rollback cleanup..."

    # Clean up any failed pods
    kubectl delete pods -n "$NAMESPACE" --field-selector=status.phase=Failed --ignore-not-found=true

    # Clean up old replica sets (keep last 3)
    kubectl delete replicaset -n "$NAMESPACE" \
        $(kubectl get replicaset -n "$NAMESPACE" -o jsonpath='{.items[?(@.spec.replicas==0)].metadata.name}' | tr ' ' '\n' | tail -n +4) \
        2>/dev/null || log_info "No old replica sets to clean up"

    # Update deployment annotations
    kubectl annotate deployment payment-platform-backend-$ENVIRONMENT -n "$NAMESPACE" \
        rollback.payment-platform.com/timestamp="$ROLLBACK_TIMESTAMP" \
        rollback.payment-platform.com/target-version="$TARGET_VERSION" \
        --overwrite

    kubectl annotate deployment payment-platform-frontend-$ENVIRONMENT -n "$NAMESPACE" \
        rollback.payment-platform.com/timestamp="$ROLLBACK_TIMESTAMP" \
        rollback.payment-platform.com/target-version="$TARGET_VERSION" \
        --overwrite

    log_success "Post-rollback cleanup completed"
}

send_rollback_notification() {
    local status="$1"

    if [[ -n "${SLACK_WEBHOOK_URL:-}" ]]; then
        local color="good"
        local emoji="✅"
        local message="Rollback completed successfully"

        if [[ "$status" == "FAILED" ]]; then
            color="danger"
            emoji="❌"
            message="Rollback failed"
        fi

        local payload=$(cat <<EOF
{
    "attachments": [
        {
            "color": "$color",
            "fields": [
                {
                    "title": "$emoji Production Rollback - $ENVIRONMENT",
                    "value": "$message",
                    "short": false
                },
                {
                    "title": "Environment",
                    "value": "$ENVIRONMENT",
                    "short": true
                },
                {
                    "title": "Target Version",
                    "value": "$TARGET_VERSION",
                    "short": true
                },
                {
                    "title": "Timestamp",
                    "value": "$(date -u '+%Y-%m-%d %H:%M:%S UTC')",
                    "short": true
                }
            ]
        }
    ]
}
EOF
        )

        curl -s -X POST -H 'Content-type: application/json' \
            --data "$payload" \
            "$SLACK_WEBHOOK_URL" > /dev/null || log_warning "Failed to send Slack notification"
    fi

    # Send email notification if configured
    if [[ -n "${NOTIFICATION_EMAIL:-}" ]]; then
        local subject="Rollback $status - Payment Platform $ENVIRONMENT"
        local body="Rollback operation $status for environment $ENVIRONMENT to version $TARGET_VERSION at $(date -u '+%Y-%m-%d %H:%M:%S UTC')"

        echo "$body" | mail -s "$subject" "$NOTIFICATION_EMAIL" || log_warning "Failed to send email notification"
    fi
}

show_usage() {
    cat << EOF
Usage: $0 [environment] [target-version]

Arguments:
  environment     Target environment (staging|production) - default: production
  target-version  Target version or revision to rollback to - default: previous revision

Examples:
  $0                           # Rollback production to previous revision
  $0 staging                   # Rollback staging to previous revision
  $0 production v1.2.0         # Rollback production to specific version
  $0 production revision-5     # Rollback production to specific revision

Environment Variables:
  ROLLBACK_CREATE_DB_BACKUP   Create database backup before rollback (default: true)
  SLACK_WEBHOOK_URL          Slack webhook for notifications
  NOTIFICATION_EMAIL         Email address for notifications
EOF
}

# Script execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Handle help flag
    if [[ "${1:-}" =~ ^(-h|--help|help)$ ]]; then
        show_usage
        exit 0
    fi

    # Rollback requires explicit confirmation
    log_critical "🚨 ROLLBACK CONFIRMATION REQUIRED 🚨"
    log_critical "You are about to rollback $ENVIRONMENT environment"
    log_critical "This action will revert the application to a previous state"
    echo
    read -p "Type 'CONFIRM ROLLBACK' to proceed: " -r

    if [[ "$REPLY" != "CONFIRM ROLLBACK" ]]; then
        log_error "Rollback confirmation failed. Operation aborted."
        exit 1
    fi

    main "$@"
fi