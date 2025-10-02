#!/usr/bin/env bash

# Fix RTK Query Error Handling Issues
# Adds proper type guards for error handling

set -euo pipefail

echo "Fixing RTK Query error handling..."

# Find all files with RTK Query error handling
FILES=$(grep -rl "error\?.status" src/ --include="*.tsx" --include="*.ts" 2>/dev/null || echo "")

if [ -z "$FILES" ]; then
    echo "No files found with error.status access"
    exit 0
fi

# Add import for error handling utility
for file in $FILES; do
    # Check if file already imports error handling
    if ! grep -q "from.*errorHandling" "$file"; then
        # Find the last import line
        LAST_IMPORT=$(grep -n "^import" "$file" | tail -1 | cut -d: -f1)

        if [ -n "$LAST_IMPORT" ]; then
            # Add the import after last import
            sed -i.bak "${LAST_IMPORT}a\\
import { isFetchBaseQueryError, getErrorMessage, getErrorStatus } from '@/lib/api/errorHandling';\\
" "$file"
            rm "${file}.bak" 2>/dev/null || true
            echo "Added error handling import to $file"
        fi
    fi

    # Replace error?.status with getErrorStatus(error)
    sed -i.bak 's/error\?\.status/getErrorStatus(error)/g' "$file"

    # Replace error?.data?.message with getErrorMessage(error)
    sed -i.bak 's/error\?\.data\?\.message/getErrorMessage(error)/g' "$file"

    # Replace error.message with getErrorMessage(error) where not already wrapped
    sed -i.bak 's/\([^(]\)error\.message/\1getErrorMessage(error)/g' "$file"

    rm "${file}.bak" 2>/dev/null || true
    echo "Fixed error handling in $file"
done

echo "RTK Query error handling fixes complete!"
echo "Checking build..."

ERROR_COUNT=$(npm run build 2>&1 | grep -c "error TS" || echo "0")
echo "Remaining errors: $ERROR_COUNT"
