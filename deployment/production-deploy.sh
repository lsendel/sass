#!/bin/bash

# Production Deployment Script for Spring Boot Modulith Payment Platform
# This script deploys the performance-optimized application to production

set -euo pipefail

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
readonly DEPLOYMENT_ENV="${DEPLOYMENT_ENV:-production}"
readonly HEALTH_CHECK_TIMEOUT="${HEALTH_CHECK_TIMEOUT:-300}"
readonly ROLLBACK_ENABLED="${ROLLBACK_ENABLED:-true}"

# Colors for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Cleanup function
cleanup() {
    log_info "Cleaning up temporary files..."
    rm -f /tmp/deployment-*.log
}

trap cleanup EXIT

# Validation functions
validate_environment() {
    log_info "Validating deployment environment..."

    # Check required environment variables
    local required_vars=(
        "DATABASE_URL"
        "REDIS_URL"
        "STRIPE_SECRET_KEY"
        "JWT_SECRET"
        "SPRING_PROFILES_ACTIVE"
    )

    for var in "${required_vars[@]}"; do
        if [[ -z "${!var:-}" ]]; then
            log_error "Required environment variable $var is not set"
            exit 1
        fi
    done

    # Validate Spring profile
    if [[ "$SPRING_PROFILES_ACTIVE" != "production" ]]; then
        log_error "SPRING_PROFILES_ACTIVE must be set to 'production'"
        exit 1
    fi

    log_success "Environment validation passed"
}

validate_dependencies() {
    log_info "Validating system dependencies..."

    # Check Java version
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed"
        exit 1
    fi

    local java_version
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [[ "$java_version" -lt 21 ]]; then
        log_error "Java 21 or higher is required. Found version: $java_version"
        exit 1
    fi

    # Check Docker for database connections
    if ! command -v docker &> /dev/null; then
        log_warning "Docker not found - assuming managed database services"
    fi

    log_success "Dependencies validation passed"
}

# Database operations
run_database_migrations() {
    log_info "Running database migrations..."

    cd "$PROJECT_ROOT/backend"

    # Run Flyway migrations with production profile
    if ./gradlew flywayMigrate -Pflyway-prod > /tmp/deployment-migration.log 2>&1; then
        log_success "Database migrations completed successfully"
    else
        log_error "Database migration failed. Check /tmp/deployment-migration.log"
        cat /tmp/deployment-migration.log
        exit 1
    fi
}

validate_database_performance() {
    log_info "Validating database performance indexes..."

    # Check if performance indexes are created
    local index_check_sql="
    SELECT COUNT(*) as index_count
    FROM pg_indexes
    WHERE tablename IN ('audit_events', 'users', 'organizations', 'payments')
    AND indexname LIKE '%_performance_idx';
    "

    local index_count
    index_count=$(psql "$DATABASE_URL" -t -c "$index_check_sql" | xargs)

    if [[ "$index_count" -lt 10 ]]; then
        log_warning "Expected performance indexes may not be created. Found: $index_count"
    else
        log_success "Performance indexes validated: $index_count indexes found"
    fi
}

# Application build and deployment
build_application() {
    log_info "Building application with performance optimizations..."

    cd "$PROJECT_ROOT"

    # Build backend
    log_info "Building backend..."
    cd backend
    if ./gradlew clean build -Pprod > /tmp/deployment-backend-build.log 2>&1; then
        log_success "Backend build completed"
    else
        log_error "Backend build failed. Check /tmp/deployment-backend-build.log"
        exit 1
    fi

    # Build frontend
    log_info "Building frontend..."
    cd "$PROJECT_ROOT/frontend"
    if npm run build:prod > /tmp/deployment-frontend-build.log 2>&1; then
        log_success "Frontend build completed"
    else
        log_error "Frontend build failed. Check /tmp/deployment-frontend-build.log"
        exit 1
    fi
}

# Performance monitoring setup
setup_performance_monitoring() {
    log_info "Setting up performance monitoring..."

    # Create monitoring directories
    mkdir -p /var/log/payment-platform/performance
    mkdir -p /var/lib/payment-platform/metrics

    # Set proper permissions
    chown -R app:app /var/log/payment-platform /var/lib/payment-platform 2>/dev/null || true

    log_success "Performance monitoring directories created"
}

configure_alerting() {
    log_info "Configuring production alerting..."

    # Validate alerting configuration
    if [[ -z "${SLACK_WEBHOOK_URL:-}" ]] && [[ -z "${EMAIL_SMTP_HOST:-}" ]]; then
        log_warning "No alerting channels configured. Alerts will be logged only."
    fi

    # Create alerting configuration
    cat > /tmp/alerting-config.yml <<EOF
alerting:
  enabled: true
  channels:
    slack:
      enabled: ${SLACK_WEBHOOK_URL:+true}
      webhook-url: ${SLACK_WEBHOOK_URL:-}
    email:
      enabled: ${EMAIL_SMTP_HOST:+true}
      smtp-host: ${EMAIL_SMTP_HOST:-}
      smtp-port: ${EMAIL_SMTP_PORT:-587}
      from-address: ${EMAIL_FROM_ADDRESS:-noreply@platform.com}
  thresholds:
    api-response-critical-ms: 1000
    api-response-warning-ms: 500
    db-query-critical-ms: 500
    db-query-warning-ms: 100
    memory-critical-mb: 1536
    memory-warning-mb: 1024
    cache-hit-ratio-critical: 0.6
    cache-hit-ratio-warning: 0.8
EOF

    log_success "Alerting configuration created"
}

# Application deployment
deploy_application() {
    log_info "Deploying application..."

    # Stop existing application if running
    if pgrep -f "payment-platform" > /dev/null; then
        log_info "Stopping existing application..."
        pkill -f "payment-platform" || true
        sleep 5
    fi

    # Deploy backend
    log_info "Deploying backend application..."

    local jar_file="$PROJECT_ROOT/backend/build/libs/payment-platform-*.jar"
    local app_jar="/opt/payment-platform/app.jar"

    # Copy application jar
    mkdir -p /opt/payment-platform
    cp $jar_file "$app_jar"

    # Create systemd service
    create_systemd_service

    # Start application
    systemctl daemon-reload
    systemctl enable payment-platform
    systemctl start payment-platform

    log_success "Application deployed and started"
}

create_systemd_service() {
    log_info "Creating systemd service..."

    cat > /etc/systemd/system/payment-platform.service <<EOF
[Unit]
Description=Spring Boot Modulith Payment Platform
After=network.target

[Service]
Type=simple
User=app
ExecStart=/usr/bin/java -jar /opt/payment-platform/app.jar \\
    --spring.profiles.active=production \\
    --server.port=8080 \\
    --management.endpoints.web.exposure.include=health,metrics,prometheus,performance \\
    --management.endpoint.health.show-details=always \\
    --logging.file.name=/var/log/payment-platform/application.log \\
    --logging.level.com.platform=INFO \\
    --logging.level.com.platform.shared.monitoring=DEBUG
ExecStop=/bin/kill -TERM \$MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=payment-platform

# Performance and security settings
LimitNOFILE=65536
LimitNPROC=4096
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/log/payment-platform /var/lib/payment-platform

# Environment variables
Environment=JAVA_OPTS="-Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
Environment=SPRING_PROFILES_ACTIVE=production

[Install]
WantedBy=multi-user.target
EOF

    log_success "Systemd service created"
}

# Health checks and validation
perform_health_checks() {
    log_info "Performing health checks..."

    local health_url="http://localhost:8080/actuator/health"
    local performance_url="http://localhost:8080/actuator/performance"
    local start_time=$(date +%s)

    # Wait for application to start
    log_info "Waiting for application to start..."
    while true; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))

        if [[ $elapsed -gt $HEALTH_CHECK_TIMEOUT ]]; then
            log_error "Health check timeout after ${HEALTH_CHECK_TIMEOUT}s"
            exit 1
        fi

        if curl -sf "$health_url" > /dev/null 2>&1; then
            log_success "Application is healthy"
            break
        fi

        log_info "Waiting for application... (${elapsed}s)"
        sleep 5
    done

    # Detailed health check
    log_info "Performing detailed health checks..."

    local health_response
    health_response=$(curl -s "$health_url" | jq -r '.status')

    if [[ "$health_response" != "UP" ]]; then
        log_error "Application health check failed: $health_response"
        curl -s "$health_url" | jq '.'
        exit 1
    fi

    # Performance monitoring check
    if curl -sf "$performance_url" > /dev/null 2>&1; then
        log_success "Performance monitoring endpoint is active"
    else
        log_warning "Performance monitoring endpoint not accessible"
    fi

    log_success "All health checks passed"
}

validate_performance_monitoring() {
    log_info "Validating performance monitoring..."

    # Check metrics endpoint
    local metrics_response
    metrics_response=$(curl -s "http://localhost:8080/actuator/metrics" | jq -r '.names | length')

    if [[ "$metrics_response" -gt 50 ]]; then
        log_success "Performance metrics are being collected: $metrics_response metrics"
    else
        log_warning "Limited performance metrics available: $metrics_response metrics"
    fi

    # Check custom performance metrics
    local custom_metrics=(
        "database.queries.slow"
        "cache.hits"
        "cache.misses"
        "api.response.duration"
        "database.query.duration"
    )

    for metric in "${custom_metrics[@]}"; do
        if curl -sf "http://localhost:8080/actuator/metrics/$metric" > /dev/null 2>&1; then
            log_success "Custom metric available: $metric"
        else
            log_warning "Custom metric not found: $metric"
        fi
    done
}

# Rollback functionality
create_rollback_script() {
    if [[ "$ROLLBACK_ENABLED" != "true" ]]; then
        return 0
    fi

    log_info "Creating rollback script..."

    cat > /opt/payment-platform/rollback.sh <<'EOF'
#!/bin/bash
# Rollback script for payment platform deployment

set -euo pipefail

log_info() {
    echo -e "\033[0;34m[INFO]\033[0m $1"
}

log_error() {
    echo -e "\033[0;31m[ERROR]\033[0m $1"
}

# Stop current application
log_info "Stopping current application..."
systemctl stop payment-platform

# Restore previous version (implement based on your backup strategy)
log_info "Restoring previous application version..."
if [[ -f /opt/payment-platform/app.jar.backup ]]; then
    mv /opt/payment-platform/app.jar.backup /opt/payment-platform/app.jar
    log_info "Previous version restored"
else
    log_error "No backup version found"
    exit 1
fi

# Start application
log_info "Starting previous application version..."
systemctl start payment-platform

# Wait for health check
sleep 30
if curl -sf http://localhost:8080/actuator/health > /dev/null; then
    log_info "Rollback completed successfully"
else
    log_error "Rollback failed - application not healthy"
    exit 1
fi
EOF

    chmod +x /opt/payment-platform/rollback.sh
    log_success "Rollback script created"
}

# Main deployment function
main() {
    log_info "Starting production deployment for Spring Boot Modulith Payment Platform"
    log_info "Environment: $DEPLOYMENT_ENV"
    log_info "Timestamp: $(date)"

    # Pre-deployment validation
    validate_environment
    validate_dependencies

    # Database operations
    run_database_migrations
    validate_database_performance

    # Application build and setup
    build_application
    setup_performance_monitoring
    configure_alerting

    # Create backup of current version
    if [[ -f /opt/payment-platform/app.jar ]]; then
        cp /opt/payment-platform/app.jar /opt/payment-platform/app.jar.backup
        log_info "Current version backed up"
    fi

    # Deploy application
    deploy_application

    # Post-deployment validation
    perform_health_checks
    validate_performance_monitoring

    # Create rollback capability
    create_rollback_script

    log_success "Production deployment completed successfully!"
    log_info "Application is running at: http://localhost:8080"
    log_info "Health endpoint: http://localhost:8080/actuator/health"
    log_info "Performance metrics: http://localhost:8080/actuator/performance"
    log_info "Prometheus metrics: http://localhost:8080/actuator/prometheus"

    if [[ "$ROLLBACK_ENABLED" == "true" ]]; then
        log_info "Rollback script available at: /opt/payment-platform/rollback.sh"
    fi
}

# Script entry point
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi