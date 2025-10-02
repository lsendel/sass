#!/usr/bin/env bash

# Slack Integration Setup Script
# Automates the setup of Slack bot integration for GitHub Actions

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
print_header() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

check_dependencies() {
    print_header "Checking Dependencies"

    local missing_deps=0

    # Check for gh CLI
    if ! command -v gh &> /dev/null; then
        print_error "GitHub CLI (gh) is not installed"
        print_info "Install from: https://cli.github.com/"
        missing_deps=1
    else
        print_success "GitHub CLI found"
    fi

    # Check for curl
    if ! command -v curl &> /dev/null; then
        print_error "curl is not installed"
        missing_deps=1
    else
        print_success "curl found"
    fi

    # Check for jq
    if ! command -v jq &> /dev/null; then
        print_error "jq is not installed"
        print_info "Install: brew install jq (macOS) or apt-get install jq (Linux)"
        missing_deps=1
    else
        print_success "jq found"
    fi

    if [ $missing_deps -eq 1 ]; then
        print_error "Please install missing dependencies and try again"
        exit 1
    fi

    echo ""
}

verify_gh_auth() {
    print_header "Verifying GitHub Authentication"

    if ! gh auth status &> /dev/null; then
        print_error "Not authenticated with GitHub CLI"
        print_info "Run: gh auth login"
        exit 1
    fi

    print_success "GitHub CLI authenticated"
    echo ""
}

get_slack_token() {
    print_header "Slack Bot Token Configuration"

    echo -e "${YELLOW}Follow these steps to get your Slack Bot Token:${NC}"
    echo ""
    echo "1. Go to https://api.slack.com/apps"
    echo "2. Click 'Create New App' → 'From scratch'"
    echo "3. Name: 'SASS CI/CD Notifications', select your workspace"
    echo "4. Navigate to 'OAuth & Permissions'"
    echo "5. Under 'Bot Token Scopes', add:"
    echo "   - chat:write"
    echo "   - chat:write.public"
    echo "   - channels:read"
    echo "   - groups:read"
    echo "6. Click 'Install to Workspace' and authorize"
    echo "7. Copy the 'Bot User OAuth Token' (starts with xoxb-)"
    echo ""

    read -rp "Paste your Slack Bot User OAuth Token: " SLACK_BOT_TOKEN

    if [[ ! $SLACK_BOT_TOKEN =~ ^xoxb- ]]; then
        print_error "Invalid token format. Token should start with 'xoxb-'"
        exit 1
    fi

    print_success "Token format validated"
    echo ""
}

verify_slack_token() {
    print_header "Verifying Slack Token"

    local response=$(curl -s -X POST https://slack.com/api/auth.test \
        -H "Authorization: Bearer $SLACK_BOT_TOKEN")

    local ok=$(echo "$response" | jq -r '.ok')

    if [ "$ok" = "true" ]; then
        local bot_name=$(echo "$response" | jq -r '.user')
        local team=$(echo "$response" | jq -r '.team')
        print_success "Token verified successfully"
        print_info "Bot: $bot_name in workspace: $team"
    else
        local error=$(echo "$response" | jq -r '.error')
        print_error "Token verification failed: $error"
        exit 1
    fi

    echo ""
}

list_slack_channels() {
    print_header "Listing Slack Channels"

    local response=$(curl -s -X GET https://slack.com/api/conversations.list \
        -H "Authorization: Bearer $SLACK_BOT_TOKEN" \
        -G -d "types=public_channel,private_channel")

    local ok=$(echo "$response" | jq -r '.ok')

    if [ "$ok" = "true" ]; then
        print_success "Retrieved channel list"
        echo ""
        echo "Available channels:"
        echo "$response" | jq -r '.channels[] | "  \(.name) - \(.id)"' | head -20
        echo ""
    else
        print_warning "Could not retrieve channel list"
    fi
}

get_channel_ids() {
    print_header "Channel Configuration"

    print_info "You need to configure the following channels:"
    echo "  1. Deployment notifications (e.g., #deployments)"
    echo "  2. Performance alerts (e.g., #performance-alerts)"
    echo ""

    # Ask if user wants to create channels or use existing
    read -rp "Do you want to see your existing channels? (y/n): " show_channels

    if [[ $show_channels =~ ^[Yy]$ ]]; then
        list_slack_channels
    fi

    echo ""
    print_info "Enter channel IDs (found in channel details or from list above)"
    echo ""

    read -rp "Enter deployment channel ID (or press Enter to use 'deployments'): " DEPLOY_CHANNEL_ID
    DEPLOY_CHANNEL_ID=${DEPLOY_CHANNEL_ID:-deployments}

    read -rp "Enter performance alerts channel ID (or press Enter to use 'performance-alerts'): " PERF_CHANNEL_ID
    PERF_CHANNEL_ID=${PERF_CHANNEL_ID:-performance-alerts}

    print_success "Channels configured"
    print_info "Deployment: $DEPLOY_CHANNEL_ID"
    print_info "Performance: $PERF_CHANNEL_ID"
    echo ""
}

test_slack_message() {
    print_header "Testing Slack Integration"

    local test_message="🧪 Test message from SASS CI/CD setup script"

    read -rp "Send test message to #$DEPLOY_CHANNEL_ID? (y/n): " send_test

    if [[ $send_test =~ ^[Yy]$ ]]; then
        local response=$(curl -s -X POST https://slack.com/api/chat.postMessage \
            -H "Authorization: Bearer $SLACK_BOT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"channel\":\"$DEPLOY_CHANNEL_ID\",\"text\":\"$test_message\"}")

        local ok=$(echo "$response" | jq -r '.ok')

        if [ "$ok" = "true" ]; then
            print_success "Test message sent successfully!"
        else
            local error=$(echo "$response" | jq -r '.error')
            print_error "Failed to send test message: $error"

            if [ "$error" = "channel_not_found" ]; then
                print_info "Make sure the channel exists and the bot is invited"
            elif [ "$error" = "not_in_channel" ]; then
                print_info "Invite the bot to the channel: /invite @SASS CI/CD Notifications"
            fi
        fi
    fi

    echo ""
}

set_github_secret() {
    print_header "Setting GitHub Secret"

    print_info "Setting SLACK_BOT_TOKEN in GitHub repository secrets..."

    # Set the secret using gh CLI
    echo "$SLACK_BOT_TOKEN" | gh secret set SLACK_BOT_TOKEN

    if [ $? -eq 0 ]; then
        print_success "GitHub secret 'SLACK_BOT_TOKEN' set successfully"
    else
        print_error "Failed to set GitHub secret"
        print_info "You can set it manually: gh secret set SLACK_BOT_TOKEN"
        exit 1
    fi

    echo ""
}

update_workflow_files() {
    print_header "Updating Workflow Files"

    print_info "The following workflow files use Slack notifications:"
    echo "  - .github/workflows/cd-deploy.yml"
    echo "  - .github/workflows/performance.yml"
    echo ""

    read -rp "Do you want to update channel IDs in workflow files? (y/n): " update_files

    if [[ $update_files =~ ^[Yy]$ ]]; then
        # Update cd-deploy.yml
        if [ -f ".github/workflows/cd-deploy.yml" ]; then
            sed -i.bak "s/channel-id: 'deployments'/channel-id: '$DEPLOY_CHANNEL_ID'/g" \
                .github/workflows/cd-deploy.yml
            print_success "Updated .github/workflows/cd-deploy.yml"
        fi

        # Update performance.yml
        if [ -f ".github/workflows/performance.yml" ]; then
            sed -i.bak "s/channel-id: 'performance-alerts'/channel-id: '$PERF_CHANNEL_ID'/g" \
                .github/workflows/performance.yml
            print_success "Updated .github/workflows/performance.yml"
        fi

        print_warning "Backup files created with .bak extension"
        print_info "Review changes and commit if satisfied"
    else
        print_info "Skipping workflow file updates"
        print_info "Manually update channel-id values in workflow files if needed"
    fi

    echo ""
}

create_setup_summary() {
    print_header "Setup Summary"

    cat << EOF

${GREEN}Slack Integration Setup Complete!${NC}

${BLUE}Configuration:${NC}
  ✓ Slack Bot Token: Set in GitHub secrets
  ✓ Deployment Channel: $DEPLOY_CHANNEL_ID
  ✓ Performance Channel: $PERF_CHANNEL_ID

${BLUE}Next Steps:${NC}
  1. Ensure Slack channels exist:
     - #$DEPLOY_CHANNEL_ID
     - #$PERF_CHANNEL_ID

  2. Invite bot to channels (if private):
     /invite @SASS CI/CD Notifications

  3. Test the integration:
     - Push code to main branch
     - Or manually trigger: gh workflow run performance.yml

  4. Review and commit workflow changes (if updated)

${BLUE}Documentation:${NC}
  Full guide: docs/integrations/slack-setup.md
  Troubleshooting: docs/integrations/slack-setup.md#troubleshooting

${BLUE}Verify Setup:${NC}
  gh secret list | grep SLACK_BOT_TOKEN
  gh workflow list

EOF
}

# Main execution
main() {
    clear
    print_header "SASS Platform - Slack Integration Setup"
    echo ""

    check_dependencies
    verify_gh_auth
    get_slack_token
    verify_slack_token
    get_channel_ids
    test_slack_message
    set_github_secret
    update_workflow_files
    create_setup_summary

    print_success "Setup completed successfully!"
}

# Run main function
main
