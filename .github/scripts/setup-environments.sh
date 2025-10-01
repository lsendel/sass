#!/bin/bash
# Script to set up GitHub Environments using GitHub CLI
# This creates the staging and production environments

set -e

echo "🚀 GitHub Environments Setup Script"
echo "===================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo -e "${RED}❌ GitHub CLI (gh) is not installed${NC}"
    echo "   Install it from: https://cli.github.com/"
    exit 1
fi

echo -e "${GREEN}✓ GitHub CLI is installed${NC}"
echo ""

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo -e "${RED}❌ Not authenticated with GitHub${NC}"
    echo "   Run: gh auth login"
    exit 1
fi

echo -e "${GREEN}✓ Authenticated with GitHub${NC}"
echo ""

# Get repository information
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo -e "${BLUE}Repository:${NC} $REPO"
echo ""

# Function to create environment
create_environment() {
    local env_name=$1
    local branch_pattern=$2
    local wait_timer=$3
    local reviewers=$4

    echo -e "${BLUE}Creating environment: ${env_name}${NC}"

    # Create the environment
    if gh api "repos/{owner}/{repo}/environments/${env_name}" -X PUT &> /dev/null; then
        echo -e "${GREEN}✓${NC} Environment '${env_name}' created"
    else
        echo -e "${YELLOW}⚠${NC}  Environment '${env_name}' may already exist, updating..."
    fi

    # Set deployment branch policy
    echo "  Setting deployment branch policy..."
    gh api "repos/{owner}/{repo}/environments/${env_name}" -X PUT \
        -f "deployment_branch_policy[protected_branches]=false" \
        -f "deployment_branch_policy[custom_branch_policies]=true" &> /dev/null || true

    # Add branch pattern
    echo "  Adding branch pattern: ${branch_pattern}"
    gh api "repos/{owner}/{repo}/environments/${env_name}/deployment-branch-policies" -X POST \
        -f "name=${branch_pattern}" &> /dev/null || echo -e "  ${YELLOW}⚠${NC}  Branch pattern may already exist"

    # Set wait timer if specified
    if [ -n "$wait_timer" ] && [ "$wait_timer" -gt 0 ]; then
        echo "  Setting wait timer: ${wait_timer} minutes"
        gh api "repos/{owner}/{repo}/environments/${env_name}" -X PUT \
            -f "wait_timer=${wait_timer}" &> /dev/null || true
    fi

    # Set reviewers if specified (for production)
    if [ -n "$reviewers" ]; then
        echo "  Reviewer configuration needs to be done via GitHub UI"
        echo -e "  ${YELLOW}ℹ${NC}  Go to Settings → Environments → ${env_name} → Required reviewers"
    fi

    echo ""
}

# Create staging environment
echo "📦 Setting up Staging Environment"
echo "----------------------------------"
create_environment "staging" "develop" 0 ""

# Create production environment
echo "📦 Setting up Production Environment"
echo "-------------------------------------"
create_environment "production" "main" 5 "true"

echo -e "${GREEN}✅ Environments created successfully!${NC}"
echo ""
echo "📋 Next Steps:"
echo "-------------"
echo "1. Add environment secrets using GitHub CLI or UI"
echo ""
echo "   ${BLUE}Via GitHub CLI:${NC}"
echo "   gh secret set AWS_ACCESS_KEY_ID --env staging --body 'YOUR_KEY'"
echo "   gh secret set AWS_SECRET_ACCESS_KEY --env staging --body 'YOUR_SECRET'"
echo "   gh variable set EKS_CLUSTER_NAME_STAGING --env staging --body 'your-cluster-name'"
echo ""
echo "   gh secret set AWS_ACCESS_KEY_ID_PROD --env production --body 'YOUR_KEY'"
echo "   gh secret set AWS_SECRET_ACCESS_KEY_PROD --env production --body 'YOUR_SECRET'"
echo "   gh variable set EKS_CLUSTER_NAME_PROD --env production --body 'your-cluster-name'"
echo ""
echo "   ${BLUE}Repository-level secrets:${NC}"
echo "   gh secret set AWS_REGION --body 'us-east-1'"
echo "   gh secret set SONAR_TOKEN --body 'YOUR_TOKEN'"
echo "   gh secret set DOCKER_USERNAME --body 'YOUR_USERNAME'"
echo "   gh secret set DOCKER_PASSWORD --body 'YOUR_PASSWORD'"
echo "   gh secret set SLACK_BOT_TOKEN --body 'YOUR_TOKEN'"
echo "   gh secret set STATUSPAGE_PAGE_ID --body 'YOUR_PAGE_ID'"
echo "   gh secret set STATUSPAGE_COMPONENT_ID --body 'YOUR_COMPONENT_ID'"
echo "   gh secret set STATUSPAGE_API_KEY --body 'YOUR_API_KEY'"
echo ""
echo "2. Configure protection rules (recommended for production):"
echo "   - Go to: Settings → Environments → production"
echo "   - Enable 'Required reviewers'"
echo "   - Add team members as reviewers"
echo ""
echo "3. Run verification script to check setup:"
echo "   .github/scripts/verify-secrets.sh"
echo ""
echo "For more details, see: .github/ENVIRONMENTS.md"
