# Code Review Standards

## Overview

This document outlines the coding standards and review guidelines for the SASS platform. These standards ensure code quality, maintainability, and consistency across the entire codebase.

## General Principles

### 1. Code Quality

- **Zero ESLint Errors**: All code must pass ESLint checks without errors
- **TypeScript Strict Mode**: Full type safety with no `any` types
- **Test Coverage**: Minimum 80% test coverage for all new features
- **Performance**: No performance regressions in Core Web Vitals

### 2. Code Style

- **Prettier Formatting**: All code must be formatted with Prettier
- **Consistent Naming**: Use descriptive, consistent naming conventions
- **DRY Principle**: Don't Repeat Yourself - eliminate code duplication
- **Single Responsibility**: Each function/component should have one clear purpose

### 3. Documentation

- **JSDoc Comments**: All public APIs must have JSDoc documentation
- **README Updates**: Update documentation for any API changes
- **Code Comments**: Explain complex business logic, not obvious code

## TypeScript Standards

### Type Safety

```typescript
// ✅ Good: Proper typing
interface User {
  id: string;
  email: string;
  role: UserRole;
}

// ❌ Bad: Using any
interface User {
  id: any;
  email: any;
  role: any;
}
```

### Import/Export

```typescript
// ✅ Good: Named exports preferred
export interface User { ... }
export type UserRole = 'admin' | 'user'

// ❌ Bad: Default exports
export default interface User { ... }
```

### Error Handling

```typescript
// ✅ Good: Proper error types
try {
  await apiCall();
} catch (error) {
  const parsedError = parseApiError(error);
  logger.error("API call failed:", parsedError);
}

// ❌ Bad: Generic error handling
try {
  await apiCall();
} catch (error) {
  console.error(error); // eslint-disable-line
}
```

## React Standards

### Component Structure

```tsx
// ✅ Good: Clear component structure
interface ComponentProps {
  user: User;
  onUpdate: (user: User) => void;
}

const UserProfile: React.FC<ComponentProps> = ({ user, onUpdate }) => {
  // Component logic here
  return <div>{/* JSX */}</div>;
};

// ❌ Bad: Inline props, no TypeScript
const UserProfile = ({ user, onUpdate }) => {
  return <div>{/* JSX */}</div>;
};
```

### Hooks Usage

```tsx
// ✅ Good: Custom hooks for complex logic
const useUserProfile = (userId: string) => {
  const [user, setUser] = useState<User | null>(null);
  // Hook logic
  return { user, updateUser };
};

// ❌ Bad: Complex logic in components
const UserProfile: React.FC = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);
  // Complex logic mixed with JSX
};
```

### Performance

```tsx
// ✅ Good: Memoization when needed
const UserList = React.memo(({ users }: { users: User[] }) => {
  return <div>{/* User list */}</div>;
});

// ❌ Bad: Unnecessary re-renders
const UserList = ({ users }) => {
  return <div>{/* User list */}</div>;
};
```

## API Standards

### RTK Query Usage

```typescript
// ✅ Good: Proper API slice structure
export const userApi = api.injectEndpoints({
  endpoints: (builder) => ({
    getUser: builder.query<User, string>({
      query: (id) => `users/${id}`,
      providesTags: ["User"],
    }),
  }),
});

// ❌ Bad: Inline API calls
const fetchUser = async (id: string) => {
  const response = await fetch(`/api/users/${id}`);
  return response.json();
};
```

### Error Handling

```typescript
// ✅ Good: Centralized error parsing
const result = await apiCall().unwrap()
catch (error) {
  const parsed = parseApiError(error)
  // Handle specific error types
}

// ❌ Bad: Direct error handling
try {
  const result = await apiCall()
} catch (error: any) { // eslint-disable-line
  console.error(error.message)
}
```

## Testing Standards

### Unit Tests

```typescript
// ✅ Good: Comprehensive test coverage
describe('UserProfile', () => {
  it('displays user information', () => {
    const user = { id: '1', name: 'John' }
    render(<UserProfile user={user} />)
    expect(screen.getByText('John')).toBeInTheDocument()
  })

  it('handles loading state', () => {
    // Test loading state
  })
})

// ❌ Bad: Incomplete testing
describe('UserProfile', () => {
  it('renders', () => {
    render(<UserProfile />)
  })
})
```

### Test Organization

```
__tests__/
  components/
    UserProfile.test.tsx
  hooks/
    useUserProfile.test.ts
  utils/
    apiError.test.ts
```

## File Organization

### Directory Structure

```
src/
  components/          # Reusable UI components
    ui/               # Basic UI primitives
    forms/            # Form components
    layouts/          # Layout components
  pages/              # Page components
  hooks/              # Custom React hooks
  store/              # Redux store and slices
    api/              # RTK Query APIs
    slices/           # Redux slices
  utils/              # Utility functions
  types/              # TypeScript type definitions
  constants/          # Application constants
```

### File Naming

```typescript
// ✅ Good: Descriptive names
UserProfile.tsx;
useAuthState.ts;
userApi.ts;

// ❌ Bad: Generic names
Component.tsx;
hook.ts;
api.ts;
```

## Review Checklist

### Code Quality

- [ ] No ESLint errors or warnings
- [ ] TypeScript strict mode compliance
- [ ] No `any` types used
- [ ] Proper error handling
- [ ] No console.log statements (use logger instead)

### Testing

- [ ] Unit tests for new functionality
- [ ] Integration tests for API calls
- [ ] Test coverage > 80%
- [ ] Tests pass in CI/CD

### Performance

- [ ] No unnecessary re-renders
- [ ] Proper memoization where needed
- [ ] Bundle size impact considered
- [ ] Core Web Vitals not regressed

### Security

- [ ] Input validation implemented
- [ ] XSS prevention (proper escaping)
- [ ] CSRF protection where applicable
- [ ] Sensitive data not logged

### Documentation

- [ ] JSDoc for public APIs
- [ ] README updated if needed
- [ ] Complex logic documented
- [ ] Breaking changes documented

### Accessibility

- [ ] ARIA labels where needed
- [ ] Keyboard navigation works
- [ ] Screen reader compatible
- [ ] Color contrast sufficient

## Review Process

### 1. Self-Review

- Run all tests locally
- Check ESLint and TypeScript errors
- Review code against standards
- Test edge cases manually

### 2. Peer Review

- At least one reviewer required
- Focus on logic, not style (Prettier handles style)
- Suggest improvements, don't demand changes
- Approve only when standards are met

### 3. Automated Checks

- CI/CD pipeline must pass
- Pre-commit hooks prevent bad commits
- Code coverage requirements met
- Performance budgets not exceeded

## Common Issues to Watch For

### Anti-Patterns

1. **Large Components**: Break down components > 200 lines
2. **Deep Nesting**: Limit JSX nesting to 3-4 levels
3. **Magic Numbers**: Extract constants for any hardcoded values
4. **Long Functions**: Functions should be < 30 lines
5. **Mixed Concerns**: Separate business logic from presentation

### Performance Issues

1. **Unnecessary Renders**: Use React.memo, useMemo, useCallback appropriately
2. **Large Bundles**: Code splitting for routes and heavy components
3. **Memory Leaks**: Clean up event listeners, timers, subscriptions
4. **Inefficient Queries**: Use proper caching and pagination

### Security Issues

1. **XSS Vulnerabilities**: Sanitize user input
2. **CSRF Attacks**: Use proper token validation
3. **Information Disclosure**: Don't log sensitive data
4. **Injection Attacks**: Use parameterized queries

## Tools and Automation

### Pre-commit Hooks

- ESLint checking
- TypeScript compilation
- Prettier formatting
- Test execution

### CI/CD Pipeline

- Automated testing
- Code coverage reporting
- Security scanning
- Performance monitoring

### Code Quality Tools

- ESLint for code quality
- Prettier for formatting
- TypeScript for type checking
- SonarQube for advanced analysis

## Continuous Improvement

### Regular Reviews

- Monthly review of coding standards
- Update standards based on new patterns
- Incorporate team feedback
- Stay current with best practices

### Training

- Regular code review sessions
- Pair programming opportunities
- Documentation of common patterns
- Tool usage training

---

_This document is living and should be updated as standards evolve. Last updated: 2025-01-26_
