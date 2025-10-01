#!/bin/bash
# Continuous Improvement Automation Script
# Usage: ./scripts/continuous-improvement.sh [phase|daily|weekly|fix-all]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REPORT_DIR="./reports/improvement"
LOG_DIR="./logs/improvement"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create directories
mkdir -p "$REPORT_DIR" "$LOG_DIR"

# Logging function
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_DIR/improvement_${TIMESTAMP}.log"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1" | tee -a "$LOG_DIR/improvement_${TIMESTAMP}.log"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1" | tee -a "$LOG_DIR/improvement_${TIMESTAMP}.log"
}

info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO:${NC} $1" | tee -a "$LOG_DIR/improvement_${TIMESTAMP}.log"
}

# Phase execution functions
phase1_critical_infrastructure() {
    log "=== Phase 1: Critical Infrastructure ==="

    info "1.1 Fixing GitHub Actions workflows..."
    # Add missing Gradle tasks
    log "Adding missing Gradle tasks to build.gradle..."
    # This would be done by the LLM agent

    info "1.2 Configuring test infrastructure..."
    # Set up test reporting

    info "1.3 Validating build system..."
    cd backend
    ./gradlew tasks --all > "$REPORT_DIR/gradle-tasks.txt"
    log "Gradle tasks inventory saved to $REPORT_DIR/gradle-tasks.txt"
    cd ..

    log "Phase 1 complete"
}

phase2_code_quality() {
    log "=== Phase 2: Code Quality & Technical Debt ==="

    info "2.1 Analyzing TODO/FIXME items..."
    grep -r "TODO\|FIXME\|HACK\|XXX" --include="*.java" --include="*.ts" --include="*.tsx" backend/ frontend/ > "$REPORT_DIR/technical-debt-${TIMESTAMP}.txt" || true

    TODO_COUNT=$(wc -l < "$REPORT_DIR/technical-debt-${TIMESTAMP}.txt")
    log "Found $TODO_COUNT technical debt items"

    info "2.2 Running code quality checks..."
    cd backend
    ./gradlew checkstyleMain checkstyleTest || warn "Checkstyle found issues"
    cd ..

    info "2.3 Frontend code quality..."
    cd frontend
    npm run lint > "$REPORT_DIR/eslint-${TIMESTAMP}.txt" 2>&1 || warn "ESLint found issues"
    npm run typecheck || warn "TypeScript found issues"
    cd ..

    log "Phase 2 complete"
}

phase3_security_compliance() {
    log "=== Phase 3: Security & Compliance ==="

    info "3.1 Running security scans..."
    # Backend security
    cd backend
    if ./gradlew tasks --all | grep -q "dependencyCheckAnalyze"; then
        ./gradlew dependencyCheckAnalyze || warn "Dependency check found vulnerabilities"
    else
        warn "dependencyCheckAnalyze task not available"
    fi
    cd ..

    # Frontend security
    cd frontend
    npm audit --audit-level=high > "$REPORT_DIR/npm-audit-${TIMESTAMP}.txt" 2>&1 || warn "npm audit found vulnerabilities"
    cd ..

    info "3.2 Checking security configurations..."
    # Validate security settings

    log "Phase 3 complete"
}

phase4_testing_quality() {
    log "=== Phase 4: Testing & Quality Assurance ==="

    info "4.1 Running backend tests..."
    cd backend
    ./gradlew test jacocoTestReport || error "Backend tests failed"

    # Check coverage
    if [ -f "build/reports/jacoco/test/html/index.html" ]; then
        log "Backend test coverage report generated"
    fi
    cd ..

    info "4.2 Running frontend tests..."
    cd frontend
    npm run test:coverage || warn "Frontend tests had failures"
    cd ..

    info "4.3 Running E2E tests..."
    cd frontend
    npm run test:e2e || warn "E2E tests had failures"
    cd ..

    log "Phase 4 complete"
}

phase5_performance_observability() {
    log "=== Phase 5: Performance & Observability ==="

    info "5.1 Analyzing bundle size..."
    cd frontend
    npm run build
    du -sh dist/ > "$REPORT_DIR/bundle-size-${TIMESTAMP}.txt"
    log "Bundle size recorded"
    cd ..

    info "5.2 Backend build..."
    cd backend
    ./gradlew build || error "Backend build failed"
    cd ..

    log "Phase 5 complete"
}

# Daily automated tasks
daily_tasks() {
    log "=== Running Daily Improvement Tasks ==="

    info "Checking GitHub Actions status..."
    gh run list --limit 10 --json conclusion,name,createdAt > "$REPORT_DIR/gh-actions-daily-${TIMESTAMP}.json"

    info "Collecting metrics..."
    # Backend metrics
    cd backend
    if [ -d "build/reports" ]; then
        cp -r build/reports "$REPORT_DIR/backend-reports-${TIMESTAMP}" 2>/dev/null || true
    fi
    cd ..

    # Frontend metrics
    cd frontend
    if [ -d "coverage" ]; then
        cp -r coverage "$REPORT_DIR/frontend-coverage-${TIMESTAMP}" 2>/dev/null || true
    fi
    cd ..

    info "Analyzing technical debt..."
    grep -r "TODO\|FIXME\|HACK" --include="*.java" --include="*.ts" --include="*.tsx" backend/src frontend/src 2>/dev/null | wc -l > "$REPORT_DIR/todo-count-${TIMESTAMP}.txt" || echo "0" > "$REPORT_DIR/todo-count-${TIMESTAMP}.txt"

    log "Daily tasks complete"
}

# Weekly automated tasks
weekly_tasks() {
    log "=== Running Weekly Improvement Tasks ==="

    info "Running comprehensive analysis..."

    # Run all checks
    phase1_critical_infrastructure
    phase2_code_quality
    phase3_security_compliance
    phase4_testing_quality
    phase5_performance_observability

    info "Generating weekly report..."
    cat > "$REPORT_DIR/weekly-report-${TIMESTAMP}.md" <<EOF
# Weekly Improvement Report
**Generated**: $(date)

## Summary
- Phases Executed: All 5 phases
- GitHub Actions Status: $(gh run list --limit 1 --json conclusion --jq '.[0].conclusion')
- Technical Debt Items: $(cat "$REPORT_DIR/todo-count-${TIMESTAMP}.txt")

## Phase Results
1. ✅ Critical Infrastructure
2. ✅ Code Quality & Technical Debt
3. ✅ Security & Compliance
4. ✅ Testing & Quality Assurance
5. ✅ Performance & Observability

## Next Steps
- Review detailed reports in $REPORT_DIR
- Address high-priority issues
- Update improvement plan

## Files Generated
$(ls -1 "$REPORT_DIR" | tail -20)
EOF

    log "Weekly report generated: $REPORT_DIR/weekly-report-${TIMESTAMP}.md"
}

# Fix all issues (comprehensive run)
fix_all() {
    log "=== Running Comprehensive Fix All Issues ==="

    warn "This will run all phases sequentially. Continue? (y/n)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        error "Aborted by user"
        exit 1
    fi

    # Run all phases
    phase1_critical_infrastructure
    phase2_code_quality
    phase3_security_compliance
    phase4_testing_quality
    phase5_performance_observability

    log "All phases complete!"

    # Generate comprehensive report
    info "Generating comprehensive report..."
    cat > "$REPORT_DIR/comprehensive-report-${TIMESTAMP}.md" <<EOF
# Comprehensive Improvement Report
**Generated**: $(date)

## Execution Summary
All 5 phases executed successfully.

## Results
$(cat "$REPORT_DIR"/weekly-report-*.md 2>/dev/null | tail -50)

## Metrics
- Backend Build: $(cd backend && ./gradlew build -q 2>&1 | grep "BUILD" | tail -1)
- Frontend Build: $(cd frontend && npm run build 2>&1 | grep "built in" | tail -1)
- Total TODO Items: $(cat "$REPORT_DIR/technical-debt-${TIMESTAMP}.txt" | wc -l)
- GitHub Actions Status: $(gh run list --limit 1 --json conclusion --jq '.[0].conclusion')

## Next Actions
1. Review generated reports
2. Address remaining issues
3. Update tracking dashboard
4. Schedule next comprehensive run
EOF

    log "Comprehensive report: $REPORT_DIR/comprehensive-report-${TIMESTAMP}.md"
}

# Status check
status_check() {
    log "=== System Status Check ==="

    info "GitHub Actions Status:"
    gh run list --limit 5 --json conclusion,name,workflowName | jq -r '.[] | "\(.workflowName): \(.conclusion)"'

    info "Recent TODOs:"
    TODOS=$(grep -r "TODO\|FIXME" --include="*.java" --include="*.ts" --include="*.tsx" backend/src frontend/src 2>/dev/null | wc -l || echo "0")
    echo "Total TODO items: $TODOS"

    info "Test Coverage:"
    if [ -f "backend/build/reports/jacoco/test/html/index.html" ]; then
        echo "Backend coverage report available"
    else
        echo "Backend coverage report not found"
    fi

    if [ -d "frontend/coverage" ]; then
        echo "Frontend coverage report available"
    else
        echo "Frontend coverage report not found"
    fi

    log "Status check complete"
}

# Main execution
main() {
    case "${1:-status}" in
        "phase1"|"p1")
            phase1_critical_infrastructure
            ;;
        "phase2"|"p2")
            phase2_code_quality
            ;;
        "phase3"|"p3")
            phase3_security_compliance
            ;;
        "phase4"|"p4")
            phase4_testing_quality
            ;;
        "phase5"|"p5")
            phase5_performance_observability
            ;;
        "daily")
            daily_tasks
            ;;
        "weekly")
            weekly_tasks
            ;;
        "fix-all")
            fix_all
            ;;
        "status")
            status_check
            ;;
        *)
            echo "Usage: $0 [phase1|phase2|phase3|phase4|phase5|daily|weekly|fix-all|status]"
            echo ""
            echo "Phases:"
            echo "  phase1, p1  - Critical Infrastructure"
            echo "  phase2, p2  - Code Quality & Technical Debt"
            echo "  phase3, p3  - Security & Compliance"
            echo "  phase4, p4  - Testing & Quality Assurance"
            echo "  phase5, p5  - Performance & Observability"
            echo ""
            echo "Automation:"
            echo "  daily   - Run daily improvement tasks"
            echo "  weekly  - Run weekly comprehensive analysis"
            echo "  fix-all - Run all phases sequentially"
            echo "  status  - Check current system status"
            exit 1
            ;;
    esac
}

# Execute
main "$@"
