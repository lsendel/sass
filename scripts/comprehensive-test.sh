#!/bin/bash

# Comprehensive Testing Strategy for SASS
set -e

echo "🧪 SASS Comprehensive Testing Suite"
echo "==================================="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Test results tracking
BACKEND_TESTS_PASSED=false
FRONTEND_TESTS_PASSED=false
INTEGRATION_TESTS_PASSED=false
E2E_TESTS_PASSED=false

# Function to log with colors
log_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
log_error() { echo -e "${RED}❌ $1${NC}"; }

# 1. Backend Unit Tests
echo ""
log_info "Running Backend Unit Tests..."
cd backend
if ./gradlew test --tests "*UnitTest" --continue; then
    BACKEND_TESTS_PASSED=true
    log_success "Backend unit tests passed"
else
    log_error "Backend unit tests failed"
fi

# 2. Backend Integration Tests
echo ""
log_info "Running Backend Integration Tests..."
if ./gradlew test --tests "*IntegrationTest" --continue; then
    INTEGRATION_TESTS_PASSED=true
    log_success "Backend integration tests passed"
else
    log_error "Backend integration tests failed"
fi

# 3. Generate Coverage Report
echo ""
log_info "Generating Backend Coverage Report..."
./gradlew jacocoTestReport
COVERAGE=$(grep -o 'Total.*[0-9]\+%' build/reports/jacoco/test/html/index.html | grep -o '[0-9]\+%' | head -1 || echo "0%")
echo "Current Coverage: $COVERAGE"

if [[ ${COVERAGE%\%} -ge 85 ]]; then
    log_success "Coverage target met: $COVERAGE"
else
    log_warning "Coverage below target (85%): $COVERAGE"
fi

cd ..

# 4. Frontend Unit Tests
echo ""
log_info "Running Frontend Unit Tests..."
cd frontend
if npm run test:unit -- --run; then
    FRONTEND_TESTS_PASSED=true
    log_success "Frontend unit tests passed"
else
    log_error "Frontend unit tests failed"
fi

# 5. Frontend Integration Tests
echo ""
log_info "Running Frontend Integration Tests..."
if npm run test:integration -- --run; then
    log_success "Frontend integration tests passed"
else
    log_error "Frontend integration tests failed"
fi

# 6. E2E Tests (if services are running)
echo ""
log_info "Checking if services are running for E2E tests..."
if curl -s http://localhost:8082/actuator/health > /dev/null && curl -s http://localhost:3000 > /dev/null; then
    log_info "Running E2E Tests..."
    if npm run test:e2e; then
        E2E_TESTS_PASSED=true
        log_success "E2E tests passed"
    else
        log_error "E2E tests failed"
    fi
else
    log_warning "Services not running, skipping E2E tests"
    log_info "Start services with 'make dev' to run E2E tests"
fi

cd ..

# 7. Python Constitutional Tools Tests
echo ""
log_info "Running Python Constitutional Tools Tests..."
if python3 -m pytest tests/python/ -v; then
    log_success "Python tools tests passed"
else
    log_error "Python tools tests failed"
fi

# 8. Security Tests
echo ""
log_info "Running Security Tests..."
if bash scripts/security-check.sh; then
    log_success "Security tests passed"
else
    log_warning "Security tests found issues"
fi

# Summary Report
echo ""
echo "📊 Test Summary Report"
echo "====================="
echo ""

if $BACKEND_TESTS_PASSED; then
    log_success "Backend Unit Tests: PASSED"
else
    log_error "Backend Unit Tests: FAILED"
fi

if $INTEGRATION_TESTS_PASSED; then
    log_success "Integration Tests: PASSED"
else
    log_error "Integration Tests: FAILED"
fi

if $FRONTEND_TESTS_PASSED; then
    log_success "Frontend Tests: PASSED"
else
    log_error "Frontend Tests: FAILED"
fi

if $E2E_TESTS_PASSED; then
    log_success "E2E Tests: PASSED"
else
    log_warning "E2E Tests: SKIPPED or FAILED"
fi

echo ""
echo "📋 Recommendations:"
if ! $BACKEND_TESTS_PASSED; then
    echo "• Fix backend unit tests first - they are foundational"
fi
if ! $INTEGRATION_TESTS_PASSED; then
    echo "• Review integration test configuration and dependencies"
fi
if ! $FRONTEND_TESTS_PASSED; then
    echo "• Check frontend test setup and mock configurations"
fi
if [[ ${COVERAGE%\%} -lt 85 ]]; then
    echo "• Add more unit tests to reach 85% coverage target"
fi

echo ""
echo "🎯 Next Steps:"
echo "1. Fix failing tests systematically"
echo "2. Improve test coverage where needed"
echo "3. Set up CI/CD to run these tests automatically"
echo "4. Consider adding more integration scenarios"
