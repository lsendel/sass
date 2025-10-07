#!/bin/bash
# ACIS Report Generator
# Automated Continuous Improvement System - Report Generator
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
RUN_ID="${1:-report-$(date +%Y%m%d-%H%M%S)}"
RG_RUN_ID="rg-$(date +%Y%m%d-%H%M%S)"
export CORRELATION_ID="${CORRELATION_ID:-$RG_RUN_ID}"

log_section "Report Generator: $RG_RUN_ID"

# ============================================================================
# CONFIGURATION
# ============================================================================

METRICS_DIR=".acis/metrics"
REPORTS_DIR=".acis/reports"
QG_RESULTS_DIR=".acis/quality-gates"

mkdir -p "$REPORTS_DIR"

METRICS_FILE="$METRICS_DIR/metrics-$RUN_ID.json"
HTML_REPORT="$REPORTS_DIR/report-$RUN_ID.html"
JSON_REPORT="$REPORTS_DIR/report-$RUN_ID.json"
MD_REPORT="$REPORTS_DIR/report-$RUN_ID.md"

# ============================================================================
# UTILITY FUNCTIONS
# ============================================================================

safe_jq() {
    local query="$1"
    local file="$2"
    local default="${3:-N/A}"

    if [[ -f "$file" ]] && command -v jq &> /dev/null; then
        jq -r "$query" "$file" 2>/dev/null || echo "$default"
    else
        echo "$default"
    fi
}

format_percentage() {
    local value="$1"
    if [[ "$value" =~ ^[0-9]+\.?[0-9]*$ ]]; then
        printf "%.1f%%" "$value"
    else
        echo "$value"
    fi
}

# ============================================================================
# JSON REPORT GENERATION
# ============================================================================

generate_json_report() {
    log_info "Generating JSON report..."

    if [[ -f "$METRICS_FILE" ]]; then
        # Add report metadata to the metrics
        local report_metadata=""
        if command -v jq &> /dev/null; then
            report_metadata=$(jq --arg run_id "$RUN_ID" --arg timestamp "$(date -Iseconds)" --arg correlation_id "$CORRELATION_ID" \
                '. + {
                    "report": {
                        "run_id": $run_id,
                        "timestamp": $timestamp,
                        "correlation_id": $correlation_id,
                        "type": "ACIS_COMPREHENSIVE_REPORT",
                        "version": "1.0.0"
                    }
                }' "$METRICS_FILE")

            echo "$report_metadata" > "$JSON_REPORT"
        else
            cp "$METRICS_FILE" "$JSON_REPORT"
        fi

        log_success "JSON report generated: $JSON_REPORT"
    else
        log_error "Metrics file not found: $METRICS_FILE"
        return 1
    fi
}

# ============================================================================
# MARKDOWN REPORT GENERATION
# ============================================================================

generate_markdown_report() {
    log_info "Generating Markdown report..."

    local timestamp=$(date)
    local metrics_available="false"

    if [[ -f "$METRICS_FILE" ]]; then
        metrics_available="true"
    fi

    cat > "$MD_REPORT" << EOF
# ACIS Comprehensive Report

**Generated:** $timestamp
**Run ID:** $RUN_ID
**Correlation ID:** $CORRELATION_ID

---

## Executive Summary

This report provides a comprehensive overview of the project's current state, including code quality, test coverage, security, performance, and architectural compliance.

EOF

    if [[ "$metrics_available" == "true" ]]; then
        # Extract key metrics
        local total_files=$(safe_jq '.code_metrics.files.total_source' "$METRICS_FILE" "0")
        local total_loc=$(safe_jq '.code_metrics.lines_of_code.total' "$METRICS_FILE" "0")
        local overall_quality=$(safe_jq '.quality_gate_metrics.overall_status' "$METRICS_FILE" "UNKNOWN")
        local current_branch=$(safe_jq '.git_metrics.repository.current_branch' "$METRICS_FILE" "unknown")

        cat >> "$MD_REPORT" << EOF
### Key Metrics
- **Source Files:** $total_files
- **Lines of Code:** $total_loc
- **Quality Status:** $overall_quality
- **Current Branch:** $current_branch

---

## Quality Gates Status

EOF

        # Quality gates table
        local gates=("code-quality" "test-coverage" "security" "performance" "architecture")

        cat >> "$MD_REPORT" << EOF
| Gate | Status | Score |
|------|--------|-------|
EOF

        for gate in "${gates[@]}"; do
            local status=$(safe_jq ".quality_gate_metrics.gates.\"$gate\".status" "$METRICS_FILE" "UNKNOWN")
            local score=$(safe_jq ".quality_gate_metrics.gates.\"$gate\".score" "$METRICS_FILE" "0")

            # Format gate name
            local gate_display=$(echo "$gate" | sed 's/-/ /g' | sed 's/\b\w/\U&/g')

            # Add status emoji
            local status_emoji=""
            case "$status" in
                "PASS") status_emoji="✅" ;;
                "WARN") status_emoji="⚠️" ;;
                "FAIL") status_emoji="❌" ;;
                *) status_emoji="❓" ;;
            esac

            cat >> "$MD_REPORT" << EOF
| $gate_display | $status_emoji $status | $score |
EOF
        done

        cat >> "$MD_REPORT" << EOF

---

## Code Metrics

### File Distribution
EOF

        local java_files=$(safe_jq '.code_metrics.files.java' "$METRICS_FILE" "0")
        local js_files=$(safe_jq '.code_metrics.files.javascript' "$METRICS_FILE" "0")
        local ts_files=$(safe_jq '.code_metrics.files.typescript' "$METRICS_FILE" "0")
        local jsx_files=$(safe_jq '.code_metrics.files.jsx' "$METRICS_FILE" "0")
        local tsx_files=$(safe_jq '.code_metrics.files.tsx' "$METRICS_FILE" "0")

        cat >> "$MD_REPORT" << EOF
- **Java Files:** $java_files
- **JavaScript Files:** $js_files
- **TypeScript Files:** $ts_files
- **JSX Files:** $jsx_files
- **TSX Files:** $tsx_files

### Lines of Code
EOF

        local java_loc=$(safe_jq '.code_metrics.lines_of_code.java' "$METRICS_FILE" "0")
        local frontend_loc=$(safe_jq '.code_metrics.lines_of_code.frontend' "$METRICS_FILE" "0")

        cat >> "$MD_REPORT" << EOF
- **Java LOC:** $java_loc
- **Frontend LOC:** $frontend_loc
- **Total LOC:** $total_loc

---

## Git Repository Status

EOF

        local commit_count=$(safe_jq '.git_metrics.repository.total_commits' "$METRICS_FILE" "0")
        local unique_authors=$(safe_jq '.git_metrics.repository.unique_authors' "$METRICS_FILE" "0")
        local is_clean=$(safe_jq '.git_metrics.repository.is_clean' "$METRICS_FILE" "false")
        local commits_last_week=$(safe_jq '.git_metrics.recent_activity.commits_last_week' "$METRICS_FILE" "0")

        local clean_status=$([ "$is_clean" == "true" ] && echo "✅ Clean" || echo "⚠️ Has uncommitted changes")

        cat >> "$MD_REPORT" << EOF
- **Total Commits:** $commit_count
- **Unique Authors:** $unique_authors
- **Working Directory:** $clean_status
- **Commits (Last Week):** $commits_last_week

---

## Build Information

EOF

        local backend_build_size=$(safe_jq '.build_metrics.backend.build_size' "$METRICS_FILE" "0")
        local frontend_build_size=$(safe_jq '.build_metrics.frontend.build_size' "$METRICS_FILE" "0")
        local backend_tests=$(safe_jq '.build_metrics.backend.test_files' "$METRICS_FILE" "0")
        local frontend_tests=$(safe_jq '.build_metrics.frontend.test_files' "$METRICS_FILE" "0")

        cat >> "$MD_REPORT" << EOF
### Backend
- **Build Size:** $backend_build_size
- **Test Files:** $backend_tests

### Frontend
- **Build Size:** $frontend_build_size
- **Test Files:** $frontend_tests

---

## ACIS System Status

EOF

        local acis_enabled=$(safe_jq '.acis_metrics.acis.enabled' "$METRICS_FILE" "false")
        local auto_fix_runs=$(safe_jq '.acis_metrics.auto_fix.total_runs' "$METRICS_FILE" "0")
        local rollback_count=$(safe_jq '.acis_metrics.rollbacks.total_count' "$METRICS_FILE" "0")
        local qg_success_rate=$(safe_jq '.acis_metrics.quality_gates.success_rate' "$METRICS_FILE" "0")

        local acis_status=$([ "$acis_enabled" == "true" ] && echo "✅ Enabled" || echo "❌ Disabled")

        cat >> "$MD_REPORT" << EOF
- **ACIS Status:** $acis_status
- **Auto-Fix Runs:** $auto_fix_runs
- **Rollbacks:** $rollback_count
- **Quality Gate Success Rate:** $(format_percentage "$qg_success_rate")

---

## System Information

EOF

        local project_size=$(safe_jq '.system_metrics.disk.project_size' "$METRICS_FILE" "unknown")
        local java_version=$(safe_jq '.system_metrics.versions.java' "$METRICS_FILE" "unknown")
        local node_version=$(safe_jq '.system_metrics.versions.node' "$METRICS_FILE" "unknown")

        cat >> "$MD_REPORT" << EOF
- **Project Size:** $project_size
- **Java Version:** $java_version
- **Node Version:** $node_version

EOF

    else
        cat >> "$MD_REPORT" << EOF
⚠️ **Metrics data not available**

The detailed metrics could not be loaded. This might be because:
- The metrics collection process hasn't run yet
- The metrics file is missing or corrupted
- There was an error during metrics collection

Please run the metrics collector first:
\`\`\`bash
./scripts/acis/metrics/metrics-collector.sh
\`\`\`

EOF
    fi

    cat >> "$MD_REPORT" << EOF
---

## Recommendations

### Immediate Actions
1. **Review Failed Quality Gates** - Address any gates marked as FAIL
2. **Check Security Vulnerabilities** - Update dependencies with known vulnerabilities
3. **Improve Test Coverage** - Add tests for uncovered code paths

### Long-term Improvements
1. **Code Quality** - Establish and enforce coding standards
2. **Documentation** - Ensure all modules are properly documented
3. **Performance** - Monitor and optimize application performance
4. **Architecture** - Regular architecture reviews and compliance checks

---

**Report Generated by ACIS v1.0.0**
*Automated Continuous Improvement System*
EOF

    log_success "Markdown report generated: $MD_REPORT"
}

# ============================================================================
# HTML REPORT GENERATION
# ============================================================================

generate_html_report() {
    log_info "Generating HTML report..."

    local timestamp=$(date)
    local metrics_available="false"

    if [[ -f "$METRICS_FILE" ]]; then
        metrics_available="true"
    fi

    cat > "$HTML_REPORT" << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ACIS Comprehensive Report</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
            line-height: 1.6;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 2.5em;
            font-weight: 300;
        }
        .header .subtitle {
            margin: 10px 0 0 0;
            opacity: 0.9;
        }
        .content {
            padding: 30px;
        }
        .section {
            margin-bottom: 40px;
        }
        .section h2 {
            color: #333;
            border-bottom: 2px solid #667eea;
            padding-bottom: 10px;
            margin-bottom: 20px;
        }
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .metric-card {
            background: #f8f9fa;
            border: 1px solid #e9ecef;
            border-radius: 8px;
            padding: 20px;
            text-align: center;
        }
        .metric-card .value {
            font-size: 2em;
            font-weight: bold;
            color: #667eea;
            margin-bottom: 5px;
        }
        .metric-card .label {
            color: #6c757d;
            font-size: 0.9em;
        }
        .quality-gates {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-bottom: 30px;
        }
        .gate-card {
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 15px;
            text-align: center;
            transition: transform 0.2s;
        }
        .gate-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        }
        .gate-card.pass {
            border-color: #28a745;
            background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
        }
        .gate-card.warn {
            border-color: #ffc107;
            background: linear-gradient(135deg, #fff3cd 0%, #ffeeba 100%);
        }
        .gate-card.fail {
            border-color: #dc3545;
            background: linear-gradient(135deg, #f8d7da 0%, #f5c6cb 100%);
        }
        .gate-icon {
            font-size: 2em;
            margin-bottom: 10px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
        }
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #dee2e6;
        }
        th {
            background-color: #f8f9fa;
            font-weight: 600;
        }
        .status-pass { color: #28a745; }
        .status-warn { color: #ffc107; }
        .status-fail { color: #dc3545; }
        .footer {
            background: #f8f9fa;
            padding: 20px 30px;
            text-align: center;
            color: #6c757d;
            border-top: 1px solid #dee2e6;
        }
        .error-message {
            background: #f8d7da;
            border: 1px solid #f5c6cb;
            color: #721c24;
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>ACIS Comprehensive Report</h1>
            <div class="subtitle">Automated Continuous Improvement System</div>
EOF

    cat >> "$HTML_REPORT" << EOF
            <div style="margin-top: 15px; font-size: 0.9em;">
                Generated: $timestamp<br>
                Run ID: $RUN_ID<br>
                Correlation ID: $CORRELATION_ID
            </div>
        </div>
        <div class="content">
EOF

    if [[ "$metrics_available" == "true" ]]; then
        # Extract key metrics for display
        local total_files=$(safe_jq '.code_metrics.files.total_source' "$METRICS_FILE" "0")
        local total_loc=$(safe_jq '.code_metrics.lines_of_code.total' "$METRICS_FILE" "0")
        local overall_quality=$(safe_jq '.quality_gate_metrics.overall_status' "$METRICS_FILE" "UNKNOWN")
        local commit_count=$(safe_jq '.git_metrics.repository.total_commits' "$METRICS_FILE" "0")

        cat >> "$HTML_REPORT" << EOF
            <div class="section">
                <h2>Executive Summary</h2>
                <div class="metrics-grid">
                    <div class="metric-card">
                        <div class="value">$total_files</div>
                        <div class="label">Source Files</div>
                    </div>
                    <div class="metric-card">
                        <div class="value">$total_loc</div>
                        <div class="label">Lines of Code</div>
                    </div>
                    <div class="metric-card">
                        <div class="value">$overall_quality</div>
                        <div class="label">Quality Status</div>
                    </div>
                    <div class="metric-card">
                        <div class="value">$commit_count</div>
                        <div class="label">Total Commits</div>
                    </div>
                </div>
            </div>

            <div class="section">
                <h2>Quality Gates Status</h2>
                <div class="quality-gates">
EOF

        # Generate quality gate cards
        local gates=("code-quality" "test-coverage" "security" "performance" "architecture")
        local gate_names=("Code Quality" "Test Coverage" "Security" "Performance" "Architecture")

        for i in "${!gates[@]}"; do
            local gate="${gates[$i]}"
            local gate_name="${gate_names[$i]}"
            local status=$(safe_jq ".quality_gate_metrics.gates.\"$gate\".status" "$METRICS_FILE" "UNKNOWN")
            local score=$(safe_jq ".quality_gate_metrics.gates.\"$gate\".score" "$METRICS_FILE" "0")

            local card_class="unknown"
            local icon="❓"

            case "$status" in
                "PASS") card_class="pass"; icon="✅" ;;
                "WARN") card_class="warn"; icon="⚠️" ;;
                "FAIL") card_class="fail"; icon="❌" ;;
            esac

            cat >> "$HTML_REPORT" << EOF
                    <div class="gate-card $card_class">
                        <div class="gate-icon">$icon</div>
                        <h4>$gate_name</h4>
                        <div>Status: $status</div>
                        <div>Score: $score</div>
                    </div>
EOF
        done

        cat >> "$HTML_REPORT" << EOF
                </div>
            </div>

            <div class="section">
                <h2>Detailed Metrics</h2>
                <h3>Repository Information</h3>
                <table>
                    <tr><th>Metric</th><th>Value</th></tr>
EOF

        local current_branch=$(safe_jq '.git_metrics.repository.current_branch' "$METRICS_FILE" "unknown")
        local unique_authors=$(safe_jq '.git_metrics.repository.unique_authors' "$METRICS_FILE" "0")
        local is_clean=$(safe_jq '.git_metrics.repository.is_clean' "$METRICS_FILE" "false")

        cat >> "$HTML_REPORT" << EOF
                    <tr><td>Current Branch</td><td>$current_branch</td></tr>
                    <tr><td>Total Commits</td><td>$commit_count</td></tr>
                    <tr><td>Unique Authors</td><td>$unique_authors</td></tr>
                    <tr><td>Working Directory Clean</td><td>$is_clean</td></tr>
                </table>

                <h3>Build Information</h3>
                <table>
                    <tr><th>Component</th><th>Build Size</th><th>Test Files</th></tr>
EOF

        local backend_build_size=$(safe_jq '.build_metrics.backend.build_size' "$METRICS_FILE" "0")
        local frontend_build_size=$(safe_jq '.build_metrics.frontend.build_size' "$METRICS_FILE" "0")
        local backend_tests=$(safe_jq '.build_metrics.backend.test_files' "$METRICS_FILE" "0")
        local frontend_tests=$(safe_jq '.build_metrics.frontend.test_files' "$METRICS_FILE" "0")

        cat >> "$HTML_REPORT" << EOF
                    <tr><td>Backend</td><td>$backend_build_size</td><td>$backend_tests</td></tr>
                    <tr><td>Frontend</td><td>$frontend_build_size</td><td>$frontend_tests</td></tr>
                </table>
            </div>
EOF

    else
        cat >> "$HTML_REPORT" << EOF
            <div class="error-message">
                <h3>⚠️ Metrics Data Not Available</h3>
                <p>The detailed metrics could not be loaded. This might be because:</p>
                <ul>
                    <li>The metrics collection process hasn't run yet</li>
                    <li>The metrics file is missing or corrupted</li>
                    <li>There was an error during metrics collection</li>
                </ul>
                <p>Please run the metrics collector first: <code>./scripts/acis/metrics/metrics-collector.sh</code></p>
            </div>
EOF
    fi

    cat >> "$HTML_REPORT" << EOF
        </div>
        <div class="footer">
            <div>Report Generated by ACIS v1.0.0 - Automated Continuous Improvement System</div>
        </div>
    </div>
</body>
</html>
EOF

    log_success "HTML report generated: $HTML_REPORT"
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    log_info "Starting report generation for run: $RUN_ID"

    # Check if metrics file exists
    if [[ ! -f "$METRICS_FILE" ]]; then
        log_warn "Metrics file not found: $METRICS_FILE"
        log_info "Will generate reports with limited data"
    fi

    # Generate all report formats
    generate_json_report
    generate_markdown_report
    generate_html_report

    # Create an index file with links to all reports
    cat > "$REPORTS_DIR/index.html" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>ACIS Reports - $RUN_ID</title>
    <style>
        body { font-family: system-ui; padding: 20px; }
        .report-link { display: block; margin: 10px 0; padding: 10px; background: #f0f0f0; text-decoration: none; border-radius: 4px; }
        .report-link:hover { background: #e0e0e0; }
    </style>
</head>
<body>
    <h1>ACIS Reports</h1>
    <p>Run ID: $RUN_ID</p>
    <p>Generated: $(date)</p>

    <h2>Available Reports</h2>
    <a href="report-$RUN_ID.html" class="report-link">📊 HTML Report (Interactive)</a>
    <a href="report-$RUN_ID.md" class="report-link">📝 Markdown Report</a>
    <a href="report-$RUN_ID.json" class="report-link">📋 JSON Report (Raw Data)</a>
</body>
</html>
EOF

    log_success "Report generation completed"
    log_info "Reports saved to: $REPORTS_DIR/"
    log_info "Available formats:"
    log_info "  - HTML: $HTML_REPORT"
    log_info "  - Markdown: $MD_REPORT"
    log_info "  - JSON: $JSON_REPORT"
    log_info "  - Index: $REPORTS_DIR/index.html"

    exit 0
}

# Execute main function
main "$@"