#!/bin/bash
# ACIS Metrics Collector
# Automated Continuous Improvement System - Metrics Collector
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
RUN_ID="${1:-metrics-$(date +%Y%m%d-%H%M%S)}"
MC_RUN_ID="mc-$(date +%Y%m%d-%H%M%S)"
export CORRELATION_ID="${CORRELATION_ID:-$MC_RUN_ID}"

log_section "Metrics Collector: $MC_RUN_ID"

# ============================================================================
# CONFIGURATION
# ============================================================================

METRICS_DIR=".acis/metrics"
REPORTS_DIR=".acis/reports"
QG_RESULTS_DIR=".acis/quality-gates"
AF_RESULTS_DIR=".acis/auto-fix"
ROLLBACK_RESULTS_DIR=".acis/rollback"

mkdir -p "$METRICS_DIR" "$REPORTS_DIR"

METRICS_FILE="$METRICS_DIR/metrics-$RUN_ID.json"

# ============================================================================
# UTILITY FUNCTIONS
# ============================================================================

safe_jq() {
    local query="$1"
    local file="$2"
    local default="${3:-null}"

    if [[ -f "$file" ]] && command -v jq &> /dev/null; then
        jq -r "$query" "$file" 2>/dev/null || echo "$default"
    else
        echo "$default"
    fi
}

get_file_count() {
    local pattern="$1"
    find . -name "$pattern" -type f 2>/dev/null | wc -l
}

get_directory_size() {
    local dir="$1"
    if [[ -d "$dir" ]]; then
        du -sh "$dir" 2>/dev/null | cut -f1 || echo "0"
    else
        echo "0"
    fi
}

# ============================================================================
# CODE METRICS
# ============================================================================

collect_code_metrics() {
    log_info "Collecting code metrics..."

    local java_files=$(get_file_count "*.java")
    local js_files=$(get_file_count "*.js")
    local jsx_files=$(get_file_count "*.jsx")
    local ts_files=$(get_file_count "*.ts")
    local tsx_files=$(get_file_count "*.tsx")

    # Calculate lines of code (rough estimate)
    local java_loc=0
    local frontend_loc=0

    if command -v wc &> /dev/null; then
        java_loc=$(find . -name "*.java" -type f -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)
        frontend_loc=$(find . -name "*.js" -o -name "*.jsx" -o -name "*.ts" -o -name "*.tsx" -type f -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)
    fi

    cat > "$METRICS_DIR/code_metrics.json" << EOF
{
    "files": {
        "java": $java_files,
        "javascript": $js_files,
        "jsx": $jsx_files,
        "typescript": $ts_files,
        "tsx": $tsx_files,
        "total_source": $((java_files + js_files + jsx_files + ts_files + tsx_files))
    },
    "lines_of_code": {
        "java": $java_loc,
        "frontend": $frontend_loc,
        "total": $((java_loc + frontend_loc))
    },
    "timestamp": "$(date -Iseconds)"
}
EOF
}

# ============================================================================
# QUALITY GATE METRICS
# ============================================================================

collect_quality_gate_metrics() {
    log_info "Collecting quality gate metrics..."

    local gates=("code-quality" "test-coverage" "security" "performance" "architecture")
    local gate_results=""

    for gate in "${gates[@]}"; do
        local gate_file="$QG_RESULTS_DIR/${gate}.json"
        local status="UNKNOWN"
        local score=0

        if [[ -f "$gate_file" ]]; then
            status=$(safe_jq '.status' "$gate_file" "UNKNOWN")
            score=$(safe_jq '.score' "$gate_file" "0")
        fi

        if [[ -n "$gate_results" ]]; then
            gate_results="$gate_results,"
        fi

        gate_results="$gate_results
        \"$gate\": {
            \"status\": \"$status\",
            \"score\": $score
        }"
    done

    # Overall quality gate status
    local overall_status="UNKNOWN"
    if [[ -f "$QG_RESULTS_DIR/summary.json" ]]; then
        overall_status=$(safe_jq '.overall_status' "$QG_RESULTS_DIR/summary.json" "UNKNOWN")
    fi

    cat > "$METRICS_DIR/quality_gate_metrics.json" << EOF
{
    "overall_status": "$overall_status",
    "gates": {$gate_results
    },
    "timestamp": "$(date -Iseconds)"
}
EOF
}

# ============================================================================
# GIT METRICS
# ============================================================================

collect_git_metrics() {
    log_info "Collecting git metrics..."

    local current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
    local current_commit=$(git rev-parse HEAD 2>/dev/null || echo "unknown")
    local commit_count=$(git rev-list --count HEAD 2>/dev/null || echo 0)

    # Recent activity
    local commits_last_day=$(git rev-list --since="1 day ago" --count HEAD 2>/dev/null || echo 0)
    local commits_last_week=$(git rev-list --since="7 days ago" --count HEAD 2>/dev/null || echo 0)

    # Contributors
    local unique_authors=$(git shortlog -sn --all 2>/dev/null | wc -l || echo 0)

    # Working directory status
    local is_clean="true"
    if ! git diff-index --quiet HEAD -- 2>/dev/null; then
        is_clean="false"
    fi

    # Untracked files
    local untracked_files=$(git ls-files --others --exclude-standard 2>/dev/null | wc -l || echo 0)

    cat > "$METRICS_DIR/git_metrics.json" << EOF
{
    "repository": {
        "current_branch": "$current_branch",
        "current_commit": "$current_commit",
        "total_commits": $commit_count,
        "unique_authors": $unique_authors,
        "is_clean": $is_clean,
        "untracked_files": $untracked_files
    },
    "recent_activity": {
        "commits_last_day": $commits_last_day,
        "commits_last_week": $commits_last_week
    },
    "timestamp": "$(date -Iseconds)"
}
EOF
}

# ============================================================================
# BUILD METRICS
# ============================================================================

collect_build_metrics() {
    log_info "Collecting build metrics..."

    # Backend build metrics
    local backend_build_size="0"
    local backend_test_count=0
    local backend_build_time="unknown"

    if [[ -d "backend" ]]; then
        backend_build_size=$(get_directory_size "backend/build")

        # Count test files
        backend_test_count=$(find backend -name "*Test.java" -o -name "*Tests.java" 2>/dev/null | wc -l || echo 0)

        # Try to get build time from last gradle run
        if [[ -f "backend/build/reports/profile/profile-*.html" ]]; then
            # This is a rough estimate - actual implementation would parse the HTML
            backend_build_time="< 2 minutes"
        fi
    fi

    # Frontend build metrics
    local frontend_build_size="0"
    local frontend_test_count=0
    local node_modules_size="0"

    if [[ -d "frontend" ]]; then
        frontend_build_size=$(get_directory_size "frontend/build")
        node_modules_size=$(get_directory_size "frontend/node_modules")

        # Count test files
        frontend_test_count=$(find frontend -name "*.test.js" -o -name "*.test.jsx" -o -name "*.test.ts" -o -name "*.test.tsx" -o -name "*.spec.js" -o -name "*.spec.jsx" -o -name "*.spec.ts" -o -name "*.spec.tsx" 2>/dev/null | wc -l || echo 0)
    fi

    cat > "$METRICS_DIR/build_metrics.json" << EOF
{
    "backend": {
        "build_size": "$backend_build_size",
        "test_files": $backend_test_count,
        "build_time": "$backend_build_time"
    },
    "frontend": {
        "build_size": "$frontend_build_size",
        "node_modules_size": "$node_modules_size",
        "test_files": $frontend_test_count
    },
    "timestamp": "$(date -Iseconds)"
}
EOF
}

# ============================================================================
# ACIS METRICS
# ============================================================================

collect_acis_metrics() {
    log_info "Collecting ACIS-specific metrics..."

    # Auto-fix metrics
    local auto_fix_runs=0
    local auto_fix_successful=0
    local auto_fix_failed=0

    if [[ -d "$AF_RESULTS_DIR" ]]; then
        auto_fix_runs=$(find "$AF_RESULTS_DIR" -name "report.md" 2>/dev/null | wc -l || echo 0)
    fi

    # Rollback metrics
    local rollback_count=0
    local successful_rollbacks=0

    if [[ -d "$ROLLBACK_RESULTS_DIR" ]]; then
        rollback_count=$(find "$ROLLBACK_RESULTS_DIR" -name "rollback_report.md" 2>/dev/null | wc -l || echo 0)
        successful_rollbacks=$rollback_count  # Assume all completed rollbacks were successful
    fi

    # Quality gate history
    local quality_gate_runs=0
    local passed_gates=0
    local failed_gates=0

    if [[ -d "$QG_RESULTS_DIR" ]]; then
        quality_gate_runs=$(find "$QG_RESULTS_DIR" -name "summary.json" 2>/dev/null | wc -l || echo 0)

        # Count passed/failed from available summaries
        for summary_file in "$QG_RESULTS_DIR"/summary.json; do
            if [[ -f "$summary_file" ]]; then
                local status=$(safe_jq '.overall_status' "$summary_file" "UNKNOWN")
                case "$status" in
                    "PASS") passed_gates=$((passed_gates + 1)) ;;
                    "FAIL") failed_gates=$((failed_gates + 1)) ;;
                esac
            fi
        done
    fi

    # ACIS configuration status
    local acis_enabled="false"
    if is_acis_enabled 2>/dev/null; then
        acis_enabled="true"
    fi

    cat > "$METRICS_DIR/acis_metrics.json" << EOF
{
    "acis": {
        "enabled": $acis_enabled,
        "run_id": "$RUN_ID",
        "correlation_id": "$CORRELATION_ID"
    },
    "auto_fix": {
        "total_runs": $auto_fix_runs,
        "successful": $auto_fix_successful,
        "failed": $auto_fix_failed
    },
    "rollbacks": {
        "total_count": $rollback_count,
        "successful": $successful_rollbacks
    },
    "quality_gates": {
        "total_runs": $quality_gate_runs,
        "passed": $passed_gates,
        "failed": $failed_gates,
        "success_rate": $([ $quality_gate_runs -gt 0 ] && echo "scale=2; $passed_gates * 100 / $quality_gate_runs" | bc -l || echo "0")
    },
    "timestamp": "$(date -Iseconds)"
}
EOF
}

# ============================================================================
# SYSTEM METRICS
# ============================================================================

collect_system_metrics() {
    log_info "Collecting system metrics..."

    # Disk usage
    local project_size=$(get_directory_size ".")
    local available_space="unknown"

    if command -v df &> /dev/null; then
        available_space=$(df -h . | tail -1 | awk '{print $4}')
    fi

    # Tool availability
    local tools_available=""
    local tools=("java" "node" "npm" "git" "jq" "curl" "bc")

    for tool in "${tools[@]}"; do
        local available="false"
        if command -v "$tool" &> /dev/null; then
            available="true"
        fi

        if [[ -n "$tools_available" ]]; then
            tools_available="$tools_available,"
        fi

        tools_available="$tools_available
        \"$tool\": $available"
    done

    # Java version
    local java_version="unknown"
    if command -v java &> /dev/null; then
        java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 || echo "unknown")
    fi

    # Node version
    local node_version="unknown"
    if command -v node &> /dev/null; then
        node_version=$(node --version 2>/dev/null | sed 's/^v//' || echo "unknown")
    fi

    cat > "$METRICS_DIR/system_metrics.json" << EOF
{
    "disk": {
        "project_size": "$project_size",
        "available_space": "$available_space"
    },
    "tools": {$tools_available
    },
    "versions": {
        "java": "$java_version",
        "node": "$node_version"
    },
    "timestamp": "$(date -Iseconds)"
}
EOF
}

# ============================================================================
# AGGREGATE METRICS
# ============================================================================

aggregate_all_metrics() {
    log_info "Aggregating all metrics..."

    # Combine all metrics into a single file
    local combined_metrics="{"

    local metric_files=(
        "code_metrics.json"
        "quality_gate_metrics.json"
        "git_metrics.json"
        "build_metrics.json"
        "acis_metrics.json"
        "system_metrics.json"
    )

    for i in "${!metric_files[@]}"; do
        local file="$METRICS_DIR/${metric_files[$i]}"
        local section_name=$(basename "${metric_files[$i]}" .json)

        if [[ $i -gt 0 ]]; then
            combined_metrics="$combined_metrics,"
        fi

        combined_metrics="$combined_metrics
    \"$section_name\": "

        if [[ -f "$file" ]]; then
            combined_metrics="$combined_metrics$(cat "$file")"
        else
            combined_metrics="$combined_metrics{\"error\": \"File not found\"}"
        fi
    done

    combined_metrics="$combined_metrics,
    \"collection\": {
        \"run_id\": \"$RUN_ID\",
        \"correlation_id\": \"$CORRELATION_ID\",
        \"timestamp\": \"$(date -Iseconds)\",
        \"duration_seconds\": 0
    }
}"

    echo "$combined_metrics" > "$METRICS_FILE"

    # Create a pretty-printed version if jq is available
    if command -v jq &> /dev/null; then
        jq '.' "$METRICS_FILE" > "$METRICS_FILE.formatted" 2>/dev/null || true
    fi
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    log_info "Starting metrics collection for run: $RUN_ID"

    # Collect all metrics
    collect_code_metrics
    collect_quality_gate_metrics
    collect_git_metrics
    collect_build_metrics
    collect_acis_metrics
    collect_system_metrics

    # Aggregate all metrics
    aggregate_all_metrics

    log_success "Metrics collection completed"
    log_info "Metrics saved to: $METRICS_FILE"

    # Show summary
    log_section "Metrics Summary"

    if command -v jq &> /dev/null && [[ -f "$METRICS_FILE" ]]; then
        local total_files=$(safe_jq '.code_metrics.files.total_source' "$METRICS_FILE" "0")
        local total_loc=$(safe_jq '.code_metrics.lines_of_code.total' "$METRICS_FILE" "0")
        local overall_quality=$(safe_jq '.quality_gate_metrics.overall_status' "$METRICS_FILE" "UNKNOWN")
        local commit_count=$(safe_jq '.git_metrics.repository.total_commits' "$METRICS_FILE" "0")

        log_info "Source files: $total_files"
        log_info "Lines of code: $total_loc"
        log_info "Quality status: $overall_quality"
        log_info "Total commits: $commit_count"
    else
        log_info "Metrics aggregated successfully"
    fi

    exit 0
}

# Execute main function
main "$@"