# Frontend Troubleshooting & Debugging Guide

## Table of Contents

1. [Common Issues & Solutions](#common-issues--solutions)
2. [Debugging Strategies](#debugging-strategies)
3. [Performance Troubleshooting](#performance-troubleshooting)
4. [State Management Issues](#state-management-issues)
5. [API Integration Problems](#api-integration-problems)
6. [Build & Deployment Issues](#build--deployment-issues)
7. [Testing Failures](#testing-failures)
8. [Browser Compatibility](#browser-compatibility)
9. [Development Tools](#development-tools)
10. [Emergency Response](#emergency-response)

## Common Issues & Solutions

### 1. Authentication Problems

#### Issue: User stuck in login loop

```typescript
// Problem: Token not persisting
// Check: localStorage/sessionStorage
console.log("Access Token:", localStorage.getItem("access_token"));
console.log("Refresh Token:", localStorage.getItem("refresh_token"));

// Solution: Verify token storage
const authSlice = createSlice({
  extraReducers: (builder) => {
    builder.addMatcher(
      authApi.endpoints.login.matchFulfilled,
      (state, { payload }) => {
        // Ensure tokens are stored
        localStorage.setItem("access_token", payload.access_token);
        localStorage.setItem("refresh_token", payload.refresh_token);
      },
    );
  },
});
```

#### Issue: 401 Unauthorized after refresh

```typescript
// Debug: Check token expiry
const decodeToken = (token: string) => {
  const base64Url = token.split(".")[1];
  const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(window.atob(base64));
};

const token = localStorage.getItem("access_token");
if (token) {
  const decoded = decodeToken(token);
  console.log("Token expires:", new Date(decoded.exp * 1000));
  console.log("Current time:", new Date());
}
```

### 2. Redux State Issues

#### Issue: State not updating

```typescript
// Debug: Enable Redux DevTools
const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        // Ignore these action types
        ignoredActions: ["persist/PERSIST"],
      },
      immutableCheck: {
        // Add paths to ignore
        ignoredPaths: ["api"],
      },
    }),
  devTools: process.env.NODE_ENV !== "production",
});

// Check state updates
store.subscribe(() => {
  console.log("State changed:", store.getState());
});
```

#### Issue: RTK Query cache not invalidating

```typescript
// Debug cache behavior
const api = createApi({
  endpoints: (builder) => ({
    getUsers: builder.query({
      query: () => "/users",
      // Add cache logging
      onCacheEntryAdded: async (
        arg,
        { cacheDataLoaded, cacheEntryRemoved },
      ) => {
        console.log("Cache entry added for:", arg);
        await cacheDataLoaded;
        console.log("Cache data loaded");
        await cacheEntryRemoved;
        console.log("Cache entry removed");
      },
    }),
  }),
});
```

### 3. Component Rendering Issues

#### Issue: Infinite re-renders

```typescript
// Problem: Dependencies causing loops
const ComponentWithIssue = () => {
  const [data, setData] = useState([]);

  // BAD: Creates new object every render
  useEffect(() => {
    setData([]);
  }, [{}]); // This object is new every render!

  // GOOD: Stable dependency
  const stableObject = useMemo(() => ({}), []);
  useEffect(() => {
    setData([]);
  }, [stableObject]);
};

// Debug: Use React DevTools Profiler
// Enable "Record why each component rendered"
```

#### Issue: Components not updating

```typescript
// Debug: Check memo and dependencies
const MemoizedComponent = React.memo(
  ({ data }) => {
    console.log('Rendering with data:', data);
    return <div>{data}</div>;
  },
  (prevProps, nextProps) => {
    // Log comparison
    console.log('Props comparison:', {
      prev: prevProps,
      next: nextProps,
      equal: prevProps.data === nextProps.data,
    });
    return prevProps.data === nextProps.data;
  }
);
```

## Debugging Strategies

### 1. Redux DevTools Setup

```typescript
// Enhanced debugging configuration
const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(
      // Custom logging middleware
      (store) => (next) => (action) => {
        console.group(action.type);
        console.info("dispatching", action);
        const result = next(action);
        console.log("next state", store.getState());
        console.groupEnd();
        return result;
      },
    ),
  enhancers: [
    // Add Redux DevTools with trace
    composeWithDevTools({
      trace: true,
      traceLimit: 25,
    })(),
  ],
});
```

### 2. Network Debugging

```typescript
// Intercept all API calls
const debugApi = createApi({
  baseQuery: async (...args) => {
    console.log("API Request:", args);
    const result = await fetchBaseQuery({
      baseUrl: "/api",
      prepareHeaders: (headers) => {
        console.log("Request headers:", headers);
        return headers;
      },
    })(...args);
    console.log("API Response:", result);
    return result;
  },
});

// Network error handling
window.addEventListener("online", () => {
  console.log("Network: Online");
});

window.addEventListener("offline", () => {
  console.log("Network: Offline");
});
```

### 3. Performance Profiling

```typescript
// React DevTools Profiler API
const ProfiledComponent = () => {
  return (
    <Profiler
      id="ComponentName"
      onRender={(id, phase, actualDuration) => {
        console.log(`${id} (${phase}) took ${actualDuration}ms`);
      }}
    >
      <YourComponent />
    </Profiler>
  );
};

// Custom performance marks
performance.mark('myComponent-start');
// ... component logic
performance.mark('myComponent-end');
performance.measure(
  'myComponent',
  'myComponent-start',
  'myComponent-end'
);

const measure = performance.getEntriesByName('myComponent')[0];
console.log(`Component took ${measure.duration}ms`);
```

## Performance Troubleshooting

### 1. Bundle Size Issues

```bash
# Analyze bundle size
npm run build -- --stats
npx webpack-bundle-analyzer build/bundle-stats.json

# Find large dependencies
npm ls --depth=0 | grep -E "[0-9]+\.[0-9]+MB"

# Check for duplicate packages
npm dedupe
npm ls --depth=0 | sort | uniq -d
```

### 2. Memory Leaks

```typescript
// Detect memory leaks in components
useEffect(() => {
  const intervalId = setInterval(() => {
    console.log("Memory usage:", performance.memory);
  }, 1000);

  // Clean up to prevent leaks
  return () => clearInterval(intervalId);
}, []);

// Monitor component unmounting
useEffect(() => {
  return () => {
    console.log("Component unmounting - check for cleanup");
  };
}, []);
```

### 3. Slow Initial Load

```typescript
// Measure and optimize
const measureLoadTime = () => {
  const perfData = performance.getEntriesByType("navigation")[0];
  console.log("Load times:", {
    dns: perfData.domainLookupEnd - perfData.domainLookupStart,
    tcp: perfData.connectEnd - perfData.connectStart,
    request: perfData.responseStart - perfData.requestStart,
    response: perfData.responseEnd - perfData.responseStart,
    dom: perfData.domComplete - perfData.domInteractive,
    total: perfData.loadEventEnd - perfData.fetchStart,
  });
};

// Lazy load heavy components
const HeavyComponent = lazy(
  () => import(/* webpackChunkName: "heavy" */ "./HeavyComponent"),
);
```

## State Management Issues

### 1. Redux Persist Problems

```typescript
// Debug persist/rehydrate
const persistConfig = {
  key: "root",
  storage,
  debug: true, // Enable debug mode
  stateReconciler: (inboundState, originalState) => {
    console.log("Reconciling state:", { inboundState, originalState });
    return autoMergeLevel2(inboundState, originalState);
  },
};

// Monitor persist events
persistStore(store, null, () => {
  console.log("Rehydration complete");
});
```

### 2. Selector Performance

```typescript
// Debug selector recomputations
const selectExpensiveData = createSelector(
  [selectItems, selectFilter],
  (items, filter) => {
    console.count("Selector recomputation");
    return items.filter((item) => item.includes(filter));
  },
);

// Measure selector performance
const timedSelector = (selector) => (state) => {
  const start = performance.now();
  const result = selector(state);
  console.log(`Selector took ${performance.now() - start}ms`);
  return result;
};
```

## API Integration Problems

### 1. CORS Issues

```typescript
// Debug CORS
fetch("/api/endpoint", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
  },
  mode: "cors", // Explicitly set CORS mode
})
  .then((response) => {
    console.log("Response headers:", response.headers);
    return response.json();
  })
  .catch((error) => {
    if (error.name === "TypeError" && error.message.includes("CORS")) {
      console.error("CORS issue detected:", error);
      // Check: Network tab for preflight OPTIONS request
      // Check: Response headers for Access-Control-Allow-Origin
    }
  });
```

### 2. Request/Response Issues

```typescript
// Enhanced error handling
const baseQueryWithRetry = retry(
  async (args, api, extraOptions) => {
    const result = await fetchBaseQuery({
      baseUrl: "/api",
      prepareHeaders: (headers, { getState }) => {
        const token = (getState() as RootState).auth.token;
        if (token) {
          headers.set("Authorization", `Bearer ${token}`);
        }
        console.log("Request headers:", Object.fromEntries(headers));
        return headers;
      },
    })(args, api, extraOptions);

    if (result.error) {
      console.error("API Error:", {
        status: result.error.status,
        data: result.error.data,
        error: result.error.error,
      });
    }

    return result;
  },
  {
    maxRetries: 3,
    backoff: (attempt) => {
      console.log(`Retry attempt ${attempt}`);
      return Math.min(1000 * 2 ** attempt, 30000);
    },
  },
);
```

## Build & Deployment Issues

### 1. Build Failures

```bash
# Clear cache and rebuild
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
npm run build

# Debug build process
npm run build -- --verbose

# Check for type errors
npx tsc --noEmit

# Check for lint errors
npm run lint -- --debug
```

### 2. Environment Variables

```typescript
// Debug environment variables
console.log("Environment:", {
  NODE_ENV: process.env.NODE_ENV,
  REACT_APP_API_URL: process.env.REACT_APP_API_URL,
  // List all REACT_APP_ variables
  ...Object.keys(process.env)
    .filter((key) => key.startsWith("REACT_APP_"))
    .reduce(
      (acc, key) => ({
        ...acc,
        [key]: process.env[key],
      }),
      {},
    ),
});

// Validate required variables
const requiredEnvVars = ["REACT_APP_API_URL", "REACT_APP_STRIPE_KEY"];

requiredEnvVars.forEach((varName) => {
  if (!process.env[varName]) {
    console.error(`Missing required environment variable: ${varName}`);
  }
});
```

## Testing Failures

### 1. Test Debugging

```typescript
// Enhanced test debugging
describe('Component', () => {
  beforeEach(() => {
    // Log test environment
    console.log('Test starting:', expect.getState().currentTestName);
  });

  afterEach(() => {
    // Check for console errors
    if (console.error.mock?.calls.length > 0) {
      console.log('Console errors:', console.error.mock.calls);
    }
  });

  test('should work', () => {
    const { debug } = render(<Component />);

    // Print DOM for debugging
    debug();

    // Use screen.debug for specific elements
    screen.debug(screen.getByRole('button'));
  });
});
```

### 2. Async Test Issues

```typescript
// Debug async operations
test('async operation', async () => {
  const { rerender } = render(<Component />);

  // Log all state changes
  await waitFor(
    () => {
      console.log('Waiting for condition...');
      expect(screen.getByText('Loaded')).toBeInTheDocument();
    },
    {
      timeout: 5000,
      onTimeout: () => {
        console.error('Timeout reached, current DOM:');
        screen.debug();
      },
    }
  );
});
```

## Browser Compatibility

### 1. Feature Detection

```typescript
// Check for required features
const checkBrowserSupport = () => {
  const required = {
    localStorage: typeof Storage !== "undefined",
    fetch: typeof fetch !== "undefined",
    promises: typeof Promise !== "undefined",
    intersectionObserver: typeof IntersectionObserver !== "undefined",
  };

  const unsupported = Object.entries(required)
    .filter(([, supported]) => !supported)
    .map(([feature]) => feature);

  if (unsupported.length > 0) {
    console.error("Unsupported features:", unsupported);
  }

  return unsupported.length === 0;
};
```

### 2. Polyfill Loading

```typescript
// Conditional polyfill loading
const loadPolyfills = async () => {
  const polyfills = [];

  if (!window.IntersectionObserver) {
    polyfills.push(import("intersection-observer"));
  }

  if (!window.ResizeObserver) {
    polyfills.push(import("resize-observer-polyfill"));
  }

  await Promise.all(polyfills);
  console.log("Polyfills loaded:", polyfills.length);
};
```

## Development Tools

### 1. Chrome DevTools Setup

```javascript
// Custom formatters for debugging
window.devtoolsFormatters = [
  {
    header: (obj) => {
      if (obj.__redux_state__) {
        return ["div", {}, `Redux State: ${Object.keys(obj).length} keys`];
      }
      return null;
    },
    hasBody: () => true,
    body: (obj) => {
      return ["div", {}, JSON.stringify(obj, null, 2)];
    },
  },
];
```

### 2. VS Code Debugging

```json
// .vscode/launch.json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "chrome",
      "request": "launch",
      "name": "Debug React App",
      "url": "http://localhost:3000",
      "webRoot": "${workspaceFolder}/frontend/src",
      "sourceMaps": true,
      "sourceMapPathOverrides": {
        "webpack:///src/*": "${webRoot}/*"
      },
      "preLaunchTask": "npm: start"
    }
  ]
}
```

### 3. Error Boundary Setup

```typescript
class ErrorBoundary extends Component {
  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);

    // Send to monitoring service
    if (window.Sentry) {
      window.Sentry.captureException(error, {
        contexts: {
          react: {
            componentStack: errorInfo.componentStack,
          },
        },
      });
    }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div>
          <h2>Something went wrong</h2>
          <details>
            <summary>Error details</summary>
            <pre>{this.state.error?.toString()}</pre>
            <pre>{this.state.errorInfo?.componentStack}</pre>
          </details>
        </div>
      );
    }

    return this.props.children;
  }
}
```

## Emergency Response

### 1. Production Hotfixes

```bash
# Quick rollback
git checkout main
git pull origin main
git log --oneline -10  # Find last working commit
git checkout <commit-hash>
npm run build
npm run deploy:emergency

# Feature flag disable
curl -X POST https://api.yourdomain.com/admin/features \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"feature": "new-payment-flow", "enabled": false}'
```

### 2. Performance Emergency

```typescript
// Emergency performance mode
const enableEmergencyMode = () => {
  // Disable non-critical features
  localStorage.setItem("emergency_mode", "true");

  // Reduce API polling
  store.dispatch(
    api.util.updateQueryData("getUpdates", undefined, (draft) => {
      draft.pollingInterval = 60000; // Increase to 1 minute
    }),
  );

  // Disable animations
  document.body.classList.add("reduce-motion");

  // Clear caches
  if ("caches" in window) {
    caches.keys().then((names) => {
      names.forEach((name) => caches.delete(name));
    });
  }
};
```

### 3. Data Recovery

```typescript
// Recover from localStorage corruption
const recoverLocalStorage = () => {
  const backup = {};

  try {
    // Attempt to read all keys
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key) {
        try {
          backup[key] = localStorage.getItem(key);
        } catch (e) {
          console.error(`Failed to read ${key}:`, e);
        }
      }
    }

    // Clear and restore
    localStorage.clear();
    Object.entries(backup).forEach(([key, value]) => {
      if (value) {
        localStorage.setItem(key, value);
      }
    });

    console.log("LocalStorage recovered:", Object.keys(backup));
  } catch (error) {
    console.error("Recovery failed:", error);
    // Fall back to session storage or memory
  }
};
```

## Common Console Commands

```javascript
// Quick debugging helpers
window.debug = {
  // Get current Redux state
  getState: () => store.getState(),

  // Dispatch action
  dispatch: (action) => store.dispatch(action),

  // Clear all caches
  clearCache: () => {
    localStorage.clear();
    sessionStorage.clear();
    store.dispatch(api.util.resetApiState());
  },

  // Export state for debugging
  exportState: () => {
    const state = store.getState();
    const blob = new Blob([JSON.stringify(state, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `state-${Date.now()}.json`;
    a.click();
  },

  // Import state for testing
  importState: (state) => {
    store.dispatch({ type: "IMPORT_STATE", payload: state });
  },

  // Performance report
  perfReport: () => {
    const entries = performance.getEntriesByType("measure");
    console.table(
      entries.map((e) => ({
        name: e.name,
        duration: `${e.duration.toFixed(2)}ms`,
      })),
    );
  },
};

// Usage in console:
// debug.getState()
// debug.clearCache()
// debug.perfReport()
```

## Monitoring Checklist

### Pre-Deployment

- [ ] Run full test suite
- [ ] Check bundle size
- [ ] Verify environment variables
- [ ] Test error boundaries
- [ ] Validate API endpoints
- [ ] Check browser compatibility

### Post-Deployment

- [ ] Monitor error rates
- [ ] Check performance metrics
- [ ] Verify API response times
- [ ] Test critical user flows
- [ ] Check memory usage
- [ ] Monitor network failures

### Weekly Health Check

- [ ] Review error logs
- [ ] Analyze performance trends
- [ ] Check for memory leaks
- [ ] Review API usage patterns
- [ ] Update dependencies
- [ ] Clean up old feature flags

## Support Resources

### Documentation

- [React DevTools](https://react.dev/learn/react-developer-tools)
- [Redux DevTools](https://github.com/reduxjs/redux-devtools)
- [Chrome DevTools](https://developer.chrome.com/docs/devtools/)
- [TypeScript Debugging](https://www.typescriptlang.org/docs/handbook/debugging.html)

### Monitoring Tools

- Sentry: Error tracking
- LogRocket: Session replay
- DataDog: APM and logs
- New Relic: Performance monitoring
- Grafana: Custom dashboards

### Team Contacts

- On-call Engineer: Check PagerDuty
- DevOps Team: #devops-support
- Security Team: security@company.com
- Platform Team: #platform-help
