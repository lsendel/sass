# Frontend Architecture Guide

_Comprehensive Documentation for Spring Boot Modulith Payment Platform Frontend_

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Feature Modules](#2-feature-modules)
3. [Data Flow Patterns](#3-data-flow-patterns)
4. [API Integration](#4-api-integration)
5. [Key User Journeys](#5-key-user-journeys)
6. [Component Library](#6-component-library)
7. [Implementation Guide](#7-implementation-guide)

---

## 1. System Overview

### Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │    Pages    │  │   Layouts   │  │ Components  │         │
│  │   (Routes)  │  │ (Structure) │  │(UI Elements)│         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                   APPLICATION LAYER                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │    Hooks    │  │  Services   │  │  Utilities  │         │
│  │ (Business)  │  │ (Complex)   │  │  (Helpers)  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                STATE MANAGEMENT LAYER                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │Redux Store  │  │RTK Query   │  │  Selectors  │         │
│  │(Client)     │  │(Server)     │  │ (Derived)   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                  DATA ACCESS LAYER                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ API Client  │  │    Cache    │  │ Validation  │         │
│  │    (RTK)    │  │ Management  │  │   (Zod)     │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                 INFRASTRUCTURE LAYER                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │    Build    │  │   Testing   │  │    Config   │         │
│  │   (Vite)    │  │(Vitest/PW)  │  │    (Env)    │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack Summary

- **Runtime**: React 19.1.1 + TypeScript 5.7.2
- **State**: Redux Toolkit + RTK Query
- **Routing**: React Router DOM 7.9.2
- **UI**: TailwindCSS 4.1.13 + HeadlessUI
- **Forms**: React Hook Form + Zod validation
- **Payments**: Stripe React components
- **Testing**: Vitest + Playwright + React Testing Library

---

## 2. Feature Modules

### 2.1 Authentication Module 🔐

#### Responsibility

User authentication, session management, and security flows.

#### File Structure

```
authentication/
├── pages/
│   ├── LoginPage.tsx           # Main login interface
│   ├── CallbackPage.tsx        # OAuth2 callback handler
│   └── MockLoginPage.tsx       # Development login
├── components/
│   ├── PasswordLoginForm.tsx   # Email/password form
│   ├── OAuth2LoginButtons.tsx  # Social login buttons
│   └── mfa/
│       ├── TOTPSetup.tsx       # TOTP configuration
│       └── MFAChallenge.tsx    # MFA verification
├── api/
│   ├── authApi.ts             # Authentication endpoints
│   └── mfaApi.ts              # MFA endpoints
├── types/
│   ├── auth.ts                # Auth-related types
│   └── mfa.ts                 # MFA types
└── store/
    └── authSlice.ts           # Auth client state
```

#### Key Components

**LoginPage** (`/auth/login`)

```typescript
// Purpose: Main authentication entry point
// Layout: AuthLayout
// Route: /auth/login

interface LoginPageProps {}

// API Dependencies:
const { data: authMethods } = useGetAuthMethodsQuery();
const [login, { isLoading, error }] = useLoginMutation();

// State Dependencies:
const isAuthenticated = useAppSelector(selectIsAuthenticated);
const authLoading = useAppSelector(selectAuthLoading);

// Components Used:
// - PasswordLoginForm (email/password)
// - OAuth2LoginButtons (social login)
// - LoadingSpinner (loading states)

// Navigation Flow:
// Success → /dashboard
// Error → Stay on login with error message
```

**PasswordLoginForm**

```typescript
interface PasswordLoginFormProps {
  onSubmit: (data: LoginFormData) => Promise<void>;
  isLoading?: boolean;
  error?: string | null;
}

// Form Schema (Zod):
const LoginFormSchema = z.object({
  email: z.string().email("Invalid email address"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  rememberMe: z.boolean().optional(),
});

// Integration:
// - React Hook Form for form management
// - Zod resolver for validation
// - Parent handles API submission
```

#### API Integration

**Authentication API** (`authApi.ts`)

```typescript
// Base URL: /api/v1/auth
// RTK Query slice for auth operations

export const authApi = createApi({
  reducerPath: "authApi",
  baseQuery: createValidatedBaseQuery("/api/v1/auth"),
  tagTypes: ["User", "Session"],
  endpoints: (builder) => ({
    // Get available auth methods
    getAuthMethods: builder.query<AuthMethodsResponse, void>({
      query: () => "/methods",
    }),

    // Login with email/password
    login: builder.mutation<LoginResponse, LoginRequest>({
      query: (credentials) => ({
        url: "/login",
        method: "POST",
        body: credentials,
      }),
      invalidatesTags: ["User", "Session"],
    }),

    // Get current session
    getSession: builder.query<SessionInfo, void>({
      query: () => "/session",
      providesTags: ["Session"],
    }),

    // Logout
    logout: builder.mutation<void, void>({
      query: () => ({
        url: "/logout",
        method: "POST",
      }),
      invalidatesTags: ["User", "Session"],
    }),
  }),
});
```

#### Data Flow

**Login Flow:**

```
1. User → LoginPage
2. Form input → PasswordLoginForm
3. Validation → Zod schema
4. Submit → authApi.useLoginMutation()
5. API call → POST /api/v1/auth/login
6. Success → authSlice.setCredentials()
7. Navigation → /dashboard
8. Cache → Session persistence
```

**Error Handling:**

- Form validation → Field-level errors
- API errors → Toast notifications
- Network errors → Retry mechanism
- Session expiry → Auto-logout + redirect

### 2.2 Organization Module 🏢

#### Responsibility

Multi-tenant organization management, member administration, and organization settings.

#### File Structure

```
organization/
├── pages/
│   ├── OrganizationsPage.tsx   # Organization list/selector
│   └── OrganizationPage.tsx    # Individual org dashboard
├── components/
│   ├── CreateOrganizationModal.tsx  # New org creation
│   ├── MemberList.tsx              # Organization members
│   ├── InviteMemberModal.tsx       # Member invitations
│   └── OrganizationSettings.tsx    # Org configuration
├── api/
│   └── organizationApi.ts      # Organization endpoints
└── types/
    └── organization.ts         # Organization types
```

#### Key Components

**OrganizationsPage** (`/organizations`)

```typescript
// Purpose: Organization selection and management hub
// Layout: DashboardLayout
// Route: /organizations

// API Dependencies:
const { data: organizations } = useGetOrganizationsQuery();
const { data: currentUser } = useGetCurrentUserQuery();

// Features:
// - List user's organizations
// - Create new organization (if permitted)
// - Switch between organizations
// - Organization quick stats
```

**OrganizationPage** (`/organizations/:slug`)

```typescript
// Purpose: Individual organization dashboard
// Route: /organizations/:slug

interface OrganizationPageParams {
  slug: string; // Organization slug from URL
}

// API Dependencies:
const { data: organization } = useGetOrganizationQuery(slug);
const { data: members } = useGetOrganizationMembersQuery(slug);
const { data: settings } = useGetOrganizationSettingsQuery(slug);

// Components:
// - OrganizationHeader (name, plan, stats)
// - MemberList (team members)
// - ActivityFeed (recent activities)
// - QuickActions (common tasks)
```

#### API Integration

**Organization API** (`organizationApi.ts`)

```typescript
// Base URL: /api/v1/organizations

export const organizationApi = createApi({
  reducerPath: "organizationApi",
  baseQuery: createValidatedBaseQuery("/api/v1/organizations"),
  tagTypes: ["Organization", "Member"],
  endpoints: (builder) => ({
    // Get user's organizations
    getOrganizations: builder.query<Organization[], void>({
      query: () => "",
      providesTags: ["Organization"],
    }),

    // Get specific organization
    getOrganization: builder.query<Organization, string>({
      query: (slug) => `/${slug}`,
      providesTags: (result, error, slug) => [
        { type: "Organization", id: slug },
      ],
    }),

    // Create new organization
    createOrganization: builder.mutation<
      Organization,
      CreateOrganizationRequest
    >({
      query: (data) => ({
        url: "",
        method: "POST",
        body: data,
      }),
      invalidatesTags: ["Organization"],
    }),

    // Get organization members
    getOrganizationMembers: builder.query<Member[], string>({
      query: (orgSlug) => `/${orgSlug}/members`,
      providesTags: ["Member"],
    }),
  }),
});
```

### 2.3 Payment Module 💳

#### Responsibility

Payment method management, transaction processing, and Stripe integration.

#### File Structure

```
payment/
├── pages/
│   └── PaymentsPage.tsx        # Payment management hub
├── components/
│   ├── PaymentMethodsModal.tsx     # Add payment methods
│   ├── AddPaymentMethodForm.tsx    # Stripe payment form
│   ├── PaymentHistory.tsx          # Transaction history
│   └── InternationalPaymentProcessor.tsx  # Global payments
├── api/
│   └── paymentApi.ts          # Payment endpoints
└── types/
    └── payment.ts             # Payment types
```

#### Key Components

**PaymentsPage** (`/payments`)

```typescript
// Purpose: Central payment management interface
// Layout: DashboardLayout
// Route: /payments

// API Dependencies:
const { data: paymentMethods } = useGetPaymentMethodsQuery();
const { data: transactions } = useGetTransactionsQuery();
const [setDefaultPaymentMethod] = useSetDefaultPaymentMethodMutation();

// Features:
// - View payment methods
// - Add new payment method (Stripe)
// - Set default payment method
// - View transaction history
// - Download invoices
```

**AddPaymentMethodForm**

```typescript
// Stripe integration component
interface AddPaymentMethodFormProps {
  onSuccess: (paymentMethod: PaymentMethod) => void;
  onCancel: () => void;
}

// Dependencies:
// - @stripe/react-stripe-js (Stripe Elements)
// - Stripe CardElement for secure input
// - Zod validation for billing details

// Security:
// - No direct card data handling
// - Stripe tokenization
// - PCI compliance through Stripe
```

#### API Integration & Stripe Flow

**Payment API + Stripe Integration:**

```typescript
// Payment method creation flow:
1. User → AddPaymentMethodForm (Stripe Elements)
2. Card details → Stripe tokenization
3. Token → paymentApi.createPaymentMethod()
4. Backend → Stripe Customer attachment
5. Success → Cache invalidation
6. UI update → Show new payment method

// API endpoints:
export const paymentApi = createApi({
  endpoints: (builder) => ({
    getPaymentMethods: builder.query<PaymentMethod[], void>({
      query: () => '/payment-methods',
      providesTags: ['PaymentMethod'],
    }),

    createPaymentMethod: builder.mutation<PaymentMethod, CreatePaymentMethodRequest>({
      query: (data) => ({
        url: '/payment-methods',
        method: 'POST',
        body: data,
      }),
      invalidatesTags: ['PaymentMethod'],
    }),
  }),
})
```

### 2.4 Subscription Module 📋

#### Responsibility

Subscription lifecycle management, plan selection, billing, and invoicing.

#### File Structure

```
subscription/
├── pages/
│   └── SubscriptionPage.tsx    # Subscription management
├── components/
│   ├── PlanSelector.tsx        # Plan comparison & selection
│   ├── BillingHistory.tsx      # Invoice history
│   ├── UsageMetrics.tsx        # Plan usage tracking
│   └── BillingAddress.tsx      # Billing information
├── api/
│   └── subscriptionApi.ts      # Subscription endpoints
└── types/
    └── subscription.ts         # Subscription types
```

#### Key Components

**SubscriptionPage** (`/subscription`)

```typescript
// Purpose: Subscription management hub
// Layout: DashboardLayout
// Route: /subscription

// API Dependencies:
const { data: currentSubscription } = useGetCurrentSubscriptionQuery();
const { data: availablePlans } = useGetPlansQuery();
const { data: invoices } = useGetInvoicesQuery();

// Features:
// - Current plan overview
// - Plan comparison & upgrade/downgrade
// - Billing history
// - Usage metrics
// - Invoice downloads
```

**PlanSelector**

```typescript
interface PlanSelectorProps {
  currentPlan: Plan;
  availablePlans: Plan[];
  onPlanSelect: (plan: Plan) => void;
}

// Features:
// - Plan comparison table
// - Feature highlighting
// - Pricing calculation
// - Upgrade/downgrade flows
// - Proration preview
```

---

## 3. Data Flow Patterns

### 3.1 State Management Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    REDUX STORE                              │
│                                                             │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │  CLIENT STATE   │    │        SERVER STATE             │ │
│  │                 │    │         (RTK Query)             │ │
│  │  ┌───────────┐  │    │  ┌─────────┐  ┌─────────────┐  │ │
│  │  │ authSlice │  │    │  │ authApi │  │ userApi     │  │ │
│  │  │ uiSlice   │  │    │  │ orgApi  │  │ paymentApi  │  │ │
│  │  └───────────┘  │    │  │ subApi  │  │ mfaApi      │  │ │
│  └─────────────────┘    │  └─────────┘  └─────────────┘  │ │
│                         └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Data Flow Patterns by Operation Type

#### Query Flow (Data Fetching)

```
Component
    ↓ useQuery hook
RTK Query API Slice
    ↓ HTTP request
Backend API
    ↓ Response
RTK Query Cache
    ↓ Normalized storage
Component Re-render
    ↓ UI update
User sees data
```

#### Mutation Flow (Data Modification)

```
Component
    ↓ useMutation hook
Form Validation (Zod)
    ↓ Valid data
RTK Query API Slice
    ↓ HTTP request
Backend API
    ↓ Response
Cache Invalidation
    ↓ Related queries refetch
Component Re-render
    ↓ UI update
Success/Error Feedback
```

#### Optimistic Update Flow

```
Component
    ↓ useMutation hook
Optimistic Cache Update
    ↓ Immediate UI update
Background API Call
    ↓ Real request
Success: Keep changes
Error: Revert changes
    ↓ Final UI state
```

### 3.3 Error Handling Flow

```
API Error
    ↓
RTK Query Error Processing
    ↓
Error Type Classification
    ├─ Validation Error (400)
    │     ↓ Form field errors
    ├─ Authentication Error (401)
    │     ↓ Redirect to login
    ├─ Authorization Error (403)
    │     ↓ Access denied message
    ├─ Not Found Error (404)
    │     ↓ Navigation to 404 page
    ├─ Rate Limit Error (429)
    │     ↓ Retry with backoff
    └─ Server Error (500)
          ↓ Generic error toast
```

### 3.4 Cache Management Strategy

**RTK Query Cache Configuration:**

```typescript
// Global cache settings
defaultSerializeQueryArgs: {
  keepUnusedDataFor: 300, // 5 minutes
  refetchOnMountOrArgChange: 30, // 30 seconds
  refetchOnFocus: true,
  refetchOnReconnect: true,
}

// Tag-based invalidation
tagTypes: [
  'User',           // User profile data
  'Session',        // Session information
  'Organization',   // Organization data
  'Member',         // Organization members
  'PaymentMethod',  // Payment methods
  'Transaction',    // Payment transactions
  'Subscription',   // Subscription data
  'Invoice',        // Billing invoices
]
```

**Cache Invalidation Patterns:**

```typescript
// Pessimistic updates (default)
loginUser: builder.mutation({
  invalidatesTags: ["User", "Session"], // Refetch after success
});

// Optimistic updates
updateProfile: builder.mutation({
  onQueryStarted: async (patch, { dispatch, queryFulfilled }) => {
    // Update cache immediately
    const patchResult = dispatch(
      userApi.util.updateQueryData("getCurrentUser", undefined, (draft) => {
        Object.assign(draft, patch);
      }),
    );

    try {
      await queryFulfilled;
    } catch {
      // Revert on error
      patchResult.undo();
    }
  },
});
```

---

## 4. API Integration

### 4.1 API Architecture Overview

```
Frontend (RTK Query)  ←→  Backend (Spring Boot)
     ↓                         ↓
API Slices               REST Controllers
     ↓                         ↓
Type Validation          Request Validation
     ↓                         ↓
HTTP Client              Business Logic
     ↓                         ↓
Error Handling           Error Responses
```

### 4.2 API Contract Documentation

#### Authentication API (`/api/v1/auth`)

**POST /api/v1/auth/login**

```typescript
// Request
interface LoginRequest {
  email: string; // Required, valid email
  password: string; // Required, min 8 characters
  rememberMe?: boolean; // Optional, default false
}

// Response (200)
interface LoginResponse {
  user: User; // User profile
  sessionId: string; // Session identifier
  expiresAt: string; // ISO datetime
}

// Errors
// 400: Validation errors → Form field errors
// 401: Invalid credentials → General error message
// 429: Rate limited → Retry after delay
// 500: Server error → Generic error toast
```

**GET /api/v1/auth/session**

```typescript
// Response (200)
interface SessionInfo {
  user: User;
  session: {
    id: string;
    expiresAt: string;
    lastActivity: string;
  };
}

// Errors
// 401: No session → Redirect to login
```

#### User API (`/api/v1/users`)

**GET /api/v1/users/me**

```typescript
// Response (200)
interface User {
  id: string; // UUID
  email: string; // Email address
  firstName?: string; // Optional first name
  lastName?: string; // Optional last name
  role: UserRole; // USER | ORGANIZATION_ADMIN | ADMIN
  emailVerified: boolean;
  organizationId?: string;
  createdAt: string; // ISO datetime
  updatedAt: string; // ISO datetime
  lastLoginAt?: string; // ISO datetime
}
```

#### Organization API (`/api/v1/organizations`)

**GET /api/v1/organizations**

```typescript
// Response (200)
interface Organization[] {
  id: string           // UUID
  name: string         // Organization name
  slug: string         // URL-friendly identifier
  plan: 'FREE' | 'PRO' | 'ENTERPRISE'
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED'
  billingEmail?: string
  maxUsers: number
  currentUsers: number
  createdAt: string
  updatedAt: string
}
```

### 4.3 Type Safety Strategy

**Runtime Validation with Zod:**

```typescript
// Define Zod schemas for API responses
export const UserSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email(),
  firstName: z.string().optional(),
  lastName: z.string().optional(),
  role: z.enum(["USER", "ORGANIZATION_ADMIN", "ADMIN"]),
  emailVerified: z.boolean(),
  organizationId: z.string().uuid().optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  lastLoginAt: z.string().datetime().optional(),
});

// Infer TypeScript types
export type User = z.infer<typeof UserSchema>;

// Validate responses
const validateResponse =
  <T>(schema: z.ZodSchema<T>) =>
  (response: unknown): T => {
    const result = schema.safeParse(response);
    if (!result.success) {
      throw new Error(`API response validation failed: ${result.error}`);
    }
    return result.data;
  };
```

**RTK Query Integration:**

```typescript
export const authApi = createApi({
  baseQuery: createValidatedBaseQuery("/api/v1/auth", {
    validateResponse: true, // Enable Zod validation
  }),
  endpoints: (builder) => ({
    getSession: builder.query<SessionInfo, void>({
      query: () => "/session",
      transformResponse: validateResponse(SessionInfoSchema),
    }),
  }),
});
```

---

## 5. Key User Journeys

### 5.1 Authentication Journey 🔐

**Goal:** Unauthenticated user successfully logs in and accesses dashboard

```
┌─────────────────┐
│   User lands    │
│   on app        │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Redirect to    │
│  /auth/login    │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐    ┌─────────────────┐
│  LoginPage      │───▶│ PasswordForm    │
│  loads auth     │    │ validates       │
│  methods        │    │ input           │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          ▼                      ▼
┌─────────────────┐    ┌─────────────────┐
│  OAuth2         │    │  Submit login   │
│  providers      │    │  credentials    │
│  displayed      │    │                 │
└─────────────────┘    └─────────┬───────┘
                                 │
                                 ▼
                       ┌─────────────────┐
                       │  API call       │
                       │  POST /login    │
                       └─────────┬───────┘
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
                    ▼            ▼            ▼
          ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
          │   Success   │ │   Error     │ │  MFA        │
          │   Response  │ │   Response  │ │  Required   │
          └─────┬───────┘ └─────┬───────┘ └─────┬───────┘
                │               │               │
                ▼               ▼               ▼
      ┌─────────────────┐ ┌─────────────┐ ┌─────────────┐
      │  Store user     │ │  Show error │ │  MFA        │
      │  in Redux       │ │  message    │ │  challenge  │
      └─────┬───────────┘ └─────────────┘ └─────┬───────┘
            │                                   │
            ▼                                   ▼
  ┌─────────────────┐                ┌─────────────────┐
  │  Navigate to    │                │  TOTP input     │
  │  /dashboard     │                │  validation     │
  └─────────────────┘                └─────┬───────────┘
                                           │
                                           ▼
                                 ┌─────────────────┐
                                 │  Success →      │
                                 │  Dashboard      │
                                 └─────────────────┘
```

**Technical Implementation:**

```typescript
// Journey: Authentication
// Components: LoginPage → PasswordLoginForm → DashboardPage
// APIs: authApi.useGetAuthMethodsQuery, authApi.useLoginMutation
// State: authSlice.setCredentials, navigation state
// Error Handling: Form validation, API errors, MFA flows
```

### 5.2 Organization Setup Journey 🏢

**Goal:** New user creates an organization and sets up their team

```
┌─────────────────┐
│  New user       │
│  authenticated  │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Dashboard      │
│  shows "Create  │
│  Organization"  │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  CreateOrg      │
│  Modal opens    │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Fill org       │
│  details form   │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Submit org     │
│  creation       │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Redirect to    │
│  org setup      │
│  wizard         │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Payment        │
│  method setup   │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Plan           │
│  selection      │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Team member    │
│  invitations    │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Setup          │
│  complete       │
└─────────────────┘
```

### 5.3 Payment Management Journey 💳

**Goal:** User adds a new payment method and sets it as default

```
┌─────────────────┐
│  Navigate to    │
│  /payments      │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  PaymentsPage   │
│  loads existing │
│  methods        │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Click "Add     │
│  Payment        │
│  Method"        │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Stripe modal   │
│  opens with     │
│  card form      │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  User enters    │
│  card details   │
│  (Stripe)       │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Stripe         │
│  tokenization   │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  API call with  │
│  token to       │
│  backend        │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Backend saves  │
│  to Stripe      │
│  Customer       │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Success        │
│  response       │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Cache          │
│  invalidation   │
│  & refetch      │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  UI updates     │
│  with new       │
│  method         │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│  Option to set  │
│  as default     │
└─────────────────┘
```

---

## 6. Component Library

### 6.1 Component Hierarchy

```
App (Root)
├── Providers
│   ├── Redux Provider
│   ├── Stripe Elements Provider
│   ├── React Router Provider
│   ├── Error Boundary
│   └── Notification Provider
├── Global Components
│   ├── Toaster (react-hot-toast)
│   └── Performance Dashboard (dev only)
└── Layout System
    ├── AuthLayout
    │   ├── Header (minimal)
    │   ├── Main Content
    │   │   ├── LoginPage
    │   │   └── CallbackPage
    │   └── Footer (optional)
    └── DashboardLayout
        ├── Navigation
        │   ├── Sidebar
        │   │   ├── Navigation Links
        │   │   ├── User Menu
        │   │   └── Organization Selector
        │   ├── Top Bar
        │   │   ├── Breadcrumbs
        │   │   ├── Search
        │   │   └── Notifications
        │   └── Mobile Menu
        ├── Main Content Area
        │   ├── Page Header
        │   ├── Page Content
        │   │   ├── DashboardPage
        │   │   ├── OrganizationsPage
        │   │   ├── PaymentsPage
        │   │   ├── SubscriptionPage
        │   │   └── SettingsPage
        │   └── Page Footer
        └── Global Modals
            ├── CreateOrganizationModal
            ├── PaymentMethodsModal
            └── Confirmation Modals
```

### 6.2 Core UI Components

#### Button Component

```typescript
interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'destructive'
  size?: 'sm' | 'md' | 'lg'
  isLoading?: boolean
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
}

// Usage:
<Button
  variant="primary"
  size="md"
  isLoading={isSubmitting}
  leftIcon={<PlusIcon />}
  onClick={handleSubmit}
>
  Create Organization
</Button>
```

#### Input Component

```typescript
interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helperText?: string
  leftAddon?: React.ReactNode
  rightAddon?: React.ReactNode
}

// Integration with React Hook Form:
<Controller
  name="email"
  control={control}
  render={({ field, fieldState }) => (
    <Input
      {...field}
      label="Email Address"
      type="email"
      error={fieldState.error?.message}
      placeholder="Enter your email"
    />
  )}
/>
```

#### Card Component

```typescript
interface CardProps {
  children: React.ReactNode
  className?: string
  padding?: 'none' | 'sm' | 'md' | 'lg'
  shadow?: 'none' | 'sm' | 'md' | 'lg'
}

// Usage:
<Card padding="lg" shadow="md">
  <Card.Header>
    <h2>Payment Methods</h2>
  </Card.Header>
  <Card.Content>
    {/* Card content */}
  </Card.Content>
  <Card.Footer>
    <Button>Add Method</Button>
  </Card.Footer>
</Card>
```

### 6.3 Form Components

#### Validation Strategy

```typescript
// Zod schema definition
const CreateOrganizationSchema = z.object({
  name: z.string().min(1, 'Organization name is required'),
  email: z.string().email('Invalid email address').optional(),
  plan: z.enum(['FREE', 'PRO', 'ENTERPRISE']),
})

type CreateOrganizationData = z.infer<typeof CreateOrganizationSchema>

// React Hook Form integration
const form = useForm<CreateOrganizationData>({
  resolver: zodResolver(CreateOrganizationSchema),
  defaultValues: {
    plan: 'FREE',
  },
})

// Form component
const CreateOrganizationForm: React.FC<CreateOrganizationFormProps> = ({
  onSubmit,
  isLoading,
}) => {
  return (
    <form onSubmit={form.handleSubmit(onSubmit)}>
      <Controller
        name="name"
        control={form.control}
        render={({ field, fieldState }) => (
          <Input
            {...field}
            label="Organization Name"
            error={fieldState.error?.message}
            placeholder="Acme Corp"
          />
        )}
      />

      <Controller
        name="plan"
        control={form.control}
        render={({ field }) => (
          <Select
            {...field}
            label="Plan"
            options={[
              { value: 'FREE', label: 'Free Plan' },
              { value: 'PRO', label: 'Pro Plan' },
              { value: 'ENTERPRISE', label: 'Enterprise Plan' },
            ]}
          />
        )}
      />

      <Button type="submit" isLoading={isLoading}>
        Create Organization
      </Button>
    </form>
  )
}
```

### 6.4 Data Display Components

#### Table Component

```typescript
interface TableProps<T> {
  data: T[]
  columns: ColumnDef<T>[]
  isLoading?: boolean
  pagination?: PaginationConfig
  sorting?: SortingConfig
  filtering?: FilteringConfig
}

// Usage example:
const PaymentMethodsTable: React.FC = () => {
  const { data: paymentMethods, isLoading } = useGetPaymentMethodsQuery()

  const columns: ColumnDef<PaymentMethod>[] = [
    {
      id: 'type',
      header: 'Type',
      cell: ({ row }) => (
        <div className="flex items-center">
          <CreditCardIcon className="w-4 h-4 mr-2" />
          {row.original.type}
        </div>
      ),
    },
    {
      id: 'last4',
      header: 'Last 4 Digits',
      cell: ({ row }) => `•••• ${row.original.last4}`,
    },
    {
      id: 'isDefault',
      header: 'Default',
      cell: ({ row }) => (
        row.original.isDefault ? (
          <Badge variant="success">Default</Badge>
        ) : null
      ),
    },
    {
      id: 'actions',
      header: 'Actions',
      cell: ({ row }) => (
        <DropdownMenu>
          <DropdownMenu.Trigger>
            <Button variant="ghost" size="sm">
              <MoreVerticalIcon className="w-4 h-4" />
            </Button>
          </DropdownMenu.Trigger>
          <DropdownMenu.Content>
            <DropdownMenu.Item onClick={() => setAsDefault(row.original.id)}>
              Set as Default
            </DropdownMenu.Item>
            <DropdownMenu.Item
              onClick={() => deletePaymentMethod(row.original.id)}
              className="text-red-600"
            >
              Delete
            </DropdownMenu.Item>
          </DropdownMenu.Content>
        </DropdownMenu>
      ),
    },
  ]

  return (
    <Table
      data={paymentMethods || []}
      columns={columns}
      isLoading={isLoading}
    />
  )
}
```

---

## 7. Implementation Guide

### 7.1 Development Workflow

#### Daily Development Commands

```bash
# Start development server
npm run dev

# Type checking
npm run typecheck

# Linting and formatting
npm run lint
npm run format

# Testing
npm run test        # Unit tests
npm run test:e2e    # E2E tests
npm run test:ui     # Test UI

# Build
npm run build
npm run preview
```

#### Adding New Features

**1. Create Feature Module Structure**

```bash
# Example: Adding a new "Reports" feature
mkdir -p src/pages/reports
mkdir -p src/components/reports
mkdir -p src/store/api
touch src/store/api/reportsApi.ts
touch src/types/reports.ts
```

**2. Define Types & Schemas**

```typescript
// src/types/reports.ts
export interface Report {
  id: string;
  name: string;
  type: ReportType;
  createdAt: string;
  data: ReportData;
}

// Zod schema for validation
export const ReportSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  type: z.enum(["FINANCIAL", "USAGE", "ANALYTICS"]),
  createdAt: z.string().datetime(),
  data: z.record(z.unknown()),
});
```

**3. Create API Slice**

```typescript
// src/store/api/reportsApi.ts
export const reportsApi = createApi({
  reducerPath: "reportsApi",
  baseQuery: createValidatedBaseQuery("/api/v1/reports"),
  tagTypes: ["Report"],
  endpoints: (builder) => ({
    getReports: builder.query<Report[], void>({
      query: () => "",
      providesTags: ["Report"],
    }),
    generateReport: builder.mutation<Report, GenerateReportRequest>({
      query: (data) => ({
        url: "/generate",
        method: "POST",
        body: data,
      }),
      invalidatesTags: ["Report"],
    }),
  }),
});
```

**4. Create Page Component**

```typescript
// src/pages/reports/ReportsPage.tsx
const ReportsPage: React.FC = () => {
  const { data: reports, isLoading, error } = useGetReportsQuery()
  const [generateReport] = useGenerateReportMutation()

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage error={error} />

  return (
    <div>
      <PageHeader
        title="Reports"
        action={
          <Button onClick={() => generateReport({ type: 'FINANCIAL' })}>
            Generate Report
          </Button>
        }
      />
      <ReportsGrid reports={reports || []} />
    </div>
  )
}
```

**5. Add to Store Configuration**

```typescript
// src/store/index.ts
import { reportsApi } from "./api/reportsApi";

export const store = configureStore({
  reducer: {
    // ... existing reducers
    [reportsApi.reducerPath]: reportsApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
      // ... existing middleware
      .concat(reportsApi.middleware),
});
```

**6. Add to Routing**

```typescript
// src/App.tsx
import ReportsPage from './pages/reports/ReportsPage'

// In routes:
<Route path="reports" element={<ReportsPage />} />
```

### 7.2 Testing Strategy

#### Unit Testing Pattern

```typescript
// src/components/reports/__tests__/ReportsGrid.test.tsx
import { render, screen } from '@testing-library/react'
import { Provider } from 'react-redux'
import { store } from '@/store'
import { ReportsGrid } from '../ReportsGrid'

const renderWithProviders = (ui: React.ReactElement) => {
  return render(
    <Provider store={store}>
      {ui}
    </Provider>
  )
}

describe('ReportsGrid', () => {
  it('displays reports correctly', () => {
    const mockReports = [
      {
        id: '1',
        name: 'Financial Report',
        type: 'FINANCIAL',
        createdAt: '2023-01-01T00:00:00Z',
        data: {},
      },
    ]

    renderWithProviders(<ReportsGrid reports={mockReports} />)

    expect(screen.getByText('Financial Report')).toBeInTheDocument()
  })

  it('handles empty state', () => {
    renderWithProviders(<ReportsGrid reports={[]} />)

    expect(screen.getByText('No reports found')).toBeInTheDocument()
  })
})
```

#### E2E Testing Pattern

```typescript
// tests/e2e/reports.spec.ts
import { test, expect } from "@playwright/test";

test.describe("Reports Flow", () => {
  test.beforeEach(async ({ page }) => {
    // Login and navigate to reports
    await page.goto("/auth/login");
    await page.fill("[data-testid=email]", "test@example.com");
    await page.fill("[data-testid=password]", "password123");
    await page.click("[data-testid=login-button]");
    await page.waitForURL("/dashboard");
    await page.click("[data-testid=nav-reports]");
  });

  test("generates new report", async ({ page }) => {
    await expect(page).toHaveURL("/reports");

    await page.click("[data-testid=generate-report-button]");
    await page.click("[data-testid=report-type-financial]");
    await page.click("[data-testid=confirm-generate]");

    await expect(page.locator("[data-testid=report-item]")).toHaveCount(1);
  });
});
```

### 7.3 Performance Optimization

#### Code Splitting

```typescript
// Lazy load pages
const ReportsPage = lazy(() => import('./pages/reports/ReportsPage'))
const AnalyticsPage = lazy(() => import('./pages/analytics/AnalyticsPage'))

// In routes:
<Route
  path="reports"
  element={
    <Suspense fallback={<LoadingSpinner />}>
      <ReportsPage />
    </Suspense>
  }
/>
```

#### Memoization

```typescript
// Expensive calculations
const expensiveCalculation = useMemo(() => {
  return reports.reduce((acc, report) => {
    return acc + calculateReportValue(report)
  }, 0)
}, [reports])

// Component memoization
const ReportCard = memo<ReportCardProps>(({ report }) => {
  return (
    <Card>
      <h3>{report.name}</h3>
      <p>{report.type}</p>
    </Card>
  )
})
```

#### RTK Query Optimization

```typescript
// Prefetch on hover
const handleReportHover = (reportId: string) => {
  dispatch(reportsApi.util.prefetch("getReportDetails", reportId));
};

// Background refetching
const { data: reports } = useGetReportsQuery(undefined, {
  pollingInterval: 30000, // Poll every 30 seconds
  refetchOnFocus: true,
  refetchOnReconnect: true,
});
```

### 7.4 Security Best Practices

#### Input Validation

```typescript
// Always validate with Zod
const validateInput = <T>(schema: z.ZodSchema<T>, input: unknown): T => {
  const result = schema.safeParse(input);
  if (!result.success) {
    throw new ValidationError(result.error.errors);
  }
  return result.data;
};

// Sanitize user input
const sanitizeHtml = (html: string): string => {
  return DOMPurify.sanitize(html);
};
```

#### Authentication Headers

```typescript
// RTK Query base query with auth
const baseQueryWithAuth = fetchBaseQuery({
  baseUrl: "/api/v1",
  prepareHeaders: (headers, { getState }) => {
    // Session-based auth (no tokens in localStorage)
    headers.set("X-Requested-With", "XMLHttpRequest");
    return headers;
  },
});
```

#### CSRF Protection

```typescript
// CSRF token handling
const baseQueryWithCSRF = fetchBaseQuery({
  baseUrl: "/api/v1",
  prepareHeaders: (headers) => {
    const csrfToken = document
      .querySelector('meta[name="csrf-token"]')
      ?.getAttribute("content");
    if (csrfToken) {
      headers.set("X-CSRF-Token", csrfToken);
    }
    return headers;
  },
});
```

---

## Conclusion

This comprehensive frontend architecture guide provides a complete blueprint for understanding and working with the React TypeScript frontend of the Spring Boot Modulith Payment Platform. The hybrid approach combining feature modules, data flow patterns, API contracts, and user journeys ensures both technical clarity and business context.

**Key Takeaways:**

- **Modular Architecture**: Clear separation of concerns with feature-based modules
- **Type Safety**: Comprehensive TypeScript + Zod validation throughout
- **State Management**: RTK Query for server state, Redux for client state
- **Performance**: Code splitting, memoization, and intelligent caching
- **Testing**: Comprehensive unit, integration, and E2E testing
- **Security**: Input validation, secure API integration, CSRF protection

This documentation serves as both a reference guide and implementation blueprint for developers and LLMs working with this codebase.
