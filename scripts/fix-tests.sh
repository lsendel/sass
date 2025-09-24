#!/bin/bash

# SASS Test Fixing Script
# This script systematically fixes common test issues

set -e

echo "🔧 SASS Test Fixing Script"
echo "=========================="

# Check Java version
echo "📋 Checking Java version..."
java -version

# Clean and rebuild
echo "🧹 Cleaning build artifacts..."
cd backend
./gradlew clean

# Run tests with detailed output to identify issues
echo "🧪 Running tests with detailed output..."
./gradlew test --info --continue > ../test-analysis.log 2>&1 || true

# Analyze common test failures
echo "🔍 Analyzing test failures..."
grep -i "failed\|error\|exception" ../test-analysis.log | head -20

echo "📊 Test Summary:"
grep -E "tests completed|failed|passed" ../test-analysis.log | tail -5

# Check for common issues
echo "🔍 Checking for common issues..."

# 1. Database connection issues
if grep -q "Connection refused\|database" ../test-analysis.log; then
    echo "❌ Database connection issues detected"
    echo "💡 Suggestion: Check H2/PostgreSQL configuration"
fi

# 2. Port conflicts
if grep -q "Port already in use\|Address already in use" ../test-analysis.log; then
    echo "❌ Port conflicts detected"
    echo "💡 Suggestion: Kill existing processes or use different ports"
fi

# 3. Missing dependencies
if grep -q "ClassNotFoundException\|NoClassDefFoundError" ../test-analysis.log; then
    echo "❌ Missing dependencies detected"
    echo "💡 Suggestion: Check build.gradle dependencies"
fi

# 4. Spring context issues
if grep -q "ApplicationContextException\|BeanCreationException" ../test-analysis.log; then
    echo "❌ Spring context issues detected"
    echo "💡 Suggestion: Check application configuration"
fi

echo ""
echo "📋 Next Steps:"
echo "1. Review test-analysis.log for detailed error information"
echo "2. Fix identified issues systematically"
echo "3. Run individual test classes to isolate problems"
echo "4. Use './gradlew test --tests ClassName' for specific tests"

cd ..
