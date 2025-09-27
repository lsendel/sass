# CLAUDE.md - Frontend

This file provides guidance to Claude Code (claude.ai/code) when working with the React TypeScript frontend of the Spring Boot Modulith Payment Platform.

## Project Overview

This is the frontend client for a secure subscription management payment platform built with React 19+ and TypeScript. It features modern state management with Redux Toolkit, comprehensive form handling, Stripe payment integration, and extensive testing capabilities.

## Technology Stack

### Core Technologies
- **React**: 19.1.1 with modern hooks and functional components
- **TypeScript**: 5.7.2 with strict type checking enabled
- **Vite**: 7.1.7 for fast development and optimized builds
- **Node.js**: ES modules with `"type": "module"` configuration

### State Management & API
- **Redux Toolkit**: 2.3.0 with RTK Query for API state management
- **React Redux**: 9.1.2 for React-Redux integration
- **Axios**: 1.7.9 for HTTP client (used alongside RTK Query)

### UI & Styling
- **TailwindCSS**: 4.1.13 with custom design system
- **HeadlessUI**: 2.2.9 for accessible UI components
- **Heroicons**: 2.0.18 for consistent iconography
- **Lucide React**: 0.544.0 for additional icons
- **React Hot Toast**: 2.4.1 for notifications
- **Radix UI**: Components for avatar and slot primitives

### Forms & Validation
- **React Hook Form**: 7.63.0 for performant form management
- **Zod**: 4.1.11 for runtime type validation and schema validation
- **@hookform/resolvers**: 5.2.2 for Zod integration

### Payment Processing
- **@stripe/react-stripe-js**: 4.0.2 for React Stripe components
- **@stripe/stripe-js**: 7.9.0 for Stripe JavaScript SDK

### Routing & Navigation
- **React Router DOM**: 7.9.2 for client-side routing

### Testing Framework
- **Vitest**: 3.2.4 for unit testing with React Testing Library
- **Playwright**: 1.55.1 for end-to-end testing across browsers
- **React Testing Library**: 16.3.0 for component testing
- **MSW**: 2.11.3 for API mocking

### Development Tools
- **ESLint**: 9.36.0 with TypeScript and React plugins
- **Prettier**: 3.4.2 for code formatting
- **TypeScript**: 5.7.2 with strict configuration and path mapping
- **Storybook**: 9.1.8 for component development
- **Husky**: 9.1.7 for git hooks

## Project Structure

```
frontend/
├── src/
│   ├── components/           # Reusable UI components
│   │   ├── layouts/         # Layout components (Auth, Dashboard)
│   │   ├── organizations/   # Organization-specific components
│   │   ├── payments/        # Payment-related components
│   │   ├── subscription/    # Subscription management components
│   │   └── ui/              # Generic UI components
│   ├── pages/               # Route-level page components
│   │   ├── auth/            # Authentication pages
│   │   ├── dashboard/       # Dashboard pages
│   │   ├── organizations/   # Organization management
│   │   ├── payments/        # Payment pages
│   │   ├── settings/        # Settings pages
│   │   └── subscription/    # Subscription pages
│   ├── store/               # Redux store configuration
│   │   ├── api/             # RTK Query API definitions
│   │   ├── slices/          # Redux slices for state management
│   │   ├── hooks/           # Typed hooks for Redux
│   │   └── index.ts         # Store configuration
│   ├── types/               # TypeScript type definitions
│   ├── utils/               # Utility functions
│   ├── hooks/               # Custom React hooks
│   └── services/            # Business logic services
├── tests/e2e/               # Playwright end-to-end tests
├── public/                  # Static assets
└── Configuration files...
```

## Key Architecture Patterns

### State Management Architecture
- **Redux Toolkit**: Central state management with createSlice pattern
- **RTK Query**: API state management with automatic caching and invalidation
- **Typed Hooks**: Custom useAppDispatch and useAppSelector for type safety

### API Integration Pattern
```typescript
// RTK Query API slice pattern
export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/auth',
    prepareHeaders: (headers, { getState }) => {
      // Add auth headers
      return headers;
    },
  }),
  tagTypes: ['User', 'Session'],
  endpoints: (builder) => ({
    // Define endpoints
  }),
});
```

### Component Organization
- **Feature-based**: Components grouped by domain (auth, payments, organizations)
- **Layout System**: Dedicated layout components for different app sections
- **Reusable UI**: Generic components in `ui/` directory
- **Page Components**: Route-level components in `pages/` directory

## Development Guidelines

### TypeScript Best Practices
- **Strict Mode**: All TypeScript strict checks enabled
- **Path Aliases**: Use `@/` for clean imports with configured path mapping
- **Type Definitions**: Define types in `types/` directory, use branded types for IDs
- **Import Types**: Use `import type` for type-only imports

### React Patterns
- **Functional Components**: Use hooks-based functional components exclusively
- **Custom Hooks**: Extract reusable logic into custom hooks
- **Error Boundaries**: Implement error boundaries for fault tolerance
- **Suspense Ready**: Prepare for concurrent features with proper loading states

### State Management Patterns
- **RTK Query First**: Use RTK Query for server state, Redux slices for client state
- **Normalized State**: Keep complex data normalized in Redux store
- **Optimistic Updates**: Implement optimistic updates for better UX
- **Cache Management**: Leverage RTK Query's automatic cache invalidation

### Form Handling Best Practices
```typescript
// React Hook Form with Zod validation pattern
const schema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

type FormData = z.infer<typeof schema>;

const { handleSubmit, control, formState: { errors } } = useForm<FormData>({
  resolver: zodResolver(schema),
  mode: 'onBlur', // Validate on blur for better UX
});
```

### Styling Guidelines
- **TailwindCSS First**: Use Tailwind utility classes for styling
- **Design System**: Follow custom color palette and spacing defined in tailwind.config.js
- **Responsive Design**: Mobile-first responsive design patterns
- **Semantic Colors**: Use semantic color names (primary, success, warning, error)
- **Custom Utilities**: Extend Tailwind with custom utilities in config

## Testing Strategy

### Unit Testing with Vitest
- **Component Testing**: Test React components with React Testing Library
- **User Interactions**: Test user interactions with @testing-library/user-event
- **Store Testing**: Test Redux slices and API slices independently
- **Coverage**: Maintain good test coverage with `npm run test:coverage`

### E2E Testing with Playwright
- **Cross-Browser**: Test on Chromium, Firefox, and WebKit
- **Mobile Testing**: Include mobile viewport testing
- **User Journeys**: Test complete user workflows end-to-end
- **Visual Testing**: Include screenshot comparison when needed

### Test Organization
```
tests/e2e/
├── auth.spec.ts           # Authentication flows
├── dashboard.spec.ts      # Dashboard functionality
├── organizations.spec.ts  # Organization management
├── payments.spec.ts       # Payment processing
├── subscription.spec.ts   # Subscription management
└── user-journey.spec.ts   # Complete user workflows
```

## API Integration

### Backend Communication
- **Base URL**: Development server proxies `/api` to `http://localhost:8080`
- **Authentication**: Uses secure session-based authentication with opaque tokens
- **Error Handling**: Consistent error handling with toast notifications
- **Loading States**: Proper loading state management with Redux

### RTK Query Configuration
- **Automatic Refetching**: Configured with tag-based cache invalidation
- **Optimistic Updates**: Implemented for critical user actions
- **Error Retry**: Automatic retry logic for failed requests
- **Offline Support**: Basic offline handling with cache-first strategy

## Development Workflow

### Daily Development Commands
```bash
# Start development server
npm run dev

# Run type checking
npm run typecheck

# Run unit tests
npm run test

# Run unit tests with UI
npm run test:ui

# Run E2E tests
npm run test:e2e

# Run E2E tests with UI
npm run test:e2e:ui

# Code linting and formatting
npm run lint
npm run lint:fix
npm run format
npm run format:check

# Build for production
npm run build

# Preview production build
npm run preview
```

### Git Workflow
- **Feature Branches**: Create feature branches from `main`
- **Conventional Commits**: Use conventional commit messages
- **Pre-commit Hooks**: Automated linting and formatting
- **Test Requirements**: All tests must pass before merge

### Code Quality Standards
- **ESLint**: Zero warnings policy for production code
- **TypeScript**: No `any` types, strict type checking
- **Prettier**: Consistent code formatting across the team
- **Test Coverage**: Maintain > 80% test coverage for critical paths

## Security Guidelines

### Authentication & Authorization
- **Session Security**: Secure HTTP-only cookies for authentication
- **CSRF Protection**: Built-in CSRF protection with backend
- **Route Guards**: Implement protected routes with proper redirects
- **Token Handling**: Never store sensitive tokens in localStorage

### Data Protection
- **Input Validation**: Client-side validation with Zod schemas
- **XSS Prevention**: React's built-in XSS protection
- **Content Security Policy**: Implement CSP headers in deployment
- **HTTPS Only**: All production traffic over HTTPS

## Performance Optimization

### Bundle Optimization
- **Tree Shaking**: Vite's automatic tree shaking for smaller bundles
- **Code Splitting**: Route-based code splitting with lazy loading
- **Asset Optimization**: Optimized images and static assets
- **Bundle Analysis**: Regular bundle size analysis

### Runtime Performance
- **React.memo**: Memoize expensive components
- **useMemo/useCallback**: Optimize expensive calculations and function references
- **Virtual Scrolling**: Implement for large lists when needed
- **Lazy Loading**: Lazy load images and non-critical components

### Network Optimization
- **RTK Query Caching**: Efficient API caching and deduplication
- **Request Batching**: Batch multiple API requests when possible
- **Prefetching**: Prefetch critical data for better UX
- **Compression**: Enable gzip/brotli compression

## Stripe Payment Integration

### Payment Components
- **Stripe Elements**: Use Stripe React components for secure payment forms
- **Payment Methods**: Support multiple payment methods (cards, wallets)
- **3D Secure**: Handle 3D Secure authentication flows
- **Error Handling**: Comprehensive error handling for payment failures

### Security Best Practices
- **PCI Compliance**: Never handle card data directly
- **Stripe Elements**: Use Stripe's secure input elements
- **Webhook Validation**: Validate webhook signatures on backend
- **Amount Validation**: Server-side amount validation

## Troubleshooting Guide

### Common Development Issues

#### Build/Dev Server Issues
```bash
# Clear Vite cache
rm -rf node_modules/.vite
npm run dev

# Clear all caches and reinstall
rm -rf node_modules package-lock.json
npm install

# Check port conflicts
lsof -i :3000
```

#### TypeScript Issues
```bash
# Force TypeScript check
npm run typecheck

# Clear TypeScript cache
rm -rf node_modules/.cache
npx tsc --build --clean
```

#### Test Issues
```bash
# Clear test cache
npm run test -- --clearCache

# Run tests in watch mode for debugging
npm run test -- --watch

# Debug specific test
npm run test -- --run specific.test.ts
```

### Common Runtime Issues

#### Redux State Issues
- **Hydration Errors**: Check for server/client state mismatches
- **Stale Data**: Verify RTK Query cache invalidation tags
- **State Updates**: Ensure immutable state updates in reducers

#### Authentication Issues
- **Session Expiry**: Implement automatic session refresh
- **Redirect Loops**: Check route guard logic and authentication state
- **CORS Issues**: Verify backend CORS configuration

#### Performance Issues
- **Bundle Size**: Check for large dependencies with bundle analyzer
- **Memory Leaks**: Look for uncleaned event listeners and subscriptions
- **Render Performance**: Use React DevTools Profiler

## Environment Configuration

### Development Environment
```bash
# Required Node.js version
node >= 18.0.0

# Environment variables
VITE_API_BASE_URL=http://localhost:8080
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...
```

### Production Configuration
- **Environment Variables**: Proper environment variable handling
- **Build Optimization**: Production-optimized builds with Vite
- **Error Reporting**: Integration with error reporting services
- **Analytics**: User analytics and monitoring setup

### Browser Support
- **Modern Browsers**: Chrome 90+, Firefox 88+, Safari 14+
- **Mobile**: iOS Safari 14+, Android Chrome 90+
- **Polyfills**: Minimal polyfills for critical features only

## Best Practices Summary

### Code Organization
- Group by feature, not by file type
- Use absolute imports with path aliases
- Keep components small and focused
- Extract custom hooks for reusable logic

### Performance
- Use RTK Query for server state management
- Implement proper loading states
- Optimize re-renders with React.memo and useMemo
- Lazy load non-critical components

### Testing
- Test user behaviors, not implementation details
- Use MSW for API mocking in tests
- Write E2E tests for critical user journeys
- Maintain good test coverage

### Security
- Validate all inputs with Zod schemas
- Never store sensitive data in client storage
- Implement proper error boundaries
- Use Stripe Elements for payment security

### Developer Experience
- Use TypeScript strict mode
- Configure proper linting and formatting
- Set up pre-commit hooks
- Document complex business logic

## Integration with Backend

This frontend is designed to work seamlessly with the Spring Boot Modulith Payment Platform backend. Key integration points:

- **API Compatibility**: Matches backend API contracts and data structures
- **Authentication**: Uses backend's OAuth2/PKCE flow with secure sessions
- **Multi-tenancy**: Supports organization-based tenancy model
- **Payment Processing**: Integrates with backend's Stripe webhook handling
- **Real-time Updates**: Prepared for WebSocket integration for real-time features

For backend-specific guidance, see `../backend/CLAUDE.md`.

## Recent Code Review Improvements

Based on the comprehensive code review and backend improvements, the following frontend enhancements are recommended:

### 🚧 **High Priority Frontend Improvements**

#### **Security Enhancements**
- **Update API Integration**: Align with new production security configuration
- **Enhanced Authentication**: Support for new password policies (12+ chars, complexity)
- **Account Lockout Handling**: Display appropriate messages for locked accounts
- **Session Management**: Integrate with improved backend session handling

#### **New Module Support**
- **Payment Management**: Create components for the new payment module
  - Payment creation and confirmation flows
  - Customer management interfaces
  - Payment history and status tracking
  - Refund processing interfaces

- **Subscription Management**: Build subscription lifecycle components
  - Plan selection and subscription creation
  - Billing cycle management interfaces
  - Invoice viewing and downloading
  - Subscription pause/resume functionality

- **Audit & Compliance**: Add compliance dashboards
  - Activity reporting interfaces
  - GDPR compliance tools
  - Audit trail viewing
  - Security incident monitoring

#### **State Management Updates**
- **New API Slices**: Create RTK Query slices for payment, subscription, and audit APIs
- **Enhanced Error Handling**: Improve error handling for new backend error types
- **Optimistic Updates**: Implement for payment and subscription operations
- **Real-time Updates**: Prepare for event-driven updates from backend

#### **Type Safety Improvements**
- **Backend Type Alignment**: Sync TypeScript types with new backend DTOs
- **Branded Types**: Implement branded types for entity IDs (PaymentId, SubscriptionId)
- **Enhanced Validation**: Update Zod schemas for new password requirements
- **API Response Types**: Create comprehensive types for all new API endpoints

### ✅ **Existing Strengths to Maintain**
- **Modern React Architecture**: Already using React 18 with hooks and functional components
- **Strong Type Safety**: TypeScript strict mode and comprehensive typing
- **Robust Testing**: Vitest + Playwright for comprehensive test coverage
- **Performance Optimization**: Proper memoization and code splitting
- **Stripe Integration**: Existing secure payment component foundation

### 🔧 **Recommended Implementation Steps**

1. **Update Authentication Components**
   ```typescript
   // Enhanced password validation
   const passwordSchema = z.string()
     .min(12, 'Password must be at least 12 characters')
     .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/,
            'Password must contain uppercase, lowercase, digit, and special character');
   ```

2. **Create Payment Module Components**
   ```typescript
   // Payment processing components
   - PaymentForm (with Stripe Elements)
   - PaymentHistory
   - CustomerManagement
   - RefundInterface
   ```

3. **Build Subscription Management**
   ```typescript
   // Subscription components
   - PlanSelector
   - SubscriptionDashboard
   - BillingHistory
   - InvoiceViewer
   ```

4. **Add Audit Dashboard**
   ```typescript
   // Audit and compliance components
   - ActivityLog
   - ComplianceReport
   - SecurityMonitor
   - GDPRTools
   ```

5. **Enhanced Testing Strategy**
   ```typescript
   // E2E tests for new modules
   - payment-flow.spec.ts
   - subscription-lifecycle.spec.ts
   - security-compliance.spec.ts
   ```

### 🔗 **Backend Integration Points**
- **Event-Driven Updates**: Prepare for real-time notifications from backend events
- **Module API Integration**: RTK Query slices for all new backend modules
- **Security Alignment**: Match frontend security practices with backend hardening
- **Error Handling**: Comprehensive error handling for all new backend error types

The frontend is well-architected and ready for these enhancements. The existing foundation provides excellent support for scaling to the full payment platform feature set.