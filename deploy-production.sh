#!/bin/bash

# Production deployment script for SASS Project Management Platform
# Comprehensive deployment with health checks, rollback capability, and monitoring

set -e  # Exit on any error

# Configuration
APP_NAME="sass-platform"
VERSION="${VERSION:-$(date +%Y%m%d%H%M%S)}"
BACKUP_DIR="./backups"
DEPLOY_DIR="./deployment"
LOG_FILE="deploy-${VERSION}.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
    exit 1
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

# Check prerequisites
check_prerequisites() {
    log "Checking deployment prerequisites..."

    # Check if Docker is installed and running
    if ! docker --version &> /dev/null; then
        error "Docker is not installed or not in PATH"
    fi

    if ! docker info &> /dev/null; then
        error "Docker daemon is not running"
    fi

    # Check if Docker Compose is available
    if ! docker compose version &> /dev/null; then
        error "Docker Compose is not available"
    fi

    # Check required environment files
    if [[ ! -f ".env.production" ]]; then
        warn ".env.production file not found, using defaults"
        create_default_env
    fi

    success "Prerequisites check completed"
}

# Create default environment file
create_default_env() {
    log "Creating default production environment file..."

cat > .env.production << EOF
# Database Configuration
DB_PASSWORD=your_secure_database_password_here
POSTGRES_DB=sass_platform
POSTGRES_USER=sass_user

# Redis Configuration
REDIS_PASSWORD=your_secure_redis_password_here

# Application Security
JWT_SECRET=your_jwt_secret_key_change_this_in_production
SESSION_SECRET=your_session_secret_key_change_this_too

# CORS and API Configuration
CORS_ORIGINS=https://yourdomain.com,https://api.yourdomain.com
API_BASE_URL=https://api.yourdomain.com
WS_BASE_URL=wss://api.yourdomain.com

# SSL Configuration
SSL_CERT_PATH=./ssl/cert.pem
SSL_KEY_PATH=./ssl/key.pem

# Monitoring
GRAFANA_PASSWORD=your_grafana_admin_password

# Scaling Configuration
BACKEND_REPLICAS=2
FRONTEND_REPLICAS=2

# Backup Configuration
BACKUP_RETENTION_DAYS=30
EOF

    warn "Please edit .env.production with your actual production values before deployment!"
}

# Database backup
backup_database() {
    if [[ "$SKIP_BACKUP" == "true" ]]; then
        log "Skipping database backup as requested"
        return
    fi

    log "Creating database backup..."

    mkdir -p "$BACKUP_DIR"

    # Check if database container is running
    if docker compose ps postgres | grep -q "Up"; then
        docker compose exec postgres pg_dump -U sass_user -d sass_platform > "$BACKUP_DIR/db-backup-${VERSION}.sql"

        if [[ $? -eq 0 ]]; then
            success "Database backup created: $BACKUP_DIR/db-backup-${VERSION}.sql"
        else
            error "Database backup failed"
        fi
    else
        log "Database container not running, skipping backup"
    fi
}

# Build and test images
build_images() {
    log "Building application images..."

    # Build backend
    log "Building backend image..."
    docker build -t "${APP_NAME}-backend:${VERSION}" \
                 -t "${APP_NAME}-backend:latest" \
                 ./backend/

    # Build frontend
    log "Building frontend image..."
    docker build -t "${APP_NAME}-frontend:${VERSION}" \
                 -t "${APP_NAME}-frontend:latest" \
                 ./frontend/

    success "Images built successfully"
}

# Run tests
run_tests() {
    if [[ "$SKIP_TESTS" == "true" ]]; then
        log "Skipping tests as requested"
        return
    fi

    log "Running application tests..."

    # Backend tests
    log "Running backend tests..."
    cd backend
    ./gradlew test --no-daemon
    if [[ $? -ne 0 ]]; then
        error "Backend tests failed"
    fi
    cd ..

    # Frontend tests
    log "Running frontend tests..."
    cd frontend
    npm test -- --run
    if [[ $? -ne 0 ]]; then
        error "Frontend tests failed"
    fi
    cd ..

    success "All tests passed"
}

# Deploy application
deploy_application() {
    log "Deploying application..."

    # Stop existing services gracefully
    log "Stopping existing services..."
    docker compose --env-file .env.production down --timeout 30

    # Start services with production configuration
    log "Starting services with production configuration..."
    docker compose --env-file .env.production up -d --build

    success "Application deployment initiated"
}

# Health checks
perform_health_checks() {
    log "Performing health checks..."

    local max_attempts=30
    local attempt=1

    # Wait for services to be healthy
    log "Waiting for services to become healthy..."

    while [[ $attempt -le $max_attempts ]]; do
        log "Health check attempt $attempt/$max_attempts..."

        # Check backend health
        if curl -f -s "http://localhost:8080/actuator/health" > /dev/null; then
            success "Backend is healthy"

            # Check frontend health
            if curl -f -s "http://localhost:80/health" > /dev/null; then
                success "Frontend is healthy"
                success "All services are healthy!"
                return 0
            fi
        fi

        log "Services not ready yet, waiting 10 seconds..."
        sleep 10
        ((attempt++))
    done

    error "Health checks failed after $max_attempts attempts"
}

# Rollback function
rollback() {
    warn "Rolling back deployment..."

    # Stop current deployment
    docker compose --env-file .env.production down

    # Restore database backup if available
    local latest_backup=$(ls -t "$BACKUP_DIR"/db-backup-*.sql | head -1)
    if [[ -n "$latest_backup" ]]; then
        log "Restoring database from: $latest_backup"
        docker compose --env-file .env.production up -d postgres redis
        sleep 10
        docker compose exec -T postgres psql -U sass_user -d sass_platform < "$latest_backup"
    fi

    # Start previous version (if available)
    log "Starting previous deployment..."
    # This would typically involve using previous image tags
    docker compose --env-file .env.production up -d

    error "Rollback completed"
}

# Cleanup old backups and images
cleanup() {
    if [[ "$SKIP_CLEANUP" == "true" ]]; then
        log "Skipping cleanup as requested"
        return
    fi

    log "Cleaning up old backups and images..."

    # Remove old backups (keep last 5)
    if [[ -d "$BACKUP_DIR" ]]; then
        ls -t "$BACKUP_DIR"/db-backup-*.sql | tail -n +6 | xargs -r rm -f
        log "Old backups cleaned up"
    fi

    # Remove dangling images
    docker image prune -f

    log "Cleanup completed"
}

# Post-deployment tasks
post_deployment() {
    log "Running post-deployment tasks..."

    # Warm up caches
    log "Warming up application caches..."
    curl -s "http://localhost:8080/api/v1/dashboard/overview" > /dev/null || true

    # Send deployment notification (placeholder)
    log "Sending deployment notification..."
    # Integration with Slack/Teams/Email would go here

    # Update monitoring dashboards
    log "Updating monitoring dashboards..."
    # Grafana dashboard updates would go here

    success "Post-deployment tasks completed"
}

# Print deployment summary
print_summary() {
    log "=== DEPLOYMENT SUMMARY ==="
    log "Application: $APP_NAME"
    log "Version: $VERSION"
    log "Deployed at: $(date)"
    log "Services:"
    docker compose --env-file .env.production ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

    log ""
    log "Access URLs:"
    log "  Frontend: http://localhost:80"
    log "  Backend API: http://localhost:8080"
    log "  Health Check: http://localhost:8080/actuator/health"
    log "  Grafana (if enabled): http://localhost:3000"

    log ""
    log "Deployment logs saved to: $LOG_FILE"

    success "Deployment completed successfully!"
}

# Trap function for cleanup on error
trap 'rollback' ERR

# Main deployment flow
main() {
    log "Starting production deployment for $APP_NAME v$VERSION"

    check_prerequisites
    backup_database
    build_images
    run_tests
    deploy_application
    perform_health_checks
    post_deployment
    cleanup
    print_summary
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-backup)
            SKIP_BACKUP=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-cleanup)
            SKIP_CLEANUP=true
            shift
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --skip-backup    Skip database backup"
            echo "  --skip-tests     Skip running tests"
            echo "  --skip-cleanup   Skip cleanup tasks"
            echo "  --version        Set deployment version"
            echo "  --help           Show this help"
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            ;;
    esac
done

# Run main deployment
main