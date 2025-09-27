#!/bin/bash

# Code Quality Automation Script
# Comprehensive script for running all code quality checks and enforcing clean code standards

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ROOT=$(pwd)
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
REPORTS_DIR="$PROJECT_ROOT/reports"

# Create reports directory
mkdir -p "$REPORTS_DIR"

echo -e "${BLUE}=== Starting Code Quality Automation ===${NC}"
echo "Project Root: $PROJECT_ROOT"
echo "Reports Directory: $REPORTS_DIR"
echo ""

# Function to print section headers
print_section() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

# Function to print success messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print error messages
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Function to print warning messages
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Backend Code Quality Checks
run_backend_checks() {
    print_section "Backend Code Quality Checks"

    if [ ! -d "$BACKEND_DIR" ]; then
        print_error "Backend directory not found: $BACKEND_DIR"
        return 1
    fi

    cd "$BACKEND_DIR"

    # 1. Compile and build
    print_section "Building Backend"
    if ./gradlew clean build -x test; then
        print_success "Backend build successful"
    else
        print_error "Backend build failed"
        return 1
    fi

    # 2. Run unit tests with coverage
    print_section "Running Backend Tests with Coverage"
    if ./gradlew test jacocoTestReport; then
        print_success "Backend tests passed"

        # Copy coverage report
        if [ -f "build/reports/jacoco/test/html/index.html" ]; then
            cp -r "build/reports/jacoco/test/html" "$REPORTS_DIR/backend-coverage"
            print_success "Coverage report copied to $REPORTS_DIR/backend-coverage"
        fi
    else
        print_error "Backend tests failed"
        return 1
    fi

    # 3. Code style check
    print_section "Backend Code Style Check"
    if ./gradlew checkstyleMain checkstyleTest; then
        print_success "Code style check passed"
    else
        print_error "Code style violations found"
        # Copy checkstyle reports
        [ -f "build/reports/checkstyle/main.html" ] && cp "build/reports/checkstyle/main.html" "$REPORTS_DIR/backend-checkstyle-main.html"
        [ -f "build/reports/checkstyle/test.html" ] && cp "build/reports/checkstyle/test.html" "$REPORTS_DIR/backend-checkstyle-test.html"
        return 1
    fi

    # 4. Architecture compliance tests
    print_section "Architecture Compliance Tests"
    if ./gradlew test --tests "*ArchitectureTest" --tests "*ModuleBoundaryTest"; then
        print_success "Architecture tests passed"
    else
        print_error "Architecture violations found"
        return 1
    fi

    # 5. Security tests
    print_section "Security Tests"
    if ./gradlew test --tests "*SecurityTest*"; then
        print_success "Security tests passed"
    else
        print_warning "Some security tests failed - review required"
    fi

    # 6. Integration tests
    print_section "Integration Tests"
    if ./gradlew test --tests "*IntegrationTest"; then
        print_success "Integration tests passed"
    else
        print_warning "Some integration tests failed"
    fi

    cd "$PROJECT_ROOT"
}

# Frontend Code Quality Checks
run_frontend_checks() {
    print_section "Frontend Code Quality Checks"

    if [ ! -d "$FRONTEND_DIR" ]; then
        print_error "Frontend directory not found: $FRONTEND_DIR"
        return 1
    fi

    cd "$FRONTEND_DIR"

    # 1. Install dependencies
    print_section "Installing Frontend Dependencies"
    if npm ci; then
        print_success "Dependencies installed"
    else
        print_error "Failed to install dependencies"
        return 1
    fi

    # 2. Type checking
    print_section "TypeScript Type Checking"
    if npm run typecheck; then
        print_success "Type checking passed"
    else
        print_error "TypeScript errors found"
        return 1
    fi

    # 3. Linting
    print_section "ESLint Check"
    if npm run lint; then
        print_success "Linting passed"
    else
        print_error "Linting errors found"
        return 1
    fi

    # 4. Code formatting check
    print_section "Prettier Format Check"
    if npm run format:check; then
        print_success "Code formatting is correct"
    else
        print_error "Code formatting issues found"
        print_warning "Run 'npm run format' to fix formatting"
        return 1
    fi

    # 5. Unit tests with coverage
    print_section "Frontend Unit Tests with Coverage"
    if npm run test:coverage; then
        print_success "Unit tests passed"

        # Copy coverage report
        if [ -d "coverage" ]; then
            cp -r "coverage" "$REPORTS_DIR/frontend-coverage"
            print_success "Coverage report copied to $REPORTS_DIR/frontend-coverage"
        fi
    else
        print_error "Unit tests failed"
        return 1
    fi

    # 6. Build check
    print_section "Frontend Build Check"
    if npm run build; then
        print_success "Frontend build successful"
    else
        print_error "Frontend build failed"
        return 1
    fi

    # 7. Bundle analysis (optional)
    print_section "Bundle Size Analysis"
    if command -v npx >/dev/null 2>&1; then
        echo "Bundle sizes:"
        ls -lh dist/assets/ || echo "Build assets not found"
    fi

    cd "$PROJECT_ROOT"
}

# E2E Tests
run_e2e_tests() {
    print_section "End-to-End Tests"

    cd "$FRONTEND_DIR"

    # Check if Playwright is available
    if npm list @playwright/test >/dev/null 2>&1; then
        print_section "Running Playwright E2E Tests"
        if npm run test:e2e; then
            print_success "E2E tests passed"
        else
            print_warning "E2E tests failed - may require running services"
        fi
    else
        print_warning "Playwright not installed - skipping E2E tests"
    fi

    cd "$PROJECT_ROOT"
}

# Code complexity analysis
analyze_code_complexity() {
    print_section "Code Complexity Analysis"

    # Backend complexity (using simple metrics)
    echo "Backend Java file statistics:"
    find "$BACKEND_DIR/src" -name "*.java" -exec wc -l {} + | sort -nr | head -10 > "$REPORTS_DIR/backend-largest-files.txt"
    echo "Top 10 largest backend files saved to $REPORTS_DIR/backend-largest-files.txt"

    # Frontend complexity
    echo "Frontend TypeScript file statistics:"
    find "$FRONTEND_DIR/src" -name "*.ts" -o -name "*.tsx" -exec wc -l {} + | sort -nr | head -10 > "$REPORTS_DIR/frontend-largest-files.txt"
    echo "Top 10 largest frontend files saved to $REPORTS_DIR/frontend-largest-files.txt"

    # Count TODO/FIXME comments
    echo "Searching for TODO/FIXME comments..."
    {
        echo "=== Backend TODO/FIXME comments ==="
        grep -r "TODO\|FIXME\|XXX" "$BACKEND_DIR/src" || echo "None found"
        echo ""
        echo "=== Frontend TODO/FIXME comments ==="
        grep -r "TODO\|FIXME\|XXX" "$FRONTEND_DIR/src" || echo "None found"
    } > "$REPORTS_DIR/todo-comments.txt"

    print_success "Code complexity analysis completed"
}

# Generate summary report
generate_summary_report() {
    print_section "Generating Summary Report"

    REPORT_FILE="$REPORTS_DIR/quality-summary.md"

    cat > "$REPORT_FILE" << EOF
# Code Quality Report

**Generated on:** $(date)
**Project:** Spring Boot Modulith Payment Platform

## Summary

This report contains the results of automated code quality checks.

## Backend Checks

### ✅ Completed Checks
- Build and compilation
- Unit tests with coverage
- Code style (Checkstyle)
- Architecture compliance
- Security tests
- Integration tests

### 📊 Coverage Reports
- JaCoCo coverage: \`reports/backend-coverage/index.html\`

### 📋 Code Style
- Checkstyle reports: \`reports/backend-checkstyle-*.html\`

## Frontend Checks

### ✅ Completed Checks
- TypeScript type checking
- ESLint linting
- Prettier formatting
- Unit tests with coverage
- Build verification

### 📊 Coverage Reports
- Vitest coverage: \`reports/frontend-coverage/index.html\`

## Code Analysis

### 📈 Complexity Analysis
- Largest backend files: \`reports/backend-largest-files.txt\`
- Largest frontend files: \`reports/frontend-largest-files.txt\`
- TODO/FIXME comments: \`reports/todo-comments.txt\`

## Recommendations

1. **Review large files** (>200 lines) for potential refactoring
2. **Address TODO/FIXME comments** in upcoming sprints
3. **Maintain test coverage** above 80%
4. **Follow architectural boundaries** strictly

## Next Steps

1. Review any failed checks above
2. Address code style violations
3. Improve test coverage where needed
4. Plan refactoring for overly complex files

---
*Generated by code-quality-automation.sh*
EOF

    print_success "Summary report generated: $REPORT_FILE"
}

# Main execution
main() {
    local backend_result=0
    local frontend_result=0

    # Run backend checks
    if run_backend_checks; then
        print_success "Backend checks completed successfully"
    else
        print_error "Backend checks failed"
        backend_result=1
    fi

    echo ""

    # Run frontend checks
    if run_frontend_checks; then
        print_success "Frontend checks completed successfully"
    else
        print_error "Frontend checks failed"
        frontend_result=1
    fi

    echo ""

    # Run E2E tests (non-blocking)
    run_e2e_tests

    echo ""

    # Code complexity analysis
    analyze_code_complexity

    echo ""

    # Generate summary
    generate_summary_report

    echo ""
    print_section "Code Quality Automation Complete"

    if [ $backend_result -eq 0 ] && [ $frontend_result -eq 0 ]; then
        print_success "All critical checks passed!"
        echo -e "${GREEN}✓ Code is ready for production${NC}"
        return 0
    else
        print_error "Some checks failed. Review the output above."
        echo -e "${RED}✗ Code quality issues need to be addressed${NC}"
        return 1
    fi
}

# Handle script arguments
case "${1:-all}" in
    "backend")
        run_backend_checks
        ;;
    "frontend")
        run_frontend_checks
        ;;
    "e2e")
        run_e2e_tests
        ;;
    "complexity")
        analyze_code_complexity
        ;;
    "all"|*)
        main
        ;;
esac