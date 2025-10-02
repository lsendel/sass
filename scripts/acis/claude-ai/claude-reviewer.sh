#!/bin/bash
# ACIS Claude AI Peer Reviewer
# Uses Claude API to review code changes and validate improvements

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Source utilities
source "$SCRIPT_DIR/../utils/logger.sh"
source "$SCRIPT_DIR/../utils/config-reader.sh"

# Claude API Configuration
CLAUDE_API_KEY="${ANTHROPIC_API_KEY:-${CLAUDE_API_KEY:-}}"
CLAUDE_API_URL="${CLAUDE_API_URL:-https://api.anthropic.com/v1/messages}"
CLAUDE_MODEL="${CLAUDE_MODEL:-claude-sonnet-4-20250514}"
CLAUDE_MAX_TOKENS="${CLAUDE_MAX_TOKENS:-4096}"

# Check if Claude review is enabled
is_claude_review_enabled() {
    get_config_bool "claude_ai.enabled" "false"
}

# Validate Claude API key
validate_claude_api_key() {
    if [ -z "$CLAUDE_API_KEY" ]; then
        log_error "Claude API key not found"
        log_error "Set ANTHROPIC_API_KEY or CLAUDE_API_KEY environment variable"
        log_error "Or add to .env file"
        return 1
    fi

    # Validate key format (should start with sk-)
    if [[ ! "$CLAUDE_API_KEY" =~ ^sk- ]]; then
        log_error "Invalid Claude API key format (should start with 'sk-')"
        return 1
    fi

    log_debug "Claude API key validated"
    return 0
}

# Get diff of changes
get_changes_diff() {
    local scope="$1"  # "staged" or "all"

    case "$scope" in
        staged)
            git diff --cached
            ;;
        all)
            git diff HEAD
            ;;
        *)
            git diff --cached
            ;;
    esac
}

# Get list of changed files
get_changed_files() {
    local scope="$1"

    case "$scope" in
        staged)
            git diff --cached --name-only
            ;;
        all)
            git diff HEAD --name-only
            ;;
        *)
            git diff --cached --name-only
            ;;
    esac
}

# Load prompt template
load_prompt_template() {
    local template_name="$1"
    local template_file="$SCRIPT_DIR/prompts/${template_name}.txt"

    if [ -f "$template_file" ]; then
        cat "$template_file"
    else
        log_error "Prompt template not found: $template_file"
        return 1
    fi
}

# Build Claude API request
build_claude_request() {
    local prompt="$1"
    local diff="$2"
    local files="$3"

    # Escape JSON special characters in diff and files
    diff_escaped=$(echo "$diff" | jq -Rs .)
    files_escaped=$(echo "$files" | jq -Rs .)

    cat <<EOF
{
  "model": "$CLAUDE_MODEL",
  "max_tokens": $CLAUDE_MAX_TOKENS,
  "temperature": 0.0,
  "system": "You are a strict code reviewer for an automated continuous improvement system (ACIS). Your role is to review code changes and ensure they improve code quality without adding new features. You must be very picky and only approve changes that genuinely improve the code.",
  "messages": [
    {
      "role": "user",
      "content": "# Code Review Request\n\n$prompt\n\n## Changed Files\n$files_escaped\n\n## Diff\n$diff_escaped\n\n## Review Instructions\n\n1. **Analyze the changes carefully**\n2. **Verify this is an IMPROVEMENT, not a new feature**\n3. **Check for code quality issues**\n4. **Validate security, performance, and best practices**\n5. **Provide a decision: APPROVE or DENY**\n\n## Response Format (JSON)\n\nRespond ONLY with valid JSON in this exact format:\n\n{\n  \"decision\": \"APPROVE\" or \"DENY\",\n  \"reasoning\": \"Brief explanation of your decision\",\n  \"issues\": [\n    \"List of specific issues found (if any)\"\n  ],\n  \"improvements\": [\n    \"List of improvements made (if APPROVE)\"\n  ],\n  \"is_feature\": false,\n  \"confidence\": 0.95\n}\n\nIMPORTANT:\n- Use \"DENY\" if this adds new features\n- Use \"DENY\" if this degrades code quality\n- Use \"DENY\" if there are security concerns\n- Use \"APPROVE\" only if this genuinely improves existing code"
    }
  ]
}
EOF
}

# Call Claude API
call_claude_api() {
    local request_payload="$1"

    log_debug "Calling Claude API..."

    local response=$(curl -s -X POST "$CLAUDE_API_URL" \
        -H "Content-Type: application/json" \
        -H "x-api-key: $CLAUDE_API_KEY" \
        -H "anthropic-version: 2023-06-01" \
        -d "$request_payload")

    if [ $? -ne 0 ]; then
        log_error "Claude API call failed"
        return 1
    fi

    echo "$response"
}

# Parse Claude response
parse_claude_response() {
    local response="$1"

    # Check for API errors
    local error_type=$(echo "$response" | jq -r '.error.type // empty')
    if [ -n "$error_type" ]; then
        local error_message=$(echo "$response" | jq -r '.error.message')
        log_error "Claude API error: $error_type - $error_message"
        return 1
    fi

    # Extract content
    local content=$(echo "$response" | jq -r '.content[0].text // empty')
    if [ -z "$content" ]; then
        log_error "No content in Claude response"
        return 1
    fi

    echo "$content"
}

# Extract decision from review
extract_decision() {
    local review_content="$1"

    # Try to parse as JSON
    local decision=$(echo "$review_content" | jq -r '.decision // empty' 2>/dev/null)

    if [ -z "$decision" ]; then
        # Fallback: search for APPROVE or DENY in text
        if echo "$review_content" | grep -qi "APPROVE"; then
            echo "APPROVE"
        elif echo "$review_content" | grep -qi "DENY"; then
            echo "DENY"
        else
            echo "UNKNOWN"
        fi
    else
        echo "$decision"
    fi
}

# Perform Claude code review
claude_code_review() {
    local scope="${1:-staged}"
    local review_type="${2:-commit}"

    log_section "Claude AI Peer Review"

    # Check if enabled
    if [ "$(is_claude_review_enabled)" != "true" ]; then
        log_info "Claude AI review is disabled in configuration"
        return 0
    fi

    # Validate API key
    if ! validate_claude_api_key; then
        local required=$(get_config_bool "claude_ai.required" "false")
        if [ "$required" == "true" ]; then
            log_error "Claude API key required but not found"
            return 1
        else
            log_warn "Claude API key not found, skipping review"
            return 0
        fi
    fi

    # Get changes
    log_info "Analyzing changes..."
    local diff=$(get_changes_diff "$scope")
    local files=$(get_changed_files "$scope")

    if [ -z "$diff" ]; then
        log_info "No changes to review"
        return 0
    fi

    local file_count=$(echo "$files" | wc -l | tr -d ' ')
    log_info "Reviewing $file_count file(s)"

    # Load prompt template
    local prompt_template="${3:-improvement-review}"
    local prompt=$(load_prompt_template "$prompt_template")

    # Build request
    local request=$(build_claude_request "$prompt" "$diff" "$files")

    # Call Claude API
    log_info "Requesting Claude review..."
    local response=$(call_claude_api "$request")

    if [ $? -ne 0 ]; then
        log_error "Failed to get Claude review"

        local fail_open=$(get_config_bool "claude_ai.fail_open" "false")
        if [ "$fail_open" == "true" ]; then
            log_warn "Claude review failed but fail_open=true, allowing changes"
            return 0
        else
            return 1
        fi
    fi

    # Parse response
    local review_content=$(parse_claude_response "$response")

    if [ $? -ne 0 ] || [ -z "$review_content" ]; then
        log_error "Failed to parse Claude response"
        return 1
    fi

    # Extract decision
    local decision=$(extract_decision "$review_content")

    # Save review to file
    local review_file=".acis/reviews/claude-review-$(date +%Y%m%d-%H%M%S).json"
    mkdir -p "$(dirname "$review_file")"
    echo "$review_content" > "$review_file"
    log_debug "Review saved to: $review_file"

    # Log review details
    log_info ""
    log_info "Claude Review Decision: $decision"

    # Parse and display reasoning
    local reasoning=$(echo "$review_content" | jq -r '.reasoning // empty' 2>/dev/null || echo "")
    if [ -n "$reasoning" ]; then
        log_info "Reasoning: $reasoning"
    fi

    # Display issues if any
    local issues=$(echo "$review_content" | jq -r '.issues[]? // empty' 2>/dev/null)
    if [ -n "$issues" ]; then
        log_warn ""
        log_warn "Issues found:"
        echo "$issues" | while read -r issue; do
            log_warn "  - $issue"
        done
    fi

    # Display improvements if any
    local improvements=$(echo "$review_content" | jq -r '.improvements[]? // empty' 2>/dev/null)
    if [ -n "$improvements" ]; then
        log_success ""
        log_success "Improvements made:"
        echo "$improvements" | while read -r improvement; do
            log_success "  - $improvement"
        done
    fi

    # Check if it's a feature
    local is_feature=$(echo "$review_content" | jq -r '.is_feature // false' 2>/dev/null)
    if [ "$is_feature" == "true" ]; then
        log_error ""
        log_error "❌ Claude detected this adds NEW FEATURES"
        log_error "ACIS is for improvements only, not feature additions"
        decision="DENY"
    fi

    # Make final decision
    log_info ""
    case "$decision" in
        APPROVE)
            log_success "✅ Claude APPROVED the changes"
            return 0
            ;;
        DENY)
            log_error "❌ Claude DENIED the changes"
            log_error ""
            log_error "The code changes did not pass Claude's peer review."
            log_error "Review the issues above and fix them before committing."
            log_error ""
            log_error "Review saved at: $review_file"
            return 1
            ;;
        *)
            log_warn "⚠️  Claude review decision unclear: $decision"

            local fail_open=$(get_config_bool "claude_ai.fail_open" "false")
            if [ "$fail_open" == "true" ]; then
                log_warn "fail_open=true, allowing changes"
                return 0
            else
                log_error "fail_open=false, denying changes"
                return 1
            fi
            ;;
    esac
}

# Export functions
export -f is_claude_review_enabled validate_claude_api_key
export -f get_changes_diff get_changed_files load_prompt_template
export -f build_claude_request call_claude_api parse_claude_response
export -f extract_decision claude_code_review

# Main execution if run directly
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    claude_code_review "${1:-staged}" "${2:-commit}" "${3:-improvement-review}"
fi
