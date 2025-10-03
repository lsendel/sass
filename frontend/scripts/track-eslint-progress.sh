#!/bin/bash

# ESLint Progress Tracker
# Tracks error count reduction across categories

set -e

echo ""
echo "========================================="
echo "   ESLint Error Progress Tracker"
echo "========================================="
echo ""

# Run lint and capture output
LINT_OUTPUT=$(npm run lint 2>&1)

# Extract counts
TOTAL=$(echo "$LINT_OUTPUT" | grep -c "error" || echo "0")
WARNINGS=$(echo "$LINT_OUTPUT" | grep -c "warning" || echo "0")

# Category counts
UNSAFE_MEMBER=$(echo "$LINT_OUTPUT" | grep -c "no-unsafe-member-access" || echo "0")
UNSAFE_ASSIGN=$(echo "$LINT_OUTPUT" | grep -c "no-unsafe-assignment" || echo "0")
EXPLICIT_ANY=$(echo "$LINT_OUTPUT" | grep -c "no-explicit-any" || echo "0")
UNSAFE_ARG=$(echo "$LINT_OUTPUT" | grep -c "no-unsafe-argument" || echo "0")
UNSAFE_CALL=$(echo "$LINT_OUTPUT" | grep -c "no-unsafe-call" || echo "0")
UNSAFE_RETURN=$(echo "$LINT_OUTPUT" | grep -c "no-unsafe-return" || echo "0")

NULLISH=$(echo "$LINT_OUTPUT" | grep -c "prefer-nullish-coalescing" || echo "0")
UNUSED_VARS=$(echo "$LINT_OUTPUT" | grep -c "no-unused-vars" || echo "0")

FLOATING=$(echo "$LINT_OUTPUT" | grep -c "no-floating-promises" || echo "0")
MISUSED=$(echo "$LINT_OUTPUT" | grep -c "no-misused-promises" || echo "0")
REQUIRE_AWAIT=$(echo "$LINT_OUTPUT" | grep -c "require-await" || echo "0")

A11Y_LABEL=$(echo "$LINT_OUTPUT" | grep -c "label-has-associated-control" || echo "0")
A11Y_CLICK=$(echo "$LINT_OUTPUT" | grep -c "click-events-have-key-events" || echo "0")
A11Y_STATIC=$(echo "$LINT_OUTPUT" | grep -c "no-static-element-interactions" || echo "0")
A11Y_OTHER=$(echo "$LINT_OUTPUT" | grep -c "jsx-a11y/" || echo "0")

HOOKS_DEPS=$(echo "$LINT_OUTPUT" | grep -c "exhaustive-deps" || echo "0")
HOOKS_RULES=$(echo "$LINT_OUTPUT" | grep -c "rules-of-hooks" || echo "0")

# Calculate totals
UNSAFE_TOTAL=$((UNSAFE_MEMBER + UNSAFE_ASSIGN + UNSAFE_ARG + UNSAFE_CALL + UNSAFE_RETURN))
PROMISE_TOTAL=$((FLOATING + MISUSED + REQUIRE_AWAIT))
A11Y_TOTAL=$((A11Y_LABEL + A11Y_CLICK + A11Y_STATIC))
HOOKS_TOTAL=$((HOOKS_DEPS + HOOKS_RULES))

# Calculate progress
ORIGINAL_TOTAL=906
if [ $TOTAL -eq 0 ]; then
    PROGRESS=100
else
    PROGRESS=$((100 - (TOTAL * 100 / ORIGINAL_TOTAL)))
fi

ERRORS_FIXED=$((ORIGINAL_TOTAL - TOTAL))

# Display results
echo "📊 Overall Status"
echo "-----------------------------------------"
printf "%-25s %5s\n" "Total Errors:" "$TOTAL / $ORIGINAL_TOTAL"
printf "%-25s %5s\n" "Warnings:" "$WARNINGS"
printf "%-25s %5s\n" "Errors Fixed:" "$ERRORS_FIXED"
printf "%-25s %4s%%\n" "Progress:" "$PROGRESS"
echo ""

echo "🔴 Critical Issues (Unsafe Types)"
echo "-----------------------------------------"
printf "%-25s %5s\n" "Unsafe Member Access:" "$UNSAFE_MEMBER"
printf "%-25s %5s\n" "Unsafe Assignment:" "$UNSAFE_ASSIGN"
printf "%-25s %5s\n" "Explicit Any:" "$EXPLICIT_ANY"
printf "%-25s %5s\n" "Unsafe Argument:" "$UNSAFE_ARG"
printf "%-25s %5s\n" "Unsafe Call:" "$UNSAFE_CALL"
printf "%-25s %5s\n" "Unsafe Return:" "$UNSAFE_RETURN"
printf "%-25s %5s\n" "TOTAL UNSAFE:" "$UNSAFE_TOTAL"
echo ""

echo "🟡 Code Quality"
echo "-----------------------------------------"
printf "%-25s %5s\n" "Nullish Coalescing:" "$NULLISH"
printf "%-25s %5s\n" "Unused Variables:" "$UNUSED_VARS"
echo ""

echo "🟠 Promise Handling"
echo "-----------------------------------------"
printf "%-25s %5s\n" "Floating Promises:" "$FLOATING"
printf "%-25s %5s\n" "Misused Promises:" "$MISUSED"
printf "%-25s %5s\n" "Require Await:" "$REQUIRE_AWAIT"
printf "%-25s %5s\n" "TOTAL PROMISES:" "$PROMISE_TOTAL"
echo ""

echo "♿ Accessibility"
echo "-----------------------------------------"
printf "%-25s %5s\n" "Label Controls:" "$A11Y_LABEL"
printf "%-25s %5s\n" "Click Events:" "$A11Y_CLICK"
printf "%-25s %5s\n" "Static Interactions:" "$A11Y_STATIC"
printf "%-25s %5s\n" "TOTAL A11Y:" "$A11Y_TOTAL"
echo ""

echo "⚛️  React Hooks"
echo "-----------------------------------------"
printf "%-25s %5s\n" "Exhaustive Deps:" "$HOOKS_DEPS"
printf "%-25s %5s\n" "Rules of Hooks:" "$HOOKS_RULES"
printf "%-25s %5s\n" "TOTAL HOOKS:" "$HOOKS_TOTAL"
echo ""

echo "========================================="
echo ""

# Progress bar
PROGRESS_BAR_LENGTH=40
FILLED=$((PROGRESS * PROGRESS_BAR_LENGTH / 100))
EMPTY=$((PROGRESS_BAR_LENGTH - FILLED))

printf "Progress: ["
printf "%${FILLED}s" | tr ' ' '█'
printf "%${EMPTY}s" | tr ' ' '░'
printf "] %3s%%\n" "$PROGRESS"

echo ""
echo "========================================="

# Save to log file
LOG_FILE="eslint-progress.log"
TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
echo "$TIMESTAMP | Total: $TOTAL | Unsafe: $UNSAFE_TOTAL | Any: $EXPLICIT_ANY | Progress: $PROGRESS%" >> $LOG_FILE

# Return exit code based on error count
if [ $TOTAL -eq 0 ]; then
    echo "🎉 All ESLint errors resolved!"
    exit 0
else
    echo "💪 Keep going! $TOTAL errors remaining."
    exit 1
fi
