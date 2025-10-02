#!/bin/bash
# ACIS Cross-Platform Compatibility Layer
# Provides unified interface across macOS, Linux, and Kubernetes

set -eo pipefail

# Detect operating system
detect_os() {
    case "$(uname -s)" in
        Darwin*)
            echo "macos"
            ;;
        Linux*)
            if [ -f /etc/os-release ]; then
                . /etc/os-release
                if [ "$ID" = "alpine" ]; then
                    echo "alpine"
                else
                    echo "linux"
                fi
            elif [ -f /.dockerenv ] || [ -f /run/.containerenv ]; then
                echo "container"
            else
                echo "linux"
            fi
            ;;
        CYGWIN*|MINGW*|MSYS*)
            echo "windows"
            ;;
        *)
            echo "unknown"
            ;;
    esac
}

# Detect if running in Kubernetes
is_kubernetes() {
    [ -n "$KUBERNETES_SERVICE_HOST" ] && return 0
    [ -d /var/run/secrets/kubernetes.io ] && return 0
    return 1
}

# Get platform type
get_platform() {
    if is_kubernetes; then
        echo "kubernetes"
    else
        detect_os
    fi
}

# Cross-platform readlink -f equivalent
realpath_cross() {
    local path="$1"

    if command -v realpath >/dev/null 2>&1; then
        # GNU realpath (Linux)
        realpath "$path"
    elif command -v grealpath >/dev/null 2>&1; then
        # GNU realpath via coreutils (Mac with brew install coreutils)
        grealpath "$path"
    else
        # Fallback for macOS without coreutils
        python3 -c "import os; print(os.path.realpath('$path'))"
    fi
}

# Cross-platform date command
date_iso8601() {
    if date --version >/dev/null 2>&1; then
        # GNU date (Linux)
        date -u +"%Y-%m-%dT%H:%M:%SZ"
    else
        # BSD date (macOS)
        date -u +"%Y-%m-%dT%H:%M:%SZ"
    fi
}

# Date arithmetic (subtract hours)
date_subtract_hours() {
    local hours="$1"

    if date --version >/dev/null 2>&1; then
        # GNU date (Linux)
        date -d "$hours hours ago" +%s
    else
        # BSD date (macOS)
        date -v-${hours}H +%s
    fi
}

# Date from timestamp
date_from_timestamp() {
    local timestamp="$1"

    if date --version >/dev/null 2>&1; then
        # GNU date (Linux)
        date -d "@$timestamp" +"%Y-%m-%d %H:%M:%S"
    else
        # BSD date (macOS)
        date -r "$timestamp" +"%Y-%m-%d %H:%M:%S"
    fi
}

# Cross-platform find with GNU semantics
find_cross() {
    if find --version >/dev/null 2>&1; then
        # GNU find
        find "$@"
    else
        # BSD find (macOS) - adjust for compatibility
        find "$@"
    fi
}

# Cross-platform sed
sed_cross() {
    if sed --version >/dev/null 2>&1; then
        # GNU sed
        sed "$@"
    else
        # BSD sed (macOS) - requires -i '' for in-place editing
        sed "$@"
    fi
}

# Cross-platform stat (get file modification time)
stat_mtime() {
    local file="$1"

    if stat --version >/dev/null 2>&1; then
        # GNU stat (Linux)
        stat -c %Y "$file"
    else
        # BSD stat (macOS)
        stat -f %m "$file"
    fi
}

# Cross-platform CPU count
cpu_count() {
    if command -v nproc >/dev/null 2>&1; then
        # Linux
        nproc
    elif command -v sysctl >/dev/null 2>&1; then
        # macOS
        sysctl -n hw.ncpu
    else
        # Fallback
        echo 2
    fi
}

# Cross-platform memory info (in MB)
memory_total_mb() {
    if [ -f /proc/meminfo ]; then
        # Linux
        awk '/MemTotal/ {print int($2/1024)}' /proc/meminfo
    elif command -v sysctl >/dev/null 2>&1; then
        # macOS
        echo $(($(sysctl -n hw.memsize) / 1024 / 1024))
    else
        echo 0
    fi
}

# Check if command exists (cross-platform)
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Get default package manager
get_package_manager() {
    if command_exists apt-get; then
        echo "apt"
    elif command_exists yum; then
        echo "yum"
    elif command_exists apk; then
        echo "apk"
    elif command_exists brew; then
        echo "brew"
    elif command_exists pacman; then
        echo "pacman"
    else
        echo "none"
    fi
}

# Install package (cross-platform)
install_package() {
    local package="$1"
    local pm=$(get_package_manager)

    case "$pm" in
        apt)
            sudo apt-get update && sudo apt-get install -y "$package"
            ;;
        yum)
            sudo yum install -y "$package"
            ;;
        apk)
            sudo apk add "$package"
            ;;
        brew)
            brew install "$package"
            ;;
        pacman)
            sudo pacman -S --noconfirm "$package"
            ;;
        *)
            echo "ERROR: No supported package manager found"
            return 1
            ;;
    esac
}

# Get scheduler type for this platform
get_scheduler_type() {
    local platform=$(get_platform)

    case "$platform" in
        kubernetes)
            echo "cronjob"
            ;;
        linux)
            if command_exists systemctl; then
                echo "systemd"
            else
                echo "cron"
            fi
            ;;
        macos)
            echo "launchd"
            ;;
        *)
            echo "cron"
            ;;
    esac
}

# Validate platform compatibility
validate_platform_requirements() {
    local platform=$(get_platform)
    local errors=0

    echo "Platform: $platform"

    # Check required commands
    local required_commands=("git" "python3" "jq")

    for cmd in "${required_commands[@]}"; do
        if ! command_exists "$cmd"; then
            echo "ERROR: Required command not found: $cmd"
            errors=$((errors + 1))
        else
            echo "✓ $cmd"
        fi
    done

    # Check Python modules
    if ! python3 -c "import yaml" 2>/dev/null; then
        echo "ERROR: Python yaml module not found"
        echo "Install with: pip3 install pyyaml"
        errors=$((errors + 1))
    else
        echo "✓ Python yaml module"
    fi

    # Platform-specific checks
    case "$platform" in
        kubernetes)
            echo "✓ Running in Kubernetes"
            ;;
        linux)
            if command_exists systemctl; then
                echo "✓ systemd available"
            else
                echo "⚠ systemd not available, will use cron"
            fi
            ;;
        macos)
            if command_exists launchctl; then
                echo "✓ launchd available"
            else
                echo "ERROR: launchd not available"
                errors=$((errors + 1))
            fi
            ;;
    esac

    return $errors
}

# Get platform-specific temp directory
get_temp_dir() {
    if [ -n "$TMPDIR" ]; then
        echo "$TMPDIR"
    elif [ -d /tmp ]; then
        echo "/tmp"
    else
        echo "."
    fi
}

# Platform info summary
print_platform_info() {
    cat <<EOF
Platform Information
====================
OS: $(detect_os)
Platform: $(get_platform)
Scheduler: $(get_scheduler_type)
CPU Cores: $(cpu_count)
Memory: $(memory_total_mb) MB
Package Manager: $(get_package_manager)
Temp Dir: $(get_temp_dir)
====================
EOF
}

# Export functions
export -f detect_os is_kubernetes get_platform
export -f realpath_cross date_iso8601 date_subtract_hours date_from_timestamp
export -f find_cross sed_cross stat_mtime
export -f cpu_count memory_total_mb
export -f command_exists get_package_manager install_package
export -f get_scheduler_type validate_platform_requirements
export -f get_temp_dir print_platform_info
