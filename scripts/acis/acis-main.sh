#!/bin/bash
# ACIS Main Orchestrator
# Automated Continuous Improvement System - Main Entry Point
# Version: 2.0.0

set -eo pipefail

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source utilities
source "$SCRIPT_DIR/utils/logger.sh"
source "$SCRIPT_DIR/utils/config-reader.sh"
source "$SCRIPT_DIR/utils/git-utils.sh"

# Change to project root
cd "$PROJECT_ROOT"

# Initialize
ACIS_RUN_ID="acis-$(date +%Y%m%d-%H%M%S)"
ACIS_START_TIME=$(date +%s)
export CORRELATION_ID="$ACIS_RUN_ID"

log_section "ACIS Starting: $ACIS_RUN_ID"

# ============================================================================
# PRE-FLIGHT CHECKS
# ============================================================================

preflight_checks() {
    log_info "Running pre-flight checks..."

    # Validate configuration
    if ! validate_config; then
        log_error "Configuration validation failed"
        exit 1
    fi

    # Check if ACIS is enabled
    if ! is_acis_enabled; then
        log_warn "ACIS is disabled in configuration. Set 'enabled: true' to run."
        exit 0
    fi

    # Load configuration to environment
    load_config_to_env

    # Print configuration summary
    print_config_summary

    # Validate git repository
    if ! validate_git_repository; then
        log_error "Not a valid git repository"
        exit 1
    fi

    # Check if working directory should be clean
    local require_clean=$(get_config_bool "git.require_clean_state" "true")
    if [ "$require_clean" == "true" ] && ! is_working_directory_clean; then
        log_error "Working directory is not clean. Commit or stash changes first."
        log_info "To bypass this check, set git.require_clean_state: false in config"
        exit 1
    fi

    # Check required tools
    local missing_tools=()
    for tool in python3 jq git java; do
        if ! command -v $tool &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done

    if [ ${#missing_tools[@]} -gt 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        exit 1
    fi

    # Check Python yaml module
    if ! python3 -c "import yaml" 2>/dev/null; then
        log_error "Python yaml module is required. Install with: pip3 install pyyaml"
        exit 1
    fi

    log_success "Pre-flight checks passed"
}

# ============================================================================
# CONFIRMATION GATE
# ============================================================================

check_continuation_approval() {
    local confirmation_required=$(is_confirmation_required)

    if [ "$confirmation_required" != "true" ]; then
        log_info "Confirmation not required, proceeding automatically"
        return 0
    fi

    log_section "Continuation Confirmation Required"
    log_info "ACIS needs confirmation to proceed with automated improvements"
    log_info ""
    log_info "To approve, use one of these methods:"
    log_info "  1. Create file: touch .acis/continue"
    log_info "  2. Set environment: export ACIS_CONTINUE_APPROVED=true"
    log_info "  3. Interactive: Press ENTER to approve, Ctrl+C to cancel"
    log_info ""

    local timeout=$(get_confirmation_timeout)
    local start_time=$(date +%s)
    local auto_approve_after_timeout=$(get_config_bool "confirmation.auto_approve_after_timeout" "false")

    while true; do
        local elapsed=$(($(date +%s) - start_time))

        # Check file flag
        if [ -f ".acis/continue" ]; then
            log_success "Continuation approved via file flag"
            rm -f ".acis/continue"
            return 0
        fi

        # Check environment variable
        if [ "$ACIS_CONTINUE_APPROVED" == "true" ]; then
            log_success "Continuation approved via environment variable"
            return 0
        fi

        # Check timeout
        if [ $elapsed -ge $timeout ]; then
            if [ "$auto_approve_after_timeout" == "true" ]; then
                log_warn "Timeout reached, auto-approving (as configured)"
                return 0
            else
                log_error "Timeout reached, aborting"
                return 1
            fi
        fi

        # Interactive check (if terminal)
        if [ -t 0 ]; then
            read -t 1 -n 1 -s -r && {
                log_success "Continuation approved interactively"
                return 0
            } || true
        else
            sleep 1
        fi
    done
}

# ============================================================================
# MAIN ITERATION LOOP
# ============================================================================

run_acis_iterations() {
    local max_iterations=$(get_max_iterations)
    local iteration=0
    local status="PENDING"
    local checkpoint_tag=""
    local consecutive_rollbacks=0
    local max_consecutive_rollbacks=$(get_config_int "rollback.thresholds.consecutive_rollbacks" "3")

    # Create initial branch if configured
    local create_branch=$(get_config_bool "git.create_branch" "true")
    local original_branch=$(get_current_branch)

    if [ "$create_branch" == "true" ]; then
        local branch_prefix=$(get_config "git.branch_prefix" "acis/auto-improvement")
        if ! create_improvement_branch "$branch_prefix"; then
            log_error "Failed to create improvement branch"
            return 1
        fi
    fi

    log_section "Starting ACIS Iteration Loop"
    log_info "Max iterations: $max_iterations"
    log_info "Strict mode: $(is_strict_mode && echo 'ON' || echo 'OFF')"
    log_info "Dry run: $(is_dry_run && echo 'YES' || echo 'NO')"

    while [ $iteration -lt $max_iterations ]; do
        iteration=$((iteration + 1))

        log_section "ACIS Iteration $iteration/$max_iterations"

        # Create checkpoint before iteration
        checkpoint_tag=$(create_checkpoint "$iteration")
        if [ -z "$checkpoint_tag" ]; then
            log_error "Failed to create checkpoint"
            status="FAILED"
            break
        fi

        # Run quality gates
        log_info "Running quality gates..."
        if "$SCRIPT_DIR/quality-gates/quality-gates-validator.sh"; then
            log_success "✅ All quality gates passed!"
            status="SUCCESS"
            break
        else
            log_warn "Quality gates failed, attempting auto-fix..."
        fi

        # Attempt auto-fix
        if is_dry_run; then
            log_info "[DRY RUN] Would attempt auto-fix here"
        else
            if "$SCRIPT_DIR/auto-fix/auto-fix-engine.sh"; then
                log_info "Auto-fix completed, validating..."
                consecutive_rollbacks=0
            else
                log_error "Auto-fix failed"
                log_warn "Rolling back to checkpoint: $checkpoint_tag"

                if "$SCRIPT_DIR/rollback/rollback-manager.sh" "$checkpoint_tag"; then
                    consecutive_rollbacks=$((consecutive_rollbacks + 1))

                    if [ $consecutive_rollbacks -ge $max_consecutive_rollbacks ]; then
                        log_error "Too many consecutive rollbacks ($consecutive_rollbacks), aborting"
                        status="FAILED"
                        break
                    fi
                else
                    log_error "Rollback failed - manual intervention required"
                    status="CRITICAL_FAILURE"
                    break
                fi
            fi
        fi

        # Commit changes if auto-commit is enabled
        if is_git_auto_commit && ! is_dry_run; then
            local commit_prefix=$(get_config "git.commit_prefix" "🤖 ACIS:")
            if commit_changes "Iteration $iteration improvements" "$commit_prefix"; then
                log_success "Changes committed"
            else
                log_warn "No changes to commit"
            fi
        fi
    done

    # Summary
    log_section "ACIS Run Complete"
    log_info "Status: $status"
    log_info "Iterations completed: $iteration"
    log_info "Rollbacks: $consecutive_rollbacks"

    # Cleanup old checkpoints
    local retention_hours=$(get_config_int "rollback.checkpoints.retention_hours" "168")
    cleanup_old_checkpoints "$retention_hours"

    # Return to original branch if needed
    if [ "$create_branch" == "true" ] && [ "$status" != "SUCCESS" ]; then
        log_info "Returning to original branch: $original_branch"
        git checkout "$original_branch" >/dev/null 2>&1 || true
    fi

    # Return success/failure
    [ "$status" == "SUCCESS" ]
}

# ============================================================================
# REPORTING
# ============================================================================

generate_final_report() {
    local end_time=$(date +%s)
    local duration=$((end_time - ACIS_START_TIME))

    log_section "Generating Final Report"

    # Generate metrics report
    if "$SCRIPT_DIR/metrics/metrics-collector.sh" "$ACIS_RUN_ID"; then
        log_success "Metrics collected"
    fi

    # Generate HTML/JSON reports
    if "$SCRIPT_DIR/metrics/report-generator.sh" "$ACIS_RUN_ID"; then
        log_success "Reports generated in .acis/reports/"
    fi

    log_info "Total runtime: ${duration}s"
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    # Pre-flight checks
    preflight_checks

    # Check for continuation approval
    if ! check_continuation_approval; then
        log_error "Continuation not approved, exiting"
        exit 1
    fi

    # Run ACIS iterations
    if run_acis_iterations; then
        log_success "ACIS completed successfully"
        generate_final_report
        exit 0
    else
        log_error "ACIS failed"
        generate_final_report
        exit 1
    fi
}

# Run main function
main "$@"
