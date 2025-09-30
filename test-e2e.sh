#!/bin/bash

# E2E Test Orchestration Script
# This script ensures the backend is running before executing E2E tests

set -e

echo "Starting E2E Test Suite..."
echo "================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if backend is running
check_backend() {
    echo -e "${YELLOW}Checking if backend is running...${NC}"
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200\|404"; then
        echo -e "${GREEN}Backend is already running${NC}"
        return 0
    else
        return 1
    fi
}

# Function to start backend
start_backend() {
    echo -e "${YELLOW}Starting backend server...${NC}"
    cd backend
    ./gradlew bootRun --args='--spring.profiles.active=test' > ../backend.log 2>&1 &
    BACKEND_PID=$!
    cd ..

    # Wait for backend to be ready (max 30 seconds)
    echo -e "${YELLOW}Waiting for backend to be ready...${NC}"
    for i in {1..30}; do
        if check_backend; then
            echo -e "${GREEN}Backend is ready!${NC}"
            return 0
        fi
        echo -n "."
        sleep 1
    done

    echo -e "${RED}Backend failed to start within 30 seconds${NC}"
    return 1
}

# Function to run frontend E2E tests
run_e2e_tests() {
    echo -e "${YELLOW}Running E2E tests...${NC}"
    cd frontend
    npm run test:e2e
    E2E_EXIT_CODE=$?
    cd ..
    return $E2E_EXIT_CODE
}

# Main execution
BACKEND_STARTED=false

# Check if backend is already running
if ! check_backend; then
    # Start backend if not running
    if start_backend; then
        BACKEND_STARTED=true
    else
        echo -e "${RED}Failed to start backend server${NC}"
        exit 1
    fi
fi

# Run E2E tests
echo "================================"
echo -e "${YELLOW}Executing E2E Tests${NC}"
echo "================================"

run_e2e_tests
TEST_EXIT_CODE=$?

# Cleanup
if [ "$BACKEND_STARTED" = true ] && [ -n "$BACKEND_PID" ]; then
    echo -e "${YELLOW}Stopping backend server (PID: $BACKEND_PID)...${NC}"
    kill $BACKEND_PID 2>/dev/null || true
    wait $BACKEND_PID 2>/dev/null || true
fi

# Report results
echo "================================"
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}E2E Tests Passed Successfully!${NC}"
else
    echo -e "${RED}E2E Tests Failed with exit code: $TEST_EXIT_CODE${NC}"
    echo -e "${YELLOW}Check backend.log for server logs${NC}"
fi
echo "================================"

exit $TEST_EXIT_CODE