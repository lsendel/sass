#!/bin/bash

echo "=== API Documentation Validation Test ==="
echo "Date: $(date)"
echo ""

# Test 1: Check if SpringDoc dependencies are properly configured
echo "1. Testing SpringDoc Dependencies..."
cd /Users/lsendel/IdeaProjects/sass/backend
if grep -q "springdoc-openapi" build.gradle; then
    echo "✅ SpringDoc dependencies found in build.gradle"
else
    echo "❌ SpringDoc dependencies missing"
fi

# Test 2: Check OpenAPI configuration
echo ""
echo "2. Testing OpenAPI Configuration..."
if [ -f "src/main/java/com/platform/shared/config/OpenApiConfig.java" ]; then
    echo "✅ OpenApiConfig.java exists"
    if grep -q "@Configuration" src/main/java/com/platform/shared/config/OpenApiConfig.java; then
        echo "✅ OpenApiConfig is properly annotated"
    else
        echo "❌ OpenApiConfig missing @Configuration"
    fi
else
    echo "❌ OpenApiConfig.java not found"
fi

# Test 3: Check controller annotations
echo ""
echo "3. Testing Controller Annotations..."
controllers=("PasswordAuthController" "PaymentController" "SubscriptionController")
for controller in "${controllers[@]}"; do
    file="src/main/java/com/platform/*/api/${controller}.java"
    if ls $file 1> /dev/null 2>&1; then
        if grep -q "@Tag" $file; then
            echo "✅ $controller has @Tag annotation"
        else
            echo "❌ $controller missing @Tag annotation"
        fi
        if grep -q "@Operation" $file; then
            echo "✅ $controller has @Operation annotations"
        else
            echo "❌ $controller missing @Operation annotations"
        fi
    else
        echo "❌ $controller not found"
    fi
done

# Test 4: Check application.yml configuration
echo ""
echo "4. Testing Application Configuration..."
if grep -q "springdoc:" src/main/resources/application.yml; then
    echo "✅ SpringDoc configuration found in application.yml"
else
    echo "❌ SpringDoc configuration missing from application.yml"
fi

# Test 5: Compilation test
echo ""
echo "5. Testing Compilation..."
if ./gradlew compileJava --quiet; then
    echo "✅ Code compiles successfully"
else
    echo "❌ Compilation failed"
fi

# Test 6: Check documentation files
echo ""
echo "6. Testing Documentation Files..."
if [ -f "docs/API_DOCUMENTATION.md" ]; then
    echo "✅ API_DOCUMENTATION.md exists"
    word_count=$(wc -w < docs/API_DOCUMENTATION.md)
    echo "   Documentation contains $word_count words"
else
    echo "❌ API_DOCUMENTATION.md not found"
fi

# Test 7: Mock endpoint test (without running server)
echo ""
echo "7. Testing Endpoint Structure..."
auth_endpoints=$(grep -r "@PostMapping\|@GetMapping" src/main/java/com/platform/auth/api/ | wc -l)
payment_endpoints=$(grep -r "@PostMapping\|@GetMapping" src/main/java/com/platform/payment/api/ | wc -l)
echo "   Authentication endpoints found: $auth_endpoints"
echo "   Payment endpoints found: $payment_endpoints"

echo ""
echo "=== Documentation Validation Summary ==="
echo "✅ Static validation completed"
echo "📝 For live testing, start the backend with: ./gradlew bootRun"
echo "🌐 Then access: http://localhost:8082/swagger-ui.html"
echo ""
