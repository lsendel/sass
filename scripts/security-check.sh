#!/bin/bash

# Security Check Script for Payment Platform
# Run this before committing code to catch security issues early

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🔒 Running Security Checks for Payment Platform"
echo "================================================"

# Track failures
FAILED_CHECKS=0

# Function to print results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
}

# 1. Check for secrets in code
echo ""
echo "1. Checking for hardcoded secrets..."
if command -v gitleaks &> /dev/null; then
    gitleaks detect --no-git --verbose 2>/dev/null
    print_result $? "Secret scanning"
else
    # Fallback to grep patterns
    SECRET_PATTERNS=(
        "api[_-]key.*=.*['\"].*['\"]"
        "secret.*=.*['\"].*['\"]"
        "password.*=.*['\"].*['\"]"
        "token.*=.*['\"].*['\"]"
        "sk_test_"
        "sk_live_"
    )

    FOUND_SECRETS=0
    for pattern in "${SECRET_PATTERNS[@]}"; do
        if grep -r "$pattern" --include="*.java" --include="*.ts" --include="*.tsx" --exclude-dir=node_modules --exclude-dir=.git "$PROJECT_ROOT" 2>/dev/null; then
            FOUND_SECRETS=1
        fi
    done

    if [ $FOUND_SECRETS -eq 0 ]; then
        print_result 0 "No hardcoded secrets found"
    else
        print_result 1 "Hardcoded secrets detected!"
    fi
fi

# 2. Check for vulnerable dependencies
echo ""
echo "2. Checking for vulnerable dependencies..."

# Backend dependency check
cd "$PROJECT_ROOT/backend"
if ./gradlew dependencyCheckAnalyze 2>/dev/null; then
    print_result 0 "Backend dependencies"
else
    print_result 1 "Backend dependency vulnerabilities found"
fi

# Frontend dependency check
cd "$PROJECT_ROOT/frontend"
npm audit --audit-level=high 2>/dev/null
NPM_AUDIT_RESULT=$?
if [ $NPM_AUDIT_RESULT -eq 0 ]; then
    print_result 0 "Frontend dependencies"
else
    print_result 1 "Frontend dependency vulnerabilities found"
fi

# 3. Check for localStorage auth usage
echo ""
echo "3. Checking for insecure authentication storage..."
if grep -r "localStorage.*auth\|localStorage.*token" "$PROJECT_ROOT/frontend/src" 2>/dev/null; then
    print_result 1 "localStorage used for authentication tokens!"
    echo "   Found instances:"
    grep -r "localStorage.*auth\|localStorage.*token" "$PROJECT_ROOT/frontend/src" --include="*.ts" --include="*.tsx" | head -5
else
    print_result 0 "No localStorage auth usage"
fi

# 4. Check for console.log statements
echo ""
echo "4. Checking for console.log statements..."
CONSOLE_COUNT=$(grep -r "console\.\(log\|error\|warn\|info\)" "$PROJECT_ROOT/frontend/src" --include="*.ts" --include="*.tsx" 2>/dev/null | wc -l)
if [ $CONSOLE_COUNT -gt 10 ]; then
    print_result 1 "Found $CONSOLE_COUNT console statements (should be < 10)"
else
    print_result 0 "Console statements within limits ($CONSOLE_COUNT found)"
fi

# 5. Check security headers configuration
echo ""
echo "5. Checking security headers configuration..."
HEADERS_FOUND=0
REQUIRED_HEADERS=(
    "Content-Security-Policy"
    "Strict-Transport-Security"
    "X-Frame-Options"
    "X-Content-Type-Options"
    "Referrer-Policy"
)

for header in "${REQUIRED_HEADERS[@]}"; do
    if grep -r "$header" "$PROJECT_ROOT/backend/src" 2>/dev/null | grep -q "$header"; then
        HEADERS_FOUND=$((HEADERS_FOUND + 1))
    fi
done

if [ $HEADERS_FOUND -eq ${#REQUIRED_HEADERS[@]} ]; then
    print_result 0 "All security headers configured"
else
    print_result 1 "Missing security headers ($HEADERS_FOUND/${#REQUIRED_HEADERS[@]} found)"
fi

# 6. Check for proper input validation
echo ""
echo "6. Checking input validation..."
VALIDATION_COUNT=$(grep -r "@Valid\|@Validated\|@NotNull\|@NotBlank" "$PROJECT_ROOT/backend/src/main/java" 2>/dev/null | wc -l)
CONTROLLER_COUNT=$(find "$PROJECT_ROOT/backend/src/main/java" -name "*Controller.java" 2>/dev/null | wc -l)

if [ $VALIDATION_COUNT -gt $((CONTROLLER_COUNT * 5)) ]; then
    print_result 0 "Input validation present ($VALIDATION_COUNT annotations)"
else
    print_result 1 "Insufficient input validation ($VALIDATION_COUNT annotations for $CONTROLLER_COUNT controllers)"
fi

# 7. Check for SQL injection prevention
echo ""
echo "7. Checking SQL injection prevention..."
if grep -r "createQuery\|createNativeQuery" "$PROJECT_ROOT/backend/src" --include="*.java" 2>/dev/null | grep -v "@Query"; then
    print_result 1 "Potential SQL injection risk (raw queries found)"
else
    print_result 0 "No raw SQL queries found"
fi

# 8. Check CORS configuration
echo ""
echo "8. Checking CORS configuration..."
if grep -r "setAllowedOrigins.*\*\|addAllowedOrigin.*\*" "$PROJECT_ROOT/backend/src" --include="*.java" 2>/dev/null; then
    print_result 1 "Wildcard CORS origin found!"
else
    print_result 0 "No wildcard CORS origins"
fi

# 9. Check password configuration
echo ""
echo "9. Checking password security configuration..."
PASSWORD_LENGTH=$(grep -r "min-length:" "$PROJECT_ROOT/backend/src/main/resources/application.yml" 2>/dev/null | grep -o '[0-9]\+' | head -1)
if [ -n "$PASSWORD_LENGTH" ] && [ "$PASSWORD_LENGTH" -ge 12 ]; then
    print_result 0 "Password minimum length: $PASSWORD_LENGTH characters"
else
    print_result 1 "Password length too short or not configured"
fi

# 10. Check for rate limiting
echo ""
echo "10. Checking rate limiting configuration..."
if grep -r "RateLimiter\|Bucket4j\|rate.*limit" "$PROJECT_ROOT/backend/src" --include="*.java" 2>/dev/null | grep -q .; then
    print_result 0 "Rate limiting configured"
else
    print_result 1 "No rate limiting found"
fi

# 11. Run backend tests
echo ""
echo "11. Running backend security tests..."
cd "$PROJECT_ROOT/backend"
if ./gradlew test --tests "*SecurityTest" --tests "*AuthTest" 2>/dev/null; then
    print_result 0 "Backend security tests passed"
else
    print_result 1 "Backend security tests failed"
fi

# 12. Run frontend tests
echo ""
echo "12. Running frontend security tests..."
cd "$PROJECT_ROOT/frontend"
if npm run test -- --run --reporter=verbose auth 2>/dev/null; then
    print_result 0 "Frontend auth tests passed"
else
    print_result 1 "Frontend auth tests failed"
fi

# 13. Check for TypeScript strict mode
echo ""
echo "13. Checking TypeScript configuration..."
if grep -q '"strict": true' "$PROJECT_ROOT/frontend/tsconfig.json"; then
    print_result 0 "TypeScript strict mode enabled"
else
    print_result 1 "TypeScript strict mode not enabled"
fi

# 14. Check for environment variables
echo ""
echo "14. Checking environment configuration..."
ENV_FILES=(
    "$PROJECT_ROOT/.env"
    "$PROJECT_ROOT/backend/.env"
    "$PROJECT_ROOT/frontend/.env"
)

ENV_FOUND=0
for env_file in "${ENV_FILES[@]}"; do
    if [ -f "$env_file" ]; then
        if grep -q "SECRET\|KEY\|PASSWORD" "$env_file"; then
            ENV_FOUND=1
            echo -e "${YELLOW}⚠${NC} Warning: $env_file contains sensitive values"
        fi
    fi
done

if [ $ENV_FOUND -eq 0 ]; then
    print_result 0 "No sensitive .env files in repository"
else
    print_result 1 "Sensitive .env files found (ensure they're in .gitignore)"
fi

# 15. Check Docker security
echo ""
echo "15. Checking Docker configuration..."
if [ -f "$PROJECT_ROOT/backend/Dockerfile" ]; then
    if grep -q "USER \|--chown=" "$PROJECT_ROOT/backend/Dockerfile"; then
        print_result 0 "Docker non-root user configured"
    else
        print_result 1 "Docker running as root user"
    fi
else
    echo -e "${YELLOW}⚠${NC} No Dockerfile found"
fi

# Summary
echo ""
echo "================================================"
echo "Security Check Summary"
echo "================================================"

if [ $FAILED_CHECKS -eq 0 ]; then
    echo -e "${GREEN}✓ All security checks passed!${NC}"
    echo "Your code is ready for commit."
    exit 0
else
    echo -e "${RED}✗ $FAILED_CHECKS security checks failed${NC}"
    echo ""
    echo "Please fix the issues above before committing."
    echo "Run 'cat SECURITY_FIX_GUIDE.md' for detailed remediation steps."
    exit 1
fi