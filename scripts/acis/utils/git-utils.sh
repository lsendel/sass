#!/bin/bash
# ACIS Git Utilities
# Git operations with safety checks

set -eo pipefail

# Source logger
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/logger.sh"

# Check if working directory is clean
is_working_directory_clean() {
    if [ -n "$(git status --porcelain)" ]; then
        return 1
    fi
    return 0
}

# Create git checkpoint
create_checkpoint() {
    local iteration="$1"
    local timestamp=$(date +%Y%m%d-%H%M%S)
    local tag_name="acis/checkpoint-${iteration}-${timestamp}"

    log_info "Creating checkpoint: $tag_name"

    # Create lightweight tag
    if git tag "$tag_name"; then
        log_success "Checkpoint created: $tag_name"
        echo "$tag_name"
        return 0
    else
        log_error "Failed to create checkpoint"
        return 1
    fi
}

# Rollback to checkpoint
rollback_to_checkpoint() {
    local tag_name="$1"

    log_warn "Rolling back to checkpoint: $tag_name"

    # Verify tag exists
    if ! git rev-parse "$tag_name" >/dev/null 2>&1; then
        log_error "Checkpoint tag not found: $tag_name"
        return 1
    fi

    # Reset to checkpoint
    if git reset --hard "$tag_name"; then
        log_info "Reset to checkpoint successful"
    else
        log_error "Failed to reset to checkpoint"
        return 1
    fi

    # Clean untracked files
    if git clean -fdx; then
        log_info "Cleaned untracked files"
    else
        log_warn "Failed to clean untracked files"
    fi

    log_success "Rollback completed"
    return 0
}

# Get latest checkpoint
get_latest_checkpoint() {
    git tag -l "acis/checkpoint-*" | sort -r | head -n 1
}

# Delete old checkpoints
cleanup_old_checkpoints() {
    local retention_hours="${1:-168}"  # Default: 7 days
    local cutoff_timestamp=$(date -v-${retention_hours}H +%s 2>/dev/null || date -d "${retention_hours} hours ago" +%s)

    log_info "Cleaning up checkpoints older than $retention_hours hours"

    local deleted_count=0
    while IFS= read -r tag; do
        # Extract timestamp from tag (acis/checkpoint-1-20251002-143022)
        local tag_timestamp=$(echo "$tag" | grep -oE '[0-9]{8}-[0-9]{6}' || echo "")

        if [ -n "$tag_timestamp" ]; then
            # Convert to epoch
            local tag_epoch=$(date -j -f "%Y%m%d-%H%M%S" "$tag_timestamp" +%s 2>/dev/null || echo "0")

            if [ "$tag_epoch" -lt "$cutoff_timestamp" ]; then
                log_debug "Deleting old checkpoint: $tag"
                git tag -d "$tag" >/dev/null 2>&1
                deleted_count=$((deleted_count + 1))
            fi
        fi
    done < <(git tag -l "acis/checkpoint-*")

    if [ $deleted_count -gt 0 ]; then
        log_info "Deleted $deleted_count old checkpoints"
    fi
}

# Create branch for changes
create_improvement_branch() {
    local branch_prefix="${1:-acis/auto-improvement}"
    local timestamp=$(date +%Y%m%d-%H%M%S)
    local branch_name="${branch_prefix}-${timestamp}"

    log_info "Creating improvement branch: $branch_name"

    if git checkout -b "$branch_name"; then
        log_success "Branch created: $branch_name"
        echo "$branch_name"
        return 0
    else
        log_error "Failed to create branch"
        return 1
    fi
}

# Commit changes
commit_changes() {
    local message="$1"
    local commit_prefix="${2:-🤖 ACIS:}"

    local full_message="$commit_prefix $message"

    log_info "Committing changes: $full_message"

    # Stage all changes
    git add -A

    # Check if there are changes to commit
    if is_working_directory_clean; then
        log_warn "No changes to commit"
        return 0
    fi

    # Commit
    if git commit -m "$full_message"; then
        log_success "Changes committed"
        return 0
    else
        log_error "Failed to commit changes"
        return 1
    fi
}

# Push changes
push_changes() {
    local branch="$1"

    log_info "Pushing changes to remote: $branch"

    if git push -u origin "$branch"; then
        log_success "Changes pushed"
        return 0
    else
        log_error "Failed to push changes"
        return 1
    fi
}

# Get current branch
get_current_branch() {
    git rev-parse --abbrev-ref HEAD
}

# Get uncommitted changes count
get_uncommitted_changes_count() {
    git status --porcelain | wc -l | tr -d ' '
}

# Get files changed in working directory
get_changed_files() {
    git status --porcelain | awk '{print $2}'
}

# Validate git repository
validate_git_repository() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        log_error "Not a git repository"
        return 1
    fi

    log_debug "Git repository validated"
    return 0
}

# Get commit count since tag
get_commits_since_checkpoint() {
    local tag="$1"
    git rev-list "$tag"..HEAD --count
}

# Stash changes
stash_changes() {
    local stash_name="$1"

    log_info "Stashing changes: $stash_name"

    if git stash push -m "$stash_name"; then
        log_success "Changes stashed"
        return 0
    else
        log_error "Failed to stash changes"
        return 1
    fi
}

# Pop stashed changes
pop_stashed_changes() {
    log_info "Popping stashed changes"

    if git stash pop; then
        log_success "Changes restored from stash"
        return 0
    else
        log_error "Failed to restore from stash"
        return 1
    fi
}

# Export functions
export -f is_working_directory_clean create_checkpoint rollback_to_checkpoint
export -f get_latest_checkpoint cleanup_old_checkpoints
export -f create_improvement_branch commit_changes push_changes
export -f get_current_branch get_uncommitted_changes_count get_changed_files
export -f validate_git_repository get_commits_since_checkpoint
export -f stash_changes pop_stashed_changes
