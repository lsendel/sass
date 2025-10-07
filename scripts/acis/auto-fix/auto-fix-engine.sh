#!/bin/bash
# ACIS Auto-Fix Engine
# Automated Continuous Improvement System - Auto-Fix Engine
# Version: 1.0.0

set -eo pipefail

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
ACIS_UTILS_DIR="$(cd "$SCRIPT_DIR/../utils" && pwd)"

# Source utilities
source "$ACIS_UTILS_DIR/logger.sh"
source "$ACIS_UTILS_DIR/config-reader.sh"

# Change to project root
cd "$PROJECT_ROOT"

# Initialize
AF_RUN_ID="af-$(date +%Y%m%d-%H%M%S)"
export CORRELATION_ID="${CORRELATION_ID:-$AF_RUN_ID}"

log_section "Auto-Fix Engine: $AF_RUN_ID"

# ============================================================================
# CONFIGURATION
# ============================================================================

QG_RESULTS_DIR=".acis/quality-gates"
AF_RESULTS_DIR=".acis/auto-fix"
mkdir -p "$AF_RESULTS_DIR"

# Track fixes applied
FIXES_APPLIED=()

# ============================================================================
# UTILITY FUNCTIONS
# ============================================================================

record_fix() {
    local fix_type="$1"
    local description="$2"
    local status="$3"

    FIXES_APPLIED+=("$fix_type:$status:$description")

    cat >> "$AF_RESULTS_DIR/fixes.log" << EOF
$(date -Iseconds) [$fix_type] $status: $description
EOF
}

get_gate_status() {
    local gate_name="$1"
    if [[ -f "$QG_RESULTS_DIR/${gate_name}.json" ]]; then
        jq -r '.status' "$QG_RESULTS_DIR/${gate_name}.json" 2>/dev/null || echo "UNKNOWN"
    else
        echo "UNKNOWN"
    fi
}

# ============================================================================
# CODE QUALITY FIXES
# ============================================================================

fix_code_quality() {
    log_info "Attempting code quality fixes..."

    local fixes_applied=0

    # Backend fixes
    if [[ -d "backend" ]]; then
        log_info "Fixing backend code quality issues..."

        cd backend

        # Try to auto-fix checkstyle violations
        if [[ -f "build/reports/checkstyle/main.xml" ]]; then
            # Apply Google Java Format if available
            if command -v google-java-format &> /dev/null; then
                log_info "Applying Google Java Format..."
                find src/main/java -name "*.java" -exec google-java-format --replace {} \; 2>/dev/null || true
                fixes_applied=$((fixes_applied + 1))
                record_fix "code-quality" "Applied Google Java Format to backend" "SUCCESS"
            fi

            # Run spotless if available
            if ./gradlew tasks --all | grep -q "spotlessApply" 2>/dev/null; then
                log_info "Running Spotless auto-formatting..."
                if ./gradlew spotlessApply 2>/dev/null; then
                    fixes_applied=$((fixes_applied + 1))
                    record_fix "code-quality" "Applied Spotless formatting to backend" "SUCCESS"
                else
                    record_fix "code-quality" "Failed to apply Spotless formatting" "FAILED"
                fi
            fi
        fi

        cd ..
    fi

    # Frontend fixes
    if [[ -d "frontend" ]]; then
        log_info "Fixing frontend code quality issues..."

        cd frontend

        # Auto-fix ESLint issues
        if command -v npm &> /dev/null; then
            log_info "Running ESLint auto-fix..."
            if npm run lint -- --fix 2>/dev/null; then
                fixes_applied=$((fixes_applied + 1))
                record_fix "code-quality" "Applied ESLint auto-fixes to frontend" "SUCCESS"
            else
                record_fix "code-quality" "Failed to apply ESLint auto-fixes" "FAILED"
            fi

            # Run Prettier if available
            if npm list prettier &>/dev/null || npm list --global prettier &>/dev/null; then
                log_info "Running Prettier formatting..."
                if npx prettier --write "src/**/*.{js,jsx,ts,tsx,json,css,scss,md}" 2>/dev/null; then
                    fixes_applied=$((fixes_applied + 1))
                    record_fix "code-quality" "Applied Prettier formatting to frontend" "SUCCESS"
                else
                    record_fix "code-quality" "Failed to apply Prettier formatting" "FAILED"
                fi
            fi
        fi

        cd ..
    fi

    log_info "Code quality fixes applied: $fixes_applied"
    return $([ $fixes_applied -gt 0 ] && echo 0 || echo 1)
}

# ============================================================================
# SECURITY FIXES
# ============================================================================

fix_security() {
    log_info "Attempting security fixes..."

    local fixes_applied=0

    # Backend security fixes
    if [[ -d "backend" ]]; then
        log_info "Fixing backend security issues..."

        cd backend

        # Update Gradle dependencies if possible
        if [[ -f "gradle.properties" ]]; then
            log_info "Checking for dependency updates..."

            # Use Gradle Versions Plugin if available
            if ./gradlew tasks --all | grep -q "dependencyUpdates" 2>/dev/null; then
                if ./gradlew dependencyUpdates 2>/dev/null; then
                    record_fix "security" "Checked for dependency updates" "SUCCESS"
                fi
            fi
        fi

        cd ..
    fi

    # Frontend security fixes
    if [[ -d "frontend" ]]; then
        log_info "Fixing frontend security issues..."

        cd frontend

        # Run npm audit fix
        if command -v npm &> /dev/null; then
            log_info "Running npm audit fix..."
            if npm audit fix 2>/dev/null; then
                fixes_applied=$((fixes_applied + 1))
                record_fix "security" "Applied npm audit fixes" "SUCCESS"
            else
                # Try force fix for breaking changes
                if npm audit fix --force 2>/dev/null; then
                    fixes_applied=$((fixes_applied + 1))
                    record_fix "security" "Applied npm audit fixes (force)" "SUCCESS"
                else
                    record_fix "security" "Failed to apply npm audit fixes" "FAILED"
                fi
            fi
        fi

        cd ..
    fi

    log_info "Security fixes applied: $fixes_applied"
    return $([ $fixes_applied -gt 0 ] && echo 0 || echo 1)
}

# ============================================================================
# TEST COVERAGE FIXES
# ============================================================================

fix_test_coverage() {
    log_info "Attempting test coverage improvements..."

    local fixes_applied=0

    # This is a placeholder for test coverage improvements
    # In a real implementation, this might:
    # - Generate basic test templates for uncovered classes
    # - Add missing test cases for critical methods
    # - Improve existing test assertions

    log_info "Test coverage fixes are not automatically implementable"
    log_info "Manual intervention required to improve test coverage"

    record_fix "test-coverage" "Test coverage fixes require manual intervention" "SKIPPED"

    return 1  # Indicate no automatic fixes available
}

# ============================================================================
# PERFORMANCE FIXES
# ============================================================================

fix_performance() {
    log_info "Attempting performance fixes..."

    local fixes_applied=0

    # Check application.properties for performance settings
    if [[ -f "backend/src/main/resources/application.properties" ]]; then
        log_info "Checking application performance settings..."

        # Add basic performance optimizations if not present
        local props_file="backend/src/main/resources/application.properties"

        # JVM performance settings
        if ! grep -q "spring.jpa.hibernate.ddl-auto" "$props_file"; then
            echo "spring.jpa.hibernate.ddl-auto=validate" >> "$props_file"
            fixes_applied=$((fixes_applied + 1))
        fi

        if ! grep -q "spring.jpa.show-sql" "$props_file"; then
            echo "spring.jpa.show-sql=false" >> "$props_file"
            fixes_applied=$((fixes_applied + 1))
        fi

        if ! grep -q "spring.jpa.properties.hibernate.jdbc.batch_size" "$props_file"; then
            echo "spring.jpa.properties.hibernate.jdbc.batch_size=20" >> "$props_file"
            fixes_applied=$((fixes_applied + 1))
        fi

        if [[ $fixes_applied -gt 0 ]]; then
            record_fix "performance" "Added basic JPA performance optimizations" "SUCCESS"
        fi
    fi

    # Frontend performance fixes
    if [[ -d "frontend" ]]; then
        # Check for bundle size optimizations in package.json
        if [[ -f "frontend/package.json" ]]; then
            log_info "Checking frontend build optimizations..."
            # This is mostly a placeholder - real optimizations would be more complex
            record_fix "performance" "Frontend performance optimizations require manual tuning" "SKIPPED"
        fi
    fi

    log_info "Performance fixes applied: $fixes_applied"
    return $([ $fixes_applied -gt 0 ] && echo 0 || echo 1)
}

# ============================================================================
# ARCHITECTURE FIXES
# ============================================================================

fix_architecture() {
    log_info "Attempting architecture fixes..."

    local fixes_applied=0

    # Architecture fixes are typically structural and require manual intervention
    log_info "Architecture fixes require careful manual analysis"
    log_info "Automated architecture fixes are not safe to apply"

    record_fix "architecture" "Architecture fixes require manual intervention" "SKIPPED"

    return 1  # Indicate no automatic fixes available
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    log_info "Starting auto-fix engine..."

    # Initialize results
    echo "# Auto-Fix Report" > "$AF_RESULTS_DIR/report.md"
    echo "Generated: $(date)" >> "$AF_RESULTS_DIR/report.md"
    echo "" >> "$AF_RESULTS_DIR/report.md"

    # Clear previous fixes log
    > "$AF_RESULTS_DIR/fixes.log"

    local total_fixes=0
    local successful_fixes=0

    # Check which gates failed and apply appropriate fixes
    local quality_status=$(get_gate_status "code-quality")
    local coverage_status=$(get_gate_status "test-coverage")
    local security_status=$(get_gate_status "security")
    local performance_status=$(get_gate_status "performance")
    local architecture_status=$(get_gate_status "architecture")

    log_info "Gate statuses - Quality: $quality_status, Coverage: $coverage_status, Security: $security_status, Performance: $performance_status, Architecture: $architecture_status"

    # Apply fixes based on gate failures
    if [[ "$quality_status" == "FAIL" || "$quality_status" == "WARN" ]]; then
        log_info "Applying code quality fixes..."
        if fix_code_quality; then
            successful_fixes=$((successful_fixes + 1))
        fi
        total_fixes=$((total_fixes + 1))
    fi

    if [[ "$security_status" == "FAIL" || "$security_status" == "WARN" ]]; then
        log_info "Applying security fixes..."
        if fix_security; then
            successful_fixes=$((successful_fixes + 1))
        fi
        total_fixes=$((total_fixes + 1))
    fi

    if [[ "$coverage_status" == "FAIL" || "$coverage_status" == "WARN" ]]; then
        log_info "Attempting test coverage fixes..."
        if fix_test_coverage; then
            successful_fixes=$((successful_fixes + 1))
        fi
        total_fixes=$((total_fixes + 1))
    fi

    if [[ "$performance_status" == "FAIL" || "$performance_status" == "WARN" ]]; then
        log_info "Applying performance fixes..."
        if fix_performance; then
            successful_fixes=$((successful_fixes + 1))
        fi
        total_fixes=$((total_fixes + 1))
    fi

    if [[ "$architecture_status" == "FAIL" || "$architecture_status" == "WARN" ]]; then
        log_info "Attempting architecture fixes..."
        if fix_architecture; then
            successful_fixes=$((successful_fixes + 1))
        fi
        total_fixes=$((total_fixes + 1))
    fi

    # Generate final report
    {
        echo "## Summary"
        echo "- Total fix attempts: $total_fixes"
        echo "- Successful fixes: $successful_fixes"
        echo "- Failed/Skipped fixes: $((total_fixes - successful_fixes))"
        echo ""
        echo "## Fixes Applied"
        for fix in "${FIXES_APPLIED[@]}"; do
            echo "- $fix"
        done
        echo ""
        echo "## Detailed Log"
        echo '```'
        cat "$AF_RESULTS_DIR/fixes.log"
        echo '```'
    } >> "$AF_RESULTS_DIR/report.md"

    # Summary
    log_section "Auto-Fix Summary"
    log_info "Total fix attempts: $total_fixes"
    log_info "Successful fixes: $successful_fixes"
    log_info "Failed/Skipped fixes: $((total_fixes - successful_fixes))"

    # Exit with success if any fixes were applied
    if [[ $successful_fixes -gt 0 ]]; then
        log_success "Auto-fix engine completed with $successful_fixes successful fixes"
        exit 0
    elif [[ $total_fixes -eq 0 ]]; then
        log_info "No fixes needed - all gates already passing"
        exit 0
    else
        log_warn "Auto-fix engine completed but no fixes were successful"
        exit 1
    fi
}

# Execute main function
main "$@"