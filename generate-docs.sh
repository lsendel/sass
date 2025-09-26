#!/bin/bash

# Generate API Documentation for Spring Boot Modulith Payment Platform
# This script generates documentation for both backend (Java) and frontend (TypeScript)

set -e

echo "=========================================="
echo "Generating API Documentation"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the project root directory
PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo -e "${YELLOW}Project root: $PROJECT_ROOT${NC}"

# Generate Backend Documentation
echo -e "\n${GREEN}1. Generating Backend API Documentation...${NC}"
cd "$PROJECT_ROOT/backend"

echo "   Cleaning previous documentation..."
rm -rf ../docs/docs/backend-api-javadoc

echo "   Running Javadoc generation..."
./gradlew javadoc

if [ $? -eq 0 ]; then
    echo -e "   ${GREEN}✓ Backend documentation generated successfully${NC}"
else
    echo -e "   ${RED}✗ Failed to generate backend documentation${NC}"
    exit 1
fi

# Generate Frontend Documentation
echo -e "\n${GREEN}2. Generating Frontend API Documentation...${NC}"
cd "$PROJECT_ROOT/frontend"

echo "   Cleaning previous documentation..."
rm -rf ../docs/docs/frontend-api

echo "   Running TypeDoc generation..."
npm run docs:build

if [ $? -eq 0 ]; then
    echo -e "   ${GREEN}✓ Frontend documentation generated successfully${NC}"
else
    echo -e "   ${RED}✗ Failed to generate frontend documentation${NC}"
    exit 1
fi

# Summary
echo -e "\n${GREEN}=========================================="
echo "Documentation Generation Complete!"
echo "==========================================${NC}"
echo ""
echo "Generated documentation locations:"
echo "  • Backend (Javadoc): docs/docs/backend-api-javadoc/index.html"
echo "  • Frontend (TypeDoc): docs/docs/frontend-api/"
echo ""
echo "To view the documentation:"
echo "  1. Start Docusaurus: cd docs && npm run start"
echo "  2. Navigate to: http://localhost:3000/docs/api-reference"
echo ""
echo "To update documentation after code changes:"
echo "  Run: ./generate-docs.sh"
echo ""