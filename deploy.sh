#!/bin/bash

# Production Deployment Script for Spring Boot Modulith Payment Platform
# This script automates the deployment process for production environment

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="production"
SKIP_TESTS=false
SKIP_BUILD=false
FORCE_RECREATE=false
VERBOSE=false

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -e, --env ENVIRONMENT     Environment to deploy (production, staging) [default: production]"
    echo "  -s, --skip-tests          Skip running tests"
    echo "  -b, --skip-build          Skip building images"
    echo "  -f, --force-recreate      Force recreate containers"
    echo "  -v, --verbose             Verbose output"
    echo "  -h, --help               Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                        Deploy to production"
    echo "  $0 -e staging -s          Deploy to staging without tests"
    echo "  $0 -f -v                  Force recreate with verbose output"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -s|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -b|--skip-build)
            SKIP_BUILD=true
            shift
            ;;
        -f|--force-recreate)
            FORCE_RECREATE=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Set verbose mode
if [ "$VERBOSE" = true ]; then
    set -x
fi

print_status "Starting deployment process for environment: $ENVIRONMENT"

# Check if required files exist
if [ ! -f "docker-compose.${ENVIRONMENT}.yml" ]; then
    print_error "Docker compose file for $ENVIRONMENT not found: docker-compose.${ENVIRONMENT}.yml"
    exit 1
fi

if [ ! -f ".env.${ENVIRONMENT}" ]; then
    print_warning "Environment file not found: .env.${ENVIRONMENT}"
    print_status "Using .env.${ENVIRONMENT}.example as reference"
    if [ ! -f ".env.${ENVIRONMENT}.example" ]; then
        print_error "Example environment file not found: .env.${ENVIRONMENT}.example"
        exit 1
    fi
fi

# Check Docker and Docker Compose
print_status "Checking Docker installation..."
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed or not in PATH"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed or not in PATH"
    exit 1
fi

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    print_error "Docker daemon is not running"
    exit 1
fi

print_success "Docker installation verified"

# Load environment variables
if [ -f ".env.${ENVIRONMENT}" ]; then
    print_status "Loading environment variables from .env.${ENVIRONMENT}"
    export $(cat .env.${ENVIRONMENT} | grep -v '^#' | xargs)
else
    print_warning "No environment file found, using default values"
fi

# Pre-deployment checks
print_status "Running pre-deployment checks..."

# Check if required environment variables are set
required_vars=("POSTGRES_PASSWORD" "REDIS_PASSWORD" "STRIPE_SECRET_KEY")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        print_error "Required environment variable $var is not set"
        exit 1
    fi
done

print_success "Pre-deployment checks passed"

# Run tests if not skipped
if [ "$SKIP_TESTS" = false ]; then
    print_status "Running tests..."

    # Backend tests
    print_status "Running backend tests..."
    cd backend
    if [ "$VERBOSE" = true ]; then
        ./gradlew test --no-daemon
    else
        ./gradlew test --no-daemon > test-results.log 2>&1
    fi
    cd ..

    # Frontend tests
    print_status "Running frontend tests..."
    cd frontend
    if [ "$VERBOSE" = true ]; then
        npm test -- --run
    else
        npm test -- --run > test-results.log 2>&1
    fi
    cd ..

    print_success "All tests passed"
else
    print_warning "Skipping tests (--skip-tests flag used)"
fi

# Build images if not skipped
if [ "$SKIP_BUILD" = false ]; then
    print_status "Building Docker images..."

    if [ "$FORCE_RECREATE" = true ]; then
        docker-compose -f docker-compose.${ENVIRONMENT}.yml build --no-cache
    else
        docker-compose -f docker-compose.${ENVIRONMENT}.yml build
    fi

    print_success "Docker images built successfully"
else
    print_warning "Skipping build (--skip-build flag used)"
fi

# Create backup of current deployment
print_status "Creating backup of current deployment..."
BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

# Backup database if running
if docker-compose -f docker-compose.${ENVIRONMENT}.yml ps | grep -q postgres; then
    print_status "Backing up database..."
    docker-compose -f docker-compose.${ENVIRONMENT}.yml exec -T postgres pg_dump -U ${POSTGRES_USER:-paymentuser} ${POSTGRES_DB:-paymentplatform} > "$BACKUP_DIR/database_backup.sql"
    print_success "Database backup created: $BACKUP_DIR/database_backup.sql"
fi

# Stop existing services
print_status "Stopping existing services..."
docker-compose -f docker-compose.${ENVIRONMENT}.yml down

# Start services
print_status "Starting services..."
if [ "$FORCE_RECREATE" = true ]; then
    docker-compose -f docker-compose.${ENVIRONMENT}.yml up -d --force-recreate
else
    docker-compose -f docker-compose.${ENVIRONMENT}.yml up -d
fi

# Wait for services to be healthy
print_status "Waiting for services to be healthy..."
max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if docker-compose -f docker-compose.${ENVIRONMENT}.yml ps | grep -q "healthy"; then
        break
    fi
    sleep 10
    attempt=$((attempt + 1))
    print_status "Waiting for services... (attempt $attempt/$max_attempts)"
done

if [ $attempt -eq $max_attempts ]; then
    print_error "Services did not become healthy within expected time"
    print_status "Checking service logs..."
    docker-compose -f docker-compose.${ENVIRONMENT}.yml logs --tail=50
    exit 1
fi

# Verify deployment
print_status "Verifying deployment..."

# Check if backend is responding
backend_url="http://localhost:${BACKEND_PORT:-8080}/actuator/health"
if curl -f -s "$backend_url" > /dev/null; then
    print_success "Backend is responding at $backend_url"
else
    print_error "Backend is not responding at $backend_url"
    docker-compose -f docker-compose.${ENVIRONMENT}.yml logs backend --tail=20
    exit 1
fi

# Check if frontend is responding
frontend_url="http://localhost:${FRONTEND_PORT:-80}/health"
if curl -f -s "$frontend_url" > /dev/null; then
    print_success "Frontend is responding at $frontend_url"
else
    print_error "Frontend is not responding at $frontend_url"
    docker-compose -f docker-compose.${ENVIRONMENT}.yml logs frontend --tail=20
    exit 1
fi

# Show running services
print_status "Running services:"
docker-compose -f docker-compose.${ENVIRONMENT}.yml ps

# Show useful URLs
print_success "Deployment completed successfully!"
echo ""
echo "Service URLs:"
echo "  Frontend: http://localhost:${FRONTEND_PORT:-80}"
echo "  Backend API: http://localhost:${BACKEND_PORT:-8080}"
echo "  Backend Health: http://localhost:${BACKEND_PORT:-8080}/actuator/health"
echo "  Grafana: http://localhost:3001 (admin:${GRAFANA_ADMIN_PASSWORD:-admin})"
echo "  Prometheus: http://localhost:9090"
echo ""
echo "Logs:"
echo "  All services: docker-compose -f docker-compose.${ENVIRONMENT}.yml logs -f"
echo "  Backend only: docker-compose -f docker-compose.${ENVIRONMENT}.yml logs -f backend"
echo "  Frontend only: docker-compose -f docker-compose.${ENVIRONMENT}.yml logs -f frontend"
echo ""
echo "Backup created in: $BACKUP_DIR"

print_success "Deployment completed successfully!"
