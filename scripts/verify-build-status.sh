#!/bin/bash
#
# Build Verification Script
# Verifies the actual build status and identifies any errors/warnings
#
set -euo pipefail

echo "🔍 SASS Platform - Build Verification"
echo "======================================"
echo ""

cd "$(dirname "$0")/.."

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "📁 Working Directory: $(pwd)"
echo ""

# Test 1: Backend Compilation
echo "🔨 Test 1: Backend Compilation"
echo "------------------------------"
cd backend
if ./gradlew clean compileJava compileTestJava --console=plain 2>&1 | tee /tmp/backend-compile.log | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✅ Backend compilation: SUCCESS${NC}"
    COMPILE_ERRORS=$(grep -c "error:" /tmp/backend-compile.log || echo "0")
    COMPILE_WARNINGS=$(grep -c "warning:" /tmp/backend-compile.log || echo "0")
    echo "   Errors: $COMPILE_ERRORS"
    echo "   Warnings: $COMPILE_WARNINGS"
else
    echo -e "${RED}❌ Backend compilation: FAILED${NC}"
    COMPILE_ERRORS=$(grep -c "error:" /tmp/backend-compile.log || echo "0")
    echo "   Errors: $COMPILE_ERRORS"
    grep "error:" /tmp/backend-compile.log | head -10
fi
echo ""

# Test 2: Full Build (without tests)
echo "🏗️  Test 2: Full Build (no tests)"
echo "--------------------------------"
if ./gradlew clean build -x test --console=plain 2>&1 | tee /tmp/backend-build.log | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✅ Full build: SUCCESS${NC}"
else
    echo -e "${RED}❌ Full build: FAILED${NC}"
    grep "error:" /tmp/backend-build.log | head -10
fi
echo ""

# Test 3: Check for deprecated API usage
echo "📝 Test 3: Deprecated API Check"
echo "--------------------------------"
DEPRECATED_COUNT=$(find src -name "*.java" -exec grep -l "@Deprecated\|MockBean" {} \; 2>/dev/null | wc -l | tr -d ' ')
echo "   Files with deprecated APIs: $DEPRECATED_COUNT"
echo ""

# Test 4: List all Java files
echo "📊 Test 4: Source File Count"
echo "-----------------------------"
MAIN_FILES=$(find src/main/java -name "*.java" 2>/dev/null | wc -l | tr -d ' ')
TEST_FILES=$(find src/test/java -name "*.java" 2>/dev/null | wc -l | tr -d ' ')
echo "   Main source files: $MAIN_FILES"
echo "   Test source files: $TEST_FILES"
echo ""

# Test 5: Check Gradle status
echo "⚙️  Test 5: Gradle Configuration"
echo "--------------------------------"
if ./gradlew projects --console=plain > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Gradle configuration: VALID${NC}"
else
    echo -e "${RED}❌ Gradle configuration: INVALID${NC}"
fi
echo ""

# Test 6: IntelliJ IDEA specific checks
echo "💡 Test 6: IDE Compatibility"
echo "-----------------------------"
if [ -d "../.idea" ]; then
    echo "   IntelliJ IDEA project detected"
    echo "   Recommendation: File → Invalidate Caches / Restart"

    # Check for .iml files
    IML_COUNT=$(find .. -name "*.iml" 2>/dev/null | wc -l | tr -d ' ')
    echo "   Module files found: $IML_COUNT"
fi
echo ""

# Summary
echo "📋 Summary"
echo "=========="
echo -e "Compilation Errors: ${COMPILE_ERRORS}"
echo -e "Compilation Warnings: ${COMPILE_WARNINGS}"
echo ""

if [ "$COMPILE_ERRORS" -eq "0" ]; then
    echo -e "${GREEN}✅ BUILD IS CLEAN - No compilation errors${NC}"
else
    echo -e "${RED}❌ BUILD HAS ERRORS - See above for details${NC}"
fi

echo ""
echo "📄 Full logs saved to:"
echo "   - /tmp/backend-compile.log"
echo "   - /tmp/backend-build.log"
echo ""

# If IDE shows different results
if [ "$COMPILE_ERRORS" -eq "0" ]; then
    echo "💡 If your IDE shows errors but Gradle doesn't:"
    echo "   1. Invalidate IDE caches: File → Invalidate Caches / Restart"
    echo "   2. Reimport Gradle project: Right-click build.gradle → Gradle → Reimport"
    echo "   3. Rebuild project: Build → Rebuild Project"
    echo ""
fi

exit 0
