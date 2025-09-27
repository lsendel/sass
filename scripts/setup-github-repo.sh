#!/bin/bash
set -euo pipefail

# GitHub Repository Configuration Script
# This script configures GitHub repository settings for the CI/CD pipeline

REPO_OWNER="lsendel"
REPO_NAME="sass"

echo "🔧 Configuring GitHub repository settings for CI/CD pipeline..."

# Check if gh CLI is available
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) is not installed. Please install it first:"
    echo "   brew install gh"
    echo "   or visit: https://cli.github.com/"
    exit 1
fi

# Check if user is authenticated
if ! gh auth status &> /dev/null; then
    echo "❌ Not authenticated with GitHub CLI. Please run:"
    echo "   gh auth login"
    exit 1
fi

echo "✅ GitHub CLI is available and authenticated"

# Create environments
echo "🌍 Creating GitHub environments..."

# Create staging environment
echo "Creating staging environment..."
gh api \
  --method PUT \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "/repos/${REPO_OWNER}/${REPO_NAME}/environments/staging" \
  -f environment_name='staging' \
  --silent || echo "Staging environment might already exist"

# Create production environment with protection rules
echo "Creating production environment with protection rules..."
gh api \
  --method PUT \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "/repos/${REPO_OWNER}/${REPO_NAME}/environments/production" \
  -f environment_name='production' \
  -f wait_timer=0 \
  -f prevent_self_review=false \
  -f reviewers='[]' \
  -f deployment_branch_policy='{"protected_branches":true,"custom_branch_policies":false}' \
  --silent || echo "Production environment might already exist"

# Set repository variables
echo "📝 Setting repository variables..."

# AWS Region
gh variable set AWS_REGION --body "us-east-1" --repo "${REPO_OWNER}/${REPO_NAME}" || echo "Failed to set AWS_REGION variable"

# EKS Cluster Names
gh variable set EKS_CLUSTER_NAME_STAGING --body "payment-platform-staging" --repo "${REPO_OWNER}/${REPO_NAME}" || echo "Failed to set EKS_CLUSTER_NAME_STAGING variable"
gh variable set EKS_CLUSTER_NAME_PROD --body "payment-platform-prod" --repo "${REPO_OWNER}/${REPO_NAME}" || echo "Failed to set EKS_CLUSTER_NAME_PROD variable"

# SonarCloud Configuration
gh variable set SONAR_PROJECT_KEY --body "${REPO_OWNER}_${REPO_NAME}" --repo "${REPO_OWNER}/${REPO_NAME}" || echo "Failed to set SONAR_PROJECT_KEY variable"

# Testing Configuration
gh variable set TARGET_URL --body "https://staging-api.paymentplatform.com" --repo "${REPO_OWNER}/${REPO_NAME}" || echo "Failed to set TARGET_URL variable"
gh variable set LOAD_TEST_USERS --body "50" --repo "${REPO_OWNER}/${REPO_NAME}" || echo "Failed to set LOAD_TEST_USERS variable"

echo "✅ Repository variables configured successfully"

# Enable GitHub features
echo "⚙️ Enabling GitHub features..."

# Enable vulnerability alerts
gh api \
  --method PUT \
  -H "Accept: application/vnd.github+json" \
  "/repos/${REPO_OWNER}/${REPO_NAME}/vulnerability-alerts" \
  --silent || echo "Vulnerability alerts might already be enabled"

# Enable automated security fixes
gh api \
  --method PUT \
  -H "Accept: application/vnd.github+json" \
  "/repos/${REPO_OWNER}/${REPO_NAME}/automated-security-fixes" \
  --silent || echo "Automated security fixes might already be enabled"

# Enable Dependabot alerts
gh api \
  --method PATCH \
  -H "Accept: application/vnd.github+json" \
  "/repos/${REPO_OWNER}/${REPO_NAME}" \
  -f has_vulnerability_alerts=true \
  --silent || echo "Repository settings might already be configured"

echo "✅ GitHub security features enabled"

# Set branch protection rules
echo "🛡️ Setting branch protection rules..."

# Protect main branch
gh api \
  --method PUT \
  -H "Accept: application/vnd.github+json" \
  "/repos/${REPO_OWNER}/${REPO_NAME}/branches/main/protection" \
  -f required_status_checks='{"strict":true,"contexts":["Constitutional Compliance Check","Security Scanning","Backend Tests"]}' \
  -f enforce_admins=false \
  -f required_pull_request_reviews='{"required_approving_review_count":2,"dismiss_stale_reviews":true,"require_code_owner_reviews":true}' \
  -f restrictions=null \
  -f allow_force_pushes=false \
  -f allow_deletions=false \
  --silent || echo "Main branch protection might already be configured"

# Protect develop branch
gh api \
  --method PUT \
  -H "Accept: application/vnd.github+json" \
  "/repos/${REPO_OWNER}/${REPO_NAME}/branches/develop/protection" \
  -f required_status_checks='{"strict":true,"contexts":["Constitutional Compliance Check","Security Scanning","Backend Tests"]}' \
  -f enforce_admins=false \
  -f required_pull_request_reviews='{"required_approving_review_count":1,"dismiss_stale_reviews":true,"require_code_owner_reviews":true}' \
  -f restrictions=null \
  -f allow_force_pushes=false \
  -f allow_deletions=false \
  --silent || echo "Develop branch protection might already be configured or branch doesn't exist"

echo "✅ Branch protection rules configured"

# Display secrets that need manual configuration
echo ""
echo "🔐 MANUAL ACTION REQUIRED: Configure the following secrets in GitHub repository settings:"
echo "   Go to: https://github.com/${REPO_OWNER}/${REPO_NAME}/settings/secrets/actions"
echo ""
echo "📋 Required Repository Secrets:"
echo "   ├── AWS_ACCESS_KEY_ID (AWS access key for staging)"
echo "   ├── AWS_SECRET_ACCESS_KEY (AWS secret key for staging)"
echo "   ├── AWS_ACCESS_KEY_ID_PROD (AWS access key for production)"
echo "   ├── AWS_SECRET_ACCESS_KEY_PROD (AWS secret key for production)"
echo "   ├── STRIPE_TEST_SECRET_KEY (Stripe test environment key)"
echo "   ├── SONAR_TOKEN (SonarCloud authentication token)"
echo "   ├── SNYK_TOKEN (Snyk security scanning token)"
echo "   ├── PACT_BROKER_TOKEN (Pact contract testing token)"
echo "   └── SLACK_WEBHOOK (Slack notifications webhook URL)"
echo ""
echo "📋 Environment-specific Secrets:"
echo "   Staging Environment:"
echo "   ├── STAGING_DATABASE_URL"
echo "   └── STAGING_REDIS_URL"
echo ""
echo "   Production Environment:"
echo "   ├── PROD_DATABASE_URL"
echo "   └── PROD_REDIS_URL"
echo ""
echo "📖 For detailed setup instructions, see:"
echo "   - SECRETS_SETUP.md"
echo "   - CI_CD_IMPLEMENTATION_GUIDE.md"
echo ""
echo "🎉 GitHub repository configuration completed!"
echo "   Next: Configure the secrets above and test the CI/CD pipeline"