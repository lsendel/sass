# API Test URL Resolution Issue

**Date:** 2025-09-30
**Status:** 🔴 BLOCKED
**Priority:** Critical
**Impact:** 150+ API tests cannot execute

---

## Problem Statement

API tests using RTK Query + MSW (Mock Service Worker) in Node.js test environment are failing due to URL resolution issues. RTK Query's `fetchBaseQuery` with relative URLs cannot be intercepted by MSW in Node mode.

### Error Message

```
TypeError: Failed to parse URL from /api/v1/auth/methods
TypeError: Invalid URL: /api/v1/auth/methods
```

---

## Root Cause Analysis

### Technical Details

1. **RTK Query Configuration**
   - API slices use relative URLs: `/api/v1/auth/methods`
   - `fetchBaseQuery` configured with `baseUrl: '/api/v1'`
   - Works in browser (has `window.location.origin`)
   - Fails in Node.js test environment (no origin context)

2. **MSW Node Mode Behavior**
   - MSW in Node.js (`msw/node`) intercepts requests at Node's `http` module level
   - Requires absolute URLs for request matching
   - Cannot resolve relative URLs without a base

3. **Test Environment**
   - Vitest with `jsdom` environment
   - Tests run in Node.js, not a real browser
   - `window.location` mock doesn't affect `fetchBaseQuery` URL resolution

### Why Mocking window.location Doesn't Work

```typescript
// In setup.ts
Object.defineProperty(window, 'location', {
  value: { origin: 'http://localhost:3000', ... }
});

// This doesn't help because:
// 1. fetchBaseQuery uses Node's fetch/Request, not browser APIs
// 2. Node's URL constructor doesn't check window.location
// 3. RTK Query resolves URLs before MSW sees them
```

---

## Investigation Timeline

### Steps Taken

1. **✅ Fixed Middleware Error**
   - **Problem:** Auth slice missing from test stores
   - **Solution:** Created `testStore.ts` utility with auth reducer
   - **Result:** Middleware error resolved, but URL issue persisted

2. **✅ Added window.location Mock**
   - **Problem:** Thought missing origin was the issue
   - **Solution:** Mocked `window.location` in `setup.ts`
   - **Result:** Mock added successfully, but didn't affect URL resolution

3. **✅ Verified MSW Handlers**
   - **Problem:** Suspected MSW configuration issues
   - **Solution:** Confirmed handlers use correct URL patterns
   - **Result:** Handlers are correct, issue is in URL resolution before MSW

4. **❌ Current State**
   - Tests are properly structured
   - Store configuration is correct
   - MSW handlers are correct
   - **Blocked:** RTK Query can't resolve relative URLs in Node test environment

---

## Possible Solutions

### Option A: MSW Browser Mode (⭐ Recommended)

**Approach:** Use Vitest browser mode with MSW browser API

**Pros:**

- Most realistic test environment (actual browser)
- MSW works natively with relative URLs in browser
- Tests match production behavior
- Future-proof solution

**Cons:**

- Requires browser test infrastructure
- Slightly slower than Node tests
- More complex setup

**Implementation:**

```typescript
// vitest.config.ts
export default defineConfig({
  test: {
    browser: {
      enabled: true,
      name: 'chromium',
      provider: 'playwright',
    },
  },
})

// Update MSW to browser mode
import { setupWorker } from 'msw/browser'
const worker = setupWorker(...handlers)
await worker.start()
```

**Estimated Effort:** 2-3 hours
**Risk:** Low
**Maintenance:** Low

---

### Option B: Test-Specific API Slices with Absolute URLs

**Approach:** Create duplicate API slices for testing with absolute URLs

**Pros:**

- Quick to implement
- Works with current MSW Node setup
- No test infrastructure changes

**Cons:**

- Code duplication
- Maintain two sets of API configurations
- Divergence between test and production code
- Not following DRY principle

**Implementation:**

```typescript
// src/test/api/testAuthApi.ts
export const testAuthApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({
    baseUrl: 'http://localhost:3000/api/v1/auth', // Absolute URL
  }),
  endpoints: builder => ({
    // Copy all endpoints from authApi
  }),
})
```

**Estimated Effort:** 3-4 hours
**Risk:** Medium (code drift)
**Maintenance:** High (double maintenance)

---

### Option C: Mock fetchBaseQuery

**Approach:** Replace RTK Query's fetchBaseQuery with custom mock

**Pros:**

- Fine-grained control
- Can work with MSW Node mode

**Cons:**

- Complex implementation
- Breaks RTK Query abstraction
- Hard to maintain
- May miss edge cases

**Implementation:**

```typescript
// Mock fetchBaseQuery to prepend absolute URL
vi.mock('@reduxjs/toolkit/query/react', async () => {
  const actual = await vi.importActual('@reduxjs/toolkit/query/react')
  return {
    ...actual,
    fetchBaseQuery: (config: any) => {
      return async (args: any, api: any, extraOptions: any) => {
        // Prepend absolute URL
        const url = typeof args === 'string' ? args : args.url
        const absoluteUrl = `http://localhost:3000${url}`
        // ...
      }
    },
  }
})
```

**Estimated Effort:** 4-6 hours
**Risk:** High (may break RTK Query features)
**Maintenance:** Very High

---

### Option D: Use Actual Backend API in Tests

**Approach:** Spin up real backend server for integration tests

**Pros:**

- Most realistic testing
- No mocking complexity
- Tests real API responses

**Cons:**

- Much slower (API startup + test execution)
- Requires TestContainers or Docker
- More complex CI/CD setup
- Harder to test error scenarios

**Implementation:**

```typescript
// beforeAll hook
beforeAll(async () => {
  await startBackendServer()
  await waitForHealthCheck()
})

afterAll(async () => {
  await stopBackendServer()
})
```

**Estimated Effort:** 1-2 days
**Risk:** Medium (infrastructure complexity)
**Maintenance:** Medium

---

## Recommendation

### ⭐ **Option A: MSW Browser Mode**

**Rationale:**

1. **Most Realistic:** Tests run in actual browser, matching production
2. **Future-Proof:** Aligns with modern testing best practices
3. **Clean Architecture:** No code duplication or mocking hacks
4. **Maintainable:** Single source of truth for API configuration
5. **MSW Native:** MSW works perfectly with relative URLs in browser

**Migration Path:**

1. Install `@vitest/browser` and Playwright
2. Update `vitest.config.ts` to enable browser mode
3. Convert MSW from Node mode (`msw/node`) to browser mode (`msw/browser`)
4. Update test setup to use browser-based MSW worker
5. Run tests and verify all 150+ tests pass

**Alternative Approach (If Browser Mode Not Feasible):**
Use Option B (Test-Specific API Slices) as a temporary workaround while planning migration to browser testing.

---

## Files Affected

### Test Files (Ready, Blocked by URL Issue)

```
frontend/src/test/api/authApi.test.ts         # 60+ tests, 8 endpoints
frontend/src/test/api/auditApi.test.ts        # 50+ tests, 5 endpoints
frontend/src/test/api/subscriptionApi.test.ts # 40+ tests, 10 endpoints
```

### Utility Files (Complete)

```
frontend/src/test/utils/testStore.ts          # Test store utilities with auth
frontend/src/test/setup.ts                    # Test environment setup
```

### Configuration Files (May Need Updates)

```
frontend/vitest.config.ts                     # Test configuration
frontend/tsconfig.json                        # TypeScript config
```

---

## Test Statistics

### Created but Not Passing

- **Total Tests:** 150+
- **Total Endpoints:** 21+
- **Coverage:**
  - Auth API: 8 endpoints (login, register, OAuth, session, logout, refresh)
  - Audit API: 5 endpoints (logs, details, export, status, download)
  - Subscription API: 10 endpoints (plans, CRUD, billing, invoices)

### Test Quality

- ✅ Comprehensive error scenarios (401, 404, 429, 500, network)
- ✅ Success paths with proper assertions
- ✅ Edge cases (empty results, malformed data)
- ✅ State management (cache invalidation)
- ✅ MSW handlers properly structured

---

## Next Actions

### Immediate (Decision Required)

1. **Choose Solution:** Decide between Options A-D
2. **Get Approval:** Confirm approach with team
3. **Allocate Time:** Schedule implementation (2-3 hours to 1-2 days depending on option)

### If Choosing Option A (Browser Mode)

1. Install dependencies: `npm install -D @vitest/browser playwright`
2. Update `vitest.config.ts` with browser configuration
3. Convert MSW to browser mode
4. Run tests and verify
5. Document setup for other developers

### If Choosing Option B (Temporary Fix)

1. Create `src/test/api/testApiSlices.ts` with absolute URLs
2. Update test files to import test-specific slices
3. Add TODOs to migrate to Option A later
4. Run tests and verify
5. Schedule Option A migration

---

## References

### Related Issues

- [RTK Query Testing Docs](https://redux-toolkit.js.org/rtk-query/usage/testing)
- [MSW Node vs Browser](https://mswjs.io/docs/integrations/node)
- [Vitest Browser Mode](https://vitest.dev/guide/browser.html)

### Community Discussions

- RTK Query + MSW URL resolution is a common issue in Node test environments
- Most projects solve this by using browser mode or absolute URLs
- MSW recommends browser mode for realistic testing

---

**Last Updated:** 2025-09-30
**Owner:** Frontend Test Infrastructure
**Decision Deadline:** TBD
