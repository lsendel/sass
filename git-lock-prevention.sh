#!/bin/bash

# Git Lock Prevention and Recovery Script
# Prevents and fixes Git lock file issues in development workflow

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
}

# Check if we're in a Git repository
check_git_repo() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        error "Not in a Git repository"
        exit 1
    fi
}

# Check for running Git processes
check_git_processes() {
    local git_processes=$(ps aux | grep -v grep | grep git | wc -l | tr -d ' ')
    if [ "$git_processes" -gt 0 ]; then
        warn "Found $git_processes running Git processes:"
        ps aux | grep -v grep | grep git
        return 1
    fi
    return 0
}

# Check for lock files
check_lock_files() {
    local git_dir=$(git rev-parse --git-dir)
    local lock_files=()

    # Common Git lock files
    if [ -f "$git_dir/index.lock" ]; then
        lock_files+=("$git_dir/index.lock")
    fi

    if [ -f "$git_dir/HEAD.lock" ]; then
        lock_files+=("$git_dir/HEAD.lock")
    fi

    if [ -f "$git_dir/refs/heads/.lock" ]; then
        lock_files+=("$git_dir/refs/heads/.lock")
    fi

    # Find any .lock files in .git directory
    while IFS= read -r -d '' file; do
        lock_files+=("$file")
    done < <(find "$git_dir" -name "*.lock" -print0 2>/dev/null || true)

    if [ ${#lock_files[@]} -gt 0 ]; then
        warn "Found ${#lock_files[@]} lock file(s):"
        printf '%s\n' "${lock_files[@]}"
        return 1
    fi

    return 0
}

# Safe Git operation wrapper
safe_git() {
    local max_retries=3
    local retry_delay=2
    local attempt=1

    while [ $attempt -le $max_retries ]; do
        log "Attempting Git operation (attempt $attempt/$max_retries): $*"

        # Check for conflicts before operation
        if ! check_git_processes; then
            warn "Git processes detected, waiting ${retry_delay}s..."
            sleep $retry_delay
            ((retry_delay *= 2))
            ((attempt++))
            continue
        fi

        if ! check_lock_files; then
            warn "Lock files detected, attempting cleanup..."
            clean_locks
            sleep 1
        fi

        # Attempt the Git operation
        if timeout 30 git "$@"; then
            log "Git operation successful"
            return 0
        else
            local exit_code=$?
            error "Git operation failed with exit code $exit_code"

            if [ $attempt -eq $max_retries ]; then
                error "All retry attempts failed"
                return $exit_code
            fi

            warn "Retrying in ${retry_delay}s..."
            sleep $retry_delay
            ((retry_delay *= 2))
            ((attempt++))
        fi
    done
}

# Clean lock files
clean_locks() {
    local git_dir=$(git rev-parse --git-dir)
    local cleaned=0

    log "Cleaning Git lock files..."

    # Remove common lock files
    for lock_file in "$git_dir/index.lock" "$git_dir/HEAD.lock" "$git_dir/refs/heads/.lock"; do
        if [ -f "$lock_file" ]; then
            warn "Removing lock file: $lock_file"
            rm -f "$lock_file"
            ((cleaned++))
        fi
    done

    # Find and remove all .lock files
    while IFS= read -r -d '' file; do
        warn "Removing lock file: $file"
        rm -f "$file"
        ((cleaned++))
    done < <(find "$git_dir" -name "*.lock" -print0 2>/dev/null || true)

    if [ $cleaned -gt 0 ]; then
        log "Cleaned $cleaned lock file(s)"
    else
        log "No lock files found to clean"
    fi
}

# Kill Git processes (use with caution)
kill_git_processes() {
    warn "Killing all Git processes..."

    # Get Git processes (excluding this script and grep)
    local pids=$(ps aux | grep git | grep -v grep | grep -v "git-lock-prevention" | awk '{print $2}')

    if [ -n "$pids" ]; then
        echo "$pids" | while read -r pid; do
            if [ -n "$pid" ]; then
                warn "Killing process $pid"
                kill -TERM "$pid" 2>/dev/null || true
                sleep 1
                kill -KILL "$pid" 2>/dev/null || true
            fi
        done
        sleep 2
    else
        log "No Git processes to kill"
    fi
}

# Emergency recovery
emergency_recovery() {
    error "Performing emergency recovery..."

    kill_git_processes
    sleep 2
    clean_locks

    log "Emergency recovery complete. Try your Git operation again."
}

# Pre-commit hook to prevent concurrent operations
install_hooks() {
    local git_dir=$(git rev-parse --git-dir)
    local hooks_dir="$git_dir/hooks"

    log "Installing Git hooks for lock prevention..."

    mkdir -p "$hooks_dir"

    # Pre-commit hook
    cat > "$hooks_dir/pre-commit" << 'EOF'
#!/bin/bash
# Git Lock Prevention Pre-commit Hook

# Check for existing lock files
git_dir=$(git rev-parse --git-dir)
if find "$git_dir" -name "*.lock" | grep -q .; then
    echo "ERROR: Git lock files detected. Another Git process may be running."
    echo "Run 'bash git-lock-prevention.sh --clean' to resolve."
    exit 1
fi

# Check for running Git processes
if pgrep -f "git " > /dev/null; then
    echo "WARNING: Other Git processes detected"
    sleep 1
fi
EOF

    chmod +x "$hooks_dir/pre-commit"
    log "Pre-commit hook installed"
}

# Main script logic
main() {
    check_git_repo

    case "${1:-}" in
        --check)
            log "Checking for Git lock issues..."
            if check_git_processes && check_lock_files; then
                log "No Git lock issues detected"
            else
                warn "Git lock issues detected"
                exit 1
            fi
            ;;
        --clean)
            clean_locks
            ;;
        --kill)
            kill_git_processes
            ;;
        --emergency)
            emergency_recovery
            ;;
        --install-hooks)
            install_hooks
            ;;
        --safe-git)
            shift
            safe_git "$@"
            ;;
        --help|"")
            echo "Git Lock Prevention and Recovery Tool"
            echo ""
            echo "Usage: $0 [OPTION]"
            echo ""
            echo "Options:"
            echo "  --check          Check for lock files and processes"
            echo "  --clean          Remove all Git lock files"
            echo "  --kill           Kill all Git processes (use with caution)"
            echo "  --emergency      Full recovery (kill processes + clean locks)"
            echo "  --install-hooks  Install Git hooks for prevention"
            echo "  --safe-git CMD   Execute Git command with retry logic"
            echo "  --help           Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 --check"
            echo "  $0 --clean"
            echo "  $0 --safe-git add ."
            echo "  $0 --safe-git commit -m 'message'"
            ;;
        *)
            error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
}

main "$@"