#!/bin/bash
# Script to verify GitHub Secrets configuration
# This script checks if required secrets are configured (without revealing values)

set -e

echo "🔍 GitHub Secrets Verification Script"
echo "======================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Repository secrets to check
REPO_SECRETS=(
    "AWS_REGION"
    "SONAR_TOKEN"
    "DOCKER_USERNAME"
    "DOCKER_PASSWORD"
    "SLACK_BOT_TOKEN"
    "STATUSPAGE_PAGE_ID"
    "STATUSPAGE_COMPONENT_ID"
    "STATUSPAGE_API_KEY"
)

# Optional secrets
OPTIONAL_SECRETS=(
    "SNYK_TOKEN"
    "GITGUARDIAN_API_KEY"
)

# Environment-specific secrets
STAGING_SECRETS=(
    "AWS_ACCESS_KEY_ID"
    "AWS_SECRET_ACCESS_KEY"
)

STAGING_VARS=(
    "EKS_CLUSTER_NAME_STAGING"
)

PRODUCTION_SECRETS=(
    "AWS_ACCESS_KEY_ID_PROD"
    "AWS_SECRET_ACCESS_KEY_PROD"
)

PRODUCTION_VARS=(
    "EKS_CLUSTER_NAME_PROD"
)

# Function to check if a secret exists
check_secret() {
    local secret_name=$1
    local env_name=$2

    if [ -z "$env_name" ]; then
        # Repository-level secret
        if gh secret list | grep -q "^${secret_name}"; then
            echo -e "${GREEN}✓${NC} ${secret_name}"
            return 0
        else
            echo -e "${RED}✗${NC} ${secret_name}"
            return 1
        fi
    else
        # Environment-level secret
        if gh secret list --env "$env_name" 2>/dev/null | grep -q "^${secret_name}"; then
            echo -e "${GREEN}✓${NC} ${secret_name} (in ${env_name})"
            return 0
        else
            echo -e "${RED}✗${NC} ${secret_name} (in ${env_name})"
            return 1
        fi
    fi
}

# Function to check if a variable exists
check_variable() {
    local var_name=$1
    local env_name=$2

    if [ -z "$env_name" ]; then
        # Repository-level variable
        if gh variable list | grep -q "^${var_name}"; then
            echo -e "${GREEN}✓${NC} ${var_name}"
            return 0
        else
            echo -e "${RED}✗${NC} ${var_name}"
            return 1
        fi
    else
        # Environment-level variable
        if gh variable list --env "$env_name" 2>/dev/null | grep -q "^${var_name}"; then
            echo -e "${GREEN}✓${NC} ${var_name} (in ${env_name})"
            return 0
        else
            echo -e "${RED}✗${NC} ${var_name} (in ${env_name})"
            return 1
        fi
    fi
}

# Check if environments exist
check_environment() {
    local env_name=$1
    if gh api "repos/{owner}/{repo}/environments/${env_name}" &> /dev/null; then
        echo -e "${GREEN}✓${NC} Environment '${env_name}' exists"
        return 0
    else
        echo -e "${RED}✗${NC} Environment '${env_name}' does not exist"
        return 1
    fi
}

echo "📦 Checking Environments"
echo "------------------------"
STAGING_EXISTS=0
PRODUCTION_EXISTS=0

if check_environment "staging"; then
    STAGING_EXISTS=1
fi

if check_environment "production"; then
    PRODUCTION_EXISTS=1
fi

echo ""

# Check repository-level secrets
echo "🔐 Checking Repository Secrets (Required)"
echo "------------------------------------------"
MISSING_REPO_SECRETS=0
for secret in "${REPO_SECRETS[@]}"; do
    if ! check_secret "$secret"; then
        ((MISSING_REPO_SECRETS++))
    fi
done
echo ""

# Check optional secrets
echo "🔐 Checking Repository Secrets (Optional)"
echo "------------------------------------------"
for secret in "${OPTIONAL_SECRETS[@]}"; do
    check_secret "$secret" || echo -e "   ${YELLOW}ℹ${NC}  This is optional but recommended for security scanning"
done
echo ""

# Check staging environment
if [ $STAGING_EXISTS -eq 1 ]; then
    echo "🔐 Checking Staging Environment Secrets"
    echo "----------------------------------------"
    MISSING_STAGING_SECRETS=0
    for secret in "${STAGING_SECRETS[@]}"; do
        if ! check_secret "$secret" "staging"; then
            ((MISSING_STAGING_SECRETS++))
        fi
    done

    echo ""
    echo "📊 Checking Staging Environment Variables"
    echo "------------------------------------------"
    for var in "${STAGING_VARS[@]}"; do
        check_variable "$var" "staging"
    done
    echo ""
fi

# Check production environment
if [ $PRODUCTION_EXISTS -eq 1 ]; then
    echo "🔐 Checking Production Environment Secrets"
    echo "-------------------------------------------"
    MISSING_PRODUCTION_SECRETS=0
    for secret in "${PRODUCTION_SECRETS[@]}"; do
        if ! check_secret "$secret" "production"; then
            ((MISSING_PRODUCTION_SECRETS++))
        fi
    done

    echo ""
    echo "📊 Checking Production Environment Variables"
    echo "---------------------------------------------"
    for var in "${PRODUCTION_VARS[@]}"; do
        check_variable "$var" "production"
    done
    echo ""
fi

# Summary
echo "📋 Summary"
echo "=========="

if [ $STAGING_EXISTS -eq 0 ]; then
    echo -e "${RED}❌ Staging environment not configured${NC}"
    echo -e "   Run: gh api repos/{owner}/{repo}/environments/staging -X PUT"
fi

if [ $PRODUCTION_EXISTS -eq 0 ]; then
    echo -e "${RED}❌ Production environment not configured${NC}"
    echo -e "   Run: gh api repos/{owner}/{repo}/environments/production -X PUT"
fi

if [ $MISSING_REPO_SECRETS -gt 0 ]; then
    echo -e "${RED}❌ ${MISSING_REPO_SECRETS} required repository secret(s) missing${NC}"
else
    echo -e "${GREEN}✓ All required repository secrets configured${NC}"
fi

if [ $STAGING_EXISTS -eq 1 ] && [ ${MISSING_STAGING_SECRETS:-0} -gt 0 ]; then
    echo -e "${RED}❌ ${MISSING_STAGING_SECRETS} staging environment secret(s) missing${NC}"
elif [ $STAGING_EXISTS -eq 1 ]; then
    echo -e "${GREEN}✓ All staging environment secrets configured${NC}"
fi

if [ $PRODUCTION_EXISTS -eq 1 ] && [ ${MISSING_PRODUCTION_SECRETS:-0} -gt 0 ]; then
    echo -e "${RED}❌ ${MISSING_PRODUCTION_SECRETS} production environment secret(s) missing${NC}"
elif [ $PRODUCTION_EXISTS -eq 1 ]; then
    echo -e "${GREEN}✓ All production environment secrets configured${NC}"
fi

echo ""
echo "For help setting up secrets, see: .github/ENVIRONMENTS.md"
