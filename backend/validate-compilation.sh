#!/bin/bash

# Compilation validation script for Spring Boot Modulith backend
# Returns exit code 0 if compilation succeeds, 1 if errors exist

echo "🔍 Starting compilation validation..."

# Clean previous build artifacts
echo "Cleaning previous build..."
./gradlew clean > /dev/null 2>&1

# Attempt compilation and capture output
echo "Compiling Java sources..."
COMPILE_OUTPUT=$(./gradlew compileJava 2>&1)
COMPILE_EXIT_CODE=$?

# Count errors
ERROR_COUNT=$(echo "$COMPILE_OUTPUT" | grep -c "error:")
TOTAL_ERRORS=$(echo "$COMPILE_OUTPUT" | grep -o "[0-9]\+ error" | head -1 | grep -o "[0-9]\+")

echo "📊 Compilation Results:"
echo "  Exit Code: $COMPILE_EXIT_CODE"
echo "  Error Count: ${TOTAL_ERRORS:-0}"

if [ $COMPILE_EXIT_CODE -eq 0 ]; then
    echo "✅ Compilation SUCCESSFUL - No errors found"
    echo "🎉 Implementation validation PASSED"
    exit 0
else
    echo "❌ Compilation FAILED with ${TOTAL_ERRORS:-unknown} errors"
    echo ""
    echo "🔍 First 10 errors:"
    echo "$COMPILE_OUTPUT" | grep "error:" | head -10
    exit 1
fi