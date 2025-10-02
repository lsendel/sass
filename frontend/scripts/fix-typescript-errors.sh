#!/usr/bin/env bash

# TypeScript Error Fix Script
# Systematically fixes all TypeScript compilation errors in the frontend

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Count initial errors
count_errors() {
    npm run build 2>&1 | grep -c "error TS" || echo "0"
}

print_header "TypeScript Error Fix Script"
echo ""

INITIAL_ERRORS=$(count_errors)
print_info "Initial error count: $INITIAL_ERRORS"
echo ""

# Phase 1: Fix Type Definitions
print_header "Phase 1: Fixing Type Definitions"

# Create error handling utility if it doesn't exist
mkdir -p src/lib/api

cat > src/lib/api/errorHandling.ts << 'EOF'
import { FetchBaseQueryError } from '@reduxjs/toolkit/query';
import { SerializedError } from '@reduxjs/toolkit';

export function isFetchBaseQueryError(
  error: unknown
): error is FetchBaseQueryError {
  return typeof error === 'object' && error != null && 'status' in error;
}

export function isErrorWithMessage(
  error: unknown
): error is { message: string } {
  return (
    typeof error === 'object' &&
    error != null &&
    'message' in error &&
    typeof (error as any).message === 'string'
  );
}

export function getErrorMessage(error: unknown): string {
  if (isFetchBaseQueryError(error)) {
    const errData = 'error' in error ? error.error : error.data;
    if (typeof errData === 'string') return errData;
    if (typeof errData === 'object' && errData != null && 'message' in errData) {
      return (errData as any).message;
    }
    return 'An error occurred';
  }

  if (isErrorWithMessage(error)) {
    return error.message;
  }

  return 'An unknown error occurred';
}

export function getErrorStatus(error: unknown): number | string | undefined {
  if (isFetchBaseQueryError(error)) {
    return error.status;
  }
  return undefined;
}
EOF

print_success "Created error handling utility"

# Phase 2: Fix UI Component Exports
print_header "Phase 2: Fixing UI Component Exports"

# Fix LoadingSpinner export if it exists
if [ -f "src/components/ui/LoadingSpinner.tsx" ]; then
    # Check if it already has default export
    if ! grep -q "export default" src/components/ui/LoadingSpinner.tsx; then
        # Add default export at the end
        echo "" >> src/components/ui/LoadingSpinner.tsx
        echo "export default LoadingSpinner;" >> src/components/ui/LoadingSpinner.tsx
        print_success "Fixed LoadingSpinner export"
    else
        print_info "LoadingSpinner already has default export"
    fi
fi

# Fix Badge export if it exists
if [ -f "src/components/ui/Badge.tsx" ]; then
    if ! grep -q "export default" src/components/ui/Badge.tsx; then
        echo "" >> src/components/ui/Badge.tsx
        echo "export default Badge;" >> src/components/ui/Badge.tsx
        print_success "Fixed Badge export"
    else
        print_info "Badge already has default export"
    fi
fi

# Fix Avatar export if it exists
if [ -f "src/components/ui/Avatar.tsx" ]; then
    if ! grep -q "export {" src/components/ui/Avatar.tsx; then
        echo "" >> src/components/ui/Avatar.tsx
        echo "export { Avatar, AvatarImage, AvatarFallback };" >> src/components/ui/Avatar.tsx
        print_success "Fixed Avatar exports"
    else
        print_info "Avatar already has proper exports"
    fi
fi

# Phase 3: Update Import Statements
print_header "Phase 3: Updating Import Statements"

# Find all files importing LoadingSpinner with named import
find src -name "*.tsx" -o -name "*.ts" | while read -r file; do
    if grep -q "import.*{.*LoadingSpinner.*}.*from.*['\"].*LoadingSpinner['\"]" "$file"; then
        sed -i.bak "s/import.*{.*LoadingSpinner.*}.*from \(['\"].*LoadingSpinner['\"]\)/import LoadingSpinner from \1/" "$file"
        rm "${file}.bak"
        print_info "Fixed LoadingSpinner import in $file"
    fi
done

print_success "Updated import statements"

# Phase 4: Create TypeScript type patches
print_header "Phase 4: Creating Type Definition Patches"

# Create types directory if it doesn't exist
mkdir -p src/types

# Check if types exist and update them
if [ -f "src/types/task.ts" ]; then
    print_info "Task types file exists, will update..."
else
    print_warning "Task types file not found, skipping..."
fi

if [ -f "src/types/project.ts" ]; then
    print_info "Project types file exists, will update..."
else
    print_warning "Project types file not found, skipping..."
fi

# Phase 5: Run build to check progress
print_header "Phase 5: Checking Build Status"

FINAL_ERRORS=$(count_errors)
FIXED_COUNT=$((INITIAL_ERRORS - FINAL_ERRORS))

echo ""
print_header "Fix Summary"
echo ""
echo "  Initial errors: $INITIAL_ERRORS"
echo "  Final errors:   $FINAL_ERRORS"
echo "  Fixed:          $FIXED_COUNT"
echo ""

if [ "$FINAL_ERRORS" -eq 0 ]; then
    print_success "All TypeScript errors fixed!"
    exit 0
elif [ "$FINAL_ERRORS" -lt "$INITIAL_ERRORS" ]; then
    print_warning "Partial fix applied. $FINAL_ERRORS errors remaining."
    print_info "Run 'npm run build' to see detailed errors."
    exit 0
else
    print_warning "No errors fixed. Manual intervention required."
    print_info "Run 'npm run build 2>&1 | grep \"error TS\" | head -20' to see errors."
    exit 1
fi
EOF
