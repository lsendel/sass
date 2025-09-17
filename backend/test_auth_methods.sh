#!/bin/bash

echo "Testing /api/v1/auth/methods endpoint accessibility..."
echo ""

# Test the endpoint with curl
echo "Making request to http://localhost:8080/api/v1/auth/methods"
echo ""

# Test without authentication
curl -v \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/v1/auth/methods

echo ""
echo ""
echo "Expected response format:"
echo "{"
echo "  \"methods\": [\"PASSWORD\", \"OAUTH2\"] or [\"OAUTH2\"],"
echo "  \"passwordAuthEnabled\": true or false,"
echo "  \"oauth2Providers\": [\"google\", \"github\", \"microsoft\"]"
echo "}"
echo ""
echo "If you get a 302 redirect to login, the SecurityConfig is not allowing public access."
echo "If you get a 200 response with the above format, the endpoint is working correctly."