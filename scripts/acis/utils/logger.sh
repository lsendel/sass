#!/bin/bash
# ACIS Structured Logger
# Provides JSON and text logging with correlation IDs

set -eo pipefail

# Color codes for terminal output (only if not already defined)
if [ -z "$RED" ]; then
    readonly RED='\033[0;31m'
    readonly GREEN='\033[0;32m'
    readonly YELLOW='\033[1;33m'
    readonly BLUE='\033[0;34m'
    readonly NC='\033[0m' # No Color
fi

# Log levels (only if not already defined)
if [ -z "$LOG_LEVEL_DEBUG" ]; then
    readonly LOG_LEVEL_DEBUG=0
    readonly LOG_LEVEL_INFO=1
    readonly LOG_LEVEL_WARN=2
    readonly LOG_LEVEL_ERROR=3
fi

# Global configuration
LOG_FORMAT="${LOG_FORMAT:-text}"
LOG_LEVEL="${LOG_LEVEL:-INFO}"
LOG_FILE="${LOG_FILE:-.acis/logs/acis.log}"
CORRELATION_ID="${CORRELATION_ID:-$(uuidgen 2>/dev/null || date +%s%N)}"

# Convert log level string to number
get_log_level_number() {
    local level="$1"
    # Convert to uppercase (bash 3.x compatible)
    level=$(echo "$level" | tr '[:lower:]' '[:upper:]')
    case "$level" in
        DEBUG) echo $LOG_LEVEL_DEBUG ;;
        INFO)  echo $LOG_LEVEL_INFO ;;
        WARN)  echo $LOG_LEVEL_WARN ;;
        ERROR) echo $LOG_LEVEL_ERROR ;;
        *)     echo $LOG_LEVEL_INFO ;;
    esac
}

# Current log level as number
CURRENT_LOG_LEVEL=$(get_log_level_number "$LOG_LEVEL")

# Check if message should be logged
should_log() {
    local level=$1
    local level_number=$(get_log_level_number "$level")
    [ "$level_number" -ge "$CURRENT_LOG_LEVEL" ]
}

# Format log message as JSON
format_json() {
    local level="$1"
    local message="$2"
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%S.%3NZ")

    cat <<EOF
{"timestamp":"$timestamp","level":"$level","message":"$message","correlation_id":"$CORRELATION_ID","pid":$$}
EOF
}

# Format log message as text
format_text() {
    local level="$1"
    local message="$2"
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    local color=""

    case "$level" in
        DEBUG) color="$BLUE" ;;
        INFO)  color="$GREEN" ;;
        WARN)  color="$YELLOW" ;;
        ERROR) color="$RED" ;;
    esac

    echo -e "${color}[$timestamp] [$level] $message${NC}"
}

# Write log to file
write_to_file() {
    local message="$1"
    mkdir -p "$(dirname "$LOG_FILE")"
    echo "$message" >> "$LOG_FILE"
}

# Main log function
log() {
    local level="$1"
    shift
    local message="$*"

    if ! should_log "$level"; then
        return 0
    fi

    local formatted_message
    if [ "$LOG_FORMAT" == "json" ]; then
        formatted_message=$(format_json "$level" "$message")
    else
        formatted_message=$(format_text "$level" "$message")
    fi

    # Output to stdout/stderr
    if [ "$level" == "ERROR" ]; then
        echo "$formatted_message" >&2
    else
        echo "$formatted_message"
    fi

    # Write to file if configured
    if [ -n "$LOG_FILE" ]; then
        if [ "$LOG_FORMAT" == "json" ]; then
            write_to_file "$formatted_message"
        else
            # Remove color codes for file logging
            mkdir -p "$(dirname "$LOG_FILE")" 2>/dev/null
            echo "$formatted_message" | sed 's/\x1b\[[0-9;]*m//g' >> "$LOG_FILE" 2>/dev/null || true
        fi
    fi
}

# Convenience functions
log_debug() {
    log "DEBUG" "$@"
}

log_info() {
    log "INFO" "$@"
}

log_warn() {
    log "WARN" "$@"
}

log_error() {
    log "ERROR" "$@"
}

# Log with context
log_with_context() {
    local level="$1"
    local context="$2"
    shift 2
    local message="$*"
    log "$level" "[$context] $message"
}

# Log success/failure
log_success() {
    log "INFO" "✅ $*"
}

log_failure() {
    log "ERROR" "❌ $*"
}

# Log section headers
log_section() {
    log "INFO" ""
    log "INFO" "========================================"
    log "INFO" "$*"
    log "INFO" "========================================"
}

# Log command execution
log_command() {
    local cmd="$*"
    log_debug "Executing: $cmd"
}

# Export functions for use in other scripts
export -f log log_debug log_info log_warn log_error
export -f log_success log_failure log_section log_command
export -f should_log format_json format_text write_to_file

# Export variables
export LOG_FORMAT LOG_LEVEL LOG_FILE CORRELATION_ID
