#!/bin/bash
# ACIS Git Hooks Installer
# Install/uninstall git hooks with symlink support

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
GIT_HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

# Source utilities
source "$SCRIPT_DIR/../utils/logger.sh"
source "$SCRIPT_DIR/../utils/config-reader.sh"

# Colors (only if not already defined)
if [ -z "$GREEN" ]; then
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    RED='\033[0;31m'
    NC='\033[0m'
fi

# Hook files to install
HOOKS=(
    "pre-commit"
    "pre-push"
    "commit-msg"
)

# Backup existing hooks
backup_existing_hooks() {
    local backup_enabled=$(get_config_bool "git_hooks.installation.backup_existing" "true")

    if [ "$backup_enabled" != "true" ]; then
        log_info "Backup disabled in configuration"
        return 0
    fi

    log_info "Backing up existing hooks..."

    local backup_dir="$GIT_HOOKS_DIR/backup-$(date +%Y%m%d-%H%M%S)"
    local backed_up=0

    for hook in "${HOOKS[@]}"; do
        local hook_path="$GIT_HOOKS_DIR/$hook"

        if [ -f "$hook_path" ] && [ ! -L "$hook_path" ]; then
            mkdir -p "$backup_dir"
            mv "$hook_path" "$backup_dir/"
            log_success "Backed up: $hook → $backup_dir/"
            backed_up=$((backed_up + 1))
        fi
    done

    if [ $backed_up -gt 0 ]; then
        log_success "Backed up $backed_up existing hook(s)"
    else
        log_info "No existing hooks to backup"
    fi
}

# Install hooks
install_hooks() {
    log_section "Installing ACIS Git Hooks"

    # Check if .git directory exists
    if [ ! -d "$PROJECT_ROOT/.git" ]; then
        log_error "Not a git repository: $PROJECT_ROOT"
        exit 1
    fi

    # Create hooks directory if it doesn't exist
    mkdir -p "$GIT_HOOKS_DIR"

    # Backup existing hooks
    backup_existing_hooks

    local use_symlinks=$(get_config_bool "git_hooks.installation.symlink_hooks" "true")
    local installed_count=0

    for hook in "${HOOKS[@]}"; do
        local source_hook="$SCRIPT_DIR/$hook"
        local target_hook="$GIT_HOOKS_DIR/$hook"

        # Check if source exists
        if [ ! -f "$source_hook" ]; then
            log_warn "Source hook not found: $source_hook, skipping"
            continue
        fi

        # Remove existing hook/symlink
        if [ -e "$target_hook" ] || [ -L "$target_hook" ]; then
            rm -f "$target_hook"
        fi

        # Install hook
        if [ "$use_symlinks" == "true" ]; then
            # Create symlink (relative path)
            local rel_path=$(python3 -c "import os.path; print(os.path.relpath('$source_hook', '$GIT_HOOKS_DIR'))")
            ln -s "$rel_path" "$target_hook"
            log_success "✅ Installed (symlink): $hook"
        else
            # Copy file
            cp "$source_hook" "$target_hook"
            chmod +x "$target_hook"
            log_success "✅ Installed (copy): $hook"
        fi

        installed_count=$((installed_count + 1))
    done

    log_success ""
    log_success "Installed $installed_count git hook(s)"
    log_success ""
    log_info "Git hooks are now active!"
    log_info ""
    log_info "Configuration:"
    log_info "  - Pre-commit: $(get_config_bool "git_hooks.pre_commit.enabled" && echo "ENABLED" || echo "DISABLED")"
    log_info "  - Pre-push: $(get_config_bool "git_hooks.pre_push.enabled" && echo "ENABLED" || echo "DISABLED")"
    log_info "  - Auto-fix on commit: $(get_config_bool "git_hooks.pre_commit.auto_fix" && echo "YES" || echo "NO")"
    log_info ""
    log_info "To bypass hooks (emergency only):"
    log_info "  git commit --no-verify"
    log_info "  ACIS_SKIP_HOOKS=true git commit"
}

# Uninstall hooks
uninstall_hooks() {
    log_section "Uninstalling ACIS Git Hooks"

    local uninstalled_count=0

    for hook in "${HOOKS[@]}"; do
        local target_hook="$GIT_HOOKS_DIR/$hook"

        if [ -e "$target_hook" ] || [ -L "$target_hook" ]; then
            # Check if it's our hook (symlink or contains ACIS marker)
            if [ -L "$target_hook" ] || grep -q "ACIS" "$target_hook" 2>/dev/null; then
                rm -f "$target_hook"
                log_success "✅ Uninstalled: $hook"
                uninstalled_count=$((uninstalled_count + 1))
            else
                log_warn "⚠️  Hook exists but doesn't appear to be ACIS hook: $hook (skipped)"
            fi
        fi
    done

    log_success ""
    log_success "Uninstalled $uninstalled_count git hook(s)"

    # Restore backups if available
    local latest_backup=$(ls -dt "$GIT_HOOKS_DIR"/backup-* 2>/dev/null | head -1)
    if [ -n "$latest_backup" ]; then
        log_info ""
        log_info "Latest backup available at: $latest_backup"
        log_info "To restore: cp $latest_backup/* $GIT_HOOKS_DIR/"
    fi
}

# Status check
status_hooks() {
    log_section "ACIS Git Hooks Status"

    log_info "Configuration:"
    log_info "  Master switch: $(get_config_bool "git_hooks.enabled" && echo "ENABLED" || echo "DISABLED")"
    log_info "  Pre-commit: $(get_config_bool "git_hooks.pre_commit.enabled" && echo "ENABLED" || echo "DISABLED")"
    log_info "  Pre-push: $(get_config_bool "git_hooks.pre_push.enabled" && echo "ENABLED" || echo "DISABLED")"
    log_info ""

    log_info "Installation status:"

    for hook in "${HOOKS[@]}"; do
        local target_hook="$GIT_HOOKS_DIR/$hook"

        if [ -L "$target_hook" ]; then
            local link_target=$(readlink "$target_hook")
            echo -e "  ${GREEN}✅ $hook${NC} (symlink → $link_target)"
        elif [ -f "$target_hook" ]; then
            if grep -q "ACIS" "$target_hook" 2>/dev/null; then
                echo -e "  ${GREEN}✅ $hook${NC} (file)"
            else
                echo -e "  ${YELLOW}⚠️  $hook${NC} (exists but not ACIS)"
            fi
        else
            echo -e "  ${RED}❌ $hook${NC} (not installed)"
        fi
    done

    log_info ""

    # Check for backups
    local backup_count=$(ls -d "$GIT_HOOKS_DIR"/backup-* 2>/dev/null | wc -l | tr -d ' ')
    if [ "$backup_count" -gt 0 ]; then
        log_info "Backups available: $backup_count"
        ls -dt "$GIT_HOOKS_DIR"/backup-* 2>/dev/null | head -3 | while read -r backup; do
            log_info "  - $(basename "$backup")"
        done
    fi
}

# Test hooks
test_hooks() {
    log_section "Testing ACIS Git Hooks"

    log_info "Testing smart detector..."
    if "$SCRIPT_DIR/smart-detector.sh" report staged fast; then
        log_success "✅ Smart detector working"
    else
        log_error "❌ Smart detector failed"
    fi

    log_info ""
    log_info "Testing pre-commit hook (dry run)..."
    if DRY_RUN=true "$GIT_HOOKS_DIR/pre-commit"; then
        log_success "✅ Pre-commit hook working"
    else
        log_warn "⚠️  Pre-commit hook test returned non-zero"
    fi

    log_info ""
    log_success "Hook testing complete"
    log_info "Try making a commit to test the hooks in action!"
}

# Main menu
show_menu() {
    echo ""
    echo "╔════════════════════════════════════════════════════╗"
    echo "║       ACIS Git Hooks Installer                    ║"
    echo "╚════════════════════════════════════════════════════╝"
    echo ""
    echo "  1) Install hooks"
    echo "  2) Uninstall hooks"
    echo "  3) Status check"
    echo "  4) Test hooks"
    echo "  5) Exit"
    echo ""
    echo -n "Select option [1-5]: "
}

# Main execution
main() {
    local command="${1:-}"

    case "$command" in
        install|-i)
            install_hooks
            ;;
        uninstall|-u)
            uninstall_hooks
            ;;
        status|-s)
            status_hooks
            ;;
        test|-t)
            test_hooks
            ;;
        menu|-m|"")
            while true; do
                show_menu
                read -r choice
                case $choice in
                    1) install_hooks; break ;;
                    2) uninstall_hooks; break ;;
                    3) status_hooks ;;
                    4) test_hooks ;;
                    5) echo "Goodbye!"; exit 0 ;;
                    *) echo "Invalid option" ;;
                esac
                echo ""
                echo "Press ENTER to continue..."
                read -r
            done
            ;;
        *)
            echo "Usage: $0 {install|uninstall|status|test|menu}"
            echo ""
            echo "Options:"
            echo "  install    - Install ACIS git hooks"
            echo "  uninstall  - Uninstall ACIS git hooks"
            echo "  status     - Show installation status"
            echo "  test       - Test hooks functionality"
            echo "  menu       - Interactive menu (default)"
            exit 1
            ;;
    esac
}

main "$@"
