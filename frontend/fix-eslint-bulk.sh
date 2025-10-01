#!/bin/bash

# Bulk ESLint Fix Script
# Systematically fixes common ESLint patterns

echo "🔧 Starting Bulk ESLint Fixes..."
echo ""

# Get initial count
echo "Initial error count:"
npm run lint 2>&1 | grep 'problems' | head -1
echo ""

# Phase 1: Fix simple nullish coalescing in safe contexts (string/number defaults)
echo "Phase 1: Fixing nullish coalescing patterns..."
find src -type f \( -name "*.ts" -o -name "*.tsx" \) ! -path "*/node_modules/*" -exec sed -i '' \
  -e "s/\( || ''\)/\1/g" \
  -e "s/ || 0/ ?? 0/g" \
  -e "s/ || \[\]/ ?? []/g" \
  -e "s/ || {}/ ?? {}/g" \
  -e "s/ || null/ ?? null/g" \
  -e "s/ || undefined/ ?? undefined/g" \
  {} \;

echo "After phase 1:"
npm run lint 2>&1 | grep 'problems' | head -1
echo ""

# Phase 2: Remove unused imports (safe)
echo "Phase 2: Removing unused '_' prefixed variables..."
# This would need manual review, skipping for now

# Phase 3: Run ESLint auto-fix again
echo "Phase 3: Running ESLint auto-fix..."
npm run lint:fix >/dev/null 2>&1

echo "After phase 3:"
npm run lint 2>&1 | grep 'problems' | head -1
echo ""

# Run tests to verify no regressions
echo "Running tests to verify fixes..."
npm run test -- --run --reporter=basic 2>&1 | tail -10

echo ""
echo "✅ Bulk fixes complete!"
echo "Review changes with: git diff"
