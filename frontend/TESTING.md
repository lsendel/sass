# Comprehensive Testing Infrastructure

## Overview

This document describes the comprehensive testing infrastructure implemented for the Spring Boot Modulith Payment Platform frontend. The testing strategy covers unit tests, integration tests, API validation tests, and end-to-end tests with comprehensive coverage reporting.

## Testing Stack

### Core Testing Technologies
- **Vitest**: Fast unit and integration testing framework
- **React Testing Library**: Component testing utilities
- **Playwright**: Cross-browser end-to-end testing
- **MSW (Mock Service Worker)**: API mocking for tests
- **@testing-library/user-event**: User interaction testing

### Test Types
1. **Unit Tests**: Component and utility function testing
2. **Integration Tests**: Complete user flow testing
3. **API Validation Tests**: Runtime type validation testing
4. **E2E Tests**: Full application workflow testing

## Test Structure

```
frontend/
├── src/
│   ├── **/*.test.{ts,tsx}        # Unit tests (co-located)
│   ├── test/
│   │   ├── setup.ts              # Test configuration
│   │   ├── mocks/
│   │   │   └── handlers.ts       # MSW API handlers
│   │   ├── utils/
│   │   │   └── test-utils.tsx    # Custom render utilities
│   │   └── integration/          # Integration tests
│   │       ├── auth-flow.test.tsx
│   │       ├── payment-flow.test.tsx
│   │       └── api-validation.test.ts
├── tests/e2e/                    # Playwright E2E tests
│   ├── auth-flows.spec.ts
│   └── utils/
│       ├── global-setup.ts
│       └── global-teardown.ts
├── scripts/
│   └── test-coverage.js          # Comprehensive coverage analysis
├── vitest.config.ts              # Vitest configuration
└── playwright.config.ts          # Playwright configuration
```

## Test Configuration

### Vitest Configuration
- **Environment**: jsdom for React testing
- **Coverage Provider**: v8 for modern JavaScript coverage
- **Setup Files**: Automatic test environment setup
- **Path Aliases**: @/ alias for clean imports

### Playwright Configuration
- **Browsers**: Chromium, Firefox, WebKit, Mobile Chrome, Mobile Safari
- **Reporters**: HTML, JSON, JUnit, Allure
- **Global Setup/Teardown**: Test data management
- **Screenshots/Videos**: Failure evidence collection

## Testing Utilities

### Custom Render Function
```typescript
import { customRender, authenticatedState } from '@/test/utils/test-utils'

// Render with authenticated user
const { user } = customRender(<Component />, {
  preloadedState: authenticatedState,
  withRouter: true,
})
```

### Test State Management
```typescript
// Pre-configured states
- defaultMockState    # Basic user state
- authenticatedState  # Logged-in user
- unauthenticatedState # Guest user
- adminUserState      # Admin user
- loadingState        # Loading state
- errorState          # Error state
```

### Mock API Responses
```typescript
// MSW handlers provide realistic API responses
server.use(
  http.post('/api/auth/login', () => {
    return HttpResponse.json({
      success: true,
      data: { user: mockUser, token: 'mock-token' },
      timestamp: new Date().toISOString(),
    })
  })
)
```

## Test Commands

### Unit Tests
```bash
npm run test                    # Run all tests in watch mode
npm run test:unit              # Run unit tests once
npm run test:watch             # Run tests in watch mode
npm run test:ui                # Run tests with UI
npm run test:coverage:unit     # Unit tests with coverage
```

### Integration Tests
```bash
npm run test:integration       # Run integration tests
npm run test:validation        # Run API validation tests
npm run test:coverage:integration # Integration tests with coverage
```

### E2E Tests
```bash
npm run test:e2e              # Run all E2E tests
npm run test:e2e:headed       # Run E2E tests with browser UI
npm run test:e2e:ui           # Run E2E tests with Playwright UI
npm run test:e2e:debug        # Debug E2E tests
npm run test:e2e:auth         # Run authentication E2E tests
npm run test:e2e:payments     # Run payment E2E tests
npm run test:e2e:report       # Show E2E test report
```

### Comprehensive Testing
```bash
npm run test:all              # Run all test types
npm run test:ci               # Full CI test suite
npm run test:coverage:comprehensive # Complete coverage analysis
```

## Test Coverage

### Coverage Thresholds
- **Statements**: 80%
- **Branches**: 75%
- **Functions**: 80%
- **Lines**: 80%

### Coverage Reports
- **HTML Report**: `coverage/index.html`
- **JSON Summary**: `coverage/coverage-summary.json`
- **LCOV Report**: `coverage/lcov.info`
- **Comprehensive Report**: `test-results/TEST_COVERAGE_REPORT.md`

## Unit Testing

### Component Testing Example
```typescript
import { describe, it, expect } from 'vitest'
import { customRender, authenticatedState } from '@/test/utils/test-utils'
import { LoginPage } from '@/pages/auth/LoginPage'

describe('LoginPage', () => {
  it('should render login form', () => {
    const { user } = customRender(<LoginPage />, {
      preloadedState: authenticatedState,
    })

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
  })

  it('should handle form submission', async () => {
    const { user } = customRender(<LoginPage />)

    await testUtils.fillForm(user, {
      email: 'test@example.com',
      password: 'password123',
    })

    await testUtils.submitForm(user, 'Sign In')

    await expect(screen.getByText(/signing in/i)).toBeInTheDocument()
  })
})
```

### Utility Testing Example
```typescript
import { describe, it, expect } from 'vitest'
import { validateApiResponse, ApiValidationError } from '@/lib/api/validation'
import { UserSchema } from '@/types/api'

describe('API Validation', () => {
  it('should validate correct user data', () => {
    const validUser = {
      id: 'user-123',
      email: 'test@example.com',
      role: 'USER',
      emailVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    }

    expect(() =>
      validateApiResponse(UserSchema, validUser, 'test')
    ).not.toThrow()
  })
})
```

## Integration Testing

### Authentication Flow Testing
```typescript
describe('Authentication Flow Integration', () => {
  it('should complete successful login flow', async () => {
    const { user } = customRender(<LoginPage />, {
      preloadedState: unauthenticatedState,
      withRouter: true,
    })

    await testUtils.fillForm(user, {
      email: 'test@example.com',
      password: 'password123',
    })

    await testUtils.submitForm(user, 'Sign In')

    await waitFor(() => {
      expect(screen.queryByText(/signing in/i)).not.toBeInTheDocument()
    })
  })
})
```

### Payment Flow Testing
```typescript
describe('Payment Flow Integration', () => {
  it('should create payment intent successfully', async () => {
    const { user } = customRender(<PaymentPage />, {
      preloadedState: authenticatedState,
    })

    await testUtils.fillForm(user, {
      'Payment Amount': '29.99',
      'Payment Description': 'Test payment',
    })

    await testUtils.submitForm(user, 'Create Payment Intent')

    await waitFor(() => {
      expect(screen.queryByText(/creating/i)).not.toBeInTheDocument()
    })
  })
})
```

## E2E Testing

### Authentication E2E Tests
```typescript
test('should complete successful login flow', async ({ page }) => {
  await page.goto('/')
  await page.click('[data-testid="login-button"]')

  await page.fill('[data-testid="email-input"]', 'test@example.com')
  await page.fill('[data-testid="password-input"]', 'SecurePassword123!')

  await page.click('[data-testid="submit-button"]')

  await expect(page).toHaveURL(/.*dashboard/)
  await expect(page.locator('[data-testid="user-menu"]')).toBeVisible()
})
```

### Payment E2E Tests
```typescript
test('should process payment successfully', async ({ page }) => {
  // Login first
  await loginAsUser(page, 'test@example.com')

  await page.goto('/payments')
  await page.fill('[data-testid="amount-input"]', '50.00')

  await page.click('[data-testid="pay-button"]')

  await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
})
```

## API Validation Testing

### Schema Validation Tests
```typescript
describe('API Response Validation', () => {
  it('should validate user response schema', () => {
    const mockResponse = {
      success: true,
      data: generateRealisticMockData(UserSchema),
      timestamp: new Date().toISOString(),
    }

    const schema = ApiSuccessResponseSchema(UserSchema)
    const result = validateApiResponse(schema, mockResponse)

    expect(result.success).toBe(true)
    expect(result.data.email).toContain('@')
  })
})
```

### Error Handling Tests
```typescript
describe('Error Handling', () => {
  it('should process validation errors correctly', () => {
    const validationError = new ApiValidationError('Test error', zodError, data)
    const errorInfo = processApiError(validationError)

    expect(errorInfo.code).toBe('VALIDATION_ERROR')
    expect(errorInfo.severity).toBe('high')
    expect(errorInfo.isRetryable).toBe(true)
  })
})
```

## Mock Service Worker (MSW)

### Handler Configuration
```typescript
// Realistic API mocking
export const handlers = [
  http.post('/api/auth/login', async ({ request }) => {
    const credentials = await request.json()

    if (credentials.email === 'invalid@example.com') {
      return HttpResponse.json({
        success: false,
        error: { code: 'AUTH_001', message: 'Invalid credentials' },
      }, { status: 401 })
    }

    return HttpResponse.json({
      success: true,
      data: { user: mockUser, token: 'mock-token' },
    })
  }),
]
```

### Test Scenarios
- **Success responses** with realistic data
- **Error responses** with proper error codes
- **Network delays** for loading state testing
- **Rate limiting** scenarios
- **Validation errors** for form testing

## Performance Testing

### Bundle Analysis Integration
```typescript
// Performance monitoring in tests
const { measureRenderTime } = testUtils

const renderTime = await measureRenderTime(() => {
  render(<LargeComponent />)
})

expect(renderTime).toBeLessThan(100) // 100ms threshold
```

### Memory Leak Detection
```typescript
// Component cleanup testing
afterEach(() => {
  cleanup()
  // Check for memory leaks
  expect(document.body.innerHTML).toBe('')
})
```

## Accessibility Testing

### A11y Integration
```typescript
describe('Accessibility', () => {
  it('should be accessible', async () => {
    const { container } = render(<Component />)

    // Check ARIA attributes
    await testUtils.checkA11y()

    // Check keyboard navigation
    await user.tab()
    expect(screen.getByRole('button')).toHaveFocus()
  })
})
```

## CI/CD Integration

### GitHub Actions
```yaml
- name: Run Tests
  run: |
    npm run test:ci
    npm run test:coverage:comprehensive

- name: Upload Coverage
  uses: codecov/codecov-action@v3
  with:
    files: ./coverage/lcov.info

- name: Upload E2E Evidence
  uses: actions/upload-artifact@v3
  if: failure()
  with:
    name: playwright-evidence
    path: test-results/
```

### Coverage Enforcement
- **Fail builds** on coverage below thresholds
- **Generate reports** for all test types
- **Track coverage trends** over time
- **Alert on coverage drops**

## Best Practices

### Test Organization
1. **Co-locate unit tests** with components
2. **Group integration tests** by feature
3. **Organize E2E tests** by user journey
4. **Use descriptive test names**

### Test Data Management
1. **Use factories** for test data generation
2. **Reset state** between tests
3. **Clean up resources** after tests
4. **Isolate test data** from production

### Assertions
1. **Test user behavior**, not implementation
2. **Use semantic queries** (ByRole, ByLabelText)
3. **Avoid brittle selectors**
4. **Test error states** and edge cases

### Performance
1. **Parallel test execution**
2. **Efficient test data setup**
3. **Smart test retries**
4. **Optimized CI pipelines**

## Troubleshooting

### Common Issues

#### Test Timeouts
```bash
# Increase timeout for slow tests
npm run test -- --timeout=10000
```

#### Flaky E2E Tests
```bash
# Run with retries
npm run test:e2e -- --retries=3
```

#### Coverage Issues
```bash
# Debug coverage
npm run test:coverage -- --reporter=verbose
```

#### Mock Issues
```bash
# Reset MSW handlers
server.resetHandlers()
```

### Debugging
1. **Use test:ui** for interactive debugging
2. **Add console.log** statements in tests
3. **Use browser devtools** in E2E tests
4. **Check test artifacts** (screenshots, videos)

## Metrics and Reporting

### Test Metrics
- **Test count**: Total number of tests
- **Coverage percentage**: Code coverage by type
- **Test duration**: Performance tracking
- **Flaky test rate**: Stability monitoring

### Reports Generated
1. **HTML Coverage Report**: Visual coverage analysis
2. **JUnit XML**: CI/CD integration
3. **Allure Report**: Comprehensive test reporting
4. **Custom Markdown**: Human-readable summaries

## Future Enhancements

### Planned Features
1. **Visual regression testing** with screenshot comparison
2. **Performance budgets** with automated monitoring
3. **Cross-browser compatibility** testing
4. **Load testing** for critical paths
5. **Security testing** integration

### Tool Upgrades
1. **Vitest workspace** for monorepo support
2. **Playwright component testing** integration
3. **Advanced MSW scenarios**
4. **AI-powered test generation**

## Conclusion

This comprehensive testing infrastructure provides:

- ✅ **Complete coverage** of all application layers
- ✅ **Reliable test execution** with proper isolation
- ✅ **Performance monitoring** and optimization
- ✅ **Accessibility compliance** validation
- ✅ **CI/CD integration** with quality gates
- ✅ **Developer experience** with fast feedback loops

The testing strategy ensures high-quality, reliable code delivery while maintaining excellent developer productivity and confidence in deployments.