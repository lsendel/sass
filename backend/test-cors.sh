#!/bin/bash

# Test script to verify CORS headers are properly configured
# Run this after starting the Spring Boot backend on localhost:8082

echo "Testing CORS configuration for React frontend (localhost:3000) -> Spring Boot backend (localhost:8082)"
echo "================================================================================"

# Test the /api/v1/auth/methods endpoint with CORS preflight
echo -e "\n1. Testing OPTIONS preflight request to /api/v1/auth/methods:"
curl -X OPTIONS \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v http://localhost:8082/api/v1/auth/methods 2>&1 | grep -i "access-control"

echo -e "\n2. Testing actual GET request to /api/v1/auth/methods:"
curl -X GET \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -v http://localhost:8082/api/v1/auth/methods 2>&1 | grep -i "access-control"

echo -e "\n3. Testing with browser-like request:"
curl -X GET \
  -H "Origin: http://localhost:3000" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)" \
  -v http://localhost:8082/api/v1/auth/methods 2>&1 | grep -i "access-control"

echo -e "\n================================================================================"
echo "Expected headers in response:"
echo "- Access-Control-Allow-Origin: http://localhost:3000"
echo "- Access-Control-Allow-Credentials: true"
echo "- Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH"
echo "- Access-Control-Allow-Headers: *"
echo "================================================================================"