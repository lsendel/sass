#!/bin/bash
# ACIS Configuration Reader
# Reads YAML configuration file

set -eo pipefail

# Configuration file path
ACIS_CONFIG_FILE="${ACIS_CONFIG_FILE:-scripts/acis/config/acis-config.yml}"

# Check if config file exists
if [ ! -f "$ACIS_CONFIG_FILE" ]; then
    echo "ERROR: Configuration file not found: $ACIS_CONFIG_FILE" >&2
    exit 1
fi

# Parse YAML using Python (most reliable cross-platform approach)
parse_yaml() {
    python3 -c '
import yaml
import sys
import json

try:
    with open(sys.argv[1], "r") as f:
        config = yaml.safe_load(f)
    print(json.dumps(config))
except Exception as e:
    print(f"Error parsing YAML: {e}", file=sys.stderr)
    sys.exit(1)
' "$1"
}

# Get configuration value using jq-style path
# Usage: get_config "scheduler.interval_hours"
get_config() {
    local key="$1"
    local default="${2:-}"

    # Convert dot notation to jq path
    local jq_path=$(echo "$key" | sed 's/\./\./g')

    # Parse YAML and query with jq
    local value=$(parse_yaml "$ACIS_CONFIG_FILE" | jq -r ".$jq_path // empty")

    if [ -z "$value" ] || [ "$value" == "null" ]; then
        echo "$default"
    else
        echo "$value"
    fi
}

# Get boolean configuration
# Usage: get_config_bool "enabled" "true"
get_config_bool() {
    local key="$1"
    local default="${2:-false}"
    local value=$(get_config "$key" "$default")

    # Convert to lowercase (bash 3.x compatible)
    value=$(echo "$value" | tr '[:upper:]' '[:lower:]')
    case "$value" in
        true|yes|1|on) echo "true" ;;
        *) echo "false" ;;
    esac
}

# Get integer configuration
get_config_int() {
    local key="$1"
    local default="${2:-0}"
    local value=$(get_config "$key" "$default")

    # Validate it's a number
    if [[ "$value" =~ ^[0-9]+$ ]]; then
        echo "$value"
    else
        echo "$default"
    fi
}

# Get array configuration
# Usage: get_config_array "auto_fix.blacklist.paths"
get_config_array() {
    local key="$1"
    local jq_path=$(echo "$key" | sed 's/\./\./g')

    parse_yaml "$ACIS_CONFIG_FILE" | jq -r ".$jq_path[]? // empty"
}

# Check if ACIS is enabled
is_acis_enabled() {
    local enabled=$(get_config_bool "enabled" "false")
    [ "$enabled" == "true" ]
}

# Get scheduler interval in seconds
get_scheduler_interval_seconds() {
    local hours=$(get_config_int "scheduler.interval_hours" "1")
    echo $((hours * 3600))
}

# Get max iterations
get_max_iterations() {
    get_config_int "scheduler.max_iterations" "10"
}

# Check if strict mode is enabled
is_strict_mode() {
    get_config_bool "strict_mode" "true"
}

# Check if dry run mode
is_dry_run() {
    get_config_bool "dry_run" "false"
}

# Get git auto-commit setting
is_git_auto_commit() {
    get_config_bool "git.auto_commit" "false"
}

# Get quality gate command
get_quality_gate_command() {
    local gate="$1"
    get_config "quality_gates.$gate.command" ""
}

# Check if quality gate is enabled
is_quality_gate_enabled() {
    local gate="$1"
    get_config_bool "quality_gates.$gate.enabled" "true"
}

# Get rollback strategy
get_rollback_strategy() {
    get_config "rollback.strategy" "git_reset_hard"
}

# Get checkpoint tag prefix
get_checkpoint_prefix() {
    get_config "rollback.checkpoints.tag_prefix" "acis/checkpoint"
}

# Check if confirmation is required
is_confirmation_required() {
    get_config_bool "confirmation.enabled" "true"
}

# Get confirmation timeout
get_confirmation_timeout() {
    get_config_int "confirmation.timeout_seconds" "300"
}

# Check if path is blacklisted for auto-fix
is_path_blacklisted() {
    local file_path="$1"
    local blacklist_paths=$(get_config_array "auto_fix.blacklist.paths")

    while IFS= read -r pattern; do
        if [[ "$file_path" == $pattern ]]; then
            return 0  # Path is blacklisted
        fi
    done <<< "$blacklist_paths"

    return 1  # Path is not blacklisted
}

# Load all configuration into environment variables
load_config_to_env() {
    export ACIS_ENABLED=$(is_acis_enabled && echo "true" || echo "false")
    export ACIS_STRICT_MODE=$(is_strict_mode && echo "true" || echo "false")
    export ACIS_DRY_RUN=$(is_dry_run && echo "true" || echo "false")
    export ACIS_MAX_ITERATIONS=$(get_max_iterations)
    export ACIS_GIT_AUTO_COMMIT=$(is_git_auto_commit && echo "true" || echo "false")
    export ACIS_SCHEDULER_INTERVAL=$(get_scheduler_interval_seconds)
    export ACIS_CONFIRMATION_REQUIRED=$(is_confirmation_required && echo "true" || echo "false")
    export ACIS_CONFIRMATION_TIMEOUT=$(get_confirmation_timeout)
}

# Validate configuration
validate_config() {
    local errors=0

    # Check Python and jq availability
    if ! command -v python3 &> /dev/null; then
        echo "ERROR: python3 is required for YAML parsing" >&2
        errors=$((errors + 1))
    fi

    if ! command -v jq &> /dev/null; then
        echo "ERROR: jq is required for JSON parsing" >&2
        errors=$((errors + 1))
    fi

    # Check Python yaml module
    if ! python3 -c "import yaml" 2>/dev/null; then
        echo "ERROR: Python yaml module is required. Install with: pip3 install pyyaml" >&2
        errors=$((errors + 1))
    fi

    # Validate interval
    local interval=$(get_config_int "scheduler.interval_hours" "0")
    if [ "$interval" -lt 1 ]; then
        echo "ERROR: scheduler.interval_hours must be >= 1" >&2
        errors=$((errors + 1))
    fi

    # Validate max iterations
    local max_iter=$(get_max_iterations)
    if [ "$max_iter" -lt 1 ]; then
        echo "ERROR: scheduler.max_iterations must be >= 1" >&2
        errors=$((errors + 1))
    fi

    return $errors
}

# Print configuration summary
print_config_summary() {
    cat <<EOF
ACIS Configuration Summary
==========================
Enabled: $(is_acis_enabled && echo "YES" || echo "NO")
Strict Mode: $(is_strict_mode && echo "YES" || echo "NO")
Dry Run: $(is_dry_run && echo "YES" || echo "NO")
Scheduler Interval: $(get_config "scheduler.interval_hours") hours
Max Iterations: $(get_max_iterations)
Git Auto-Commit: $(is_git_auto_commit && echo "YES" || echo "NO")
Confirmation Required: $(is_confirmation_required && echo "YES" || echo "NO")
Rollback Strategy: $(get_rollback_strategy)
==========================
EOF
}

# Export functions
export -f get_config get_config_bool get_config_int get_config_array
export -f is_acis_enabled get_scheduler_interval_seconds get_max_iterations
export -f is_strict_mode is_dry_run is_git_auto_commit
export -f is_quality_gate_enabled get_quality_gate_command
export -f get_rollback_strategy get_checkpoint_prefix
export -f is_confirmation_required get_confirmation_timeout
export -f is_path_blacklisted load_config_to_env
export -f validate_config print_config_summary

# Export config file path
export ACIS_CONFIG_FILE
