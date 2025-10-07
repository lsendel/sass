# Frontend Architecture Diagrams & Visualizations

_Visual Documentation for Spring Boot Modulith Payment Platform Frontend_

## Table of Contents

1. [System Architecture Overview](#1-system-architecture-overview)
2. [Application Flow Diagrams](#2-application-flow-diagrams)
3. [Component Hierarchy Diagrams](#3-component-hierarchy-diagrams)
4. [Data Flow Visualizations](#4-data-flow-visualizations)
5. [API Integration Diagrams](#5-api-integration-diagrams)
6. [User Journey Flowcharts](#6-user-journey-flowcharts)
7. [State Management Diagrams](#7-state-management-diagrams)

---

## 1. System Architecture Overview

### 1.1 High-Level Architecture

```mermaid
graph TB
    subgraph "Browser Environment"
        subgraph "React Application"
            UI[UI Components]
            Pages[Page Components]
            Layouts[Layout Components]
        end

        subgraph "State Management"
            Redux[Redux Store]
            RTK[RTK Query]
            Cache[API Cache]
        end

        subgraph "Infrastructure"
            Router[React Router]
            Stripe[Stripe Elements]
            Forms[React Hook Form]
        end
    end

    subgraph "External Services"
        Backend[Spring Boot Backend]
        StripeAPI[Stripe API]
    end

    UI --> Redux
    Pages --> RTK
    RTK --> Cache
    RTK --> Backend
    Stripe --> StripeAPI
    Forms --> UI
    Router --> Pages

    classDef frontend fill:#e1f5fe
    classDef state fill:#f3e5f5
    classDef external fill:#fff3e0

    class UI,Pages,Layouts frontend
    class Redux,RTK,Cache state
    class Backend,StripeAPI external
```

### 1.2 Layer Architecture

```
╔══════════════════════════════════════════════════════════════╗
║                      PRESENTATION LAYER                      ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  ║
║  │    Pages    │  │   Layouts   │  │    UI Components    │  ║
║  │  (Routes)   │  │ (Structure) │  │   (Reusable UI)     │  ║
║  │             │  │             │  │                     │  ║
║  │ • LoginPage │  │ • AuthLayout│  │ • Button            │  ║
║  │ • Dashboard │  │ • DashLayout│  │ • Input             │  ║
║  │ • Payments  │  │             │  │ • Card              │  ║
║  │ • Settings  │  │             │  │ • Modal             │  ║
║  └─────────────┘  └─────────────┘  └─────────────────────┘  ║
╚══════════════════════════════════════════════════════════════╝
╔══════════════════════════════════════════════════════════════╗
║                     APPLICATION LAYER                        ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  ║
║  │    Hooks    │  │  Services   │  │     Utilities       │  ║
║  │ (Business)  │  │ (Complex)   │  │    (Helpers)        │  ║
║  │             │  │             │  │                     │  ║
║  │ • useAuth   │  │ • authSvc   │  │ • validation        │  ║
║  │ • useOrg    │  │ • paymentSvc│  │ • errorHandling     │  ║
║  │ • usePay    │  │ • subSvc    │  │ • formatting        │  ║
║  └─────────────┘  └─────────────┘  └─────────────────────┘  ║
╚══════════════════════════════════════════════════════════════╝
╔══════════════════════════════════════════════════════════════╗
║                  STATE MANAGEMENT LAYER                      ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  ║
║  │Redux Store  │  │ RTK Query   │  │     Selectors       │  ║
║  │(Client)     │  │ (Server)    │  │   (Derived)         │  ║
║  │             │  │             │  │                     │  ║
║  │ • authSlice │  │ • authApi   │  │ • selectUser        │  ║
║  │ • uiSlice   │  │ • userApi   │  │ • selectOrgs        │  ║
║  │             │  │ • orgApi    │  │ • selectLoading     │  ║
║  │             │  │ • payApi    │  │ • selectErrors      │  ║
║  └─────────────┘  └─────────────┘  └─────────────────────┘  ║
╚══════════════════════════════════════════════════════════════╝
╔══════════════════════════════════════════════════════════════╗
║                    DATA ACCESS LAYER                         ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  ║
║  │ API Client  │  │    Cache    │  │    Validation       │  ║
║  │    (HTTP)   │  │ Management  │  │      (Zod)          │  ║
║  │             │  │             │  │                     │  ║
║  │ • baseQuery │  │ • tag cache │  │ • UserSchema        │  ║
║  │ • auth      │  │ • timeout   │  │ • OrgSchema         │  ║
║  │ • retry     │  │ • invalidate│  │ • PaymentSchema     │  ║
║  └─────────────┘  └─────────────┘  └─────────────────────┘  ║
╚══════════════════════════════════════════════════════════════╝
```

### 1.3 Module Dependencies

```mermaid
graph LR
    subgraph "Frontend Modules"
        Auth[Authentication Module]
        Org[Organization Module]
        Payment[Payment Module]
        Sub[Subscription Module]
        Shared[Shared Module]
    end

    subgraph "Backend APIs"
        AuthAPI[Auth API]
        UserAPI[User API]
        OrgAPI[Organization API]
        PayAPI[Payment API]
        SubAPI[Subscription API]
    end

    Auth --> AuthAPI
    Auth --> UserAPI
    Org --> OrgAPI
    Org --> UserAPI
    Payment --> PayAPI
    Sub --> SubAPI

    Auth --> Shared
    Org --> Shared
    Payment --> Shared
    Sub --> Shared

    Org -.->|depends on| Auth
    Payment -.->|depends on| Auth
    Sub -.->|depends on| Auth
    Sub -.->|depends on| Payment

    classDef module fill:#e3f2fd
    classDef api fill:#fff3e0
    classDef shared fill:#f1f8e9

    class Auth,Org,Payment,Sub module
    class AuthAPI,UserAPI,OrgAPI,PayAPI,SubAPI api
    class Shared shared
```

---

## 2. Application Flow Diagrams

### 2.1 Application Bootstrap Flow

```mermaid
sequenceDiagram
    participant Browser
    participant App
    participant Redux
    participant RTK
    participant Backend

    Browser->>App: Load application
    App->>Redux: Initialize store
    Redux->>RTK: Setup API slices
    App->>App: Check authentication

    alt User has session
        App->>RTK: getSession query
        RTK->>Backend: GET /api/v1/auth/session
        Backend-->>RTK: Session data
        RTK-->>Redux: Update auth state
        Redux-->>App: User authenticated
        App->>App: Navigate to /dashboard
    else No session
        App->>App: Navigate to /auth/login
    end

    App->>Browser: Render application
```

### 2.2 Page Navigation Flow

```mermaid
stateDiagram-v2
    [*] --> Loading
    Loading --> Unauthenticated : No session
    Loading --> Authenticated : Valid session

    state Unauthenticated {
        [*] --> LoginPage
        LoginPage --> CallbackPage : OAuth2 flow
        CallbackPage --> LoginPage : Auth error
        LoginPage --> MockLoginPage : Dev mode
    }

    state Authenticated {
        [*] --> DashboardPage
        DashboardPage --> OrganizationsPage
        DashboardPage --> PaymentsPage
        DashboardPage --> SubscriptionPage
        DashboardPage --> SettingsPage

        OrganizationsPage --> OrganizationDetailPage
        PaymentsPage --> PaymentMethodModal
        SubscriptionPage --> PlanSelectionModal
    }

    Unauthenticated --> Authenticated : Successful login
    Authenticated --> Unauthenticated : Logout
```

---

## 3. Component Hierarchy Diagrams

### 3.1 Complete Component Tree

```
App
├── Providers
│   ├── Provider (Redux)
│   ├── Elements (Stripe)
│   ├── BrowserRouter
│   ├── ErrorBoundary
│   └── NotificationProvider
├── AppContent
│   └── Routes
│       ├── AuthLayout
│       │   ├── Header (minimal)
│       │   ├── Outlet
│       │   │   ├── LoginPage
│       │   │   │   ├── PasswordLoginForm
│       │   │   │   │   ├── Input (email)
│       │   │   │   │   ├── Input (password)
│       │   │   │   │   ├── Checkbox (remember)
│       │   │   │   │   └── Button (submit)
│       │   │   │   ├── OAuth2LoginButtons
│       │   │   │   │   ├── Button (Google)
│       │   │   │   │   ├── Button (GitHub)
│       │   │   │   │   └── Button (Microsoft)
│       │   │   │   └── LoadingSpinner
│       │   │   └── CallbackPage
│       │   │       ├── CallbackHandler
│       │   │       └── LoadingSpinner
│       │   └── Footer (optional)
│       └── DashboardLayout
│           ├── Sidebar
│           │   ├── Logo
│           │   ├── NavigationMenu
│           │   │   ├── NavLink (Dashboard)
│           │   │   ├── NavLink (Organizations)
│           │   │   ├── NavLink (Payments)
│           │   │   ├── NavLink (Subscription)
│           │   │   └── NavLink (Settings)
│           │   ├── OrganizationSelector
│           │   └── UserMenu
│           │       ├── UserAvatar
│           │       ├── Dropdown
│           │       │   ├── ProfileLink
│           │       │   ├── SettingsLink
│           │       │   └── LogoutButton
│           ├── TopBar
│           │   ├── Breadcrumbs
│           │   ├── SearchBox
│           │   └── NotificationBell
│           ├── MainContent
│           │   ├── PageHeader
│           │   │   ├── Title
│           │   │   ├── Description
│           │   │   └── Actions
│           │   └── Outlet
│           │       ├── DashboardPage
│           │       │   ├── StatsCards
│           │       │   │   ├── StatsCard (Users)
│           │       │   │   ├── StatsCard (Revenue)
│           │       │   │   └── StatsCard (Subscriptions)
│           │       │   ├── RecentActivity
│           │       │   └── QuickActions
│           │       ├── OrganizationsPage
│           │       │   ├── OrganizationGrid
│           │       │   │   └── OrganizationCard[]
│           │       │   └── CreateOrgButton
│           │       ├── OrganizationPage
│           │       │   ├── OrganizationHeader
│           │       │   ├── TabNavigation
│           │       │   ├── MembersList
│           │       │   │   └── MemberCard[]
│           │       │   └── Settings
│           │       ├── PaymentsPage
│           │       │   ├── PaymentMethodsList
│           │       │   │   └── PaymentMethodCard[]
│           │       │   ├── TransactionHistory
│           │       │   │   └── TransactionRow[]
│           │       │   └── AddPaymentButton
│           │       ├── SubscriptionPage
│           │       │   ├── CurrentPlanCard
│           │       │   ├── PlanComparison
│           │       │   │   └── PlanCard[]
│           │       │   └── BillingHistory
│           │       │       └── InvoiceRow[]
│           │       └── SettingsPage
│           │           ├── ProfileSettings
│           │           ├── SecuritySettings
│           │           ├── NotificationSettings
│           │           └── DangerZone
│           └── GlobalModals
│               ├── CreateOrganizationModal
│               │   └── CreateOrganizationForm
│               ├── PaymentMethodModal
│               │   └── AddPaymentMethodForm
│               │       └── StripeCardElement
│               └── ConfirmationModal
├── Toaster (react-hot-toast)
└── PerformanceDashboard (dev only)
```

### 3.2 Authentication Components

```mermaid
graph TB
    subgraph "Authentication Components"
        LoginPage[LoginPage]
        PasswordForm[PasswordLoginForm]
        OAuth2Buttons[OAuth2LoginButtons]
        CallbackPage[CallbackPage]
        MFAChallenge[MFAChallenge]
        TOTPSetup[TOTPSetup]
    end

    subgraph "Shared UI Components"
        Input[Input]
        Button[Button]
        LoadingSpinner[LoadingSpinner]
        ErrorMessage[ErrorMessage]
    end

    LoginPage --> PasswordForm
    LoginPage --> OAuth2Buttons
    LoginPage --> LoadingSpinner

    PasswordForm --> Input
    PasswordForm --> Button
    PasswordForm --> ErrorMessage

    OAuth2Buttons --> Button

    CallbackPage --> LoadingSpinner
    CallbackPage --> MFAChallenge

    MFAChallenge --> Input
    MFAChallenge --> Button

    TOTPSetup --> Input
    TOTPSetup --> Button

    classDef auth fill:#e8f5e8
    classDef ui fill:#fff3e0

    class LoginPage,PasswordForm,OAuth2Buttons,CallbackPage,MFAChallenge,TOTPSetup auth
    class Input,Button,LoadingSpinner,ErrorMessage ui
```

### 3.3 Payment Components

```mermaid
graph TB
    subgraph "Payment Components"
        PaymentsPage[PaymentsPage]
        PaymentMethodsList[PaymentMethodsList]
        PaymentMethodCard[PaymentMethodCard]
        AddPaymentMethodForm[AddPaymentMethodForm]
        PaymentMethodModal[PaymentMethodModal]
        TransactionHistory[TransactionHistory]
    end

    subgraph "Stripe Components"
        StripeCardElement[CardElement]
        StripeElements[Elements]
    end

    subgraph "Shared Components"
        Card[Card]
        Button[Button]
        Modal[Modal]
        Table[Table]
    end

    PaymentsPage --> PaymentMethodsList
    PaymentsPage --> TransactionHistory
    PaymentsPage --> PaymentMethodModal

    PaymentMethodsList --> PaymentMethodCard
    PaymentMethodCard --> Card
    PaymentMethodCard --> Button

    PaymentMethodModal --> Modal
    PaymentMethodModal --> AddPaymentMethodForm

    AddPaymentMethodForm --> StripeElements
    StripeElements --> StripeCardElement

    TransactionHistory --> Table

    classDef payment fill:#e3f2fd
    classDef stripe fill:#f3e5f5
    classDef shared fill:#fff3e0

    class PaymentsPage,PaymentMethodsList,PaymentMethodCard,AddPaymentMethodForm,PaymentMethodModal,TransactionHistory payment
    class StripeCardElement,StripeElements stripe
    class Card,Button,Modal,Table shared
```

---

## 4. Data Flow Visualizations

### 4.1 Redux State Flow

```mermaid
graph TB
    subgraph "Components"
        LoginPage[LoginPage]
        Dashboard[Dashboard]
        PaymentsPage[PaymentsPage]
    end

    subgraph "Redux Store"
        subgraph "Client State"
            AuthSlice[authSlice]
            UISlice[uiSlice]
        end

        subgraph "Server State (RTK Query)"
            AuthAPI[authApi]
            UserAPI[userApi]
            PaymentAPI[paymentApi]
            Cache[API Cache]
        end
    end

    subgraph "Backend"
        AuthService[Auth Service]
        PaymentService[Payment Service]
    end

    LoginPage -->|useAppDispatch| AuthSlice
    LoginPage -->|useLoginMutation| AuthAPI

    Dashboard -->|useAppSelector| AuthSlice
    Dashboard -->|useGetUserQuery| UserAPI

    PaymentsPage -->|useGetPaymentMethodsQuery| PaymentAPI

    AuthAPI -->|HTTP Request| AuthService
    PaymentAPI -->|HTTP Request| PaymentService

    AuthService -->|Response| AuthAPI
    PaymentService -->|Response| PaymentAPI

    AuthAPI -->|Cache Update| Cache
    PaymentAPI -->|Cache Update| Cache

    Cache -->|Subscription| Dashboard
    Cache -->|Subscription| PaymentsPage

    classDef component fill:#e1f5fe
    classDef state fill:#f3e5f5
    classDef api fill:#e8f5e8
    classDef backend fill:#fff3e0

    class LoginPage,Dashboard,PaymentsPage component
    class AuthSlice,UISlice state
    class AuthAPI,UserAPI,PaymentAPI,Cache api
    class AuthService,PaymentService backend
```

### 4.2 Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant LoginPage
    participant PasswordForm
    participant AuthAPI
    participant Backend
    participant Redux
    participant Dashboard

    User->>LoginPage: Navigate to /auth/login
    LoginPage->>PasswordForm: Render form
    User->>PasswordForm: Enter credentials
    PasswordForm->>PasswordForm: Validate with Zod

    alt Valid input
        PasswordForm->>AuthAPI: useLoginMutation()
        AuthAPI->>Backend: POST /api/v1/auth/login

        alt Successful login
            Backend-->>AuthAPI: User + Session data
            AuthAPI-->>Redux: Update auth state
            Redux-->>LoginPage: Authentication success
            LoginPage->>Dashboard: Navigate to /dashboard
        else Login failure
            Backend-->>AuthAPI: Error response
            AuthAPI-->>PasswordForm: Display error
        end
    else Invalid input
        PasswordForm->>PasswordForm: Display field errors
    end
```

### 4.3 Payment Method Addition Flow

```mermaid
sequenceDiagram
    participant User
    participant PaymentsPage
    participant Modal
    participant StripeForm
    participant Stripe
    participant PaymentAPI
    participant Backend

    User->>PaymentsPage: Click "Add Payment Method"
    PaymentsPage->>Modal: Open modal
    Modal->>StripeForm: Render Stripe form
    User->>StripeForm: Enter card details
    StripeForm->>Stripe: Tokenize card

    alt Tokenization success
        Stripe-->>StripeForm: Return token
        StripeForm->>PaymentAPI: Submit token + billing details
        PaymentAPI->>Backend: POST /api/v1/payment-methods
        Backend->>Backend: Save to Stripe Customer
        Backend-->>PaymentAPI: Payment method created
        PaymentAPI->>PaymentAPI: Invalidate cache tags
        PaymentAPI-->>PaymentsPage: Refetch payment methods
        PaymentsPage->>Modal: Close modal
        PaymentsPage->>PaymentsPage: Show success message
    else Tokenization failure
        Stripe-->>StripeForm: Return error
        StripeForm->>StripeForm: Display error
    end
```

### 4.4 RTK Query Cache Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    RTK Query Cache Management               │
└─────────────────────────────────────────────────────────────┘

Query Request
     ↓
┌─────────────┐    Cache Hit?    ┌──────────────┐
│  Component  │ ────────────────▶│    Cache     │
└─────────────┘                  └──────────────┘
     │                                  │
     │ No                              │ Yes
     ▼                                  ▼
┌─────────────┐                  ┌──────────────┐
│   Network   │                  │ Return Data  │
│   Request   │                  │   + Status   │
└─────────────┘                  └──────────────┘
     │
     ▼
┌─────────────┐
│  Response   │
│  Processing │
└─────────────┘
     │
     ▼
┌─────────────┐    Tag-based     ┌──────────────┐
│ Cache Store │ ◀─ Invalidation ─│   Mutation   │
└─────────────┘                  └──────────────┘
     │
     ▼
┌─────────────┐
│ Component   │
│ Re-render   │
└─────────────┘
```

---

## 5. API Integration Diagrams

### 5.1 API Architecture Overview

```mermaid
graph LR
    subgraph "Frontend (RTK Query)"
        AuthAPI[authApi]
        UserAPI[userApi]
        OrgAPI[organizationApi]
        PayAPI[paymentApi]
        SubAPI[subscriptionApi]
        MFAApi[mfaApi]
        AnalyticsAPI[analyticsApi]
    end

    subgraph "Backend (Spring Boot)"
        AuthController[AuthController]
        UserController[UserController]
        OrgController[OrganizationController]
        PayController[PaymentController]
        SubController[SubscriptionController]
    end

    subgraph "External Services"
        StripeAPI[Stripe API]
        OAuth2Providers[OAuth2 Providers]
    end

    AuthAPI -->|/api/v1/auth| AuthController
    UserAPI -->|/api/v1/users| UserController
    OrgAPI -->|/api/v1/organizations| OrgController
    PayAPI -->|/api/v1/payments| PayController
    SubAPI -->|/api/v1/subscriptions| SubController

    AuthController -.->|OAuth2| OAuth2Providers
    PayController -.->|Payments| StripeAPI

    classDef frontend fill:#e3f2fd
    classDef backend fill:#e8f5e8
    classDef external fill:#fff3e0

    class AuthAPI,UserAPI,OrgAPI,PayAPI,SubAPI,MFAApi,AnalyticsAPI frontend
    class AuthController,UserController,OrgController,PayController,SubController backend
    class StripeAPI,OAuth2Providers external
```

### 5.2 API Request/Response Flow

```mermaid
sequenceDiagram
    participant Component
    participant RTKQuery
    participant Middleware
    participant BaseQuery
    participant Backend
    participant Database

    Component->>RTKQuery: useQuery() / useMutation()
    RTKQuery->>RTKQuery: Check cache

    alt Cache miss or mutation
        RTKQuery->>Middleware: Process request
        Middleware->>BaseQuery: Prepare HTTP request
        BaseQuery->>BaseQuery: Add headers & auth
        BaseQuery->>Backend: HTTP request
        Backend->>Database: Data operation
        Database-->>Backend: Data response
        Backend-->>BaseQuery: HTTP response
        BaseQuery->>BaseQuery: Validate response (Zod)
        BaseQuery-->>Middleware: Processed response
        Middleware-->>RTKQuery: Update cache
        RTKQuery-->>Component: Data + loading state
    else Cache hit
        RTKQuery-->>Component: Cached data + loading state
    end
```

### 5.3 Error Handling Flow

```mermaid
graph TB
    APIError[API Error Response]

    APIError --> ErrorClassification{Error Classification}

    ErrorClassification -->|400| ValidationError[Validation Error]
    ErrorClassification -->|401| AuthError[Authentication Error]
    ErrorClassification -->|403| AuthzError[Authorization Error]
    ErrorClassification -->|404| NotFoundError[Not Found Error]
    ErrorClassification -->|429| RateLimitError[Rate Limit Error]
    ErrorClassification -->|500| ServerError[Server Error]

    ValidationError --> FormFieldErrors[Show field-level errors]
    AuthError --> RedirectLogin[Redirect to login]
    AuthzError --> AccessDenied[Show access denied message]
    NotFoundError --> NotFoundPage[Navigate to 404 page]
    RateLimitError --> RetryWithBackoff[Retry with exponential backoff]
    ServerError --> GenericToast[Show generic error toast]

    FormFieldErrors --> UserFeedback[User sees specific field errors]
    RedirectLogin --> LoginFlow[User re-authenticates]
    AccessDenied --> UserFeedback
    NotFoundPage --> UserFeedback
    RetryWithBackoff --> AutoRetry[Automatic retry attempt]
    GenericToast --> UserFeedback

    classDef error fill:#ffebee
    classDef handling fill:#e8f5e8
    classDef outcome fill:#e3f2fd

    class APIError,ValidationError,AuthError,AuthzError,NotFoundError,RateLimitError,ServerError error
    class ErrorClassification,FormFieldErrors,RedirectLogin,AccessDenied,NotFoundPage,RetryWithBackoff,GenericToast handling
    class UserFeedback,LoginFlow,AutoRetry outcome
```

### 5.4 Type Safety & Validation

```
┌─────────────────────────────────────────────────────────────┐
│                    Type Safety Pipeline                     │
└─────────────────────────────────────────────────────────────┘

Backend Response
       ↓
┌─────────────┐
│   Network   │ ← HTTP Response (unknown type)
│   Layer     │
└─────────────┘
       ↓
┌─────────────┐
│    Zod      │ ← Runtime validation
│ Validation  │   schema.safeParse(response)
└─────────────┘
       ↓                    ↓
  ✅ Valid              ❌ Invalid
       ↓                    ↓
┌─────────────┐       ┌─────────────┐
│ TypeScript  │       │ Validation  │
│  Inference  │       │   Error     │
└─────────────┘       └─────────────┘
       ↓                    ↓
┌─────────────┐       ┌─────────────┐
│ Type-safe   │       │  Error      │
│ Component   │       │ Handling    │
│    Data     │       └─────────────┘
└─────────────┘
       ↓
┌─────────────┐
│  Rendered   │
│     UI      │
└─────────────┘
```

---

## 6. User Journey Flowcharts

### 6.1 Complete Authentication Journey

```mermaid
flowchart TD
    Start([User opens app]) --> CheckAuth{Authenticated?}

    CheckAuth -->|No| LoginPage[Navigate to /auth/login]
    CheckAuth -->|Yes| Dashboard[Navigate to /dashboard]

    LoginPage --> AuthMethods[Load auth methods]
    AuthMethods --> ChooseMethod{Choose auth method}

    ChooseMethod -->|Password| PasswordForm[Fill password form]
    ChooseMethod -->|OAuth2| OAuth2Flow[OAuth2 provider flow]

    PasswordForm --> ValidateForm{Form valid?}
    ValidateForm -->|No| FormErrors[Show field errors]
    FormErrors --> PasswordForm
    ValidateForm -->|Yes| SubmitLogin[Submit login]

    OAuth2Flow --> OAuthCallback[OAuth2 callback]
    OAuthCallback --> ProcessAuth[Process auth response]

    SubmitLogin --> AuthResponse{Auth response}
    ProcessAuth --> AuthResponse

    AuthResponse -->|Success| CheckMFA{MFA required?}
    AuthResponse -->|Error| AuthError[Show auth error]
    AuthError --> LoginPage

    CheckMFA -->|No| SetSession[Set user session]
    CheckMFA -->|Yes| MFAChallenge[MFA challenge]

    MFAChallenge --> MFAValid{MFA valid?}
    MFAValid -->|No| MFAError[Show MFA error]
    MFAError --> MFAChallenge
    MFAValid -->|Yes| SetSession

    SetSession --> Dashboard
    Dashboard --> Success([Authentication complete])

    classDef start fill:#e8f5e8
    classDef decision fill:#fff3e0
    classDef process fill:#e3f2fd
    classDef error fill:#ffebee
    classDef success fill:#f1f8e9

    class Start,Success start
    class CheckAuth,ChooseMethod,ValidateForm,AuthResponse,CheckMFA,MFAValid decision
    class LoginPage,AuthMethods,PasswordForm,OAuth2Flow,SubmitLogin,OAuthCallback,ProcessAuth,SetSession,Dashboard,MFAChallenge process
    class FormErrors,AuthError,MFAError error
```

### 6.2 Payment Method Addition Journey

```mermaid
flowchart TD
    Start([User needs to add payment method]) --> PaymentsPage[Navigate to /payments]

    PaymentsPage --> LoadMethods[Load existing payment methods]
    LoadMethods --> ShowMethods[Display payment methods]
    ShowMethods --> AddButton[Click "Add Payment Method"]

    AddButton --> OpenModal[Open payment method modal]
    OpenModal --> StripeForm[Show Stripe card form]

    StripeForm --> FillCard[User fills card details]
    FillCard --> ValidateStripe{Stripe validation}

    ValidateStripe -->|Invalid| StripeError[Show card errors]
    StripeError --> FillCard

    ValidateStripe -->|Valid| FillBilling[Fill billing details]
    FillBilling --> ValidateBilling{Billing valid?}

    ValidateBilling -->|No| BillingError[Show billing errors]
    BillingError --> FillBilling

    ValidateBilling -->|Yes| SubmitForm[Submit form]
    SubmitForm --> StripeTokenize[Stripe tokenization]

    StripeTokenize --> TokenResponse{Tokenization result}
    TokenResponse -->|Error| TokenError[Show tokenization error]
    TokenError --> StripeForm

    TokenResponse -->|Success| APICall[API call with token]
    APICall --> BackendResponse{Backend response}

    BackendResponse -->|Error| APIError[Show API error]
    APIError --> StripeForm

    BackendResponse -->|Success| UpdateCache[Update payment methods cache]
    UpdateCache --> CloseModal[Close modal]
    CloseModal --> RefreshList[Refresh payment methods list]
    RefreshList --> ShowSuccess[Show success message]
    ShowSuccess --> Success([Payment method added])

    classDef start fill:#e8f5e8
    classDef decision fill:#fff3e0
    classDef process fill:#e3f2fd
    classDef error fill:#ffebee
    classDef success fill:#f1f8e9

    class Start,Success start
    class ValidateStripe,ValidateBilling,TokenResponse,BackendResponse decision
    class PaymentsPage,LoadMethods,ShowMethods,AddButton,OpenModal,StripeForm,FillCard,FillBilling,SubmitForm,StripeTokenize,APICall,UpdateCache,CloseModal,RefreshList,ShowSuccess process
    class StripeError,BillingError,TokenError,APIError error
```

### 6.3 Organization Creation Journey

```mermaid
flowchart TD
    Start([New user needs organization]) --> Dashboard[User on dashboard]

    Dashboard --> NoOrg{Has organization?}
    NoOrg -->|No| CreatePrompt[Show "Create Organization" prompt]
    NoOrg -->|Yes| ExistingOrg[Show existing organization]

    CreatePrompt --> ClickCreate[Click "Create Organization"]
    ClickCreate --> OpenModal[Open creation modal]

    OpenModal --> OrgForm[Show organization form]
    OrgForm --> FillDetails[Fill organization details]

    FillDetails --> ValidateForm{Form valid?}
    ValidateForm -->|No| FormErrors[Show validation errors]
    FormErrors --> FillDetails

    ValidateForm -->|Yes| SubmitOrg[Submit organization]
    SubmitOrg --> CreateResponse{Creation response}

    CreateResponse -->|Error| CreateError[Show creation error]
    CreateError --> OrgForm

    CreateResponse -->|Success| OrgCreated[Organization created]
    OrgCreated --> SetupWizard{Start setup wizard?}

    SetupWizard -->|Skip| CompleteBasic[Basic setup complete]
    SetupWizard -->|Yes| PaymentSetup[Payment method setup]

    PaymentSetup --> PaymentAdded[Payment method added]
    PaymentAdded --> PlanSelection[Plan selection]
    PlanSelection --> PlanSelected[Plan selected]
    PlanSelected --> TeamInvites[Team member invitations]
    TeamInvites --> InvitesSent[Invitations sent]
    InvitesSent --> CompleteAdvanced[Advanced setup complete]

    CompleteBasic --> Success([Organization ready])
    CompleteAdvanced --> Success
    ExistingOrg --> Success

    classDef start fill:#e8f5e8
    classDef decision fill:#fff3e0
    classDef process fill:#e3f2fd
    classDef error fill:#ffebee
    classDef success fill:#f1f8e9

    class Start,Success start
    class NoOrg,ValidateForm,CreateResponse,SetupWizard decision
    class Dashboard,CreatePrompt,ClickCreate,OpenModal,OrgForm,FillDetails,SubmitOrg,OrgCreated,PaymentSetup,PaymentAdded,PlanSelection,PlanSelected,TeamInvites,InvitesSent,CompleteBasic,CompleteAdvanced,ExistingOrg process
    class FormErrors,CreateError error
```

---

## 7. State Management Diagrams

### 7.1 Redux Store Structure

```
┌─────────────────────────────────────────────────────────────┐
│                      REDUX STORE                            │
├─────────────────────────────────────────────────────────────┤
│                   CLIENT STATE                              │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │   authSlice     │    │         uiSlice                 │ │
│  │                 │    │                                 │ │
│  │ • user          │    │ • theme                         │ │
│  │ • isAuth        │    │ • sidebar                       │ │
│  │ • loading       │    │ • notifications                 │ │
│  │ • error         │    │ • modals                        │ │
│  └─────────────────┘    └─────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                  SERVER STATE (RTK Query)                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   authApi   │  │   userApi   │  │   organizationApi   │ │
│  │             │  │             │  │                     │ │
│  │ • login     │  │ • getMe     │  │ • getOrgs          │ │
│  │ • logout    │  │ • updateMe  │  │ • createOrg        │ │
│  │ • session   │  │ • changePass│  │ • getMembers       │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ paymentApi  │  │ subscripApi │  │      mfaApi         │ │
│  │             │  │             │  │                     │ │
│  │ • getMethods│  │ • getCurrent│  │ • setupTOTP        │ │
│  │ • addMethod │  │ • getPlans  │  │ • verifyTOTP       │ │
│  │ • deleteMth │  │ • subscribe │  │ • getBackupCodes   │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 RTK Query Cache Tags

```mermaid
graph TB
    subgraph "Cache Tags"
        User[User]
        Session[Session]
        Organization[Organization]
        Member[Member]
        PaymentMethod[PaymentMethod]
        Transaction[Transaction]
        Subscription[Subscription]
        Invoice[Invoice]
        MFA[MFA]
    end

    subgraph "Queries"
        GetUser[getUser]
        GetSession[getSession]
        GetOrgs[getOrganizations]
        GetMembers[getMembers]
        GetPayments[getPaymentMethods]
        GetTransactions[getTransactions]
        GetSubscription[getSubscription]
        GetInvoices[getInvoices]
    end

    subgraph "Mutations"
        Login[login]
        UpdateUser[updateUser]
        CreateOrg[createOrganization]
        AddPayment[addPaymentMethod]
        CreateSub[createSubscription]
    end

    GetUser -.->|provides| User
    GetSession -.->|provides| Session
    GetOrgs -.->|provides| Organization
    GetMembers -.->|provides| Member
    GetPayments -.->|provides| PaymentMethod
    GetTransactions -.->|provides| Transaction
    GetSubscription -.->|provides| Subscription
    GetInvoices -.->|provides| Invoice

    Login -->|invalidates| User
    Login -->|invalidates| Session
    UpdateUser -->|invalidates| User
    CreateOrg -->|invalidates| Organization
    AddPayment -->|invalidates| PaymentMethod
    CreateSub -->|invalidates| Subscription

    classDef tag fill:#e3f2fd
    classDef query fill:#e8f5e8
    classDef mutation fill:#fff3e0

    class User,Session,Organization,Member,PaymentMethod,Transaction,Subscription,Invoice,MFA tag
    class GetUser,GetSession,GetOrgs,GetMembers,GetPayments,GetTransactions,GetSubscription,GetInvoices query
    class Login,UpdateUser,CreateOrg,AddPayment,CreateSub mutation
```

### 7.3 State Update Flow

```mermaid
sequenceDiagram
    participant Component
    participant Hook
    participant RTKSlice
    participant Middleware
    participant Store
    participant UI

    Component->>Hook: useAppDispatch()
    Hook->>RTKSlice: dispatch(action)
    RTKSlice->>RTKSlice: Reducer processes action
    RTKSlice->>Middleware: Action intercepted
    Middleware->>Store: State update
    Store->>Hook: useAppSelector() subscription
    Hook->>Component: Re-render with new state
    Component->>UI: Updated UI

    Note over Component,UI: Synchronous state update

    rect rgb(255, 245, 238)
        Note over Component,UI: RTK Query async flow
        Component->>Hook: useMutation()
        Hook->>RTKSlice: API call initiated
        RTKSlice->>Middleware: Async thunk
        Middleware-->>RTKSlice: Pending state
        RTKSlice-->>Store: Loading state
        Store-->>UI: Show loading

        Middleware->>Middleware: HTTP request
        Middleware-->>RTKSlice: Response
        RTKSlice->>RTKSlice: Cache update
        RTKSlice-->>Store: Success state
        Store-->>UI: Show data
    end
```

---

## Conclusion

These visual diagrams and flowcharts provide comprehensive documentation of the frontend architecture from multiple perspectives:

1. **System Overview** - High-level architecture and module dependencies
2. **Application Flow** - Bootstrap, navigation, and page transitions
3. **Component Hierarchy** - Complete component tree and relationships
4. **Data Flow** - State management, API integration, and caching
5. **User Journeys** - Complete user workflows and decision points
6. **State Management** - Redux store structure and RTK Query patterns

**Key Visual Insights:**

- **Modular Architecture**: Clear separation between feature modules
- **Type-Safe Pipeline**: Runtime validation with compile-time safety
- **Unidirectional Data Flow**: Predictable state updates through Redux
- **User-Centric Design**: Workflows optimized for user experience
- **Performance Optimization**: Strategic caching and code splitting

These diagrams serve as both documentation and implementation guides for developers and LLMs working with this complex React TypeScript frontend architecture.
