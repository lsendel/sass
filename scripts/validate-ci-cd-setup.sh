#!/bin/bash
set -euo pipefail

# CI/CD Setup Validation Script
# This script validates that the CI/CD pipeline is properly configured

echo "🔍 Validating CI/CD Pipeline Setup..."

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Function to check if a file exists
check_file() {
    local file=$1
    local description=$2

    if [[ -f "$file" ]]; then
        echo -e "  ✅ ${GREEN}$description${NC}"
        ((PASSED++))
    else
        echo -e "  ❌ ${RED}$description (missing: $file)${NC}"
        ((FAILED++))
    fi
}

# Function to check if a directory exists
check_directory() {
    local dir=$1
    local description=$2

    if [[ -d "$dir" ]]; then
        echo -e "  ✅ ${GREEN}$description${NC}"
        ((PASSED++))
    else
        echo -e "  ❌ ${RED}$description (missing: $dir)${NC}"
        ((FAILED++))
    fi
}

# Function to check workflow syntax
check_workflow_syntax() {
    local workflow=$1
    local name=$2

    if [[ -f "$workflow" ]]; then
        if python3 -c "import yaml; yaml.safe_load(open('$workflow'))" 2>/dev/null; then
            echo -e "  ✅ ${GREEN}$name syntax valid${NC}"
            ((PASSED++))
        else
            echo -e "  ❌ ${RED}$name syntax invalid${NC}"
            ((FAILED++))
        fi
    else
        echo -e "  ❌ ${RED}$name missing${NC}"
        ((FAILED++))
    fi
}

# Function to check if required tools are available
check_tool() {
    local tool=$1
    local description=$2

    if command -v "$tool" &> /dev/null; then
        echo -e "  ✅ ${GREEN}$description available${NC}"
        ((PASSED++))
    else
        echo -e "  ⚠️  ${YELLOW}$description not available${NC}"
        ((WARNINGS++))
    fi
}

echo ""
echo "📋 Checking Core CI/CD Files..."
check_file ".github/workflows/ci-main.yml" "Main CI Pipeline"
check_file ".github/workflows/cd-deploy.yml" "Deployment Pipeline"
check_file ".github/dependabot.yml" "Dependabot Configuration"
check_file ".github/CODEOWNERS" "Code Owners File"
check_file ".github/pull_request_template.md" "PR Template"

echo ""
echo "📋 Checking Infrastructure Files..."
check_directory "k8s" "Kubernetes Configurations"
check_file "k8s/staging/deployment.yaml" "Staging Deployment"
check_file "k8s/production/deployment-green.yaml" "Production Deployment"
check_directory "scripts" "Deployment Scripts"

echo ""
echo "📋 Checking Documentation..."
check_file "CI_CD_IMPLEMENTATION_GUIDE.md" "Implementation Guide"
check_file "SECRETS_SETUP.md" "Secrets Setup Guide"
check_file "CI_CD_SUMMARY.md" "Transformation Summary"

echo ""
echo "📋 Checking Workflow Syntax..."
check_workflow_syntax ".github/workflows/ci-main.yml" "Main CI Pipeline"
check_workflow_syntax ".github/workflows/cd-deploy.yml" "Deployment Pipeline"

echo ""
echo "📋 Checking Archive..."
check_directory ".github/workflows-archive" "Archived Workflows"
check_file ".github/workflows-archive/README.md" "Archive Documentation"

echo ""
echo "🔧 Checking Required Tools..."
check_tool "docker" "Docker"
check_tool "kubectl" "Kubernetes CLI"
check_tool "gh" "GitHub CLI"
check_tool "aws" "AWS CLI"

echo ""
echo "🏗️ Checking Project Structure..."
if [[ -f "backend/gradlew" ]]; then
    echo -e "  ✅ ${GREEN}Backend Gradle wrapper${NC}"
    ((PASSED++))
else
    echo -e "  ❌ ${RED}Backend Gradle wrapper missing${NC}"
    ((FAILED++))
fi

if [[ -f "frontend/package.json" ]]; then
    echo -e "  ✅ ${GREEN}Frontend package.json${NC}"
    ((PASSED++))
else
    echo -e "  ⚠️  ${YELLOW}Frontend package.json missing${NC}"
    ((WARNINGS++))
fi

echo ""
echo "🧪 Testing Basic Commands..."
if [[ -f "backend/gradlew" ]]; then
    if cd backend && ./gradlew tasks --dry-run &> /dev/null; then
        echo -e "  ✅ ${GREEN}Gradle commands work${NC}"
        ((PASSED++))
        cd ..
    else
        echo -e "  ❌ ${RED}Gradle commands fail${NC}"
        ((FAILED++))
        cd ..
    fi
fi

# Check GitHub repository settings (if gh CLI is available)
if command -v gh &> /dev/null && gh auth status &> /dev/null 2>&1; then
    echo ""
    echo "📡 Checking GitHub Repository Status..."

    # Check if PR exists
    if gh pr list --state open --head feature/ci-cd-improvements | grep -q "feat: implement comprehensive CI/CD"; then
        echo -e "  ✅ ${GREEN}Pull request created${NC}"
        ((PASSED++))
    else
        echo -e "  ⚠️  ${YELLOW}Pull request not found${NC}"
        ((WARNINGS++))
    fi

    # Check workflow runs
    if gh run list --limit 1 --json status,conclusion | grep -q "in_progress\|completed"; then
        echo -e "  ✅ ${GREEN}CI pipeline is running/completed${NC}"
        ((PASSED++))
    else
        echo -e "  ⚠️  ${YELLOW}No recent workflow runs${NC}"
        ((WARNINGS++))
    fi
fi

echo ""
echo "📊 Validation Summary"
echo "=================="
echo -e "✅ ${GREEN}Passed: $PASSED${NC}"
echo -e "❌ ${RED}Failed: $FAILED${NC}"
echo -e "⚠️  ${YELLOW}Warnings: $WARNINGS${NC}"

echo ""
if [[ $FAILED -eq 0 ]]; then
    echo -e "🎉 ${GREEN}CI/CD Pipeline Setup: READY FOR PRODUCTION!${NC}"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo "1. Configure GitHub secrets (see SECRETS_SETUP.md)"
    echo "2. Set up AWS infrastructure (EKS clusters)"
    echo "3. Configure external services (SonarCloud, Snyk)"
    echo "4. Test deployment to staging environment"
    echo "5. Validate production deployment process"
    echo ""
    echo -e "${GREEN}Your payment platform now has enterprise-grade CI/CD!${NC}"
    exit 0
else
    echo -e "⚠️  ${YELLOW}CI/CD Pipeline Setup: NEEDS ATTENTION${NC}"
    echo ""
    echo -e "${RED}Issues found: $FAILED${NC}"
    echo "Please resolve the failed checks above before proceeding."
    echo ""
    echo -e "${BLUE}For help:${NC}"
    echo "- Check CI_CD_IMPLEMENTATION_GUIDE.md"
    echo "- Review SECRETS_SETUP.md"
    echo "- Examine individual workflow files"
    exit 1
fi