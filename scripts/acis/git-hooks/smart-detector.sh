#!/bin/bash
# ACIS Smart Change Detector
# Intelligently detects what changed and which checks to run

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../utils/logger.sh"

# Detect changed files based on scope
detect_changed_files() {
    local scope="${1:-staged}"

    case "$scope" in
        staged)
            # Only staged files (for pre-commit)
            git diff --cached --name-only --diff-filter=ACMR
            ;;
        all_changes)
            # All uncommitted changes
            git diff HEAD --name-only --diff-filter=ACMR
            ;;
        diff_from_main)
            # Diff from main branch
            local main_branch=$(git symbolic-ref refs/remotes/origin/HEAD | sed 's@^refs/remotes/origin/@@')
            git diff "$main_branch"...HEAD --name-only --diff-filter=ACMR
            ;;
        diff_from_remote)
            # Diff from remote tracking branch
            local current_branch=$(git rev-parse --abbrev-ref HEAD)
            local remote_branch=$(git rev-parse --abbrev-ref --symbolic-full-name @{u} 2>/dev/null || echo "origin/$current_branch")
            git diff "$remote_branch"...HEAD --name-only --diff-filter=ACMR 2>/dev/null || git diff HEAD --name-only --diff-filter=ACMR
            ;;
        *)
            log_error "Unknown scope: $scope"
            return 1
            ;;
    esac
}

# Categorize files by type
categorize_files() {
    local files="$1"

    declare -A categories=(
        [java]=0
        [test]=0
        [config]=0
        [sql]=0
        [docs]=0
        [frontend]=0
        [build]=0
    )

    while IFS= read -r file; do
        case "$file" in
            *.java)
                if [[ "$file" == *"/test/"* ]] || [[ "$file" == *"Test.java" ]]; then
                    categories[test]=1
                else
                    categories[java]=1
                fi
                ;;
            *.yml|*.yaml|*.properties|*.xml)
                categories[config]=1
                ;;
            *.sql)
                categories[sql]=1
                ;;
            *.md|*.txt)
                categories[docs]=1
                ;;
            *.ts|*.tsx|*.js|*.jsx|*.css|*.scss)
                categories[frontend]=1
                ;;
            *build.gradle|*pom.xml|*package.json)
                categories[build]=1
                ;;
        esac
    done <<< "$files"

    # Export categories as JSON
    cat <<EOF
{
    "java": ${categories[java]},
    "test": ${categories[test]},
    "config": ${categories[config]},
    "sql": ${categories[sql]},
    "docs": ${categories[docs]},
    "frontend": ${categories[frontend]},
    "build": ${categories[build]}
}
EOF
}

# Determine which gates to run based on changes
determine_required_gates() {
    local categories_json="$1"
    local mode="${2:-fast}"

    local gates=()

    # Always run compilation if Java code changed
    local has_java=$(echo "$categories_json" | jq -r '.java')
    local has_test=$(echo "$categories_json" | jq -r '.test')
    local has_config=$(echo "$categories_json" | jq -r '.config')
    local has_build=$(echo "$categories_json" | jq -r '.build')

    if [ "$has_java" == "1" ] || [ "$has_test" == "1" ]; then
        gates+=("compilation")
        gates+=("code_style")
    fi

    # Run tests if code or tests changed
    if [ "$has_java" == "1" ] || [ "$has_test" == "1" ]; then
        if [ "$mode" == "fast" ]; then
            gates+=("fast_tests")
        else
            gates+=("testing")
        fi
    fi

    # Run architecture checks if Java code changed
    if [ "$has_java" == "1" ] && [ "$mode" != "fast" ]; then
        gates+=("architecture")
    fi

    # Run security checks if dependencies changed
    if [ "$has_build" == "1" ] && [ "$mode" != "fast" ]; then
        gates+=("security")
    fi

    # If no specific changes detected, run default gates
    if [ ${#gates[@]} -eq 0 ]; then
        if [ "$mode" == "fast" ]; then
            gates=("compilation" "code_style")
        else
            gates=("compilation" "code_style" "testing")
        fi
    fi

    # Output gates as JSON array
    printf '%s\n' "${gates[@]}" | jq -R . | jq -s .
}

# Get list of changed modules (for Modulith projects)
detect_changed_modules() {
    local files="$1"

    # Extract module paths (assumes structure: backend/src/main/java/com/platform/{module}/)
    echo "$files" | grep -E "src/main/java/com/platform/[^/]+/" | \
        sed -E 's|.*src/main/java/com/platform/([^/]+)/.*|\1|' | \
        sort -u
}

# Check if critical files changed (require comprehensive validation)
has_critical_changes() {
    local files="$1"

    # Critical patterns
    local critical_patterns=(
        "**/payment/**"
        "**/security/**"
        "**/authentication/**"
        "**/authorization/**"
        "**/db/migration/**"
        "**/*SecurityConfig.java"
        "**/*AuthenticationProvider.java"
    )

    while IFS= read -r file; do
        for pattern in "${critical_patterns[@]}"; do
            if [[ "$file" == $pattern ]]; then
                return 0  # Has critical changes
            fi
        done
    done <<< "$files"

    return 1  # No critical changes
}

# Generate smart validation report
generate_validation_report() {
    local scope="$1"
    local mode="${2:-fast}"

    log_info "Analyzing changes (scope: $scope, mode: $mode)..."

    # Detect changed files
    local changed_files=$(detect_changed_files "$scope")

    if [ -z "$changed_files" ]; then
        log_warn "No changes detected"
        echo '{"has_changes": false}'
        return 0
    fi

    local file_count=$(echo "$changed_files" | wc -l | tr -d ' ')
    log_info "Detected $file_count changed file(s)"

    # Categorize files
    local categories=$(categorize_files "$changed_files")

    # Determine required gates
    local required_gates=$(determine_required_gates "$categories" "$mode")

    # Detect changed modules
    local changed_modules=$(detect_changed_modules "$changed_files")
    local module_count=$(echo "$changed_modules" | grep -v '^$' | wc -l | tr -d ' ')

    # Check for critical changes
    local is_critical="false"
    if has_critical_changes "$changed_files"; then
        is_critical="true"
        log_warn "⚠️  Critical files detected - comprehensive validation required"
    fi

    # Generate report
    cat <<EOF
{
    "has_changes": true,
    "file_count": $file_count,
    "categories": $categories,
    "required_gates": $required_gates,
    "changed_modules": $(echo "$changed_modules" | jq -R . | jq -s . 2>/dev/null || echo '[]'),
    "module_count": $module_count,
    "is_critical": $is_critical,
    "scope": "$scope",
    "mode": "$mode"
}
EOF
}

# Main execution
main() {
    local command="${1:-report}"
    local scope="${2:-staged}"
    local mode="${3:-fast}"

    case "$command" in
        report)
            generate_validation_report "$scope" "$mode"
            ;;
        files)
            detect_changed_files "$scope"
            ;;
        categories)
            local files=$(detect_changed_files "$scope")
            categorize_files "$files"
            ;;
        gates)
            local files=$(detect_changed_files "$scope")
            local categories=$(categorize_files "$files")
            determine_required_gates "$categories" "$mode"
            ;;
        modules)
            local files=$(detect_changed_files "$scope")
            detect_changed_modules "$files"
            ;;
        critical)
            local files=$(detect_changed_files "$scope")
            if has_critical_changes "$files"; then
                echo "true"
                exit 0
            else
                echo "false"
                exit 1
            fi
            ;;
        *)
            log_error "Unknown command: $command"
            echo "Usage: $0 {report|files|categories|gates|modules|critical} [scope] [mode]"
            exit 1
            ;;
    esac
}

# Export functions
export -f detect_changed_files categorize_files determine_required_gates
export -f detect_changed_modules has_critical_changes generate_validation_report

# Run if executed directly
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    main "$@"
fi
