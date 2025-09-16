# Research Findings: Spring Boot Modulith + React Payment Platform

## Technology Stack Research

### 1. Spring Modulith Integration

**Decision**: Spring Modulith 1.1+ with ArchUnit enforcement

**Research Summary**:
- Spring Modulith provides compile-time module boundary enforcement through ArchUnit rules
- Event-driven communication via Spring ApplicationEventPublisher with transaction support
- Documentation generation for module dependencies and architecture visualization
- Easy migration path to microservices if needed (modules can be extracted)

**Rationale**:
- Enforces clean architecture without microservice complexity
- Built-in support for domain events and module verification
- Spring team official support and integration with Spring Boot
- Provides modular monolith benefits with operational simplicity

**Alternatives Considered**:
- Plain Spring Boot: No boundary enforcement, risk of tight coupling
- Full microservices: Too complex for initial implementation, operational overhead
- Hexagonal architecture: More complex, no Spring integration

**Implementation Notes**:
- Use `@ApplicationModule` annotation for module definition
- Configure ArchUnit rules in test classes for boundary enforcement
- Leverage `@TransactionalEventListener` for cross-module communication

### 2. OAuth2/PKCE Implementation

**Decision**: Spring Security OAuth2 Client with custom token storage

**Research Summary**:
- Spring Security OAuth2 Client provides RFC-compliant OAuth2/PKCE implementation
- Supports multiple OAuth2 providers (Google, GitHub, Microsoft)
- Built-in CSRF protection and state parameter validation
- Customizable token storage and session management

**Rationale**:
- Production-ready security implementation following RFC standards
- Extensive community support and security patches
- Integrates seamlessly with Spring Boot Security configuration
- Allows custom token storage to meet opaque token requirements

**Alternatives Considered**:
- Custom OAuth2 implementation: Security risks, maintenance burden
- JWT tokens: Violates opaque token constraint
- Third-party libraries: Less Spring integration, additional dependencies

**Implementation Notes**:
- Configure custom `AuthorizedClientService` for token storage
- Implement SHA-256 hashing with per-token salts
- Use Redis for session storage and token introspection caching

### 3. Payment Processing Architecture

**Decision**: Stripe integration with webhook idempotency

**Research Summary**:
- Stripe provides comprehensive payment processing with strong API design
- Webhook system for real-time payment status updates
- Built-in fraud detection and PCI compliance handling
- Extensive documentation and client libraries

**Rationale**:
- Industry standard with proven scalability
- Handles complex payment scenarios (subscriptions, retries, disputes)
- Strong security model with webhook signature verification
- Comprehensive test mode for development

**Alternatives Considered**:
- PayPal: Less comprehensive subscription features
- Square: Limited international support
- Braintree: More complex integration
- Custom payment processing: PCI compliance burden

**Implementation Notes**:
- Use Stripe Java SDK for API integration
- Implement idempotency using Redis for webhook processing
- Configure webhook endpoints with signature verification
- Use Stripe Customer Portal for subscription management UI

### 4. Database Architecture

**Decision**: PostgreSQL with partitioned audit tables, Redis for sessions

**Research Summary**:
- PostgreSQL provides ACID compliance with JSON/JSONB support
- Native support for table partitioning for large audit datasets
- Strong consistency guarantees for financial data
- Redis provides fast session storage with TTL support

**Rationale**:
- ACID compliance critical for payment processing
- JSON support eliminates need for complex object mapping
- Partitioning handles large audit log volumes efficiently
- Redis provides fast session lookup and caching

**Alternatives Considered**:
- MongoDB: No ACID guarantees, consistency issues
- MySQL: Limited JSON support, partitioning complexity
- In-memory only: Data persistence requirements
- DynamoDB: Vendor lock-in, complex transaction handling

**Implementation Notes**:
- Use Flyway for database migrations
- Configure monthly partitions for audit_events table
- Implement soft delete with scheduled hard delete jobs
- Use Redis TTL for session expiration

### 5. Frontend Architecture

**Decision**: React 18+ with TypeScript, Redux Toolkit for state

**Research Summary**:
- React 18 provides concurrent features and improved SSR
- TypeScript adds compile-time type safety and better IDE support
- Redux Toolkit reduces boilerplate and provides best practices
- Vite for fast development builds and HMR

**Rationale**:
- Large ecosystem with extensive component libraries
- Strong typing reduces runtime errors
- Redux provides predictable state management for complex apps
- Modern build tools improve development experience

**Alternatives Considered**:
- Vue.js: Smaller ecosystem, less enterprise adoption
- Angular: Heavy framework, steep learning curve
- Vanilla JavaScript: No type safety, higher error rates
- Svelte: Smaller community, fewer libraries

**Implementation Notes**:
- Use React Query for server state management
- Implement strict TypeScript configuration
- Configure ESLint and Prettier for code quality
- Use React Testing Library for component tests

### 6. Container and Deployment Strategy

**Decision**: Docker containers with multi-stage builds, Kubernetes-ready

**Research Summary**:
- Docker provides consistent deployment environments
- Multi-stage builds reduce final image size
- Kubernetes provides orchestration and scaling capabilities
- GitHub Actions integration for CI/CD pipelines

**Rationale**:
- Environment consistency across development and production
- Scalability requirements for multi-tenant SaaS
- Industry standard deployment pattern
- Cloud-agnostic deployment options

**Alternatives Considered**:
- Traditional deployment: Environment inconsistency
- Serverless: Cold start issues, vendor lock-in
- VM-based: Resource inefficiency
- Platform-specific deployment: Vendor lock-in

**Implementation Notes**:
- Use distroless base images for security
- Configure health checks for container orchestration
- Implement graceful shutdown handling
- Use init containers for database migrations

### 7. Observability and Monitoring

**Decision**: Micrometer metrics with Prometheus, structured logging

**Research Summary**:
- Micrometer provides vendor-neutral metrics collection
- Prometheus offers powerful querying and alerting
- Structured logging enables efficient log analysis
- OpenTelemetry for distributed tracing

**Rationale**:
- Vendor neutrality allows switching monitoring backends
- Prometheus provides powerful alerting capabilities
- Structured logs improve debugging and analysis
- Tracing helps with performance optimization

**Alternatives Considered**:
- Vendor-specific solutions: Lock-in concerns
- Basic logging: Insufficient for production debugging
- Custom metrics: Development and maintenance overhead
- No observability: Impossible to debug production issues

**Implementation Notes**:
- Configure Logback for structured JSON logging
- Implement correlation ID tracing across requests
- Set up custom business metrics for payment processing
- Configure PII redaction in log output

## Security Considerations Research

### Authentication Security
- Opaque tokens prevent token inspection attacks
- SHA-256 with salt prevents rainbow table attacks
- Session storage allows immediate revocation
- PKCE prevents authorization code interception

### API Security
- CORS configuration for cross-origin requests
- CSRF protection for state-changing operations
- Rate limiting to prevent abuse
- Input validation on all endpoints

### Data Security
- PII redaction in logs and audit trails
- Encryption at rest for sensitive data
- TLS termination at load balancer
- Database connection encryption

### Compliance Requirements
- GDPR data retention and deletion policies
- Audit logging for all significant actions
- Data export capabilities for data portability
- Consent management for data processing

## Performance Considerations

### Database Performance
- Connection pooling with HikariCP
- Query optimization with proper indexing
- Partition strategy for audit logs
- Read replicas for reporting queries

### Caching Strategy
- Redis for session and token introspection
- Application-level caching for static data
- CDN for static assets
- Browser caching for API responses

### API Performance
- Async processing for non-critical operations
- Bulk operations for batch processing
- Pagination for large result sets
- Compression for API responses

## Integration Points

### External Services
- Stripe API for payment processing
- OAuth2 providers for authentication
- Email service for notifications
- Monitoring services for observability

### Internal Communication
- Event-driven architecture for module communication
- REST APIs for synchronous operations
- Database as integration point for shared data
- Message queues for async processing (future)

## Frontend OAuth2 Integration Research

### 8. React OAuth2/PKCE Implementation Patterns

**Decision**: React hooks with Redux Toolkit state management and PKCE flow

**Research Summary**:
- Multiple libraries available for OAuth2 PKCE integration (react-oauth2-code-pkce, react-pkce)
- Custom hooks pattern preferred for maintainability and control
- Authorization Code Flow with PKCE is the secure standard for SPAs
- Token storage security critical - avoid localStorage, prefer secure patterns

**Rationale**:
- PKCE provides additional security layer for public clients (SPAs)
- React hooks provide clean, reusable authentication logic
- Redux Toolkit offers predictable state management for auth flows
- Industry consensus on avoiding client-side token storage

**Alternatives Considered**:
- Implicit Flow: Deprecated due to security vulnerabilities
- Client-side token validation: Insecure, can be bypassed
- Traditional OAuth2: Lacks PKCE protection for public clients
- Third-party auth providers only: Reduces flexibility

**Implementation Notes**:
```typescript
// Custom useAuth hook pattern
const useAuth = () => {
  const dispatch = useAppDispatch()
  const { isAuthenticated, user, loading } = useAppSelector(state => state.auth)

  const login = useCallback(async (provider: string) => {
    // Generate PKCE challenge
    const { code_verifier, code_challenge } = generatePKCE()
    // Store in Redux for callback handling
    dispatch(setPKCEChallenge({ code_verifier, code_challenge }))
    // Redirect to authorization server
  }, [dispatch])

  return { isAuthenticated, user, loading, login, logout }
}
```

### 9. Redux Toolkit State Management for Authentication

**Decision**: RTK Query for API calls with auth slice for local state

**Research Summary**:
- RTK Query provides excellent error handling and caching for auth endpoints
- Auth slice manages local authentication state (user, tokens, loading states)
- Global error handling middleware for consistent UX
- Automatic token injection via prepareHeaders

**Rationale**:
- RTK Query reduces boilerplate and provides built-in optimistic updates
- Centralized error handling improves user experience
- Token management centralized in Redux store
- TypeScript integration provides compile-time safety

**Implementation Notes**:
```typescript
// Auth slice structure
interface AuthState {
  user: User | null
  isAuthenticated: boolean
  loading: boolean
  error: string | null
  codeVerifier: string | null
  codeChallenge: string | null
}

// RTK Query API definition
const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/v1/auth',
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.accessToken
      if (token) {
        headers.set('authorization', `Bearer ${token}`)
      }
      return headers
    },
  }),
  endpoints: (builder) => ({
    exchangeCodeForToken: builder.mutation({
      query: ({ code, codeVerifier }) => ({
        url: '/token',
        method: 'POST',
        body: { code, code_verifier: codeVerifier },
      }),
    }),
  }),
})
```

### 10. OAuth2 Callback Handling in React

**Decision**: React Router with callback component and error boundaries

**Research Summary**:
- Callback URL handling requires careful parameter extraction and validation
- State parameter validation prevents CSRF attacks
- Error handling for various OAuth2 error scenarios
- Loading states during token exchange process

**Implementation Notes**:
```typescript
// OAuth2 Callback Component
const OAuth2Callback: React.FC = () => {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const { codeVerifier, codeChallenge } = useAppSelector(state => state.auth)
  const [exchangeCode] = useExchangeCodeForTokenMutation()

  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search)
    const code = urlParams.get('code')
    const state = urlParams.get('state')
    const error = urlParams.get('error')

    if (error) {
      dispatch(setAuthError(error))
      navigate('/login?error=' + error)
      return
    }

    if (code && codeVerifier) {
      exchangeCode({ code, codeVerifier })
        .unwrap()
        .then((result) => {
          dispatch(setTokens(result))
          navigate('/dashboard')
        })
        .catch((error) => {
          dispatch(setAuthError(error.message))
          navigate('/login?error=token_exchange_failed')
        })
    }
  }, [])

  return <LoadingSpinner message="Completing authentication..." />
}
```

### 11. Error Handling and User Experience Patterns

**Decision**: Global error middleware with user-friendly error messages

**Research Summary**:
- RTK Query provides comprehensive error handling capabilities
- Global middleware intercepts auth failures for consistent handling
- User-friendly error messages mapped from OAuth2 error codes
- Automatic retry logic for network failures

**Implementation Notes**:
```typescript
// Global error handling middleware
const authErrorMiddleware: Middleware = (store) => (next) => (action) => {
  if (isRejectedWithValue(action)) {
    const error = action.payload

    // Handle auth-specific errors
    if (error?.status === 401) {
      store.dispatch(logout())
      toast.error('Your session has expired. Please log in again.')
    } else if (error?.status === 403) {
      toast.error('You do not have permission to access this resource.')
    } else if (action.type.endsWith('/auth/')) {
      // OAuth2 specific error handling
      const oauthError = error?.data?.error
      const userMessage = mapOAuth2ErrorToUserMessage(oauthError)
      toast.error(userMessage)
    }
  }

  return next(action)
}

// Error message mapping
const mapOAuth2ErrorToUserMessage = (error: string): string => {
  const errorMap = {
    'invalid_request': 'Authentication request is invalid. Please try again.',
    'unauthorized_client': 'This application is not authorized. Please contact support.',
    'access_denied': 'Access was denied. Please try logging in again.',
    'unsupported_response_type': 'Authentication method not supported.',
    'invalid_scope': 'Requested permissions are not available.',
    'server_error': 'Authentication server error. Please try again later.',
    'temporarily_unavailable': 'Authentication service is temporarily unavailable.',
  }

  return errorMap[error] || 'An unexpected authentication error occurred.'
}
```

### 12. Security Considerations for OAuth2 in SPAs

**Decision**: Backend-for-Frontend (BFF) pattern with secure token handling

**Research Summary**:
- Never store tokens in localStorage due to XSS vulnerability
- BFF pattern keeps sensitive tokens server-side
- HTTP-only cookies with SameSite=strict for maximum security
- Client-side validation should never be relied upon

**Security Implementation Notes**:
- **Token Storage**: Use HTTP-only cookies managed by backend
- **XSS Protection**: Implement CSP headers and input sanitization
- **CSRF Protection**: Use SameSite=strict and CSRF tokens
- **Redirect URI Validation**: Strict validation on authorization server
- **State Parameter**: Always include and validate for CSRF protection

```typescript
// Secure API configuration
const secureApi = createApi({
  reducerPath: 'secureApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/v1',
    credentials: 'include', // Include HTTP-only cookies
    prepareHeaders: (headers) => {
      // Add CSRF token header
      const csrfToken = getCSRFToken()
      if (csrfToken) {
        headers.set('X-CSRF-Token', csrfToken)
      }
      return headers
    },
  }),
})
```

### 13. React Hooks for OAuth2 Flow Management

**Decision**: Custom hooks with TypeScript for type safety

**Available Libraries**:
- `react-oauth2-code-pkce` (v1.23.2): Provider agnostic, latest 2024
- `react-pkce` (Uber5): Zero dependencies, minimal implementation
- `@tasoskakour/react-use-oauth2`: Custom hook implementation

**Custom Hook Pattern**:
```typescript
// useOAuth2.ts
interface OAuth2Config {
  clientId: string
  authorizationEndpoint: string
  tokenEndpoint: string
  redirectUri: string
  scope: string[]
}

export const useOAuth2 = (config: OAuth2Config) => {
  const dispatch = useAppDispatch()
  const { isAuthenticated, loading, error } = useAppSelector(state => state.auth)

  const generatePKCE = useCallback(() => {
    const codeVerifier = generateRandomString(128)
    const codeChallenge = base64URLEncode(sha256(codeVerifier))
    return { codeVerifier, codeChallenge }
  }, [])

  const initiateLogin = useCallback(async (provider?: string) => {
    const { codeVerifier, codeChallenge } = generatePKCE()
    const state = generateRandomString(32)

    dispatch(setPKCEData({ codeVerifier, codeChallenge, state }))

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: config.clientId,
      redirect_uri: config.redirectUri,
      scope: config.scope.join(' '),
      state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
    })

    window.location.href = `${config.authorizationEndpoint}?${params}`
  }, [config, dispatch, generatePKCE])

  return {
    isAuthenticated,
    loading,
    error,
    login: initiateLogin,
    logout: () => dispatch(logout()),
  }
}
```

### 14. Performance and UX Optimizations

**Optimistic Updates**:
- Update UI immediately for better perceived performance
- Rollback changes if server operation fails
- Show loading states during critical operations

**Caching Strategy**:
- Use RTK Query automatic caching for user data
- Implement proper cache invalidation on logout
- Prefetch user profile data after successful authentication

**Loading States**:
```typescript
// Enhanced loading states
interface AuthUIState {
  isLoggingIn: boolean
  isExchangingCode: boolean
  isRefreshingToken: boolean
  isLoggingOut: boolean
}

// Loading component with context
const AuthLoadingStates: React.FC = () => {
  const { isLoggingIn, isExchangingCode } = useAppSelector(state => state.authUI)

  if (isLoggingIn) return <LoadingSpinner message="Redirecting to login..." />
  if (isExchangingCode) return <LoadingSpinner message="Completing authentication..." />

  return null
}
```

---

*Research completed: All NEEDS CLARIFICATION items resolved for implementation planning*