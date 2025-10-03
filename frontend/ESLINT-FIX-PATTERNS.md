# ESLint Fix Patterns - Quick Reference Guide

## Pattern Categories

### 1. Unsafe Type Operations

#### Pattern 1.1: Replace `any` in MSW Handlers

**Problem:** `@typescript-eslint/no-unsafe-member-access`, `@typescript-eslint/no-explicit-any`

```typescript
// ❌ BEFORE
import { http, HttpResponse } from 'msw';

export const handlers = [
  http.post('/api/organizations', async ({ request }) => {
    const data = await request.json() as any;
    return HttpResponse.json({
      id: 'org-1',
      name: data.name,
      slug: data.slug,
    });
  }),
];
```

```typescript
// ✅ AFTER
import { http, HttpResponse } from 'msw';
import type { CreateOrganizationRequest, OrganizationResponse } from '@/types/api';

// Type guard for runtime validation
function isCreateOrganizationRequest(data: unknown): data is CreateOrganizationRequest {
  return (
    typeof data === 'object' &&
    data !== null &&
    'name' in data &&
    typeof (data as { name: unknown }).name === 'string' &&
    'slug' in data &&
    typeof (data as { slug: unknown }).slug === 'string'
  );
}

export const handlers = [
  http.post<never, CreateOrganizationRequest, OrganizationResponse>(
    '/api/organizations',
    async ({ request }) => {
      const data = await request.json();

      if (!isCreateOrganizationRequest(data)) {
        return HttpResponse.json(
          { error: 'Invalid request body' },
          { status: 400 }
        );
      }

      return HttpResponse.json({
        id: 'org-1',
        name: data.name,
        slug: data.slug,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      });
    }
  ),
];
```

#### Pattern 1.2: Type-Safe Event Handlers

**Problem:** `@typescript-eslint/no-unsafe-member-access` in WebSocket/MessageEvent

```typescript
// ❌ BEFORE
function handleMessage(event: any) {
  const data = event.data;
  console.log(data.type, data.payload);
}
```

```typescript
// ✅ AFTER
interface WebSocketMessage<T = unknown> {
  type: string;
  payload: T;
  timestamp: number;
  correlationId?: string;
}

interface UserJoinedPayload {
  userId: string;
  userName: string;
  avatar?: string;
}

interface MessagePayload {
  messageId: string;
  content: string;
  senderId: string;
}

type KnownMessageTypes =
  | { type: 'user_joined'; payload: UserJoinedPayload }
  | { type: 'message'; payload: MessagePayload }
  | { type: 'user_left'; payload: { userId: string } };

function isKnownMessage(msg: WebSocketMessage): msg is KnownMessageTypes {
  return ['user_joined', 'message', 'user_left'].includes(msg.type);
}

function handleMessage(event: MessageEvent<WebSocketMessage>) {
  const message = event.data;

  if (!isKnownMessage(message)) {
    console.warn('Unknown message type:', message.type);
    return;
  }

  switch (message.type) {
    case 'user_joined':
      console.log(`${message.payload.userName} joined`);
      break;
    case 'message':
      console.log(`Message from ${message.payload.senderId}: ${message.payload.content}`);
      break;
    case 'user_left':
      console.log(`User ${message.payload.userId} left`);
      break;
  }
}
```

#### Pattern 1.3: Replace `any` in Error Handling

**Problem:** `@typescript-eslint/no-unsafe-assignment`, `@typescript-eslint/no-explicit-any`

```typescript
// ❌ BEFORE
try {
  await fetchData();
} catch (error: any) {
  console.error(error.message);
  toast.error(error.response?.data?.message || 'An error occurred');
}
```

```typescript
// ✅ AFTER
import { isAxiosError } from 'axios';

interface ApiErrorResponse {
  message: string;
  code?: string;
  details?: Record<string, unknown>;
}

function isApiErrorResponse(data: unknown): data is ApiErrorResponse {
  return (
    typeof data === 'object' &&
    data !== null &&
    'message' in data &&
    typeof (data as { message: unknown }).message === 'string'
  );
}

try {
  await fetchData();
} catch (error) {
  // Type-safe error handling
  if (isAxiosError(error)) {
    const errorData = error.response?.data;
    if (isApiErrorResponse(errorData)) {
      console.error(`API Error [${errorData.code ?? 'UNKNOWN'}]:`, errorData.message);
      toast.error(errorData.message);
    } else {
      console.error('API Error:', error.message);
      toast.error('An unexpected error occurred');
    }
  } else if (error instanceof Error) {
    console.error('Error:', error.message);
    toast.error(error.message);
  } else {
    console.error('Unknown error:', error);
    toast.error('An unexpected error occurred');
  }
}
```

#### Pattern 1.4: Type-Safe Redux State Access

**Problem:** `@typescript-eslint/no-unsafe-member-access` in dynamic reducer access

```typescript
// ❌ BEFORE
const result = await store.dispatch(
  organizationApi.endpoints.getOrganization.initiate('org-1')
);
expect((result as any).data.name).toBe('Test Org');
```

```typescript
// ✅ AFTER
import type { SerializedError } from '@reduxjs/toolkit';
import type { FetchBaseQueryError } from '@reduxjs/toolkit/query';

type QueryResult<T> =
  | { data: T; error?: never }
  | { data?: never; error: FetchBaseQueryError | SerializedError };

const result = await store.dispatch(
  organizationApi.endpoints.getOrganization.initiate('org-1')
);

// Type guard for successful result
if ('data' in result && result.data) {
  expect(result.data.name).toBe('Test Org');
} else if ('error' in result) {
  throw new Error(`Failed to fetch organization: ${JSON.stringify(result.error)}`);
}
```

### 2. Nullish Coalescing

#### Pattern 2.1: Replace `||` with `??`

**Problem:** `@typescript-eslint/prefer-nullish-coalescing`

```typescript
// ❌ BEFORE - Fails for '', 0, false
const name = user.name || 'Anonymous'; // '' becomes 'Anonymous'
const count = user.count || 0; // 0 becomes 0 (redundant)
const isActive = user.isActive || false; // false becomes false (redundant)

// ✅ AFTER - Only null/undefined trigger default
const name = user.name ?? 'Anonymous'; // '' stays ''
const count = user.count ?? 0; // 0 stays 0
const isActive = user.isActive ?? false; // false stays false

// ✅ WHEN || IS CORRECT (falsy coalescing)
const displayName = user.name.trim() || 'Anonymous'; // Empty string should be replaced
```

#### Pattern 2.2: Optional Chaining with Nullish Coalescing

```typescript
// ❌ BEFORE
const value = data && data.nested && data.nested.value || 'default';

// ✅ AFTER
const value = data?.nested?.value ?? 'default';
```

### 3. Promise Handling

#### Pattern 3.1: Floating Promises in Event Handlers

**Problem:** `@typescript-eslint/no-floating-promises`

```typescript
// ❌ BEFORE
<button onClick={() => handleSubmit()}>Submit</button>

// ✅ AFTER - Option 1: Void operator (fire and forget)
<button onClick={() => void handleSubmit()}>Submit</button>

// ✅ AFTER - Option 2: Error handling wrapper
const handleClick = () => {
  handleSubmit().catch((error) => {
    console.error('Submit failed:', error);
    toast.error('Failed to submit form');
  });
};
<button onClick={handleClick}>Submit</button>

// ✅ AFTER - Option 3: Async wrapper (recommended)
const handleClick = async () => {
  try {
    await handleSubmit();
    toast.success('Submitted successfully');
  } catch (error) {
    console.error('Submit failed:', error);
    toast.error('Failed to submit form');
  }
};
<button onClick={() => void handleClick()}>Submit</button>
```

#### Pattern 3.2: Misused Promises in React Props

**Problem:** `@typescript-eslint/no-misused-promises`

```typescript
// ❌ BEFORE - onClick expects void, gets Promise<void>
<button onClick={async () => await save()}>Save</button>

// ❌ BEFORE - useEffect cleanup expects void, gets Promise<void>
useEffect(() => {
  return async () => await cleanup();
}, []);

// ✅ AFTER - onClick
const handleSave = async () => {
  try {
    await save();
  } catch (error) {
    handleError(error);
  }
};
<button onClick={() => void handleSave()}>Save</button>

// ✅ AFTER - useEffect cleanup
useEffect(() => {
  return () => {
    void cleanup();
  };
}, []);
```

#### Pattern 3.3: Remove Unnecessary Async

**Problem:** `@typescript-eslint/require-await`

```typescript
// ❌ BEFORE
const getData = async () => {
  return computeValue(); // No await, no need for async
};

// ✅ AFTER
const getData = () => {
  return computeValue();
};

// ✅ WHEN ASYNC IS NEEDED
const getData = async () => {
  const value = await fetchValue();
  return processValue(value);
};
```

### 4. Accessibility

#### Pattern 4.1: Form Labels

**Problem:** `jsx-a11y/label-has-associated-control`

```typescript
// ❌ BEFORE
<div>
  <label>Email</label>
  <input type="email" />
</div>

// ✅ AFTER - Option 1: htmlFor + id
<div>
  <label htmlFor="email">Email</label>
  <input type="email" id="email" />
</div>

// ✅ AFTER - Option 2: Nested input
<label>
  Email
  <input type="email" />
</label>
```

#### Pattern 4.2: Interactive Elements

**Problem:** `jsx-a11y/click-events-have-key-events`, `jsx-a11y/no-static-element-interactions`

```typescript
// ❌ BEFORE
<div onClick={handleClick}>Click me</div>

// ✅ AFTER - Option 1: Use semantic button
<button onClick={handleClick}>Click me</button>

// ✅ AFTER - Option 2: Full keyboard support
<div
  role="button"
  tabIndex={0}
  onClick={handleClick}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick();
    }
  }}
>
  Click me
</div>
```

#### Pattern 4.3: Remove Autofocus

**Problem:** `jsx-a11y/no-autofocus`

```typescript
// ❌ BEFORE
<input autoFocus />

// ✅ AFTER - Use ref with useEffect for controlled focus
const inputRef = useRef<HTMLInputElement>(null);

useEffect(() => {
  // Only focus if user hasn't interacted yet
  if (inputRef.current && !document.activeElement) {
    inputRef.current.focus();
  }
}, []);

<input ref={inputRef} />
```

### 5. React Hooks

#### Pattern 5.1: Conditional Hook Calls

**Problem:** `react-hooks/rules-of-hooks`

```typescript
// ❌ BEFORE
const id = isNew ? null : useId();

// ✅ AFTER
const generatedId = useId();
const id = isNew ? null : generatedId;
```

#### Pattern 5.2: Exhaustive Dependencies

**Problem:** `react-hooks/exhaustive-deps`

```typescript
// ❌ BEFORE
const [data, setData] = useState([]);

useEffect(() => {
  fetchData(userId).then(setData);
}, []); // Missing userId

// ✅ AFTER - Option 1: Add all dependencies
useEffect(() => {
  fetchData(userId).then(setData);
}, [userId]);

// ✅ AFTER - Option 2: Use callback to stabilize function reference
const fetchData = useCallback(async (id: string) => {
  const result = await api.fetchUser(id);
  setData(result);
}, []);

useEffect(() => {
  void fetchData(userId);
}, [userId, fetchData]);

// ✅ AFTER - Option 3: Move fetch inside effect
useEffect(() => {
  const fetchAndSetData = async () => {
    const result = await api.fetchUser(userId);
    setData(result);
  };
  void fetchAndSetData();
}, [userId]);
```

#### Pattern 5.3: Stable Callback References

```typescript
// ❌ BEFORE
const Component = () => {
  const handleClick = () => {
    console.log('clicked');
  };

  return <Child onClick={handleClick} />; // New function every render
};

// ✅ AFTER
const Component = () => {
  const handleClick = useCallback(() => {
    console.log('clicked');
  }, []); // Stable reference

  return <Child onClick={handleClick} />;
};
```

### 6. Unused Variables

#### Pattern 6.1: Unused Catch Variables

**Problem:** `@typescript-eslint/no-unused-vars`

```typescript
// ❌ BEFORE
try {
  doSomething();
} catch (error) {
  // Not using error
  showGenericError();
}

// ✅ AFTER
try {
  doSomething();
} catch (_error) {
  // Underscore prefix indicates intentionally unused
  showGenericError();
}
```

#### Pattern 6.2: Unused Function Parameters

```typescript
// ❌ BEFORE
function handleEvent(event: Event, data: unknown) {
  // Only using event
  console.log(event.type);
}

// ✅ AFTER
function handleEvent(event: Event, _data: unknown) {
  console.log(event.type);
}
```

### 7. Miscellaneous

#### Pattern 7.1: Unbound Methods

**Problem:** `@typescript-eslint/unbound-method`

```typescript
// ❌ BEFORE
<button onClick={this.handleClick}>Click</button>

// ✅ AFTER - Option 1: Arrow function
<button onClick={(e) => this.handleClick(e)}>Click</button>

// ✅ AFTER - Option 2: Bind in constructor
constructor(props) {
  super(props);
  this.handleClick = this.handleClick.bind(this);
}

// ✅ AFTER - Option 3: Class property arrow function
class MyComponent {
  handleClick = (e: React.MouseEvent) => {
    // this is automatically bound
  };
}
```

#### Pattern 7.2: Template Expression Restrictions

**Problem:** `@typescript-eslint/restrict-template-expressions`

```typescript
// ❌ BEFORE
const message = `User: ${user}`; // Object stringification

// ✅ AFTER
const message = `User: ${user.name}`;

// ✅ WITH TYPE GUARD
function stringifyUser(user: unknown): string {
  if (typeof user === 'string') return user;
  if (typeof user === 'object' && user !== null && 'name' in user) {
    return String((user as { name: unknown }).name);
  }
  return 'Unknown User';
}
const message = `User: ${stringifyUser(user)}`;
```

#### Pattern 7.3: Base to String

**Problem:** `@typescript-eslint/no-base-to-string`

```typescript
// ❌ BEFORE
const id = String(user); // '[object Object]'

// ✅ AFTER - Custom toString
class User {
  constructor(public name: string, public email: string) {}

  toString(): string {
    return `${this.name} (${this.email})`;
  }
}

// ✅ AFTER - Explicit property
const id = user.id.toString();
```

## Testing Patterns After Fixes

### Pattern: Test Type Safety

```typescript
// Verify type inference works
it('should infer correct types', () => {
  const result = await store.dispatch(
    api.endpoints.getUser.initiate('user-1')
  );

  if ('data' in result) {
    // TypeScript should know result.data is User
    expectTypeOf(result.data).toEqualTypeOf<User>();
    expect(result.data.name).toBe('Test User');
  }
});
```

### Pattern: Test Type Guards

```typescript
describe('Type Guards', () => {
  it('should validate correct data', () => {
    const validData = { name: 'Test', slug: 'test' };
    expect(isCreateOrganizationRequest(validData)).toBe(true);
  });

  it('should reject invalid data', () => {
    const invalidData = { name: 'Test' }; // Missing slug
    expect(isCreateOrganizationRequest(invalidData)).toBe(false);
  });
});
```

## Quick Fix Commands

```bash
# Fix nullish coalescing automatically
npm run lint -- --fix --rule '@typescript-eslint/prefer-nullish-coalescing'

# Fix unused variables automatically
npm run lint -- --fix --rule '@typescript-eslint/no-unused-vars'

# Check specific file
npm run lint -- path/to/file.ts

# Count remaining errors by type
npm run lint 2>&1 | grep -oE '@typescript-eslint/[a-z-]+' | sort | uniq -c | sort -rn
```

## Verification Checklist

After fixing each file:
- [ ] ESLint errors reduced or eliminated
- [ ] TypeScript compilation succeeds: `npm run typecheck`
- [ ] Tests pass: `npm run test -- path/to/test.ts`
- [ ] No runtime errors introduced
- [ ] Type inference works as expected
- [ ] Code still readable and maintainable

## Common Pitfalls

### Pitfall 1: Over-using `unknown`
```typescript
// ❌ LAZY
function process(data: unknown) {
  // Forces type guards everywhere
}

// ✅ BETTER
function process(data: ProcessableData) {
  // Type is known, no guards needed
}
```

### Pitfall 2: Nullish vs Falsy Coalescing
```typescript
// ❌ WRONG - Empty string needs to be replaced
const name = userName ?? 'Anonymous'; // '' stays ''

// ✅ CORRECT - Use || for falsy values
const name = userName.trim() || 'Anonymous';
```

### Pitfall 3: Async Without Await
```typescript
// ❌ WRONG - async without await adds unnecessary Promise
const getData = async () => {
  return data;
};

// ✅ CORRECT - Remove async if no await
const getData = () => {
  return data;
};
```

---

**Last Updated:** 2025-10-02
**Use this guide alongside:** ESLINT-FIX-PLAN.md
