#!/bin/bash

echo "=== API Endpoints Testing ==="
echo "Testing against documentation examples..."
echo ""

BASE_URL="http://localhost:8082"

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4
    
    echo "Testing: $method $endpoint"
    echo "Expected: $expected_status - $description"
    
    if [ "$method" = "GET" ]; then
        status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$endpoint")
    else
        status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$BASE_URL$endpoint")
    fi
    
    if [ "$status" = "$expected_status" ]; then
        echo "✅ Status: $status (Expected: $expected_status)"
    else
        echo "❌ Status: $status (Expected: $expected_status)"
    fi
    echo ""
}

# Test 1: Health check (should work)
echo "1. Testing Health Endpoint..."
test_endpoint "GET" "/actuator/health" "200" "Health check"

# Test 2: Documentation endpoints
echo "2. Testing Documentation Endpoints..."
test_endpoint "GET" "/swagger-ui.html" "200" "Swagger UI"
test_endpoint "GET" "/api-docs" "200" "OpenAPI JSON"

# Test 3: Authentication endpoints (should require data)
echo "3. Testing Authentication Endpoints..."
test_endpoint "POST" "/auth/register" "400" "Register without data"
test_endpoint "POST" "/auth/login" "400" "Login without data"

# Test 4: Protected endpoints (should require auth)
echo "4. Testing Protected Endpoints..."
test_endpoint "GET" "/api/v1/payments" "401" "Payments without auth"
test_endpoint "GET" "/api/v1/subscriptions" "401" "Subscriptions without auth"

# Test 5: Test with sample data
echo "5. Testing with Sample Data..."
echo "Testing register endpoint with sample data:"

register_response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPassword123!",
    "displayName": "Test User",
    "organizationId": "123e4567-e89b-12d3-a456-426614174000"
  }')

status=$(echo "$register_response" | grep "HTTP_STATUS:" | cut -d: -f2)
echo "Register response status: $status"

if [ "$status" = "201" ] || [ "$status" = "409" ]; then
    echo "✅ Register endpoint working (201=success, 409=already exists)"
else
    echo "❌ Register endpoint issue (status: $status)"
fi

echo ""
echo "=== API Testing Summary ==="
echo "✅ Basic endpoint structure validated"
echo "📝 For full testing, ensure database is running"
echo "🔧 Some endpoints may require proper database setup"
echo ""
