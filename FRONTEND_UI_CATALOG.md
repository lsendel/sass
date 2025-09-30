# Frontend UI Catalog - SASS Platform
## Comprehensive Web Pages, Forms, and API Integration Documentation

---

## Table of Contents
1. [Application Overview](#application-overview)
2. [Authentication Pages](#authentication-pages)
3. [Dashboard Page](#dashboard-page)
4. [Organizations Pages](#organizations-pages)
5. [Project Management Pages](#project-management-pages)
6. [Task Management Components](#task-management-components)
7. [Subscription Management](#subscription-management)
8. [Settings Page](#settings-page)
9. [API Service Integration](#api-service-integration)
10. [Component Library](#component-library)
11. [State Management](#state-management)
12. [Security & Data Flow](#security--data-flow)

---

## Application Overview

### Technology Stack
- **Frontend Framework**: React 19.1.1 with TypeScript 5.7.2
- **State Management**: Redux Toolkit 2.3.0 with RTK Query
- **Form Handling**: React Hook Form 7.63.0 with Zod validation
- **Styling**: TailwindCSS 4.1.13
- **Routing**: React Router DOM 7.9.2
- **Build Tool**: Vite 7.1.7
- **Testing**: Vitest + Playwright
- **Drag & Drop**: @hello-pangea/dnd for Kanban boards

### Application Structure
```
/ (root)
├── /auth
│   ├── /login
│   └── /callback
├── /dashboard (protected)
├── /organizations (protected)
│   └── /:slug
├── /projects (protected)
│   └── /:projectId
├── /subscription (protected)
└── /settings (protected)
```

---

## Authentication Pages

### 1. Login Page (`/auth/login`)
**Component**: `frontend/src/pages/auth/LoginPage.tsx`

#### Functionality
- Primary authentication entry point
- Supports multiple authentication methods
- Auto-redirect if already authenticated
- Dynamic auth method detection

#### Forms & Fields
- **Password Login Form** (when enabled)
  - Email field (required, email validation)
  - Password field (required, min 12 chars)
  - Submit button with loading state

#### API Connections
```typescript
// API Endpoints Used
GET  /api/v1/auth/methods        // Check available auth methods
POST /api/v1/auth/login          // Password-based login
GET  /api/v1/auth/session        // Check current session
```

#### Wiring & Data Flow
1. Component checks authentication status via Redux store
2. Queries available auth methods from backend
3. Renders appropriate login form based on auth configuration
4. On successful login, updates Redux auth slice
5. Redirects to dashboard

---

### 2. Mock Login Page (`/auth/login` - Development)
**Component**: `frontend/src/pages/auth/MockLoginPage.tsx`

#### Functionality
- Development/demo authentication
- Pre-filled demo credentials
- Glassmorphism UI with animated background
- Mock user session creation

#### Forms & Fields
- **Demo Login Form**
  - Email field (pre-filled: demo@example.com)
  - Password field (pre-filled: DemoPassword123!)
  - Auto-fill button for demo credentials

#### Wiring & Data Flow
1. No backend API calls (mock implementation)
2. Creates mock user object in Redux store
3. Stores mock token in localStorage
4. Simulates authentication delay (1 second)
5. Redirects to dashboard on success

---

### 3. OAuth Callback Page (`/auth/callback`)
**Component**: `frontend/src/pages/auth/CallbackPage.tsx`

#### Functionality
- OAuth/OIDC callback handler
- Processes authorization codes
- Exchanges codes for tokens

#### API Connections
```typescript
POST /api/v1/auth/callback       // Exchange auth code for token
{
  code: string,
  state?: string
}
```

---

## Dashboard Page

### Dashboard Overview (`/dashboard`)
**Component**: `frontend/src/pages/dashboard/DashboardPage.tsx`

#### Functionality
- Personalized user dashboard
- Real-time statistics display
- Quick action navigation
- Activity feed with recent events
- Auto-refresh with WebSocket support

#### Display Components
- **Stats Grid** (4 cards)
  - Organizations count
  - Total payments
  - Revenue metrics
  - Subscription status
- **Quick Actions**
  - Navigate to Organizations
  - Navigate to Payments
  - Navigate to Subscription
- **Recent Activity Feed**
  - Organization creation events
  - Subscription status updates

#### API Connections
```typescript
GET /api/v1/organizations        // Fetch user organizations
GET /api/v1/subscriptions/statistics/{orgId}  // Get subscription stats
// Real-time updates via WebSocket (when enabled)
```

#### Wiring & Data Flow
1. Fetches current user from Redux store
2. Queries user organizations via RTK Query
3. Fetches subscription statistics for primary org
4. Sets up real-time update polling (30-second intervals)
5. Displays aggregated metrics and activity

---

## Organizations Pages

### 1. Organizations List (`/organizations`)
**Component**: `frontend/src/pages/organizations/OrganizationsPage.tsx`

#### Functionality
- Display all user organizations
- Create new organizations
- Navigate to organization details
- Empty state handling

#### Display Components
- **Organization Cards**
  - Organization name and slug
  - Creation date
  - Member count indicator
  - Click to navigate to details

#### Actions
- **Create Organization Button**
  - Opens modal dialog
  - Form validation
  - Auto-slug generation

#### API Connections
```typescript
GET  /api/v1/organizations       // List user organizations
POST /api/v1/organizations       // Create new organization
{
  name: string,
  slug: string,
  settings?: object
}
```

---

### 2. Create Organization Modal
**Component**: `frontend/src/components/organizations/CreateOrganizationModal.tsx`

#### Forms & Fields
- **Organization Creation Form**
  - Name field (3-50 chars, alphanumeric + special)
  - Slug field (3-50 chars, lowercase, auto-generated)
  - Description field (optional, max 200 chars)

#### Features
- Real-time slug generation from name
- Draft auto-save to localStorage
- Form validation with Zod schemas
- Loading states during submission
- Success/error notifications

#### Validation Rules
```typescript
name: z.string()
  .min(3, 'Name must be at least 3 characters')
  .max(50, 'Name must be less than 50 characters')
  .regex(/^[a-zA-Z0-9\s-_.]+$/)

slug: z.string()
  .min(3, 'Slug must be at least 3 characters')
  .max(50, 'Slug must be less than 50 characters')
  .regex(/^[a-z0-9-]+$/)
  .refine(slug => !slug.startsWith('-') && !slug.endsWith('-'))

description: z.string()
  .max(200, 'Keep description under 200 characters')
  .optional()
```

---

### 3. Organization Details (`/organizations/:slug`)
**Component**: `frontend/src/pages/organizations/OrganizationPage.tsx`

#### Functionality
- Display organization details
- Manage members and roles
- Send invitations
- Update organization settings

#### API Connections
```typescript
GET    /api/v1/organizations/slug/{slug}     // Get org by slug
GET    /api/v1/organizations/{id}/members    // List members
POST   /api/v1/organizations/{id}/invitations // Invite user
PUT    /api/v1/organizations/{id}            // Update organization
DELETE /api/v1/organizations/{id}/members/{userId} // Remove member
PUT    /api/v1/organizations/{id}/members/{userId}/role // Update role
```

---

## Subscription Management

### Subscription Page (`/subscription`)
**Component**: `frontend/src/pages/subscription/SubscriptionPage.tsx`

#### Functionality
- View current subscription details
- Manage billing and plans
- View and download invoices
- Cancel/reactivate subscriptions
- Upgrade/downgrade plans

#### Display Components
- **Subscription Card**
  - Current plan name
  - Status badge (Active/Trial/Canceled)
  - Price and billing interval
  - Current period dates
  - Cancellation warning (if scheduled)

- **Invoice History Table**
  - Invoice number
  - Date
  - Amount and currency
  - Status (Paid/Open/Draft)
  - Download action

#### Actions
- **Change Plan Button**
  - Opens upgrade modal
  - Plan comparison view
  - Payment method selection

- **Cancel Subscription**
  - Confirmation dialog
  - End-of-period cancellation
  - Immediate cancellation option

- **Reactivate Subscription**
  - Restore canceled subscription
  - Resume billing

#### API Connections
```typescript
GET  /api/v1/subscriptions/{orgId}           // Get subscription
GET  /api/v1/subscriptions/plans             // Available plans
GET  /api/v1/subscriptions/{orgId}/invoices  // Invoice history
POST /api/v1/subscriptions                   // Create subscription
PUT  /api/v1/subscriptions/{id}/plan         // Change plan
POST /api/v1/subscriptions/{id}/cancel       // Cancel subscription
POST /api/v1/subscriptions/{id}/reactivate   // Reactivate
```

---

### Upgrade Plan Modal
**Component**: `frontend/src/components/subscription/UpgradePlanModal.tsx`

#### Forms & Fields
- **Plan Selection**
  - Radio buttons for available plans
  - Feature comparison grid
  - Price display with currency
  - Trial eligibility indicator

- **Payment Method** (if required)
  - Stripe card element
  - Saved payment methods
  - Add new card option

#### Features
- Plan feature comparison
- Proration calculation
- Stripe Elements integration
- 3D Secure handling
- Loading states

---

## Settings Page

### Settings Management (`/settings`)
**Component**: `frontend/src/pages/settings/SettingsPage.tsx`

#### Tab Structure
1. **Profile Tab**
2. **Account Tab**
3. **Notifications Tab**
4. **Security Tab**
5. **Billing Tab**
6. **Danger Zone Tab**

#### Forms & Fields

##### Profile Settings Form
```typescript
{
  name: string (required, 1-100 chars),
  email: string (email validation),
  timezone: string (optional),
  language: string (optional)
}
```

##### Notification Preferences
```typescript
{
  notifications: {
    email: boolean,
    push: boolean,
    sms: boolean
  }
}
```

##### Security Settings
- Change password form
- Two-factor authentication setup
- Active sessions management
- API keys management

#### API Connections
```typescript
GET  /api/v1/users/current         // Get current user
PUT  /api/v1/users/profile        // Update profile
PUT  /api/v1/users/password       // Change password
POST /api/v1/users/2fa/enable     // Enable 2FA
GET  /api/v1/users/sessions       // List active sessions
POST /api/v1/users/sessions/{id}/revoke // Revoke session
```

---

## API Service Integration

### RTK Query API Slices

#### 1. Auth API (`authApi`)
**File**: `frontend/src/store/api/authApi.ts`

```typescript
Endpoints:
- getAuthMethods()           // Check available auth methods
- passwordLogin()            // Email/password authentication
- passwordRegister()         // New user registration
- getAuthUrl()              // OAuth authorization URL
- getSession()              // Current session info
- handleCallback()          // OAuth callback processing
- logout()                  // End session
- refreshToken()            // Refresh auth token
```

#### 2. Organization API (`organizationApi`)
**File**: `frontend/src/store/api/organizationApi.ts`

```typescript
Endpoints:
- createOrganization()       // Create new org
- getOrganization()         // Get org by ID
- getOrganizationBySlug()   // Get org by slug
- getUserOrganizations()    // List user's orgs
- updateOrganization()      // Update org details
- updateSettings()          // Update org settings
- deleteOrganization()      // Delete org
- getOrganizationMembers()  // List members
- inviteUser()             // Send invitation
- acceptInvitation()       // Accept invite
- declineInvitation()      // Decline invite
- removeMember()           // Remove member
- updateMemberRole()       // Change member role
```

#### 3. Subscription API (`subscriptionApi`)
**File**: `frontend/src/store/api/subscriptionApi.ts`

```typescript
Endpoints:
- getOrganizationSubscription()  // Get subscription
- getAvailablePlans()            // List plans
- getOrganizationInvoices()      // List invoices
- createSubscription()           // New subscription
- changePlan()                   // Upgrade/downgrade
- cancelSubscription()           // Cancel sub
- reactivateSubscription()       // Reactivate
- getSubscriptionStatistics()    // Usage stats
```

#### 4. User API (`userApi`)
**File**: `frontend/src/store/api/userApi.ts`

```typescript
Endpoints:
- getCurrentUser()          // Get profile
- updateProfile()          // Update profile
- changePassword()         // Change password
- enable2FA()             // Setup 2FA
- disable2FA()            // Remove 2FA
- getSessions()           // List sessions
- revokeSession()         // End session
```

#### 5. Audit API (`auditApi`)
**File**: `frontend/src/store/api/auditApi.ts`

```typescript
Endpoints:
- getAuditLogs()          // Fetch audit logs
- getAuditLogDetail()     // Get single log
- exportAuditLogs()       // Export to CSV/JSON
- getComplianceReport()   // GDPR compliance
```

---

## Component Library

### UI Components

#### Form Components
- **LoadingButton**: Button with loading spinner
- **ErrorBoundary**: Error catching wrapper
- **FeedbackSystem**: Toast notifications
- **AutoSaveComponents**: Auto-saving forms

#### Display Components
- **StatsCard**: Metric display card
- **LoadingSpinner**: Loading indicator
- **PageHeader**: Page title header
- **EmptyState**: No data placeholder
- **ApiErrorDisplay**: Error message display

#### Layout Components
- **AuthLayout**: Authentication pages wrapper
- **DashboardLayout**: Main app layout with sidebar
- **AccessibleDashboardLayout**: WCAG-compliant layout

### Custom Hooks

```typescript
// Real-time updates
useRealTimeUpdates(updateFn, options)

// Auto-save functionality
useAutoSave(data, saveFunction, delay)

// Cross-component data sync
useCrossComponentSync()

// Accessibility features
useAccessibility()

// Notification system
useNotifications()
```

---

## State Management

### Redux Store Structure

```typescript
store: {
  // RTK Query API slices
  authApi: {...},
  organizationApi: {...},
  subscriptionApi: {...},
  userApi: {...},
  auditApi: {...},

  // Regular Redux slices
  auth: {
    user: User | null,
    isAuthenticated: boolean,
    loading: boolean
  },

  ui: {
    sidebarOpen: boolean,
    theme: 'light' | 'dark',
    notifications: Notification[]
  }
}
```

### Data Flow Patterns

1. **Authentication Flow**
   ```
   Login Form → authApi.passwordLogin →
   Update authSlice → Redirect to Dashboard
   ```

2. **Organization Creation Flow**
   ```
   Create Modal → organizationApi.createOrganization →
   Invalidate cache → Refetch list → Update UI
   ```

3. **Subscription Management Flow**
   ```
   Plan Selection → Stripe Payment →
   subscriptionApi.createSubscription →
   Webhook confirmation → Update status
   ```

---

## Security & Data Flow

### Authentication Security
- JWT tokens stored in httpOnly cookies
- PKCE flow for OAuth2 authentication
- Session-based authentication with opaque tokens
- Automatic token refresh before expiration
- XSS protection via React's built-in escaping
- CSRF tokens for state-changing operations

### Form Security
- Client-side validation with Zod schemas
- Server-side validation on all endpoints
- Input sanitization for XSS prevention
- File upload restrictions and validation
- Rate limiting on sensitive operations

### API Communication
- HTTPS-only in production
- Request/response interceptors for auth
- Automatic retry with exponential backoff
- Request deduplication via RTK Query
- Optimistic updates for better UX
- Cache invalidation strategies

### Data Protection
- No sensitive data in localStorage
- PII data encryption at rest
- Audit logging for compliance
- GDPR-compliant data handling
- Secure payment processing via Stripe

---

## Performance Optimizations

### Frontend Optimizations
- Code splitting by route
- Lazy loading of components
- Image optimization and lazy loading
- Bundle size optimization
- Virtual scrolling for large lists
- Memoization of expensive computations

### API Optimizations
- RTK Query caching
- Request batching
- Pagination for large datasets
- Debounced search inputs
- Prefetching on hover
- Background data refresh

### Real-time Features
- WebSocket connections for live updates
- Server-Sent Events as fallback
- Presence indicators
- Optimistic UI updates
- Conflict resolution strategies
- Connection retry logic

---

## Testing Coverage

### Unit Tests
- Component rendering tests
- Hook functionality tests
- Redux slice tests
- Utility function tests
- Form validation tests

### Integration Tests
- API integration tests
- Authentication flow tests
- Payment processing tests
- Data synchronization tests

### E2E Tests (Playwright)
```typescript
// Test scenarios
- User registration and login
- Organization creation and management
- Subscription purchase flow
- Settings update
- Invoice download
- Member invitation flow
```

---

## Deployment Configuration

### Environment Variables
```bash
VITE_API_BASE_URL=https://api.platform.com
VITE_STRIPE_PUBLISHABLE_KEY=pk_live_xxx
VITE_SENTRY_DSN=https://xxx@sentry.io/xxx
VITE_GA_TRACKING_ID=G-XXXXXXXXXX
```

### Build Process
```bash
npm run build         # Production build
npm run preview      # Preview production build
npm run test         # Run all tests
npm run test:e2e     # Run E2E tests
npm run analyze      # Bundle analysis
```

---

## Future Enhancements

### Planned Features
1. **Advanced Dashboard Analytics**
   - Custom date ranges
   - Export capabilities
   - Advanced filtering

2. **Team Collaboration**
   - Real-time collaboration
   - Comments and mentions
   - Activity feeds

3. **Mobile Application**
   - React Native implementation
   - Offline support
   - Push notifications

4. **API Management**
   - API key generation
   - Usage analytics
   - Rate limit monitoring

5. **Advanced Security**
   - Biometric authentication
   - Hardware key support
   - Advanced audit trails

---

*Document Version: 1.0.0*
*Last Updated: 2025-09-28*
*Platform: SASS Payment Management System*