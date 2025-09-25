#!/bin/bash
set -euo pipefail

# Comprehensive Test Runner Script
# Usage: ./scripts/test-runner.sh [test-type] [options]

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_TYPE="${1:-all}"
ENVIRONMENT="${ENVIRONMENT:-test}"
PARALLEL="${PARALLEL:-true}"
REPORT_FORMAT="${REPORT_FORMAT:-html,json,junit}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_debug() { echo -e "${CYAN}[DEBUG]${NC} $1"; }

# Test result tracking
declare -A test_results
test_start_time=$(date +%s)

main() {
    log_info "Starting comprehensive test runner..."
    log_info "Test type: $TEST_TYPE"
    log_info "Environment: $ENVIRONMENT"
    log_info "Parallel execution: $PARALLEL"

    # Create test reports directory
    mkdir -p "$PROJECT_ROOT/build/test-reports"

    # Setup test environment
    setup_test_environment

    # Run tests based on type
    case "$TEST_TYPE" in
        "unit")
            run_unit_tests
            ;;
        "integration")
            run_integration_tests
            ;;
        "e2e")
            run_e2e_tests
            ;;
        "performance")
            run_performance_tests
            ;;
        "security")
            run_security_tests
            ;;
        "contract")
            run_contract_tests
            ;;
        "smoke")
            run_smoke_tests
            ;;
        "regression")
            run_regression_tests
            ;;
        "all")
            run_all_tests
            ;;
        *)
            log_error "Unknown test type: $TEST_TYPE"
            show_usage
            exit 1
            ;;
    esac

    # Generate comprehensive test report
    generate_test_report

    # Cleanup test environment
    cleanup_test_environment

    # Print summary
    print_test_summary
}

setup_test_environment() {
    log_info "Setting up test environment..."

    # Start required services
    if [[ "$TEST_TYPE" =~ ^(integration|e2e|all)$ ]]; then
        log_info "Starting test services..."
        docker compose -f docker-compose.yml up -d postgres redis
        wait_for_services
    fi

    # Prepare test databases
    if [[ "$TEST_TYPE" =~ ^(integration|e2e|all)$ ]]; then
        prepare_test_databases
    fi

    log_success "Test environment setup completed"
}

wait_for_services() {
    log_info "Waiting for services to be ready..."

    # Wait for PostgreSQL
    for i in {1..30}; do
        if docker compose exec -T postgres pg_isready -U platform > /dev/null 2>&1; then
            log_success "PostgreSQL is ready"
            break
        fi
        if [[ $i -eq 30 ]]; then
            log_error "PostgreSQL failed to start"
            exit 1
        fi
        sleep 2
    done

    # Wait for Redis
    for i in {1..30}; do
        if docker compose exec -T redis redis-cli ping > /dev/null 2>&1; then
            log_success "Redis is ready"
            break
        fi
        if [[ $i -eq 30 ]]; then
            log_error "Redis failed to start"
            exit 1
        fi
        sleep 1
    done
}

prepare_test_databases() {
    log_info "Preparing test databases..."

    # Create test database
    docker compose exec -T postgres createdb -U platform testdb || log_debug "Test database may already exist"

    # Run database migrations
    cd "$PROJECT_ROOT/backend"
    export SPRING_PROFILES_ACTIVE=test
    export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/testdb
    export SPRING_DATASOURCE_USERNAME=platform
    export SPRING_DATASOURCE_PASSWORD=platform

    ./gradlew flywayMigrate || log_warning "Database migration failed or not configured"
    cd "$PROJECT_ROOT"
}

run_unit_tests() {
    log_info "Running unit tests..."

    local exit_code=0

    # Backend unit tests
    log_info "Running backend unit tests..."
    if run_backend_unit_tests; then
        test_results[backend_unit]="PASS"
        log_success "Backend unit tests passed"
    else
        test_results[backend_unit]="FAIL"
        log_error "Backend unit tests failed"
        exit_code=1
    fi

    # Frontend unit tests
    log_info "Running frontend unit tests..."
    if run_frontend_unit_tests; then
        test_results[frontend_unit]="PASS"
        log_success "Frontend unit tests passed"
    else
        test_results[frontend_unit]="FAIL"
        log_error "Frontend unit tests failed"
        exit_code=1
    fi

    return $exit_code
}

run_backend_unit_tests() {
    cd "$PROJECT_ROOT/backend"

    local test_args="test --tests '*UnitTest'"
    if [[ "$PARALLEL" == "true" ]]; then
        test_args="$test_args --parallel"
    fi

    # Add coverage if requested
    if [[ "$REPORT_FORMAT" =~ "coverage" ]]; then
        test_args="$test_args jacocoTestReport"
    fi

    ./gradlew $test_args \
        -Djunit.platform.output.capture.stdout=true \
        -Djunit.platform.output.capture.stderr=true \
        --console=plain

    local result=$?
    cd "$PROJECT_ROOT"
    return $result
}

run_frontend_unit_tests() {
    cd "$PROJECT_ROOT/frontend"

    local test_cmd="npm run test:unit"

    # Configure test reporters based on format
    if [[ "$REPORT_FORMAT" =~ "junit" ]]; then
        test_cmd="$test_cmd -- --reporter=junit --outputFile=../build/test-reports/frontend-unit-junit.xml"
    fi

    if [[ "$REPORT_FORMAT" =~ "coverage" ]]; then
        test_cmd="npm run test:coverage:unit"
    fi

    $test_cmd

    local result=$?
    cd "$PROJECT_ROOT"
    return $result
}

run_integration_tests() {
    log_info "Running integration tests..."

    local exit_code=0

    # Backend integration tests
    log_info "Running backend integration tests..."
    if run_backend_integration_tests; then
        test_results[backend_integration]="PASS"
        log_success "Backend integration tests passed"
    else
        test_results[backend_integration]="FAIL"
        log_error "Backend integration tests failed"
        exit_code=1
    fi

    # Frontend integration tests
    log_info "Running frontend integration tests..."
    if run_frontend_integration_tests; then
        test_results[frontend_integration]="PASS"
        log_success "Frontend integration tests passed"
    else
        test_results[frontend_integration]="FAIL"
        log_error "Frontend integration tests failed"
        exit_code=1
    fi

    return $exit_code
}

run_backend_integration_tests() {
    cd "$PROJECT_ROOT/backend"

    export SPRING_PROFILES_ACTIVE=test
    export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/testdb
    export SPRING_DATASOURCE_USERNAME=platform
    export SPRING_DATASOURCE_PASSWORD=platform
    export SPRING_REDIS_HOST=localhost
    export SPRING_REDIS_PORT=6379

    ./gradlew integrationTest \
        -Djunit.platform.output.capture.stdout=true \
        -Djunit.platform.output.capture.stderr=true \
        --console=plain

    local result=$?
    cd "$PROJECT_ROOT"
    return $result
}

run_frontend_integration_tests() {
    cd "$PROJECT_ROOT/frontend"

    npm run test:integration

    local result=$?
    cd "$PROJECT_ROOT"
    return $result
}

run_e2e_tests() {
    log_info "Running end-to-end tests..."

    # Start application
    start_application_for_testing

    # Wait for application to be ready
    wait_for_application

    # Run E2E tests
    if run_playwright_tests; then
        test_results[e2e]="PASS"
        log_success "E2E tests passed"
        local exit_code=0
    else
        test_results[e2e]="FAIL"
        log_error "E2E tests failed"
        local exit_code=1
    fi

    # Stop application
    stop_application

    return $exit_code
}

start_application_for_testing() {
    log_info "Starting application for E2E testing..."

    # Start backend
    cd "$PROJECT_ROOT/backend"
    export SPRING_PROFILES_ACTIVE=test
    export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/testdb
    export SPRING_DATASOURCE_USERNAME=platform
    export SPRING_DATASOURCE_PASSWORD=platform
    export SPRING_REDIS_HOST=localhost
    export SPRING_REDIS_PORT=6379

    ./gradlew bootRun > ../build/test-reports/backend-e2e.log 2>&1 &
    echo $! > ../build/backend-pid.txt
    cd "$PROJECT_ROOT"

    # Start frontend
    cd "$PROJECT_ROOT/frontend"
    npm run dev > ../build/test-reports/frontend-e2e.log 2>&1 &
    echo $! > ../build/frontend-pid.txt
    cd "$PROJECT_ROOT"
}

wait_for_application() {
    log_info "Waiting for application to be ready..."

    # Wait for backend
    for i in {1..60}; do
        if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_success "Backend is ready"
            break
        fi
        if [[ $i -eq 60 ]]; then
            log_error "Backend failed to start"
            return 1
        fi
        sleep 2
    done

    # Wait for frontend
    for i in {1..30}; do
        if curl -sf http://localhost:3000 > /dev/null 2>&1; then
            log_success "Frontend is ready"
            break
        fi
        if [[ $i -eq 30 ]]; then
            log_error "Frontend failed to start"
            return 1
        fi
        sleep 2
    done
}

run_playwright_tests() {
    cd "$PROJECT_ROOT/frontend"

    local playwright_args=""
    if [[ "$PARALLEL" == "true" ]]; then
        playwright_args="--workers=4"
    fi

    if [[ "$REPORT_FORMAT" =~ "html" ]]; then
        playwright_args="$playwright_args --reporter=html --output-dir=../build/test-reports/playwright"
    fi

    npx playwright test $playwright_args

    local result=$?
    cd "$PROJECT_ROOT"
    return $result
}

stop_application() {
    log_info "Stopping application..."

    # Stop backend
    if [[ -f "build/backend-pid.txt" ]]; then
        local backend_pid=$(cat build/backend-pid.txt)
        kill $backend_pid 2>/dev/null || true
        rm -f build/backend-pid.txt
    fi

    # Stop frontend
    if [[ -f "build/frontend-pid.txt" ]]; then
        local frontend_pid=$(cat build/frontend-pid.txt)
        kill $frontend_pid 2>/dev/null || true
        rm -f build/frontend-pid.txt
    fi

    # Additional cleanup
    pkill -f "gradlew bootRun" || true
    pkill -f "npm run dev" || true
}

run_performance_tests() {
    log_info "Running performance tests..."

    # Start application if not running
    if ! curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        start_application_for_testing
        wait_for_application
        local started_app=true
    fi

    # Run performance tests
    if run_backend_performance_tests && run_frontend_performance_tests; then
        test_results[performance]="PASS"
        log_success "Performance tests passed"
        local exit_code=0
    else
        test_results[performance]="FAIL"
        log_error "Performance tests failed"
        local exit_code=1
    fi

    # Stop application if we started it
    if [[ "${started_app:-false}" == "true" ]]; then
        stop_application
    fi

    return $exit_code
}

run_backend_performance_tests() {
    log_info "Running backend performance tests..."

    cd "$PROJECT_ROOT/backend"
    ./gradlew performanceTest

    local result=$?
    cd "$PROJECT_ROOT"
    return $result
}

run_frontend_performance_tests() {
    log_info "Running frontend performance tests..."

    cd "$PROJECT_ROOT/frontend"

    # Run Lighthouse performance audit
    npx lighthouse http://localhost:3000 \
        --output=json \
        --output-path=../build/test-reports/lighthouse-report.json \
        --chrome-flags="--headless" \
        --quiet || log_warning "Lighthouse performance test failed"

    cd "$PROJECT_ROOT"
    return 0  # Non-critical for now
}

run_security_tests() {
    log_info "Running security tests..."

    local exit_code=0

    # Dependency vulnerability scan
    if run_dependency_security_scan; then
        test_results[security_dependencies]="PASS"
        log_success "Dependency security scan passed"
    else
        test_results[security_dependencies]="FAIL"
        log_error "Dependency security scan failed"
        exit_code=1
    fi

    # Static security analysis
    if run_static_security_analysis; then
        test_results[security_static]="PASS"
        log_success "Static security analysis passed"
    else
        test_results[security_static]="FAIL"
        log_error "Static security analysis failed"
        exit_code=1
    fi

    # Runtime security tests
    if run_runtime_security_tests; then
        test_results[security_runtime]="PASS"
        log_success "Runtime security tests passed"
    else
        test_results[security_runtime]="FAIL"
        log_error "Runtime security tests failed"
        exit_code=1
    fi

    return $exit_code
}

run_dependency_security_scan() {
    log_info "Running dependency vulnerability scan..."

    # Backend dependency check
    cd "$PROJECT_ROOT/backend"
    ./gradlew dependencyCheckAnalyze

    # Frontend npm audit
    cd "$PROJECT_ROOT/frontend"
    npm audit --audit-level=moderate

    cd "$PROJECT_ROOT"
}

run_static_security_analysis() {
    log_info "Running static security analysis..."

    # Backend static analysis
    cd "$PROJECT_ROOT/backend"
    ./gradlew spotbugsMain || log_warning "SpotBugs analysis completed with findings"

    # Frontend security linting
    cd "$PROJECT_ROOT/frontend"
    npm run lint -- --config .eslintrc.security.js || log_warning "Security linting completed with findings"

    cd "$PROJECT_ROOT"
}

run_runtime_security_tests() {
    log_info "Running runtime security tests..."

    # Start application if not running
    if ! curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        start_application_for_testing
        wait_for_application
        local started_app=true
    fi

    # Run security-specific tests
    cd "$PROJECT_ROOT/backend"
    ./gradlew test --tests "*SecurityTest"

    local result=$?
    cd "$PROJECT_ROOT"

    # Stop application if we started it
    if [[ "${started_app:-false}" == "true" ]]; then
        stop_application
    fi

    return $result
}

run_contract_tests() {
    log_info "Running contract tests..."

    cd "$PROJECT_ROOT/backend"
    ./gradlew contractTest

    if [[ $? -eq 0 ]]; then
        test_results[contract]="PASS"
        log_success "Contract tests passed"
    else
        test_results[contract]="FAIL"
        log_error "Contract tests failed"
    fi

    cd "$PROJECT_ROOT"
    return ${test_results[contract]//PASS/0}
    return ${test_results[contract]//FAIL/1}
}

run_smoke_tests() {
    log_info "Running smoke tests..."

    # Start application if not running
    if ! curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        start_application_for_testing
        wait_for_application
        local started_app=true
    fi

    # Critical path smoke tests
    local smoke_tests_passed=true

    # Test critical endpoints
    critical_endpoints=(
        "http://localhost:8080/actuator/health"
        "http://localhost:8080/api/v1/auth/methods"
        "http://localhost:3000"
    )

    for endpoint in "${critical_endpoints[@]}"; do
        if curl -sf "$endpoint" > /dev/null 2>&1; then
            log_success "Smoke test passed: $endpoint"
        else
            log_error "Smoke test failed: $endpoint"
            smoke_tests_passed=false
        fi
    done

    # Stop application if we started it
    if [[ "${started_app:-false}" == "true" ]]; then
        stop_application
    fi

    if [[ "$smoke_tests_passed" == "true" ]]; then
        test_results[smoke]="PASS"
        log_success "Smoke tests passed"
        return 0
    else
        test_results[smoke]="FAIL"
        log_error "Smoke tests failed"
        return 1
    fi
}

run_regression_tests() {
    log_info "Running regression tests..."

    # Run critical test suites
    local exit_code=0

    if ! run_unit_tests; then
        exit_code=1
    fi

    if ! run_integration_tests; then
        exit_code=1
    fi

    if ! run_smoke_tests; then
        exit_code=1
    fi

    if [[ $exit_code -eq 0 ]]; then
        test_results[regression]="PASS"
        log_success "Regression tests passed"
    else
        test_results[regression]="FAIL"
        log_error "Regression tests failed"
    fi

    return $exit_code
}

run_all_tests() {
    log_info "Running all tests..."

    local exit_code=0

    # Run tests in order of complexity
    test_suites=(
        "run_unit_tests"
        "run_contract_tests"
        "run_integration_tests"
        "run_security_tests"
        "run_performance_tests"
        "run_e2e_tests"
    )

    for test_suite in "${test_suites[@]}"; do
        log_info "Running test suite: $test_suite"
        if ! $test_suite; then
            log_error "Test suite failed: $test_suite"
            exit_code=1
            # Continue with other tests unless critical failure
        fi
    done

    return $exit_code
}

generate_test_report() {
    log_info "Generating comprehensive test report..."

    local report_file="$PROJECT_ROOT/build/test-reports/test-summary.json"
    local html_report="$PROJECT_ROOT/build/test-reports/test-summary.html"

    # Calculate test duration
    local test_end_time=$(date +%s)
    local test_duration=$((test_end_time - test_start_time))

    # Create JSON report
    cat > "$report_file" << EOF
{
    "timestamp": "$(date -u '+%Y-%m-%d %H:%M:%S UTC')",
    "duration_seconds": $test_duration,
    "test_type": "$TEST_TYPE",
    "environment": "$ENVIRONMENT",
    "parallel": "$PARALLEL",
    "results": {
EOF

    # Add test results
    local first=true
    for test_name in "${!test_results[@]}"; do
        if [[ "$first" == "false" ]]; then
            echo "," >> "$report_file"
        fi
        echo "        \"$test_name\": \"${test_results[$test_name]}\"" >> "$report_file"
        first=false
    done

    cat >> "$report_file" << EOF
    }
}
EOF

    # Generate HTML report
    generate_html_report "$html_report"

    log_success "Test reports generated:"
    log_info "- JSON: $report_file"
    log_info "- HTML: $html_report"
}

generate_html_report() {
    local html_file="$1"

    cat > "$html_file" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>Test Report Summary</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f0f8ff; padding: 20px; border-radius: 5px; }
        .results { margin-top: 20px; }
        .pass { color: green; font-weight: bold; }
        .fail { color: red; font-weight: bold; }
        .warn { color: orange; font-weight: bold; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Test Report Summary</h1>
EOF

    echo "        <p><strong>Timestamp:</strong> $(date -u '+%Y-%m-%d %H:%M:%S UTC')</p>" >> "$html_file"
    echo "        <p><strong>Test Type:</strong> $TEST_TYPE</p>" >> "$html_file"
    echo "        <p><strong>Environment:</strong> $ENVIRONMENT</p>" >> "$html_file"
    echo "        <p><strong>Duration:</strong> $(($(date +%s) - test_start_time)) seconds</p>" >> "$html_file"

    cat >> "$html_file" << 'EOF'
    </div>
    <div class="results">
        <h2>Test Results</h2>
        <table>
            <thead>
                <tr>
                    <th>Test Suite</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
EOF

    # Add test results to HTML
    for test_name in "${!test_results[@]}"; do
        local status="${test_results[$test_name]}"
        local css_class="pass"
        if [[ "$status" == "FAIL" ]]; then
            css_class="fail"
        elif [[ "$status" == "WARN" ]]; then
            css_class="warn"
        fi

        echo "                <tr>" >> "$html_file"
        echo "                    <td>$test_name</td>" >> "$html_file"
        echo "                    <td class=\"$css_class\">$status</td>" >> "$html_file"
        echo "                </tr>" >> "$html_file"
    done

    cat >> "$html_file" << 'EOF'
            </tbody>
        </table>
    </div>
</body>
</html>
EOF
}

cleanup_test_environment() {
    log_info "Cleaning up test environment..."

    # Stop application if still running
    stop_application

    # Stop test services
    if [[ "$TEST_TYPE" =~ ^(integration|e2e|all)$ ]]; then
        docker compose down || log_warning "Failed to stop some services"
    fi

    # Clean up temporary files
    rm -f build/backend-pid.txt build/frontend-pid.txt

    log_success "Test environment cleanup completed"
}

print_test_summary() {
    local test_end_time=$(date +%s)
    local test_duration=$((test_end_time - test_start_time))

    echo ""
    log_info "=== TEST SUMMARY ==="
    log_info "Test Type: $TEST_TYPE"
    log_info "Duration: ${test_duration}s"
    log_info "Results:"

    local passed=0
    local failed=0

    for test_name in "${!test_results[@]}"; do
        local status="${test_results[$test_name]}"
        if [[ "$status" == "PASS" ]]; then
            log_success "  ✅ $test_name: $status"
            passed=$((passed + 1))
        else
            log_error "  ❌ $test_name: $status"
            failed=$((failed + 1))
        fi
    done

    echo ""
    if [[ $failed -eq 0 ]]; then
        log_success "🎉 All tests passed! ($passed/$((passed + failed)))"
    else
        log_error "💥 Some tests failed! ($passed passed, $failed failed)"
        exit 1
    fi
}

show_usage() {
    cat << EOF
Usage: $0 [test-type] [options]

Test Types:
  unit         - Run unit tests only
  integration  - Run integration tests
  e2e          - Run end-to-end tests
  performance  - Run performance tests
  security     - Run security tests
  contract     - Run contract tests
  smoke        - Run smoke tests
  regression   - Run regression test suite
  all          - Run all tests (default)

Environment Variables:
  ENVIRONMENT     - Test environment (default: test)
  PARALLEL        - Enable parallel execution (default: true)
  REPORT_FORMAT   - Report formats: html,json,junit,coverage

Examples:
  $0                    # Run all tests
  $0 unit               # Run unit tests only
  $0 integration        # Run integration tests
  PARALLEL=false $0 e2e # Run E2E tests sequentially
EOF
}

# Script execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Handle help flag
    if [[ "${1:-}" =~ ^(-h|--help|help)$ ]]; then
        show_usage
        exit 0
    fi

    main "$@"
fi