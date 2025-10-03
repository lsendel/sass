# ESLint Fix - Immediate Action Plan

## START HERE - Day 1 Quick Wins

### Step 1: Auto-Fix What We Can (5 minutes)

Run ESLint auto-fix to eliminate easy errors:

```bash
cd /Users/lsendel/IdeaProjects/sass/frontend

# Fix auto-fixable errors (5 errors)
npm run lint -- --fix

# Verify what was fixed
git diff

# Count remaining errors
npm run lint 2>&1 | tail -5
```

**Expected result:** ~901 errors remaining

### Step 2: Fix Unused Variables (30 minutes)

These are the easiest manual fixes. 37 errors total.

**Files to fix:**
1. `tests/e2e/utils/global-setup.ts` - 1 error
2. `tests/e2e/utils/global-teardown.ts` - 2 errors
3. `viewport-test.js` - 2 errors
4. Various component files - 32 errors

**Command to find all:**
```bash
npm run lint 2>&1 | grep "no-unused-vars" | awk -F':' '{print $1}' | sort -u
```

**Fix pattern:**
```typescript
// Before
try {
  doSomething();
} catch (error) {  // ❌ unused
  showError();
}

// After
try {
  doSomething();
} catch (_error) {  // ✅ prefixed with _
  showError();
}
```

**Verification:**
```bash
npm run lint 2>&1 | grep -c "no-unused-vars"
# Should show 0
```

### Step 3: Create Type Definitions File (15 minutes)

Create a central location for type guards and utilities:

```bash
touch src/types/typeGuards.ts
```

Add this content:

```typescript
// src/types/typeGuards.ts

/**
 * Type guards for runtime type checking
 */

import type {
  CreateOrganizationRequest,
  CreateProjectRequest,
  CreateTaskRequest,
  User,
  Organization,
  Project,
} from './api';

/**
 * Check if value is a non-null object
 */
export function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

/**
 * Check if value has a specific property
 */
export function hasProperty<K extends string>(
  obj: unknown,
  key: K
): obj is Record<K, unknown> {
  return isObject(obj) && key in obj;
}

/**
 * Type guard for CreateOrganizationRequest
 */
export function isCreateOrganizationRequest(
  data: unknown
): data is CreateOrganizationRequest {
  return (
    isObject(data) &&
    hasProperty(data, 'name') &&
    typeof data.name === 'string' &&
    hasProperty(data, 'slug') &&
    typeof data.slug === 'string'
  );
}

/**
 * Type guard for CreateProjectRequest
 */
export function isCreateProjectRequest(
  data: unknown
): data is CreateProjectRequest {
  return (
    isObject(data) &&
    hasProperty(data, 'name') &&
    typeof data.name === 'string' &&
    hasProperty(data, 'slug') &&
    typeof data.slug === 'string' &&
    hasProperty(data, 'workspaceId') &&
    typeof data.workspaceId === 'string'
  );
}

/**
 * Type guard for CreateTaskRequest
 */
export function isCreateTaskRequest(
  data: unknown
): data is CreateTaskRequest {
  return (
    isObject(data) &&
    hasProperty(data, 'title') &&
    typeof data.title === 'string' &&
    hasProperty(data, 'projectId') &&
    typeof data.projectId === 'string'
  );
}

/**
 * Type guard for error objects
 */
export interface ApiErrorResponse {
  message: string;
  code?: string;
  details?: Record<string, unknown>;
  status?: number;
}

export function isApiErrorResponse(data: unknown): data is ApiErrorResponse {
  return (
    isObject(data) &&
    hasProperty(data, 'message') &&
    typeof data.message === 'string'
  );
}

/**
 * Type guard for Axios errors
 */
import { isAxiosError as axiosIsAxiosError } from 'axios';
export { axiosIsAxiosError as isAxiosError };

/**
 * Safely get error message from unknown error
 */
export function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  if (isObject(error) && hasProperty(error, 'message')) {
    return String(error.message);
  }
  return 'An unknown error occurred';
}

/**
 * WebSocket message types
 */
export interface WebSocketMessage<T = unknown> {
  type: string;
  payload: T;
  timestamp: number;
  correlationId?: string;
}

export function isWebSocketMessage(data: unknown): data is WebSocketMessage {
  return (
    isObject(data) &&
    hasProperty(data, 'type') &&
    typeof data.type === 'string' &&
    hasProperty(data, 'payload') &&
    hasProperty(data, 'timestamp') &&
    typeof data.timestamp === 'number'
  );
}
```

### Step 4: Fix First High-Impact File (2 hours)

Target: `src/test/api/organizationApi.test.ts` (102 errors → 0 errors)

**Current issues:**
- 102 errors (11% of all errors)
- Heavy use of `any` types
- Unsafe type operations

**Action plan:**

1. **Import type guards:**
```typescript
import {
  isCreateOrganizationRequest,
  isApiErrorResponse,
  getErrorMessage,
} from '@/types/typeGuards';
```

2. **Fix MSW handlers:**

```typescript
// BEFORE (unsafe)
const handler = http.post('/api/organizations', async ({ request }) => {
  const data = await request.json() as any;
  return HttpResponse.json({ ...data, id: 'org-1' });
});

// AFTER (type-safe)
const handler = http.post<never, CreateOrganizationRequest>(
  '/api/organizations',
  async ({ request }) => {
    const data = await request.json();

    if (!isCreateOrganizationRequest(data)) {
      return HttpResponse.json(
        { message: 'Invalid request body', code: 'VALIDATION_ERROR' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      id: `org-${Date.now()}`,
      name: data.name,
      slug: data.slug,
      description: data.description ?? null,
      settings: data.settings ?? {},
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
  }
);
```

3. **Fix test assertions:**

```typescript
// BEFORE (unsafe)
const result = await store.dispatch(
  organizationApi.endpoints.create.initiate(testData)
);
expect((result as any).data.name).toBe('Test Org');

// AFTER (type-safe)
const result = await store.dispatch(
  organizationApi.endpoints.create.initiate(testData)
);

if ('data' in result && result.data) {
  expect(result.data.name).toBe('Test Org');
  expect(result.data.slug).toBe('test-org');
} else {
  throw new Error(`Expected data, got error: ${JSON.stringify(result.error)}`);
}
```

4. **Fix error handling:**

```typescript
// BEFORE (unsafe)
catch (error: any) {
  expect(error.message).toBe('Failed');
}

// AFTER (type-safe)
catch (error) {
  expect(getErrorMessage(error)).toBe('Failed');
}
```

**Verification:**
```bash
npm run lint -- src/test/api/organizationApi.test.ts
# Should show 0 errors

npm run test -- src/test/api/organizationApi.test.ts
# Should pass
```

### Step 5: Apply Same Pattern to Other Test Files (4 hours)

Now that you have the pattern, fix the remaining test files:

1. `src/test/api/projectManagementApi.test.ts` (51 errors)
2. `src/test/api/userApi.test.ts` (24 errors)
3. `src/test/api/subscriptionApi.test.ts` (24 errors)
4. `src/test/api/auditApi.test.ts` (14 errors)
5. `src/test/setup.ts` (13 errors)

**Total:** 126 errors → 0 errors

**Progress tracking:**
```bash
# After each file
npm run lint 2>&1 | tail -1
git add . && git commit -m "fix: resolve ESLint errors in [filename]"
```

**Expected Day 1 Results:**
- **Starting:** 906 errors
- **After auto-fix:** 901 errors
- **After unused vars:** 864 errors
- **After test files:** 738 errors (18.5% reduction)
- **Time spent:** 6-7 hours

---

## Day 2-3: Service Workers and Real-Time (104 errors)

### Target Files:
1. `src/services/serviceWorker.ts` (26 errors)
2. `src/hooks/useRealTimeCollaboration.ts` (38 errors)
3. `tests/e2e/auth-flow-debug.spec.ts` (50 errors)
4. `src/services/websocketService.ts` (14 errors)
5. `tests/e2e/project-management.spec.ts` (34 errors)

### Pattern to Apply:

**1. Define message types:**
```typescript
// src/types/websocket.ts
export interface BaseWebSocketMessage<T = unknown> {
  type: string;
  payload: T;
  timestamp: number;
}

export type WebSocketEvent =
  | { type: 'user_joined'; payload: UserJoinedPayload }
  | { type: 'user_left'; payload: UserLeftPayload }
  | { type: 'message'; payload: MessagePayload }
  | { type: 'task_updated'; payload: TaskUpdatedPayload };
```

**2. Update service worker:**
```typescript
// src/services/serviceWorker.ts
self.addEventListener('message', (event: ExtendableMessageEvent) => {
  const message = event.data as WebSocketMessage;

  if (isWebSocketMessage(message)) {
    handleWebSocketMessage(message);
  }
});
```

**Expected Day 2-3 Results:**
- **Starting:** 738 errors
- **After service workers:** 634 errors (30% total reduction)
- **Time spent:** 12-16 hours cumulative

---

## Day 4-5: Components and Pages (55 errors)

### Target Files:
1. `src/pages/settings/SettingsPage.tsx` (21 errors)
2. `src/components/organizations/CreateOrganizationModal.tsx` (14 errors)
3. `src/components/project/CreateProjectModal.tsx` (13 errors)

### Common Patterns to Fix:

**1. Promise handling in forms:**
```typescript
// BEFORE
<form onSubmit={handleSubmit}>

// AFTER
<form onSubmit={(e) => {
  e.preventDefault();
  void handleSubmit();
}}>
```

**2. Accessibility:**
```typescript
// BEFORE
<label>Name</label>
<input type="text" />

// AFTER
<label htmlFor="name">Name</label>
<input type="text" id="name" />
```

**Expected Day 4-5 Results:**
- **Starting:** 634 errors
- **After components:** 579 errors (36% total reduction)
- **Time spent:** 20-24 hours cumulative

---

## Progress Tracking Script

Create this file: `scripts/track-eslint-progress.sh`

```bash
#!/bin/bash

# Track ESLint progress
TOTAL=$(npm run lint 2>&1 | grep -c "error")
UNSAFE=$(npm run lint 2>&1 | grep -c "no-unsafe")
EXPLICIT_ANY=$(npm run lint 2>&1 | grep -c "no-explicit-any")
PROMISES=$(npm run lint 2>&1 | grep -E "no-floating-promises|no-misused-promises" | wc -l)
A11Y=$(npm run lint 2>&1 | grep -c "jsx-a11y")

echo "========================================="
echo "ESLint Error Progress Tracker"
echo "========================================="
echo "Total Errors:          $TOTAL"
echo "Unsafe Operations:     $UNSAFE"
echo "Explicit Any:          $EXPLICIT_ANY"
echo "Promise Issues:        $PROMISES"
echo "Accessibility:         $A11Y"
echo "========================================="
echo "Progress: $((100 - (TOTAL * 100 / 906)))% complete"
echo "========================================="
```

Make it executable:
```bash
chmod +x scripts/track-eslint-progress.sh
```

Run it after each file:
```bash
./scripts/track-eslint-progress.sh
```

---

## Commit Strategy

### Commit Template:

```
fix(eslint): resolve [error-type] errors in [file/directory]

- Fix [specific issue 1]
- Fix [specific issue 2]
- Add type guards for [type]

Errors: [before] → [after]
Impact: [X]% reduction
```

### Example Commits:

```bash
git commit -m "fix(eslint): resolve no-unused-vars errors in test utilities

- Prefix unused catch variables with underscore
- Remove unused imports in global-setup.ts
- Remove unused event parameters

Errors: 906 → 869
Impact: 4% reduction"
```

```bash
git commit -m "fix(eslint): resolve unsafe type errors in organizationApi.test.ts

- Add type guards for CreateOrganizationRequest
- Replace 'any' types with proper MSW type parameters
- Add type-safe error handling with isAxiosError

Errors: 869 → 767
Impact: 15% reduction"
```

---

## Emergency Rollback

If something breaks:

```bash
# See what changed
git diff HEAD~1

# Rollback last commit
git reset --soft HEAD~1

# Or hard reset (loses changes)
git reset --hard HEAD~1

# Run tests to verify
npm run test
npm run test:e2e
```

---

## Daily Checklist

At the end of each day:

- [ ] All tests passing: `npm run test`
- [ ] TypeScript compiles: `npm run typecheck`
- [ ] App builds: `npm run build`
- [ ] Error count reduced from previous day
- [ ] Changes committed with descriptive message
- [ ] Progress documented in tracking script
- [ ] No new runtime errors introduced

---

## When You Get Stuck

### Pattern Not Working?
1. Check ESLINT-FIX-PATTERNS.md for examples
2. Search codebase for similar fixes: `git log --grep="eslint"`
3. Check ESLint rule docs: `https://typescript-eslint.io/rules/[rule-name]`

### Tests Failing?
1. Run just the failing test: `npm run test -- path/to/test.ts`
2. Check if types changed: `npm run typecheck`
3. Verify test setup still works: `npm run test -- src/test/setup.ts`

### Too Many Errors?
1. Focus on one file at a time
2. Run `./scripts/track-eslint-progress.sh` to see progress
3. Take breaks - this is a marathon, not a sprint

---

## Next Steps After Day 5

Once you've completed the immediate actions (Days 1-5), you should have:

- **Reduced errors:** 906 → ~580 (36% reduction)
- **Fixed categories:**
  - ✅ All unused variables
  - ✅ All test file unsafe types
  - ✅ Service worker types
  - ✅ Component promise handling

**Then continue with:**
- Day 6-10: Nullish coalescing (85 errors)
- Day 11-15: Remaining unsafe operations
- Day 16-20: Accessibility and React hooks
- Day 21-22: Final cleanup and verification

See ESLINT-FIX-PLAN.md for the full roadmap.

---

**Start Time:** [Record when you start]
**Target Completion:** Day 5 (579 errors remaining)
**Success Metric:** 36% error reduction, all tests passing
