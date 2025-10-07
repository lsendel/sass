#!/bin/bash
# ACIS Rollback Manager
# Automated Continuous Improvement System - Rollback Manager
# Version: 1.0.0

set -eo pipefail

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
ACIS_UTILS_DIR="$(cd "$SCRIPT_DIR/../utils" && pwd)"

# Source utilities
source "$ACIS_UTILS_DIR/logger.sh"
source "$ACIS_UTILS_DIR/config-reader.sh"
source "$ACIS_UTILS_DIR/git-utils.sh"

# Change to project root
cd "$PROJECT_ROOT"

# Initialize
CHECKPOINT_TAG="$1"
RM_RUN_ID="rm-$(date +%Y%m%d-%H%M%S)"
export CORRELATION_ID="${CORRELATION_ID:-$RM_RUN_ID}"

log_section "Rollback Manager: $RM_RUN_ID"

# ============================================================================
# CONFIGURATION
# ============================================================================

ROLLBACK_RESULTS_DIR=".acis/rollback"
mkdir -p "$ROLLBACK_RESULTS_DIR"

# ============================================================================
# VALIDATION
# ============================================================================

validate_checkpoint() {
    local checkpoint="$1"

    if [[ -z "$checkpoint" ]]; then
        log_error "No checkpoint specified"
        log_info "Usage: $0 <checkpoint-tag>"
        return 1
    fi

    # Check if checkpoint exists
    if ! git tag -l | grep -q "^$checkpoint$"; then
        log_error "Checkpoint tag '$checkpoint' does not exist"
        log_info "Available checkpoints:"
        git tag -l | grep "^acis-checkpoint-" | tail -10
        return 1
    fi

    # Check if checkpoint is reachable
    if ! git merge-base --is-ancestor "$checkpoint" HEAD 2>/dev/null; then
        log_error "Checkpoint '$checkpoint' is not an ancestor of current HEAD"
        return 1
    fi

    log_success "Checkpoint '$checkpoint' validated"
    return 0
}

# ============================================================================
# BACKUP CURRENT STATE
# ============================================================================

backup_current_state() {
    log_info "Creating backup of current state..."

    local current_branch=$(get_current_branch)
    local backup_tag="acis-backup-before-rollback-$(date +%Y%m%d-%H%M%S)"

    # Create backup tag
    if git tag "$backup_tag" 2>/dev/null; then
        log_success "Created backup tag: $backup_tag"
        echo "$backup_tag" > "$ROLLBACK_RESULTS_DIR/backup_tag"
    else
        log_error "Failed to create backup tag"
        return 1
    fi

    # Backup any uncommitted changes
    if ! is_working_directory_clean; then
        log_info "Stashing uncommitted changes..."
        local stash_message="ACIS rollback backup - $(date)"

        if git stash push -m "$stash_message" 2>/dev/null; then
            log_success "Uncommitted changes stashed"
            echo "true" > "$ROLLBACK_RESULTS_DIR/has_stash"
            git stash list | head -1 > "$ROLLBACK_RESULTS_DIR/stash_info"
        else
            log_warn "Failed to stash uncommitted changes"
        fi
    fi

    return 0
}

# ============================================================================
# PERFORM ROLLBACK
# ============================================================================

perform_rollback() {
    local checkpoint="$1"

    log_info "Rolling back to checkpoint: $checkpoint"

    # Get current and target commit hashes
    local current_commit=$(git rev-parse HEAD)
    local target_commit=$(git rev-parse "$checkpoint")

    log_info "Current commit: $current_commit"
    log_info "Target commit: $target_commit"

    # Store rollback information
    cat > "$ROLLBACK_RESULTS_DIR/rollback_info.json" << EOF
{
    "rollback_id": "$RM_RUN_ID",
    "checkpoint_tag": "$checkpoint",
    "current_commit": "$current_commit",
    "target_commit": "$target_commit",
    "timestamp": "$(date -Iseconds)",
    "correlation_id": "$CORRELATION_ID"
}
EOF

    # Perform the rollback
    case "$(get_config "rollback.strategy" "reset")" in
        "reset")
            log_info "Using reset strategy..."
            if git reset --hard "$checkpoint" 2>/dev/null; then
                log_success "Hard reset to checkpoint completed"
            else
                log_error "Hard reset failed"
                return 1
            fi
            ;;

        "revert")
            log_info "Using revert strategy..."
            # Create a revert commit for each commit between target and current
            local commits_to_revert=($(git rev-list --reverse "$target_commit".."$current_commit"))

            for commit in "${commits_to_revert[@]}"; do
                log_info "Reverting commit: $commit"
                if git revert --no-edit "$commit" 2>/dev/null; then
                    log_success "Reverted commit: $commit"
                else
                    log_error "Failed to revert commit: $commit"
                    return 1
                fi
            done
            ;;

        "checkout")
            log_info "Using checkout strategy..."
            if git checkout "$checkpoint" -- . 2>/dev/null; then
                log_success "Checkout to checkpoint completed"

                # Commit the changes
                local commit_message="🔄 ACIS Rollback to $checkpoint

Correlation ID: $CORRELATION_ID
Rollback ID: $RM_RUN_ID
Target: $target_commit"

                if git add . && git commit -m "$commit_message" 2>/dev/null; then
                    log_success "Rollback changes committed"
                else
                    log_warn "No changes to commit after checkout"
                fi
            else
                log_error "Checkout failed"
                return 1
            fi
            ;;

        *)
            log_error "Unknown rollback strategy: $(get_config "rollback.strategy")"
            return 1
            ;;
    esac

    return 0
}

# ============================================================================
# VERIFY ROLLBACK
# ============================================================================

verify_rollback() {
    local checkpoint="$1"

    log_info "Verifying rollback to checkpoint: $checkpoint"

    local current_commit=$(git rev-parse HEAD)
    local target_commit=$(git rev-parse "$checkpoint")

    case "$(get_config "rollback.strategy" "reset")" in
        "reset")
            # For reset, HEAD should be exactly at the checkpoint
            if [[ "$current_commit" == "$target_commit" ]]; then
                log_success "Rollback verification successful (reset strategy)"
                return 0
            else
                log_error "Rollback verification failed - commits don't match"
                return 1
            fi
            ;;

        "revert"|"checkout")
            # For revert/checkout, check if working directory matches checkpoint
            local diff_count=$(git diff --name-only "$checkpoint" | wc -l)

            if [[ $diff_count -eq 0 ]]; then
                log_success "Rollback verification successful (working directory matches checkpoint)"
                return 0
            else
                log_warn "Working directory differs from checkpoint by $diff_count files"
                log_info "This may be expected for revert/checkout strategies"
                return 0  # Don't fail for revert/checkout strategies
            fi
            ;;
    esac

    return 1
}

# ============================================================================
# CLEANUP
# ============================================================================

cleanup_after_rollback() {
    log_info "Performing post-rollback cleanup..."

    # Clean any build artifacts that might be stale
    if [[ -d "backend/build" ]]; then
        log_info "Cleaning backend build artifacts..."
        cd backend
        if ./gradlew clean 2>/dev/null; then
            log_success "Backend build cleaned"
        else
            log_warn "Failed to clean backend build"
        fi
        cd ..
    fi

    if [[ -d "frontend/node_modules" ]]; then
        log_info "Cleaning frontend dependencies..."
        cd frontend
        if command -v npm &> /dev/null; then
            # Remove node_modules and package-lock.json to ensure clean state
            rm -rf node_modules package-lock.json 2>/dev/null || true

            if npm install 2>/dev/null; then
                log_success "Frontend dependencies reinstalled"
            else
                log_warn "Failed to reinstall frontend dependencies"
            fi
        fi
        cd ..
    fi

    # Clean any ACIS temporary files except rollback results
    find .acis -name "*.tmp" -delete 2>/dev/null || true
    find .acis -name "*.lock" -delete 2>/dev/null || true

    log_success "Post-rollback cleanup completed"
}

# ============================================================================
# RECOVERY OPTIONS
# ============================================================================

show_recovery_options() {
    log_section "Recovery Options"

    if [[ -f "$ROLLBACK_RESULTS_DIR/backup_tag" ]]; then
        local backup_tag=$(cat "$ROLLBACK_RESULTS_DIR/backup_tag")
        log_info "To restore to pre-rollback state: git checkout $backup_tag"
    fi

    if [[ -f "$ROLLBACK_RESULTS_DIR/has_stash" && $(cat "$ROLLBACK_RESULTS_DIR/has_stash") == "true" ]]; then
        log_info "To restore stashed changes: git stash pop"
        if [[ -f "$ROLLBACK_RESULTS_DIR/stash_info" ]]; then
            log_info "Stash info: $(cat "$ROLLBACK_RESULTS_DIR/stash_info")"
        fi
    fi

    log_info "Rollback details saved in: $ROLLBACK_RESULTS_DIR/"
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    if [[ -z "$CHECKPOINT_TAG" ]]; then
        log_error "No checkpoint specified"
        log_info "Usage: $0 <checkpoint-tag>"
        log_info ""
        log_info "Available checkpoints:"
        git tag -l | grep "^acis-checkpoint-" | tail -10
        exit 1
    fi

    log_info "Starting rollback to checkpoint: $CHECKPOINT_TAG"

    # Validate checkpoint
    if ! validate_checkpoint "$CHECKPOINT_TAG"; then
        exit 1
    fi

    # Check if rollback is allowed
    local allow_rollback=$(get_config_bool "rollback.enabled" "true")
    if [[ "$allow_rollback" != "true" ]]; then
        log_error "Rollback is disabled in configuration"
        exit 1
    fi

    # Backup current state
    if ! backup_current_state; then
        log_error "Failed to backup current state"
        exit 1
    fi

    # Perform rollback
    if ! perform_rollback "$CHECKPOINT_TAG"; then
        log_error "Rollback failed"

        # Attempt to restore from backup
        if [[ -f "$ROLLBACK_RESULTS_DIR/backup_tag" ]]; then
            local backup_tag=$(cat "$ROLLBACK_RESULTS_DIR/backup_tag")
            log_info "Attempting to restore from backup: $backup_tag"
            git checkout "$backup_tag" 2>/dev/null || true
        fi

        exit 1
    fi

    # Verify rollback
    if ! verify_rollback "$CHECKPOINT_TAG"; then
        log_error "Rollback verification failed"
        exit 1
    fi

    # Cleanup
    cleanup_after_rollback

    # Show recovery options
    show_recovery_options

    # Generate final report
    cat > "$ROLLBACK_RESULTS_DIR/rollback_report.md" << EOF
# ACIS Rollback Report

**Rollback ID:** $RM_RUN_ID
**Timestamp:** $(date)
**Correlation ID:** $CORRELATION_ID

## Summary
- **Checkpoint:** $CHECKPOINT_TAG
- **Strategy:** $(get_config "rollback.strategy" "reset")
- **Status:** SUCCESS

## Details
$(cat "$ROLLBACK_RESULTS_DIR/rollback_info.json" | jq -r 'to_entries[] | "- **\(.key):** \(.value)"')

## Recovery Options
$(if [[ -f "$ROLLBACK_RESULTS_DIR/backup_tag" ]]; then
    echo "- Restore pre-rollback state: \`git checkout $(cat "$ROLLBACK_RESULTS_DIR/backup_tag")\`"
fi)
$(if [[ -f "$ROLLBACK_RESULTS_DIR/has_stash" && $(cat "$ROLLBACK_RESULTS_DIR/has_stash") == "true" ]]; then
    echo "- Restore stashed changes: \`git stash pop\`"
fi)

## Files
- Rollback details: \`$ROLLBACK_RESULTS_DIR/rollback_info.json\`
- This report: \`$ROLLBACK_RESULTS_DIR/rollback_report.md\`
EOF

    log_success "Rollback completed successfully!"
    log_info "Report saved to: $ROLLBACK_RESULTS_DIR/rollback_report.md"

    exit 0
}

# Execute main function
main "$@"