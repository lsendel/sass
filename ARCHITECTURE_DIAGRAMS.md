# Architecture Diagrams - Spring Boot Modulith Payment Platform

This document provides visual representations of the system architecture, data flows, and component interactions.

## System Overview

### High-Level Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        Browser[Web Browser]
        Mobile[Mobile App]
    end

    subgraph "Frontend Layer"
        React[React Application<br/>Port 3000]
        Redux[Redux Store<br/>State Management]
        RTK[RTK Query<br/>API Client]
    end

    subgraph "API Gateway"
        Gateway[Spring Boot Gateway<br/>Port 8080]
        Security[Spring Security<br/>OAuth2 + Session]
        CORS[CORS Filter]
    end

    subgraph "Application Layer - Spring Modulith"
        Auth[Auth Module<br/>OAuth2/PKCE]
        Payment[Payment Module<br/>Stripe Integration]
        User[User Module<br/>Multi-tenant Orgs]
        Subscription[Subscription Module<br/>Billing & Plans]
        Audit[Audit Module<br/>GDPR Compliance]
        Shared[Shared Module<br/>Security & Utils]
    end

    subgraph "Data Layer"
        DB[(PostgreSQL<br/>Primary Database)]
        Redis[(Redis<br/>Session Store)]
        Cache[(Redis<br/>Application Cache)]
    end

    subgraph "External Services"
        Stripe[Stripe API<br/>Payment Processing]
        Google[Google OAuth2]
        GitHub[GitHub OAuth2]
        Microsoft[Microsoft OAuth2]
        Email[Email Service<br/>SMTP]
    end

    subgraph "Infrastructure"
        Docker[Docker Containers]
        K8s[Kubernetes Cluster]
        Monitoring[Prometheus + Grafana]
        Logs[ELK Stack]
    end

    Browser --> React
    Mobile --> React
    React --> Redux
    Redux --> RTK
    RTK --> Gateway

    Gateway --> Security
    Security --> CORS
    Gateway --> Auth
    Gateway --> Payment
    Gateway --> User
    Gateway --> Subscription
    Gateway --> Audit

    Auth --> Shared
    Payment --> Shared
    User --> Shared
    Subscription --> Shared
    Audit --> Shared

    Auth --> DB
    Payment --> DB
    User --> DB
    Subscription --> DB
    Audit --> DB

    Auth --> Redis
    Payment --> Cache
    User --> Cache

    Payment --> Stripe
    Auth --> Google
    Auth --> GitHub
    Auth --> Microsoft
    Auth --> Email

    Gateway -.-> Docker
    Docker -.-> K8s
    K8s --> Monitoring
    K8s --> Logs

    classDef frontend fill:#e3f2fd
    classDef backend fill:#f3e5f5
    classDef data fill:#e8f5e8
    classDef external fill:#fff3e0
    classDef infra fill:#fce4ec

    class Browser,Mobile,React,Redux,RTK frontend
    class Gateway,Security,CORS,Auth,Payment,User,Subscription,Audit,Shared backend
    class DB,Redis,Cache data
    class Stripe,Google,GitHub,Microsoft,Email external
    class Docker,K8s,Monitoring,Logs infra
```

## Module Dependencies

### Spring Modulith Module Structure

```mermaid
graph TD
    subgraph "API Layer"
        AuthAPI[Auth API<br/>Controllers & DTOs]
        PaymentAPI[Payment API<br/>Controllers & DTOs]
        UserAPI[User API<br/>Controllers & DTOs]
        SubscriptionAPI[Subscription API<br/>Controllers & DTOs]
        AuditAPI[Audit API<br/>Controllers & DTOs]
    end

    subgraph "Business Logic Layer"
        AuthInternal[Auth Internal<br/>Services & Entities]
        PaymentInternal[Payment Internal<br/>Services & Entities]
        UserInternal[User Internal<br/>Services & Entities]
        SubscriptionInternal[Subscription Internal<br/>Services & Entities]
        AuditInternal[Audit Internal<br/>Services & Entities]
    end

    subgraph "Event Layer"
        AuthEvents[Auth Events<br/>UserRegistered, UserLogin]
        PaymentEvents[Payment Events<br/>PaymentCompleted, PaymentFailed]
        UserEvents[User Events<br/>OrganizationCreated, UserInvited]
        SubscriptionEvents[Subscription Events<br/>SubscriptionCreated, BillingCycleCompleted]
    end

    subgraph "Shared Infrastructure"
        SharedSecurity[Shared Security<br/>Authentication & Authorization]
        SharedConfig[Shared Configuration<br/>Properties & Beans]
        SharedUtils[Shared Utilities<br/>Validation & Serialization]
    end

    AuthAPI --> AuthInternal
    PaymentAPI --> PaymentInternal
    UserAPI --> UserInternal
    SubscriptionAPI --> SubscriptionInternal
    AuditAPI --> AuditInternal

    AuthInternal --> AuthEvents
    PaymentInternal --> PaymentEvents
    UserInternal --> UserEvents
    SubscriptionInternal --> SubscriptionEvents

    AuthInternal --> SharedSecurity
    PaymentInternal --> SharedSecurity
    UserInternal --> SharedSecurity
    SubscriptionInternal --> SharedSecurity
    AuditInternal --> SharedSecurity

    AuthInternal --> SharedConfig
    PaymentInternal --> SharedConfig
    UserInternal --> SharedConfig
    SubscriptionInternal --> SharedConfig
    AuditInternal --> SharedConfig

    AuthInternal --> SharedUtils
    PaymentInternal --> SharedUtils
    UserInternal --> SharedUtils
    SubscriptionInternal --> SharedUtils
    AuditInternal --> SharedUtils

    AuthEvents -.->|Event Publishing| PaymentInternal
    UserEvents -.->|Event Publishing| AuditInternal
    PaymentEvents -.->|Event Publishing| AuditInternal
    SubscriptionEvents -.->|Event Publishing| AuditInternal

    classDef api fill:#e3f2fd
    classDef internal fill:#f3e5f5
    classDef events fill:#e8f5e8
    classDef shared fill:#fff3e0

    class AuthAPI,PaymentAPI,UserAPI,SubscriptionAPI,AuditAPI api
    class AuthInternal,PaymentInternal,UserInternal,SubscriptionInternal,AuditInternal internal
    class AuthEvents,PaymentEvents,UserEvents,SubscriptionEvents events
    class SharedSecurity,SharedConfig,SharedUtils shared
```

## Authentication Flow

### OAuth2/PKCE Authentication Sequence

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant B as Backend
    participant P as OAuth2 Provider
    participant DB as Database
    participant R as Redis

    U->>F: Click "Login with Google"
    F->>B: GET /auth/oauth2/providers
    B->>F: Available providers list
    F->>B: GET /auth/oauth2/authorize/google
    B->>B: Generate PKCE challenge
    B->>F: Authorization URL + PKCE params
    F->>U: Redirect to OAuth2 provider
    U->>P: Authenticate with provider
    P->>U: Redirect with auth code
    U->>F: Callback with auth code
    F->>B: GET /auth/oauth2/callback/google?code=...
    B->>P: Exchange code for tokens (PKCE)
    P->>B: Access token + user info
    B->>DB: Create/update user record
    B->>R: Create session
    B->>F: Session cookie + user info
    F->>F: Update Redux state
    F->>U: Redirect to dashboard

    Note over B,P: PKCE verification ensures security
    Note over B,R: Session stored in Redis with TTL
    Note over F: State management via Redux
```

## Payment Processing Flow

### Stripe Payment Intent Flow

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant B as Backend
    participant S as Stripe API
    participant DB as Database
    participant W as Stripe Webhook

    U->>F: Select subscription plan
    F->>B: POST /api/v1/payments/intents
    B->>S: Create PaymentIntent
    S->>B: PaymentIntent with client_secret
    B->>DB: Store payment record
    B->>F: PaymentIntent response
    F->>F: Initialize Stripe Elements
    U->>F: Enter payment details
    F->>S: Confirm payment (client-side)
    S->>B: Webhook: payment_intent.succeeded
    B->>B: Validate webhook signature
    B->>DB: Update payment status
    B->>B: Publish PaymentCompleted event
    B->>F: Payment confirmation
    F->>U: Show success message

    Note over S,B: Webhook provides reliable payment confirmation
    Note over B: Event-driven architecture for payment updates
    Note over F,S: Stripe Elements handle PCI compliance
```

## Data Flow Architecture

### Multi-Tenant Data Access

```mermaid
graph TD
    subgraph "Request Flow"
        Request[HTTP Request]
        Auth[Authentication Filter]
        Tenant[Tenant Context Filter]
        Controller[Controller Layer]
        Service[Service Layer]
        Repository[Repository Layer]
    end

    subgraph "Security Context"
        UserPrincipal[User Principal]
        OrgContext[Organization Context]
        TenantGuard[Tenant Guard]
    end

    subgraph "Database Layer"
        DB[(PostgreSQL)]
        OrgTable[Organizations Table]
        UserTable[Users Table]
        PaymentTable[Payments Table]
        SubscriptionTable[Subscriptions Table]
    end

    Request --> Auth
    Auth --> UserPrincipal
    Auth --> Tenant
    Tenant --> OrgContext
    Tenant --> Controller
    Controller --> TenantGuard
    TenantGuard --> Service
    Service --> Repository
    Repository --> DB

    DB --> OrgTable
    DB --> UserTable
    DB --> PaymentTable
    DB --> SubscriptionTable

    OrgContext -.->|Filters queries by org_id| Repository
    TenantGuard -.->|Validates access| Service

    classDef security fill:#ffebee
    classDef business fill:#e8f5e8
    classDef data fill:#e3f2fd

    class Auth,Tenant,UserPrincipal,OrgContext,TenantGuard security
    class Controller,Service business
    class Repository,DB,OrgTable,UserTable,PaymentTable,SubscriptionTable data
```

## Deployment Architecture

### Kubernetes Deployment

```mermaid
graph TB
    subgraph "Ingress Layer"
        Ingress[Nginx Ingress Controller]
        TLS[TLS Termination]
    end

    subgraph "Application Pods"
        Frontend1[Frontend Pod 1<br/>React App]
        Frontend2[Frontend Pod 2<br/>React App]
        Backend1[Backend Pod 1<br/>Spring Boot]
        Backend2[Backend Pod 2<br/>Spring Boot]
        Backend3[Backend Pod 3<br/>Spring Boot]
    end

    subgraph "Data Layer"
        PostgresPod[PostgreSQL<br/>StatefulSet]
        RedisPod[Redis<br/>StatefulSet]
        PVC1[Persistent Volume<br/>PostgreSQL Data]
        PVC2[Persistent Volume<br/>Redis Data]
    end

    subgraph "Configuration"
        ConfigMap[ConfigMap<br/>App Configuration]
        Secret[Secret<br/>Sensitive Data]
    end

    subgraph "Monitoring"
        Prometheus[Prometheus<br/>Metrics Collection]
        Grafana[Grafana<br/>Visualization]
        AlertManager[AlertManager<br/>Alerting]
    end

    subgraph "Logging"
        Fluentd[Fluentd<br/>Log Collection]
        Elasticsearch[Elasticsearch<br/>Log Storage]
        Kibana[Kibana<br/>Log Visualization]
    end

    Ingress --> TLS
    TLS --> Frontend1
    TLS --> Frontend2
    TLS --> Backend1
    TLS --> Backend2
    TLS --> Backend3

    Backend1 --> PostgresPod
    Backend2 --> PostgresPod
    Backend3 --> PostgresPod
    Backend1 --> RedisPod
    Backend2 --> RedisPod
    Backend3 --> RedisPod

    PostgresPod --> PVC1
    RedisPod --> PVC2

    Backend1 --> ConfigMap
    Backend2 --> ConfigMap
    Backend3 --> ConfigMap
    Backend1 --> Secret
    Backend2 --> Secret
    Backend3 --> Secret

    Backend1 -.->|Metrics| Prometheus
    Backend2 -.->|Metrics| Prometheus
    Backend3 -.->|Metrics| Prometheus
    Prometheus --> Grafana
    Prometheus --> AlertManager

    Backend1 -.->|Logs| Fluentd
    Backend2 -.->|Logs| Fluentd
    Backend3 -.->|Logs| Fluentd
    Fluentd --> Elasticsearch
    Elasticsearch --> Kibana

    classDef ingress fill:#e3f2fd
    classDef app fill:#f3e5f5
    classDef data fill:#e8f5e8
    classDef config fill:#fff3e0
    classDef monitoring fill:#fce4ec
    classDef logging fill:#f1f8e9

    class Ingress,TLS ingress
    class Frontend1,Frontend2,Backend1,Backend2,Backend3 app
    class PostgresPod,RedisPod,PVC1,PVC2 data
    class ConfigMap,Secret config
    class Prometheus,Grafana,AlertManager monitoring
    class Fluentd,Elasticsearch,Kibana logging
```

## Event-Driven Architecture

### Inter-Module Communication

```mermaid
graph TD
    subgraph "Event Publishers"
        AuthModule[Auth Module]
        UserModule[User Module]
        PaymentModule[Payment Module]
        SubscriptionModule[Subscription Module]
    end

    subgraph "Event Bus"
        EventBus[Spring Application Events<br/>@EventListener]
    end

    subgraph "Event Consumers"
        AuditModule[Audit Module<br/>Logs all events]
        EmailService[Email Service<br/>Sends notifications]
        MetricsService[Metrics Service<br/>Records business metrics]
    end

    subgraph "Event Types"
        UserRegistered[UserRegistered]
        UserLogin[UserLogin]
        OrganizationCreated[OrganizationCreated]
        PaymentCompleted[PaymentCompleted]
        PaymentFailed[PaymentFailed]
        SubscriptionCreated[SubscriptionCreated]
        SubscriptionCanceled[SubscriptionCanceled]
    end

    AuthModule -->|Publishes| UserRegistered
    AuthModule -->|Publishes| UserLogin
    UserModule -->|Publishes| OrganizationCreated
    PaymentModule -->|Publishes| PaymentCompleted
    PaymentModule -->|Publishes| PaymentFailed
    SubscriptionModule -->|Publishes| SubscriptionCreated
    SubscriptionModule -->|Publishes| SubscriptionCanceled

    UserRegistered --> EventBus
    UserLogin --> EventBus
    OrganizationCreated --> EventBus
    PaymentCompleted --> EventBus
    PaymentFailed --> EventBus
    SubscriptionCreated --> EventBus
    SubscriptionCanceled --> EventBus

    EventBus --> AuditModule
    EventBus --> EmailService
    EventBus --> MetricsService

    classDef publisher fill:#e3f2fd
    classDef event fill:#f3e5f5
    classDef consumer fill:#e8f5e8
    classDef bus fill:#fff3e0

    class AuthModule,UserModule,PaymentModule,SubscriptionModule publisher
    class UserRegistered,UserLogin,OrganizationCreated,PaymentCompleted,PaymentFailed,SubscriptionCreated,SubscriptionCanceled event
    class AuditModule,EmailService,MetricsService consumer
    class EventBus bus
```

## Security Architecture

### Security Layers and Controls

```mermaid
graph TB
    subgraph "Network Security"
        WAF[Web Application Firewall]
        LoadBalancer[Load Balancer]
        TLS[TLS 1.3 Encryption]
    end

    subgraph "Application Security"
        CORS[CORS Protection]
        CSP[Content Security Policy]
        RateLimit[Rate Limiting]
        InputValidation[Input Validation]
    end

    subgraph "Authentication & Authorization"
        OAuth2[OAuth2/PKCE Flow]
        SessionMgmt[Session Management]
        JWTValidation[JWT Validation]
        RBAC[Role-Based Access Control]
        TenantIsolation[Tenant Isolation]
    end

    subgraph "Data Protection"
        Encryption[Data Encryption at Rest]
        PIIRedaction[PII Redaction]
        AuditLogging[Audit Logging]
        BackupEncryption[Backup Encryption]
    end

    subgraph "Infrastructure Security"
        NetworkPolicies[Kubernetes Network Policies]
        PodSecurity[Pod Security Standards]
        SecretMgmt[Secret Management]
        VulnScanning[Vulnerability Scanning]
    end

    WAF --> LoadBalancer
    LoadBalancer --> TLS
    TLS --> CORS
    CORS --> CSP
    CSP --> RateLimit
    RateLimit --> InputValidation

    InputValidation --> OAuth2
    OAuth2 --> SessionMgmt
    SessionMgmt --> JWTValidation
    JWTValidation --> RBAC
    RBAC --> TenantIsolation

    TenantIsolation --> Encryption
    Encryption --> PIIRedaction
    PIIRedaction --> AuditLogging
    AuditLogging --> BackupEncryption

    BackupEncryption --> NetworkPolicies
    NetworkPolicies --> PodSecurity
    PodSecurity --> SecretMgmt
    SecretMgmt --> VulnScanning

    classDef network fill:#ffebee
    classDef app fill:#e8f5e8
    classDef auth fill:#e3f2fd
    classDef data fill:#f3e5f5
    classDef infra fill:#fff3e0

    class WAF,LoadBalancer,TLS network
    class CORS,CSP,RateLimit,InputValidation app
    class OAuth2,SessionMgmt,JWTValidation,RBAC,TenantIsolation auth
    class Encryption,PIIRedaction,AuditLogging,BackupEncryption data
    class NetworkPolicies,PodSecurity,SecretMgmt,VulnScanning infra
```

## Frontend Architecture

### React Component Hierarchy

```mermaid
graph TD
    subgraph "Application Root"
        App[App Component<br/>Router & Providers]
        Store[Redux Store<br/>State Management]
        Router[React Router<br/>Navigation]
    end

    subgraph "Layout Components"
        AuthLayout[Auth Layout<br/>Login/Register Pages]
        DashboardLayout[Dashboard Layout<br/>Main Application]
        PublicLayout[Public Layout<br/>Landing Pages]
    end

    subgraph "Page Components"
        LoginPage[Login Page]
        DashboardPage[Dashboard Page]
        PaymentsPage[Payments Page]
        SettingsPage[Settings Page]
        OrganizationsPage[Organizations Page]
    end

    subgraph "Feature Components"
        AuthComponents[Auth Components<br/>Login Forms, OAuth Buttons]
        PaymentComponents[Payment Components<br/>Stripe Elements, Payment Forms]
        UserComponents[User Components<br/>Profile, Organization Management]
        UIComponents[UI Components<br/>Buttons, Modals, Forms]
    end

    subgraph "API Layer"
        AuthAPI[Auth API Slice<br/>RTK Query]
        PaymentAPI[Payment API Slice<br/>RTK Query]
        UserAPI[User API Slice<br/>RTK Query]
        BaseAPI[Base API Configuration<br/>Axios + RTK Query]
    end

    App --> Store
    App --> Router
    Router --> AuthLayout
    Router --> DashboardLayout
    Router --> PublicLayout

    AuthLayout --> LoginPage
    DashboardLayout --> DashboardPage
    DashboardLayout --> PaymentsPage
    DashboardLayout --> SettingsPage
    DashboardLayout --> OrganizationsPage

    LoginPage --> AuthComponents
    PaymentsPage --> PaymentComponents
    SettingsPage --> UserComponents
    OrganizationsPage --> UserComponents

    AuthComponents --> UIComponents
    PaymentComponents --> UIComponents
    UserComponents --> UIComponents

    Store --> AuthAPI
    Store --> PaymentAPI
    Store --> UserAPI
    AuthAPI --> BaseAPI
    PaymentAPI --> BaseAPI
    UserAPI --> BaseAPI

    classDef root fill:#e3f2fd
    classDef layout fill:#f3e5f5
    classDef page fill:#e8f5e8
    classDef component fill:#fff3e0
    classDef api fill:#fce4ec

    class App,Store,Router root
    class AuthLayout,DashboardLayout,PublicLayout layout
    class LoginPage,DashboardPage,PaymentsPage,SettingsPage,OrganizationsPage page
    class AuthComponents,PaymentComponents,UserComponents,UIComponents component
    class AuthAPI,PaymentAPI,UserAPI,BaseAPI api
```

## Monitoring and Observability

### Observability Stack

```mermaid
graph TB
    subgraph "Application Layer"
        Backend[Spring Boot Backend<br/>Micrometer Metrics]
        Frontend[React Frontend<br/>Performance Metrics]
    end

    subgraph "Metrics Collection"
        Prometheus[Prometheus<br/>Metrics Storage]
        ActuatorEndpoints[Spring Actuator<br/>Health & Metrics Endpoints]
    end

    subgraph "Visualization"
        Grafana[Grafana<br/>Dashboards & Alerting]
        BusinessDashboard[Business Metrics Dashboard]
        TechnicalDashboard[Technical Metrics Dashboard]
        AlertDashboard[Alert Management]
    end

    subgraph "Logging"
        ApplicationLogs[Application Logs<br/>Structured JSON]
        AuditLogs[Audit Logs<br/>Security & Compliance]
        AccessLogs[Access Logs<br/>HTTP Requests]
        LogAggregation[Log Aggregation<br/>ELK Stack]
    end

    subgraph "Alerting"
        AlertManager[Alert Manager<br/>Alert Processing]
        NotificationChannels[Notification Channels<br/>Email, Slack, PagerDuty]
        EscalationPolicies[Escalation Policies<br/>On-call Management]
    end

    subgraph "Distributed Tracing"
        Tracing[Spring Cloud Sleuth<br/>Request Tracing]
        SpanCollection[Span Collection<br/>Zipkin/Jaeger]
        TraceAnalysis[Trace Analysis<br/>Performance Bottlenecks]
    end

    Backend --> ActuatorEndpoints
    Backend --> ApplicationLogs
    Backend --> AuditLogs
    Backend --> Tracing
    Frontend --> ApplicationLogs

    ActuatorEndpoints --> Prometheus
    ApplicationLogs --> LogAggregation
    AuditLogs --> LogAggregation
    AccessLogs --> LogAggregation
    Tracing --> SpanCollection

    Prometheus --> Grafana
    Grafana --> BusinessDashboard
    Grafana --> TechnicalDashboard
    Grafana --> AlertDashboard

    Grafana --> AlertManager
    AlertManager --> NotificationChannels
    NotificationChannels --> EscalationPolicies

    SpanCollection --> TraceAnalysis

    classDef app fill:#e3f2fd
    classDef metrics fill:#f3e5f5
    classDef viz fill:#e8f5e8
    classDef logging fill:#fff3e0
    classDef alerting fill:#fce4ec
    classDef tracing fill:#f1f8e9

    class Backend,Frontend app
    class Prometheus,ActuatorEndpoints metrics
    class Grafana,BusinessDashboard,TechnicalDashboard,AlertDashboard viz
    class ApplicationLogs,AuditLogs,AccessLogs,LogAggregation logging
    class AlertManager,NotificationChannels,EscalationPolicies alerting
    class Tracing,SpanCollection,TraceAnalysis tracing
```

## Integration Patterns

### External Service Integration

```mermaid
graph LR
    subgraph "Payment Platform"
        PaymentService[Payment Service]
        AuthService[Auth Service]
        NotificationService[Notification Service]
        AuditService[Audit Service]
    end

    subgraph "Stripe Integration"
        StripeAPI[Stripe API]
        StripeWebhooks[Stripe Webhooks]
        StripeEvents[Stripe Events]
    end

    subgraph "OAuth Providers"
        GoogleOAuth[Google OAuth2]
        GitHubOAuth[GitHub OAuth2]
        MicrosoftOAuth[Microsoft OAuth2]
    end

    subgraph "Communication Services"
        EmailProvider[Email Service Provider<br/>SendGrid/AWS SES]
        SMSProvider[SMS Provider<br/>Twilio]
    end

    subgraph "Monitoring & Logging"
        MetricsCollector[Metrics Collector<br/>Prometheus]
        LogAggregator[Log Aggregator<br/>ELK Stack]
        ErrorTracking[Error Tracking<br/>Sentry]
    end

    PaymentService -->|API Calls| StripeAPI
    PaymentService <--|Webhooks| StripeWebhooks
    PaymentService -->|Event Processing| StripeEvents

    AuthService -->|OAuth Flow| GoogleOAuth
    AuthService -->|OAuth Flow| GitHubOAuth
    AuthService -->|OAuth Flow| MicrosoftOAuth

    NotificationService -->|Send Emails| EmailProvider
    NotificationService -->|Send SMS| SMSProvider

    PaymentService -.->|Metrics| MetricsCollector
    AuthService -.->|Metrics| MetricsCollector
    PaymentService -.->|Logs| LogAggregator
    AuthService -.->|Logs| LogAggregator
    PaymentService -.->|Errors| ErrorTracking
    AuthService -.->|Errors| ErrorTracking

    AuditService -.->|Compliance Logs| LogAggregator

    classDef internal fill:#e3f2fd
    classDef payment fill:#f3e5f5
    classDef auth fill:#e8f5e8
    classDef communication fill:#fff3e0
    classDef monitoring fill:#fce4ec

    class PaymentService,AuthService,NotificationService,AuditService internal
    class StripeAPI,StripeWebhooks,StripeEvents payment
    class GoogleOAuth,GitHubOAuth,MicrosoftOAuth auth
    class EmailProvider,SMSProvider communication
    class MetricsCollector,LogAggregator,ErrorTracking monitoring
```

---

## Diagram Key

### Color Coding
- 🔵 **Blue**: Frontend components and user interfaces
- 🟣 **Purple**: Backend services and business logic
- 🟢 **Green**: Data storage and caching layers
- 🟡 **Yellow**: External services and integrations
- 🔴 **Red**: Security and authentication components
- 🟠 **Orange**: Infrastructure and deployment components

### Component Types
- **Rectangles**: Services, modules, and applications
- **Cylinders**: Databases and storage systems
- **Diamonds**: Decision points and gateways
- **Circles**: Events and messages
- **Hexagons**: External systems and APIs

### Connection Types
- **Solid Lines**: Direct dependencies and data flow
- **Dashed Lines**: Event publishing and asynchronous communication
- **Dotted Lines**: Monitoring and observability connections

---

This architecture documentation provides comprehensive visual representations of the Spring Boot Modulith Payment Platform, enabling better understanding of system components, data flows, and integration patterns.