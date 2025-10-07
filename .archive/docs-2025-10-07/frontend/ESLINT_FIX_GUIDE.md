# ESLint Fix Guide

**Current Status**: 890 errors, 5 warnings
**Target**: 0 errors
**Strategy**: Systematic category-by-category fixes

---

## Error Breakdown

| Category                    | Count | Priority | Estimated Time |
| --------------------------- | ----- | -------- | -------------- |
| `no-unsafe-member-access`   | 199   | High     | 2-3 days       |
| `no-explicit-any`           | 136   | High     | 2 days         |
| `no-unsafe-assignment`      | 125   | High     | 1-2 days       |
| `prefer-nullish-coalescing` | 110   | Medium   | 4-6 hours      |
| `no-unsafe-argument`        | 56    | High     | 1 day          |
| `no-unsafe-call`            | 51    | High     | 1 day          |
| `no-unused-vars`            | 47    | Low      | 2-3 hours      |
| `no-floating-promises`      | 33    | High     | 4-6 hours      |
| `no-misused-promises`       | 21    | High     | 2-3 hours      |
| `require-await`             | 11    | Low      | 1 hour         |
| Others                      | ~100  | Mixed    | 1-2 days       |

---

## Phase 1: Quick Wins (4-6 hours)

### 1. Fix `prefer-nullish-coalescing` (110 errors)

**Issue**: Using `||` instead of `??` for default values
**Risk**: Low - mostly straightforward replacements

**Pattern**:

```typescript
// ❌ Before
const value = input || 'default'

// ✅ After
const value = input ?? 'default'
```

**Caution**: Only replace when dealing with `null`/`undefined` checks, not boolean checks

**Files to fix**:

- `src/components/layouts/AccessibleDashboardLayout.tsx`
- `src/components/layouts/DashboardLayout.tsx`
- `src/components/audit/AuditLogFilters.tsx` (already started)
- `src/components/dashboard/DashboardStats.tsx`
- `src/components/error/EnhancedErrorBoundary.tsx`
- And ~10 more files

### 2. Fix `no-unused-vars` (47 errors)

**Issue**: Unused variables, imports, or parameters
**Risk**: Very Low - safe to remove

**Pattern**:

```typescript
// ❌ Before
import { unused, used } from 'module'
const handler = (event, _unused) => {}

// ✅ After
import { used } from 'module'
const handler = (event, _) => {} // Prefix with _ if needed for interface
```

**Action**: Remove or prefix with `_` if required by interface

### 3. Fix `require-await` (11 errors)

**Issue**: Async functions without await
**Risk**: Low - remove async or add await

**Pattern**:

```typescript
// ❌ Before
async function doSomething() {
  return value
}

// ✅ After (Option 1: Remove async)
function doSomething() {
  return value
}

// ✅ After (Option 2: Make properly async)
async function doSomething() {
  const result = await fetchData()
  return result
}
```

---

## Phase 2: Type Safety (5-7 days)

### 1. Fix Type Safety Issues (~500 errors combined)

**Priority Order**:

1. Test files (can be more lenient)
2. Component files (user-facing)
3. Utility files (foundational)

#### A. Fix `no-explicit-any` (136 errors)

**Files with most `any` types**:

- `src/test/utils/testStore.ts` - ✅ Already fixed (0 any types)
- `src/utils/logger.ts` - 5 any types
- `src/store/api/authApi.ts` - 5 any types
- `src/services/websocketService.ts` - 7 any types

**Pattern**:

```typescript
// ❌ Before
function handleData(data: any) {
  return data.value
}

// ✅ After
interface DataType {
  value: string
}
function handleData(data: DataType) {
  return data.value
}

// ✅ Or use unknown for truly unknown types
function handleData(data: unknown) {
  if (typeof data === 'object' && data !== null && 'value' in data) {
    return (data as { value: string }).value
  }
  throw new Error('Invalid data')
}
```

#### B. Fix `no-unsafe-*` errors (431 errors)

These stem from `any` types propagating through code:

```typescript
// ❌ Before (any propagates)
const data: any = await response.json()
const value = data.user.name // unsafe-member-access
handleUser(data.user) // unsafe-argument
data.process() // unsafe-call

// ✅ After (proper typing)
interface ApiResponse {
  user: {
    name: string
    email: string
  }
}
const data: ApiResponse = await response.json()
const value = data.user.name // Safe
handleUser(data.user) // Safe
```

**Strategy**:

1. Define proper interfaces for API responses
2. Type function parameters properly
3. Use type guards for runtime checks

---

## Phase 3: Promise Handling (6-8 hours)

### 1. Fix `no-floating-promises` (33 errors)

**Issue**: Promises not awaited or caught
**Risk**: High - can hide errors

**Pattern**:

```typescript
// ❌ Before
function handler() {
  fetchData() // Floating promise
}

// ✅ After (Option 1: Await)
async function handler() {
  await fetchData()
}

// ✅ After (Option 2: Catch)
function handler() {
  fetchData().catch(err => console.error(err))
}

// ✅ After (Option 3: Void if intentional)
function handler() {
  void fetchData() // Explicitly ignore
}
```

### 2. Fix `no-misused-promises` (21 errors)

**Issue**: Promise-returning functions used in void contexts
**Risk**: Medium - type mismatch

**Pattern**:

```typescript
// ❌ Before
<button onClick={async () => await save()}>Save</button>

// ✅ After
<button onClick={() => { void save(); }}>Save</button>

// Or wrap properly
const handleClick = async () => {
  try {
    await save();
  } catch (err) {
    console.error(err);
  }
};
<button onClick={handleClick}>Save</button>
```

---

## Phase 4: Accessibility & React (3-4 hours)

### 1. Fix JSX A11Y Issues (~50 errors)

**Common issues**:

- Missing keyboard handlers
- Unassociated labels
- Interactive elements without roles

**Pattern**:

```typescript
// ❌ Before
<div onClick={handleClick}>Click me</div>

// ✅ After
<button
  type="button"
  onClick={handleClick}
  onKeyDown={(e) => e.key === 'Enter' && handleClick()}
>
  Click me
</button>

// ❌ Before
<label>Name</label>
<input type="text" />

// ✅ After
<label htmlFor="name-input">Name</label>
<input type="text" id="name-input" />
```

### 2. Fix React Hooks Issues (40 errors)

**Issue**: Missing dependencies in useEffect/useCallback/useMemo

**Pattern**:

```typescript
// ❌ Before
useEffect(() => {
  fetchData(userId)
}, []) // Missing userId

// ✅ After
useEffect(() => {
  fetchData(userId)
}, [userId])

// Or use useCallback
const fetch = useCallback(() => {
  fetchData(userId)
}, [userId])

useEffect(() => {
  fetch()
}, [fetch])
```

---

## Automated Fix Scripts

### Script 1: Fix Nullish Coalescing

```bash
# Use sed to replace || with ?? (review carefully!)
# Run on specific files after backup
find src -name "*.tsx" -o -name "*.ts" | while read file; do
  # Backup
  cp "$file" "$file.bak"
  # Replace (BE CAREFUL - review each change!)
  # sed -i '' 's/ || / ?? /g' "$file"
done
```

### Script 2: Remove Unused Imports

```bash
# Install organize-imports-cli
npm install -g organize-imports-cli

# Run on all TypeScript files
organize-imports-cli src/**/*.{ts,tsx}
```

### Script 3: Fix Async/Await

Requires manual review - no safe automation

---

## Daily Fix Routine

### Monday-Friday (1-2 hours/day)

**Morning** (30 min):

1. Pick one error category
2. Fix 5-10 instances
3. Run tests: `npm run test -- --run`
4. Commit: `git commit -m "fix(lint): address [category] errors"`

**Afternoon** (30 min):

1. Review and test changes
2. Fix any test failures
3. Update progress tracker

**Weekly Goal**: Reduce errors by 150-200

---

## Progress Tracking

Create a tracking file:

```bash
# Track daily progress
echo "$(date): $(npm run lint 2>&1 | grep 'problems' | head -1)" >> eslint-progress.log
```

**Week 1 Target**: 890 → 650 (240 errors fixed)

- Day 1-2: Nullish coalescing (110 errors)
- Day 3: Unused vars (47 errors)
- Day 4-5: Start type safety (~80 errors)

**Week 2 Target**: 650 → 400 (250 errors fixed)

- Continue type safety issues
- Start promise handling

**Week 3 Target**: 400 → 150 (250 errors fixed)

- Finish type safety
- Complete promise handling
- Start accessibility

**Week 4 Target**: 150 → 0 (150 errors fixed)

- Finish accessibility
- React hooks
- Final cleanup

---

## Safety Checklist

Before committing any fixes:

- [ ] Run `npm run test -- --run` - All tests pass
- [ ] Run `npm run lint` - Error count decreased
- [ ] Test affected features manually
- [ ] Review git diff for unintended changes
- [ ] Commit with descriptive message

---

## High-Priority Files (Fix First)

Based on user-facing impact:

1. **Authentication**:
   - `src/components/auth/PasswordLoginForm.tsx` (17 errors)
   - `src/store/api/authApi.ts` (5 any types)

2. **Dashboard**:
   - `src/components/layouts/AccessibleDashboardLayout.tsx` (27 errors)
   - `src/components/dashboard/DashboardStats.tsx` (3 errors)

3. **Projects**:
   - `src/components/project/CreateProjectModal.tsx` (23 errors)
   - `src/components/task/TaskDetailModal.tsx` (13 errors)

4. **Core Utilities**:
   - `src/utils/logger.ts` (5 any types)
   - `src/services/websocketService.ts` (7 any types)

---

## When You're Stuck

1. **ESLint Rule Documentation**: https://typescript-eslint.io/rules/
2. **Ask for Help**: Create GitHub discussion with specific error
3. **Skip for Now**: Add `// eslint-disable-next-line rule-name` with TODO comment
4. **Refactor Instead**: Sometimes redesign is better than patching

---

## Success Metrics

- **Week 1**: 890 → <700 errors (21% reduction)
- **Week 2**: <700 → <450 errors (36% reduction)
- **Week 3**: <450 → <200 errors (56% reduction)
- **Week 4**: <200 → 0 errors (100% complete)

**Daily commits** show steady progress to the team!

---

**Last Updated**: 2025-09-30
**Current Count**: 890 errors
**Target Date**: 2025-10-28 (4 weeks)
